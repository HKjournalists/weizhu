package com.weizhu.common.rpc;

import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.weizhu.common.CommonProtos;

public class AutoSwitchRpcClient implements RpcInvoker {

	private final InetSocketAddress[] remoteAddresses;
	private final NioEventLoopGroup eventLoop;
	private final AtomicReferenceArray<Holder> holders;
	
	public AutoSwitchRpcClient(List<InetSocketAddress> remoteAddressList, NioEventLoopGroup eventLoop) {
		if (remoteAddressList.isEmpty()) {
			throw new IllegalArgumentException("remoteAddressList is empty");
		}
		
		this.remoteAddresses = remoteAddressList.toArray(new InetSocketAddress[remoteAddressList.size()]);
		this.eventLoop = eventLoop;
		this.holders = new AtomicReferenceArray<Holder>(this.remoteAddresses.length);
	}
	
	@Override
	public ListenableFuture<CommonProtos.RpcResponsePacket> invoke(CommonProtos.RpcRequestPacket requestPacket) {
		final SettableFuture<CommonProtos.RpcResponsePacket> responseFuture = SettableFuture.create();
		this.invoke0(new InvokeObj(requestPacket, responseFuture));
		return responseFuture;
	}
	
	private final AtomicInteger pollingSeq = new AtomicInteger(0);
	
	private void invoke0(final InvokeObj invokeObj) {
		
		final long now = System.currentTimeMillis();
		final int seq = pollingSeq.getAndIncrement();
		
		// 第一轮: 选取 active 的client 直接发送。这样做延迟最小
		for (int i=0; i<holders.length(); ++i) {
			final int idx = mod(i + seq, holders.length());
			
			Holder h = holders.get(idx);
			if (h == null) {
				InetSocketAddress remoteAddress = remoteAddresses[idx];
				RpcClient client = new RpcClient(remoteAddress, eventLoop);
				SettableFuture<Void> connectFuture = SettableFuture.create();
				
				boolean succ = holders.compareAndSet(idx, h, new Holder(now, client, connectFuture));
				if (succ) {
					client.connect0(connectFuture);
				}
				// if succ == false : 说明同时有其他线程操作, 丢弃当前所做操作
			} else if (h.client.isActive()) {
				h.client.invoke0(invokeObj);
				return;
			} else if (h.connectFuture.isDone()) {
				// 已连接过，且 inactive, 说明此client已关闭. 隔3s后重新连接
				if (now - h.createTime > 3000) {
					InetSocketAddress remoteAddress = remoteAddresses[idx];
					RpcClient client = new RpcClient(remoteAddress, eventLoop);
					SettableFuture<Void> connectFuture = SettableFuture.create();
					
					boolean succ = holders.compareAndSet(idx, h, new Holder(now, client, connectFuture));
					if (succ) {
						client.connect0(connectFuture);
					}
				}
			} else {
				// 正在连接中...
			}
		}
		
		// 第一轮未发送成功, 则第二轮选中正在连接的client 等待连接完成后发送
		for (int i=0; i<holders.length(); ++i) {
			final int idx = mod(i + seq, holders.length());
			
			Holder h = holders.get(idx);
			if (h != null) {
				if (h.client.isActive()) {
					h.client.invoke0(invokeObj);
					return;
				} else if (!h.connectFuture.isDone()) {
					// 正在连接中...
					final RpcClient client = h.client;
					Futures.addCallback(h.connectFuture, new FutureCallback<Void>() {
	
						@Override
						public void onSuccess(Void result) {
							client.invoke0(invokeObj);
						}
	
						@Override
						public void onFailure(Throwable t) {
							invokeObj.responseFuture().setException(t);
						}
						
					});
					return;
				}
			}
		}
		
		// 两轮都未发送成功,返回失败
		invokeObj.responseFuture().setException(new RpcException("no active client"));
	}
	
	private static int mod(int n, int mod) {
		int x = n % mod;
		return x < 0 ? x + mod : x;
	}
	
	private static class Holder {
		final long createTime;
		final RpcClient client;
		final ListenableFuture<Void> connectFuture;
		
		Holder(long createTime, RpcClient client, ListenableFuture<Void> connectFuture) {
			this.createTime = createTime;
			this.client = client;
			this.connectFuture = connectFuture;
		}
	}
	
}
