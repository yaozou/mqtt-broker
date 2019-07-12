package com.yao.broker.core.netty;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/7/12 18:08
 */
public interface BrokerAcceptor {

    void connection();

    void close();
}
