/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 企业管理-职级管理
 */
Wz.namespace('Wz.qa');
Wz.qa.categoryManage = function(){
    $.parser.parse('#main-contain');
    
    var editWin = $('#qa-category-edit-win');
    var editForm = $('#qa-category-edit-form');
    var importWin = $('#qa-category-import-win');
    var importForm = importWin.find('#qa-category-import-form');
    var categoryTable = $('#qa-category-table').datagrid({
        url: './api/qa/get_category.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'category_name',
            title: '分类名称',
            width: '30%',
            align: 'left',
            formatter: function(val,obj,row){
            	return (Wz.getPermission('QA_QUESTION_READ')?'<a href="javascript:void(0)" class="tablelink" onclick="Wz.qa.categoryManage.showQuestions('+obj.category_id+',\''+obj.category_name+'\')">'+val+'</a>':val);
            }
        },{
        	field: 'user_name',
            title: '创建人',
            width: '10%',
            align: 'left'
        },{
            field: 'create_time',
            title: '创建时间',
            width: '20%',
            align: 'right',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
        	field: 'question_num',
            title: '问题总量',
            width: '20%',
            align: 'right'
        },{
            field: 'category_id',
            title: '操作',
            align: 'center',
            width: '15%',
            hidden: !(Wz.getPermission('qa/category/update')||Wz.getPermission('qa/category/delete')||Wz.getPermission('qa/question/import')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('qa/question/import')?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.qa.categoryManage.openImportWin('+val+')">导入问题</a>':'') +
                	(Wz.getPermission('qa/category/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.qa.categoryManage.edit('+row+')">编辑</a>':'') + 
                	(Wz.getPermission('qa/category/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.qa.categoryManage.delcategory('+val+')">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'business-user-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('qa/category/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=category_id]').val('');
            	editWin.window({title:'新建分类'}).window('open');
            }
        }],
        loadFilter: function(data){
            return {
                total: data.category.length,
                rows: data.category
            };
        }
    });
    
    function edit(row){
    	var cateogry = categoryTable.datagrid('getData').rows[row];
    	editForm.form('load',cateogry);
    	editWin.window({title:'编辑分类'}).window('open');
    }
    
    function saveEdit(){
    	var category_id = editForm.find('input[name=category_id]').val();
    	var url = './api/qa/add_category.json';
    	if(category_id != ''){
    		url = './api/qa/update_category.json';
    	}
    	editForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				editWin.window('close');
    				categoryTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function delcategory(category_id){
    	$.messager.confirm('提示','请确认是否要删除问答分类？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/qa/delete_category.json',
    				data: {
    					category_id: category_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						categoryTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    importForm.find('.download-btn').click(function(){
    	Wz.downloadFile('./static/res/question_template.xlsx');
    });
    function openImportWin(cid){
    	importForm.find('input[name=category_id]').val(cid);
    	importWin.window('open');
    }
    
    function importQuestions(){
    	var category_id = importForm.find('input[name=category_id]').val();
    	importForm.form('submit',{
    		url: './api/qa/import_question.json?company_id='+Wz.cur_company_id + '&category_id=' + category_id,
    		onSubmit: function(){
    			Wz.showLoadingMask();
    		},
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				importWin.window('close');
    				categoryTable.datagrid('reload');
    			}else{
    				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
    					if(ok){
    						Wz.downloadFile("./api/qa/get_import_fail_log.download?_t=" + new Date().getTime());
    					}
    				});
    			}
    		}
    	});
    }
    
    function showQuestions(category_id,category_name){
    	Wz.menu.changeRout('qa-question',{category_id:category_id,category_name: category_name});
    }
    
    return {
    	edit: edit,
    	saveEdit: saveEdit,
    	delcategory: delcategory,
    	importQuestions: importQuestions,
    	openImportWin: openImportWin,
    	showQuestions: showQuestions
    };
}()

