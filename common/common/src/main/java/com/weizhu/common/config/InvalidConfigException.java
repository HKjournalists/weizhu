package com.weizhu.common.config;

import javax.annotation.Nullable;

@SuppressWarnings("serial")
public class InvalidConfigException extends RuntimeException {

	private final String key;
	private final String value;
	
	public InvalidConfigException(String key, @Nullable String value, String msg) {
		super("'" + key + "=" + value + "': " + msg);
		this.key = key;
		this.value = value;
	}
	
	public String key() {
		return key;
	}

	public String value() {
		return value;
	}
}
