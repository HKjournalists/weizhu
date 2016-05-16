package com.weizhu.webapp.admin.api.absence;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.proto.AbsenceProtos;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerRequest;
import com.weizhu.proto.AbsenceProtos.GetAbsenceSerResponse;
import com.weizhu.proto.AbsenceService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetUserByIdResponse;
import com.weizhu.proto.AdminUserProtos.GetUserListRequest;
import com.weizhu.proto.AdminUserProtos.GetUserListResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
public class DownloadAbsenceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AbsenceService absenceService;
	private final AdminUserService adminUserService;
	
	@Inject
	public DownloadAbsenceServlet(Provider<AdminHead> adminHeadProvider, AbsenceService absenceService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.absenceService = absenceService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int start = ParamUtil.getInt(httpRequest, "start", 0);
		final int length = ParamUtil.getInt(httpRequest, "length", 50);
		
		final String userName = ParamUtil.getString(httpRequest, "user_name", "");
		final Integer startTime = ParamUtil.getInt(httpRequest, "start_time", null);
		final Integer endTime = ParamUtil.getInt(httpRequest, "end_time", null);
		final String actionStr = ParamUtil.getString(httpRequest, "action", "");
		
		final AdminHead head = adminHeadProvider.get();
		
		List<Long> userIdList = Lists.newArrayList();
		if (!userName.isEmpty()) {
			GetUserListResponse getUserResponse = Futures.getUnchecked(adminUserService.getUserList(head, GetUserListRequest.newBuilder()
					.setStart(0)
					.setLength(200)
					.setKeyword(userName)
					.build()));
			for (UserProtos.User user : getUserResponse.getUserList()) {
				userIdList.add(user.getBase().getUserId());
			}
		}
		
		GetAbsenceSerRequest.Action action = null;
		for (GetAbsenceSerRequest.Action tmpAction : GetAbsenceSerRequest.Action.values()) {
			if (tmpAction.name().equals(actionStr)) {
				action = tmpAction;
			}
		}
		
		GetAbsenceSerRequest.Builder requestBuilder = GetAbsenceSerRequest.newBuilder()
				.setStart(start)
				.setLength(length);
				
		if (!userIdList.isEmpty()) {
			requestBuilder.addAllUserId(userIdList);
		}
		if (startTime != null) {
			requestBuilder.setStartTime(startTime);
		}
		if (endTime != null) {
			requestBuilder.setEndTime(endTime);
		}
		if (action != null) {
			requestBuilder.setAction(action);
		}

		GetAbsenceSerResponse response = Futures.getUnchecked(absenceService.getAbsenceSer(head, requestBuilder.build()));
		if (response.getAbsenceCount() == 0) {
			return;
		}
		
		Set<Long> userIdSet = Sets.newHashSet();
		for (AbsenceProtos.Absence absence : response.getAbsenceList()) {
			userIdSet.add(absence.getCreateUser());
		}
		
		GetUserByIdResponse getUserByIdResponse = Futures.getUnchecked(adminUserService.getUserById(head, GetUserByIdRequest.newBuilder()
				.addAllUserId(userIdSet)
				.build()));
		Map<Long, UserProtos.User> userMap = Maps.newHashMap();
		for (UserProtos.User user : getUserByIdResponse.getUserList()) {
			userMap.put(user.getBase().getUserId(), user);
		}
		
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
		
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, head, response, userMap, teamMap, positionMap);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("absence_result.xlsx".getBytes("utf-8"),"iso8859-1"));
			
			wb.write(httpResponse.getOutputStream());
		} finally {
			if (wb != null) {
				try {
					wb.close();
				} catch (Exception ex) {
					
				}
			}
		}
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private void writeExecl(SXSSFWorkbook wb, AdminHead head, GetAbsenceSerResponse response, Map<Long, UserProtos.User> userMap, Map<Integer, UserProtos.Team> teamMap, Map<Integer, UserProtos.Position> positionMap) {
		SXSSFSheet sheet = wb.createSheet("调研结果");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 13);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("请假统计");
		headCell.setCellStyle(headStyle);
		
		// 表的列名称
		SXSSFRow row = sheet.createRow(1);
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		
		Map<Integer, Integer> cellWidth = Maps.newHashMap();
		
		createCell(row, 0, "一级部门", style, cellWidth);
		createCell(row, 1, "二级部门", style, cellWidth);
		createCell(row, 2, "三级部门", style, cellWidth);
		createCell(row, 3, "四级部门", style, cellWidth);
		createCell(row, 4, "五级部门", style, cellWidth);
		createCell(row, 5, "六级部门", style, cellWidth);
		createCell(row, 6, "岗位", style, cellWidth);
		createCell(row, 7, "姓名", style, cellWidth);
		createCell(row, 8, "工号", style, cellWidth);
		createCell(row, 9, "电话", style, cellWidth);
		createCell(row, 10, "请假类型", style, cellWidth);
		createCell(row, 11, "请假开始时间", style, cellWidth);
		createCell(row, 12, "请假预计结束时间", style, cellWidth);
		createCell(row, 13, "请假实际结束时间", style, cellWidth);
		createCell(row, 14, "请假描述", style, cellWidth);
		
		// 请假内容
		int rowIndex = 2;
		for (AbsenceProtos.Absence absence : response.getAbsenceList()) {
			SXSSFRow row1 = sheet.createRow(rowIndex); // 题目行
			// 写用户请假信息
			writeUserAbsence(wb, row1, absence, userMap, teamMap, positionMap, cellWidth);
			rowIndex++;
		}
		
		for (Entry<Integer, Integer> entry : cellWidth.entrySet()) {
			int cellIndex = entry.getKey();
			int width = entry.getValue();
			sheet.setColumnWidth(cellIndex, width * 300);
		}
		
	}
	
	private void createCell(SXSSFRow row, int cellNum, String cellValue, @Nullable CellStyle style, Map<Integer, Integer> cellWidth) {
		SXSSFCell cell = row.createCell(cellNum);
		cell.setCellValue(cellValue);
		if (style != null) {
			cell.setCellStyle(style);
		}
		if (cellWidth.get(cellNum) == null || cellWidth.get(cellNum) < cellValue.getBytes().length) {
			cellWidth.put(cellNum, cellValue.getBytes().length);
		}
	}
	
	private void writeUserAbsence(SXSSFWorkbook wb, SXSSFRow row, AbsenceProtos.Absence absence, 
			Map<Long, UserProtos.User> userMap, 
			Map<Integer, UserProtos.Team> teamMap, 
			Map<Integer, UserProtos.Position> positionMap, 
			Map<Integer, Integer> cellWidth) {
		long userId = absence.getCreateUser();
		UserProtos.User user = userMap.get(userId);

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (user != null) {
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
				while (teamList.size() < 6) {
					teamList.add(UserProtos.Team.newBuilder()
							.setTeamId(0)
							.setTeamName("")
							.build());
				}

				int teamCell = 0;
				for (UserProtos.Team team : teamList) {
					createCell(row, teamCell ++, team.getTeamName(), null, cellWidth);
				}

				if (userTeam.hasPositionId()) {
					UserProtos.Position position = positionMap.get(userTeam.getPositionId());
					createCell(row, 6, position == null ? "" : position.getPositionName(), null, cellWidth);
				}
			}
			
			createCell(row, 7, user.getBase().getUserName(), null, cellWidth);

			createCell(row, 8, user.getBase().getRawId(), null, cellWidth);
			
			StringBuilder mobileStr = new StringBuilder();
			createCell(row, 9, DBUtil.COMMA_JOINER.appendTo(mobileStr, user.getBase().getMobileNoList()).toString(), null, cellWidth);
		
			createCell(row, 10, absence.getType(), null, cellWidth);
			createCell(row, 11, absence.hasCreateTime() ? df.format(new Date(absence.getStartTime() * 1000L)) : "", null, cellWidth);
			if (!absence.hasPreEndTime() || !absence.hasFacEndTime()
					|| ((absence.getPreEndTime() - absence.getFacEndTime()) > 8*3600 || (absence.getPreEndTime() - absence.getFacEndTime()) < -8*3600)) {
				CellStyle style = wb.createCellStyle();
				style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
				style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
				createCell(row, 12, absence.hasPreEndTime() ? df.format(new Date(absence.getPreEndTime() * 1000L)) : "", style, cellWidth);
				createCell(row, 13, absence.hasFacEndTime() ? df.format(new Date(absence.getFacEndTime() * 1000L)) : "", style, cellWidth);
			} else {
				createCell(row, 12, absence.hasPreEndTime() ? df.format(new Date(absence.getPreEndTime() * 1000L)) : "", null, cellWidth);
				createCell(row, 13, absence.hasFacEndTime() ? df.format(new Date(absence.getFacEndTime() * 1000L)) : "", null, cellWidth);
			}
			createCell(row, 14, absence.getDesc(), null, cellWidth);
		} else {
			createCell(row, 7, "【未知用户】：" + userId, null, cellWidth);
			createCell(row, 10, absence.getType(), null, cellWidth);
			createCell(row, 11, absence.hasCreateTime() ? df.format(new Date(absence.getStartTime() * 1000L)) : "", null, cellWidth);
			if (!absence.hasPreEndTime() || !absence.hasFacEndTime()
					|| ((absence.getPreEndTime() - absence.getFacEndTime()) > 8*3600 || (absence.getPreEndTime() - absence.getFacEndTime()) < -8*3600)) {
				CellStyle style = wb.createCellStyle();
				style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
				style.setFillForegroundColor(IndexedColors.RED.getIndex());
				style.setAlignment(XSSFCellStyle.ALIGN_CENTER);
				createCell(row, 12, absence.hasPreEndTime() ? df.format(new Date(absence.getPreEndTime() * 1000L)) : "", style, cellWidth);
				createCell(row, 13, absence.hasFacEndTime() ? df.format(new Date(absence.getFacEndTime() * 1000L)) : "", style, cellWidth);
			} else {
				createCell(row, 12, absence.hasPreEndTime() ? df.format(new Date(absence.getPreEndTime() * 1000L)) : "", null, cellWidth);
				createCell(row, 13, absence.hasFacEndTime() ? df.format(new Date(absence.getFacEndTime() * 1000L)) : "", null, cellWidth);
			}
			createCell(row, 14, absence.getDesc(), null, cellWidth);
		}
		
	}
	
}
