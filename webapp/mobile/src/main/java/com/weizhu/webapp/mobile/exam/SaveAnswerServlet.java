package com.weizhu.webapp.mobile.exam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.weizhu.common.utils.JsonUtil;
import com.weizhu.proto.ExamProtos;
import com.weizhu.proto.ExamProtos.SaveAnswerResponse;
import com.weizhu.proto.ExamService;
import com.weizhu.proto.ExamProtos.SaveAnswerRequest;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.ParamUtil;

/**
 * <pre>
 * request path: /api/exam/save_answer.json
 * 
 * request arg: 
 *     exam_id int类型， 考试id
 *     answer string类型，需要保存的答案拼接后的字符串。
 *         格式为：answer1|answer2|answer3|...|answern
 *         answerX格式: question_id,option_id1,option_id2,...,option_idn
 *         
 *         例如：1,20001,20002|2,3000|4|5,5001
 * </pre>
 * 
 * @author lindongjlu
 *
 */
@Singleton
@SuppressWarnings("serial")
public class SaveAnswerServlet extends HttpServlet {

	private final Provider<RequestHead> requestHeadProvider;
	private final ExamService examService;
	
	@Inject
	public SaveAnswerServlet(Provider<RequestHead> requestHeadProvider, ExamService examService) {
		this.requestHeadProvider = requestHeadProvider;
		this.examService = examService;
	}
	
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		this.doPost(httpRequest, httpResponse);
	}
	
	private static final Splitter ANSWER_SPLITTER = Splitter.on("|").trimResults().omitEmptyStrings();
	private static final Splitter ANSWER_FIELD_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();
	
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		int examId = ParamUtil.getInt(httpRequest, "exam_id", -1);
		String str = ParamUtil.getString(httpRequest, "answer", "");
		
		// parse answer str
		
		List<ExamProtos.UserAnswer> answerList = new ArrayList<ExamProtos.UserAnswer>();
		ExamProtos.UserAnswer.Builder tmpAnswerBuilder = ExamProtos.UserAnswer.newBuilder();
		
		for (String answerStr : ANSWER_SPLITTER.split(str)) {
			try {
				Iterator<Integer> it = Iterables.transform(ANSWER_FIELD_SPLITTER.split(answerStr), Ints.stringConverter()).iterator();
				
				if (it.hasNext()) {
					tmpAnswerBuilder.clear();
					
					tmpAnswerBuilder.setQuestionId(it.next());
					while (it.hasNext()) {
						tmpAnswerBuilder.addAnswerOptionId(it.next());
					}
					answerList.add(tmpAnswerBuilder.build());
				}
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		
		final RequestHead head = requestHeadProvider.get();
		
		SaveAnswerRequest request = SaveAnswerRequest.newBuilder()
				.setExamId(examId)
				.addAllUserAnswer(answerList)
				.build();
		
		SaveAnswerResponse response = Futures.getUnchecked(this.examService.saveAnswer(head, request));
		
		httpResponse.setContentType("application/json;charset=UTF-8");
		JsonUtil.PROTOBUF_JSON_FORMAT.print(response, httpResponse.getWriter());
	}
	
}
