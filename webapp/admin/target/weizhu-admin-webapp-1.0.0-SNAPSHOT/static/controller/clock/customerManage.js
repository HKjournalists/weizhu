/*
 * 产品闹钟-顾客管理
 * 
 * 
 */
Wz.namespace('Wz.clock');
$.parser.parse('#main-contain');
Wz.clock.customerManage = function(){
var query_params = {
		customer_name:'',
		has_product:'',
		saler_id:''
};
var query_param_user = {
		team_id: '',
    	position_id: '',
    	keyword: ''
};
var query_param_product = {
		product_name:''
};
var query_param_communite = {
		customer_id: ''
};
var query_param_buyproduct = {
		customer_id:''
};
var usersearch = {
		keyword: ''
}
var searchcustomerform = $('#clock-customer-search-form');
var customeredit = $('#clock-customer-edit');
var customereditform = $('#clock-customer-edit-form');
var customerassign = $('#clock-customer-assign');
var editassignform = $('#clock-customer-assign-form');
var importWin = $('#business-user-import-win');
var productlist = $('#clock-customer-product');
var productlistsearch = $('#get-product-list');
var communitecord = $('#customer_communicate_remark');
var newcustomerproductform = $('#new-customer-product');
var customerproductoperation=$('#customer_product_operation');
var suctomerproducadd = $('#customer_product_add');
var importWin = $('#clock-customer-import-win');
var importForm = $('#clock-customer-import-form');
var customerTable = $('#clock-customer-table').datagrid({
	url:'./api/tools/productclock/get_customer.json',
	queryParams: query_params,
    fitColumns: true,
    checkOnSelect: true,
    striped: true,
    fit: true,
   // nowrap:false,
    pagination: true,
    rownumbers: true,
    pageSize: 20,
    frozenColumns:[[{
    	field: 'ck',
        checkbox: true,
    },{
    	field:'customer_name',
    	title:'客户姓名',
    	width:'80px',
    	align:'center'
    },{
    	field:'mobile_no',
    	title:'手机号',
    	width:'100px',
    	align:'center'
    },{
    	field:'gender',
    	title:'性别',
    	width:'50px',
    	align:'center',
    	formatter: function(val,obj,row){
        	return val=='FEMALE'?'女':'男';//Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
        }
    },{
    	field:'state',
    	title:'状态',
    	width:'80px',
    	align:'center',
    	formatter: function(val,obj,row){
    		if(obj.product_list.length == 0){
    			return '未成交';
    		}
    		else{
    			return '已成交';
    		}
    		
    	}
    },{
    	field:'belong_saler',
    	title:'所属销售',
    	width:'80px',
    	align:'center'
    },{
    	field:'handle',
    	title:'操作',
    	width:'170px',
    	align:'center',
        hidden: !(Wz.getPermission('clock/customer/update')||Wz.getPermission('clock/customer/delete')||Wz.getPermission('clock/customer/product')),
    	formatter:function(val,obj,row){
    		return (Wz.getPermission('clock/customer/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.customerManage.editcustomer('+row+','+obj.customer_id+')">编辑</a>':'') +
    		(Wz.getPermission('clock/customer/delete')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.customerManage.delcustomer('+obj.customer_id+')">删除</a>':'') +
    		(Wz.getPermission('clock/customer/product')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.customerManage.editcustomerproduct('+obj.customer_id+')">购买产品详情</a>':'');
    	}
    }
    ]],
    columns:[[{
    	field:'address',
    	title:'住址',
    	width:'150px',
    	align:'center'
    },
    {
    	field:'birthday_lunar',
    	title:'生日（阴历）',
    	width:'150px',
    	align:'center',
    	formatter: function(val,obj,row){
    		return !!val?calendar.stamp2lunar(val):'';
        }
    },{
    	field:'birthday_solar',
    	title:'生日（阳历）',
    	width:'150px',
    	align:'center',
    	formatter: function(val,obj,row){
    		return !!val?calendar.stamp2solar(val):'';
        }
    },{
    	field:'wedding_lunar',
    	title:'结婚纪念日（阴历）',
    	width:'150px',
    	align:'center',
    	formatter: function(val,obj,row){
    		return !!val?calendar.stamp2lunar(val):'';
        }
    },{
    	field:'wedding_solar',
    	title:'结婚纪念日（阳历）',
    	width:'150px',
    	align:'center',
    	formatter: function(val,obj,row){
    		return !!val?calendar.stamp2solar(val):'';
        }
    },{
    	field:'product_buy',
    	title:'已购产品',
    	width:'150px',
    	align:'center',
    	formatter: function(val,obj,row){
    		var product_list=obj.product_list,team=[];
    		for(var i=0,len=product_list.length;i<len;i++){
    			team.push(product_list[i].product_name);
    		}
    		team.join(',');
    		return '<span title="'+team+'" class="easyui-tooltip">'+team + '</span>';
    	}
    },{
    	field:'communication_record',
    	title:'沟通记录',
    	width:'80px',
    	align:'center',
        hidden: !(Wz.getPermission('clock/customer/communicate')),
    	formatter: function(val,obj,row){
    		return ('<a href="javascript:void(0)" class="tablelink comm-btn" onclick="Wz.clock.customerManage.commrecord('+obj.customer_id+')">沟通记录</a>')
    	}
    },{
    	field:'remark',
    	title:'备注',
    	width:150,
    	align:'center',
    	formatter: function(val,obj,row){
    		return !!val?obj.remark:'';
    	}
    },{
    	field:'create_time',
    	title:'创建时间',
    	width:150,
    	align:'center',
    	formatter: function(val,obj,row){
    		var result='';
    		if(!!val){
    			result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
    		}
    		return result;
    	}
    }
    ]],
    toolbar:[{
    	id:'clock-customer-add',
    	text:Wz.lang.common.gridtool_create_btn,
    	iconCls:'icon-add',
		disabled: !Wz.getPermission('clock/product/create'),
    	handler: function(){
    		//var borther = customeredit.find('div[name=buy_product_true]');
    		//borther.empty();
    		///customerproductlist.datagrid('uncheckAll');
    		customereditform.form('reset');
    		customereditform.find('input[name=customer_id]').val('');
    		customereditform.find('input[name=customer_name]').parents('.form-item').show();
    		customeredit.window({title:'新建客户信息'}).window('open');
    	}
    },{
    	id:'clcok-customer-del',
    	text:Wz.lang.common.grid_del_btn,
    	iconCls:'icon-remove',
		disabled: !Wz.getPermission('clock/customer/delete'),
    	handler: function(){
    		var users=customerTable.datagrid('getChecked');
    		if(users.length == 0){
    			$.messager.alert('提示','请选择需要删除的客户信息','info');
        		return false;
    		}
    		var customer_id = [];
			for(var i=0;i<users.length;i++){
				customer_id.push(users[i].customer_id);
			}
			delcustomer(customer_id.join(','));
    	}
    },{
    	id: 'clock-customer-import-btn',
        text: '导入客户信息',
        disabled: !Wz.getPermission('clock/customer/import'),
        iconCls: 'icon-import',
        handler: function(){
        	importWin.window('open');
        }
    },{
    	id: 'clock-customer-export-btn',
        text: '导出客户信息',
        disabled: !Wz.getPermission('clock/customer/export'),
        iconCls: 'icon-export',
        handler: function(){
        	var param = [];
        	for(var name in query_params){
            	param.push(name + '=' + query_params[name]);
            }
        	Wz.downloadFile("./api/tools/productclock/download_customer.json?_t=" + new Date().getTime()+'&'+param.join('&'));
        }
    },{
    	id:'clock-customer-assign',
    	text:'指派',
    	iconCls: 'icon-redo',
		disabled: !Wz.getPermission('clock/customer/assigned'),
    	handler: function(){
    		var customer_list=customerTable.datagrid('getSelections');
    		if(customer_list.length == 0){
    			$.messager.alert('提示','请先选择客户','info');
    			return false;
    		}
    		else{
    			editassignform.form('reset');
    			customerassigntable.datagrid('reload');
    			customerassign.window({title:'指派销售人员'}).window('open');
    		}
    		
    	}
    }
    ],
    changePages: function(params,pageObj){
    	$.extend(params,{
    		start: (pageObj.page-1)*pageObj.rows,
    		length: pageObj.rows
    	});
    },
    loadFilter: function(data){
        return {
            total: data.filtered_size, //数据过滤
            rows: data.customer_list //数据格式重定义
        };
    }
});
var customerassigntable = $('#clock-customer-assign-table').datagrid({
	url:'./api/user/get_user_list.json',
	queryParams: query_param_user,
    fitColumns: true,
    checkOnSelect: true,
    singleSelect:true,
    striped: true,
    fit: true,
    pagination: true,
    rownumbers: true,
    pageSize: 20,
    columns:[[{
    	field:'ck',
    	checkbox:false,
    	formatter: function(val, obj, row){
              return '<input type="radio" name="selectRadio" id="selectRadio' + row + '"    value="' + obj.oid + '" />';
          }
    },{
    	field:'raw_id',
    	title:'工号',
    	width:'80px',
    	hidden:true,
    	align:'center'
    },{
    	field:'user_name',
    	title:'姓名',
    	width:'100px',
    	align:'center'
    },{
    	field:'gender',
    	title:'性别',
    	width:'100px',
    	align:'center',
    	 formatter: function(val,obj,row){
         	return val=='FEMALE'?'女':'男';//Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
         }
    },{
    	field:'mobile_no',
    	title:'手机号',
    	width:'150px;',
    	align:'center'
    },{
    	field:'team',
    	title:'部门',
    	width:'250px',
    	align:'center',
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
    	field:'position',
    	title:'职务',
    	width:'180px',
    	align:'center',
    	 formatter: function(val,obj,row){
         	return !!val?val.position_name: '';
         }
    },{
    	field:'level',
    	title:'职级',
    	hidden:true,
    	width:'100px',
    	align:'center',
    	formatter: function(val,obj,row){
        	return !!val?val.level_name: '';
        }
    }
    ]],
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
    },
    onClickRow: function(index,row){
    	var rows = 'selectRadio' + index + '';
    	$('#'+rows+'')[0].checked="checked";
    }
});

var postcustomerproduct = newcustomerproductform.find('input[name=product_name]').combogrid({
	panelWidth:80,
	url:'./api/tools/productclock/get_product.json',
	fitColumns: true,
    checkOnSelect: false,
    striped: true,
    pagination: true,
    rownumbers: true,
    required: false,
    pageSize: 20,
    idField: 'product_id',
    textField: 'product_name',
    keyHandler: {
    	query: function(q) {
    		postUser.combogrid("grid").datagrid("reload", { 'keyword': q });  
    		postUser.combogrid("setValue",q);
        } 
    },
    width:250,
    panelWidth: 300,
    frozenColumns: [[{
    	field: 'product_name',
    	title: '产品名称',
    	width: '100px',
    	align: 'center'
    },{
    	field:'product_desc',
    	tifle: '产品简介',
    	width:'150px',
    	align: 'center'
    }]],
    changePages: function(params,pageObj){
    	$.extend(params,{
    		start: (pageObj.page-1)*pageObj.rows,
    		length: pageObj.rows
    	});
    },loadFilter: function(data){
        return {
            total: data.filtered_size,
            rows: data.product_list
        };
    }
});
var postUser = searchcustomerform.find('input[name=sale_by]').combogrid({
	panelWidth:80,
	queryParams: usersearch,
	url: './api/user/get_user_list.json',
    fitColumns: true,
    checkOnSelect: true,
    striped: true,
    pagination: true,
    rownumbers: true,
    required: false,
    multiple: true,
    pageSize: 20,
    editable: false,
    idField: 'user_id',
    textField: 'user_name',
    keyHandler: {
    	query: function(q) {
    		postUser.combogrid("grid").datagrid("reload", { 'keyword': q });  
    		postUser.combogrid("setValue", q);
        } 
    },
    width: 150,
    panelWidth: 500,
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
    },
    toolbar: '#customer_user_search_tb'
});
$('#customer_user_search_tb').find('.search-btn').click(function(){
	usersearch.keyword = $('#customer_user_search_tb').find('input[name=keyword]').val();
	postUser.combogrid('grid').datagrid('reload');
});
var chooseproduct = $('#choose-product-list').datagrid({
	data:[],
	fitColumns: true,
    striped: true,
    fit: true,
    pagination: true,
    rownumbers: true,
    pageSize: 20,
    columns:[[{
		field: 'product_name',
		title: '选择的产品名称',
		width: '100px',
		align: 'center',
		formatter: function(val,obj,row){
			return !!val?obj.product_name:'';
		}
	},{
		field: 'img_name',
		title: '选择的产品图片',
		width: '100px',
		align: 'center',
		formatter: function(val,obj,row){
			return '<img src="'+obj.image_url+'" style="width:115px;height:65px;"/>'
		}
	},{
		field: 'product_desc',
		title: '选择的产品描述',
		width: '140px',
		align: 'center',
		formatter: function(val,obj,row){
			return !!val?obj.product_desc:'';
		}
	},{
        field: 'caozuo',
        title: '操作',
        align: 'center',
        width: 60,
        formatter: function(val,obj,row){
        	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
        }
    }]]
});
var communiteremark = $('#customer_communicate_table').datagrid({
	url: './api/tools/productclock/get_communicate_record.json',
	queryParams: query_param_communite,
	fitColumns: true,
    striped: true,
    fit: true,
    nowrap:false,
    pagination: true,
    rownumbers: true,
    pageSize: 20,
    columns: [[{
    	field: 'ck',
    	checkbox:true,
    },{
    	field: 'create_time',
    	title: '沟通记录时间',
    	width: '150px',
    	align: 'center',
    	formatter: function(val,obj,row){
    		var result='';
    		if(!!val){
    			result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
    		}
    		return result;
    	}
    },{
    	field: 'content_text',
    	title: '沟通记录',
    	width: '350px',
    	align: 'center'
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
            rows: data.communicate_record
        };
    }
});
var customerproductlist = $('#customer_product_list').datagrid({
	url:'./api/tools/productclock/get_customer_product.json',
	queryParams: query_param_buyproduct,
    fitColumns: true,
    checkOnSelect: false,
    striped: true,
    fit: true,
    pagination: true,
    rownumbers: true,
    pageSize: 20,
    columns:[[{
    	field:'ck',
    	checkbox:true
    },{
    	field:'product_name',
    	title: '产品名称',
    	width: '100px',
    	align: 'center',
    },{
    	field:'default_remind_day',
    	title: '提醒周期',
    	width: '80px',
    	align:'center',
    	formatter: function(val,obj,row){
    		return (val?obj.default_remind_day+'天':'');
    	}
    },{
    	field: 'image_name',
    	title: '产品图片',
    	width: '100px',
    	align: 'center',
    	formatter: function(val,obj,row){
    		if(obj.image_name==''){
    			return '<span>无图！</span>'
    		}else{
    			return '<img src="'+obj.image_url+'" style="width:115px;height:65px;"/>'
    		}
    	}
    },{
    	field: 'buy_time',
    	title:'购买日期',
    	width:'100px',
    	align:'center',
    	formatter: function(val,obj,row){
    		var result='';
    		if(!!val){
    			result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd');
    		}
    		return result;
    	}
    },{
    	field:'caozuo',
    	title:'操作',
    	width:'120px',
    	align:'center',
    	formatter: function(val,obj,row){
    		return ('<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.customerManage.ediecustomerproudctr('+row+')">编辑</a>') +
    		('<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.customerManage.delproduct('+obj.product_id+')">删除</a>') ;
    	}
    }]],
    toolbar: [{
    	id:'customer-product-add',
    	text:Wz.lang.common.gridtool_create_btn,
    	iconCls:'icon-add',
    	handler: function(){
    		newcustomerproductform.form('reset');
    		$('#customer_product_add').window('open');
    	}
    },{
    	id:'customer_product_del',
    	text:Wz.lang.common.grid_del_btn,
    	iconCls:'icon-remove',
    	handler: function(){
    		var row=customerproductlist.datagrid('getSelections');
    		var list=[];
		if(row.length == 0){
    		$.messager.alert('提示','请选择需要删除的购买产品信息','info');
    		return false;
    	}
		else{
			for(var i=0,len=row.length;i<len;i++){
			list.push(row[i].product_id);
			};
			delproduct(list.join(','));
			}
    		
    	}
    }],changePages: function(params,pageObj){
    	$.extend(params,{
    		start: (pageObj.page-1)*pageObj.rows,
    		length: pageObj.rows
    	});
    },
    loadFilter: function(data){
        return {
            total: data.product_list.length,
            rows: data.product_list
        };
    }
    
});
productlist.on('click','.del-btn',function(){
	var user = chooseproduct.datagrid('getData').rows[$(this).parents('tr').index()];
	var rows = customerproductlist.datagrid('getData').rows;
	var index = 0;
	for(;index<rows.length;index++){
		if(rows[index].product_id == user.product_id){
			break;
		}
	}
	customerproductlist.datagrid('uncheckRow',index);
});
productlist.on('click','.next-btn',function(){
	var val = $('#choose-product-list').datagrid('getData').rows;
	var len = val.length;
	var borther = customeredit.find('div[name=buy_product_true]');
	borther.empty();
	for(var i=0;i<len;i++){
		borther.append('<a href="javascript:void(0)" product_id='+val[i].product_id+'>'+val[i].product_name+'</a>');
	}
	productlist.window('close');
});
productlist.on('click','.over-btn',function(){
	productlist.window('close');
});
customeredit.find('.finish-btn').click(function(){
	var url='./api/tools/productclock/create_customer.json';
	var customer_id=customereditform.find('input[name=customer_id]').val();
	if(customer_id!=''){
		url='./api/tools/productclock/update_customer.json';
	};
	var customer_name=customereditform.find('input[name=customer_name]').val();
	var mobile_no=customereditform.find('input[name=mobile_no]').val();
	var gender=customereditform.find('input[name=gender]').val();
	var is_remind=customereditform.find('input[name=is_remind]').val();
	var days_ago_remind=customereditform.find('input[name=days_ago_remind]').val();
	var address=customereditform.find('input[name=address]').val();
	var remark=customereditform.find('input[name=remark]').val();
	if(customereditform.find('input[name=birthday_solar]').prev().val()==''){
		var birthday_solar='';
	}
	else{	
		var birthday_solar=customereditform.find('input[textboxname=birthday_solar]').calendarbox('getTimeStap');
	}
	var birthday_lunar=birthday_solar;
	if(customereditform.find('input[name=wedding_solar]').prev().val()==''){
		var wedding_solar='';
	}else{
		var wedding_solar=customereditform.find('input[textboxname=wedding_solar]').calendarbox('getTimeStap');
	}
	var wedding_lunar=wedding_solar;
	Wz.ajax({
		type:'post',
		url:url,
		data:{
			customer_name:customer_name,
			mobile_no:mobile_no,
			gender:gender,
			is_remind:is_remind,
			days_ago_remind:days_ago_remind,
			address:address,
			remark:remark,
			birthday_solar:birthday_solar,
			birthday_lunar:birthday_lunar,
			wedding_solar:wedding_solar,
			wedding_lunar:wedding_lunar,
			customer_id:customer_id
		},
		success: function(json){
			if(json.result=="SUCC"){
				customeredit.window('close');
				customerTable.datagrid('reload');
			}else{
				$.messager.alert('错误',json.fail_text,'error');
			}
		}
		
	})
/*	customereditform.form('submit',{
		url: url,
		onSubmit: function(){
			var valid = $(this).form('validate');
			if(valid){
				Wz.showLoadingMask('正在处理中，请稍后......');
			}
			return valid;
		},
		success: function(json){
			Wz.hideLoadingMask();
			json = JSON.parse(json);
			if(json.result == 'SUCC'){
				customeredit.window('close');
				customerTable.datagrid('reload');
			}else{
				$.messager.alert('错误',json.fail_text,'error');
				return false;
			}
		}
	});*/
});
customereditform.find('input[name=birthday_solar]').calendarbox();
customereditform.find('input[name=wedding_solar]').calendarbox();
var searchTeam = editassignform.find('input[name=team_id]').combotree({
	panelWidth: 350,
	url: './api/user/get_team.json',
	queryParams: {team_id:''},
	editable: false,
	cascadeCheck: false,
	onBeforeExpand: function(row){
		searchTeam.combotree('options').queryParams.team_id = row.team_id;
		return true;
	},
	onShowPanel: function(){
		searchTeam.combotree('options').queryParams.team_id = '';
		searchTeam.combotree('reload');
	},
	loadFilter: function(data){
		if(!!data){
			for(var i=0;i<data.length;i++){
				data[i].id = data[i].team_id;
				data[i].text = data[i].team_name;
				data[i].state = (data[i].has_sub_team?'closed':'open');
			}
			if(searchTeam.combotree('options').queryParams.team_id == ''){
				data.unshift({
					id: '',
					text: '---全部部门---'
				});
			}
		}
		return data;
	}
});

var searchPosition = editassignform.find('input[name=position_id]').combobox({
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
				
				var position = json.position||[];
				position.unshift({
					position_id: '',
					position_name: '---全部职务---'
				});
				success(json.position);
			}
		});
	},
	onShowPanel: function(){
		$(this).combobox('reload');
	}
});


