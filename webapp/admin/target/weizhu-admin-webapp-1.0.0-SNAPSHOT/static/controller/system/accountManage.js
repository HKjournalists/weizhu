/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 用户名管理界面
 */
Wz.namespace('Wz.system');
Wz.system.accountManage = function(){
    $.parser.parse('#main-contain');
    var searchForm = $('#system-account-search-form');
    var editWin = $('#system-account-edit');
	var editForm = editWin.find('#system-account-edit-form');
    var edit_role = editWin.find('input[name=role_id]').combobox({
    	url: './api/get_role_list.json?start=0&length=1000',
    	valueField: 'role_id',
    	textField: 'role_name',
    	multiple: true,
    	editable: false,
    	width: 360,
    	onShowPanel: function(){
    		edit_role.combobox('reload');
    	},
    	loadFilter: function(data){
    		return data.data;
    	}
    });
    var edit_team = editWin.find('input[name=permit_team_id]').combotree({
    	url: './api/user/get_team.json',
    	multiple: true,
    	editable: false,
    	multiline: true,
    	cascadeCheck:false,
    	height: 50,
    	width: 360,
    	oldText: '',
    	onBeforeExpand: function(row){
    		edit_team.combotree('options').oldText = edit_team.combotree('getText'); 
    		edit_team.combotree('options').queryParams.team_id = row.team_id;
    		return true;
    	},
    	onShowPanel: function(){
    		edit_team.combotree('options').oldText = edit_team.combotree('getText'); 
    		edit_team.combotree('options').queryParams.team_id = '';
    		edit_team.combotree('reload');
    	},
    	onLoadSuccess: function(node,data){
    		edit_team.combotree('setText',edit_team.combotree('options').oldText);
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
    var query_params={
    	enable: '',
    	name_keyword: ''
    };
    searchForm.find('.search-btn').click(function(){
    	query_params.enable = searchForm.find('input[name=enable]').val();
    	query_params.name_keyword = searchForm.find('input[name=name_keyword]').val();
    	accountTable.datagrid('reload');
    });
    var accountTable = $('#system-account-table').datagrid({
        url: './api/get_admin_list.json',
        queryParams: query_params,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'admin_name',
            title: '用户名',
            width: '12%',
            align: 'left'
        },{
            field: 'admin_email',
            title: '邮箱',
            width: '20%',
            align: 'left'
        },{
            field: 'role',
            title: '角色',
            width: '20%',
            align: 'left',
            formatter: function(val,obj,row){
            	var roles = []
            	for(var i=0;i<val.length;i++){
            		roles.push(val[i].role_name);
            	}
            	return roles.join(',');
            }
        }/*,{
            field: 'team',
            title: '管理部门',
            width: '150px',
            align: 'center',
            formatter: function(val,obj,row){
            	val = val || [];
            	var teams = []
            	for(var i=0;i<val.length;i++){
            		teams.push('【'+val[i].team_name+'】');
            	}
            	return teams.join('');
            }
        }*/,{
            field: 'create_time',
            title: '开通时间',
            width: '16%',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
            }
        },{
            field: 'state',
            title: '状态',
            width: '5%',
            align: 'center',
            formatter: function(val,obj,row){
            	return val=='NORMAL'?'正常':'禁用';
            }
        },{
            field: 'admin_id',
            title: '操作',
            width: '20%',
            align: 'center',
            hidden: !(Wz.getPermission('system/admin/update')||Wz.getPermission('system/admin/delete')),
            formatter: function(val,obj,row){
            	var state = '启用';
            	if(obj.state == 'NORMAL'){
            		state = '禁用'
            	}
                return (!!Wz.getPermission('system/admin/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.system.accountManage.edit('+row+')">编辑</a>':'')+
                (!!Wz.getPermission('system/admin/update')?'<a href="javascript:void(0)" onclick="Wz.system.accountManage.changeState('+row+')" class="tablelink status-btn">'+state+'</a>':'')+
                (!!Wz.getPermission('system/admin/delete')?'<a href="javascript:void(0)" onclick="Wz.system.accountManage.deleteAccount('+val+')" class="tablelink delete-btn">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'system-account-add',
            text: Wz.lang.common.gridtool_create_btn,
            iconCls: 'icon-add',
            disabled: !Wz.getPermission('system/admin/create'),
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=admin_id]').val('');
                editForm.find('input[name=admin_password]').parents('.form-item').show();
                editForm.find('input[name=admin_rpassword]').parents('.form-item').show();
            	editWin.find('input[name=is_enable]').parents('.form-item').hide();
            	editWin.find('input[name=force_reset_password]').parents('.form-item').hide();
        		edit_team.parents('.form-item').hide();
            	editWin.window({title:'新建用户名'});
            	editWin.window('open');
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
    })

    function edit(row){
        var admin = accountTable.datagrid('getData').rows[row];
		var roleArr = [];
		for(var i=0;i<admin.role.length;i++){
			roleArr.push(admin.role[i].role_id);
		}
		admin.team = admin.team || [];
        edit_role.combobox('setValues',roleArr);
        edit_team.combotree('setValues',admin.team);
        editForm.form('load',admin);
        editForm.find('input[name=enable_team_permit]').prop('checked',admin.enable_team_permit);
        editForm.find('input[name=force_reset_password]').prop('checked',admin.force_reset_password);
        editForm.find('input[name=admin_password]').parents('.form-item').hide();
        editForm.find('input[name=admin_rpassword]').parents('.form-item').hide();
    	editWin.find('input[name=is_enable]').parents('.form-item').show();
    	editWin.find('input[name=force_reset_password]').parents('.form-item').show();
    	if(!!admin.enable_team_permit){
    		edit_team.parents('.form-item').show();
    	}else{
    		edit_team.parents('.form-item').hide();
    	}
    	editWin.window({title:'编辑用户名'});
        editWin.window('open');
    }
    
    function saveEdit(){
		var admin_id = editForm.find('input[name=admin_id]').val(),
			admin_name = editForm.find('input[name=admin_name]').val(),
			admin_email = editForm.find('input[name=admin_email]').val(),
			admin_password = editForm.find('input[name=admin_password]').val(),
			admin_rpassword = editForm.find('input[name=admin_rpassword]').val(),
			enable_team_permit = editForm.find('input[name=enable_team_permit]').prop('checked'),
			is_enable = editForm.find('input[name=is_enable]').prop('checked'),
			force_reset_password = editForm.find('input[name=force_reset_password]').prop('checked'),
			role_id = edit_role.combobox('getValues'),
			permit_team_id = edit_team.combotree('getValues'),
			url = './api/create_admin.json',
			params = {};
		
		if(!editForm.find('input[textboxname=admin_name]').textbox('isValid')){
			$.messager.alert('错误','请正确填写用户名！','error');
			return false;
		}
		if(!editForm.find('input[textboxname=admin_email]').textbox('isValid')){
			$.messager.alert('错误','请正确填写邮箱！','error');
			return false;
		}
    	if(admin_id == ''){
    		if(admin_password == ''){
    			return false;
    		}
    		if(admin_rpassword == '' || admin_rpassword != admin_password){
    			return false;
    		}
    		params = {
    			admin_name: admin_name,
    			admin_email: admin_email,
    			admin_password: admin_password,
    			is_enable: is_enable,
    			enable_team_permit: enable_team_permit,
    			role_id: role_id.join(','),
    			permit_team_id: permit_team_id.join(',')
    		};
    	}else{
    		url = './api/update_admin.json';
    		params = {
    			admin_id: admin_id,
    			admin_name: admin_name,
    			admin_email: admin_email,
    			is_enable: is_enable,
    			force_reset_password: force_reset_password,
    			enable_team_permit: enable_team_permit,
    			role_id: role_id.join(','),
    			permit_team_id: permit_team_id.join(',')
    		};
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: params,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				editWin.window('close');
        			accountTable.datagrid('reload');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    editWin.on('change','input[name=enable_team_permit]',function(){
    	if($(this).prop('checked')){
    		edit_team.parents('.form-item').show();
    	}else{
    		edit_team.parents('.form-item').hide();
    	}
    });
    
    function changeState(row){
    	var admin = accountTable.datagrid('getRows')[row];
    	var state = admin.state;
    	var admin_id = admin.admin_id;
    	var msg = state=='DISABLE'?'请确认是否要启用该账号？':'请确认是否要禁用该账号？';
    	state = (state=='NORMAL'?'DISABLE':'NORMAL');
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/update_admin_state.json',
    				data: {
    					admin_id: admin_id,
    					state: state
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						accountTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	}); 
    }
    
    function deleteAccount(admin_id){
    	$.messager.confirm('提示','请确认是否要删除该账号？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/delete_admin.json',
    				data: {
    					admin_id: admin_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						accountTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    return {
        edit: edit,
        deleteAccount: deleteAccount,
        saveEdit: saveEdit,
        changeState: changeState
    };
}()

