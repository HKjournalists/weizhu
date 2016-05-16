package com.weizhu.service.apns.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class FeedbackConnection {
	
	private static final Logger logger = LoggerFactory.getLogger(FeedbackConnection.class);

	private final InetSocketAddress remoteAddress;
	private final SslContext sslContext;
	private final NioEventLoopGroup eventLoop;
	private final FeedbackListener listener;
	
	public FeedbackConnection(InetSocketAddress remoteAddress, SslContext sslContext, NioEventLoopGroup eventLoop, FeedbackListener listener) {
		this.remoteAddress = remoteAddress;
		this.sslContext = sslContext;
		this.eventLoop = eventLoop;
		this.listener = listener;
	}
	
	private volatile NioSocketChannel channel;
	
	public ListenableFuture<Void> connect() {
		SettableFuture<Void> connectFuture = SettableFuture.create();
		this.doConnect(connectFuture);
		return connectFuture;
	}
	
	private void doConnect(final SettableFuture<Void> connectFuture) {
		channel = new NioSocketChannel();
		channel.config().setConnectTimeoutMillis(30000);
		channel.config().setReuseAddress(true);
		channel.config().setKeepAlive(true);
		channel.config().setTcpNoDelay(true);
		
		channel.pipeline().addLast("ssl", sslContext.newHandler(channel.alloc(), remoteAddress.getHostString(), remoteAddress.getPort()));
		channel.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler(30));
		channel.pipeline().addLast("handler", new Handler());
		
		eventLoop.register(channel).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					if (future.isCancelled()) {
						connectFuture.cancel(false);
					} else {
						connectFuture.setException(new RuntimeException("register fail: " + remoteAddress, future.cause()));
					}
					return;
				}
				
				future.channel().connect(remoteAddress).addListener(new ChannelFutureListener() {

					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (!future.isSuccess()) {
							if (future.isCancelled()) {
								connectFuture.cancel(false);
							} else {
								connectFuture.setException(new RuntimeException("connect fail: " + remoteAddress, future.cause()));
							}
							return;
						}
						
						connectFuture.set(null);
					}
					
				});
			}
			
		});
	}
	
	public ListenableFuture<Void> close() {
		final NioSocketChannel ch = this.channel;
		if (ch == null || !ch.isActive()) {
			return Futures.<Void>immediateFuture(null);
		}
		
		SettableFuture<Void> closeFuture = SettableFuture.create();
		this.doClose(ch, closeFuture);
		return closeFuture;
	}
	
	private void doClose(final NioSocketChannel channel, final SettableFuture<Void> closeFuture) {
		channel.close().addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					closeFuture.set(null);
				} else if (future.isCancelled()) {
					closeFuture.cancel(false);
				} else {
					closeFuture.setException(future.cause());
				}
			}
		
		});
	}
	
	public boolean isActive() {
		final NioSocketChannel ch = this.channel;
		return ch != null && ch.isActive() ;
	}
	
	/**
	 * https://developer.apple.com/library/ios/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/Chapters/CommunicatingWIthAPS.html#//apple_ref/doc/uid/TP40008194-CH101-SW4
	 * @author lindongjlu
	 *
	 */
	private class Handler extends ChannelDuplexHandler {
		
		private final ByteBuf recvBuf = Unpooled.buffer(38);
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (!(msg instanceof ByteBuf)) {
				ctx.fireChannelRead(msg);
				return;
			}
			ByteBuf buf = (ByteBuf) msg;
			try {
				while (buf.isReadable()) {
					if (buf.readableBytes() <= recvBuf.writableBytes()) {
						recvBuf.writeBytes(buf);
					} else {
						recvBuf.writeBytes(buf, recvBuf.writableBytes());
					}
					
					if (!recvBuf.isWritable()) {
						final int timestamp = recvBuf.readInt();
						recvBuf.skipBytes(2);
						final byte[] deviceToken = new byte[32];
						recvBuf.readBytes(deviceToken);
						
						listener.handleExpiredToken(timestamp, deviceToken);
						
						recvBuf.clear();
					}
				}
			} finally {
				buf.release();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			if (cause instanceof ReadTimeoutException) {
				logger.info("feedback connection read timeout");
			} else {
				logger.error("feedback connection closed unexpected", cause);
			}
		}
		
	}
	
}