searchcustomerform.find('.search-btn').click(function(){
	query_params.has_product=searchcustomerform.find('input[name=hasproduct]').val();
	query_params.customer_name=searchcustomerform.find('input[name=customer_name]').val();
	var ids = postUser.combogrid('getValues');
	if(postUser.combogrid('grid').datagrid('getSelections')[0] == undefined){
		ids='';
	}else{
		var user_id,uset_ids ;
		var len = postUser.combogrid('grid').datagrid('getSelections').length;
		for(var i = 0;i<len;i++){
			var usert = ids.pop();
			if(usert !=''){
				ids.push(usert);
			}
		}
		ids = ids.join(',')
		//ids = postUser.combogrid('grid').datagrid('getSelections')[0].user_id;
	}
	query_params.saler_id=ids;
	customerTable.datagrid('reload');
});
searchcustomerform.find('.reset-btn').click(function(){
	searchcustomerform.form('reset');
});
editassignform.find('.search-btn').click(function(){
	query_param_user.team_id = searchTeam.combotree('getValue');
	query_param_user.position_id = searchPosition.combotree('getValue');
	query_param_user.keyword = editassignform.find('input[name=user_name]').val();
	customerassigntable.datagrid('reload');
});
editassignform.find('.reset-btn').click(function(){
	editassignform.form('reset');
});
customerassign.find('.point-btn').click(function(){
	var customer_list=customerTable.datagrid('getSelections');
	var customer_id_list=[],list;
	var slider = customerassigntable.datagrid('getSelections');
	var len_c=customer_list.length;
	for(var i=0;i<len_c;i++){
		customer_id_list.push(customer_list[i].customer_id);
	}
	var len_s=slider.length;
	if(len_s==0){
		$.messager.alert('提示','请选择指派的人员','info');
		return false;
	}else{
		var slider_id=slider[0].user_id;
		list=customer_id_list.join(',');
		Wz.ajax({
			type: 'post',
			url: './api/tools/productclock/assigned_saler.json',
			data: {
				customer_id_list:list,
				saler_id: slider_id
			},
			success: function(json){
				if(json.result == 'SUCC'){
					customerassign.window('close');
					customerTable.datagrid('reload');
				}else{
					$.messager.alert('错误',json.fail_text,'error');
    				return false;
				}
			}
		})
	}
});
productlistsearch.find('.search-btn').click(function(){
	query_param_product.product_name=productlistsearch.find('input[name=product_name]').val();
	$('#customer-product-list').datagrid('reload');
});
customeredit.find('.over-btn').click(function(){
	customeredit.window('close');
});
importForm.find('.download-btn').click(function(){
	Wz.downloadFile('./static/res/import_customer_file.xlsx');
});
suctomerproducadd.find('.finish-btn').click(function(){
	var url = './api/tools/productclock/create_customer_product.json';
	var ids;
	var product_id=newcustomerproductform.find('input[name=product_id]').val();
	if(postcustomerproduct.combogrid('grid').datagrid('getSelections')[0] == undefined){
		ids='';
	}else{
		ids = postcustomerproduct.combogrid('grid').datagrid('getSelections')[0].product_id;
	};
	if(newcustomerproductform.find('input[name=old_product_id]').val()==''){
		newcustomerproductform.find('input[name=product_id]').val(ids);
		
	}else{
		url='./api/tools/productclock/update_customer_product.json';
		if(ids==''){
			newcustomerproductform.find('input[name=new_product_id]').val(product_id);
		}else{
			newcustomerproductform.find('input[name=new_product_id]').val(ids);
		}
	}
	var dateStr = newcustomerproductform.find('input[name=buy_time_old]').val();
	var newstr = dateStr.replace(/-/g,'/'); 
    var date =  new Date(newstr); 
    var time_str = date.getTime().toString();
    var buy_time=time_str.substr(0, 10);
	newcustomerproductform.find('input[name=buy_time]').val(buy_time);
	//newcustomerproductform.find('input[name=buy_time]').val(newcustomerproductform.find('input[name=buy_time_old]').val());
	newcustomerproductform.find('input[name=customer_id]').val(query_param_buyproduct.customer_id);
	newcustomerproductform.form('submit',{
		url: url,
		onSubmit: function(){
			var valid = $(this).form('validate');
			if(valid){
				Wz.showLoadingMask('正在处理中，请稍后......');
			}
			return valid;
		},
		success: function(json){
			Wz.hideLoadingMask();
			json = JSON.parse(json);
			if(json.result == 'SUCC'){
				suctomerproducadd.window('close');
				customerproductlist.datagrid('reload');
				customerTable.datagrid('reload');
			}else{
				$.messager.alert('错误',json.fail_text,'error');
				return false;
			}
		}
	});
	
});
function delcustomer(customer_id){
	$.messager.confirm('提示','请确认是否要删除顾客信息？',function(ok){
		if(ok){
			Wz.ajax({
				type: 'post',
				url: './api/tools/productclock/delete_customer.json',
				data: {
					customer_id_list: customer_id
				},
				success: function(json){
					if(json.result == 'SUCC'){
						customerTable.datagrid('reload');
					}else{
						$.messager.alert('错误',json.fail_text,'error');
	    				return false;
					}
				}
			});    			
		}
	}); 
}

