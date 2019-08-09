package com.yao.broker.core.netty.publisher;

import com.yao.broker.core.netty.bean.ClientSession;
import com.yao.broker.core.netty.bean.ConnectionInfo;
import com.yao.broker.core.netty.bean.StoreMessage;
import com.yao.broker.core.netty.bean.Subscription;
import com.yao.broker.core.netty.bean.Topic;
import com.yao.broker.core.netty.repository.ConnectionRepository;
import com.yao.broker.core.netty.repository.SessionRepository;
import com.yao.broker.core.netty.repository.SubscriptionRepository;
import com.yao.broker.core.netty.sender.QueueMessageSender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/9 11:05
 */
@Service
@Slf4j
public class MessagesPublisher {
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private QueueMessageSender queueMessageSender;


    public void publishToSubscribers(StoreMessage message , Topic topic){
        List<Subscription> subs = subscriptionRepository.match(topic);
        final MqttQoS pubQos = message.getQos();
        final ByteBuf pubPayload = message.getPayload();

        for(Subscription sub : subs){
            MqttQoS qos = lowerQos(sub.getQos(),pubQos);
            ClientSession session = sessionRepository.get(sub.getClientId());

            // 1、判断客户端是否还是存活状态
            boolean isActive = connectionRepository.isConnected(sub.getClientId());
            if (isActive){
                log.debug("Sending PUBLISH message to active subscriber. CId={}, topicFilter={}, qos={}",
                        sub.getClientId(), sub.getTopic(), qos);
                ByteBuf payload = pubPayload.retainedDuplicate();

                int messageId = 0;
                if (!qos.equals(MqttQoS.AT_MOST_ONCE)){
                    // QoS 1 and QoS 2 need messageId
                    messageId = session.getMessage(message);
                }
                MqttPublishMessage publishMessage = notRetainedPublish(topic.getTopic(),qos,payload,messageId);
                queueMessageSender.sendPublish(session,publishMessage,message);
            }else {
                // TODO：客服端不断开连接

                // TODO：抛出异常
            }

        }
    }


    private MqttQoS lowerQos(MqttQoS subQos , MqttQoS pubQos){
        if (pubQos.value() > subQos.value()){
            return subQos;
        }
        return pubQos;
    }

    private MqttPublishMessage notRetainedPublish(String topic, MqttQoS qos, ByteBuf message,int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false,
                0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }
}
