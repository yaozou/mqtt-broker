package com.yao.broker.core;

import com.yao.broker.core.config.NettyConfig;
import com.yao.broker.core.handler.NettyMqttHandler;
import com.yao.broker.core.handler.SslOperatorHandler;
import com.yao.broker.core.handler.TcpIdleTimeoutHandler;
import com.yao.broker.core.pipeline.PipelineInitializer;
import com.yao.broker.core.server.IMqttMsgServer;
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
    @Autowired
    private IMqttMsgServer mqttMsgServer;


    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private Class<? extends ServerSocketChannel> channel;

    @Override
    public void start() {
        // 用于监听和接受客户端连接的Reactor线程
        bossGroup = new NioEventLoopGroup();
        // 处理I/O读写的Reactor线程组
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


    @Override
    public boolean send(String clientId, String msg) {
        return mqttMsgServer.sendMsg2Client(clientId,msg);
    }


    private void startSslTCPTransport(){
        String host = nettyConfig.getHost();
        int    port = nettyConfig.getPort();

        final TcpIdleTimeoutHandler timeoutHandler = new TcpIdleTimeoutHandler();
        startFactory(host, port, channel -> {
            ChannelPipeline pipeline = channel.pipeline();

            if (nettyConfig.isNeedBlokerSsl()){
                // ssl连接
                pipeline.addLast("ssl",createSSLHandler(channel));
            }


            // 设置心跳机制
            pipeline.addFirst("idleStateHandler",new IdleStateHandler(nettyConfig.getChannelTimeoutSeconds(),0,0));
            pipeline.addAfter("idleStateHandler","idleEventHandler",timeoutHandler);

            // 加解密 (netty自带的MQTT协议解析类)
            pipeline.addLast("decoder", new MqttDecoder());
            pipeline.addLast("encoder", MqttEncoder.INSTANCE);

            // 应用消息处理
            pipeline.addLast("handler",nettyMqttHandler);
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
                // 两个队列总和的最大值 默认为100
                .option(ChannelOption.SO_BACKLOG,nettyConfig.getSoBacklog())
                // 是否允许重复使用本地地址和端口
                .option(ChannelOption.SO_REUSEADDR,nettyConfig.isSoReuseaddr())
                // 设置是否启用Nagle算法：用将小的碎片数据连接成更大的报文来提高发送效率。如果需要发送一些较小的报文，则需要禁用该算法
                /**
                 * Nagle算法是将小的数据包组装为更大的帧然后进行发送，而不是输入一次发送一次,因此在数据包不足的时候会等待其他数据的到了，组装成大的数据包进行发送，虽然该方式有效提高网络的有效
                 *
                 * 负载，但是却造成了延时，而该参数的作用就是禁止使用Nagle算法，使用于小数据即时传输，于TCP_NODELAY相对应的是TCP_CORK，该选项是需要等到发送的数据量最大的时候，一次性发送
                 *
                 * 数据，适用于文件传输。
                 */
                .childOption(ChannelOption.TCP_NODELAY,nettyConfig.isTcpNodelay())
                // 设置TCP层keepalive
                .childOption(ChannelOption.SO_KEEPALIVE,nettyConfig.isSoKeepalive())
                // 重用缓冲区 ByteBuf分配器  大多池化，堆外
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
