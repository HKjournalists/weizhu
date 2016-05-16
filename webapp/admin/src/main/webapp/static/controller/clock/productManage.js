/*
 * 产品闹钟-产品管理
 * 
 * 
 */
Wz.namespace('Wz.clock');
Wz.clock.productManage = function(){
$.parser.parse('#main-contain');
var query_params = {
		product_name:''
}
var addnewproduct=$('#product-new-add');
var newproductform=$('#product-add-form');
var editWin = $('#product-edit');
var importWin = $('#clock-product-import-win');
var importForm = $('#product-import-forms')
var searchproduct = $('#clock-product-search-form');
var userTable = $('#clock-product-table').datagrid({
	url:'./api/tools/productclock/get_product.json',
	queryParams:query_params,
	fitColumns: true,
	checkOnSelect: false,
    striped: true,
    fit: true,
    pagination: true,
    rownumbers: true,
    pageSize: 20,
	columns: [[{
		field: 'ck',
		checkbox: true
	},{
		field: 'product_name',
		title: '产品名称',
		width: '100px',
		align: 'center',
		formatter: function(val,obj,row){
			return !!val?obj.product_name:'';
		}
	},{
		field: 'img_name',
		title: '产品图片',
		width: '150px',
		align: 'center',
		formatter: function(val,obj,row){
			if(obj.image_name==''){
				return '<span>无图，添加图片请点编辑</span>'
			}else{
				return '<img src="'+obj.image_url+'" style="width:115px;height:65px;"/>'
			}
			
		}
	},{
		field: 'remind_period_day',
		title: '提醒周期',
		width: '100px',
		align: 'center',
		formatter: function(val,obj,row){
			return !!val?obj.remind_period_day+'天':'';
		}
	},{
		field: 'product_desc',
		title: '产品描述',
		width: '250px',
		align: 'center',
		formatter: function(val,obj,row){
			return !!val?obj.product_desc:'';
		}
		
	},{
		field: 'create_time',
		hidden:true,
		title: '创建时间',
		width: '100px',
		align: 'center',
		formatter: function(val,obj,row){
			var result='';
			if(!!val && !!val.create_time){
				result = Wz.dateFormat(new Date(val.active_time*1000),'yyyy-MM-dd hh:mm:ss');
			}
			return result;
		}
	},{
		field:'create_admin_name',
		title:'创建人',
		width:'150px',
		align:'center',
		formatter: function(val,obj,row){
			return !!val?obj.create_admin_name:'';
		}
	},{
		 field: 'is_expert',
         title: '操作',
         width: '225px',
         align: 'center',
         hidden: !(Wz.getPermission('clock/product/update')||Wz.getPermission('clock/product/delete')),
         formatter: function(val,obj,row){
        	return  (Wz.getPermission('clock/product/update')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.productManage.editor('+row+')">编辑</a>':'') +
        	 (Wz.getPermission('clock/product/delete')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.clock.productManage.delproduct('+obj.product_id+')">删除</a>':'');
         }
	}
	]],
	toolbar: [{
		id: 'product_add',
		text: Wz.lang.common.gridtool_create_btn,
		iconCls: 'icon-add',
		disabled: !Wz.getPermission('clock/product/create'),
		handler: function(){
			newproductform.find('input[name=product_id]').val('');
			newproductform.form('reset');
			uploadImge.reset();
			editWin.window('open');
			addnewproduct.show();
		}
	},{
		id:'product_remove',
		text: Wz.lang.common.grid_del_btn,
		iconCls: 'icon-remove',
		disabled: !Wz.getPermission('clock/product/delete'),
		handler: function(){
			var row=userTable.datagrid('getSelections');
			var list=[];
			if(row.length == 0){
        		$.messager.alert('提示','请选择需要删除的员工信息','info');
        		return false;
        	}
			else{
				for(var i=0,len=row.length;i<len;i++){
				list.push(row[i].product_id);
				};
				delproduct(list.join(','));
				}
			}
	},{
        id: 'product-import-btn',
        text: '导入产品信息',
		disabled: !Wz.getPermission('clock/product/import'),
        iconCls: 'icon-import',
        handler: function(){
        	importWin.window('open');
        }
    },{
        id: 'product-export-btn',
        text: '导出搜索产品信息',
        iconCls: 'icon-export',
		disabled: !Wz.getPermission('clock/product/export'),
        handler: function(){
        	var param = [];
        	for(var name in query_params){
            	param.push(name + '=' + query_params[name]);
            }
        	Wz.downloadFile("./api/tools/productclock/download_product.json?_t=" + new Date().getTime()+'&'+param.join('&'));
        }
    }],
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
var uploadImge = addnewproduct.find('input[name=image_name]').uploadimage({
	url: '../upload/api/admin/upload_image.json',
	name: 'upload_file',
	wrapWidth: 350,
	tipInfo: '建议图片尺寸120x120(px)<br>支持jpg、png格式，大小1M以内',
	maxSize: 1,
	params: {
		image_tag: '产品闹钟,图标'
	}
});
function delproduct(product_id){
	$.messager.confirm('提示','是否确定删除该产品？',function(ok){
		if(ok){
			Wz.ajax({
				type: 'post',
				url: './api/tools/productclock/delete_product.json',
				data: {
					product_id_list:product_id
				},
				success: function(json){
					if(json.result=="SUCC"){
						userTable.datagrid('reload');
					}else{
						$.messager.alert('错误',json.fail_text,'error');
					}
				}
				
			})
		}
	})
};
function editor(row){
	var product=$.extend({},userTable.datagrid('getRows')[row]);
	newproductform.form('reset');
	uploadImge.reset();
	if(product.image_name!=''){
		uploadImge.setValue(product.image_name,product.image_url);
	}
	newproductform.form('load',product);
	editWin.window('open');
};
function importProduct(){
	importForm.form('submit',{
		url: './api/tools/productclock/import_product.json?company_id='+Wz.cur_company_id,
		onSubmit: function(){
			Wz.showLoadingMask();
		},
		success: function(result){
			Wz.hideLoadingMask();
			result = $.parseJSON(result);
			if(result.result == 'SUCC'){
				importWin.window('close');
				userTable.datagrid('reload');
				$.messager.alert('提示','<p style="padding-left:42px">导入新增产品</p>','info');
			}else{
				$.messager.confirm('导入错误','请确认是否导出错误信息？',function(ok){
					if(ok){
						Wz.downloadFile("./api/tools/productclock/get_import_fail_log.json?_t=" + new Date().getTime());
					}
				});
			}
		}
	});
}
function saveedit(){
	var url = './api/tools/productclock/create_product.json';
	var user_id = newproductform.find('input[name=product_id]').val();
	if(user_id != ''){
		url = './api/tools/productclock/update_product.json';
	};
	newproductform.form('submit',{
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
				editWin.window('close');
    			userTable.datagrid('reload');
			}else{
				$.messager.alert('错误',json.fail_text,'error');
				return false;
			}
		}
	});
}
searchproduct.find('.search-btn').click(function(){
	query_params.product_name = searchproduct.find('input[name=product_name]').val();
	userTable.datagrid('reload');
});
importForm.find('.download-btn').click(function(){
	Wz.downloadFile('./static/res/import_product_file.xlsx');
});
return  {
	saveedit:saveedit,
	importProduct:importProduct,
	editor:editor,
	delproduct:delproduct
		}
}()
 