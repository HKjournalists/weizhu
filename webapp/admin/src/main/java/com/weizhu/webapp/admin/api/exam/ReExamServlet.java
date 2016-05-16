package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.ReExamRequest;
import com.weizhu.proto.AdminExamProtos.ReExamResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class ReExamServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(ReExamServlet.class);

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public ReExamServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		final String examName = ParamUtil.getString(httpRequest, "exam_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final String startTimeStr = ParamUtil.getString(httpRequest, "start_time", "");
		final String endTimeStr = ParamUtil.getString(httpRequest, "end_time", "");
		
		int startTime = 0;
		int endTime = 0;
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try {
			startTime = (int) (df.parse(startTimeStr).getTime() / 1000L);
			endTime = (int) (df.parse(endTimeStr).getTime() / 1000L);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_TIME_INVALID");
			result.addProperty("fail_text", "请传入正确的时间！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		ReExamRequest.Builder reExamReqeustBuilder = ReExamRequest.newBuilder()
				.setExamId(examId)
				.setExamName(examName)
				.setStartTime(startTime)
				.setEndTime(endTime);
		
		if (imageName != null) {
			reExamReqeustBuilder.setImageName(imageName);
		}
		
		final AdminHead head = adminHeadProvider.get();
		ReExamResponse reExamResponse = Futures.getUnchecked(adminExamService.reExam(head, reExamReqeustBuilder.build()));
		
		try {
			adminExamService.loadExamSubmitTask(head, ServiceUtil.EMPTY_REQUEST).get();
		} catch (Exception ex) {
			// 有错误提示，不结束
			logger.info("load exam submit task : " + examName);
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(reExamResponse, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
