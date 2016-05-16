/*
 *请假管理-请假管理 
 * 
 */
Wz.namespace('Wz.offlineTraining');
Wz.offlineTraining.courseManage = function(){
	 $.parser.parse('#main-contain');
	 var searchForm = $('#training-course-search-form');
	 var editWin = $('#training-course-edit-win');
	 var editForm = $('#training-course-edit-form');
	 var itemListWin = $('#training-course-itemlist-win');
	 var selectListWin = $('#training-course-selectitem-win');
	 var resultWin = $('#training-course-result-win');
	 var ercodeWin = $('#training-course-show-ercode-win');
	 var allitemSearchForm = $('#training-course-allitem-talbe-tb');
	 var resultSearchForm = $('#training-course-result-talbe-tb');
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
	 
	 var courseTable = $('#training-course-table').datagrid({
		 /*url:'./api/offline_training/get_train_list.json',
		 queryParams:query_params,*/
		 data: {
			 recordsFiltered: 3,
			 data: [{
				 course_id: 1,
				 course_name: '一线主管的角色定位',
				 course_desc: '管理的定义、管理的作用及存在价值',
				 auth_cnt: 3,
				 discover_item_id: [],
				 image_name: '23e72a5216c55168cb25a3a46f4b03a0.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/23e72a5216c55168cb25a3a46f4b03a0.png',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,2,12,44,12).getTime()/1000
			 },{
				 course_id: 2,
				 course_name: '用户界面设计原则',
				 course_desc: '设计是通过阐明，简化，明确，修饰使之庄严，有说服性',
				 auth_cnt: 2,
				 discover_item_id: [],
				 image_name: 'a88d1ebd3dd3a4d3d0e7e51d7aa90ac9.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/a88d1ebd3dd3a4d3d0e7e51d7aa90ac9.png',
				 create_admin_name: '赵君',
				 create_time: new Date(2016,5,1,12,44,12).getTime()/1000
			 },{
				 course_id: 3,
				 course_name: '知识萃取',
				 course_desc: '企业如何做好知识萃取',
				 auth_cnt: 4,
				 discover_item_id: [],
				 image_name: '976454cc075486dc474b27afc25eb307.png',
				 image_url: 'http://7xlo7y.com1.z0.glb.clouddn.com/image/thumb60/976454cc075486dc474b27afc25eb307.png',
				 create_admin_name: '谢丽娜',
				 create_time: new Date(2016,4,21,12,44,12).getTime()/1000
			 }]
		 },
		 fitColumns: true,
		 checkOnSelect: true,
		 autoRowHeight: true,
		 striped:true,
		 nowrap: false,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     frozenColumns:[[{
	    	 field:'course_name',
	    	 title:'课程名称',
	    	 width:'20%'
	     },{
	    	 field:'course_desc',
	    	 title:'课程简介',
	    	 width:'20%'
	     },{
	    	 field:'auth_cnt',
	    	 title:'已认证讲师',
	    	 align:'center',
	    	 width:'10%'
	     },{
	    	 field:'create_admin_name',
	    	 title:'创建人',
	    	 width:'14%'
	     },{
	    	 field:'create_time',
	    	 title:'创建时间',
	    	 width:'18%',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	    	 }
	     },{
	    	 field:'course_id',
	    	 title:'操作',
	    	 width:'14%',
	    	 align:'center',
	         hidden: !(Wz.getPermission('offlineTraining/training/update')||Wz.getPermission('offlineTraining/training/delete')||Wz.getPermission('offlineTraining/training/state')||Wz.getPermission('offlineTraining/training/courses')||Wz.getPermission('offlineTraining/training/result')),
	    	 formatter: function(val,obj,row){
	    		 return (Wz.getPermission('offlineTraining/training/update')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.courseManage.editCourse('+row+')" href="javascript:void(0);">编辑</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/delete')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.courseManage.delCourse('+row+')" href="javascript:void(0);">删除</a>':'')+
	    		 (Wz.getPermission('offlineTraining/training/courses')?'<a class="tablelink edit-btn" onclick="Wz.offlineTraining.courseManage.showItems('+row+')" href="javascript:void(0);">关联课程</a>':'');
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
	    		 editForm.find('input[name=course_id]').val('');
	    		 editWin.window({title:'新建课程'}).window('open');
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
	 

	 var itemParam = {
		item_id: ''
	};
	var itemListTable = $('#offline-course-itemlist-table').datagrid({
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
	var allListTable = $('#offline-course-allitem-table').datagrid({
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
    	toolbar: '#offline-course-allitem-talbe-tb',
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
        toolbar: '#offline-course-allitem-talbe-tb',
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
    var selectedListTable = $('#offline-course-item-selected-table').datagrid({
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
    	itemParam.item_id = item_id.join(',');
		itemListTable.datagrid('reload');
		selectListWin.window('close');
		courseTable.datagrid('reload');
    	/*Wz.showLoadingMask('正在处理中，请稍后......');
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
    	});*/
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
	 
	 function editCourse(row){
		 editForm.form('reset');
		 var course = $.extend({},courseTable.datagrid('getRows')[row]);
		 if(course.image_name!=''){
			 uploadImge.setValue(course.image_name,course.image_url);
		 }else{
			 uploadImge.reset();
		 }
		 editForm.form('load',course);
		 editWin.window({title:'编辑课程'}).window('open');
	 }
	 
	 function saveCourse(){
		 if(!editForm.form('validate')){
			 $.messager.alert('错误','请正确填写表单','error');
			 return false;
		 }
		 var image_name = uploadImge.getValue();
		 var image_url = uploadImge.getUrl();
		 
		 var course_id = 101;
		 var course_name = editForm.find('input[name=course_name]').val();
		 var course_desc = editForm.find('input[name=course_desc]').val();
		 courseTable.datagrid('insertRow',{
			index: 0,
			row: {
				course_id: course_id,
				course_name: course_name,
				course_desc: course_desc,
				image_name: image_name,
				image_url: image_url,
				auth_cnt: 0,
				discover_item_id: [],
				create_admin_name: '赵君',
				create_time: new Date().getTime()/1000
			}
		 });
		 editWin.window('close');	 
	 }
	 
	 function delCourse(row){
		var course = $.extend({},courseTable.datagrid('getRows')[row]);
		$.messager.confirm('提示','请确认是否要删除课程？',function(ok){
    		if(ok){
    			courseTable.datagrid('deleteRow',row);    			
    		}
    	});
	 }
	 function showItems(row){
		var course = $.extend({},courseTable.datagrid('getRows')[row]);
		itemParam.item_id = (course.discover_item_id||[]).join(',');
		itemListWin.find('input[name=course_id]').val(course.train_id);
    	itemListTable.datagrid('reload');
    	itemListWin.window('open');
	 }
	return {
		editCourse: editCourse,
		saveCourse: saveCourse,
		delCourse: delCourse,
		showItems: showItems
	};
}()