package com.weizhu.webapp.mobile.credits;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsRequest;
import com.weizhu.proto.CreditsProtos.DuibaConsumeCreditsResponse;
import com.weizhu.proto.CreditsService;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class DuibaConsumeCreditsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Provider<AnonymousHead> anonymousHeadProvider;
	private CreditsService creditsService;
	
	@Inject
	public DuibaConsumeCreditsServlet(Provider<AnonymousHead> anonymousHeadProvider, CreditsService creditsService) {
		this.anonymousHeadProvider = anonymousHeadProvider;
		this.creditsService = creditsService;
	}
	
	private static final Splitter SPLITTER = Splitter.on(':').trimResults().omitEmptyStrings();
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String uid = ParamUtil.getString(httpRequest, "uid", ""); // company_id + ":" + user_id
		
		Long companyId;
		Long userId;
		try {
			List<String> list = SPLITTER.splitToList(uid);
			companyId = list.size() > 0 ? Long.parseLong(list.get(0)) : null;
			userId = list.size() > 1 ? Long.parseLong(list.get(1)) : null;
		} catch (NumberFormatException e) {
			companyId = null;
			userId = null;
		}
		
		if (companyId == null || userId == null) {
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid arg uid");
			return;
		}

		final long credits = ParamUtil.getLong(httpRequest, "credits", 0L);
		final String appKey = ParamUtil.getString(httpRequest, "appKey", "");
		final String timestamp = ParamUtil.getString(httpRequest, "timestamp", "");
		final String description = ParamUtil.getString(httpRequest, "description", null);
		final String orderNum = ParamUtil.getString(httpRequest, "orderNum", "");
		final String type = ParamUtil.getString(httpRequest, "type", "");
		final Integer facePrice = ParamUtil.getInt(httpRequest, "facePrice", null);
		final int actualPrice = ParamUtil.getInt(httpRequest, "actualPrice", 0);
		final String ip = ParamUtil.getString(httpRequest, "ip", null);
		final Boolean waitAudit = ParamUtil.getBoolean(httpRequest, "waitAudit", null);
		final String params = ParamUtil.getString(httpRequest, "params", null);
		final String sign = ParamUtil.getString(httpRequest, "sign", "");

		DuibaConsumeCreditsRequest.Builder requestBuilder = DuibaConsumeCreditsRequest.newBuilder()
				.setUid(uid)
				.setCredits(credits)
				.setAppKey(appKey)
				.setTimeStamp(timestamp)
				.setOrderNum(orderNum)
				.setType(type)
				.setActualPrice(actualPrice)
				.setSign(sign);
		if (description != null) {
			requestBuilder.setDescription(description);
		}
		if (facePrice != null) {
			requestBuilder.setFacePrice(facePrice);
		}
		if (ip != null) {
			requestBuilder.setIp(ip);
		}
		if (waitAudit != null) {
			requestBuilder.setWaitAudit(waitAudit);
		}
		if (params != null) {
			requestBuilder.setParams(params);
		}
		
		final AnonymousHead head = anonymousHeadProvider.get().toBuilder()
				.setCompanyId(companyId)
				.build();
		
		DuibaConsumeCreditsResponse response = Futures.getUnchecked(
				creditsService.duibaConsumeCredits(head, requestBuilder.build()));
		
		JsonObject result = new JsonObject();
		result.addProperty("status", response.getStatus());
		result.addProperty("bizId", response.getBizId());
		result.addProperty("credits", response.getCredits());
		if (response.getErrorMessage() != null) {
			result.addProperty("errorMessage", response.getErrorMessage());
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
