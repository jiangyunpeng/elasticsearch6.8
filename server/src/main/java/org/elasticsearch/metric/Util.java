/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.metric;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {

    private static final ThreadLocal<SimpleDateFormat> dateMinuteFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
            format.setLenient(false);
            return format;
        }
    };

    public static String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        return dateMinuteFormatter.get().format(date);

    }
}
