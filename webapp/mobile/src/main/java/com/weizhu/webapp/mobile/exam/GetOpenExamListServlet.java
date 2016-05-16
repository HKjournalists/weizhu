package com.weizhu.webapp.mobile.exam;

import java.io.IOException;

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
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.GetOpenExamListRequest;
import com.weizhu.proto.ExamProtos.GetOpenExamListResponse;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.UploadProtos.GetUploadUrlPrefixResponse;
import com.weizhu.proto.UploadService;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

@Singleton
@SuppressWarnings("serial")
public class GetOpenExamListServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ExamService examService;
	private final UploadService uploadService;
	
	@Inject
	public GetOpenExamListServlet(Provider<RequestHead> requestHeadProvider, ExamService examService, UploadService uploadService) {
		this.requestHeadProvider = requestHeadProvider;
		this.examService = examService;
		this.uploadService = uploadService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		final Integer lastExamId = ParamUtil.getInt(httpRequest, "last_exam_id", null);
		final Integer lastExamEndTime = ParamUtil.getInt(httpRequest, "last_exam_end_time", null);
		final int size = ParamUtil.getInt(httpRequest, "size", 10);
		
		final RequestHead head = requestHeadProvider.get();
		
		GetOpenExamListRequest.Builder getOpenExamListRequestBuilder = GetOpenExamListRequest.newBuilder();
		if (lastExamId != null) {
			getOpenExamListRequestBuilder.setLastExamId(lastExamId);
		}
		if (lastExamEndTime != null) {
			getOpenExamListRequestBuilder.setLastExamEndTime(lastExamEndTime);
		}
		
		GetOpenExamListResponse response = Futures.getUnchecked(examService.getOpenExamList(head, getOpenExamListRequestBuilder
				.setSize(size)
				.build()));
		
		GetUploadUrlPrefixResponse getUploadUrlPrefixResponse = Futures.getUnchecked(this.uploadService.getUploadUrlPrefix(head, ServiceUtil.EMPTY_REQUEST));
		
		JsonArray examArray = new JsonArray();
		for (ExamProtos.Exam exam : response.getExamList()) {
			JsonObject obj = new JsonObject();
			
			obj.addProperty("exam_id", exam.getExamId());
			obj.addProperty("exam_name", exam.getExamName());
			obj.addProperty("image_name", exam.getImageName());
			obj.addProperty("image_url", getUploadUrlPrefixResponse.getImageUrlPrefix() + exam.getImageName());
			obj.addProperty("start_time", exam.getStartTime());
			obj.addProperty("end_time", exam.getEndTime());
			
			examArray.add(obj);
		}
		
		JsonObject result = new JsonObject();
		result.add("exam", examArray);
		result.addProperty("has_more", response.getHasMore());
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.GSON.toJson(result, httpResponse.getWriter());
	}

}
