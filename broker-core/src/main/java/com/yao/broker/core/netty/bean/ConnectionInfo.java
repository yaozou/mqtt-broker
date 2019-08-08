package com.yao.broker.core.netty.bean;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: 连接信息
 * @author: yaozou
 * @Date: 2019/8/8 10:06
 */
@Slf4j
@Data
public class ConnectionInfo {
    private final String clientId;
    private final Channel channel;
    private final boolean cleanSession;

    public ConnectionInfo(String clientId,Channel channel,boolean cleanSession) {
        this.clientId = clientId;
        this.channel  = channel;
        this.cleanSession = cleanSession;
    }

    public void writeAndFlush(Object payload, ChannelFutureListener listener){
        this.channel.writeAndFlush(payload).addListener(listener);
    }
}
