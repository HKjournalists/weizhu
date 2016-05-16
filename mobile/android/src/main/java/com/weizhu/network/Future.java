package com.weizhu.network;

public interface Future<V> extends java.util.concurrent.Future<V> {
	
	void addCallback(Callback<V> callback);
	
}
