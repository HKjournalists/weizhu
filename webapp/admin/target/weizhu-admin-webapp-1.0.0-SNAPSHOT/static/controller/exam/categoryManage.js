/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 考试管理-题库管理
 */
Wz.namespace('Wz.exam');
Wz.exam.categoryManage = function(){
    $.parser.parse('#main-contain');
    var editWin = $('#exam-category-edit-win');
    var editForm = $('#exam-category-edit-form');
    var questionListWin = $('#exam-category-questionlist-win');
    var questionSearchForm = $('#exam-category-question-search-form');
    var questionWin = $('#exam-question-edit-win');
    var questionForm = $('#exam-question-edit-form');
    var importWin = $('#exam-question-import-win');
    var importForm = $('#exam-question-import-form');
    var questionMoveWin = $('#exam-question-move-win');
    var questionMoveForm = $('#exam-question-move-form');
    
    var searchCategory = questionSearchForm.find('input[name=category_id]').combotree({
    	mode: 'remote',
    	treeField: 'category_name',
    	idField: 'category_id',
    	editable: false,
    	cascadeCheck: false,
    	panelWidth: 200,
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/adminExam/get_question_category.json',
				success: function(json){
					var result = [];
					var data = json.question_category;
		    		if(!!data){
		    			(function(data,arr){
		        			for(var i=0;i<data.length;i++){
		        				var node = {
		        					id: data[i].category_id,
		        					text: data[i].category_name,
		        					children: []
		        				};
		        				data[i].children = data[i].children.question_category || [];
		        				if(data[i].children.length > 0){
		        					arguments.callee(data[i].children,node.children);
		        				}
		        				arr.push(node);
		        			}    				
		    			}(data,result));
		    		}
					success(result);
				}
			});
		},
		onChange: function(newValue,oldValue){
			query_params.category_id = newValue;
			questionTable.datagrid('reload');
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });
    var moveQuestionCategory = questionMoveForm.find('input[name=new_category_id]').combotree({
    	mode: 'remote',
    	treeField: 'category_name',
    	idField: 'category_id',
    	editable: false,
    	cascadeCheck: false,
    	required: true,
    	panelWidth: 200,
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/adminExam/get_question_category.json',
				success: function(json){
					var result = [];
					var data = json.question_category;
		    		if(!!data){
		    			(function(data,arr){
		        			for(var i=0;i<data.length;i++){
		        				var node = {
		        					id: data[i].category_id,
		        					text: data[i].category_name,
		        					children: []
		        				};
		        				data[i].children = data[i].children.question_category || [];
		        				if(data[i].children.length > 0){
		        					arguments.callee(data[i].children,node.children);
		        				}
		        				arr.push(node);
		        			}    				
		    			}(data,result));
		    		}
					success(result);
				}
			});
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });
    var questionType = questionForm.find('input[name=type]').combobox({
    	width:350,
    	valueField:'value',
    	value:'OPTION_SINGLE',
    	textField:'name',
    	data:[{name:'单选题',value:'OPTION_SINGLE'},{name:'多选题',value:'OPTION_MULTI'},{name:'判断题',value:'OPTION_TF'}],
    	editable:false,
    	panelHeight:'auto',
    	onChange: function(newValue,oldValue){
    		if(newValue == 'OPTION_TF'){
    			optionTable.parents('.form-item').hide();
    			questionForm.find('input[name=question_answer]').parents('.form-item').show();
    		}else{
    			questionForm.find('input[name=question_answer]').parents('.form-item').hide();
    			optionTable.parents('.form-item').show();
    		}
    	}
    });
    
    var categoryTable = $('#exam-category-table').treegrid({
    	url: './api/adminExam/get_question_category.json',
    	treeField: 'category_name',
    	idField: 'category_id',
        rownumbers: true,
        checkOnSelect: true,
    	columns: [[{
            field: 'ck',
            checkbox: true
        },{
    		field: 'category_name',
    		width: '50%',
    		title: '题库名称'
    	},{
    		field: 'create_admin_name',
    		title: '创建人',
            align: 'center',
    		width: '15%'
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
            field: 'category_id',
            title: '操作',
            align: 'center',
            width: '10%',
            hidden: !(Wz.getPermission('exam/category/list_question')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('exam/category/list_question')?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.exam.categoryManage.showQuestion('+val+')">查看题目</a>':'');
            }
        }]],
    	toolbar: [{
            id: 'exam-category-add',
            text: '创建同级题库',
            disabled: !Wz.getPermission('exam/category/create'),
            iconCls: 'icon-add',
            handler: function(){
            	var categorys = categoryTable.treegrid('getChecked');
            	if(categorys.length == 0 && categoryTable.treegrid('getRoots').length > 0){
            		$.messager.alert('提示','请先选择一个题库进行创建！','info');
            		return false;
            	}
            	var category = categorys[0]||{parent_category_id: ''};
            	editForm.form('reset');
            	editForm.find('input[name=category_id]').val('');
            	editWin.find('input[name=type]').val('sibling');
            	editWin.find('input[name=parent_category_id]').val(category.parent_category_id);
            	editWin.window({title:'创建同级题库'}).window('open');
            }
        },{
            id: 'exam-subcategory-add',
            text: '创建子级题库',
            disabled: !Wz.getPermission('exam/category/create'),
            iconCls: 'icon-add',
            handler: function(){
            	var categorys = categoryTable.treegrid('getChecked');
            	if(categorys.length == 0){
            		$.messager.alert('提示','请先选择一个题库进行创建！','info');
            		return false;
            	}
            	editForm.form('reset');
            	editForm.find('input[name=category_id]').val('');
            	editWin.find('input[name=type]').val('sub');
            	editWin.find('input[name=parent_category_id]').val(categorys[0].category_id);
            	editWin.window({title:'添加子级题库'}).window('open');
            }
        },{
            id: 'exam-category-edit',
            text: '编辑',
            disabled: !Wz.getPermission('exam/category/update'),
            iconCls: 'icon-edit',
            handler: function(){
            	var categorys = categoryTable.treegrid('getChecked');
            	if(categorys.length == 0){
            		$.messager.alert('提示','请先选择一个题库进行编辑！','info');
            		return false;
            	}

            	editForm.form('load',categorys[0]);
            	editWin.find('input[name=type]').val('');
            	editWin.window({title:'编辑部门'}).window('open');
            }
        },{
            id: 'exam-category-del',
            text: '删除',
            disabled: !Wz.getPermission('exam/category/delete'),
            iconCls: 'icon-remove',
            handler: function(){
            	var categorys = categoryTable.treegrid('getChecked');
            	if(categorys.length == 0){
            		$.messager.alert('提示','请先选择一个题库进行删除！','info');
            		return false;
            	}
            	$.messager.confirm('提示','请确认是否删除选中题库及其子题库吗？',function(ok){
            		if(ok){
            			Wz.ajax({
            				type: 'post',
            				url: './api/adminExam/delete_question_category.json',
            				data: {
            					question_category_id: categorys[0].category_id
            				},
            				success: function(json){
            					if(json.result == 'SUCC'){
            						categoryTable.treegrid('remove',categorys[0].category_id);
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
    	loadFilter: function(data){
    		data = data.question_category;
    		if(!!data){
    			(function(data,pid){
        			for(var i=0;i<data.length;i++){
        				data[i].children = data[i].children.question_category || [];
        				data[i].parent_category_id = pid;
        				arguments.callee(data[i].children,data[i].category_id);
        			}    				
    			}(data,''));
    		}
    		return data;
    	}
    });
    
    var query_params = {
    	category_id: '',
    	condition: ''
    };
    var questionTable = $('#exam-category-question-table').datagrid({
        url: './api/adminExam/get_question_by_category.json',
        queryParams: query_params,
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
            field: 'question_name',
            title: '考题内容',
            width: '20%',
            align: 'left'
        },{
            field: 'type',
            title: '考题类型',
            align: 'left',
            width: '10%',
            formatter: function(val,obj,row){
            	return val=='OPTION_MULTI'?'多选题':(val=='OPTION_SINGLE'?'单选题':'判断题');
            }
        },{
    		field: 'create_admin_name',
    		title: '创建人',
    		width: '20%'
    	},{
    		field: 'create_time',
    		title: '创建时间',
    		width: '15%',
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
            width: '20%',
            hidden: !(Wz.getPermission('exam/category/update_question')||Wz.getPermission('exam/category/delete_question')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('exam/category/update_question')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.exam.categoryManage.editQuestion('+row+')">编辑</a>':'') + 
                (Wz.getPermission('exam/category/delete_question')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.exam.categoryManage.delQuestion('+val+')">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'exam-question-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('exam/category/create_question'),
            iconCls: 'icon-add',
            handler: function(){
            	questionForm.form('reset');
            	questionForm.find('input[name=question_category_id]').val(searchCategory.combotree('getValue'));
            	questionForm.find('input[name=question_id]').val('');
            	questionForm.find('input[name=option]').val('');
            	optionTable.datagrid('loadData',[]);
            	questionWin.window('open');
            }
        },{
            id: 'exam-question-del',
            text: Wz.lang.common.grid_del_btn,
            disabled: !Wz.getPermission('exam/category/delete_question'),
            iconCls: 'icon-remove',
            handler: function(){
            	var questions = questionTable.datagrid('getChecked');
            	if(questions.length == 0){
            		$.messager.alert('提示','请选择需要删除的考题','info');
            		return false;
            	}
            	var question_id = [];
    			for(var i=0;i<questions.length;i++){
    				question_id.push(questions[i].question_id);
    			}
    			delQuestion(question_id.join(','));
            }
        },{
            id: 'exam-question-move',
            text: '迁移',
            disabled: !Wz.getPermission('exam/category/move_question'),
            iconCls: 'icon-move',
            handler: function(){
            	var questions = questionTable.datagrid('getChecked');
            	if(questions.length == 0){
            		$.messager.alert('提示','请选择需要迁移的考题','info');
            		return false;
            	}
            	var question_id = [];
    			for(var i=0;i<questions.length;i++){
    				question_id.push(questions[i].question_id);
    			}
    			questionMoveForm.form('load',{
    				old_category_id: query_params.category_id,
    				question_id: question_id.join(',')
    			});
    			questionMoveWin.window('open');
            }
        },{
            id: 'exam-question-import-btn',
            text: '导入考题',
            disabled: !Wz.getPermission('exam/category/import_question'),
            iconCls: 'icon-import',
            handler: function(){
            	importForm.form('reset');
            	importWin.find('input[name=category_id]').val(searchCategory.combotree('getValue'));
            	importWin.window('open');
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
                total: data.total,
                rows: data.question||[]
            };
        }
    });
    
    var optionTable = $('#exam-question-option-table').datagrid({
    	data: [],
    	fitColumns: true,
        striped: true,
        title: '选项列表：',
        fit: true,
        columns: [[{
        	field: 'option_name',
        	width: 320,
        	editor: {
        		type: 'textbox',
        		options: {
        			required: true,
        			prompt:'请输入选项内容，1~190个字符',
        			validType:'length[1,190]'
        		}
        	},
        	title: '选项文字'
        },{
        	field: 'is_right',
        	editor: {
        		type: 'checkbox',
        		options: {
        			on: true,
        			off: false
        		}
        	},
        	title: '正确答案（是/否）',
        	align: 'center',
        	formatter: function(val,obj,row){
        		return val=='true'?'是':'否';
        	}
        },{
        	field: 'option_id',
        	title: '操作',
        	align: 'center',
        	formatter: function(val,obj,row){
        		return '<a href="javascript:void(0)" onclick="Wz.exam.categoryManage.delQuestionOption('+row+')" class="table-cell-icon icon-remove">&nbsp;</a>';
        	}
        }]],
        toolbar: [{
            id: 'exam-question-add',
            text: Wz.lang.common.gridtool_create_btn,
            iconCls: 'icon-add',
            handler: function(){
            	var editIndex = optionTable.datagrid('options').editIndex;
            	if(optionTable.datagrid('validateRow',editIndex)){
            		if(editIndex != undefined){
            			optionTable.datagrid('endEdit',editIndex);
            		}
            		optionTable.datagrid('appendRow',{status:'false'});
            		editIndex = optionTable.datagrid('getRows').length-1;
            		optionTable.datagrid('beginEdit', editIndex);
            		optionTable.datagrid('options').editIndex = editIndex;            		
            	}
            }
        }],
        editIndex: undefined,
        onClickCell: function(index,field){
        	if(field == 'option_id') return false;
        	var editIndex = optionTable.datagrid('options').editIndex;
        	if(editIndex != index){
    			if(optionTable.datagrid('validateRow',editIndex)){
            		if(editIndex != undefined){
            			optionTable.datagrid('endEdit',editIndex);
            		}
    				optionTable.datagrid('beginEdit',index);
    				optionTable.datagrid('options').editIndex = index;
    			}
        	}
        	
        }
    });
    
    function delQuestionOption(row){
    	optionTable.datagrid('deleteRow',row);
    }
    
    function editQuestion(row){
    	var question = questionTable.datagrid('getData').rows[row];
    	questionForm.form('load',question);
    	questionForm.find('input[name=question_id]').val(question.question_id);
    	questionForm.find('input[name=question_category_id]').val(searchCategory.combotree('getValue'));
    	questionForm.find('input[name=type]').val(question.type);
    	questionForm.find('input[name=option]').val(JSON.stringify({option:question.option}));
    	var options = [];
    	for(var i=0;i<question.option.length;i++){
    		var option = question.option[i]; 
    		option.is_right = option.is_right + '';
    		options.push(option);
    	}
    	
    	optionTable.datagrid('loadData',options);
    	questionWin.window('open');
    }
    
    function saveCategory(){
    	var url = './api/adminExam/create_question_category.json';
    	var category_id = editForm.find('input[name=category_id]').val();
    	var category_name = editForm.find('input[name=category_name]').val();
    	var parent_category_id = editForm.find('input[name=parent_category_id]').val();
    	if(category_id != ''){
    		url = './api/adminExam/update_question_category.json';
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
    				if(category_id != ''){
    					categoryTable.treegrid('update',{
    						id: category_id,
    						row: {
    							category_name: category_name
    						}
    					})
    				}else{
    					categoryTable.treegrid('reload');
    				}    				
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    			}
    		}
    	});
    }
    
    function saveQuestion(){
		Wz.showLoadingMask('正在处理中，请稍后......');
    	var url = './api/adminExam/create_question.json';
    	var question_id = questionForm.find('input[name=question_id]').val();
    	if(question_id != ''){
    		url = './api/adminExam/update_question.json';
    	}
    	questionForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			var question_type = questionType.combobox('getValue');
    			if(question_type != 'OPTION_TF'){
    				var editIndex = optionTable.datagrid('options').editIndex;
    				var options = [];
    				var rightNum = 0;
    				if(editIndex != undefined){
    					if(!optionTable.datagrid('validateRow',editIndex)){
    						optionTable.datagrid('deleteRow',editIndex);
    					}else{
    						optionTable.datagrid('endEdit',editIndex)
    					}
    				}
    				if(!$(this).form('validate')){
    	    			Wz.hideLoadingMask();
    					return false;
    				}
    				var rows = optionTable.datagrid('getData').rows;
    				if(rows.length < 2){
    					$.messager.alert('提示','请至少提供两个选项！','info');
    	    			Wz.hideLoadingMask();
    					return false;
    				}else{
    					for(var i=0;i<rows.length;i++){
    						if(rows[i].is_right == 'true') rightNum++;
    						options.push({
    							option_id: rows[i].option_id || 0,
    							option_name: rows[i].option_name,
    							is_right: (rows[i].is_right == 'true'?true:false)
    						});
    					}
    					if(rightNum == 0){
    						$.messager.alert('提示','您提供的选项中至少需要一个正确答案！','info');
    		    			Wz.hideLoadingMask();
    						return false;
    					}else if(question_type == 'OPTION_MULTI' && rightNum < 2){
    						$.messager.alert('提示','多选题必须至少有两个正确答案！','info');
    		    			Wz.hideLoadingMask();
    						return false;
    					}else if(question_type == 'OPTION_SINGLE' && rightNum > 1){
    						$.messager.alert('提示','单选题有且只能有一个正确答案！','info');
    		    			Wz.hideLoadingMask();
    						return false;
    					}
    					questionForm.find('input[name=option]').val(JSON.stringify({option:options}));
    					return true;
    				}
    			}else{
    				var question_answer = questionForm.find('input[textboxname=question_answer]').combobox('getValue');
    				if(question_answer=='true'){
    					questionForm.find('input[name=option]').val('{"option":[{"option_id":0,"option_name":"正确","is_right":true},{"option_id":0,"option_name":"错误","is_right":false}]}');
    				}else{
    					questionForm.find('input[name=option]').val('{"option":[{"option_id":0,"option_name":"正确","is_right":true},{"option_id":0,"option_name":"错误","is_right":true}]}');
    				}
    			}
    			return true;
    		},
    		dataType: 'json',
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				questionWin.window('close');
    				questionTable.datagrid('reload');			
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    function showQuestion(category_id){
    	searchCategory.combotree('setValue',category_id);
    	query_params.category_id = category_id;
    	questionTable.datagrid('reload');
    	questionListWin.window('open');
    }
    function delQuestion(question_id){
    	$.messager.confirm('提示','请确认是否要删除考题？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/adminExam/delete_question.json',
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
    function importQuestion(){
    	var category_id = importWin.find('input[name=category_id]').val();
    	importForm.form('submit',{
    		url: './api/adminExam/import_question.json?question_category_id='+category_id+'&company_id='+Wz.cur_company_id,
    		onSubmit: function(){
    			Wz.showLoadingMask();
    		},
    		success: function(result){
    			Wz.hideLoadingMask();
    			console.info(result);
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				importWin.window('close');
    				questionTable.datagrid('reload');
    			}else{
    				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
    					if(ok){
    						Wz.downloadFile("./api/adminExam/get_import_fail_log.json?_t=" + new Date().getTime());
    					}
    				});
    			}
    		}
    	});
    }
    importForm.find('.download-btn').click(function(){
    	Wz.downloadFile('./static/res/exam_template.xlsx');
    });
    function moveQuestion(){
    	questionMoveForm.form('submit',{
    		url: './api/adminExam/update_question_in_question_category.json',
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
    				questionMoveWin.window('close');
    				questionTable.datagrid('reload');  				
    			}else{
					$.messager.alert('错误',result.fail_text,'error');
    				return false;
				}
    		}
    	});
    }
    return {
    	importQuestion: importQuestion,
    	delQuestion: delQuestion,
    	saveQuestion: saveQuestion,
    	delQuestionOption: delQuestionOption,
    	editQuestion: editQuestion,
    	showQuestion: showQuestion,
    	saveCategory: saveCategory,
    	moveQuestion: moveQuestion
    };
}()

