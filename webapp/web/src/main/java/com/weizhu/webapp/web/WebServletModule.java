package com.weizhu.webapp.web;

import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import com.weizhu.proto.WeizhuProtos.AnonymousHead;
import com.weizhu.proto.WeizhuProtos.RequestHead;
import com.weizhu.web.filter.UserWebLoginSessionFilter;

public class WebServletModule extends ServletModule {

	public WebServletModule() {
	}
	
	@Override
	protected void configureServlets() {
		filter("/*").through(UserWebLoginSessionFilter.class);
		filter("/*").through(WebappWebFilter.class);
		
		serve("/api/test_login.json").with(com.weizhu.webapp.web.api.TestLoginServlet.class);
		serve("/api/qr_code.jpg").with(com.weizhu.webapp.web.api.QRCodeServlet.class);

		serve("/api/login/web_login_by_token.json").with(com.weizhu.webapp.web.api.login.WebLoginByTokenServlet.class);
		serve("/api/login/web_logout.json").with(com.weizhu.webapp.web.api.login.WebLogoutServlet.class);

		// 发现
		serve("/api/discover/get_discover_home.json").with(com.weizhu.webapp.web.api.discover.GetDiscoverHomeServlet.class);

		serve("/api/discover/get_module_prompt_index.json").with(com.weizhu.webapp.web.api.discover.GetModulePromptIndexServlet.class);
		serve("/api/discover/get_module_category_item_list.json").with(com.weizhu.webapp.web.api.discover.GetModuleCategoryItemListServlet.class);

		serve("/api/discover/get_item_by_id.json").with(com.weizhu.webapp.web.api.discover.GetItemByIdServlet.class);
		serve("/api/discover/search_item.json").with(com.weizhu.webapp.web.api.discover.SearchItemServlet.class);
		serve("/api/discover/get_item_comment_list.json").with(com.weizhu.webapp.web.api.discover.GetItemCommentListServlet.class);
		serve("/api/discover/get_item_learn_list.json").with(com.weizhu.webapp.web.api.discover.GetItemLearnListServlet.class);
		serve("/api/discover/get_item_Like_list.json").with(com.weizhu.webapp.web.api.discover.GetItemLikeListServlet.class);
		serve("/api/discover/get_item_score_list.json").with(com.weizhu.webapp.web.api.discover.GetItemScoreListServlet.class);
		serve("/api/discover/get_item_share_list.json").with(com.weizhu.webapp.web.api.discover.GetItemShareListServlet.class);

		serve("/api/discover/get_user_discover.json").with(com.weizhu.webapp.web.api.discover.GetUserDiscoverServlet.class);
		serve("/api/discover/get_user_comment_list.json").with(com.weizhu.webapp.web.api.discover.GetUserCommentListServlet.class);
		serve("/api/discover/get_user_learn_list.json").with(com.weizhu.webapp.web.api.discover.GetUserLearnListServlet.class);
		serve("/api/discover/get_user_like_list.json").with(com.weizhu.webapp.web.api.discover.GetUserLikeListServlet.class);
		serve("/api/discover/get_user_score_list.json").with(com.weizhu.webapp.web.api.discover.GetUserScoreListServlet.class);
		serve("/api/discover/get_user_share_list.json").with(com.weizhu.webapp.web.api.discover.GetUserShareListServlet.class);

		serve("/api/discover/comment_item.json").with(com.weizhu.webapp.web.api.discover.CommentItemServlet.class);
		serve("/api/discover/delete_comment.json").with(com.weizhu.webapp.web.api.discover.DeleteCommentServlet.class);
		serve("/api/discover/learn_item.json").with(com.weizhu.webapp.web.api.discover.LearnItemServlet.class);
		serve("/api/discover/like_item.json").with(com.weizhu.webapp.web.api.discover.LikeItemServlet.class);
		serve("/api/discover/score_item.json").with(com.weizhu.webapp.web.api.discover.ScoreItemServlet.class);
		serve("/api/discover/share_item.json").with(com.weizhu.webapp.web.api.discover.ShareItemServlet.class);
		serve("/api/discover/report_learn_item.json").with(com.weizhu.webapp.web.api.discover.ReportLearnItemServlet.class);

		serve("/api/discover/get_auth_url.json").with(com.weizhu.webapp.web.api.discover.GetAuthUrlServlet.class);

		// 考试
		serve("/api/exam/exam_info.json").with(com.weizhu.webapp.web.api.exam.ExamInfoServlet.class);
		serve("/api/exam/save_answer.json").with(com.weizhu.webapp.web.api.exam.SaveAnswerServlet.class);
		serve("/api/exam/submit_exam.json").with(com.weizhu.webapp.web.api.exam.SubmitExamServlet.class);
		serve("/api/exam/get_open_exam_list.json").with(com.weizhu.webapp.web.api.exam.GetOpenExamListServlet.class);
		serve("/api/exam/get_closed_exam_list.json").with(com.weizhu.webapp.web.api.exam.GetClosedExamListServlet.class);
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
