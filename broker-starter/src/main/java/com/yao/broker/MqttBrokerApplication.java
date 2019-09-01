package com.yao.broker;

import com.yao.broker.core.NettyServer;
import com.yao.broker.core.server.IServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/11 17:58
 */
@SpringBootApplication
@Controller
@Slf4j
public class MqttBrokerApplication {
    @Autowired
    private NettyServer nettyServer;
    @PostConstruct
    public void startNetty(){
        log.info("netty server begin to  start.");
        nettyServer.start();
        log.info("netty server started successful.");
    }
    public static void main(String[] args) {
        try {
            SpringApplication.run(MqttBrokerApplication.class, args);
            System.out.println("***************************************");
            System.out.println("***************************************");
            System.out.println("*******Platform  MQTT 启动成功*********");
            System.out.println("***************************************");
            System.out.println("***************************************");

        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("***************************************");
            System.out.println(ex.getMessage());
            System.out.println("***************************************");
            System.out.println("***************************************");
            System.out.println("******Platform  MQTT 启动失败**********");
            System.out.println("***************************************");
            System.out.println("***************************************");

        }

    }
}
