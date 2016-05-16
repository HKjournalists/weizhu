/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 考试管理-试卷管理
 */
Wz.namespace('Wz.exam');
Wz.exam.examinationManage = function(){
    $.parser.parse('#main-contain');
    var query_params = {
    	state: '',
    	condition: ''
    };
    var resultParam = {
    	exam_id: ''
    };
    var searchForm = $('#exam-examination-search-form');
    var editWin = $('#exam-examination-edit');
    var firstTab = $('#exam-examination-firststep-tab');
    var firststepForm = $('#exam-examination-firststep-form');
    var secondTab = $('#exam-examination-secondstep-tab');
    var thirdTab = $('#exam-examination-thirdstep-tab');
    var thirdstepForm = $('#exam-examination-thirdstep-form');
    var manualQuestionPanel = $('#exam-examination-manual-panel');
    var autoQuestionPanel = $('#exam-examination-auto-panel');
    var manualView = $('#exam-examination-manual-view');
    var autoView = $('#exam-examination-auto-view');
    var userResultWin = $('#exam-examination-result-user-view');
    var teamResultTab = $('#exam-examination-result-team');
    var resultTabs = $('#exam-examination-result-tabs').tabs({
    	fit: true,
    	onSelect: function(title,index){
    		if(index==0){
    			loadStatistics();
    		}else if(index == 3){
    			resultQuestionTable.datagrid('reload');
    		}else if(index == 2){
    			resultPositionTable.datagrid('reload');
    		}else if(index == 1){
    			resultTeamTable.datagrid('reload');
    		}else if(index == 4){
    			resultTable.datagrid('reload');
    		}
    	}
    });
    var editTabs = $('#exam-examination-edit-steps').tabs({
    	onSelect: function(title,index){
    		if(index == 0){
    			$('#exam-examination-edit-steps').tabs('disableTab',1).tabs('disableTab',2);
    		}else if(index == 1){
    			$('#exam-examination-edit-steps').tabs('disableTab',2);
    		}
    	}
    });
    var selectQuestionWin = $('#exam-examination-selectquestion');
    var autoQuestionWin = $('#exam-examination-autoquestion');
    var copyWin = $('#exam-examination-copy');
    var copyForm = $('#exam-examination-copy-form');
    var viewWin = $('#exam-examination-view');
    var resultWin = $('#exam-examination-result-win');
    var curExam = {
    	exam: {},
    	questions: [],
    	isCopy: false
    };

    var editStep = $('#exam-examination-edit-steps').tabs({
    	tabPosition: 'left',
    	tabWidth: 100,
    	headerWidth: 100
    });
    var searchSate = searchForm.find('input[name=state]').combotree({
    	data:[{id: '0',text:'未开始'},{id: '1',text:'考试中'},{id: '2',text:'已结束'}],
    	panelWidth: 200,
    	panelHeight: 'auto',
    	editable: false
    });
    searchForm.find('.search-btn').click(function(){
    	query_params.state = searchSate.combotree('getValue');
    	query_params.condition = searchForm.find('input[name=condition]').val();
    	examTable.datagrid('reload');
    });
    searchForm.find('.reset-btn').click(function(){
    	searchForm.form('reset');
    });
    
    var uploadImge = firststepForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '建议图片尺寸120x120(px)<br>支持jpg、png格式，大小1M以内',
    	maxSize: 1,
    	params: {
    		image_tag: '考试,图标'
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
    var restartTime = copyForm.find('input[name=start_time]').datetimebox({
    	editable: false,
        required: true
    });
    var reendTime = copyForm.find('input[name=end_time]').datetimebox({
    	editable: false,
        required: true
    });
    var allowModel = Wz.comm.allowService(firststepForm.find('input[name=allow_model_id]'),{
    	copy_enable: true,
    	copy_win_title: '选择已有考试对象',
    	copy_table_columns: [[{
    		field: 'ck',
            checkbox: true
    	},{
            field: 'exam_name',
            title: '考试名称',
            width: '200px'
        },{
            field: 'start_time',
            title: '考试时间',
            align: 'center',
            formatter: function(val,obj,row){
            	return Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss') + '至' + Wz.dateFormat(new Date(obj.end_time*1000),'yyyy-MM-dd hh:mm:ss');
            }
        }]],
        copy_table_url: './api/adminExam/get_exam_list.json',
        id_field: 'allow_model_id',
        name_field: 'allow_model_name',
        copyParam: {
        	condition: ''
        }
    });
    var questionNum = secondTab.find('input[textboxname=question_num]').combobox({
    	onChange: function(newValue,oldValue){
    		freshQuestionNum();
    	}
    });
    
    var teamResultSearch = teamResultTab.find('input[name=team_id]').combotree({
		panelWidth: 200,
    	url: './api/user/get_team.json',
    	editable: false,
    	cascadeCheck: false,
    	onBeforeExpand: function(row){
    		teamResultSearch.combotree('options').queryParams.team_id = row.team_id;
    		return true;
    	},
    	onShowPanel: function(){
    		teamResultSearch.combotree('options').queryParams.team_id = '';
    		teamResultSearch.combotree('reload');
    	},
    	loadFilter: function(data){
    		var team = [];
    		if(!!data){
    			for(var i=0;i<data.length;i++){
    				data[i].id = data[i].team_id;
    				data[i].text = data[i].team_name;
    				data[i].state = (data[i].has_sub_team?'closed':'open');
    				if(data[i].has_sub_team){
    					team.push(data[i]);
    				}
    			}
    			if(teamResultSearch.combotree('options').queryParams.team_id == ''){
    				team.unshift({
    					id: '',
    					text: '---全部部门---'
    				});
    			}
    		}
    		return team;
    	}
    });
    
    var examTable = $('#exam-examination-table').datagrid({
        url: './api/adminExam/get_exam_list.json',
        queryParams: query_params,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'exam_name',
            title: '考试名称',
            width: '20%'
        },{
            field: 'start_time',
            title: '考试时间',
            align: 'center',
            width: '26%',
            formatter: function(val,obj,row){
            	return Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss') + '至' + Wz.dateFormat(new Date(obj.end_time*1000),'yyyy-MM-dd hh:mm:ss');
            }
        },{
            field: 'create_exam_name',
            title: '创建人',
            width: '10%',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val:'';
            }
        },{
            field: 'create_time',
            title: '创建时间',
            width: '14%',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'state',
            title: '状态',
            width: '5%',
            align: 'center',
            formatter: function(val,obj,row){
            	return val=='0'?'未开始':(val=='1'?'考试中':'已结束');
            }
        },{
            field: 'exam_id',
            title: '操作',
            align: 'center',
            width: '20%',
            formatter: function(val,obj,row){
                return (Wz.getPermission('exam/exam/update')?(obj.state !='2'?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.exam.examinationManage.editExam('+val+')">编辑</a>':''):'') +
                	(Wz.getPermission('exam/exam/makeup')?(obj.state == '2'?'<a href="javascript:void(0)" class="tablelink reexam-btn" onclick="Wz.exam.examinationManage.copyExam('+val+')">补考</a>':''):'') +
                	(Wz.getPermission('exam/exam/delete')?(obj.state != '1'?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.exam.examinationManage.delExam('+val+')">删除</a>':''):'') +
                	(obj.type=='MANUAL'?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.exam.examinationManage.viewExam('+val+')">试卷预览</a>':'') + 
                	(Wz.getPermission('exam/exam/list_result')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.exam.examinationManage.showResult('+row+')">查看结果</a>':'');
            }
        }]],
        toolbar: [{
            id: 'exam-examination-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('exam/exam/create'),
            iconCls: 'icon-add',
            handler: function(){
				curExam.isCopy = false;
				curExam.exam = {};
				curExam.questions = [];
				firststepForm.form('reset');
				firststepForm.find('input[name=exam_id]').val('');
				uploadImge.reset();
				allowModel.setValue({model_id:'',model_name:''});
            	editWin.window({title:'新建考试'}).window('open');
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
                total: data.filterd_size,
                rows: data.exam
            };
        }
    });
    
    var examQuestionTable = $('#exam-examination-examquestion').datagrid({
    	data: [],
    	fitColumns: true,
        striped: true,
        fit: true,
        rownumbers: true,
        columns: [[{
            field: 'question_name',
            title: '题目',
            width: 400
        },{
            field: 'score',
            title: '分数',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
            	return '<input style="width:100%;height:100%;" type="text" name="score" value="'+val+'"/>';
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
            	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
            }
        }]],
        toolbar: [{
            id: 'exam-examination-questin-add',
            text: '手动选考题',
            iconCls: 'icon-add',
            handler: function(){
            	selectQuestionWin.window('open');
            }
        },{
            id: 'exam-examination-questin-auto',
            text: '自动生成考题',
            iconCls: 'icon-add',
            handler: function(){
            	categoryTable.treegrid('reload');
            	autoQuestionWin.window('open');
            }
        }],
        editIndex: null
    });
    
    var autoCategoryTable = $('#exam-examination-auto-category-table').treegrid({
    	url: './api/adminExam/get_question_category.json',
    	treeField: 'category_name',
    	idField: 'category_id',
        rownumbers: true,
    	fitColumns: true,
    	cascadeCheck: true,
    	singleSelect: false,
        fit: true,
        checkbox: true,
    	columns: [[{
    		field: 'category_name',
    		width: 300,
    		title: '题库名称'
    	},{
    		field: 'question_count',
    		width: 100,
    		align: 'right',
    		title: '题目数'
    	}]],
    	onCheckNode: function(){
    		freshQuestionNum();
    	},
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
    
    var questionParam = {
    	category_id: ''
    };
    var categoryQuery = selectQuestionWin.find('input[name=category_id]').combotree({
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
			questionParam.category_id = newValue;
			questionTable.datagrid('reload');
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });
    var questionTable = $('#exam-examination-question-table').datagrid({
        url: './api/adminExam/get_question_by_category.json',
        queryParams: questionParam,
        title: '备选考题',
        fitColumns: true,
        checkOnSelect: true,
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
            width: 400,
            align: 'left'
        },{
            field: 'type',
            title: '考题类型',
            align: 'left',
            formatter: function(val,obj,row){
            	return val=='OPTION_MULTI'?'多选题':'单选题';
            }
        }]],
        toolbar: '#exam-examination-question-tb',
        onLoadSuccess: function(data){
        	data = data.rows;
        	var sd = questionSelectedTable.datagrid('getData').rows;
        	for(var i=0;i<data.length;i++){
        		for(var j=0;j<sd.length;j++){
        			if(sd[j].question_id==data[i].question_id){
        				questionTable.datagrid('checkRow',i);
        				break;
        			}
        		}
        	}
        },
        onCheck: function(index,row){
        	var data = questionSelectedTable.datagrid('getData').rows;
        	var data = $.grep(data,function(o){
        		return o.question_id == row.question_id;
        	});
        	if(data.length == 0){
        		questionSelectedTable.datagrid('insertRow',{
        			index: 0,
        			row: row
        		});
        	}
        },
        onUncheck: function(index,row){
        	var data = questionSelectedTable.datagrid('getData').rows;
        	$.grep(data,function(o,i){
        		if(!!!o)return false;
        		if(row.question_id == o.question_id){
        			questionSelectedTable.datagrid('deleteRow',i);
        		}
        		return true;
        	});
        },
        onUncheckAll: function(rows){
        	var oldData = questionSelectedTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		for(var i=oldData.length;i>0;i--){
        			if(oldData[i-1].question_id == o.question_id){
        				questionSelectedTable.datagrid('deleteRow',i-1);
        				break;
        			}
        		}
        	});
        },
        onCheckAll: function(rows){
        	var oldData = questionSelectedTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		var same = $.grep(oldData,function(od){
        			return od.question_id == o.question_id;
        		});                    		
        		if(same.length == 0){
        			questionSelectedTable.datagrid('insertRow',{
            			index: 0,
            			row: o
            		});
        		}
        	});
        },
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
            return {
                total: data.total||0,
                rows: data.question||[]
            };
        }
    });
    var questionSelectedTable = $('#exam-examination-selectedquestion-table').datagrid({
        data: [],
        fitColumns: true,
        title: '已选考题',
        striped: true,
        fit: true,
        rownumbers: true,
        columns: [[{
            field: 'question_name',
            title: '考题内容',
            width: 400,
            align: 'left'
        },{
            field: 'type',
            title: '考题类型',
            align: 'left',
            formatter: function(val,obj,row){
            	return val=='OPTION_MULTI'?'多选题':'单选题';
            }
        },{
            field: 'question_id',
            title: '操作',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
            }
        }]]
    });
    var categoryTable = $('#exam-examination-category-table').treegrid({
    	url: './api/adminExam/get_question_category.json',
    	treeField: 'category_name',
    	idField: 'category_id',
        rownumbers: true,
        checkOnSelect: true,
        singleSelect: false,
    	fitColumns: true,
    	cascadeCheck: true,
        fit: true,
    	columns: [[{
            field: 'ck',
            checkbox: true
        },{
    		field: 'category_name',
    		width: 300,
    		title: '题库名称'
    	},{
    		field: 'question_count',
    		width: 100,
    		align: 'right',
    		title: '题目数'
    	}]],
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
    var showExamTable = $('#exam-examination-showexam').datagrid({
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
    var viewExamTable = $('#exam-examination-showexam-view').datagrid({
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
    var resultTable = $('#exam-examination-result-table').datagrid({
    	url: './api/adminExam/get_exam_user_result.json',
        queryParams: resultParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name',
            title: '姓名',
            width: '10%'
        },{
            field: 'user_team',
            title: '部门',
            width: '20%',
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
            width: '10%',
            formatter: function(val,obj,row){
            	return !!val?val:'';
            }
        },{
            field: 'start_time',
            title: '考试时间',
            width: '16%',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'submit_time',
            title: '交卷时间',
            width: '16%',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'score',
            title: '成绩',
            width: '10%',
            align: 'center'
        },{
            field: 'user_id',
            title: '试卷查阅',
            width: 60,
            align: 'center',
            formatter: function(val,obj,row){
            	return (!!obj.start_time?'<a href="javascript:void(0)" title="查阅" onclick="Wz.exam.examinationManage.showUserResultDetail('+row+')" class="table-cell-icon icon-eye view-btn">&nbsp;</a>':'');
            }
        }]],
        onBeforeLoad: function(param){
        	if(param.exam_id == ''){
        		return false;
        	}
        },
        toolbar: [{
            id: 'exam-examination-exportresult',
            text: '导出考试结果',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('exam/exam/export_result'),
            handler: function(){
            	Wz.downloadFile('./api/adminExam/download_exam_result.json?exam_id='+resultParam.exam_id+'&start=0&length=50000'+'&t='+new Date().getTime());
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
                rows: data.user_result
            };
        }
    });
    
    var resultQuestionTable = $('#exam-examination-result-question-table').datagrid({
    	url: './api/adminExam/exam_question_statistics.json',
        queryParams: resultParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'question',
            title: '考题名称',
            width: '70%',
            formatter: function(val,obj,row){
            	return val.question_name||'';
            }
        },{
            field: 'answer_num',
            title: '正确率',
            width: '20%',
            align: 'right',
            formatter: function(val,obj,row){
            	if(obj.answer_num == 0) return 0;
            	
            	return Math.round(10000*obj.correct_num/obj.answer_num)/100 + '%';
            }
        }]],
        onBeforeLoad: function(param){
        	if(param.exam_id == ''){
        		return false;
        	}
        },
        toolbar: [{
            id: 'exam-examination-export-question-result',
            text: '导出统计结果',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('exam/exam/export_result'),
            handler: function(){
            	Wz.downloadFile('./api/adminExam/download_exam_question_statistics.json?exam_id='+resultParam.exam_id+'&start=0&length=50000'+'&t='+new Date().getTime());
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
                rows: data.question_correct||[]
            };
        }
    });
    
    var resultTeamTable = $('#exam-examination-result-team-table').datagrid({
    	url: './api/adminExam/exam_team_statistics.json',
        queryParams: resultParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'team_name',
            title: '部门',
            width: '25%',
            formatter: function(val,obj,row){
            	return !!val?val:'其他';
            }
        },{
            field: 'total_num',
            title: '应考人数',
            width: '10%',
            align: 'right'
        },{
            field: 'take_num',
            title: '参考人数',
            width: '10%',
            align: 'right'
        },{
            field: 'take_rate',
            title: '参考率',
            width: '10%',
            align: 'right'
        },{
            field: 'pass_num',
            title: '通过人数',
            width: '10%',
            align: 'right'
        },{
            field: 'pass_rate',
            title: '通过率',
            width: '10%',
            align: 'right'
        },{
            field: 'average',
            title: '平均分',
            width: '10%',
            align: 'right'
        }]],
        onBeforeLoad: function(param){
        	if(param.exam_id == ''){
        		return false;
        	}
        },
        toolbar: '#exam-examination-result-team-tb',
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
            return {
                total: data.filtered_size,
                rows: data.team_statistics||[]
            };
        }
    });
    
    var resultPositionTable = $('#exam-examination-result-position-table').datagrid({
    	url: './api/adminExam/exam_position_statistics.json',
        queryParams: resultParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'position_name',
            title: '职务名称',
            width: '25%'
        },{
            field: 'total_num',
            title: '应考人数',
            width: '10%',
            align: 'right'
        },{
            field: 'take_num',
            title: '参考人数',
            width: '10%',
            align: 'right'
        },{
            field: 'take_rate',
            title: '参考率',
            width: '10%',
            align: 'right'
        },{
            field: 'pass_num',
            title: '通过人数',
            width: '10%',
            align: 'right'
        },{
            field: 'pass_rate',
            title: '通过率',
            width: '10%',
            align: 'right'
        },{
            field: 'average',
            title: '平均分',
            width: '10%',
            align: 'right'
        }]],
        onBeforeLoad: function(param){
        	if(param.exam_id == ''){
        		return false;
        	}
        },
        toolbar: [{
            id: 'exam-examination-export-question-result',
            text: '导出统计结果',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('exam/exam/export_result'),
            handler: function(){
            	Wz.downloadFile('./api/adminExam/download_exam_position_statistics.json?exam_id='+resultParam.exam_id+'&start=0&length=50000'+'&t='+new Date().getTime());
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
                rows: data.position_statistics||[]
            };
        }
    });
    var userResultTable = $('#exam-examination-result-user-table').datagrid({
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
    var userResultView = $.extend({}, $.fn.datagrid.defaults.view, {
        renderRow: function(target, fields, frozen, rowIndex, rowData){
        	if(!!!rowData.question_id) return '';
        	var html = ['<td class="exam-question-info">'];
        	var options = rowData.option;
        	var oHtml = [];
        	var optionTags = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'];
        	var rightTags = [];
        	var answerTags = [];
        	for(var i=0;i<options.length;i++){
        		var style = '';
        		if($.inArray(options[i].option_id,rowData.answer.answer_option_id)>-1){
        			style = 'color: #53C553;color: #53C553;';
        		}
        		oHtml.push('<div class="exam-question-option" style="'+style+'">'+optionTags[i]+': '+options[i].option_name+'</div>');
        		if(options[i].is_right){
        			rightTags.push(optionTags[i]);
        		}
        	}
        	html.push(['<h4 ',(!!!rowData.answer.is_right?'style="color:red"':''),'>',(rowIndex+1),'. ',rowData.question_name,'(',rowData.score,'分)</h4><div class="exam-question-optionlist">',oHtml.join(''),'</div>',
        	           '<p class="exam-question-answer">正确答案：',rightTags.join(','),'</p>'].join(''));
        	html.push('</td>')
        	
            return html.join('');
        }
    });
    
    var questionView = $.extend({}, $.fn.datagrid.defaults.view, {
        renderRow: function(target, fields, frozen, rowIndex, rowData){
        	if(!!!rowData.question_id) return '';
        	var html = ['<td class="exam-question-info">'];
        	var options = rowData.option;
        	var oHtml = [];
        	var optionTags = ['A','B','C','D','E','F','G','H','I','J','K','L','M','N','O'];
        	var rightTags = [];
        	for(var i=0;i<options.length;i++){
        		oHtml.push('<div class="exam-question-option">'+optionTags[i]+': '+options[i].option_name+'</div>');
        		if(options[i].is_right){
        			rightTags.push(optionTags[i]);
        		}
        	}
        	html.push(['<h4>',(rowIndex+1),'. ',rowData.question_name,'(',rowData.score,'分)</h4><div class="exam-question-optionlist">',oHtml.join(''),'</div>',
        	           '<p class="exam-question-answer">正确答案：',rightTags.join(','),'</p>'].join(''));
        	html.push('</td>')
        	
            return html.join('');
        }
    });
    
    autoQuestionWin.find('.edit-save').click(function(){
    	var score = autoQuestionWin.find('input[name=score]').val();
    	var categorys = categoryTable.treegrid('getChecked');
    	var category_id = [];
    	for(var i=0;i<categorys.length;i++){
    		category_id.push(categorys[i].category_id);
    	}
    	if(category_id.length=='0'){
    		$.messager.alert('错误','请先选择题库再执行！','error');
    		return false;
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		url: './api/adminExam/create_exam_question_random.json',
    		data: {
    			score: score,
    			question_category_id_str: category_id.join(','),
    			_t: new Date().getTime()
    		},
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				var questions = json.question;
    				for(var i=questions.length;i>0;i--){
    		    		questions[i-1].score = score;
    		    	}
    		    	examQuestionTable.datagrid('loadData',questions);
    		    	freshTotalScore();
    		    	autoQuestionWin.window('close');
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    			}
    		}
    	});
    });
    
    selectQuestionWin.on('click','.del-btn',function(){
    	var question = questionSelectedTable.datagrid('getData').rows[$(this).parents('tr').index()];
		var rows = questionTable.datagrid('getData').rows;
		var index = 0;
		for(;index<rows.length;index++){
			if(rows[index].question_id == question.question_id){
				break;
			}
		}
		questionTable.datagrid('uncheckRow',index);
    });
    selectQuestionWin.find('.edit-save').click(function(){
    	var score = selectQuestionWin.find('input[name=score]').val();
    	var questions = questionSelectedTable.datagrid('getData').rows;
    	if(questions.length == 0){
    		$.messager.alert('错误','请选择要添加到试卷中的考题！','error');
    		return false;
    	}
    	for(var i=questions.length;i>0;i--){
    		questions[i-1].score = score;
    	}
    	examQuestionTable.datagrid('loadData',questions);
    	freshTotalScore();
    	selectQuestionWin.window('close');
    });
    firstTab.find('.next-btn').click(function(){ 
    	if(!firststepForm.form('validate')){
    		$.messager.alert('错误','请正确填写表单内容！','error');
    		return false;
    	}
    	curExam.exam.allow_model_id = allowModel.getValue();
    	if(curExam.exam.allow_model_id == ''){
    		$.messager.alert('错误','请为考试选择考试对象！','error');
    		return false;
    	}
    	curExam.exam.image_name = uploadImge.getValue();
    	curExam.exam.exam_id = firststepForm.find('input[name=exam_id]').val();
    	curExam.exam.exam_name = firststepForm.find('input[name=exam_name]').val();
    	curExam.exam.pass_mark = firststepForm.find('input[name=pass_mark]').val();
    	curExam.exam.show_result = firststepForm.find('input[name=show_result]').val();
    	curExam.exam.type = firststepForm.find('input[textboxname=type]').combobox('getValue');
    	curExam.exam.start_time = startTime.datetimebox('getValue');
    	curExam.exam.end_time = endTime.datetimebox('getValue');
    	
    	if(curExam.exam.start_time > curExam.exam.end_time){
    		$.messager.alert('错误','考试开始时间不能大于考试结束时间！','error');
    		return false;
    	}
    	
    	if(curExam.exam.type == 'MANUAL'){
    		autoQuestionPanel.hide()
    		manualQuestionPanel.show()
        	examQuestionTable.datagrid('loadData',curExam.questions||[]);
        	freshTotalScore();
    	}else{
    		curExam.category_id_list = curExam.category_id_list||[]; 
			questionNum.combobox('setValue',curExam.exam.question_num);
    		for(var i=0;i<curExam.category_id_list.length;i++){
    			autoCategoryTable.treegrid('checkNode',curExam.category_id_list[i]);
    		}
    		freshQuestionNum();
    		manualQuestionPanel.hide()
    		autoQuestionPanel.show()
    	}
    	editTabs.tabs('enableTab',1).tabs('select',1);
    });
    secondTab.find('.prev-btn').click(function(){
    	editTabs.tabs('select',0);
    });
    secondTab.on('click','.move-up-btn',function(){
    	var index = $(this).parents('tr').index();
    	if(index > 0){
    		var row = examQuestionTable.datagrid('getData').rows[index];
    		examQuestionTable.datagrid('deleteRow',index);
    		examQuestionTable.datagrid('insertRow',{
    			index: index-1,
    			row: row
    		});
    	}
    });
    secondTab.on('click','.move-down-btn',function(){
    	var index = $(this).parents('tr').index();
    	var rows = examQuestionTable.datagrid('getData').rows
    	if(index < rows.length-1){
    		var row = rows[index];
    		examQuestionTable.datagrid('deleteRow',index);
    		examQuestionTable.datagrid('insertRow',{
    			index: index+1,
    			row: row
    		});
    	}
    });
    secondTab.on('click','.del-btn',function(){
    	var index = $(this).parents('tr').index();
    	examQuestionTable.datagrid('deleteRow',index);
    	freshTotalScore();
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
    	var row = examQuestionTable.datagrid('getData').rows[index];
    	row.score = parseInt(this.value);
    	examQuestionTable.datagrid('updateRow',{
    		index: index,
    		row: row
    	});
    });
    secondTab.find('.next-btn').click(function(){
    	if(curExam.exam.type == 'MANUAL'){
    		var totalScore = freshTotalScore();
    		if(totalScore != 100){
    			$.messager.alert('错误','请修改分值设置，保证所选考题总分数为100分！','error');
    			return false;
    		}
    		curExam.questions = examQuestionTable.datagrid('getData').rows;
    		thirdTab.find('.exam-examination-info').prev().text(curExam.exam.exam_name);
    		thirdTab.find('.exam-examination-info').html('<span>出题方式：'+(curExam.exam.type=='MANUAL'?'指定考题':'随机抽取考题')+'</span><span>总分：100分</span><span>通过分数：'+curExam.exam.pass_mark+'分</span><span>题目数：'+curExam.questions.length+'</span><span>考试时间：'+curExam.exam.start_time+' 至 '+curExam.exam.end_time+'</span></div>');
    		showExamTable.datagrid({
    			data: curExam.questions,
    			view: questionView
    		});    
    		autoView.hide();
    		manualView.show();
    	}else{
    		var validate = freshQuestionNum();
    		if(!validate){
    			$.messager.alert('错误','请保证所选题库中有足够的题给试卷随机抽取！','error');
    			return false;
    		}else{
    			thirdTab.find('.exam-examination-info2').prev().text(curExam.exam.exam_name);
        		thirdTab.find('.exam-examination-info2').html('<span>出题方式：'+(curExam.exam.type=='MANUAL'?'指定考题':'随机抽取考题')+'</span><span>总分：100分</span><span>通过分数：'+curExam.exam.pass_mark+'分</span><span>题目数：'+curExam.exam.question_num+'</span><span>考试时间：'+curExam.exam.start_time+' 至 '+curExam.exam.end_time+'</span></div>');
    		}
    		manualView.hide();
    		autoView.show();
    	}
    	editTabs.tabs('enableTab',2).tabs('select',2);
    });
    thirdTab.find('.next-btn').click(function(){
    	var url = './api/adminExam/create_exam.json';
    	if(curExam.exam.exam_id != ''){
    		url = './api/adminExam/update_exam.json';
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: curExam.exam,
    		success: function(json){
    			if(json.result == 'SUCC'){
    				curExam.exam.exam_id = json.exam_id || curExam.exam.exam_id;
    				var param = {
						exam_id: curExam.exam.exam_id
    				};
    				if(curExam.exam.type == 'MANUAL'){
    					param.exam_question = JSON.stringify({exam_question:curExam.questions});
    				}else{
    					param.question_num = curExam.exam.question_num;
    					param.category_id_list = curExam.category_id_list.join(',');
    					param.exam_question = '{"question_info": []}';
    				}
    				
    				Wz.ajax({
    					type: 'post',
    					url: './api/adminExam/update_question_exam.json',
    					data: param,
    					success: function(json){
    		    			Wz.hideLoadingMask();
    						if(json.result == 'SUCC'){
    							examTable.datagrid('reload');
    							editWin.window('close');
    						}else{
    							$.messager.alert('错误',json.fail_text,'error');
    						}
    					}
    				});
    			}else{
        			Wz.hideLoadingMask();
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	})
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
    
    function freshQuestionNum(){
    	var cur_question_num = 0;
    	curExam.exam.question_num = parseInt(autoQuestionPanel.find('input[textboxname=question_num]').combobox('getValue')||'0');
    	var selections = autoCategoryTable.treegrid('getCheckedNodes');
    	
    	var subid = {};
    	curExam.category_id_list = [];
    	for(var i=0;i<selections.length;i++){
    		if(!subid[selections[i]._parentId]){
    			cur_question_num += selections[i].question_count;
    		}
    		subid[selections[i].category_id] = true;
    		curExam.category_id_list.push(selections[i].category_id);
    	}
    	
    	$('.exam-examination-auto-questionnum em').text(curExam.exam.question_num + '/' + cur_question_num);
    	return cur_question_num>=curExam.exam.question_num;
    }
    
    function delExam(exam_id){
    	$.messager.confirm('提示','请确认是否删除考试？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/adminExam/delete_exam.json',
    				data: {
    					exam_id: exam_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						examTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    					}
    				}
    			});    			
    		}
    	});
    }
    function editExam(exam_id){
    	Wz.ajax({
    		url: './api/adminExam/get_exam_by_id.json',
    		data: {
    			exam_id: exam_id,
    			_t: new Date().getTime()
    		},
    		success: function(json){
    			var exam = json.exam[0];
    			if(!!exam){
    				exam.start_time = Wz.dateFormat(new Date(exam.start_time*1000),'yyyy-MM-dd hh:mm:ss');
    				exam.end_time = Wz.dateFormat(new Date(exam.end_time*1000),'yyyy-MM-dd hh:mm:ss');
    				exam.show_result = exam.show_result || 'NONE';
    				firststepForm.form('load',exam);
    				curExam.isCopy = false;
    				uploadImge.setValue(exam.image_name,exam.image_url);
    				allowModel.setValue({model_id: exam.allow_model_id,model_name:exam.allow_model_name});
    				editTabs.tabs('disableTab',1).tabs('disableTab',2);
                	editWin.window('open');
    				editTabs.tabs('select',0);
                	

        			Wz.ajax({
        				url: './api/adminExam/get_exam_question.json',
        				data: {
        					exam_id: exam_id,
        					_t: new Date().getTime()
        				},
        				success: function(json){
        					if(exam.type == 'MANUAL'){
        						curExam.questions = json.question;        						
        					}else{
        						curExam.exam.question_num = json.question_num;
        						curExam.category_id_list = [];
        						for(var i=0;i<json.question_category.length;i++){
        							curExam.category_id_list.push(json.question_category[i].category_id);
        						}
        					}
        				}
        			});
    			}
    		}
    	});
    }
    function copyExam(exam_id){
    	Wz.ajax({
    		url: './api/adminExam/get_exam_by_id.json',
    		data: {
    			exam_id: exam_id,
    			_t: new Date().getTime()
    		},
    		success: function(json){
    			var exam = json.exam[0];
    			if(!!exam){
    				exam.exam_name = exam.exam_name + '【补考】';
    				exam.start_time = Wz.dateFormat(new Date(),'yyyy-MM-dd hh:mm:ss');
    				exam.end_time = Wz.dateFormat(new Date(new Date().getTime()+3600*24*1000),'yyyy-MM-dd hh:mm:ss');
    				exam.show_result = exam.show_result || 'NONE';
    				curExam.isCopy = true;
    				copyForm.form('load',exam);
    				copyWin.window('open');
    			}else{
    				examTable.datagrid('reload');
    			}
    		}
    	});
    }
    function saveCopy(){
    	copyForm.form('submit',{
    		url: './api/adminExam/re_exam.json',
    		onSubmit: function(){
    			var start_time = copyForm.find('input[name=start_time]').val();
    			var end_time = copyForm.find('input[name=end_time]').val();
    			if(start_time > end_time){
    	    		$.messager.alert('错误','考试开始时间不能大于考试结束时间！','error');
    	    		return false;
    	    	}    			
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				examTable.datagrid('reload');
    				copyWin.window('close');
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    			}
    		}
    	});
    }
    function viewExam(exam_id){
    	Wz.ajax({
    		url: './api/adminExam/get_exam_by_id.json',
    		data: {
    			exam_id: exam_id,
    			_t: new Date().getTime()
    		},
    		success: function(json){
    			var exam = json.exam[0];
    			if(!!exam){
        			Wz.ajax({
        				url: './api/adminExam/get_exam_question.json',
        				data: {
        					exam_id: exam_id,
        					_t: new Date().getTime()
        				},
        				success: function(json){
        					var questions = json.question||[];
        					viewWin.find('.exam-examination-info').prev().text(exam.exam_name);
        					viewWin.find('.exam-examination-info').html('<span>总分：100分</span><span>通过分数：'+exam.pass_mark+'分</span><span>题目数：'+questions.length+'</span><span>考试时间：'+Wz.dateFormat(new Date(exam.start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(exam.end_time*1000),'yyyy-MM-dd hh:mm:ss')+'</span></div>');
        			    	viewExamTable.datagrid({
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
    function showResult(row){
    	var exam = examTable.datagrid('getRows')[row];
    	resultParam.exam_id = exam.exam_id;
    	resultWin.window('open');
    	if(exam.is_load_all_user){
        	loadStatistics();
    		if(exam.type == 'AUTO'){
    			resultTabs.tabs('enableTab',0);
    			resultTabs.tabs('enableTab',1);
    			resultTabs.tabs('enableTab',2);
    			resultTabs.tabs('disableTab',3);
    		}else{
    			resultTabs.tabs('enableTab',0);
    			resultTabs.tabs('enableTab',1);
    			resultTabs.tabs('enableTab',2);
    			resultTabs.tabs('enableTab',3);
    		}
        	resultTabs.tabs('select',0); 		
    	}else{
        	resultTable.datagrid('reload');
    		resultTabs.tabs('disableTab',0);
    		resultTabs.tabs('disableTab',1);
    		resultTabs.tabs('disableTab',2);
    		resultTabs.tabs('disableTab',3);
        	resultTabs.tabs('select',4);
    	}
    }
    
    function loadStatistics(){
    	if(resultParam.exam_id == '')return false;
    	Wz.ajax({
    		url: './api/adminExam/exam_statistics.json',
    		data: {
    			exam_id_list: resultParam.exam_id
    		},
    		success: function(json){
    			var exam = json.exam_statistics[0]||{};
    			var html = ['<div class="exam-examination-statistics-item"><strong>',exam.total_num,'</strong><p>应考人数</p></div>',
    			            '<div class="exam-examination-statistics-item"><strong>',exam.take_num,'</strong><p>参考人数</p></div>',
    			            '<div class="exam-examination-statistics-item"><strong>',exam.take_rate,'</strong><p>参考率</p></div>',
    			            '<div class="exam-examination-statistics-item"><strong>',exam.pass_num,'</strong><p>通过人数</p></div>',
    			            '<div class="exam-examination-statistics-item"><strong>',exam.pass_rate,'</strong><p>通过率</p></div>',
    			            '<div class="exam-examination-statistics-item"><strong>',exam.average,'</strong><p>平均分</p></div>'].join('');
    			resultWin.find('.exam-examination-statistics-wrap').html(html);
    		}
    	});
    }
    
    function showUserResultDetail(row){
    	var user = $.extend({},resultTable.datagrid('getRows')[row]);
    	Wz.ajax({
    		url: './api/adminExam/get_user_answer.json',
    		data: {
    			exam_id: resultParam.exam_id,
    			user_id: user.user_id
    		},
    		success: function(json){
    			var exam = json.exam;
    			var questions = json.question||[];
    			var answers = json.user_answer||[];
    			var answersMap = {};
    			var resultInfo = json.user_result;
    			var userTeam = [];
    			for(var i=0;i<user.user_team.length;i++){
    				userTeam.push(user.user_team[i].team_name);
    			}
    			for(var i=0;i<answers.length;i++){
    				answersMap[answers[i].question_id] = answers[i];
    			}
    			for(var i=0;i<questions.length;i++){
    				questions[i].answer = answersMap[questions[i].question_id]||{};
    			}
    			userTeam = userTeam.join('-');
    			userResultWin.find('.exam-examination-info').prev().text(exam.exam_name);
    			userResultWin.find('.exam-examination-info').html(['<span>姓名：'+user.user_name+'</span><span>部门：'+userTeam+'</span><span>成绩：'+resultInfo.score+'</span><span>参考情况：'+(!!resultInfo.submit_time?'已交卷':(!!resultInfo.start_time?'未交卷':'未参考'))+'</span><span>通过情况：'+(exam.pass_mark>resultInfo.score?'未通过':'已通过')+'</span>'].join(''))
    	    	userResultTable.datagrid({
    	    		data: questions,
    	    		view: userResultView
    	    	});
    	    	userResultWin.window('open');
    		}
    	});
    }
    
    teamResultTab.find('.search-btn').click(function(){
    	var team_id = [];
    	team_id.push(teamResultSearch.combotree('getValue'));
    	(function(id){
    		var target = teamResultSearch.combotree('tree').tree('find',id).target;
    		var parent = teamResultSearch.combotree('tree').tree('getParent',target);
    		if(!!parent){
    			team_id.unshift(parent.team_id);
    			arguments.callee(parent.team_id);
    		}    		
    	}(team_id[0]));
    	resultTeamTable.datagrid('options').queryParams.team_id = team_id.join(',');
    	resultTeamTable.datagrid('reload');
    });
    teamResultTab.find('.export-btn').click(function(){
    	var team_id = resultTeamTable.datagrid('options').queryParams.team_id||'';
    	Wz.downloadFile('./api/adminExam/download_exam_team_statistics.json?exam_id='+resultParam.exam_id+'&team_id='+team_id+'&start=0&length=50000'+'&t='+new Date().getTime());
    });
    
    return {
    	delExam: delExam,
    	editExam: editExam,
    	copyExam: copyExam,
    	saveCopy: saveCopy,
    	viewExam: viewExam,
    	showResult: showResult,
    	showUserResultDetail: showUserResultDetail
    };
}()