function importcustomer(){
	importForm.form('submit',{
		url: './api/tools/productclock/import_customer.json?company_id='+Wz.cur_company_id,
		onSubmit: function(){
			Wz.showLoadingMask();
		},
		success: function(result){
			Wz.hideLoadingMask();
			result = $.parseJSON(result);
			if(result.result == 'SUCC'){
				importWin.window('close');
				customerTable.datagrid('reload');
				$.messager.alert('提示','<p style="padding-left:42px">导入新增顾客</p>','info');
			}else{
				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
					if(ok){
						Wz.downloadFile("./api/tools/productclock/get_import_fail_log.json?_t=" + new Date().getTime());
					}
				});
			}
		}
	});
};
function commrecord(id){
	communitecord.window({title:'沟通记录'}).window('open');
	query_param_communite.customer_id = id;
	communiteremark.datagrid('reload');
}
function buyproduct(){
	productlist.window({title:'产品列表'}).window('open');	
}
function editcustomer(row,customer_id){
	var editcustomermessage = customerTable.datagrid('getRows')[row];
	//editcustomermessage.customer_id=customer_id;
	customereditform.form('reset');
	customereditform.form('load',editcustomermessage);
	customereditform.find('input[textboxname=customer_name]').val(editcustomermessage.customer_name);
	customereditform.find('input[textboxname=mobile_no]').val(editcustomermessage.mobile_no);
	customereditform.find('input[textboxname=gender]').val(editcustomermessage.gender);
	customereditform.find('input[textboxname=is_remind]').val(editcustomermessage.is_remind);
	customereditform.find('input[textboxname=days_ago_remind]').val(editcustomermessage.days_ago_remind);
	customereditform.find('input[textboxname=address]').val(editcustomermessage.address);
	customereditform.find('input[textboxname=remark]').val(editcustomermessage.remark);
	if(editcustomermessage.birthday_solar!=undefined){
		customereditform.find('input[textboxname=birthday_solar]').calendarbox('setTimeStap',editcustomermessage.birthday_solar);
	}
	if(editcustomermessage.birthday_lunar!=undefined){
		customereditform.find('input[name=birthday_lunar]').val(editcustomermessage.birthday_lunar);
	}
	if(editcustomermessage.wedding_solar!=undefined){
		customereditform.find('input[textboxname=wedding_solar]').calendarbox('setTimeStap',editcustomermessage.wedding_solar);
	}
	if(editcustomermessage.wedding_lunar!=undefined){
		customereditform.find('input[name=wedding_lunar]').val(editcustomermessage.wedding_lunar);	
	}
	customereditform.find('input[name=customer_id]').val(editcustomermessage.customer_id);
	customeredit.window({title:'编辑顾客信息'}).window('open');
}
function ediecustomerproudctr(row){
	var customerproduct = $.extend({},customerproductlist.datagrid('getRows')[row]);
	customerproduct.remind_period_day=customerproduct.default_remind_day;
	customerproduct.old_product_id=customerproduct.product_id;
	newcustomerproductform.form('reset');
	customerproduct.buy_time_old= Wz.dateFormat(new Date(customerproduct.buy_time*1000),'yyyy-MM-dd');
	newcustomerproductform.form('load',customerproduct);
	$('#customer_product_add').window('open');
}
function editcustomerproduct(customer_id){
	query_param_buyproduct.customer_id=customer_id;
	customerproductlist.datagrid('reload');
	customerproductoperation.window('open');
}
function delproduct(product_id){
	$.messager.confirm('提示','是否确定删除该产品？',function(ok){
		if(ok){
			Wz.ajax({
				type: 'post',
				url: './api/tools/productclock/delete_customer_product.json',
				data: {
					product_id_list:product_id,
					customer_id:query_param_buyproduct.customer_id
				},
				success: function(json){
					if(json.result=="SUCC"){
						customerproductlist.datagrid('reload');
						customerTable.datagrid('reload');
					}else{
						$.messager.alert('错误',json.fail_text,'error');
					}
				}
				
			})
		}
	});
	
};

