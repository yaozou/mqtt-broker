package com.yao.broker.core.repository;

import com.yao.broker.core.bean.RetainMessage;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Description: 保留消息
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Repository
public class RetainMessageRepository {
    Map<String, ConcurrentLinkedQueue<RetainMessage>> cache = new ConcurrentHashMap<>(16);
    public void add(RetainMessage msg,boolean clearFlag){
        ConcurrentLinkedQueue<RetainMessage> retainMessages = cache.getOrDefault(msg.getTopic(), new ConcurrentLinkedQueue<>());
        if (clearFlag){
            retainMessages.clear();
        }
        while (!retainMessages.add(msg)){};
        cache.put(msg.getTopic(),retainMessages);
    }

    public ConcurrentLinkedQueue<RetainMessage> get(String topic){
        return cache.getOrDefault(topic, new ConcurrentLinkedQueue<>());
    }
}
