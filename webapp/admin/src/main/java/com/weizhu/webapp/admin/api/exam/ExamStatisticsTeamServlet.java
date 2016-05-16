package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse.TeamStatistics;
import com.weizhu.proto.AdminExamProtos.StatisticalParams;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.web.ParamUtil;

@Singleton
public class ExamStatisticsTeamServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminUserService adminUserService;

	@Inject
	public ExamStatisticsTeamServlet(Provider<AdminHead> adminHeadProvider,
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
		
		GetTeamStatisticsRequest.Builder requestBuilder = GetTeamStatisticsRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(length);
		
		// team_id中间用“,”分隔
		final String teamId = ParamUtil.getString(httpRequest, "team_id", null);
		
		if (teamId != null && !teamId.isEmpty()) {
			requestBuilder.setTeamId(teamId);
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		GetTeamStatisticsResponse response = Futures.getUnchecked(adminExamService.getTeamStatistics(head, requestBuilder.build()));
		Set<Integer> teamIdSet = Sets.newTreeSet();
		for (TeamStatistics teamStatistics : response.getTeamStatisticsList()) {
			teamIdSet.addAll(teamStatistics.getTeamIdList());
		}
		
		GetTeamByIdResponse getTeamByIdResponse = Futures.getUnchecked(adminUserService.getTeamById(head, GetTeamByIdRequest.newBuilder()
				.addAllTeamId(teamIdSet)
				.build()));
		Map<Integer, Team> teamMap = Maps.newHashMap();
		for (Team team : getTeamByIdResponse.getTeamList()) {
			teamMap.put(team.getTeamId(), team);
		}
		
		JsonArray array = new JsonArray();
		for (TeamStatistics teamStatistics : response.getTeamStatisticsList()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("team_id", DBUtil.COMMA_JOINER.join(teamStatistics.getTeamIdList()));
			Iterator<Integer> it = teamStatistics.getTeamIdList().iterator();
			StringBuilder teamName = new StringBuilder();
			while (it.hasNext()) {
				int id = it.next();
				String name = teamMap.get(id) == null ? "【未知部门】" : teamMap.get(id).getTeamName();
				teamName.append(name).append("/");
			}
			obj.addProperty("team_name", teamName.toString());
			if (teamStatistics.hasStatisticalParams()) {
				StatisticalParams statisticalParams = teamStatistics.getStatisticalParams();
				UserInfoUtil.getStatisticParams(obj, statisticalParams);
			}
			array.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("team_statistics", array);
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
