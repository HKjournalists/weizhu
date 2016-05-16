package com.weizhu.common.service;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import com.weizhu.common.CommonProtos;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.influxdb.ServiceApiMetric;
import com.weizhu.common.rpc.RpcInvoker;
import com.weizhu.common.server.ServerConst;
import com.weizhu.common.service.exception.HeadUnknownException;
import com.weizhu.common.service.exception.InvokeUnknownException;
import com.weizhu.common.service.exception.RequestParseException;
import com.weizhu.common.service.exception.ResponseParseException;
import com.weizhu.common.service.exception.ServiceException;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.ResponseType;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.weizhu.proto.WriteMethod;

public final class ServiceStub {
	
	private static final Logger logger = LoggerFactory.getLogger(ServiceStub.class);
	
	private ServiceStub() {
	}

	@SuppressWarnings("unchecked")
	public static <T> T createServiceApi(Class<T> serviceApi, T serviceImpl, Executor serviceExecutor) {
		return (T) Proxy.newProxyInstance(serviceApi.getClassLoader(), new Class<?>[]{serviceApi}, 
				new ServiceImplApiHandler(serviceApi, serviceImpl, serviceExecutor));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T createServiceApi(Class<T> serviceApi, ServiceInvoker serviceInvoker) {
		return (T) Proxy.newProxyInstance(serviceApi.getClassLoader(), new Class<?>[]{serviceApi}, 
				new ServiceInvokerApiHandler(serviceApi, serviceInvoker));
	}
	
	public static <T> ServiceInvoker createServiceInvoker(Class<T> serviceApi, T serviceImpl, Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return new LocalServiceInvoker(serviceApi, serviceImpl, serviceExecutor, influxDBReporter);
	}
	
	public static ServiceInvoker createServiceInvoker(String serviceName, RpcInvoker rpcInvoker, Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
		return new RpcServiceInvoker(serviceName, rpcInvoker, serviceExecutor, influxDBReporter);
	}
	
	public static RpcInvoker createRpcInvoker(Set<ServiceInvoker> serviceInvokerSet) {
		return new ServiceRpcInvoker(serviceInvokerSet);
	}
	
	private static final class ServiceImplApiHandler implements InvocationHandler {

		private final String serviceName;
		private final ImmutableMap<String, ImmutableMap<Class<?>, Func>> funcMap;
		private final Object serviceImpl;
		private final Executor serviceExecutor;
		
		ServiceImplApiHandler(Class<?> serviceApi, Object serviceImpl, Executor serviceExecutor) {
			Map<String, Map<Class<?>, Func>> tmpFuncMap = new HashMap<String, Map<Class<?>, Func>>();
			for (Method method : serviceApi.getMethods()) {
				checkMethod(serviceApi, method);
				
				final String functionName = method.getName();
				final Class<?> headType = method.getParameterTypes()[0];
				final boolean isWriteMethod = method.getAnnotation(WriteMethod.class) != null;
				final boolean isAsyncImpl;
				try {
					isAsyncImpl = serviceImpl.getClass().getMethod(method.getName(), method.getParameterTypes()).getAnnotation(AsyncImpl.class) != null;
				} catch (Throwable th) {
					throw new Error("invalid impl func : " + method.getName(), th);
				}
				
				Map<Class<?>, Func> headMap = tmpFuncMap.get(functionName);
				if (headMap == null) {
					headMap = new HashMap<Class<?>, Func>();
					tmpFuncMap.put(functionName, headMap);
				}
				headMap.put(headType, new Func(isWriteMethod, isAsyncImpl));
			}
			
			Map<String, ImmutableMap<Class<?>, Func>> funcMap = Maps.newTreeMap();
			for (Map.Entry<String, Map<Class<?>, Func>> entry : tmpFuncMap.entrySet()) {
				funcMap.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
			}
			
			this.serviceName = serviceApi.getSimpleName();
			this.funcMap = ImmutableMap.copyOf(funcMap);
			this.serviceImpl = serviceImpl;
			this.serviceExecutor = serviceExecutor;
		}
		
		@Override
		public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
			final ImmutableMap<Class<?>, Func> headMap = this.funcMap.get(method.getName());
			if (headMap == null) {
				return method.invoke(serviceImpl, args);
			}
			
			final Class<?> headClazz = method.getParameterTypes().length > 0 ? method.getParameterTypes()[0] : null;
			final Func func = headClazz == null ? null : headMap.get(headClazz);
			if (func == null) {
				return Futures.immediateFailedFuture(new HeadUnknownException("func: " + method.getName() + ", head: " + headClazz));
			}
			
			final long beginTime = System.currentTimeMillis();
			
			ListenableFuture<Message> future = null;
			try {
				future = invoke0(method, args, func);
			} catch (Throwable th) {
				future = Futures.immediateFailedFuture(th);
			}
		
			Futures.addCallback(future, 
					new ServiceApiLogCallback(serviceName, method.getName(), func.isWriteMethod, 
							args.length > 0 && args[0] instanceof Message ? (Message)args[0] : null,
							args.length > 1 && args[1] instanceof Message ? (Message)args[1] : null,
							beginTime
					));
			return future;
		}
		
		@SuppressWarnings("unchecked")
		private ListenableFuture<Message> invoke0(final Method method, final Object[] args, final Func func) throws Throwable {
			if (func.isAsyncImpl || (Thread.currentThread() instanceof ServiceThread)) {
				try {
					return (ListenableFuture<Message>) method.invoke(serviceImpl, args);
				} catch (InvocationTargetException e) {
					return Futures.immediateFailedFuture(e.getTargetException());
				} catch (Throwable th) {
					return Futures.immediateFailedFuture(th);
				}
			} else {
				final SettableFuture<Message> settableFuture = SettableFuture.create();
				this.serviceExecutor.execute(new Runnable() {

					@Override
					public void run() {
						ListenableFuture<Message> future = null;
						try {
							future = (ListenableFuture<Message>) method.invoke(serviceImpl, args);
						} catch (InvocationTargetException e) {
							settableFuture.setException(e.getTargetException());
							return;
						} catch (Throwable th) {
							settableFuture.setException(th);
							return;
						}
						
						try {
							settableFuture.set(future.get());
						} catch (ExecutionException e) {
							settableFuture.setException(e.getCause());
						} catch (Throwable th) {
							settableFuture.setException(th);
						}
					}
					
				});
				return settableFuture;
			}
		}
		
		private static final class Func {
			final boolean isWriteMethod;
			final boolean isAsyncImpl;
			Func(boolean isWriteMethod, boolean isAsyncImpl) {
				this.isWriteMethod = isWriteMethod;
				this.isAsyncImpl = isAsyncImpl;
			}
		}
		
	}
	
