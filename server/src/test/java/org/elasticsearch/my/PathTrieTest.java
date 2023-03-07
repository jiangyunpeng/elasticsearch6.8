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

import org.elasticsearch.common.path.PathTrie;
import org.junit.Test;

/**
 * @author bairen
 * @description
 **/
public class PathTrieTest {
    public static final PathTrie.Decoder NO_DECODER = new PathTrie.Decoder() {
        @Override
        public String decode(String value) {
            return value;
        }
    };

    @Test
    public void testPath(){
        PathTrie<String> trie = new PathTrie<>(NO_DECODER);
        trie.insert("a/b","a1");
        trie.insert("a/c","a2");
        System.out.println(trie.retrieve("a/b"));
        System.out.println(trie.retrieve("a/c"));

        trie.insert("{index}/insert","b1");
        trie.insert("{index}/update","b2");
        System.out.println(trie.retrieve("a/insert"));
        System.out.println(trie.retrieve("a/update"));


    }
}
