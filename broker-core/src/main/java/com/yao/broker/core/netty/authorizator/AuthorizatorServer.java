package com.yao.broker.core.netty.authorizator;

import com.yao.broker.core.netty.bean.Topic;

import org.springframework.stereotype.Service;

/**
 * @Description:鉴权
 * @author: yaozou
 * @Date: 2019/8/8 15:39
 */
@Service
public class AuthorizatorServer {

    public boolean canRead(Topic topic, String user, String client) {
        return true;
    }

    public boolean canWrite(Topic topic, String user, String client) {
        return true;
    }

}
