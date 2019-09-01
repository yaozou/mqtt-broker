package com.yao.broker.core.bean;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Builder;
import lombok.Data;

/**
 * @Description: 订阅信息
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Data
@Builder
public class SubscribeInfo {
    private String topic;
    private MqttQoS qos;

    @Override
    public String toString(){
        return "{topic=" +topic+",qos="+qos.toString()+
                "}";
    }
}
