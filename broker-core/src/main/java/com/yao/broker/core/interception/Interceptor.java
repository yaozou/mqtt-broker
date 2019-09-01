package com.yao.broker.core.interception;

import io.netty.handler.codec.mqtt.MqttPublishMessage;

/**
 * @Description: 拦截器
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
public interface Interceptor {
    /**
     * notifyTopicPublished
     *
     * @param msg      MqttPublishMessage
     * @param clientID String
     * @param username String
     */
    void notifyTopicPublished(MqttPublishMessage msg, String clientID, String username);
}
