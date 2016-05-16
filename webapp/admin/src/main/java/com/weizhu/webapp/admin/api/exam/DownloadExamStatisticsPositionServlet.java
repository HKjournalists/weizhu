package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.math.BigDecimal;
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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
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
public class DownloadExamStatisticsPositionServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	private final AdminUserService adminUserService;

	@Inject
	public DownloadExamStatisticsPositionServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService, AdminUserService adminUserService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
		this.adminUserService = adminUserService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int examId = ParamUtil.getInt(httpRequest, "exam_id", 0);
		
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, examId);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("position_exam_statistics_result.xlsx".getBytes("utf-8"),"iso8859-1"));
			
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
	
	private void writeExecl(SXSSFWorkbook wb, int examId) {
		SXSSFSheet sheet = wb.createSheet("统计结果");
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 6);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("考试职位统计");
		headCell.setCellStyle(headStyle);
		
		// 表的列名称
		SXSSFRow row = sheet.createRow(1);
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		
		Map<Integer, Integer> cellWidth = Maps.newHashMap();
		
		UserInfoUtil.createCell(row, 0, "职务名称", style, cellWidth);
		UserInfoUtil.createCell(row, 1, "应考人数", style, cellWidth);
		UserInfoUtil.createCell(row, 2, "参考人数", style, cellWidth);
		UserInfoUtil.createCell(row, 3, "参考率", style, cellWidth);
		UserInfoUtil.createCell(row, 4, "通过人数", style, cellWidth);
		UserInfoUtil.createCell(row, 5, "通过率", style, cellWidth);
		UserInfoUtil.createCell(row, 6, "平均分", style, cellWidth);
		
		final AdminHead head = adminHeadProvider.get();
		
		GetPositionResponse getPositionResponse = Futures.getUnchecked(adminUserService.getPosition(head, EmptyRequest.getDefaultInstance()));
		Map<Integer, Position> positionMap = Maps.newHashMap();
		for (Position position : getPositionResponse.getPositionList()) {
			positionMap.put(position.getPositionId(), position);
		}
		
		int start = 0;
		GetPositionStatisticsResponse response = Futures.getUnchecked(adminExamService.getPositionStatistics(head, GetPositionStatisticsRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(50)
				.build()));
		
		int i = 2;
		while (response.getPostionStatisticsCount() > 0) {
			for (PositionStatistics positionStatistic : response.getPostionStatisticsList()) {
				int positionId = positionStatistic.getPositionId();
				if (positionId == 0) {
					continue;
				}
				
				row = sheet.createRow(i);
				String positionName = "【未知职位】" + positionId;
				Position position = positionMap.get(positionId);
				if (position != null) {
					positionName = position.getPositionName();
				}
				
				StatisticalParams statisticalParams = positionStatistic.hasStatisticalParams() ? positionStatistic.getStatisticalParams() : null;
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

				UserInfoUtil.createCell(row, 0, positionName, style, cellWidth);
				UserInfoUtil.createCell(row, 1, String.valueOf(totalNum), style, cellWidth);
				UserInfoUtil.createCell(row, 2, String.valueOf(takeNum), style, cellWidth);
				UserInfoUtil.createCell(row, 3, (totalNum.intValue() == 0 ? 0 : takeNum.divide(totalNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%", style, cellWidth);
				UserInfoUtil.createCell(row, 4, String.valueOf(passNum), style, cellWidth);
				UserInfoUtil.createCell(row, 5, (takeNum.intValue() == 0 ? 0 : passNum.divide(takeNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%", style, cellWidth);
				UserInfoUtil.createCell(row, 6, String.valueOf(average), style, cellWidth);
				i++;
			}
			
			start += 50;
			response = Futures.getUnchecked(adminExamService.getPositionStatistics(head, GetPositionStatisticsRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(50)
				.build()));
		}
		
		UserInfoUtil.adjustWidth(sheet, cellWidth);
	}
}
