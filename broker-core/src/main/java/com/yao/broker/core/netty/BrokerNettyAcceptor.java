package com.yao.broker.core.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/12 18:08
 */
@Slf4j
public class BrokerNettyAcceptor implements BrokerAcceptor {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Override
    public void connection() {
        boolean epoll = false;
        if (epoll){
            bossGroup = new EpollEventLoopGroup();
            workerGroup = new EpollEventLoopGroup();
        }else{
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
        }

    }

    @Override
    public void close() {

    }
}
