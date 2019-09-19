package com.yao.broker.core.server.impl;

import com.yao.broker.core.bean.ClientSessionInfo;
import com.yao.broker.core.bean.RetainMessage;
import com.yao.broker.core.bean.SendMqttMessage;
import com.yao.broker.core.bean.WillMessage;
import com.yao.broker.core.enums.ConfirmStatusEnum;
import com.yao.broker.core.handler.AutoFlushHandler;
import com.yao.broker.core.repository.RetainMessageRepository;
import com.yao.broker.core.repository.SendMqttMessageRepository;
import com.yao.broker.core.repository.SessionRepository;
import com.yao.broker.core.repository.WillRepository;
import com.yao.broker.core.server.IMqttChannelServer;
import com.yao.broker.core.utils.MessageIdUtils;
import com.yao.broker.core.utils.NettyUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static java.util.Optional.ofNullable;

/**
 * @Description: Mqtt通道服务器
 * @Author yao.zou
 * @Date 2019/8/28 0028
 * @Version V1.0
 **/
@Service
@Slf4j
public class MqttChannelServer implements IMqttChannelServer {
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private WillRepository willRepository;
    @Autowired
    private RetainMessageRepository retainMessageRepository;
    @Autowired
    private SendMqttMessageRepository sendMqttMessageRepository;

    private ExecutorService executorService = new ThreadPoolExecutor(20,200,0L,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());

    @Override
    public boolean isChannelActive(String clientId) {
        return sessionRepository.isActive(clientId);
    }

    @Override
    public void doConnectSuccess(Channel channel, MqttConnectMessage msg, String clientId) {
        MqttConnectPayload payload = msg.payload();
        MqttConnectVariableHeader variableHeader = msg.variableHeader();

        boolean cleanSession = variableHeader.isCleanSession();

        int keepAlive = variableHeader.keepAliveTimeSeconds();
        NettyUtils.clientID(channel,clientId);
        NettyUtils.login(channel,true);
        NettyUtils.keepAlive(channel, keepAlive);
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());

        // 初始化心跳时间
        int idleTime = Math.round(keepAlive*1.5f);
        final String idleStateHandler = "idleStateHandler";
        final ChannelPipeline pipeline = channel.pipeline();
        if (pipeline.names().contains(idleStateHandler)){
            pipeline.remove(idleStateHandler);
        }
        pipeline.addFirst(idleStateHandler,new IdleStateHandler(idleTime,0,0));

