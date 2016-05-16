package com.weizhu.webapp.demo;

import com.google.inject.servlet.ServletModule;
import com.weizhu.webapp.demo.home.CreateUserInfoServlet;
import com.weizhu.webapp.demo.home.DeleteUserInfoServlet;
import com.weizhu.webapp.demo.home.DownloadUserInfoServlet;
import com.weizhu.webapp.demo.home.GetUserInfoServlet;
import com.weizhu.webapp.demo.home.UpdateUserInfoServlet;

public class DemoServletModule extends ServletModule {

	public DemoServletModule() {
	}
	
	@Override
	protected void configureServlets() {
		serve("/api/home/create_user_info.json").with(CreateUserInfoServlet.class);
		serve("/api/home/delete_user_info.json").with(DeleteUserInfoServlet.class);
		serve("/api/home/download_user_info.json").with(DownloadUserInfoServlet.class);
		serve("/api/home/get_user_info.json").with(GetUserInfoServlet.class);
		serve("/api/home/update_user_info.json").with(UpdateUserInfoServlet.class);
	}
	
}
