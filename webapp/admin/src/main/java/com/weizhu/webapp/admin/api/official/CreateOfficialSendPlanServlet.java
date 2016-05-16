package com.weizhu.webapp.admin.api.official;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialSendPlanRequest;
import com.weizhu.proto.AdminOfficialProtos.CreateOfficialSendPlanResponse;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminOfficialService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.OfficialProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class CreateOfficialSendPlanServlet extends HttpServlet {
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminOfficialService adminOfficialService;
	
	@Inject
	public CreateOfficialSendPlanServlet(Provider<AdminHead> adminHeadProvider, AdminOfficialService adminOfficialService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminOfficialService = adminOfficialService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		// 1. 取出参数
		long officialId = ParamUtil.getLong(httpRequest, "official_id", 0L);
		
		String sendMsgTextContent = ParamUtil.getString(httpRequest, "send_msg_text_content", "");
		String sendMsgImageName = ParamUtil.getString(httpRequest, "send_msg_image_name", null);
		Long sendMsgUserUserId = ParamUtil.getLong(httpRequest, "send_msg_user_user_id", null);
		
		boolean isSendImmediately = ParamUtil.getBoolean(httpRequest, "is_send_immediately", false);
		Integer sendTime = ParamUtil.getInt(httpRequest, "send_time", null);
		
		Integer modelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		// 2. 调用Service
		
		final AdminHead head = adminHeadProvider.get();
		
		OfficialProtos.OfficialMessage.Builder sendMsgBuilder = OfficialProtos.OfficialMessage.newBuilder();
		sendMsgBuilder.setMsgSeq(0);
		sendMsgBuilder.setMsgTime(0);
		sendMsgBuilder.setIsFromUser(false);
		if (sendMsgUserUserId != null) {
			sendMsgBuilder.setUser(OfficialProtos.OfficialMessage.User.newBuilder().setUserId(sendMsgUserUserId).build());
		} else if (sendMsgImageName != null) {
			sendMsgBuilder.setImage(OfficialProtos.OfficialMessage.Image.newBuilder().setName(sendMsgImageName).build());
		} else {
			sendMsgBuilder.setText(OfficialProtos.OfficialMessage.Text.newBuilder().setContent(sendMsgTextContent).build());
		}
		
		CreateOfficialSendPlanRequest.Builder requestBuilder = CreateOfficialSendPlanRequest.newBuilder()
				.setOfficialId(officialId)
				.setSendMsg(sendMsgBuilder.build())
				.setIsSendImmediately(isSendImmediately);
		
		if (modelId != null) {
			requestBuilder.setAllowModelId(modelId);
		}
		if (sendTime != null) {
			requestBuilder.setSendTime(sendTime);
		}
		
		CreateOfficialSendPlanResponse response = Futures.getUnchecked(adminOfficialService.createOfficialSendPlan(head, requestBuilder.build()));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}

}
