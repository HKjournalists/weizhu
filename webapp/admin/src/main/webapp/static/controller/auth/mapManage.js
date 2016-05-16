/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 调研管理-调研管理
 */
Wz.namespace('Wz.auth');
Wz.auth.mapManage = function(){
	
	var mapTable = $('#auth-map-table').datagrid({
        /*url: './api/map/get_map_list.json',
        queryParams: query_params,*/
		data: {
			filtered_size: 5,
			map: [{
				map_name: ''
			}]
		},
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'map_name',
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
            field: 'map_id',
            title: '操作',
            align: 'center',
            width: 230,
            //hidden: !(Wz.getPermission('map/map/create')||Wz.getPermission('map/map/update')||Wz.getPermission('map/map/delete')||Wz.getPermission('map/map/set_state')||Wz.getPermission('map/map/list_result')),
            formatter: function(val,obj,row){
            	var state = (obj.state=='NORMAL'?'禁用':'启用');
                return (Wz.getPermission('auth-map/auth-map/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.auth-map.auth-mapManage.editauth-map('+row+')">编辑</a>':'') +
                	(Wz.getPermission('auth-map/auth-map/copy')?'<a href="javascript:void(0)" class="tablelink reauth-map-btn" onclick="Wz.auth-map.auth-mapManage.copyauth-map('+row+')">复制</a>':'') +
                	(Wz.getPermission('auth-map/auth-map/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.delauth-map('+val+')">删除</a>':'') +
                	(Wz.getPermission('auth-map/auth-map/set_state')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.changeState('+row+')">'+state+'</a>':'') + 
                	(Wz.getPermission('auth-map/auth-map/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.showauth-map('+val+','+row+')">预览</a>':'') + 
                	(Wz.getPermission('auth-map/auth-map/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.showResult('+val+')">查看结果</a>':'');
            }
        }]],
        toolbar: [{
            id: 'auth-map-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('auth-map/auth-map/create'),
            iconCls: 'icon-add',
            handler: function(){
				curmap.isCopy = false;
				curmap.map = {};
				curmap.questions = [];
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
                rows: data.map
            };
        }
    });
    /*$.parser.parse('#main-contain');
    var query_params = {
    	map_name: ''
    };
    var searchForm = $('#auth-map-search-form');
    var editWin = $('#auth-map-edit');
    var firstTab = $('#auth-map-firststep-tab');
    var firststepForm = $('#auth-map-firststep-form');
    var secondTab = $('#auth-map-secondstep-tab');
    var thirdTab = $('#auth-map-thirdstep-tab');
    var showview = $('#auth-map-showexam-view');
    var thirdstepForm = $('#auth-map-thirdstep-form');
    var editTabs = $('#auth-map-edit-steps').tabs({
    	onSelect: function(title,index){
    		if(index == 0){
    			$('#auth-map-edit-steps').tabs('disableTab',1).tabs('disableTab',2);
    		}else if(index == 1){
    			$('#auth-map-edit-steps').tabs('disableTab',2);
    		}
    	}
    });
    var editQuestionWin = $('#auth-map-question-edit-win');
    var editQuestionForm = $('#auth-map-question-edit-form');
    var copyWin = $('#auth-map-copy');
    var copyForm = $('#auth-map-copy-form');
    var viewWin = $('#auth-map-view');
    var resultWin = $('#auth-map-result-win');
    var importWin = $('#auth-map-question-import-win');
    var importForm = $('#auth-map-question-import-form');
    var curmap = {
    	map: {},
    	questions: [],
    	isCopy: false
    };

    var editStep = $('#auth-map-edit-steps').tabs({
    	tabPosition: 'left',
    	tabWidth: 100,
    	headerWidth: 100
    });
    searchForm.find('.search-btn').click(function(){
    	query_params.map_name = searchForm.find('input[name=map_name]').val();
    	mapTable.datagrid('reload');
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
    var mapDesc = KindEditor.create(editWin.find('textarea[name=map_desc]'), {
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
            field: 'map_name',
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
        copy_table_url: './api/map/get_map_list.json',
        id_field: 'allow_model_id',
        name_field: 'allow_model_name',
        copyParam: {
        	map_name: ''
        },
        dName: 'map',
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
    var mapTable = $('#auth-map-table').datagrid({
        url: './api/map/get_map_list.json',
        queryParams: query_params,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'map_name',
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
            field: 'map_id',
            title: '操作',
            align: 'center',
            width: 230,
            //hidden: !(Wz.getPermission('map/map/create')||Wz.getPermission('map/map/update')||Wz.getPermission('map/map/delete')||Wz.getPermission('map/map/set_state')||Wz.getPermission('map/map/list_result')),
            formatter: function(val,obj,row){
            	var state = (obj.state=='NORMAL'?'禁用':'启用');
                return (Wz.getPermission('auth-map/auth-map/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.auth-map.auth-mapManage.editauth-map('+row+')">编辑</a>':'') +
                	(Wz.getPermission('auth-map/auth-map/copy')?'<a href="javascript:void(0)" class="tablelink reauth-map-btn" onclick="Wz.auth-map.auth-mapManage.copyauth-map('+row+')">复制</a>':'') +
                	(Wz.getPermission('auth-map/auth-map/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.delauth-map('+val+')">删除</a>':'') +
                	(Wz.getPermission('auth-map/auth-map/set_state')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.changeState('+row+')">'+state+'</a>':'') + 
                	(Wz.getPermission('auth-map/auth-map/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.showauth-map('+val+','+row+')">预览</a>':'') + 
                	(Wz.getPermission('auth-map/auth-map/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.auth-map.auth-mapManage.showResult('+val+')">查看结果</a>':'');
            }
        }]],
        toolbar: [{
            id: 'auth-map-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('auth-map/auth-map/create'),
            iconCls: 'icon-add',
            handler: function(){
				curmap.isCopy = false;
				curmap.map = {};
				curmap.questions = [];
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
                rows: data.map
            };
        }
    });
    var questionParam = {
    	map_id: ''
    };
    var mapQuestionTable = $('#auth-map-question-talbe').datagrid({
    	url: './api/map/get_map_by_id.json',
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
            id: 'auth-map-questin-add',
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
            id: 'auth-map-questin-import',
            text: '导入问题',
            iconCls: 'icon-import',
            handler: function(){
            	importForm.form('reset');
            	importWin.find('input[name=map_id]').val(curmap.map.map_id);
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
    
    var optionTable = $('#auth-map-question-option-table').datagrid({
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
            id: 'auth-map-question-option-add',
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
    
    var showmapTable = $('#auth-map-showauth-map').datagrid({
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
    var showmapview = $('#auth-map-showexam-view').datagrid({
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
        	var html = ['<td class="auth-map-question-info">'];
        	var options = rowData.options||[];
        	var oHtml = [];
        	var optionTags = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'];
        	for(var i=0;i<options.length;i++){
        		oHtml.push('<div class="auth-map-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
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
        	html.push(['<h4>',(rowIndex+1),'. ',rowData.question_name,'(',type_tag,')</h4><div class="auth-map-question-optionlist">',oHtml.join(''),'</div>'].join(''));
        	html.push('</td>')
        	
            return html.join('');
        }
    });
    var resultAnswerTable = $('#auth-map-result-answer-table').datagrid({
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
        	var html = ['<td class="auth-map-question-info">'];
        	var options = rowData.options||[];
        	var oHtml = [];
        	var optionTags = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'];
        	var type_tag = '填空题';
        	var answers = [];
        	if(rowData.type == 'INPUT_SELECT'){
        		type_tag = '下拉框';
        		for(var i=0;i<options.length;i++){
            		oHtml.push('<div class="auth-map-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
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
            		oHtml.push('<div class="auth-map-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
            		if($.inArray(options[i].option_id,rowData.result)>-1){
            			answers.push(optionTags[i])
            		}
            	}
        	}else{
        		answers.push(rowData.result);
        	}
        	html.push(['<h4>',(rowIndex+1),'. ',rowData.question_name,'(',type_tag,')</h4><div class="auth-map-question-optionlist">',oHtml.join(''),'</div>',
        	           (!!rowData.user_name?'<p class="exam-question-answer">'+rowData.user_name+'回答：'+answers.join(',')+'</p>':'')].join(''));
        	html.push('</td>')
        	
            return html.join('');
        }
    });
    var resultParam = {
    	map_id: ''
    };
    var resultTable = $('#auth-map-result-byuser-table').datagrid({
    	url: './api/auth-map/get_auth-map_result_list.json',
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
            field: 'map_result',
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
            id: 'auth-map-exportresult',
            text: '导出调研结果',
            disabled: !Wz.getPermission('auth-map/auth-map/export_result'),
            iconCls: 'icon-export',
            handler: function(){
            	Wz.downloadFile('./api/auth-map/download_auth-map_result.json?auth-map_id='+resultParam.auth-map_id+'&t='+new Date().getTime());
            }
        }],
        onCheck: function(index,row){
        	var answers = JSON.parse(row.auth-map_result).answer || [];
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
                rows: data.map_result_list
            };
        }
    });
    var showquestionTable = $('#auth-map-result-byuser-table').datagrid({
    	
    });
    	
    
    
    
    firstTab.find('.next-btn').click(function(){ 
    	
    });
    secondTab.find('.prev-btn').click(function(){
    	editTabs.tabs('select',0);
    });
    secondTab.on('click','.move-up-btn',function(){
    	
    });
    secondTab.on('click','.move-down-btn',function(){
    	
    });
    secondTab.on('click','.edit-btn',function(){
    	
    	
    });
    secondTab.on('click','.del-btn',function(){
    	
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
    	var row = auth-mapQuestionTable.datagrid('getData').rows[index];
    	row.score = parseInt(this.value);
    	auth-mapQuestionTable.datagrid('updateRow',{
    		index: index,
    		row: row
    	});
    });
    secondTab.find('.next-btn').click(function(){
    	curauth-map.questions = auth-mapQuestionTable.datagrid('getData').rows;
    	thirdTab.find('.auth-map-info').prev().text(curauth-map.auth-map.auth-map_name);
    	thirdTab.find('.auth-map-info').html('<span><span>题目数：'+curauth-map.questions.length+'</span><span>调研时间：'+Wz.dateFormat(new Date(curauth-map.auth-map.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(curauth-map.auth-map.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
    	thirdTab.find('.auth-map-desc').html(curauth-map.auth-map.auth-map_desc);
    	showauth-mapTable.datagrid({
    		data: curauth-map.questions,
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
    
    function delauth-map(auth-map_id){
    	$.messager.confirm('提示','请确认是否删除调研？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/auth-map/delete_auth-map.json',
    				data: {
    					auth-map_id_list: auth-map_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						auth-mapTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    					}
    				}
    			});    			
    		}
    	});
    }
    function editauth-map(row){
    	var auth-map = auth-mapTable.datagrid('getRows')[row];
    	curauth-map.auth-map = auth-map;
    	questionParam.auth-map_id = auth-map.auth-map_id;
		auth-map.start_time = Wz.dateFormat(new Date(auth-map.start_time*1000),'yyyy-MM-dd hh:mm:ss');
		auth-map.end_time = Wz.dateFormat(new Date(auth-map.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		auth-map.show_result_type = auth-map.show_result_type || 'NONE';
		firststepForm.form('load',auth-map);
		curauth-map.isCopy = false;
		uploadImge.setValue(auth-map.image_name,auth-map.image_url);
		allowModel.setValue({model_id: auth-map.allow_model_id,model_name:auth-map.allow_model_name});
		auth-mapDesc.html(auth-map.auth-map_desc);
		editTabs.tabs('disableTab',1).tabs('disableTab',2);
    	editWin.window({title:'修改调研'}).window('open');
		editTabs.tabs('select',0);
    }
    
    function copyauth-map(row){
    	var auth-map = auth-mapTable.datagrid('getRows')[row];
    	curauth-map.auth-map = auth-map;
    	questionParam.auth-map_id = auth-map.auth-map_id;
    	auth-map.auth-map_name = auth-map.auth-map_name + '(复制)';
		auth-map.start_time = Wz.dateFormat(new Date(auth-map.start_time*1000),'yyyy-MM-dd hh:mm:ss');
		auth-map.end_time = Wz.dateFormat(new Date(auth-map.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		auth-map.show_result_type = auth-map.show_result_type || 'NONE';
		firststepForm.form('load',auth-map);
		curauth-map.isCopy = true;
		uploadImge.setValue(auth-map.image_name,auth-map.image_url);
		allowModel.setValue({model_id: auth-map.allow_model_id,model_name:auth-map.allow_model_name});
		auth-mapDesc.html(auth-map.auth-map_desc);
		editTabs.tabs('disableTab',1).tabs('disableTab',2);
    	editWin.window({title:'复制调研'}).window('open');
		editTabs.tabs('select',0);
    }
    
    function viewauth-map(auth-map_id){
    	Wz.ajax({
    		url: './api/adminauth-map/get_auth-map_by_id.json',
    		data: {
    			auth-map_id: auth-map_id,
    			_t: new Date().getTime()
    		},
    		success: function(json){
    			var auth-map = json.auth-map[0];
    			if(!!auth-map){
        			Wz.ajax({
        				url: './api/adminauth-map/get_auth-map_question.json',
        				data: {
        					auth-map_id: auth-map_id,
        					_t: new Date().getTime()
        				},
        				success: function(json){
        					var questions = json.question;
        					viewWin.find('.auth-map-info').prev().text(auth-map.auth-map_name);
        					viewWin.find('.auth-map-info').html('<span>总分：100分</span><span>通过分数：'+auth-map.pass_mark+'分</span><span>题目数：'+questions.length+'</span><span>考试时间：'+Wz.dateFormat(new Date(auth-map.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(auth-map.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
        			    	viewauth-mapTable.datagrid({
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
    function showResult(auth-map_id){
    	resultParam.auth-map_id = auth-map_id;
    	resultTable.datagrid('reload');
    	Wz.ajax({
    		url: './api/auth-map/get_auth-map_by_id.json',
    		data: resultParam,
    		success: function(json){
    			if(!!json.auth-map){
    				resultAnswerTable.datagrid({
    					data: json.questions.questions,
    					view: resultView
    				});
    			}else{
    				auth-mapTable.datagrid('reload');
    			}    			
    		}
    	});
    	resultWin.window('open');
    }
    function showauth-map(auth-map_id,row){
    	var auth-map = auth-mapTable.datagrid('getRows')[row];
    	resultParam.auth-map_id = auth-map_id;
    	Wz.ajax({
    		url: './api/auth-map/get_auth-map_by_id.json',
    		data: resultParam,
    		success: function(json){
    				showauth-mapview.datagrid({
    					data: json.questions.questions,
    					view: resultView
    				});		
    		}
    	});
    	viewWin.find('.auth-map-info').prev().text(auth-map.auth-map_name);
    	viewWin.find('.auth-map-info').html('<span><span style="margin-top:10px;text-align:center">描述：'+auth-map.auth-map_desc+'</span><span style="margin-top:10px;text-align:center">调研时间：'+Wz.dateFormat(new Date(auth-map.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(auth-map.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
    	viewWin.find('#auth-map-showexam-view').html(curauth-map.auth-map.auth-map_desc);
    	viewWin.window('open');
    }
    
    function saveQuestion(){
    	var question_id = editQuestionForm.find('input[name=question_id]').val();
    	var question_name = editQuestionForm.find('input[name=question_name]').val();
    	var question_type = editQuestionForm.find('input[name=question_type]').val();
    	var is_optional = editQuestionForm.find('input[name=is_optional]').prop('checked');
    	var url = './api/auth-map/create_question.json';
    	var param = {
    		auth-map_id: curauth-map.auth-map.auth-map_id,
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
    		url = './api/map/update_question.json';
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		url: url,
    		data: param,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				mapQuestionTable.datagrid('reload');
    				editQuestionWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    	
    }
    
    function sortQuestion(){
    	var rows = mapQuestionTable.datagrid('getRows');
    	var question_id_list = [];
    	for(var i=0;i<rows.length;i++){
    		question_id_list.push(rows[i].question_id);
    	}
    	Wz.ajax({
    		type: 'post',
    		url: './api/map/question_sort.json',
    		data: {
    			map_id: curmap.map.map_id,
    			question_id_list: question_id_list.join(',')
    		},
    		success: function(json){
    			
    		}
    	});
    }
    
    function changeState(row){
    	var map = mapTable.datagrid('getRows')[row];
    	var msg = '请确认是否禁用该调研？';
    	var url = './api/map/disable_map.json';
    	if(map.state == 'DISABLE'){
    		msg = '请确认是否启用该调研?';
    		url = './api/map/enable_map.json';
    	}
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: url,
    				data: {
    					map_id_list: map.map_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						mapTable.datagrid('reload');
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
    	var map_id = importWin.find('input[name=map_id]').val();
    	importForm.form('submit',{
    		url: './api/map/import_question.json?map_id='+map_id+'&company_id='+Wz.cur_company_id,
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
    				mapQuestionTable.datagrid('reload');
    			}else{
    				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
    					if(ok){
    						Wz.downloadFile("./api/map/get_import_fail_log.json?_t=" + new Date().getTime());
    					}
    				});
    			}
    		}
    	});
    }
    
    importForm.find('.download-btn').click(function(){
    	Wz.downloadFile('./static/res/map_template.xlsx');
    });
    
    return {
    	saveQuestion: saveQuestion,
    	delMap: delMap,
    	editMap: editMap,
    	copyMap: copyMap,
    	showMap: showMap,
    	viewMap: viewMap,
    	showResult: showResult,
    	changeState: changeState,
    	importQuestion: importQuestion
    };*/
}()

