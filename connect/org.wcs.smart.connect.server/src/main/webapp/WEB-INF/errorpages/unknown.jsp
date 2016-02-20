<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="../includes.jsp" %>
	<title><fmt:message key="unknownerror.pagetitle"/></title>
</head>

<body>
<div id="mainheader">SMART Connect</div>
<div style="display:block; padding:20px">
<div class="pageheader"><fmt:message key="unknownerror.title"/></div>
<p><fmt:message key="unknownerror.message"/></p>
<p style="padding-top:10px"><a href="${pageContext.request.contextPath}/connect/home">Connect Home</a></p>
</div>
</body>
</html>