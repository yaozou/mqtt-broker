package com.yao.broker.core.netty.bean;

import lombok.Data;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2019/8/8 15:44
 */
@Data
public class Token {
    private String username;

    public Token(String username){
        this.username = username;
    }

    @Override
    public String toString() {
        return username;
    }
}
