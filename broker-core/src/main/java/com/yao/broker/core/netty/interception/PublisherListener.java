package com.yao.broker.core.netty.interception;

import com.yao.broker.core.netty.interception.messages.PublishInterceptMessage;
import com.yao.broker.core.utils.ByteUtil;

/**
 * 发布者监听
 * @author yaozou
 */
public class PublisherListener extends AbstractInterceptHandler{
    @Override
    public void onPublish(PublishInterceptMessage msg) {
        byte[] bytes = new byte[msg.getPayload().readableBytes()];
        msg.getPayload().getBytes(0, bytes);
        final String decodedPayload = ByteUtil.bytesToHexString(bytes);
        System.out.println("Received on topic: " + msg.getTopicName() + " content: " + decodedPayload);
    }
}
