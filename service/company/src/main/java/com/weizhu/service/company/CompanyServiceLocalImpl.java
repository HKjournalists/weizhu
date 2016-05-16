package com.weizhu.service.company;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.CompanyProtos.GetCompanyListResponse;
import com.weizhu.proto.CompanyProtos.GetCompanyResponse;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyRequest;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyResponse;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

import io.netty.channel.nio.NioEventLoopGroup;

public class CompanyServiceLocalImpl implements CompanyService {

	private static final Logger logger = LoggerFactory.getLogger(CompanyServiceLocalImpl.class);
	
	private final ImmutableMap<Long, CompanyProtos.Company> companyMap;
	private final ImmutableMap<String, CompanyProtos.Company> companyKeyMap;
	
	@Inject
	public CompanyServiceLocalImpl(
			@Named("service_executor") Executor serviceExecutor, 
			HikariDataSource hikariDataSource, NioEventLoopGroup eventLoop) {
		
		Map<Long, CompanyProtos.Company> companyMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			companyMap = CompanyDB.getCompany(dbConn);
		} catch (SQLException e) {
			logger.error("load company fail", e);
			throw new Error(e);
		} finally {
			DBUtil.closeQuietly(dbConn);
		}
		
		this.companyMap = ImmutableMap.copyOf(companyMap);
		
		Map<String, CompanyProtos.Company> companyKeyMap = Maps.newTreeMap();
		for (CompanyProtos.Company company : companyMap.values()) {
			for (String companyKey : company.getCompanyKeyList()) {
				companyKeyMap.put(companyKey, company);
			}
		}
		this.companyKeyMap = ImmutableMap.copyOf(companyKeyMap);
		
		logger.info("load company " + companyMap.size());
	}

	@Override
	@AsyncImpl
	public ListenableFuture<VerifyCompanyKeyResponse> verifyCompanyKey(AnonymousHead head, VerifyCompanyKeyRequest request) {
		final CompanyProtos.Company company = this.companyKeyMap.get(request.getCompanyKey());
		if (company == null) {
			return Futures.immediateFuture(VerifyCompanyKeyResponse.newBuilder()
					.build());
		}
		
		return Futures.immediateFuture(VerifyCompanyKeyResponse.newBuilder()
				.setCompany(company)
				.build());
	}
	
	private static final ListenableFuture<GetCompanyResponse> GET_COMPANY_EMPTY_RESPONSE = 
			Futures.immediateFuture(GetCompanyResponse.newBuilder().build());
	
	private ListenableFuture<GetCompanyResponse> doGetCompany(boolean hasCompanyId, long companyId) {
		if (!hasCompanyId) {
			return GET_COMPANY_EMPTY_RESPONSE;
		}
		
		CompanyProtos.Company company = this.companyMap.get(companyId);
		if (company == null) {
			return GET_COMPANY_EMPTY_RESPONSE;
		}
		return Futures.immediateFuture(GetCompanyResponse.newBuilder()
				.setCompany(company)
				.build());
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<GetCompanyResponse> getCompany(AnonymousHead head, EmptyRequest request) {
		return this.doGetCompany(head.hasCompanyId(), head.getCompanyId());
	}

	@Override
	@AsyncImpl
	public ListenableFuture<GetCompanyResponse> getCompany(RequestHead head, EmptyRequest request) {
		return this.doGetCompany(true, head.getSession().getCompanyId());
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<GetCompanyListResponse> getCompanyList(AdminAnonymousHead head, EmptyRequest request) {
		return Futures.immediateFuture(GetCompanyListResponse.newBuilder()
				.addAllCompany(this.companyMap.values())
				.build());
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<GetCompanyListResponse> getCompanyList(AdminHead head, EmptyRequest request) {
		return Futures.immediateFuture(GetCompanyListResponse.newBuilder()
				.addAllCompany(this.companyMap.values())
				.build());
	}
	
	@Override
	public ListenableFuture<GetCompanyListResponse> getCompanyList(BossHead head, EmptyRequest request) {
		return Futures.immediateFuture(GetCompanyListResponse.newBuilder()
				.addAllCompany(this.companyMap.values())
				.build());
	}
	
	@Override
	@AsyncImpl
	public ListenableFuture<GetCompanyListResponse> getCompanyList(SystemHead head, EmptyRequest request) {
		return Futures.immediateFuture(GetCompanyListResponse.newBuilder()
				.addAllCompany(this.companyMap.values())
				.build());
	}

}
