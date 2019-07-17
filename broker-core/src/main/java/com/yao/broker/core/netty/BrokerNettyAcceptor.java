package com.yao.broker.core.netty;

import com.yao.broker.core.config.NettyConfig;
import com.yao.broker.core.netty.channel.EpollServerSocketChannel;
import com.yao.broker.core.netty.channel.NioServerSocketChannel;
import com.yao.broker.core.netty.handler.NettyMqttHandler;
import com.yao.broker.core.netty.handler.TcpIdleTimeoutHandler;
import com.yao.broker.core.netty.pipeline.PipelineInitializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/12 18:08
 */
@Slf4j
@Service
public class BrokerNettyAcceptor implements BrokerAcceptor {

    @Autowired
    private NettyConfig nettyConfig;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Class<? extends ServerSocketChannel> channel;
    private NettyMqttHandler nettyMqttHandler;

    @Override
    public void connection() {
        boolean epoll = nettyConfig.isEpoll();
        if (epoll){
            bossGroup = new EpollEventLoopGroup();
            workerGroup = new EpollEventLoopGroup();
            channel = EpollServerSocketChannel.class;
        }else{
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            channel = NioServerSocketChannel.class;
        }
        nettyMqttHandler = new NettyMqttHandler();
        boolean ssl = nettyConfig.isSsl();
        if (ssl){
            startSslTCPTransport();
        }else {
            startPlainTCPTransport();
        }

    }

    @Override
    public void close() {

    }

    private void startPlainTCPTransport(){
        String host = nettyConfig.getHost();
        int port = nettyConfig.getPort();

        final TcpIdleTimeoutHandler timeoutHandler = new TcpIdleTimeoutHandler();

        startFactory(host, port, "TCP MQTT", new PipelineInitializer() {
            @Override
            public void init(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                // 设置心跳机制
                pipeline.addFirst("idleStateHandler",new IdleStateHandler(nettyConfig.getChannelTimeoutSeconds(),0,0));
                pipeline.addAfter("idleStateHandler","idleEventHandler",timeoutHandler);

                // 加解密 (netty自带的MQTT协议解析类)
                pipeline.addLast("decoder", new MqttDecoder());
                pipeline.addLast("encoder", MqttEncoder.INSTANCE);

                // 应用消息处理
                pipeline.addLast("handler",nettyMqttHandler);
            }
        });
    }

    private void startSslTCPTransport() {
        String host = nettyConfig.getHost();
        int port = nettyConfig.getPort();

        final TcpIdleTimeoutHandler timeoutHandler = new TcpIdleTimeoutHandler();

        startFactory(host,port,"SSL TCP MQTT",new PipelineInitializer(){
            @Override
            public void init(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast("ssl",createSslHandler(channel));

                // 设置心跳机制
                pipeline.addFirst("idleStateHandler",new IdleStateHandler(nettyConfig.getChannelTimeoutSeconds(),0,0));
                pipeline.addAfter("idleStateHandler","idleEventHandler",timeoutHandler);

                // 加解密 (netty自带的MQTT协议解析类)
                pipeline.addLast("decoder", new MqttDecoder());
                pipeline.addLast("encoder", MqttEncoder.INSTANCE);

                // 应用消息处理
                pipeline.addLast("handler",nettyMqttHandler);
            }
        });
    }

    private void startFactory(String host, int port,String protocol,final PipelineInitializer initializer){
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup,workerGroup).channel(channel)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        initializer.init(socketChannel);
                    }
                })
                .option(ChannelOption.SO_BACKLOG,nettyConfig.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR,nettyConfig.isSoReuseaddr())
                .childOption(ChannelOption.TCP_NODELAY,nettyConfig.isTcpNodelay())
                .childOption(ChannelOption.SO_KEEPALIVE,nettyConfig.isSoKeepalive());
        try{
            ChannelFuture future = bootstrap.bind(host,port);
            future.sync().addListener(FIRE_EXCEPTION_ON_FAILURE);
            log.info("Server bound to host={}, port={}，protocol={}", host, port,protocol);
        }catch (Exception e){
            log.error("An Exception was caught while starting server.error:{}",e.getMessage());
        }
    }


    private ChannelHandler createSslHandler(SocketChannel channel) throws Exception{
        SslContext sslContext = createSslContext();
        SSLEngine engine = sslContext.newEngine(
                channel.alloc(),
                channel.remoteAddress().getHostString(),
                channel.remoteAddress().getPort());
        boolean needsClientAuth = nettyConfig.isNeedsClientAuth();
        engine.setNeedClientAuth(needsClientAuth);
        return new SslHandler(engine);
    }

    private SslContext createSslContext( )throws Exception{
        String keyPwd = nettyConfig.getKeyManagerPassword();
        KeyStore keyStore = getKeyStore();
        SslContextBuilder contextBuilder;

        SslProvider sslProvider = SslProvider.valueOf(nettyConfig.getSslProvider());
        switch (sslProvider){
            case JDK:
                contextBuilder = builderWithJdkProvider(keyStore,keyPwd);
                break;
            case OPENSSL:
            case OPENSSL_REFCNT:
                contextBuilder = builderWithOpenSSLProvider(keyStore,keyPwd);
                break;
                default:
                    return null;
        }

        contextBuilder.sslProvider(sslProvider);
        SslContext sslContext = contextBuilder.build();
        return sslContext;
    }

    private KeyStore getKeyStore() throws Exception{
        final String jksPath = nettyConfig.getJksPath();
        final String keyStorePassword = nettyConfig.getKeyStorePassword();

        String keyType = "jks";
        final KeyStore keyStore = KeyStore.getInstance(keyType);
        try (InputStream jksInputStream = jksDatastore(jksPath)) {
            keyStore.load(jksInputStream, keyStorePassword.toCharArray());
        }
        return keyStore;
    }

    private InputStream jksDatastore(String jksPath) throws Exception {
        URL jksUrl = getClass().getClassLoader().getResource(jksPath);
        if (jksUrl != null) {
            return getClass().getClassLoader().getResourceAsStream(jksPath);
        }
        File jksFile = new File(jksPath);
        if (jksFile.exists()) {
            return new FileInputStream(jksFile);
        }

        throw new FileNotFoundException(
                "The keystore file does not exist. Url = " + jksFile.getAbsolutePath());
    }

    private static SslContextBuilder builderWithJdkProvider(KeyStore ks, String keyPassword)
            throws GeneralSecurityException {
        final KeyManagerFactory kmf = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyPassword.toCharArray());
        return SslContextBuilder.forServer(kmf);
    }

    private static SslContextBuilder builderWithOpenSSLProvider(KeyStore ks, String keyPassword)
            throws GeneralSecurityException {
        for (String alias : Collections.list(ks.aliases())) {
            if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                PrivateKey key = (PrivateKey) ks.getKey(alias, keyPassword.toCharArray());
                Certificate[] chain = ks.getCertificateChain(alias);
                X509Certificate[] certChain = new X509Certificate[chain.length];
                System.arraycopy(chain, 0, certChain, 0, chain.length);
                return SslContextBuilder.forServer(key, certChain);
            }
        }
        throw new KeyManagementException("the SSL key-store does not contain a private key");
    }
}
