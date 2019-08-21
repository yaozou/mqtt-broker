package com.yao.broker.core.netty.interception;

import com.yao.broker.core.netty.bean.Subscription;
import com.yao.broker.core.netty.bean.Topic;
import com.yao.broker.core.netty.interception.messages.*;
import com.yao.broker.core.netty.repository.MessageRepository;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 网关拦截器
 * @author yaozou
 */
@Slf4j
public class BrokerInterceptor implements Interceptor {
    private final Map<Class<?>, List<InterceptHandler>> handlers;
    private final ExecutorService executor;
    private MessageRepository messageRepository;

    public BrokerInterceptor(List<InterceptHandler> handlers,MessageRepository messageRepository) {
        this(1, handlers);
        this.messageRepository = messageRepository;
    }

    private BrokerInterceptor(int poolSize, List<InterceptHandler> handlers) {
        log.info("Initializing broker interceptor. InterceptorIds={}", getInterceptorIds(handlers));
        this.handlers = new HashMap<>();
        for (Class<?> messageType : InterceptHandler.ALL_MESSAGE_TYPES) {
            this.handlers.put(messageType, new CopyOnWriteArrayList<InterceptHandler>());
        }
        for (InterceptHandler handler : handlers) {
            this.addInterceptHandler(handler);
        }
        executor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger poolNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "broker-interceptor-" + poolNumber.getAndIncrement());
            }
        });
    }

    void stop(){
        log.info("Shutting down interceptor thread pool...");
        executor.shutdown();
        try {
            log.info("Waiting for thread pool tasks to terminate...");
            executor.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!executor.isTerminated()) {
            log.warn("Forcing shutdown of interceptor thread pool...");
            executor.shutdownNow();
        }
        log.info("interceptors stopped");
    }

    @Override
    public void notifyClientConnected(MqttConnectMessage msg) {
        for (final InterceptHandler handler : this.handlers.get(ConnectInterceptMessage.class)) {
            log.debug("Sending MQTT CONNECT message to interceptor. CId={}, interceptorId={}",
                    msg.payload().clientIdentifier(), handler.getID());
            executor.execute(() -> handler.onConnect(new ConnectInterceptMessage(msg)));
        }
    }

    @Override
    public void notifyClientDisconnected(String clientID, String username) {
        for (final InterceptHandler handler : this.handlers.get(InterceptDisconnectMessage.class)) {
            log.debug("Notifying MQTT client disconnection to interceptor. CId={}, username={}, interceptorId={}",
                    clientID, username, handler.getID());
            executor.execute(() -> handler.onDisconnect(new InterceptDisconnectMessage(clientID, username)));
        }
    }

    @Override
    public void notifyClientConnectionLost(String clientID, String username) {
        for (final InterceptHandler handler : this.handlers.get(InterceptConnectionLostMessage.class)) {
            log.debug("Notifying unexpected MQTT client disconnection to interceptor CId={}, username={}, " +
                            "interceptorId={}", clientID, username, handler.getID());
            executor.execute(() -> handler.onConnectionLost(new InterceptConnectionLostMessage(clientID, username)));
        }
    }

    @Override
    public void notifyTopicPublished(MqttPublishMessage msg, String clientID, String username) {
        msg.retain();
        executor.execute(() -> {
            try {
                int messageId = msg.variableHeader().packetId();
                String topic = msg.variableHeader().topicName();
                for (InterceptHandler handler : handlers.get(PublishInterceptMessage.class)) {
                    log.debug("Notifying MQTT PUBLISH message to interceptor. CId={}, messageId={}, topic={}, "
                                    + "interceptorId={}", clientID, messageId, topic, handler.getID());
                    handler.onPublish(new PublishInterceptMessage(msg, clientID, username));
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        });
    }

    @Override
    public void notifyTopicSubscribed(Subscription sub, String username) {
        for (final InterceptHandler handler : this.handlers.get(InterceptSubscribeMessage.class)) {
            log.debug("Notifying MQTT SUBSCRIBE message to interceptor. CId={}, topicFilter={}, interceptorId={}",
                    sub.getClientId(), sub.getTopic(), handler.getID());
            executor.execute(() -> handler.onSubscribe(new InterceptSubscribeMessage(sub, username)));
        }
    }

    @Override
    public void notifyTopicUnsubscribed(String topic, String clientID, String username) {
        for (final InterceptHandler handler : this.handlers.get(InterceptUnsubscribeMessage.class)) {
            log.debug("Notifying MQTT UNSUBSCRIBE message to interceptor. CId={}, topic={}, interceptorId={}",
                    clientID,
                    topic, handler.getID());
            executor.execute(() -> handler.onUnsubscribe(new InterceptUnsubscribeMessage(topic, clientID, username)));
        }
    }

    @Override
    public void notifyMessageAcknowledged(final InterceptAcknowledgedMessage msg) {
        for (final InterceptHandler handler : this.handlers.get(InterceptAcknowledgedMessage.class)) {
            log.debug(
                    "Notifying MQTT ACK message to interceptor. CId={}, messageId={}, topic={}, interceptorId={}",
                    msg.getMsg().getClientId(), msg.getPacketID(), msg.getTopic(), handler.getID());
            executor.execute(() -> handler.onMessageAcknowledged(msg));
        }
        // 删除缓存消息
        Topic topic = new Topic(msg.getTopic());
        messageRepository.clear(topic);
    }

    @Override
    public void addInterceptHandler(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = getInterceptedMessageTypes(interceptHandler);
        log.info("Adding MQTT message interceptor. InterceptorId={}, handledMessageTypes={}",
                interceptHandler.getID(), interceptedMessageTypes);
        for (Class<?> interceptMessageType : interceptedMessageTypes) {
            this.handlers.get(interceptMessageType).add(interceptHandler);
        }
    }

    @Override
    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = getInterceptedMessageTypes(interceptHandler);
        log.info("Removing MQTT message interceptor. InterceptorId={}, handledMessageTypes={}",
                interceptHandler.getID(), interceptedMessageTypes);
        for (Class<?> interceptMessageType : interceptedMessageTypes) {
            this.handlers.get(interceptMessageType).remove(interceptHandler);
        }
    }

    private static Class<?>[] getInterceptedMessageTypes(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = interceptHandler.getInterceptedMessageTypes();
        if (interceptedMessageTypes == null) {
            return InterceptHandler.ALL_MESSAGE_TYPES;
        }
        return interceptedMessageTypes;
    }

    public static <T extends InterceptHandler> Collection<String> getInterceptorIds(
            Collection<T> handlers) {
        Collection<String> result = new ArrayList<>(handlers.size());
        for (T handler : handlers) {
            result.add(handler.getID());
        }
        return result;
    }
}
