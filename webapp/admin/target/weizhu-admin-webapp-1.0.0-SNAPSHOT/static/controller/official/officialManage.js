/**
 * Created by allenpeng on 15-12-17.
 * 功能介绍： 服务号-服务号管理
 */
Wz.namespace('Wz.official');
Wz.official.officialManage = function(){
    $.parser.parse('#main-contain');
    
    var editWin = $('#official-official-edit-win');
    var editForm = $('#official-official-edit-form');
    var msgWin = $('#official-msg-win');
    var sendMsgWin = $('#official-official-sendmsg-win');
    var sendMsgForm = $('#official-official-sendmsg-form');
    
    var uploadImge = editForm.find('input[name=avatar_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '图片格式必须为:png,jpg,gif；<br/>图片质量不可大于1M,图片尺寸建议120X120(px)',
    	maxSize: 1,
    	params: {
    		image_tag: '服务号,头像'
    	}
    });
    var allowModel = Wz.comm.allowService(editForm.find('input[name=allow_model_id]'),{});
    var sendModel = Wz.comm.allowService(sendMsgForm.find('input[name=allow_model_id]'),{});
    var sendImage = sendMsgForm.find('input[name=send_msg_image_name]').uploadimage({
    	url: '../upload/api/admin/upload_image.json',
    	name: 'upload_file',
    	wrapWidth: 350,
    	tipInfo: '支持jpg、png格式，大小2M以内',
    	maxSize: 2,
    	params: {
    		image_tag: '服务号'
    	}
    });
    /*sendMsgForm.find('input[name=msg_type]').combobox({
    	width:350,
    	valueField:'value',
    	value:'0',
    	textField:'name',
    	data:[{name:'文字消息',value:'0'},{name:'图片消息',value:'1'},{name:'用户名片',value:'2'}],
    	editable:false,
    	panelHeight:'auto',
    	onChange: function(newValue,oldValue){
    		if(newValue == '2'){
    			sendMsgForm.find('input[name=send_msg_user_user_id]').parents('.form-item').show();
    			sendMsgForm.find('input[name=send_msg_image_name]').parents('.form-item').hide();
    			sendMsgForm.find('input[name=send_msg_text_content]').parents('.form-item').hide();
    		}else if(newValue == '1'){
    			sendMsgForm.find('input[name=send_msg_user_user_id]').parents('.form-item').hide();
    			sendMsgForm.find('input[name=send_msg_image_name]').parents('.form-item').show();
    			sendMsgForm.find('input[name=send_msg_text_content]').parents('.form-item').hide();
    		}else{
    			sendMsgForm.find('input[name=send_msg_user_user_id]').parents('.form-item').hide();
    			sendMsgForm.find('input[name=send_msg_image_name]').parents('.form-item').hide();
    			sendMsgForm.find('input[name=send_msg_text_content]').parents('.form-item').show();
    		}
    	}
    });*/
    sendMsgForm.find('input[name=is_send_immediately]').combobox({
    	width:350,
    	valueField:'value',
    	value:'true',
    	textField:'name',
    	data:[{name:'立即发送',value:'true'},{name:'预约发送',value:'false'}],
    	editable:false,
    	panelHeight:'auto',
    	onChange: function(newValue,oldValue){
    		if(newValue == 'true'){
    			sendMsgForm.find('input[name=send_time]').parents('.form-item').hide();
    		}else if(newValue == 'false'){
    			sendMsgForm.find('input[name=send_time]').parents('.form-item').show();
    		}
    	}
    });
    var sendTime = sendMsgForm.find('input[name=send_time]').datetimebox({
    	editable: false,
        required: true
    });
    sendTime.datetimebox('calendar').calendar({
		validator: function(date){
			var now = new Date();
			return date>=new Date(now.getFullYear(), now.getMonth(), now.getDate());
		}
	});
    /*sendMsgForm.find('select[name=send_msg_user_user_id]').combogrid({
    	width:350,
    	idField:'user_id',
    	textField:'user_name',
    	url: './api/user/get_user_list.json',
    	editable:false,
        pagination: true,
        pageSize: 20,
    	columns: [[{
    		field: 'user_name',
    		title: '姓名'
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
    		field: 'mobile_no',
    		title: '手机号'
    	}]],
    	onShowPanel: function(){
    		var keyword = $(this).combogrid('getValue');
    	},
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
    });*/
    var officialTable = $('#official-official-table').datagrid({
        url: './api/official/get_official_list.json',
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
        	field: 'avatar_url',
        	title: '头像',
        	width: 60,
        	align: 'center',
        	formatter: function(val,obj,row){
        		return '<img style="width:50px;height:50px;" src="'+val+'" />';
        	}
        },{
            field: 'official_name',
            title: '服务号名称',
            width: 400,
            align: 'left',
            formatter: function(val,obj,row){
            	return val;
            }
        },{
        	field: 'create_admin_name',
            title: '创建人',
            width: '80px',
            align: 'left'
        },{
            field: 'create_time',
            title: '创建时间',
            width: 160,
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
        	field: 'is_enable',
            title: '状态',
            width: 60,
            align: 'center',
            formatter: function(val,obj,row){
            	return val?'正常':'禁用';
            }
        },{
        	field: 'sm',
            title: '服务消息',
            align: 'center',
            width: 80,
            hidden: !(Wz.getPermission('official/official/list_message')),
            formatter: function(val,obj,row){
            	return '<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.official.officialManage.showMsg('+row+')">消息管理</a>';
            }
        },{
            field: 'official_id',
            title: '操作',
            align: 'center',
            width: 120,
            hidden: !(Wz.getPermission('official/official/update')||Wz.getPermission('official/official/set_state')),
            formatter: function(val,obj,row){
            	var stateText = (obj.is_enable?'禁用':'启用');
                return (Wz.getPermission('official/official/update')?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.official.officialManage.editOfficial('+row+')">编辑</a>':'') +
                (Wz.getPermission('official/official/set_state')?'<a href="javascript:void(0)" class="tablelink edit-btn" onclick="Wz.official.officialManage.changeState('+row+')">'+stateText+'</a>':''); 
                	//'<a href="javascript:void(0)" class="tablelink delete-btn" onclick="Wz.official.officialManage.delOfficial('+val+')">删除</a>';
            }
        }]],
        toolbar: [{
            id: 'business-user-add',
            text: Wz.lang.common.gridtool_create_btn,
            disabled: !Wz.getPermission('official/official/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[name=official_id]').val('');
            	uploadImge.reset();
            	allowModel.setValue({model_id:'',model_name:''});
            	editWin.window({title:'新建服务号'}).window('open');
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
                total: data.recordsFiltered,
                rows: data.data
            };
        }
    });
    
    var msgParam = {
    	official_id: ''
    };
    var sendMsgTable = $('#official-sendmsg-table').datagrid({
    	url: './api/official/get_official_send_plan_list.json',
    	queryParams: msgParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
        	field: 'send_msg',
        	title: '消息内容',
        	width: 300,
        	formatter: function(val,obj,row){
        		return val.text.content;
        	}
        },{
            field: 'send_time',
            title: '发布时间',
            width: 140,
            align: 'left',
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
        	field: 'create_admin_name',
            title: '创建人',
            width: '80px',
            align: 'left'
        },{
            field: 'create_time',
            title: '创建时间',
            width: 140,
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
            }
        },{
        	field: 'send_state',
            title: '状态',
            align: 'center',
            formatter: function(val,obj,row){
            	return val=='ALREADY_SEND'?'已发布':(val=='CANCEL_SEND'?'已取消':'未发布');
            }
        },{
            field: 'plan_id',
            title: '操作',
            align: 'center',
            width: 80,
            hidden: !(Wz.getPermission('official/official/cancel_message')),
            formatter: function(val,obj,row){
                return (obj.send_state == 'WAIT_SEND'?'<a href="javascript:void(0)" class="tablelink import-btn" onclick="Wz.official.officialManage.cancelSend('+val+')">取消发布</a>':'');
            }
        }]],
        toolbar: [{
            id: 'official-sendsmg-add',
            text: '发布消息',
            disabled: !Wz.getPermission('official/official/send_message'),
            iconCls: 'icon-add',
            handler: function(){
            	sendMsgForm.form('reset');
            	sendModel.setValue({model_id:'',model_name:''});
            	sendImage.reset();
            	sendMsgForm.find('input[name=official_id]').val(msgParam.official_id);
    			sendMsgForm.find('input[name=send_msg_user_user_id]').parents('.form-item').hide();
    			sendMsgForm.find('input[name=send_msg_image_name]').parents('.form-item').hide();
    			sendMsgForm.find('input[name=send_msg_text_content]').parents('.form-item').show();
    			sendMsgForm.find('input[name=send_time]').parents('.form-item').hide();
            	sendMsgWin.window('open');
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
                total: data.recordsFiltered,
                rows: data.data||[]
            };
        }
    });
    
    var rcvMsgTable = $('#official-rcvmsg-table').datagrid({
    	url: './api/official/get_official_recv_message.json',
    	queryParams: msgParam,
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        rownumbers: true,
        pagination: true,
        pageSize: 20,
        columns: [[{
        	field: 'text',
        	title: '消息内容',
        	width: 500,
        	formatter: function(val,obj,row){
        		var result = val && val.content;
                if(!!!result){
                    if(!!obj.image){
                        result = '<a href="'+obj.image.url+'" target="_blank"><img style="height:50px;" src="'+ obj.image.url +'" /></a>';
                    }else if(!!o.user){
                        result = obj.user.user_name + '的名片';
                    }else if(!!obj.discover_item){
                        result = obj.discover_item.item_id + '课程';
                    }else if(!!o.voice){
                        result = obj.voice.duration + '语音';
                    }else{
                        result = '未知信息源';
                    }
                }
                return result;
        	}
        },{
            field: 'user_name',
            title: '发送人',
            width: 100,
            align: 'center'
        },{
            field: 'msg_time',
            title: '消息时间',
            align: 'center',
            width: 140,
            formatter: function(val,obj,row){
            	var result = '';
            	if(!!val){
            		result = Wz.dateFormat(new Date(val*1000),'yyyy-MM-dd hh:mm:ss');
            	}
            	return result;
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
                rows: data.data||[]
            };
        }
    });
    

    var msgTabs = $('#official-msg-tabs').tabs({
    	justified: true,
    	narrow: true,
    	plain: true,
    	onSelect: function(title,index){
    		if(index == 0){
    			sendMsgTable.datagrid('reload');
    		}else{
    			rcvMsgTable.datagrid('reload');
    		}
    	}
    });
    
    function editOfficial(row){
    	var official = officialTable.datagrid('getData').rows[row];
    	editForm.form('load',official);
    	uploadImge.setValue(official.avatar,official.avatar_url);
    	allowModel.setValue({model_id:official.allow_model_id,model_name:official.allow_model_name});
    	editWin.window({title:'编辑服务号'}).window('open');
    }
    
    function saveEdit(){
    	var official_id = editForm.find('input[name=official_id]').val();
    	var url = './api/official/create_official.json';
    	if(official_id != ''){
    		url = './api/official/update_official.json';
    	}
    	editForm.form('submit',{
    		url: url,
    		onSubmit: function(){
    			var valid = $(this).form('validate');
    			var avatar_name = uploadImge.getValue();
    			var allow_model_id = allowModel.getValue();
    			if(avatar_name == ''){
    				$.messager.alert('错误','服务号头像不能为空！','error');
    				valid = false;
    			}
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
    				officialTable.datagrid('reload');		
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    
    function delOfficial(official_id){
    	$.messager.confirm('提示','请确认是否要删除服务号？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/official/delete_official.json',
    				data: {
    					official_id: official_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						officialTable.datagrid('reload');
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
    	var official = officialTable.datagrid('getData').rows[row];
    	var is_enable = !official.is_enable;
    	$.messager.confirm('提示','请确认是否要'+(is_enable?'启用':'禁用')+'服务号？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/official/set_official_enable.json',
    				data: {
    					official_id: official.official_id,
    					is_enable: is_enable
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						officialTable.datagrid('reload');
    					}else{
    						$.messager.alert('错误',json.fail_text,'error');
    	    				return false;
    					}
    				}
    			});    			
    		}
    	});
    }
    
    function showMsg(row){
    	var official = officialTable.datagrid('getData').rows[row];
    	msgParam.official_id = official.official_id;
    	msgWin.window('open');
		sendMsgTable.datagrid('reload');
    	msgTabs.tabs('select',0);
    	
    }
    function sendMsg(){
    	var official_id = sendMsgForm.find('input[name=official_id]').val();
    	var allow_model_id = sendModel.getValue();
    	var msg_type = sendMsgForm.find('input[name=msg_type]').val();
    	var send_msg_text_content = sendMsgForm.find('input[name=send_msg_text_content]').val();
    	var send_msg_image_name = sendMsgForm.find('input[name=send_msg_image_name]').val();
    	var send_msg_user_user_id = sendMsgForm.find('input[name=send_msg_user_user_id]').val();
    	var is_send_immediately = sendMsgForm.find('input[name=is_send_immediately]').val();
    	var send_time = sendMsgForm.find('input[name=send_time]').val();
    	var params = {
    		official_id: official_id,
    		allow_model_id: allow_model_id,
    		is_send_immediately: is_send_immediately,
    		send_msg_text_content: send_msg_text_content
    	};
    	
    	if(allow_model_id == ''){
    		$.messager.alert('错误','请创建发送消息的发送对象！','error');
			return false;
    	}
    	/*if(msg_type == '0'){
    		if(send_msg_text_content == ''){
    			$.messager.alert('错误','消息内容不能为空！','error');
    			return false;    			
    		}else{
    			params.send_msg_text_content = send_msg_text_content;
    		}
    	}
    	if(msg_type == '1'){
    		if(send_msg_image_name == ''){
    			$.messager.alert('错误','请上传要发送的图片！','error');
    			return false;    			
    		}else{
    			params.send_msg_image_name = send_msg_image_name;
    		}
    	}
    	if(msg_type == '2'){
    		if(send_msg_user_user_id == ''){
    			$.messager.alert('错误','请选择要发送的名片！','error');
    			return false;    			
    		}else{
    			params.send_msg_user_user_id = send_msg_user_user_id;
    		}
    	}*/
    	if(is_send_immediately == 'false'){
    		if(send_time == ''){
    			$.messager.alert('错误','预约发送时发送时间不能为空！','error');
    			return false;
    		}
    		if(Wz.parseDate(send_time).getTime()<new Date().getTime()){
    			$.messager.alert('错误','预约发送时发送时间不能小于当前时间！','error');
    			return false;
    		}
    		params.send_time =Wz.parseDate(send_time).getTime()/1000;
    	}

		Wz.showLoadingMask('正在处理中，请稍后......');
    	Wz.ajax({
    		type: 'post',
    		url: './api/official/create_official_send_plan.json',
    		data: params,
    		success: function(json){
    			Wz.hideLoadingMask();
    			if(json.result == 'SUCC'){
    				sendMsgTable.datagrid('reload');
    				sendMsgWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    				return false;
    			}
    		}
    	});
    }
    function cancelSend(plan_id){
    	$.messager.confirm('提示','请确认是否要取消发布？',function(ok){
    		if(ok){
    			Wz.ajax({
    				type: 'post',
    				url: './api/official/cancel_official_send_plan.json',
    				data: {
    					plan_id: plan_id
    				},
    				success: function(json){
    					if(json.result == 'SUCC'){
    						sendMsgTable.datagrid('reload');
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
    	editOfficial: editOfficial,
    	saveEdit: saveEdit,
    	changeState: changeState,
    	delOfficial: delOfficial,
    	showMsg: showMsg,
    	sendMsg: sendMsg,
    	cancelSend: cancelSend
    };
}()

