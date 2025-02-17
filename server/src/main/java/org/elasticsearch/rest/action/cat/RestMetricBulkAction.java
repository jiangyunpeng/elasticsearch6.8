/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.rest.action.cat;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Table;
import org.elasticsearch.metric.Meter;
import org.elasticsearch.metric.MetricRegistry;
import org.elasticsearch.metric.MetricRegistry.MetricEntry;
import org.elasticsearch.rest.RestRequest;

import java.util.Comparator;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestMetricBulkAction extends AbstractCatAction {
    @Override
    public String getName() {
        return "cat_metric_action";
    }

    @Override
    public List<Route> routes() {
        return singletonList(new Route(GET, "/_cat/metric/bulk"));
    }

    private static double roundx1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    @Override
    protected RestChannelConsumer doCatRequest(RestRequest request, NodeClient client) {
        return channel -> {
            Table table = getTableWithHeader(request);
            Comparator<MetricEntry> comparator = (m1, m2) -> Double.valueOf(m2.meter("docCount").getOneMinuteRate()).compareTo(m1.meter("docCount").getOneMinuteRate());
            for (MetricEntry entry : MetricRegistry.measure("bulk").entries(comparator)) {
                String shardName = entry.getName();
                Meter count = entry.meter("docCount");
                table.startRow();
                table.addCell(shardName);
                table.addCell(count.getCount());    //bulk docCount
                table.addCell(roundx1(count.getOneMinuteRate()));//qps
                table.endRow();
            }
            channel.sendResponse(RestTable.buildResponse(table, channel));
        };
    }


    @Override
    protected void documentation(StringBuilder sb) {
        sb.append("/_cat/metric/bulk\n");
    }

    @Override
    protected Table getTableWithHeader(RestRequest request) {
        Table table = new Table();
        table.startHeaders();
        table.addCell("index", "alias:i,idx;desc:index name");
        table.addCell("bulk.count", "alias:dc,docsCount;text-align:right;desc:bulk doc count");
        table.addCell("bulk.qps", "alias:qps,docsQPS;text-align:right;desc:bulk doc qps");
        table.endHeaders();
        return table;
    }
}
