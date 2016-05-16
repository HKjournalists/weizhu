/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 问答管理-问题管理
 */
Wz.namespace('Wz.qa');
Wz.qa.questionManage = function(){
    $.parser.parse('#main-contain');
    var query_params = {
    	category_id: '',
    	keyword: ''
    };
    var searchForm = $('#qa-question-search-form');
    var editWin = $('#qa-question-edit-win');
    var editForm = $('#qa-question-edit-form');
    var importWin = $('#qa-question-import-win');
    var importForm = importWin.find('#qa-question-import-form');
    var moveWin = $('#qa-question-move-win');
    var moveForm = moveWin.find('#qa-question-move-form');
    var answerListWin = $('#qa-question-answerlist-win');
    var answerWin = $('#qa-question-answer-edit-win');
    var answerForm = $('#qa-question-answer-edit-form');
    
    var searchCategory = searchForm.find('input[name=category_id]').combobox({
    	mode: 'remote',
    	valueField: 'category_id',
    	textField: 'category_name',
    	editable: false,
    	cascadeCheck: false,
    	height: 'auto',
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/qa/get_category.json',
				success: function(json){
					success(json.category);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    
    var importCategory = importForm.find('input[name=category_id]').combobox({
    	mode: 'remote',
    	valueField: 'category_id',
    	textField: 'category_name',
    	editable: false,
    	cascadeCheck: false,
    	width: 260,
    	required: true,
    	height: 'auto',
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/qa/get_category.json',
				success: function(json){
					success(json.category);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });    
    
    var editCategory = editForm.find('input[name=category_id]').combobox({
    	mode: 'remote',
    	valueField: 'category_id',
    	textField: 'category_name',
    	editable: false,
    	cascadeCheck: false,
    	width: 350,
    	required: true,
    	height: 'auto',
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/qa/get_category.json',
				success: function(json){
					success(json.category);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    
    var moveCategory = moveForm.find('input[name=category_id]').combobox({
    	mode: 'remote',
    	valueField: 'category_id',
    	textField: 'category_name',
    	editable: false,
    	cascadeCheck: false,
    	width: 350,
    	required: true,
    	height: 'auto',
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/qa/get_category.json',
				success: function(json){
					success(json.category);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    
    searchForm.find('.search-btn').click(function(){
    	query_params.category_id = searchForm.find('input[name=category_id]').val();
    	query_params.keyword = searchForm.find('input[name=keyword]').val();
    	questionTable.datagrid('reload');
    });
    searchForm.find('.reset-btn').click(function(){
    	searchForm.form('reset');
    });
    var questionTable = $('#qa-question-table').datagrid({
        url: './api/qa/get_question.json',
        queryParams: query_params,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'question_content',
            title: '问题内容',
            width: '30%',
            align: 'left'
        },{
        	field: 'answer_num',
            title: '回答数量',
            width: '10%',
            align: 'left'
        },{
        	field: 'user_name',
            title: '提问人',
            width: '10%',
            align: 'left'
        },{
        	field: 'category_name',
            title: '所属分类',
            width: '10%',
            align: 'left'
        },{
            field: 'create_time',
            title: '提问时间',
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
            field: 'question_id',
            title: '操作',
            align: 'center',
            hidden: !(Wz.getPermission('qa/question/list')||Wz.getPermission('qa/question/delete')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('qa/question/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.qa.questionManage.delQuestion('+val+')">删除</a>':'') +
                	(Wz.getPermission('qa/question/list')?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.qa.questionManage.showAnswerList('+row+')">查看答案</a>':'');
            }
        }]],
        toolbar: [{
            id: 'qa-question-add',
            text: '添加',
            disabled: !Wz.getPermission('qa/question/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editWin.window('open');
            }
        },{
            id: 'qa-question-del',
            text: '删除',
            disabled: !Wz.getPermission('qa/question/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var selects = questionTable.datagrid('getChecked');
            	if(selects.length == 0){
            		$.messager.alert('提示','请选择需要删除的问题！','info');
            		return false;
            	}
            	var question_id = [];
            	for(var i=0;i<selects.length;i++){
            		question_id.push(selects[i].question_id);
            	}
            	delQuestion(question_id.join(','));
            }
        },{
            id: 'qa-question-import',
            text: '导入问题',
            disabled: !Wz.getPermission('qa/question/import'),
            iconCls: 'icon-import',
            handler: function(){
            	importForm.form('reset');
            	importWin.window('open');
            }
        },{
            id: 'qa-question-import',
            text: '导出问题',
            disabled: !Wz.getPermission('qa/question/import'),
            iconCls: 'icon-export',
            handler: function(){
            	Wz.downloadFile("./api/qa/export_question.download?_t=" + new Date().getTime());
            }
        },{
            id: 'qa-question-move',
            text: '迁移',
            disabled: !Wz.getPermission('qa/question/move'),
            iconCls: 'icon-move',
            handler: function(){
            	moveForm.form('reset');
            	var questions = questionTable.datagrid('getChecked');
            	if(questions.length == 0){
            		$.messager.alert('提示','请选择需要迁移的问题！','info');
            		return false;
            	}
            	var question_id = [];
            	for(var i=0;i<questions.length;i++){
            		question_id.push(questions[i].question_id);
            	}
            	moveForm.find('input[name=question_id]').val(question_id.join(','));
            	moveWin.window('open');
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
                total: data.filtered_size,
                rows: data.question
            };
        }
    });
    
    var answer_params = {
    	question_id: ''
    };
    
    var answerTable = $('#qa-question-answer-table').datagrid({
    	url: './api/qa/get_answer.json',
        queryParams: answer_params,
        fitColumns: true,
        title: '回答列表:',
        checkOnSelect: false,
        striped: true,
        fit: true,
        showHeader: false,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name'
        },{
            field: 'answer_content'
        },{
            field: 'answer_id'
        },{
            field: 'create_time'
        },{
            field: 'like_num'
        },{
            field: 'question_id'
        },{
            field: 'user_name'
        }]],
        toolbar: [{
            id: 'qa-question-answer-add',
            text: '我来回答',
            iconCls: 'icon-add',
            disabled: !Wz.getPermission('qa/question/create_answer'),
            handler: function(){
            	answerForm.form('reset');
            	answerForm.find('input[name=question_id]').val(answer_params.question_id);
            	answerWin.window('open');
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
                total: data.filtered_size,
                rows: data.answer
            };
        }
    });
    
    var answerView = $.extend({}, $.fn.datagrid.defaults.view, {
        renderRow: function(target, fields, frozen, rowIndex, rowData){
        	if(!!!rowData.answer_id)return '';
        	var html = ['<td class="qa-question-answer-info">'];
        	html.push(['<h4>',rowData.user_name,'</h4><div><span>回答时间：',(!!rowData.create_time?Wz.dateFormat(new Date(rowData.create_time*1000),'yyyy-MM-dd hh:mm:ss'):''),'</span><span class="like-btn">',
        	           '<span class="l-btn-icon icon-heart"></span><span class="like-count">',rowData.like_num,'</span></span></div>',
        	           '<p>',rowData.answer_content,'</p>'].join(''));
        	html.push('</td>'+(Wz.getPermission('qa/question/delete_answer')?'<td><a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.qa.questionManage.delAnswer('+rowData.answer_id+')">删除</a></td>':''))
        	
            return html.join('');
        }
    });
    
    function showAnswerList(row){
    	var question = questionTable.datagrid('getData').rows[row];
    	answerListWin.find('.qa-question-question').html('问题：' + question.question_content);
    	answer_params.question_id = question.question_id;
    	answerListWin.window('open');
    	answerTable.datagrid({
    		view: answerView
    	});
    }
    
    function move(){
    	var url = './api/qa/change_question_category.json';
    	moveForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				moveWin.window('close');
    				questionTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function saveEdit(){
    	var url = './api/qa/add_question.json';
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
    				questionTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function saveAnswer(){
    	var url = './api/qa/add_answer.json';
    	answerForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				answerWin.window('close');
    				answerTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function delQuestion(question_id){
    	$.messager.confirm('提示','请确认是否要删除问题？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/qa/delete_question.json',
    				data: {
    					question_id: question_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						questionTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    
    function delAnswer(answer_id){
    	$.messager.confirm('提示','请确认是否要删除回答吗？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/qa/delete_answer.json',
    				data: {
    					answer_id: answer_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						answerTable.datagrid('reload');
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
    	var category_id = importCategory.combobox('getValue');
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
    				questionTable.datagrid('reload');
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
    

    function init(data){
    	if(!!data){
    		searchCategory.combobox('setValue',data.category_id);
    		searchForm.find('.search-btn').trigger('click');
    	}
    }
    
    return {
    	init: init,
    	move: move,
    	saveEdit: saveEdit,
    	delQuestion: delQuestion,
    	importQuestions: importQuestions,
    	openImportWin: openImportWin,
    	showAnswerList: showAnswerList,
    	saveAnswer: saveAnswer,
    	delAnswer: delAnswer
    };
}()

