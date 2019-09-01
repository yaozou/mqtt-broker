package com.yao.broker.core.bean;

import com.yao.broker.core.utils.NettyUtils;
import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Data;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description: 会话状态
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Data
@Builder
public class ClientSessionInfo {
    private String clientId;
    private boolean cleanSession;

    /** 客户端订阅信息 */
    private Set<String> topics;

    /** 待确认的消息 messageId - message(qos1) */
    private ConcurrentHashMap<Integer,SendMqttMessage>  message;

    private boolean isWill;

    private Channel channel;

    private boolean sessionStatus;
    private boolean subscribeStatus;


    public boolean isActive(){
        return channel != null && channel.isActive();
    }

    public boolean isLogin(){
        return NettyUtils.login(channel) && isActive();
    }

    public void close(){
        Optional.ofNullable(this.channel).ifPresent(channel1 -> channel1.close());
    }

}
