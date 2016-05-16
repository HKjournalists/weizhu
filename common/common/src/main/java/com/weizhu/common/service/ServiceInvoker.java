package com.weizhu.common.service;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public interface ServiceInvoker {

	String serviceName();
	
	/**
	 * return future throw {@link com.weizhu.common.service.exception.ServiceException}
	 */
	ListenableFuture<ByteString> invoke(String functionName, Message head, ByteString requestBody);
	
}
