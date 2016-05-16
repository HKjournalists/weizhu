package com.weizhu.service.system.authurl;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.base.Charsets;

public class QiniuAuthUrl implements AuthUrl {

	private final String urlPrefix;
	private final String accessKey;
	private final String secretKey;
	private final int expireTime;
	
	public QiniuAuthUrl(String urlPrefix, String accessKey, String secretKey, int expireTime) {
		this.urlPrefix = urlPrefix;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.expireTime = expireTime;
	}

	@Override
	public String auth(long companyId, String url) {
		if (!url.startsWith(urlPrefix.replace("${company_id}", Long.toString(companyId)))) {
			return null;
		}
		
		String downloadUrl = url + "?e=" + ((int) (System.currentTimeMillis() / 1000L) + expireTime);
		return downloadUrl + "&token=" + this.accessKey + ":" + hmacSHA1(downloadUrl, this.secretKey);
	}
	
	private static String hmacSHA1(String message, String key) {
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(Charsets.UTF_8), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(signingKey);  
			byte[] rawHmac = mac.doFinal(message.getBytes(Charsets.UTF_8));
			return Base64.getUrlEncoder().encodeToString(rawHmac);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void main(String args[]) throws UnsupportedEncodingException {
		long begin = System.currentTimeMillis();
		String str = null;
		for (int i=0; i<1; ++i) {
		
			String accessKey = "2By9Dv_ga4ZwK5x4xSOPGAwMqXYxCgfZk8ESA0Tu";
			String secretKey = "bGxbx3pDz4bQ9kqBTdUFkWs_3gMx_Rdfe_HAjntn";
			
			int e = ((int) (System.currentTimeMillis() / 1000L)) + 3600;
			
			String downloadUrl = "http://dn-weizhu-media.qbox.me/1/discover/video/3c2ce1b2a6f079baf4121a424fd41ff9.mp4?e=" + e;
			String realDownloadUrl = downloadUrl + "&token=" + accessKey + ":" + hmacSHA1(downloadUrl, secretKey);
			
			str = realDownloadUrl;
		}
		System.out.println(System.currentTimeMillis() - begin + "(ms)");
		System.out.println(str);
	}

}