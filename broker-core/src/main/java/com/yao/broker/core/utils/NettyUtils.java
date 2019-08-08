package com.yao.broker.core.utils;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/16 15:46
 */
public class NettyUtils {

    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_CLIENTID = "ClientID";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String CLEAN_SESSION = "removeTemporaryQoS2";

    private static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey
            .valueOf(ATTR_CLIENTID);
    private static final AttributeKey<Object> ATTR_KEY_KEEPALIVE = AttributeKey
            .valueOf(KEEP_ALIVE);
    private static final AttributeKey<Object> ATTR_KEY_CLEANSESSION = AttributeKey
            .valueOf(CLEAN_SESSION);
    private static final AttributeKey<Object> ATTR_KEY_USERNAME = AttributeKey
            .valueOf(ATTR_USERNAME);

    public static void clientID(Channel channel, String clientID) {
        channel.attr(NettyUtils.ATTR_KEY_CLIENTID).set(clientID);
    }

    public static String clientID(Channel channel) {
        return (String) channel.attr(NettyUtils.ATTR_KEY_CLIENTID).get();
    }

    public static void keepAlive(Channel channel, int keepAlive) {
        channel.attr(NettyUtils.ATTR_KEY_KEEPALIVE).set(keepAlive);
    }

    public static void cleanSession(Channel channel, boolean cleanSession) {
        channel.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(cleanSession);
    }
    public static String userName(Channel channel) {
        return (String) channel.attr(NettyUtils.ATTR_KEY_USERNAME).get();
    }
}
