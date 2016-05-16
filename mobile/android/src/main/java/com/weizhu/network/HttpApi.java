package com.weizhu.network;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.weizhu.proto.ServiceInvoker;
import com.weizhu.proto.WeizhuProtos;

public class HttpApi implements ServiceInvoker {

	private final HttpClient httpClient;
	private final ThreadPoolExecutor threadPool;
	private final WeizhuProtos.Weizhu weizhuVersion;
	private final WeizhuProtos.Android android;
	
	private volatile String apiUrl = "http://127.0.0.1:8090/api/pb";
	private volatile ByteString sessionKey = ByteString.EMPTY;
	private volatile WeizhuProtos.Network.Type networkType = WeizhuProtos.Network.Type.UNKNOWN;
	
	public HttpApi(HttpClient httpClient, int poolSize, int queueSize, WeizhuProtos.Weizhu weizhuVersion, WeizhuProtos.Android android) {
		this.httpClient = httpClient;
		this.threadPool = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, 
				new PriorityBlockingQueue<Runnable>(queueSize));
		this.weizhuVersion = weizhuVersion;
		this.android = android;
	}
	
	public void shutdown() {
		this.threadPool.shutdown();
	}
	
	public HttpClient httpClient() {
		return httpClient;
	}
	
	public String getApiUrl() {
		return this.apiUrl;
	}
	
	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}
	
	public ByteString getSessionKey() {
		return this.sessionKey;
	}
	
	public void setSessionKey(ByteString sessionKey) {
		this.sessionKey = sessionKey == null ? ByteString.EMPTY : sessionKey; 
	}
	
	public WeizhuProtos.Network.Type getNetworkType() {
		return this.networkType;
	}
	
	public void setNetworkType(WeizhuProtos.Network.Type type) {
		this.networkType = type == null ? WeizhuProtos.Network.Type.UNKNOWN : type;
	}
	
	@Override
	public <V extends MessageLite> Future<V> invoke(String serviceName, String functionName, 
			MessageLite request, Parser<V> responseParser, int priorityNum) {
		
		WeizhuProtos.Invoke invoke = WeizhuProtos.Invoke.newBuilder()
				.setServiceName(serviceName)
				.setFunctionName(functionName)
				.setInvokeId(0)
				.build();
		
		FutureTask<V> task = new FutureTask<V>(new HttpApiTask<V>(sessionKey, invoke, request, responseParser), priorityNum);
		
		threadPool.execute(task);
		
		return task;
	}
	
	private class HttpApiTask<V extends MessageLite> implements Callable<V> {

		private final ByteString sessionKey;
		private final WeizhuProtos.Invoke invoke;
		private final MessageLite request;
		private final Parser<V> responseParser;
		
		HttpApiTask(ByteString sessionKey, WeizhuProtos.Invoke invoke, MessageLite request, Parser<V> responseParser) {
			this.sessionKey = sessionKey;
			this.invoke = invoke;
			this.request = request;
			this.responseParser = responseParser;
		}
		
		@Override
		public V call() throws Exception {
			WeizhuProtos.HttpApiRequest apiRequest = WeizhuProtos.HttpApiRequest.newBuilder()
					.setSessionKey(sessionKey)
					.setInvoke(invoke)
					.setNetworkType(HttpApi.this.networkType)
					.setWeizhu(HttpApi.this.weizhuVersion)
					.setAndroid(HttpApi.this.android)
					.setRequestBody(request.toByteString())
					.build();
			
			HttpPost httpPost = new HttpPost(HttpApi.this.apiUrl);
			httpPost.setEntity(new ByteArrayEntity(apiRequest.toByteArray()));
			
			HttpResponse httpResponse;
			try {
				httpResponse = HttpApi.this.httpClient.execute(httpPost);
			} catch (IOException e) {
				throw new Exception("网络连接错误", e);
			}
			
			WeizhuProtos.HttpApiResponse apiResponse = WeizhuProtos.HttpApiResponse
					.parseFrom(httpResponse.getEntity().getContent());
			
			switch(apiResponse.getResult()) {
				case SUCC:
					return responseParser.parseFrom(apiResponse.getResponseBody());
				default:
					throw new HttpApiException(apiResponse.getResult(), apiResponse.getFailText(), 
							apiResponse.getResult() + ":" + apiResponse.getFailText());
			}
		}
		
	}
	
}
