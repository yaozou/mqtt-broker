package com.yao.broker.core.bean;

import lombok.Builder;
import lombok.Data;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/8/27 0027
 * @Version V1.0
 **/
@Data
@Builder
public class WillMessage {
   private boolean willRetain;
   private String willTopic;
   private String willMessage;
   private int     willQos;

   public WillMessage(boolean willRetain,String willTopic,String willMessage,int  willQos){
       this.willRetain = willRetain;
       this.willTopic  = willTopic;
       this.willMessage = willMessage;
       this.willQos     = willQos;
   }
}
