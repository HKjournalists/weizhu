/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 企业管理-职级管理
 */
Wz.namespace('Wz.business');
Wz.business.levelManage = function(){
    $.parser.parse('#main-contain');
    
    var editWin = $('#business-level-edit-win');
    var editForm = $('#business-level-edit-form');
    var levelTable = $('#business-level-table').datagrid({
        url: './api/user/get_level.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'level_name',
            title: '职级名称',
            width: '60%',
            align: 'left'
        },{
            field: 'create_time',
            title: '创建时间',
            width: '20%',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'level_id',
            title: '操作',
            align: 'center',
            hidden: !(Wz.getPermission('company/level/update')||Wz.getPermission('company/level/delete')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('company/level/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.business.levelManage.edit('+row+')">编辑</a>':'') + 
                	(Wz.getPermission('company/level/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.business.levelManage.dellevel('+val+')">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'business-level-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('company/level/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=level_id]').val('');
            	editWin.window('open');
            }
        },{
            id: 'business-level-del',
            text: Wz.lang.common.grid_del_btn,
            disabled: !Wz.getPermission('company/level/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var levels = levelTable.datagrid('getChecked');
            	if(levels.length == 0){
            		$.messager.alert('提示','请选择需要删除的职级信息','info');
            		return false;
            	}
            	var level_id = [];
    			for(var i=0;i<levels.length;i++){
    				level_id.push(levels[i].level_id);
    			}
    			dellevel(level_id.join(','));
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
                total: data.level.length,
                rows: data.level
            };
        }
    });
    
    function edit(row){
    	var level = levelTable.datagrid('getData').rows[row];
    	editForm.form('load',level);
    	editWin.window('open');
    }
    
    function saveEdit(){
    	var level_id = editForm.find('input[name=level_id]').val();
    	var url = './api/user/create_level.json';
    	if(level_id != ''){
    		url = './api/user/update_level.json';
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
    		dataType: 'json',
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				editWin.window('close');
    				levelTable.datagrid('reload');		
    			}else{
					$.messager.alert('错误',result.fail_text,'error');
    				return false;
				}
    		}
    	});
    }
    
    function dellevel(level_id){
    	$.messager.confirm('提示','请确认是否要删除职级？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/user/delete_level.json',
    				data: {
    					level_id: level_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						levelTable.datagrid('reload');
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
    	saveEdit: saveEdit,
    	dellevel: dellevel
    };
}()

