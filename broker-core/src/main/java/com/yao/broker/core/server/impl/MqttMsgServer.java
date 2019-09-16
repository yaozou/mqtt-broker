package com.yao.broker.core.server.impl;

import com.yao.broker.core.bean.RetainMessage;
import com.yao.broker.core.enums.ConfirmStatusEnum;
import com.yao.broker.core.interception.BrokerInterceptor;
import com.yao.broker.core.server.IMqttChannelServer;
import com.yao.broker.core.server.IMqttMsgServer;
import com.yao.broker.core.utils.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
* @Description: MQTT消息处理服务
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Service
@Slf4j
public class MqttMsgServer implements IMqttMsgServer {
    @Autowired
    private BrokerInterceptor brokerInterceptor;
    @Autowired
    private IMqttChannelServer mqttChannelServer;

    @Override
    public void connect(Channel channel, MqttConnectMessage msg) {
        String clientId = msg.payload().clientIdentifier();
        log.info("create connection,clientID is {}",clientId);
        // 1、判断是否为二次连接
        if (mqttChannelServer.isChannelActive(clientId)){
            mqttChannelServer.sendConnack(channel,false, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION,clientId);
            return;
        }

        // 2、可变报文
        MqttConnectVariableHeader variableHeader = msg.variableHeader();
        // 2.1 版本级别
        int version = variableHeader.version();
        if (version != MqttVersion.MQTT_3_1.protocolLevel() && version != MqttVersion.MQTT_3_1_1.protocolLevel()){
            mqttChannelServer.sendConnack(channel,false, MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION,clientId);
            return;
        }

        // 3、有效载荷
        boolean cleanSession = variableHeader.isCleanSession();
        // 3.1 客户端标识符
        /*
        * 如果客户端提供了一个零字节的客户端标识符，它 必须同时将清理会话标志设置为 1。
        * 如果清理会话标志位为0，服务端必须发送返回码为0x02的CONNACK报文，然后关闭连接。
        * 当clientId为0且cleanSession为1时，可以随机生成一个clientId。
        */
        if (clientId == null || clientId.length() == 0){
            if (!cleanSession){
                mqttChannelServer.sendConnack(channel,false, MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED,clientId);
                return;
            }
            clientId = UUID.randomUUID().toString().replace("-", "");
        }

        // 4、用户、密码登录以及鉴权


        // 5、连接成功，响应
        mqttChannelServer.doConnectSuccess(channel,msg,clientId);
        log.info("created connection successfully,clientID is {}",clientId);
    }

    @Override
    public void subscribe(Channel channel, MqttSubscribeMessage msg) {
       final String clientId = NettyUtils.clientID(channel);
       final int    msgId = msg.variableHeader().messageId();
        log.info("ClientId({}) subscribe......",clientId);
        List<MqttTopicSubscription> subscriptions = msg.payload().topicSubscriptions();
        List<Integer> qosLevels = new ArrayList<>(subscriptions.size());

        if (!CollectionUtils.isEmpty(subscriptions)){
            Set<String> topics = new HashSet<>(subscriptions.size());
            subscriptions.forEach(sub ->{
                qosLevels.add(sub.qualityOfService().value());
                topics.add(sub.topicName());
                log.info("subscribe topic is {}",sub.topicName());
            });

            // 1、订阅成功
            mqttChannelServer.doSubscribeSuccess(clientId,topics);
        }

        // 2、订阅确认
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBACK,false, MqttQoS.AT_MOST_ONCE,
                false,0);
        MqttSubAckPayload payload = new MqttSubAckPayload(qosLevels);

