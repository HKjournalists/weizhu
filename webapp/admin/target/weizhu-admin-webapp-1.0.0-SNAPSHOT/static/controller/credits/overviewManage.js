Wz.namespace('Wz.credits');
Wz.credits.overviewManage = function(){
	$.parser.parse('#main-contain');
	var query_params = {
		user_name:'',
		team_id: ''
	};
	var searchForm = $('#credits-overview-search-form');
	var editWin = $('#credits-overview-edit-win');
	var editForm = $('#credits-overview-edit-form');
	var detailWin = $('#credits-overview-detail-win');
	
	var searchTeam = searchForm.find('input[name=team_id]').combotree({
		panelWidth: 200,
    	url: './api/user/get_all_team.json',
    	editable: false,
    	cascadeCheck: false,
    	onShowPanel: function(){
    		searchTeam.combotree('reload');
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
    		return data;
    	}
	});
	
	var overviewTable = $('#credits-overview-table').datagrid({
        url: './api/credits/get_user_credits.json',
        queryParams: query_params,
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
            width: '8%',
            align: 'left'
        },{
            field: 'user_team',
            title: '部门',
            width: '20%',
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
            title: '手机号',
            width: '10%',
            align: 'right'
        },{
            field: 'total_credits',
            title: '总积分',
            width: '10%',
            align: 'right'
        },{
            field: 'expense_credits',
            title: '已兑换积分',
            width: '10%',
            align: 'right'
        },{
            field: 'useable_credits',
            title: '可用积分',
            width: '10%',
            align: 'right'
        },{
            field: 'user_id',
            title: '积分详情',
            width: '10%',
            align: 'center',
            hidden: !(Wz.getPermission('credits/overview/detail')),
            formatter: function(val,obj,row){
                return '<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.credits.overviewManage.showDetail('+val+')">查看</a>';
            }
        },{
            field: 'opt',
            title: '操作',
            width: '20%',
            align: 'center',
            hidden: !(Wz.getPermission('credits/overview/update')||Wz.getPermission('credits/overview/clean')),
            formatter: function(val,obj,row){
                return (Wz.getPermission('credits/overview/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.credits.overviewManage.editCredits('+row+')">修改积分</a>':'') + 
                	(Wz.getPermission('credits/overview/clean')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.credits.overviewManage.clearCredits('+obj.user_id+')">积分清零</a>':'');
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
                total: data.filtered_size,
                rows: data.credits
            };
        }
    });
	 
	searchForm.find('.search-btn').click(function(){
		query_params.user_name = searchForm.find('input[name=user_name]').val();
		query_params.team_id = searchForm.find('input[name=team_id]').val();
		overviewTable.datagrid('reload');
	});
	searchForm.find('.reset-btn').click(function(){
		searchForm.form('reset');
	});
	
	var detailParam = {
		user_id: ''
	};
	
	var detailTable = $('#credits-overview-detail-table').datagrid({
        url: './api/credits/get_credits_order.json',
        queryParams: detailParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'desc',
            title: '事由（备注）',
            width: '60%',
            align: 'left'
        },{
            field: 'create_time',
            title: '时间',
            width: '20%',
            align: 'center',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
            field: 'credits_delta',
            title: '积分',
            width: '10%',
            align: 'right'
        }]],
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
            return {
                total: data.filtered_size,
                rows: data.credits_order
            };
        }
    });
	
	editForm.find('input[textboxname=credits_delta]').numberbox({
		onChange: function(newValue,oldValue){
			var value = editForm.find('input[textboxname=useable_credits]').numberbox('getValue');
			var flag = editForm.find('input[textboxname=flag]').combobox('getValue');
			editForm.find('input[textboxname=useable_credits]').numberbox('setValue',parseInt(value||'0')+parseInt(flag+((parseInt(newValue||'0')-parseInt(oldValue||'0'))||'0')));
		}
	})
	editForm.find('input[textboxname=flag]').combobox({
		onChange: function(newValue,oldValue){
			var value = editForm.find('input[textboxname=useable_credits]').numberbox('getValue');
			var addValue = editForm.find('input[textboxname=credits_delta]').numberbox('getValue');
			editForm.find('input[textboxname=useable_credits]').textbox('setValue',parseInt(value||'0')+2*parseInt(newValue+(parseInt(addValue||'0')||'0')));
		}
	});
	
	function showDetail(user_id){
		detailParam.user_id = user_id;
		detailTable.datagrid('reload');
		detailWin.window('open');
	}
	
	function editCredits(row){
		var user = overviewTable.datagrid('getRows')[row];
		editForm.form('reset');
		editForm.find('input[name=user_id]').val(user.user_id);
		editForm.find('input[textboxname=useable_credits]').numberbox('setValue',user.useable_credits);
		editWin.window({title:'积分修改【'+user.user_name+'】'}).window('open');
	}
	
	function saveEdit(){
		if(editForm.form('validate')){
			var result_credits = editForm.find('input[textboxname=useable_credits]').numberbox('getValue');
			if(result_credits<0){
				$.messager.alert('错误','修改后现有积分不能为负值！','error');
				return false;
			}
			var desc = editForm.find('input[textboxname=desc]').textbox('getValue');
			var param = {
				user_credits_delta: [{
					user_id: editForm.find('input[name=user_id]').val(),
					credits_delta: parseInt(editForm.find('input[textboxname=flag]').combobox('getValue') + editForm.find('input[textboxname=credits_delta]').numberbox('getValue'))
				}]
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
						overviewTable.datagrid('reload');
						editWin.window('close');
					}else{
						$.messager.alert('错误',json.fail_text,'error');
					}
				}
			});
		}
	}
	
	function clearCredits(user_id){
		$.messager.confirm('提示','清零操作将回收当前人员名下所有可用积分，回收的积分会归入企业现有积分。请确认清零吗？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/credits/clear_user_credits.json',
    				data: {
    					user_id: user_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						overviewTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	}); 
	}
	
	return {
		showDetail: showDetail,
		editCredits: editCredits,
		saveEdit: saveEdit,
		clearCredits: clearCredits
	}
}()