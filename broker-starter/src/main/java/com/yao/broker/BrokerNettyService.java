package com.yao.broker;

import com.yao.broker.core.server.IServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @Description: 网关netty服务
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Service
@Slf4j
public class BrokerNettyService {
    @Autowired
    private IServer nettyServer;

    @PostConstruct
    public void startServer(){
        log.info("Netty broker begin to start.");
        nettyServer.start();
        log.info("Netty broker started successfully.");
    }
}
