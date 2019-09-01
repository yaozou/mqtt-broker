package com.yao.broker.core.pipeline;

import io.netty.channel.socket.SocketChannel;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
public interface PipelineInitializer {
    void init(SocketChannel channel) throws Exception;
}
