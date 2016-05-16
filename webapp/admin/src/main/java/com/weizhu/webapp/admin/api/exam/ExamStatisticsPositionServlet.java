package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetPositionStatisticsResponse.PositionStatistics;
import com.weizhu.proto.AdminExamProtos.StatisticalParams;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetPositionResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class ExamStatisticsPositionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminUserService adminUserService;

	@Inject
	public ExamStatisticsPositionServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int examId = ParamUtil.getInt(httpRequest, "exam_id", 0);
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 50);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetPositionStatisticsResponse response = Futures.getUnchecked(adminExamService.getPositionStatistics(head, GetPositionStatisticsRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(length)
				.build()));
		
		GetPositionResponse getPositionResponse = Futures.getUnchecked(adminUserService.getPosition(head, EmptyRequest.getDefaultInstance()));
		Map<Integer, Position> positionMap = Maps.newHashMap();
		for (Position position : getPositionResponse.getPositionList()) {
			positionMap.put(position.getPositionId(), position);
		}
		
		JsonArray array = new JsonArray();
		for (PositionStatistics positionStatistics : response.getPostionStatisticsList()) {
			JsonObject obj = new JsonObject();
			int positionId = positionStatistics.getPositionId();
			obj.addProperty("position_id", positionId);
			obj.addProperty("position_name", positionMap.get(positionId) == null ? "【未知职位】" : positionMap.get(positionId).getPositionName());
			if (positionStatistics.hasStatisticalParams()) {
				StatisticalParams statisticalParams = positionStatistics.getStatisticalParams();
				UserInfoUtil.getStatisticParams(obj, statisticalParams);
			}
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("position_statistics", array);
		result.addProperty("total", response.getTotal());
		result.addProperty("filtered_size", response.getFilteredSize());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
