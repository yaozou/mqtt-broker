package com.yao.broker.core.bean;

import com.yao.broker.core.enums.ConfirmStatusEnum;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Builder;
import lombok.Data;

/**
 * @Description: 发送MQTT消息
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Data
@Builder
public class SendMqttMessage {
    private int msgId;
    private String topic;
    private MqttQoS qos;

    private ConfirmStatusEnum confirmStatusEnum;

    private byte[]  byteBuf;
}