	private static final class ServiceInvokerApiHandler implements InvocationHandler {
		
		private final ImmutableMap<String, ImmutableMap<Class<?>, Parser<? extends Message>>> responseParserMap;
		private final ServiceInvoker serviceInvoker;
		
		ServiceInvokerApiHandler(Class<?> serviceApi, ServiceInvoker serviceInvoker) {
			
			Map<String, Map<Class<?>, Parser<? extends Message>>> tmpResponseParserMap = new HashMap<String, Map<Class<?>, Parser<? extends Message>>>();
			for (Method method : serviceApi.getMethods()) {
				final String functionName = method.getName();
				final Class<?> returnType = method.getReturnType();
				final Class<?>[] paramTypes = method.getParameterTypes();
				final ResponseType responseType = method.getAnnotation(ResponseType.class);
				
				if (returnType == ListenableFuture.class 
						&& paramTypes.length == 2 
						&& Message.class.isAssignableFrom(paramTypes[0])
						&& Message.class.isAssignableFrom(paramTypes[1])
						&& responseType != null
						&& Message.class.isAssignableFrom(responseType.value())) {
					
					Map<Class<?>, Parser<? extends Message>> headParserMap = tmpResponseParserMap.get(functionName);
					if (headParserMap == null) {
						headParserMap = new HashMap<Class<?>, Parser<? extends Message>>();
						tmpResponseParserMap.put(functionName, headParserMap);
					}
					headParserMap.put(paramTypes[0], fetchParser(responseType.value()));
				}
			}
			
			Map<String, ImmutableMap<Class<?>, Parser<? extends Message>>> responseParserMap = Maps.newTreeMap();
			for (Map.Entry<String, Map<Class<?>, Parser<? extends Message>>> entry : tmpResponseParserMap.entrySet()) {
				responseParserMap.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
			}
			
			this.responseParserMap = ImmutableMap.copyOf(responseParserMap);
			this.serviceInvoker = serviceInvoker;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final String functionName = method.getName();
			final Map<Class<?>, Parser<? extends Message>> headParserMap = responseParserMap.get(functionName);
			
			if (headParserMap == null) {
				throw new InvokeUnknownException("func: " + functionName);
			}
			
			final Class<?> headType = method.getParameterTypes().length > 0 ? method.getParameterTypes()[0] : null;
			final Parser<? extends Message> responseParser = headType != null ? headParserMap.get(headType) : null;
			
			if (responseParser == null) {
				throw new HeadUnknownException("func: " + functionName + ", head: " + headType);
			}
			
			ListenableFuture<ByteString> future = serviceInvoker.invoke(functionName, (Message) args[0], ((Message) (args[1])).toByteString());
			return Futures.transform(future, new Function<ByteString, Message>() {

				@Override
				public Message apply(ByteString responseBody) {
					try {
						return (Message) responseParser.parseFrom(responseBody);
					} catch(InvalidProtocolBufferException e) {
						throw new UndeclaredThrowableException(new ResponseParseException(e));
					}
				}
				
			});
		}
		
	}
	
