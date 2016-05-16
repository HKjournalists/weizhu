package com.weizhu.service.apns.net;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.common.base.Charsets;
import com.weizhu.common.utils.HexUtil;

public class PushNotification {

	private final byte[] deviceToken; // 32byte
	private final byte[] payload; // variable length, less than or equal to 2 kilobytes  (2048)
	private final int expirationTimestamp;
	private final byte priority; // 10 - immediately , 5 - conserves power
	private final int retry;
	
	public PushNotification(byte[] deviceToken, byte[] payload, int expirationTimestamp, byte priority) {
		this.deviceToken = deviceToken;
		this.payload = payload;
		this.expirationTimestamp = expirationTimestamp;
		this.priority = priority;
		this.retry = 0;
	}
	
	public PushNotification(PushNotification notification, int retry) {
		this.deviceToken = notification.deviceToken();
		this.payload = notification.payload();
		this.expirationTimestamp = notification.expirationTimestamp();
		this.priority = notification.priority();
		this.retry = retry;
	}

	public byte[] deviceToken() {
		return deviceToken;
	}

	public byte[] payload() {
		return payload;
	}

	public int expirationTimestamp() {
		return expirationTimestamp;
	}

	public byte priority() {
		return priority;
	}
	
	public int retry() {
		return retry;
	}
	
	public boolean checkValid() {
		if (deviceToken == null || deviceToken.length != 32) {
			return false;
		}
		if (payload == null || payload.length > 2048) {
			return false;
		}
		if (priority != 10 && priority != 5) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PushNotification [deviceToken=");
		builder.append(HexUtil.bin2Hex(deviceToken));
		builder.append(", payload=");
		builder.append(new String(payload, Charsets.UTF_8));
		builder.append(", expirationTimestamp=");
		builder.append(expirationTimestamp <= 0 ? expirationTimestamp : new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(expirationTimestamp * 1000L)));
		builder.append(", priority=");
		builder.append(priority);
		builder.append(", retry=");
		builder.append(retry);
		builder.append("]");
		return builder.toString();
	}
	
}
