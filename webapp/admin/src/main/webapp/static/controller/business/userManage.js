/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 企业管理-人员管理
 */
Wz.namespace('Wz.business');
Wz.business.userManage = function(){
    $.parser.parse('#main-contain');
    var query_params = {
    	is_expert: '',
    	team_id: '',
    	position_id: '',
    	keyword: '',
    	mobile_no: ''
    };
    var searchForm = $('#business-user-search-form');
    var editWin = $('#business-user-edit');
    var editForm = editWin.find('#business-user-edit-form');
    var importWin = $('#business-user-import-win');
    var importForm = importWin.find('#business-user-import-form');
    var onlineInfoWin = $('#business-user-onlineinfo-win');
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
    var searchPosition = searchForm.find('input[name=position_id]').combobox({
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
					var position = json.position||[];
					position.unshift({
						position_id: '',
						position_name: '---全部职务---'
					});
					success(json.position);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    var editTeam = editForm.find('input[name=team_id]').combotree({
    	url: './api/user/get_team.json',
    	editable: false,
    	cascadeCheck: false,
    	width: 350,
    	onBeforeExpand: function(row){
    		editTeam.combotree('options').oldText = editTeam.combotree('getText'); 
    		editTeam.combotree('options').queryParams.team_id = row.team_id;
    		return true;
    	},
    	onShowPanel: function(){
    		editTeam.combotree('options').oldText = editTeam.combotree('getText'); 
    		editTeam.combotree('options').queryParams.team_id = '';
    		editTeam.combotree('reload');
    	},
    	onLoadSuccess: function(node,data){
    		editTeam.combotree('setText',editTeam.combotree('options').oldText);
    	},
    	loadFilter: function(data){
    		if(!!data){
    			for(var i=0;i<data.length;i++){
    				data[i].id = data[i].team_id;
    				data[i].text = data[i].team_name;
    				data[i].state = (data[i].has_sub_team?'closed':'open');
    			}
    		}
    		return data;
    	}
    });
    var editPosition = editForm.find('input[name=position_id]').combobox({
    	mode: 'remote',
    	valueField: 'position_id',
    	textField: 'position_name',
    	editable: false,
    	cascadeCheck: false,
    	width: 350,
    	height: 'auto',
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/user/get_position.json',
				success: function(json){
					success(json.position||[]);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    var editLevel = editForm.find('input[name=level_id]').combobox({
    	mode: 'remote',
    	valueField: 'level_id',
    	textField: 'level_name',
    	editable: false,
    	cascadeCheck: false,
    	width: 350,
    	height: 'auto',
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/user/get_level.json',
				success: function(json){
					success(json.level||[]);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    
    importForm.find('.download-btn').click(function(){
    	Wz.downloadFile('./static/res/personinfo_template.xlsx');
    });
    searchForm.find('.search-btn').click(function(){
    	query_params.is_expert = searchForm.find('input[name=is_expert]').val();
    	query_params.team_id = searchTeam.combotree('getValue');
    	query_params.position_id = searchPosition.combotree('getValue');
    	query_params.keyword = searchForm.find('input[name=keyword]').val();
    	query_params.mobile_no = searchForm.find('input[name=mobile_no]').val();
    	userTable.datagrid('reload');
    });
    searchForm.find('.reset-btn').click(function(){
    	searchForm.form('reset');
    });
    var userTable = $('#business-user-table').datagrid({
        url: './api/user/get_user_list.json',
        queryParams: query_params,
        fitColumns: true,
    	checkOnSelect: true,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        frozenColumns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'raw_id',
            title: '工号',
            width: '80px',
            align: 'center'
        },{
            field: 'user_name',
            title: '姓名',
            width: '80px',
            align: 'center'
        },{
            field: 'gender',
            title: '性别',
            width: '40px',
            align: 'center',
            formatter: function(val,obj,row){
            	return val=='FEMALE'?'女':'男';//Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
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
            field: 'is_expert',
            title: '操作',
            width: '225px',
            align: 'center',
            hidden: !(Wz.getPermission('company/user/update')||Wz.getPermission('company/user/delete')||Wz.getPermission('company/user/set_state')||Wz.getPermission('company/user/set_expert')||Wz.getPermission('company/user/list_session')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('company/user/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.business.userManage.edit('+row+')">编辑</a>':'') + 
                	(Wz.getPermission('company/user/set_state')?'<a href="javascript:void(0)" class="tablelink status-btn" onclick="Wz.business.userManage.changeState('+row+')">'+(obj.state=='APPROVE'?'审核':(obj.state=='DISABLE'?'启用':'禁用'))+'</a>':'') +
                	(Wz.getPermission('company/user/set_expert')?'<a href="javascript:void(0)" class="tablelink status-btn" onclick="Wz.business.userManage.setExpert('+row+')">'+(!!val?'取消专家':'设为专家')+'</a>':'') +
                	(Wz.getPermission('company/user/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.business.userManage.delUser('+obj.user_id+')">删除</a>':'') +
                	(Wz.getPermission('company/user/list_session')?'<a href="javascript:void(0)" class="tablelink showview-btn" onclick="Wz.business.userManage.refreshLoginInfo('+row+')">在线信息</a>':'');
            }
        }]],
        columns: [[{
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
            field: 'level',
            title: '职级',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val.level_name: '';
            }
        },{
            field: 'sesstion_data',
            title: '最近活动时间',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val && !!val.active_time){
            		result = Wz.dateFormat(new Date(val.active_time*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'sesstion_data',
            title: 'APP平台',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val && !!val.weizhu_platform){
            		result = val.weizhu_platform;
            	}
            	return result;
            }
        },{
            field: 'sesstion_data',
            title: 'APP版本',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val && !!val.weizhu_version_name){
            		result = val.weizhu_version_name;
            	}
            	return result;
            }
        },{
            field: 'exts',
            title: '扩展字段',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = [];
            	if(!!val){
            		for(var i=0;i<val.length;i++){
            			result.push(val[i].name + ':' + val[i].value);
            		}
            	}
            	return result.join('<br/>');
            }
        },{
            field: 'create_time',
            title: '创建时间',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'create_admin',
            title: '创建人',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val:'';
            }
        }]],
        toolbar: [{
            id: 'business-user-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('company/user/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=user_id]').val('');
            	editForm.find('input[name=raw_id]').parents('.form-item').show();
            	editWin.window({title:'新建员工信息'}).window('open');
            }
        },{
            id: 'business-user-add',
            text: '审核',
            disabled: !Wz.getPermission('company/user/create'),
            iconCls: 'icon-checked',
            handler: function(){
            	var users = userTable.datagrid('getChecked');
            	if(users.length == 0){
            		$.messager.alert('提示','请选择需要审核的员工信息','info');
            		return false;
            	}
            	var user_id = [];
            	for(var i=0,len=users.length;i<len;i++){
            		if(users[i].state == 'APPROVE'){
            			user_id.push(users[i].user_id);
            		}
            	}
            	if(user_id.length == 0){
            		$.messager.alert('提示','您选择的人员中没有需要审核的！','info');
            		return false;
            	}
            	$.messager.confirm('提示','请确认是否审核通过选中的人员？',function(ok){
            		if(ok){
            			Wz.ajax({
            				type: 'post',
            				url: './api/user/set_state.json',
            				data: {
            					user_id: user_id.join(','),
            					state: 'NORMAL'
            				},
            				success: function(json){
            					if(json.result == 'SUCC'){
            						userTable.datagrid('reload');
            					}else{
            						$.messager.alert('错误',json.fail_text,'error');
            	    				return false;
            					}
            				}
            			});    			
            		}
            	}); 
            }
        },{
            id: 'business-user-del',
            text: Wz.lang.common.grid_del_btn,
            disabled: !Wz.getPermission('company/user/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var users = userTable.datagrid('getChecked');
            	if(users.length == 0){
            		$.messager.alert('提示','请选择需要删除的员工信息','info');
            		return false;
            	}
            	var user_id = [];
    			for(var i=0;i<users.length;i++){
    				user_id.push(users[i].user_id);
    			}
    			delUser(user_id.join(','));
            }
        },{
            id: 'business-user-import-btn',
            text: '导入员工信息',
            disabled: !Wz.getPermission('company/user/import'),
            iconCls: 'icon-import',
            handler: function(){
            	importWin.window('open');
            }
        },{
            id: 'business-user-export-btn',
            text: '导出搜索员工信息',
            disabled: !Wz.getPermission('company/user/export'),
            iconCls: 'icon-export',
            handler: function(){
            	var param = [];
            	for(var name in query_params){
                	param.push(name + '=' + query_params[name]);
                }
            	Wz.downloadFile("./api/user/export_user.download?_t=" + new Date().getTime()+'&'+param.join('&'));
            }
        }/*,{
            id: 'business-user-onlineinfo-btn',
            text: '查看在线信息',
            disabled: !Wz.getPermission('company/user/list_session'),
            iconCls: 'icon-search',
            handler: function(){
            	var users = userTable.datagrid('getChecked');
            	if(users.length == 0){
            		$.messager.alert('提示','请选择需要查看的员工信息','info');
            		return false;
            	}
            	refreshLoginInfo(users[0]);
            }
        }*/],
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
    var sessionTable = $('#business-user-session-table').datagrid({
    	data: [],
    	fitColumns: true,
    	checkOnSelect: true,
        striped: true,
        fit: true,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'login_time',
            title: '登录时间',
            align: 'center',
            width: '14%',
            formatter: function(val,obj,row){
            	console.info(val);
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'active_time',
            title: '最近活动时间',
            align: 'center',
            width: '14%',
            formatter: function(val,obj,row){
            	console.info(val);
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
        	field: 'weizhu_platform',
            width: '10%',
        	title: 'APP平台'
        },{
        	field: 'weizhu_version_name',
            width: '10%',
        	title: 'APP版本'
        },{
        	field: 'weizhu_stage',
            width: '10%',
        	title: 'APP版本阶段'
        },{
        	field: 'weizhu_build_time',
            width: '14%',
        	title: 'APP构建时间',
        	align: 'center',
            formatter: function(val,obj,row){
            	console.info(val);
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
        	field: 'device_info',
            width: '24%',
        	title: '设备相关信息',
        	align: 'center'
        }]],
        toolbar: [{
            id: 'business-user-session-del',
            text: Wz.lang.common.grid_del_btn,
            disabled: !Wz.getPermission('company/user/delete_session'),
            iconCls: 'icon-remove',
            handler: function(){
            	var user_id = onlineInfoWin.find('input[name=user_id]').val();
            	var sessions = sessionTable.datagrid('getChecked');
            	if(sessions.length == 0){
            		$.messager.alert('提示','请选择需要删除的会话信息','info');
            		return false;
            	}
            	var session_id = [];
    			for(var i=0;i<sessions.length;i++){
    				session_id.push(sessions[i].session_id);
    			}
    			Wz.ajax({
    				type: 'post',
    				url: './api/user/delete_user_session.json',
    				data: {
    					user_id: user_id,
    					session_id: session_id.join(',')
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						refreshLoginInfo(user_id);
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});
            }
        }]
    });

    function refreshLoginInfo(row){
    	var user = userTable.datagrid('getRows')[row];
    	var user_id = user.user_id;
    	if(typeof user == 'string'){
    		user_id = user;
    	}
    	Wz.ajax({
    		url: './api/user/get_user_login_session.json',
    		data: {
    			user_id: user_id
    		},
    		success: function(data){
    			var login = data.login;
    			var sessionData = data.session;
    			if(!!login){
    				onlineInfoWin.find('.user-code-info').html('<span>验证码：'+login.code+'</span><span>验证码发送时间：'+Wz.dateFormat(new Date(login.create_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span><span>手机号：'+login.mobile_no+'</span><span>验证码过期：'+(login.is_expired?'已过期':'未过期')+'</span>').parent().show();
    			}else{
    				onlineInfoWin.find('.user-code-info').parent().hide();
    			}
    			sessionTable.datagrid('loadData',sessionData);
    			if(typeof user != 'string'){
    				onlineInfoWin.find('input[name=user_id]').val(user_id)
    				onlineInfoWin.window({title:user.user_name+'的在线信息'}).window('open');
    			}
    		}
    	});
    }
    
    function delUser(user_id){
    	$.messager.confirm('提示','请确认是否要删除员工信息？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/user/delete_user.json',
    				data: {
    					user_id: user_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						userTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	}); 
    }

    function setExpert(row){
    	var user = userTable.datagrid('getData').rows[row];
    	var is_expert = user.is_expert;
    	var msg = (is_expert?'请确认是否取消专家？':'请确认是设置专家?');
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/user/set_expert.json',
    				data: {
    					user_id: user.user_id,
    					is_expert: !is_expert
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						userTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    
    function changeState(row){
    	var user = userTable.datagrid('getData').rows[row];
    	var state = user.state;
    	var user_id = user.user_id;
    	var msg = (state=='APPROVE'?'请确认是否要审核该用户？':(state=='DISABLE'?'请确认是否要启用该用户？':'请确认是否要禁用该用户？'));
    	state = (state=='NORMAL'?'DISABLE':'NORMAL');
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/user/set_state.json',
    				data: {
    					user_id: user_id,
    					state: state
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						userTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	}); 
    	
    }
    
    function edit(row){
        var userInfo = userTable.datagrid('getRows')[row];
        editForm.form('reset');
        editForm.form('load',userInfo);
    	editForm.find('input[name=raw_id]').parents('.form-item').hide();
    	var teamName = ((userInfo.team&&userInfo.team.length>0)?userInfo.team[userInfo.team.length-1].team_name:'');
    	editTeam.combotree('setText',teamName);
        editWin.window({title:'编辑员工信息'}).window('open');
    }
    
    function saveEdit(){
    	var url = './api/user/create_user.json';
    	var user_id = editForm.find('input[name=user_id]').val();
    	if(user_id != ''){
    		url = './api/user/update_user.json';
    	}
    	editForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			var valid = $(this).form('validate');
    			if(valid){
    				Wz.showLoadingMask('正在处理中，请稍后......');
    			}
    			return valid;
    		},
    		success: function(json){
    			Wz.hideLoadingMask();
    			json = JSON.parse(json);
    			if(json.result == 'SUCC'){
    				editWin.window('close');
        			userTable.datagrid('reload');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function importUsers(){
    	importForm.form('submit',{
    		url: './api/user/import_user.json?company_id='+Wz.cur_company_id,
    		onSubmit: function(){
    			Wz.showLoadingMask();
    		},
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				importWin.window('close');
    				userTable.datagrid('reload');
    				$.messager.alert('提示','<p style="padding-left:42px">这次导入新增人员<strong style="color:red;font-size:16px;">'+result.create_user_cnt+'</strong>人<br/>修改人员<strong style="color:red;font-size:16px;">'+result.update_user_cnt+'</strong>人<br/>创建部门<strong style="color:red;font-size:16px;">'+result.create_team_cnt+'</strong>个<br/>创建职务<strong style="color:red;font-size:16px;">'+result.create_position_cnt+'</strong>个<br/>创建职级<strong style="color:red;font-size:16px;">'+result.create_level_cnt+'</strong>个</p>','info');
    			}else{
    				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
    					if(ok){
    						Wz.downloadFile("./api/user/get_import_fail_log.download?_t=" + new Date().getTime());
    					}
    				});
    			}
    		}
    	});
    }
    return {
        edit: edit,
        saveEdit: saveEdit,
        delUser: delUser,
        importUsers: importUsers,
        changeState: changeState,
        setExpert: setExpert,
        refreshLoginInfo: refreshLoginInfo
    };
}()

