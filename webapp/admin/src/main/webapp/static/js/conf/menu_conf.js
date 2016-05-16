/**
 * Created by Administrator on 15-12-14.
 */
Wz.menu.init(function(){
	return [{
	    id: 'system',
	    title: Wz.lang.menu.system_manage,
	    permission: true,
	    icon: './static/images/leftico01.png',
	    children: [{
	        id: 'system-home',
	        title: Wz.lang.menu.system_home,
	        parrentTitle: Wz.lang.menu.system_manage,
	        permission: true,
	        className: 'Wz.system.home',
	        files: {
	            tpl: './static/view/system/home.html',
	            controller: './static/controller/system/home.js'
	        }
	    },{
	        id: 'system-role',
	        title: Wz.lang.menu.system_role_manage,
	        parrentTitle: Wz.lang.menu.system_manage,
	        permission: Wz.getPermission('system/role/list'),
	        className: 'Wz.system.role',
	        files: {
	            tpl: './static/view/system/roleManage.html',
	            controller: './static/controller/system/roleManage.js'
	        }
	    },{
	        id: 'system-account',
	        title: Wz.lang.menu.system_account_manage,
	        parrentTitle: Wz.lang.menu.system_manage,
	        permission: Wz.getPermission('system/admin/list'),
	        className: 'Wz.system.accountManage',
	        files: {
	            tpl: './static/view/system/accountManage.html',
	            controller: './static/controller/system/accountManage.js'
	        }
	    }]
	},{
	    id: 'business',
	    title: Wz.lang.menu.business_manage,
	    permission: Wz.getPermission('company/user/list') || Wz.getPermission('company/team/list') || Wz.getPermission('company/position/list') || Wz.getPermission('company/level/list'),
	    icon: './static/images/leftico02.png',
	    children: [{
	        id: 'business-user',
	        title: Wz.lang.menu.business_user_manage,
	        parrentTitle: Wz.lang.menu.business_manage,
	        permission: Wz.getPermission('company/user/list'),
	        className: 'Wz.company.userManage',
	        files: {
	            tpl: './static/view/business/userManage.html',
	            controller: './static/controller/business/userManage.js'
	        }
	    },{
	        id: 'business-team',
	        title: Wz.lang.menu.business_team_manage,
	        parrentTitle: Wz.lang.menu.business_manage,
	        permission: Wz.getPermission('company/team/list'),
	        className: 'Wz.company.teamManage',
	        files: {
	            tpl: './static/view/business/teamManage.html',
	            controller: './static/controller/business/teamManage.js'
	        }
	    },{
	        id: 'business-position',
	        title: Wz.lang.menu.business_position_manage,
	        parrentTitle: Wz.lang.menu.business_manage,
	        permission: Wz.getPermission('company/position/list'),
	        className: 'Wz.company.positionManage',
	        files: {
	            tpl: './static/view/business/positionManage.html',
	            controller: './static/controller/business/positionManage.js'
	        }
	    },{
	        id: 'business-level',
	        title: Wz.lang.menu.business_level_manage,
	        parrentTitle: Wz.lang.menu.business_manage,
	        permission: Wz.getPermission('company/level/list'),
	        className: 'Wz.company.levelManage',
	        files: {
	            tpl: './static/view/business/levelManage.html',
	            controller: './static/controller/business/levelManage.js'
	        }
	    }]
	},{
	    id: 'discover',
	    title: Wz.lang.menu.discover_manage,
	    permission: Wz.getPermission('discover/module/list') || Wz.getPermission('discover/banner/list') || Wz.getPermission('discover/item/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'discover-module',
	        title: Wz.lang.menu.discover_module_manage,
	        parrentTitle: Wz.lang.menu.discover_manage,
	        permission: Wz.getPermission('discover/module/list'),
	        className: 'Wz.discover.moduleManage',
	        files: {
	            tpl: './static/view/discover/moduleManage.html',
	            controller: './static/controller/discover/moduleManage.js'
	        }
	    },{
	        id: 'discover-banner',
	        title: Wz.lang.menu.discover_banner_manage,
	        parrentTitle: Wz.lang.menu.discover_manage,
	        permission: Wz.getPermission('discover/banner/list'),
	        className: 'Wz.discover.bannerManage',
	        files: {
	            tpl: './static/view/discover/bannerManage.html',
	            controller: './static/controller/discover/bannerManage.js'
	        }
	    },{
	        id: 'discover-item',
	        title: Wz.lang.menu.discover_item_manage,
	        parrentTitle: Wz.lang.menu.discover_manage,
	        permission: Wz.getPermission('discover/item/list'),
	        className: 'Wz.discover.itemManage',
	        files: {
	            tpl: './static/view/discover/itemManage.html',
	            controller: './static/controller/discover/itemManage.js'
	        }
	    }]
	},{
	    id: 'community',
	    title: Wz.lang.menu.community_manage,
	    permission: Wz.getPermission('community/board/list') || Wz.getPermission('community/post/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'community-module',
	        title: Wz.lang.menu.community_module_manage,
	        parrentTitle: Wz.lang.menu.community_manage,
	        permission: Wz.getPermission('community/board/list'),
	        className: 'Wz.community.moduleMange',
	        files: {
	            tpl: './static/view/community/moduleManage.html',
	            controller: './static/controller/community/moduleManage.js'
	        }
	    },{
	        id: 'community-post',
	        title: Wz.lang.menu.community_post_manage,
	        parrentTitle: Wz.lang.menu.community_manage,
	        permission: Wz.getPermission('community/post/list'),
	        className: 'Wz.community.postManage',
	        files: {
	            tpl: './static/view/community/postManage.html',
	            controller: './static/controller/community/postManage.js'
	        }
	    }]
	},{
	    id: 'official',
	    title: Wz.lang.menu.official,
	    permission: Wz.getPermission('official/official/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'official-manage',
	        title: Wz.lang.menu.official_manage,
	        parrentTitle: Wz.lang.menu.official,
	        permission: Wz.getPermission('official/official/list'),
	        className: 'Wz.service.officialManage',
	        files: {
	            tpl: './static/view/official/officialManage.html',
	            controller: './static/controller/official/officialManage.js'
	        }
	    }]
	},{
	    id: 'exam',
	    title: Wz.lang.menu.exam_manage,
	    permission: Wz.getPermission('exam/category/list') || Wz.getPermission('exam/exam/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'exam-category',
	        title: Wz.lang.menu.exam_questionbank_manage,
	        parrentTitle: Wz.lang.menu.exam_manage,
	        permission: Wz.getPermission('exam/category/list'),
	        className: 'Wz.exam.categoryManage',
	        files: {
	            tpl: './static/view/exam/categoryManage.html',
	            controller: './static/controller/exam/categoryManage.js'
	        }
	    },{
	        id: 'exam-examination',
	        title: Wz.lang.menu.exam_exampage_manage,
	        parrentTitle: Wz.lang.menu.exam_manage,
	        permission: Wz.getPermission('exam/exam/list'),
	        className: 'Wz.exam.examinationManage',
	        files: {
	            tpl: './static/view/exam/examinationManage.html',
	            controller: './static/controller/exam/examinationManage.js'
	        }
	    }]
	},{
	    id: 'survey',
	    title: Wz.lang.menu.survey_manage,
	    permission: Wz.getPermission('survey/survey/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'survey-manage',
	        title: Wz.lang.menu.survey_survey_manage,
	        parrentTitle: Wz.lang.menu.survey_manage,
	        permission: Wz.getPermission('survey/survey/list'),
	        className: 'Wz.survey.surveyManage',
	        files: {
	            tpl: './static/view/survey/surveyManage.html',
	            controller: './static/controller/survey/surveyManage.js'
	        }
	    }]
	},{
	    id: 'qa',
	    title: Wz.lang.menu.qa_manage,
	    permission: Wz.getPermission('qa/category/list') || Wz.getPermission('qa/question/list'),
	    icon: './static/images/leftico03.png',
	    children: [{
	        id: 'qa-category',
	        title: Wz.lang.menu.qa_category_manage,
	        parrentTitle: Wz.lang.menu.qa_manage,
	        permission: Wz.getPermission('qa/category/list'),
	        className: 'Wz.qa.categoryManage',
	        files: {
	            tpl: './static/view/qa/categoryManage.html',
	            controller: './static/controller/qa/categoryManage.js'
	        }
	    },{
	        id: 'qa-question',
	        title: Wz.lang.menu.qa_question_manage,
	        parrentTitle: Wz.lang.menu.qa_manage,
	        permission: Wz.getPermission('qa/question/list'),
	        className: 'Wz.qa.questionManage',
	        files: {
	            tpl: './static/view/qa/questionManage.html',
	            controller: './static/controller/qa/questionManage.js'
	        }
	    }]
	},{
	    id: 'credits',
	    title: Wz.lang.menu.credits,
	    permission: Wz.getPermission('credits/overview/list') || Wz.getPermission('credits/grant/list') || Wz.getPermission('credits/exchange/list') || Wz.getPermission('credits/rule/list'),
	    icon: './static/images/leftico03.png',
	    children: [{
	        id: 'credits-overview',
	        title: Wz.lang.menu.credits_overview,
	        parrentTitle: Wz.lang.menu.credits,
	        permission: Wz.getPermission('credits/overview/list'),
	        className: 'Wz.credits.overviewManage',
	        files: {
	            tpl: './static/view/credits/overviewManage.html',
	            controller: './static/controller/credits/overviewManage.js'
	        }
	    },{
	        id: 'credits-grant',
	        title: Wz.lang.menu.credits_grant,
	        parrentTitle: Wz.lang.menu.credits,
	        permission: Wz.getPermission('credits/overview/list'),
	        className: 'Wz.credits.grantManage',
	        files: {
	            tpl: './static/view/credits/grantManage.html',
	            controller: './static/controller/credits/grantManage.js'
	        }
	    },{
	        id: 'credits-exchange',
	        title: Wz.lang.menu.credits_exchange,
	        parrentTitle: Wz.lang.menu.credits,
	        permission: Wz.getPermission('credits/overview/list'),
	        className: 'Wz.credits.exchangeManage',
	        files: {
	            tpl: './static/view/credits/exchangeManage.html',
	            controller: './static/controller/credits/exchangeManage.js'
	        }
	    },{
	        id: 'credits-rule',
	        title: Wz.lang.menu.credits_rule,
	        parrentTitle: Wz.lang.menu.credits,
	        permission: Wz.getPermission('credits/rule/list'),
	        className: 'Wz.credits.ruleManage',
	        files: {
	            tpl: './static/view/credits/ruleManage.html',
	            controller: './static/controller/credits/ruleManage.js'
	        }
	    }]
	},{
	    id: 'absence',
	    title: Wz.lang.menu.absence_manage,
	    permission: Wz.getPermission('absence/absence/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'absence-manage',
	        title: Wz.lang.menu.absence_absence_manage,
	        parrentTitle: Wz.lang.menu.absence_manage,
	        permission: Wz.getPermission('absence/absence/list'),
	        className: 'Wz.absence.absenceManage',
	        files: {
	            tpl: './static/view/absence/absenceManage.html',
	            controller: './static/controller/absence/absenceManage.js'
	        }
	    }]
	},{
		id: 'clock',
		title:Wz.lang.menu.clock,
		permission: Wz.getPermission('clock/product/list') || Wz.getPermission('clock/customer/list'),
		icon: './static/images/leftico04.png',
		children:[{
				id: 'clock-product',
				title:Wz.lang.menu.clock_product_manage,
				parrentTitle: Wz.lang.menu.clock,
				permission: Wz.getPermission('clock/product/list'),
				className: 'Wz.clock.productManage',
				files: {
					tpl: './static/view/clock/productManage.html',
					controller: './static/controller/clock/productManage.js'
				}
				},{
				id: 'clock-customer',
				title:Wz.lang.menu.clock_customer_manage,
				parrentTitle: Wz.lang.menu.clock,
				permission: Wz.getPermission('clock/customer/list'),
				className: 'Wz.clock.customerManage',
				files: {
					tpl: './static/view/clock/customerManage.html',
					controller: './static/controller/clock/customerManage.js'
				}			
		}]
	},{
	    id: 'offline-training',
	    title: Wz.lang.menu.offline_training_manage,
	    permission: Wz.getPermission('offlineTraining/training/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'offline-training-manage',
	        title: '培训班级管理',
	        parrentTitle: Wz.lang.menu.offline_training_manage,
	        permission: Wz.getPermission('offlineTraining/training/list'),
	        className: 'Wz.offlineTraining.trainingManage',
	        files: {
	            tpl: './static/view/offlineTraining/trainingManage.html',
	            controller: './static/controller/offlineTraining/trainingManage.js'
	        }
	    },{
	        id: 'training-course-manage',
	        title: '课程管理',
	        parrentTitle: Wz.lang.menu.offline_training_manage,
	        permission: Wz.getPermission('offlineTraining/course/list'),
	        className: 'Wz.offlineTraining.courseManage',
	        files: {
	            tpl: './static/view/offlineTraining/courseManage.html',
	            controller: './static/controller/offlineTraining/courseManage.js'
	        }
	    },{
	        id: 'training-auth-manage',
	        title: '讲师认证',
	        parrentTitle: Wz.lang.menu.offline_training_manage,
	        permission: Wz.getPermission('offlineTraining/auth/list'),
	        className: 'Wz.offlineTraining.authManage',
	        files: {
	            tpl: './static/view/offlineTraining/authManage.html',
	            controller: './static/controller/offlineTraining/authManage.js'
	        }
	    },{
	        id: 'training-lecturer-manage',
	        title: '讲师管理',
	        parrentTitle: Wz.lang.menu.offline_training_manage,
	        permission: Wz.getPermission('offlineTraining/lecturer/list'),
	        className: 'Wz.offlineTraining.lecturerManage',
	        files: {
	            tpl: './static/view/offlineTraining/lecturerManage.html',
	            controller: './static/controller/offlineTraining/lecturerManage.js'
	        }
	    },{
	        id: 'training-arrangement-manage',
	        title: '课程安排',
	        parrentTitle: Wz.lang.menu.offline_training_manage,
	        permission: Wz.getPermission('offlineTraining/arrangement/list'),
	        className: 'Wz.offlineTraining.arrangementManage',
	        files: {
	            tpl: './static/view/offlineTraining/arrangementManage.html',
	            controller: './static/controller/offlineTraining/arrangementManage.js'
	        }
	    }]
	}/*,{
	    id: 'auth',
	    title: '学员认证',
	    permission: Wz.getPermission('offlineTraining/training/list'),
	    icon: './static/images/leftico04.png',
	    children: [{
	        id: 'auth-map-manage',
	        title: '地图设置',
	        parrentTitle: '学员认证',
	        permission: true,//Wz.getPermission('auth/map/list'),
	        className: 'Wz.auth.mapManage',
	        files: {
	            tpl: './static/view/auth/mapManage.html',
	            controller: './static/controller/auth/mapManage.js'
	        }
	    },{
	        id: 'auth-manage',
	        title: '认证管理',
	        parrentTitle: '学员认证',
	        permission: true,//Wz.getPermission('auth/auth/list'),
	        className: 'Wz.auth.authManage',
	        files: {
	            tpl: './static/view/auth/authManage.html',
	            controller: './static/controller/auth/authManage.js'
	        }
	    }]
	}*/];
})