package com.weizhu.proto;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.weizhu.network.Future;

public interface ServiceInvoker {

	<V extends MessageLite> Future<V> invoke(String serviceName, String functionName, MessageLite request, Parser<V> responseParser, int priorityNum);
	
}
