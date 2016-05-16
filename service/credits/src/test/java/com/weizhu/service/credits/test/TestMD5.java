package com.weizhu.service.credits.test;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class TestMD5 {

	public static void main(String args[]) throws NoSuchAlgorithmException {
		String uid = "test1";
		int credits = 100;
		String appKey = "3E9TTxbaiN5cMYRQryvHe5w974xC";
		String appSecret = "ADW564kbSzR93SpCDSE7EEq8QDH";
		long timestamp = System.currentTimeMillis();

		StringBuilder url = new StringBuilder("http://www.duiba.com.cn/autoLogin/autologin?");
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update((appKey+appSecret+credits+timestamp+uid).getBytes());
		byte b[] = md.digest();
		
		int i;
		
		StringBuffer buf = new StringBuffer("");
		for (int offset = 0; offset < b.length; offset ++) {
			i = b[offset];
			if (i < 0)
				i += 256;
			if (i < 16)
				buf.append("0");
			buf.append(Integer.toHexString(i));
		}
		
		System.out.println(buf.toString());
		
		System.out.println();
		
		url.append("uid="+uid).append("&");
		url.append("credits="+credits).append("&");
		url.append("appKey="+appKey).append("&");
		url.append("timestamp="+timestamp).append("&");
		
		System.out.println(url + "sign=" + buf.toString());
		System.out.println(url + "sign=" + Hashing.md5().hashBytes((appKey+appSecret+credits+timestamp+uid).getBytes(Charsets.UTF_8)).toString());
	}
	
}
