/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 企业管理-部门管理
 */
Wz.namespace('Wz.business');
Wz.business.teamManage = function(){
    $.parser.parse('#main-contain');
    var editWin = $('#business-team-edit-win');
    var editForm = $('#business-team-edit-form');
    var teamTable = $('#business-team-table').treegrid({
    	url: './api/user/get_team.json',
    	treeField: 'team_name',
    	idField: 'team_id',
        rownumbers: true,
        checkOnSelect: true,
    	columns: [[{
            field: 'ck',
            checkbox: true
        },{
    		field: 'team_name',
    		width: '90%',
    		title: '部门名称'
    	}]],
    	toolbar: [{
            id: 'business-team-add',
            text: '添加同级部门',
            disabled: !Wz.getPermission('company/team/create'),
            iconCls: 'icon-add',
            handler: function(){
            	var teams = teamTable.treegrid('getChecked');
            	if(teams.length == 0 && teamTable.treegrid('getRoots').length > 0){
            		$.messager.alert('提示','请先选择一个部门进行添加！','info');
            		return false;
            	}
            	var team = teams[0] || {parent_team_id: ''};
            	editForm.form('reset');
            	editForm.find('input[name=team_id]').val('');
            	editWin.find('input[name=type]').val('sibling');
            	editWin.find('input[name=parent_team_id]').val(team.parent_team_id);
            	editWin.window({title:'添加同级部门'}).window('open');
            }
        },{
            id: 'business-subteam-add',
            text: '添加子级部门',
            disabled: !Wz.getPermission('company/team/create'),
            iconCls: 'icon-add',
            handler: function(){
            	var teams = teamTable.treegrid('getChecked');
            	if(teams.length == 0){
            		$.messager.alert('提示','请先选择一个部门进行添加！','info');
            		return false;
            	}
            	editForm.form('reset');
            	editForm.find('input[name=team_id]').val('');
            	editWin.find('input[name=type]').val('sub');
            	editWin.find('input[name=parent_team_id]').val(teams[0].team_id);
            	editWin.window({title:'添加子级部门'}).window('open');
            }
        },{
            id: 'business-team-edit',
            text: '编辑',
            disabled: !Wz.getPermission('company/team/update'),
            iconCls: 'icon-edit',
            handler: function(){
            	var teams = teamTable.treegrid('getChecked');
            	if(teams.length == 0){
            		$.messager.alert('提示','请先选择一个部门进行编辑！','info');
            		return false;
            	}

            	editForm.form('load',teams[0]);
            	editWin.find('input[name=type]').val('');
            	editWin.window({title:'编辑部门'}).window('open');
            }
        },{
            id: 'business-team-del',
            text: '删除',
            disabled: !Wz.getPermission('company/team/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var teams = teamTable.treegrid('getChecked');
            	if(teams.length == 0){
            		$.messager.alert('提示','请先选择一个部门进行删除！','info');
            		return false;
            	}
            	$.messager.confirm('提示','请确认是否删除选中部门及其子部门吗？',function(ok){
            		if(ok){
            			Wz.ajax({
            				type: 'post',
            				url: './api/user/delete_team.json',
            				data: {
            					team_id: teams[0].team_id,
            					recursive: true
            				},
            				success: function(json){
            					if(json.result == 'SUCC'){
            						teamTable.treegrid('remove',teams[0].team_id);
            					}else{
            						$.messager.alert('错误',result.fail_text,'error');
            	    				return false;
            					}
            				}
            			});    			
            		}
            	});
            }
        }],
    	onBeforeExpand: function(row){
    		$('#business-team-table').treegrid('options').url = './api/user/get_team.json?team_id='+row.team_id;
    		return true;
    	},
    	loadFilter: function(data){
    		if(!!data){
    			for(var i=0;i<data.length;i++){
    				data[i].state = (data[i].has_sub_team?'closed':'open');
    			}
    		}
    		return data;
    	}
    });
    
    function saveTeam(){
    	var url = './api/user/create_team.json';
    	var team_id = editForm.find('input[name=team_id]').val();
    	var team_name = editForm.find('input[name=team_name]').val();
    	var parent_team_id = editForm.find('input[name=parent_team_id]').val();
    	if(team_id != ''){
    		url = './api/user/update_team.json';
    	}
    	var type = editWin.find('input[name=type]').val();
    	editForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			var valid = $(this).form('validate');
    			if(valid){
    				Wz.showLoadingMask('正在处理中，请稍后......');
    			}
    			return valid;
    		},
    		dataType: 'json',
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				editWin.window('close');
    				if(team_id != ''){
    					teamTable.treegrid('update',{
    						id: team_id,
    						row: {
								team_name: team_name
    						}
    					})
    				}else{
    					teamTable.treegrid('expand',parent_team_id);
						teamTable.treegrid('append',{
							parent: parent_team_id,
							data: [{
								team_id: result.team_id,
								team_name: team_name,
								parent_team_id: parent_team_id
							}]
						});
    				}    				
    			}else{
					$.messager.alert('错误',result.fail_text,'error');
    				return false;
				}
    		}
    	});
    }
    
    return {
    	saveTeam: saveTeam
    };
}()

