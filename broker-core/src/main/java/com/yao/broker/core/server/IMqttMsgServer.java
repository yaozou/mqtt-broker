package com.yao.broker.core.server;

import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * @Description: mqtt协议消息服务
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
public interface IMqttMsgServer {
    /**
     * 连接服务
     * @param channel
     * @param msg
     */
    void connect(Channel channel, MqttConnectMessage msg);

    /**
     * 客户端订阅消息
     * @param channel
     * @param msg
     */
    void subscribe(Channel channel, MqttSubscribeMessage msg);

    /**
     * 取消订阅消息
     * @param channel
     * @param msg
     */
    void unsubscribe(Channel channel, MqttUnsubscribeMessage msg);

    /**
     * 心跳请求
     * @param channel
     */
    void ping(Channel channel);

    /**
     * 断开连接
     * @param channel
     */
    void disconnect(Channel channel);

    /**
     * 发布消息
     * @param channel
     * @param msg
     */
    void pulish(Channel channel, MqttPublishMessage msg);

    /**
     * 发布确认
     * @param channel
     */
    void puback(Channel channel, MqttPubAckMessage msg);

    /**
     * 发布收到 （QoS2，第一步）
     * @param channel
     * @param msg
     */
    void pubrec(Channel channel, MqttMessage msg);

    /**
     * 发布释放（QoS2，第二步）
     * @param channel
     * @param msg
     */
    void pubrel(Channel channel, MqttMessage msg);

    /**
     * 发布完成 （QoS2，第三步）
     * @param channel
     * @param msg
     */
    void pubcomp(Channel channel, MqttMessage msg);

    /**
     * 关闭
     * @param channel
     */
    void close(Channel channel);

    /**
     * 心跳超时
     * @param channel
     */
    void idleTimeout(Channel channel, IdleStateEvent evt);

    /**
     * 发送消息到终端
     * @param clientId 终端唯一标识
     * @param msg 消息
     */
    boolean sendMsg2Client(String clientId, String msg);
}
