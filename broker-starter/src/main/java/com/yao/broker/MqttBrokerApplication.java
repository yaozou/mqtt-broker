package com.yao.broker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/11 17:58
 */
@SpringBootApplication
public class MqttBrokerApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(MqttBrokerApplication.class, args);

            System.out.println("***************************************");
            System.out.println("***************************************");
            System.out.println("*******Platform  MQTT 启动成功*********");
            System.out.println("***************************************");
            System.out.println("***************************************");

        } catch (Exception ex) {

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
