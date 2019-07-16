package com.yao.broker.core.netty;

import com.yao.broker.core.config.NettyConfig;
import com.yao.broker.core.netty.channel.EpollServerSocketChannel;
import com.yao.broker.core.netty.channel.NioServerSocketChannel;
import com.yao.broker.core.netty.handler.NettyMqttHandler;
import com.yao.broker.core.netty.handler.TcpIdleTimeoutHandler;
import com.yao.broker.core.netty.pipeline.PipelineInitializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
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
                pipeline.addLast("idleStateHandler",new IdleStateHandler(nettyConfig.getChannelTimeoutSeconds(),0,0));
                pipeline.addAfter("idleStateHandler","idleEventHandler",timeoutHandler);

                // 加解密 (netty自带的MQTT协议解析类)
                pipeline.addLast("decoder", new MqttDecoder());
                pipeline.addLast("encoder", MqttEncoder.INSTANCE);

                // 应用消息处理
                pipeline.addLast("handler",nettyMqttHandler);
            }
        });
    }

    private void startFactory(String host, int port,
             String protocol,final PipelineInitializer initializer){
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
}
