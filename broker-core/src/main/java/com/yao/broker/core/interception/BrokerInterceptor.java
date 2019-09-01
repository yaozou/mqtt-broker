package com.yao.broker.core.interception;

import com.yao.broker.core.config.NettyConfig;
import com.yao.broker.core.interception.message.PublishInterceptMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Slf4j
@Service
public class BrokerInterceptor implements Interceptor{

    @Autowired
    private NettyConfig nettyConfig;

    private ExecutorService executor;
    private Map<Class<?>, List<InterceptHandler>> handlers;

    @PostConstruct
    public void init(){
        this.handlers = new HashMap<>();
        for (Class<?> messageType : InterceptHandler.ALL_MESSAGE_TYPES) {
            this.handlers.put(messageType, new CopyOnWriteArrayList<InterceptHandler>());
        }

        List<InterceptHandler> userHandlers = Collections.singletonList(new PublisherListener());
        for (InterceptHandler handler : userHandlers){
            this.addInterceptHandler(handler);
        }

        // TODO 优化
        executor = new ThreadPoolExecutor(nettyConfig.getMsgHandlerThreadPool(), nettyConfig.getMsgHandlerThreadPool(), 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            private final AtomicInteger poolNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "broker-interceptor-" + poolNumber.getAndIncrement());
            }
        });
    }
    @Override
    public void notifyTopicPublished(MqttPublishMessage msg, String clientID, String username) {
        executor.submit(()->{
            try {
                int messageId = msg.variableHeader().packetId();
                String topic = msg.variableHeader().topicName();
                for (InterceptHandler handler : handlers.get(PublishInterceptMessage.class)) {
                    log.debug("Notifying MQTT PUBLISH message to interceptor. CId={}, messageId={}, topic={}", clientID, messageId, topic);
                    handler.onPublish(PublishInterceptMessage.builder().msg(msg).clientID(clientID).username(username).build());
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        });
    }

    public void addInterceptHandler(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = getInterceptedMessageTypes(interceptHandler);
        for (Class<?> interceptMessageType : interceptedMessageTypes) {
            this.handlers.get(interceptMessageType).add(interceptHandler);
        }
    }

    private static Class<?>[] getInterceptedMessageTypes(InterceptHandler interceptHandler) {
        Class<?>[] interceptedMessageTypes = interceptHandler.getInterceptedMessageTypes();
        if (interceptedMessageTypes == null) {
            return InterceptHandler.ALL_MESSAGE_TYPES;
        }
        return interceptedMessageTypes;
    }
}
