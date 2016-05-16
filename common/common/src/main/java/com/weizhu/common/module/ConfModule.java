package com.weizhu.common.module;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public final class ConfModule extends AbstractModule {

	private final Properties confProperties;
	
	public ConfModule() {
		this.confProperties = new Properties();
		try {
			confProperties.load(new FileReader(System.getProperty("server.conf")));
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	public ConfModule(URL confUrl) {
		this.confProperties = new Properties();
		try {
			confProperties.load(new InputStreamReader(confUrl.openStream(), Charsets.UTF_8));
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	@Override
	protected void configure() {
		this.bind(Properties.class).annotatedWith(Names.named("server_conf")).toInstance(this.confProperties);
	}

}
