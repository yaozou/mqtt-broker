package com.yao.broker.core.netty.repository;

import com.yao.broker.core.netty.bean.ClientSession;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: session信息
 * @author: yaozou
 * @Date: 2019/8/8 10:24
 */
@Service
public class SessionRepository {
    Map<String, ClientSession> cacheSession = new HashMap<>(64);

    public boolean containsKey(String clientId){
        return cacheSession.containsKey(clientId);
    }

    public ClientSession get(String clientId){
        return cacheSession.get(clientId);
    }

    public ClientSession createOrLoadClientSession(String clientId, boolean cleanSession){
        ClientSession clientSession = this.get(clientId);
        boolean addFlag = true;
        if (clientSession != null && !cleanSession){
            addFlag = false;
        }

        if (cleanSession){
            cacheSession.remove(clientId);
        }

        if (addFlag){
            clientSession = new ClientSession(clientId);
            cacheSession.put(clientId,clientSession);
        }

        clientSession.cleanSession();

        return clientSession;
    }

}
