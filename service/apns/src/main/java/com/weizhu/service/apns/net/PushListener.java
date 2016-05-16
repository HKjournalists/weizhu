package com.weizhu.service.apns.net;

public interface PushListener {

	void handleNotificationResent(PushNotification notification);
	
}
