/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 发现管理——轮播管理
 */
Wz.namespace('Wz.discover');
Wz.discover.bannerManage = function(){
    $.parser.parse('#main-contain');
    var m_prefix_url = 'http://'+location.host;

    var mainWrap = $('#discover-banner-wrap');
    var editWin = $('#discover-banner-edit-win');
    var editForm = $('#discover-banner-edit-form');
    
    var uploadImge = editForm.find('input[name=image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '图片格式必须为:png,jpg,gif；<br/>图片质量不可大于1M,图片尺寸建议640*260(px)',
    	maxSize: 1,
    	params: {
    		image_tag: '发现,轮播图'
    	}
    });
    var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
    editForm.find('input[name=banner_type]').combobox({
    	width:350,
    	valueField:'value',
    	value:'0',
    	textField:'name',
    	data:[{name:'无',value:'0'},{name:'链接',value:'1'},{name:'发现课件',value:'5'},{name:'考试中心',value:'2'},{name:'调研中心',value:'4'},{name:'问答中心',value:'3'}],
    	editable:false,
    	panelHeight:'auto',
    	onChange: function(newValue,oldValue){
    		if(newValue == '1'){
    			editForm.find('input[name=item_id]').parents('.form-item').hide();
    			editForm.find('input[name=web_url]').parents('.form-item').show();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').show();
    		}else if(newValue == '5'){
    			editForm.find('input[name=web_url]').parents('.form-item').hide();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
    			editForm.find('input[name=item_id]').parents('.form-item').show();
    		}else{
    			editForm.find('input[name=item_id]').parents('.form-item').hide();
    			editForm.find('input[name=web_url]').parents('.form-item').hide();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
    		}
    	}
    });
    var bannerItem = editForm.find('input[name=item_id]').combogrid({
    	url: './api/discover/get_discover_item.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        pagination: true,
        rownumbers: true,
        required: true,
        pageSize: 20,
        idField: 'item_id',
        textField: 'item_name',
        keyHandler: {
        	query: function(q) {
        		bannerItem.combogrid("grid").datagrid("reload", { 'item_name': q });  
        		bannerItem.combogrid("setValue", q);
            } 
        },
        width: 350,
        panelWidth: 500,
        frozenColumns: [[{
            field: 'item_name',
            title: '标题',
            width: '330px',
        },{
            field: 'create_time',
            title: '发布时间',
            width: '140px',
            align: 'center'
        },]],
        changePages: function(params,pageObj){
        	$.extend(params,{
        		start: (pageObj.page-1)*pageObj.rows,
        		length: pageObj.rows
        	});
        },
        loadFilter: function(data){
        	var list = data.item||[];
        	for(var i=0,len=list.length;i<len;i++){
        		list[i] = list[i].base||{};
        	}
            return {
                total: data.filtered_size,
                rows: list
            };
        }
    });
    var bannerTable = $('#discover-banner-table').datagrid({
        url: './api/discover/get_discover_banner.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        idField: 'banner_id',
        columns: [[{
        	field: 'image_url',
        	title: '轮播图片',
        	width: 120,
        	align: 'center',
        	formatter: function(val,obj,row){
        		return '<img style="width:115px;height:65px;" src="'+val+'" />';
        	}
        },{
            field: 'banner_name',
            title: '标题',
            width: 300,
            align: 'left'
        },{
        	field: 'create_admin_name',
            title: '创建人',
            width: '80px',
            align: 'center'
        },{
            field: 'create_time',
            title: '创建时间',
            width: 160,
            align: 'center'
        },{
    		field: 'banner_id',
    		title: '排序',
    		width: '150px',
    		align: 'center',
            hidden: !(Wz.getPermission('discover/banner/update_order')),
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-top move-top-btn">&nbsp;</a><a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-up move-up-btn">&nbsp;</a><a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-down move-down-btn">&nbsp;</a><a href="javascript:void(0)" bid="'+val+'" class="table-cell-icon icon-bottom move-bottom-btn">&nbsp;</a>';
            }    		
    	},{
            field: 'opt',
            title: '操作',
            align: 'center',
            width: 120,
            hidden: !(Wz.getPermission('discover/banner/update')||Wz.getPermission('discover/banner/delete')||Wz.getPermission('discover/banner/set_state')),
            formatter: function(val,obj,row){
            	var stateText = (obj.state=='DISABLE'?'启用':'禁用');
                return (Wz.getPermission('discover/banner/update')?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.discover.bannerManage.editBanner('+row+')">编辑</a>':'') +
                (Wz.getPermission('discover/banner/set_state')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.discover.bannerManage.changeState('+row+')">'+stateText+'</a>':'') +
                (Wz.getPermission('discover/banner/delete')?'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.discover.bannerManage.delBanner('+obj.banner_id+')">删除</a>':'');
            }
        }]],
        toolbar: [{
            id: 'discover-banner-add',
            text: '添加轮播图',
            disabled: !Wz.getPermission('discover/banner/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=banner_id]').val('');
            	uploadImge.reset();
            	allowModel.setValue({model_id:'',model_name:''});
            	editWin.window({title:'添加轮播图'}).window('open');
            }
        }],
        loadFilter: function(data){
            return {
                total: data.banner.length,
                rows: data.banner
            };
        }
    });
    
    function editBanner(row){
    	var banner = $.extend({},bannerTable.datagrid('getRows')[row]);
    	if(banner.web_url == '{}' || banner.web_url == ''){
    		if(banner.item_id == ''){
    			banner.banner_type = '0';
    			editForm.find('input[name=web_url]').parents('.form-item').hide();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
    			editForm.find('input[name=item_id]').parents('.form-item').hide();		
    		}else{
    			banner.banner_type = '5';
    			editForm.find('input[name=web_url]').parents('.form-item').hide();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').hide();
    			editForm.find('input[name=item_id]').parents('.form-item').show();
    		}
    	}else{
    		try{
    			var web_url = JSON.parse(banner.web_url);    			
    		}catch(e){
    			web_rul = {web_url:'',is_weizhu:''};
    		}
    		if(web_url.web_url.indexOf('mobile/survey/survey_list.html')>-1){
    			banner.banner_type = '4';
    		}else if(web_url.web_url.indexOf('mobile/qa/qa_info.html')>-1){
    			banner.banner_type = '3';
    		}else if(web_url.web_url.indexOf('mobile/exam/exam_list.html')>-1){
    			banner.banner_type = '2';
    		}else{
    			banner.banner_type = '1';
    			editForm.find('input[name=web_url]').parents('.form-item').show();
    			editForm.find('input[name=is_weizhu]').parents('.form-item').show();
    		}
    		banner.web_url = web_url.web_url;
    		banner.is_weizhu = web_url.is_weizhu;
    	}
    	editForm.form('load',banner);
    	uploadImge.setValue(banner.image_name,banner.image_url);
    	allowModel.setValue({model_id:banner.allow_model_id,model_name:banner.allow_model_name});
    	editWin.window({title:'编辑轮播图'}).window('open');
    }
    
    function saveEdit(){
    	if(bannerItem.combogrid('getValue') == bannerItem.combogrid('getText')){
    		bannerItem.combogrid('setValue','');
		}
    	var banner_id = editForm.find('input[name=banner_id]').val();
    	var banner_name = editForm.find('input[textboxname=banner_name]').textbox('getValue');
    	var banner_type = editForm.find('input[textboxname=banner_type]').combobox('getValue');
		var image_name = uploadImge.getValue();
		var image_url = uploadImge.getUrl();
		var allow_model_id = allowModel.getValue();
		var allow_model_name = allowModel.getName();
    	var item_id = editForm.find('input[textboxname=item_id]').textbox('getValue');
		var web_url = editForm.find('input[textboxname=web_url]').textbox('getValue');
		var is_weizhu = editForm.find('input[name=is_weizhu]').prop('checked');
    	var url = './api/discover/create_discover_banner.json';
    	var param = {
    		banner_id: banner_id,
    		banner_name: banner_name,
    		allow_model_id: allow_model_id,
    		image_name: image_name
    	};
    	if(!editForm.find('input[textboxname=banner_name]').textbox('isValid')){
    		$.messager.alert('错误','请输入轮播标题！','error');
    		return false;
    	}
    	if(image_name == ''){
    		$.messager.alert('错误','请上传轮播图片！','error');
    		return false;
    	}
    	if(banner_type == '1'){
    		if(!editForm.find('input[textboxname=web_url]').textbox('isValid')){
    			$.messager.alert('错误','请输入正确的链接地址！','error');
        		return false;
    		}
    		param.web_url = JSON.stringify({web_url:web_url,is_weizhu:is_weizhu});
    	}else if(banner_type == '2'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/exam/exam_list.html',is_weizhu:true});
    	}else if(banner_type == '3'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/qa/qa_info.html',is_weizhu:true});
    	}else if(banner_type == '4'){
    		param.web_url = JSON.stringify({web_url:m_prefix_url+'/mobile/survey/survey_list.html',is_weizhu:true});
    	}else if(banner_type == '5'){
    		if(item_id == ''){
    			$.messager.alert('错误','请选择发现课件','error');
        		return false;
    		}
    		param.item_id = item_id;
    	}
    	
    	if(banner_id != ''){
    		url = './api/discover/update_discover_banner.json';
    	}
		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: url,
    		data: param,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				bannerTable.datagrid('reload');
    				editWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    			}
    		}
    	});
    }
    
    function delBanner(banner_id){
    	$.messager.confirm('提示','请确认是否要删除轮播图？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/discover/delete_discover_banner.json',
    				data: {
    					banner_id: banner_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						bannerTable.datagrid('reload');
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
    	var banner = bannerTable.datagrid('getRows')[row];
    	var url = './api/discover/disable_discover_banner.json';
    	var state = (banner.state == 'DISABLE');
    	if(state){
    		url = './api/discover/display_discover_banner.json'
    	}
    	$.messager.confirm('提示','请确认是否要禁用/启用轮播图？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: url,
    				data: {
    					banner_id: banner.banner_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						bannerTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    mainWrap.on('mouseup','.table-cell-icon',function(){
    	var banner_id = $(this).attr('bid');
    	var index = bannerTable.datagrid('getRowIndex',banner_id);
    	var banner = bannerTable.datagrid('getRows')[index];
    	if($(this).hasClass('move-top-btn')){
			if(index > 0){
				bannerTable.datagrid('deleteRow',index);
				bannerTable.datagrid('insertRow',{index:0,row:banner});
			}
    	}else if($(this).hasClass('move-bottom-btn')){
			if(index < bannerTable.datagrid('getRows').length-1){
				bannerTable.datagrid('deleteRow',index);
				bannerTable.datagrid('appendRow',banner);
			}
    	}else if($(this).hasClass('move-up-btn')){
			if(index > 0){
				bannerTable.datagrid('deleteRow',index);
				bannerTable.datagrid('insertRow',{index:index-1,row:banner});
			}
    	}else if($(this).hasClass('move-down-btn')){
			if(index < bannerTable.datagrid('getRows').length-1){
				bannerTable.datagrid('deleteRow',index);
				bannerTable.datagrid('insertRow',{index:index+1,row:banner});
			}
    	}
    	var banner_order_str = [];
    	var banners = bannerTable.datagrid('getRows');
    	for(var i=0;i<banners.length;i++){
    		banner_order_str.push(banners[i].banner_id);
    	}
    	Wz.ajax({
    		type: 'post',
    		url: './api/discover/update_discover_banner_order.json',
    		data: {
    			banner_order_str: banner_order_str.join(',')
    		},
    		success: function(json){
    			//不作处理
    		}
    	});
    });
    return {
    	editBanner: editBanner,
    	saveEdit: saveEdit,
    	changeState: changeState,
    	delBanner: delBanner,
    };
}()

