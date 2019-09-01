package com.yao.broker.core.utils;


import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
public class NettyUtils {

    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_CLIENTID = "ClientID";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String CLEAN_SESSION = "removeTemporaryQoS2";
    public static final String ATTR_LOGIN = "login";

    public static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey
            .valueOf(ATTR_CLIENTID);
    public static final AttributeKey<Object> ATTR_KEY_KEEPALIVE = AttributeKey
            .valueOf(KEEP_ALIVE);
    public static final AttributeKey<Object> ATTR_KEY_CLEANSESSION = AttributeKey
            .valueOf(CLEAN_SESSION);
    public static final AttributeKey<Object> ATTR_KEY_USERNAME = AttributeKey
            .valueOf(ATTR_USERNAME);
    public static final AttributeKey<Object> ATTR_KEY_LOGIN = AttributeKey
            .valueOf(ATTR_LOGIN);


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

    public static void login(Channel channel, boolean login) {
        channel.attr(NettyUtils.ATTR_KEY_LOGIN).set(login);
    }

    public static boolean login(Channel channel){
        return channel != null && channel.hasAttr(ATTR_KEY_LOGIN);
    }
}
