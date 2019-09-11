package com.yao.broker.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/9/2 0002
 * @Version V1.0
 **/
public class TraceIdConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        return TraceIdThreadLocal.get();
    }
}
