package com.weizhu.network;

public interface Callback<V> {

	void onSucc(V result);
	void onFail(Throwable th);
	void onCancel();
	
}
