package com.weizhu.common.rpc;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.CommonProtos;

public class RpcServer {

	private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);
	
	private final NioEventLoopGroup bossGroup;
	private final NioEventLoopGroup workerGroup;
	private final InetSocketAddress bindAddress;
	private final RpcInvoker rpcInvoker;
	
	@Inject
	public RpcServer(
			@Named("boss_group") NioEventLoopGroup bossGroup, 
			@Named("worker_group") NioEventLoopGroup workerGroup, 
			@Named("rpc_bind_addr") InetSocketAddress bindAddress, 
			RpcInvoker rpcInvoker) {
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
		this.bindAddress = bindAddress;
		this.rpcInvoker = rpcInvoker;
	}
	
	private volatile NioServerSocketChannel serverChannel;
	
	public void start() {
		if (serverChannel != null) {
			throw new IllegalStateException("rpc server already start");
		}
		
		serverChannel = new NioServerSocketChannel();
		serverChannel.config().setBacklog(1024);
		serverChannel.pipeline().addLast("Acceptor", new Acceptor());
		
		bossGroup.register(serverChannel).syncUninterruptibly();
		serverChannel.bind(bindAddress).syncUninterruptibly();
		
		logger.info("rpc server start succ , bind addr : " + bindAddress);
	}
	
	public void stop() {
		serverChannel.close();
		serverChannel = null;
		
		logger.info("rpc server stop ");
	}
	
	private class Acceptor extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			final NioSocketChannel child = (NioSocketChannel) msg;
			
			child.config().setReuseAddress(true);
			child.config().setTcpNoDelay(true);
			child.config().setKeepAlive(true);
			
			child.pipeline().addLast("FrameEncoder", new LengthFieldPrepender(4));
			child.pipeline().addLast("FrameDecoder", new LengthFieldBasedFrameDecoder(16 * 1024 * 1024, 0, 4, 0, 4));
			child.pipeline().addLast("Handler", new Handler());
			
			RpcServer.this.workerGroup.register(child).addListener(FORCE_CLOSE_ON_FAILURE);
			
			logger.info("accept : " + child.remoteAddress());
		}
		
	}
	
	private static final ChannelFutureListener FORCE_CLOSE_ON_FAILURE = new ChannelFutureListener() {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (!future.isSuccess()) {
				future.channel().unsafe().closeForcibly();
			}
		}
		
	};
	
	private class Handler extends ChannelDuplexHandler {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (!(msg instanceof ByteBuf)) {
				ctx.fireChannelRead(msg);
				return;
			}
			
			final ByteBuf buf = (ByteBuf) msg;
			try {
				final CommonProtos.RpcRequestPacket requestPacket = CommonProtos.RpcRequestPacket.parseFrom(new ByteBufInputStream(buf));
				final int invokeId = requestPacket.getInvokeId();
				
				ListenableFuture<CommonProtos.RpcResponsePacket> future; 
				try {
					future = RpcServer.this.rpcInvoker.invoke(requestPacket);
				} catch (Throwable th) {
					future = Futures.immediateFailedFuture(th);
				}
				
				final Channel channel = ctx.channel();
				Futures.addCallback(future, new FutureCallback<CommonProtos.RpcResponsePacket>() {

					@Override
					public void onSuccess(CommonProtos.RpcResponsePacket responsePacket) {
						channel.writeAndFlush(responsePacket.toBuilder()
								.setInvokeId(invokeId)
								.build(), channel.voidPromise());
					}

					@Override
					public void onFailure(Throwable th) {
						
						logger.warn("invoke exception", th);
						
						channel.writeAndFlush(CommonProtos.RpcResponsePacket.newBuilder()
								.setInvokeId(invokeId)
								.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_SERVER_EXCEPTION)
								.setFailText("服务处理异常: " + th.getClass().getSimpleName() + ", " + th.getMessage())
								.build(), channel.voidPromise());
					}
					
				});

			} finally {
				buf.release();
			}
		}
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception { 
			if (!(msg instanceof CommonProtos.RpcResponsePacket)) {
				ctx.write(msg, promise);
				return;
			}
			
			CommonProtos.RpcResponsePacket responsePacket = (CommonProtos.RpcResponsePacket) msg;
			ByteBuf buf = ctx.alloc().ioBuffer(responsePacket.getSerializedSize());
			responsePacket.writeTo(new ByteBufOutputStream(buf));
			ctx.write(buf, promise);
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			logger.error("exception", cause);
		}
		
	}
	
}
