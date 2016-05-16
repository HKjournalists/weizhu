<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyResponse verifyResponse = (com.weizhu.proto.SessionProtos.VerifyWebLoginSessionKeyResponse) request.getAttribute(com.weizhu.web.filter.UserWebLoginSessionFilter.REQUEST_ATTR_USER_VERIFY_WEB_LOGIN_SESSION_RESPONSE);
%>
<html>
<head>
<meta charset="UTF-8">
<title>身份验证失败</title>
</head>
<body>
失败原因: <%=verifyResponse == null ? "未知" : verifyResponse.getFailText() %>
</body>
</html>