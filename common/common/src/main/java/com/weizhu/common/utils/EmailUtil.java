package com.weizhu.common.utils;

import java.util.regex.Pattern;

public class EmailUtil {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^(\\w)+(\\.\\w+)*@(\\w)+((\\.\\w+)+)$");
	
	public static boolean isValid(String email) {
		return email != null && email.length() < 255 && EMAIL_PATTERN.matcher(email).find();
	}
	
}
