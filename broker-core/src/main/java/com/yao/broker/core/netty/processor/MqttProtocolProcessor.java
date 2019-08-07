package com.yao.broker.core.netty.processor;

import java.util.UUID;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/7 15:24
 */
@Slf4j
public class MqttProtocolProcessor {


    /**
     * 客户端连接服务端
     * 描述：当客户端到服务端的网络建立连接后，客户端发送给服务端的第一个报文必须是CONNECT
     *
     * @param channel
     * @param msg
     */
    public void processConnect(Channel channel, MqttConnectMessage msg){
        //有效载荷
        MqttConnectPayload payload = msg.payload();
        //客户端标识
        String clientId = payload.clientIdentifier();
        //用户名
        String name = payload.userName();

        log.debug("processing connect message.ClientId={},Username={}",clientId,name);

        // 1、判断是否为3.1.1版本的mqtt协议
        // 如果不是则返回码为 0x01的 CONNACK报文响应，然后断开连接
        if (!isProtocolVersion(msg,MqttVersion.MQTT_3_1) && !isProtocolVersion(msg,MqttVersion.MQTT_3_1_1)){
           log.error("MQTT protocol version is not valid.ClientId={}",clientId);
            MqttConnAckMessage ackMessage = createConnAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION,false);

            channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }

        boolean cleanSession = msg.variableHeader().isCleanSession();
        // 2、如果客户端提供了一个零字节的客户端标识符，它 必须同时将清理会话标志设置为 1。
        // 如果客户端提供的 ClientId 为零字节且清理会话标志为 0，服务端 必须发送返回码为 0x02（表示标识符不
        //合格）的 CONNACK 报文响应客户端的 CONNECT 报文，然后关闭网络连接
        if (clientId == null || clientId.length() == 0){
            if (!cleanSession){
                MqttConnAckMessage ackMessage = createConnAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED,false);

                channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
                channel.close().addListener(CLOSE_ON_FAILURE);
                return;
            }
            clientId = UUID.randomUUID().toString().replace("-", "");
            log.info("Client has connected with server generated id={}, username={}", clientId, name);
        }

        // 3、登录账号
        // todo : 若需要用户名和密码实现账号登录可以在此添加登录代码
        byte[] pwd = payload.passwordInBytes();

        // 4、储存更新连接信息

        // 5、初始化session

        // 6、初始化心跳时间

        // 7、保存加载clientSession

        // 8、定时刷新channel

        // 9、清理session时重新鉴权订阅主题

        // 10、连接应答



    }

    /**
     * 确认客服端连接请求消息
     * @param returnCode 返回码
     * @param sessionPresent 当前会话标志
     * @return
     */
    private MqttConnAckMessage createConnAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false,
                MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode,
                sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    public void processSubscribe(Channel channel, MqttSubscribeMessage msg){

    }

    public void processUnsubscribe(Channel channel, MqttUnsubscribeMessage msg){

    }

    public void processPublish(Channel channel, MqttPublishMessage msg){

    }

    public void processPubAck(Channel channel, MqttPubAckMessage msg){

    }

    public void processPubRec(Channel channel, MqttMessage msg){

    }

    public void processPubRel(Channel channel, MqttMessage msg){

    }

    public void processPubComp(Channel channel, MqttMessage msg) {

    }

    public void processDisconnect(Channel channel){

    }

    private boolean isProtocolVersion(MqttConnectMessage message , MqttVersion version){
        return version.protocolLevel() == message.variableHeader().version();
    }

}
