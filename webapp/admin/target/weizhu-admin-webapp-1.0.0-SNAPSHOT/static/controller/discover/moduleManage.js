/**
 * Created by allenpeng on 16-2-3.
 * 功能介绍： 社区管理-板块管理
 */
Wz.namespace('Wz.discover');
Wz.discover.moduleManage = function(){
    $.parser.parse('#main-contain');
    var m_prefix_url = 'http://'+location.host;
    var mainWrap = $('#discover-module-wrap');
    var discoverForm = $('#discover-discover-edit-form');
    var editBtn = discoverForm.find('.edit-btn');
    var allitemSearchForm = $('#discover-allitem-talbe-tb');
    var discoverName = discoverForm.find('input[name=discover_name]').textbox({
    	disabled:true,
    	required: true,
    	prompt: '社区名称长度为1~10个字符',
    	width: 300,
    	validType: 'length[1,10]'
    });
    var editWin = $('#discover-module-edit-win');
    var editForm = $('#discover-module-edit-form');
    var categoryWin = $('#discover-category-edit-win');
    var categoryForm = $('#discover-category-edit-form');
    var itemListWin = $('#discover-category-itemlist-win');
    var selectListWin = $('#discover-module-selectitem-win');
    var selectListForm = $('#discover-module-selectitem-form');
    var uploadImge = editForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '图片格式必须为:png,jpg,gif；<br/>图片质量不可大于1M,图片尺寸建议60X60(px)',
    	maxSize: 1,
    	params: {
    		image_tag: '发现,图标'
    	}
    });
    editForm.find('input[name=module_type]').combobox({
    	width:350,
    	valueField:'value',
    	value:'0',
    	textField:'name',
    	data:[{name:'课程库',value:'0'},{name:'考试中心',value:'2'},{name:'调研中心',value:'4'},{name:'问答中心',value:'3'},{name:'链接类',value:'1'},{name:'线下培训',value:'5'}],
    	editable:false,
    	panelHeight:'auto',
    	onChange: function(newValue,oldValue){
    		if(newValue == '1'){
    			editForm.find('input[name=web_url]').parents('.form-item').show();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').show();
    		}else{
    			editForm.find('input[name=web_url]').parents('.form-item').hide();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
    		}
    	}
    });
    var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
    var categoryAllowModel = Wz.comm.allowService(categoryForm.find('input[name=allow_model_id]'),{});
    var curCategoryTable = null;
    var curCategoryIndex = 0;

    var moduleTable = $('#discover-module-table').datagrid({
    	url: './api/discover/get_discover_module.json',
    	fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        idField: 'module_id',
    	columns: [[{
    		field: 'module_name',
    		width: 500,
    		title: '板块名称'
    	},{
    		field: 'image_url',
    		width: '80px',
    		title: '图标',
    		align: 'center',
    		formatter: function(val,obj,row){
    			return '<img src="'+val+'" style="width:40px;height:40px;"/>';
    		}
    	},{
    		field: 'module_id',
    		title: '排序',
    		width: '150px',
    		align: 'center',
            hidden: !(Wz.getPermission('discover/module/update_order')),
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" mid="'+val+'" class="table-cell-icon icon-top move-top-btn">&nbsp;</a><a href="javascript:void(0)" mid="'+val+'" class="table-cell-icon icon-up move-up-btn">&nbsp;</a><a href="javascript:void(0)" mid="'+val+'" class="table-cell-icon icon-down move-down-btn">&nbsp;</a><a href="javascript:void(0)" mid="'+val+'" class="table-cell-icon icon-bottom move-bottom-btn">&nbsp;</a>';
            }    		
    	},{
            field: 'state',
            title: '操作',
            width: 120,
            align: 'center',
            hidden: !(Wz.getPermission('discover/module/update')||Wz.getPermission('discover/module/set_state')||Wz.getPermission('discover/module/delete')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('discover/module/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.editModule('+row+')">编辑</a>':'')+
                (Wz.getPermission('discover/module/set_state')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.displayModule('+obj.module_id+',\''+val+'\')">'+(val=='NORMAL'?'禁用':'启用')+'</a>':'')+
                (Wz.getPermission('discover/module/delete')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.delModule('+obj.module_id+')">删除</a>':'');
            }
        }]],
    	toolbar: [{
            id: 'discover-module-add',
            text: '创建板块',
            iconCls: 'icon-add',
            disabled: !Wz.getPermission('discover/module/create'),
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=module_id]').val('');
            	uploadImge.reset();
            	allowModel.setValue({model_id:'',model_name:''});
    			editForm.find('input[name=web_url]').parents('.form-item').hide();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
    			editForm.find('input[textboxname=module_type]').combobox('enable');
            	editWin.window({title:'创建板块'}).window('open');
            }
        }],
    	loadFilter: function(data){
    		data = data.banner || data.rows || [];
    		return {
                total: data.length,
                rows: data
            };
    	}
    });
    moduleTable.datagrid({
    	view: detailview,
    	detailFormatter:function(index,row){
            return '<div class="discover-category-wrap"><table class="discover-category-talbe"></table></div>';
        },
        onExpandRow: function(index,row){
            var categoryTable = $(this).datagrid('getRowDetail',index).find('table.discover-category-talbe');
            categoryTable.datagrid({
                data:row.category,
                fitColumns:true,
                singleSelect:true,
                loadMsg:'',
                height:'auto',
                idField: 'category_id',
                columns:[[{
            		field: 'category_name',
            		width: 497,
            		title: '分类名称'
            	},{
            		field: 'cl',
            		width: '80px',
            		title: '课件列表',
            		align: 'center',
                    hidden: !(Wz.getPermission('discover/module/list_category_item')),
            		formatter: function(val,obj,row){
            			return '<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.showItems('+obj.category_id+')">查看课件</a>';
            		}
            	},{
            		field: 'category_id',
            		title: '排序',
            		width: '150px',
            		align: 'center',
                    hidden: !(Wz.getPermission('discover/module/update_category_order')),
                    formatter: function(val,obj,row){
                    	return '<a href="javascript:void(0)" mid="'+obj.module_id+'" cid="'+val+'" class="table-cell-icon icon-top move-top-btn">&nbsp;</a><a href="javascript:void(0)" mid="'+obj.module_id+'" cid="'+val+'" class="table-cell-icon icon-up move-up-btn">&nbsp;</a><a href="javascript:void(0)" mid="'+obj.module_id+'" cid="'+val+'" class="table-cell-icon icon-down move-down-btn">&nbsp;</a><a href="javascript:void(0)" mid="'+obj.module_id+'" cid="'+val+'" class="table-cell-icon icon-bottom move-bottom-btn">&nbsp;</a>';
                    }    		
            	},{
                    field: 'state',
                    title: '操作',
                    width: 120,
                    align: 'center',
                    hidden: !(Wz.getPermission('discover/module/update_category')||Wz.getPermission('discover/module/set_category_state')||Wz.getPermission('discover/module/delete_category')),
                    formatter: function(val,obj,row){
                        return (Wz.getPermission('discover/module/update_category')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.editCategory('+obj.module_id+','+obj.category_id+')">编辑</a>':'')+
                        (Wz.getPermission('discover/module/set_category_state')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.displayCategory('+obj.module_id+','+obj.category_id+',\''+val+'\')">'+(val=='NORMAL'?'禁用':'启用')+'</a>':'')+
                        (Wz.getPermission('discover/module/delete_category')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.delCategory('+obj.module_id+','+obj.category_id+')">删除</a>':'');
                    }
                }]],
            	toolbar: [{
                    id: 'exam-category-add',
                    text: '创建课件分类',
                    disabled: !Wz.getPermission('discover/module/create_category'),
                    iconCls: 'icon-add',
                    handler: function(){
                    	curCategoryTable = categoryTable;
                    	curCategoryIndex = index;
                    	categoryForm.form('reset');
                    	categoryForm.find('input[name=module_id]').val(row.module_id);
                    	categoryForm.find('input[name=category_id]').val('');
                    	categoryAllowModel.setValue({model_id:'',model_name:''});
                    	categoryWin.window({title:'创建课件分类'}).window('open');
                    }
                }],
                onResize:function(){
                	moduleTable.datagrid('fixDetailRowHeight',index);
                },
                onLoadSuccess:function(){
                    setTimeout(function(){
                    	moduleTable.datagrid('fixDetailRowHeight',index);
                    },0);
                }
            });
            $('#dg').datagrid('fixDetailRowHeight',index);
        }
    });
    
    var itemParam = {
    	category_id: '',
    	start: 0,
    	length: 1000
    };
    
    var itemListTable = $('#discover-category-itemlist-table').datagrid({
    	url: './api/discover/get_discover_item.json',
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
                return '<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.moduleManage.delItem('+val+')">移除</a>';
            }
        }]],
    	toolbar: [{
            id: 'discover-module-item-add',
            text: '添加课件',
            disabled: !Wz.getPermission('discover/module/add_category_item'),
            iconCls: 'icon-add',
            handler: function(){
            	selectListForm.form('reset');
            	selectListWin.find('input[name=category_id]').val(itemParam.category_id);
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
    var allListTable = $('#discover-allitem-table').datagrid({
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
    	toolbar: '#discover-allitem-talbe-tb',
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
    var selectedListTable = $('#discover-item-selected-table').datagrid({
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
    	var category_id = selectListWin.find('input[name=category_id]').val();
    	if(items.length == 0){
    		$.messager.alert('错误','请选择课件之后 保存！','error');
    		return false;
    	}
    	var item_id = [];
    	for(var i=0;i<items.length;i++){
    		item_id.push(items[i].item_id);
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: './api/discover/add_item_to_category.json',
    		data: {
    			item_id: item_id.join(','),
    			category_id: category_id
    		},
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				itemListTable.datagrid('reload');
    				selectListWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    });
    mainWrap.on('mouseup','.table-cell-icon',function(){
    	var module_id = $(this).attr('mid');
    	var category_id = $(this).attr('cid');
    	var module_index = moduleTable.datagrid('getRowIndex',module_id);
    	var module = moduleTable.datagrid('getRows')[module_index];
    	var category_talbe = null;
    	var category_index = 0;
    	var category = null;
    	if(!!category_id){
    		category_talbe = moduleTable.datagrid('getRowDetail',module_index).find('table.discover-category-talbe');
    		category_index = category_talbe.datagrid('getRowIndex',category_id);
    		category = category_talbe.datagrid('getRows')[category_index];
    	}
    	if($(this).hasClass('move-top-btn')){
			if(!!category){
				if(category_index > 0){
					category_talbe.datagrid('deleteRow',category_index);
					category_talbe.datagrid('insertRow',{index:0,row:category});
				}
			}else{
				if(module_index > 0){
					moduleTable.datagrid('deleteRow',module_index);
					moduleTable.datagrid('insertRow',{index:0,row:module});
				}
			}
    	}else if($(this).hasClass('move-bottom-btn')){
    		if(!!category){
				if(category_index < category_talbe.datagrid('getRows').length-1){
					category_talbe.datagrid('deleteRow',category_index);
					category_talbe.datagrid('appendRow',category);
				}
			}else{
				if(module_index < moduleTable.datagrid('getRows').length-1){
					moduleTable.datagrid('deleteRow',module_index);
					moduleTable.datagrid('appendRow',module);
				}
			}
    	}else if($(this).hasClass('move-up-btn')){
    		if(!!category){
				if(category_index > 0){
					category_talbe.datagrid('deleteRow',category_index);
					category_talbe.datagrid('insertRow',{index:category_index-1,row:category});
				}
			}else{
				if(module_index > 0){
					moduleTable.datagrid('deleteRow',module_index);
					moduleTable.datagrid('insertRow',{index:module_index-1,row:module});
				}
			}
    	}else if($(this).hasClass('move-down-btn')){
    		if(!!category){
				if(category_index < category_talbe.datagrid('getRows').length-1){
					category_talbe.datagrid('deleteRow',category_index);
					category_talbe.datagrid('insertRow',{index:category_index,row:category});
				}
			}else{
				if(module_index < moduleTable.datagrid('getRows').length-1){
					moduleTable.datagrid('deleteRow',module_index);
					moduleTable.datagrid('insertRow',{index:module_index+1,row:module});
				}
			}
    	}
    	var param = {};
    	var url = './api/discover/update_discover_module_order.json';
    	if(!!category){
    		url = './api/discover/update_discover_module_category_order.json';
    		param.module_id = module_id;
    		param.category_order_str = [];
    		var cdata = category_talbe.datagrid('getRows');
    		for(var i=0;i<cdata.length;i++){
    			param.category_order_str.push(cdata[i].category_id);
    		}
    		param.category_order_str = param.category_order_str.join(',');
    	}else{
    		param.module_order_str = [];
    		var mdata = moduleTable.datagrid('getRows');
    		for(var i=0;i<mdata.length;i++){
    			param.module_order_str.push(mdata[i].module_id);
    		}
    		param.module_order_str = param.module_order_str.join(',');
    	}
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: param,
    		success: function(json){
    			//不作处理
    		}
    	});
    });
    
    allitemSearchForm.find('.search-btn').click(function(){
    	allItemParam.item_name = allitemSearchForm.find('input[name=item_name]').val();
    	allListTable.datagrid('reload');
    });
    function editModule(row){
    	var module = $.extend({},moduleTable.datagrid('getRows')[row]);
		editForm.find('input[name=web_url]').parents('.form-item').hide();
		editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
		editForm.find('input[textboxname=module_type]').combobox('disable');
    	if(module.web_url == '{}'){
    		module.module_type == '0';
    	}else{
    		try{
    			var web_url = JSON.parse(module.web_url);    			
    		}catch(e){
    			web_rul = {web_url:'',is_weizhu:false};
    		}
    		if(web_url.web_url.indexOf('mobile/survey/survey_list.html')>-1){
    			module.module_type = '4';
    		}else if(web_url.web_url.indexOf('mobile/qa/qa_info.html')>-1){
    			module.module_type = '3';
    		}else if(web_url.web_url.indexOf('mobile/exam/exam_list.html')>-1){
    			module.module_type = '2';
    		}else if(web_url.web_url.indexOf('mobile/offline_training/training_list.html')>-1){
    			module.module_type = '5';
    		}else{
    			module.module_type = '1';
    			editForm.find('input[name=web_url]').parents('.form-item').show();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').show();
    		}
    		module.web_url = web_url.web_url;
    		module.is_weizhu = web_url.is_weizhu;
    	}
    	editForm.form('load',module);
    	editForm.find('input[name=is_weizhu]').prop('checked',!!module.is_weizhu);
    	uploadImge.setValue(module.image_name,module.image_url);
    	allowModel.setValue({model_id:module.allow_model_id,model_name:module.allow_model_name});
    	editWin.window({title:'修改板块'}).window('open');
    }
    
    function editCategory(module_id,category_id){
    	curCategoryIndex = moduleTable.datagrid('getRowIndex',module_id);
    	curCategoryTable = moduleTable.datagrid('getRowDetail',curCategoryIndex).find('table.discover-category-talbe');
    	var categoryIndex = curCategoryTable.datagrid('getRowIndex',category_id);
    	var category = curCategoryTable.datagrid('getRows')[categoryIndex];
    	categoryForm.form('load',category);
    	categoryAllowModel.setValue({model_id:category.allow_model_id,model_name:category.allow_model_name});
    	categoryWin.window({title:'修改课件分类'}).window('open');
    }
    
    function delModule(module_id){
    	var index = moduleTable.datagrid('getRowIndex',module_id);
    	var module = moduleTable.datagrid('getRows')[index];
    	var msg = '请确认是否删除该板块？';
    	if(module.category.length > 0){
    		msg = '该板块删除后会一并删除其下边的分类，请确认是否删除？'
    	}
		$.messager.confirm({
			title: '请确认',
			msg: msg,
			ok: '确认',
			cancel: '取消',
			fn: function(ok){
				if(ok){
					Wz.ajax({
						type: 'post',
						url: './api/discover/delete_discover_module.json',
						data: {
							module_id: module_id
						},
						success: function(json){
							if(json.result == 'SUCC'){
								moduleTable.datagrid('deleteRow',index);
							}else{
								$.messager.alert('错误',json.fail_text,'error');
							}
						}
					});
				}
			}
		});
    }
    function delCategory(module_id,category_id){
    	curCategoryIndex = moduleTable.datagrid('getRowIndex',module_id);
    	var module = moduleTable.datagrid('getRows')[curCategoryIndex];
    	var category_index = moduleTable.datagrid('getRowIndex',module_id);
    	curCategoryTable = moduleTable.datagrid('getRowDetail',curCategoryIndex).find('table.discover-category-talbe');
    	var category_index = curCategoryTable.datagrid('getRowIndex',category_id);
    	var category = curCategoryTable.datagrid('getRows')[category_index];
		$.messager.confirm({
			title: '请确认',
			msg: '请确认删除该课程分类吗？',
			ok: '确认',
			cancel: '取消',
			fn: function(ok){
				if(ok){
					Wz.ajax({
						type: 'post',
						url: './api/discover/delete_discover_module_category.json',
						data: {
							category_id: category_id
						},
						success: function(json){
							if(json.result == 'SUCC'){
								curCategoryTable.datagrid('deleteRow',category_index);
								moduleTable.datagrid('fixDetailRowHeight',curCategoryIndex);
							}else{
								$.messager.alert('错误',json.fail_text,'error');
							}
						}
					});
				}
			}
		});
    }
    
    function saveModule(){
    	var module_id = editForm.find('input[name=module_id]').val();
    	var module_name = editForm.find('input[textboxname=module_name]').textbox('getValue');
    	var module_type = editForm.find('input[textboxname=module_type]').combobox('getValue');
		var image_name = uploadImge.getValue();
		var image_url = uploadImge.getUrl();
		var allow_model_id = allowModel.getValue();
		var allow_model_name = allowModel.getName();
		var prompt_dot = editForm.find('input[name=prompt_dot]').prop('checked');
		var web_url = editForm.find('input[textboxname=web_url]').textbox('getValue');
		var is_weizhu = editForm.find('input[name=is_weizhu]').prop('checked');
    	var url = './api/discover/create_discover_module.json';
    	var param = {
    		module_id: module_id,
    		module_name: module_name,
    		prompt_dot: prompt_dot,
    		allow_model_id: allow_model_id,
    		image_name: image_name
    	};
    	if(!editForm.find('input[textboxname=module_name]').textbox('isValid')){
    		$.messager.alert('错误','请输入板块名称！','error');
    		return false;
    	}
    	if(image_name == ''){
    		$.messager.alert('错误','请上传板块图标！','error');
    		return false;
    	}
    	if(module_type == '1'){
    		if(!editForm.find('input[textboxname=web_url]').textbox('isValid')){
    			$.messager.alert('错误','请输入正确的链接地址！','error');
        		return false;
    		}
    		param.web_url = JSON.stringify({web_url:web_url,is_weizhu:is_weizhu});
    	}else if(module_type == '2'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/exam/exam_list.html',is_weizhu:true});
    	}else if(module_type == '3'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/qa/qa_info.html',is_weizhu:true});
    	}else if(module_type == '4'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/survey/survey_list.html',is_weizhu:true});
    	}else if(module_type == '5'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/offline_training/training_list.html',is_weizhu:true});
    	}
    	if(module_id != ''){
    		url = './api/discover/update_discover_module.json';
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: param,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				var module = {
    					module_id: json.module_id,
    					module_name: module_name,
    					image_name: image_name,
    					image_url: image_url,
    					state: 'DISABLE',
    					web_url: param.web_url||'{}',
    					category: [],
						allow_model_name: allow_model_name,
						allow_model_id: allow_model_id
    				};
    				if(module_id == ''){
    					moduleTable.datagrid('appendRow',module);
    				}else{
    					var index = moduleTable.datagrid('getRowIndex',module_id);
    					var oldModule = moduleTable.datagrid('getRows')[index];
    					module.category = oldModule.category||[];
    					module.state = oldModule.state;
    					moduleTable.datagrid('updateRow',{
    						index: index,
    						row: module
    					});
    				}
    				editWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    }
    
    function saveCategory(){
    	var module_id = categoryForm.find('input[name=module_id]').val();
    	var category_id = categoryForm.find('input[name=category_id]').val();
    	var category_name = categoryForm.find('input[textboxname=category_name]').textbox('getValue');
		var allow_model_id = categoryAllowModel.getValue();
		var allow_model_name = categoryAllowModel.getName();
    	var url = './api/discover/create_discover_module_category.json';
    	if(category_id != ''){
    		url = './api/discover/update_discover_module_category.json';
    	}
    	categoryForm.form('submit',{
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
    			result = JSON.parse(result);
    			if(result.result == 'SUCC'){
    				var category = {
    					module_id: module_id,
    					category_id: result.category_id,
    					category_name: category_name,
    					state: 'DISABLE',
						allow_model_name: allow_model_name,
						allow_model_id: allow_model_id
    				};
    				if(category_id == ''){
    					curCategoryTable.datagrid('appendRow',category);
    					moduleTable.datagrid('fixDetailRowHeight',curCategoryIndex);
    				}else{
    					var index = curCategoryTable.datagrid('getRowIndex',category_id);
    					var oldCategory = curCategoryTable.datagrid('getRows')[index];
    					category.state = oldCategory.state;
    					curCategoryTable.datagrid('updateRow',{
    						index: index,
    						row: category
    					});
    				}
    				categoryWin.window('close');	
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function displayModule(module_id,state){
    	var index = moduleTable.datagrid('getRowIndex',module_id);
    	var module = $.extend({},moduleTable.datagrid('getRows')[index]);
    	var url = './api/discover/disable_discover_module.json';
    	var msg = '板块禁用后，该板块及其涵盖内容将不再出现在APP端，用户将无法查看，确定停用板块吗？';
    	if(state == 'DISABLE'){
    		msg = '板块启用后APP端将可以正常显示该板块，请确认是否启用？';
        	url = './api/discover/display_discover_module.json';
        	module.state = 'NORMAL';
    	}else{
    		module.state = 'DISABLE';
    	}
    	$.messager.confirm({
			title: '请确认',
			msg: msg,
			fn: function(ok){
				if(ok){
					Wz.ajax({
						type: 'post',
						url: url,
						data: {
							module_id: module_id
						},
						success: function(json){
							if(json.result == 'SUCC'){
								moduleTable.datagrid('updateRow',{
									index: index,
									row: module
								});
							}else{
								$.messager.alert('错误',json.fail_text,'error');
							}
						}
					});
				}
			}
		});
    }
    function displayCategory(module_id,category_id,state){
		curCategoryIndex = moduleTable.datagrid('getRowIndex',module_id);
    	var module = moduleTable.datagrid('getRows')[curCategoryIndex];
    	var category_index = moduleTable.datagrid('getRowIndex',module_id);
    	curCategoryTable = moduleTable.datagrid('getRowDetail',curCategoryIndex).find('table.discover-category-talbe');
    	var category_index = curCategoryTable.datagrid('getRowIndex',category_id);
    	var category = $.extend({},curCategoryTable.datagrid('getRows')[category_index]);
    	var url = './api/discover/disable_discover_module_category.json';
    	var msg = '分类禁用后，该分类及其涵盖内容将不再出现在APP端，用户将无法查看，确定停用分类吗？';
    	if(state == 'DISABLE'){
    		msg = '分类启用后APP端将可以正常显示该分类，请确认是否启用？';
        	url = './api/discover/display_discover_module_category.json';
        	category.state = 'NORMAL';
    	}else{
    		category.state = 'DISABLE';
    	}
    	$.messager.confirm({
			title: '请确认',
			msg: msg,
			fn: function(ok){
				if(ok){
					Wz.ajax({
						type: 'post',
						url: url,
						data: {
							category_id: category_id
						},
						success: function(json){
							if(json.result == 'SUCC'){
								curCategoryTable.datagrid('updateRow',{
									index: category_index,
									row: category
								});
							}else{
								$.messager.alert('错误',json.fail_text,'error');
							}
						}
					});
				}
			}
		});
    }
    
    function showItems(category_id){
    	itemParam.category_id = category_id;
    	itemListTable.datagrid('reload');
    	itemListWin.window('open');
    }
    
    function delItem(item_id){
    	$.messager.confirm({
			title: '请确认',
			msg: '请确认是否移除该课件？',
			fn: function(ok){
				if(ok){
					Wz.ajax({
						type: 'post',
						url: './api/discover/delete_discover_item_from_category.json',
						data: {
							category_id: itemParam.category_id,
							item_id: item_id
						},
						success: function(json){
							if(json.result == 'SUCC'){
						    	itemListTable.datagrid('reload');
							}else{
								$.messager.alert('错误',json.fail_text,'error');
							}
						}
					});
				}
			}
		});
    }
    
    return {
    	delItem: delItem,
    	showItems: showItems,
    	delCategory: delCategory,
    	delModule: delModule,
    	saveModule: saveModule,
    	editModule: editModule,
    	saveCategory: saveCategory,
    	editCategory: editCategory,
    	displayModule: displayModule,
    	displayCategory: displayCategory
    };
}()

