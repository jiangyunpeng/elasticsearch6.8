/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.elasticsearch.common.SourceLogger;

import java.util.List;

/**
 * https://github.com/elastic/elasticsearch/pull/62673
 *
 * 当socket buffer比较小，也就是我们每次从channel中读取的数量小时，如果还是保留64kb到应用层会浪费很多内存
 * 所以转为小的buf避免浪费内存
 */
@ChannelHandler.Sharable
public class NettyByteBufSizer extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        // 获取当前 ByteBuf 可读取的字节数
        SourceLogger.info(this.getClass(),"receive {}",buf);
        int readableBytes = buf.readableBytes();
        if (buf.capacity() >= 1024) {
            SourceLogger.info(this.getClass(),"resize buffer from {} to {} ",buf.capacity(),readableBytes);
            //discardReadBytes()会丢弃已经读取的字节数，capacity()会重新申请新新的内存并回收老的
            ByteBuf resized = buf.discardReadBytes().capacity(readableBytes);
            assert resized.readableBytes() == readableBytes;
            out.add(resized.retain());
        } else {
            out.add(buf.retain());
        }
    }
}
