package com.yao.broker.core.repository;

import com.yao.broker.core.bean.WillMessage;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Repository
public class WillRepository {
    Map<String, WillMessage> cache = new ConcurrentHashMap<>(32);

    public void add(WillMessage message,String clientId){
        cache.put(clientId,message);
    }

    public void remove(String clientId){
        cache.remove(clientId);
    }

    public WillMessage get(String clientId){
        return cache.get(clientId);
    }
}
