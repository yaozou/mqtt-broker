package com.yao.broker.core.netty.repository;

import com.yao.broker.core.netty.bean.Subscription;
import com.yao.broker.core.netty.bean.Topic;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/8 11:27
 */
@Service
public class SubscriptionRepository {

    public void remove(Topic topic , String client){

    }

    public void add(Subscription newSubscription){

    }

    public List<Subscription> match(Topic topic){
        return new ArrayList<>();
    }

}
