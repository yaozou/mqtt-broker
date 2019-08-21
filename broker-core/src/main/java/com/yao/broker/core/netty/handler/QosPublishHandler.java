package com.yao.broker.core.netty.handler;

import com.yao.broker.core.netty.authorizator.AuthorizatorServer;
import com.yao.broker.core.netty.bean.StoreMessage;
import com.yao.broker.core.netty.bean.Topic;
import com.yao.broker.core.netty.interception.BrokerInterceptor;
import com.yao.broker.core.netty.interception.InterceptHandler;
import com.yao.broker.core.netty.interception.PublisherListener;
import com.yao.broker.core.netty.publisher.MessagesPublisher;
import com.yao.broker.core.netty.repository.MessageRepository;
import com.yao.broker.core.utils.NettyUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Description: qos消息处理
 * @author: yaozou
 * @Date: 2019/8/9 10:10
 */
@Service
@Slf4j
public class QosPublishHandler {
    @Autowired
    private AuthorizatorServer authorizatorServer;
    @Autowired
    private MessagesPublisher messagesPublisher;
    @Autowired
    private MessageRepository messageRepository;

    private BrokerInterceptor m_interceptor;

    public QosPublishHandler(){
        List<InterceptHandler> userHandlers = Collections
                .singletonList(new PublisherListener());
        m_interceptor = new BrokerInterceptor(userHandlers,messageRepository);
    }

    public void receivedPublishQos0(Channel channel, MqttPublishMessage msg){
        String topicName = msg.variableHeader().topicName();
        Topic topic = new Topic(topicName);

        String clientId = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);

        // 1、鉴权
        if (!authorizatorServer.canWrite(topic,username,clientId)){
            log.error("The client is't authorized to publish on topic.ClientId={},topic={}.",clientId,topic.toString());
            return;
        }

        // 2、生成存储信息
        StoreMessage storeMessage = toStoreMessage(msg,clientId);
        // 3、发送给订阅者
        messagesPublisher.publishToSubscribers(storeMessage,topic);

        /**
         * <ul>
         *     <li>如果客户端发给服务端的 PUBLISH 报文的保留（RETAIN）标志被设置为 1，服务端 必须存储这个应用消
         * 息和它的服务质量等级（QoS），以便它可以被分发给未来的主题名匹配的订阅者 [MQTT-3.3.1-5]</li>
         *      <li>一个新的订阅建立时，对每个匹配的主题名，如果存在最近保留的消息，它 必须被发送给这个订阅者 [MQTT-
         * 3.3.1-6]。</li>
         *      <li>如果服务端收到一条保留（RETAIN）标志为 1 的 QoS 0 消息，它 必须丢弃之前为那个主题保留
         * 的任何消息。它 应该将这个新的 QoS 0 消息当作那个主题的新保留消息，但是任何时候都 可以选择丢弃它
         * — 如果这种情况发生了，那个主题将没有保留消息 [MQTT-3.3.1-7]。</li>
         *  <ul/>
         */
        // 4、是否保留
        if (msg.fixedHeader().isRetain()){
            messageRepository.cleanRetained(topic);
        }

        // 5、处理消息
        m_interceptor.notifyTopicPublished(msg, clientId, username);

    }

    private StoreMessage toStoreMessage(MqttPublishMessage msg,String clientId){
        byte[]  payload = byteBufToByte(msg.payload());
        StoreMessage storeMessage = new StoreMessage(
                msg.fixedHeader().qosLevel(),payload,
                msg.variableHeader().topicName(),clientId,
                msg.fixedHeader().isRetain());

        return storeMessage;
    }

    private byte[] byteBufToByte(ByteBuf payload){
        byte[] payloadContent = new byte[payload.readableBytes()];
        int mark = payload.readerIndex();
        payload.readBytes(payloadContent);
        payload.readerIndex(mark);
        return payloadContent;
    }
}
