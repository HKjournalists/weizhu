package com.weizhu.common.module;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.weizhu.common.server.ServerEntry;
import com.weizhu.common.service.ServiceThread;

public final class ExecutorModule extends AbstractModule {

	private static final Logger logger = LoggerFactory.getLogger(ExecutorModule.class);
	
	@Override
	protected void configure() {
	}
	
	@Provides
	@Singleton
	@Named("service_scheduled_executor")
	public ScheduledThreadPoolExecutor provideServiceScheduledExecutor() {
		ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(5, 
				new ThreadFactoryBuilder()
					.setNameFormat("service-scheduled-%d")
					.setDaemon(false)
					.setPriority(Thread.NORM_PRIORITY)
					.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

						@Override
						public void uncaughtException(Thread t, Throwable e) {
							logger.error("thread uncaught exception : " + t.getName(), e);
						}
					
					})	
					.build());
		scheduledExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		scheduledExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		scheduledExecutor.setKeepAliveTime(1, TimeUnit.MINUTES);
		return scheduledExecutor;
	}
	
	@Provides
	@Singleton
	@Named("service_scheduled_executor")
	public ScheduledExecutorService provideServiceScheduledExecutor1(@Named("service_scheduled_executor") ScheduledThreadPoolExecutor scheduledExecutor) {
		return scheduledExecutor;
	}
	
	@Provides
	@Singleton
	@Named("service_scheduled_executor")
	public ListeningScheduledExecutorService provideServiceScheduledExecutor2(@Named("service_scheduled_executor") ScheduledThreadPoolExecutor scheduledExecutor) {
		return MoreExecutors.listeningDecorator(scheduledExecutor);
	}
	
	@Provides
	@Singleton
	@Named("service_executor")
	public ThreadPoolExecutor provideServiceExecutor() {
		ThreadPoolExecutor serviceExecutor = new ThreadPoolExecutor(
				10, 100, // thread size
				1, TimeUnit.MINUTES, // keep alive
				new ArrayBlockingQueue<Runnable>(10), // 队列长度不要设置很大，会导致无法启动非core的线程
				new ThreadFactoryBuilder()
					.setNameFormat("service-%d")
					.setDaemon(false)
					.setPriority(Thread.NORM_PRIORITY)
					.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

						@Override
						public void uncaughtException(Thread t, Throwable e) {
							logger.error("thread uncaught exception : " + t.getName(), e);
						}
						
					})
					.setThreadFactory(new ServiceThreadFactory())
					.build()
				);
		return serviceExecutor;
	}
	
	@Provides
	@Singleton
	@Named("service_executor")
	public Executor provideServiceExecutor1(@Named("service_executor") ThreadPoolExecutor serviceExecutor) {
		return serviceExecutor;
	}
	
	@Provides
	@Singleton
	@Named("service_executor")
	public ExecutorService provideServiceExecutor2(@Named("service_executor") ThreadPoolExecutor serviceExecutor) {
		return serviceExecutor;
	}
	
	@Provides
	@Singleton
	@Named("service_executor")
	public ListeningExecutorService provideServiceExecutor3(@Named("service_executor") ThreadPoolExecutor serviceExecutor) {
		return MoreExecutors.listeningDecorator(serviceExecutor);
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideExecutorShutdownHook(
			@Named("service_scheduled_executor") final ScheduledThreadPoolExecutor scheduledExecutor, 
			@Named("service_executor") final ThreadPoolExecutor serviceExecutor
			) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public Order order() {
				return ServerEntry.ShutdownHook.Order.EXECUTOR;
			}
			
			@Override
			public void execute() {
				MoreExecutors.shutdownAndAwaitTermination(scheduledExecutor, 1, TimeUnit.MINUTES);
				MoreExecutors.shutdownAndAwaitTermination(serviceExecutor, 1, TimeUnit.MINUTES);
			}
			
		};
	}
	
	private static final class ServiceThreadFactory implements ThreadFactory {
		private final ThreadGroup group;

        public ServiceThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :  Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
        	return new ServiceThread(group, r);
        }
	}
	
}
