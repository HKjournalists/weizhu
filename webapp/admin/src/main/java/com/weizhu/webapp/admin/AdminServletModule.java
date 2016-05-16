package com.weizhu.webapp.admin;

import java.io.File;
import java.util.Properties;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.ServletModule;
import com.weizhu.proto.AdminProtos.AdminAnonymousHead;
import com.weizhu.proto.AdminProtos.AdminHead;
import com.weizhu.web.filter.AdminInfo;
import com.weizhu.web.filter.AdminSessionFilter;
import com.weizhu.webapp.admin.api.AdminForgotPasswordResetServlet;
import com.weizhu.webapp.admin.api.AdminForgotPasswordServlet;
import com.weizhu.webapp.admin.api.AdminLoginServlet;
import com.weizhu.webapp.admin.api.AdminLogoutServlet;
import com.weizhu.webapp.admin.api.AdminResetPasswordServlet;
import com.weizhu.webapp.admin.api.UpdateAdminStateServlet;
import com.weizhu.webapp.admin.api.UpdateRoleServlet;
import com.weizhu.webapp.admin.api.UpdateRoleStateServlet;
import com.weizhu.webapp.admin.api.CreateAdminServlet;
import com.weizhu.webapp.admin.api.CreateRoleServlet;
import com.weizhu.webapp.admin.api.DeleteAdminServlet;
import com.weizhu.webapp.admin.api.DeleteRoleServlet;
import com.weizhu.webapp.admin.api.GetAdminByIdServlet;
import com.weizhu.webapp.admin.api.GetAdminInfoServlet;
import com.weizhu.webapp.admin.api.GetAdminListServlet;
import com.weizhu.webapp.admin.api.GetCompanyListServlet;
import com.weizhu.webapp.admin.api.GetPermissionTreeServlet;
import com.weizhu.webapp.admin.api.GetRoleByIdServlet;
import com.weizhu.webapp.admin.api.GetRoleListServlet;
import com.weizhu.webapp.admin.api.QRCodeServlet;
import com.weizhu.webapp.admin.api.UpdateAdminServlet;
import com.weizhu.webapp.admin.api.official.CancelOfficialSendPlanServlet;
import com.weizhu.webapp.admin.api.official.CreateOfficialSendPlanServlet;
import com.weizhu.webapp.admin.api.official.CreateOfficialServlet;
import com.weizhu.webapp.admin.api.official.DeleteOfficialServlet;
import com.weizhu.webapp.admin.api.official.ExportOfficialMsgServlet;
import com.weizhu.webapp.admin.api.official.GetOfficialByIdServlet;
import com.weizhu.webapp.admin.api.official.GetOfficialListServlet;
import com.weizhu.webapp.admin.api.official.GetOfficialRecvMessageServlet;
import com.weizhu.webapp.admin.api.official.GetOfficialSendPlanByIdServlet;
import com.weizhu.webapp.admin.api.official.GetOfficialSendPlanListServlet;
import com.weizhu.webapp.admin.api.official.SetOfficialEnableServlet;
import com.weizhu.webapp.admin.api.official.UpdateOfficialServlet;
import com.weizhu.webapp.admin.api.user.CreateLevelServlet;
import com.weizhu.webapp.admin.api.user.CreatePositionServlet;
import com.weizhu.webapp.admin.api.user.CreateTeamServlet;
import com.weizhu.webapp.admin.api.user.CreateUserServlet;
import com.weizhu.webapp.admin.api.user.DeleteLevelServlet;
import com.weizhu.webapp.admin.api.user.DeletePositionServlet;
import com.weizhu.webapp.admin.api.user.DeleteTeamServlet;
import com.weizhu.webapp.admin.api.user.DeleteUserServlet;
import com.weizhu.webapp.admin.api.user.DeleteUserSessionServlet;
import com.weizhu.webapp.admin.api.user.ExportAutoLoginXmlServlet;
import com.weizhu.webapp.admin.api.user.ExportUserServlet;
import com.weizhu.webapp.admin.api.user.GetAllTeamServlet;
import com.weizhu.webapp.admin.api.user.GetImportFailLogServlet;
import com.weizhu.webapp.admin.api.user.GetLevelServlet;
import com.weizhu.webapp.admin.api.user.GetPositionServlet;
import com.weizhu.webapp.admin.api.user.GetTeamServlet;
import com.weizhu.webapp.admin.api.user.GetUserByIdServlet;
import com.weizhu.webapp.admin.api.user.GetUserListServlet;
import com.weizhu.webapp.admin.api.user.GetUserLoginSessionServlet;
import com.weizhu.webapp.admin.api.user.ImportUserServlet;
import com.weizhu.webapp.admin.api.user.SetExpertServlet;
import com.weizhu.webapp.admin.api.user.SetStateServlet;
import com.weizhu.webapp.admin.api.user.UpdateLevelServlet;
import com.weizhu.webapp.admin.api.user.UpdatePositionServlet;
import com.weizhu.webapp.admin.api.user.UpdateTeamServlet;
import com.weizhu.webapp.admin.api.user.UpdateUserServlet;

