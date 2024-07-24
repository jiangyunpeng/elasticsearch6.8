/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.gateway;

import com.carrotsearch.hppc.ObjectFloatHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.SourceLogger;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.discovery.zen.ElectMasterService;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndicesService;

import java.util.Arrays;
import java.util.Map;

public class Gateway {

    private static final Logger logger = LogManager.getLogger(Gateway.class);

    private final ClusterService clusterService;

    private final TransportNodesListGatewayMetaState listGatewayMetaState;

    private final int minimumMasterNodes;
    private final IndicesService indicesService;

    public Gateway(Settings settings, ClusterService clusterService,
                   TransportNodesListGatewayMetaState listGatewayMetaState,
                   IndicesService indicesService) {
        this.indicesService = indicesService;
        this.clusterService = clusterService;
        this.listGatewayMetaState = listGatewayMetaState;
        this.minimumMasterNodes = ElectMasterService.DISCOVERY_ZEN_MINIMUM_MASTER_NODES_SETTING.get(settings);
    }

    public void performStateRecovery(final GatewayStateRecoveredListener listener) throws GatewayException {
        // 获取集群中所有 master 节点的 ID
        String[] nodesIds = clusterService.state().nodes().getMasterNodes().keys().toArray(String.class);
        SourceLogger.info("performing state recovery from {}", Arrays.toString(nodesIds));

        // 从 master 节点获取元数据状态
        TransportNodesListGatewayMetaState.NodesGatewayMetaState nodesState = listGatewayMetaState.list(nodesIds, null).actionGet();

        int requiredAllocation = Math.max(1, minimumMasterNodes);


        if (nodesState.hasFailures()) {
            for (FailedNodeException failedNodeException : nodesState.failures()) {
                logger.warn("failed to fetch state from node", failedNodeException);
            }
        }
        // 用于存储索引和其出现的次数的映射
        ObjectFloatHashMap<Index> indices = new ObjectFloatHashMap<>();
        MetaData electedGlobalState = null;
        int found = 0;
        // 遍历所有节点的状态，找出最新的全局元数据和所有索引的元数据
        for (TransportNodesListGatewayMetaState.NodeGatewayMetaState nodeState : nodesState.getNodes()) {
            if (nodeState.metaData() == null) {
                continue;
            }
            found++;
            if (electedGlobalState == null) {
                electedGlobalState = nodeState.metaData();
            } else if (nodeState.metaData().version() > electedGlobalState.version()) { //选择版本最大作为最新的
                electedGlobalState = nodeState.metaData();
            }
            for (ObjectCursor<IndexMetaData> cursor : nodeState.metaData().indices().values()) {
                indices.addTo(cursor.value.getIndex(), 1);
            }
        }
        // 如果找到的元数据状态数小于需要的分配数量，触发失败回调
        if (found < requiredAllocation) {
            listener.onFailure("found [" + found + "] metadata states, required [" + requiredAllocation + "]");
            return;
        }
        // 更新全局状态，清除索引元数据，将它们放入下一个阶段的选择过程
        MetaData.Builder metaDataBuilder = MetaData.builder(electedGlobalState).removeAllIndices();

        assert !indices.containsKey(null);
        final Object[] keys = indices.keys;
        // 遍历所有索引，选出最新的索引元数据
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                Index index = (Index) keys[i];
                IndexMetaData electedIndexMetaData = null;
                int indexMetaDataCount = 0;
                // 遍历所有节点的状态，找出最新的全局元数据和所有索引的元数据
                for (TransportNodesListGatewayMetaState.NodeGatewayMetaState nodeState : nodesState.getNodes()) {
                    if (nodeState.metaData() == null) {
                        continue;
                    }
                    IndexMetaData indexMetaData = nodeState.metaData().index(index);
                    if (indexMetaData == null) {
                        continue;
                    }
                    if (electedIndexMetaData == null) {
                        electedIndexMetaData = indexMetaData;
                    } else if (indexMetaData.getVersion() > electedIndexMetaData.getVersion()) {
                        electedIndexMetaData = indexMetaData;
                    }
                    indexMetaDataCount++;
                }
                if (electedIndexMetaData != null) {
                    // 如果找到的元数据状态数小于需要的分配数量，触发失败回调
                    if (indexMetaDataCount < requiredAllocation) {
                        logger.debug("[{}] found [{}], required [{}], not adding", index, indexMetaDataCount, requiredAllocation);
                    } // TODO if this logging statement is correct then we are missing an else here
                    try {
                        if (electedIndexMetaData.getState() == IndexMetaData.State.OPEN) {
                            // 验证能否实际创建此索引，如果不能则将其恢复为关闭状态
                            indicesService.verifyIndexMetadata(electedIndexMetaData, electedIndexMetaData);
                        }
                    } catch (Exception e) {
                        final Index electedIndex = electedIndexMetaData.getIndex();
                        logger.warn(() -> new ParameterizedMessage("recovering index {} failed - recovering as closed", electedIndex), e);
                        electedIndexMetaData = IndexMetaData.builder(electedIndexMetaData).state(IndexMetaData.State.CLOSE).build();
                    }

                    metaDataBuilder.put(electedIndexMetaData, false);
                }
            }
        }
        // 构建集群状态并触发成功回调
        final ClusterState.Builder builder = upgradeAndArchiveUnknownOrInvalidSettings(metaDataBuilder);
        listener.onSuccess(builder.build());
    }

    ClusterState.Builder upgradeAndArchiveUnknownOrInvalidSettings(MetaData.Builder metaDataBuilder) {
        final ClusterSettings clusterSettings = clusterService.getClusterSettings();
        metaDataBuilder.persistentSettings(
            clusterSettings.archiveUnknownOrInvalidSettings(
                clusterSettings.upgradeSettings(metaDataBuilder.persistentSettings()),
                e -> logUnknownSetting("persistent", e),
                (e, ex) -> logInvalidSetting("persistent", e, ex)));
        metaDataBuilder.transientSettings(
            clusterSettings.archiveUnknownOrInvalidSettings(
                clusterSettings.upgradeSettings(metaDataBuilder.transientSettings()),
                e -> logUnknownSetting("transient", e),
                (e, ex) -> logInvalidSetting("transient", e, ex)));
        ClusterState.Builder builder = clusterService.newClusterStateBuilder();
        builder.metaData(metaDataBuilder);
        return builder;
    }

    private void logUnknownSetting(String settingType, Map.Entry<String, String> e) {
        logger.warn("ignoring unknown {} setting: [{}] with value [{}]; archiving", settingType, e.getKey(), e.getValue());
    }

    private void logInvalidSetting(String settingType, Map.Entry<String, String> e, IllegalArgumentException ex) {
        logger.warn(() -> new ParameterizedMessage("ignoring invalid {} setting: [{}] with value [{}]; archiving",
                    settingType, e.getKey(), e.getValue()), ex);
    }

    public interface GatewayStateRecoveredListener {
        void onSuccess(ClusterState build);

        void onFailure(String s);
    }
}
