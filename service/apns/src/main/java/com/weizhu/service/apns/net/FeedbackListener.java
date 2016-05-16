package com.weizhu.service.apns.net;

public interface FeedbackListener {
	
	void handleExpiredToken(int timestamp, byte[] deviceToken);
	
}
