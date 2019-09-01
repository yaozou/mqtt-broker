package com.yao.broker.core.interception;

import com.yao.broker.core.interception.message.PublishInterceptMessage;

/**
 * @Description: 拦截处理器
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
public interface InterceptHandler {
     Class<?>[] ALL_MESSAGE_TYPES = {PublishInterceptMessage.class};

     /**
      * 发布消息
      * @param msg 消息
      */
     void onPublish(PublishInterceptMessage msg);

     Class<?>[] getInterceptedMessageTypes();
}
