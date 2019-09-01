package com.yao.broker.core.bean;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Builder;
import lombok.Data;

/**
 * @Description: 保留消息
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Data
@Builder
public class RetainMessage {
    private String topic;
    private MqttQoS qos;
    private  byte[]  byteBuf;

}
