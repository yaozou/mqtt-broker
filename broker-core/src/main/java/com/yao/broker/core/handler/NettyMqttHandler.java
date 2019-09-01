package com.yao.broker.core.handler;

import com.yao.broker.core.server.IMqttMsgServer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Description: mqtt消息处理
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Service
@ChannelHandler.Sharable
public class NettyMqttHandler extends SimpleChannelInboundHandler<MqttMessage> {
    @Autowired
    private IMqttMsgServer mqttMsgServer;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        MqttFixedHeader fixedHeader = msg.fixedHeader();
        MqttMessageType msgType = fixedHeader.messageType();
        switch (msgType){
            case CONNECT:
                mqttMsgServer.connect(ctx.channel(),(MqttConnectMessage) msg);
                break;
            case SUBSCRIBE:
                mqttMsgServer.subscribe(ctx.channel(),(MqttSubscribeMessage) msg);
                break;
            case UNSUBSCRIBE:
                mqttMsgServer.unsubscribe(ctx.channel(),(MqttUnsubscribeMessage) msg);
                break;
            case PINGREQ:
                mqttMsgServer.ping(ctx.channel());
                break;
            case DISCONNECT:
                mqttMsgServer.disconnect(ctx.channel());
                break;
            case PUBLISH:
                mqttMsgServer.pulish(ctx.channel(),(MqttPublishMessage) msg);
                break;
            case PUBACK:
                mqttMsgServer.puback(ctx.channel(),(MqttPubAckMessage) msg);
                break;
            case PUBREC:
                mqttMsgServer.pubrec(ctx.channel(),msg);
                break;
            case PUBREL:
                mqttMsgServer.pubrel(ctx.channel(),msg);
                break;
            case PUBCOMP:
                mqttMsgServer.pubcomp(ctx.channel(),msg);
                break;
            default:
                break;

        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        mqttMsgServer.close(ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
            mqttMsgServer.idleTimeout(ctx.channel(),(IdleStateEvent)evt);
        }
        super.userEventTriggered(ctx, evt);
    }
}
