package com.yao.broker.core.repository;

import com.yao.broker.core.bean.SendMqttMessage;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Description: 发送消息存储
 * @Author yao.zou
 * @Date 2019/8/28 0028
 * @Version V1.0
 **/
@Repository
public class SendMqttMessageRepository {
    private static ConcurrentHashMap<String, ConcurrentLinkedQueue<SendMqttMessage>> cache  = new ConcurrentHashMap<>();

    public void add(String clientId, SendMqttMessage message){
        ConcurrentLinkedQueue<SendMqttMessage> queue = cache.getOrDefault(clientId,new ConcurrentLinkedQueue<>());
        while (!queue.offer(message)){}
        cache.put(clientId,queue);
    }

    public ConcurrentLinkedQueue<SendMqttMessage> get(String clientId){
        return cache.getOrDefault(clientId,new ConcurrentLinkedQueue<>());
    }
}
