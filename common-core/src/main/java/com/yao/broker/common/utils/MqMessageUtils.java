package com.yao.broker.common.utils;

import com.alibaba.fastjson.JSONObject;

/**
 * @Description: TODO
 * @Author yao.zou
 * @Date 2019/9/9 0009
 * @Version V1.0
 **/
public class MqMessageUtils {
    private static final String REQUEST_ID = "request-id";
    private static final String BODY = "body";
    private static final String CLIENT_ID = "client-id";


    public static String packageMsg(String requestId,String msg){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(REQUEST_ID,requestId);
        jsonObject.put(BODY,msg);
        return jsonObject.toJSONString();
    }

    public static String packageMsg(String requestId,String clientId,String msg){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(REQUEST_ID,requestId);
        jsonObject.put(CLIENT_ID,clientId);
        jsonObject.put(BODY,msg);
        return jsonObject.toJSONString();
    }

    public static String requestId(JSONObject jsonObject){
        return jsonObject.getString(REQUEST_ID);
    }

    public static String clientId(JSONObject jsonObject){
        return jsonObject.getString(CLIENT_ID);
    }

    public static String msg(JSONObject jsonObject){
        return jsonObject.getString(BODY);
    }

    public static JSONObject str2Json(String msg){
        return JSONObject.parseObject(msg);
    }
}
