/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.my;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.bytes.ReleasablePagedBytesReference;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.PageCacheRecycler;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.SearchSlowLog;
import org.elasticsearch.indices.breaker.NoneCircuitBreakerService;
import org.junit.Test;


import java.io.IOException;

/**
 * PageCacheRecycler 基于页的Recycler，默认每页16kb，按byte,int,long,object拆分
 * @see org.elasticsearch.common.recycler.AbstractRecyclerTestCase
 * @see org.elasticsearch.common.network.NetworkModuleTests
 * @author bairen
 * @description
 **/
public class PageCacheRecyclerTest {

    @Test
    public void test1() throws IOException {
        Settings settings = Settings.builder()
            .build();
        //基于page的Recycler
        PageCacheRecycler pageCacheRecycler = new PageCacheRecycler(settings);
        BigArrays bigArrays = new BigArrays(pageCacheRecycler, new NoneCircuitBreakerService(), CircuitBreaker.REQUEST);
        ReleasableBytesStreamOutput output = new ReleasableBytesStreamOutput(bigArrays);

        output.writeByte((byte)'h');
        output.writeByte((byte)'e');
        output.writeByte((byte)'l');
        output.writeByte((byte)'l');
        output.writeByte((byte)'o');

        ReleasablePagedBytesReference reference = output.bytes();


    }

    @Test
    public void test2() {
        Settings settings = Settings.builder()
            .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
            .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
            .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
            .put(IndexMetaData.SETTING_INDEX_UUID, UUIDs.randomBase64UUID())
            .build();
        IndexMetaData metaData = IndexMetaData.builder(IndexMetaData.INDEX_UUID_NA_VALUE).settings(settings).build();
        IndexSettings indexSettings = new IndexSettings(metaData, Settings.EMPTY);
        for (int i = 0; i < 10; ++i) {
            long begin = System.currentTimeMillis();
            new SearchSlowLog(indexSettings);
            long end = System.currentTimeMillis();
            System.out.println(end - begin);
        }

    }
}
