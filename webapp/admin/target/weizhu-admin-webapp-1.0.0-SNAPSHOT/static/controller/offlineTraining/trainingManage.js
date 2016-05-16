/*
 *请假管理-请假管理 
 * 
 */
Wz.namespace('Wz.offlineTraining');
Wz.offlineTraining.trainingManage = function(){
	 $.parser.parse('#main-contain');
	 var query_params = {
		 train_name:'',
		 state:'',
		 create_admin_id: '',
		 start_time:'',
		 end_time:''
	 };
	 var searchForm = $('#offlineTraining-search-form');
	 var editWin = $('#offlineTraining-edit-win');
	 var editForm = $('#offlineTraining-edit-form');
	 var itemListWin = $('#offline-training-itemlist-win');
	 var selectListWin = $('#offline-training-selectitem-win');
	 var resultWin = $('#offline-training-result-win');
	 var ercodeWin = $('#offline-training-show-ercode-win');
	 var allitemSearchForm = $('#offline-training-allitem-talbe-tb');
	 var resultSearchForm = $('#offline-training-result-talbe-tb');
	 var uploadImge = editForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 375,
    	tipInfo: '图片格式必须为:png,jpg,gif；<br>图片质量不可大于1M,图片尺寸建议640*320(px)',
    	maxSize: 1,
    	params: {
    		image_tag: '培训,图标'
    	}
    });
	
	 var queryPosition = $('#offline-training-user-tb input[name=position_id]').combobox({
     	mode: 'remote',
     	valueField: 'position_id',
     	textField: 'position_name',
     	editable: false,
     	cascadeCheck: false,
     	height: 'auto',
     	loader: function(param,success,error){
 			Wz.ajax({
 				url: './api/user/get_position.json',
 				success: function(json){
 					success(json.position);
 				}
 			});
 		},
 		onShowPanel: function(){
 			$(this).combobox('reload');
 		}
     });
 	var queryTeam = $('#offline-training-user-tb input[name=team_id]').combotree({
 		panelWidth: 200,
     	url: './api/user/get_team.json',
     	editable: false,
     	cascadeCheck: false,
     	onBeforeExpand: function(row){
     		queryTeam.combotree('options').queryParams.team_id = row.team_id;
     		return true;
     	},
     	onShowPanel: function(){
     		queryTeam.combotree('options').queryParams.team_id = '';
     		queryTeam.combotree('reload');
     	},
     	loadFilter: function(data){
     		if(!!data){
     			for(var i=0;i<data.length;i++){
     				data[i].id = data[i].team_id;
     				data[i].text = data[i].team_name;
     				data[i].state = (data[i].has_sub_team?'closed':'open');
     			}
     			if(queryTeam.combotree('options').queryParams.team_id == ''){
     				data.unshift({
     					id: '',
     					text: '---全部部门---'
     				});
     			}
     		}
     		return data;
     	}
     });
	 
	 var lecturerParam = {
		team_id: '',
    	position_id: '',
    	keyword: '',
    	mobile_no: ''
	 };
	 
	 var lecturerUser = editForm.find('input[name=lecturer_user_id]').combogrid({
    	url: './api/user/get_user_list.json',
    	queryParams: lecturerParam,
        fitColumns: true,
        checkOnSelect: true,
        striped: true,
        pagination: true,
        rownumbers: true,
        required: true,
        multiple: true,
        pageSize: 20,
        idField: 'user_id',
        textField: 'user_name',
        editable: false,
        width: 375,
        panelWidth: 700,
        toolbar: '#offline-training-user-tb',
        frozenColumns: [[{
        	field: 'ck',
        	checkbox: true
        },{
            field: 'user_name',
            title: '姓名',
            width: '80px',
            align: 'center'
        },{
            field: 'gender',
            title: '性别',
            width: '40px',
            align: 'center',
            formatter: function(val,obj,row){
            	return val=='FEMALE'?'女':'男';
            }
        },{
            field: 'mobile_no',
            title: '手机号',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val.join('<br/>'): '';
            }
        },{
            field: 'team',
            title: '部门',
            width: '250px',
            align: 'center',
            formatter: function(val,obj,row){
            	val = val || [];
            	var team = [];
            	for(var i=0;i<val.length;i++){
            		team.push(val[i].team_name);
            	}
            	team = team.join('-');
            	return '<span title="'+team+'" class="easyui-tooltip">'+team + '</span>';
            }
        }]],
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
            return {
                total: data.recordsFiltered,
                rows: data.data
            };
        }
    });
	 $('#offline-training-user-tb .search-btn').click(function(){
		 lecturerParam.team_id = queryTeam.combotree('getValue');
		 lecturerParam.position_id = queryPosition.combobox('getValue');
		 lecturerParam.keyword = $('#offline-training-user-tb input[textboxname=keyword]').textbox('getValue');
		 lecturerParam.mobile_no = $('#offline-training-user-tb input[textboxname=mobile_no]').textbox('getValue');
		 lecturerUser.combogrid('grid').datagrid('reload');
	 });
	 var startTime = editForm.find('input[name=start_time]').datetimebox({
    	editable: false,
        required: true,
        onChange: function(newValue,oldValue){
        	var st = Wz.parseDate(newValue).getTime();
        	var et = endTime.datetimebox('getValue');
        	if(et == ''){
        		endTime.datetimebox('setValue',Wz.dateFormat(new Date(st+4*3600000),'yyyy-MM-dd hh:mm:ss'));
        	}
        	var cst = checkInStartTime.datetimebox('getValue');
        	if(cst == ''){
        		checkInStartTime.datetimebox('setValue',Wz.dateFormat(new Date(st-1*3600000),'yyyy-MM-dd hh:mm:ss'));
        	}
        	var cet = checkInEndTime.datetimebox('getValue');
        	if(cet == ''){
        		checkInEndTime.datetimebox('setValue',Wz.dateFormat(new Date(st+4*3600000),'yyyy-MM-dd hh:mm:ss'));
        	}
        	var ast = applyStartTime.datetimebox('getValue');
        	if(ast == ''){
        		applyStartTime.datetimebox('setValue',Wz.dateFormat(new Date(),'yyyy-MM-dd hh:mm:ss'));
        	}
        	var aet = applyEndTime.datetimebox('getValue');
        	if(aet == ''){
        		applyEndTime.datetimebox('setValue',Wz.dateFormat(new Date(st-3*3600000),'yyyy-MM-dd hh:mm:ss'));
        	}
        }
	 });
	 var endTime = editForm.find('input[name=end_time]').datetimebox({
    	editable: false,
        required: true,
        onChange: function(newValue,oldValue){
        	var et = Wz.parseDate(newValue).getTime();
        	var cet = checkInEndTime.datetimebox('getValue');
        	if(cet == ''){
        		checkInEndTime.datetimebox('setValue',Wz.dateFormat(new Date(et),'yyyy-MM-dd hh:mm:ss'));
        	}
        }
	 });
	var applyStartTime = editForm.find('input[name=apply_start_time]').datetimebox({
		editable: false,
	    required: true
	});
	var applyEndTime = editForm.find('input[name=apply_end_time]').datetimebox({
		editable: false,
	    required: true
	});
	
	var checkInStartTime = editForm.find('input[name=check_in_start_time]').datetimebox({
		editable: false,
	    required: true
	});
	var checkInEndTime = editForm.find('input[name=check_in_end_time]').datetimebox({
		editable: false,
	    required: true
	});
	editForm.find('input[textboxname=apply_enable]').combobox({
		onChange: function(newValue,oldValue){
			if(newValue){
				editForm.find('input[name=apply_user_count]').parents('.form-item').show();
				editForm.find('input[name=apply_start_time]').parents('.form-item').show();
			}else{
				editForm.find('input[name=apply_user_count]').parents('.form-item').hide();
				editForm.find('input[name=apply_start_time]').parents('.form-item').hide();
			}
		}
	});
	var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
	 var offlineTrainingTable = $('#offlineTraining-table').datagrid({
		 url:'./api/offline_training/get_train_list.json',
		 queryParams:query_params,
		 fitColumns: true,
		 checkOnSelect: true,
		 autoRowHeight: true,
		 striped:true,
		 nowrap: false,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     frozenColumns:[[{
	    	 field:'train_name',
	    	 title:'培训名称',
	    	 width:'20%'
	     },{
	    	 field:'lecturer_name',
	    	 title:'讲师',
	    	 width:'14%',
	    	 formatter: function(val,obj,row){
	    		 var user = [];
	    		 obj.lecturer_user = obj.lecturer_user || [];
	    		 for(var i=0;i<obj.lecturer_user.length;i++){
	    			 user.push(obj.lecturer_user[i].user_name);
	    		 }
	    		 user.push(val);
	    		 return user.join(',');
	    	 }
	     },{
	    	 field:'start_time',
	    	 title:'培训时间',
	    	 width:'12%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss')+'至'+Wz.dateFormat(new Date(obj.end_time*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'train_address',
	    	 title:'培训地点',
	    	 width:'20%'
	     },{
	    	 field:'train_state',
	    	 title:'培训状态',
	    	 width:'8%',
	    	 align: 'center'
	     },{
	    	 field:'train_ercode',
	    	 title:'二维码',
	    	 width:'6%',
	    	 align: 'center',
	    	 formatter: function(val,obj,row){
	    		 return '<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.downErcode('+row+')" href="javascript:void(0);">查看</a>'
	    	 }
	     },{
	    	 field:'offlineTraining_id',
	    	 title:'操作',
	    	 width:'14%',
	    	 align:'center',
	            hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/courses')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 var state = (obj.state=='NORMAL'?'禁用':'启用');
	    		 return (Wz.getPermission('offlineTraining/training/update')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.editTraining('+row+')" href="javascript:void(0);">编辑</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.delTraining('+row+')" href="javascript:void(0);">删除</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/state')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.changeState('+row+')" href="javascript:void(0);">'+state+'</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/courses')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.showItems('+row+')" href="javascript:void(0);">关联课程</a>':'')+
	    		 //'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.showActives('+row+')" href="javascript:void(0);">关联互动</a>'+
	    		 (Wz.getPermission('offlineTraining/training/result')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.trainingManage.showResult('+row+')" href="javascript:void(0);">查看结果</a>':'');
	    	 }
	     }]],
	     toolbar:[{
	    	 id: 'offlineTraining-add-btn',
	    	 text: '新建',
	    	 disabled: !Wz.getPermission('offlineTraining/training/create'),
	    	 iconCls: 'icon-add',
	    	 handler: function(){
	    		 editForm.form('reset');
	    		 uploadImge.reset();
	    		 editForm.find('input[name=train_id]').val('');
	    		 allowModel.setValue({model_id:'',model_name:''});
	    		 editWin.find('input[name=enable_notify_user]').parents('.form-item').hide();
	    		 editWin.window({title:'新建培训'}).window('open');
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
	                total: data.recordsFiltered,
	                rows: data.data
	            };
	        }
	 });
	 
	searchForm.find('.search-btn').click(function(){
		query_params.train_name = searchForm.find('input[name=train_name]').val();
		query_params.start_time = searchForm.find('input[name=start_time]').val();
		query_params.end_time = searchForm.find('input[name=end_time]').val();
		query_params.state = searchForm.find('input[name=state]').val();
		if(query_params.start_time != ''){
			query_params.start_time = new Date(query_params.start_time).getTime()/1000;
		}
		if(query_params.end_time != ''){
			query_params.end_time = new Date(query_params.end_time).getTime()/1000;
		}
		offlineTrainingTable.datagrid('reload');
	});
	searchForm.find('.reset-btn').click(function(){
		searchForm.form('reset');
	});
	 
	var itemParam = {
		item_id: ''
	};
	var itemListTable = $('#offline-training-itemlist-table').datagrid({
    	url: './api/discover/get_discover_item_by_ids.json',
    	queryParams: itemParam,
    	fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        idField: 'item_id',
    	columns: [[{
    		field: 'item_name',
    		width: 500,
    		title: '标题'
    	},{
        	field: 'content',
            title: '类型',
            width: '6%',
            align: 'center',
            formatter: function(val,obj,row){
            	if(typeof val.web_url != 'undefined'){
            		return '链接';
            	}else if(typeof val.video_url != 'undefined'){
            		return '视频';
            	}else if(typeof val.audio_url != 'undefined'){
            		return '音频';
            	}else if(typeof val.document_url != 'undefined'){
            		return 'PDF文档';
            	}
            }
        },{
        	field: 'create_admin_name',
            title: '创建人',
            width: '12%',
            align: 'center'
        },{
    		field: 'item_desc',
    		width: 400,
    		title: '描述'
    	},{
            field: 'item_id',
            title: '操作',
            width: 120,
            align: 'center',
            hidden: !(Wz.getPermission('discover/module/delete_category_item')),
            formatter: function(val,obj,row){
                return '<a href="javascript:void(0)" class="tablelink del-btn">移除</a>';
            }
        }]],
    	toolbar: [{
            id: 'discover-module-item-add',
            text: '添加课件',
            disabled: !Wz.getPermission('discover/module/add_category_item'),
            iconCls: 'icon-add',
            handler: function(){
            	allListTable.datagrid('reload');
            	allListTable.datagrid('clearChecked');
            	selectedListTable.datagrid('loadData',[]);
            	selectListWin.window('open');
            }
        }],
    	loadFilter: function(data){
    		var list = data.item;
    		for(var i=0;i<list.length;i++){
    			list[i].item_id = list[i].base.item_id;
    			list[i].item_name = list[i].base.item_name;
    			list[i].item_desc = list[i].base.item_desc;
    			list[i].content = JSON.parse(list[i].base.content||'{}');
    			list[i].create_admin_name = list[i].base.create_admin_name;
    		}
    		return {
                total: data.filtered_size,
                rows: list
            };
    	}
    });
	
	var allItemParam = {
    	item_name: ''
    };
	var allListTable = $('#offline-training-allitem-table').datagrid({
    	url: './api/discover/get_discover_item.json',
    	queryParams: allItemParam,
    	fitColumns: true,
        checkOnSelect: true,
        title: '备选课件',
        striped: true,
        fit: true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        idField: 'item_id',
    	columns: [[{
    		field:'ck',
    		checkbox: true
    	},{
    		field: 'item_name',
    		width: 500,
    		title: '标题',
    		formatter: function(val,obj,row){	
    			if(!!obj.is_added){
    				val = '【已添加】'+val;
    			}
    			return val;
    		}
    	},{
    		field: 'item_desc',
    		width: 400,
    		title: '描述'
    	}]],
    	toolbar: '#offline-training-allitem-talbe-tb',
    	onCheck: function(index,row){
    		if(!!row.is_added)return false;
        	var data = selectedListTable.datagrid('getData').rows;
        	var data = $.grep(data,function(o){
        		return o.item_id == row.item_id;
        	});
        	if(data.length == 0){
        		selectedListTable.datagrid('insertRow',{
        			index: 0,
        			row: row
        		});
        	}
        },
        onUncheck: function(index,row){
    		if(!!row.is_added)return false;
        	var data = selectedListTable.datagrid('getData').rows;
        	$.grep(data,function(o,i){
        		if(!!!o)return false;
        		if(row.item_id == o.item_id){
        			selectedListTable.datagrid('deleteRow',i);
        		}
        		return true;
        	});
        },
        onUncheckAll: function(rows){
        	var oldData = selectedListTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		if(!!!o.is_added){
        			for(var i=oldData.length;i>0;i--){
        				if(oldData[i-1].item_id == o.item_id){
        					selectedListTable.datagrid('deleteRow',i-1);
        					break;
        				}
        			}        			
        		}
        	});
        },
        onCheckAll: function(rows){
        	var oldData = selectedListTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		if(!!!o.is_added){
        			var same = $.grep(oldData,function(od){
        				return od.item_id == o.item_id;
        			});                    		
        			if(same.length == 0){
        				selectedListTable.datagrid('insertRow',{
        					index: 0,
        					row: o
        				});
        			}        			
        		}
        	});
        },
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        toolbar: '#offline-training-allitem-talbe-tb',
    	loadFilter: function(data){
    		var list = data.item;
    		var alist = [];
    		var blist = [];
    		for(var i=0;i<list.length;i++){
    			list[i].item_id = list[i].base.item_id;
    			list[i].item_name = list[i].base.item_name;
    			list[i].item_desc = list[i].base.item_desc;
    			var flag = false;
    			var vdata = itemListTable.datagrid('getRows');
        		for(var j=0;j<vdata.length;j++){
        			if(list[i].item_id == vdata[j].item_id){
        				list[i].is_added = true;
        				flag = true;
        				break;
        			}
            	}
        		if(flag){
        			alist.push(list[i]);        			
        		}else{
        			blist.push(list[i]);       
        		}
    		}
    		list = blist.concat(alist);
    		return {
                total: data.filtered_size,
                rows: list
            };
    	}
    });
    var selectedListTable = $('#offline-training-item-selected-table').datagrid({
    	data: [],
    	fitColumns: true,
        checkOnSelect: false,
        title: '已选课件',
        striped: true,
        fit: true,
        rownumbers: true,
        pagination: true,
        idField: 'item_id',
    	columns: [[{
    		field: 'item_name',
    		width: 500,
    		title: '标题'
    	},{
    		field: 'item_desc',
    		width: 400,
    		title: '描述'
    	},{
            field: 'user_id',
            title: '操作',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
            }
        }]],
    	loadFilter: function(data){
    		return {
                total: data.length,
                rows: data
            };
    	}
    });
    
    selectListWin.on('click','.del-btn',function(){
    	var item = selectedListTable.datagrid('getData').rows[$(this).parents('tr').index()];
		var rows = allListTable.datagrid('getRows');
		var index = 0;
		for(;index<rows.length;index++){
			if(rows[index].item_id == item.item_id){
				break;
			}
		}
		if(index<rows.length){
			allListTable.datagrid('uncheckRow',index);			
		}
    });
    
    selectListWin.find('.edit-save').click(function(){
    	var items = selectedListTable.datagrid('getRows');
    	var train_id = itemListWin.find('input[name=train_id]').val();
    	if(items.length == 0){
    		$.messager.alert('错误','请选择课件之后 保存！','error');
    		return false;
    	}
    	var item_id = [itemParam.item_id];
    	for(var i=0;i<items.length;i++){
    		item_id.push(items[i].item_id);
    	}
    	Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: './api/offline_training/update_train_discover_item.json',
    		data: {
    			discover_item_id: item_id.join(','),
    			train_id: train_id
    		},
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    		    	itemParam.item_id = item_id.join(',');
    				itemListTable.datagrid('reload');
    				selectListWin.window('close');
    				offlineTrainingTable.datagrid('reload');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    });
    
    itemListWin.on('click','.del-btn',function(){
		var items = itemListTable.datagrid('getRows');
		var index = $(this).parents('tr').index();
		var train_id = itemListWin.find('input[name=train_id]').val();
		var item_id = [];
		for(var i=0;i<items.length;i++){
			if(i != index){
				item_id.push(items[i].item_id);    			
			}
		}
    	$.messager.confirm({
			title: '请确认',
			msg: '请确认是否移除该课件？',
			fn: function(ok){
				if(ok){
					Wz.showLoadingMask('正在处理中，请稍后......');
					Wz.ajax({
						type: 'post',
						url: './api/offline_training/update_train_discover_item.json',
						data: {
							discover_item_id: item_id.join(','),
							train_id: train_id
						},
						success: function(json){
							Wz.hideLoadingMask();
							if(json.result == 'SUCC'){
								itemParam.item_id = item_id.join(',');
								itemListTable.datagrid('reload');
								offlineTrainingTable.datagrid('reload');
							}else{
								$.messager.alert('错误',json.fail_text,'error');
							}
						}
					});
				}
			}
    	});
    });

    allitemSearchForm.find('.search-btn').click(function(){
    	allItemParam.item_name = allitemSearchForm.find('input[name=item_name]').val();
    	allListTable.datagrid('reload');
    });
    var resultParam = {
    	train_id: '',
    	is_check_in: '',
    	is_leave: ''
    };
    var resultTable = $('#offline-training-result-user-table').datagrid({
    	url:'./api/offline_training/get_train_user_list.json',
		 queryParams:resultParam,
		 checkOnSelect: true,
		 autoRowHeight: true,
		 striped:true,
		 nowrap: false,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     columns:[[{
	    	 field:'raw_id',
	    	 title:'工号',
	    	 width:'60px'
	     },{
	    	 field:'user_name',
	    	 title:'姓名',
	    	 width:'60px'
	     },{
		    field: 'mobile_no',
		    title: '手机号',
		    width: '100px',
		    align: 'center',
		    formatter: function(val,obj,row){
		    	return !!val?val.join('<br/>'): '';
		    }
		},{
            field: 'team',
            title: '部门',
            width: '250px',
            align: 'center',
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
	    	 field:'is_apply',
	    	 title:'报名状态',
	    	 width:'60px',
	    	 align: 'center',
	    	 formatter: function(val,obj,row){
	    		 return val?'已报名':'未报名';
	    	 }
	     },{
	    	 field:'is_check_in',
	    	 title:'签到状态',
	    	 width:'60px',
	    	 align: 'center',
	    	 formatter: function(val,obj,row){
	    		 return val?'已签到':'未签到';
	    	 }
	     },{
	    	 field:'check_in_time',
	    	 title:'签到时间',
	    	 width:'80px',
	    	 align: 'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'--';
	    	 }
	     },{
	    	 field:'is_leave',
	    	 title:'请假状态',
	    	 width:'80px',
	    	 align: 'right',
	    	 formatter: function(val,obj,row){
	    		 return val?'已请假':'未请假';
	    	 }
	     },{
	    	 field:'leave_reason',
	    	 title:'请假事由',
	    	 width:'80px'
	     }]],
	     toolbar: '#offline-training-result-talbe-tb',
	     changePages: function(params,pageObj){
	        	$.extend(params,{
	        		start: (pageObj.page-1)*pageObj.rows,
	        		length: pageObj.rows
	        	});
	        },
	      loadFilter: function(data){
	    	  for(var i=0;i<data.data.length;i++){
	    		  $.extend(data.data[i],data.data[i].user);
	    	  }
	            return {
	                total: data.recordsFiltered,
	                rows: data.data
	            };
	        }
    });
    resultSearchForm.find('.search-btn').click(function(){
    	resultParam.is_check_in = resultSearchForm.find('input[name=is_check_in]').val();
    	resultParam.is_leave = resultSearchForm.find('input[name=is_leave]').val();
    	resultTable.datagrid('reload');
    });
	function editTraining(row){
		 editForm.form('reset');
		 var training = $.extend({},offlineTrainingTable.datagrid('getRows')[row]);
		 training.start_time = Wz.dateFormat(new Date(training.start_time*1000),'yyyy-MM-dd hh:mm:ss');
		 training.end_time = Wz.dateFormat(new Date(training.end_time*1000),'yyyy-MM-dd hh:mm:ss');
		 training.apply_start_time = Wz.dateFormat(new Date(training.apply_start_time*1000),'yyyy-MM-dd hh:mm:ss');
		 training.apply_end_time = Wz.dateFormat(new Date(training.apply_end_time*1000),'yyyy-MM-dd hh:mm:ss');
		 training.check_in_start_time = Wz.dateFormat(new Date(training.check_in_start_time*1000),'yyyy-MM-dd hh:mm:ss');
		 training.check_in_end_time = Wz.dateFormat(new Date(training.check_in_end_time*1000),'yyyy-MM-dd hh:mm:ss');
		 editForm.form('load',training);
		 if(training.image_name!=''){
			 uploadImge.setValue(training.image_name,Wz.config.image_60_url_prefix+training.image_name);
		 }else{
			 uploadImge.reset();
		 }
		 allowModel.setValue({model_id:training.allow_model_id||'',model_name:training.allow_model_name||''});
		 var lecturer_user = {
			text: '',
			value: []
		 };
		 training.lecturer_user = training.lecturer_user || [];
		 for(var i=0;i<training.lecturer_user.length;i++){
			 lecturer_user.text += training.lecturer_user[i].user_name + ',';
			 lecturer_user.value.push(training.lecturer_user[i].user_id);
		 }
		 lecturerUser.combogrid('setValues',lecturer_user.value);
		 lecturerUser.combogrid('setText',lecturer_user.text);
		 editWin.find('input[name=enable_notify_user]').parents('.form-item').show();
		 editWin.window({title:'编辑培训'}).window('open');
	 }
	 
	function saveEdit(){
		var train_id = editForm.find('input[name=train_id]').val();
		var train_name = editForm.find('input[textboxname=train_name]').textbox('getValue');
		var image_name = uploadImge.getValue();
		var lecturer_user_id = lecturerUser.combogrid('getValues');
		var lecturer_name = editForm.find('input[textboxname=lecturer_name]').textbox('getValue');
		var start_time = startTime.datetimebox('getValue');
		var end_time = endTime.datetimebox('getValue');
		var train_address = editForm.find('input[textboxname=train_address]').textbox('getValue');
		var arrangement_text = editForm.find('input[textboxname=arrangement_text]').textbox('getValue');
		var describe_text = editForm.find('input[textboxname=describe_text]').textbox('getValue');
		var apply_enable = editForm.find('input[textboxname=apply_enable]').combobox('getValue');
		var apply_start_time = applyStartTime.datetimebox('getValue');
		var apply_end_time = applyEndTime.datetimebox('getValue');
		var check_in_start_time = checkInStartTime.datetimebox('getValue');
		var check_in_end_time = checkInEndTime.datetimebox('getValue');
		var apply_user_count = editForm.find('input[textboxname=apply_user_count]').numberbox('getValue');
		var allow_model_id = allowModel.getValue();
		var enable_notify_user = editWin.find('input[name=enable_notify_user]').prop('checked');
		var url = './api/offline_training/create_train.json';
		if(train_id != ''){
			url = './api/offline_training/update_train.json';
		}
		var tuser = lecturer_user_id.pop();
		if(tuser != ''){
			lecturer_user_id.push(tuser);
		}
		if(!editForm.find('input[textboxname=train_name]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写培训名称！','error');
    		return false;
    	}
		if(!editForm.find('input[textboxname=lecturer_name]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写外部讲师姓名！','error');
    		return false;
    	}
		
		if(lecturer_user_id.length == 0 && lecturer_name){
			$.messager.alert('错误','请至少选择一个内部讲师，或者输入一个外部讲师姓名！','error');
    		return false;
		}
		
		if(start_time == ''){
			$.messager.alert('错误','培训开始时间不能为空！','error');
			return false;
		}else if(end_time == ''){
			$.messager.alert('错误','培训结束时间不能为空！','error');
			return false;
		}else{
			var now = new Date().getTime()/1000;
			start_time = Wz.parseDate(start_time).getTime()/1000;
			end_time = Wz.parseDate(end_time).getTime()/1000;
			apply_start_time = Wz.parseDate(apply_start_time).getTime()/1000;
			apply_end_time = Wz.parseDate(apply_end_time).getTime()/1000;
			check_in_start_time = Wz.parseDate(check_in_start_time).getTime()/1000;
			check_in_end_time = Wz.parseDate(check_in_end_time).getTime()/1000;
			/*if(start_time < now){
				$.messager.alert('错误','培训开始时间必须大于当前时间！','error');
				return false;
			}*/
			if(start_time >= end_time){
				$.messager.alert('错误','培训开始时间不能大于培训结束时间！','error');
				return false;
			}
			if(check_in_start_time >= check_in_end_time){
				$.messager.alert('错误','签到开始时间不能大于签到结束时间！','error');
				return false;
			}
			if(check_in_start_time >= end_time){
				$.messager.alert('错误','签到开始时间不能大于培训结束时间！','error');
				return false;
			}
			if(check_in_end_time > end_time){
				$.messager.alert('错误','签到结束时间不能大于培训结束时间！','error');
				return false;
			}
		}
		if(!editForm.find('input[textboxname=train_address]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写培训地点！','error');
    		return false;
    	}
		if(!editForm.find('input[textboxname=arrangement_text]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写培训安排！','error');
    		return false;
    	}
		if(!editForm.find('input[textboxname=describe_text]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写培训简介！','error');
    		return false;
    	}

		if(apply_enable == 'true'){
			if(apply_user_count == ''){
				$.messager.alert('错误','请填写报名人数限制！','error');
	    		return false;
			}
			if(apply_start_time >= apply_end_time){
				$.messager.alert('错误','报名开始时间不能大于报名结束时间！','error');
				return false;
			}
			if(apply_start_time > start_time){
				$.messager.alert('错误','报名开始时间不能大于培训开始时间！','error');
				return false;
			}	
			if(check_in_start_time < apply_end_time){
				$.messager.alert('错误','签到开始时间不能小于报名结束时间！','error');
				return false;
			}	
		}
		if(allow_model_id == ''){
			$.messager.alert('错误','请选择培训对象！','error');
    		return false;
		}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: {
    			train_id: train_id,
    			train_name: train_name,
    			image_name: image_name,
    			start_time: start_time,
    			end_time: end_time,
    			apply_enable: apply_enable,
    			apply_start_time: apply_start_time,
    			apply_end_time: apply_end_time,
    			apply_user_count: apply_user_count,
    			train_address: train_address,
    			lecturer_name: lecturer_name,
    			lecturer_user_id: lecturer_user_id.join(','),
    			check_in_start_time: check_in_start_time,
    			check_in_end_time: check_in_end_time,
    			arrangement_text: arrangement_text,
    			describe_text: describe_text,
    			allow_model_id: allow_model_id,
    			enable_notify_user: enable_notify_user
    		},
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				offlineTrainingTable.datagrid('reload');
    				editWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
	}
	
	function delTraining(row){
		var training = $.extend({},offlineTrainingTable.datagrid('getRows')[row]);
		$.messager.confirm('提示','请确认是否要删除本次培训？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/offline_training/update_train_state.json',
    				data: {
    					train_id: training.train_id,
    					state: 'DELETE'
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						offlineTrainingTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
	}
	
	function changeState(row){
		var training = $.extend({},offlineTrainingTable.datagrid('getRows')[row]);
		var state = (training.state=='NORMAL'?'DISABLE':'NORMAL');
		var msg = state=='NORMAL'?'请确认是否启用该培训？':'请确认是否禁用该培训？';
		$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/offline_training/update_train_state.json',
    				data: {
    					train_id: training.train_id,
    					state: state
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						offlineTrainingTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
	}
	function showResult(row){
		var training = $.extend({},offlineTrainingTable.datagrid('getRows')[row]);
		var user = [];
		training.lecturer_user = training.lecturer_user || [];
		 for(var i=0;i<training.lecturer_user.length;i++){
			 user.push(training.lecturer_user[i].user_name);
		 }
		 user.push(training.lecturer_name);
		resultWin.find('.offline-training-title').text(training.train_name);
		resultWin.find('.offline-training-img').attr('src',(!!training.image_name?Wz.config.image_240_url_prefix+training.image_name:'../static/images/vodDefault.png'));
		resultWin.find('.offline-training-desc p').text(training.describe_text);
		resultWin.find('.offline-training-arrangement p').html(training.arrangement_text.replace(/\n/g,'<br/>'));
		resultWin.find('.offline-training-info').html(['<span class="offline-training-info-item">培训时间: ',Wz.dateFormat(new Date(training.start_time*1000),'yyyy-MM-dd hh:mm:ss'),' 至 ',Wz.dateFormat(new Date(training.end_time*1000),'yyyy-MM-dd hh:mm:ss'),'</span>',
		               (!!training.aplly_start_time?'<span class="offline-training-info-item">报名时间: '+Wz.dateFormat(new Date(training.aplly_start_time*1000),'yyyy-MM-dd hh:mm:ss')+' 至 '+Wz.dateFormat(new Date(training.aplly_end_time*1000)+'yyyy-MM-dd hh:mm:ss')+'</span>':''),
		               '<span class="offline-training-info-item">签到时间: ',Wz.dateFormat(new Date(training.check_in_start_time*1000),'yyyy-MM-dd hh:mm:ss'),' 至 ',Wz.dateFormat(new Date(training.check_in_end_time*1000),'yyyy-MM-dd hh:mm:ss'),'</span>',
		               '<span class="offline-training-info-item">地点: ',training.train_address,'</span>',
		               '<span class="offline-training-info-item">讲师: ',user.join(','),'</span>',
		               (training.apply_enable?'<span class="offline-training-info-item">最大报名人数: <strong>'+training.apply_user_count+'</strong></span>':''),
		               (training.apply_enable?'<span class="offline-training-info-item">实际报名人数: <strong>'+training.user_apply_count+'</strong></span>':''),
		               '<span class="offline-training-info-item">签到人数: <strong>',training.user_check_in_count,'</strong></span>',
		               '<span class="offline-training-info-item">请假人数: <strong>',training.user_leave_count,'</strong></span>'].join(''));
		resultParam.train_id = training.train_id;
		loadCourses((training.discover_item_id||[]).join(','));
		resultTable.datagrid('reload');
		resultWin.window('open');
	}
	
	function showItems(row){
		var training = $.extend({},offlineTrainingTable.datagrid('getRows')[row]);
		itemParam.item_id = (training.discover_item_id||[]).join(',');
		itemListWin.find('input[name=train_id]').val(training.train_id);
    	itemListTable.datagrid('reload');
    	itemListWin.window('open');
	}
	
	function downErcode(row){
		var training = $.extend({},offlineTrainingTable.datagrid('getRows')[row]);
		var url = 'http:\/\/'+location.host+'/mobile/offline_training/training_info.html?train_id='+training.train_id+'&from=app';
		url = './api/qr_code.jpg?size=200&content=' + encodeURIComponent(url);
		ercodeWin.find('.ercode-show').attr('src',url);
		ercodeWin.window('open');
	}
	function loadCourses(item_id){
		item_id = item_id||'';
		Wz.ajax({
			url: './api/discover/get_discover_item_by_ids.json',
			data: {
				item_id: item_id
			},
			success: function(data){
				var items = data.item||[];
				var html = [];
				for(var i=0;i<items.length;i++){
					html.push('<a href="javascript:void(0)" class="review-item">'+items[i].base.item_name+'</a>');
				}
				resultWin.find('.offline-training-courses p').html(html.join(''));
			}
		});
	}
	return {
		editTraining: editTraining,
		saveEdit: saveEdit,
		delTraining: delTraining,
		changeState: changeState,
		showItems: showItems,
		showResult: showResult,
		downErcode: downErcode
	};
}()