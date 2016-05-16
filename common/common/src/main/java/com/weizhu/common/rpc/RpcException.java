package com.weizhu.common.rpc;

@SuppressWarnings("serial")
public class RpcException extends Exception {

	public RpcException() {
		super();
	}

	public RpcException(String message, Throwable cause) {
		super(message, cause);
	}

	public RpcException(String message) {
		super(message);
	}

	public RpcException(Throwable cause) {
		super(cause);
	}
	
}
