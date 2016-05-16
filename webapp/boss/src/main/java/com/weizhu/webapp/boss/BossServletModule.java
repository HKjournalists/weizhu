package com.weizhu.webapp.boss;

import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import com.weizhu.proto.BossProtos.BossAnonymousHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.web.filter.BossSessionFilter;
import com.weizhu.webapp.boss.api.GetBossConfigServlet;
import com.weizhu.webapp.boss.api.LoginServlet;
import com.weizhu.webapp.boss.api.LogoutServlet;
import com.weizhu.webapp.boss.api.company.GetCompanyListServlet;
import com.weizhu.webapp.boss.api.discover.CreateItemServlet;
import com.weizhu.webapp.boss.api.discover.GetAuthUrlServlet;
import com.weizhu.webapp.boss.api.discover.GetItemListServlet;
import com.weizhu.webapp.boss.api.discover.GetModuleServlet;
import com.weizhu.webapp.boss.api.profile.GetProfileServlet;
import com.weizhu.webapp.boss.api.profile.SetProfileServlet;
import com.weizhu.webapp.boss.api.stats.LoadDimServlet;

public class BossServletModule extends ServletModule {
	
	public BossServletModule() {
	}
	
	@Override
	protected void configureServlets() {
		filter("/*").through(BossSessionFilter.class);
		filter("/*").through(WebappBossFilter.class);
		
		serve("/api/login.json").with(LoginServlet.class);
		serve("/api/logout.json").with(LogoutServlet.class);
		serve("/api/get_boss_config.json").with(GetBossConfigServlet.class);
		
		serve("/api/company/get_company_list.json").with(GetCompanyListServlet.class);
		
		serve("/api/discover/get_module.json").with(GetModuleServlet.class);
		serve("/api/discover/get_item_list.json").with(GetItemListServlet.class);
		serve("/api/discover/create_item.json").with(CreateItemServlet.class);
		serve("/api/discover/get_auth_url.json").with(GetAuthUrlServlet.class);
		
		serve("/api/profile/get_profile.json").with(GetProfileServlet.class);
		serve("/api/profile/set_profile.json").with(SetProfileServlet.class);
		
		serve("/api/stats/load_dim.json").with(LoadDimServlet.class);
	}
	
	@Provides
	@RequestScoped
	public BossHead provideBossHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public BossAnonymousHead provideBossAnonymousHead() {
		return null;
	}

}
