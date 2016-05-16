package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsRequest;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse;
import com.weizhu.proto.AdminExamProtos.GetTeamStatisticsResponse.TeamStatistics;
import com.weizhu.proto.AdminExamProtos.StatisticalParams;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos.GetPositionResponse;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdRequest;
import com.weizhu.proto.AdminUserProtos.GetTeamByIdResponse;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.UserProtos.Position;
import com.weizhu.proto.UserProtos.Team;
import com.weizhu.proto.WeizhuProtos.EmptyRequest;
import com.weizhu.web.ParamUtil;

@Singleton
public class DownloadExamStatisticsTeamServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminUserService adminUserService;

	@Inject
	public DownloadExamStatisticsTeamServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int examId = ParamUtil.getInt(httpRequest, "exam_id", 0);
		final String teamIdStr = ParamUtil.getString(httpRequest, "team_id", null);
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, examId, teamIdStr);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("team_exam_statistics_result.xlsx".getBytes("utf-8"),"iso8859-1"));
			
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
	
	private void writeExecl(SXSSFWorkbook wb, int examId, @Nullable String teamIdStr) {
		SXSSFSheet sheet = wb.createSheet("统计结果");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 7);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("考试部门统计");
		headCell.setCellStyle(headStyle);
		
		// 表的列名称
		SXSSFRow row = sheet.createRow(1);
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		
		Map<Integer, Integer> cellWidth = Maps.newHashMap();
		
		UserInfoUtil.createCell(row, 0, "一级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 1, "二级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 2, "三级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 3, "四级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 4, "五级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 5, "六级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 6, "七级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 7, "八级部门", style, cellWidth);
		UserInfoUtil.createCell(row, 8, "应考人数", style, cellWidth);
		UserInfoUtil.createCell(row, 9, "参考人数", style, cellWidth);
		UserInfoUtil.createCell(row, 10, "参考率", style, cellWidth);
		UserInfoUtil.createCell(row, 11, "通过人数", style, cellWidth);
		UserInfoUtil.createCell(row, 12, "通过率", style, cellWidth);
		UserInfoUtil.createCell(row, 13, "平均分", style, cellWidth);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetPositionResponse getPositionResponse = Futures.getUnchecked(adminUserService.getPosition(head, EmptyRequest.getDefaultInstance()));
		Map<Integer, Position> positionMap = Maps.newHashMap();
		for (Position position : getPositionResponse.getPositionList()) {
			positionMap.put(position.getPositionId(), position);
		}
		
		int start = 0;
		GetTeamStatisticsRequest.Builder requestBuilder = GetTeamStatisticsRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(50);
		if (teamIdStr != null) {
			requestBuilder.setTeamId(teamIdStr);
		}
		
		GetTeamStatisticsResponse response = Futures.getUnchecked(adminExamService.getTeamStatistics(head, requestBuilder.build()));
		
		int i = 2;
		while (response.getTeamStatisticsCount() > 0) {
			Set<Integer> teamIdSet = Sets.newHashSet();
			for (TeamStatistics teamStatistic : response.getTeamStatisticsList()) {
				teamIdSet.addAll(teamStatistic.getTeamIdList());
			}
			
			GetTeamByIdResponse getTeamByIdResponse = Futures.getUnchecked(adminUserService.getTeamById(head, GetTeamByIdRequest.newBuilder()
					.addAllTeamId(teamIdSet)
					.build()));
			Map<Integer, Team> teamMap = Maps.newHashMap();
			for (Team team : getTeamByIdResponse.getTeamList()) {
				teamMap.put(team.getTeamId(), team);
			}
			
			for (TeamStatistics teamStatistic : response.getTeamStatisticsList()) {
				if (teamStatistic.getTeamIdCount() == 0) {
					continue;
				}
				
				row = sheet.createRow(i);
				
				Iterator<Integer> it = teamStatistic.getTeamIdList().iterator();
				List<String> teamNameList = Lists.newArrayList();
				while (teamNameList.size() < 8) {
					if (it.hasNext()) {
						int teamId = it.next();
						teamNameList.add(teamMap.get(teamId) == null ? "【未知部门】" + teamId : teamMap.get(teamId).getTeamName());
					} else {
						teamNameList.add("");
					}
				}
				
				StatisticalParams statisticalParams = teamStatistic.hasStatisticalParams() ? teamStatistic.getStatisticalParams() : null;
				BigDecimal totalNum = null;
				BigDecimal takeNum = null;
				BigDecimal passNum = null;
				int average = 0;
				if (statisticalParams != null) {
					totalNum = new BigDecimal(statisticalParams.hasTakeExamNum() ? statisticalParams.getTotalExamNum() : 0);
					takeNum = new BigDecimal(statisticalParams.hasTakeExamNum() ? statisticalParams.getTakeExamNum() : 0);
					passNum = new BigDecimal(statisticalParams.hasPassExamNum() ? statisticalParams.getPassExamNum() : 0);
					average = statisticalParams.getAverageScore();
				} else {
					totalNum = new BigDecimal(0);
					takeNum = new BigDecimal(0);
					passNum = new BigDecimal(0);
				}

				for (int j=0; j<8; j++) {
					UserInfoUtil.createCell(row, j, teamNameList.get(j), style, cellWidth);
				}
				UserInfoUtil.createCell(row, 8, String.valueOf(totalNum), style, cellWidth);
				UserInfoUtil.createCell(row, 9, String.valueOf(takeNum), style, cellWidth);
				UserInfoUtil.createCell(row, 10, (totalNum.intValue() == 0 ? 0 : takeNum.divide(totalNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%", style, cellWidth);
				UserInfoUtil.createCell(row, 11, String.valueOf(passNum), style, cellWidth);
				UserInfoUtil.createCell(row, 12, (takeNum.intValue() == 0 ? 0 : passNum.divide(takeNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%", style, cellWidth);
				UserInfoUtil.createCell(row, 13, String.valueOf(average), style, cellWidth);
				i++;
			}
			
			start += 50;
			requestBuilder = GetTeamStatisticsRequest.newBuilder()
					.setExamId(examId)
					.setStart(start)
					.setLength(50);
			if (teamIdStr != null) {
				requestBuilder.setTeamId(teamIdStr);
			}
			response = Futures.getUnchecked(adminExamService.getTeamStatistics(head, requestBuilder.build()));
		}
		
		UserInfoUtil.adjustWidth(sheet, cellWidth);
	}
}
