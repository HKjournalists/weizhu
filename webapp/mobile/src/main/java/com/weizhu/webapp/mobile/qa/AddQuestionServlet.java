package com.weizhu.webapp.mobile.qa;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.QAProtos;
import com.weizhu.proto.QAService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class AddQuestionServlet extends HttpServlet {
	private final Provider<RequestHead> requestHeadProvider;
	private final QAService qaService;

	@Inject
	public AddQuestionServlet(Provider<RequestHead> requestHeadProvider, QAService qaService) {
		this.requestHeadProvider = requestHeadProvider;
		this.qaService = qaService;
	}

	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// 1. 取出参数
		String questionContent = ParamUtil.getString(httpRequest, "question_content", "");
		int categoryId = ParamUtil.getInt(httpRequest, "category_id", -1);

		final RequestHead head = requestHeadProvider.get();

		QAProtos.AddQuestionRequest request = QAProtos.AddQuestionRequest.newBuilder().setQuestionContent(questionContent).setCategoryId(categoryId).build();

		QAProtos.AddQuestionResponse response = Futures.getUnchecked(this.qaService.addQuestion(head, request));

		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
}
