package com.weizhu.common.rpc;

import com.google.common.util.concurrent.SettableFuture;
import com.weizhu.common.CommonProtos;

class InvokeObj {

	private final CommonProtos.RpcRequestPacket requestPacket;
	private final SettableFuture<CommonProtos.RpcResponsePacket> responseFuture;
	
	public InvokeObj(CommonProtos.RpcRequestPacket requestPacket, SettableFuture<CommonProtos.RpcResponsePacket> responseFuture) {
		this.requestPacket = requestPacket;
		this.responseFuture = responseFuture;
	}

	public CommonProtos.RpcRequestPacket requestPacket() {
		return requestPacket;
	}
	
	public SettableFuture<CommonProtos.RpcResponsePacket> responseFuture() {
		return responseFuture;
	}

}
