package com.weizhu.webapp.mobile.credits;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.CreditsProtos.DuibaNotifyRequest;
import com.weizhu.proto.CreditsProtos.DuibaNotifyResponse;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.CreditsService;
import com.weizhu.web.ParamUtil;

@Singleton
public class DuibaNotifyServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Provider<AnonymousHead> anonymousHeadProvider;
	private CreditsService creditsService;
	
	@Inject
	public DuibaNotifyServlet(Provider<AnonymousHead> anonymousHeadProvider, CreditsService creditsService) {
		this.anonymousHeadProvider = anonymousHeadProvider;
		this.creditsService = creditsService;
	}
	
	private final static Splitter SPLITTER = Splitter.on(':').trimResults().omitEmptyStrings();
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String bizId = ParamUtil.getString(httpRequest, "bizId", ""); // company_id + ":" + order_id
		
		Long companyId;
		try {
			List<String> list = SPLITTER.splitToList(bizId);
			companyId = list.size() > 0 ? Long.parseLong(list.get(0)) : null;
		} catch (NumberFormatException e) {
			companyId = null;
		}
		
		if (companyId == null) {
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid arg uid");
			return;
		}
		
		final String appKey = ParamUtil.getString(httpRequest, "appKey", "");
		final long timestamp = ParamUtil.getLong(httpRequest, "timestamp", 0L);
		final boolean success = ParamUtil.getBoolean(httpRequest, "success", false);
		final String errorMessage = ParamUtil.getString(httpRequest, "errorMessage", null);
		final String orderNum = ParamUtil.getString(httpRequest, "orderNum", "");
		final String uid = ParamUtil.getString(httpRequest, "uid", "");
		
		final String sign = ParamUtil.getString(httpRequest, "sign", "");
		
		DuibaNotifyRequest.Builder requestBuilder = DuibaNotifyRequest.newBuilder()
				.setAppKey(appKey)
				.setTimeStamp(timestamp)
				.setSuccess(success)
				.setOrderNum(orderNum)
				.setSign(sign)
				.setUid(uid)
				.setBizId(bizId);
		
		if (errorMessage != null) {
			requestBuilder.setErrorMessage(errorMessage);
		}
		
		final AnonymousHead head = anonymousHeadProvider.get().toBuilder()
				.setCompanyId(companyId)
				.build();
		
		DuibaNotifyResponse response = Futures.getUnchecked(creditsService.duibaNotify(head, requestBuilder.build()));
	
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(response.getResult(), httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
