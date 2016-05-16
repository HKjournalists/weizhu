package com.weizhu.service.apns.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

public class PushConnection {
	
	private static final Logger logger = LoggerFactory.getLogger(PushConnection.class);

	private final InetSocketAddress remoteAddress;
	private final SslContext sslContext;
	private final NioEventLoopGroup eventLoop;
	private final PushListener listener;
	
	public PushConnection(InetSocketAddress remoteAddress, SslContext sslContext, NioEventLoopGroup eventLoop, PushListener listener) {
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
	
	public void sendNotification(final PushNotification notification) {
		if (!notification.checkValid()) {
			throw new RuntimeException("invalid notification");
		}
		
		final NioSocketChannel ch = this.channel;
		if (ch == null) {
			throw new RuntimeException("not connect");
		}
		
		ch.writeAndFlush(notification).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					if (future.isCancelled()) {
						logger.error("send notification cancelled : " + notification);
					} else {
						logger.error("send notification failed : " + notification, future.cause());
					}
					
					PushConnection.this.listener.handleNotificationResent(notification);
				}
			}
			
		});
	}
	
	/**
	 * https://developer.apple.com/library/ios/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/Chapters/CommunicatingWIthAPS.html#//apple_ref/doc/uid/TP40008194-CH101-SW4
	 * @author lindongjlu
	 *
	 */
	private class Handler extends ChannelDuplexHandler {
		
		private static final int NOTIFICATION_QUEUE_MAX_SIZE = 100;
		
		private int notificationIdGenerator = 0;
		private final Queue<NotificationItem> notificationQueue = new ArrayDeque<NotificationItem>();
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (!(msg instanceof PushNotification)) {
				ctx.write(msg, promise);
				return;
			}
			
			final PushNotification notification = (PushNotification) msg;
			final int notificationId = notificationIdGenerator++ ;
			
			promise.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						// 发送成功后，加入到队列中
						notificationQueue.offer(new NotificationItem(notificationId, notification));
						while (notificationQueue.size() > NOTIFICATION_QUEUE_MAX_SIZE) {
							notificationQueue.poll();
						}
					}
				}
			
			});
			
			int frameLength = 5 * (1 + 2) + notification.deviceToken().length + notification.payload().length + 4 + 4 + 1;
			
			ByteBuf buf = ctx.alloc().ioBuffer(1 + 4 + frameLength);
			
			// Command
			buf.writeByte(2);
			// Frame length
			buf.writeInt(frameLength);
			// Frame data
			
			// Item ID 
			// 1, Device token
			buf.writeByte(1);
			// Item data length
			buf.writeShort(notification.deviceToken().length);
			// Item data
			// Device token
			buf.writeBytes(notification.deviceToken());
			
			// Item ID 
			// 2, Payload
			buf.writeByte(2);
			// Item data length
			buf.writeShort(notification.payload().length);
			// Item data
			// Device token
			buf.writeBytes(notification.payload());
			
			// Item ID 
			// 3, Notification identifier
			buf.writeByte(3);
			// Item data length
			buf.writeShort(4);
			// Item data
			// Notification identifier
			buf.writeInt(notificationId);
			
			// Item ID 
			// 4, Expiration date
			buf.writeByte(4);
			// Item data length
			buf.writeShort(4);
			// Item data
			// Notification identifier
			buf.writeInt(notification.expirationTimestamp());
			
			// Item ID 
			// 5, Priority
			buf.writeByte(5);
			// Item data length
			buf.writeShort(1);
			// Item data
			// Notification identifier
			buf.writeByte(notification.priority());

			ctx.write(buf, promise);
		}
		
		private final ByteBuf recvBuf = Unpooled.buffer(6);
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (!(msg instanceof ByteBuf)) {
				ctx.fireChannelRead(msg);
				return;
			}
			ByteBuf buf = (ByteBuf) msg;
			try {
				if (buf.readableBytes() <= recvBuf.writableBytes()) {
					recvBuf.writeBytes(buf);
				} else {
					recvBuf.writeBytes(buf, recvBuf.writableBytes());
				}
			} finally {
				buf.release();
			}
			
			if (recvBuf.isWritable()) {
				return;
			}
			
			final byte command = recvBuf.readByte();
			final short statusCode = recvBuf.readUnsignedByte();
			final int notificationId = recvBuf.readInt();
			
			if (command == 8) {
				PushNotification notification = null;
				for (NotificationItem item : notificationQueue) {
					if (item.notificationId == notificationId) {
						notification = item.notification;
						break;
					}
				}
				logger.warn("push connection recv: " + statusCode + "[" + getStatusCodeDescription(statusCode) + "], " + notificationId + ", " + notification);
				
				while (!notificationQueue.isEmpty()) {
					NotificationItem item = notificationQueue.poll();
					if (item.notificationId > notificationId) {
						listener.handleNotificationResent(item.notification);
					}
				}
			} else {
				logger.error("push connection recv: unexpected command, " + command + ", " + statusCode + ", " + notificationId);
			}
			
			ctx.close();
		}
		
		private String getStatusCodeDescription(short statusCode) {
			switch (statusCode) {
				case 0:
					return "No errors encountered";
				case 1:
					return "Processing error";
				case 2:
					return "Missing device token";
				case 3:
					return "Missing topic";
				case 4:
					return "Missing payload";
				case 5:
					return "Invalid token size";
				case 6:
					return "Invalid topic size";
				case 7:
					return "Invalid payload size";
				case 8:
					return "Invalid token";
				case 10:
					return "Shutdown";
				case 255:
					return "None (unknown)";
				default:
					break;
			}
			return "Unkonwn Status Code";
		}
		
		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			notificationQueue.clear();
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			logger.error("push connection exception", cause);
		}
		
	}
	
	private static class NotificationItem {
		final int notificationId;
		final PushNotification notification;
		
		NotificationItem(int notificationId, PushNotification notification) {
			this.notificationId = notificationId;
			this.notification = notification;
		}
	}
	
}
