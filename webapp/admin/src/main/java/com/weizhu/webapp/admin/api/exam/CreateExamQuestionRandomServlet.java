package com.weizhu.webapp.admin.api.exam;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.db.DBUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.AdminExamProtos.CreateExamQuestionRandomResponse;
import com.weizhu.proto.AdminExamService;
import com.weizhu.proto.AdminExamProtos.CreateExamQuestionRandomRequest;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.ParamUtil;

@Singleton
public class CreateExamQuestionRandomServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private final Provider<AdminHead> adminHeadProvider;
	private final AdminExamService adminExamService;
	
	@Inject
	public CreateExamQuestionRandomServlet(Provider<AdminHead> adminHeadProvider, AdminExamService adminExamService) {
		this.adminHeadProvider = adminHeadProvider;
		this.adminExamService = adminExamService;
	}
	
	@Override
	public void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		final int score = ParamUtil.getInt(httpRequest, "score", 0);
		final String questionCategoryIdStr = ParamUtil.getString(httpRequest, "question_category_id_str", "");
		
		CreateExamQuestionRandomRequest.Builder createExamQuestionRandomBuilder = CreateExamQuestionRandomRequest.newBuilder();
		
		try {
			List<String> questionCategoryIdList = DBUtil.COMMA_SPLITTER.splitToList(questionCategoryIdStr);
			for (String questionCategoryId : questionCategoryIdList) {
				createExamQuestionRandomBuilder.addQuestionCategoryId(Integer.parseInt(questionCategoryId));
			}
		} catch (Exception ex) {
			JsonObject result = new JsonObject();
			result.addProperty("result", "FAIL_QUESTION_CATEGORY_INVALID");
			result.addProperty("fail_text", "题库编号不合法！");

			httpResponse.setContentType("application/json;charset=UTF-8");
			JsonUtil.GSON.toJson(result, httpResponse.getWriter());
		}
		
		final AdminHead head = adminHeadProvider.get();
		
		CreateExamQuestionRandomResponse response = Futures.getUnchecked(adminExamService.createExamQuestionRandom(head, createExamQuestionRandomBuilder.setScore(score).build()));
	
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
	@Override
	public void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
		this.doPost(httpRequest, httpResponse);
	}
}
