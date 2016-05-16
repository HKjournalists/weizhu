/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 发现管理-课件管理
 */
Wz.namespace('Wz.discover');
Wz.discover.itemManage = function(){
    $.parser.parse('#main-contain');
    var query_params = {
    	category_id: '',
    	item_name: ''
    };
    var curParam = {
    	item_id: ''
    };
    var searchForm = $('#discover-item-search-form');
    var editWin = $('#discover-item-edit-win');
    var editForm = $('#discover-item-edit-form');
    var detailWin = $('#discover-item-detail-win');
    var statWin = $('#discover-item-stat-win');
    
    var searchCategory = searchForm.find('input[name=category_id]').combotree({
    	mode: 'remote',
    	editable: false,
    	cascadeCheck: false,
    	height: 'auto',
    	onSelect : function(node) {
            var tree = $(this).tree;
            var isLeaf = tree('isLeaf', node.target);
            if (!isLeaf) {
            	searchCategory.combotree('clear');
            	return false;
            }
        },
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/discover/get_discover_module.json',
				success: function(json){
					var data = json.banner;
					var result = [];
        			for(var i=0;i<data.length;i++){
        				data[i].id = 'm_'+ data[i].module_id;
        				data[i].text = data[i].module_name;
        				if(data[i].category.length > 0){
            				var children = [];
        					var categorys = data[i].category;
        					for(var j=0;j<categorys.length;j++){
        						children.push({
        							id: categorys[j].category_id,
        							text: categorys[j].category_name
        						});
        					}
        					data[i].children = children;
        					result.push(data[i]);
        				}
        			}
					success(result);
				}
			});
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });
        
    var uploadImage = editForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 430,
    	tipInfo: '图片格式必须为:png,jpg,gif；<br/>图片质量不可大于1M,图片尺寸建议640*320(px)',
    	maxSize: 1,
    	params: {
    		image_tag: '社区,帖子'
    	}
    });
    var audioUpload = editForm.find('input[name=audio]').uploadfile({
    	url: '../upload/api/admin/upload_discover_audio.json',
    	name: 'upload_file',
    	supportType: ['mp3','avi'],
    	wrapWidth: 420,
    	maxSize: 40,
    	formatter: function(data){
    		return {
    			file_url: data.audio_url,
    			file_type: data.audio_type,
    			file_size: data.audio_size,
    			time: data.audio_time,
    			check_md5: data.check_md5,
    			is_auth_url: data.is_auth_url
    		};
    	},
    	resultFormatter: function(data){
    		var result = {
    			audio_url: data.file_url,
    			audio_type: data.file_name.match(/\.(\w+)$/)[1],
    			audio_size: data.file_size,
    			audio_time: data.time,
    			check_md5: data.check_md5,
    			is_auth_url: data.is_auth
    		};
    		return JSON.stringify(result);
    	}
    });
    var videoUpload = editForm.find('input[name=video]').uploadfile({
    	url: '../upload/api/admin/upload_discover_video.json',
    	name: 'upload_file',
    	supportType: ['mp4'],
    	wrapWidth: 420,
    	maxSize: 40,
    	formatter: function(data){
    		return {
    			file_url: data.video_url,
    			file_type: data.video_type,
    			file_size: data.video_size,
    			time: data.video_time,
    			check_md5: data.check_md5,
    			is_auth_url: data.is_auth_url
    		};
    	},
    	resultFormatter: function(data){
    		var result = {
    			video_url: data.file_url,
    			video_type: data.file_name.match(/\.(\w+)$/)[1],
    			video_size: data.file_size,
    			video_time: data.time,
    			check_md5: data.check_md5,
    			is_auth_url: data.is_auth
    		};
    		return JSON.stringify(result);
    	}
    });
    var documentUpload = editForm.find('input[name=document]').uploadfile({
    	url: '../upload/api/admin/upload_discover_document.json',
    	name: 'upload_file',
    	supportType: ['pdf'],
    	wrapWidth: 420,
    	maxSize: 40,
    	formatter: function(data){
    		return {
    			file_url: data.document_url,
    			file_type: data.document_type,
    			file_size: data.document_size,
    			check_md5: data.check_md5,
    			is_auth_url: data.is_auth_url
    		};
    	},
    	resultFormatter: function(data){
    		var result = {
    			document_url: data.file_url,
    			document_type: data.file_name.match(/\.(\w+)$/)[1],
    			document_size: data.file_size,
    			check_md5: data.check_md5,
    			is_auth_url: data.is_auth
    		};
    		return JSON.stringify(result);
    	}
    });
    editForm.find('input[name=item_type]').combobox({
    	width:350,
    	valueField:'value',
    	value:'0',
    	textField:'name',
    	data:[{name:'链接',value:'0'},{name:'PDF文档',value:'1'},{name:'视频',value:'2'},{name:'音频',value:'3'}],
    	editable:false,
    	panelHeight:'auto',
    	onChange: function(newValue,oldValue){
    		if(newValue == '0'){
    			editForm.find('input[name=web_url]').parents('.discover-item-content').show();
    			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
    			editForm.find('input[name=video]').parents('.discover-item-content').hide();
    			editForm.find('input[name=document]').parents('.discover-item-content').hide();
    			editForm.find('input[name=is_download]').parents('.form-checkbox').hide();
    		}else if(newValue == '1'){
    			editForm.find('input[name=web_url]').parents('.discover-item-content').hide();
    			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
    			editForm.find('input[name=video]').parents('.discover-item-content').hide();
    			editForm.find('input[name=document]').parents('.discover-item-content').show();
    			editForm.find('input[name=is_download]').parents('.form-checkbox').show();
    		}else if(newValue == '2'){
    			editForm.find('input[name=web_url]').parents('.discover-item-content').hide();
    			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
    			editForm.find('input[name=video]').parents('.discover-item-content').show();
    			editForm.find('input[name=document]').parents('.discover-item-content').hide();
    			editForm.find('input[name=is_download]').parents('.form-checkbox').show();
    		}else if(newValue == '3'){
    			editForm.find('input[name=web_url]').parents('.discover-item-content').hide();
    			editForm.find('input[name=audio]').parents('.discover-item-content').show();
    			editForm.find('input[name=video]').parents('.discover-item-content').hide();
    			editForm.find('input[name=document]').parents('.discover-item-content').hide();
    			editForm.find('input[name=is_download]').parents('.form-checkbox').show();
    		}
    	}
    });
    var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
    searchForm.find('.search-btn').click(function(){
    	query_params.category_id = searchCategory.combotree('getValue');
    	query_params.item_name = searchForm.find('input[name=item_name]').val();
    	itemTable.datagrid('reload');
    });
    searchForm.find('.reset-btn').click(function(){
    	searchForm.form('reset');
    });
    var itemTable = $('#discover-item-table').datagrid({
        url: './api/discover/get_discover_item.json',
        queryParams: query_params,
        checkOnSelect: false,
        striped: true,
        fit:true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        idField: 'item_id',
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'item_name',
            title: '标题',
            width: '40%',
            align: 'left'
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
            field: 'create_time',
            title: '创建时间',
            width: '15%',
            align: 'center'
        },{
            field: 'item_id',
            title: '操作',
            align: 'center',
            width: '18%',
            hidden: !(Wz.getPermission('itemManage.js')||Wz.getPermission('discover/item/set_state')||Wz.getPermission('discover/item/delete')||Wz.getPermission('discover/item/show_detail')),
            formatter: function(val,obj,row){
            	var state = '禁用';
            	if(obj.state == 'DISABLE'){
            		state = '启用';
            	}
                return (Wz.getPermission('discover/item/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.itemManage.editItem('+row+')">编辑</a>':'') +
                (Wz.getPermission('discover/item/set_state')?'<a href="javascript:void(0)" class="tablelink state-btn" onclick="Wz.discover.itemManage.changeState('+row+')">'+state+'</a>':'') +
                (Wz.getPermission('discover/item/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.discover.itemManage.delItem('+val+')">删除</a>':'') +
                (Wz.getPermission('discover/item/show_detail')?'<a href="javascript:void(0)" class="tablelink" onclick="Wz.discover.itemManage.showItem('+row+')">查看详情</a>':'');
            }
        }]],
        toolbar: [{
            id: 'discover-item-create',
            text: '新建',
            iconCls: 'icon-add',
            disabled: !Wz.getPermission('discover/item/create'),
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=item_id]').val('');
            	editForm.find('input[name=web_url]').parents('.discover-item-content').show();
    			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
    			editForm.find('input[name=video]').parents('.discover-item-content').hide();
    			editForm.find('input[name=document]').parents('.discover-item-content').hide();
    			itemCategoryTree.tree('reload');
    			uploadImage.reset();
    			audioUpload.reset();
    			videoUpload.reset();
    			documentUpload.reset();
    			allowModel.setValue({model_id:'',model_name:''});
            	editWin.window('open');
            }
        },{
            id: 'discover-item-del',
            text: '删除',
            iconCls: 'icon-remove',
            disabled: !Wz.getPermission('discover/item/delete'),
            handler: function(){
            	var selects = itemTable.datagrid('getChecked');
            	if(selects.length == 0){
            		$.messager.alert('提示','请选择需要删除的课件！','info');
            		return false;
            	}
            	var item_id = [];
            	for(var i=0;i<selects.length;i++){
            		item_id.push(selects[i].item_id);
            	}
            	delItem(item_id.join(','));
            }
        },{
            id: 'discover-item-export',
            text: '导出搜索课件',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('discover/item/export'),
            handler: function(){
            	var btn = $(this);
            	btn.linkbutton('disable');
            	setTimeout(function(){
            		btn.linkbutton('enable');
            	},10000)
            	var param = [];
            	for(var name in query_params){
            		param.push(name + '=' + query_params[name]);
            	}
            	Wz.downloadFile("./api/discover/export_discover_item.json?"+param.join('&')+"&_t=" + new Date().getTime());
            }
        }],
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
        	var items = data.item;
        	for(var i=0;i<items.length;i++){
        		for(var n in items[i].base){
        			items[i][n] = items[i].base[n];
        		}
        		items[i].content = JSON.parse(items[i].content);
        	}
            return {
                total: data.filtered_size,
                rows: items
            };
        }
    });
    
    var itemCategoryTree = $('#discover-item-category-tree').tree({
    	mode: 'remote',
    	cascadeCheck: true,
    	height: 'auto',
    	checkbox: true,
    	onlyLeafCheck: true,
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/discover/get_discover_module.json',
				success: function(json){
					var data = json.banner;
					var result = [];
        			for(var i=0;i<data.length;i++){
        				data[i].id = 'm_'+ data[i].module_id;
        				data[i].text = data[i].module_name;
        				if(data[i].category.length > 0){
            				var children = [];
        					var categorys = data[i].category;
        					for(var j=0;j<categorys.length;j++){
        						children.push({
        							id: categorys[j].category_id,
        							text: categorys[j].category_name
        						});
        					}
        					data[i].children = children;
        					result.push(data[i]);
        				}
        			}
					success(result);
				}
			});
		},
    });    
    
    var scoreTable = $('#discover-item-score-table').datagrid({
    	url: './api/discover/get_discover_item_score.json',
        queryParams: curParam,
        striped: true,
        fit:true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name',
            title: '评分用户',
            width: '25%',
            align: 'left'
        },{
            field: 'score_number',
            title: '评分分数',
            width: '30%',
            align: 'right'
        },{
            field: 'like_time',
            title: '点赞时间',
            width: '30%',
            align: 'center'
        }]],
        toolbar: [{
            id: 'discover-item-score-export',
            text: '导出',
            disabled: !Wz.getPermission('discover/item/export_score'),
            iconCls: 'icon-export',
            handler: function(){
            	var btn = $(this);
            	btn.linkbutton('disable');
            	setTimeout(function(){
            		btn.linkbutton('enable');
            	},10000);
            	var param = [];
            	for(var name in curParam){
            		param.push(name + '=' + curParam[name]);
            	}
            	Wz.downloadFile("./api/discover/export_discover_item_score.json?"+param.join('&')+"&_t=" + new Date().getTime());
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
                rows: data.item_score
            };
        }
    });
    var studyTable = $('#discover-item-study-table').datagrid({
    	url: './api/discover/get_discover_item_learn.json',
        queryParams: curParam,
        striped: true,
        fit:true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name',
            title: '学习用户',
            width: '20%',
            align: 'left'
        },{
            field: 'learn_duration',
            title: '累计学习时间（秒）',
            width: '20%',
            align: 'right'
        },{
            field: 'learn_time',
            title: '最近学习时刻',
            width: '25%',
            align: 'center'
        },{
            field: 'learn_cnt',
            title: '学习次数',
            width: '20%',
            align: 'right'
        }]],
        toolbar: [{
            id: 'discover-item-study-export',
            text: '导出',
            disabled: !Wz.getPermission('discover/item/export_learn'),
            iconCls: 'icon-export',
            handler: function(){
            	var btn = $(this);
            	btn.linkbutton('disable');
            	setTimeout(function(){
            		btn.linkbutton('enable');
            	},10000);
            	var param = [];
            	for(var name in curParam){
            		param.push(name + '=' + curParam[name]);
            	}
            	Wz.downloadFile("./api/discover/export_discover_item_learn.json?"+param.join('&')+"&_t=" + new Date().getTime());
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
                rows: data.item_learn
            };
        }
    });
    var commentTable = $('#discover-item-comment-table').datagrid({
    	url: './api/discover/get_discover_item_comment.json',
        queryParams: curParam,
        striped: true,
        fit:true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name',
            title: '评论用户',
            width: '14%',
            align: 'left'
        },{
            field: 'comment_text',
            title: '评论内容',
            width: '40%',
            align: 'left'
        },{
            field: 'comment_time',
            title: '评论时间',
            width: '20%',
            align: 'center'
        },{
            field: 'is_delete',
            title: '是否被删除',
            width: '10%',
            align: 'center',
            formatter: function(val,obj,row){
            	return val?'是':'否';
            }
        }]],
        toolbar: [{
            id: 'discover-item-comment-export',
            text: '导出',
            disabled: !Wz.getPermission('discover/item/export_comment'),
            iconCls: 'icon-export',
            handler: function(){
            	var btn = $(this);
            	btn.linkbutton('disable');
            	setTimeout(function(){
            		btn.linkbutton('enable');
            	},10000);
            	var param = [];
            	for(var name in curParam){
            		param.push(name + '=' + curParam[name]);
            	}
            	Wz.downloadFile("./api/discover/export_discover_item_comment.json?"+param.join('&')+"&_t=" + new Date().getTime());
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
                rows: data.item_comment
            };
        }
    });
    var likeTable = $('#discover-item-like-table').datagrid({
    	url: './api/discover/get_discover_item_like.json',
        queryParams: curParam,
        striped: true,
        fit:true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name',
            title: '点赞用户',
            width: '50%',
            align: 'left'
        },{
            field: 'like_time',
            title: '点赞时间',
            width: '40%',
            align: 'center'
        }]],
        toolbar: [{
            id: 'discover-item-like-export',
            text: '导出',
            disabled: !Wz.getPermission('discover/item/export_like'),
            iconCls: 'icon-export',
            handler: function(){
            	var btn = $(this);
            	btn.linkbutton('disable');
            	setTimeout(function(){
            		btn.linkbutton('enable');
            	},10000);
            	var param = [];
            	for(var name in curParam){
            		param.push(name + '=' + curParam[name]);
            	}
            	Wz.downloadFile("./api/discover/export_discover_item_like.json?"+param.join('&')+"&_t=" + new Date().getTime());
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
                rows: data.item_like
            };
        }
    });
    
    var statTab = $('#discover-item-stat-tabs').tabs({
    	justified: true,
    	narrow: true,
    	plain: true,
    	onSelect: function(title,index){
    		if(index == 0){
    			likeTable.datagrid('reload');
    		}else if(index == 1){
    			scoreTable.datagrid('reload');
    		}else if(index == 2){
    			studyTable.datagrid('reload');
    		}else if(index == 3){
    			commentTable.datagrid('reload');
    		}
    	}  
    });
    function editItem(row){
    	editForm.form('reset');
    	var checked = itemCategoryTree.tree('getChecked')||[];
    	for(var i=0;i<checked.length;i++){
    		itemCategoryTree.tree('uncheck',checked[i].target);
    	}
    	var item = $.extend({},itemTable.datagrid('getRows')[row]);
    	for(var i=0;i<item.category.length;i++){
    		itemCategoryTree.tree('check',itemCategoryTree.tree('find',item.category[i].category_id).target);
    	}
    	if(typeof item.content.web_url != 'undefined'){
    		item.web_url = item.content.web_url;
    		item.is_weizhu = item.content.is_weizhu;
    		item.item_type = '0';
    		editForm.find('input[name=web_url]').parents('.discover-item-content').show();
			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
			editForm.find('input[name=video]').parents('.discover-item-content').hide();
			editForm.find('input[name=document]').parents('.discover-item-content').hide();
			editForm.find('input[name=is_download]').parents('.form-checkbox').hide();
    	}else if(typeof item.content.audio_url != 'undefined'){
    		audioUpload.setValue(item.content);
    		item.is_download = item.content.is_download
    		item.item_type = '3';
    		editForm.find('input[name=web_url]').parents('.discover-item-content').hide();
			editForm.find('input[name=audio]').parents('.discover-item-content').show();
			editForm.find('input[name=video]').parents('.discover-item-content').hide();
			editForm.find('input[name=document]').parents('.discover-item-content').hide();
			editForm.find('input[name=is_download]').parents('.form-checkbox').show();
    	}else if(typeof item.content.video_url != 'undefined'){
    		videoUpload.setValue(item.content);
    		item.is_download = item.content.is_download
    		item.item_type = '2';
    		editForm.find('input[name=web_url]').parents('.discover-item-content').hide();
			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
			editForm.find('input[name=video]').parents('.discover-item-content').show();
			editForm.find('input[name=document]').parents('.discover-item-content').hide();
			editForm.find('input[name=is_download]').parents('.form-checkbox').show();
    	}else if(typeof item.content.document_url != 'undefined'){
    		documentUpload.setValue(item.content);
    		item.is_download = item.content.is_download
    		item.item_type = '1';
    		editForm.find('input[name=web_url]').parents('.discover-item-content').hide();
			editForm.find('input[name=audio]').parents('.discover-item-content').hide();
			editForm.find('input[name=video]').parents('.discover-item-content').hide();
			editForm.find('input[name=document]').parents('.discover-item-content').show();
			editForm.find('input[name=is_download]').parents('.form-checkbox').show();
    	}
    	editForm.form('load',item);
    	editForm.find('input[name=enable_comment]').prop('checked',item.enable_comment);
    	editForm.find('input[name=enable_external_share]').prop('checked',item.enable_external_share);
    	editForm.find('input[name=enable_like]').prop('checked',item.enable_like);
    	editForm.find('input[name=enable_score]').prop('checked',item.enable_score);
    	editForm.find('input[name=enable_share]').prop('checked',item.enable_share);
    	editForm.find('input[name=is_download]').prop('checked',item.is_download);
    	uploadImage.setValue(item.image_name,item.image_url);
    	allowModel.setValue({model_id:item.allow_model_id,model_name:item.allow_model_name});
    	editWin.window('open');
    }
    
    function saveItem(){
    	var item_id = editForm.find('input[name=item_id]').val();
    	var item_name = editForm.find('input[name=item_name]').val();
    	var image_name = uploadImage.getValue();
    	var item_desc = editForm.find('input[name=item_desc]').val();
    	var category_id = [];
    	var selectes = itemCategoryTree.tree('getChecked');
    	for(var i=0;i<selectes.length;i++){
    		category_id.push(selectes[i].id);
    	}
    	var item_type = editForm.find('input[name=item_type]').val();
    	var enable_score = editForm.find('input[name=enable_score]').prop('checked');
    	var enable_comment = editForm.find('input[name=enable_comment]').prop('checked');
    	var enable_external_share = editForm.find('input[name=enable_external_share]').prop('checked');
    	var enable_like = editForm.find('input[name=enable_like]').prop('checked');
    	var enable_share = editForm.find('input[name=enable_share]').prop('checked');
    	var is_download = editForm.find('input[name=is_download]').prop('checked');
    	var allow_model_id = allowModel.getValue();
    	var url = './api/discover/create_discover_item.json';
    	var param = {
    		item_name: item_name,
    		image_name: image_name,
    		item_desc: item_desc,
    		allow_model_id: allow_model_id,
    		enable_comment: enable_comment,
    		enable_score: enable_score,
    		enable_remind: false,
    		enable_like: enable_like,
    		enable_share: enable_share,
    		enable_external_share: enable_external_share,
    		category_id: category_id.join(',')
    	};
    	if(image_name == ''){
    		$.messager.alert('错误','请上传封面图片！','error');
    		return false;
    	}
    	if(!editForm.find('input[textboxname=item_name]').textbox('isValid')){
    		$.messager.alert('错误','请正确填写课件标题！','error');
    		return false;
    	}
    	
    	if(item_type == '0'){
    		if(!editForm.find('input[textboxname=web_url]').textbox('isValid')){
    			$.messager.alert('错误','请输入正确的链接地址！','error');
        		return false;
    		}
    		var web_url = editForm.find('input[name=web_url]').val();
    		var is_weizhu = editForm.find('input[name=is_weizhu]').prop('checked');
    		param.web_url = JSON.stringify({web_url:web_url,is_weizhu:is_weizhu});
    	}else if(item_type == '1'){
    		var document = documentUpload.getValue();
    		if(document == ''){
    			$.messager.alert('错误','请上传PDF文档！','error');
        		return false;
    		}
    		document = JSON.parse(document);
    		document.is_download = is_download;
    		param.document = JSON.stringify(document);
    	}else if(item_type == '2'){
    		var video = videoUpload.getValue();
    		if(video == ''){
    			$.messager.alert('错误','请上传视频！','error');
        		return false;
    		}
    		video = JSON.parse(video);
    		video.is_download = is_download;
    		param.video = JSON.stringify(video);
    	}else if(item_type == '3'){
    		var audio = audioUpload.getValue();
    		if(audio == ''){
    			$.messager.alert('错误','请上传视频！','error');
        		return false;
    		}
    		audio = JSON.parse(audio);
    		audio.is_download = is_download;
    		param.audio = JSON.stringify(audio);
    	}
    	if(item_id != ''){
    		param.item_id = item_id;
    		url = './api/discover/update_discover_item.json';
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: param,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				itemTable.datagrid('reload');
    				editWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    }
    
    function delItem(item_id){
    	$.messager.confirm('提示','请确认是否要删除课件？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/discover/delete_discover_item.json',
    				data: {
    					item_id: item_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						itemTable.datagrid('reload');
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
    	var item = itemTable.datagrid('getRows')[row];
    	var msg = '请确认是否禁用该课件？';
    	var url = './api/discover/disable_discover_item.json';
    	if(item.state == 'DISABLE'){
    		msg = '请确认是否启用该课件?';
    		url = './api/discover/display_discover_item.json';
    	}
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: url,
    				data: {
    					item_id: item.item_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						itemTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    function showItem(row){
    	var item = itemTable.datagrid('getRows')[row];
    	curParam.item_id = item.item_id;
    	Wz.ajax({
    		url: './api/discover/get_discover_item_by_ids.json',
    		data: curParam,
    		success: function(json){
    			if(json.item.length == 0){
    				$.messager.show({
		                title:'错误',
		                msg:'您查看的课件已不存在！',
		                showType:'fade',
		                style:{
		                    right:'',
		                    top:document.body.scrollTop+document.documentElement.scrollTop,
		                    bottom:''
		                },
		                timeout: 1000
		            });
    				itemTable.datagrid('reload');
    			}else{
    				loadDetailWin(json.item[0]);
    		    	detailWin.window('open');
    			}
    		}
    	});
    }
    
    function loadDetailWin(item){
    	var count = JSON.parse(item.count);
    	var base = item.base;
    	var category = item.category;
    	var content = JSON.parse(base.content);
    	
    	detailWin.find('.discover-item-like-count i').text(count.like_cnt);
    	detailWin.find('.discover-item-score-count i').text(count.score_number);
    	detailWin.find('.discover-item-study-count i').text(count.learn_cnt);
    	detailWin.find('.discover-item-comment-count i').text(count.comment_cnt);
    	detailWin.find('.discover-item-score-people i').text(count.score_user_cnt);
    	detailWin.find('.discover-item-study-people i').text(count.learn_user_cnt);
    	detailWin.find('.discover-item-comment-people i').text(count.comment_user_cnt);
    	
    	var category_list = [];
    	for(var i=0;i<category.length;i++){
    		category_list.push(['<li class="discover-item-category-item"><div class="discover-item-category-icon"><img src="',category[i].module.image_url,
    			'"></div><div class="discover-item-category-info"><span>',category[i].module.module_name,'</span><span>',category[i].category_name,'</span></div></li>'].join(''));
    	}
    	detailWin.find('.discover-item-category-list').html(category_list.join(''));
    	
    	if(base.enable_score){
    		detailWin.find('.discover-item-setting-item .icon-score').parent().addClass('active'); 		
    	}else{
    		detailWin.find('.discover-item-setting-item .icon-score').parent().removeClass('active');    		
    	}
    	
    	if(base.enable_comment){
    		detailWin.find('.discover-item-setting-item .icon-comment').parent().addClass('active'); 		
    	}else{
    		detailWin.find('.discover-item-setting-item .icon-comment').parent().removeClass('active');    		
    	}
    	
    	if(base.enable_like){
    		detailWin.find('.discover-item-setting-item .icon-like').parent().addClass('active'); 		
    	}else{
    		detailWin.find('.discover-item-setting-item .icon-like').parent().removeClass('active');    		
    	}
    	
    	if(base.enable_share){
    		detailWin.find('.discover-item-setting-item .icon-share').parent().addClass('active'); 		
    	}else{
    		detailWin.find('.discover-item-setting-item .icon-share').parent().removeClass('active');    		
    	}
    	
    	if(base.enable_external_share){
    		detailWin.find('.discover-item-setting-item .icon-share2').parent().addClass('active'); 		
    	}else{
    		detailWin.find('.discover-item-setting-item .icon-share2').parent().removeClass('active');    		
    	}

		var detail_info = [];
		detail_info.push('<div class="discover-item-info-item"><label>简介：</label><p>'+base.item_desc+'</p></div>');
    	if(typeof content.web_url != 'undefined'){
    		detailWin.find('.discover-item-setting-item .icon-download').parent().hide();
    		detail_info.push('<div class="discover-item-info-item"><label>内容：</label><p><a href="javascript:void(0)" class="discover-item-preview" type="web"  src="'+content.web_url+'">点我预览</a></p></div>');
    		detail_info.push('<div class="discover-item-info-item"><label>课件类型：</label><p>链接</p></div>');
    	}else{
    		detailWin.find('.discover-item-setting-item .icon-download').parent().show();
    		if(content.is_download){
    			detailWin.find('.discover-item-setting-item .icon-download').parent().addClass('active');
    		}else{
    			detailWin.find('.discover-item-setting-item .icon-download').parent().removeClass('active');
    		}
    		if(typeof content.audio_url != 'undefined'){
    			detail_info.push('<div class="discover-item-info-item"><label>内容：</label><p><a href="javascript:void(0)" class="discover-item-preview" type="audio" src="'+content.audio_url+'">点我预览</a></p></div>');
    			detail_info.push('<div class="discover-item-info-item"><label>课件类型：</label><p>音频</p></div>');
    			detail_info.push('<div class="discover-item-info-item"><label>文件大小：</label><p>'+(Math.floor(content.audio_size/1024/1024*100)/100)+'</p></div>');
    		}else if(typeof content.video_url != 'undefined'){
    			detail_info.push('<div class="discover-item-info-item"><label>内容：</label><p><a href="javascript:void(0)" class="discover-item-preview" type="video" src="'+content.video_url+'">点我预览</a></p></div>');
    			detail_info.push('<div class="discover-item-info-item"><label>课件类型：</label><p>视频</p></div>');
    			detail_info.push('<div class="discover-item-info-item"><label>文件大小：</label><p>'+(Math.floor(content.video_size/1024/1024*100)/100)+'</p></div>');
    		}else if(typeof content.document_url != 'undefined'){
    			detail_info.push('<div class="discover-item-info-item"><label>内容：</label><p><a href="javascript:void(0)" class="discover-item-preview" type="document" src="'+content.document_url+'">点我预览</a></p></div>');
    			detail_info.push('<div class="discover-item-info-item"><label>课件类型：</label><p>PDF文档</p></div>');
    			detail_info.push('<div class="discover-item-info-item"><label>文件大小：</label><p>'+(Math.floor(content.document_size/1024/1024*100)/100)+'</p></div>');
    		}
    	}
    	detailWin.find('.discover-item-info').html(detail_info.join(''));
    }
    
    detailWin.find('.discover-item-count').click(function(){
		statWin.window('open');
    	if($(this).hasClass('discover-item-like-count')){
    		statTab.tabs('select',0);
    	}else if($(this).hasClass('discover-item-score-count') || $(this).hasClass('discover-item-score-people')){
    		statTab.tabs('select',1);
    	}else if($(this).hasClass('discover-item-study-count') || $(this).hasClass('discover-item-study-people')){
    		statTab.tabs('select',2);
    	}else if($(this).hasClass('discover-item-comment-count') || $(this).hasClass('discover-item-comment-people')){
    		statTab.tabs('select',3);
    	}
    });
    detailWin.on('click','.discover-item-preview',function(){
    	var url = $(this).attr('src')||'';
    	var type = $(this).attr('type');
    	if(url == '') return false;
    	if(type == 'web'){
    		Wz.reviewCourse(json.url);
    	}else{
    		Wz.ajax({
    			url: './api/discover/get_auth_url.json',
    			data: {
    				url: url
    			},
    			success: function(json){
    				if(json.result == 'SUCC'){
    					Wz.reviewCourse(json.auth_url);
    				}else{
    					$.messager.alert('错误',json.fail_text,'error');
    					return false;
    				}
    			}
    		});    		
    	}
    });
    
    return {
    	changeState: changeState,
    	editItem: editItem,
    	saveItem: saveItem,
    	delItem: delItem,
    	showItem: showItem
    };
}()

