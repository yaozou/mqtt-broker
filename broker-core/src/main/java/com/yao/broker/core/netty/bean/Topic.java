package com.yao.broker.core.netty.bean;

import java.util.List;

import lombok.Data;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/8 11:17
 */
@Data
public class Topic {
    private String topic;
    private List<Token> tokens;

    public Topic(String topic){
        this.topic = topic;
    }
}
