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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.stream.Stream;

/**
 * netty 零拷贝
 * https://cloud.tencent.com/developer/article/1488088
 *
 * @author bairen
 * @description
 **/
public class MyByteBuffTest {

    @Test
    public void testRead() throws IOException {
        String jdkHome = System.getenv("JAVA_HOME");
        File srcZip = new File(jdkHome, "/lib/src.zip");

        Stream<byte[]> streams = Stream.of(
            fileReadWithMMap(srcZip),
            fileReadWithByteBuffer(srcZip)
        );

        streams.forEach(MyByteBuffTest::saveFile);

    }

    @Test
    public void testPerformance() throws IOException {
        String jdkHome = System.getenv("JAVA_HOME");
        File srcZip = new File(jdkHome, "/lib/src.zip");

        profile("mmap", () -> fileReadWithMMap(srcZip), 5);
        profile("buffer", () -> fileReadWithByteBuffer(srcZip), 5);

    }

    private static void saveFile(byte[] output) {
        File file = new File("/data/tmp/src.zip");
        for (int i = 0; file.exists(); ++i) {
            file = new File("/data/tmp/src" + i + ".zip");
        }
        try {
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(output);
                System.out.println("save "+file+" success");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    interface IRunnable {
        void run() throws Exception;
    }

    static void profile(String name, IRunnable task, int times) {
        for (int i = 0; i < times; ++i) {
            long begin = System.currentTimeMillis();
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            System.out.println(name + "\t cost:" + (end - begin));
        }
    }

    public static byte[] fileReadWithMMap(File file) throws IOException {
        int length = (int) file.length();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) file.length());

        try (FileChannel channel = new FileInputStream(file).getChannel()) {

            MappedByteBuffer buff = channel.map(MapMode.READ_ONLY, 0, length);

            for (int offset = 0; offset < length; offset += BUFFER_SIZE) {
                byte[] b;
                if (length - offset > BUFFER_SIZE) {
                    b = new byte[BUFFER_SIZE];
                } else {
                    b = new byte[length - offset];
                }
                buff.get(b);
                outputStream.write(b);
            }
        }
        return outputStream.toByteArray();
    }

    public static byte[] fileReadWithByteBuffer(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream((int) file.length());
            byte[] buf = new byte[BUFFER_SIZE];
            int len = 0;
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            return outputStream.toByteArray();
        }
    }

    private static final int BUFFER_SIZE = 1024;


}
