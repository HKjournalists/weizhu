package com.weizhu.webapp.mobile.qa;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.service.ServiceUtil;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAProtos.Question;
import com.weizhu.proto.QAService;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.UserProtos;
import com.weizhu.proto.UserService;
import com.weizhu.proto.UserProtos.GetUserResponse;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class SearchQuestionServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final QAService qaService;
	private final UserService userService;
	private final UploadService uploadService;

	@Inject
	public SearchQuestionServlet(Provider<RequestHead> requestHeadProvider, QAService qaService, UserService userService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.qaService = qaService;
		this.userService = userService;
		this.uploadService = uploadService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String keyword = ParamUtil.getString(httpRequest, "keyword", "");

		final RequestHead head = requestHeadProvider.get();

		QAProtos.SearchQuestionRequest request = QAProtos.SearchQuestionRequest.newBuilder().setKeyword(keyword).build();
		QAProtos.SearchQuestionResponse response = Futures.getUnchecked(this.qaService.searchQuestion(head, request));
		//获取用户信息
		Set<Long> userIds = new TreeSet<Long>();
		for (QAProtos.Question question : response.getQuestionList()) {
			if (question.hasUserId()) {
				userIds.add(question.getUserId());
			}
		}
		for (QAProtos.Answer answer : response.getRefAnswerList()) {
			if (answer.hasUserId()) {
				userIds.add(answer.getUserId());
			}
		}

		for (QAProtos.Category category : response.getRefCategoryList()) {
			if (category.hasUserId()) {
				userIds.add(category.getUserId());
			}
		}
		GetUserResponse userResponse = Futures.getUnchecked(this.userService.getUserById(head, UserProtos.GetUserByIdRequest.newBuilder().addAllUserId(userIds).build()));
		String imageUrlPrefix = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST)).getImageUrlPrefix();
		Map<Long, JsonObject> userJsonMap = QAServletUtil.getUserJson(userResponse, imageUrlPrefix);
		Map<Integer, JsonObject> answerInfoMap = QAServletUtil.getAnswerJsonMap(response.getRefAnswerList(), userJsonMap);
		Map<Integer, List<Question>> cateQuesMap = QAServletUtil.getCategoryQuestionMap(response.getRefCategoryList(), response.getQuestionList());
		JsonObject result = new JsonObject();
		JsonArray categorys = new JsonArray();
		for (QAProtos.Category category : response.getRefCategoryList()) {
			List<Question> questionList = cateQuesMap.get(category.getCategoryId());
			JsonObject categoryJson = QAServletUtil.getCategoryJson(category, userJsonMap);
			JsonArray questions = new JsonArray();
			for (Question question : questionList) {
				questions.add(QAServletUtil.getQuestionJson(question, userJsonMap, answerInfoMap));
			}
			categoryJson.add("question", questions);
			categorys.add(categoryJson);
		}
		result.add("category", categorys);
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}
}
