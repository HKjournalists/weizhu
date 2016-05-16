package com.weizhu.service.apns.net;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class APNsManager {
	
	private static final Logger logger = LoggerFactory.getLogger(APNsManager.class);
	
	private static final InetSocketAddress PUSH_CONNECTION_PRODUCTION_ADDRESS = new InetSocketAddress("gateway.push.apple.com", 2195);
	private static final InetSocketAddress PUSH_CONNECTION_DEVELOPMENT_ADDRESS = new InetSocketAddress("gateway.sandbox.push.apple.com", 2195);
	
	private static final InetSocketAddress FEEDBACK_CONNECTION_PRODUCTION_ADDRESS = new InetSocketAddress("feedback.push.apple.com", 2196);
	private static final InetSocketAddress FEEDBACK_CONNECTION_DEVELOPMENT_ADDRESS = new InetSocketAddress("feedback.sandbox.push.apple.com", 2196);
	
	private static final int MAX_NOTIFICATION_RETRY = 3;

	private final String appId;
	private final boolean isProduction;
	private final SslContext sslContext;
	private final NioEventLoopGroup eventLoop;
	private final FeedbackListener feedbackListener;
	
	private final BlockingDeque<PushNotification> notificationDeque;
	private final PushConnectionHolder[] pushConnectionHolders;
	private final DispatchThread dispatchThread;
	
	private FeedbackConnectionHolder feedbackConnectionHolder;
	
	public APNsManager(String appId, boolean isProduction, SslContext sslContext, NioEventLoopGroup eventLoop, int pushConnectionSize, FeedbackListener feedbackListener) {
		this.appId = appId;
		this.isProduction = isProduction;
		this.sslContext = sslContext;
		this.eventLoop = eventLoop;
		this.feedbackListener = feedbackListener;
		
		this.notificationDeque = new LinkedBlockingDeque<PushNotification>();
		this.pushConnectionHolders = new PushConnectionHolder[pushConnectionSize];
		this.dispatchThread = new DispatchThread();
		this.dispatchThread.setName("APNsDispatch-" + this.appId + "-" + (this.isProduction ? "production" : "development"));
		this.dispatchThread.setDaemon(true);
		this.dispatchThread.start();
		
		this.feedbackConnectionHolder = null;
	}
	
	public String appId() {
		return appId;
	}
	
	public boolean isProduction() {
		return isProduction;
	}
	
	public void sendNotification(PushNotification notification) {
		this.notificationDeque.offerLast(notification);
	}
	
	public void tryFeedbackConnect() {
		synchronized (this) {
			if (feedbackConnectionHolder != null 
					&& (!feedbackConnectionHolder.connectFuture.isDone() || feedbackConnectionHolder.feedbackConnection.isActive())) {
				return;
			}
			
			FeedbackConnection conn = new FeedbackConnection(
					isProduction ? FEEDBACK_CONNECTION_PRODUCTION_ADDRESS : FEEDBACK_CONNECTION_DEVELOPMENT_ADDRESS,
					sslContext, eventLoop, feedbackListener
					);
			feedbackConnectionHolder = new FeedbackConnectionHolder(conn, conn.connect());
		}
	}
	
	public void shutdown() {
		this.dispatchThread.interrupt();
		
		synchronized (this) {
			if (feedbackConnectionHolder != null) {
				if (!feedbackConnectionHolder.connectFuture.isDone() || feedbackConnectionHolder.feedbackConnection.isActive()) {
					feedbackConnectionHolder.feedbackConnection.close();
				}
				feedbackConnectionHolder = null;
			}
		}
	}
	
	private final PushListener pushListener = new PushListener() {

		@Override
		public void handleNotificationResent(PushNotification notification) {
			notificationDeque.offerFirst(new PushNotification(notification, notification.retry() + 1));
		}
		
	};
	
	private class DispatchThread extends Thread {

		@Override
		public void run() {
			int pollingSeq = 0;
			while (true) {
				try {
					final PushNotification notification = notificationDeque.takeFirst();
					if (notification.retry() >= MAX_NOTIFICATION_RETRY) {
						logger.error("notification fail : " + notification);
						continue;
					}
					
					final PushConnection pushConnection = getAlivePushConnection(pollingSeq);
					pushConnection.sendNotification(notification);
					
					pollingSeq++;
					pollingSeq %= pushConnectionHolders.length;
				} catch (InterruptedException e) {
					break;
				}
			}
			
			while (!notificationDeque.isEmpty()) {
				final PushNotification notification = notificationDeque.pollFirst();
				logger.info("notification discard : " + notification);
			}
			
			for (int i=0; i<pushConnectionHolders.length; ++i) {
				final PushConnectionHolder h = pushConnectionHolders[i];
				
				if (h != null) {
					if (!h.connectFuture.isDone() || h.pushConnection.isActive()) {
						h.pushConnection.close();
					}
					pushConnectionHolders[i] = null;
				}
			}
			
			logger.info("notification dispatch thread stop");
		}
		
		private PushConnection getAlivePushConnection(int pollingSeq) throws InterruptedException {
			while (true) {
				
				Long now = null;
				for (int i=0; i<pushConnectionHolders.length; ++i) {
					final int idx = (i + pollingSeq) % pushConnectionHolders.length;
					
					final PushConnectionHolder h = pushConnectionHolders[idx];
					if (h != null && h.pushConnection.isActive()) {
						return h.pushConnection;
					}
					
					if (now == null) {
						now = System.currentTimeMillis();
					}
					
					if (h == null // 第一次立即连接
							|| (h.connectFuture.isDone() && !h.pushConnection.isActive() && now - h.createTime > 60 * 1000) // 如果连接已断，保证隔1分钟后再连
							) {
						PushConnection conn = new PushConnection(
								isProduction ? PUSH_CONNECTION_PRODUCTION_ADDRESS : PUSH_CONNECTION_DEVELOPMENT_ADDRESS,
								sslContext, eventLoop, pushListener);
						pushConnectionHolders[idx] = new PushConnectionHolder(now, conn, conn.connect());
					}
				}
				
				TimeUnit.MILLISECONDS.sleep(500);
			}
		}
		
	}
	
	private static class PushConnectionHolder {
		final long createTime;
		final PushConnection pushConnection;
		final ListenableFuture<Void> connectFuture;
		
		PushConnectionHolder(long createTime, PushConnection pushConnection, ListenableFuture<Void> connectFuture) {
			this.createTime = createTime;
			this.pushConnection = pushConnection;
			this.connectFuture = connectFuture;
		}
	}
	
	private static class FeedbackConnectionHolder {
		final FeedbackConnection feedbackConnection;
		final ListenableFuture<Void> connectFuture;
		
		FeedbackConnectionHolder(FeedbackConnection feedbackConnection, ListenableFuture<Void> connectFuture) {
			this.feedbackConnection = feedbackConnection;
			this.connectFuture = connectFuture;
		}
	}
	
}
