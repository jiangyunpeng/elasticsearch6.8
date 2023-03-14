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

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPool.Names;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * @see org.elasticsearch.threadpool.ESThreadPoolTestCase
 * @author bairen
 * @description
 **/
public class ThreadPoolTest {

    @Test
    public void testReject() throws InterruptedException {
        String threadPoolName = Names.GET;
        int size = 4;
        int queueSize = 16;

        Settings nodeSettings = Settings.builder()
            .put("node.name", "testRejectedExecutionCounter")
            .put("thread_pool." + threadPoolName + ".size", size)
            .put("thread_pool." + threadPoolName + ".queue_size", queueSize)
            .build();

        final CountDownLatch latch = new CountDownLatch(size);
        final CountDownLatch block = new CountDownLatch(1);

        ThreadPool threadPool = new ThreadPool(nodeSettings);
        for (int i = 0; i < size; i++) {
            threadPool.executor(threadPoolName).execute(() -> {
                try {
                    latch.countDown();
                    block.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }


        latch.await();


        // these tasks will fill the thread pool queue
        for (int i = 0; i < queueSize; i++) {
            threadPool.executor(threadPoolName).execute(() -> {
            });
        }
        // these tasks will be rejected
        try {
            threadPool.executor(threadPoolName).execute(() -> {
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
