package com.yao.broker.core.netty.processor;

import com.yao.broker.core.netty.authorizator.AuthorizatorServer;
import com.yao.broker.core.netty.bean.ClientSession;
import com.yao.broker.core.netty.bean.ConnectionInfo;
import com.yao.broker.core.netty.bean.Subscription;
import com.yao.broker.core.netty.bean.Topic;
import com.yao.broker.core.netty.handler.AutoFlushHandler;
import com.yao.broker.core.netty.repository.ConnectionRepository;
import com.yao.broker.core.netty.repository.SessionRepository;
import com.yao.broker.core.netty.repository.SubscriptionRepository;
import com.yao.broker.core.utils.NettyUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/7 15:24
 */
@Service
@Slf4j
public class MqttProtocolProcessor {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private AuthorizatorServer authorizatorServer;

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
        final String name = payload.userName();

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

       final boolean cleanSession = msg.variableHeader().isCleanSession();
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
        // 4.1、根据 MQTT-3.1.2-4(清理会话) MQTT-3.1.2-5（遗嘱标志） MQTT-3.1.2-6（遗嘱QoS） 判断是否新建链接
        ConnectionInfo connectionInfo = new ConnectionInfo(clientId,channel,cleanSession);
        connectionRepository.addConnection(connectionInfo);

        // 5、初始化session
        ClientSession clientSession  = sessionRepository.get(clientId);
        if (clientSession != null && cleanSession){
            // 会话标志位为1时，删除所有的订阅关系
            for(Subscription sub : clientSession.getSubscriptions()){
                subscriptionRepository.remove(sub.getTopic(),sub.getClientId());
            }
        }

        // 6、初始化心跳时间
        initKeepAliveTimeout(channel,msg,clientId);

        // 7、保存加载clientSession
        sessionRepository.createOrLoadClientSession(clientId,cleanSession);

        // 8、定时刷新channel
        // (keepAlive * 1000)/2
        int flushIntervalMs = 500;
        autoFlusher(channel,flushIntervalMs);

        // 9、清理session时重新鉴权订阅主题
        if (!cleanSession){
            reauthorizeOnExistingSubscriptions(clientId,name);
        }

        // 10、连接应答
        MqttConnAckMessage ackMessage = createSuccConnAsk(cleanSession,clientId);
        String finalClientId = clientId;
        connectionInfo.writeAndFlush(ackMessage, new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()){
                    log.debug("ConnAck has been sent.ClientId={}", finalClientId);

                }else {
                    future.channel().pipeline().fireExceptionCaught(future.cause());
                }
            }
        });
    }

    public void processSubscribe(Channel channel, MqttSubscribeMessage msg){
        final String clientId = NettyUtils.clientID(channel);
        final String username = NettyUtils.userName(channel);
        final int msgId = messageId(msg);
        log.debug("processing subscribe message.ClientId={},messageId={}",clientId,msgId);
        // todo:防止并发

        List<MqttTopicSubscription> subscriptions = msg.payload().topicSubscriptions();
        // 1、发送订阅确认消息
        MqttSubAckMessage ackMessage = createSubAskMessage(subscriptions,msgId);
        log.debug("send SubAck message.ClientId={},messageId={}",clientId,msgId);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
        // 2、持久化已订阅的消息
        for(MqttTopicSubscription sub : subscriptions){
            Subscription subscription = new Subscription(clientId,new Topic(sub.topicName()),sub.qualityOfService());
            subscriptionRepository.add(subscription);
        }
    }

    public void processUnsubscribe(Channel channel, MqttUnsubscribeMessage msg){
        final String clientId = NettyUtils.clientID(channel);
        List<String> topics = msg.payload().topics();
        log.debug("processing Unsubscribe message.ClientId={},topics={}",clientId,topics);

        // 1、清理订阅消息缓存
        for(String s : topics){
            Topic topic = new Topic(s);
            subscriptionRepository.remove(topic,clientId);
        }

        // 2、取消订阅应答
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, AT_MOST_ONCE,
                false, 0);
        int msgId = msg.variableHeader().messageId();
        MqttUnsubAckMessage ackMessage = new MqttUnsubAckMessage(fixedHeader, from(msgId));
        log.debug("send UnsubAck message.ClientId={},messageId={}",clientId,msgId);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    public void processPublish(Channel channel, MqttPublishMessage msg){
        final MqttQoS qos = msg.fixedHeader().qosLevel();
        final String clientId = NettyUtils.clientID(channel);
        log.debug("processing Publish message.ClientId={},qos={},messageId={}",clientId,qos,messageId(msg));
        switch (qos){
            case AT_MOST_ONCE:

                break;
            case AT_LEAST_ONCE:

                break;
            case EXACTLY_ONCE:

                break;
                default:
                    log.error("Unknown QoS-Type:{}", qos);
                    break;
        }
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


    private void reauthorizeOnExistingSubscriptions(String clientId, String username) {
        if (!sessionRepository.containsKey(clientId)) {
            return;
        }
        ClientSession session = sessionRepository.get(clientId);
        final Collection<Subscription> clientSubscriptions = session.getSubscriptions();
        for (Subscription sub : clientSubscriptions) {
            final Topic topic = sub.getTopic();
            // 鉴权
            final boolean readAuthorized = authorizatorServer.canRead(topic, username, clientId);
            if (!readAuthorized) {
                // 移除订阅
            }
        }
    }


    private void autoFlusher(Channel channel,int flushIntervalMs) {
        try {
            channel.pipeline().addAfter("idleEventHandler","autoFlusher",new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS));
        }catch (Exception e){
            channel.pipeline().addFirst("autoFlusher",new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS));
        }
    }

    private void initKeepAliveTimeout(Channel channel, MqttConnectMessage msg , String clientId) {
        int keepAlive = msg.variableHeader().keepAliveTimeSeconds();

        NettyUtils.keepAlive(channel, keepAlive);
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());
        NettyUtils.clientID(channel, clientId);

        int idleTime = Math.round(keepAlive*1.5f);
        final String idleStateHandler = "idleStateHandler";
        final ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.names().contains(idleStateHandler)){
            pipeline.remove(idleStateHandler);
        }
        pipeline.addFirst(idleStateHandler,new IdleStateHandler(idleTime,0,0));
    }

    private MqttConnAckMessage createSuccConnAsk(final boolean cleanSession, String clientId){
        boolean isSessionAlreadyStored  = sessionRepository.containsKey(clientId);
        boolean sessionPresent = false;
        if (!cleanSession && !isSessionAlreadyStored){
            sessionPresent = true;
        }
        return createConnAck(CONNECTION_ACCEPTED,sessionPresent);
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

    public static int messageId(MqttMessage mqttMessage){
        return ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId();
    }

    private MqttSubAckMessage createSubAskMessage(List<MqttTopicSubscription> subscriptions, int messageId) {
        List<Integer> qoSLevels = new ArrayList<>();
        for(MqttTopicSubscription subscription : subscriptions){
            qoSLevels.add(subscription.qualityOfService().value());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.SUBACK, false, AT_MOST_ONCE,
                false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(qoSLevels);

        return new MqttSubAckMessage(fixedHeader,MqttMessageIdVariableHeader.from(messageId),payload);
    }

}