	private static final class LocalServiceInvoker implements ServiceInvoker {
		
		private final String serviceName;
		private final ImmutableMap<String, ImmutableMap<Class<?>, Func>> funcMap;
		private final Object serviceImpl;
		private final Executor serviceExecutor;
		private final InfluxDBReporter influxDBReporter;
		
		LocalServiceInvoker(Class<?> serviceApi, Object serviceImpl, Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
			
			this.serviceName = serviceApi.getSimpleName();
			
			Map<String, Map<Class<?>, Func>> tmpFuncMap = new HashMap<String, Map<Class<?>, Func>>();
			for (Method method : serviceApi.getMethods()) {
				checkMethod(serviceApi, method);

				final String functionName = method.getName();
				final Class<?> headType = method.getParameterTypes()[0];
				final Class<?> requestType = method.getParameterTypes()[1];
				final boolean isWriteMethod = method.getAnnotation(WriteMethod.class) != null;
				final boolean isAsyncImpl;
				try {
					isAsyncImpl = serviceImpl.getClass().getMethod(method.getName(), method.getParameterTypes()).getAnnotation(AsyncImpl.class) != null;
				} catch (Throwable th) {
					throw new Error("invalid impl func : " + method.getName(), th);
				}
				
				Map<Class<?>, Func> headMap = tmpFuncMap.get(functionName);
				if (headMap == null) {
					headMap = new HashMap<Class<?>, Func>();
					tmpFuncMap.put(functionName, headMap);
				}
				headMap.put(headType, new Func(fetchParser(requestType), method, isWriteMethod, isAsyncImpl));
			}
			
			Map<String, ImmutableMap<Class<?>, Func>> funcMap = Maps.newTreeMap();
			for (Map.Entry<String, Map<Class<?>, Func>> entry : tmpFuncMap.entrySet()) {
				funcMap.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
			}
			
			this.funcMap = ImmutableMap.copyOf(funcMap);
			this.serviceImpl = serviceImpl;
			this.serviceExecutor = serviceExecutor;
			this.influxDBReporter = influxDBReporter;
		}
		
