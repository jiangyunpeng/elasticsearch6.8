/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.mytest;

import com.carrotsearch.hppc.LongObjectHashMap;

public class LongObjectHashMapTest {


    public static void main(String[] args) {
        LongObjectHashMap map = new LongObjectHashMap(8);
        for (int i = 0; i < 100; ++i) {
            System.out.println(i + "\t" + map.indexOf(i));
        }
    }
}
