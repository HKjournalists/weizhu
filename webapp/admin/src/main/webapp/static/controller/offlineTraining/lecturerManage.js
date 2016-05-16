/*
 *请假管理-请假管理 
 * 
 */
Wz.namespace('Wz.offlineTraining');
Wz.offlineTraining.lecturerManage = function(){
	 $.parser.parse('#main-contain');
	 var searchForm = $('#training-lecturer-search-form');
	 var editWin = $('#training-lecturer-edit-win');
	 var editForm = $('#training-lecturer-edit-form');
	 var courseWin = $('#training-lecturer-course-win');
	 var arrangementWin = $('#training-lecturer-arrangement-win');
	 var searchTeam = searchForm.find('input[name=team_id]').combotree({
	    	panelWidth: 200,
	    	url: './api/user/get_team.json',
	    	editable: false,
	    	cascadeCheck: false,
	    	onBeforeExpand: function(row){
	    		searchTeam.combotree('options').queryParams.team_id = row.team_id;
	    		return true;
	    	},
	    	onShowPanel: function(){
	    		searchTeam.combotree('options').queryParams.team_id = '';
	    		searchTeam.combotree('reload');
	    	},
	    	loadFilter: function(data){
	    		if(!!data){
	    			for(var i=0;i<data.length;i++){
	    				data[i].id = data[i].team_id;
	    				data[i].text = data[i].team_name;
	    				data[i].state = (data[i].has_sub_team?'closed':'open');
	    			}
	    			if(searchTeam.combotree('options').queryParams.team_id == ''){
	    				data.unshift({
	    					id: '',
	    					text: '---全部部门---'
	    				});
	    			}
	    		}
	    		return data;
	    	}
	    });
	 var lecturerTable = $('#training-lecturer-table').datagrid({
		 url: './api/user/get_user_list.json',
	     queryParams: {
	    	 mobile_no: '7'
	     },
		 fitColumns: true,
		 checkOnSelect: true,
		 autoRowHeight: true,
		 striped:true,
		 nowrap: false,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     frozenColumns:[[{
	    	 field:'user_name',
	    	 title:'姓名',
	    	 width:'10%'
	     },{
	    	 field:'mobile_no',
	    	 title:'手机号',
	    	 width:'10%',
             align: 'center',
             formatter: function(val,obj,row){
             	return !!val?val.join('<br/>'): '';
             }
	     },{
	    	 field:'team',
	    	 title:'部门',
	    	 width:'30%',
			 formatter: function(val,obj,row){
				val = val || [];
				var team = [];
				for(var i=0;i<val.length;i++){
					team.push(val[i].team_name);
				}
				team = team.join('-');
			 	return '<span title="'+team+'" class="easyui-tooltip">'+team + '</span>';
			 }
	     },{
	    	 field:'auth_course_ctn',
	    	 title:'已认证课程数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'score',
	    	 title:'平均评分',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'teach_course_ctn',
	    	 title:'已讲课程数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'unteach_course_ctn',
	    	 title:'未讲课程数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'lecturer_id',
	    	 title:'操作',
	    	 width:'15%',
	    	 align:'center',
	         hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/lecturers')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 return (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.lecturerManage.showCourse('+row+')" href="javascript:void(0);">认证课程</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/update')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.lecturerManage.showArrangement('+row+')" href="javascript:void(0);">课表</a>':'');
	    	 }
	     }]],
	     onClickCell: function(index,field,value){
	    	if(field == 'pass_ctn' || field == 'apply_ctn' || field == 'fail_ctn' || field == 'undo_ctn'){
	    		lecturerUserWin.window('open');
	    	}
	     },
	     changePages: function(params,pageObj){
	        	$.extend(params,{
	        		start: (pageObj.page-1)*pageObj.rows,
	        		length: pageObj.rows
	        	});
	        },
	      loadFilter: function(data){
	    	  	for(var i=0;i<data.data.length;i++){
	    	  		data.data[i].auth_course_ctn=Math.ceil(Math.random()*20);
	    	  		data.data[i].score = Math.ceil(Math.random()*50)/10;
	    	  		data.data[i].teach_course_ctn = Math.ceil(Math.random()*data.data[i].auth_course_ctn);
	    	  		data.data[i].unteach_course_ctn = data.data[i].auth_course_ctn-data.data[i].teach_course_ctn;
		    	}
	            return {
	                total: data.recordsFiltered,
	                rows: data.data
	            };
	        }
	 });
	 
	 var courseTable = $('#training-lecturer-course-table').datagrid({
		 data: {
			 recordsFiltered: 3,
			 data: [{
				 course_id: 1,
				 course_name: '一线主管的角色定位',
				 course_desc: '管理的定义、管理的作用及存在价值',
				 auth_cnt: 3,
				 teach_cnt: 6,
				 discover_item_id: [],
				 image_name: '23e72a5216c55168cb25a3a46f4b03a0.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/23e72a5216c55168cb25a3a46f4b03a0.png',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,2,12,44,12).getTime()/1000
			 },{
				 course_id: 2,
				 course_name: '用户界面设计原则',
				 course_desc: '设计是通过阐明，简化，明确，修饰使之庄严，有说服性',
				 auth_cnt: 2,
				 teach_cnt: 11,
				 discover_item_id: [],
				 image_name: 'a88d1ebd3dd3a4d3d0e7e51d7aa90ac9.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/a88d1ebd3dd3a4d3d0e7e51d7aa90ac9.png',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,1,12,44,12).getTime()/1000
			 },{
				 course_id: 3,
				 course_name: '知识萃取',
				 course_desc: '企业如何做好知识萃取',
				 auth_cnt: 4,
				 teach_cnt: 2,
				 discover_item_id: [],
				 image_name: '976454cc075486dc474b27afc25eb307.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/976454cc075486dc474b27afc25eb307.png',
				 create_admin_name: '谢丽娜',
				 create_time: new Date(2016,4,21,12,44,12).getTime()/1000
			 }]
		 },
		 fitColumns: true,
		 checkOnSelect: true,
		 autoRowHeight: true,
		 striped:true,
		 nowrap: false,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     frozenColumns:[[{
	    	 field:'course_name',
	    	 title:'课程名称',
	    	 width:'25%'
	     },{
	    	 field:'course_desc',
	    	 title:'课程简介',
	    	 width:'30%'
	     },{
	    	 field:'teach_cnt',
	    	 title:'授课次数',
	    	 align:'center',
	    	 width:'10%'
	     },{
	    	 field:'create_time',
	    	 title:'完成时间',
	    	 width:'20%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     }]],
	     changePages: function(params,pageObj){
	        	$.extend(params,{
	        		start: (pageObj.page-1)*pageObj.rows,
	        		length: pageObj.rows
	        	});
	        },
	      loadFilter: function(data){
	            return {
	                total: data.recordsFiltered,
	                rows: data.data
	            };
	        }
	 });

	 var arrangementTable = $('#training-lecturer-arrangement-table').datagrid({
		 data: {
			 recordsFiltered: 3,
			 data: [{
				 arrangement_id: 1,
				 course_name: '一线主管的角色定位',
				 course_id: 1,
				 course_desc: '管理的定义、管理的作用及存在价值',
				 start_time: new Date(2016,4,20,12,44,12).getTime()/1000,
				 end_time: new Date(2016,4,20,16,44,12).getTime()/1000,
				 arrangement_addr: '北京市 海淀区 中关村软件园 华夏大厦 302',
				 user_id: 12,
				 user_name: '刘菲',
				 apply_state: 'APPLY_RUNING',
				 course_state: 'COURSE_NOT_START',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,2,12,44,12).getTime()/1000
			 },{
				 arrangement_id: 2,
				 course_name: '用户界面设计原则',
				 course_id: 2,
				 course_desc: '设计是通过阐明，简化，明确，修饰使之庄严，有说服性',
				 start_time: new Date(2016,4,12,9,44,12).getTime()/1000,
				 end_time: new Date(2016,4,12,16,44,12).getTime()/1000,
				 arrangement_addr: '北京市 海淀区 中关村软件园 华夏大厦 302',
				 user_id: 33,
				 user_name: '李小冉',
				 apply_state: 'APPLY_END',
				 course_state: 'COURSE_RUNING',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,1,12,44,12).getTime()/1000
			 },{
				 arrangement_id: 3,
				 course_name: '知识萃取',
				 course_id: 3,
				 course_desc: '企业如何做好知识萃取',
				 start_time: new Date(2016,4,10,9,44,12).getTime()/1000,
				 end_time: new Date(2016,4,10,16,44,12).getTime()/1000,
				 arrangement_addr: '北京市 海淀区 中关村软件园 华夏大厦 302',
				 user_id: 34,
				 user_name: '权志龙',
				 apply_state: 'APPLY_END',
				 course_state: 'COURSE_END',
				 create_admin_name: '谢丽娜',
				 create_time: new Date(2016,4,1,12,44,12).getTime()/1000
			 }]
		 },
		 fitColumns: true,
		 checkOnSelect: true,
		 autoRowHeight: true,
		 striped:true,
		 nowrap: false,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     frozenColumns:[[{
	    	 field:'course_name',
	    	 title:'课程名称',
	    	 width:'25%'
	     },{
	    	 field:'start_time',
	    	 title:'课程开始时间',
	    	 width:'25%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'end_time',
	    	 title:'课程结束时间',
	    	 align:'center',
	    	 width:'25%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'state',
	    	 title:'预约状态',
	    	 width:'10%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return val=='NOT_START'?'未开始':(val=='RUNING'?'进行中':'已完成');
	    	 }
	     }]],
	     toolbar:'#training-lecturer-arrangement-table-tb',
	     changePages: function(params,pageObj){
	        	$.extend(params,{
	        		start: (pageObj.page-1)*pageObj.rows,
	        		length: pageObj.rows
	        	});
	        },
	      loadFilter: function(data){
	            return {
	                total: data.recordsFiltered,
	                rows: data.data
	            };
	        }
	 });
		 
		 
	 function showCourse(row){
		var lecturer = $.extend({},lecturerTable.datagrid('getRows')[row]);
    	courseWin.window('open');
	 }
	 
	 function showArrangement(row){
		 arrangementWin.window('open');
	 } 
	 
	return {
		showCourse: showCourse,
		showArrangement: showArrangement
	};
}()