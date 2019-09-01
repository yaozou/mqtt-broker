package com.yao.broker.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * @Description: netty 配置参数
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Data
@Component
@Configuration
@PropertySource("classpath:netty.properties")
@ConfigurationProperties(prefix = "netty")
public class NettyConfig {
    private String host;
    private int port;

    private int soBacklog = 128;
    private boolean soReuseaddr;
    private boolean tcpNodelay;
    private boolean soKeepalive = true;

    private boolean needsClientAuth = true;
    private String keyManagerPassword;
    private String sslProvider;
    private String jksPath;
    private String keyStorePassword;

    private int channelTimeoutSeconds;


    private int msgHandlerThreadPool = 1;
}
