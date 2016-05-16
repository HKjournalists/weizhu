package com.weizhu.proto;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.weizhu.network.Future;

public class ServiceProxy {

	private ServiceProxy() {
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> serviceApi, ServiceInvoker serviceInvoker) {
		return (T) Proxy.newProxyInstance(serviceApi.getClassLoader(), new Class<?>[]{serviceApi}, new ServiceProxyHandler(serviceApi, serviceInvoker));
	}
	
	private static class ServiceProxyHandler implements InvocationHandler {

		private final String serviceName;
		private final Map<String, Parser<?>> parserMap;
		private final ServiceInvoker serviceInvoker;
		
		ServiceProxyHandler(Class<?> serviceApi, ServiceInvoker serviceInvoker) {
			this.serviceName = serviceApi.getSimpleName();
			
			Method[] methods = serviceApi.getMethods();
			this.parserMap = new HashMap<String, Parser<?>>(methods.length);
			for (Method method : methods) {
				ResponseType responseType = method.getAnnotation(ResponseType.class);
				Class<?> returnType = method.getReturnType();
				Class<?>[] paramTypes = method.getParameterTypes();
				
				if (responseType != null
						&& MessageLite.class.isAssignableFrom(responseType.value())
						&& returnType == Future.class 
						&& paramTypes.length == 2 
						&& MessageLite.class.isAssignableFrom(paramTypes[0])
						&& paramTypes[1] == int.class) {
					
					try {
						MessageLite defaultInstance = (MessageLite) responseType.value().getMethod("getDefaultInstance").invoke(null);
						this.parserMap.put(method.getName(), defaultInstance.getParserForType());
					} catch (Exception e) {
						throw new IllegalArgumentException("service function invalid : " + serviceName + "." + method.getName(), e);
					}
				} else {
					throw new IllegalArgumentException("service function invalid : " + serviceName + "." + method.getName());
				}
			}
			
			this.serviceInvoker = serviceInvoker;
		}		
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String functionName = method.getName();
			Parser parser = parserMap.get(functionName);
			if (parser == null) {
				throw new UnsupportedOperationException(serviceName + "." + functionName + " invalid!");
			}
			return serviceInvoker.invoke(serviceName, functionName, (MessageLite) args[0], parser, (Integer) args[1]);
		}
		
	}
	
}
