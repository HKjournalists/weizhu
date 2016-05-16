/*
 *请假管理-请假管理 
 * 
 */
Wz.namespace('Wz.absence');
Wz.absence.absenceManage = function(){
	 $.parser.parse('#main-contain');
	 var query_params = {
		 user_name:'',
		 action:'',
		 start_time:'',
		 end_time:''
	 };
	 var searchForm = $('#absence-search-form');
	 var editWin = $('#absence-edit-win');
	 var editForm = $('#absence-edit-form');
	 
	 var startTime = editForm.find('input[name=start_time]').datetimebox({
    	editable: false,
        required: true,
        width: 350
    });
    var preEndTime = editForm.find('input[name=pre_end_time]').datetimebox({
    	editable: false,
        required: true,
        width: 350
    });
    var facEndTime = editForm.find('input[name=fac_end_time]').datetimebox({
    	editable: false,
        required: true,
        width: 350
    });
	 
	 var absenceTable = $('#absence-table').datagrid({
		 url:'./api/absence/get_absence_list.json',
		 queryParams:query_params,
		 fitColumns: true,
		 checkOnSelect: true,
		 striped:true,
		 pagination: true,
	     rownumbers: true,
	     pageSize: 20,
	     frozenColumns:[[{
	    	 field:'user_name',
	    	 title:'姓名',
	    	 width:'80px',
	    	 align:'center'
	     },{
	    	 field:'type',
	    	 title:'请假类型',
	    	 width:'80px',
	    	 align:'center'
	     },{
	    	 field:'start_time',
	    	 title:'开始时间',
	    	 width:'140px',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return !!val?Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss'):'';
	         }
	     },{
	    	 field:'pre_end_time',
	    	 title:'结束时间',
	    	 width:'140px',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	            	var result = '';
	            	if(!!val){
	            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
	            	}
	            	return result;
	            }
	     },{
	    	 field:'fac_end_time',
	    	 title:'销假时间',
	    	 width:'140px',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	            	var result = '';
	            	if(!!val){
	            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
	            	}
	            	return result;
	            }
	     },{
	    	 field:'days',
	    	 title:'请假天数',
	    	 width:'60px',
	    	 align:'center'
	     },{
	    	 field:'absence_id',
	    	 title:'操作',
	    	 width:'80px',
	    	 align:'center',
	    	 formatter: function(val,obj,row){
	    		 return '<a class="tablelink edit-btn" onclick="Wz.absence.absenceManage.editAbsence('+row+')" href="javascript:void(0);">修改</a>';
	    	 }
	     }]],
	     columns:[[{
	    	 field:'user_team',
	    	 title:'部门',
	    	 width:'250px',
	    	 align:'left',
	    	 formatter: function(val,obj,row){
	    		 var result = [];
	    		 for(var i=0;i<val.length;i++){
	    			 result.push(val[i].team_name);
	    		 }
	    		 return result.join('-')
	    	 }
	     },{
	    	 field:'mobile_no',
	    	 title:'手机号码',
	    	 width:'120px',
	    	 align:'lest'
	     },{
	    	 field:'create_time',
	    	 title:'创建时间',
	    	 width:'120px',
	    	 align:'center'
	     },{
	    	 field:'desc',
	    	 title:'备注信息',
	    	 width:'120px'
	     },{
	    	 field:'user_list',
	    	 title:'通知人',
	    	 width:'120px',
	    	 formatter: function(val,obj,row){
	    		 var result = [];
	    		 for(var i=0;i<val.length;i++){
	    			 result.push(val[i].user_name);
	    		 }
	    		 return result.join(',');
	    	 }
	     }]],
	     toolbar:[{
	    	 id: 'absence-export-btn',
	    	 text: '导出筛选结果',
	    	 disabled: !Wz.getPermission('company/user/export'),
	    	 iconCls: 'icon-export',
	    	 handler: function(){
	            	var param = [];
	            	for(var name in query_params){
	                	param.push(name + '=' + query_params[name]);
	                }
	            	Wz.downloadFile("./api/absence/download_absence.json?_t=" + new Date().getTime()+'&'+param.join('&'));
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
	                rows: data.absence_list
	            };
	        }
	 });
	 
	searchForm.find('.search-btn').click(function(){
		query_params.user_name = searchForm.find('input[name=user_name]').val();
		query_params.start_time = searchForm.find('input[name=start_time]').val();
		query_params.end_time = searchForm.find('input[name=end_time]').val();
		query_params.action = searchForm.find('input[name=action]').val();
		if(query_params.start_time != ''){
			query_params.start_time = new Date(query_params.start_time).getTime()/1000;
		}
		if(query_params.end_time != ''){
			query_params.end_time = new Date(query_params.end_time).getTime()/1000;
		}
		absenceTable.datagrid('reload');
	});
	searchForm.find('.reset-btn').click(function(){
		searchForm.form('reset');
	});
	 
	function editAbsence(row){
		 var absence = $.extend({},absenceTable.datagrid('getRows')[row]);
		 var team = [];
		 for(var i=0;i<absence.user_team.length;i++){
			 team.push(absence.user_team[i].team_name);
		 }
		 absence.team = team.join('-');
		 absence.start_time = (!!absence.start_time?Wz.dateFormat(new Date(absence.start_time*1000),'yyyy-MM-dd hh:mm:ss'):'');
		 absence.pre_end_time = (!!absence.pre_end_time?Wz.dateFormat(new Date(absence.pre_end_time*1000),'yyyy-MM-dd hh:mm:ss'):'');
		 absence.fac_end_time = (!!absence.fac_end_time?Wz.dateFormat(new Date(absence.fac_end_time*1000),'yyyy-MM-dd hh:mm:ss'):'');
		 editForm.form('reset').form('load',absence);
		 editWin.window('open');
	 }
	 
	function saveEdit(){
		if(editForm.form('validate')){
			var absence_id = editForm.find('input[name=absence_id]').val();
			var type = editForm.find('input[textboxname=type]').combobox('getValue');
			var start_time = editForm.find('input[name=start_time]').val();
			var pre_end_time = editForm.find('input[name=pre_end_time]').val();
			var fac_end_time = editForm.find('input[name=fac_end_time]').val();
			var days = editForm.find('input[textboxname=days]').numberbox('getValue');
			var desc = editForm.find('input[textboxname=desc]').numberbox('getValue');
			if(start_time > pre_end_time || start_time > fac_end_time){
	    		$.messager.alert('错误','请假开始时间不能大于请假结束时间！','error');
	    		return false;
	    	}

			start_time = Wz.parseDate(start_time).getTime()/1000;
			pre_end_time = Wz.parseDate(pre_end_time).getTime()/1000;
			fac_end_time = Wz.parseDate(fac_end_time).getTime()/1000;

			Wz.showLoadingMask('正在处理中，请稍后......');
			Wz.ajax({
				type: 'post',
				url: './api/absence/update_absence.json',
				data: {
					absence_id: absence_id,
					type: type,
					start_time: start_time,
					pre_end_time: pre_end_time,
					fac_end_time: fac_end_time,
					days: days,
					desc: desc
				},
				success: function(json){
					Wz.hideLoadingMask();
					if(json.result == 'SUCC'){
	    				editWin.window('close');
	    				absenceTable.datagrid('reload');		
	    			}else{
	    				$.messager.alert('错误',json.fail_text,'error');
	    			}
				}
			});
		}
	}
	return {
		editAbsence: editAbsence,
		saveEdit: saveEdit
	};
}()