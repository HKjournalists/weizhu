package com.weizhu.common.utils;

public class HexUtil {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();
    
    public static String bin2Hex(byte[] data) {
    	StringBuilder sb = new StringBuilder(2 * data.length);
        for (byte b : data) {
          sb.append(DIGITS[(b >> 4) & 0xf]).append(DIGITS[b & 0xf]);
        }
        return sb.toString();
    }
    
	public static byte[] hex2bin(CharSequence hex) {
		if (hex.length() % 2 != 0) {
			throw new IllegalArgumentException("hex length must be odd");
		}

		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < hex.length(); i += 2) {
			int ch1 = decode(hex.charAt(i)) << 4;
			int ch2 = decode(hex.charAt(i + 1));
			bytes[i / 2] = (byte) (ch1 + ch2);
		}

		return bytes;
	}

	private static int decode(char ch) {
		if (ch >= '0' && ch <= '9') {
			return ch - '0';
		}
		if (ch >= 'a' && ch <= 'f') {
			return ch - 'a' + 10;
		}
		throw new IllegalArgumentException("Illegal hexadecimal character: " + ch);
	}
	
}
