<%@ page import="java.net.URLEncoder" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	String token = com.weizhu.web.ParamUtil.getString(request, "token", "");
%>
<html>
<head>
<meta charset="UTF-8">
<title>确认web登录</title>
</head>
<body>
<a href="api/login/get_web_login_by_token.json?token=<%=URLEncoder.encode(token, "UTF-8")%>">点此查看web登录信息</a><br/>
<a href="api/login/notify_web_login_by_token.json?token=<%=URLEncoder.encode(token, "UTF-8")%>">点此确认web登录</a><br/>
</body>
</html>