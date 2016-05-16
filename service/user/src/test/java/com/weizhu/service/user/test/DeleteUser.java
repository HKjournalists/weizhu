package com.weizhu.service.user.test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DeleteUser {

	public static void main(String[] args) throws Throwable {

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		System.out.println(df.parse("2015-05-22 00:00:00").getTime());
		System.out.println(df.parse("2015-05-29 00:00:00").getTime());
	}

}
