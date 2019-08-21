package com.yao.broker.core.utils;

import com.yao.broker.core.config.MsgConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;


/**
 * @Description: 字节工具
 * @author: yaozou
 * @Date: 2018/10/30 10:58
 */
public class ByteUtil {

  /**
   * 16进制数字字符集
   */
  private static String hexString = "0123456789ABCDEF";

  /**
   * 将字符串编码成16进制数字,适用于所有字符（包括中文）
   */
  public static String stringToHexStr(String str) {
    // 根据默认编码获取字节数组
    byte[] bytes = str.getBytes();
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    // 将字节数组中每个字节拆解成2位16进制整数
    for (int i = 0; i < bytes.length; i++) {
      sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
      sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
    }
    return sb.toString();
  }

  /**
   * 将16进制数字解码成字符串,适用于所有字符（包括中文）
   */
  public static String hexToString(String bytes) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length() / 2);
    // 将每2位16进制整数组装成一个字节
    int i = 0;
    while (i < bytes.length()) {
      baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString.indexOf(bytes.charAt(i + 1))));
      i = +2;
    }
    return new String(baos.toByteArray());
  }

  /**
   * 将byte[]转为各种进制的字符串
   *
   * @param bytes byte[]
   * @param radix 基数可以转换进制的范围，从Character.MIN_RADIX到Character.MAX_RADIX，
   *              超出范围后变为10进制
   * @return 转换后的字符串
   */
  public static String binary(byte[] bytes, int radix) {
    /* 这里的1代表正数 */
    return new BigInteger(1, bytes).toString(radix);
  }

  /**
   * Convert byte[] to hex
   * string.这里我们可以将byte转换成int，然后利用Integer.toHexString(int)来转换成16进制字符串。
   *
   * @return hex string
   */
  public static String bytesToHexString(byte[] bytes) {
    StringBuilder stringBuilder = new StringBuilder("");
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

  /**
   * Convert hex string to byte[]
   *
   * @param hexString the hex string
   * @return byte[]
   */
  public static byte[] hexStringToBytes(String hexString) {
    if (hexString == null || "".equals(hexString)) {
      return null;
    }

    hexString = hexString.toUpperCase();
    int length = hexString.length() / 2;
    char[] hexChars = hexString.toCharArray();
    byte[] d = new byte[length];
    for (int i = 0; i < length; i++) {
      int pos = i * 2;
      d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
    }

    return d;
  }

  /**
   * Convert char to byte
   *
   * @param c char
   * @return byte
   */
  private static byte charToByte(char c) {
    return (byte) hexString.indexOf(c);
  }

  /**
   * word占2个字节，因此有4位16进制组成
   */
  public static String wordToHexStr(int num) {

    String hexStr = Integer.toHexString(num);

    return fillToTotalBit(MsgConstants.WORD_TO_HEX_BIT, hexStr);
  }

  /**
   * word占2个字节，因此有16位二进制组成
   */
  public static String wordByHexStrToBinary(String hexStr) {

    byte[] tempByte = ByteUtil.hexStringToBytes(hexStr);
    String tempBinary = ByteUtil.binary(tempByte, 2);

    return fillToTotalBit(MsgConstants.WORD_TO_BINARY, tempBinary);
  }

  /**
   * word占2个字节，因此有16位二进制组成
   */
  public static String wordByBytesToBinary(byte[] bytes) {

    String tempBinary = ByteUtil.binary(bytes, 2);

    return fillToTotalBit(MsgConstants.WORD_TO_BINARY, tempBinary);
  }

  /**
   * 将高位补0，让字符串填充到指定位数
   *
   * @param totalBit 总共占的位数
   */
  public static String fillToTotalBit(int totalBit, String str) {

    StringBuilder builder = new StringBuilder();
    int dValue = totalBit - str.length();

    for (int i = 0; i < dValue; i++) {
      builder.append('0');
    }
    builder.append(str);

    return builder.toString();
  }

  /**
   * 将十六进制的数转换成double型
   */
  public static double convertHexStrToDouble(String hexStr) {

    byte[] hexByte = hexStringToBytes(hexStr);
    String str = binary(hexByte, 10);

    return Double.valueOf(str);
  }

  /**
   * 将Byte转换成double型
   */
  public static double convertBytesToDouble(byte[] bytes) {
    String str = binary(bytes, 10);
    return Double.valueOf(str);
  }

  /**
   * 将十六进制的数转换成int型
   */
  public static int convertHexStrToInt(String hexStr) {

    byte[] hexByte = hexStringToBytes(hexStr);
    String str = binary(hexByte, 10);

    return Integer.valueOf(str);
  }

  /**
   * 将字节转换成int型
   */
  public static int bytesToInt(byte[] bytes) {

    String str = binary(bytes, 10);

    return Integer.valueOf(str);
  }

  /**
   * 将字节转换成long型
   */
  public static long bytesToLong(byte[] bytes) {

    String str = binary(bytes, 10);

    return Long.valueOf(str);
  }

  /**
   * short整型转为byte类型的数组
   */
  public static byte[] unsignedIntTwoByte(int length) {
    byte[] targets = new byte[2];
    for (int i = 0; i < targets.length; i++) {
      int offset = (targets.length - 1 - i) * 8;
      targets[i] = (byte) ((length >>> offset) & 0xff);
    }
    return targets;
  }

  /**
   * 无符号整型转化为字节数组
   */
  public static byte[] unsignedIntFourByte(long length) {
    byte[] targets = new byte[4];
    for (int i = 0; i < targets.length; i++) {
      int offset = (targets.length - 1 - i) * 8;
      targets[i] = (byte) ((length >>> offset) & 0xff);
    }
    return targets;
  }

  /**
   * 无符号byte类型转为数组
   */
  public static byte[] unsignedCharTwoByte(char res) {
    byte[] bytelen = new byte[1];
    bytelen[0] = (byte) res;

    return bytelen;
  }

  public static byte[] unsignedLongEightByte(long length) {
    byte[] targets = new byte[8];
    for (int i = 0; i < targets.length; i++) {
      int offset = (targets.length - 1 - i) * 8;
      targets[i] = (byte) ((length >>> offset) & 0xff);
    }
    return targets;
  }

  public static byte[] unsignedFloatFourBytes(float d) {
    /**把float转换为byte[]*/
    int fbit = Float.floatToIntBits(d);

    byte[] b = new byte[4];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) (fbit >> (24 - i * 8));
    }

    /** 翻转数组 */
    int len = b.length;
    /** 建立一个与源数组元素类型相同的数组 */
    byte[] dest = new byte[len];
    /** 为了防止修改源数组，将源数组拷贝一份副本 */
    System.arraycopy(b, 0, dest, 0, len);
    byte temp;
    /** 将顺位第i个与倒数第i个交换 */
    int n = 2;
    for (int i = 0; i < len / n; ++i) {
      temp = dest[i];
      dest[i] = dest[len - i - 1];
      dest[len - i - 1] = temp;
    }

    return dest;
  }

  public static Float fourBytesFloat(byte[] b) {
    int l, index = 0;
    l = b[index + 0];
    l &= 0xff;
    l |= ((long) b[index + 1] << 8);
    l &= 0xffff;
    l |= ((long) b[index + 2] << 16);
    l &= 0xffffff;
    l |= ((long) b[index + 3] << 24);
    return Float.intBitsToFloat(l);
  }

  public static byte[] unsignedFloatTwoBytes(float d) {
    /**把float转换为byte[]*/
    int fbit = Float.floatToIntBits(d);
    byte[] b = new byte[2];
    for (int i = 0; i < b.length; i++) {
      b[i] = (byte) (fbit >> (24 - i * 8));
    }
    return b;
  }

  public static Float twoBytesFloat(byte[] b) {
    int l, index = 0;
    l = b[index + 0];
    l &= 0xff;
    l |= ((long) b[index + 1] << 8);
    return Float.intBitsToFloat(l);
  }

  public static String convertBytesToFloatStr(byte[] bytes) {
    return new Float(fourBytesFloat(bytes)).toString();
  }

  /**
   * 读取1个字节转为无符号整型
   */
  public static int byteToUnsignedInt(byte data) {
    return data & 0xff;
  }

  /**
   * 读取2个字节转为无符号整型
   */
  public static int twoByteToInt(byte[] res) {
    DataInputStream dataInputStream = new DataInputStream(
      new ByteArrayInputStream(res));
    int a = 0;
    try {
      a = dataInputStream.readUnsignedShort();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return a;
  }

  /**
   * 读取4个字节转为无符号长整型
   */
  public static long fourByteToLong(byte[] res) {
    int firstByte = 0;
    int secondByte = 0;
    int thirdByte = 0;
    int fourthByte = 0;
    int index = 0;
    firstByte = (0x000000FF & ((int) res[index]));
    secondByte = (0x000000FF & ((int) res[index + 1]));
    thirdByte = (0x000000FF & ((int) res[index + 2]));
    fourthByte = (0x000000FF & ((int) res[index + 3]));
    index = index + 4;
    return ((long) (firstByte << 24 | secondByte << 16 | thirdByte << 8 | fourthByte)) & 0xFFFFFFFFL;
  }

}
