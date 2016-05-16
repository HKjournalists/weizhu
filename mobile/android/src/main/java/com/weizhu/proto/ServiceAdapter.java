package com.weizhu.proto;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.weizhu.network.Future;

public class ServiceAdapter implements ServiceInvoker {

	private volatile ServiceInvoker serviceInvoker;
	
	public ServiceAdapter(ServiceInvoker serviceInvoker) {
		if (serviceInvoker == null) {
			throw new NullPointerException("serviceInvoker");
		}
		this.serviceInvoker = serviceInvoker;
	}
	
	public void setServiceInvoker(ServiceInvoker serviceInvoker) {
		if (serviceInvoker == null) {
			throw new NullPointerException("serviceInvoker");
		}
		this.serviceInvoker = serviceInvoker;
	}
	
	@Override
	public <V extends MessageLite> Future<V> invoke(String serviceName,
			String functionName, MessageLite request, Parser<V> responseParser,
			int priorityNum) {
		return this.serviceInvoker.invoke(serviceName, functionName, request, responseParser, priorityNum);
	}

}
