package com.weizhu.common.jedis;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;
import com.google.protobuf.Message;

public final class JedisUtil {

	private JedisUtil() {
	}
	
	public static final byte[] EMPTY_BYTES = new byte[0];
	
	private static final byte[] COLON_BYTES = ":".getBytes(Charsets.UTF_8);
	
	public static byte[] makeKey(byte[] domain, int key) {
		return Bytes.concat(domain, Integer.toString(key).getBytes(Charsets.UTF_8));
	}
	
	public static byte[] makeKey(byte[] domain, long key) {
		return Bytes.concat(domain, Long.toString(key).getBytes(Charsets.UTF_8));
	}
	
	public static byte[] makeKey(byte[] domain, String key) {
		return Bytes.concat(domain, key.getBytes(Charsets.UTF_8));
	}
	
	public static byte[] makeKey(byte[] domain, long key1, long key2) {
		return Bytes.concat(domain, Long.toString(key1).getBytes(Charsets.UTF_8), COLON_BYTES, Long.toString(key2).getBytes(Charsets.UTF_8));
	}
	
	public static byte[] makeValue(int value) {
		return Integer.toString(value).getBytes(Charsets.UTF_8);
	}
	
	public static byte[] makeValue(long value) {
		return Long.toString(value).getBytes(Charsets.UTF_8);
	}
	
	public static <T extends Message> byte[] makeValue(T value) {
		if (value == null) {
			return EMPTY_BYTES;
		} else {
			return value.toByteArray();
		}
	}
	
}
