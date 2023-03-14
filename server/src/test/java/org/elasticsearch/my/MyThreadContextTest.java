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
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.junit.Test;
import org.locationtech.jts.util.Assert;

/**
 * @author bairen
 * @description
 **/
public class MyThreadContextTest {

    @Test
    public void testStashContext(){
        Settings build = Settings.builder().put("request.headers.default", "1").build();
        ThreadContext threadContext = new ThreadContext(build);
        threadContext.putHeader("foo", "bar");
        threadContext.putTransient("ctx.foo", 1);

        try (ThreadContext.StoredContext ctx = threadContext.stashContext()) {
            threadContext.putHeader("foo","bingo");

            System.out.println("foo="+threadContext.getHeader("foo"));
            System.out.println("ctx.foo="+threadContext.getTransient("ctx.foo"));
            System.out.println("default="+threadContext.getHeader("default"));
        }
        //被还原
        System.out.println("foo="+threadContext.getHeader("foo"));
        System.out.println("ctx.foo="+threadContext.getTransient("ctx.foo"));
        System.out.println("default="+threadContext.getHeader("default"));
    }
}
