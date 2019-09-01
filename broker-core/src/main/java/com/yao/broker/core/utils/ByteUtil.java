package com.yao.broker.core.utils;


/**
 * @Description: 字节工具
 * @author: yaozou
 * @Date: 2018/10/30 10:58
 */
public class ByteUtil {
  /**
   * Convert byte[] to hex
   * string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
   *
   * @return hex string
   */
  public static String bytesToHexString(byte[] bytes) {
    StringBuilder stringBuilder = new StringBuilder();
    if (bytes == null || bytes.length <= 0) {
      return null;
    }
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      String hv = Integer.toHexString(v);
      if (hv.length() < 2) {
        stringBuilder.append(0);
      }
      stringBuilder.append(hv);
    }
    return stringBuilder.toString();
  }

}