        // CONNACK应答
        sendConnack(channel,cleanSession, MqttConnectReturnCode.CONNECTION_ACCEPTED,clientId);

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                // 5.1 清理会话
                boolean willFlag = variableHeader.isWillFlag();
                ClientSessionInfo clientSessionInfo = ClientSessionInfo.builder()
                        .cleanSession(cleanSession)
                        .channel(channel)
                        .isWill(willFlag)
                        .clientId(clientId)
                        .sessionStatus(true)
                        .subscribeStatus(false)
                        .message(new ConcurrentHashMap<>(32))
                        .topics(new HashSet<>()).build();
                String finalClientId = clientId;
                boolean success = ofNullable(sessionRepository.get(clientId))
                        .map(info -> {
                            if (info.isSessionStatus()){
                                return false;
                            }else{
                                if (info.isSubscribeStatus()){
                                    // 清理订阅主题
                                    info.setTopics(new HashSet<>());
                                }
                            }
                            sessionRepository.add(finalClientId,clientSessionInfo);
                            return true;
                        }).orElseGet(()->{
                            sessionRepository.add(finalClientId,clientSessionInfo);
                            return true;
                        });
                if (success){
                    // 5.3 遗嘱
                    boolean willRetain = variableHeader.isWillRetain();
                    int     willQos = variableHeader.willQos();
                    if (willFlag){
                        String willTopic =  payload.willTopic();
                        String willMessage = payload.willMessage();
                        willRepository.add(new WillMessage(willRetain, willTopic, willMessage, willQos),clientId);
                    }else{
                        willRepository.remove(clientId);
                    }

                    // 5.5 消息分发（终端离线时发送给终端的信息）
                    ConcurrentLinkedQueue<SendMqttMessage> sendMqttMessages = sendMqttMessageRepository.get(clientId);
                    sendMqttMessages.forEach(sendMqttMessage-> sendPublish(channel,sendMqttMessage));

                    // 5.6 保持连接状态监视（定时刷新channel）
                    // (keepAlive * 1000)/2
                    int flushIntervalMs = 500;
                    autoFlusher(channel,flushIntervalMs);
                }
            }
        });
    }

    @Override
    public void doSubscribeSuccess(String clientId,Set<String> topics) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                ofNullable(sessionRepository.get(clientId)).ifPresent(info ->{
                    if (NettyUtils.login(info.getChannel())){
                        info.getTopics().addAll(topics);
                        info.setSubscribeStatus(true);
                        // 发送保留消息
                        sendRetainMsg(topics,info);
                    }
                });
            }
        });

    }

    @Override
    public void doUnsubscribeSuccess(String clientId, List<String> topics) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                ofNullable(sessionRepository.get(clientId)).ifPresent(info -> topics.forEach(topic -> info.getTopics().remove(topic)));
            }
        });
    }

    @Override
    public void doRetainMessage(RetainMessage retainMessage,boolean clearRetain) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                retainMessageRepository.add(retainMessage,clearRetain);
            }
        });
    }

    @Override
    public void doSendMessageConfirmStatus(String clientId, int msgId, ConfirmStatusEnum confirmStatusEnum) {
        ofNullable(sessionRepository.get(clientId))
                .flatMap(sessionInfo -> ofNullable(sessionInfo.getMessage()).flatMap(messages -> ofNullable(messages.get(msgId))))
                .ifPresent(sendMqttMessage -> sendMqttMessage.setConfirmStatusEnum(confirmStatusEnum));
    }

    @Override
    public void closeChannel(Channel channel, boolean isDisconnect){
        String clientId = NettyUtils.clientID(channel);
        ofNullable(sessionRepository.get(clientId)).ifPresent(sessionInfo -> {
            sessionInfo.setSessionStatus(false);
            sessionInfo.close();
            sessionInfo.setChannel(null);
            if (!sessionInfo.isCleanSession()){ // 保存会话
                // 处理待确认的方法
                for (Map.Entry<Integer, SendMqttMessage> entry : sessionInfo.getMessage().entrySet()) {
                    SendMqttMessage message = entry.getValue();
                    sendMqttMessageRepository.add(clientId, message);
                }
            }else{
                sessionRepository.remove(clientId);
            }

            if (!isDisconnect && sessionInfo.isWill()){
                // 发送遗嘱信息
                ofNullable(willRepository.get(clientId)).ifPresent(this::sendWillMessage);
            }

            if (sessionInfo.isWill()){
                // 必须丢弃任何与当前连接关联的未发布的遗嘱消息
                willRepository.remove(clientId);
            }

        });
    }

    @Override
    public void sendConnack(Channel channel, boolean cleanSession, MqttConnectReturnCode returnCode, final String finalClientId){
        boolean sessionPresent = false;
        if (returnCode.byteValue() == 0 && cleanSession){
            sessionPresent = true;
        }
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNACK,false, MqttQoS.AT_MOST_ONCE,
                false,0);
        MqttConnAckVariableHeader variableHeader = new MqttConnAckVariableHeader(returnCode,sessionPresent);

        MqttConnAckMessage message = new MqttConnAckMessage(fixedHeader,variableHeader);
        if (returnCode.byteValue() > 0){
            channel.writeAndFlush(message).addListener(FIRE_EXCEPTION_ON_FAILURE);
            channel.close().addListener(CLOSE_ON_FAILURE);
        }else{
            channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()){
                    log.debug("ConnAck has been sent.ClientId={}", finalClientId);
                }else {
                    future.channel().pipeline().fireExceptionCaught(future.cause());
                }
            });
        }
    }

    @Override
    public void sendPublish(Channel channel, SendMqttMessage msg){
        MqttFixedHeader header = new MqttFixedHeader(
                MqttMessageType.PUBLISH,false, msg.getQos(),
                false,0);
        int packetId = 0;
        if (!msg.getQos().equals(MqttQoS.AT_MOST_ONCE)){
            packetId = msg.getMsgId();
        }
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(msg.getTopic(),packetId);

        final ByteBuf origPayload = Unpooled.copiedBuffer(msg.getByteBuf());
        ByteBuf payload = origPayload.retainedDuplicate();
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(header,variableHeader, payload);

        channel.writeAndFlush(mqttPublishMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void sendPublishAck(Channel channel, int msgId){
        MqttFixedHeader header = new MqttFixedHeader(
                MqttMessageType.PUBACK,false, MqttQoS.AT_MOST_ONCE,
                false,0x02);
        MqttPubAckMessage msg = new MqttPubAckMessage(header, MqttMessageIdVariableHeader.from(msgId));
        channel.writeAndFlush(msg).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void sendPublishRec(Channel channel, int msgId){
        MqttFixedHeader header = new MqttFixedHeader(
                MqttMessageType.PUBREC,false, MqttQoS.AT_MOST_ONCE,
                false,0x02);
        MqttPubAckMessage msg = new MqttPubAckMessage(header, MqttMessageIdVariableHeader.from(msgId));
        channel.writeAndFlush(msg).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void sendPublishRel(Channel channel, int msgId){
        MqttFixedHeader header = new MqttFixedHeader(
                MqttMessageType.PUBREL,false, MqttQoS.AT_MOST_ONCE,
                false,0x02);
        MqttPubAckMessage msg = new MqttPubAckMessage(header, MqttMessageIdVariableHeader.from(msgId));
        channel.writeAndFlush(msg).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void sendPublishComp(Channel channel, int msgId){
        MqttFixedHeader header = new MqttFixedHeader(
                MqttMessageType.PUBCOMP,false, MqttQoS.AT_MOST_ONCE,
                false,0x02);
        MqttPubAckMessage msg = new MqttPubAckMessage(header, MqttMessageIdVariableHeader.from(msgId));
        channel.writeAndFlush(msg).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public void sendRetainMsg(Set<String> topics, ClientSessionInfo info) {
        if (info.isActive()){
            topics.forEach(topic->{
                ConcurrentLinkedQueue<RetainMessage> retainMessages = retainMessageRepository.get(topic);
                retainMessages.forEach(retainMessage -> {
                    if (retainMessage.getTopic().equals(topic)){
                        SendMqttMessage sendMqttMessage = SendMqttMessage.builder()
                                .topic(topic).qos(retainMessage.getQos())
                                .byteBuf(retainMessage.getByteBuf())
                                .confirmStatusEnum(ConfirmStatusEnum.PUB)
                                .msgId(MessageIdUtils.messageId()).build();
                        if (!sendMqttMessage.getQos().equals(MqttQoS.AT_MOST_ONCE)){
                            info.getMessage().put(sendMqttMessage.getMsgId(),sendMqttMessage);
                            sendMqttMessageRepository.add(info.getClientId(),sendMqttMessage);
                        }
                        sendPublish(info.getChannel(),sendMqttMessage);
                    }
                });
            });
        }
    }

    @Override
    public void sendWillMessage(WillMessage will) {
        SendMqttMessage mqttMessage = SendMqttMessage.builder()
                .msgId(MessageIdUtils.messageId())
                .topic(will.getWillTopic()).qos(MqttQoS.valueOf(will.getWillQos()))
                .confirmStatusEnum(ConfirmStatusEnum.PUB)
                .byteBuf(will.getWillMessage().getBytes())
                .build();
        // TODO 是否需要发送遗嘱消息给订阅的终端
    }


    private void autoFlusher(Channel channel, int flushIntervalMs) {
        try {
            channel.pipeline().addAfter("idleEventHandler","autoFlusher",new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS));
        }catch (Exception e){
            channel.pipeline().addFirst("autoFlusher",new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS));
        }
    }


}
