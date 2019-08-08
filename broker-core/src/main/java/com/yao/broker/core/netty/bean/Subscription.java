package com.yao.broker.core.netty.bean;

import java.io.Serializable;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/8 11:16
 */
@Data
public class Subscription implements Serializable {
    private final MqttQoS qos;
    private final String clientId;
    private final Topic  topic;
    private final boolean active;

    public Subscription(String clientId, Topic topic, MqttQoS qos) {
        this.qos = qos;
        this.clientId = clientId;
        this.topic = topic;
        this.active = true;
    }
}
