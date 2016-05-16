package com.weizhu.webapp.mobile;

import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.filter.UserSessionFilter;
import com.weizhu.webapp.mobile.absence.CancelAbsenceServlet;
import com.weizhu.webapp.mobile.absence.CreateAbsenceServlet;
import com.weizhu.webapp.mobile.absence.GetAbsenceNotifyUserServlet;
import com.weizhu.webapp.mobile.absence.GetAbsenceNowServlet;
import com.weizhu.webapp.mobile.absence.GetAbsenceServlet;
import com.weizhu.webapp.mobile.api.TestLoginServlet;
import com.weizhu.webapp.mobile.api.VerifySessionServlet;
import com.weizhu.webapp.mobile.credits.DuibaConsumeCreditsServlet;
import com.weizhu.webapp.mobile.credits.DuibaNotifyServlet;
import com.weizhu.webapp.mobile.discover.ItemContentServlet;
import com.weizhu.webapp.mobile.exam.ExamInfoServlet;
import com.weizhu.webapp.mobile.exam.GetClosedExamListServlet;
import com.weizhu.webapp.mobile.exam.GetOpenExamListServlet;
import com.weizhu.webapp.mobile.exam.SaveAnswerServlet;
import com.weizhu.webapp.mobile.exam.SubmitExamServlet;
import com.weizhu.webapp.mobile.login.GetWebLoginByTokenServlet;
import com.weizhu.webapp.mobile.login.NotifyWebLoginByTokenServlet;
import com.weizhu.webapp.mobile.qa.AddAnswerServlet;
import com.weizhu.webapp.mobile.qa.AddQuestionServlet;
import com.weizhu.webapp.mobile.qa.DeleteAnswerServlet;
import com.weizhu.webapp.mobile.qa.DeleteQuestionServlet;
import com.weizhu.webapp.mobile.qa.GetAnswerServlet;
import com.weizhu.webapp.mobile.qa.GetCategoryServlet;
import com.weizhu.webapp.mobile.qa.GetQuestionServlet;
import com.weizhu.webapp.mobile.qa.LikeAnswerServlet;
import com.weizhu.webapp.mobile.qa.SearchMoreQuestionServlet;
import com.weizhu.webapp.mobile.qa.SearchQuestionServlet;
import com.weizhu.webapp.mobile.register.RegisterBySmsCodeServlet;
import com.weizhu.webapp.mobile.register.SendRegisterSmsCodeServlet;
import com.weizhu.webapp.mobile.scene.GetSceneHomeServlet;
import com.weizhu.webapp.mobile.scene.GetSceneItemServlet;
import com.weizhu.webapp.mobile.scene.tool.recommender.GetRecommenderCompetitorProductServlet;
import com.weizhu.webapp.mobile.scene.tool.recommender.GetRecommenderHomeServlet;
import com.weizhu.webapp.mobile.scene.tool.recommender.GetRecommenderRecommendProductServlet;
import com.weizhu.webapp.mobile.survey.GetClosedSurveyListServlet;
import com.weizhu.webapp.mobile.survey.GetOpenSurveyListServlet;
import com.weizhu.webapp.mobile.survey.GetQuestionAnswerServlet;
import com.weizhu.webapp.mobile.survey.GetSurveyByIdServlet;
import com.weizhu.webapp.mobile.survey.GetSurveyResultServlet;
import com.weizhu.webapp.mobile.survey.SubmitSurveyServlet;
import com.weizhu.webapp.mobile.tools.info.GetInfoHomeServlet;
import com.weizhu.webapp.mobile.tools.info.GetInfoItemContentServlet;
import com.weizhu.webapp.mobile.tools.productclock.CreateCommunicateRecordServlet;
import com.weizhu.webapp.mobile.tools.productclock.CreateCustomerProductServlet;
import com.weizhu.webapp.mobile.tools.productclock.CreateCustomerServlet;
import com.weizhu.webapp.mobile.tools.productclock.DeleteCommunicateRecordServlet;
import com.weizhu.webapp.mobile.tools.productclock.DeleteCustomerProductServlet;
import com.weizhu.webapp.mobile.tools.productclock.DeleteCustomerServlet;
import com.weizhu.webapp.mobile.tools.productclock.GetCommunicateRecordServlet;
import com.weizhu.webapp.mobile.tools.productclock.GetCustomerByIdServlet;
import com.weizhu.webapp.mobile.tools.productclock.GetCustomerListServlet;
import com.weizhu.webapp.mobile.tools.productclock.GetCustomerProductServlet;
import com.weizhu.webapp.mobile.tools.productclock.GetProductListServlet;
import com.weizhu.webapp.mobile.tools.productclock.UpdateCommunicateRecordServlet;
import com.weizhu.webapp.mobile.tools.productclock.UpdateCustomerProductServlet;
import com.weizhu.webapp.mobile.tools.productclock.UpdateCustomerServlet;

