package com.weizhu.webapp.mobile;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MobileServletUtil {
	public static String getDate(int date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateFormate = sdf.format(new Date(date * 1000L));
		return dateFormate;
	}
}
