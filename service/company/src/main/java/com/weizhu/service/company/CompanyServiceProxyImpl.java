package com.weizhu.service.company;

import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.weizhu.common.CommonProtos;
import com.weizhu.common.CommonProtos.RpcRequestPacket;
import com.weizhu.common.CommonProtos.RpcResponsePacket;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.influxdb.InfluxDBReporter;
import com.weizhu.common.rpc.AutoSwitchRpcClient;
import com.weizhu.common.rpc.RpcInvoker;
import com.weizhu.common.service.AsyncImpl;
import com.weizhu.common.service.ServiceInvoker;
import com.weizhu.common.service.ServiceStub;
import com.weizhu.common.service.exception.HeadUnknownException;
import com.weizhu.common.service.exception.InvokeUnknownException;
import com.weizhu.common.service.exception.RequestParseException;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.BossProtos.BossHead;
import com.weizhu.proto.CompanyProtos.GetCompanyListResponse;
import com.weizhu.proto.CompanyProtos.GetCompanyResponse;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyRequest;
import com.weizhu.proto.CompanyProtos.VerifyCompanyKeyResponse;
import com.weizhu.proto.CompanyProtos;
import com.weizhu.proto.CompanyService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.proto.WeizhuProtos.SystemHead;
import com.zaxxer.hikari.HikariDataSource;

public class CompanyServiceProxyImpl implements RpcInvoker, CompanyService {

	private static final Logger logger = LoggerFactory.getLogger(CompanyServiceProxyImpl.class);
	
	private final ImmutableMap<Long, CompanyProtos.Company> companyMap;
	private final ImmutableMap<String, CompanyProtos.Company> companyKeyMap;
	@SuppressWarnings("unused")
	private final ImmutableMap<String, CompanyProtos.Server> serverMap;
	private final ImmutableMap<String, AutoSwitchRpcClient> serverClientMap;
	
	private final ServiceInvoker companyServiceInvoker;
	
	@Inject
	public CompanyServiceProxyImpl(
			@Named("service_executor") Executor serviceExecutor, 
			HikariDataSource hikariDataSource, NioEventLoopGroup eventLoop,
			@Nullable InfluxDBReporter influxDBReporter) {
		
		Map<Long, CompanyProtos.Company> companyMap;
		Map<String, CompanyProtos.Server> serverMap;
		Connection dbConn = null;
		try {
			dbConn = hikariDataSource.getConnection();
			companyMap = CompanyDB.getCompany(dbConn);
			serverMap = CompanyDB.getServer(dbConn);
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
		
		this.serverMap = ImmutableMap.copyOf(serverMap);
		
		Map<String, AutoSwitchRpcClient> serverClientMap = Maps.newTreeMap();
		for (CompanyProtos.Server server : serverMap.values()) {
			if (server.getAddressCount() <= 0) {
				continue;
			}
			
			List<InetSocketAddress> addrList = new ArrayList<InetSocketAddress>(server.getAddressCount());
			for (CompanyProtos.Server.Address addr : server.getAddressList()) {
				addrList.add(new InetSocketAddress(addr.getHost(), addr.getPort()));
			}
			serverClientMap.put(server.getServerName(), new AutoSwitchRpcClient(addrList, eventLoop));
		}
		this.serverClientMap = ImmutableMap.copyOf(serverClientMap);
		
		this.companyServiceInvoker = ServiceStub.createServiceInvoker(CompanyService.class, this, serviceExecutor, influxDBReporter);
		
		logger.info("load company " + companyMap.size() + ", server " + serverMap.size());
	}
	
	@Override
	public ListenableFuture<RpcResponsePacket> invoke(RpcRequestPacket requestPacket) {
		Long companyId = null;
		Message head = null;
		switch (requestPacket.getHeadCase()) {
			case SYSTEM_HEAD:
				head = requestPacket.getSystemHead();
				if (requestPacket.getSystemHead().hasCompanyId()) {
					companyId = requestPacket.getSystemHead().getCompanyId();
				}
				break;
			case REQUEST_HEAD:
				companyId = requestPacket.getRequestHead().getSession().getCompanyId();
				head = requestPacket.getRequestHead();
				break;
			case ANONYMOUS_HEAD:
				if (requestPacket.getAnonymousHead().hasCompanyId()) {
					companyId = requestPacket.getAnonymousHead().getCompanyId();
				}
				head = requestPacket.getAnonymousHead();
				break;
			case ADMIN_HEAD:
				if (requestPacket.getAdminHead().hasCompanyId()) {
					companyId = requestPacket.getAdminHead().getCompanyId();
				}
				head = requestPacket.getAdminHead();
				break;
			case ADMIN_ANONYMOUS_HEAD:
				if (requestPacket.getAdminAnonymousHead().hasCompanyId()) {
					companyId = requestPacket.getAdminAnonymousHead().getCompanyId();
				}
				head = requestPacket.getAdminAnonymousHead();
				break;
			case BOSS_HEAD:
				if (requestPacket.getBossHead().hasCompanyId()) {
					companyId = requestPacket.getBossHead().getCompanyId();
				}
				head = requestPacket.getBossHead();
				break;
			case BOSS_ANONYMOUS_HEAD:
				if (requestPacket.getBossAnonymousHead().hasCompanyId()) {
					companyId = requestPacket.getBossAnonymousHead().getCompanyId();
				}
				head = requestPacket.getBossAnonymousHead();
				break;	
			default:
				break;
		}
		
		if (head == null) {
			return Futures.immediateFuture(CommonProtos.RpcResponsePacket.newBuilder()
					.setInvokeId(0)
					.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_HEAD_UNKNOWN)
					.setFailText("head : " + requestPacket.getHeadCase())
					.build());
		}
		
		if ("CompanyService".equals(requestPacket.getServiceName())) {
			ListenableFuture<ByteString> responseFuture;
			try {
				responseFuture = companyServiceInvoker.invoke(requestPacket.getFunctionName(), head, requestPacket.getRequestBody());
			} catch (Throwable th) {
				responseFuture = Futures.immediateFailedFuture(th);
			}
			
			return Futures.catching(Futures.transform(responseFuture, SUCC_FUNCTION), Throwable.class, FAIL_FALLBACK);
		}
		
		if (companyId == null) {
			return Futures.immediateFuture(CommonProtos.RpcResponsePacket.newBuilder()
					.setInvokeId(0)
					.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_HEAD_UNKNOWN)
					.setFailText("head : " + requestPacket.getHeadCase())
					.build());
		}
		