		@Override
		public String serviceName() {
			return serviceName;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public ListenableFuture<ByteString> invoke(String functionName, final Message head, ByteString requestBody) {
			
			final ImmutableMap<Class<?>, Func> headMap = this.funcMap.get(functionName);
			if (headMap == null) {
				return Futures.immediateFailedFuture(new InvokeUnknownException("func: " + functionName));
			}
			
			final Func func = headMap.get(head.getClass());
			if (func == null) {
				return Futures.immediateFailedFuture(new HeadUnknownException("func: " + functionName + ", head: " + head.getClass()));
			}
			
			final Message request;
			try {
				request = func.requestParser.parseFrom(requestBody);
			} catch (InvalidProtocolBufferException e) {
				return Futures.immediateFailedFuture(new RequestParseException(e));
			}
			
			final long beginTime = System.currentTimeMillis();
			
			if (func.isAsyncImpl || (Thread.currentThread() instanceof ServiceThread)) {
				ListenableFuture<Message> repsonseFuture;
				try {
					repsonseFuture = (ListenableFuture<Message>) func.method.invoke(serviceImpl, head, request);
				} catch (InvocationTargetException e) {
					repsonseFuture = Futures.immediateFailedFuture(e.getTargetException());
				} catch (Throwable th) {
					repsonseFuture = Futures.immediateFailedFuture(th);
				}
				
				if (this.influxDBReporter != null) {
					Futures.addCallback(repsonseFuture, new LocalServiceApiMetricCallback(this.influxDBReporter, serviceName, func.method.getName(), request.getSerializedSize(), beginTime));
				}
				Futures.addCallback(repsonseFuture, new ServiceApiLogCallback(serviceName, func.method.getName(), func.isWriteMethod, head, request, beginTime));
				
				return Futures.transform(repsonseFuture, TRANSFORM_FUNCTION);
			} else {
				final SettableFuture<ByteString> settableFuture = SettableFuture.create();
				this.serviceExecutor.execute(new Runnable() {

					@Override
					public void run() {
						ListenableFuture<Message> future = null;
						try {
							future = (ListenableFuture<Message>) func.method.invoke(serviceImpl, head, request);
						} catch (InvocationTargetException e) {
							future = Futures.immediateFailedFuture(e.getTargetException());
						} catch (Throwable th) {
							future = Futures.immediateFailedFuture(th);
						}
						
						try {
							settableFuture.set(future.get().toByteString());
						} catch (ExecutionException e) {
							settableFuture.setException(e.getCause());
						} catch (Throwable th) {
							settableFuture.setException(th);
						}
						
						if (influxDBReporter != null) {
							Futures.addCallback(future, new LocalServiceApiMetricCallback(influxDBReporter, serviceName, func.method.getName(), request.getSerializedSize(), beginTime));
						}
						Futures.addCallback(future, new ServiceApiLogCallback(serviceName, func.method.getName(), func.isWriteMethod, head, request, beginTime));
						
					}
					
				});
				return settableFuture;
			}
		}
		
		private static final Function<Message, ByteString> TRANSFORM_FUNCTION = new Function<Message, ByteString>() {

			@Override
			public ByteString apply(Message response) {
				return response.toByteString();
			}
			
		};
		
		private static final class Func {
			final Parser<? extends Message> requestParser;
			final Method method;
			final boolean isWriteMethod;
			final boolean isAsyncImpl;
			
			Func(Parser<? extends Message> requestParser, Method method, boolean isWriteMethod, boolean isAsyncImpl) {
				this.requestParser = requestParser;
				this.method = method;
				this.isWriteMethod = isWriteMethod;
				this.isAsyncImpl = isAsyncImpl;
			}
		}
		
	}
	
	private static final class RpcServiceInvoker implements ServiceInvoker {

		private final String serviceName;
		private final RpcInvoker rpcInvoker;
		private final Executor serviceExecutor;
		private final InfluxDBReporter influxDBReporter;
		
