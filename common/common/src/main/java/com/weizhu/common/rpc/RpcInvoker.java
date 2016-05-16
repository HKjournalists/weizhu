package com.weizhu.common.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.weizhu.common.CommonProtos;

public interface RpcInvoker {

	/**
	 * throw {@link RpcException}
	 */
	ListenableFuture<CommonProtos.RpcResponsePacket> invoke(CommonProtos.RpcRequestPacket requestPacket);
	
}
