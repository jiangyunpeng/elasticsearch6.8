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

import org.elasticsearch.common.recycler.DequeRecycler;
import org.elasticsearch.common.recycler.Recycler;
import org.elasticsearch.common.recycler.Recycler.V;
import org.elasticsearch.common.recycler.Recyclers;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author bairen
 * @description
 * @see org.elasticsearch.common.recycler.AbstractRecyclerTestCase
 **/
public class RecyclerTest {
    // marker states for data
    protected static final byte FRESH = 1;
    protected static final byte RECYCLED = 2;
    protected static final byte DEAD = 42;

    //定义一个Recycler控制器
    static Recycler.C<byte[]> c = new Recycler.C<byte[]>() {

        @Override
        public byte[] newInstance(int sizing) {
            System.out.println("newInstance " + sizing);
            byte[] value = new byte[10];
            // "fresh" is intentionally not 0 to ensure we covered this code path
            Arrays.fill(value, FRESH);
            return value;
        }

        @Override
        public void recycle(byte[] value) {
            System.out.println("recycle ");
            Arrays.fill(value, RECYCLED);
        }

        @Override
        public void destroy(byte[] value) {
            System.out.println("destroy ");
            Arrays.fill(value, DEAD);
        }
    };



    protected void reuse(Recycler<byte[]> r) {
        //o3和o4复用了o1、o2，避免频繁创建对象
        V<byte[]> o1 = r.obtain();
        V<byte[]> o2 = r.obtain();
        o1.close();
        o2.close();
        V<byte[]> o3 =r.obtain();
        V<byte[]> o4 =r.obtain();
        System.out.println(o1.isRecycled()+","+o4.isRecycled());

        //回收是有limit，超过限制会被销毁
        r.obtain().close();
        o3.close();
        o4.close();//这里超过了限制触发destroy
    }

    @Test
    public void testDequeRecycler() {
        //最简单的DequeRecycler
        Recycler<byte[]> r = Recyclers.deque(c, 2);
        reuse(r);
    }

    @Test
    public void testConcurrent() {
        //并发Recycler
        Recycler<byte[]> r = Recyclers.concurrent(Recyclers.dequeFactory(c,2));
        reuse(r);


    }

    @Test
    public void testConcurrentDeque() {
        //ConcurrentLinkedDeque Recycler
        Recycler<byte[]> r = Recyclers.concurrentDeque(c, 2);
        reuse(r);
    }

}
