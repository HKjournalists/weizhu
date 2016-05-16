package com.weizhu.network;

import com.weizhu.proto.WeizhuProtos;

@SuppressWarnings("serial")
public class HttpApiException extends Exception {

	private final WeizhuProtos.HttpApiResponse.Result failResult;
	private final String failText;

	public HttpApiException(WeizhuProtos.HttpApiResponse.Result failResult, String failText) {
		super();
		this.failResult = failResult;
		this.failText = failText;
	}

	public HttpApiException(WeizhuProtos.HttpApiResponse.Result failResult, String failText, String message, Throwable cause) {
		super(message, cause);
		this.failResult = failResult;
		this.failText = failText;
	}

	public HttpApiException(WeizhuProtos.HttpApiResponse.Result failResult, String failText, String message) {
		super(message);
		this.failResult = failResult;
		this.failText = failText;
	}

	public HttpApiException(WeizhuProtos.HttpApiResponse.Result failResult, String failText, Throwable cause) {
		super(cause);
		this.failResult = failResult;
		this.failText = failText;
	}

	public WeizhuProtos.HttpApiResponse.Result failResult() {
		return failResult;
	}

	public String failText() {
		return failText;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HttpApiException [failResult=");
		builder.append(failResult);
		builder.append(", failText=");
		builder.append(failText);
		builder.append("]");
		return builder.toString();
	}

}
