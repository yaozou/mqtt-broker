package com.yao.broker.core.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 自动刷新
 * @Description: 在一段时间后通道上不执行读取操作，只刷新数据。避免从协议处理器主动刷新
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
public class AutoFlushHandler extends ChannelDuplexHandler {
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private long writerIdleTimeNanos;

    /** 0 - none, 1 - initialized, 2 - destroyed */
    private volatile int state;
    private volatile long lastWriteTime;
    private  volatile ScheduledFuture<?> writerIdleTimeout;

    public AutoFlushHandler(long writerIdleTime, TimeUnit unit){
        if (unit == null){
            unit = TimeUnit.SECONDS;
        }
        writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()){
            initialize(ctx);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()){
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    private void destroy(){
        state = 2;
        if (writerIdleTimeout != null){
            writerIdleTimeout.cancel(false);
            writerIdleTimeout = null;
        }
    }

    private void initialize(ChannelHandlerContext ctx) {
        state = 1;
        EventExecutor executor = ctx.executor();

        lastWriteTime = System.nanoTime();
        executor.schedule(new WriterIdleTimeoutTask(ctx), writerIdleTimeNanos, TimeUnit.NANOSECONDS);

    }

    private final class WriterIdleTimeoutTask implements Runnable {
        private final ChannelHandlerContext ctx;
        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }
            long nextDelay = writerIdleTimeNanos - (System.nanoTime() - lastWriteTime);
            if (nextDelay <= 0) {
                writerIdleTimeout = ctx.executor().schedule(this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
                try {
                    ctx.channel().flush();
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                writerIdleTimeout = ctx.executor().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
