package top.nino.mydb.backend.utils;

import java.nio.ByteBuffer;

/**
 * 转换工具类
 * @Author：zengzhj
 * @Date：2023/11/15 17:06
 */
public class Parser {

    public static long parseLong(byte[] buffer) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, 8);
        return byteBuffer.getLong();
    }

    public static byte[] long2Byte(long value) {
        return ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(value).array();
    }
}
