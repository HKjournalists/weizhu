Wz.namespace('Wz.credits');
Wz.credits.ruleManage = function(){
	$.parser.parse('#main-contain');
	freshRuleInfo();
	
	var ruleForm = $('#credits-rule-edit-form');
	var editBtn = ruleForm.find('.edit-btn');
	var saveBtn = ruleForm.find('.save-btn');
	var cancelBtn = ruleForm.find('.cancel-btn');
	var textArea = ruleForm.find('input[textboxname=credits_rule]');
	
	editBtn.click(function(){
		textArea.textbox('enable');
		editBtn.hide();
		saveBtn.show();
		cancelBtn.show();
	});
	saveBtn.click(function(){
		ruleForm.form('submit',{
			url: './api/credits/update_credits_rule.json',
			onSubmit: function(){
    			var valid = $(this).form('validate');
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
    				textArea.textbox('disable');
    				saveBtn.hide();
    				cancelBtn.hide();
    				editBtn.show();
    			}else{
    				$.messager.alert('错误',result.fail_text,'error');
    			}
    		}
		})
	});
	cancelBtn.click(function(){
		textArea.textbox('disable');
		freshRuleInfo();
		saveBtn.hide();
		cancelBtn.hide();
		editBtn.show();
	});
	function freshRuleInfo(){
		Wz.ajax({
			url: './api/credits/get_credits_rule.json',
			success: function(json){
				textArea.textbox('setValue',json.credits_rule||'');
			}
		});
	}
}()