////////////////////////////////////////////阴历阳历日期控件////////////////////////////////////////////
var lunarInfo=[0x04bd8,0x04ae0,0x0a570,0x054d5,0x0d260,0x0d950,0x16554,0x056a0,0x09ad0,0x055d2,//1900-1909
           0x04ae0,0x0a5b6,0x0a4d0,0x0d250,0x1d255,0x0b540,0x0d6a0,0x0ada2,0x095b0,0x14977,//1910-1919
           0x04970,0x0a4b0,0x0b4b5,0x06a50,0x06d40,0x1ab54,0x02b60,0x09570,0x052f2,0x04970,//1920-1929
           0x06566,0x0d4a0,0x0ea50,0x06e95,0x05ad0,0x02b60,0x186e3,0x092e0,0x1c8d7,0x0c950,//1930-1939
           0x0d4a0,0x1d8a6,0x0b550,0x056a0,0x1a5b4,0x025d0,0x092d0,0x0d2b2,0x0a950,0x0b557,//1940-1949
           0x06ca0,0x0b550,0x15355,0x04da0,0x0a5b0,0x14573,0x052b0,0x0a9a8,0x0e950,0x06aa0,//1950-1959
           0x0aea6,0x0ab50,0x04b60,0x0aae4,0x0a570,0x05260,0x0f263,0x0d950,0x05b57,0x056a0,//1960-1969
           0x096d0,0x04dd5,0x04ad0,0x0a4d0,0x0d4d4,0x0d250,0x0d558,0x0b540,0x0b6a0,0x195a6,//1970-1979
           0x095b0,0x049b0,0x0a974,0x0a4b0,0x0b27a,0x06a50,0x06d40,0x0af46,0x0ab60,0x09570,//1980-1989
           0x04af5,0x04970,0x064b0,0x074a3,0x0ea50,0x06b58,0x055c0,0x0ab60,0x096d5,0x092e0,//1990-1999
           0x0c960,0x0d954,0x0d4a0,0x0da50,0x07552,0x056a0,0x0abb7,0x025d0,0x092d0,0x0cab5,//2000-2009
           0x0a950,0x0b4a0,0x0baa4,0x0ad50,0x055d9,0x04ba0,0x0a5b0,0x15176,0x052b0,0x0a930,//2010-2019
           0x07954,0x06aa0,0x0ad50,0x05b52,0x04b60,0x0a6e6,0x0a4e0,0x0d260,0x0ea65,0x0d530,//2020-2029
           0x05aa0,0x076a3,0x096d0,0x04bd7,0x04ad0,0x0a4d0,0x1d0b6,0x0d250,0x0d520,0x0dd45,//2030-2039
           0x0b5a0,0x056d0,0x055b2,0x049b0,0x0a577,0x0a4b0,0x0aa50,0x1b255,0x06d20,0x0ada0//2040-2049
       ];
