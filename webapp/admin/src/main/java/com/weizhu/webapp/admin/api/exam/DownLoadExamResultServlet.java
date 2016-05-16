package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.GetExamByIdRequest;
import com.weizhu.proto.AdminExamProtos.GetExamByIdResponse;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultRequest;
import com.weizhu.proto.AdminExamProtos.GetExamUserResultResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.AllowProtos;
import com.weizhu.proto.AllowProtos.CheckAllowRequest;
import com.weizhu.proto.AllowProtos.CheckAllowResponse;
import com.weizhu.proto.AllowService;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.UserResult;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class DownLoadExamResultServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminUserService adminUserService;
	private final AllowService allowService;

	@Inject
	public DownLoadExamResultServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService, AdminUserService adminUserService,
			AllowService allowService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminUserService = adminUserService;
		this.allowService = allowService;
	}

	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		final AdminHead head = adminHeadProvider.get();

		GetExamByIdRequest getExamByIdRequest = GetExamByIdRequest.newBuilder().addAllExamId(Collections.singleton(examId)).build();
		GetExamByIdResponse getExamByIdResponse = Futures.getUnchecked(adminExamService.getExamById(head, getExamByIdRequest));
		if (getExamByIdResponse.getExamCount() <= 0) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_EXAM_NOT_EXIST");
			result.addProperty("fail_text", "没获取到此次考试信息！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
			return ;
		}
		
		ExamProtos.Exam exam = getExamByIdResponse.getExamList().get(0);
		
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, exam);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("exam_result.xlsx".getBytes("utf-8"),"iso8859-1"));
			
			wb.write(httpResponse.getOutputStream());
		} catch (Exception ex) {
			
		} finally {
			if (wb != null) {
				wb.close();
			}
		}
		
	}

	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private void writeExecl(SXSSFWorkbook wb, ExamProtos.Exam exam) {
		final AdminHead head = adminHeadProvider.get();
		
		int length = 100;
		GetExamUserResultRequest.Builder getExamUserResultRquestBuilder = GetExamUserResultRequest.newBuilder()
				.setExamId(exam.getExamId())
				.setStart(0)
				.setLength(length);
		GetExamUserResultResponse getExamUserResultResponse = Futures.getUnchecked(adminExamService.getExamUserResult(head, getExamUserResultRquestBuilder.build()));
		int total = getExamUserResultResponse.getTotal();
		if (getExamUserResultResponse.getTotal() > length) {
			getExamUserResultResponse = Futures.getUnchecked(adminExamService.getExamUserResult(head, getExamUserResultRquestBuilder.setLength(total).build()));
		}
		
		Map<Long, UserResult> userResultMap = new HashMap<Long, UserResult>();
		for (UserResult userResult : getExamUserResultResponse.getUserReusltList()) {
			userResultMap.put(userResult.getUserId(), userResult);
		}
		
		int allowModelId = exam.getAllowModelId();
		
		// 获取所有满足权限的用户
		Map<Long, UserProtos.User> allowUserMap = this.getAllowUser(head, allowModelId);

		GetUserByIdRequest getUserByIdRequest = GetUserByIdRequest.newBuilder()
				.addAllUserId(allowUserMap.keySet())
				.build();
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, getUserByIdRequest));

		Map<Integer, UserProtos.Team> teamMap = new HashMap<Integer, UserProtos.Team>();
		for (int i = 0; i < getUserByIdResponse.getRefTeamCount(); ++i) {
			UserProtos.Team team = getUserByIdResponse.getRefTeam(i);
			teamMap.put(team.getTeamId(), team);
		}

		Map<Integer, UserProtos.Position> positionMap = new HashMap<Integer, UserProtos.Position>();
		for (int i = 0; i < getUserByIdResponse.getRefPositionCount(); ++i) {
			UserProtos.Position position = getUserByIdResponse
					.getRefPosition(i);
			positionMap.put(position.getPositionId(), position);
		}

		Map<Integer, UserProtos.Level> levelMap = new HashMap<Integer, UserProtos.Level>();
		for (int i = 0; i < getUserByIdResponse.getRefLevelCount(); ++i) {
			UserProtos.Level level = getUserByIdResponse.getRefLevel(i);
			levelMap.put(level.getLevelId(), level);
		}
		
		String examName = exam.getExamName();

		SXSSFSheet sheet = wb.createSheet("考试成绩明细");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 17);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue(examName + "考试成绩统计");
		headCell.setCellStyle(headStyle);

		SXSSFRow row = sheet.createRow(1);

		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		
		Map<Integer, Integer> cellWidth = Maps.newHashMap();

		/*
		 * 序号、中心（所属二级部门）、渠道（所属三级部门）、职位（所属四级部门）、门店（所属五级部门）、岗位、姓名、工号、电话、考试内容、考试开始时间
		 * 、交卷时间、考试成绩、参考情况、通过情况
		 */
		try {
			UserInfoUtil.createCell(row, 0, "一级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 1, "二级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 2, "三级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 3, "四级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 4, "五级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 5, "六级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 6, "七级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 7, "八级部门", style, cellWidth);
			UserInfoUtil.createCell(row, 8, "岗位", style, cellWidth);
			UserInfoUtil.createCell(row, 9, "姓名", style, cellWidth);
			UserInfoUtil.createCell(row, 10, "工号", style, cellWidth);
			UserInfoUtil.createCell(row, 11, "电话", style, cellWidth);
			UserInfoUtil.createCell(row, 12, "考试名称", style, cellWidth);
			UserInfoUtil.createCell(row, 13, "考试时间", style, cellWidth);
			UserInfoUtil.createCell(row, 14, "交卷时间", style, cellWidth);
			UserInfoUtil.createCell(row, 15, "参考情况", style, cellWidth);
			UserInfoUtil.createCell(row, 16, "成绩", style, cellWidth);
			UserInfoUtil.createCell(row, 17, "是否通过", style, cellWidth);

			int cellLine = 1;
			for (long userId : allowUserMap.keySet()) {
				cellLine++;
				row = sheet.createRow(cellLine);
				UserProtos.User user = allowUserMap.get(userId);

				if (user.getTeamCount() > 0) {
					UserProtos.UserTeam userTeam = user.getTeam(0);

					LinkedList<UserProtos.Team> teamList = new LinkedList<UserProtos.Team>();
					int tmpTeamId = userTeam.getTeamId();
					while (true) {
						UserProtos.Team team = teamMap.get(tmpTeamId);
						if (team == null) {
							// warn : cannot find team
							teamList.clear();
							break;
						}

						teamList.addFirst(team);

						if (team.hasParentTeamId()) {
							tmpTeamId = team.getParentTeamId();
						} else {
							break;
						}
					}
					while (teamList.size() < 8) {
						teamList.add(UserProtos.Team.newBuilder()
								.setTeamId(0)
								.setTeamName("")
								.build());
					}

					int teamCell = 0;
					for (UserProtos.Team team : teamList) {
						UserInfoUtil.createCell(row, teamCell, team.getTeamName(), style, cellWidth);
						teamCell++;
					}

					if (userTeam.hasPositionId()) {
						UserProtos.Position position = positionMap.get(userTeam.getPositionId());
						UserInfoUtil.createCell(row, 8, position == null ? "" : position.getPositionName(), style, cellWidth);
					}
				}

				UserInfoUtil.createCell(row, 9, user.getBase().getUserName(), style, cellWidth);
				UserInfoUtil.createCell(row, 10, user.getBase().getRawId(), style, cellWidth);
				UserInfoUtil.createCell(row, 11, DBUtil.COMMA_JOINER.join(user.getBase().getMobileNoList()), style, cellWidth);
				UserInfoUtil.createCell(row, 12, examName, style, cellWidth);

				UserResult userResult = userResultMap.get(user.getBase()
						.getUserId());

				if (userResult == null) {
					continue;
				}
				
				SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				UserInfoUtil.createCell(row, 13, !userResult.hasStartTime() || userResult.getStartTime() == 0 ? 
						"" : sf.format(userResult.getStartTime() * 1000L), style, cellWidth);
				UserInfoUtil.createCell(row, 14, !userResult.hasSubmitTime() || userResult.getSubmitTime() == 0 ? 
						"" : sf.format(userResult.getSubmitTime() * 1000L), style, cellWidth);

				UserInfoUtil.createCell(row, 15, userResult.hasStartTime() && userResult.getStartTime() > 0 ? "参考" : "缺考", style, cellWidth);
				UserInfoUtil.createCell(row, 16, userResult.hasScore() ? String.valueOf(userResult.getScore()) : "0", style, cellWidth);
				UserInfoUtil.createCell(row, 17, exam.getPassMark() > userResult.getScore() ? "不通过" : "通过", style, cellWidth);
			}
			
			UserInfoUtil.adjustWidth(sheet, cellWidth);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	private Map<Long, UserProtos.User> getAllowUser(AdminHead head, int allowModelId) {
		GetUserListRequest.Builder getUserListRequestBuilder = GetUserListRequest.newBuilder()
				.setStart(0)
				.setLength(1000);
		
		Map<Long, UserProtos.User> allowUserMap = new HashMap<Long, UserProtos.User>();
		boolean hasMore = true;
		int start = 0;
		while (hasMore) {
			GetUserListResponse getUserListResponse = Futures.getUnchecked(adminUserService.getUserList(head, getUserListRequestBuilder
					.setStart(start)
					.setLength(1000)
					.build()));
			start += 1000;
			
			if (getUserListResponse.getUserCount() < 1000) {
				hasMore = false;
			}
			Map<Long, UserProtos.User> userMap = new HashMap<Long, UserProtos.User>();
			for (UserProtos.User user : getUserListResponse.getUserList()) {
				userMap.put(user.getBase().getUserId(), user);
			}
			
			CheckAllowRequest checkAllowRequest = CheckAllowRequest.newBuilder()
					.addAllModelId(Collections.singleton(allowModelId))
					.addAllUserId(userMap.keySet())
					.build();
			CheckAllowResponse checkAllowResponse = Futures.getUnchecked(allowService.checkAllow(head, checkAllowRequest));
			for (AllowProtos.CheckAllowResponse.CheckResult checkResult : checkAllowResponse.getCheckResultList()) {
				if (checkResult.getModelId() == allowModelId) {
					for (long userId : checkResult.getAllowUserIdList()) {
						UserProtos.User user = userMap.get(userId);
						if (user != null) {
							allowUserMap.put(userId, user);
						}
					}
				}
			}
		}
		
		return allowUserMap;
	}
}
