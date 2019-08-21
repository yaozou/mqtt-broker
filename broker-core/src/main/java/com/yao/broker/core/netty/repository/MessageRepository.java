package com.yao.broker.core.netty.repository;

import com.yao.broker.core.netty.bean.StoreMessage;
import com.yao.broker.core.netty.bean.Topic;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/9 11:07
 */
@Service
public class MessageRepository {
    Map<Topic, StoreMessage> cache = new HashMap<>(64);

    public void cleanRetained(Topic topic){
        cache.remove(topic);
    }

    public void clear(Topic topic){cache.remove(topic);}
}
