<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
	String bossId = null;
	///C=CN/ST=Beijing/L=Beijing/O=weizhu/OU=weizhu/CN=francislin/emailAddress=francislin@wehelpu.cn
	String sslClientSubjectDN = request.getHeader("X-SSL-Client-Subject-DN");
	if (sslClientSubjectDN != null) {
		String[] fields = sslClientSubjectDN.split("/");
		for (String field : fields) {
			if (field.startsWith("CN=")) {
				bossId = field.substring("CN=".length());
			}
		}
	}
	
	String redirectUrl = request.getParameter("redirect_url");
	if (redirectUrl == null) {
		redirectUrl = "index.html";
	}
%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="renderer" content="webkit">
<title>微助Boss管理平台——登录</title>
<link rel="stylesheet" type="text/css" href="static/jquery-easyui/themes/default/easyui.css">
<link rel="stylesheet" type="text/css" href="static/jquery-easyui/themes/icon.css">
<link href="static/css/style.css" rel="stylesheet" type="text/css" />
<script type="text/javascript" src="static/jquery-easyui/jquery.min.js"></script>
<script type="text/javascript" src="static/jquery-easyui/jquery.easyui.min.js"></script>
<script type="text/javascript" src="static/jquery-easyui/locale/easyui-lang-zh_CN.js"></script>
<script type="text/javascript" src="static/js/main.js"></script>
</head>
<body style="background-color:#df7611; background-image:url(images/light.png); background-repeat:no-repeat; background-position:center top; overflow:hidden;">
    <div id="mainBody">
      <div id="cloud1" class="cloud"></div>
      <div id="cloud2" class="cloud"></div>
    </div>  
	<div class="logintop">    
	    <span>欢迎登陆微助Boss管理平台</span>    
	    <ul>
		    <li><a href="#">帮助</a></li>
		    <li><a href="#">关于</a></li>
	    </ul>    
    </div>    
    <div class="loginbody">    
	    <span class="systemlogo"></span> 	       
	    <div class="loginbox loginbox3">	    
		    <form id="login-form" class="easyui-form" method="post" data-options="novalidate:true">
		        <ul>
		            <li><input name="boss_id" class="easyui-textbox easyui-validatebox" style="width:340px;height:48px;padding:12px" data-options="prompt:'请输入账号',iconCls:'icon-man',iconWidth:38,required:true"></li>
		            <li><input name="boss_password" class="easyui-textbox easyui-validatebox" type="password" style="width:340px;height:48px;padding:12px" data-options="prompt:'请输入密码',iconCls:'icon-lock',iconWidth:38,required:true"></li>
		            <li class="login-error">错误信息</li>
		            <li><input type="submit" class="loginbtn" value="登录" /></li>
		        </ul>
		    </form>   	    
	    </div>
	</div>       
    <div class="loginbm">Copyright © 2015 www.wehelpu.cn All Rights Reserved.</div>
	<script type="text/javascript">
		$(function(){
		    var loginForm = $('#login-form');
		    loginForm.form({
    			url: './api/login.json',
                onSubmit: function(){
                    if(!!$(this).form('enableValidation').form('validate')){
                    	return true;
                    }
                    return false;
                },
                success: function(data){
                	var data = eval('('+data+')');
                	if(data.result == 'SUCC'){
                		window.location.href='<%=redirectUrl%>'; 
                	}else if(data.result == 'FAIL_ADMIN_FORCE_RESET_PASSWORD'){
                		$('.login-error').html('该管理员需要 <a href="javascript:void(0)" class="reset-password">重置密码</a> 后再登陆').show();
                	}else{
                		$('.login-error').text(data.fail_text).show();
                	}
                	return false;
                }
            });
	    });
	</script>
</body>
</html>