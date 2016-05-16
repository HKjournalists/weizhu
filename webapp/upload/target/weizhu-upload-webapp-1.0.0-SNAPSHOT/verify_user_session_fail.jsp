<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<%
	com.weizhu.proto.SessionProtos.VerifySessionKeyResponse verifyResponse = (com.weizhu.proto.SessionProtos.VerifySessionKeyResponse) request.getAttribute(com.weizhu.web.filter.UserSessionFilter.REQUEST_ATTR_USER_VERIFY_SESSION_RESPONSE);
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