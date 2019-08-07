package com.yao.broker.core.netty.handler;

import com.yao.broker.core.netty.processor.MqttProtocolProcessor;
import com.yao.broker.core.utils.NettyUtils;

import org.omg.CORBA.CODESET_INCOMPATIBLE;

import java.io.IOException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import lombok.extern.slf4j.Slf4j;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/16 15:05
 */
@Slf4j
public class NettyMqttHandler extends ChannelInboundHandlerAdapter {

    MqttProtocolProcessor processor;
    public NettyMqttHandler(){
        processor = new MqttProtocolProcessor();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MqttMessage message = (MqttMessage) msg;
        if (message.fixedHeader() == null){
            throw new IOException("Unkonwn packet");
        }
        MqttMessageType messageType = message.fixedHeader().messageType();
        log.debug("Processing MQTT message, type: {}",message);
        switch (messageType){
            case CONNECT:
                processor.processConnect(ctx.channel(),(MqttConnectMessage) message);
                break;
            case SUBSCRIBE:
                processor.processSubscribe(ctx.channel(),(MqttSubscribeMessage)message);
                break;
            case UNSUBSCRIBE:
                processor.processUnsubscribe(ctx.channel(), (MqttUnsubscribeMessage) message);
                break;
            case PUBLISH:
                processor.processPublish(ctx.channel(), (MqttPublishMessage) message);
                break;
            case PUBACK:
                processor.processPubAck(ctx.channel(), (MqttPubAckMessage) message);
                break;
            case PUBREC:
                processor.processPubRec(ctx.channel(),message);
                break;
            case PUBREL:
                processor.processPubRel(ctx.channel(),message);
                break;
            case DISCONNECT:
                processor.processDisconnect(ctx.channel());
                break;
            case PINGREQ:
                // 发送心跳请求
                MqttFixedHeader pingHeader = new MqttFixedHeader(
                        MqttMessageType.PINGRESP,false, MqttQoS.AT_MOST_ONCE,
                        false,0);
                MqttMessage pingResp = new MqttMessage(pingHeader);
                ctx.writeAndFlush(pingResp).addListener(CLOSE_ON_FAILURE);
                break;
            default:
                log.error("Unknown messageType:{}",messageType);
                break;
        }

        String clientID = NettyUtils.clientID(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }
}
