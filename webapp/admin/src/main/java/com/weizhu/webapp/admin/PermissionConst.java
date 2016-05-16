package com.weizhu.webapp.admin;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.weizhu.common.db.DBUtil;

public class PermissionConst {

	public static class Permission {

		private final String permissionId;
		private final String permissionName;
		
		public Permission(String permissionId, String permissionName) {
			this.permissionId = permissionId;
			this.permissionName = permissionName;
		}
		
		public String permissionId() {
			return this.permissionId;
		}
		
		public String permissionName() {
			return this.permissionName;
		}
	}
	
	public static class Group {
		
		private final String groupId;
		private final String groupName;
		private final ImmutableList<Group> groupList;
		private final ImmutableList<Permission> permissionList;
		
		public Group(String groupId, String groupName, ImmutableList<Group> groupList, ImmutableList<Permission> permissionList) {
			this.groupId = groupId;
			this.groupName = groupName;
			this.groupList = groupList;
			this.permissionList = permissionList;
		}
		
		public String groupId() {
			return groupId;
		}
		
		public String groupName() {
			return groupName;
		}
		
		public ImmutableList<Group> groupList() {
			return groupList;
		}
		
		public ImmutableList<Permission> permissionList() {
			return permissionList;
		}
	}
	
	private static final ImmutableList<Group> PERMISSION_GROUP_LIST;
	private static final ImmutableMap<String, Permission> PERMISSION_MAP;
	private static final ImmutableMap<String, ImmutableSet<String>> PERMISSION_ID_TO_PATH_MAP;
	private static final ImmutableSet<String> PATH_SET;
	
