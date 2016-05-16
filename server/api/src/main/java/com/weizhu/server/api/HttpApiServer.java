package com.weizhu.server.api;

import java.net.InetSocketAddress;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class HttpApiServer {

	private static final Logger logger = LoggerFactory.getLogger(HttpApiServer.class);
	
	private final Server jettyServer;
	
	@Inject
	public HttpApiServer (
			@Named("http_api_server_bind_addr") InetSocketAddress httpApiBindAddress, 
			HttpApiServlet httpApiServlet
			) {

		ServletContextHandler httpApiServletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		httpApiServletHandler.setContextPath("/api");
		httpApiServletHandler.addServlet(new ServletHolder(httpApiServlet), "/pb");
		
		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { httpApiServletHandler, new DefaultHandler() });
		
		this.jettyServer = new Server(httpApiBindAddress);
		this.jettyServer.setHandler(handlerList);
	}
	
	public void start() {
		logger.info("HttpApiServer starting...");
		
		try {
			this.jettyServer.start();
		} catch (Exception e) {
			logger.error("HttpApiServer start fail!", e);
			throw new Error(e);
		}
		
		logger.info("HttpApiServer start succ");
	}
	
	public void stop() {
		logger.info("HttpApiServer stopping...");
		
		try {
			this.jettyServer.stop();
		} catch (Exception e) {
			logger.error("HttpApiServer stop fail!", e);
			throw new Error(e);
		}
		
		logger.info("HttpApiServer stop succ");
	}
	
}