        MqttSubAckMessage ackMessage = new MqttSubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(msgId),payload);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
        log.info("ClientId({}) subscribed successfully......",clientId);
    }

    @Override
    public void unsubscribe(Channel channel, MqttUnsubscribeMessage msg) {
        int msgId = msg.variableHeader().messageId();
        final String clientId = NettyUtils.clientID(channel);
        List<String> topics = msg.payload().topics();
        log.info("ClientId({}) unsubscribe......",clientId);
        // 1、取消订阅成功
        if (!CollectionUtils.isEmpty(topics)){
            mqttChannelServer.doUnsubscribeSuccess(clientId,topics);
            log.info("subscribe topics is {}",topics.toString());
        }

        // 2、取消订阅确认
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.UNSUBACK,false, MqttQoS.AT_MOST_ONCE,
                false,0);
        MqttUnsubAckMessage unsubAckMessage = new MqttUnsubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(msgId));
        channel.writeAndFlush(unsubAckMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
        log.info("ClientId({}) unsubscribed successfully......",clientId);
    }

    @Override
    public void ping(Channel channel) {
        // 发送心跳请求
        MqttFixedHeader header = new MqttFixedHeader(
                MqttMessageType.PINGRESP,false, MqttQoS.AT_MOST_ONCE,
                false,0);
        MqttMessage pingResp = new MqttMessage(header);
        channel.writeAndFlush(pingResp).addListener(CLOSE_ON_FAILURE);
        log.info("ClientId({}) ping......",NettyUtils.clientID(channel));
    }

    @Override
    public void disconnect(Channel channel) {
        mqttChannelServer.closeChannel(channel,true);
        log.info("ClientId({}) disconnect......",NettyUtils.clientID(channel));
    }

    @Override
    public void pulish(Channel channel, MqttPublishMessage msg) {
        log.info("ClientId({}) pulish msg({})",NettyUtils.clientID(channel),msg.toString());
        // 固定报头
        MqttQoS qos = msg.fixedHeader().qosLevel();
        boolean isRetain = msg.fixedHeader().isRetain();

        // 可变报头
        String topic = msg.variableHeader().topicName();
        int msgId = msg.variableHeader().packetId();

        boolean clearRetain = false;
        switch (qos){
            case AT_MOST_ONCE:
                break;
            case AT_LEAST_ONCE:
                clearRetain = true;
                mqttChannelServer.sendPublishAck(channel,msgId);
                break;
            case EXACTLY_ONCE:
                mqttChannelServer.sendPublishRec(channel,msgId);
                break;
            default:
                channel.close().addListener(CLOSE_ON_FAILURE);
                break;
        }

        // 应用消息
        ByteBuf buf = msg.payload();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);

        brokerInterceptor.notifyTopicPublished(msg,bytes,NettyUtils.clientID(channel),NettyUtils.userName(channel));

        // 保留消息
        if (isRetain){
            RetainMessage retainMessage = RetainMessage.builder().topic(topic).qos(qos).byteBuf(bytes).build();
            mqttChannelServer.doRetainMessage(retainMessage,clearRetain);
        }

    }

    @Override
    public void puback(Channel channel, MqttPubAckMessage msg) {
        String clientId = NettyUtils.clientID(channel);
        log.info("ClientId({}) puback msg({})",clientId,msg.toString());
        int msgId = msg.variableHeader().messageId();
        mqttChannelServer.doSendMessageConfirmStatus(clientId,msgId,ConfirmStatusEnum.COMPLETE);
    }

    @Override
    public void pubrec(Channel channel, MqttMessage msg) {
        String clientId = NettyUtils.clientID(channel);
        log.info("ClientId({}) pubrec msg({})",clientId,msg.toString());
        MqttMessageIdVariableHeader messageIdVariableHeader = (MqttMessageIdVariableHeader) msg.variableHeader();
        int msgId = messageIdVariableHeader.messageId();
        mqttChannelServer.sendPublishRel(channel,msgId);
        mqttChannelServer.doSendMessageConfirmStatus(clientId,msgId,ConfirmStatusEnum.PUBREC);
    }

    @Override
    public void pubrel(Channel channel, MqttMessage msg) {
        String clientId = NettyUtils.clientID(channel);
        log.info("ClientId({}) pubrel msg({})",clientId,msg.toString());
        MqttMessageIdVariableHeader messageIdVariableHeader = (MqttMessageIdVariableHeader) msg.variableHeader();
        int msgId = messageIdVariableHeader.messageId();
        mqttChannelServer.sendPublishComp(channel,msgId);
        mqttChannelServer.doSendMessageConfirmStatus(clientId,msgId,ConfirmStatusEnum.PUBREL);
    }

    @Override
    public void pubcomp(Channel channel, MqttMessage msg) {
        String clientId = NettyUtils.clientID(channel);
        log.info("ClientId({}) pubcomp msg({})",clientId,msg.toString());
        MqttMessageIdVariableHeader messageIdVariableHeader = (MqttMessageIdVariableHeader) msg.variableHeader();
        int msgId = messageIdVariableHeader.messageId();
        mqttChannelServer.doSendMessageConfirmStatus(clientId,msgId,ConfirmStatusEnum.COMPLETE);
    }

    @Override
    public void close(Channel channel) {
        mqttChannelServer.closeChannel(channel,false);
        channel.close();
        log.info("ClientId({}) close.........",NettyUtils.clientID(channel));
    }

    @Override
    public void idleTimeout(Channel channel, IdleStateEvent evt) {
        switch (evt.state()) {
            case READER_IDLE:
            case ALL_IDLE:
            case WRITER_IDLE:
                close(channel);
                break;
            default:
        }
    }

    @Override
    public boolean sendMsg2Client(String clientId, String msg) {

        return false;
    }
}
