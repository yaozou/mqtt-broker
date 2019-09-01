package com.yao.broker.core.server;

import com.yao.broker.core.bean.ClientSessionInfo;
import com.yao.broker.core.bean.RetainMessage;
import com.yao.broker.core.bean.SendMqttMessage;
import com.yao.broker.core.bean.WillMessage;
import com.yao.broker.core.enums.ConfirmStatusEnum;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

import java.util.List;
import java.util.Set;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/28 0028
 * @Version V1.0
 **/
public interface IMqttChannelServer {

    /**
     * client是否活跃
     * @param clientId
     * @return
     */
    boolean isChannelActive(String clientId);

    /**
     * 连接成功
     *
     * @param channel
     * @param message 连接消息
     */
    void doConnectSuccess(Channel channel, MqttConnectMessage message, String clientId);

    /**
     * 订阅成功
     * @param clientId
     * @param topics
     */
    void doSubscribeSuccess(String clientId, Set<String> topics);

    /**
     * 取消订阅成功
     * @param clientId 客户端唯一标识
     * @param topics 取消的主题
     */
    void doUnsubscribeSuccess(String clientId, List<String> topics);

    /**
     * 保留消息
     */
    void doRetainMessage(RetainMessage retainMessage, boolean clearRetain);

    /**
     * 处理发送消息的提交状态
     * @param clientId
     */
    void doSendMessageConfirmStatus(String clientId, int msgId, ConfirmStatusEnum confirmStatusEnum);

    /**
     * 关闭通道
     *
     * @param channel
     * @param isDisconnect 师傅是取消连接，取消连接不发送遗嘱消息
     */
    void closeChannel(Channel channel, boolean isDisconnect);

    /**
     * 发送连接应答
     *
     * @param channel
     * @param cleanSession  是否清理会话
     * @param returnCode    返回码 0x00 0x01 0x02
     * @param finalClientId
     */
    void sendConnack(Channel channel, boolean cleanSession, MqttConnectReturnCode returnCode, final String finalClientId);

    /**
     * 发布消息到client
     * @param channel
     * @param msg 发送消息
     */
    void sendPublish(Channel channel, SendMqttMessage msg);

    /**
     * 发送发布确认
     * @param channel
     * @param msgId 报文标识符
     */
    void sendPublishAck(Channel channel, int msgId);

    /**
     * 发送发布收到消息
     * @param channel
     * @param msgId 报文标识符
     */
    void sendPublishRec(Channel channel, int msgId);

    /**
     * 发送发布释放消息
     * @param channel
     * @param msgId 报文标识符
     */
    void sendPublishRel(Channel channel, int msgId);

    /**
     * 发送发布完成消息
     * @param channel
     * @param msgId 报文标识符
     */
    void sendPublishComp(Channel channel, int msgId);

    /**
     * 发送保留消息
     * @param topics
     * @param info
     */
    void sendRetainMsg(Set<String> topics, ClientSessionInfo info);

    /**
     * 发送遗嘱消息
     * @param will
     */
    void sendWillMessage(WillMessage will);
}