		RpcServiceInvoker(String serviceName, RpcInvoker rpcInvoker, Executor serviceExecutor, @Nullable InfluxDBReporter influxDBReporter) {
			this.serviceName = serviceName;
			this.rpcInvoker = rpcInvoker;
			this.serviceExecutor = serviceExecutor;
			this.influxDBReporter = influxDBReporter;
		}
		
		@Override
		public String serviceName() {
			return serviceName;
		}
		
		@Override
		public ListenableFuture<ByteString> invoke(String functionName, Message head, ByteString requestBody) {
			long beginTime = System.currentTimeMillis();
			ListenableFuture<ByteString> future = this.invoke0(functionName, head, requestBody);
			if (this.influxDBReporter != null) {
				Futures.addCallback(future, new RemoteServiceApiMetricCallback(this.influxDBReporter, this.serviceName, functionName, requestBody.size(), beginTime));
			}
			return future;
		}
		
		private ListenableFuture<ByteString> invoke0(String functionName, Message head, ByteString requestBody) {
			CommonProtos.RpcRequestPacket.Builder requestPacketBuilder = CommonProtos.RpcRequestPacket.newBuilder()
					.setInvokeId(0)
					.setServiceName(serviceName)
					.setFunctionName(functionName)
					.setRequestBody(requestBody);
			
			if (head instanceof SystemHead) {
				requestPacketBuilder.setSystemHead((SystemHead) head);
			} else if (head instanceof RequestHead) {
				requestPacketBuilder.setRequestHead((RequestHead) head);
			} else if (head instanceof AnonymousHead) {
				requestPacketBuilder.setAnonymousHead((AnonymousHead) head);
			} else if (head instanceof AdminHead) {
				requestPacketBuilder.setAdminHead((AdminHead) head);
			} else if (head instanceof AdminAnonymousHead) {
				requestPacketBuilder.setAdminAnonymousHead((AdminAnonymousHead) head);
			} else if (head instanceof BossHead) {
				requestPacketBuilder.setBossHead((BossHead) head);
			} else if (head instanceof BossAnonymousHead) {
				requestPacketBuilder.setBossAnonymousHead((BossAnonymousHead) head);
			} else {
				return Futures.immediateFailedFuture(new HeadUnknownException("head : " + head.getClass()));
			}
			
			ListenableFuture<CommonProtos.RpcResponsePacket> future = rpcInvoker.invoke(requestPacketBuilder.build());
			
			return Futures.transform(future, FUTURE_TRANSFORM_FUNCTION, serviceExecutor);
		}
		
		private static final Function<CommonProtos.RpcResponsePacket, ByteString> FUTURE_TRANSFORM_FUNCTION = 
				new Function<CommonProtos.RpcResponsePacket, ByteString>(){

			@Override
			public ByteString apply(CommonProtos.RpcResponsePacket responsePacket) {
				switch(responsePacket.getResult()) {
					case SUCC:
						return responsePacket.getResponseBody();
					case FAIL_INVOKE_UNKNOWN:
						throw new UndeclaredThrowableException(new InvokeUnknownException(responsePacket.getFailText()));
					case FAIL_HEAD_UNKNOWN:
						throw new UndeclaredThrowableException(new HeadUnknownException(responsePacket.getFailText()));
					case FAIL_BODY_PARSE_FAIL:
						throw new UndeclaredThrowableException(new RequestParseException(responsePacket.getFailText()));
					case FAIL_SERVER_EXCEPTION:
						throw new UndeclaredThrowableException(new ServiceException(responsePacket.getFailText()));
					default:
						throw new UndeclaredThrowableException(new ServiceException("unknown result : " + responsePacket.getResult() + "," + responsePacket.getFailText()));
				}
			}
			
		};
		
	}
	
	public static class ServiceRpcInvoker implements RpcInvoker {

		private final ImmutableMap<String, ServiceInvoker> serviceInvokerMap;

