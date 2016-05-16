/*
 *请假管理-请假管理 
 * 
 */
Wz.namespace('Wz.offlineTraining');
Wz.offlineTraining.arrangementManage = function(){
	 $.parser.parse('#main-contain');
	 var searchForm = $('#training-arrangement-search-form');
	 var editWin = $('#training-arrangement-edit-win');
	 var editForm = $('#training-arrangement-edit-form');
	 var itemListWin = $('#training-arrangement-itemlist-win');
	 var selectListWin = $('#training-arrangement-selectitem-win');
	 var resultWin = $('#training-arrangement-result-win');
	 var ercodeWin = $('#training-arrangement-show-ercode-win');
	 var allitemSearchForm = $('#training-arrangement-allitem-talbe-tb');
	 var resultSearchForm = $('#training-arrangement-result-talbe-tb');
	 var userSelectWin = $('#training-arrangement-select-user-win');
	 var uploadImge = editForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 375,
    	tipInfo: '图片格式必须为:png,jpg,gif；<br>图片质量不可大于1M,图片尺寸建议640*320(px)',
    	maxSize: 1,
    	params: {
    		image_tag: '培训,图标'
    	}
    });
	 var queryPosition = $('#training-arrangement-user-tb input[name=position_id]').combobox({
     	mode: 'remote',
     	valueField: 'position_id',
     	textField: 'position_name',
     	editable: false,
     	cascadeCheck: false,
     	height: 'auto',
     	loader: function(param,success,error){
 			Wz.ajax({
 				url: './api/user/get_position.json',
 				success: function(json){
 					success(json.position);
 				}
 			});
 		},
 		onShowPanel: function(){
 			$(this).combobox('reload');
 		}
     });
 	var queryTeam = $('#training-arrangement-user-tb input[name=team_id]').combotree({
 		panelWidth: 200,
     	url: './api/user/get_team.json',
     	editable: false,
     	cascadeCheck: false,
     	onBeforeExpand: function(row){
     		queryTeam.combotree('options').queryParams.team_id = row.team_id;
     		return true;
     	},
     	onShowPanel: function(){
     		queryTeam.combotree('options').queryParams.team_id = '';
     		queryTeam.combotree('reload');
     	},
     	loadFilter: function(data){
     		if(!!data){
     			for(var i=0;i<data.length;i++){
     				data[i].id = data[i].team_id;
     				data[i].text = data[i].team_name;
     				data[i].state = (data[i].has_sub_team?'closed':'open');
     			}
     			if(queryTeam.combotree('options').queryParams.team_id == ''){
     				data.unshift({
     					id: '',
     					text: '---全部部门---'
     				});
     			}
     		}
     		return data;
     	}
     });
	 var lecturerParam = {
				team_id: '',
		    	position_id: '',
		    	keyword: '',
		    	mobile_no: ''
			 };
	 var searchUser = searchForm.find('input[name=user_id]').combogrid({
	    	url: './api/user/get_user_list.json',
	    	queryParams: lecturerParam,
	        fitColumns: true,
	        checkOnSelect: true,
	        striped: true,
	        pagination: true,
	        rownumbers: true,
	        required: true,
	        pageSize: 20,
	        idField: 'user_id',
	        textField: 'user_name',
	        editable: false,
	        panelWidth: 700,
	        toolbar: '#training-arrangement-user-tb',
	        frozenColumns: [[{
	        	field: 'ck',
	        	checkbox: true
	        },{
	            field: 'user_name',
	            title: '姓名',
	            width: '80px',
	            align: 'center',
	            formatter: function(val,obj,row){
	            	return !!val?val:'---';
	            }
	        },{
	            field: 'gender',
	            title: '性别',
	            width: '40px',
	            align: 'center',
	            formatter: function(val,obj,row){
	            	return val=='FEMALE'?'女':'男';
	            }
	        },{
	            field: 'mobile_no',
	            title: '手机号',
	            width: '100px',
	            align: 'center',
	            formatter: function(val,obj,row){
	            	return !!val?val.join('<br/>'): '';
	            }
	        },{
	            field: 'team',
	            title: '部门',
	            width: '250px',
	            align: 'center',
	            formatter: function(val,obj,row){
	            	val = val || [];
	            	var team = [];
	            	for(var i=0;i<val.length;i++){
	            		team.push(val[i].team_name);
	            	}
	            	team = team.join('-');
	            	return '<span title="'+team+'" class="easyui-tooltip">'+team + '</span>';
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
	 $('#training-arrangement-user-tb .search-btn').click(function(){
		 lecturerParam.team_id = queryTeam.combotree('getValue');
		 lecturerParam.position_id = queryPosition.combobox('getValue');
		 lecturerParam.keyword = $('#training-arrangement-user-tb input[textboxname=keyword]').textbox('getValue');
		 lecturerParam.mobile_no = $('#training-arrangement-user-tb input[textboxname=mobile_no]').textbox('getValue');
		 searchUser.combogrid('grid').datagrid('reload');
	 });
	 
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
	        toolbar: '#training-arrangement-course-tb',
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
	 
	 var startTime = editForm.find('input[name=start_time]').datetimebox({
    	editable: false,
        required: true,
        onChange: function(newValue,oldValue){
        	var st = Wz.parseDate(newValue).getTime();
        	var et = endTime.datetimebox('getValue');
        	if(et == ''){
        		endTime.datetimebox('setValue',Wz.dateFormat(new Date(st+4*3600000),'yyyy-MM-dd hh:mm:ss'));
        	}
        	var aet = applyEndTime.datetimebox('getValue');
        	if(aet == ''){
        		applyEndTime.datetimebox('setValue',Wz.dateFormat(new Date(st-3*3600000),'yyyy-MM-dd hh:mm:ss'));
        	}
        }
	 });
	 var endTime = editForm.find('input[name=end_time]').datetimebox({
    	editable: false,
        required: true
	 });
	 var applyEndTime = editForm.find('input[name=apply_end_time]').datetimebox({
		editable: false,
	    required: true
	});
	 
	 var arrangementTable = $('#training-arrangement-table').datagrid({
		 /*url:'./api/offline_training/get_train_list.json',
		 queryParams:query_params,*/
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
	    	field: 'ck',
	    	checkbox: true
	     },{
	    	 field:'course_name',
	    	 title:'课程名称',
	    	 width:'20%'
	     },{
	    	 field:'start_time',
	    	 title:'课程开始时间',
	    	 width:'15%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'end_time',
	    	 title:'课程结束时间',
	    	 align:'center',
	    	 width:'15%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'user_name',
	    	 title:'讲师',
	    	 width:'8%',
	    	 align: 'center'
	     },{
	    	 field:'apply_state',
	    	 title:'预约状态',
	    	 width:'8%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return val=='APPLY_RUNING'?'预约中':'预约已结束';
	    	 }
	     },{
	    	 field:'course_state',
	    	 title:'课程状态',
	    	 width:'8%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return val=='COURSE_NOT_START'?'未开始':(val=='COURSE_RUNING'?'进行中':'已结束');
	    	 }
	     },{
	    	 field:'arrangement_id',
	    	 title:'操作',
	    	 width:'14%',
	    	 align:'center',
	         hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/arrangements')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 return (Wz.getPermission('offlineTraining/training/update')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.arrangementManage.editArrangement('+row+')" href="javascript:void(0);">编辑</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.arrangementManage.delArrangement('+row+')" href="javascript:void(0);">删除</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.arrangementManage.showResult('+row+')" href="javascript:void(0);">查看结果</a>':'');
	    	 }
	     }]],
	     toolbar:[{
	    	 id: 'training-arrangement-add-btn',
	    	 text: '新建',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 iconCls: 'icon-add',
	    	 handler: function(){
	    		 editForm.form('reset');
	    		 uploadImge.reset();
	    		 editForm.find('input[name=arrangement_id]').val('');
	    		 editWin.window({title:'新建课程'}).window('open');
	         }
	     },{
	    	 id: 'training-arrangement-add-btn',
	    	 text: '批量删除',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 iconCls: 'icon-remove',
	    	 handler: function(){
	    		var arrangement = arrangementTable.datagrid('getChecked');
	    		if(arrangement.length == 0){
	    			$.messager.alert('错误','请选择需要删除的课程安排！','error');
	    			return false;
	    		}
				$.messager.confirm('提示','请确认是否要删除选中的课程安排？',function(ok){
					if(ok){
						for(var i=0;i<arrangement.length;i++){
							arrangementTable.datagrid('deleteRow',arrangementTable.datagrid('getRowIndex',arrangement[i]));							
						}
					}
				});
	         }
	     }],
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
	 

	 var courseUserTable = $('#training-arrangement-user-table').datagrid({
        data: [],
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        columns: [[{
            field: 'user_name',
            title: '姓名',
            width: '10%',
            align: 'left'
        },{
            field: 'team',
            title: '部门信息',
            width: '40%',
            align: 'left',
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
            field: 'mobile_no',
            title: '手机号码',
            width: '20%',
            align: 'right'
        }]],
        toolbar: [{
            id: 'training-arrangement-add',
            text: '选择讲师',
            disabled: !Wz.getPermission('company/position/create'),
            iconCls: 'icon-add',
            handler: function(){
            	userSelectWin.window('open');
            	return false;
            }
        }]
    });
	 
	 var userParam = {
    	team_id: '',
    	position_id: '',
    	keyword: '',
    	mobile_no: ''
	}
	var userTable = $('#training-arrangement-userall-table').datagrid({
        url: './api/user/get_user_list.json',
        queryParams: userParam,
        fitColumns: true,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        title: '待选讲师',
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'user_name',
            title: '姓名',
            width: '80px',
            align: 'center'
        },{
            field: 'team',
            title: '部门',
            width: '250px',
            align: 'center',
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
            field: 'position',
            title: '职务',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val.position_name: '';
            }
        },{
            field: 'mobile_no',
            title: '手机号',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val.join('<br/>'): '';
            }
        }]],
        onLoadSuccess: function(data){
        	data = data.rows;
        	var sd = userSelectedTable.datagrid('getData').rows;
        	for(var i=0;i<data.length;i++){
        		for(var j=0;j<sd.length;j++){
        			if(sd[j].user_id==data[i].user_id){
        				userTable.datagrid('checkRow',i);
        				break;
        			}
        		}
        	}
        },
        onCheck: function(index,row){
        	var data = userSelectedTable.datagrid('getData').rows;
        	var data = $.grep(data,function(o){
        		return o.user_id == row.user_id;
        	});
        	if(data.length == 0){
        		userSelectedTable.datagrid('insertRow',{
        			index: 0,
        			row: row
        		});
        	}
        },
        onUncheck: function(index,row){
        	var data = userSelectedTable.datagrid('getData').rows;
        	$.grep(data,function(o,i){
        		if(!!!o)return false;
        		if(row.user_id == o.user_id){
        			userSelectedTable.datagrid('deleteRow',i);
        		}
        		return true;
        	});
        },
        onUncheckAll: function(rows){
        	var oldData = userSelectedTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		for(var i=oldData.length;i>0;i--){
        			if(oldData[i-1].user_id == o.user_id){
        				userSelectedTable.datagrid('deleteRow',i-1);
        				break;
        			}
        		}
        	});
        },
        onCheckAll: function(rows){
        	var oldData = userSelectedTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		var same = $.grep(oldData,function(od){
        			return od.user_id == o.user_id;
        		});                    		
        		if(same.length == 0){
        			userSelectedTable.datagrid('insertRow',{
            			index: 0,
            			row: o
            		});
        		}
        	});
        },
        toolbar: '#training-arrangement-userall-tb',
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
	var userSelectedTable = $('#training-arrangement-userselected-table').datagrid({
		data: [],
        fitColumns: true,
        striped: true,
        fit: true,
        rownumbers: true,
        title: '已选讲师',
        columns: [[{
            field: 'user_name',
            title: '姓名',
            width: '80px',
            align: 'center'
        },{
            field: 'team',
            title: '部门',
            width: '250px',
            align: 'center',
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
            field: 'position',
            title: '职务',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	if(!!val){
            		if(typeof val == 'string'){
            			return val;
            		}else{
            			return val.position_name;
            		}                        		
            	}else{
            		return '';
            	}
            }
        },{
            field: 'mobile_no',
            title: '手机号',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	if(!!val){
            		if(val instanceof Array){
            			return val.join('<br/>');
            		}else{
            			return val.replace(/,/g,'<br/>');
            		}
            	}
            	
            }
        },{
            field: 'user_id',
            title: '操作',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
            }
        }]]
    });
	
	userSelectWin.find('.search-btn').click(function(){
		userParam.keyword = userSelectWin.find('input[name=keyword]').val();
		userParam.position_id = queryPosition.combobox('getValue');
		userParam.mobile_no = userSelectWin.find('input[name=mobile_no]').val();
		userParam.team_id = queryTeam.combotree('getValue');
		userTable.datagrid('reload');
	});
	userSelectWin.on('click','.del-btn',function(){
		var user = userSelectedTable.datagrid('getData').rows[$(this).parents('tr').index()];
		var rows = userTable.datagrid('getData').rows;
		var index = 0;
		for(;index<rows.length;index++){
			if(rows[index].user_id == user.user_id){
				break;
			}
		}
		userTable.datagrid('uncheckRow',index);
	});
	
	userSelectWin.on('click','.edit-save',function(){
		var users = userSelectedTable.datagrid('getRows');
		courseUserTable.datagrid('loadData',users);
		userSelectWin.window('close');
	});
	 
	var resultUserTable = $('#offline-arrangement-result-user-table').datagrid({
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
	    	 field: 'teach_cnt',
	    	 title: '授课次数',
	    	 width: '10%'
	     },{
	    	 field:'state',
	    	 title:'状态',
	    	 align:'center',
	    	 width:'10%',
	         formatter: function(val,obj,row){
	        	 return val == 'ACCESS'?'已接受':(val=='REFUSE'?'拒绝':'未响应');
	         }
	     },{
	    	 field: 'desc',
	    	 title: '备注',
	    	 width: '10%'
	     },{
	    	 field:'auth_id',
	    	 title:'操作',
	    	 width:'10%',
	    	 align:'center',
	         hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/auths')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 return (obj.state =='ACCESS'?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.authManage.setTeacher('+row+')" href="javascript:void(0);">设为讲师</a>':'')
	    		 
	    	 }
	     }]],
	     toolbar: '#offline-arrangement-result-talbe-tb',
	     changePages: function(params,pageObj){
       	$.extend(params,{
       		start: (pageObj.page-1)*pageObj.rows,
       		length: pageObj.rows
       	});
       },
	    loadFilter: function(data){
	    	var state = ['ACCESS','REFUSE','WAIT'];
	    	for(var i=0;i<data.data.length;i++){
	    		data.data[i].state = state[Math.floor(Math.random()*3)];
	    		data.data[i].teach_cnt = Math.floor(Math.random()*10);
	    	}
           return {
               total: data.recordsFiltered,
               rows: data.data
           };
       }
	 })
	
	 function editArrangement(row){
		 editForm.form('reset');
		 var arrangement = $.extend({},arrangementTable.datagrid('getRows')[row]);
		 arrangement.start_time = Wz.dateFormat(new Date(arrangement.start_time*1000),'yyyy-MM-dd hh:mm:ss');
		 arrangement.end_time = Wz.dateFormat(new Date(arrangement.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		 arrangement.apply_end_time = Wz.dateFormat(new Date(arrangement.apply_end_time*1000),'yyyy-MM-dd hh:mm:ss');
		 editForm.form('load',arrangement);
		 editWin.window({title:'编辑课程'}).window('open');
	 }
	 
	 function saveArrangement(){
		 if(!editForm.form('validate')){
			 $.messager.alert('错误','请正确填写表单','error');
			 return false;
		 }		 
		 var arrangement_id = 101;
		 var course_id = editForm.find('input[textboxname=course_id]').combogrid('getValue');
		 var course_name = editForm.find('input[textboxname=course_id]').combogrid('getText');
		 var start_time = startTime.datetimebox('getValue');
		 var end_time = endTime.datetimebox('getValue');
		 var apply_end_time = applyEndTime.datetimebox('getValue');
		 var arrangement_addr = editForm.find('input[name=arrangement_addr]').val();
		 var user_list = courseUserTable.datagrid('getData');
		 start_time = Wz.parseDate(start_time).getTime()/1000;
		 end_time = Wz.parseDate(end_time).getTime()/1000;
		 apply_end_time = Wz.parseDate(apply_end_time).getTime()/1000;
		 if(start_time > end_time){
			$.messager.alert('错误','开始时间不能大于结束时间！','error');
			return false;
		 }
		 if(apply_end_time > start_time){
			$.messager.alert('错误','报名截止时间不能大于课程开始时间！','error');
			return false;
		 }
		 
		 arrangementTable.datagrid('insertRow',{
			index: 0,
			row: {
				arrangement_id: arrangement_id,
				course_id: course_id,
				course_name: course_name,
				arrangement_addr: arrangement_addr,
				start_time: start_time,
				end_time: end_time,
				apply_end_time: apply_end_time,
				apply_state: 'APPLY_RUNING',
				course_state: 'COURSE_NOT_START',
				auth_cnt: 0,
				discover_item_id: [],
				create_admin_name: '赵君',
				create_time: new Date().getTime()/1000
			}
		 });
		 editWin.window('close');	 
	 }
	 
	 function delArrangement(row){
		var arrangement = $.extend({},arrangementTable.datagrid('getRows')[row]);
		$.messager.confirm('提示','请确认是否要删除课程？',function(ok){
    		if(ok){
    			arrangementTable.datagrid('deleteRow',row);    			
    		}
    	});
	 }
	 function showResult(row){
		
    	resultWin.window('open');
	 }
	return {
		editArrangement: editArrangement,
		saveArrangement: saveArrangement,
		delArrangement: delArrangement,
		showResult: showResult
	};
}()