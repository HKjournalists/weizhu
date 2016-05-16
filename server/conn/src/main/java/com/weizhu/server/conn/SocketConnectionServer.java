package com.weizhu.server.conn;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.proto.PushService;
import com.weizhu.proto.SessionService;

@Singleton
public class SocketConnectionServer {
	
	private static final Logger logger = LoggerFactory.getLogger(SocketConnectionServer.class);
	
	private final NioEventLoopGroup bossGroup;
	private final NioEventLoopGroup workerGroup;
	private final InetSocketAddress bindAddress;
	private final SessionService sessionService;
	private final PushService pushService;
	private final ImmutableMap<String, ServiceInvoker> serviceInvokerMap;
	private final SocketRegistry socketRegistry;
	
	@Inject
	public SocketConnectionServer(
			@Named("boss_group") NioEventLoopGroup bossGroup,
			@Named("worker_group") NioEventLoopGroup workerGroup,
			@Named("socket_connection_server_bind_addr") InetSocketAddress bindAddress, 
			SessionService sessionService, 
			PushService pushService,
			@Named("socket_connection_server_service_invoker") Set<ServiceInvoker> serviceInvokerSet, 
			SocketRegistry socketRegistry) {
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
		this.bindAddress = bindAddress;
		this.sessionService = sessionService;
		this.pushService = pushService;
		
		Map<String, ServiceInvoker> serviceInvokerMap = Maps.newTreeMap();
		for (ServiceInvoker serviceInvoker : serviceInvokerSet) {
			serviceInvokerMap.put(serviceInvoker.serviceName(), serviceInvoker);
		}
		
		this.serviceInvokerMap = ImmutableMap.copyOf(serviceInvokerMap);

		this.socketRegistry = socketRegistry;
	}
	
	private ServerSocketChannel serverChannel;
	
	public synchronized void start() {
		
		serverChannel = new NioServerSocketChannel();
		
		// 1. server channel config
		serverChannel.config().setBacklog(1024);
		serverChannel.config().setReuseAddress(true);
		
		// 2. server channel add handler
		serverChannel.pipeline().addLast("Acceptor", new Acceptor());
		
		// 3. server channel register and bind
		bossGroup.register(serverChannel).syncUninterruptibly();
		serverChannel.bind(bindAddress).syncUninterruptibly();
	}
	
	public synchronized void stop() {
		if (serverChannel != null) {
			serverChannel.close().syncUninterruptibly();
			serverChannel = null;
		}
	}
	
	private static final PacketCodec PACKET_CODEC = new PacketCodec();
	private static final ChannelFutureListener CHILD_REGISTER_LISTENER = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                forceClose(future.channel(), future.cause());
            }
        }
    };
    
    private static void forceClose(Channel child, Throwable t) {
        child.unsafe().closeForcibly();
        logger.warn("Failed to register an accepted channel: " + child, t);
    }
	
	private class Acceptor extends ChannelInboundHandlerAdapter {

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			final SocketChannel child = (SocketChannel) msg;
			
			logger.info("accept|" + child.remoteAddress());
			
			// 1. child channel config
			child.config().setKeepAlive(true);
			child.config().setReuseAddress(true);
			child.config().setTcpNoDelay(true);
			
			// 2. child channel add handler
			child.pipeline().addLast("FrameDecoder", new LengthFieldBasedFrameDecoder(1 * 1024 * 1024, 0, 4, 0, 4));
			child.pipeline().addLast("PacketCodec", PACKET_CODEC);
			child.pipeline().addLast("Handler", 
					new SocketConnectionHandler(sessionService, pushService, serviceInvokerMap, socketRegistry));
			
			// 3. child channel register
            try {
                workerGroup.register(child).addListener(CHILD_REGISTER_LISTENER);
            } catch (Throwable t) {
                forceClose(child, t);
            }
        }

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			logger.warn("exception", cause);
			
			final ChannelConfig config = ctx.channel().config();
            if (config.isAutoRead()) {
                // stop accept new connections for 1 second to allow the channel to recover
                // See https://github.com/netty/netty/issues/1328
                config.setAutoRead(false);
                ctx.channel().eventLoop().schedule(new Runnable() {
                    @Override
                    public void run() {
                       config.setAutoRead(true);
                    }
                }, 1, TimeUnit.SECONDS);
            }
            // still let the exceptionCaught event flow through the pipeline to give the user
            // a chance to do something with it
            ctx.fireExceptionCaught(cause);
		}
		
	}
	
}
