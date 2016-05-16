/*
 *请假管理-请假管理 
 * 
 */
Wz.namespace('Wz.offlineTraining');
Wz.offlineTraining.authManage = function(){
	 $.parser.parse('#main-contain');
	 var searchForm = $('#training-auth-search-form');
	 var editWin = $('#training-auth-edit-win');
	 var editForm = $('#training-auth-edit-form');
	 var authUserWin = $('#training-auth-user-win');
	 var noticeWin = $('#training-auth-notice-win');
	 var endTime = editForm.find('input[name=end_time]').datetimebox({
    	editable: false,
        required: true,
        prompt: '请选择截止时间',
        width: 375
	 });
	 var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
	 
	 var authCourse = editForm.find('input[name=course_id]').combogrid({
	    	/*url: './api/discover/get_discover_item.json',
	    	queryParams: courseParam,*/
		 	data: {
				 recordsFiltered: 3,
				 data: [{
					 course_id: 1,
					 course_name: '一线主管的角色定位',
					 course_desc: '管理的定义、管理的作用及存在价值',
					 auth_cnt: 3,
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
					 discover_item_id: [],
					 image_name: '976454cc075486dc474b27afc25eb307.png',
					 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/976454cc075486dc474b27afc25eb307.png',
					 create_admin_name: '谢丽娜',
					 create_time: new Date(2016,4,21,12,44,12).getTime()/1000
				 }]
			},
	        fitColumns: true,
	        checkOnSelect: true,
	        striped: true,
	        pagination: true,
	        rownumbers: true,
	        required: true,
	        pageSize: 20,
	        idField: 'course_id',
	        textField: 'course_name',
	        editable: false,
	        width: 375,
	        panelWidth: 400,
	        toolbar: '#training-auth-course-tb',
	        frozenColumns: [[{
	        	field: 'ck',
	        	checkbox: true
	        },{
	            field: 'course_name',
	            title: '课程名称',
	            width: '100%'
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
	 var searchTeam = authUserWin.find('input[name=team_id]').combotree({
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
	 var authTable = $('#training-auth-table').datagrid({
		 /*url:'./api/offline_training/get_train_list.json',
		 queryParams:query_params,*/
		 data: {
			 recordsFiltered: 3,
			 data: [{
				 auth_id: 1,
				 auth_name: '国家初级绩效改进师认证',
				 start_time: new Date(2016,5,2,12,44,12).getTime()/1000,
				 end_time: new Date(2016,4,2,12,44,12).getTime()/1000,
				 state: 'AUTH_RUNING',
				 course_id: 1,
				 course_name: '一线主管的角色定位',
				 apply_ctn: 100,
				 pass_ctn: 89,
				 fail_ctn: 10,
				 undo_ctn: 1,
				 auth_desc: '国家绩效改进师既是绩效改进领域的专业人才，同时也是企业各级管理者能力提升的重要标志，将按照全国统一标准分为初级、中级和高级进行系统地培养、考核和管理。初级绩效改进师需要理解绩效改进的基本原理、流程、方法和工具；部分或完整参与一个有关绩效改进项目。',
				 discover_item_id: [],
				 image_name: '23e72a5216c55168cb25a3a46f4b03a0.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/23e72a5216c55168cb25a3a46f4b03a0.png',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,2,12,44,12).getTime()/1000
			 },{
				 auth_id: 2,
				 auth_name: '萃取讲师认证',
				 start_time: new Date(2016,4,12,12,44,12).getTime()/1000,
				 end_time: new Date(2016,4,2,12,44,12).getTime()/1000,
				 state: 'AUTH_END',
				 course_id: 2,
				 course_name: '用户界面设计原则',
				 apply_ctn: 100,
				 pass_ctn: 90,
				 fail_ctn: 10,
				 undo_ctn: 0,
				 auth_desc: '',
				 discover_item_id: [],
				 image_name: 'a88d1ebd3dd3a4d3d0e7e51d7aa90ac9.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/a88d1ebd3dd3a4d3d0e7e51d7aa90ac9.png',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,1,12,44,12).getTime()/1000
			 },{
				 auth_id: 3,
				 auth_name: 'UED设计讲师认证',
				 start_time: new Date(2016,4,2,12,44,12).getTime()/1000,
				 end_time: new Date(2016,3,22,12,44,12).getTime()/1000,
				 state: 'AUTH_RUNING',
				 course_id: 3,
				 course_name: '知识萃取',
				 apply_ctn: 50,
				 pass_ctn: 32,
				 fail_ctn: 10,
				 undo_ctn: 8,
				 auth_desc: '用户界面设计原则',
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
	    	 field: 'ck',
	    	 checkbox: true
	     },{
	    	 field:'auth_name',
	    	 title:'认证名称',
	    	 width:'20%'
	     },{
	    	 field:'course_name',
	    	 title:'课程名称',
	    	 width:'15%'
	     },{
	    	 field:'end_time',
	    	 title:'认证截止时间',
	    	 width:'15%',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'state',
	    	 title:'认证状态',
	    	 align:'center',
	    	 width:'5%',
	         formatter: function(val,obj,row){
	        	 return val == 'AUTH_RUNING'?'进行中':(val=='AUTH_END'?'已结束':'未开始');
	         }
	     },{
	    	 field:'apply_ctn',
	    	 title:'总报名人数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'pass_ctn',
	    	 title:'已通过人数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'fail_ctn',
	    	 title:'已失败人数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'undo_ctn',
	    	 title:'未处理人数',
	    	 width:'6%',
	    	 align: 'center'
	     },{
	    	 field:'auth_id',
	    	 title:'操作',
	    	 width:'15%',
	    	 align:'center',
	         hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/auths')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 return (obj.state=='AUTH_RUNING'?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.authManage.editAuth('+row+')" href="javascript:void(0);">编辑</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.authManage.delAuth('+row+')" href="javascript:void(0);">删除</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/update')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.authManage.showItems('+row+')" href="javascript:void(0);">关闭认证</a>':'');
	    	 }
	     }]],
	     toolbar:[{
	    	 id: 'training-auth-add-btn',
	    	 text: '新建',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 iconCls: 'icon-add',
	    	 handler: function(){
	    		 editForm.form('reset');
	    		 editForm.find('input[name=auth_id]').val('');
	    		 editWin.window({title:'新建认证'}).window('open');
	         }
	     },{
	    	 id: 'training-auth-del-btn',
	    	 text: '批量删除',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 iconCls: 'icon-remove',
	    	 handler: function(){
	    		var auth = authTable.datagrid('getChecked');
	    		if(auth.length == 0){
	    			$.messager.alert('错误','请选择需要删除的认证！','error');
	    			return false;
	    		}
				$.messager.confirm('提示','请确认是否要删除选中的认证？',function(ok){
					if(ok){
						for(var i=0;i<auth.length;i++){
							authTable.datagrid('deleteRow',authTable.datagrid('getRowIndex',auth[i]));							
						}
					}
				});
	         }
	     }],
	     onClickCell: function(index,field,value){
	    	if(field == 'pass_ctn' || field == 'apply_ctn' || field == 'fail_ctn' || field == 'undo_ctn'){
	    		authUserWin.window('open');
	    	}
	     },
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
	 
	 var authUserTable = $('#training-auth-user-table').datagrid({
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
	    	 field: 'ck',
	    	 checkbox: true
	     },{
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
	    	 field:'state',
	    	 title:'认证状态',
	    	 align:'center',
	    	 width:'10%',
	         formatter: function(val,obj,row){
	        	 return val == 'AUTH_PASS'?'已通过':(val=='AUTH_FAIL'?'未通过':'待认证');
	         }
	     },{
	    	 field:'auth_id',
	    	 title:'操作',
	    	 width:'20%',
	    	 align:'center',
	         hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/auths')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 return (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.authManage.authPass('+row+')" href="javascript:void(0);">审核通过</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/update')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.authManage.authFail('+row+')" href="javascript:void(0);">审核不通过</a>':'');
	    	 }
	     }]],
	     toolbar:[{
	    	 id: 'training-auth-user-pass-btn',
	    	 text: '审核通过',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 //iconCls: 'icon-add',
	    	 handler: function(){
	    		var user = authUserTable.datagrid('getChecked');
	    		if(user.length == 0){
	    			$.messager.alert('错误','请选择需要审核的讲师！','error');
	    			return false;
	    		}
				$.messager.confirm('提示','请确认是否要将选中讲师审核通过',function(ok){
					if(ok){
						
					}
				});
	         }
	     },{
	    	 id: 'training-auth-del-btn',
	    	 text: '审核不通过',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 //iconCls: 'icon-remove',
	    	 handler: function(){
	    		 var user = authUserTable.datagrid('getChecked');
		    		if(user.length == 0){
		    			$.messager.alert('错误','请选择需要审核的讲师！','error');
		    			return false;
		    		}
					$.messager.confirm('提示','请确认是否要将选中讲师审核不通过',function(ok){
						if(ok){
							
						}
					});
	         }
	     },{
	    	 id: 'training-auth-del-btn',
	    	 text: '发送通知',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 //iconCls: 'icon-remove',
	    	 handler: function(){
	    		 noticeWin.window('open');
	         }
	     },{
	    	 id: 'training-auth-del-btn',
	    	 text: '导出人员',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 //iconCls: 'icon-remove',
	    	 handler: function(){
	    		 
	         }
	     }],
	     onClickCell: function(index,field,value){
	    	if(field == 'pass_ctn' || field == 'apply_ctn' || field == 'fail_ctn' || field == 'undo_ctn'){
	    		authUserWin.window('open');
	    	}
	     },
	     changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
	    loadFilter: function(data){
	    	var state = ['AUTH_WAIT','AUTH_PASS','AUTH_FAIL'];
	    	for(var i=0;i<data.data.length;i++){
	    		data.data[i].state = state[Math.floor(Math.random()*3)];
	    	}
            return {
                total: data.recordsFiltered,
                rows: data.data
            };
        }
	 })
	 
	 function editAuth(row){
		 editForm.form('reset');
		 var auth = $.extend({},authTable.datagrid('getRows')[row]);
		 auth.end_time = Wz.dateFormat(new Date(auth.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		 editForm.form('load',auth);
		 editWin.window({title:'编辑认证'}).window('open');
	 }
	 
	 function saveAuth(){
		 if(!editForm.form('validate')){
			 $.messager.alert('错误','请正确填写表单','error');
			 return false;
		 }
		 var allow_model_id = allowModel.getValue();
		 var allow_model_name = allowModel.getName();
		 var auth_id = 101;
		 var end_time = endTime.datetimebox('getValue');
		 var auth_name = editForm.find('input[name=auth_name]').val();
		 var auth_desc = editForm.find('input[name=auth_desc]').val();
		 var course_id = editForm.find('input[textboxname=course_id]').combogrid('getValue');
		 var course_name = editForm.find('input[textboxname=course_id]').combogrid('getText');
		 var now = new Date().getTime()/1000;
		 end_time = Wz.parseDate(end_time).getTime()/1000;
		 if(end_time == ''){
			$.messager.alert('错误','认证截止时间不能为空！','error');
			return false;
		 }
		 if(end_time < now){
			$.messager.alert('错误','认证截止时间不能小于当前时间！','error');
			return false;
		 }
		 authTable.datagrid('insertRow',{
			index: 0,
			row: {
				auth_id: auth_id,
				auth_name: auth_name,
				auth_desc: auth_desc,
				state: 'AUTH_RUNING',
				auth_cnt: 0,
				end_time: end_time,
				discover_item_id: [],
				create_admin_name: '赵君',
				course_name: course_name,
				course_id: course_id,
				apply_ctn: 100,
				pass_ctn: 0,
				fail_ctn: 0,
				undo_ctn: 100,
				allow_model_name: allow_model_name,
				allow_model_id: allow_model_id,
				create_time: new Date().getTime()/1000
			}
		 });
		 editWin.window('close');	 
	 }
	 
	 function delAuth(row){
		var auth = $.extend({},authTable.datagrid('getRows')[row]);
		$.messager.confirm('提示','请确认是否要删除课程？',function(ok){
    		if(ok){
    			authTable.datagrid('deleteRow',row);    			
    		}
    	});
	 }
	 function showItems(row){
		var auth = $.extend({},authTable.datagrid('getRows')[row]);
		itemParam.item_id = (auth.discover_item_id||[]).join(',');
		itemListWin.find('input[name=auth_id]').val(auth.train_id);
    	itemListTable.datagrid('reload');
    	itemListWin.window('open');
	 }
	 
	 function authPass(row){
		 $.messager.confirm('提示','请确认是否要将该讲师审核通过？',function(ok){
    		if(ok){
    			   			
    		}
    	});
	 }
	 function authFail(row){
		 $.messager.confirm('提示','请确认是否要将该讲师审核不通过？',function(ok){
    		if(ok){
    			   			
    		}
    	});
	 }
	 function sendNotice(){
		 var notice_text = noticeWin.find('input[name=notice_text]').val();
		 if(notice_text.length == ''){
			 $.messager.alert('错误','请填写通知内容！','error');
			 return false;
		 }
		 $.messager.alert('提示','消息发送成功！','info');
		 noticeWin.window('close');
	 }
	return {
		editAuth: editAuth,
		saveAuth: saveAuth,
		delAuth: delAuth,
		showItems: showItems,
		authPass: authPass,
		authFail: authFail,
		sendNotice: sendNotice
	};
}()