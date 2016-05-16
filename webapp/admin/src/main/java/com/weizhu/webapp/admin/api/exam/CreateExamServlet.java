package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.CreateExamRequest;
import com.weizhu.proto.AdminExamProtos.CreateExamResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.ExamProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateExamServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(CreateExamServlet.class);

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;

	@Inject
	public CreateExamServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final String examName = ParamUtil.getString(httpRequest, "exam_name", "");
		final String imageName = ParamUtil.getString(httpRequest, "image_name", null);
		final String startTimeStr = ParamUtil.getString(httpRequest, "start_time", null);
		final String endTimeStr = ParamUtil.getString(httpRequest, "end_time", null);
		final Integer passMark = ParamUtil.getInt(httpRequest, "pass_mark", 0);
		
		final String typeParam = ParamUtil.getString(httpRequest, "type", "MANUAL");
		ExamProtos.Exam.Type type = null;
		for (ExamProtos.Exam.Type t : ExamProtos.Exam.Type.values()) {
			if (t.name().equals(typeParam)) {
				type = t;
			}
		}
		
		final List<String> userIdStrList = DBUtil.COMMA_SPLITTER.splitToList(ParamUtil.getString(httpRequest, "user_id", ""));
		List<Long> userIdList = new ArrayList<Long>();
		try {
			// parse userId
			for (String userIdStr : userIdStrList) {
				userIdList.add(Long.parseLong(userIdStr));
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_USER_NOT_EXIST");
			result.addProperty("fail_text", "用户编号不合法！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final List<String> teamIdStrList = DBUtil.COMMA_SPLITTER.splitToList(ParamUtil.getString(httpRequest, "team_id", ""));
		List<Integer> teamIdList = new ArrayList<Integer>();
		try {
			// parse userId
			for (String teamIdStr : teamIdStrList) {
				teamIdList.add(Integer.parseInt(teamIdStr));
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_TEAM_NOT_EXIST");
			result.addProperty("fail_text", "组织编号不合法！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		final AdminHead head = adminHeadProvider.get();
		
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
		
		final Integer allowModelId = ParamUtil.getInt(httpRequest, "allow_model_id", 0);
		CreateExamRequest.Builder requestBuilder = CreateExamRequest.newBuilder()
				.setExamName(examName)
				.setStartTime(startTime)
				.setEndTime(endTime)
				.setPassMark(passMark)
				.setAllowModelId(allowModelId)
				.setShowResult(showResult)
				.setType(type);
		if (imageName != null) {
			requestBuilder.setImageName(imageName);
		}
		
		CreateExamResponse response = Futures.getUnchecked(adminExamService.createExam(head, requestBuilder.build()));
		
		try {
			adminExamService.loadExamSubmitTask(head, ServiceUtil.EMPTY_REQUEST);
		} catch (Exception ex) {
			// 有错误提示，不结束
			logger.info("load exam submit task : " + examName);
		}
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
