package com.yao.broker.core.utils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Description: 消息id
 * @Author yao.zou
 * @Date 2019/8/28 0028
 * @Version V1.0
 **/
public class MessageIdUtils {
    private static AtomicInteger index = new AtomicInteger(1);
    /**
     * 获取messageId
     * @return id
     */
    public  static int  messageId(){
        for (;;) {
            int current = index.get();
            int next = (current >= Integer.MAX_VALUE ? 0: current + 1);
            if (index.compareAndSet(current, next)) {
                return current;
            }
        }
    }
}
