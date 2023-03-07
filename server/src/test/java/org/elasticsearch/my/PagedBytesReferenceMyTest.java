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

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefIterator;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.bytes.ReleasablePagedBytesReference;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.PageCacheRecycler;
import org.elasticsearch.indices.breaker.NoneCircuitBreakerService;
import org.junit.Test;

import java.io.IOException;

/**
 * @author bairen
 * @description
 * @see org.elasticsearch.common.bytes.AbstractBytesReferenceTestCase
 **/
public class PagedBytesReferenceMyTest {
    protected static final int PAGE_SIZE = PageCacheRecycler.BYTE_PAGE_SIZE;
    protected final BigArrays bigarrays = new BigArrays(null, new NoneCircuitBreakerService(), CircuitBreaker.REQUEST);


    @Test
    public void testToBytesRef() throws IOException {
        //这里我们只申请了一页的空间，但是写了超过一页的内容，在写的过程中会触发扩容
        ReleasableBytesStreamOutput out = new ReleasableBytesStreamOutput(PAGE_SIZE , bigarrays);
        int length = 20000;
        char[] data = {'a', 'b', 'c'};
        for (int i = 0; i < length; ++i) {
            out.writeByte((byte) data[(i % 3)]);
        }

        ReleasablePagedBytesReference reference = out.bytes();

        //完整BytesRef
        BytesRef ref = reference.toBytesRef();
        System.out.println(ref.length + "," + ref.offset);
        System.out.println("=================================");

        //按page分块的BytesRef
        BytesRef bytesRef = null;
        BytesRefIterator iterator = reference.iterator();//注意不要重复调用
        while ((bytesRef = iterator.next()) != null) {
            System.out.println(bytesRef.length);
        }

    }
}
