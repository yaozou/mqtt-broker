package com.yao.broker.core.netty.bean;

import java.io.Serializable;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Data;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/9 11:16
 */
@Data
public class StoreMessage implements Serializable {

    private final MqttQoS qos;
    private final byte[]  payload;
    private final String  topicName;
    private final String  clientId;
    private final boolean retained;

    public StoreMessage( MqttQoS qos,byte[]  payload,
                         String  topicName,String  clientId,
                         boolean retained){
        this.qos = qos;
        this.payload = payload;
        this.topicName = topicName;
        this.clientId  = clientId;
        this.retained = retained;
    }

    public ByteBuf getPayload(){
        return Unpooled.copiedBuffer(payload);
    }

}
