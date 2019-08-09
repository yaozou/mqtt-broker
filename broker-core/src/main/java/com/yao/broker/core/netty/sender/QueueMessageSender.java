package com.yao.broker.core.netty.sender;

import com.yao.broker.core.netty.bean.ClientSession;
import com.yao.broker.core.netty.bean.ConnectionInfo;
import com.yao.broker.core.netty.bean.StoreMessage;
import com.yao.broker.core.netty.repository.ConnectionRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * @Description: 队列消息发送
 * @author: yaozou
 * @Date: 2019/8/9 15:43
 */
@Slf4j
@Service
public class QueueMessageSender {

    @Autowired
    private ConnectionRepository connectionRepository;

    public boolean sendPublish(ClientSession session , MqttPublishMessage pubMessage , StoreMessage storeMessage){
        String clientId = session.getClientId();
        final int messageId = pubMessage.variableHeader().packetId();
        final String topicName = pubMessage.variableHeader().topicName();
        MqttQoS qos = pubMessage.fixedHeader().qosLevel();

        boolean isSend = false;

        final Optional<ConnectionInfo> optDescriptor = connectionRepository.lookupDescriptor(clientId);
        if (optDescriptor.isPresent()){
            final ConnectionInfo descriptor = optDescriptor.get();
            try{
                descriptor.writeAndFlush(pubMessage);
            }catch (Throwable e){
                log.error("Unable to send {} message. CId=<{}>, messageId={}",
                        pubMessage.fixedHeader().messageType(),
                        clientId, messageId, e);
            }
        }

        if (!isSend){
            // 放入队列中重新发送
            if (qos != AT_MOST_ONCE && !session.isCleanSession()) {
                session.enqueue(storeMessage);
            }
        }
        return isSend;
    }
}
