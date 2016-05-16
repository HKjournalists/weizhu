/**
 * Created by Administrator on 15-12-11.
 */
var Wz = function(){
	var _sessionkey = $.cookie('x-admin-session-key');
	var _cur_company_id = $.cookie('cur_company_id');
	if((typeof _sessionkey =='undefined')||(typeof _cur_company_id =='undefined')){
		_clearCookie();
	}
	
    var _version = '2016050301';
    var _admin_info = {};
    var _config = {};
    var _cacheData = {};
    var _cacheDom = $('<div/>');
    var _curRout = '';
	var _permission_tree = [];
	var _permissionMap = {};
	var _hasAllPermission = true;
	var _url_prefix = location.href.replace(/main\.html.*$/,'');
	var _cur_company_id = $.cookie('cur_company_id');
	var _company_info = {};
	var _company_list = [];
	var _client_user = JSON.parse($.cookie('client_user')||'{}');
    var supportHashChange = ('onhashchange' in window) && (document.documentMode === void 0 || document.documentMode > 7);
    
    var cache = function(key,value){
        if(!key) return false;
        if(!!value){
            _cacheData[key] = value;
        }else{
            return _cacheData[key];
        }
    };
    var getObjByName = function(fname){
        var nameList = fname.split('.');
        var obj = window;
        for(var i=0;i<nameList.length;i++){
            if(!!!obj[nameList[i]]){
                return false;
            }else{
            	obj = obj[nameList[i]];
            }
        }
        return obj;
    };

    var namespace = function(fname){
        var nameList = fname.split('.');
        var fun = window;
        for(var i=0;i<nameList.length;i++){
            if(!!!fun[nameList[i]]){
                fun[nameList[i]] = {};
            }else{
                fun = fun[nameList[i]];
            }
        }
    };

    var getVersion = function(){
        return _version;
    }

    var getCurRout = function(){
        return _curRout;
    };
    var setCurRout = function(rout){
        _curRout = rout;
    }
    var addCacheDom = function(dom){
    	_cacheDom.append(dom);
    }
    var dateFormat = function(date,fmt){
    	var o = {   
		    "M+" : date.getMonth()+1,                 //月份   
		    "d+" : date.getDate(),                    //日   
		    "h+" : date.getHours(),                   //小时   
		    "m+" : date.getMinutes(),                 //分   
		    "s+" : date.getSeconds(),                 //秒   
		    "q+" : Math.floor((date.getMonth()+3)/3), //季度   
		    "S"  : date.getMilliseconds()             //毫秒   
		  };   
		  if(/(y+)/.test(fmt))   
		    fmt=fmt.replace(RegExp.$1, (date.getFullYear()+"").substr(4 - RegExp.$1.length));   
		  for(var k in o)   
		    if(new RegExp("("+ k +")").test(fmt))   
		  fmt = fmt.replace(RegExp.$1, (RegExp.$1.length==1) ? (o[k]) : (("00"+ o[k]).substr((""+ o[k]).length)));   
		  return fmt;
    }
    
    var getPermission = function(name){
    	return _permissionMap[name];
    };
    
    var ajax = function(opt){
    	var type = opt.type || 'get',
    		url = opt.url,
    		data = opt.data||{},
    		dataType = opt.dataType||'json',
    		success = opt.success || function(json){},
    		error = opt.error || function(e){
    			
    		};
    	data.company_id = _cur_company_id;
    	$.ajax({
    		type: type,
    		url: url,
    		data: data,
    		dataType: dataType,
    		success: function(data){
    			success(data);
    		},
    		error: function(e){
    			error(e);
    			if(e.status == 403){
    				$.messager.alert('错误','您没有改操作权限！','error');
    				location.href = location.href;
    			}else if(e.status == 500){
    				$.messager.alert('错误','服务器错误，请联系管理员！','error');
    			}else if(e.status == 401){
    				_clearCookie();
    			}
    		}
    	});
    };
    
    function showLoadingMask(msg){
    	msg = msg || '正在处理中，请稍后......';
    	var mask = $('.loading-mask');
    	var msgDom = $('.loading-mask-msg');
    	if(mask.length == 0){
    		mask = $('<div class="loading-mask"></div>');
    		msgDom = $('<div class="loading-mask-msg">'+msg+'</div>');
    		$('body').append(mask).append(msgDom);
    	}else{
    		msgDom.html(msg);
    	}
    	msgDom.show();
    	mask.show();
    }
    
    function hideLoadingMask(){
    	$('.loading-mask').hide();
    	$('.loading-mask-msg').hide();
    }
    
    function downloadFile(file_url){
    	var iframe = $("#download-iframe");
	    if(iframe.length == 0){
	    	iframe = $('<iframe src="" style="display:none;" id="download-iframe"></iframe>');
	    	$('body').append(iframe);
	    }
	    var urlList = file_url.split('?');
	    file_url = urlList.shift() + '?company_id='+_cur_company_id + '&' + urlList.join('&');
	    iframe.attr("src",file_url);
    }
    
    function logout(){
    	ajax({
    		type: 'post',
    		url: './api/admin_logout.json',
    		success: function(json){
    			if(json.result == 'SUCC'){
    				_clearCookie();
    			}
    		}
    	});
    }
    
    function _clearCookie(){
    	$.cookie('x-admin-session-key','',{expires:-1});
		$.cookie('first_login','',{expires:-1});
		$.cookie('admin_name','',{expires:-1});
		$.cookie('admin_email','',{expires:-1});
		$.cookie('login_time','',{expires:-1});
		$.cookie('cur_company_id','',{expires:-1});
		$.cookie('company_list','',{expires:-1});
		location.href = './login.html';
    }
    
    function getLoginInfo(){
    	return {
        	admin_name: $.cookie('admin_name'),
        	admin_email: $.cookie('admin_email'),
        	login_time: dateFormat(new Date($.cookie('login_time')),'yyyy-MM-dd hh:mm:ss')
        };
    }
    
    function initMainPage(succFn){
    	ajax({
        	url: './api/get_admin_info.json',
        	success: function(json){
        		Wz.admin_info = json.admin;
        		Wz.config = json.config;
        		Wz.company_info = json.company;
        		companyList.combobox('setValue',json.company.company_id);
        		for(var i=0,len=json.permission.length;i<len;i++){
        			_permissionMap[json.permission[i].permission_id] = true;
        		}
        		succFn();
        		$('#header-admin-user').text(Wz.admin_info.admin_name);
        	}
        });
    }
    
    function showMsg(title,msg){
    	$.messager.show({
    		title: title,
    		msg: msg,
    		showType:'slide',
    		timeout:2000,
    		style:{
    			right:'',
    			top:document.body.scrollTop+document.documentElement.scrollTop,
    			bottom:''
    		}
    	});
    }
    
    var companyList = $('#main-company-list').combobox({
    	mode: 'remote',
    	valueField: 'company_id',
    	textField: 'company_name',
    	editable: false,
    	panelHeight: 200,
    	onChange: function(newValue,oldValue){
    		if(newValue != Wz.cur_company_id){
    			$.cookie('cur_company_id',newValue);
    			location.reload();    			
    		}
    	},loader: function(param,success,error){
			$.ajax({
				url: './api/get_company_list.json',
				dataType: 'json',
				success: function(json){
					var company_list = json.company_list||[];
					success(json.company_list);
					_company_list = company_list;
				}
			});
		},
		onShowPanel: function(){
			$(this).combobox('reload');
		}
    });
    
    function getClientUser(){
    	return _client_user;
    }
    
    function setClientUser(user){
    	if(!!user){
    		_client_user = user;
    		$.cookie('client_user',JSON.stringify(user));    		
    	}
    }
    function reviewCourse(url){
    	var iWidth=800;
    	var iHeight=600;
    	var iTop = (window.screen.availHeight-30-iHeight)/2;
    	var iLeft = (window.screen.availWidth-10-iWidth)/2;
    	window.open(url,"_blank","height="+iHeight+", width="+iWidth+", top="+iTop+", left="+iLeft); 
    }
    
    $(document).on('click','.form-checkbox',function(e){
    	var ele = e.target || window.event.srcElement;
    	if(ele.tagName.toLowerCase() != 'input'){
    		$(this).find('input').trigger('click');    		
    	}
    });
    function parseDate(date){
    	if(!!!date) return '';
    	var da = date.split(/[- :]/g);
    	da[1] = parseInt(da[1])-1;
    	return new Function('return new Date('+da.join(',')+')')();
    }
    
    function openHelp(){
    	var route = getCurRout();
    	reviewCourse('./static/help/index.html#'+route);
    }
    
    return {
        version: _version,
        supportHashChange: supportHashChange,
        getObjByName: getObjByName,
        namespace: namespace,
        cache: cache,
        getCurRout: getCurRout,
        setCurRout: setCurRout,
        getVersion: getVersion,
        dateFormat: dateFormat,
        addCacheDom: addCacheDom,
        getPermission: getPermission,
        ajax: ajax,
        showLoadingMask: showLoadingMask,
        hideLoadingMask: hideLoadingMask,
        downloadFile: downloadFile,
        url_prefix: _url_prefix,
        logout: logout,
        initMainPage: initMainPage,
        cur_company_id: _cur_company_id,
        showMsg: showMsg,
        getClientUser: getClientUser,
        setClientUser: setClientUser,
        clearCookie: _clearCookie,
        reviewCourse: reviewCourse,
        parseDate: parseDate,
        openHelp: openHelp
    };
}();
Wz.menu = function(){
    var menu_wrap = $('.leftmenu');
    var main_contain = $('#main-contain');
    var curInitData = null;
    var init = function(menu_cfg){
    	Wz.initMainPage(function(){
			loadMenu(menu_cfg());
			window.onhashchange = function(){
				loadModule();
			};
			loadModule();
    	});
    };

    function loadMenu(menu_cfg){
        for(var i=0;i<menu_cfg.length;i++){
            var menuNode = menu_cfg[i];
            if(!!!menuNode.permission) continue;
            var childMenu = menuNode.children;

            var nodeDom = $(['<dd id="',('menu-'+menuNode.id),'"><div class="title"><span><img src="',menuNode.icon,'" /></span>',menuNode.title,'</div><ul class="menuson"></ul></dd>'].join(''));

            for(var j=0;j<childMenu.length;j++){
                var menuleaf = childMenu[j];
                if(!!!menuleaf.permission) continue;
                var leafDom = $('<li id="'+('menu-'+menuleaf.id)+'" class="menu-item"><cite></cite><a href="javascript:void(0)">'+menuleaf.title+'</a><i></i></li>');
                leafDom.data('data',menuleaf);
                nodeDom.find('.menuson').append(leafDom);
                leafDom.bind('click',function(){
                    menu_wrap.find('li.active').removeClass('active');
                    $(this).addClass('active');
                    location.hash = $(this).data('data').id;
                });
            }
            menu_wrap.append(nodeDom);
            nodeDom.find('.title').bind('click',function(){
                var parent = $(this).parent();
                if(!parent.hasClass('menu-open')){
                    parent.siblings('.menu-open').removeClass('menu-open');
                    parent.addClass('menu-open');
                }
            });
        }
    }

    var loadModule = function(){
        var rid = location.hash || '#system-home';
        rid = rid.substr(1);
        var menu_dom = $('#menu-'+rid);
        if(menu_dom.length == 1 && menu_dom.hasClass('menu-item')){
            var menuNode = menu_dom.parent().prev();
            menuNode.trigger('click');
            menu_dom.trigger('click');
            var item = menu_dom.data('data');
            var className = item.className;
            var curRout = Wz.getCurRout();
            if(!!curRout){
                var curModule = Wz.cache(curRout)||{};
                curModule.dom = main_contain.children();
                curModule.time = new Date().getTime();
                Wz.cache(curRout,curModule);
                if(curModule.dom.length > 0){
                	Wz.addCacheDom(curModule.dom);
                }
            }
            $('.placeul').html('<li><a>'+item.parrentTitle+'</a>&gt;<a href="#'+item.id+'">'+item.title+'</a></li>');
        	var classObj = Wz.getObjByName(className);
        	$('.easyui-window').window('close');
            if(!!Wz.cache(item.id)){
                main_contain.html(Wz.cache(item.id).dom);
                Wz.setCurRout(rid);
                if(!!classObj.init){
        			classObj.init(curInitData);
        			curInitData = null;
        		}
            }else{
                var tpl_url = item.files.tpl;
                var controller_url = item.files.controller;
                $.ajax({
                    url: tpl_url,
                    type: 'get',
                    dataType: 'html',
                    success: function(xhr){
                        main_contain.html(template(xhr,{permission:{},lang:Wz.lang}));
                        $.parser.parse('#main-contain');
                        var script = document.createElement('script');
                        script.type = "text/javascript";
                        script.src = controller_url + '?_v='+Wz.version;
                        document.body.appendChild(script);
                        Wz.setCurRout(rid);
                        var curModule = Wz.cache(rid)||{};
                        curModule.dom = main_contain.children();
                        curModule.time = new Date().getTime();
                        Wz.cache(rid,curModule);
                        (function(){
                        	var me = arguments.callee;
                        	classObj = Wz.getObjByName(className);
                        	if(!!!classObj){
                        		setTimeout(function(){
                        			me();
                        		},100);
                        	}else{
                        		if(!!classObj.init){
                        			classObj.init(curInitData);
                        			curInitData = null;
                        		}
                    			return true;
                        	}
                        }());
                    },
                    error: function(e){
                        main_contain.html('<div class="error-wrap"><div class="error"><h2>非常遗憾，您访问的页面不存在！</h2><p>看到这个提示，就自认倒霉吧!</p><div class="reindex"><a href="#system-home">返回首页</a></div></div></div>');
                    }
                });
            }
        }else{
            location.hash = 'system-home';
        }
    };
    function changeRout(rout,data){
    	location.hash = rout;
    	curInitData = data || null;
    }

    return {
        init: init,
        changeRout: changeRout
    };
}();

