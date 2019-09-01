package com.yao.broker.core;

import com.yao.broker.core.config.NettyConfig;
import com.yao.broker.core.handler.NettyMqttHandler;
import com.yao.broker.core.handler.SslOperatorHandler;
import com.yao.broker.core.handler.TcpIdleTimeoutHandler;
import com.yao.broker.core.pipeline.PipelineInitializer;
import com.yao.broker.core.server.IServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
@Service
@Slf4j
public class NettyServer implements IServer {

    @Autowired
    private NettyConfig nettyConfig;
    @Autowired
    private SslOperatorHandler sslOperatorHandler;
    @Autowired
    private NettyMqttHandler nettyMqttHandler;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Class<? extends ServerSocketChannel> channel;

    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        channel = NioServerSocketChannel.class;
        startSslTCPTransport();
    }

    @Override
    public void stop() {
        if (bossGroup != null && workerGroup != null){
            try {
                workerGroup.shutdownGracefully().sync();
                bossGroup.shutdownGracefully().sync();
            }catch (Exception e){
                log.error("An Exception was caught while stopping server.error:{}",e.getMessage());
            }
        }
    }

    private void startSslTCPTransport(){
        String host = nettyConfig.getHost();
        int    port = nettyConfig.getPort();

        final TcpIdleTimeoutHandler timeoutHandler = new TcpIdleTimeoutHandler();
        startFactory(host, port, new PipelineInitializer() {
            @Override
            public void init(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();

                // ssl连接
                pipeline.addLast("ssl",createSSLHandler(channel));

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

    private void startFactory(String host,int port,final PipelineInitializer initializer){
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup,workerGroup)
                .channel(channel)
                // 责任链
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        initializer.init(ch);
                    }
                })
                .option(ChannelOption.SO_BACKLOG,nettyConfig.getSoBacklog())
                .option(ChannelOption.SO_REUSEADDR,nettyConfig.isSoReuseaddr())
                .childOption(ChannelOption.TCP_NODELAY,nettyConfig.isTcpNodelay())
                .childOption(ChannelOption.SO_KEEPALIVE,nettyConfig.isSoKeepalive())
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        ChannelFuture future = bootstrap.bind(host,port);
        try {
            future.sync().addListener(FIRE_EXCEPTION_ON_FAILURE);
            log.info("Server bound to host={}, port={}，protocol={}", host, port,"SSL TCP MQTT");
        } catch (InterruptedException e) {
            log.error("An Exception was caught while starting server.error:{}",e.getMessage());
        }
    }

    private ChannelHandler createSSLHandler(SocketChannel channel) throws Exception {
        return sslOperatorHandler.createSsl(channel);
    }

}
