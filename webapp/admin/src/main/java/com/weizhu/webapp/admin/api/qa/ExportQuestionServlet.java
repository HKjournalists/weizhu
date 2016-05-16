package com.weizhu.webapp.admin.api.qa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.io.Resources;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.proto.AdminQAProtos.ExportQuestionRequest;
import com.weizhu.proto.AdminQAProtos.ExportQuestionResponse;
import com.weizhu.proto.AdminQAService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.proto.AdminUserProtos;
import com.weizhu.proto.AdminUserProtos.GetUserByIdRequest;
import com.weizhu.proto.AdminProtos;
import com.weizhu.proto.AdminService;
import com.weizhu.proto.AdminUserService;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.UserProtos;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class ExportQuestionServlet extends HttpServlet {
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminQAService adminQAService;
	private final AdminUserService adminUserService;
	private final AdminService adminService;

	@Inject
	public ExportQuestionServlet(Provider<AdminHead> adminHeadProvider, AdminQAService adminQAService, AdminUserService adminUserService,
			AdminService adminService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminQAService = adminQAService;
		this.adminUserService = adminUserService;
		this.adminService = adminService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {

		final AdminHead head = adminHeadProvider.get();

		final XSSFWorkbook wb = new XSSFWorkbook(Resources.getResource("com/weizhu/webapp/admin/api/qa/export_qa_question_file.xlsx").openStream());
		

		Integer lastQuestionId = null;
		int size = 1000;
		boolean hasMore = false;
		Integer categoryId = ParamUtil.getInt(httpRequest, "category_id", null);
		
		try {
			Sheet sheet = wb.getSheetAt(0);
			int idx = 2;
			
			XSSFCellStyle cellStyle = wb.createCellStyle(); //在工作薄的基础上建立一个样式
			cellStyle.setBorderBottom((short) 1); //设置边框样式
			cellStyle.setBorderLeft((short) 1); //左边框
			cellStyle.setBorderRight((short) 1); //右边框
			cellStyle.setBorderTop((short) 1); //顶边框

			XSSFFont font = wb.createFont();
			font.setFontHeight(10);
			font.setFontName("微软雅黑");
			cellStyle.setFont(font);

			do {
				ExportQuestionRequest.Builder requestBuilder = ExportQuestionRequest.newBuilder();
				if (lastQuestionId != null) {
					requestBuilder.setLastQuestionId(lastQuestionId);
				}
				if (categoryId != null) {
					requestBuilder.setCategoryId(categoryId);
				}
				requestBuilder.setSize(size);

				ExportQuestionResponse response = Futures.getUnchecked(adminQAService.exportQuestion(head, requestBuilder.build()));

				if (response.getQuestionCount() <= 0) {
					// error
					break;
				}

				lastQuestionId = response.getQuestion(response.getQuestionCount() - 1).getQuestionId();
				hasMore = response.getHasMore();
				//获取问题列表对应的用户列表信息
				List<QAProtos.Question> questions = response.getQuestionList();
				Set<Long> userIds = new TreeSet<Long>();
				Set<Long> adminIds = new TreeSet<Long>();
				for (QAProtos.Question question : questions) {
					if (question.hasAdminId()) {
						adminIds.add(question.getAdminId());
					} else {
						userIds.add(question.getUserId());
					}
				}
				AdminUserProtos.GetUserByIdResponse userResponse = Futures.getUnchecked(this.adminUserService.getUserById(head,
						GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
				AdminProtos.GetAdminByIdResponse adminResponse = Futures.getUnchecked(this.adminService.getAdminById(head,
						AdminProtos.GetAdminByIdRequest.newBuilder().addAllAdminId(adminIds).build()));
				Map<Long, AdminProtos.Admin> adminMap = QAServletUtil.getAdminMap(adminResponse.getAdminList());
				Map<Long, UserProtos.User> userMap = QAServletUtil.getUserMap(userResponse.getUserList());
				Map<Integer, QAProtos.Category> categoryMap = QAServletUtil.getCategoryMap(response.getRefCategoryList());
				//写出到excel文件
				for (int i = 0; i < response.getQuestionCount(); ++i) {
					final QAProtos.Question question = questions.get(i);
					long userId = question.hasUserId() ? question.getUserId() : question.getAdminId();
					boolean isAdmin = !question.hasUserId();
					Row row = sheet.createRow(idx++);
					row.createCell(0).setCellValue(question.getQuestionId());
					row.createCell(1).setCellValue(question.getQuestionContent());
					row.createCell(2).setCellValue(QAServletUtil.getUserName(userMap, adminMap, userId, isAdmin));
					row.createCell(3).setCellValue(question.getAnswerNum());
					row.createCell(4).setCellValue(QAServletUtil.getCategoryName(categoryMap, question.getCategoryId()));
					row.createCell(5).setCellValue(QAServletUtil.getDate(question.getCreateTime()));
					
					for(int j=0;j<=5;j++){
						row.getCell(j).setCellStyle(cellStyle);
					}
				}

			} while (hasMore);

			httpResponse.setContentType("text/plain");
			httpResponse.setHeader("Content-Disposition", "attachment;filename=export_qa_question.xlsx");

			wb.write(httpResponse.getOutputStream());
		} finally {
			wb.close();
		}
	}

}
