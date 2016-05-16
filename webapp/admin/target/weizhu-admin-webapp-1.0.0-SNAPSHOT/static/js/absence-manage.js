Wz.afterCreate['absence-manage'] = function(){
    var queryData = {
        user_name: '',
        start_time: '',
        end_time: '',
        action: ''
    };
    
    $('#search_start_time').datetimepicker({
		format: 'yyyy-MM-dd hh:mm:ss',
        language: 'pt-BR'
    }).on('changeDate',function(ev){
    	searchEndTime.setStartDate(ev.localDate);
    });
	var searchStartTime = $('#search_start_time').data('datetimepicker');
	$('#search_end_time').datetimepicker({
		format: 'yyyy-MM-dd hh:mm:ss',
        language: 'pt-BR',
        startDate:  new Date()
    }).on('changeDate',function(ev){
    	searchStartTime.setEndDate(ev.localDate);
    });
	var searchEndTime = $('#search_end_time').data('datetimepicker');

    var searchAction = new Wz.ui.Combobox({
        id: "search_action",
        type: "list",
        remote: false,
        data: [{name:"全部",value:""},{name:"请假中",value:"LEAVE"},{name:"已销假",value:"COME_BACK"}]
    }).setValue({name:"全部",value:""});
    var absenceType = new Wz.ui.Combobox({
        id: "absence_type",
        type: "list",
        remote: false,
        data: [{name:"事假",value:"事假"},{name:"病假",value:"病假"},{name:"年假",value:"年假"},{name:"婚假",value:"婚假"},{name:"产假",value:"产假"},{name:"丧假",value:"丧假"}]
    }).setValue({name:"事假",value:"事假"});
    
    $('#start_time').datetimepicker({
		format: 'yyyy-MM-dd hh:mm:ss',
        language: 'pt-BR'
    }).on('changeDate',function(ev){
    	preEndTime.setStartDate(ev.localDate);
    	facEndTime.setStartDate(ev.localDate);
    });
	var startTime = $('#start_time').data('datetimepicker');
	$('#pre_end_time').datetimepicker({
		format: 'yyyy-MM-dd hh:mm:ss',
        language: 'pt-BR',
        startDate:  new Date()
    }).on('changeDate',function(ev){
    	startTime.setEndDate(ev.localDate);
    });
	var preEndTime = $('#pre_end_time').data('datetimepicker');
	$('#fac_end_time').datetimepicker({
		format: 'yyyy-MM-dd hh:mm:ss',
        language: 'pt-BR',
        startDate:  new Date()
    }).on('changeDate',function(ev){
    	startTime.setEndDate(ev.localDate);
    });
	var facEndTime = $('#fac_end_time').data('datetimepicker');
	$('input[name=days]').bind('input',function(){
		this.value = parseInt(this.value.replace(/[^\d]/g,''))||'';
	});
    $('#credit-edit-win').on('input','input[name=credits_delta]',function(){
    	this.value = parseInt(this.value.replace(/[^\d]/g,''))||'0';
    	var edit_type = editType.getValue();
    	if(edit_type == '-'){
    		var cur = parseInt($('#credit-edit-win .total-credit').attr('credit'))||0;
    		cur = cur - (parseInt(this.value)||0);
    		$('#credit-edit-win .total-credit').text(cur);
    		if(cur<0){
    			$('#credit-edit-win .total-credit').addClass('error');
    		}
    	}
    });
    var table = $('#absence-table').dataTable({
        "sAjaxSource": './api/absence/get_absence_list.json',
        aoColumns: [{
        	mDataProp:"user_name"
        },{
            mDataProp: 'user_team',
            mRender: function(o){
                if(!!o){
                    var team = [];
                    for(var i=0;i<o.length;i++){
                        team.push(o[i].team_name);
                    }
                    return team.join("/");
                }
                return "";
            }
        },{
        	mDataProp:"mobile_no",
        	mRender: function(o){
        		return o.split(',').join('<br/>');
        	}
        },{
        	mDataProp:"type"
        },{
        	mDataProp:"start_time",
        	mRender: function(o){
        		return !!o?Wz.util.formatTime(o*1000):'';
        	}
        },{
        	mDataProp:"pre_end_time",
        	mRender: function(o){
        		return !!o?Wz.util.formatTime(o*1000):'';
        	}
        },{
        	mDataProp: "fac_end_time",
        	mRender: function(o){
        		return !!o?Wz.util.formatTime(o*1000):'';
        	}
        },{
        	mDataProp: "days",
        	mRender: function(o){
        		return !!o?(o+'天'):'';
        	}
        },{
        	mDataProp:"create_time"
        },{
        	mDataProp: 'absence_id',
        	mRender: function(o){
        		return '<a class="mr10 edit_btn" href="javascript:void(0);">修改</a>';
        	}
        }],
        'bProcessing': true,
        'bServerSide': true,
        'bAutoWidth': false,
        'bFilter': false,
        'bLengthChange': false,
        'iDisplayLength': 20,
        'ordering' : false,
        'info' : true,
        'fnServerData': function(url,data,callbackFun){
            Wz.util.ajax({
                type: "get",
                url: url,
                data: {
                    _t: new Date().getTime(),
                    user_name: queryData.user_name,
                    start_time: queryData.start_time,
                    end_time: queryData.end_time,
                    action: queryData.action,
                    start: data.iDisplayStart,
                    length: 20
                },
                dataType: "json",
                success: function(json){
                    callbackFun({
                        iTotalRecords: json.total,
                        iTotalDisplayRecords: json.filtered_size,
                        aaData: json.absence_list
                    });
                }
            })
        },
        'oLanguage' : {
            'sLengthMenu' : '每页显示 _MENU_ 条记录',
            'sZeroRecord' : '抱歉，没有找到',
            'sInfo' : '从_START_到_END_/共_TOTAL_条数据',
            'sInfoEmpty' : '没有数据',
            'sInfoFiltered' : '(从_MAX_条数据中检索)',
            'oPaginate' : {
                'sFirst' : '首页',
                'sPrevious' : '前一页',
                'sNext' : '后一页',
                'sLast' : '尾页'
            },
            'sZeroRecords' : '没有检索到数据'
        },
        'dom' : 'it<"bottom"flp>'
    });
    var tableData = $('#absence-table').DataTable();
    
    $("#dt-sch-btn").click(function(){
        queryData.user_name = $("#search_user_name").val();
        queryData.start_time = (new Date($('#search_start_time input[name=search_start_time]').val()||0).getTime()/1000)||'';
        queryData.end_time = (new Date($('#search_end_time input[name=search_end_time]').val()||0).getTime()/1000)||'';
        queryData.action = searchAction.getValue();
        table._fnAjaxUpdate();
    });
    
    table.on('click','.edit_btn',function(){
    	var modal = $('#absence-edit-win');
    	var absence = tableData.row($(this).parents('tr')).data();
    	modal.find('input[name=absence_id]').val(absence.absence_id);
    	absenceType.setValue({name:absence.type,value:absence.type})
    	modal.find('.absence-user-name').text(absence.user_name);
    	modal.find('.absence-team').text(function(team){
    		var result = [];
    		for(var i=0;i<team.length;i++){
    			result.push(team[i].team_name);
    		}
    		return result.join('-');
    	}(absence.user_team));
    	modal.find('.absence-mobile-no').text(absence.mobile_no);
    	modal.find('input[name=start_time]').val((!!absence.start_time?Wz.util.formatTime(absence.start_time*1000):''));
    	modal.find('input[name=pre_end_time]').val((!!absence.pre_end_time?Wz.util.formatTime(absence.pre_end_time*1000):''));
    	modal.find('input[name=fac_end_time]').val((!!absence.fac_end_time?Wz.util.formatTime(absence.fac_end_time*1000):''));
    	modal.find('input[name=days]').val(absence.days);
    	modal.find('textarea[name=desc]').val(absence.desc);
    	modal.modal('show');
    });
    
    $('#absence-edit-win').on('click','#btn-save-edit',function(){
    	var modal = $('#absence-edit-win'); 
    	var absence_id = modal.find('input[name=absence_id]').val();
    	var type = absenceType.getValue();
    	var start_time = (new Date(modal.find('input[name=start_time]').val()||0).getTime()/1000)||'';
    	var pre_end_time = (new Date(modal.find('input[name=pre_end_time]').val()||0).getTime()/1000)||'';
    	var fac_end_time = (new Date(modal.find('input[name=fac_end_time]').val()||0).getTime()/1000)||'';
    	var days = modal.find('input[name=days]').val();
    	var desc = modal.find('textarea[name=desc]').val();
    	
    	if(type == ''){
    		Wz.util.showMsg({msg: '请假类型不能为空！',success: false});
    		return false;
    	}
    	if(start_time == ''){
    		Wz.util.showMsg({msg: '开始时间！',success: false});
    		return false;
    	}
    	if(pre_end_time == ''){
    		Wz.util.showMsg({msg: '结束时间！',success: false});
    		return false;
    	}
    	if(fac_end_time == ''){
    		Wz.util.showMsg({msg: '销假时间！',success: false});
    		return false;
    	}
    	if(start_time > pre_end_time || start_time > fac_end_time){
    		Wz.util.showMsg({msg: '开始时间不能大于结束时间或销假时间！',success: false});
    		return false;
    	}
    	if(days == ''){
    		Wz.util.showMsg({msg: '请假天数不能为空！',success: false});
    		return false;
    	}
    	Wz.util.ajax({
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
    			if(json.result='SUCC'){
    				modal.modal('hide');
    				table._fnAjaxUpdate();
    			}else{
    				Wz.util.showMsg({
						msg: json.fail_text,
						success: false
					});
    			}
    		}
    	});
    });
    
    $("#export_absence").click(function(){
    	var btn = $(this);
    	btn.attr("disabled",true);
    	setTimeout(function(){
    		btn.attr("disabled",false);
    	},10000)
    	var params = [];
    	for(var p in queryData){
    		params.push(p + '=' + queryData[p]);
    	}
    	params.push('start=0&length=10000');
        var iframe = $("#download-iframe");
		if(iframe.length == 0){
		   iframe = $('<iframe src="" style="display:none;" id="download-iframe"></iframe>');
		   $('body').append(iframe);
	    }
	    iframe.attr("src","./api/absence/download_absence.json?"+params.join('&')+"&_t=" + new Date().getTime());
    });
}