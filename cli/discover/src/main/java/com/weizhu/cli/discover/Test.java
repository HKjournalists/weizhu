package com.weizhu.cli.discover;

import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.google.common.base.Charsets;

public class Test {

	public static void main(String[] args) throws Exception {
		if (System.getProperty("logback.configurationFile") == null) {
			System.setProperty("logback.configurationFile", "com/weizhu/cli/discover/logback.xml");
		}
		
		KeyStore tks = KeyStore.getInstance(KeyStore.getDefaultType());
		tks.load(new FileInputStream("./cert/weizhu_ca.jks"), "weizhu2015".toCharArray());
		
		KeyStore cks = KeyStore.getInstance("PKCS12");
		cks.load(new FileInputStream("./cert/francislin.p12"), "weizhu2015".toCharArray());
		
		SSLContext sslcontext = SSLContexts.custom()
				.loadTrustMaterial(tks, new TrustSelfSignedStrategy())
				.loadKeyMaterial(cks, "weizhu2015".toCharArray())
				.build();
		
		SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslcontext);
		
//		System.setProperty("javax.net.debug", "ssl");
//		System.setProperty("javax.net.ssl.trustStoreType", "jks");
//		System.setProperty("javax.net.ssl.trustStore", "./cert/weizhu_ca.jks");
//		System.setProperty("javax.net.ssl.trustStorePassword", "weizhu2015");
//		System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");
//		System.setProperty("javax.net.ssl.keyStore", "./cert/francislin.p12");
//		System.setProperty("javax.net.ssl.keyStorePassword", "weizhu2015");
//		
		CloseableHttpClient httpClient = HttpClients.custom()
				.setSSLSocketFactory(csf)
				.build();
		
		try {
			HttpGet httpGet = new HttpGet("https://boss.wehelpu.cn:8443/boss");
			
			CloseableHttpResponse response = httpClient.execute(httpGet);
			try {
				HttpEntity entity = response.getEntity();
				String content = EntityUtils.toString(entity, Charsets.UTF_8);
				
				System.out.println("StatusLine: " + response.getStatusLine());
				System.out.println("StatusLine: " + content);
			} finally {
				response.close();
			}
		} finally {
			httpClient.close();
		}
	}

}
