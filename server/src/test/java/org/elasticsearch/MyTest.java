/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch;

import org.elasticsearch.metric.MetricRegistry;
import org.elasticsearch.metric.MetricRegistry.Measure;
import org.elasticsearch.metric.Tsdb;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class MyTest {

    @Test
    public void testMetric() throws InterruptedException {
        Measure measure = MetricRegistry.measure("bulk");

        AtomicBoolean stop = new AtomicBoolean();
        Thread t = new Thread(() -> {
            while (!stop.get()) {
                measure.entry("fun-test").meter("doc").mark(1);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
        System.out.println("wait");
        Thread.sleep(120*1000);
        System.out.println("end");
//        System.out.println("wait 5s");
//        Thread.sleep(5*1000);
//        Tsdb.writeRollingData(measure.rolling());
//
//        System.out.println("wait 5s");
//        Thread.sleep(5*1000);
//        Tsdb.writeRollingData(measure.rolling());
    }
}
