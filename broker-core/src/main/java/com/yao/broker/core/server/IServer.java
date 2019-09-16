package com.yao.broker.core.server;

/**
 * @Description: 网关服务
 * @Author yao.zou
 * @Date 2019/8/26 0026
 * @Version V1.0
 **/
public interface IServer {
    /**
     * 启动服务
     * @Author yao.zou
     */
    void start();

    /**
     * 关闭服务
     * @Author yao.zou
     */
    void stop();

    /**
     * 发送消息
     * @param clientId
     * @param msg
     */
    boolean send(String clientId, String msg);
}
