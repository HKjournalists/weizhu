/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 账号管理界面
 */
Wz.namespace('Wz.system');
Wz.system.roleManage = function(){
	var thisDom = Wz.cache(Wz.getCurRout()).dom;
	var editWin = $('#system-role-edit');
	var editForm = editWin.find('#system-role-edit-form');
	var permissionTree = editWin.find('.permission-tree').tree({
		url: './api/get_permission_tree.json',
		checkbox: true,
		cascadeCheck: true,
		loadFilter: function(data,parent){
			var data = data.permission_tree;
			(function(nodes){
				for(var i=0,len=nodes.length;i<len;i++){
					nodes[i].id = nodes[i].group_id||nodes[i].permission_id ||'';
					nodes[i].text = nodes[i].group_name||nodes[i].permission_name ||'';
					if(!!nodes[i].group && nodes[i].group.length>0){
						nodes[i].children = nodes[i].group;
						arguments.callee(nodes[i].children);
					}
				}				
			}(data))
			return data;
		}
	});
    var roleTable = $('#system-role-table').datagrid({
        url: './api/get_role_list.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'role_name',
            title: '角色名',
            width: '25%'
        },{
            field: 'create_time',
            title: '创建时间',
            width: '25%',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
            }
        },{
            field: 'create_admin_name',
            title: '创建人',
            width: '25%',
            align: 'center',
            formatter: function(val,obj,row){
            	return val || '';
            }
        },{
            field: 'admin_id',
            title: '操作',
            width: '20%',
            align: 'center',
            hidden: !(Wz.getPermission('system/role/update')||Wz.getPermission('system/role/delete')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('system/role/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.system.roleManage.edit('+row+')">编辑</a>':'')+
                	(Wz.getPermission('system/role/delete')?'<a href="javascript:void(0)" onclick="Wz.system.roleManage.delRole('+row+')" class="tablelink delete-btn">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'system-role-add',
            text: '新建',
            disabled: !Wz.getPermission('system/role/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=role_id]').val('');
            	editForm.find('input[name=permission_id]').val('');
            	permissionTree = editWin.find('.permission-tree').tree('reload');
            	editWin.window('open');
            }
        },{
            id: 'system-role-del',
            text: '删除',
            disabled: !Wz.getPermission('system/role/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var roles = roleTable.datagrid('getChecked');
            	if(roles.length == 0){
            		$.messager.alert('提示','请选择需要删除的角色','info');
            		return false;
            	}
            	$.messager.confirm('提示','请确认是否要删除选中的角色？',function(ok){
            		if(ok){
            			var role_id = [];
            			for(var i=0;i<roles.length;i++){
            				role_id.push(roles[i].role_id);
            			}
            			Wz.ajax({
            				type: 'post',
            				url: './api/delete_role.json',
            				data: {
            					role_id: role_id.join(',')
            				},
            				success: function(json){
            					if(json.result == 'SUCC'){
            						roleTable.datagrid('reload');
            					}else{
            						$.messager.alert('错误',json.fail_text,'error');
            	    				return false;
            					}
            				}
            			});    			
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
                total: data.recordsTotal,
                rows: data.data
            };
        }
    })

    function edit(row){
        var data = roleTable.datagrid('getData').rows[row];
        var permissions = [];
        Wz.ajax({
        	url: './api/get_role_by_id.json',
        	data: {
        		role_id: data.role_id,
        		_t: new Date().getTime()
        	},
        	success: function(json){
        		var data = json.role[0];
        		if(!!data){
        			var checked = editWin.find('.permission-tree').tree('getChecked');
        			for(var i=0,len=checked.length;i<len;i++){
        				permissionTree.tree('uncheck',checked[i].target);
        			}
        			var permission_id = data.permission_id || [];
        			for(var i=0,len=permission_id.length;i<len;i++){
        				var node = permissionTree.tree('find',permission_id[i]);
        				if(!!node){
        					permissionTree.tree('check',node.target);        					
        				}
        			}
        			data.permission_id = permission_id.join(',');
        			editForm.form('load',data);
        			editWin.window('open');        			
        		}else{
        			$.messager.show({
        				title:'提示',
        				msg:'该角色已经不存在！',
        				timeout:1000,
        				showType:'slide',
        				style:{
        					right:'',
        					top:document.body.scrollTop+document.documentElement.scrollTop,
        					bottom:''
        				}
        			});
        			roleTable.datagrid('reload');
        		}
        	}
        });
    }

    function saveEdit(){
    	var url = './api/create_role.json';
    	var role_id = editForm.find('input[name=role_id]').val();
    	if(role_id != ''){
    		url = './api/update_role.json';
    	}
    	editForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			var permission_id = [];
    			var permissions = permissionTree.tree('getChecked');
    			for(var i=0;i<permissions.length;i++){
    				permission_id.push(permissions[i].id);
    			}
    			editWin.find('input[name=permission_id]').val(permission_id.join(','));
    			var valid = $(this).form('validate');
    			if(valid){
    				Wz.showLoadingMask('正在处理中，请稍后......');
    			}
    			return valid;
    		},
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = JSON.parse(result);
    			if(result.result == 'SUCC'){
    				editWin.window('close');
    				roleTable.datagrid('reload');    				
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    var delRole = function(row){
    	var role_id = roleTable.datagrid('getData').rows[row].role_id;
    	$.messager.confirm('提示','请确认是否要删除该角色？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/delete_role.json',
    				data: {
    					role_id: role_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						roleTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    };
    
    return {
        edit: edit,
        saveEdit: saveEdit,
        delRole: delRole
    };
}()

