package com.yao.broker.core.netty.handler;

import com.yao.broker.core.utils.NettyUtils;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/16 15:38
 */
@Slf4j
public class TcpIdleTimeoutHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleState e = ((IdleStateEvent) evt).state();
            if (e == IdleState.READER_IDLE) {
                log.info("Firing channel inactive event. MqttClientId = {}.", NettyUtils.clientID(ctx.channel()));
                ctx.fireChannelInactive();
                ctx.close().addListener(CLOSE_ON_FAILURE);
            }
        }else {
            super.userEventTriggered(ctx, evt);
        }

    }
}
