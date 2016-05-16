package com.weizhu.cli.utils;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Charsets;
import com.google.common.net.HostAndPort;
import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.weizhu.common.rpc.RpcClient;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.BossProtos;
import com.weizhu.proto.ResponseType;
import com.weizhu.proto.WeizhuProtos;

import io.netty.channel.nio.NioEventLoopGroup;

public class InvokeService implements Callable<Integer> {

	private final InputStream in;
	private final PrintStream out;
	private final PrintStream err;
	
	private final String serverAddr;
	private final String serviceName;
	private final String functionName;
	
	private final String headType;
	private final String headDataJson;
	
	public InvokeService(
			InputStream in, PrintStream out, PrintStream err, 
			String serverAddr, String serviceName, String functionName, 
			String headType, String headDataJson
			) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.serverAddr = serverAddr;
		this.serviceName = serviceName;
		this.functionName = functionName;
		this.headType = headType;
		this.headDataJson = headDataJson;
	}
	
	@Override
	public Integer call() throws Exception {
		Message.Builder headBuilder;
		if ("SystemHead".equals(this.headType)) {
			headBuilder = WeizhuProtos.SystemHead.newBuilder();
		} else if ("RequestHead".equals(this.headType)) {
			headBuilder = WeizhuProtos.RequestHead.newBuilder();
		} else if ("AnonymousHead".equals(this.headType)) {
			headBuilder = WeizhuProtos.AnonymousHead.newBuilder();
		} else if ("AdminHead".equals(this.headType)) {
			headBuilder = AdminProtos.AdminHead.newBuilder();
		} else if ("AdminAnonymousHead".equals(this.headType)) {
			headBuilder = AdminProtos.AdminAnonymousHead.newBuilder();
		} else if ("BossHead".equals(this.headType)) {
			headBuilder = BossProtos.BossHead.newBuilder();
		} else if ("BossAnonymousHead".equals(this.headType)) {
			headBuilder = BossProtos.BossAnonymousHead.newBuilder();
		} else {
			this.err.println("invalid head type : " + this.headType);
			return 1;
		}
		
		JsonUtil.PROTOBUF_JSON_FORMAT.merge(this.headDataJson, ExtensionRegistry.getEmptyRegistry(), headBuilder);
		final Message head = headBuilder.build();
		
		Class<?> serviceApi = Class.forName("com.weizhu.proto." + this.serviceName);
		Method funcMethod = null;
		for (Method method : serviceApi.getMethods()) {
			if (this.functionName.equals(method.getName()) 
					&& method.getParameterTypes().length == 2 
					&& method.getParameterTypes()[0] == head.getClass()
					&& Message.class.isAssignableFrom(method.getParameterTypes()[1])) {
				funcMethod = method;
				break;
			}
		}
		
		if (funcMethod == null) {
			this.err.println("invalid function : " + this.serviceName + "." + this.functionName + "(" + this.headType + ", XXXRequest)" );
			return 1;
		}
		
		final Message requestDefaultInstance = (Message) funcMethod.getParameterTypes()[1].getMethod("getDefaultInstance").invoke(null);
		final Message responseDefaultInstance = (Message) funcMethod.getAnnotation(ResponseType.class).value().getMethod("getDefaultInstance").invoke(null);;
		
		Message.Builder requestBuilder = requestDefaultInstance.newBuilderForType();
		JsonUtil.PROTOBUF_JSON_FORMAT.merge(this.in, requestBuilder);
		final ByteString requestByteString = requestBuilder.build().toByteString();
		
		final ExecutorService serviceExcutor = Executors.newSingleThreadExecutor();
		final NioEventLoopGroup eventLoop = new NioEventLoopGroup(1);
		
		HostAndPort hp = HostAndPort.fromString(this.serverAddr);
		
		final RpcClient rpcClient = new RpcClient(new InetSocketAddress(hp.getHostText(), hp.getPort()), eventLoop);

		rpcClient.connect().get();
		final ByteString responseByteString = 
				ServiceStub.createServiceInvoker(this.serviceName, rpcClient, serviceExcutor, null)
				.invoke(this.functionName, head, requestByteString)
				.get();
		
		JsonUtil.PROTOBUF_JSON_FORMAT.print(
				responseDefaultInstance.newBuilderForType().mergeFrom(responseByteString).build(), 
				this.out, Charsets.UTF_8);
		this.out.println();
		
		eventLoop.shutdownGracefully(0, 0, TimeUnit.SECONDS);
		serviceExcutor.shutdown();
		return 0;
	}

}
