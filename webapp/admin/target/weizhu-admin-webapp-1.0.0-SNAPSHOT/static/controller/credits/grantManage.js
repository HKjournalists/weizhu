Wz.namespace('Wz.credits');
Wz.credits.grantManage = function(){
	$.parser.parse('#main-contain');
	freshCredits();
	var searchForm = $('#credits-grant-search-form');
	var editWin = $('#credits-grant-edit-win');
	var editForm = $('#credits-grant-edit-form');
	var detailWin = $('#credits-grant-detail-win');
	var userSelectWin = $('#credits-grant-select-user-win');
	var total_credits = 0;
	
	var grantTable = $('#credits-grant-table').datagrid({
        url: './api/credits/get_credits_operation.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'desc',
            title: '事由',
            width: '50%',
            align: 'left'
        },{
            field: 'create_admin',
            title: '发放人',
            width: '10%',
            align: 'left'
        },{
            field: 'create_time',
            title: '发放时间',
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
            field: 'credits_total',
            title: '发放总积分',
            width: '10%',
            align: 'right'
        },{
            field: 'operation_id',
            title: '发放对象',
            width: '10%',
            align: 'center',
            hidden: !(Wz.getPermission('credits/grant/list_user')),
            formatter: function(val,obj,row){
                return '<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.credits.grantManage.showDetail('+row+')">查看</a>';
            }
        }]],
        toolbar: [{
            id: 'credits-grant-add',
            text: '发积分',
            disabled: !Wz.getPermission('credits/grant/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[textboxname=credits_total]').numberbox('setValue',total_credits);
            	grantUserTable.datagrid('loadData',[]);
            	editWin.window('open');
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
                rows: data.credits_operation
            };
        }
    });
	 
	searchForm.find('.search-btn').click(function(){
		query_params.user_name = searchForm.find('input[name=user_name]').val();
		query_params.team_id = searchForm.find('input[name=team_id]').val();
		grantTable.datagrid('reload');
	});
	searchForm.find('.reset-btn').click(function(){
		searchForm.form('reset');
	});
	
	var detailParam = {
		user_id: ''
	};
	
	var grantUserTable = $('#credits-grant-user-table').datagrid({
        data: [],
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        columns: [[{
            field: 'user_name',
            title: '姓名',
            width: '10%',
            align: 'left'
        },{
            field: 'team',
            title: '部门信息',
            width: '40%',
            align: 'left',
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
            field: 'mobile_no',
            title: '手机号码',
            width: '20%',
            align: 'right'
        },{
            field: 'credits_delta',
            title: '发放分值',
            width: '10%',
            align: 'center',
            formatter: function(val,obj,row){
            	var value = editForm.find('input[textboxname=credits_delta]').numberbox('getValue');
            	return '<input class="easyui-numberbox form-item-box" type="text" name="user_credits" data-options="fit:true,required:true,value:'+value+',prompt:\'请输入积分数，只能输入数字1-9999\',min:1,max:9999" />';
            }
        }]],
        toolbar: [{
            id: 'credits-grant-add',
            text: '选择发放人员',
            disabled: !Wz.getPermission('company/position/create'),
            iconCls: 'icon-add',
            handler: function(){
            	userSelectWin.window('open');
            	return false;
            }
        }],
        onLoadSuccess: function(){
        	editWin.find('input[name=user_credits]').numberbox({
        		onChange: function(newValue,oldValue){
        			var tc = parseInt(editForm.find('input[textboxname=credits_total]').numberbox('getValue')||'0');
        			editForm.find('input[textboxname=credits_total]').numberbox('setValue',tc-parseInt(newValue||'0') + parseInt(oldValue||'0'));
        		}
        	});
        }
    });
	
	var detailTable = $('#credits-grant-detail-table').datagrid({
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        columns: [[{
            field: 'user_name',
            title: '姓名',
            width: '10%',
            align: 'left'
        },{
            field: 'user_team',
            title: '部门',
            width: '40%',
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
            width: '10%',
            align: 'center'
        },{
            field: 'user_mobile',
            title: '手机号',
            width: '14%',
            align: 'center'
        },{
            field: 'credits_delta',
            title: '发放分值',
            width: '10%',
            align: 'center'
        }]]
    });
	
	var queryPosition = userSelectWin.find('input[name=position_id]').combobox({
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
					json.position.unshift({position_id:'',position_name:'---所有职务---'})
					success(json.position);
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
	var queryTeam = userSelectWin.find('input[name=team_id]').combotree({
		panelWidth: 200,
    	url: './api/user/get_all_team.json',
    	editable: false,
    	cascadeCheck: false,
    	onShowPanel: function(){
    		queryTeam.combotree('reload');
    	},
    	loadFilter: function(data){
    		var data = data.team||[];
    		(function(node){
    			if(node.length>0){
    				for(var i=0;i<node.length;i++){
    					node[i].id = node[i].team_id;
    					node[i].text = node[i].team_name;
    					if(node[i].sub_team.length>0){
    						node[i].children = node[i].sub_team;
    						arguments.callee(node[i].children);
    					}
    				}
    			}    			
    		}(data))
    		data.unshift({id:'',text:'---所有部门---'})
    		return data;
    	}
    });
	
	var userParam = {
    	team_id: '',
    	position_id: '',
    	keyword: '',
    	mobile_no: ''
	}
	var userTable = $('#credits-grant-userall-table').datagrid({
        url: './api/user/get_user_list.json',
        queryParams: userParam,
        fitColumns: true,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        title: '待选人员',
        pageSize: 20,
        columns: [[{
            field: 'ck',
            checkbox: true
        },{
            field: 'user_name',
            title: '姓名',
            width: '80px',
            align: 'center'
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
            field: 'position',
            title: '职务',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val.position_name: '';
            }
        },{
            field: 'mobile_no',
            title: '手机号',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	return !!val?val.join('<br/>'): '';
            }
        }]],
        onLoadSuccess: function(data){
        	data = data.rows;
        	var sd = userSelectedTable.datagrid('getData').rows;
        	for(var i=0;i<data.length;i++){
        		for(var j=0;j<sd.length;j++){
        			if(sd[j].user_id==data[i].user_id){
        				userTable.datagrid('checkRow',i);
        				break;
        			}
        		}
        	}
        },
        onCheck: function(index,row){
        	var data = userSelectedTable.datagrid('getData').rows;
        	var data = $.grep(data,function(o){
        		return o.user_id == row.user_id;
        	});
        	if(data.length == 0){
        		userSelectedTable.datagrid('insertRow',{
        			index: 0,
        			row: row
        		});
        	}
        },
        onUncheck: function(index,row){
        	var data = userSelectedTable.datagrid('getData').rows;
        	$.grep(data,function(o,i){
        		if(!!!o)return false;
        		if(row.user_id == o.user_id){
        			userSelectedTable.datagrid('deleteRow',i);
        		}
        		return true;
        	});
        },
        onUncheckAll: function(rows){
        	var oldData = userSelectedTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		for(var i=oldData.length;i>0;i--){
        			if(oldData[i-1].user_id == o.user_id){
        				userSelectedTable.datagrid('deleteRow',i-1);
        				break;
        			}
        		}
        	});
        },
        onCheckAll: function(rows){
        	var oldData = userSelectedTable.datagrid('getData').rows;
        	$.grep(rows,function(o){
        		var same = $.grep(oldData,function(od){
        			return od.user_id == o.user_id;
        		});                    		
        		if(same.length == 0){
        			userSelectedTable.datagrid('insertRow',{
            			index: 0,
            			row: o
            		});
        		}
        	});
        },
        toolbar: '#credits-grant-userall-tb',
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
	var userSelectedTable = $('#credits-grant-userselected-table').datagrid({
		data: [],
        fitColumns: true,
        striped: true,
        fit: true,
        rownumbers: true,
        title: '已选人员',
        columns: [[{
            field: 'user_name',
            title: '姓名',
            width: '80px',
            align: 'center'
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
            field: 'position',
            title: '职务',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	if(!!val){
            		if(typeof val == 'string'){
            			return val;
            		}else{
            			return val.position_name;
            		}                        		
            	}else{
            		return '';
            	}
            }
        },{
            field: 'mobile_no',
            title: '手机号',
            width: '100px',
            align: 'center',
            formatter: function(val,obj,row){
            	if(!!val){
            		if(val instanceof Array){
            			return val.join('<br/>');
            		}else{
            			return val.replace(/,/g,'<br/>');
            		}
            	}
            	
            }
        },{
            field: 'user_id',
            title: '操作',
            align: 'center',
            width: 60,
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
            }
        }]]
    });
	
	userSelectWin.find('.search-btn').click(function(){
		userParam.keyword = userSelectWin.find('input[name=keyword]').val();
		userParam.position_id = queryPosition.combobox('getValue');
		userParam.mobile_no = userSelectWin.find('input[name=mobile_no]').val();
		userParam.team_id = queryTeam.combotree('getValue');
		userTable.datagrid('reload');
	});
	userSelectWin.on('click','.del-btn',function(){
		var user = userSelectedTable.datagrid('getData').rows[$(this).parents('tr').index()];
		var rows = userTable.datagrid('getData').rows;
		var index = 0;
		for(;index<rows.length;index++){
			if(rows[index].user_id == user.user_id){
				break;
			}
		}
		userTable.datagrid('uncheckRow',index);
	});
	
	userSelectWin.on('click','.edit-save',function(){
		var users = userSelectedTable.datagrid('getRows');
		grantUserTable.datagrid('loadData',users);
		var vc = parseInt(editForm.find('input[textboxname=credits_delta]').numberbox('getValue')||'0');
		editForm.find('input[textboxname=credits_total]').numberbox('setValue',total_credits-users.length*vc)
		userSelectWin.window('close');
	});
	
	function showDetail(row){
		var userList = grantTable.datagrid('getRows')[row].user_credits_delta||[];
		detailTable.datagrid('loadData',userList);
		detailWin.window('open');
	}
	
	function saveEdit(){
		if(editForm.form('validate')){
			var result_credits = editForm.find('input[textboxname=credits_total]').numberbox('getValue');
			if(result_credits<0){
				$.messager.alert('错误','现有积分不够发放，请重新设置！','error');
				return false;
			}
			var desc = editForm.find('input[textboxname=desc]').textbox('getValue');
			var param = {
				user_credits_delta: []
			};
			var users = grantUserTable.datagrid('getRows');
			for(var i=0;i<users.length;i++){
				param.user_credits_delta.push({
					user_id: users[i].user_id,
					credits_delta: editForm.find('input[textboxname=user_credits]:eq('+i+')').numberbox('getValue')
				});
			}
			Wz.showLoadingMask('正在处理中，请稍后......');
			Wz.ajax({
				type: 'post',
				url: './api/credits/create_credits_order.json',
				data: {
					desc: desc,
					param: JSON.stringify(param)
				},
				success: function(json){
	    			Wz.hideLoadingMask();
					if(json.result == 'SUCC'){
						grantTable.datagrid('reload');
						freshCredits();
						editWin.window('close');
					}else{
						$.messager.alert('错误',json.fail_text,'error');
					}
				}
			});
		}
	}
	
	function freshCredits(){
		Wz.ajax({
			url: './api/credits/get_credits.json',
			success: function(json){
				total_credits = json.credits || 0;
				$('.credits-grant-info-wrap em').text(total_credits);
			}
		});
	}
	
	return {
		showDetail: showDetail,
		saveEdit: saveEdit,
	}
}()