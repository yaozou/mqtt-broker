package com.yao.broker.core.utils;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/16 15:46
 */
public class NettyUtils {

    public static final String ATTR_CLIENTID = "ClientID";

    private static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey
            .valueOf(ATTR_CLIENTID);

    public static void clientID(Channel channel, String clientID) {
        channel.attr(NettyUtils.ATTR_KEY_CLIENTID).set(clientID);
    }

    public static String clientID(Channel channel) {
        return (String) channel.attr(NettyUtils.ATTR_KEY_CLIENTID).get();
    }
}
