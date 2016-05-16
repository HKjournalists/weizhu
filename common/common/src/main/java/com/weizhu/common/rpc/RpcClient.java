package com.weizhu.common.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.weizhu.common.CommonProtos;

public class RpcClient implements RpcInvoker {

	private final InetSocketAddress remoteAddress;
	private final NioEventLoopGroup eventLoop;
	
	public RpcClient(InetSocketAddress remoteAddress, NioEventLoopGroup eventLoop) {
		this.remoteAddress = remoteAddress;
		this.eventLoop = eventLoop;
	}
	
	private volatile NioSocketChannel channel;
	
	public ListenableFuture<Void> connect() {
		if (channel != null) {
			throw new IllegalStateException("rpc client already connect");
		}
		
		final SettableFuture<Void> connectFuture = SettableFuture.create();
		this.connect0(connectFuture);
		return connectFuture;
	}
	
	void connect0(final SettableFuture<Void> connectFuture) {
		channel = new NioSocketChannel();
		channel.config().setConnectTimeoutMillis(1000);
		channel.config().setReuseAddress(true);
		channel.config().setKeepAlive(true);
		channel.config().setTcpNoDelay(true);
		
		channel.pipeline().addLast("FrameEncoder", new LengthFieldPrepender(4));
		channel.pipeline().addLast("FrameDecoder", new LengthFieldBasedFrameDecoder(16 * 1024 * 1024, 0, 4, 0, 4));
		channel.pipeline().addLast("Handler", new Handler());
		
		eventLoop.register(channel).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					if (future.isCancelled()) {
						connectFuture.cancel(false);
					} else {
						connectFuture.setException(new RpcException("register fail: " + remoteAddress, future.cause()));
					}
					return;
				}
				
				RpcClient.this.channel.connect(RpcClient.this.remoteAddress).addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (!future.isSuccess()) {
							if (future.isCancelled()) {
								connectFuture.cancel(false);
							} else {
								connectFuture.setException(new RpcException("connect fail: " + remoteAddress, future.cause()));
							}
							return;
						}
						connectFuture.set(null);
					}
					
				});
			}
			
		});
	}
	
	public boolean isActive() {
		final NioSocketChannel ch = this.channel;
		return ch != null && ch.isActive();
	}
	

	@Override
	public ListenableFuture<CommonProtos.RpcResponsePacket> invoke(CommonProtos.RpcRequestPacket requestPacket) {
		SettableFuture<CommonProtos.RpcResponsePacket> responseFuture = SettableFuture.create();
		this.invoke0(new InvokeObj(requestPacket, responseFuture));
		return responseFuture;
	}
	
	void invoke0(final InvokeObj invokeObj) {
		final NioSocketChannel ch = this.channel;
		if (ch == null || !ch.isActive()) {
			invokeObj.responseFuture().setException(new RpcException("client is not active: " + remoteAddress));
		} else {
			ch.writeAndFlush(invokeObj, ch.voidPromise());
		}
	}
	
	private static class Handler extends ChannelDuplexHandler {

		private int invokeIdGenerator = 0;
		private final LinkedList<InvokeContext> invokeContextList = new LinkedList<InvokeContext>();
		private final CommonProtos.RpcRequestPacket.Builder tmpRequestPacketBuilder = CommonProtos.RpcRequestPacket.newBuilder();
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (!(msg instanceof InvokeObj)) {
				ctx.write(msg, promise);
				return;
			}
			
			final InvokeObj invokeObj = (InvokeObj) msg;
			final int invokeId = invokeIdGenerator++;
			final CommonProtos.RpcRequestPacket requestPacket;
			
			try {
				requestPacket = tmpRequestPacketBuilder.clear()
						.mergeFrom(invokeObj.requestPacket())
						.setInvokeId(invokeId)
						.build();
			} finally {
				tmpRequestPacketBuilder.clear();
			}
			
			ByteBuf buf = ctx.alloc().ioBuffer(requestPacket.getSerializedSize());
			requestPacket.writeTo(new ByteBufOutputStream(buf));
			ctx.write(buf, promise);
			
			invokeContextList.add(new InvokeContext(invokeId, invokeObj.responseFuture()));
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (!(msg instanceof ByteBuf)) {
				ctx.fireChannelRead(msg);
				return;
			}
			ByteBuf buf = (ByteBuf) msg;
			try {
				CommonProtos.RpcResponsePacket responsePacket = CommonProtos.RpcResponsePacket.parseFrom(new ByteBufInputStream(buf));
				
				Iterator<InvokeContext> invokeContextIt = invokeContextList.iterator();
				while (invokeContextIt.hasNext()) {
					final InvokeContext invokeContext = invokeContextIt.next();
					if (invokeContext.invokeId == responsePacket.getInvokeId()) {
						invokeContextIt.remove();
						invokeContext.responseFuture.set(responsePacket);
						return;
					}
				}
			} finally {
				buf.release();
			}
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			for (InvokeContext invokeContext : invokeContextList) {
				invokeContext.responseFuture.setException(new RpcException("socket inactive"));
			}
			invokeContextList.clear();
		}
		
	}
	
	private static class InvokeContext {
		final int invokeId;
		final SettableFuture<CommonProtos.RpcResponsePacket> responseFuture;
		
		InvokeContext(int invokeId, SettableFuture<CommonProtos.RpcResponsePacket> responseFuture) {
			this.invokeId = invokeId;
			this.responseFuture = responseFuture;
		}
	}

}