var nStr1 = new Array('日', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十');
var nStr2 = new Array('初', '十', '廿', '卅');
var solarMonth=[31,28,31,30,31,30,31,31,30,31,30,31];
//阴历转阳历//

function lunar2solar(y,m,d,isLeapMonth) {  //参数区间1900.1.31~2100.12.1
    var leapOffset = 0;
    var leapMonth = leapMonth(y);
    var leapDay = leapDays(y);
    if(isLeapMonth&&(leapMonth!=m)) {return -1;}//传参要求计算该闰月公历 但该年得出的闰月与传参的月份并不同
    if(y==2100&&m==12&&d>1 || y==1900&&m==1&&d<31) {return -1;}//超出了最大极限值
    var day = monthDays(y,m);
    if(y<1900 || y>2100 || d>day) {return -1;}//参数合法性效验

    //计算农历的时间差
    var offset = 0;
    for(var i=1900;i<y;i++) {
        offset+=lYearDays(i);
    }
    var leap = 0,isAdd= false;
    for(var i=1;i<m;i++) {
        leap = leapMonth(y);
        if(!isAdd) {//处理闰月
            if(leap<=i && leap>0) {
                offset+=leapDays(y);isAdd = true;
            }
        }
        offset+=monthDays(y,i);
    }
    //转换闰月农历 需补充该年闰月的前一个月的时差
    if(isLeapMonth) {offset+=day;}
    //1900年农历正月一日的公历时间为1900年1月30日0时0分0秒(该时间也是本农历的最开始起始点)
    var stmap = Date.UTC(1900,1,30,0,0,0);
    var calObj = new Date((offset+d-31)*86400000+stmap);
    var cY = calObj.getUTCFullYear();
    var cM = calObj.getUTCMonth()+1;
    var cD = calObj.getUTCDate();

    return solar2lunar(cY,cM,cD);
}
//阳历转阴历//

function solar2lunar(y,m,d) { //参数区间1900.1.31~2100.12.31
    if(y<1900 || y>2100) {return -1;}//年份限定、上限
    if(y==1900&&m==1&&d<31) {return -1;}//下限
    if(!y) { //未传参 获得当天
        var objDate = new Date();
    }else {
        var objDate = new Date(y,parseInt(m)-1,d)
    }
    var i, leap=0, temp=0;
    //修正ymd参数
    var y = objDate.getFullYear(),m = objDate.getMonth()+1,d = objDate.getDate();
    var offset = (Date.UTC(objDate.getFullYear(),objDate.getMonth(),objDate.getDate()) - Date.UTC(1900,0,31))/86400000;
    for(i=1900; i<2101 && offset>0; i++) { temp=lYearDays(i); offset-=temp; }
    if(offset<0) { offset+=temp; i--; }

    //是否今天
    var isTodayObj = new Date(),isToday=false;
    if(isTodayObj.getFullYear()==y && isTodayObj.getMonth()+1==m && isTodayObj.getDate()==d) {
        isToday = true;
    }
    //星期几
    var nWeek = objDate.getDay(),cWeek = nStr1[nWeek];
    if(nWeek==0) {nWeek =7;}//数字表示周几顺应天朝周一开始的惯例
    //农历年
    var year = i;

    var leap = leapMonth(i); //闰哪个月
    var isLeap = false;

    //效验闰月
    for(i=1; i<13 && offset>0; i++) {
        //闰月
        if(leap>0 && i==(leap+1) && isLeap==false){
            --i;
            isLeap = true; temp = leapDays(year); //计算农历闰月天数
        }
        else{
            temp = monthDays(year, i);//计算农历普通月天数
        }
        //解除闰月
        if(isLeap==true && i==(leap+1)) { isLeap = false; }
        offset -= temp;
    }

    if(offset==0 && leap>0 && i==leap+1)
        if(isLeap){
            isLeap = false;
        }else{
            isLeap = true; --i;
        }
    if(offset<0){ offset += temp; --i; }
    //农历月
    var month = i;
    //农历日
    var day = offset + 1;

    return {'lYear':year,'lMonth':month,'lDay':day};
};
function monthDays(y,m) {       //返回农历y年m月（非闰月）的总天数
    if(m>12 || m<1) {return -1}//月份参数从1至12，参数错误返回-1
    return( (lunarInfo[y-1900] & (0x10000>>m))? 30: 29 );
};
function leapDays(y) { //返回农历y年闰月的天数
    if(leapMonth(y)) {
        return((lunarInfo[y-1900] & 0x10000)? 30: 29);
    }
    return(0);
};
function leapMonth(y) { //闰字编码 \u95f0 返回农历y年闰月是哪个月；若y年没有闰月 则返回0
    return(lunarInfo[y-1900] & 0xf);
};
function lYearDays(y) { //返回农历y年一整年的总天数
    var i, sum = 348;
    for(i=0x8000; i>0x8; i>>=1) { sum += (lunarInfo[y-1900] & i)? 1: 0; }
    return(sum+leapDays(y));
};
////////////////////////////////////////////////////////////////////////////////////////////
return  {
	importcustomer:importcustomer,
	delcustomer:delcustomer,
	buyproduct:buyproduct,
	editcustomer:editcustomer,
	commrecord:commrecord,
	lYearDays:lYearDays,
	leapMonth:leapMonth,
	leapDays:leapDays,
	monthDays:monthDays,
	solar2lunar:solar2lunar,
	lunar2solar:lunar2solar,
	editcustomerproduct:editcustomerproduct,
	ediecustomerproudctr:ediecustomerproudctr,
	delproduct:delproduct
	}
}()