public class AdminServletModule extends ServletModule {

	public AdminServletModule() {
	}
	
	@Override
	protected void configureServlets() {
		filter("/*").through(AdminSessionFilter.class);
		filter("/*").through(WebappAdminFilter.class);
		
		// not login, not company_id
		serve("/api/get_permission_tree.json").with(GetPermissionTreeServlet.class);
		serve("/api/admin_login.json").with(AdminLoginServlet.class);
		serve("/api/admin_reset_password.json").with(AdminResetPasswordServlet.class);
		serve("/api/admin_forgot_password.json").with(AdminForgotPasswordServlet.class);
		serve("/api/admin_forgot_password_reset.json").with(AdminForgotPasswordResetServlet.class);
		
		// login, not company_id
		serve("/api/admin_logout.json").with(AdminLogoutServlet.class);
		serve("/api/get_company_list.json").with(GetCompanyListServlet.class);
		serve("/api/qr_code.jpg").with(QRCodeServlet.class);
		
		// login, company_id
		
		serve("/api/get_admin_info.json").with(GetAdminInfoServlet.class);
		
		serve("/api/get_admin_by_id.json").with(GetAdminByIdServlet.class);
		serve("/api/get_admin_list.json").with(GetAdminListServlet.class);
		serve("/api/create_admin.json").with(CreateAdminServlet.class);
		serve("/api/update_admin.json").with(UpdateAdminServlet.class);
		serve("/api/update_admin_state.json").with(UpdateAdminStateServlet.class);
		serve("/api/delete_admin.json").with(DeleteAdminServlet.class);
		
		serve("/api/get_role_by_id.json").with(GetRoleByIdServlet.class);
		serve("/api/get_role_list.json").with(GetRoleListServlet.class);
		serve("/api/create_role.json").with(CreateRoleServlet.class);
		serve("/api/update_role.json").with(UpdateRoleServlet.class);
		serve("/api/update_role_state.json").with(UpdateRoleStateServlet.class);
		serve("/api/delete_role.json").with(DeleteRoleServlet.class);
		
		/* AdminUserService */
		
		serve("/api/user/import_user.json").with(ImportUserServlet.class);
		serve("/api/user/get_import_fail_log.download").with(GetImportFailLogServlet.class);
		serve("/api/user/export_user.download").with(ExportUserServlet.class);
		serve("/api/user/export_auto_login_xml.download").with(ExportAutoLoginXmlServlet.class);
		
		serve("/api/user/get_user_by_id.json").with(GetUserByIdServlet.class);
		serve("/api/user/get_user_list.json").with(GetUserListServlet.class);
		serve("/api/user/create_user.json").with(CreateUserServlet.class);
		serve("/api/user/update_user.json").with(UpdateUserServlet.class);
		serve("/api/user/delete_user.json").with(DeleteUserServlet.class);
		
		serve("/api/user/get_level.json").with(GetLevelServlet.class);
		serve("/api/user/create_level.json").with(CreateLevelServlet.class);
		serve("/api/user/update_level.json").with(UpdateLevelServlet.class);
		serve("/api/user/delete_level.json").with(DeleteLevelServlet.class);
		
		serve("/api/user/get_position.json").with(GetPositionServlet.class);
		serve("/api/user/create_position.json").with(CreatePositionServlet.class);
		serve("/api/user/update_position.json").with(UpdatePositionServlet.class);
		serve("/api/user/delete_position.json").with(DeletePositionServlet.class);
		
		serve("/api/user/get_all_team.json").with(GetAllTeamServlet.class);
		serve("/api/user/get_team.json").with(GetTeamServlet.class);
		serve("/api/user/create_team.json").with(CreateTeamServlet.class);
		serve("/api/user/update_team.json").with(UpdateTeamServlet.class);
		serve("/api/user/delete_team.json").with(DeleteTeamServlet.class);
		
		serve("/api/user/set_expert.json").with(SetExpertServlet.class);
		serve("/api/user/set_state.json").with(SetStateServlet.class);
		
		serve("/api/user/get_user_login_session.json").with(GetUserLoginSessionServlet.class);
		serve("/api/user/delete_user_session.json").with(DeleteUserSessionServlet.class);
		
		/* AdminOfficialService */
		
		serve("/api/official/get_official_by_id.json").with(GetOfficialByIdServlet.class);
		serve("/api/official/get_official_list.json").with(GetOfficialListServlet.class);
		serve("/api/official/create_official.json").with(CreateOfficialServlet.class);
		serve("/api/official/update_official.json").with(UpdateOfficialServlet.class);
		serve("/api/official/delete_official.json").with(DeleteOfficialServlet.class);
		serve("/api/official/set_official_enable.json").with(SetOfficialEnableServlet.class);
		serve("/api/official/get_official_send_plan_by_id.json").with(GetOfficialSendPlanByIdServlet.class);
		serve("/api/official/create_official_send_plan.json").with(CreateOfficialSendPlanServlet.class);
		serve("/api/official/cancel_official_send_plan.json").with(CancelOfficialSendPlanServlet.class);
		serve("/api/official/get_official_send_plan_list.json").with(GetOfficialSendPlanListServlet.class);
		serve("/api/official/get_official_recv_message.json").with(GetOfficialRecvMessageServlet.class);
		serve("/api/official/export_official_msg.json").with(ExportOfficialMsgServlet.class);
		
		/* admin Exam */
		serve("/api/adminExam/create_question.json").with(com.weizhu.webapp.admin.api.exam.CreateQuestionServlet.class);
		serve("/api/adminExam/get_question_list.json").with(com.weizhu.webapp.admin.api.exam.GetQuestionServlet.class);
		serve("/api/adminExam/update_question.json").with(com.weizhu.webapp.admin.api.exam.UpdateQuestionServlet.class);
		serve("/api/adminExam/delete_question.json").with(com.weizhu.webapp.admin.api.exam.DeleteQuestionServlet.class);
		serve("/api/adminExam/get_exam_list.json").with(com.weizhu.webapp.admin.api.exam.GetExamServlet.class);
		serve("/api/adminExam/create_exam.json").with(com.weizhu.webapp.admin.api.exam.CreateExamServlet.class);
		serve("/api/adminExam/update_exam.json").with(com.weizhu.webapp.admin.api.exam.UpdateExamServlet.class);
		serve("/api/adminExam/delete_exam.json").with(com.weizhu.webapp.admin.api.exam.DeleteExamServlet.class);
		serve("/api/adminExam/update_question_exam.json").with(com.weizhu.webapp.admin.api.exam.UpdateExamQuestionServlet.class);
		serve("/api/adminExam/get_exam_question.json").with(com.weizhu.webapp.admin.api.exam.GetExamQuestionServlet.class);
		serve("/api/adminExam/get_exam_user_result.json").with(com.weizhu.webapp.admin.api.exam.GetExamUserResultServlet.class);
		serve("/api/adminExam/get_team_user.json").with(com.weizhu.webapp.admin.api.exam.GetTeamUserServlet.class);
		serve("/api/adminExam/download_exam_result.json").with(com.weizhu.webapp.admin.api.exam.DownLoadExamResultServlet.class);
		serve("/api/adminExam/create_question_category.json").with(com.weizhu.webapp.admin.api.exam.CreateQuestionCategoryServlet.class);
		serve("/api/adminExam/get_question_by_category.json").with(com.weizhu.webapp.admin.api.exam.GetQuestionByCategoryIdServlet.class);
		serve("/api/adminExam/get_question_category.json").with(com.weizhu.webapp.admin.api.exam.GetQuestionCategoryServlet.class);
		serve("/api/adminExam/update_question_category.json").with(com.weizhu.webapp.admin.api.exam.UpdateQuestionCategoryServlet.class);
		serve("/api/adminExam/update_question_in_question_category.json").with(com.weizhu.webapp.admin.api.exam.UpdateQuestionInQuestionCategoryServlet.class);
		serve("/api/adminExam/re_exam.json").with(com.weizhu.webapp.admin.api.exam.ReExamServlet.class);
		serve("/api/adminExam/create_exam_question_random.json").with(com.weizhu.webapp.admin.api.exam.CreateExamQuestionRandomServlet.class);
		serve("/api/adminExam/import_question.json").with(com.weizhu.webapp.admin.api.exam.ImportQuestionServlet.class);
		serve("/api/adminExam/get_import_fail_log.json").with(com.weizhu.webapp.admin.api.exam.GetImportFailLogServlet.class);
		serve("/api/adminExam/delete_question_category.json").with(com.weizhu.webapp.admin.api.exam.DeleteQuestionCategoryServlet.class);
		serve("/api/adminExam/get_user_by_position.json").with(com.weizhu.webapp.admin.api.exam.GetUserByPositionServlet.class);
		serve("/api/adminExam/get_exam_by_id.json").with(com.weizhu.webapp.admin.api.exam.GetExamByIdServlet.class);
		serve("/api/adminExam/move_question_category.json").with(com.weizhu.webapp.admin.api.exam.MoveQuestionCategoryServlet.class);
		serve("/api/adminExam/exam_statistics.json").with(com.weizhu.webapp.admin.api.exam.ExamStatisticsServlet.class);
		serve("/api/adminExam/exam_position_statistics.json").with(com.weizhu.webapp.admin.api.exam.ExamStatisticsPositionServlet.class);
		serve("/api/adminExam/download_exam_position_statistics.json").with(com.weizhu.webapp.admin.api.exam.DownloadExamStatisticsPositionServlet.class);;
		serve("/api/adminExam/exam_team_statistics.json").with(com.weizhu.webapp.admin.api.exam.ExamStatisticsTeamServlet.class);
		serve("/api/adminExam/download_exam_team_statistics.json").with(com.weizhu.webapp.admin.api.exam.DownloadExamStatisticsTeamServlet.class);
		serve("/api/adminExam/exam_question_statistics.json").with(com.weizhu.webapp.admin.api.exam.ExamStatisticsQuestionRateServlet.class);
		serve("/api/adminExam/download_exam_question_statistics.json").with(com.weizhu.webapp.admin.api.exam.DownloadExamStatisticsQuestionRateServlet.class);
		serve("/api/adminExam/get_user_answer.json").with(com.weizhu.webapp.admin.api.exam.GetUserAnswerServlet.class);
		
		/* AdminQAService */
		
		serve("/api/qa/get_category.json").with(com.weizhu.webapp.admin.api.qa.GetCategoryServlet.class);
		serve("/api/qa/add_category.json").with(com.weizhu.webapp.admin.api.qa.AddCategoryServlet.class);
		serve("/api/qa/delete_category.json").with(com.weizhu.webapp.admin.api.qa.DeleteCategoryServlet.class);
		serve("/api/qa/update_category.json").with(com.weizhu.webapp.admin.api.qa.UpdateCategoryServlet.class);
		
		serve("/api/qa/get_question.json").with(com.weizhu.webapp.admin.api.qa.GetQuestionServlet.class);
		serve("/api/qa/add_question.json").with(com.weizhu.webapp.admin.api.qa.AddQuestionServlet.class);
		serve("/api/qa/delete_question.json").with(com.weizhu.webapp.admin.api.qa.DeleteQuestionServlet.class);
		serve("/api/qa/import_question.json").with(com.weizhu.webapp.admin.api.qa.ImportQuestionServlet.class);
		serve("/api/qa/get_import_fail_log.download").with(com.weizhu.webapp.admin.api.qa.GetImportFailLogServlet.class);
		serve("/api/qa/export_question.download").with(com.weizhu.webapp.admin.api.qa.ExportQuestionServlet.class);
		serve("/api/qa/change_question_category.json").with(com.weizhu.webapp.admin.api.qa.ChangeQuestionCategoryServlet.class);
		
		serve("/api/qa/get_answer.json").with(com.weizhu.webapp.admin.api.qa.GetAnswerServlet.class);
		serve("/api/qa/add_answer.json").with(com.weizhu.webapp.admin.api.qa.AddAnswerServlet.class);
		serve("/api/qa/delete_answer.json").with(com.weizhu.webapp.admin.api.qa.DeleteAnswerServlet.class);
		
		serve("/api/allow/check_allow.json").with(com.weizhu.webapp.admin.api.allow.CheckAllowServlet.class);
		serve("/api/allow/create_model.json").with(com.weizhu.webapp.admin.api.allow.CreateModelServlet.class);
		serve("/api/allow/delete_model.json").with(com.weizhu.webapp.admin.api.allow.DeleteModelServlet.class);
		serve("/api/allow/delete_rule.json").with(com.weizhu.webapp.admin.api.allow.DeleteRuleServlet.class);
		serve("/api/allow/get_model_by_id.json").with(com.weizhu.webapp.admin.api.allow.GetModelByIdServlet.class);
		serve("/api/allow/get_model_list.json").with(com.weizhu.webapp.admin.api.allow.GetModelListServlet.class);
		serve("/api/allow/update_model_rule_order.json").with(com.weizhu.webapp.admin.api.allow.UpdateModelRuleOrderServlet.class);
		serve("/api/allow/update_model.json").with(com.weizhu.webapp.admin.api.allow.UpdateModelServlet.class);
		serve("/api/allow/update_position_rule.json").with(com.weizhu.webapp.admin.api.allow.UpdatePositionRuleServlet.class);
		serve("/api/allow/update_team_rule.json").with(com.weizhu.webapp.admin.api.allow.UpdateTeamRuleServlet.class);
		serve("/api/allow/update_user_rule.json").with(com.weizhu.webapp.admin.api.allow.UpdateUserRuleServlet.class);
		serve("/api/allow/create_user_rule.json").with(com.weizhu.webapp.admin.api.allow.CreateUserRuleServlet.class);
		serve("/api/allow/create_position_rule.json").with(com.weizhu.webapp.admin.api.allow.CreatePositionRuleServlet.class);
		serve("/api/allow/create_team_rule.json").with(com.weizhu.webapp.admin.api.allow.CreateTeamRuleServlet.class);
		serve("/api/allow/update_model_info_rule.json").with(com.weizhu.webapp.admin.api.allow.UpdateModelInfoServlet.class);
		serve("/api/allow/copy_model.json").with(com.weizhu.webapp.admin.api.allow.CopyModelServlet.class);
		serve("/api/allow/import_user.json").with(com.weizhu.webapp.admin.api.allow.ImportUserRuleServlet.class);
		
		/* AdminCommunityService */
		
		serve("/api/community/get_community.json").with(com.weizhu.webapp.admin.api.community.GetCommunityServlet.class);
		serve("/api/community/set_community.json").with(com.weizhu.webapp.admin.api.community.SetCommunityServlet.class);
		serve("/api/community/update_board_order.json").with(com.weizhu.webapp.admin.api.community.UpdateBoardOrderServlet.class);
		
		serve("/api/community/get_board.json").with(com.weizhu.webapp.admin.api.community.GetBoardServlet.class);
		serve("/api/community/create_board.json").with(com.weizhu.webapp.admin.api.community.CreateBoardServlet.class);
		serve("/api/community/update_board.json").with(com.weizhu.webapp.admin.api.community.UpdateBoardServlet.class);
		serve("/api/community/delete_board.json").with(com.weizhu.webapp.admin.api.community.DeleteBoardServlet.class);
		serve("/api/community/create_board_tag.json").with(com.weizhu.webapp.admin.api.community.CreateBoardTagServlet.class);
		serve("/api/community/delete_board_tag.json").with(com.weizhu.webapp.admin.api.community.DeleteBoardTagServlet.class);
		serve("/api/community/get_board_tag.json").with(com.weizhu.webapp.admin.api.community.GetBoardTagServlet.class);
		
		serve("/api/community/get_post.json").with(com.weizhu.webapp.admin.api.community.GetPostServlet.class);
		serve("/api/community/migrate_post.json").with(com.weizhu.webapp.admin.api.community.MigratePostServlet.class);
		serve("/api/community/delete_post.json").with(com.weizhu.webapp.admin.api.community.DeletePostServlet.class);
		serve("/api/community/export_post.json").with(com.weizhu.webapp.admin.api.community.ExportPostServlet.class);
		serve("/api/community/recommend_post.json").with(com.weizhu.webapp.admin.api.community.RecommendPostServlet.class);
		serve("/api/community/set_sticky_post.json").with(com.weizhu.webapp.admin.api.community.SetStickyPostServlet.class);
		serve("/api/community/create_post.json").with(com.weizhu.webapp.admin.api.community.CreatePostServlet.class);
		serve("/api/community/export_post_like.json").with(com.weizhu.webapp.admin.api.community.ExportPostLikeServlet.class);
		
		serve("/api/community/get_comment.json").with(com.weizhu.webapp.admin.api.community.GetCommentServlet.class);
		serve("/api/community/delete_comment.json").with(com.weizhu.webapp.admin.api.community.DeleteCommentServlet.class);
		serve("/api/community/export_comment.json").with(com.weizhu.webapp.admin.api.community.ExportCommentServlet.class);
		serve("/api/community/create_comment.json").with(com.weizhu.webapp.admin.api.community.CreateCommentServlet.class);
		
		/* AdminDiscoverService */
		
		serve("/api/discover/get_discover_banner.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverBannerServlet.class);
		serve("/api/discover/create_discover_banner.json").with(com.weizhu.webapp.admin.api.discover.CreateDiscoverBannerServlet.class);
		serve("/api/discover/update_discover_banner.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverBannerServlet.class);
		serve("/api/discover/delete_discover_banner.json").with(com.weizhu.webapp.admin.api.discover.DeleteDiscoverBannerServlet.class);
		serve("/api/discover/disable_discover_banner.json").with(com.weizhu.webapp.admin.api.discover.DisableDiscoverBannerServlet.class);
		serve("/api/discover/display_discover_banner.json").with(com.weizhu.webapp.admin.api.discover.DisplayDiscoverBannerServlet.class);
		serve("/api/discover/update_discover_banner_order.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverBannerOrderServlet.class);
		
		serve("/api/discover/get_discover_module.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverModuleServlet.class);
		serve("/api/discover/create_discover_module.json").with(com.weizhu.webapp.admin.api.discover.CreateDiscoverModuleServlet.class);
		serve("/api/discover/update_discover_module.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverModuleServlet.class);
		serve("/api/discover/delete_discover_module.json").with(com.weizhu.webapp.admin.api.discover.DeleteDiscoverModuleServlet.class);
		serve("/api/discover/disable_discover_module.json").with(com.weizhu.webapp.admin.api.discover.DisableDiscoverModuleServlet.class);
		serve("/api/discover/display_discover_module.json").with(com.weizhu.webapp.admin.api.discover.DisplayDiscoverModuleServlet.class);
		serve("/api/discover/update_discover_module_order.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverModuleOrderServlet.class);
		
		serve("/api/discover/create_discover_module_category.json").with(com.weizhu.webapp.admin.api.discover.CreateDiscoverModuleCategoryServlet.class);
		serve("/api/discover/update_discover_module_category.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverModuleCategoryServlet.class);
		serve("/api/discover/delete_discover_module_category.json").with(com.weizhu.webapp.admin.api.discover.DeleteDiscoverModuleCategoryServlet.class);
		serve("/api/discover/disable_discover_module_category.json").with(com.weizhu.webapp.admin.api.discover.DisableDiscoverModuleCategoryServlet.class);
		serve("/api/discover/display_discover_module_category.json").with(com.weizhu.webapp.admin.api.discover.DisplayDiscoverModuleCategoryServlet.class);
		serve("/api/discover/update_discover_module_category_order.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverModuleCategoryOrderServlet.class);
		serve("/api/discover/migrate_discover_module_category.json").with(com.weizhu.webapp.admin.api.discover.MigrateDiscoverModuleCategoryServlet.class);
		serve("/api/discover/add_item_to_category.json").with(com.weizhu.webapp.admin.api.discover.AddItemToCategoryServlet.class);
		serve("/api/discover/delete_discover_item_from_category.json").with(com.weizhu.webapp.admin.api.discover.DeleteDiscoverItemFromCategoryServlet.class);
		
		serve("/api/discover/get_discover_item_by_ids.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverItemByIdsServlet.class);
		serve("/api/discover/get_discover_item.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverItemServlet.class);
		serve("/api/discover/create_discover_item.json").with(com.weizhu.webapp.admin.api.discover.CreateDiscoverItemServlet.class);
		serve("/api/discover/update_discover_item.json").with(com.weizhu.webapp.admin.api.discover.UpdateDiscoverItemServlet.class);
		serve("/api/discover/delete_discover_item.json").with(com.weizhu.webapp.admin.api.discover.DeleteDiscoverItemServlet.class);
		serve("/api/discover/disable_discover_item.json").with(com.weizhu.webapp.admin.api.discover.DisableDiscoverItemServlet.class);
		serve("/api/discover/display_discover_item.json").with(com.weizhu.webapp.admin.api.discover.DisplayDiscoverItemServlet.class);
		serve("/api/discover/import_discover_item.json").with(com.weizhu.webapp.admin.api.discover.ImportDiscoverItemServlet.class);
		serve("/api/discover/export_discover_item.json").with(com.weizhu.webapp.admin.api.discover.ExportDiscoverItemServlet.class);

		serve("/api/discover/get_discover_item_comment.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverItemCommentServlet.class);
		serve("/api/discover/get_discover_item_learn.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverItemlearnServlet.class);
		serve("/api/discover/get_discover_item_score.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverItemScoreServlet.class);
		serve("/api/discover/get_discover_item_like.json").with(com.weizhu.webapp.admin.api.discover.GetDiscoverItemLikeServlet.class);
		serve("/api/discover/export_discover_item_comment.json").with(com.weizhu.webapp.admin.api.discover.ExportDiscoverItemCommentServlet.class);
		serve("/api/discover/export_discover_item_learn.json").with(com.weizhu.webapp.admin.api.discover.ExportDiscoverItemLearnServlet.class);
		serve("/api/discover/export_discover_item_score.json").with(com.weizhu.webapp.admin.api.discover.ExportDiscoverItemScoreServlet.class);
		serve("/api/discover/export_discover_item_like.json").with(com.weizhu.webapp.admin.api.discover.ExportDiscoverItemLikeServlet.class);
		serve("/api/discover/export_discover_item_share.json").with(com.weizhu.webapp.admin.api.discover.ExportDiscoverItemShareServlet.class);
		
		serve("/api/discover/get_auth_url.json").with(com.weizhu.webapp.admin.api.discover.GetAuthUrlServlet.class);

		/* SurveyService */
		
		serve("/api/survey/get_survey_by_id.json").with(com.weizhu.webapp.admin.api.survey.GetSurveyByIdServlet.class);
		serve("/api/survey/get_survey_list.json").with(com.weizhu.webapp.admin.api.survey.GetSurveyListServlet.class);
		serve("/api/survey/create_survey.json").with(com.weizhu.webapp.admin.api.survey.CreateSurveyServlet.class);
		serve("/api/survey/update_survey.json").with(com.weizhu.webapp.admin.api.survey.UpdateSurveyServlet.class);
		serve("/api/survey/delete_survey.json").with(com.weizhu.webapp.admin.api.survey.DeleteSurveyServlet.class);
		serve("/api/survey/disable_survey.json").with(com.weizhu.webapp.admin.api.survey.DisableSurveyServlet.class);
		serve("/api/survey/enable_survey.json").with(com.weizhu.webapp.admin.api.survey.EnableSurveyServlet.class);
		
		serve("/api/survey/create_question.json").with(com.weizhu.webapp.admin.api.survey.CreateQuestionServlet.class);
		serve("/api/survey/update_question.json").with(com.weizhu.webapp.admin.api.survey.UpdateQuestionServlet.class);
		serve("/api/survey/delete_question.json").with(com.weizhu.webapp.admin.api.survey.DeleteQuestionServlet.class);
		
		serve("/api/survey/get_survey_result_list.json").with(com.weizhu.webapp.admin.api.survey.GetSurveyResultListServlet.class);
		serve("/api/survey/download_survey_result.json").with(com.weizhu.webapp.admin.api.survey.DownLoadSurveyResultServlet.class);
		serve("/api/survey/question_sort.json").with(com.weizhu.webapp.admin.api.survey.QuestionSortServlet.class);
		serve("/api/survey/copy_survey.json").with(com.weizhu.webapp.admin.api.survey.CopySurveyServlet.class);
		serve("/api/survey/import_question.json").with(com.weizhu.webapp.admin.api.survey.ImportQuestionServlet.class);
		serve("/api/survey/get_import_fail_log.json").with(com.weizhu.webapp.admin.api.survey.GetImportFailLogServlet.class);;
		
		/* credits */
		serve("/api/credits/get_credits.json").with(com.weizhu.webapp.admin.api.credits.GetCreditsServlet.class);
		serve("/api/credits/add_credits.json").with(com.weizhu.webapp.admin.api.credits.AddCreditsServlet.class);
		serve("/api/credits/get_credits_log.json").with(com.weizhu.webapp.admin.api.credits.GetCreditsLogServlet.class);
		serve("/api/credits/get_user_credits.json").with(com.weizhu.webapp.admin.api.credits.GetUserCreditsServlet.class);
		serve("/api/credits/get_credits_order.json").with(com.weizhu.webapp.admin.api.credits.GetCreditsOrderServlet.class);
		serve("/api/credits/create_credits_order.json").with(com.weizhu.webapp.admin.api.credits.CreateCreditsOrderServlet.class);
		serve("/api/credits/clear_user_credits.json").with(com.weizhu.webapp.admin.api.credits.ClearUserCreditsServlet.class);
		serve("/api/credits/get_credits_rule.json").with(com.weizhu.webapp.admin.api.credits.GetCreditsRuleServlet.class);
		serve("/api/credits/update_credits_rule.json").with(com.weizhu.webapp.admin.api.credits.UpdateCreditsRuleServlet.class);
		serve("/api/credits/get_credits_operation.json").with(com.weizhu.webapp.admin.api.credits.GetCreditsOperationServlet.class);
		serve("/api/credits/get_expense_credits.json").with(com.weizhu.webapp.admin.api.credits.GetExpenseCreditsServlet.class);
	
		serve("/api/absence/get_absence_by_id.json").with(com.weizhu.webapp.admin.api.absence.GetAbsenceByIdServlet.class);
		serve("/api/absence/get_absence_list.json").with(com.weizhu.webapp.admin.api.absence.GetAbsenceServlet.class);
		serve("/api/absence/update_absence.json").with(com.weizhu.webapp.admin.api.absence.UpdateAbsenceServlet.class);
		serve("/api/absence/download_absence.json").with(com.weizhu.webapp.admin.api.absence.DownloadAbsenceServlet.class);
	
		serve("/api/tools/productclock/get_customer.json").with(com.weizhu.webapp.admin.api.tools.productclock.GetCustomerAdminServlet.class);
		serve("/api/tools/productclock/create_customer.json").with(com.weizhu.webapp.admin.api.tools.productclock.CreateCustomerAdminServlet.class);
		serve("/api/tools/productclock/update_customer.json").with(com.weizhu.webapp.admin.api.tools.productclock.UpdateCustomerAdminServlet.class);
		serve("/api/tools/productclock/delete_customer.json").with(com.weizhu.webapp.admin.api.tools.productclock.DeleteCustomerAdminServlet.class);
		serve("/api/tools/productclock/import_customer.json").with(com.weizhu.webapp.admin.api.tools.productclock.ImportCustomerSerlvet.class);
		serve("/api/tools/productclock/assigned_saler.json").with(com.weizhu.webapp.admin.api.tools.productclock.AssignedSalerServlet.class);
		serve("/api/tools/productclock/get_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.GetProductAdminServlet.class);
		serve("/api/tools/productclock/create_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.CreateProductServlet.class);
		serve("/api/tools/productclock/update_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.UpdateProductServlet.class);
		serve("/api/tools/productclock/delete_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.DeleteProductServlet.class);
		serve("/api/tools/productclock/import_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.ImportProductServlet.class);
		serve("/api/tools/productclock/get_import_fail_log.json").with(com.weizhu.webapp.admin.api.tools.productclock.GetImportFailLogServlet.class);
		serve("/api/tools/productclock/download_customer.json").with(com.weizhu.webapp.admin.api.tools.productclock.DownloadCustomerServlet.class);
		serve("/api/tools/productclock/download_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.DownloadProductServlet.class);
		serve("/api/tools/productclock/get_customer_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.GetCustomerProductServlet.class);
		serve("/api/tools/productclock/create_customer_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.CreateCustomerProductServlet.class);
		serve("/api/tools/productclock/update_customer_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.UpdateCustomerProductServlet.class);
		serve("/api/tools/productclock/delete_customer_product.json").with(com.weizhu.webapp.admin.api.tools.productclock.DeleteCustomerProductServlet.class);
		serve("/api/tools/productclock/get_communicate_record.json").with(com.weizhu.webapp.admin.api.tools.productclock.GetCommunicateRecordServlet.class);
		
		serve("/api/offline_training/get_train_by_id.json").with(com.weizhu.webapp.admin.api.offline_training.GetTrainByIdServlet.class);
		serve("/api/offline_training/get_train_list.json").with(com.weizhu.webapp.admin.api.offline_training.GetTrainListServlet.class);
		serve("/api/offline_training/create_train.json").with(com.weizhu.webapp.admin.api.offline_training.CreateTrainServlet.class);
		serve("/api/offline_training/update_train.json").with(com.weizhu.webapp.admin.api.offline_training.UpdateTrainServlet.class);
		serve("/api/offline_training/update_train_state.json").with(com.weizhu.webapp.admin.api.offline_training.UpdateTrainStateServlet.class);
		serve("/api/offline_training/update_train_discover_item.json").with(com.weizhu.webapp.admin.api.offline_training.UpdateTrainDiscoverItemServlet.class);
		serve("/api/offline_training/get_train_user_list.json").with(com.weizhu.webapp.admin.api.offline_training.GetTrainUserListServlet.class);
	}
	
	@Provides
	@Singleton
	@Named("admin_upload_tmp_dir")
	public File provideUploadTmpDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_upload_tmp_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_upload_tmp_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_upload_tmp_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@Singleton
	@Named("admin_user_import_fail_log_dir")
	public File provideUserImportFailLogDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_user_import_fail_log_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_user_import_fail_log_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_user_import_fail_log_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@Singleton
	@Named("admin_qa_import_fail_log_dir")
	public File provideQAImportFailLogDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_qa_import_fail_log_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_qa_import_fail_log_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_qa_import_fail_log_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@Singleton
	@Named("admin_question_import_fail_log_dir")
	public File provideQuestionImportFailLogDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_question_import_fail_log_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_question_import_fail_log_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_question_import_fail_log_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@Singleton
	@Named("admin_discover_import_fail_log_dir")
	public File provideDiscoverImportFailLogDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_discover_import_fail_log_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_discover_import_fail_log_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_discover_import_fail_log_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@Singleton
	@Named("admin_survey_question_import_fail_log_dir")
	public File provideSurveyQuestionImportFailLogDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_survey_question_import_fail_log_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_survey_question_import_fail_log_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_survey_question_import_fail_log_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@Singleton
	@Named("admin_tools_productclock_import_fail_log_dir")
	public File provideToolsProductclockImportFailLogDir(@Named("server_conf") Properties confProperties) {
		File dir = new File(confProperties.getProperty("admin_tools_productclock_import_fail_log_dir"));
		if (dir.exists() && !dir.isDirectory()) {
			throw new Error("admin_tools_productclock_import_fail_log_dir is not dir");
		}
		if (!dir.exists() && !dir.mkdir()) {
			throw new Error("admin_tools_productclock_import_fail_log_dir create fail");
		}
		return dir;
	}
	
	@Provides
	@RequestScoped
	public AdminHead provideAdminHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public AdminAnonymousHead provideAdminAnonymousHead() {
		return null;
	}
	
	@Provides
	@RequestScoped
	public AdminInfo provideAdminInfo() {
		return null;
	}
}
