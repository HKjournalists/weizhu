package com.weizhu.service.discover_v2;

import java.util.Properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.weizhu.common.config.ConfigUtil;

@Singleton
public class DiscoverV2Config {
	
	private final String webappMobileUrlPrefix;
	
	@Inject
	public DiscoverV2Config(@Named("server_conf") Properties confProperties) {
		this.webappMobileUrlPrefix = ConfigUtil.getNullToEmpty(confProperties, "discover_webapp_mobile_url_prefix");
	}
	
	public String webappMobileUrlPrefix() {
		return webappMobileUrlPrefix;
	}
}
