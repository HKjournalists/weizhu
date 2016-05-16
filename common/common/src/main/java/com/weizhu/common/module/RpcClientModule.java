package com.weizhu.common.module;

import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.weizhu.common.rpc.AutoSwitchRpcClient;
import com.weizhu.common.rpc.RpcInvoker;

public class RpcClientModule extends PrivateModule {

	private final String rpcServerName;
	
	public RpcClientModule(String rpcServerName) {
		this.rpcServerName = rpcServerName;
	}
	
	@Override
	protected void configure() {
		bind(RpcInvoker.class).annotatedWith(Names.named(rpcServerName))
			.to(Key.get(RpcInvoker.class, Names.named("_internal_rpc_invoker")))
			.in(Singleton.class);
		
		expose(RpcInvoker.class).annotatedWith(Names.named(rpcServerName));
	}
	
	private static final Splitter ADDR_SPLITTER = Splitter.on(CharMatcher.anyOf(",;")).trimResults().omitEmptyStrings();
	
	@Provides
	@Singleton
	@Named("_internal_rpc_invoker")
	public RpcInvoker provideInternalRpcInvoker(@Named("server_conf") Properties confProperties, NioEventLoopGroup eventLoop) {
		String str = confProperties.getProperty(rpcServerName + "_addr");
		if (str == null) {
			throw new RuntimeException("cannot find rpc client addr conf : " + rpcServerName + "_addr");
		}
		
		List<String> addrStrList = ADDR_SPLITTER.splitToList(str);
		List<InetSocketAddress> addrList = new ArrayList<InetSocketAddress>();
		for (String addrStr : addrStrList) {
			HostAndPort hp = HostAndPort.fromString(addrStr);
			addrList.add(new InetSocketAddress(hp.getHostText(), hp.getPort()));
		}
		return new AutoSwitchRpcClient(addrList, eventLoop);
	}

}
