<%@ page import="java.util.Base64" %>
<%@ page import="java.util.UUID" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	UUID uuid = UUID.randomUUID();
	String token = Base64.getUrlEncoder().encodeToString(com.google.common.primitives.Bytes.concat(
			com.google.common.primitives.Longs.toByteArray(uuid.getLeastSignificantBits()), 
			com.google.common.primitives.Longs.toByteArray(uuid.getMostSignificantBits())
			));
%>
<html>
<head>
<meta charset="UTF-8">
<title>扫二维码登录</title>
</head>
<body>
<img alt="二维码" src="api/qr_code.jpg?content=<%=URLEncoder.encode("http://" + request.getServerName() + "/mobile/confirm_web_login.jsp?token=" + URLEncoder.encode(token, "UTF-8"), "UTF-8")%>&size=430&">
<a href="api/login/web_login_by_token.json?token=<%=URLEncoder.encode(token, "UTF-8")%>">点此等待登录结果</a>
</body>
</html>