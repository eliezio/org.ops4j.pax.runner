package org.ops4j.pax.runner.commons;

import java.io.UnsupportedEncodingException;

public class StringUtils {

    public static String newStringUtf8(byte[] bytes) {
        return newStringUtf8(bytes, 0, bytes.length);
    }

    public static String newStringUtf8(byte[] bytes, int offset, int n) {
        try {
            return new String(bytes, offset, n, StandardCharEncodings.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] getBytesUtf8(String string) {
        try {
            return string.getBytes(StandardCharEncodings.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