Wz.comm = {};
Wz.comm.allowService = (function(){
	var _curps = {};
	var _setValue = function(){};
	return function(odom,settings){
		var def = {
			del_enable: true,
			copy_enable: false,
			data:　{
				model_id: '',
				model_name: ''
			},
			copy_win_title: '复制模型',
			copy_table_columns: [[]],
			copy_table_url: '',
			id_field: 'model_id',
			name_field: 'model_name',
			copyParam: {},
			dName: 'exam',
			fName: 'filtered_size'
		};
		
		var ps = $.extend({},def,settings)
		if(odom.length == 0)return false;
		var vdom = $('<div class="allow-service-model"><span class="allow-model-name"></span><a href="javascript:void(0)" class="allow-model-btn allow-model-edit easyui-linkbutton">选择对象</a>'+(ps.copy_enable?'<a href="javascript:void(0)" class="allow-model-btn allow-model-copy easyui-linkbutton">复制已有对象</a>':'')+'<a href="javascript:void(0)" class="allow-model-btn allow-model-del easyui-linkbutton">删除对象</a></div>');
		var editBtn = vdom.find('.allow-model-edit');
		var copyBtn = vdom.find('.allow-model-copy');
		var delBtn = vdom.find('.allow-model-del');
		var modelName = vdom.find('.allow-model-name');
		var allowModelWrap = $('#comm-allowmodel-wrap');
		
		setValue(ps.data);
		
		odom.hide();
		odom.before(vdom);
		$.parser.parse(vdom);
		if(allowModelWrap.length == 0){
			$.ajax({
	            url: './static/view/comm/allowModel.html',
	            type: 'get',
	            dataType: 'html',
	            success: function(xhr){
	            	if($('#comm-allowmodel-wrap').length > 0) return false;
	            	allowModelWrap = $(xhr);
	            	$('body').append(allowModelWrap);
	            	$.parser.parse(allowModelWrap);
	            	
	            	var editModelWin = $('#comm-allowmodel-edit');
	            	var editModelForm = $('#comm-allowmodel-edit-form');
	            	var editUserWin = $('#comm-allowmodel-userrule');
	            	var editUserForm = $('#comm-allowmodel-userrule-form');
	            	var editPositionWin = $('#comm-allowmodel-positionrule');
	            	var editPositionForm = $('#comm-allowmodel-positionrule-form');
	            	var editTeamWin = $('#comm-allowmodel-teamrule');
	            	var editTeamForm = $('#comm-allowmodel-teamrule-form');
	            	var copyModelWin = $('#comm-allowmodel-copy');
	            	var modelType = editModelWin.find('input[textboxname=model_type]').combobox({
	            		onChange: function(newValue,oldValue){
	            			if(newValue == '0'){
	            				editModelWin.find('input[name=mobile_no]').parents('.form-item').hide();
	            				editModelWin.find('input[name=rule_list]').parents('.form-item').show();
	            			}else{
	            				editModelWin.find('input[name=mobile_no]').parents('.form-item').show();
	            				editModelWin.find('input[name=rule_list]').parents('.form-item').hide();
	            			}
	            		}
	            	});
	            	var queryPosition = editUserWin.find('input[name=position_id]').combobox({
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
	            					success(json.position);
	            				}
	            			});
	            		},
	            		onShowPanel: function(){
	            			$(this).combobox('reload');
	            		}
	                });
	            	var queryTeam = editUserWin.find('input[name=team_id]').combotree({
	            		panelWidth: 200,
	                	url: './api/user/get_team.json',
	                	editable: false,
	                	cascadeCheck: false,
	                	onBeforeExpand: function(row){
	                		queryTeam.combotree('options').queryParams.team_id = row.team_id;
	                		return true;
	                	},
	                	onShowPanel: function(){
	                		queryTeam.combotree('options').queryParams.team_id = '';
	                		queryTeam.combotree('reload');
	                	},
	                	loadFilter: function(data){
	                		if(!!data){
	                			for(var i=0;i<data.length;i++){
	                				data[i].id = data[i].team_id;
	                				data[i].text = data[i].team_name;
	                				data[i].state = (data[i].has_sub_team?'closed':'open');
	                			}
	                			if(queryTeam.combotree('options').queryParams.team_id == ''){
	                				data.unshift({
	                					id: '',
	                					text: '---全部部门---'
	                				});
	                			}
	                		}
	                		return data;
	                	}
	                });
	            	var ruleTable = $('#allow-model-rule-list').datagrid({
	            		data: [],
	                    fitColumns: true,
	                    checkOnSelect: false,
	                    rownumbers: true,
	                    striped: true,
	                    fit: true,
	                    columns: [[{
	                        field: 'rule_name',
	                        title: '选择名称',
	                        width: 120,
	                        align: 'left'
	                    },{
	                        field: 'rule_type',
	                        title: '选择方式',
	                        width: 60,
	                        align: 'center',
	                        formatter: function(val,obj,row){
	                        	return val=='USER_RULE'?'按姓名选择':(val=='POSITION_RULE'?'按职务选择':'按部门选择');
	                        }
	                    },{
	                        field: 'rule_action',
	                        title: '是否允许参加',
	                        align: 'center',
	                        width: 60,
	                        formatter: function(val,obj,row){
	                            return val=='ALLOW'?'允许':'禁止';
	                        }
	                    },{
	                        field: 'sule_sort',
	                        title: '排序',
	                        align: 'center',
	                        width: 60,
	                        formatter: function(val,obj,row){
	                            return '<a href="javascript:void(0)" class="table-cell-icon icon-up move-up-btn">&nbsp;</a><a href="javascript:void(0)" class="table-cell-icon icon-down move-down-btn">&nbsp;</a>'
	                        }
	                    },{
	                        field: 'rule_id',
	                        title: '操作',
	                        align: 'center',
	                        width: 60,
	                        formatter: function(val,obj,row){
	                        	return '<a href="javascript:void(0)" class="table-cell-icon icon-edit edit-btn">&nbsp;</a><a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>'
	                        }
	                    }]],
	                    toolbar: [{
	                        id: 'allowmodel-user-add',
	                        text: '按姓名选择',
	                        iconCls: 'icon-add',
	                        handler: function(){
	                        	editUserForm.form('reset');
	                        	editUserForm.find('input[name=rule_id]').val('');
	                        	editUserForm.find('input[name=model_id]').val(editModelForm.find('input[name=model_id]').val());
	                        	editUserForm.find('input[name=row_index]').val('');
	                        	editUserForm.form('load',{
	                        		rule_name: 'MODEL_RULE_'+new Date().getTime()
	                        	});
	                        	userSelectedTable.datagrid('loadData',[]);
	                        	userTable.datagrid('reload');
	                        	editUserWin.window({title:'按姓名选择'}).window('open');
	                        }
	                    },{
	                        id: 'allowmodel-position-del',
	                        text: '按职务选择',
	                        iconCls: 'icon-add',
	                        handler: function(){
	                        	editPositionForm.form('reset');
	                        	editPositionForm.find('input[name=model_id]').val(editModelForm.find('input[name=model_id]').val());
	                        	editPositionForm.find('input[name=rule_id]').val('');
	                        	editPositionForm.find('input[name=row_index]').val('');
	                        	editPositionForm.form('load',{
	                        		rule_name: 'MODEL_RULE_'+new Date().getTime()
	                        	});
	                        	positionSelectedTable.datagrid('loadData',[]);
	                        	positionTable.datagrid('reload');
	                        	editPositionWin.window({title:'按职务选择'}).window('open');
	                        }
	                    },{
	                        id: 'allowmodel-team-del',
	                        text: '按部门选择',
	                        iconCls: 'icon-add',
	                        handler: function(){
	                        	editTeamForm.form('reset');
	                        	editTeamForm.find('input[name=model_id]').val(editModelForm.find('input[name=model_id]').val());
	                        	editTeamWin.find('input[name=rule_id]').val('');
	                        	editTeamWin.find('input[name=row_index]').val('');
	                        	editTeamForm.form('load',{
	                        		rule_name: 'MODEL_RULE_'+new Date().getTime()
	                        	});
	                        	teamSelectedTable.datagrid('loadData',[]);
	                        	teamTable.treegrid('options').url = './api/user/get_team.json?team_id=';
	                        	teamTable.treegrid('reload');
	                        	teamTable.treegrid('uncheckAll');
	                        	editTeamWin.window({title:'按部门选择'}).window('open');
	                        }
	                    }/*,{
	                        id: 'allowmodel-team-del',
	                        text: '添加职级规则',
	                        iconCls: 'icon-add',
	                        handler: function(){
	                        	var levels = levelTable.datagrid('getChecked');
	                        	if(levels.length == 0){
	                        		$.messager.alert('提示','请选择需要删除的职级信息','info');
	                        		return false;
	                        	}
	                        	var level_id = [];
	                			for(var i=0;i<levels.length;i++){
	                				level_id.push(levels[i].level_id);
	                			}
	                			dellevel(level_id.join(','));
	                        }
	                    }*/]
	            	});
	            	var userParam = {
	        	    	team_id: '',
	        	    	position_id: '',
	        	    	keyword: '',
	        	    	mobile_no: ''
	            	}
	            	var userTable = $('#comm-allowmodel-user').datagrid({
	                    url: './api/user/get_user_list.json',
	                    queryParams: userParam,
	                    fitColumns: true,
	                    striped: true,
	                    fit: true,
	                    pagination: true,
	                    rownumbers: true,
	                    title: '待选用户',
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
	                    toolbar: '#comm-allowmodel-user-tb',
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
	            	var userSelectedTable = $('#comm-allowmodel-selecteduser').datagrid({
	            		data: [],
	                    fitColumns: true,
	                    striped: true,
	                    fit: true,
	                    rownumbers: true,
	                    title: '已选用户',
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
	            	
	            	var positionParam = {
	        	    	keyword: ''
	            	};
	            	var positionTable = $('#comm-allowmodel-position').datagrid({
	                    url: './api/user/get_position.json',
	                    fitColumns: true,
	                    striped: true,
	                    rownumbers: true,
	                    fit: true,
	                    title: '待选职务',
	                    columns: [[{
	                        field: 'ck',
	                        checkbox: true
	                    },{
	                        field: 'position_name',
	                        title: '职务名称',
	                        width: 500,
	                        align: 'left'
	                    }]],
	                    onLoadSuccess: function(data){
	                    	data = data.rows;
	                    	var sd = positionSelectedTable.datagrid('getData').rows;
	                    	for(var i=0;i<data.length;i++){
	                    		for(var j=0;j<sd.length;j++){
	                    			if(sd[j].position_id==data[i].position_id){
	                    				positionTable.datagrid('checkRow',i);
	                    				break;
	                    			}
	                    		}
	                    	}
	                    },
	                    onCheck: function(index,row){
	                    	var data = positionSelectedTable.datagrid('getData').rows;
	                    	var data = $.grep(data,function(o){
	                    		return o.position_id == row.position_id;
	                    	});
	                    	if(data.length == 0){
	                    		positionSelectedTable.datagrid('insertRow',{
	                    			index: 0,
	                    			row: row
	                    		});
	                    	}
	                    },
	                    onUncheck: function(index,row){
	                    	var data = positionSelectedTable.datagrid('getData').rows;
	                    	$.grep(data,function(o,i){
	                    		if(!!!o)return false;
	                    		if(row.position_id == o.position_id){
	                    			positionSelectedTable.datagrid('deleteRow',i);
	                    		}
	                    		return true;
	                    	});
	                    },
	                    onUncheckAll: function(rows){
	                    	var oldData = positionSelectedTable.datagrid('getData').rows;
	                    	$.grep(rows,function(o){
	                    		for(var i=oldData.length;i>0;i--){
	                    			if(oldData[i-1].position_id == o.position_id){
	                    				positionSelectedTable.datagrid('deleteRow',i-1);
	                    				break;
	                    			}
	                    		}
	                    	});
	                    },
	                    onCheckAll: function(rows){
	                    	var oldData = positionSelectedTable.datagrid('getData').rows;
	                    	$.grep(rows,function(o){
	                    		var same = $.grep(oldData,function(od){
	                    			return od.position_id == o.position_id;
	                    		});                    		
	                    		if(same.length == 0){
	                    			positionSelectedTable.datagrid('insertRow',{
	                        			index: 0,
	                        			row: o
	                        		});
	                    		}
	                    	});
	                    },
	                    toolbar: '#comm-allowmodel-position-tb',
	                    loadFilter: function(data){
	                    	if(positionParam.keyword == ''){
	                    		return {
	                                total: data.position.length,
	                                rows: data.position
	                            };
	                    	}else{
	                    		var position = $.grep(data.position,function(o){
	                    			return o.position_name.indexOf(positionParam.keyword)>-1;
	                    		});
	                    		return {
	                                total: position.length,
	                                rows: position
	                            }; 
	                    	}
	                        
	                    }
	                });
	            	var positionSelectedTable = $('#comm-allowmodel-selectedposition').datagrid({
	            		data: [],
	                    fitColumns: true,
	                    striped: true,
	                    fitColumns: true,
	                    fit: true,
	                    rownumbers: true,
	                    title: '已选职务',
	                    columns: [[{
	                        field: 'position_name',
	                        title: '职务名称',
	                        width: 500,
	                        align: 'left'
	                    },{
	                        field: 'position_id',
	                        title: '操作',
	                        align: 'center',
	                        width: 60,
	                        formatter: function(val,obj,row){
	                        	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>';
	                        }
	                    }]]
	                });
	            	
	            	var teamTable = $('#comm-allowmodel-team').treegrid({
	                	url: './api/user/get_team.json',
	                	treeField: 'team_name',
	                	idField: 'team_id',
	                    rownumbers: true,
	                    fitColumns: true,
	                    singleSelect: false,
	                    fit: true,
	                    checkOnSelect: true,
	                	columns: [[{
	                        field: 'ck',
	                        checkbox: true
	                    },{
	                		field: 'team_name',
	                		width: 450,
	                		title: '部门名称'
	                	}]],
	                	onBeforeExpand: function(row){
	                		teamTable.treegrid('options').url = './api/user/get_team.json?team_id='+row.team_id;
	                		return true;
	                	},
	                	onLoadSuccess: function(data){
	                    	var sd = teamSelectedTable.datagrid('getData').rows;
	                    	for(var i=0;i<sd.length;i++){
	                    		teamTable.treegrid('select',sd[i].team_id);
	                    	}
	                    },
	                    onCheck: function(row){
	                    	var data = teamSelectedTable.datagrid('getData').rows;
	                    	var data = $.grep(data,function(o){
	                    		return o.team_id == row.team_id;
	                    	});
	                    	if(data.length == 0){
	                    		teamSelectedTable.datagrid('insertRow',{
	                    			index: 0,
	                    			row: row
	                    		});
	                    	}
	                    },
	                    onUncheck: function(row){
	                    	var data = teamSelectedTable.datagrid('getData').rows;
	                    	$.grep(data,function(o,i){
	                    		if(!!!o)return false;
	                    		if(row.team_id == o.team_id){
	                    			teamSelectedTable.datagrid('deleteRow',i);
	                    		}
	                    		return true;
	                    	});
	                    },
	                    onUncheckAll: function(rows){
	                    	var oldData = teamSelectedTable.datagrid('getData').rows;
	                    	$.grep(rows,function(o){
	                    		for(var i=oldData.length;i>0;i--){
	                    			if(oldData[i-1].team_id == o.team_id){
	                    				teamSelectedTable.datagrid('deleteRow',i-1);
	                    				break;
	                    			}
	                    		}
	                    	});
	                    },
	                    onCheckAll: function(rows){
	                    	var oldData = teamSelectedTable.datagrid('getData').rows;
	                    	$.grep(rows,function(o){
	                    		var same = $.grep(oldData,function(od){
	                    			return od.team_id == o.team_id;
	                    		});                    		
	                    		if(same.length == 0){
	                    			teamSelectedTable.datagrid('insertRow',{
	                        			index: 0,
	                        			row: o
	                        		});
	                    		}
	                    	});
	                    },
	                	loadFilter: function(data){
	                		if(!!data){
	                			for(var i=0;i<data.length;i++){
	                				data[i].state = (data[i].has_sub_team?'closed':'open');
	                			}
	                		}
	                		return data;
	                	}
	                });  
	            	var teamSelectedTable = $('#comm-allowmodel-selectedteam').datagrid({
	            		data: [],
	                    fitColumns: true,
	                    striped: true,
	                    fitColumns: true,
	                    fit: true,
	                    rownumbers: true,
	                    title: '已选部门',
	                    columns: [[{
	                        field: 'team_name',
	                        title: '部门名称',
	                        width: 500,
	                        align: 'left'
	                    },{
	                        field: 'team_id',
	                        title: '操作',
	                        align: 'center',
	                        width: 60,
	                        formatter: function(val,obj,row){
	                        	return '<a href="javascript:void(0)" class="table-cell-icon icon-remove del-btn">&nbsp;</a>';
	                        }
	                    }]]
	            	}); 
	            	var copyTable = $('#comm-allowmodel-copy-table').datagrid({
	                    url: _curps.copy_table_url,
	                    queryParams: _curps.copyParam,
	                    fitColumns: true,
	                    striped: true,
	                    fit: true,
	                    pagination: true,
	                    rownumbers: true,
	                    singleSelect: true,
	                    pageSize: 20,
	                    columns: _curps.copy_table_columns,
	                    toolbar: '#comm-allowmodel-copy-tb',
	                    changePages: function(params,pageObj){
	                    	$.extend(params,{
	                    		start: (pageObj.page-1)*pageObj.rows,
	                    		length: pageObj.rows
	                    	});
	                    },
	                    loadFilter: function(data){
	                        return {
	                            total: data[_curps.fName],
	                            rows: data[_curps.dName]
	                        };
	                    }
	                });
	            	
	            	copyModelWin.find('.search-btn').click(function(){
	            		for(var name in _curps.copyParam){
	            			_curps.copyParam[name] = copyModelWin.find('input[name='+name+']').val();
	            		}
	            		copyTable.datagrid('reload');
	            	});
	            	
	            	editTeamWin.on('click','.del-btn',function(){
	            		var team = teamSelectedTable.datagrid('getData').rows[$(this).parents('tr').index()];
	            		teamTable.treegrid('unselect',team.team_id);
	            	});
	            	editTeamWin.find('.edit-save').click(function(){
	            		var model_id = editTeamWin.find('input[name=model_id]').val();
	            		var rule_id = editTeamWin.find('input[name=rule_id]').val();
	            		var rule_name = editTeamWin.find('input[name=rule_name]').val();
	            		var action = editTeamWin.find('input[name=rule_action]').val();
	            		var row_index = editTeamWin.find('input[name=row_index]').val();
	            		var teams = teamSelectedTable.datagrid('getData');
	            		var url = './api/allow/create_team_rule.json';
	            		if(rule_name == ''){
	            			$.messager.alert('提示','规则名称不能为空！','error');
	            			return false;
	            		}
	            		if(teams.total == 0){
	            			$.messager.alert('提示','请至少选择一个部门作为规则对象！','error');
	            			return false;
	            		}
	            		var team_id = [];
	            		for(var i=teams.rows.length;i>0;i--){
	            			team_id.push(teams.rows[i-1].team_id);
	            		}
	            		if(model_id == ''){
	            			var rule = {
	            				rule_id: 0,
	            				rule_name: rule_name,
	            				rule_action: action,
	            				action: action,
	            				obj_ids: {team_id:team_id},
	            				rule_type: 'TEAM_RULE',
	            				rule_data: teams.rows
	            			};
	            			if(row_index == ''){
	            				ruleTable.datagrid('insertRow',{
	            					index: 0,
	            					row: rule
	            				});
	            			}else{
	            				ruleTable.datagrid('updateRow',{
	            					index: parseInt(row_index),
	            					row: rule
	            				});
	            			}
	            			editTeamWin.window('close');
	            		}else{
	            			if(rule_id != ''){
	            				url = './api/allow/update_team_rule.json';
	            			}
	            			Wz.ajax({
	            				type: 'post',
	            				url: url,
	            				data: {
	            					model_id: model_id,
	            					rule_id: rule_id,
	            					rule_name: rule_name,
	            					rule_action: action,
	            					team_id: team_id.join(',')
	            				},
	            				success: function(json){
	            					if(json.result == 'SUCC'){
	        							var rule = {
	    									rule_id: json.rule_id,
	    									rule_name: rule_name,
	    									rule_action: action,
	    									action: action,
	    									obj_ids: {team_id:team_id},
	    									rule_type: 'TEAM_RULE',
	    									rule_data: teams.rows
	        							};
	            						if(!!!rule_id){
	            							ruleTable.datagrid('insertRow',{
	            								index: 0,
	            								row: rule
	            							});            							
	            						}else{
	            							ruleTable.datagrid('updateRow',{
	            								index: parseInt(row_index),
	            								row: rule
	            							}); 
	            						}
	            						editTeamWin.window('close');
	            					}else{
	            						$.messager.alert('错误',json.fail_text,'error');
		        	    				return false;
	            					}
	            				}
	            			});
	            		}
	            	});
	            	
	            	editPositionWin.find('.search-btn').click(function(){
	            		positionParam.keyword = editPositionWin.find('input[name=keyword]').val();
	            		positionTable.datagrid('reload');
	            	});
	            	editPositionWin.on('click','.del-btn',function(){
	            		var position = positionSelectedTable.datagrid('getData').rows[$(this).parents('tr').index()];
	            		var rows = positionTable.datagrid('getData').rows;
	            		var index = 0;
	            		for(;index<rows.length;index++){
	            			if(rows[index].position_id == position.position_id){
	            				break;
	            			}
	            		}
	            		positionTable.datagrid('uncheckRow',index);
	            	});
	            	editPositionWin.find('.edit-save').click(function(){
	            		var model_id = editPositionWin.find('input[name=model_id]').val();
	            		var rule_id = editPositionWin.find('input[name=rule_id]').val();
	            		var rule_name = editPositionWin.find('input[name=rule_name]').val();
	            		var action = editPositionWin.find('input[name=rule_action]').val();
	            		var row_index = editPositionWin.find('input[name=row_index]').val();
	            		var positions = positionSelectedTable.datagrid('getData');
	            		var url = './api/allow/create_position_rule.json';
	            		if(rule_name == ''){
	            			$.messager.alert('提示','规则名称不能为空！','error');
	            			return false;
	            		}
	            		if(positions.total == 0){
	            			$.messager.alert('提示','请至少选择一个职务作为规则对象！','error');
	            			return false;
	            		}
	            		var position_id = [];
	            		for(var i=positions.rows.length;i>0;i--){
	            			position_id.push(positions.rows[i-1].position_id);
	            		}
	            		if(model_id == ''){
	            			var rule = {
	            				rule_id: 0,
	            				rule_name: rule_name,
	            				rule_action: action,
	            				action: action,
	            				obj_ids: {position_id:position_id},
	            				rule_type: 'POSITION_RULE',
	            				rule_data: positions.rows
	            			};
	            			if(row_index == ''){
	            				ruleTable.datagrid('insertRow',{
	            					index: 0,
	            					row: rule
	            				});
	            			}else{
	            				ruleTable.datagrid('updateRow',{
	            					index: parseInt(row_index),
	            					row: rule
	            				});
	            			}
	            			editPositionWin.window('close');
	            		}else{
	            			if(rule_id != ''){
	            				url = './api/allow/update_position_rule.json';
	            			}
	            			Wz.ajax({
	            				type: 'post',
	            				url: url,
	            				data: {
	            					model_id: model_id,
	            					rule_id: rule_id,
	            					rule_name: rule_name,
	            					rule_action: action,
	            					position_id: position_id.join(',')
	            				},
	            				success: function(json){
	            					if(json.result == 'SUCC'){
	        							var rule = {
	    									rule_id: json.rule_id,
	    									rule_name: rule_name,
	    									rule_action: action,
	    									action: action,
	    									obj_ids: {position_id:position_id},
	    									rule_type: 'POSITION_RULE',
	    									rule_data: positions.rows
	        							};
	            						if(!!!rule_id){
	            							ruleTable.datagrid('insertRow',{
	            								index: 0,
	            								row: rule
	            							});            							
	            						}else{
	            							ruleTable.datagrid('updateRow',{
	            								index: parseInt(row_index),
	            								row: rule
	            							}); 
	            						}
	            						editPositionWin.window('close');
	            					}else{
	            						$.messager.alert('错误',json.fail_text,'error');
		        	    				return false;
	            					}
	            				}
	            			});
	            		}
	            	});
	            	
	            	editUserWin.find('.search-btn').click(function(){
	            		userParam.keyword = editUserWin.find('input[name=keyword]').val();
	            		userParam.position_id = queryPosition.combobox('getValue');
	            		userParam.mobile_no = editUserWin.find('input[name=mobile_no]').val();
	            		userParam.team_id = queryTeam.combotree('getValue');
	            		userTable.datagrid('reload');
	            	});
	            	
	            	editUserWin.on('click','.del-btn',function(){
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
	            	editUserWin.find('.edit-save').click(function(){
	            		var model_id = editUserWin.find('input[name=model_id]').val();
	            		var rule_id = editUserWin.find('input[name=rule_id]').val();
	            		var rule_name = editUserWin.find('input[name=rule_name]').val();
	            		var action = editUserWin.find('input[name=rule_action]').val();
	            		var row_index = editUserWin.find('input[name=row_index]').val();
	            		var users = userSelectedTable.datagrid('getData');
	            		var url = './api/allow/create_user_rule.json';
	            		if(rule_name == ''){
	            			$.messager.alert('提示','规则名称不能为空！','error');
	            			return false;
	            		}
	            		if(users.total == 0){
	            			$.messager.alert('提示','请至少选择一个用户作为规则对象！','error');
	            			return false;
	            		}
	            		var user_id = [];
	            		for(var i=users.rows.length;i>0;i--){
	            			user_id.push(users.rows[i-1].user_id);
	            		}
	            		if(model_id == ''){
	            			var rule = {
	            				rule_id: 0,
	            				rule_name: rule_name,
	            				rule_action: action,
	            				action: action,
	            				obj_ids: {user_id:user_id},
	            				rule_type: 'USER_RULE',
	            				rule_data: users.rows
	            			};
	            			if(row_index == ''){
	            				ruleTable.datagrid('insertRow',{
	            					index: 0,
	            					row: rule
	            				});
	            			}else{
	            				ruleTable.datagrid('updateRow',{
	            					index: parseInt(row_index),
	            					row: rule
	            				});
	            			}
	            			editUserWin.window('close');
	            		}else{
	            			if(rule_id != ''){
	            				url = './api/allow/update_user_rule.json';
	            			}
	            			Wz.ajax({
	            				type: 'post',
	            				url: url,
	            				data: {
	            					model_id: model_id,
	            					rule_id: rule_id,
	            					rule_name: rule_name,
	            					rule_action: action,
	            					user_id: user_id.join(',')
	            				},
	            				success: function(json){
	            					if(json.result == 'SUCC'){
	        							var rule = {
	    									rule_id: json.rule_id,
	    									rule_name: rule_name,
	    									rule_action: action,
	    									action: action,
	    									obj_ids: {user_id:user_id},
	    									rule_type: 'USER_RULE',
	    									rule_data: users.rows
	        							};
	            						if(!!!rule_id){
	            							ruleTable.datagrid('insertRow',{
	            								index: 0,
	            								row: rule
	            							});            							
	            						}else{
	            							ruleTable.datagrid('updateRow',{
	            								index: parseInt(row_index),
	            								row: rule
	            							}); 
	            						}
	            						editUserWin.window('close');
	            					}else{
	            						$.messager.alert('错误',json.fail_text,'error');
		        	    				return false;
	            					}
	            				}
	            			});
	            		}
	            	});
	            	
	            	copyModelWin.find('.edit-save').click(function(){
	            		var rows = copyTable.datagrid('getChecked');
	            		if(rows.length == 0){
	            			$.messager.alert('错误','请选择一个需要复制的对象！','error');
	            			return false;
	            		}
	            		Wz.ajax({
	            			type: 'post',
	            			url: './api/allow/copy_model.json',
	            			data: {
	            				allow_model_id: rows[0].allow_model_id
	            			},
	            			success: function(json){
	            				if(json.result == 'SUCC'){
	            					_setValue({
	            						model_id: json.new_model_id,
	            						model_name: rows[0].exam_name + '考试模型'
	            					});
	            					copyModelWin.window('close');
	            				}else{
	            					$.messager.alert('错误',json.fail_text,'error');
	            				}
	            			}
	            		});
	            	});
	            	editModelWin.on('click','.move-up-btn',function(){
	            		var row_index = $(this).parents('tr').index();
	            		var ruleData = ruleTable.datagrid('getData').rows[row_index];
	            		if(row_index>0){
	            			ruleTable.datagrid('deleteRow',row_index);
	            			ruleTable.datagrid('insertRow',{
	            				index: row_index-1,
	            				row: ruleData
	            			});
	            		}
	            	});
	            	editModelWin.on('click','.move-down-btn',function(){
	            		var row_index = $(this).parents('tr').index();
	            		var rows = ruleTable.datagrid('getData').rows;
	            		var ruleData = rows[row_index];
	            		if(row_index<rows.length-1){
	            			ruleTable.datagrid('deleteRow',row_index);
	            			ruleTable.datagrid('insertRow',{
	            				index: row_index+1,
	            				row: ruleData
	            			});
	            		}
	            	});
	            	editModelWin.on('click','.edit-btn',function(){
	            		var row_index = $(this).parents('tr').index();
	            		var ruleData = ruleTable.datagrid('getData').rows[row_index];
	            		if(ruleData.rule_type == 'USER_RULE'){
	                    	editUserForm.find('input[name=model_id]').val(editModelForm.find('input[name=model_id]').val());
	                    	editUserForm.form('load',ruleData);
	                    	userSelectedTable.datagrid('loadData',ruleData.rule_data);
	                    	userTable.datagrid('reload');
	                    	editUserForm.find('input[name=row_index]').val(row_index);
	                    	editUserWin.window({title:'按姓名选择'}).window('open');
	            		}else if(ruleData.rule_type == 'POSITION_RULE'){
	                    	editPositionForm.find('input[name=model_id]').val(editModelForm.find('input[name=model_id]').val());
	            			editPositionForm.form('load',ruleData);
	                    	positionSelectedTable.datagrid('loadData',ruleData.rule_data);
	                    	positionTable.datagrid('reload');
	                    	editPositionForm.find('input[name=row_index]').val(row_index);
	                    	editPositionWin.window({title:'按职务选择'}).window('open');
	            		}else if(ruleData.rule_type == 'TEAM_RULE'){
	            			editTeamForm.find('input[name=model_id]').val(editModelForm.find('input[name=model_id]').val());
	            			editTeamForm.form('load',ruleData);
	                    	teamTable.treegrid('uncheckAll');
	                    	teamSelectedTable.datagrid('loadData',ruleData.rule_data);
	                    	teamTable.treegrid('options').url = './api/user/get_team.json?team_id=';
	                    	teamTable.treegrid('reload');
	                    	editTeamForm.find('input[name=row_index]').val(row_index);
	                    	editTeamWin.window({title:'按 部门选择'}).window('open');
	            		}
	            	});
	            	editModelWin.on('click','.del-btn',function(){
	            		var row_index = $(this).parents('tr').index();
	            		var model_id = editModelWin.find('input[name=model_id]').val();
	            		var ruleData = ruleTable.datagrid('getData').rows[row_index];
	            		if(model_id == ''){
	            			ruleTable.datagrid('deleteRow',row_index);
	            		}else{
	            			$.messager.confirm('提示','删除之后访问模型会直接生效，请确认要删除吗？',function(ok){
	            				if(ok){
	            					Wz.ajax({
	            						type: 'post',
	            						url: './api/allow/delete_rule.json',
	            						data: {
	            							model_id: model_id,
	            							rule_id: ruleData.rule_id
	            						},
	            						success: function(json){
	            							if(json.result == 'SUCC'){
	            								ruleTable.datagrid('deleteRow',row_index);
	            							}else{
	            								$.messager.alert('错误',json.fail_text,'error');
	            							}
	            						}
	            					});    			
	            				}
	            			});            			
	            		}
	            	});
	            	editModelWin.find('.edit-save').click(function(){
	            		var model_id = editModelWin.find('input[name=model_id]').val();
	            		var model_name = editModelWin.find('input[name=model_name]').val();
	            		var model_type = editModelWin.find('input[name=model_type]').val();
	            		var default_action = editModelWin.find('input[name=default_action]').val();
	            		var mobile_no_list = editModelWin.find('input[name=mobile_no]').val().split('\n');
	            		var url = './api/allow/create_model.json';
	            		var param = {
	            			model_id: model_id,
	            			model_name: model_name,
	            			default_action: default_action
	            		};
	            		if(model_name == ''){
	            			$.messager.alert('提示','模型名称不能为空！','error');
	            			return false;
	            		}
	            		if(model_type == '1'){
	            			url = './api/allow/import_user.json';
	            			param.mobile_no_list = mobile_no_list.join(',');
	            		}else{
	            			if(model_id != ''){
	            				url = './api/allow/update_model.json';
	            			}else{
	            				var rules = ruleTable.datagrid('getData').rows;
	            				var rule_list = {rule: []};
	            				for(var i=rules.length;i>0;i--){
	            					var ruleObj = {
	            						rule_id: rules[i-1].rule_id,
	            						rule_name: rules[i-1].rule_name,
	            						action: rules[i-1].action
	            					};
	            					ruleObj[rules[i-1].rule_type.toLowerCase()] = rules[i-1].obj_ids;
	            					rule_list.rule.push(ruleObj);
	            				}
	            				param.rule_list = JSON.stringify(rule_list);
	            			}
	            		}
	            		Wz.ajax({
	            			type: 'post',
	            			url: url,
	            			data: param,
	            			success: function(json){
	            				if(json.result == 'SUCC'){
	            					_setValue({
	            						model_id: (json.model_id || param.model_id),
	            						model_name: param.model_name||''
	            					});
	            					editModelWin.window('close');
	            				}else{
	            					$.messager.alert('错误',json.fail_text,'error');
	        	    				return false;
	            				}
	            			}
	            		});
	            	});
	            }
			});
		}
	
		editBtn.click(function(){
			_curps = ps;
			_setValue = setValue;
			var editModelWin = $('#comm-allowmodel-edit');
	    	var editModelForm = $('#comm-allowmodel-edit-form');
	    	var ruleTable = $('#allow-model-rule-list');
			editModelForm.form('reset');
			if(!!!ps.data.model_id){
				var ld = {
					model_id: ps.data.model_id,
					model_name: ps.data.model_name||('MODEL_'+new Date().getTime()),
					default_action: ps.data.default_action || 'DENY'
				}
				editModelForm.form('load',ld);
				ruleTable.datagrid('loadData',[]);
				editModelWin.find('input[name=mobile_no]').parents('.form-item').hide();
				editModelWin.find('input[name=rule_list]').parents('.form-item').show();				
	    		editModelWin.window({title:'选择对象'}).window('open');
			}else{
				editModelForm.find('input[name=model_type]').parents('.form-item').hide();
				editModelWin.find('input[name=mobile_no]').parents('.form-item').hide();
				editModelWin.find('input[name=rule_list]').parents('.form-item').show();
				Wz.ajax({
					url: './api/allow/get_model_by_id.json',
					data: {
						model_id: ps.data.model_id,
						_t: new Date().getTime()
					},
					success: function(json){
						if(json.result == 'SUCC'){
							editModelForm.form('load',json.model[0]);
							ruleTable.datagrid('loadData',json.model[0].rule_list);
							editModelWin.window({title:'重新选择对象'}).window('open');
						}
					}
				})
			}
		});
		delBtn.click(function(){
			setValue({
				model_id: '',
				model_name: ''
			});
		});
		copyBtn.click(function(){
			_curps = ps;
			_setValue = setValue;
			$('#comm-allowmodel-copy-table').datagrid({
				url: _curps.copy_table_url,
	            queryParams: _curps.copyParam,
	            columns: _curps.copy_table_columns,
			});
			$('#comm-allowmodel-copy').window({'title':ps.copy_win_title}).window('open');
		});
		function setValue(obj){
			ps.data.model_name = obj.model_name || '';
			ps.data.model_id = obj.model_id || '';
			modelName.text(ps.data.model_name);
			odom.val(ps.data.model_id);
			if(ps.data.model_id == ''){
				editBtn.linkbutton({text:'选择对象'});
				delBtn.hide();
			}else{
				editBtn.linkbutton({text:'重新选择对象'});
				delBtn.show();
			}
		}
		
		function getName(){
			return ps.data.model_name || '';
		}
		
		function getValue(){
			return ps.data.model_id || '';
		}
		
		return {
			getValue: getValue,
			getName: getName,
			setValue: setValue
		};
	}
})()


//easyui validatebox验证扩展
$.extend($.fn.validatebox.defaults.rules, {
	charNum: {//验证必须包含数字和字母
		validator: function(value){
			return !!(/\d/.test(value)&&/[a-zA-Z]/.test(value));
		},
		message: '必须包含数字和字母'
	},
    idcard: {// 验证身份证
        validator: function (value) {
            return /^\d{15}(\d{2}[A-Za-z0-9])?$/i.test(value);
        },
        message: '身份证号码格式不正确'
    },
    minLength: {
        validator: function (value, param) {
            return value.length >= param[0];
        },
        message: '请输入至少（2）个字符.'
    },
    length: { validator: function (value, param) {
        var len = $.trim(value).length;
        return len >= param[0] && len <= param[1];
    },
        message: "输入内容长度必须介于{0}和{1}之间."
    },
    phone: {// 验证电话号码
        validator: function (value) {
            return /^((\d2,3)|(\d{3}\-))?(0\d2,3|0\d{2,3}-)?[1-9]\d{6,7}(\-\d{1,4})?(,((\d2,3)|(\d{3}\-))?(0\d2,3|0\d{2,3}-)?[1-9]\d{6,7}(\-\d{1,4})?)*$/i.test(value);
        },
        message: '格式不正确,请使用下面格式:020-88888888'
    },
    mobile: {// 验证手机号码
        validator: function (value) {
            return /^1\d{10}(,1\d{10})*$/i.test(value);
        },
        message: '手机号码格式不正确'
    },
    multiline_mobile: {
    	validator: function (value) {
            return /^1\d{10}(\n1\d{10})*[\n]?$/i.test(value);
        },
        message: '您输入了错误的手机号码，请检查！'
    },
    intOrFloat: {// 验证整数或小数
        validator: function (value) {
            return /^\d+(\.\d+)?$/i.test(value);
        },
        message: '请输入数字，并确保格式正确'
    },
    currency: {// 验证货币
        validator: function (value) {
            return /^\d+(\.\d+)?$/i.test(value);
        },
        message: '货币格式不正确'
    },
    qq: {// 验证QQ,从10000开始
        validator: function (value) {
            return /^[1-9]\d{4,9}$/i.test(value);
        },
        message: 'QQ号码格式不正确'
    },
    integer: {// 验证整数 可正负数
        validator: function (value) {
            //return /^[+]?[1-9]+\d*$/i.test(value);

            return /^([+]?[0-9])|([-]?[0-9])+\d*$/i.test(value);
        },
        message: '请输入整数'
    },
    age: {// 验证年龄
        validator: function (value) {
            return /^(?:[1-9][0-9]?|1[01][0-9]|120)$/i.test(value);
        },
        message: '年龄必须是0到120之间的整数'
    },

    chinese: {// 验证中文
        validator: function (value) {
            return /^[\Α-\￥]+$/i.test(value);
        },
        message: '请输入中文'
    },
    english: {// 验证英语
        validator: function (value) {
            return /^[A-Za-z]+$/i.test(value);
        },
        message: '请输入英文'
    },
    unnormal: {// 验证是否包含空格和非法字符
        validator: function (value) {
            return /.+/i.test(value);
        },
        message: '输入值不能为空和包含其他非法字符'
    },
    username: {// 验证用户名
        validator: function (value) {
            return /^[a-zA-Z][a-zA-Z0-9_]{5,15}$/i.test(value);
        },
        message: '用户名不合法（字母开头，允许6-16字节，允许字母数字下划线）'
    },
    faxno: {// 验证传真
        validator: function (value) {
            //            return /^[+]{0,1}(\d){1,3}[ ]?([-]?((\d)|[ ]){1,12})+$/i.test(value);
            return /^((\d2,3)|(\d{3}\-))?(0\d2,3|0\d{2,3}-)?[1-9]\d{6,7}(\-\d{1,4})?$/i.test(value);
        },
        message: '传真号码不正确'
    },
    zip: {// 验证邮政编码
        validator: function (value) {
            return /^[1-9]\d{5}$/i.test(value);
        },
        message: '邮政编码格式不正确'
    },
    ip: {// 验证IP地址
        validator: function (value) {
            return /d+.d+.d+.d+/i.test(value);
        },
        message: 'IP地址格式不正确'
    },
    name: {// 验证姓名，可以是中文或英文
        validator: function (value) {
            return /^[\Α-\￥]+$/i.test(value) | /^\w+[\w\s]+\w+$/i.test(value);
        },
        message: '请输入姓名'
    },
    date: {// 验证姓名，可以是中文或英文
        validator: function (value) {
            //格式yyyy-MM-dd或yyyy-M-d
            return /^(?:(?!0000)[0-9]{4}([-]?)(?:(?:0?[1-9]|1[0-2])\1(?:0?[1-9]|1[0-9]|2[0-8])|(?:0?[13-9]|1[0-2])\1(?:29|30)|(?:0?[13578]|1[02])\1(?:31))|(?:[0-9]{2}(?:0[48]|[2468][048]|[13579][26])|(?:0[48]|[2468][048]|[13579][26])00)([-]?)0?2\2(?:29))$/i.test(value);
        },
        message: '清输入合适的日期格式'
    },
    msn: {
        validator: function (value) {
            return /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/.test(value);
        },
        message: '请输入有效的msn账号(例：abc@hotnail(msn/live).com)'
    },
    same: {
        validator: function (value, param) {
            if ($("#" + param[0]).val() != "" && value != "") {
                return $("#" + param[0]).val() == value;
            } else {
                return true;
            }
        },
        message: '两次输入的密码不一致！'
    }
});
(function($){
	$.extend($.fn,{
		tagarea: function(setting){
			$.fn.tagarea.defaults = {
				wrapWidth: 300,
				wrapHeight: 60,
				name: 'tag',
				tipInfo: '',
				required: false,
				remote: false,
				data: [],
				loader: function(){},
				params: {}
			};
			
			var ps = $.extend({},$.fn.tagarea.defaults,setting);
			
			var myDom = $(this);
			var wrapDom = $('<div class="tag-wrap" style="width:'+ps.wrapWidth+'px;height:'+ps.wrapHeight+'px;"></div>');
			myDom.hide();
			myDom.before(wrapDom);
			loadData();
			
			wrapDom.on('click','.tag-item',function(){
				if($(this).hasClass('active')){
					$(this).removeClass('active');
				}else{
					$(this).addClass('active');
				}
				var val = [];
				wrapDom.find('.active').each(function(){
					val.push($(this).attr('value'));
				});
				myDom.val(val.join(','));
			});
			
			function loadData(){
				if(ps.remote){
					ps.loader(success)
				}else{
					success(ps.data);
				}
			}
			
			function success(data){
				var items = [];
				for(var i=0;i<data.length;i++){
					items.push('<a href="javascript:void(0)" class="tag-item" value="'+data[i]+'">'+data[i]+'</a>');
				}
				wrapDom.html(items.join(''));
			}
			
			function getValue(){
				return myDom.val();
			}
			function setValue(val){
				val = val.split(',');
				for(var i=0;i<val.length;i++){
					wrapDom.find('.tag-item[value="'+val+'"]').addClass('active');					
				}
				myDom.val(val);
			}
			function validate(){
				if(ps.required && getValue()==''){
					wrapDom.addClass('textbox-invalid');
					return false;
				}
			}
			
			function reset(){
				wrapDom.find('.tag-item.active').removeClass('active');
				myDom.val('');
			}
			
			function reload(){
				loadData();
			}
			
			return {
				getValue: getValue,
				setValue: setValue,
				validate: validate,
				reset: reset,
				reload: reload
			};
		}
	});
})(jQuery);
(function($){
	$.extend($.fn,{
		uploadimage: function(setting){
			$.fn.uploadimage.defaults = {
				wrapWidth: 300,
				wrapHeight: 80,
				imgWidth: 60,
				imgHeight: 60,
				name: 'upload_file',
				tipInfo: '',
				supportType: ['jpg','png','gif','jpeg'],
				sizeType: '60',
				maxSize: 2,
				defaultImage: './static/images/add-img.png',
				required: false,
				params: {}
			};
			
			var ps = $.extend({},$.fn.uploadimage.defaults,setting);
			ps.url = ps.url+'?company_id='+Wz.cur_company_id;
			var myDom = $(this);
			var uploadDom = $('<div class="uploadimage-wrap" style="width:'+ps.wrapWidth+'px;"><div class="image-view"><img src="'+ps.defaultImage+'"><input type="file" name="'+ps.name+'" /><a href="javascript:void(0)" style="display:none;" class="reload-img">重选</a><a href="javascript:void(0)" style="display:none;" class="del-img"></a></div><p class="upload-img-tipinfo">'+ps.tipInfo+'</p></div>');
			var imageDom = uploadDom.find('img');
			var fileInput = uploadDom.find('input');
			var modifyBtn = uploadDom.find('.reload-img');
			var delBtn = uploadDom.find('.del-img');
			var iframe = $('#wz-file-upload-iframe');
			if(iframe.length == 0){
				iframe = $('<iframe id="wz-file-upload-iframe" style="display:none;" name="uploadfile-iframe">').appendTo('body');
			}
			var formDom = $('<form style="display:none;" method="post" action="'+ps.url+'" enctype="multipart/form-data" target="uploadfile-iframe"></form>').appendTo('body');
			for(var p in ps.params){
				formDom.append('<input name="'+p+'" value="'+ps.params[p]+'"/>');
			}
			$(this).hide();
			$(this).before(uploadDom);
			
			function showLoadMask(){
				uploadDom.find('.image-view').append('<div class="image-load-mask"></div><div class="image-load-loading"></div>');
			}
			function hideLoadMask(){
				uploadDom.find('.image-load-mask,.image-load-loading').remove();
			}
			function getValue(){
				return myDom.val();
			}
			function setValue(name,url){
				var url = url || Wz.config['image_'+ps.sizeType+'_url_prefix'] + name;
				myDom.val(name);
				imageDom.attr('src',url);
				modifyBtn.show();
				delBtn.show();
			}
			function validate(){
				if(ps.required && getValue()==''){
					uploadDom.addClass('textbox-invalid');
					return false;
				}
			}
			
			function loadIframe(){
				hideLoadMask();
				uploadDom.find('.image-view').append(fileInput.val(''));
				try{
					var result = this.contentDocument.body.innerHTML;
					result = result.replace(/^(<[^>]*>)+|(<[^>]*>)+$/g,'');
					result = JSON.parse(result);
					if(result.result == 'SUCC'){
						imageDom.attr('src',result['image_'+ps.sizeType+'_url']);
						myDom.val(result.image_name);
						uploadDom.removeClass('textbox-invalid');
						modifyBtn.show();
						delBtn.show();
					}else{
						$.messager.show({
			                title:'错误',
			                msg:result.fail_text,
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
					}
				}catch(e){
					$.messager.show({
		                title:'错误',
		                msg:'图片上传失败！',
		                showType:'fade',
		                style:{
		                    right:'',
		                    top:document.body.scrollTop+document.documentElement.scrollTop,
		                    bottom:''
		                },
		                timeout: 1000
		            });
				}
			}
			fileInput.bind('change',function(){
				showLoadMask();
				if(!!this.files && this.files.length>0){
					var file = this.files[0];
					var file_type = file.type;
					var file_size = file.size;
					if(!RegExp('image\/('+ps.supportType.join('|')+')','i').test(file_type)){
						$.messager.show({
			                title:'错误',
			                msg:'请上传'+ps.supportType.join('|')+'格式的图片！',
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
						hideLoadMask();
						return false;
					}
					if(file_size/1024/1024 > ps.maxSize){
						$.messager.show({
			                title:'错误',
			                msg:'请上传'+px.maxSize+'M以下的图片！',
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
						hideLoadMask();
						return false;
					}
				}else{
					var file_name = this.value;
					if(!!!file_name.match(RegExp('\.('+ps.supportType.join('|')+')$','i'))){
						$.messager.show({
			                title:'错误',
			                msg:'请上传'+ps.supportType.join('|')+'格式的图片！',
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
						hideLoadMask();
						return false;
					}
				}
				iframe.one('load',loadIframe);
				formDom.append(fileInput);
				formDom.submit();
			});
			delBtn.bind('click',function(){
				imageDom.attr('src',ps.defaultImage);
				myDom.val('');
				modifyBtn.hide();
				delBtn.hide();
			});
			function reset(){
				imageDom.attr('src',ps.defaultImage);
				myDom.val('');
				modifyBtn.hide();
				delBtn.hide();
			}
			function getUrl(){
				return imageDom.attr('src');
			}
			
			return {
				getValue: getValue,
				setValue: setValue,
				validate: validate,
				getUrl: getUrl,
				reset: reset
			};
		}
	});
})(jQuery);
(function($){
	$.extend($.fn,{
		uploadfile: function(setting){
			$.fn.uploadimage.defaults = {
				wrapWidth: 300,
				name: 'upload_file',
				supportType: ['avi','mp4'],
				maxSize: 40,
				required: false,
				resultFormatter: function(val){
					return val.file_name;
				},
				params: {}
			};
			
			var ps = $.extend({},$.fn.uploadfile.defaults,setting);

			ps.url = ps.url+'?company_id='+Wz.cur_company_id;
			var myDom = $(this);
			var uploadDom = $('<div class="wz-file-upload" style="width:'+ps.wrapWidth+'px;"><a class="wz-file-upload-btn" href="javascript:void(0)">上传文件 <input type="file" name="'+ps.name+'" class="wz-file-upload-input"></a><span class="wz-file-upload-tip">提示：支持'+ps.supportType.join(',')+'格式，大小&lt;'+ps.maxSize+'M</span><div class="wz-file-upload-info" style="display:none;">文件类型： <em>MP3</em>  文件大小：<em>12.3</em>M  <a href="javascript:void(0)">点我预览</a></div></div>');
			var typeField = uploadDom.find('.wz-file-upload-info em:first');
			var sizeField = uploadDom.find('.wz-file-upload-info em:last');
			var previewBtn = uploadDom.find('.wz-file-upload-info a');
			var fileInput = uploadDom.find('input');
			var iframe = $('#wz-file-upload-iframe');
			if(iframe.length == 0){
				iframe = $('<iframe id="wz-file-upload-iframe" style="display:none;" name="uploadfile-iframe">').appendTo('body');
			}
			var formDom = $('<form method="post" action="'+ps.url+'" enctype="multipart/form-data" target="uploadfile-iframe"></form>').appendTo('body');
			for(var p in ps.params){
				formDom.append('<input name="'+p+'" value="'+ps.params[p]+'"/>');
			}
			$(this).hide();
			$(this).before(uploadDom);
			
			function showLoadMask(){
				uploadDom.append('<div class="image-load-mask"></div><div class="image-load-loading"></div>');
			}
			function hideLoadMask(){
				uploadDom.find('.image-load-mask,.image-load-loading').remove();
			}
			function getValue(){
				return myDom.val();
			}
			function setValue(obj){
				myDom.val(JSON.stringify(obj));
				obj = ps.formatter(obj);
				typeField.text(obj.file_type);
				sizeField.text(Math.round(obj.file_size/1024/1024 * 100)/100);
				uploadDom.find('.wz-file-upload-info').show();
			}
			function validate(){
				if(ps.required && getValue()==''){
					uploadDom.addClass('textbox-invalid');
					return false;
				}
			}
			
			function loadIframe(){
				hideLoadMask();
				uploadDom.find('.wz-file-upload-btn').append(fileInput.val(''));
				try{
					var result = this.contentDocument.body.innerHTML;
					result = result.replace(/^(<[^>]*>)+|(<[^>]*>)+$/g,'');
					result = JSON.parse(result);
					if(result.result == 'SUCC'){
						myDom.val(ps.resultFormatter(result));
						uploadDom.removeClass('textbox-invalid');
						var type = result.file_name.match(/\.(\w+)$/)[1];
						typeField.text(type);
						sizeField.text(Math.floor(result.file_size/1024/1024*100)/100);
						uploadDom.find('.wz-file-upload-info').show();
						fileInput.val('');
					}else{
						$.messager.show({
			                title:'错误',
			                msg:result.fail_text,
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
					}
				}catch(e){
					$.messager.show({
		                title:'错误',
		                msg:'文件上传失败！',
		                showType:'fade',
		                style:{
		                    right:'',
		                    top:document.body.scrollTop+document.documentElement.scrollTop,
		                    bottom:''
		                },
		                timeout: 1000
		            });
				}
			}
			fileInput.bind('change',function(){
				showLoadMask();
				if(!!this.files && this.files.length>0){
					var file = this.files[0];
					var file_type = file.type;
					var file_size = file.size;
					if(!RegExp('('+ps.supportType.join('|')+')','i').test(file_type)){
						$.messager.show({
			                title:'错误',
			                msg:'请上传'+ps.supportType.join('|')+'格式的文件！',
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
						hideLoadMask();
						return false;
					}
					if(file_size/1024/1024 > ps.maxSize){
						$.messager.show({
			                title:'错误',
			                msg:'请上传'+px.maxSize+'M以下的文件！',
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
						hideLoadMask();
						return false;
					}
				}else{
					var file_name = this.value;
					if(file_name == "") {
						hideLoadMask();
						return false;
					}
					if(!!!file_name.match(RegExp('\.('+ps.supportType.join('|')+')$','i'))){
						$.messager.show({
			                title:'错误',
			                msg:'请上传'+ps.supportType.join('|')+'格式的文件！',
			                showType:'fade',
			                style:{
			                    right:'',
			                    top:document.body.scrollTop+document.documentElement.scrollTop,
			                    bottom:''
			                },
			                timeout: 1000
			            });
						hideLoadMask();
						return false;
					}
				}
				iframe.one('load',loadIframe);
				formDom.append(fileInput);
				formDom.submit();
			});
			previewBtn.bind('click',function(){
				var url = ps.formatter(JSON.parse(getValue()||'{}')).file_url;
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
			});
			function reset(){
				myDom.val('');
				uploadDom.find('.wz-file-upload-info').hide();
			}
			
			return {
				getValue: getValue,
				setValue: setValue,
				validate: validate,
				reset: reset
			};
		}
	});
})(jQuery);


/*
 * 日历处理方法集合
 */
var calendar = {
    /**
     * 农历1900-2100的润大小信息表
     * @Array Of Property
     * @return Hex
     */
    lunarInfo:[0x04bd8,0x04ae0,0x0a570,0x054d5,0x0d260,0x0d950,0x16554,0x056a0,0x09ad0,0x055d2,//1900-1909
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
    ],


    /**
     * 公历每个月份的天数普通表
     * @Array Of Property
     * @return Number
     */
    solarMonth:[31,28,31,30,31,30,31,31,30,31,30,31],


    /**
     * 天干地支之天干速查表
     * @Array Of Property trans["甲","乙","丙","丁","戊","己","庚","辛","壬","癸"]
     * @return Cn string
     */
    Gan:["\u7532","\u4e59","\u4e19","\u4e01","\u620a","\u5df1","\u5e9a","\u8f9b","\u58ec","\u7678"],


    /**
     * 天干地支之地支速查表
     * @Array Of Property
     * @trans["子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"]
     * @return Cn string
     */
    Zhi:["\u5b50","\u4e11","\u5bc5","\u536f","\u8fb0","\u5df3","\u5348","\u672a","\u7533","\u9149","\u620c","\u4ea5"],


    /**
     * 天干地支之地支速查表<=>生肖
     * @Array Of Property
     * @trans["鼠","牛","虎","兔","龙","蛇","马","羊","猴","鸡","狗","猪"]
     * @return Cn string
     */
    Animals:["\u9f20","\u725b","\u864e","\u5154","\u9f99","\u86c7","\u9a6c","\u7f8a","\u7334","\u9e21","\u72d7","\u732a"],


    /**
     * 24节气速查表
     * @Array Of Property
     * @trans["小寒","大寒","立春","雨水","惊蛰","春分","清明","谷雨","立夏","小满","芒种","夏至","小暑","大暑","立秋","处暑","白露","秋分","寒露","霜降","立冬","小雪","大雪","冬至"]
     * @return Cn string
     */
    solarTerm:["\u5c0f\u5bd2","\u5927\u5bd2","\u7acb\u6625","\u96e8\u6c34","\u60ca\u86f0","\u6625\u5206","\u6e05\u660e","\u8c37\u96e8","\u7acb\u590f","\u5c0f\u6ee1","\u8292\u79cd","\u590f\u81f3","\u5c0f\u6691","\u5927\u6691","\u7acb\u79cb","\u5904\u6691","\u767d\u9732","\u79cb\u5206","\u5bd2\u9732","\u971c\u964d","\u7acb\u51ac","\u5c0f\u96ea","\u5927\u96ea","\u51ac\u81f3"],


    /**
     * 1900-2100各年的24节气日期速查表
     * @Array Of Property
     * @return 0x string For splice
     */
    sTermInfo:[ '9778397bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e','97bcf97c3598082c95f8c965cc920f',
        '97bd0b06bdb0722c965ce1cfcc920f','b027097bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e',
        '97bcf97c359801ec95f8c965cc920f','97bd0b06bdb0722c965ce1cfcc920f','b027097bd097c36b0b6fc9274c91aa',
        '97b6b97bd19801ec9210c965cc920e','97bcf97c359801ec95f8c965cc920f', '97bd0b06bdb0722c965ce1cfcc920f',
        'b027097bd097c36b0b6fc9274c91aa','9778397bd19801ec9210c965cc920e','97b6b97bd19801ec95f8c965cc920f',
        '97bd09801d98082c95f8e1cfcc920f','97bd097bd097c36b0b6fc9210c8dc2','9778397bd197c36c9210c9274c91aa',
        '97b6b97bd19801ec95f8c965cc920e','97bd09801d98082c95f8e1cfcc920f', '97bd097bd097c36b0b6fc9210c8dc2',
        '9778397bd097c36c9210c9274c91aa','97b6b97bd19801ec95f8c965cc920e','97bcf97c3598082c95f8e1cfcc920f',
        '97bd097bd097c36b0b6fc9210c8dc2','9778397bd097c36c9210c9274c91aa','97b6b97bd19801ec9210c965cc920e',
        '97bcf97c3598082c95f8c965cc920f','97bd097bd097c35b0b6fc920fb0722','9778397bd097c36b0b6fc9274c91aa',
        '97b6b97bd19801ec9210c965cc920e','97bcf97c3598082c95f8c965cc920f', '97bd097bd097c35b0b6fc920fb0722',
        '9778397bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e','97bcf97c359801ec95f8c965cc920f',
        '97bd097bd097c35b0b6fc920fb0722','9778397bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e',
        '97bcf97c359801ec95f8c965cc920f','97bd097bd097c35b0b6fc920fb0722','9778397bd097c36b0b6fc9274c91aa',
        '97b6b97bd19801ec9210c965cc920e','97bcf97c359801ec95f8c965cc920f', '97bd097bd07f595b0b6fc920fb0722',
        '9778397bd097c36b0b6fc9210c8dc2','9778397bd19801ec9210c9274c920e','97b6b97bd19801ec95f8c965cc920f',
        '97bd07f5307f595b0b0bc920fb0722','7f0e397bd097c36b0b6fc9210c8dc2','9778397bd097c36c9210c9274c920e',
        '97b6b97bd19801ec95f8c965cc920f','97bd07f5307f595b0b0bc920fb0722','7f0e397bd097c36b0b6fc9210c8dc2',
        '9778397bd097c36c9210c9274c91aa','97b6b97bd19801ec9210c965cc920e','97bd07f1487f595b0b0bc920fb0722',
        '7f0e397bd097c36b0b6fc9210c8dc2','9778397bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e',
        '97bcf7f1487f595b0b0bb0b6fb0722','7f0e397bd097c35b0b6fc920fb0722', '9778397bd097c36b0b6fc9274c91aa',
        '97b6b97bd19801ec9210c965cc920e','97bcf7f1487f595b0b0bb0b6fb0722','7f0e397bd097c35b0b6fc920fb0722',
        '9778397bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e','97bcf7f1487f531b0b0bb0b6fb0722',
        '7f0e397bd097c35b0b6fc920fb0722','9778397bd097c36b0b6fc9274c91aa','97b6b97bd19801ec9210c965cc920e',
        '97bcf7f1487f531b0b0bb0b6fb0722','7f0e397bd07f595b0b6fc920fb0722', '9778397bd097c36b0b6fc9274c91aa',
        '97b6b97bd19801ec9210c9274c920e','97bcf7f0e47f531b0b0bb0b6fb0722','7f0e397bd07f595b0b0bc920fb0722',
        '9778397bd097c36b0b6fc9210c91aa','97b6b97bd197c36c9210c9274c920e','97bcf7f0e47f531b0b0bb0b6fb0722',
        '7f0e397bd07f595b0b0bc920fb0722','9778397bd097c36b0b6fc9210c8dc2','9778397bd097c36c9210c9274c920e',
        '97b6b7f0e47f531b0723b0b6fb0722','7f0e37f5307f595b0b0bc920fb0722', '7f0e397bd097c36b0b6fc9210c8dc2',
        '9778397bd097c36b0b70c9274c91aa','97b6b7f0e47f531b0723b0b6fb0721','7f0e37f1487f595b0b0bb0b6fb0722',
        '7f0e397bd097c35b0b6fc9210c8dc2','9778397bd097c36b0b6fc9274c91aa','97b6b7f0e47f531b0723b0b6fb0721',
        '7f0e27f1487f595b0b0bb0b6fb0722','7f0e397bd097c35b0b6fc920fb0722', '9778397bd097c36b0b6fc9274c91aa',
        '97b6b7f0e47f531b0723b0b6fb0721','7f0e27f1487f531b0b0bb0b6fb0722','7f0e397bd097c35b0b6fc920fb0722',
        '9778397bd097c36b0b6fc9274c91aa','97b6b7f0e47f531b0723b0b6fb0721','7f0e27f1487f531b0b0bb0b6fb0722',
        '7f0e397bd097c35b0b6fc920fb0722','9778397bd097c36b0b6fc9274c91aa','97b6b7f0e47f531b0723b0b6fb0721',
        '7f0e27f1487f531b0b0bb0b6fb0722','7f0e397bd07f595b0b0bc920fb0722', '9778397bd097c36b0b6fc9274c91aa',
        '97b6b7f0e47f531b0723b0787b0721','7f0e27f0e47f531b0b0bb0b6fb0722','7f0e397bd07f595b0b0bc920fb0722',
        '9778397bd097c36b0b6fc9210c91aa','97b6b7f0e47f149b0723b0787b0721','7f0e27f0e47f531b0723b0b6fb0722',
        '7f0e397bd07f595b0b0bc920fb0722','9778397bd097c36b0b6fc9210c8dc2','977837f0e37f149b0723b0787b0721',
        '7f07e7f0e47f531b0723b0b6fb0722','7f0e37f5307f595b0b0bc920fb0722','7f0e397bd097c35b0b6fc9210c8dc2',
        '977837f0e37f14998082b0787b0721','7f07e7f0e47f531b0723b0b6fb0721','7f0e37f1487f595b0b0bb0b6fb0722',
        '7f0e397bd097c35b0b6fc9210c8dc2','977837f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721',
        '7f0e27f1487f531b0b0bb0b6fb0722','7f0e397bd097c35b0b6fc920fb0722','977837f0e37f14998082b0787b06bd',
        '7f07e7f0e47f531b0723b0b6fb0721','7f0e27f1487f531b0b0bb0b6fb0722','7f0e397bd097c35b0b6fc920fb0722',
        '977837f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721','7f0e27f1487f531b0b0bb0b6fb0722',
        '7f0e397bd07f595b0b0bc920fb0722','977837f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721',
        '7f0e27f1487f531b0b0bb0b6fb0722','7f0e397bd07f595b0b0bc920fb0722', '977837f0e37f14998082b0787b06bd',
        '7f07e7f0e47f149b0723b0787b0721','7f0e27f0e47f531b0b0bb0b6fb0722','7f0e397bd07f595b0b0bc920fb0722',
        '977837f0e37f14998082b0723b06bd','7f07e7f0e37f149b0723b0787b0721','7f0e27f0e47f531b0723b0b6fb0722',
        '7f0e397bd07f595b0b0bc920fb0722','977837f0e37f14898082b0723b02d5','7ec967f0e37f14998082b0787b0721',
        '7f07e7f0e47f531b0723b0b6fb0722','7f0e37f1487f595b0b0bb0b6fb0722','7f0e37f0e37f14898082b0723b02d5',
        '7ec967f0e37f14998082b0787b0721','7f07e7f0e47f531b0723b0b6fb0722','7f0e37f1487f531b0b0bb0b6fb0722',
        '7f0e37f0e37f14898082b0723b02d5','7ec967f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721',
        '7f0e37f1487f531b0b0bb0b6fb0722','7f0e37f0e37f14898082b072297c35','7ec967f0e37f14998082b0787b06bd',
        '7f07e7f0e47f531b0723b0b6fb0721','7f0e27f1487f531b0b0bb0b6fb0722','7f0e37f0e37f14898082b072297c35',
        '7ec967f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721', '7f0e27f1487f531b0b0bb0b6fb0722',
        '7f0e37f0e366aa89801eb072297c35','7ec967f0e37f14998082b0787b06bd','7f07e7f0e47f149b0723b0787b0721',
        '7f0e27f1487f531b0b0bb0b6fb0722','7f0e37f0e366aa89801eb072297c35','7ec967f0e37f14998082b0723b06bd',
        '7f07e7f0e47f149b0723b0787b0721','7f0e27f0e47f531b0723b0b6fb0722','7f0e37f0e366aa89801eb072297c35',
        '7ec967f0e37f14998082b0723b06bd','7f07e7f0e37f14998083b0787b0721','7f0e27f0e47f531b0723b0b6fb0722',
        '7f0e37f0e366aa89801eb072297c35','7ec967f0e37f14898082b0723b02d5','7f07e7f0e37f14998082b0787b0721',
        '7f07e7f0e47f531b0723b0b6fb0722','7f0e36665b66aa89801e9808297c35', '665f67f0e37f14898082b0723b02d5',
        '7ec967f0e37f14998082b0787b0721','7f07e7f0e47f531b0723b0b6fb0722', '7f0e36665b66a449801e9808297c35',
        '665f67f0e37f14898082b0723b02d5','7ec967f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721',
        '7f0e36665b66a449801e9808297c35','665f67f0e37f14898082b072297c35', '7ec967f0e37f14998082b0787b06bd',
        '7f07e7f0e47f531b0723b0b6fb0721','7f0e26665b66a449801e9808297c35', '665f67f0e37f1489801eb072297c35',
        '7ec967f0e37f14998082b0787b06bd','7f07e7f0e47f531b0723b0b6fb0721', '7f0e27f1487f531b0b0bb0b6fb0722'],


    /**
     * 数字转中文速查表
     * @Array Of Property
     * @trans ['日','一','二','三','四','五','六','七','八','九','十']
     * @return Cn string
     */
    nStr1:["\u65e5","\u4e00","\u4e8c","\u4e09","\u56db","\u4e94","\u516d","\u4e03","\u516b","\u4e5d","\u5341"],


    /**
     * 日期转农历称呼速查表
     * @Array Of Property
     * @trans ['初','十','廿','卅']
     * @return Cn string
     */
    nStr2:["\u521d","\u5341","\u5eff","\u5345"],


    /**
     * 月份转农历称呼速查表
     * @Array Of Property
     * @trans ['正','一','二','三','四','五','六','七','八','九','十','冬','腊']
     * @return Cn string
     */
    nStr3:["\u6b63","\u4e8c","\u4e09","\u56db","\u4e94","\u516d","\u4e03","\u516b","\u4e5d","\u5341","\u51ac","\u814a"],


    /**
     * 返回农历y年一整年的总天数
     * @param lunar Year
     * @return Number
     * @eg:var count = calendar.lYearDays(1987) ;//count=387
     */
    lYearDays:function(y) {
        var i, sum = 348;
        for(i=0x8000; i>0x8; i>>=1) { sum += (calendar.lunarInfo[y-1900] & i)? 1: 0; }
        return(sum+calendar.leapDays(y));
    },


    /**
     * 返回农历y年闰月是哪个月；若y年没有闰月 则返回0
     * @param lunar Year
     * @return Number (0-12)
     * @eg:var leapMonth = calendar.leapMonth(1987) ;//leapMonth=6
     */
    leapMonth:function(y) { //闰字编码 \u95f0
        return(calendar.lunarInfo[y-1900] & 0xf);
    },


    /**
     * 返回农历y年闰月的天数 若该年没有闰月则返回0
     * @param lunar Year
     * @return Number (0、29、30)
     * @eg:var leapMonthDay = calendar.leapDays(1987) ;//leapMonthDay=29
     */
    leapDays:function(y) {
        if(calendar.leapMonth(y)) {
            return((calendar.lunarInfo[y-1900] & 0x10000)? 30: 29);
        }
        return(0);
    },


    /**
     * 返回农历y年m月（非闰月）的总天数，计算m为闰月时的天数请使用leapDays方法
     * @param lunar Year
     * @return Number (-1、29、30)
     * @eg:var MonthDay = calendar.monthDays(1987,9) ;//MonthDay=29
     */
    monthDays:function(y,m) {
        if(m>12 || m<1) {return -1}//月份参数从1至12，参数错误返回-1
        return( (calendar.lunarInfo[y-1900] & (0x10000>>m))? 30: 29 );
    },


    /**
     * 返回公历(!)y年m月的天数
     * @param solar Year
     * @return Number (-1、28、29、30、31)
     * @eg:var solarMonthDay = calendar.leapDays(1987) ;//solarMonthDay=30
     */
    solarDays:function(y,m) {
        if(m>12 || m<1) {return -1} //若参数错误 返回-1
        var ms = m-1;
        if(ms==1) { //2月份的闰平规律测算后确认返回28或29
            return(((y%4 == 0) && (y%100 != 0) || (y%400 == 0))? 29: 28);
        }else {
            return(calendar.solarMonth[ms]);
        }
    },


    /**
     * 传入offset偏移量返回干支
     * @param offset 相对甲子的偏移量
     * @return Cn string
     */
    toGanZhi:function(offset) {
        return(calendar.Gan[offset%10]+calendar.Zhi[offset%12]);
    },


    /**
     * 传入公历(!)y年获得该年第n个节气的公历日期
     * @param y公历年(1900-2100)；n二十四节气中的第几个节气(1~24)；从n=1(小寒)算起
     * @return day Number
     * @eg:var _24 = calendar.getTerm(1987,3) ;//_24=4;意即1987年2月4日立春
     */
    getTerm:function(y,n) {
        if(y<1900 || y>2100) {return -1;}
        if(n<1 || n>24) {return -1;}
        var _table = calendar.sTermInfo[y-1900];
        var _info = [
            parseInt('0x'+_table.substr(0,5)).toString() ,
            parseInt('0x'+_table.substr(5,5)).toString(),
            parseInt('0x'+_table.substr(10,5)).toString(),
            parseInt('0x'+_table.substr(15,5)).toString(),
            parseInt('0x'+_table.substr(20,5)).toString(),
            parseInt('0x'+_table.substr(25,5)).toString()
        ];
        var _calday = [
            _info[0].substr(0,1),
            _info[0].substr(1,2),
            _info[0].substr(3,1),
            _info[0].substr(4,2),

            _info[1].substr(0,1),
            _info[1].substr(1,2),
            _info[1].substr(3,1),
            _info[1].substr(4,2),

            _info[2].substr(0,1),
            _info[2].substr(1,2),
            _info[2].substr(3,1),
            _info[2].substr(4,2),

            _info[3].substr(0,1),
            _info[3].substr(1,2),
            _info[3].substr(3,1),
            _info[3].substr(4,2),

            _info[4].substr(0,1),
            _info[4].substr(1,2),
            _info[4].substr(3,1),
            _info[4].substr(4,2),

            _info[5].substr(0,1),
            _info[5].substr(1,2),
            _info[5].substr(3,1),
            _info[5].substr(4,2)
        ];
        return parseInt(_calday[n-1]);
    },


    /**
     * 传入农历数字月份返回汉语通俗表示法
     * @param lunar month
     * @return Cn string
     * @eg:var cnMonth = calendar.toChinaMonth(12) ;//cnMonth='腊月'
     */
    toChinaMonth:function(m) { // 月 => \u6708
        if(m>12 || m<1) {return -1} //若参数错误 返回-1
        var s = calendar.nStr3[m-1];
        s+= "\u6708";//加上月字
        return s;
    },


    /**
     * 传入农历日期数字返回汉字表示法
     * @param lunar day
     * @return Cn string
     * @eg:var cnDay = calendar.toChinaDay(21) ;//cnMonth='廿一'
     */
    toChinaDay:function(d){ //日 => \u65e5
        var s;
        switch (d) {
            case 10:
                s = '\u521d\u5341'; break;
            case 20:
                s = '\u4e8c\u5341'; break;
                break;
            case 30:
                s = '\u4e09\u5341'; break;
                break;
            default :
                s = calendar.nStr2[Math.floor(d/10)];
                s += calendar.nStr1[d%10];
        }
        return(s);
    },


    /**
     * 年份转生肖[!仅能大致转换] => 精确划分生肖分界线是“立春”
     * @param y year
     * @return Cn string
     * @eg:var animal = calendar.getAnimal(1987) ;//animal='兔'
     */
    getAnimal: function(y) {
        return calendar.Animals[(y - 4) % 12]
    },


    /**
     * 传入公历年月日获得详细的公历、农历object信息 <=>JSON
     * @param y  solar year
     * @param m solar month
     * @param d  solar day
     * @return JSON object
     * @eg:console.log(calendar.solar2lunar(1987,11,01));
     */
    solar2lunar:function (y,m,d) { //参数区间1900.1.31~2100.12.31
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
        for(i=1900; i<2101 && offset>0; i++) { temp=calendar.lYearDays(i); offset-=temp; }
        if(offset<0) { offset+=temp; i--; }

        //是否今天
        var isTodayObj = new Date(),isToday=false;
        if(isTodayObj.getFullYear()==y && isTodayObj.getMonth()+1==m && isTodayObj.getDate()==d) {
            isToday = true;
        }
        //星期几
        var nWeek = objDate.getDay(),cWeek = calendar.nStr1[nWeek];
        if(nWeek==0) {nWeek =7;}//数字表示周几顺应天朝周一开始的惯例
        //农历年
        var year = i;

        var leap = calendar.leapMonth(i); //闰哪个月
        var isLeap = false;

        //效验闰月
        for(i=1; i<13 && offset>0; i++) {
            //闰月
            if(leap>0 && i==(leap+1) && isLeap==false){
                --i;
                isLeap = true; temp = calendar.leapDays(year); //计算农历闰月天数
            }
            else{
                temp = calendar.monthDays(year, i);//计算农历普通月天数
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

        //天干地支处理
        var sm = m-1;
        var term3 = calendar.getTerm(year,3); //该农历年立春日期
        var gzY = calendar.toGanZhi(year-4);//普通按年份计算，下方尚需按立春节气来修正

        //依据立春日进行修正gzY
        if(sm<2 && d<term3) {
            gzY = calendar.toGanZhi(year-5);
        }else {
            gzY = calendar.toGanZhi(year-4);
        }

        //月柱 1900年1月小寒以前为 丙子月(60进制12)
        var firstNode = calendar.getTerm(y,(m*2-1));//返回当月「节」为几日开始
        var secondNode = calendar.getTerm(y,(m*2));//返回当月「节」为几日开始

        //依据12节气修正干支月
        var gzM = calendar.toGanZhi((y-1900)*12+m+11);
        if(d>=firstNode) {
            gzM = calendar.toGanZhi((y-1900)*12+m+12);
        }

        //传入的日期的节气与否
        var isTerm = false;
        var Term = null;
        if(firstNode==d) {
            isTerm = true;
            Term = calendar.solarTerm[m*2-2];
        }
        if(secondNode==d) {
            isTerm = true;
            Term = calendar.solarTerm[m*2-1];
        }
        //日柱 当月一日与 1900/1/1 相差天数
        var dayCyclical = Date.UTC(y,sm,1,0,0,0,0)/86400000+25567+10;
        var gzD = calendar.toGanZhi(dayCyclical+d-1);

        return {'lYear':year,'lMonth':month,'lDay':day,'Animal':calendar.getAnimal(year),'IMonthCn':(isLeap?"\u95f0":'')+calendar.toChinaMonth(month),'IDayCn':calendar.toChinaDay(day),'cYear':y,'cMonth':m,'cDay':d,'gzYear':gzY,'gzMonth':gzM,'gzDay':gzD,'isToday':isToday,'isLeap':isLeap,'nWeek':nWeek,'ncWeek':"\u661f\u671f"+cWeek,'isTerm':isTerm,'Term':Term};
    },


    /**
     * 传入公历年月日以及传入的月份是否闰月获得详细的公历、农历object信息 <=>JSON
     * @param y  lunar year
     * @param m lunar month
     * @param d  lunar day
     * @param isLeapMonth  lunar month is leap or not.
     * @return JSON object
     * @eg:console.log(calendar.lunar2solar(1987,9,10));
     */
    lunar2solar:function(y,m,d,isLeapMonth) {  //参数区间1900.1.31~2100.12.1
        var leapOffset = 0;
        var leapMonth = calendar.leapMonth(y);
        var leapDay = calendar.leapDays(y);
        if(isLeapMonth&&(leapMonth!=m)) {return -1;}//传参要求计算该闰月公历 但该年得出的闰月与传参的月份并不同
        if(y==2100&&m==12&&d>1 || y==1900&&m==1&&d<31) {return -1;}//超出了最大极限值
        var day = calendar.monthDays(y,m);
        if(y<1900 || y>2100 || d>day) {return -1;}//参数合法性效验

        //计算农历的时间差
        var offset = 0;
        for(var i=1900;i<y;i++) {
            offset+=calendar.lYearDays(i);
        }
        var leap = 0,isAdd= false;
        for(var i=1;i<m;i++) {
            leap = calendar.leapMonth(y);
            if(!isAdd) {//处理闰月
                if(leap<=i && leap>0) {
                    offset+=calendar.leapDays(y);isAdd = true;
                }
            }
            offset+=calendar.monthDays(y,i);
        }
        //转换闰月农历 需补充该年闰月的前一个月的时差
        if(isLeapMonth) {offset+=day;}
        //1900年农历正月一日的公历时间为1900年1月30日0时0分0秒(该时间也是本农历的最开始起始点)
        var stmap = Date.UTC(1900,1,30,0,0,0);
        var calObj = new Date((offset+d-31)*86400000+stmap);
        var cY = calObj.getUTCFullYear();
        var cM = calObj.getUTCMonth()+1;
        var cD = calObj.getUTCDate();

        return calendar.solar2lunar(cY,cM,cD);
    },
    stamp2solar: function(time){
    	var date = new Date(time*1000);
    	return date.getUTCFullYear()+'年'+('0'+(date.getUTCMonth()+1)).slice(-2)+'月'+('0'+(date.getUTCDate()+1)).slice(-2)+'日';
    },
    stamp2lunar: function(time){
    	var date = new Date(time*1000);
    	var year = date.getUTCFullYear();
    	var month = date.getUTCMonth()+1;
    	var day = date.getUTCDate()+1;
    	var leap = calendar.leapMonth(year);
    	date = calendar.solar2lunar(year,month,day);
    	return date.gzYear+'('+year+')年'+date.IMonthCn+date.IDayCn;
    }
};


/**
 * calendarbox - jQuery EasyUI
 *
 * Dependencies:
 *   combo
 *
 */
(function($){
    var solarYearData = [],lunarYearData = [];
    var now = new Date();
    for(var i=1900;i<=now.getFullYear();i++){
        solarYearData.unshift({name: i+'年',value:i});
    }
    for(var i=1900;i<=calendar.solar2lunar(now.getFullYear(),now.getMonth(),now.getDate()).lYear;i++){
        lunarYearData.unshift({name: calendar.toGanZhi(i-4)+'('+i+')年',value:i});
    }

    /**
     * create date box
     */
    function createBox(target){
        var state = $.data(target, 'calendarbox');
        var opts = state.options;

        $(target).addClass('calendarbox-f').combo($.extend({}, opts, {
            editable: false,
            onShowPanel:function(){
                setTimeStap(this, $(this).calendarbox('getTimeStap'), $(this).calendarbox('getType'));
                opts.onShowPanel.call(this);
            }
        }));

        /**
         * if the calendar isn't created, create it.
         */
        if (!state.calendarbox){
            var panel = $(target).combo('panel').css('overflow','hidden');
            panel.panel('options').onBeforeDestroy = function(){
                var c = $(this).find('.calendarbox-shared');
                if (c.length){
                    c.insertBefore(c[0].pholder);
                }
            };
            var cc = $('<div class="calendarbox-calendar-inner"></div>').prependTo(panel);
            state.calendarbox = cc;
            var switchBtn = $('<input type="text">').appendTo(cc);
            switchBtn.switchbutton({
                checked: true,
                onText: '阳历',
                offText: '阴历',
                width: 60,
                height: 27,
                onChange: function(checked){
                    $(this).data('value',checked);
                    var year = parseInt(yearCombo.combobox('getValue')||"0");
                    var month = monthCombo.combobox('getValue')||'0';
                    var day = parseInt(dayCombo.combobox('getValue')||"0");
                    var isLeap = /^\d+_leap$/.test(month);
                    month = parseInt(month);
                    if(checked){
                        var date = calendar.lunar2solar(year,month,day,isLeap);
                        loadYear(target,checked);
                        yearCombo.combobox('setValue',date.cYear);
                        loadMonth(target,date.cYear,checked);
                        monthCombo.combobox('setValue',date.cMonth);
                        loadDay(target,date.cYear,date.cMonth,date.isLeap,checked);
                        dayCombo.combobox('setValue',date.cDay);
                    }else{
                        var date = calendar.solar2lunar(year,month,day);
                        loadYear(target,checked);
                        yearCombo.combobox('setValue',date.lYear);
                        loadMonth(target,date.lYear,checked);
                        monthCombo.combobox('setValue',date.lMonth+(date.isLeap?'_leap':0));
                        loadDay(target,date.lYear,date.lMonth,date.isLeap,checked);
                        dayCombo.combobox('setValue',date.lDay);
                    }
                    changeValue(target);
                }
            }).data('value',true);
            var cd = $('<div class="calendar-date-wrap"></div>').appendTo(cc);
            var yearCombo = $('<input type="text">').appendTo(cd);
            var monthCombo = $('<input type="text">').appendTo(cd);
            var dayCombo = $('<input type="text">').appendTo(cd);
            yearCombo.combobox({
               width: 100,
               valueField: 'value',
               textField: 'name',
                editable: false,
               data: [],
                onShowPanel: function(){
                    loadYear(target,switchBtn.data('value'));
                },
                onChange: function(){
                    changeValue(target);
                }
            });
            monthCombo.combobox({
                width: 80,
                valueField: 'value',
                textField: 'name',
                editable: false,
                data: [],
                onShowPanel: function(){
                    var year = parseInt(yearCombo.combobox('getValue')||'0');
                    loadMonth(target,year,switchBtn.data('value'));
                },
                onChange: function(){
                    changeValue(target);
                }
            });
            dayCombo.combobox({
                width: 80,
                valueField: 'value',
                textField: 'name',
                editable: false,
                data: [],
                onShowPanel: function(){
                    var year = parseInt(yearCombo.combobox('getValue')||'0');
                    var month = monthCombo.combobox('getValue'||'0');
                    var isLeap = false;
                    month = parseInt(month);
                    if(/^\d+_leap$/.test(month)){
                        isLeap = true;
                    }
                    loadDay(target,year,month,isLeap,switchBtn.data('value'));
                },
                onChange: function(){
                    changeValue(target);
                }
            });
            state.switchBtn = switchBtn;
            state.yearCombo = yearCombo;
            state.monthCombo = monthCombo;
            state.dayCombo = dayCombo;
        }

        $(target).combo('textbox').parent().addClass('calendarbox');
        $(target).calendarbox('initValue', opts.value);
    }

    function loadYear(target,type){
        var state = $(target).data('calendarbox');
        if(type){
            state.yearCombo.combobox('loadData',solarYearData);
        }else{
            state.yearCombo.combobox('loadData',lunarYearData);
        }
    }
    function loadMonth(target,year,type){
        var state = $(target).data('calendarbox');
        var monthData = [];
        for(var i= 1;i<=12;i++){
            monthData.push({name:('0'+i).slice(-2) + '月',value: i});
        }
        if(!type){
            monthData = [];
            var leap = calendar.leapMonth(year);
            for(var i=1;i<=12;i++){
                monthData.push({name: calendar.toChinaMonth(i),value:i});
                if(i==leap){
                    monthData.push({name:'润'+calendar.toChinaMonth(i),value: i+'_leap'});
                }
            }
        }
        state.monthCombo.combobox('loadData',monthData);
    }

    function loadDay(target,year,month,isleap,type){
        var state = $(target).data('calendarbox');
        var dayData = [];
        var date = calendar.solarDays(year,month);
        for(var i= 1;i<=date;i++){
            dayData.push({name:('0'+i).slice(-2) + '日',value: i});
        }
        if(!type){
            dayData = [];
            date = calendar.monthDays(year,month,isleap);
            for(var i=1;i<=date;i++){
                dayData.push({name: calendar.toChinaDay(i),value:i});
            }
        }
        state.dayCombo.combobox('loadData',dayData);
    }

    /**
     * called when user inputs some value in text box
     */
    function doQuery(target, q){
        setValue(target, q, true);
    }

    function changeValue(target){
        var state = $(target).data('calendarbox');
        var type = state.switchBtn.data('value');
        var text = '';
        var date = {
            type: type
        };

        text += state.yearCombo.combobox('getText');
        text += state.monthCombo.combobox('getText');
        text += state.dayCombo.combobox('getText');
        if(type){
            date.year = parseInt(state.yearCombo.combobox('getValue'));
            date.month = parseInt(state.monthCombo.combobox('getValue'));
            date.day = parseInt(state.dayCombo.combobox('getValue'));
        }else{
            var year = parseInt(state.yearCombo.combobox('getValue'));
            var month = state.monthCombo.combobox('getValue');
            var day = parseInt(state.dayCombo.combobox('getValue'));
            var leap = false;
            if(/^\d+_leap$/.test(month)){
                leap = true;
            }
            month = parseInt(month);
            var cd = calendar.lunar2solar(year,month,day,leap);
            date.year = cd.cYear;
            date.month = cd.cMonth;
            date.day = cd.cDay;
        }
        $(target).combo('setText',text);
        $(target).data('date',date);
    }

    function initWrap(target){
        var state = $(target).data('calendarbox');
        var date = $(target).data('date');
        if(date.type){
            state.switchBtn.switchbutton('check');
        }else{
            state.switchBtn.switchbutton('uncheck');
        }
        if(date.type){
            loadYear(target,date.type);
            state.yearCombo.combobox('setValue',date.year);
            loadMonth(target,date.year,date.type);
            state.monthCombo.combobox('setValue',date.month);
            loadDay(target,date.year,date.month,false,date.type);
            state.dayCombo.combobox('setValue',date.day);
        }else{
            var date = calendar.solar2lunar(date.year,date.month,date.day);
            var lm = calendar.leapMonth(date.year);
            var isLeap = false;
            if(lm == date.month){
                isLeap = true;
            }
            loadYear(target,date.type);
            state.yearCombo.combobox('setValue',date.lYear);
            loadMonth(target,date.lYear,date.type);
            state.monthCombo.combobox('setValue',date.lMonth+(isLeap?'_leap':0));
            loadDay(target,date.lYear,date.lMonth,date.isLeap,date.type);
            state.dayCombo.combobox('setValue',date.lDay);
        }
    }

    function setTimeStap(target, time, type){
        if(time){
            var state = $.data(target, 'calendarbox');
            var opts = state.options;
            time = new Date(time*1000);
            var date = {
                year: time.getFullYear(),
                month: time.getMonth()+1,
                day: time.getDate(),
                type: !!type
            };
            $(target).data('date',date);
            if(type){
                $(target).combo('setText',date.year+'年'+('0'+date.month).slice(-2)+'月'+('0'+date.day).slice(-2)+'日');
            }else{
                var ld = calendar.solar2lunar(date.year,date.month,date.day);
                $(target).combo('setText',ld.gzYear+'('+ld.lYear+')年'+ld.IMonthCn+ld.IDayCn);
            }
        }else{
            var date = new Date();
            $(target).data('date',{
                year: date.getFullYear(),
                month: date.getMonth()+1,
                day: date.getDate(),
                type: true
            });
        }

        initWrap(target);
    }

    function getTimeStap(target){
        var date = $(target).data('date')||'';
        return !!date?new Date(date.year,date.month-1,date.day).getTime()/1000:'';
    }

    function getType(target){
        var date = $(target).data('date')||'';
        return !!date?date.type:true;
    }

    $.fn.calendarbox = function(options, param){
        if (typeof options == 'string'){
            var method = $.fn.calendarbox.methods[options];
            if (method){
                return method(this, param);
            } else {
                return this.combo(options, param);
            }
        }

        options = options || {};
        return this.each(function(){
            var state = $.data(this, 'calendarbox');
            if (state){
                $.extend(state.options, options);
            } else {
                $.data(this, 'calendarbox', {
                    options: $.extend({}, $.fn.calendarbox.defaults, $.fn.calendarbox.parseOptions(this), options)
                });
            }
            createBox(this);
        });
    };

    $.fn.calendarbox.methods = {
        options: function(jq){
            var copts = jq.combo('options');
            return $.extend($.data(jq[0], 'calendarbox').options, {
                width: copts.width,
                height: copts.height,
                originalValue: copts.originalValue,
                disabled: copts.disabled,
                readonly: copts.readonly
            });
        },
        cloneFrom: function(jq, from){
            return jq.each(function(){
                $(this).combo('cloneFrom', from);
                $.data(this, 'calendarbox', {
                    options: $.extend(true, {}, $(from).calendarbox('options'))
                });
                $(this).addClass('calendarbox-f');
            });
        },
        initValue: function(jq, value){
            return jq.each(function(){
                var opts = $(this).calendarbox('options');
                var value = opts.value;
                if (value){
                    value = opts.formatter.call(this, opts.parser.call(this, value));
                }
                $(this).combo('initValue', value).combo('setText', value);
            });
        },
        setTimeStap: function(jq, time, type){
            return jq.each(function(){
                setTimeStap(this, time, type);
            });
        },
        setValue: function(jq,value){
        	return jq.each(function(){
                setTimeStap(this, value, true);
            });
        },
        getTimeStap: function(jq){
            return getTimeStap(jq[0]);
        },
        getType: function(jq){
            return getType(jq[0]);
        },
        reset: function(jq){
            return jq.each(function(){
                $(this).data('date',null);
                $(this).combo('setValue','');
                $(this).combo('setText','');
            });
        }
    };

    $.fn.calendarbox.parseOptions = function(target){
        return $.extend({}, $.fn.combo.parseOptions(target), $.parser.parseOptions(target, ['sharedCalendar']));
    };

    $.fn.calendarbox.defaults = $.extend({}, $.fn.combo.defaults, {
        panelWidth:280,
        panelHeight:'auto',
        sharedCalendar:null,
        formatter:function(date){
            var y = date.getFullYear();
            var m = date.getMonth()+1;
            var d = date.getDate();
            return (m<10?('0'+m):m)+'/'+(d<10?('0'+d):d)+'/'+y;
        },
        parser:function(s){
            if (!s) return new Date();
            var ss = s.split('/');
            var m = parseInt(ss[0],10);
            var d = parseInt(ss[1],10);
            var y = parseInt(ss[2],10);
            if (!isNaN(y) && !isNaN(m) && !isNaN(d)){
                return new Date(y,m-1,d);
            } else {
                return new Date();
            }
        },

        onChange:function(date){}
    });
})(jQuery);