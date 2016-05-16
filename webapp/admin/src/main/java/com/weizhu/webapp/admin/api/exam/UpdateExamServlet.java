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
import com.weizhu.proto.AdminExamProtos.UpdateExamRequest;
import com.weizhu.proto.AdminExamProtos.UpdateExamResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateExamServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(UpdateExamServlet.class);
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public UpdateExamServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		final String examName = ParamUtil.getString(httpRequest, "exam_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final String startTimeStr = ParamUtil.getString(httpRequest, "start_time", "");
		final String endTimeStr = ParamUtil.getString(httpRequest, "end_time", "");
		final Integer passMark = ParamUtil.getInt(httpRequest, "pass_mark", 0);
		
		final Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", null);
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		int startTime = 0;
		int endTime = 0;
		try {
			startTime = (int) (df.parse(startTimeStr).getTime() / 1000L);
			endTime = (int) (df.parse(endTimeStr).getTime() / 1000L);
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_EXAM_TIME_INVALID");
			result.addProperty("fail_text", "时间格式不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final String showResultParam = ParamUtil.getString(httpRequest, "show_result", "AFTER_EXAM_END");
		ExamProtos.ShowResult showResult = null;
		for (ExamProtos.ShowResult sr : ExamProtos.ShowResult.values()) {
			if (sr.name().equals(showResultParam)) {
				showResult = sr;
			}
		}
		
		if (showResult == null) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_SHOW_RESULT_INVALID");
			result.addProperty("fail_text", "显示答案类型不正确！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		UpdateExamRequest.Builder requestBuilder = UpdateExamRequest.newBuilder()
				.setExamId(examId)
				.setExamName(examName)
				.setStartTime(startTime)
				.setEndTime(endTime)
				.setPassMark(passMark)
				.setAllowModelId(allowModelId)
				.setShowResult(showResult);
		
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		if (allowModelId != null) {
			requestBuilder.setAllowModelId(allowModelId);
		}
		
		UpdateExamResponse response = Futures.getUnchecked(adminExamService.updateExam(head, requestBuilder.build()));
		
		try {
			adminExamService.loadExamSubmitTask(head, ServiceUtil.EMPTY_REQUEST).get();
		} catch (Exception ex) {
			// 有错误提示，不结束
			logger.info("load exam submit task : " + examName);
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
