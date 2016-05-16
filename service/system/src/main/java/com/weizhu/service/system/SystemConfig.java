package com.weizhu.service.system;

import java.util.List;
import java.util.Properties;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.config.ConfigUtil;
import com.weizhu.service.system.authurl.AuthUrl;
import com.weizhu.service.system.authurl.CompositeAuthUrl;
import com.weizhu.service.system.authurl.QiniuAuthUrl;

public class SystemConfig {

	private final ImmutableList<String> httpApiUrlList;
	private final ImmutableList<String> socketConnAddrList;
	private final ImmutableList<String> webRTCIceServerAddrList;
	private final String webappAdminUrlPrefix;
	private final String webappMobileUrlPrefix;
	private final String webappWebUrlPrefix;
	private final String webappUploadUrlPrefix;
	
	private final AuthUrl authUrl;
	
	@Inject
	public SystemConfig(@Named("server_conf") Properties confProperties) {
		Splitter splitter = Splitter.on(CharMatcher.anyOf(";,")).trimResults().omitEmptyStrings();
		
		this.httpApiUrlList = ImmutableList.copyOf(splitter.split(ConfigUtil.getNotEmpty(confProperties, "system_http_api_url")));
		this.socketConnAddrList = ImmutableList.copyOf(splitter.split(ConfigUtil.getNotEmpty(confProperties, "system_socket_conn_addr")));
		this.webRTCIceServerAddrList = ImmutableList.copyOf(splitter.split(ConfigUtil.getNullToEmpty(confProperties, "system_webrtc_ice_server_addr")));
		this.webappAdminUrlPrefix = ConfigUtil.getNotEmpty(confProperties, "system_webapp_admin_url_prefix");
		this.webappMobileUrlPrefix = ConfigUtil.getNotEmpty(confProperties, "system_webapp_mobile_url_prefix");
		this.webappWebUrlPrefix = ConfigUtil.getNotEmpty(confProperties, "system_webapp_web_url_prefix");
		this.webappUploadUrlPrefix = ConfigUtil.getNotEmpty(confProperties, "system_webapp_upload_url_prefix");
		
		ImmutableList.Builder<AuthUrl> builder = ImmutableList.builder();
		final List<String> nameList = splitter.splitToList(ConfigUtil.getNullToEmpty(confProperties, "system_authurl_name_list"));
		for (String name : nameList) {
			String keyPrefix = "system_authurl_item_" + name;
			String type = ConfigUtil.getNullToEmpty(confProperties, keyPrefix + "_type");
			if ("qiniu".equalsIgnoreCase(type)) {
				String urlPrefix = ConfigUtil.getNotEmpty(confProperties, keyPrefix + "_qiniu_url_prefix");
				String accessKey = ConfigUtil.getNotEmpty(confProperties, keyPrefix + "_qiniu_access_key");
				String secretKey = ConfigUtil.getNotEmpty(confProperties, keyPrefix + "_qiniu_secret_key");
				int expireTime = ConfigUtil.getInt(confProperties, keyPrefix + "_qiniu_expire_time");
				
				builder.add(new QiniuAuthUrl(urlPrefix, accessKey, secretKey, expireTime));
			}
		}
		
		this.authUrl = new CompositeAuthUrl(builder.build());
	}
	
	public ImmutableList<String> httpApiUrlList() {
		return httpApiUrlList;
	}

	public ImmutableList<String> socketConnAddrList() {
		return socketConnAddrList;
	}
	
	public ImmutableList<String> webRTCIceServerAddrList() {
		return webRTCIceServerAddrList;
	}
	
	public String webappAdminUrlPrefix() {
		return webappAdminUrlPrefix;
	}
	
	public String webappMobileUrlPrefix() {
		return webappMobileUrlPrefix;
	}
	
	public String webappWebUrlPrefix() {
		return webappWebUrlPrefix;
	}
	
	public String webappUploadUrlPrefix() {
		return webappUploadUrlPrefix;
	}
	
	public AuthUrl authUrl() {
		return authUrl;
	}

}
