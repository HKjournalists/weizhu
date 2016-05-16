package com.weizhu.common.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.ProvidesIntoSet;
import com.google.inject.name.Named;
import com.weizhu.common.server.ServerEntry;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;

public final class NettyModule extends AbstractModule {
	
	@Override
	protected void configure() {
	}
	
	@Provides
	@Singleton
	public NioEventLoopGroup provideRpcClientEventLoop() {
		return new NioEventLoopGroup();
	}
	
	@Provides
	@Singleton
	@Named("boss_group")
	public NioEventLoopGroup provideRpcServerBossEventLoop() {
		return new NioEventLoopGroup(1);
	}
	
	@Provides
	@Singleton
	@Named("worker_group")
	public NioEventLoopGroup provideRpcServerWorkerEventLoop() {
		return new NioEventLoopGroup();
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideRpcClientEventLoopShutdownHook(final NioEventLoopGroup eventLoop) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public Order order() {
				return ServerEntry.ShutdownHook.Order.RESOURCE;
			}

			@Override
			public void execute() {
				eventLoop.shutdownGracefully().syncUninterruptibly();
			}
			
		};
	}
	
	@ProvidesIntoSet
	public ServerEntry.ShutdownHook provideRpcServerEventLoopShutdownHook(
			@Named("boss_group") final NioEventLoopGroup bossGroup, 
			@Named("worker_group") final NioEventLoopGroup workerGroup) {
		return new ServerEntry.ShutdownHook() {

			@Override
			public Order order() {
				return ServerEntry.ShutdownHook.Order.NETTY_EVENTLOOP;
			}
			
			@Override
			public void execute() {
				Future<?> f1 = bossGroup.shutdownGracefully();
				Future<?> f2 = workerGroup.shutdownGracefully();
				f1.syncUninterruptibly();
				f2.syncUninterruptibly();
			}
			
		};
	}

}
