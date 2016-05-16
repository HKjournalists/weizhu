package com.weizhu.common.utils;

public class PasswordUtil {
	
	public static boolean isValid(String password) {
		if (password == null || password.length() < 6 || password.length() > 22) {
			return false;
		}
		
		boolean hasNum = false;
		boolean hasChar = false;
		for (int i=0; i<password.length(); ++i) {
			char c = password.charAt(i);
			if (c >= '0' && c <= '9') {
				hasNum = true;
			} else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
				hasChar = true;
			}
		}

		return hasNum && hasChar;
	}
	
	public static String tips() {
		return "密码必须包含数字与字母，长度6~22";
	}
	
	public static void main(String[] args) {
		System.out.println(isValid("0a~!@#$%^&*."));
	}
	
}
