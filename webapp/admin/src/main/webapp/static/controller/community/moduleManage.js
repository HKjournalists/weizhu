/**
 * Created by allenpeng on 16-2-3.
 * 功能介绍： 社区管理-板块管理
 */
Wz.namespace('Wz.community');
Wz.community.moduleManage = function(){
    $.parser.parse('#main-contain');
    var mainWrap = $('#community-module-wrap');
    var communityForm = $('#community-community-edit-form');
    var editBtn = communityForm.find('.edit-btn');
    var saveBtn = communityForm.find('.save-btn');
    var cancelBtn = communityForm.find('.cancel-btn');
    var tagManageWin = $('#community-module-tagmanage-win');
    var tagManageForm = $('#community-module-tagmanage-form');
    editBtn.click(function(){
    	communityForm.find('input[textboxname=community_name]').textbox('enable');
    	editBtn.hide();
    	saveBtn.show();
    	cancelBtn.show();
    });
    saveBtn.click(function(){
    	communityForm.form('submit',{
    		url: './api/community/set_community.json',
    		onSubmit: function(){
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				communityForm.find('input[textboxname=community_name]').textbox('disable');
    		    	saveBtn.hide();
    		    	cancelBtn.hide();
    		    	editBtn.show();
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    });
    cancelBtn.click(function(){
    	freshCommunityName();
    	communityForm.find('input[textboxname=community_name]').textbox('disable');
    	saveBtn.hide();
    	cancelBtn.hide();
    	editBtn.show();
    	
    });
    var communityName = communityForm.find('input[name=community_name]').textbox({
    	disabled:true,
    	required: true,
    	prompt: '社区名称长度为1~10个字符',
    	width: 300,
    	validType: 'length[1,10]'
    });
    var editWin = $('#community-module-edit-win');
    var editForm = $('#community-module-edit-form');
    var uploadImge = editForm.find('input[name=board_icon]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '建议图片尺寸120x120(px)<br>支持jpg、png格式，大小1M以内',
    	maxSize: 1,
    	params: {
    		image_tag: '社区,图标'
    	}
    });
    var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
    freshCommunityName();

    var boardTable = $('#community-module-table').treegrid({
    	url: './api/community/get_board.json',
    	treeField: 'board_name',
    	idField: 'board_id',
        rownumbers: true,
        checkOnSelect: true,
        autoRowHeight: true,
    	columns: [[{
            field: 'ck',
            checkbox: true
        },{
    		field: 'board_name',
    		width: '40%',
    		title: '板块名称',
    		formatter: function(val,obj,row){
    			return val+'('+obj.board_desc+')';
    		}
    	},{
    		field: 'board_icon_url',
    		width: '50px',
    		title: '图标',
    		align: 'center',
    		formatter: function(val,obj,row){
    			return '<img src="'+val+'" style="width:40px;height:40px;float:left;"/>';
    		}
    	},{
    		field: 'post_total_count',
    		title: '发帖总数',
    		align: 'right',
    		width: '100px'
    	},{
    		field: 'board_id',
    		title: '排序',
    		width: '150px',
    		align: 'center',
            hidden: !(Wz.getPermission('community/board/update_order')),
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-top move-top-btn">&nbsp;</a><a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-up move-up-btn">&nbsp;</a><a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-down move-down-btn">&nbsp;</a><a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-bottom move-bottom-btn">&nbsp;</a>';
            }    		
    	},{
            field: 'is_leaf_board',
            title: '标签',
            align: 'center',
            width: '80px',
            hidden: !(Wz.getPermission('community/board/list_tag')),
            formatter: function(val,obj,row){
                return !!val?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.community.moduleManage.tagManage('+obj.board_id+')">标签管理</a>':'';
            }
        },{
            field: 'opt',
            title: '操作',
            align: 'center',
            width: '100px',
            hidden: !(Wz.getPermission('community/board/update')||Wz.getPermission('community/board/delete')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('community/board/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.community.moduleManage.editBoard('+obj.board_id+')">编辑</a>':'')+
                	(Wz.getPermission('community/board/delete')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.community.moduleManage.delBoard('+obj.board_id+')">删除</a>':'');
            }
        }]],
    	toolbar: [{
            id: 'exam-category-add',
            text: '创建同级板块',
            disabled: !Wz.getPermission('community/board/create'),
            iconCls: 'icon-add',
            handler: function(){
            	var board = boardTable.treegrid('getChecked');
            	if(board.length == 0 && boardTable.treegrid('getRoots').length > 0){
            		$.messager.alert('提示','请先选择一个板块进行创建！','info');
            		return false;
            	}
            	if(board instanceof Array){
            		board = board[0];
            	}
            	board = board||{parent_board_id:''};
            	editForm.form('reset');
            	editForm.find('input[name=parent_board_id]').val(board.parent_board_id||'');
            	editForm.find('input[name=board_id]').val('');
            	uploadImge.reset();
            	allowModel.setValue({model_id:'',model_name:''});
            	editWin.window({title:'创建同级板块'}).window('open');
            }
        },{
            id: 'exam-subcategory-add',
            text: '创建子级板块',
            disabled: !Wz.getPermission('community/board/create'),
            iconCls: 'icon-add',
            handler: function(){
            	var board = boardTable.treegrid('getChecked');
            	if(board.length == 0){
            		$.messager.alert('提示','请先选择一个板块进行创建！','info');
            		return false;
            	}
            	editForm.form('reset');
            	editForm.find('input[name=parent_board_id]').val(board[0].board_id||'');
            	editForm.find('input[name=board_id]').val('');
            	uploadImge.reset();
            	allowModel.setValue({model_id:'',model_name:''});
            	editWin.window({title:'创建子级板块'}).window('open');
            }
        }],
    	loadFilter: function(data){
    		data = data.board||data;
    		return data;
    	}
    });
    var tagTable = $('#community-module-tag-table').datagrid({
    	data: [],
    	fitColumns: true,
        striped: true,
        fit: true,
        title: '标签列表：',
        columns: [[{
        	field: 'tag_name',
        	width: 320,
        	title: '标签名'
        },{
        	field: 'tag_id',
        	title: '操作',
        	align: 'center',
        	formatter: function(val,obj,row){
        		return '<a href="javascript:void(0)" class="table-cell-icon icon-remove">&nbsp;</a>';
        	}
        }]]
    });
    
    mainWrap.on('click','.table-cell-icon',function(){
    	var board_id = $(this).attr('bid');
    	var parent = boardTable.treegrid('getParent',board_id);
		var children = [];
		if(!!parent){
			children = boardTable.treegrid('getChildren',parent.board_id);    			
		}else{
			children = boardTable.treegrid('getRoots');    
		}
    	if($(this).hasClass('move-top-btn')){
			if(children.length > 1 && children[0].board_id != board_id){
				var board = boardTable.treegrid('pop',board_id);
				boardTable.treegrid('insert',{
					before: children[0].board_id,
					data: board
				});
			}
    	}else if($(this).hasClass('move-bottom-btn')){
			if(children.length > 1 && children[children.length-1].board_id != board_id){
				var board = boardTable.treegrid('pop',board_id);
				boardTable.treegrid('append',{
					parent: (!!parent?parent.board_id:''),
					data: [board]
				});
			}
    	}else if($(this).hasClass('move-up-btn')){
    		if(children.length > 1 && children[0].board_id != board_id){
    			var before_id = '';
    			for(var i=0;i<children.length;i++){
    				if(children[i].board_id == board_id) break;
    				before_id = children[i].board_id;
    			}
				var board = boardTable.treegrid('pop',board_id);
				boardTable.treegrid('insert',{
					before: before_id,
					data: board
				});
			}
    	}else if($(this).hasClass('move-down-btn')){
			if(children.length > 1 && children[children.length-1].board_id != board_id){
				var after_id = '';
    			for(var i=children.length-1;i>-1;i--){
    				if(children[i].board_id == board_id) break;
    				after_id = children[i].board_id;
    			}
				var board = boardTable.treegrid('pop',board_id);
				boardTable.treegrid('insert',{
					after: after_id,
					data: board
				});
			}
    	}
    	var tdata = boardTable.treegrid('getData');
    	var sort_id = [];
    	(function(data){
    		for(var i=0;i<data.length;i++){
    			var children = [];
    			sort_id.push(data[i].board_id);
    			children = children.concat(data[i].children||[]);
    			if(children.length > 0){
    				arguments.callee(children);
    			}
    		}    		
    	}(tdata));
    	Wz.ajax({
    		type: 'post',
    		url: './api/community/update_board_order.json',
    		data: {
    			board_id_order_str: sort_id.join(',')
    		},
    		success: function(json){
    			//不作处理
    		}
    	});
    });
    function editBoard(board_id){
    	var board = boardTable.treegrid('find',board_id);
    	editForm.form('load',board);
    	uploadImge.setValue(board.board_icon,board.board_icon_url);
    	allowModel.setValue({model_id:board.allow_model_id,model_name:board.allow_model_name});
    	editWin.window({title:'修改社区板块'}).window('open');
    }
    function delBoard(board_id){
    	var board = boardTable.treegrid('find',board_id);
    	if(!board.is_leaf_board){
    		$.messager.alert({
    			title: '提示',
    			msg: '请将该板块下的子版块删除之后再进行删除！',
    		});
    	}else if(board.post_total_count > 0){
    		$.messager.confirm({
    			title: '请确认',
    			msg: '删除操作会同事删除该板块下的帖子，帖子删除后将无法恢复，建议将帖子迁移后再删除。',
    			ok: '直接删除',
    			cancel: '取消',
    			fn: function(ok){
    				if(ok){
    					del_board();
    				}
    			}
    		});
    	}else{
    		$.messager.confirm({
    			title: '请确认',
    			msg: '请确认删除该板块吗？',
    			ok: '确认',
    			cancel: '取消',
    			fn: function(ok){
    				if(ok){
    					del_board();
    				}
    			}
    		});
    	}
    	function del_board(){
    		Wz.ajax({
    			type: 'post',
    			url: './api/community/delete_board.json',
    			data: {
    				board_id: board.board_id,
    				is_force_delete: true,
    			},
    			success: function(json){
    				if(json.result == 'SUCC'){
    					var parent = boardTable.treegrid('getParent',board.board_id);
    					boardTable.treegrid('remove',board.board_id);
    					var children = boardTable.treegrid('getChildren',parent.board_id);
    					parent.post_total_count -= board.post_total_count;
    					if(children.length == 0){
    						parent.is_leaf_board = true;
    					}
    					boardTable.treegrid('update',{id: parent.board_id,row: parent});
    					(function(b){
    						var p = boardTable.treegrid('getParent',b.board_id);
    						if(!!p){
    							p.post_total_count -= board.post_total_count;
    							boardTable.treegrid('update',{id: p.board_id,row: p});
    							arguments.callee(p);
    						}
    					}(parent));
    				}else{
    					$.messager.alert('错误',json.fail_text,'error');
    				}
    			}
    		});
    	}
    }
    
    function saveModule(){
    	var board_id = editForm.find('input[name=board_id]').val();
    	var board_name = editForm.find('input[name=board_name]').val();
    	var board_desc = editForm.find('input[name=board_desc]').val();
    	var parent_board_id = editForm.find('input[name=parent_board_id]').val();
		var board_icon = uploadImge.getValue();
		var board_icon_url = uploadImge.getUrl();
		var allow_model_id = allowModel.getValue();
		var allow_model_name = allowModel.getName();
    	var url = './api/community/create_board.json';
    	if(board_id != ''){
    		url = './api/community/update_board.json';
    	}
    	editForm.form('submit',{
    		url: url,
    		conSubmit: function(){
    			if(board_icon == ''){
    				$.messager.alert('错误','请上传板块图标！','error');
    				return false;
    			}
    			var valid = $(this).form('validate');
    			if(valid){
    				Wz.showLoadingMask('正在处理中，请稍后......');
    			}
    			return valid;
    		},
    		success: function(json){
    			Wz.hideLoadingMask();
    			var json = $.parseJSON(json);
				if(json.result == 'SUCC'){
					var board = {
						board_id: json.boad_id,
						board_name: board_name,
						board_icon: board_icon,
						board_desc: board_desc,
						board_icon_url: board_icon_url,
						children: [],
						is_leaf_board: true,
						parent_board_id: parent_board_id,
						post_total_count: 0,
						allow_model_name: allow_model_name,
						allow_model_id: allow_model_id
					};
					if(board_id == ''){
						if(parent_board_id != ''){
							var parent = boardTable.treegrid('getSelected');
							parent.is_leaf_board = false;
							boardTable.treegrid('update',{id:parent.board_id,row:parent});
							board.post_total_count = parent.post_total_count;
						}
						boardTable.treegrid('append',{
							parent: parent_board_id,
							data: {board:[board]}
						});
					}else{
						var node = boardTable.treegrid('find',board_id);
						board.post_total_count = node.post_total_count;
						board.is_leaf_board = node.is_leaf_board;
						board.children = node.children;
						boardTable.treegrid('update',{id:board_id,row:board});
					}
					editWin.window('close');
				}else{
					$.messager.alert('错误',json.fail_text,'error');
				}
    		}
    	});
    }
    
    function freshCommunityName(){
    	Wz.ajax({
    		url: './api/community/get_community.json',
    		success: function(json){
    			communityForm.form('load',json);
    		}
    	});
    }
    
    function tagManage(board_id){
    	tagManageForm.form('reset');
		tagTable.datagrid('loadData',[]);
		tagManageForm.find('input[name=board_id]').val(board_id);
    	Wz.ajax({
    		url: './api/community/get_board_tag.json',
    		data: {
    			board_id: board_id
    		},
    		success: function(data){
				data = data.tag||[];
				var tags = [];
				for(var i=0;i<data.length;i++){
					tags.push({
						tag_name: data[i],
						tag_id: data[i],
					});
				}
				tagTable.datagrid('loadData',tags);
    	    	tagManageWin.window('open');
    		}
    	});
    }
    
    tagManageWin.on('click','.add-btn',function(){
    	var tag = tagManageForm.find('input[name=tag]').val();
    	tagManageForm.form('submit',{
    		url: './api/community/create_board_tag.json',
    		conSubmit: function(){
    			return $(this).form('validate');
    		},
    		success: function(json){
    			var json = $.parseJSON(json);
				if(json.result == 'SUCC'){
					tagTable.datagrid('insertRow',{
						index: 0,
						row: {
							tag_name: tag,
							tag_id: tag
						}
					});
					tagManageForm.find('input[textboxname=tag]').textbox('setValue','');
				}else{
					$.messager.alert('错误',json.fail_text,'error');
				}
    		}
    	});
    });
    tagManageWin.on('click','.icon-remove',function(){
    	var index = $(this).parents('tr').index();
    	var board_id = tagManageForm.find('input[name=board_id]').val();
    	var tag_name = tagTable.datagrid('getRows')[index].tag_name;
    	Wz.ajax({
    		type: 'post',
    		url: './api/community/delete_board_tag.json',
    		data: {
    			board_id: board_id,
    			tag: tag_name
    		},
    		success: function(json){
    			if(json.result == 'SUCC'){
					tagTable.datagrid('deleteRow',index);
				}else{
					$.messager.alert('错误',json.fail_text,'error');
				}
    		}
    	})
    });
    
    return {
    	delBoard: delBoard,
    	saveModule: saveModule,
    	editBoard: editBoard,
    	tagManage: tagManage
    };
}()

