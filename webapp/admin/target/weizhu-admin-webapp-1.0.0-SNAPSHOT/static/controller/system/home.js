/**
 * Created by Administrator on 15-12-16.
 */
Wz.namespace('Wz.system');
Wz.system.home = function(){
	var mainWrap = $('#system-home-wrap');
	var modifyBtn = $('#home-modify-password-btn');
	var passwordEditWin = $('#system-home-password-edit');
	var passwordEditForm = $('#system-home-password-edit-form');
	$('#system-home-portal').portal({
		fit: true,
		border: false
	});
	
	modifyBtn.click(function(){
		passwordEditForm.form('reset');
		passwordEditForm.form('load',Wz.admin_info);
		passwordEditWin.window('open');
	});
	
	function savePwdEdit(){
		passwordEditForm.form('submit',{
    		url: './api/admin_reset_password.json',
    		onSubmit: function(){
    			return $(this).form('validate');
    		},
    		dataType: 'json',
    		success: function(result){
    			result = $.parseJSON(result);
    			if(result.result == 'SUCC'){
    				Wz.showMsg('提示','密码修改成功！');	
    				passwordEditWin.window('close');
    			}else{
    				$.messager.alert('错误',json.fail_text,'error');
    				return false;
    			}
    		}
    	});
	}
	
    return {
        savePwdEdit: savePwdEdit
    };
}()
