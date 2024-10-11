/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.metric;


import org.elasticsearch.metric.MetricRegistry.MetricEntry;
import org.elasticsearch.metric.MetricRegistry.StatRollingData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Tsdb {


    private static final Appender logAppender = new Appender();

    /**
     * 格式：
     * |@timestamp|key|count|meter|
     */
    public static void writeRollingData(StatRollingData data) {
        long currentTs = data.getTimeSlot();
        List<MetricEntry> entryList = data.entries(null);
        StringBuffer sb = new StringBuffer();
        for (MetricEntry entry : entryList) {
            sb.append(Util.formatTimestamp(currentTs));
            sb.append("|");
            String entryName = entry.getName();
            sb.append(entryName);
            sb.append("|");
            //counter
            List<String> countNameList = entry.getAllCountName();
            for (String countName : countNameList) {
                sb.append(countName + "=" + entry.counter(countName).getCount() + ",");
            }
            sb.append("|");
            //meter
            List<String> meterNameList = entry.getAllMeterName();
            for (String meterName : meterNameList) {
                sb.append(meterName + ".count" + "=" + entry.meter(meterName).getCount() + ",");
                sb.append(meterName + ".rate" + "=" + entry.meter(meterName).getOneMinuteRateRoundx1() + ",");
            }
            sb.append("\n");
        }
        //写入到日志文件
        logAppender.append(new Entry(data.getMeasure().getName(), sb.toString()));
    }

    private static class Appender {

        private static final int LOG_QUEUE_SIZE = 5000;
        private static final BlockingQueue<Entry> logQueue = new ArrayBlockingQueue<>(LOG_QUEUE_SIZE);
        private BaseFileWriter baseFileWriter = new BaseFileWriter();

        public Appender() {
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        write(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            t.setName("TSDB-Write-Thread");
            t.setDaemon(true);
            t.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    write(false);
                    baseFileWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        private void write(boolean block) throws Exception {
            List<Entry> parcel = new ArrayList<>();
            if (block) {
                Entry message = logQueue.take();
                parcel.add(message);
            }
            logQueue.drainTo(parcel);
            for (Entry entry : parcel) {
                //writer.write(log + "\r\n");
                baseFileWriter.write(entry);
            }
            baseFileWriter.flush();
        }

        public void append(Entry entry) {
            logQueue.add(entry);
        }
    }

    private static class BaseFileWriter {
        private final Map<String, BufferedWriter> writer = new ConcurrentHashMap<>();

        public void close() {
            //close all ;
            writer.values().forEach(i -> {
                try {
                    i.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        public void write(Entry entry) throws IOException {
            BufferedWriter bufferedWriter = writer.computeIfAbsent(entry.measure, (k) -> createBufferedWriter(k));
            bufferedWriter.write(entry.content);
        }

        private BufferedWriter createBufferedWriter(String name) {
            File logDir = new File(System.getProperty("user.dir"), "logs");
            logDir.mkdirs();
            File logFile = new File(logDir, "tsdb-" + name + ".log");
            System.out.println("logFile path: " + logFile);
            try {
                return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFile,true)));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public void flush() {
            writer.values().forEach(i -> {
                try {
                    i.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static class Entry {
        String measure;
        String content;

        public Entry(String measure, String content) {
            this.measure = measure;
            this.content = content;
        }
    }
}
