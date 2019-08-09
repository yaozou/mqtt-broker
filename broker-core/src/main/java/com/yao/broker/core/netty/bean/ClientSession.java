package com.yao.broker.core.netty.bean;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/8 11:04
 */
@Data
public class ClientSession {
    private final String clientId;
    private Set<Subscription>  subscriptions = new HashSet<>();

    public ClientSession(String clientId){
        this.clientId = clientId;
    }

    public void cleanSession(){

    }

    public int getMessage(StoreMessage message){
        return 1;
    }

    public boolean isCleanSession() {
        return false;
    }

    public void enqueue(StoreMessage message){

    }
}
