package com.yao.broker.core.netty.interception;


import com.yao.broker.core.netty.interception.messages.*;

public interface InterceptHandler {
    Class<?>[] ALL_MESSAGE_TYPES = {ConnectInterceptMessage.class, InterceptDisconnectMessage.class,
            InterceptConnectionLostMessage.class, PublishInterceptMessage.class,
            InterceptSubscribeMessage.class,
            InterceptUnsubscribeMessage.class, InterceptAcknowledgedMessage.class};

    /**
     * Returns the identifier of this intercept handler.
     *
     * @return String
     */
    String getID();

    /**
     * Returns the InterceptMessage subtypes that this handler can process. If the result is null or
     * equal to ALL_MESSAGE_TYPES, all the message types will be processed.
     *
     * @return Class<?>[]
     */
    Class<?>[] getInterceptedMessageTypes();

    /**
     * onConnect
     *
     * @param msg ConnectInterceptMessage
     */
    void onConnect(ConnectInterceptMessage msg);

    /**
     * onDisconnect
     *
     * @param msg InterceptDisconnectMessage
     */
    void onDisconnect(InterceptDisconnectMessage msg);

    /**
     * onConnectionLost
     *
     * @param msg InterceptConnectionLostMessage
     */
    void onConnectionLost(InterceptConnectionLostMessage msg);

    /**
     * onPublish
     *
     * @param msg PublishInterceptMessage
     */
    void onPublish(PublishInterceptMessage msg);

    /**
     * onSubscribe
     *
     * @param msg InterceptSubscribeMessage
     */
    void onSubscribe(InterceptSubscribeMessage msg);

    /**
     * onUnsubscribe
     *
     * @param msg InterceptUnsubscribeMessage
     */
    void onUnsubscribe(InterceptUnsubscribeMessage msg);

    /**
     * onMessageAcknowledged
     *
     * @param msg InterceptAcknowledgedMessage
     */
    void onMessageAcknowledged(InterceptAcknowledgedMessage msg);
}
