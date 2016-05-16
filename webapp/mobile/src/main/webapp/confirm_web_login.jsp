<%@ page import="java.net.URLEncoder" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	String token = com.weizhu.web.ParamUtil.getString(request, "token", "");
%>
<html>
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=0">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-status-bar-style" content="black">
<meta name="format-detection" content="telephone=no">
<title>登录</title>
<style>
	body{padding: 0px;margin: 0px;font-family: arial, tahoma, 'Microsoft Yahei', '\5b8b\4f53', sans-serif;}
	.main{position: absolute;width: 100%;height: 100%;top: 0px;left: 0px;text-align: center;}
	.login-icon{text-align: center;margin: 25% auto;}
	.login-tip{font-size: 16px;color: #666666;height: 30px;line-height: 30px;}
	.login-btn{display:inline-block;padding: 10px 60px;background-color: #2095f2;color: #fff;font-size: 16px;text-decoration: none;font-weight: bold;-webkit-border-radius: 5px;border-radius: 5px;}
	.login-error{display:none;color:#e92626;height: 24px;line-height:24px;}
	.login-succ{display:none;color:#4EAF58;height: 24px;line-height:24px;}
	.show{display:block;}
</style>
</head>
<body>
	<div class="main">
		<div class="login-icon">
			<img src="./static/images/web-login.png" />
			<div class="login-tip">电脑登录确认</div>
			<div class="login-error">登录确认已失效，请重新扫码登录</div>
			<div class="login-succ">您已经在电脑浏览器中成功登录</div>
		</div>
		<a href="javascript:void(0)" class="login-btn">登&nbsp;录</a>
	</div>
	<script>
		!function($d){
			var errTip = $d.querySelector('.login-error');
			var succTip = $d.querySelector('.login-succ');
			var loginBtn = $d.querySelector('.login-btn');
			var url = 'api/login/notify_web_login_by_token.json?token=<%=URLEncoder.encode(token, "UTF-8")%>';
			loginBtn.addEventListener('click',function(){
				var xmlhttp = new XMLHttpRequest();
				xmlhttp.onreadystatechange = function(){
					if (xmlhttp.readyState==4){
					 	if (xmlhttp.status==200){
							var data = JSON.parse(xmlhttp.responseText);
							if(data.result == 'SUCC'){
								errTip.classList.remove('show');
								loginBtn.style.display = 'none';
								succTip.classList.add('show');
							}else{
								loginBtn.style.display = 'none';
								succTip.classList.remove('show');
								errTip.innerHTML = data.fail_text;
								errTip.classList.add('show');
							}
						}else{
							loginBtn.style.display = 'none';
							succTip.classList.remove('show');
							errTip.innerHtml = '登录失败，请重新扫码登录';
							errTip.classList.add('show');
					    }
					}
				};
				xmlhttp.open("GET",url,true);
				xmlhttp.send();
			});		
		}(document);
	</script>
</body>
</html>