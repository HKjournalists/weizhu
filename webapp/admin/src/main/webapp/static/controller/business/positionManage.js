/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 企业管理-职务管理
 */
Wz.namespace('Wz.business');
Wz.business.positionManage = function(){
    $.parser.parse('#main-contain');
    
    var editWin = $('#business-position-edit-win');
    var editForm = $('#business-position-edit-form')
    var positionTable = $('#business-position-table').datagrid({
        url: './api/user/get_position.json',
        fitColumns: true,
        selectOnCheck: true,
        striped: true,
        fit: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'position_name',
            title: '职务名称',
            width: '20%',
            align: 'left'
        },{
            field: 'position_desc',
            title: '职务描述',
            width: '60%',
            align: 'left'
        },{
            field: 'position_id',
            title: '操作',
            align: 'center',
            hidden: !(Wz.getPermission('company/position/update')||Wz.getPermission('company/position/delete')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('company/position/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.business.positionManage.edit('+row+')">编辑</a>':'') + 
                	(Wz.getPermission('company/position/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.business.positionManage.delPosition('+val+')">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'business-position-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('company/position/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=position_id]').val('');
            	editWin.window('open');
            }
        },{
            id: 'business-position-del',
            text: Wz.lang.common.grid_del_btn,
            disabled: !Wz.getPermission('company/position/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var positions = positionTable.datagrid('getChecked');
            	if(positions.length == 0){
            		$.messager.alert('提示','请选择需要删除的职务信息','info');
            		return false;
            	}
            	var position_id = [];
    			for(var i=0;i<positions.length;i++){
    				position_id.push(positions[i].position_id);
    			}
    			delPosition(position_id.join(','));
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
                total: data.position.length,
                rows: data.position
            };
        }
    });
    
    function edit(row){
    	var position = positionTable.datagrid('getData').rows[row];
    	editForm.form('load',position);
    	editWin.window('open');
    }
    
    function saveEdit(){
    	var position_id = editForm.find('input[name=position_id]').val();
    	var url = './api/user/create_position.json';
    	if(position_id != ''){
    		url = './api/user/update_position.json';
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
    		ajax: true,
    		dataType: 'json',
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				editWin.window('close');
    				positionTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    			}
    		}
    	});
    }
    
    function delPosition(position_id){
    	$.messager.confirm('提示','请确认是否要删除职务？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/user/delete_position.json',
    				data: {
    					position_id: position_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						positionTable.datagrid('reload');
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
    	delPosition: delPosition
    };
}()

