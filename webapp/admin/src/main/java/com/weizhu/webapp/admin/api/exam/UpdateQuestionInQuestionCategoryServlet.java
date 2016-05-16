package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionInQuestionCategoryRequest;
import com.weizhu.proto.AdminExamProtos.UpdateQuestionInQuestionCategoryResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class UpdateQuestionInQuestionCategoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public UpdateQuestionInQuestionCategoryServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final Integer oldCategoryId = ParamUtil.getInt(httpRequest, "old_category_id", -1);
		final Integer newCategoryId = ParamUtil.getInt(httpRequest, "new_category_id", -1);
		final String questionIdStr = ParamUtil.getString(httpRequest, "question_id", "");
		
		List<String> questionIdStrList = DBUtil.COMMA_SPLITTER.splitToList(questionIdStr);
		List<Integer> questionIdList = new ArrayList<Integer>();
		try {
			for (String questionId : questionIdStrList) {
				questionIdList.add(Integer.parseInt(questionId));
			}
		} catch (Exception ex) {
			throw new RuntimeException("parse fail", ex);
		}
		
		UpdateQuestionInQuestionCategoryRequest request = UpdateQuestionInQuestionCategoryRequest.newBuilder()
				.setOldCategoryId(oldCategoryId)
				.setNewCategoryId(newCategoryId)
				.addAllQuestionId(questionIdList)
				.build();
		
		final AdminHead adminHead = adminHeadProvider.get();
		
		UpdateQuestionInQuestionCategoryResponse response = Futures.getUnchecked(adminExamService.updateQuestionInQuestionCategory(adminHead, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
