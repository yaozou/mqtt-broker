package com.yao.broker.core.config;

/**
 * @Description:
 * @author: yaozou
 * @Date: 2018/10/30 11:02
 */
public class MsgConstants {

  public static int version = 1;

  /**
   * WORD转换成16进制，由4位组成
   */
  public static int WORD_TO_HEX_BIT = 4;

  /**
   * WORD转换成2进制，由16位组成
   * WORD占2个无符号字节
   */
  public final static int WORD_TO_BINARY = 16;
  public final static int WORD_TO_BYTE = 2;

  /**
   * DWORD占4个无符号字节
   * DWORD转换成2进制，由32位组成
   */
  public final static int DWORD_TO_BYTE = 4;
  public final static int DWORD_TO_BINARY = 32;


  public final static int BYTE_TO_BYTE = 1;
  public final static int BYTE_TO_BINARY = 8;

  /**
   * 里程数据单元长度
   */
  public final static int LOCATION_LENGTH = 24;

  /**
   * 终端SN号长度
   */
  public final static int CLIENT_LENGTH = 10;

  /**
   * 超时时间
   */
  public final static byte OUT_TIME = 10;

}
