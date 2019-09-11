package com.yao.broker.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * @Description: 应用监听
 * @Author yao.zou
 * @Date 2019/9/9 0009
 * @Version V1.0
 **/
@Component
@Slf4j
public class ApplicationEventListener implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextClosedEvent || event instanceof ContextClosedEvent){
            try {

            } catch (Exception e) {
               log.error("Close rabbitmq error:{}",e.getMessage());
            }
        }
    }
}
