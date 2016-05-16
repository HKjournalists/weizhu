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
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateRequest;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse;
import com.weizhu.proto.AdminExamProtos.GetQuestionCorrectRateResponse.QuestionCorrect;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class DownloadExamStatisticsQuestionRateServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;

	@Inject
	public DownloadExamStatisticsQuestionRateServlet(Provider<AdminHead> adminHeadProvider,
			AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int examId = ParamUtil.getInt(httpRequest, "exam_id", 0);
		
		SXSSFWorkbook wb = new SXSSFWorkbook();
		try {
			writeExecl(wb, examId);
			
			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=" + new String("question_exam_statistics_result.xlsx".getBytes("utf-8"),"iso8859-1"));
			
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
		CellRangeAddress cra = new CellRangeAddress(0, 0, 0, 3);
		sheet.addMergedRegion(cra);

		SXSSFRow headRow = sheet.createRow(0);
		CellStyle headStyle = wb.createCellStyle();
		headStyle.setAlignment(CellStyle.ALIGN_CENTER);
		SXSSFCell headCell = headRow.createCell(0);
		headCell.setCellValue("考题正确率统计");
		headCell.setCellStyle(headStyle);
		
		// 表的列名称
		SXSSFRow row = sheet.createRow(1);
		CellStyle style = wb.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		
		Map<Integer, Integer> cellWidth = Maps.newHashMap();
		
		UserInfoUtil.createCell(row, 0, "考题名称", style, cellWidth);
		UserInfoUtil.createCell(row, 1, "答题人数", style, cellWidth);
		UserInfoUtil.createCell(row, 2, "正确人数", style, cellWidth);
		UserInfoUtil.createCell(row, 3, "正确率", style, cellWidth);
		
		final AdminHead head = adminHeadProvider.get();
		
		int start = 0;
		GetQuestionCorrectRateResponse response = Futures.getUnchecked(adminExamService.getQuestionCorrectRate(head, GetQuestionCorrectRateRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(50)
				.build()));
		
		int i = 2;
		while (response.getQuestionCorrectCount() > 0) {
			for (QuestionCorrect questionCorrect : response.getQuestionCorrectList()) {
				row = sheet.createRow(i);
				
				BigDecimal answerNum = new BigDecimal(questionCorrect.hasAnswerNum() ? questionCorrect.getAnswerNum() : 0);
				BigDecimal correctNum = new BigDecimal(questionCorrect.hasCorrectNum() ? questionCorrect.getCorrectNum() : 0);
				UserInfoUtil.createCell(row, 0, questionCorrect.getQuestion().getQuestionName(), style, cellWidth);
				UserInfoUtil.createCell(row, 1, String.valueOf(answerNum.intValue()), style, cellWidth);
				UserInfoUtil.createCell(row, 2, String.valueOf(correctNum.intValue()), style, cellWidth);
				UserInfoUtil.createCell(row, 3, (answerNum.intValue() == 0 ? 0 : correctNum.divide(answerNum, 2, BigDecimal.ROUND_HALF_EVEN).floatValue())*100 + "%", style, cellWidth);
				i++;
			}
			
			start += 50;
			response = Futures.getUnchecked(adminExamService.getQuestionCorrectRate(head, GetQuestionCorrectRateRequest.newBuilder()
				.setExamId(examId)
				.setStart(start)
				.setLength(50)
				.build()));
		}
		
		UserInfoUtil.adjustWidth(sheet, cellWidth);
	}
}