		public ServiceRpcInvoker(Set<ServiceInvoker> serviceInvokerSet) {
			Map<String, ServiceInvoker> serviceInvokerMap = Maps.newTreeMap();
			for (ServiceInvoker serviceInvoker : serviceInvokerSet) {
				serviceInvokerMap.put(serviceInvoker.serviceName(), serviceInvoker);
			}	
			this.serviceInvokerMap = ImmutableMap.copyOf(serviceInvokerMap);
		}
		
		@Override
		public ListenableFuture<CommonProtos.RpcResponsePacket> invoke(CommonProtos.RpcRequestPacket requestPacket) {
			Message head = null;
			switch (requestPacket.getHeadCase()) {
				case SYSTEM_HEAD:
					head = requestPacket.getSystemHead();
					break;
				case REQUEST_HEAD:
					head = requestPacket.getRequestHead();
					break;
				case ANONYMOUS_HEAD:
					head = requestPacket.getAnonymousHead();
					break;
				case ADMIN_HEAD:
					head = requestPacket.getAdminHead();
					break;
				case ADMIN_ANONYMOUS_HEAD:
					head = requestPacket.getAdminAnonymousHead();
					break;
				case BOSS_HEAD:
					head = requestPacket.getBossHead();
					break;
				case BOSS_ANONYMOUS_HEAD:
					head = requestPacket.getBossAnonymousHead();
					break;
				default:
					break;
			}
			
			if (head == null) {
				return Futures.immediateFuture(CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_HEAD_UNKNOWN)
						.setFailText("head : " + requestPacket.getHeadCase())
						.build());
			}
			
			ServiceInvoker serviceInvoker = serviceInvokerMap.get(requestPacket.getServiceName());
			if (serviceInvoker == null) {
				return Futures.immediateFuture(CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_INVOKE_UNKNOWN)
						.setFailText("service : " + requestPacket.getServiceName())
						.build());
			}
			
			ListenableFuture<ByteString> responseFuture;
			try {
				responseFuture = serviceInvoker.invoke(requestPacket.getFunctionName(), head, requestPacket.getRequestBody());
			} catch (Throwable th) {
				responseFuture = Futures.immediateFailedFuture(th);
			}
			
			return Futures.catching(Futures.transform(responseFuture, SUCC_FUNCTION), Throwable.class, FAIL_FALLBACK);
		}

