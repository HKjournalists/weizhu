/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 社区管理-帖子管理
 */
Wz.namespace('Wz.community');
Wz.community.postManage = function(){
    $.parser.parse('#main-contain');
    var query_params = {
    	board_id: '',
    	post_title: '',
    	is_recommend: false
    };
    var searchForm = $('#community-post-search-form');
    var editWin = $('#community-post-edit-win');
    var editForm = $('#community-post-edit-form');
    var importWin = $('#community-post-import-win');
    var importForm = importWin.find('#community-post-import-form');
    var moveWin = $('#community-post-move-win');
    var moveForm = moveWin.find('#community-post-move-form');
    var commentListWin = $('#community-post-commentlist-win');
    var commentWin = $('#community-post-comment-edit-win');
    var commentForm = $('#community-post-comment-edit-form');
    
    var searchBoard = searchForm.find('input[name=board_id]').combotree({
    	mode: 'remote',
    	treeField: 'board_name',
    	idField: 'board_id',
    	editable: false,
    	cascadeCheck: false,
    	height: 'auto',
    	onSelect : function(node) {
            var tree = $(this).tree;
            var isLeaf = tree('isLeaf', node.target);
            if (!isLeaf) {
            	searchBoard.combotree('clear');
            	return false;
            }
        },
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/community/get_board.json',
				success: function(json){
					var board = json.board;
					(function(data){
	        			for(var i=0;i<data.length;i++){
	        				data[i].id = data[i].board_id;
	        				data[i].text = data[i].board_name;
	        				if(data[i].children.length > 0){
	        					arguments.callee(data[i].children);
	        				}
	        			}    				
	    			}(board));
					success(board);
				}
			});
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });   
    
    var editBoard = editForm.find('input[name=board_id]').combotree({
    	mode: 'remote',
    	treeField: 'board_name',
    	idField: 'board_id',
    	editable: false,
    	cascadeCheck: false,
    	required: true,
    	height: 'auto',
    	width: 350,
    	onSelect : function(node) {
            var tree = $(this).tree;
            var isLeaf = tree('isLeaf', node.target);
            if (!isLeaf) {
            	editBoard.combotree('clear');
            	return false;
            }
        },
        onChange: function(newValue,oldValue){
        	editTag.reload();
        },
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/community/get_board.json',
				success: function(json){
					var board = json.board;
					(function(data){
	        			for(var i=0;i<data.length;i++){
	        				data[i].id = data[i].board_id;
	        				data[i].text = data[i].board_name;
	        				if(data[i].children.length > 0){
	        					arguments.callee(data[i].children);
	        				}
	        			}    				
	    			}(board));
					success(board);
				}
			});
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });
    
    var moveBoard = moveForm.find('input[name=board_id]').combotree({
    	mode: 'remote',
    	treeField: 'board_name',
    	idField: 'board_id',
    	editable: false,
    	cascadeCheck: false,
    	height: 'auto',
    	width: 350,
    	required: true,
    	onSelect : function(node) {
            var tree = $(this).tree;
            var isLeaf = tree('isLeaf', node.target);
            if (!isLeaf) {
            	moveBoard.combotree('clear');
            	return false;
            }
        },
    	loader: function(param,success,error){
			Wz.ajax({
				url: './api/community/get_board.json',
				success: function(json){
					var board = json.board;
					(function(data){
	        			for(var i=0;i<data.length;i++){
	        				data[i].id = data[i].board_id;
	        				data[i].text = data[i].board_name;
	        				if(data[i].children.length > 0){
	        					arguments.callee(data[i].children);
	        				}
	        			}    				
	    			}(board));
					success(board);
				}
			});
		},
		onShowPanel: function(){
			$(this).combotree('reload');
		}
    });
    
    var uploadImge = editForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '支持jpg、png格式，大小2M以内',
    	maxSize: 2,
    	params: {
    		image_tag: '社区,帖子'
    	}
    });
    
    var editTag = editForm.find('input[name=tag]').tagarea({
    	remote: true,
    	wrapWidth: 350,
    	loader: function(success){
    		var board_id = editBoard.combotree('getValue');
    		Wz.ajax({
    			url: './api/community/get_board_tag.json',
    			data: {
    				board_id: board_id
    			},
    			success: function(json){
    				success(json.tag||[]);
    			}
    		})
    	}
    });
    var postUser = editForm.find('input[name=create_user_id]').combogrid({
    	url: './api/user/get_user_list.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        pagination: true,
        rownumbers: true,
        required: true,
        pageSize: 20,
        idField: 'user_id',
        textField: 'user_name',
        keyHandler: {
        	query: function(q) {
        		postUser.combogrid("grid").datagrid("reload", { 'keyword': q });  
        		postUser.combogrid("setValue", q);
            } 
        },
        width: 350,
        panelWidth: 500,
        frozenColumns: [[{
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
    var commentUser = commentForm.find('input[name=create_user_id]').combogrid({
    	url: './api/user/get_user_list.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        pagination: true,
        rownumbers: true,
        required: true,
        pageSize: 20,
        idField: 'user_id',
        textField: 'user_name',
        keyHandler: {
        	query: function(q) {
        		commentUser.combogrid("grid").datagrid("reload", { 'keyword': q });  
        		commentUser.combogrid("setValue", q);
            } 
        },
        width: 350,
        panelWidth: 500,
        frozenColumns: [[{
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
    })
    
    searchForm.find('.search-btn').click(function(){
    	query_params.board_id = searchForm.find('input[name=board_id]').val();
    	query_params.post_title = searchForm.find('input[name=post_title]').val();
    	query_params.is_recommend = searchForm.find('input[name=is_recommend]').prop('checked');
    	postTable.datagrid('reload');
    });
    searchForm.find('.reset-btn').click(function(){
    	searchForm.form('reset');
    });
    var postTable = $('#community-post-table').datagrid({
        url: './api/community/get_post.json',
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
            field: 'post_title',
            title: '标题',
            width: 200,
            align: 'left'
        },{
        	field: 'board_name',
            title: '板块',
            width: 100,
            align: 'left'
        },{
        	field: 'create_user_name',
            title: '作者',
            width: 80,
            align: 'left'
        },{
        	field: 'like_count',
            title: '点赞数',
            width: 60,
            align: 'right'
        },{
        	field: 'comment_count',
            title: '回帖数',
            width: 60,
            align: 'right'
        },{
            field: 'create_time',
            title: '提问时间',
            width: 140,
            align: 'center'
        },{
            field: 'is_recommend',
            title: '推荐',
            align: 'center',
            width: 60,
            hidden: !(Wz.getPermission('community/post/recommend')),
            formatter: function(val,obj,row){
                return '<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.community.postManage.recommendPost('+obj.post_id+','+val+')">'+(val?'取消推荐':'推荐')+'</a>';
            }
        },{
            field: 'is_sticky',
            title: '置顶',
            align: 'center',
            width: 60,
            hidden: !(Wz.getPermission('community/post/sticky')),
            formatter: function(val,obj,row){
                return '<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.community.postManage.stickyPost('+obj.post_id+','+val+')">'+(val?'取消置顶':'置顶')+'</a>';
            }
        },{
            field: 'post_id',
            title: '操作',
            align: 'center',
            width: 100,
            hidden: !(Wz.getPermission('community/post/delete')||Wz.getPermission('community/post/list')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('community/post/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.community.postManage.delPost('+obj.post_id+')">删除</a>':'') +
                	(Wz.getPermission('community/post/list')?'<a href="javascript:void(0)" class="tablelink" onclick="Wz.community.postManage.showPost('+row+')">查看详情</a>':'');
            }
        }]],
        toolbar: [{
            id: 'community-post-add',
            text: '发帖',
            iconCls: 'icon-add',
            disabled: !Wz.getPermission('community/post/create'),
            handler: function(){
            	var user = Wz.getClientUser();
            	editForm.form('reset');
            	uploadImge.reset();
            	editTag.reload();
            	editTag.reset();
            	postUser.combogrid('setValue',user.user_id);
            	postUser.combogrid('setText',user.user_name);
            	editWin.window('open');
            }
        },{
            id: 'community-post-del',
            text: '删除',
            iconCls: 'icon-remove',
            disabled: !Wz.getPermission('community/post/delete'),
            handler: function(){
            	var selects = postTable.datagrid('getChecked');
            	if(selects.length == 0){
            		$.messager.alert('提示','请选择需要删除的问题！','info');
            		return false;
            	}
            	var post_id = [];
            	for(var i=0;i<selects.length;i++){
            		post_id.push(selects[i].post_id);
            	}
            	delPost(post_id.join(','));
            }
        },{
            id: 'community-post-export',
            text: '导出搜索帖子',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('community/post/export'),
            handler: function(){
            	var param = [];
            	for(var name in query_params){
            		param.push(name + '=' + query_params[name]);
            	}
            	Wz.downloadFile("./api/community/export_post.json?"+param.join('&')+"&_t=" + new Date().getTime());
            }
        },{
            id: 'community-post-move',
            text: '迁移',
            iconCls: 'icon-move',
            disabled: !Wz.getPermission('community/post/move'),
            handler: function(){
            	moveForm.form('reset');
            	var posts = postTable.datagrid('getChecked');
            	if(posts.length == 0){
            		$.messager.alert('提示','请选择需要迁移的帖子！','info');
            		return false;
            	}
            	var post_id = [];
            	for(var i=0;i<posts.length;i++){
            		post_id.push(posts[i].post_id);
            	}
            	moveForm.find('input[name=post_id]').val(post_id.join(','));
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
                rows: data.post
            };
        }
    });
    
    var comment_params = {
    	post_id: ''
    };
    
    var commentTable = $('#community-post-comment-table').datagrid({
    	url: './api/community/get_comment.json',
        queryParams: comment_params,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        showHeader: false,
        pagination: true,
        pageSize: 20,
        columns: [[{
            field: 'comment_id'
        }]],
        toolbar: [{
            id: 'community-post-comment-add',
            text: '我来评论',
            iconCls: 'icon-add',
            disabled: !Wz.getPermission('community/post/create_comment'),
            handler: function(){
            	var user = Wz.getClientUser();
            	commentForm.form('reset');
            	commentForm.find('input[name=post_id]').val(comment_params.post_id);
            	commentForm.find('input[name=reply_comment_id]').val('');
            	commentUser.combogrid('setValue',user.user_id);
            	commentUser.combogrid('setText',user.user_name);
            	commentWin.window('open');
            }
        },{
            id: 'community-postcomment-export',
            text: '导出评论',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('community/post/export_comment'),
            handler: function(){
            	var param = [];
            	for(var name in comment_params){
            		param.push(name + '=' + comment_params[name]);
            	}
            	Wz.downloadFile("./api/community/export_comment.json?"+param.join('&')+"&_t=" + new Date().getTime());
            }
        },{
            id: 'community-postlike-export',
            text: '导出点赞',
            iconCls: 'icon-export',
            disabled: !Wz.getPermission('community/post/export_like'),
            handler: function(){
            	var param = [];
            	for(var name in comment_params){
            		param.push(name + '=' + comment_params[name]);
            	}
            	Wz.downloadFile("./api/community/export_post_like.json?"+param.join('&')+"&_t=" + new Date().getTime());
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
                rows: data.comment
            };
        }
    });
    
    var commentView = $.extend({}, $.fn.datagrid.defaults.view, {
        renderRow: function(target, fields, frozen, rowIndex, rowData){
        	if(!!!rowData.comment_id)return '';
        	var html = ['<td class="community-post-comment-info">'];
        	html.push(['<h4>',rowData.create_user_name,'</h4><div><span>',rowData.comment_id,'楼&nbsp;',rowData.create_time,'</span><span class="like-btn">',
        	           '<span class="l-btn-icon icon-heart"></span><span class="like-count">',rowData.like_count,'</span></span></div>',
        	           '<p>回复',(rowData.reply_comment_id>0?(rowData.reply_comment_id+'楼: '):'楼主: '),rowData.content,'</p>'].join(''));
        	html.push('</td>'+((Wz.getPermission('community/post/create_comment')||Wz.getPermission('community/post/delete_comment'))?
        			('<td>'+(((Wz.getPermission('community/post/create_comment')?'<a href="javascript:void(0)" class="tablelink comment-btn" onclick="Wz.community.postManage.reComment('+rowData.comment_id+')">评论</a>':'')+(Wz.getPermission('community/post/delete_comment')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.community.postManage.delcomment('+rowData.comment_id+')">删除</a>':'')))+'</td>'):''))
        	
            return html.join('');
        }
    });
    
    function showPost(row){
    	var post = postTable.datagrid('getData').rows[row];
    	var post_part = post.post_part[0] || {};
    	commentListWin.find('.community-post-title').html(post.post_title);
    	commentListWin.find('.community-post-author').html(post.create_user_name);
    	commentListWin.find('.community-post-info .like-count').html(post.like_count);
    	commentListWin.find('.community-post-info .community-post-time').html(post.create_time);
    	commentListWin.find('.community-post-text').html(post_part.text||'');
    	if(!!post_part.image_url){
        	commentListWin.find('.community-post-image').attr('src',post_part.image_url).show();
    	}else{
        	commentListWin.find('.community-post-image').hide();
    	}
    	comment_params.post_id = post.post_id;
    	commentListWin.window('open');
    	commentTable.datagrid({
    		view: commentView
    	});
    }
    
    function saveMove(){
    	var url = './api/community/migrate_post.json';
    	moveForm.form('submit',{
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
    				moveWin.window('close');
    				postTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function saveEdit(){
    	var url = './api/community/create_post.json';
    	editForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			if(postUser.combogrid('getValue') == postUser.combogrid('getText')){
    				postUser.combogrid('setValue','');
    			}
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
    				postTable.datagrid('reload');
    				Wz.setClientUser({
    					user_id: postUser.combogrid('getValue'),
    					user_name: postUser.combogrid('getText')
    				});
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function saveComment(){
    	var url = './api/community/create_comment.json';
    	commentForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			if(commentUser.combogrid('getValue') == commentUser.combogrid('getText')){
    				commentUser.combogrid('setValue','');
				}
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				commentWin.window('close');
    				commentTable.datagrid('reload');		
    				Wz.setClientUser({
    					user_id: commentUser.combogrid('getValue'),
    					user_name: commentUser.combogrid('getText')
    				});
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function delPost(post_id){
    	$.messager.confirm('提示','请确认是否要删除帖子？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/community/delete_post.json',
    				data: {
    					post_id: post_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						postTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    
    function delcomment(comment_id){
    	$.messager.confirm('提示','请确认是否要删除评论吗？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/community/delete_comment.json',
    				data: {
    					post_id: comment_params.post_id,
    					comment_id: comment_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						commentTable.datagrid('reload');
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
    		url: './api/qa/import_question.json?category_id=' + category_id,
    		onSubmit: function(){
    			Wz.showLoadingMask();
    		},
    		success: function(result){
    			Wz.hideLoadingMask();
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				importWin.window('close');
    				postTable.datagrid('reload');
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
    
    function recommendPost(post_id,state){
    	var msg = '请确认是否将帖子设置为推荐贴';
    	if(state){
    		msg = '请确认是否将帖子取消推荐';
    	}
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/community/recommend_post.json',
    				data: {
    					post_id: post_id,
    					is_recommend: !state
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						postTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    
    function stickyPost(post_id,state){
    	var msg = '请确认是否将帖子设置为置顶贴';
    	if(state){
    		msg = '请确认是否将帖子取消置顶';
    	}
    	$.messager.confirm('提示',msg,function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/community/set_sticky_post.json',
    				data: {
    					post_id: post_id,
    					is_sticky: !state
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						postTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }

    function reComment(comment_id){
    	var user = Wz.getClientUser();
    	commentForm.form('reset');
    	commentForm.find('input[name=post_id]').val(comment_params.post_id);
    	commentForm.find('input[name=reply_comment_id]').val(comment_id);
    	commentUser.combogrid('setValue',user.user_id);
    	commentUser.combogrid('setText',user.user_name);
    	commentWin.window('open');
    }
    
    function init(data){
    	if(!!data){
    		searchCategory.combobox('setValue',data.category_id);
    		searchForm.find('.search-btn').trigger('click');
    	}
    }
    
    return {
    	init: init,
    	saveMove: saveMove,
    	saveEdit: saveEdit,
    	delPost: delPost,
    	recommendPost: recommendPost,
    	stickyPost: stickyPost,
    	showPost: showPost,
    	saveComment: saveComment,
    	delcomment: delcomment,
    	reComment: reComment
    };
}()

