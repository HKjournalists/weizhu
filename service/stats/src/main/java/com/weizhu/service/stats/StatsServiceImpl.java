package com.weizhu.service.stats;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.proto.AdminDiscoverService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.StatsService;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.EmptyResponse;
import com.weizhu.service.stats.dim.LoadDimDateTask;
import com.weizhu.service.stats.dim.LoadDimDiscoverTask;
import com.weizhu.service.stats.dim.LoadDimUserTask;
import com.zaxxer.hikari.HikariDataSource;

public class StatsServiceImpl implements StatsService {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(StatsServiceImpl.class);
	
	@SuppressWarnings("unused")
	private final Executor serviceExecutor;
	private final HikariDataSource hikariDataSource;
	private final CompanyService companyService;
	private final AdminUserService adminUserService;
	private final AdminDiscoverService adminDiscoverService;
	
	@Inject
	public StatsServiceImpl(
			@Named("service_executor") Executor serviceExecutor,
			HikariDataSource hikariDataSource, 
			CompanyService companyService, 
			AdminUserService adminUserService, 
			AdminDiscoverService adminDiscoverService
			) {
		this.serviceExecutor = serviceExecutor;
		this.hikariDataSource = hikariDataSource;
		this.companyService = companyService;
		this.adminUserService = adminUserService;
		this.adminDiscoverService = adminDiscoverService;
	}

	@Override
	public ListenableFuture<EmptyResponse> loadDimDate(BossHead head, EmptyRequest request) {
		new LoadDimDateTask(this.hikariDataSource).run();
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> loadDimUser(BossHead head, EmptyRequest request) {
		new LoadDimUserTask(this.hikariDataSource, this.companyService, this.adminUserService).run();
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}

	@Override
	public ListenableFuture<EmptyResponse> loadDimDiscover(BossHead head, EmptyRequest request) {
		new LoadDimDiscoverTask(this.hikariDataSource, this.companyService, this.adminDiscoverService).run();
		return ServiceUtil.EMPTY_RESPONSE_IMMEDIATE_FUTURE;
	}
	
}