		private static final Function<ByteString, CommonProtos.RpcResponsePacket> SUCC_FUNCTION = new Function<ByteString, CommonProtos.RpcResponsePacket>() {

			@Override
			public CommonProtos.RpcResponsePacket apply(ByteString responseBody) {
				return CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.SUCC)
						.setResponseBody(responseBody)
						.build();
			}
			
		};
		
		private static final Function<Throwable, CommonProtos.RpcResponsePacket> FAIL_FALLBACK = new Function<Throwable, CommonProtos.RpcResponsePacket>() {

			@Override
			public CommonProtos.RpcResponsePacket apply(Throwable t) {
				if (t instanceof InvokeUnknownException) {
					return CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_INVOKE_UNKNOWN)
						.setFailText(t.getMessage())
						.build();
				} else if (t instanceof HeadUnknownException) {
					return CommonProtos.RpcResponsePacket.newBuilder()
							.setInvokeId(0)
							.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_HEAD_UNKNOWN)
							.setFailText(t.getMessage())
							.build();
				} else if (t instanceof RequestParseException) {
					return CommonProtos.RpcResponsePacket.newBuilder()
							.setInvokeId(0)
							.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_BODY_PARSE_FAIL)
							.setFailText(t.getMessage())
							.build();
				} else {
					return CommonProtos.RpcResponsePacket.newBuilder()
							.setInvokeId(0)
							.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_SERVER_EXCEPTION)
							.setFailText(t.getMessage())
							.build();
				}
			}
			
		};
		
	}
	
	private static void checkMethod(Class<?> serviceApi, Method method) {
		final Class<?> returnType = method.getReturnType();
		final Class<?>[] paramTypes = method.getParameterTypes();
		
		if (returnType != ListenableFuture.class 
				|| paramTypes.length != 2 
				|| !Message.class.isAssignableFrom(paramTypes[0])
				|| !Message.class.isAssignableFrom(paramTypes[1])) {
			throw new Error("invalid method : " + serviceApi.getSimpleName() + "." + method.getName());
		}
	}
	
	private static Parser<? extends Message> fetchParser(Class<?> type) {
		try {
			return ((Message) (type.getMethod("getDefaultInstance").invoke(null))).getParserForType();
		} catch (Exception e) {
			throw new IllegalArgumentException("type cannot fetch parser : " + type, e);
		}
	}
	
	private static final Logger SERVICE_INVOKE_READ_LOGGER = LoggerFactory.getLogger("weizhu_service_invoke_read");
	private static final Logger SERVICE_INVOKE_WRITE_LOGGER = LoggerFactory.getLogger("weizhu_service_invoke_write");
	
	private static final class ServiceApiLogCallback implements FutureCallback<Message> {

		private final String serviceName;
		private final String functionName;
		private final boolean isWriteMethod;
		private final Message head;
		private final Message request;
		private final long beginTime;
		
		ServiceApiLogCallback(String serviceName, String functionName, boolean isWriteMethod, Message head, Message request, long beginTime) {
			this.serviceName = serviceName;
			this.functionName = functionName;
			this.isWriteMethod = isWriteMethod;
			this.head = head;
			this.request = request;
			this.beginTime = beginTime;
		}
		
		@Override
		public void onSuccess(Message response) {
			if ((!this.isWriteMethod && SERVICE_INVOKE_READ_LOGGER.isInfoEnabled())
					|| (this.isWriteMethod && SERVICE_INVOKE_WRITE_LOGGER.isInfoEnabled())
					) {
				
				StringBuilder json = new StringBuilder();
				try {
					json.append("{\"duration\":").append(System.currentTimeMillis() - this.beginTime);
					json.append(",\"server\":\"").append(ServerConst.SERVER_NAME);
					json.append("\",\"service\":\"").append(this.serviceName);
					json.append("\",\"function\":\"").append(this.functionName);
					json.append("\"");
					if (head != null) {
						json.append(",\"head\":{\"type\":\"").append(head.getClass().getSimpleName()).append("\",\"data\":");
						JsonUtil.PROTOBUF_JSON_FORMAT.print(head, json);
						json.append("}");
					}
					if (request != null) {
						json.append(",\"request\":{\"type\":\"").append(request.getClass().getSimpleName()).append("\",\"data\":");
						JsonUtil.PROTOBUF_JSON_FORMAT.print(request, json);
						json.append("}");
					}
					if (response != null) {
						json.append(",\"response\":{\"type\":\"").append(response.getClass().getSimpleName()).append("\",\"data\":");
						JsonUtil.PROTOBUF_JSON_FORMAT.print(response, json);
						json.append("}");
					}
					json.append("}");
					
					if (this.isWriteMethod) {
						SERVICE_INVOKE_WRITE_LOGGER.info(json.toString());
					} else {
						SERVICE_INVOKE_READ_LOGGER.info(json.toString());
					}
				} catch (IOException e) {
					logger.error("print fail! json fragment : " + json.toString(), e);
				}
			}
		}

		@Override
		public void onFailure(Throwable t) {
			if ((!this.isWriteMethod && SERVICE_INVOKE_READ_LOGGER.isErrorEnabled())
					|| (this.isWriteMethod && SERVICE_INVOKE_WRITE_LOGGER.isErrorEnabled())
					) {
				
				StringBuilder json = new StringBuilder();
				try {
					json.append("{\"duration\":").append(System.currentTimeMillis() - this.beginTime);
					json.append(",\"server\":\"").append(ServerConst.SERVER_NAME);
					json.append("\",\"service\":\"").append(this.serviceName);
					json.append("\",\"function\":\"").append(this.functionName);
					json.append("\"");
					if (head != null) {
						json.append(",\"head\":{\"type\":\"").append(head.getClass().getSimpleName()).append("\",\"data\":");
						JsonUtil.PROTOBUF_JSON_FORMAT.print(head, json);
						json.append("}");
					}
					if (request != null) {
						json.append(",\"request\":{\"type\":\"").append(request.getClass().getSimpleName()).append("\",\"data\":");
						JsonUtil.PROTOBUF_JSON_FORMAT.print(request, json);
						json.append("}");
					}
					if (t != null) {
						json.append(",\"exception\":");
						
						JsonObject exceptionObj = new JsonObject();
						exceptionObj.addProperty("type", t.getClass().getSimpleName());
						exceptionObj.addProperty("message", t.getMessage());
						exceptionObj.addProperty("stack_strace", Throwables.getStackTraceAsString(t));
						
						JsonUtil.GSON.toJson(exceptionObj, json);
					}
					json.append("}");
					
					if (this.isWriteMethod) {
						SERVICE_INVOKE_WRITE_LOGGER.error(json.toString());
					} else {
						SERVICE_INVOKE_READ_LOGGER.error(json.toString());
					}
				} catch (IOException e) {
					logger.error("print fail! json fragment : " + json.toString(), e);
					logger.error("invoke fail", t);
				}
			}
		}
		
	}
	
	private static final class LocalServiceApiMetricCallback implements FutureCallback<Message> {

		private final InfluxDBReporter influxDBReporter;
		private final String serviceName;
		private final String functionName;
		private final int requestPacketSize;
		private final long beginTime;
		
		LocalServiceApiMetricCallback (
				InfluxDBReporter influxDBReporter, 
				String serviceName, String functionName, 
				int requestPacketSize, long beginTime
				) {
			this.influxDBReporter = influxDBReporter;
			this.serviceName = serviceName;
			this.functionName = functionName;
			this.requestPacketSize = requestPacketSize;
			this.beginTime = beginTime;
		}
		
		@Override
		public void onSuccess(Message result) {
			ServiceApiMetric metric = this.influxDBReporter.serviceApiMetric("api", this.serviceName, this.functionName, false, "local");
			metric.record(System.currentTimeMillis() - this.beginTime, this.requestPacketSize, result.getSerializedSize());
		}

		@Override
		public void onFailure(Throwable t) {
			ServiceApiMetric metric = this.influxDBReporter.serviceApiMetric("api", this.serviceName, this.functionName, true, "local");
			metric.record(System.currentTimeMillis() - this.beginTime, this.requestPacketSize, 0);
		}
		
	}
	
	private static final class RemoteServiceApiMetricCallback implements FutureCallback<ByteString> {

		private final InfluxDBReporter influxDBReporter;
		private final String serviceName;
		private final String functionName;
		private final int requestPacketSize;
		private final long beginTime;
		
		RemoteServiceApiMetricCallback (
				InfluxDBReporter influxDBReporter, 
				String serviceName, String functionName, 
				int requestPacketSize, long beginTime
				) {
			this.influxDBReporter = influxDBReporter;
			this.serviceName = serviceName;
			this.functionName = functionName;
			this.requestPacketSize = requestPacketSize;
			this.beginTime = beginTime;
		}
		
		@Override
		public void onSuccess(ByteString result) {
			ServiceApiMetric metric = this.influxDBReporter.serviceApiMetric("api", this.serviceName, this.functionName, false, "remote");
			metric.record(System.currentTimeMillis() - this.beginTime, this.requestPacketSize, result.size());
		}

		@Override
		public void onFailure(Throwable t) {
			ServiceApiMetric metric = this.influxDBReporter.serviceApiMetric("api", this.serviceName, this.functionName, true, "remote");
			metric.record(System.currentTimeMillis() - this.beginTime, this.requestPacketSize, 0);
		}
		
	}
}
