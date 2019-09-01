package com.yao.broker.core.interception.message;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Builder;
import lombok.Data;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Data
@Builder
public class PublishInterceptMessage {
    private final MqttPublishMessage msg;
    private final String clientID;
    private final String username;
    public String getTopicName() {
        return msg.variableHeader().topicName();
    }

    public ByteBuf getPayload() {
        return msg.payload();
    }

    public String getClientID() {
        return clientID;
    }

    public String getUsername() {
        return username;
    }

}
