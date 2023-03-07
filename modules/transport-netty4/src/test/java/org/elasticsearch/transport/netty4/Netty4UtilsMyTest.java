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

package org.elasticsearch.transport.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.bytes.ReleasablePagedBytesReference;
import org.elasticsearch.common.io.stream.ReleasableBytesStreamOutput;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.indices.breaker.NoneCircuitBreakerService;
import org.elasticsearch.transport.netty4.Netty4Utils;
import org.junit.Test;

import java.io.IOException;

/**
 * @see org.elasticsearch.transport.netty4.Netty4UtilsTests
 * @author bairen
 * @description
 **/
public class Netty4UtilsMyTest {

    private final BigArrays bigarrays = new BigArrays(null, new NoneCircuitBreakerService(), CircuitBreaker.REQUEST);


    @Test
    public void test() throws IOException {
        ReleasableBytesStreamOutput out = new ReleasableBytesStreamOutput(32, bigarrays);
        out.write("hello".getBytes());
        ReleasablePagedBytesReference ref = out.bytes();
        System.out.println(ref.length());


        BytesRef bytesRef = ref.toBytesRef();
        ByteBuf byteBuf1 =Unpooled.wrappedBuffer(bytesRef.bytes, bytesRef.offset,bytesRef.length);

        ByteBuf byteBuf2 = Netty4Utils.toByteBuf(ref);
    }
}
