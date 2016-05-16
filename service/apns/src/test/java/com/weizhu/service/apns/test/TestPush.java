package com.weizhu.service.apns.test;

import java.security.KeyStore;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;

import com.google.common.io.Resources;
//import com.relayrides.pushy.apns.util.ApnsPayloadBuilder;
import com.weizhu.common.utils.HexUtil;
import com.weizhu.service.apns.net.APNsManager;
import com.weizhu.service.apns.net.FeedbackListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;


public class TestPush {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws Throwable {
		
		// SSLContext sslContext = SSLContextUtil.createDefaultSSLContext(Resources.getResource("com/weizhu/service/apns/test/cert/com.21tb.weizhu_development.p12").openStream(), "123");
		
		final KeyStore keyStore = KeyStore.getInstance("PKCS12");
		final char[] password = "123".toCharArray();

		keyStore.load(Resources.getResource("com/weizhu/service/apns/test/cert/com.21tb.weizhu_development.p12").openStream(), password);
		
		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		
		System.out.println("algorithm" + algorithm);
		if (algorithm == null) {
			algorithm = "SunX509";
		}
		
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(algorithm);
		keyManagerFactory.init(keyStore, password);
		
		SslContext sslContext = SslContextBuilder.forClient().keyManager(keyManagerFactory).build();
		
		NioEventLoopGroup eventLoop = new NioEventLoopGroup();
		
		APNsManager apnsManager = new APNsManager("com.21tb.weizhu", false, sslContext, eventLoop, 3, new FeedbackListener() {

			@Override
			public void handleExpiredToken(int timestamp, byte[] deviceToken) {
				System.out.println("ExpiredToken: " + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(timestamp * 1000L)) + "," + HexUtil.bin2Hex(deviceToken));
			}
			
		});
		
//		final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
//
//		payloadBuilder.setAlertBody("Ring ring, Neo.");
//		// payloadBuilder.setSoundFileName("ring-ring.aiff");
//		payloadBuilder.setBadgeNumber(966);

//		final String payload = payloadBuilder.buildWithDefaultMaximumLength();
//		
//		PushNotification notification = new PushNotification(HexUtil.hex2bin("3a2baf0fbfe41d3eff1dec9824a17821079230494c41673295e6bac82b4d6e7b"), payload.getBytes(Charsets.UTF_8), 0, (byte)10);
//		
//		apnsManager.sendNotification(notification);
//		apnsManager.tryFeedbackConnect();
//		
//		TimeUnit.SECONDS.sleep(10);
//		
//		apnsManager.shutdown();
//		
//		eventLoop.shutdownGracefully().get();
	}

}
