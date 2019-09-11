package com.yao.broker.common.logging;

import java.util.UUID;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/9/2 0002
 * @Version V1.0
 **/
public class TraceIdThreadLocal {
    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void set(String uuid){
        threadLocal.set(uuid);
    }

    public static String get(){
        String uuid = threadLocal.get();
        if(uuid == null){
            uuid = set();
        }
        return uuid;
    }

    public static String set(){
        String uuid = UUID.randomUUID().toString().replaceAll("-","");
        set(uuid);
        return uuid;
    }
}
