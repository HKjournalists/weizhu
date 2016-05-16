package com.weizhu.service.apns.test;

//import com.relayrides.pushy.apns.util.ApnsPayloadBuilder;

public class TestPushy {

//	public static void main(String args[]) throws Exception {
//		final PushManager<SimpleApnsPushNotification> pushManager =
//			    new PushManager<SimpleApnsPushNotification>(
//			    	ApnsEnvironment.getSandboxEnvironment(),
//			        SSLContextUtil.createDefaultSSLContext(Resources.getResource("com/weizhu/service/apns/cert/com.21tb.weizhu_development.p12").openStream(), "123"),
//			        null, // Optional: custom event loop group
//			        null, // Optional: custom ExecutorService for calling listeners
//			        null, // Optional: custom BlockingQueue implementation
//			        new PushManagerConfiguration(),
//			        "ExamplePushManager");
//
//		pushManager.start();
//		
//		final byte[] token = TokenUtil.tokenStringToByteArray(
//			    "3a2baf0fbfe41d3eff1dec9824a17821079230494c41673295e6bac82b4d6e7b");
//
//		final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
//
//		payloadBuilder.setAlertBody("Ring ring, Neo.");
//		// payloadBuilder.setSoundFileName("ring-ring.aiff");
//		payloadBuilder.setBadgeNumber(222);
//
//		final String payload = payloadBuilder.buildWithDefaultMaximumLength();
//
//		pushManager.getQueue().put(new SimpleApnsPushNotification(token, payload));
//		
//		TimeUnit.SECONDS.sleep(20);
//		
//		pushManager.shutdown();
//	}
	
	public static void main(String args[]) throws Exception {
//		final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
//
//		payloadBuilder.setAlertBody("Ring ring, Neo.");
//		// payloadBuilder.setSoundFileName("ring-ring.aiff");
//		payloadBuilder.setBadgeNumber(966);
//
//		final String payload = payloadBuilder.buildWithDefaultMaximumLength();
//		
//		System.out.println(payload);
	}
	
}