		CompanyProtos.Company company = this.companyMap.get(companyId);
		AutoSwitchRpcClient rpcClient = company == null ? null : this.serverClientMap.get(company.getServerName());
		if (rpcClient == null) {
			
			logger.warn("company not found, companyId=" + companyId + ", head_type=" + (head == null ? "null" : head.getClass().getName()) + ", head=" + head);
			
			return Futures.immediateFuture(CommonProtos.RpcResponsePacket.newBuilder()
					.setInvokeId(0)
					.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_SERVER_EXCEPTION)
					.setFailText("company not found")
					.build());
		}
		
		return rpcClient.invoke(requestPacket);
	}
	
	private static final Function<ByteString, CommonProtos.RpcResponsePacket> SUCC_FUNCTION = new Function<ByteString, CommonProtos.RpcResponsePacket>() {

		@Override
		public CommonProtos.RpcResponsePacket apply(ByteString responseBody) {
			return CommonProtos.RpcResponsePacket.newBuilder()
					.setInvokeId(0)
					.setResult(CommonProtos.RpcResponsePacket.Result.SUCC)
					.setResponseBody(responseBody)
					.build();
		}
		
	};
	
	private static final Function<Throwable, CommonProtos.RpcResponsePacket> FAIL_FALLBACK = new Function<Throwable, CommonProtos.RpcResponsePacket>() {

		@Override
		public CommonProtos.RpcResponsePacket apply(Throwable t) {
			if (t instanceof InvokeUnknownException) {
				return CommonProtos.RpcResponsePacket.newBuilder()
					.setInvokeId(0)
					.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_INVOKE_UNKNOWN)
					.setFailText(t.getMessage())
					.build();
			} else if (t instanceof HeadUnknownException) {
				return CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_HEAD_UNKNOWN)
						.setFailText(t.getMessage())
						.build();
			} else if (t instanceof RequestParseException) {
				return CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_BODY_PARSE_FAIL)
						.setFailText(t.getMessage())
						.build();
			} else {
				return CommonProtos.RpcResponsePacket.newBuilder()
						.setInvokeId(0)
						.setResult(CommonProtos.RpcResponsePacket.Result.FAIL_SERVER_EXCEPTION)
						.setFailText(t.getMessage())
						.build();
			}
		}
		
	};
	
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
	@AsyncImpl
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
