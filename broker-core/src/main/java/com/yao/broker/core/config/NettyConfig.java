package com.yao.broker.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Data;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/16 14:16
 */
@Data
@Configuration
@PropertySource("classpath:netty.properties")
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {
    private String host;
    private int port;

    private boolean epoll = false;
    private boolean ssl = false;

    private int channelTimeoutSeconds = 10;

    private int soBacklog = 128;
    private boolean soReuseaddr = true;
    private boolean tcpNodelay = true;
    private boolean soKeepalive = true;

}