	static {
		List<Group> permissionGroupList = Lists.newArrayList();
		
		// TODO add permission tree
		
		permissionGroupList.add(
			new Group("system", "系统管理", ImmutableList.of(
				new Group("system/role", "角色管理", ImmutableList.of(), ImmutableList.of(
					new Permission("system/role/list", "查看角色"),
					new Permission("system/role/create", "创建角色"),
					new Permission("system/role/update", "编辑角色"),
					new Permission("system/role/delete", "删除角色")
					)), 
				new Group("system/admin", "帐号管理", ImmutableList.of(), ImmutableList.of(
					new Permission("system/admin/list", "查看账号"),
					new Permission("system/admin/create", "创建帐号"),
					new Permission("system/admin/update", "编辑帐号"),
					new Permission("system/admin/delete", "删除帐号")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("company", "企业管理", ImmutableList.of(
				new Group("company/user", "人员管理", ImmutableList.of(), ImmutableList.of(
					new Permission("company/user/list", "查看人员"),
					new Permission("company/user/create", "创建人员"),
					new Permission("company/user/update", "编辑人员"),
					new Permission("company/user/delete", "删除人员"),
					new Permission("company/user/import", "导入人员"),
					new Permission("company/user/export", "导出人员"),
					new Permission("company/user/set_expert", "设置专家"),
					new Permission("company/user/set_state", "设置人员状态"),
					new Permission("company/user/list_session", "查看人员在线信息"),
					new Permission("company/user/delete_session", "删除人员在线信息")
					)), 
				new Group("company/team", "部门管理", ImmutableList.of(), ImmutableList.of(
					new Permission("company/team/list", "查看部门"),
					new Permission("company/team/create", "创建部门"),
					new Permission("company/team/update", "编辑部门"),
					new Permission("company/team/delete", "删除部门")
					)), 
				new Group("company/position", "职务管理", ImmutableList.of(), ImmutableList.of(
					new Permission("company/position/list", "查看职务"),
					new Permission("company/position/create", "创建职务"),
					new Permission("company/position/update", "编辑职务"),
					new Permission("company/position/delete", "删除职务")
					)), 
				new Group("company/level", "职级管理", ImmutableList.of(), ImmutableList.of(
					new Permission("company/level/list", "查看职级"),
					new Permission("company/level/create", "创建职级"),
					new Permission("company/level/update", "编辑职级"),
					new Permission("company/level/delete", "删除职级")
					))
				), ImmutableList.of()
			));

		permissionGroupList.add(
			new Group("discover", "发现管理", ImmutableList.of(
				new Group("discover/module", "模块管理", ImmutableList.of(), ImmutableList.of(
					new Permission("discover/module/list", "查看模块"),
					new Permission("discover/module/create", "创建模块"),
					new Permission("discover/module/update", "编辑模块"),
					new Permission("discover/module/delete", "删除模块"),
					new Permission("discover/module/set_state", "设置模块状态"),
					new Permission("discover/module/update_order", "调整模块顺序"),
					new Permission("discover/module/create_category", "创建模块分类"),
					new Permission("discover/module/update_category", "编辑模块分类"),
					new Permission("discover/module/delete_category", "删除模块分类"),
					new Permission("discover/module/set_category_state", "设置模块分类状态"),
					new Permission("discover/module/update_category_order", "调整模块分类顺序"),
					new Permission("discover/module/list_category_item", "查看分类关联条目"),
					new Permission("discover/module/add_category_item", "创建分类关联条目"),
					new Permission("discover/module/delete_category_item", "删除分类关联条目")
					)),
				new Group("discover/banner", "轮播管理", ImmutableList.of(), ImmutableList.of(
					new Permission("discover/banner/list", "查看轮播"),
					new Permission("discover/banner/create", "创建轮播"),
					new Permission("discover/banner/update", "编辑轮播"),
					new Permission("discover/banner/delete", "删除轮播"),
					new Permission("discover/banner/set_state", "设置轮播状态"),
					new Permission("discover/banner/update_order", "调整轮播顺序")
					)),
				new Group("discover/item", "条目管理", ImmutableList.of(), ImmutableList.of(
					new Permission("discover/item/list", "查看条目"),
					new Permission("discover/item/create", "创建条目"),
					new Permission("discover/item/update", "编辑条目"),
					new Permission("discover/item/delete", "删除条目"),
					new Permission("discover/item/set_state", "设置条目状态"),
					new Permission("discover/item/export", "导出条目"),
					new Permission("discover/item/show_detail", "查看条目详情"),
					new Permission("discover/item/export_comment", "导出条目评论"),
					new Permission("discover/item/export_learn", "导出条目学习"),
					new Permission("discover/item/export_score", "导出条目评分"),
					new Permission("discover/item/export_like", "导出条目点赞")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("community", "社区管理", ImmutableList.of(
				new Group("community/board", "板块管理", ImmutableList.of(), ImmutableList.of(
					new Permission("community/board/update_name", "社区名称修改"),
					new Permission("community/board/list", "查看板块"),
					new Permission("community/board/create", "创建板块"),
					new Permission("community/board/update", "编辑板块"),
					new Permission("community/board/delete", "删除板块"),
					new Permission("community/board/update_order", "调整板块顺序"),
					new Permission("community/board/list_tag", "查看板块标签"),
					new Permission("community/board/update_tag", "编辑板块标签")
					)), 
				new Group("community/post", "帖子管理", ImmutableList.of(), ImmutableList.of(
					new Permission("community/post/list", "查看帖子"),
					new Permission("community/post/create", "创建帖子"),
					new Permission("community/post/delete", "删除帖子"),
					new Permission("community/post/export", "导出帖子"),
					new Permission("community/post/recommend", "推荐帖子"),
					new Permission("community/post/sticky", "置顶帖子"),
					new Permission("community/post/move", "迁移帖子"),
					new Permission("community/post/create_comment", "添加帖子评论"),
					new Permission("community/post/delete_comment", "删除帖子评论"),
					new Permission("community/post/export_comment", "导出帖子评论"),
					new Permission("community/post/export_like", "导出帖子点赞")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("official", "服务号管理", ImmutableList.of(
				new Group("official/official", "服务号管理", ImmutableList.of(), ImmutableList.of(
					new Permission("official/official/list", "查看服务号"),
					new Permission("official/official/create", "创建服务号"),
					new Permission("official/official/update", "编辑服务号"),
					new Permission("official/official/set_state", "设置服务号状态"),
					new Permission("official/official/list_message", "查看消息"),
					new Permission("official/official/send_message", "发布消息"),
					new Permission("official/official/cancel_message", "取消发布消息")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("exam", "考试管理", ImmutableList.of(
				new Group("exam/category", "题库管理", ImmutableList.of(), ImmutableList.of(
					new Permission("exam/category/list", "查看题库"),
					new Permission("exam/category/create", "创建题库"),
					new Permission("exam/category/update", "编辑题库"),
					new Permission("exam/category/delete", "删除题库"),
					new Permission("exam/category/list_question", "查看考题"),
					new Permission("exam/category/create_question", "创建考题"),
					new Permission("exam/category/update_question", "编辑考题"),
					new Permission("exam/category/delete_question", "删除考题"),
					new Permission("exam/category/import_question", "导入考题"),
					new Permission("exam/category/move_question", "迁移考题")
					)),
				new Group("exam/exam", "试卷管理", ImmutableList.of(), ImmutableList.of(
					new Permission("exam/exam/list", "查看试卷"),
					new Permission("exam/exam/create", "创建试卷"),
					new Permission("exam/exam/update", "编辑试卷"),
					new Permission("exam/exam/delete", "删除试卷"),
					new Permission("exam/exam/makeup", "补考"),
					new Permission("exam/exam/list_result", "查看考试结果"),
					new Permission("exam/exam/export_result", "导出考试结果")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("survey", "调研", ImmutableList.of(
				new Group("survey/survey", "调研管理", ImmutableList.of(), ImmutableList.of(
					new Permission("survey/survey/list", "查看调研"),
					new Permission("survey/survey/create", "创建调研"),
					new Permission("survey/survey/update", "编辑调研"),
					new Permission("survey/survey/copy", "复制调研"),
					new Permission("survey/survey/delete", "删除调研"),
					new Permission("survey/survey/set_state", "设置调研状态"),
					new Permission("survey/survey/list_result", "查看调研结果"),
					new Permission("survey/survey/export_result", "导出调研结果")
					))
				), ImmutableList.of()
			));

		permissionGroupList.add(
			new Group("qa", "问答管理", ImmutableList.of(
				new Group("qa/category", "分类管理", ImmutableList.of(), ImmutableList.of(
					new Permission("qa/category/list", "查看分类"),
					new Permission("qa/category/create", "创建分类"),
					new Permission("qa/category/update", "编辑分类"),
					new Permission("qa/category/delete", "删除分类")
					)), 
				new Group("qa/question", "问题管理", ImmutableList.of(), ImmutableList.of(
					new Permission("qa/question/list", "查看问题"),
					new Permission("qa/question/create", "创建问题"),
					new Permission("qa/question/import", "导入问题"),
					new Permission("qa/question/move", "迁移问题"),
					new Permission("qa/question/delete", "删除问题"),
					new Permission("qa/question/create_answer", "创建回答"),
					new Permission("qa/question/delete_answer", "删除回答")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("credits", "积分商城", ImmutableList.of(
				new Group("credits/overview", "积分概况", ImmutableList.of(), ImmutableList.of(
					new Permission("credits/overview/list", "查看积分概况"),
					new Permission("credits/overview/detail", "查看积分详情"),
					new Permission("credits/overview/update", "编辑积分"),
					new Permission("credits/overview/clean", "清零积分")
					)),
				new Group("credits/grant", "积分发放", ImmutableList.of(), ImmutableList.of(
					new Permission("credits/grant/list", "查看积分发放"),
					new Permission("credits/grant/create", "发放积分"),
					new Permission("credits/grant/list_user", "查看发放对象")
					)),
				new Group("credits/exchange", "兑换纪录", ImmutableList.of(), ImmutableList.of(
					new Permission("credits/exchange/list", "查看兑换纪录")
					)),
				new Group("credits/rule", "积分规则", ImmutableList.of(), ImmutableList.of(
					new Permission("credits/rule/list", "查看积分规则"),
					new Permission("credits/rule/update", "修改积分规则")
					))
				), ImmutableList.of()
			));
		
		permissionGroupList.add(
			new Group("absence", "请假", ImmutableList.of(
				new Group("absence/absence", "请假管理", ImmutableList.of(), ImmutableList.of(
					new Permission("absence/absence/list", "查看假条"),
					new Permission("absence/absence/update", "编辑假条"),
					new Permission("absence/absence/export", "导出假条")
					))
				), ImmutableList.of()
			));
		

		permissionGroupList.add(
			new Group("clock", "产品闹钟", ImmutableList.of(
				new Group("clock/product", "产品管理", ImmutableList.of(), ImmutableList.of(
					new Permission("clock/product/list", "查看产品"),
					new Permission("clock/product/create", "新建产品"),
					new Permission("clock/product/update", "编辑产品"),
					new Permission("clock/product/import", "导入产品"),
					new Permission("clock/product/export", "导出产品"),
					new Permission("clock/product/delete", "删除产品")
					)),
				new Group("clock/customer", "顾客管理", ImmutableList.of(), ImmutableList.of(
					new Permission("clock/customer/list", "查看顾客"),
					new Permission("clock/customer/create", "新建顾客"),
					new Permission("clock/customer/update", "编辑顾客"),
					new Permission("clock/customer/import", "导入顾客"),
					new Permission("clock/customer/export", "导出顾客"),
					new Permission("clock/customer/assigned", "顾客指派"),
					new Permission("clock/customer/product", "购买产品"),
					new Permission("clock/customer/communicate", "沟通记录"),
					new Permission("clock/customer/delete", "删除顾客")
					))
				), ImmutableList.of()
			));
		permissionGroupList.add(
				new Group("clock", "培训管理", ImmutableList.of(
					new Group("offlineTraining/training", "培训管理", ImmutableList.of(), ImmutableList.of(
						new Permission("offlineTraining/training/list", "查看培训"),
						new Permission("offlineTraining/training/create", "新建培训"),
						new Permission("offlineTraining/training/update", "编辑培训"),
						new Permission("offlineTraining/training/courses", "培训课程管理"),
						new Permission("offlineTraining/training/delete", "删除培训"),
						new Permission("offlineTraining/training/state", "设置状态"),
						new Permission("offlineTraining/training/result", "查看结果")
						)),
					new Group("offlineTraining/course", "课程管理", ImmutableList.of(), ImmutableList.of(
							new Permission("offlineTraining/course/list", "查看课程")
							)),
					new Group("offlineTraining/auth", "讲师认证", ImmutableList.of(), ImmutableList.of(
							new Permission("offlineTraining/auth/list", "查看讲师认证")
							)),
					new Group("offlineTraining/lecturer", "讲师管理", ImmutableList.of(), ImmutableList.of(
							new Permission("offlineTraining/lecturer/list", "查看讲师")
							)),
					new Group("offlineTraining/arrangement", "课程安排", ImmutableList.of(), ImmutableList.of(
							new Permission("offlineTraining/arrangement/list", "查看课程安排")
							))
				), ImmutableList.of()
			));
		PERMISSION_GROUP_LIST = ImmutableList.copyOf(permissionGroupList);
		Map<String, Permission> permissionMap = Maps.newTreeMap();
		Queue<Group> queue = new LinkedList<Group>();
		queue.addAll(PERMISSION_GROUP_LIST);
		while (!queue.isEmpty()) {
			Group group = queue.remove();
			for (Permission p : group.permissionList()) {
				permissionMap.put(p.permissionId(), p);
			}
			for (Group g : group.groupList()) {
				queue.add(g);
			}
		}
		PERMISSION_MAP = ImmutableMap.copyOf(permissionMap);
		
		Map<String, ImmutableSet<String>> permissionIdToPathMap = Maps.newTreeMap();
		
		// TODO add permission to action map
		
		permissionIdToPathMap.put("system/role/list", ImmutableSet.of("/api/get_role_list.json"));
		permissionIdToPathMap.put("system/role/create", ImmutableSet.of("/api/create_role.json"));
		permissionIdToPathMap.put("system/role/update", ImmutableSet.of("/api/update_role.json", "/api/update_role_state.json"));
		permissionIdToPathMap.put("system/role/delete", ImmutableSet.of("/api/delete_role.json"));
		permissionIdToPathMap.put("system/admin/list", ImmutableSet.of("/api/get_admin_list.json"));
		permissionIdToPathMap.put("system/admin/create", ImmutableSet.of("/api/create_admin.json"));
		permissionIdToPathMap.put("system/admin/update", ImmutableSet.of("/api/update_admin.json", "/api/update_admin_state.json"));
		permissionIdToPathMap.put("system/admin/delete", ImmutableSet.of("/api/delete_admin.json"));
		
		permissionIdToPathMap.put("company/user/list", ImmutableSet.of()); // "/api/user/get_user_list.json"
		permissionIdToPathMap.put("company/user/create", ImmutableSet.of("/api/user/create_user.json"));
		permissionIdToPathMap.put("company/user/update", ImmutableSet.of("/api/user/update_user.json", "/api/user/set_expert.json", "/api/user/set_state.json"));
		permissionIdToPathMap.put("company/user/delete", ImmutableSet.of("/api/user/delete_user.json"));
		permissionIdToPathMap.put("company/user/import", ImmutableSet.of("/api/user/import_user.json", "/api/user/get_import_fail_log.download"));
		permissionIdToPathMap.put("company/user/export", ImmutableSet.of("/api/user/export_user.download", "/api/user/export_auto_login_xml.download"));
		permissionIdToPathMap.put("company/user/set_expert", ImmutableSet.of("/api/user/set_expert.json"));
		permissionIdToPathMap.put("company/user/set_state", ImmutableSet.of("/api/user/set_state.json"));
		permissionIdToPathMap.put("company/user/list_session", ImmutableSet.of("/api/user/get_user_login_session.json"));
		permissionIdToPathMap.put("company/user/delete_session", ImmutableSet.of("/api/user/delete_user_session.json"));
		permissionIdToPathMap.put("company/team/list", ImmutableSet.of()); //"/api/user/get_team.json"));
		permissionIdToPathMap.put("company/team/create", ImmutableSet.of("/api/user/create_team.json"));
		permissionIdToPathMap.put("company/team/update", ImmutableSet.of("/api/user/update_team.json"));
		permissionIdToPathMap.put("company/team/delete", ImmutableSet.of("/api/user/delete_team.json"));
		permissionIdToPathMap.put("company/position/list", ImmutableSet.of()); //"/api/user/get_position.json"));
		permissionIdToPathMap.put("company/position/create", ImmutableSet.of("/api/user/create_position.json"));
		permissionIdToPathMap.put("company/position/update", ImmutableSet.of("/api/user/update_position.json"));
		permissionIdToPathMap.put("company/position/delete", ImmutableSet.of("/api/user/delete_position.json"));
		permissionIdToPathMap.put("company/level/list", ImmutableSet.of()); //"/api/user/get_level.json"));
		permissionIdToPathMap.put("company/level/create", ImmutableSet.of("/api/user/create_level.json"));
		permissionIdToPathMap.put("company/level/update", ImmutableSet.of("/api/user/update_level.json"));
		permissionIdToPathMap.put("company/level/delete", ImmutableSet.of("/api/user/delete_level.json"));
		
		permissionIdToPathMap.put("qa/category/list", ImmutableSet.of("/api/qa/get_category.json"));
		permissionIdToPathMap.put("qa/category/create", ImmutableSet.of("/api/qa/add_category.json"));
		permissionIdToPathMap.put("qa/category/update", ImmutableSet.of("/api/qa/update_category.json"));
		permissionIdToPathMap.put("qa/category/delete", ImmutableSet.of("/api/qa/delete_category.json"));
		permissionIdToPathMap.put("qa/question/list", ImmutableSet.of("/api/qa/get_question.json", "/api/qa/get_answer.json", "/api/qa/export_question.download"));
		permissionIdToPathMap.put("qa/question/create", ImmutableSet.of("/api/qa/add_question.json"));
		permissionIdToPathMap.put("qa/question/import", ImmutableSet.of("/api/qa/import_question.json", "/api/qa/get_import_fail_log.download"));
		permissionIdToPathMap.put("qa/question/move", ImmutableSet.of("/api/qa/change_question_category.json"));
		permissionIdToPathMap.put("qa/question/delete", ImmutableSet.of("/api/qa/delete_question.json"));
		permissionIdToPathMap.put("qa/question/create_answer", ImmutableSet.of("/api/qa/add_answer.json"));
		permissionIdToPathMap.put("qa/question/delete_answer", ImmutableSet.of("/api/qa/delete_answer.json"));
		
		permissionIdToPathMap.put("exam/category/list", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/create", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/update", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/delete", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/list_question", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/create_question", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/update_question", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/delete_question", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/import_question", ImmutableSet.of());
		permissionIdToPathMap.put("exam/category/move_question", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/list", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/create", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/update", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/delete", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/makeup", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/list_result", ImmutableSet.of());
		permissionIdToPathMap.put("exam/exam/export_result", ImmutableSet.of());
		
		permissionIdToPathMap.put("official/official/list", ImmutableSet.of("/api/official/get_official_list.json"));
		permissionIdToPathMap.put("official/official/create", ImmutableSet.of("/api/official/create_official.json"));
		permissionIdToPathMap.put("official/official/update", ImmutableSet.of("/api/official/update_official.json", "/api/official/set_official_enable.json"));
		permissionIdToPathMap.put("official/official/set_state", ImmutableSet.of("/api/official/set_official_enable.json"));
		permissionIdToPathMap.put("official/official/list_message", ImmutableSet.of("/api/official/get_official_send_plan_list.json", "/api/official/get_official_recv_message.json", "/api/official/export_official_msg.json"));
		permissionIdToPathMap.put("official/official/send_message", ImmutableSet.of("/api/official/create_official_send_plan.json"));
		permissionIdToPathMap.put("official/official/cancel_message", ImmutableSet.of("/api/official/cancel_official_send_plan.json"));
		
		permissionIdToPathMap.put("community/board/update_name", ImmutableSet.of("/api/community/set_community.json"));
		permissionIdToPathMap.put("community/board/list", ImmutableSet.of("/api/community/get_board.json"));
		permissionIdToPathMap.put("community/board/create", ImmutableSet.of("/api/community/create_board.json"));
		permissionIdToPathMap.put("community/board/update", ImmutableSet.of("/api/community/update_board.json"));
		permissionIdToPathMap.put("community/board/delete", ImmutableSet.of("/api/community/delete_board.json"));
		permissionIdToPathMap.put("community/board/update_order", ImmutableSet.of("/api/community/update_board_order.json"));
		permissionIdToPathMap.put("community/board/list_tag", ImmutableSet.of("/api/community/get_board_tag.json"));
		permissionIdToPathMap.put("community/board/update_tag", ImmutableSet.of("/api/community/create_board_tag.json", "/api/community/delete_board_tag.json"));
		permissionIdToPathMap.put("community/post/list", ImmutableSet.of("/api/community/get_post.json", "/api/community/get_comment.json"));
		permissionIdToPathMap.put("community/post/create", ImmutableSet.of("/api/community/create_post.json"));
		permissionIdToPathMap.put("community/post/delete", ImmutableSet.of("/api/community/delete_post.json"));
		permissionIdToPathMap.put("community/post/export", ImmutableSet.of("/api/community/export_post.json"));
		permissionIdToPathMap.put("community/post/recommend", ImmutableSet.of("/api/community/recommend_post.json"));
		permissionIdToPathMap.put("community/post/sticky", ImmutableSet.of("/api/community/set_sticky_post.json"));
		permissionIdToPathMap.put("community/post/move", ImmutableSet.of("/api/community/migrate_post.json"));
		permissionIdToPathMap.put("community/post/create_comment", ImmutableSet.of("/api/community/create_comment.json"));
		permissionIdToPathMap.put("community/post/delete_comment", ImmutableSet.of("/api/community/delete_comment.json"));
		permissionIdToPathMap.put("community/post/export_comment", ImmutableSet.of("/api/community/export_comment.json"));
		permissionIdToPathMap.put("community/post/export_like", ImmutableSet.of("/api/community/export_post_like.json"));
		
		permissionIdToPathMap.put("discover/module/list", ImmutableSet.of());
		permissionIdToPathMap.put("discover/module/create", ImmutableSet.of("/api/discover/create_discover_module.json"));
		permissionIdToPathMap.put("discover/module/update", ImmutableSet.of("/api/discover/update_discover_module.json", "/api/discover/disable_discover_module.json", "/api/discover/display_discover_module.json"));
		permissionIdToPathMap.put("discover/module/delete", ImmutableSet.of("/api/discover/delete_discover_module.json"));
		permissionIdToPathMap.put("discover/module/set_state", ImmutableSet.of("/api/discover/disable_discover_module.json", "/api/discover/display_discover_module.json"));
		permissionIdToPathMap.put("discover/module/update_order", ImmutableSet.of("/api/discover/update_discover_module_order.json"));
		permissionIdToPathMap.put("discover/module/create_category", ImmutableSet.of("/api/discover/create_discover_module_category.json"));
		permissionIdToPathMap.put("discover/module/update_category", ImmutableSet.of("/api/discover/update_discover_module_category.json", "/api/discover/disable_discover_module_category.json", "/api/discover/display_discover_module_category.json"));
		permissionIdToPathMap.put("discover/module/delete_category", ImmutableSet.of("/api/discover/delete_discover_module_category.json"));
		permissionIdToPathMap.put("discover/module/set_category_state", ImmutableSet.of("/api/discover/disable_discover_module_category.json", "/api/discover/display_discover_module_category.json"));
		permissionIdToPathMap.put("discover/module/update_category_order", ImmutableSet.of("/api/discover/update_discover_module_category_order.json"));
		permissionIdToPathMap.put("discover/module/list_category_item", ImmutableSet.of()); // "/api/discover/get_discover_item.json"
		permissionIdToPathMap.put("discover/module/add_category_item", ImmutableSet.of("/api/discover/add_item_to_category.json", "/api/discover/migrate_discover_module_category.json"));
		permissionIdToPathMap.put("discover/module/delete_category_item", ImmutableSet.of("/api/discover/delete_discover_item_from_category.json", "/api/discover/migrate_discover_module_category.json"));
		permissionIdToPathMap.put("discover/banner/list", ImmutableSet.of("/api/discover/get_discover_banner.json"));
		permissionIdToPathMap.put("discover/banner/create", ImmutableSet.of("/api/discover/create_discover_banner.json"));
		permissionIdToPathMap.put("discover/banner/update", ImmutableSet.of("/api/discover/update_discover_banner.json"));
		permissionIdToPathMap.put("discover/banner/delete", ImmutableSet.of("/api/discover/delete_discover_banner.json"));
		permissionIdToPathMap.put("discover/banner/set_state", ImmutableSet.of("/api/discover/disable_discover_banner.json", "/api/discover/display_discover_banner.json"));
		permissionIdToPathMap.put("discover/banner/update_order", ImmutableSet.of("/api/discover/update_discover_banner_order.json"));
		permissionIdToPathMap.put("discover/item/list", ImmutableSet.of()); // "/api/discover/get_discover_item.json"
		permissionIdToPathMap.put("discover/item/create", ImmutableSet.of("/api/discover/create_discover_item.json"));
		permissionIdToPathMap.put("discover/item/update", ImmutableSet.of("/api/discover/update_discover_item.json"));
		permissionIdToPathMap.put("discover/item/delete", ImmutableSet.of("/api/discover/delete_discover_item.json"));
		permissionIdToPathMap.put("discover/item/set_state", ImmutableSet.of("/api/discover/disable_discover_item.json", "/api/discover/display_discover_item.json"));
		permissionIdToPathMap.put("discover/item/export", ImmutableSet.of("/api/discover/export_discover_item.json"));
		permissionIdToPathMap.put("discover/item/show_detail", ImmutableSet.of("/api/discover/get_discover_item_comment.json", "/api/discover/get_discover_item_learn.json", "/api/discover/get_discover_item_score.json", "/api/discover/get_discover_item_like.json"));
		permissionIdToPathMap.put("discover/item/export_comment", ImmutableSet.of("/api/discover/export_discover_item_comment.json"));
		permissionIdToPathMap.put("discover/item/export_learn", ImmutableSet.of("/api/discover/export_discover_item_learn.json"));
		permissionIdToPathMap.put("discover/item/export_score", ImmutableSet.of("/api/discover/export_discover_item_score.json"));
		permissionIdToPathMap.put("discover/item/export_like", ImmutableSet.of("/api/discover/export_discover_item_like.json"));
		permissionIdToPathMap.put("discover/item/export_share", ImmutableSet.of("/api/discover/export_discover_item_share.json"));
		
		permissionIdToPathMap.put("survey/survey/list", ImmutableSet.of("/api/survey/get_survey_list.json"));
		permissionIdToPathMap.put("survey/survey/create", ImmutableSet.of("/api/survey/create_survey.json","/api/survey/create_question.json","/api/survey/update_question.json","/api/survey/delete_question.json","/api/survey/question_sort.json","/api/survey/import_question.json","/api/survey/get_import_fail_log.json"));
		permissionIdToPathMap.put("survey/survey/update", ImmutableSet.of("/api/survey/update_survey.json"));
		permissionIdToPathMap.put("survey/survey/delete", ImmutableSet.of("/api/survey/delete_survey.json"));
		permissionIdToPathMap.put("survey/survey/set_state", ImmutableSet.of("/api/survey/disable_survey.json","/api/survey/enable_survey.json"));
		permissionIdToPathMap.put("survey/survey/list_result", ImmutableSet.of("/api/survey/get_survey_result_list.json"));
		permissionIdToPathMap.put("survey/survey/export_result", ImmutableSet.of("/api/survey/download_survey_result.json"));
		permissionIdToPathMap.put("survey/survey/copy", ImmutableSet.of("/api/survey/copy_survey.json","/api/survey/create_question.json","/api/survey/update_question.json","/api/survey/delete_question.json","/api/survey/question_sort.json","/api/survey/import_question.json","/api/survey/get_import_fail_log.json"));
		
		permissionIdToPathMap.put("credits/overview/list", ImmutableSet.of("/api/credits/get_user_credits.json","/api/credits/get_credits_order.json"));
		permissionIdToPathMap.put("credits/overview/detail", ImmutableSet.of("/api/credits/get_credits_order.json"));
		permissionIdToPathMap.put("credits/overview/update", ImmutableSet.of("/api/credits/create_credits_order.json"));
		permissionIdToPathMap.put("credits/overview/clean", ImmutableSet.of("/api/credits/clear_user_credits.json"));
		permissionIdToPathMap.put("credits/grant/list", ImmutableSet.of("/api/credits/get_credits.json","/api/credits/get_credits_operation.json"));
		permissionIdToPathMap.put("credits/grant/create", ImmutableSet.of("/api/credits/create_credits_order.json"));
		permissionIdToPathMap.put("credits/grant/list_user", ImmutableSet.of());
		permissionIdToPathMap.put("credits/exchange/list", ImmutableSet.of("/api/credits/get_expense_credits.json","/api/credits/get_credits_order.json"));
		permissionIdToPathMap.put("credits/rule/list", ImmutableSet.of("/api/credits/get_expense_credits.json"));
		permissionIdToPathMap.put("credits/rule/update", ImmutableSet.of("/api/credits/update_credits_rule.json"));
		

		permissionIdToPathMap.put("clock/product/list", ImmutableSet.of("/api/tools/productclock/get_product.json"));
		permissionIdToPathMap.put("clock/product/create", ImmutableSet.of("/api/tools/productclock/create_product.json"));
		permissionIdToPathMap.put("clock/product/update", ImmutableSet.of("/api/tools/productclock/update_product.json"));
		permissionIdToPathMap.put("clock/product/import", ImmutableSet.of("/api/tools/productclock/import_product.json","/api/tools/productclock/get_import_fail_log.json"));
		permissionIdToPathMap.put("clock/product/export", ImmutableSet.of("/api/tools/productclock/download_product.json"));
		permissionIdToPathMap.put("clock/product/delete", ImmutableSet.of("/api/tools/productclock/delete_product.json"));
		
		permissionIdToPathMap.put("clock/customer/list", ImmutableSet.of("/api/tools/productclock/get_customer.json"));
		permissionIdToPathMap.put("clock/customer/create", ImmutableSet.of("/api/tools/productclock/create_customer.json"));
		permissionIdToPathMap.put("clock/customer/update", ImmutableSet.of("/api/tools/productclock/update_customer.json"));
		permissionIdToPathMap.put("clock/customer/import", ImmutableSet.of("/api/tools/productclock/import_customer.json","/api/tools/productclock/get_import_fail_log.json"));
		permissionIdToPathMap.put("clock/customer/export", ImmutableSet.of("/api/tools/productclock/download_customer.json"));
		permissionIdToPathMap.put("clock/customer/assigned", ImmutableSet.of("/api/tools/productclock/assigned_saler.json"));
		permissionIdToPathMap.put("clock/customer/product", ImmutableSet.of("/api/tools/productclock/get_customer_product.json"));
		permissionIdToPathMap.put("clock/customer/communicate", ImmutableSet.of("/api/tools/productclock/get_communicate_record.json"));
		permissionIdToPathMap.put("clock/customer/delete", ImmutableSet.of("/api/tools/productclock/delete_customer.json"));
		
		permissionIdToPathMap.put("offlineTraining/training/list", ImmutableSet.of("/api/offline_training/get_train_list.json"));
		permissionIdToPathMap.put("offlineTraining/training/create", ImmutableSet.of("/api/offline_training/create_train.json"));
		permissionIdToPathMap.put("offlineTraining/training/update", ImmutableSet.of("/api/offline_training/update_train.json"));
		permissionIdToPathMap.put("offlineTraining/training/courses", ImmutableSet.of("/api/offline_training/update_train_discover_item.json"));
		permissionIdToPathMap.put("offlineTraining/training/delete", ImmutableSet.of("/api/offline_training/update_train_state.json"));
		permissionIdToPathMap.put("offlineTraining/training/state", ImmutableSet.of("/api/offline_training/update_train_state.json"));
		permissionIdToPathMap.put("offlineTraining/training/result", ImmutableSet.of("/api/offline_training/get_train_user_list.json"));
		

		permissionIdToPathMap.put("offlineTraining/course/list", ImmutableSet.of());
		permissionIdToPathMap.put("offlineTraining/auth/list", ImmutableSet.of());
		permissionIdToPathMap.put("offlineTraining/lecturer/list", ImmutableSet.of());
		permissionIdToPathMap.put("offlineTraining/arrangement/list", ImmutableSet.of());
		
		PERMISSION_ID_TO_PATH_MAP = ImmutableMap.copyOf(permissionIdToPathMap);
		
		Set<String> pathSet = Sets.newTreeSet();
		for (ImmutableSet<String> set : PERMISSION_ID_TO_PATH_MAP.values()) {
			pathSet.addAll(set);
		}
		PATH_SET = ImmutableSet.copyOf(pathSet);
	}
	
	public static ImmutableList<Group> permissionGroupList() {
		return PERMISSION_GROUP_LIST;
	}
	
	public static ImmutableMap<String, Permission> permissionMap() {
		return PERMISSION_MAP;
	}
	
	public static ImmutableMap<String, ImmutableSet<String>> permissionIdToPathMap() {
		return PERMISSION_ID_TO_PATH_MAP;
	}
	
	public static ImmutableSet<String> pathSet() {
		return PATH_SET;
	}
	
	public static void main(String[] args) throws Exception {
		for (Permission p : PERMISSION_MAP.values()) {
			System.out.println("(1, '" + DBUtil.SQL_STRING_ESCAPER.escape(p.permissionId) + "'), ");
		}
	}
	
}
