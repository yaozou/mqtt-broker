package com.yao.broker.core.netty.repository;

import com.yao.broker.core.netty.bean.ConnectionInfo;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @Description:连接信息
 * @author: yaozou
 * @Date: 2019/8/8 10:42
 */
@Service
public class ConnectionRepository {
    /** key:clientId value:keepAlive*/
    Map<String, ConnectionInfo> cache = new HashMap(64);


    public void addConnection(ConnectionInfo info){
        cache.putIfAbsent(info.getClientId(),info);
    }

    public boolean isConnected(String client){
        return cache.containsKey(client);
    }

    public Optional<ConnectionInfo> lookupDescriptor(String clientID) {
        if (clientID == null) {
            return Optional.empty();
        }

        ConnectionInfo descriptor = cache.get(clientID);
        if (descriptor == null) {
            return Optional.empty();
        }
        return Optional.of(descriptor);
    }
}
