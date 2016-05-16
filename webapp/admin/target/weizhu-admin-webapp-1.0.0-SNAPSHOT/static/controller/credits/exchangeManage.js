Wz.namespace('Wz.credits');
Wz.credits.exchangeManage = function(){
	$.parser.parse('#main-contain');
	freshCredits();
	var total_credits = 0;
	
	var exchangeTable = $('#credits-exchange-table').datagrid({
        url: './api/credits/get_credits_order.json',
        queryParams: {is_expense: true},
        fitColumns: true,
        checkOnSelect: false,
        striped: true,
        fit: true,
        pagination: true,
        rownumbers: true,
        pageSize: 20,
        columns: [[{
            field: 'user_name',
            title: '兑换人',
            width: '10%',
            align: 'left'
        },{
            field: 'mobile_no',
            title: '手机号码',
            width: '12%',
            align: 'left'
        },{
            field: 'create_time',
            title: '兑换时间',
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
            field: 'desc',
            title: '兑换物品',
            width: '30%',
            align: 'left'
        },{
            field: 'credits_delta',
            title: '消耗积分',
            width: '10%',
            align: 'right'
        },{
            field: 'state',
            title: '兑换状态',
            width: '10%',
            align: 'center',
            formatter: function(val,obj,row){
                return val=='SUCCESS'?'兑换成功':(val=='CONFIRM'?'兑换中':'兑换失败');
            }
        }]],
        toolbar: [{
            id: 'credits-exchange-add',
            text: '发积分',
            disabled: !Wz.getPermission('company/position/create'),
            iconCls: 'icon-add',
            handler: function(){
            	editForm.form('reset');
            	editForm.find('input[textboxname=credits_total]').numberbox('setValue',total_credits);
            	exchangeUserTable.datagrid('loadData',[]);
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
                rows: data.credits_order
            };
        }
    });
	
	function freshCredits(){
		Wz.ajax({
			url: './api/credits/get_expense_credits.json',
			success: function(json){
				total_credits = Math.abs(json.credits || 0);
				$('.credits-exchange-info-wrap em').text(total_credits);
			}
		});
	}
}()