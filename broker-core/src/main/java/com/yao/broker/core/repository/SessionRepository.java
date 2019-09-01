package com.yao.broker.core.repository;

import com.yao.broker.core.bean.ClientSessionInfo;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: session存储
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Repository
public class SessionRepository {
    /** clientId -> ClientSessionInfo */
    Map<String, ClientSessionInfo> cache = new ConcurrentHashMap<>(32);

   public boolean isActive(String clientId){
        ClientSessionInfo sessionInfo =  cache.get(clientId);
        return sessionInfo != null && sessionInfo.isActive();
    }

    public void add(String clientId,ClientSessionInfo sessionInfo){
       cache.put(clientId,sessionInfo);
    }

    public ClientSessionInfo get(String clientId){
       return cache.get(clientId);
    }

    public void remove(String clientId){
       cache.remove(clientId);
    }
}
