package com.yao.broker.core.netty.pipeline;

import io.netty.channel.socket.SocketChannel;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/16 15:11
 */
public abstract class PipelineInitializer {
    public abstract void init(SocketChannel channel) throws Exception;
}
