package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetExamStatisticsResponse.ExamStatistics;
import com.weizhu.proto.AdminExamProtos.StatisticalParams;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class ExamStatisticsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;

	@Inject
	public ExamStatisticsServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final List<Integer> examIdList = ParamUtil.getIntList(httpRequest, "exam_id_list", Collections.emptyList());
		
		final AdminHead head = adminHeadProvider.get();
		
		GetExamStatisticsResponse response = Futures.getUnchecked(adminExamService.getExamStatistics(head, GetExamStatisticsRequest.newBuilder()
				.addAllExamId(examIdList)
				.build()));
		
		JsonArray array = new JsonArray();
		for (ExamStatistics examStatistics : response.getExamStatisticsList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("exam_id", examStatistics.getExamId());
			if (examStatistics.hasStatisticalParams()) {
				StatisticalParams statisticalParams = examStatistics.getStatisticalParams();
				UserInfoUtil.getStatisticParams(obj, statisticalParams);
			}
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("exam_statistics", array);
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