public class MobileServletModule extends ServletModule {

	public MobileServletModule() {
	}
	
	@Override
	protected void configureServlets() {
		filter("/*").through(UserSessionFilter.class);
		filter("/*").through(WebappMobileFilter.class);
		
		serve("/api/test/test_login.json").with(TestLoginServlet.class);
		serve("/api/verify_session").with(VerifySessionServlet.class);
		
		serve("/api/login/get_web_login_by_token.json").with(GetWebLoginByTokenServlet.class);
		serve("/api/login/notify_web_login_by_token.json").with(NotifyWebLoginByTokenServlet.class);
		
		serve("/discover/item_content").with(ItemContentServlet.class);
		
		serve("/exam/exam_info").with(ExamInfoServlet.class);
		serve("/api/exam/save_answer.json").with(SaveAnswerServlet.class);
		serve("/api/exam/submit_exam.json").with(SubmitExamServlet.class);
		serve("/api/exam/get_open_exam_list.json").with(GetOpenExamListServlet.class);
		serve("/api/exam/get_closed_exam_list.json").with(GetClosedExamListServlet.class);
		//QAService
		serve("/api/qa/get_category.json").with(GetCategoryServlet.class);
		
		serve("/api/qa/get_question.json").with(GetQuestionServlet.class);
		serve("/api/qa/add_question.json").with(AddQuestionServlet.class);
		serve("/api/qa/delete_question.json").with(DeleteQuestionServlet.class);
		serve("/api/qa/search_question.json").with(SearchQuestionServlet.class);		
		serve("/api/qa/search_more_question.json").with(SearchMoreQuestionServlet.class);
		
		serve("/api/qa/get_answer.json").with(GetAnswerServlet.class);
		serve("/api/qa/add_answer.json").with(AddAnswerServlet.class);
		serve("/api/qa/delete_answer.json").with(DeleteAnswerServlet.class);	
		serve("/api/qa/like_answer.json").with(LikeAnswerServlet.class);
		//SceneService
		serve("/api/scene/get_scene_home.json").with(GetSceneHomeServlet.class);
		serve("/api/scene/get_scene_item.json").with(GetSceneItemServlet.class);	
		
		serve("/api/scene/tool/recommender/get_recommender_home.json").with(GetRecommenderHomeServlet.class);
		serve("/api/scene/tool/recommender/get_recommender_competitor_product.json").with(GetRecommenderCompetitorProductServlet.class);
		serve("/api/scene/tool/recommender/get_recommender_recommend_product.json").with(GetRecommenderRecommendProductServlet.class);
		
		// for register
		serve("/api/register/send_register_sms_code.json").with(SendRegisterSmsCodeServlet.class);
		serve("/api/register/register_by_sms_code.json").with(RegisterBySmsCodeServlet.class);
		
		serve("/api/survey/get_closed_survey_list.json").with(GetClosedSurveyListServlet.class);
		serve("/api/survey/get_open_survey_list.json").with(GetOpenSurveyListServlet.class);
		serve("/api/survey/get_question_answer.json").with(GetQuestionAnswerServlet.class);
		serve("/api/survey/get_survey_by_id.json").with(GetSurveyByIdServlet.class);
		serve("/api/survey/get_survey_result.json").with(GetSurveyResultServlet.class);
		serve("/api/survey/submit_survey.json").with(SubmitSurveyServlet.class);
		
		// tools
		serve("/api/tools/info/get_info_home.json").with(GetInfoHomeServlet.class);
		serve("/api/tools/info/get_info_item_content.json").with(GetInfoItemContentServlet.class);
		
		serve("/api/credits/duiba_consume_credits.json").with(DuibaConsumeCreditsServlet.class);
		serve("/api/credits/duiba_notify_credits.json").with(DuibaNotifyServlet.class);
		
		serve("/api/absence/create_absence.json").with(CreateAbsenceServlet.class);
		serve("/api/absence/get_absence_now.json").with(GetAbsenceNowServlet.class);
		serve("/api/absence/get_absence_list.json").with(GetAbsenceServlet.class);
		serve("/api/absence/cancel_absence.json").with(CancelAbsenceServlet.class);
		serve("/api/absence/get_absence_notify_user.json").with(GetAbsenceNotifyUserServlet.class);
		
		serve("/api/tools/productclock/get_customer.json").with(GetCustomerListServlet.class);
		serve("/api/tools/productclock/create_customer.json").with(CreateCustomerServlet.class);
		serve("/api/tools/productclock/update_customer.json").with(UpdateCustomerServlet.class);
		serve("/api/tools/productclock/delete_customer.json").with(DeleteCustomerServlet.class);
		serve("/api/tools/productclock/get_product.json").with(GetProductListServlet.class);
		serve("/api/tools/productclock/get_customer_product.json").with(GetCustomerProductServlet.class);
		serve("/api/tools/productclock/get_communicate_record.json").with(GetCommunicateRecordServlet.class);
		serve("/api/tools/productclock/create_communicate_record.json").with(CreateCommunicateRecordServlet.class);
		serve("/api/tools/productclock/update_communicate_record.json").with(UpdateCommunicateRecordServlet.class);
		serve("/api/tools/productclock/delete_communicate_record.json").with(DeleteCommunicateRecordServlet.class);
		serve("/api/tools/productclock/create_customer_product.json").with(CreateCustomerProductServlet.class);
		serve("/api/tools/productclock/update_customer_product.json").with(UpdateCustomerProductServlet.class);
		serve("/api/tools/productclock/delete_customer_product.json").with(DeleteCustomerProductServlet.class);
		serve("/api/tools/productclock/get_customer_by_id.json").with(GetCustomerByIdServlet.class);
		
		serve("/api/offline_training/get_open_train_list.json").with(com.weizhu.webapp.mobile.offline_training.GetOpenTrainListServlet.class);
		serve("/api/offline_training/get_closed_train_list.json").with(com.weizhu.webapp.mobile.offline_training.GetClosedTrainListServlet.class);
		serve("/api/offline_training/get_train_by_id.json").with(com.weizhu.webapp.mobile.offline_training.GetTrainByIdServlet.class);
		serve("/api/offline_training/apply_train.json").with(com.weizhu.webapp.mobile.offline_training.ApplyTrainServlet.class);
		serve("/api/offline_training/check_in_train.json").with(com.weizhu.webapp.mobile.offline_training.CheckInTrainServlet.class);
		serve("/api/offline_training/leave_train.json").with(com.weizhu.webapp.mobile.offline_training.LeaveTrainServlet.class);
	}
	
	@Provides
	@RequestScoped
	public AnonymousHead provideAnonymousHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public RequestHead provideRequestHead() {
		return null;
	}
}
