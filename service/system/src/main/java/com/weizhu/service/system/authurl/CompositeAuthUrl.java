package com.weizhu.service.system.authurl;

import com.google.common.collect.ImmutableList;

public class CompositeAuthUrl implements AuthUrl {

	private final ImmutableList<AuthUrl> authUrlList;
	
	public CompositeAuthUrl(ImmutableList<AuthUrl> authUrlList) {
		this.authUrlList = authUrlList;
	}
	
	@Override
	public String auth(long companyId, String url) {
		for (AuthUrl authUrl : authUrlList) {
			String u = authUrl.auth(companyId, url);
			if (u != null) {
				return u;
			}
		}
		return null;
	}

}
