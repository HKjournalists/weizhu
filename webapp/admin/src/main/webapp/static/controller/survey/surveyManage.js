/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 调研管理-调研管理
 */
Wz.namespace('Wz.survey');
Wz.survey.surveyManage = function(){
    $.parser.parse('#main-contain');
    var query_params = {
    	survey_name: ''
    };
    var searchForm = $('#survey-search-form');
    var editWin = $('#survey-edit');
    var firstTab = $('#survey-firststep-tab');
    var firststepForm = $('#survey-firststep-form');
    var secondTab = $('#survey-secondstep-tab');
    var thirdTab = $('#survey-thirdstep-tab');
    var showview = $('#survey-showexam-view');
    var thirdstepForm = $('#survey-thirdstep-form');
    var editTabs = $('#survey-edit-steps').tabs({
    	onSelect: function(title,index){
    		if(index == 0){
    			$('#survey-edit-steps').tabs('disableTab',1).tabs('disableTab',2);
    		}else if(index == 1){
    			$('#survey-edit-steps').tabs('disableTab',2);
    		}
    	}
    });
    var editQuestionWin = $('#survey-question-edit-win');
    var editQuestionForm = $('#survey-question-edit-form');
    var copyWin = $('#survey-copy');
    var copyForm = $('#survey-copy-form');
    var viewWin = $('#survey-view');
    var resultWin = $('#survey-result-win');
    var importWin = $('#survey-question-import-win');
    var importForm = $('#survey-question-import-form');
    var curSurvey = {
    	survey: {},
    	questions: [],
    	isCopy: false
    };

    var editStep = $('#survey-edit-steps').tabs({
    	tabPosition: 'left',
    	tabWidth: 100,
    	headerWidth: 100
    });
    searchForm.find('.search-btn').click(function(){
    	query_params.survey_name = searchForm.find('input[name=survey_name]').val();
    	surveyTable.datagrid('reload');
    });
    
    var uploadImge = firststepForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '建议图片尺寸120x120(px)<br>支持jpg、png格式，大小1M以内',
    	maxSize: 1,
    	params: {
    		image_tag: '调研,图标'
    	}
    });
    var startTime = firststepForm.find('input[name=start_time]').datetimebox({
    	editable: false,
        required: true
    });
    var endTime = firststepForm.find('input[name=end_time]').datetimebox({
    	editable: false,
        required: true
    });
    var surveyDesc = KindEditor.create(editWin.find('textarea[name=survey_desc]'), {
		resizeType : 1,
		allowPreviewEmoticons : false,
		allowImageUpload : false,
		minWidth: 400,
		items : [
			'fontname', 'fontsize', '|', 'forecolor', 'hilitecolor', 'bold', 'italic', 'underline',
			'removeformat', '|', 'justifyleft', 'justifycenter', 'justifyright', 'insertorderedlist',
			'insertunorderedlist', '|', 'link']
	});
    var allowModel = Wz.comm.allowService(firststepForm.find('input[name=allow_model_id]'),{
    	copy_enable: true,
    	copy_win_title: '复制调研对象',
    	copy_table_columns: [[{
    		field: 'ck',
            checkbox: true
    	},{
            field: 'survey_name',
            title: '调研标题',
            width: '200px'
        },{
            field: 'start_time',
            title: '调研时间',
            align: 'center',
            formatter: function(val,obj,row){
            	return Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss') + '至' + Wz.dateFormat(new Date(obj.end_time*1000),'yyyy-MM-dd hh:mm:ss');
            }
        }]],
        copy_table_url: './api/survey/get_survey_list.json',
        id_field: 'allow_model_id',
        name_field: 'allow_model_name',
        copyParam: {
        	survey_name: ''
        },
        dName: 'survey',
        fName: 'filtered_size'
    });
    editQuestionForm.find('input[name=question_type]').combobox({
    	width: 350,
    	textField: 'label',
    	valueField: 'value',
    	panelHeight: 'auto',
    	editable: false,
    	data: [{label:'单选题',value:'0'},{label:'多选题',value:'1'},{label:'下拉框',value:'2'},{label:'问答题',value:'3'}],
    	value: '0',
    	onChange: function(newValue,oldValue){
    		if(newValue == '3'){
    			optionTable.parents('.form-item').hide();
    		}else{
    			optionTable.parents('.form-item').show();
    		}
    	}
    });
    var surveyTable = $('#survey-table').datagrid({
        url: './api/survey/get_survey_list.json',
        queryParams: query_params,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'survey_name',
            title: '标题',
            width: '200px'
        },{
            field: 'start_time',
            title: '调研时间',
            align: 'center',
            width: 260,
            formatter: function(val,obj,row){
            	return Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss') + '至' + Wz.dateFormat(new Date(obj.end_time*1000),'yyyy-MM-dd hh:mm:ss');
            }
        },{
            field: 'create_admin_name',
            title: '创建人',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val:'';
            }
        },{
            field: 'create_time',
            title: '创建时间',
            width: '140px',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'survey_id',
            title: '操作',
            align: 'center',
            width: 230,
            hidden: !(Wz.getPermission('survey/survey/create')||Wz.getPermission('survey/survey/update')||Wz.getPermission('survey/survey/delete')||Wz.getPermission('survey/survey/set_state')||Wz.getPermission('survey/survey/list_result')),
            formatter: function(val,obj,row){
            	var state = (obj.state=='NORMAL'?'禁用':'启用');
                return (Wz.getPermission('survey/survey/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.survey.surveyManage.editSurvey('+row+')">编辑</a>':'') +
                	(Wz.getPermission('survey/survey/copy')?'<a href="javascript:void(0)" class="tablelink resurvey-btn" onclick="Wz.survey.surveyManage.copySurvey('+row+')">复制</a>':'') +
                	(Wz.getPermission('survey/survey/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.survey.surveyManage.delSurvey('+val+')">删除</a>':'') +
                	(Wz.getPermission('survey/survey/set_state')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.survey.surveyManage.changeState('+row+')">'+state+'</a>':'') + 
                	(Wz.getPermission('survey/survey/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.survey.surveyManage.showSurvey('+val+','+row+')">预览</a>':'') + 
                	(Wz.getPermission('survey/survey/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.survey.surveyManage.showResult('+val+')">查看结果</a>':'');
            }
        }]],
        toolbar: [{
            id: 'survey-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('survey/survey/create'),
            iconCls: 'icon-add',
            handler: function(){
				curSurvey.isCopy = false;
				curSurvey.survey = {};
				curSurvey.questions = [];
				firststepForm.form('reset');
            	editWin.window({title:'新建调研'}).window('open');
            	editTabs.tabs('select',0);
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
                rows: data.survey
            };
        }
    });
    var questionParam = {
    	survey_id: ''
    };
    var surveyQuestionTable = $('#survey-question-talbe').datagrid({
    	url: './api/survey/get_survey_by_id.json',
    	queryParams: questionParam,
    	fitColumns: true,
        striped: true,
        fit: true,
        rownumbers: true,
        columns: [[{
            field: 'question_name',
            title: '题目',
            width: 400
        },{
            field: 'type',
            title: '题目类型',
            width: 80,
            formatter: function(val,obj,row){
            	var result = '问答题';
            	if(val == 'VOTE'){
            		result = (obj.check_num == 1?'单选题':'多选题');
            	}else if(val == 'INPUT_SELECT'){
            		result = '下拉框';
            	}
            	return result;
            }
        },{
            field: 'sort',
            title: '排序',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
                return '<a href="javascript:void(0)" class="table-cell-icon icon-up move-up-btn">&nbsp;</a><a href="javascript:void(0)" class="table-cell-icon icon-down move-down-btn">&nbsp;</a>'
            }
        },{
            field: 'question_id',
            title: '操作',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" class="table-cell-icon icon-edit edit-btn">&nbsp;</a><a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
            }
        }]],
        toolbar: [{
            id: 'survey-questin-add',
            text: '创建问题',
            iconCls: 'icon-add',
            handler: function(){
            	editQuestionForm.form('reset');
            	editQuestionForm.find('input[name=question_id]').val('');
            	optionTable.parents('.form-item').show();
            	optionTable.datagrid('loadData',[]);
            	editQuestionForm.find('input[textboxname=question_type]').combobox('enable');
            	editQuestionWin.window({title:'创建问题'}).window('open');
            }
        },{
            id: 'survey-questin-import',
            text: '导入问题',
            iconCls: 'icon-import',
            handler: function(){
            	importForm.form('reset');
            	importWin.find('input[name=survey_id]').val(curSurvey.survey.survey_id);
            	importWin.window('open');
            }
        }],
        loadFilter: function(data){
        	var questions = data.questions.questions||[];
            return {
                total: questions.length,
                rows: questions
            };
        },
        editIndex: null
    });
    
    var optionTable = $('#survey-question-option-table').datagrid({
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
        	field: 'option_id',
        	title: '操作',
        	align: 'center',
        	formatter: function(val,obj,row){
        		return '<a href="javascript:void(0)" onclick="Wz.exam.categoryManage.delQuestionOption('+row+')" class="table-cell-icon icon-remove">&nbsp;</a>';
        	}
        }]],
        toolbar: [{
            id: 'survey-question-option-add',
            text: Wz.lang.common.gridtool_create_btn,
            iconCls: 'icon-add',
            handler: function(){
            	var editIndex = optionTable.datagrid('options').editIndex;
            	if(optionTable.datagrid('validateRow',editIndex)){
            		if(editIndex != undefined){
            			optionTable.datagrid('endEdit',editIndex);
            		}
            		optionTable.datagrid('appendRow',{});
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
    
    var showSurveyTable = $('#survey-showSurvey').datagrid({
    	data: [],
    	fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        showHeader: false,
        columns: [[{
            field: 'question_name'
        }]]
    });
    var showSurveyview = $('#survey-showexam-view').datagrid({
    	data: [],
    	fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        showHeader: false,
        columns: [[{
            field: 'question_name'
        }]]
    });
    
    var questionView = $.extend({}, $.fn.datagrid.defaults.view, {
        renderRow: function(target, fields, frozen, rowIndex, rowData){
        	if(!!!rowData.question_id) return '';
        	var html = ['<td class="survey-question-info">'];
        	var options = rowData.options||[];
        	var oHtml = [];
        	var optionTags = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'];
        	for(var i=0;i<options.length;i++){
        		oHtml.push('<div class="survey-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
        	}
        	var type_tag = '填空题';
        	if(rowData.type == 'INPUT_SELECT'){
        		type_tag = '下拉框';
        	}else if(rowData.type == 'VOTE'){
        		type_tag = '多选题';
        		if(rowData.check_num == 1){
        			type_tag = '单选题';
        		}
        	}
        	html.push(['<h4>',(rowIndex+1),'. ',rowData.question_name,'(',type_tag,')</h4><div class="survey-question-optionlist">',oHtml.join(''),'</div>'].join(''));
        	html.push('</td>')
        	
            return html.join('');
        }
    });
    var resultAnswerTable = $('#survey-result-answer-table').datagrid({
    	data: [],
    	fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        showHeader: false,
        title: '回答信息',
        columns: [[{
            field: 'question_name'
        }]]
    });
    var resultView = $.extend({}, $.fn.datagrid.defaults.view, {
        renderRow: function(target, fields, frozen, rowIndex, rowData){
        	if(!!!rowData.question_id) return '';
        	var html = ['<td class="survey-question-info">'];
        	var options = rowData.options||[];
        	var oHtml = [];
        	var optionTags = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'];
        	var type_tag = '填空题';
        	var answers = [];
        	if(rowData.type == 'INPUT_SELECT'){
        		type_tag = '下拉框';
        		for(var i=0;i<options.length;i++){
            		oHtml.push('<div class="survey-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
            		if($.inArray(options[i].option_id,rowData.result)>-1){
            			answers.push(optionTags[i])
            		}
            	}
        	}else if(rowData.type == 'VOTE'){
        		type_tag = '多选题';
        		if(rowData.check_num == 1){
        			type_tag = '单选题';
        		}
            	for(var i=0;i<options.length;i++){
            		oHtml.push('<div class="survey-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
            		if($.inArray(options[i].option_id,rowData.result)>-1){
            			answers.push(optionTags[i])
            		}
            	}
        	}else{
        		answers.push(rowData.result);
        	}
        	html.push(['<h4>',(rowIndex+1),'. ',rowData.question_name,'(',type_tag,')</h4><div class="survey-question-optionlist">',oHtml.join(''),'</div>',
        	           (!!rowData.user_name?'<p class="exam-question-answer">'+rowData.user_name+'回答：'+answers.join(',')+'</p>':'')].join(''));
        	html.push('</td>')
        	
            return html.join('');
        }
    });
    var resultParam = {
    	survey_id: ''
    };
    var resultTable = $('#survey-result-byuser-table').datagrid({
    	url: './api/survey/get_survey_result_list.json',
        queryParams: resultParam,
        fitColumns: true,
        checkOnSelect: true,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        singleSelect:true,
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'user_name',
            title: '姓名',
            width: '100px'
        },{
            field: 'user_team',
            title: '部门',
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
            field: 'user_position',
            title: '职务',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val:'';
            }
        },{
            field: 'survey_result',
            title: '提交时间',
            width: '130px',
            align: 'center',
            formatter: function(val,obj,row){
            	val = JSON.parse(val).submit_time;
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        }]],
        toolbar: [{
            id: 'survey-exportresult',
            text: '导出调研结果',
            disabled: !Wz.getPermission('survey/survey/export_result'),
            iconCls: 'icon-export',
            handler: function(){
            	Wz.downloadFile('./api/survey/download_survey_result.json?survey_id='+resultParam.survey_id+'&t='+new Date().getTime());
            }
        }],
        onCheck: function(index,row){
        	var answers = JSON.parse(row.survey_result).answer || [];
        	var questions = resultAnswerTable.datagrid('getRows');

        	if(answers.length == 0){
        		for(var i=0;i<questions.length;i++){
        			if(questions[i].user_name){
        				delete questions[i].result;
        				delete questions[i].user_name;
        			}
        		}
        	}else{
        		while(answers.length > 0){
        			var answer = answers.pop();
        			for(var i=0;i<questions.length;i++){
        				var question = questions[i];
        				questions[i].user_name = row.user_name;
        				if(questions[i].question_id == answer.question_id){
        					if(question.type == 'INPUT_TEXT'){
        						questions[i].result = answer.input_text.result_text;
        					}else if(question.type == 'VOTE'){
        						questions[i].result = answer.vote.option_id;
        					}else if(question.type == 'INPUT_SELECT'){
        						
        						questions[i].result = [answer.input_select.option_id];
        					}
        					break;
        				}else{
        					
        					if(i == questions.length-1){
        						delete questions[i].result;
        						delete questions[i].user_name;
        					}
        				}
        			}
        			
        		}
        	}
        	resultAnswerTable.datagrid('loadData',questions);
        },
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
            return {
                total: data.filtered_size,
                rows: data.survey_result_list
            };
        }
    });
    var showquestionTable = $('#survey-result-byuser-table').datagrid({
    	
    });
    	
    
    
    
    firstTab.find('.next-btn').click(function(){ 
    	var url = './api/survey/create_survey.json';
    	if(!firststepForm.form('validate')){
    		$.messager.alert('错误','请正确填写表单内容！','error');
    		return false;
    	}
    	curSurvey.survey.allow_model_id = allowModel.getValue();
    	if(curSurvey.survey.allow_model_id == ''){
    		$.messager.alert('错误','请为调研创建访问模型！','error');
    		return false;
    	}
    	curSurvey.survey.image_name = uploadImge.getValue();
    	curSurvey.survey.survey_id = firststepForm.find('input[name=survey_id]').val();
    	curSurvey.survey.survey_name = firststepForm.find('input[name=survey_name]').val();
    	curSurvey.survey.show_result_type = firststepForm.find('input[name=show_result_type]').val();
    	curSurvey.survey.start_time = Math.floor(Wz.parseDate(startTime.datetimebox('getValue')).getTime()/1000);
    	curSurvey.survey.end_time = Math.floor(Wz.parseDate(endTime.datetimebox('getValue')).getTime()/1000);
    	curSurvey.survey.survey_desc = surveyDesc.html();
    	
    	if(curSurvey.survey.start_time > curSurvey.survey.end_time){
    		$.messager.alert('错误','调研开始时间不能大于调研结束时间！','error');
    		return false;
    	}
    	
    	if(curSurvey.survey.survey_id != ''){
    		url = './api/survey/update_survey.json';
    		if(curSurvey.isCopy){
    			url = './api/survey/copy_survey.json';
    			curSurvey.survey.question_list = '{"question":[]}';
    		}
    	}else{
    		curSurvey.survey.question_list = '{"question":[]}';
    	}

		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: curSurvey.survey,
    		success:function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				curSurvey.survey.survey_id = json.survey_id || curSurvey.survey.survey_id;
    				questionParam.survey_id = curSurvey.survey.survey_id;
    				firststepForm.find('input[name=survey_id]').val(curSurvey.survey.survey_id);
    		    	surveyQuestionTable.datagrid('reload');
    		    	editTabs.tabs('enableTab',1).tabs('select',1);
    		    	surveyTable.datagrid('reload');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    });
    secondTab.find('.prev-btn').click(function(){
    	editTabs.tabs('select',0);
    });
    secondTab.on('click','.move-up-btn',function(){
    	var index = $(this).parents('tr').index();
    	if(index > 0){
    		var row = surveyQuestionTable.datagrid('getData').rows[index];
    		surveyQuestionTable.datagrid('deleteRow',index);
    		surveyQuestionTable.datagrid('insertRow',{
    			index: index-1,
    			row: row
    		});
        	sortQuestion();
    	}
    });
    secondTab.on('click','.move-down-btn',function(){
    	var index = $(this).parents('tr').index();
    	var rows = surveyQuestionTable.datagrid('getData').rows
    	if(index < rows.length-1){
    		var row = rows[index];
    		surveyQuestionTable.datagrid('deleteRow',index);
    		surveyQuestionTable.datagrid('insertRow',{
    			index: index+1,
    			row: row
    		});
        	sortQuestion();
    	}
    });
    secondTab.on('click','.edit-btn',function(){
    	var index = $(this).parents('tr').index();
    	var question = surveyQuestionTable.datagrid('getRows')[index];
    	if(question.type == 'VOTE'){
    		if(question.check_num == 1){
    			question.question_type = '0';
    		}else{
    			question.question_type = '1';
    		}
    	}else if(question.type == 'INPUT_SELECT'){
    		question.question_type = '2';
    	}else{
    		question.question_type = '3';
    	}
    	editQuestionForm.form('load',question);
    	if(question.question_type == '3'){
    		optionTable.parents('.form-item').hide();
    	}else{
    		optionTable.datagrid('loadData',question.options);
    		optionTable.parents('.form-item').show();
    	}
    	editQuestionForm.find('input[textboxname=question_type]').combobox('disable');
    	editQuestionForm.find('input[name=is_optional]').prop('checked',question.is_optional);
    	editQuestionWin.window({title:'修改问题'}).window('open');
    	
    });
    secondTab.on('click','.del-btn',function(){
    	var index = $(this).parents('tr').index();
    	var question = surveyQuestionTable.datagrid('getRows')[index];
    	$.messager.confirm('提示','请确认是否要删除问题？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/survey/delete_question.json',
    				data: {
    					survey_id: questionParam.survey_id,
    					question_id_list: question.question_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						surveyQuestionTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});    	
    });
    secondTab.on('keyup','input[name=score]',function(e){
    	var val = this.value;
    	val = val.replace(/[^\d]/g,'');
    	val = val.replace(/^0+/,'')||'0';
    	val = parseInt(val);
    	val = (val>100?100:val);
    	$(this).val(val);
    	freshTotalScore();
    }).on('blur','input[name=score]',function(e){
    	var index = $(this).parents('tr').index();
    	var row = surveyQuestionTable.datagrid('getData').rows[index];
    	row.score = parseInt(this.value);
    	surveyQuestionTable.datagrid('updateRow',{
    		index: index,
    		row: row
    	});
    });
    secondTab.find('.next-btn').click(function(){
    	curSurvey.questions = surveyQuestionTable.datagrid('getData').rows;
    	thirdTab.find('.survey-info').prev().text(curSurvey.survey.survey_name);
    	thirdTab.find('.survey-info').html('<span><span>题目数：'+curSurvey.questions.length+'</span><span>调研时间：'+Wz.dateFormat(new Date(curSurvey.survey.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(curSurvey.survey.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
    	thirdTab.find('.survey-desc').html(curSurvey.survey.survey_desc);
    	showSurveyTable.datagrid({
    		data: curSurvey.questions,
    		view: questionView
    	});
    	editTabs.tabs('enableTab',2).tabs('select',2);
    });
    thirdTab.find('.next-btn').click(function(){
    	editWin.window('close');
    });
    thirdTab.find('.prev-btn').click(function(){
    	editTabs.tabs('select',1);
    });
    function freshTotalScore(){
    	var totalScore = 0;
    	secondTab.find('input[name=score]').each(function(){
    		totalScore += parseInt(this.value);
    	});
    	secondTab.find('.total-score').text(totalScore + '/100');
    	return totalScore;
    }
    
    function delSurvey(survey_id){
    	$.messager.confirm('提示','请确认是否删除调研？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/survey/delete_survey.json',
    				data: {
    					survey_id_list: survey_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						surveyTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    					}
    				}
    			});    			
    		}
    	});
    }
    function editSurvey(row){
    	var survey = surveyTable.datagrid('getRows')[row];
    	curSurvey.survey = survey;
    	questionParam.survey_id = survey.survey_id;
		survey.start_time = Wz.dateFormat(new Date(survey.start_time*1000),'yyyy-MM-dd hh:mm:ss');
		survey.end_time = Wz.dateFormat(new Date(survey.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		survey.show_result_type = survey.show_result_type || 'NONE';
		firststepForm.form('load',survey);
		curSurvey.isCopy = false;
		uploadImge.setValue(survey.image_name,survey.image_url);
		allowModel.setValue({model_id: survey.allow_model_id,model_name:survey.allow_model_name});
		surveyDesc.html(survey.survey_desc);
		editTabs.tabs('disableTab',1).tabs('disableTab',2);
    	editWin.window({title:'修改调研'}).window('open');
		editTabs.tabs('select',0);
    }
    
    function copySurvey(row){
    	var survey = surveyTable.datagrid('getRows')[row];
    	curSurvey.survey = survey;
    	questionParam.survey_id = survey.survey_id;
    	survey.survey_name = survey.survey_name + '(复制)';
		survey.start_time = Wz.dateFormat(new Date(survey.start_time*1000),'yyyy-MM-dd hh:mm:ss');
		survey.end_time = Wz.dateFormat(new Date(survey.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		survey.show_result_type = survey.show_result_type || 'NONE';
		firststepForm.form('load',survey);
		curSurvey.isCopy = true;
		uploadImge.setValue(survey.image_name,survey.image_url);
		allowModel.setValue({model_id: survey.allow_model_id,model_name:survey.allow_model_name});
		surveyDesc.html(survey.survey_desc);
		editTabs.tabs('disableTab',1).tabs('disableTab',2);
    	editWin.window({title:'复制调研'}).window('open');
		editTabs.tabs('select',0);
    }
    
    function viewSurvey(survey_id){
    	Wz.ajax({
    		url: './api/adminSurvey/get_survey_by_id.json',
    		data: {
    			survey_id: survey_id,
    			_t: new Date().getTime()
    		},
    		success: function(json){
    			var survey = json.survey[0];
    			if(!!survey){
        			Wz.ajax({
        				url: './api/adminSurvey/get_survey_question.json',
        				data: {
        					survey_id: survey_id,
        					_t: new Date().getTime()
        				},
        				success: function(json){
        					var questions = json.question;
        					viewWin.find('.survey-info').prev().text(survey.survey_name);
        					viewWin.find('.survey-info').html('<span>总分：100分</span><span>通过分数：'+survey.pass_mark+'分</span><span>题目数：'+questions.length+'</span><span>考试时间：'+Wz.dateFormat(new Date(survey.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(survey.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
        			    	viewSurveyTable.datagrid({
        			    		data: questions,
        			    		view: questionView
        			    	});
        			    	viewWin.window('open');
        				}
        			});
    			}
    		}
    	});
    }
    function showResult(survey_id){
    	resultParam.survey_id = survey_id;
    	resultTable.datagrid('reload');
    	Wz.ajax({
    		url: './api/survey/get_survey_by_id.json',
    		data: resultParam,
    		success: function(json){
    			if(!!json.survey){
    				resultAnswerTable.datagrid({
    					data: json.questions.questions,
    					view: resultView
    				});
    			}else{
    				surveyTable.datagrid('reload');
    			}    			
    		}
    	});
    	resultWin.window('open');
    }
    function showSurvey(survey_id,row){
    	var survey = surveyTable.datagrid('getRows')[row];
    	resultParam.survey_id = survey_id;
    	Wz.ajax({
    		url: './api/survey/get_survey_by_id.json',
    		data: resultParam,
    		success: function(json){
    				showSurveyview.datagrid({
    					data: json.questions.questions,
    					view: resultView
    				});		
    		}
    	});
    	viewWin.find('.survey-info').prev().text(survey.survey_name);
    	viewWin.find('.survey-info').html('<span><span style="margin-top:10px;text-align:center">描述：'+survey.survey_desc+'</span><span style="margin-top:10px;text-align:center">调研时间：'+Wz.dateFormat(new Date(survey.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(survey.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
    	viewWin.find('#survey-showexam-view').html(curSurvey.survey.survey_desc);
    	viewWin.window('open');
    }
    
    function saveQuestion(){
    	var question_id = editQuestionForm.find('input[name=question_id]').val();
    	var question_name = editQuestionForm.find('input[name=question_name]').val();
    	var question_type = editQuestionForm.find('input[name=question_type]').val();
    	var is_optional = editQuestionForm.find('input[name=is_optional]').prop('checked');
    	var url = './api/survey/create_question.json';
    	var param = {
    		survey_id: curSurvey.survey.survey_id,
    		question_id: question_id,
    		question_name: question_name,
    		image_name: '',
    		is_optional: is_optional,
    		type_param: '{"input_text":{"input_prompt":""}}'
    	};
    	if(!editQuestionForm.find('input[textboxname=question_name]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写问题题目！','error');
    		return false;
    	}
    	if(question_type != '3'){
    		var editIndex = optionTable.datagrid('options').editIndex;
			var options = [];
			if(editIndex != undefined){
				if(!optionTable.datagrid('validateRow',editIndex)){
					optionTable.datagrid('deleteRow',editIndex);
				}else{
					optionTable.datagrid('endEdit',editIndex)
				}
			}
			var rows = optionTable.datagrid('getRows');
			if(rows.length < 2){
				$.messager.alert('提示','请至少提供两个选项！','info');
    			return false;
			}else{
				for(var i=0;i<rows.length;i++){
					options.push({
						option_id: rows[i].option_id || 0,
						option_name: rows[i].option_name
					});
				}
				
			}
			if(question_type == '0'){
				param.type_param =JSON.stringify({
					vote: {
						option: options,
						check_num: 1
					}
				});
			}else if(question_type == '1'){
				param.type_param =JSON.stringify({
					vote: {
						option: options,
						check_num: options.length
					}
				});
			}else if(question_type == '2'){
				param.type_param =JSON.stringify({
					input_select: {
						option: options
					}
				});
			}
    	}
    	if(question_id != ''){
    		url = './api/survey/update_question.json';
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		url: url,
    		data: param,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				surveyQuestionTable.datagrid('reload');
    				editQuestionWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    	
    }
    
    function sortQuestion(){
    	var rows = surveyQuestionTable.datagrid('getRows');
    	var question_id_list = [];
    	for(var i=0;i<rows.length;i++){
    		question_id_list.push(rows[i].question_id);
    	}
    	Wz.ajax({
    		type: 'post',
    		url: './api/survey/question_sort.json',
    		data: {
    			survey_id: curSurvey.survey.survey_id,
    			question_id_list: question_id_list.join(',')
    		},
    		success: function(json){
    			
    		}
    	});
    }
    
    function changeState(row){
    	var survey = surveyTable.datagrid('getRows')[row];
    	var msg = '请确认是否禁用该调研？';
    	var url = './api/survey/disable_survey.json';
    	if(survey.state == 'DISABLE'){
    		msg = '请确认是否启用该调研?';
    		url = './api/survey/enable_survey.json';
    	}
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: url,
    				data: {
    					survey_id_list: survey.survey_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						surveyTable.datagrid('reload');
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
    	var survey_id = importWin.find('input[name=survey_id]').val();
    	importForm.form('submit',{
    		url: './api/survey/import_question.json?survey_id='+survey_id+'&company_id='+Wz.cur_company_id,
    		onSubmit: function(){
    			Wz.showLoadingMask();
    		},
    		success: function(result){
    			Wz.hideLoadingMask();
    			try{
    				result = JSON.parse(result);
    			}catch(e){
    				$.messager.alert('错误','导入失败！','error');
    				return false;
    			}
    			if(result.result == 'SUCC'){
    				importWin.window('close');
    				surveyQuestionTable.datagrid('reload');
    			}else{
    				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
    					if(ok){
    						Wz.downloadFile("./api/survey/get_import_fail_log.json?_t=" + new Date().getTime());
    					}
    				});
    			}
    		}
    	});
    }
    
    importForm.find('.download-btn').click(function(){
    	Wz.downloadFile('./static/res/survey_template.xlsx');
    });
    
    return {
    	saveQuestion: saveQuestion,
    	delSurvey: delSurvey,
    	editSurvey: editSurvey,
    	copySurvey: copySurvey,
    	showSurvey: showSurvey,
    	viewSurvey: viewSurvey,
    	showResult: showResult,
    	changeState: changeState,
    	importQuestion: importQuestion
    };
}()

