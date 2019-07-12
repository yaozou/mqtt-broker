package com.yao.broker.core;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/12 17:50
 */
@Slf4j
@Service
public class BrokerServer {
    public void startServer(){
        long start = System.currentTimeMillis();

        // TCP/IP连接 网关接收器


        log.info("MQTT broker has been started successfully in {} ms.",System.currentTimeMillis()-start);
    }

    public void closeServer(){

    }
}
