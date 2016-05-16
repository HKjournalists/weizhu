package com.weizhu.network;

import com.weizhu.proto.WeizhuProtos;

@SuppressWarnings("serial")
public class SocketApiException extends Exception {

	private final WeizhuProtos.SocketApiResponse.Result failResult;
	private final String failText;

	public SocketApiException(WeizhuProtos.SocketApiResponse.Result failResult, String failText) {
		super();
		this.failResult = failResult;
		this.failText = failText;
	}

	public SocketApiException(WeizhuProtos.SocketApiResponse.Result failResult, String failText, String message, Throwable cause) {
		super(message, cause);
		this.failResult = failResult;
		this.failText = failText;
	}

	public SocketApiException(WeizhuProtos.SocketApiResponse.Result failResult, String failText, String message) {
		super(message);
		this.failResult = failResult;
		this.failText = failText;
	}

	public SocketApiException(WeizhuProtos.SocketApiResponse.Result failResult, String failText, Throwable cause) {
		super(cause);
		this.failResult = failResult;
		this.failText = failText;
	}

	public WeizhuProtos.SocketApiResponse.Result failResult() {
		return failResult;
	}

	public String failText() {
		return failText;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SocketApiException [failResult=");
		builder.append(failResult);
		builder.append(", failText=");
		builder.append(failText);
		builder.append("]");
		return builder.toString();
	}

}
