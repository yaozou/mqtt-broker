package com.yao.broker.core.interception;

import com.yao.broker.core.interception.message.PublishInterceptMessage;
import com.yao.broker.core.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Slf4j
public class PublisherListener implements InterceptHandler{
    @Override
    public void onPublish(PublishInterceptMessage msg) {
       try {
           final String decodedPayload = ByteUtil.bytesToHexString(msg.getBytes());
           log.info("Received on topic: " + msg.getTopicName() + " HexString: " + decodedPayload);
           log.info("Received on topic: " + msg.getTopicName() + " String:"+new String(msg.getBytes()));
       }catch (Exception e){
           e.printStackTrace();
       }
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return ALL_MESSAGE_TYPES;
    }
}
