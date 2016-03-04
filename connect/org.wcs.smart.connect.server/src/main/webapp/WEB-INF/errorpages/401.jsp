<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="../includes.jsp" %>
	<title><fmt:message key="401.pagetitle"/></title>
</head>

<body style="${style_bodycss}">

<div id="mainheader" <c:if test="${not empty style_headercss}"> style="${style_headercss}" </c:if>  >
<c:out value="${empty style_headername ? 'SMART Connect' : style_headername}"/>
</div>

<div style="display:block; padding:20px">
<div class="pageheader"><fmt:message key="401.title"/></div>
<p><fmt:message key="401.message"/></p>
<p style="padding-top:10px"><a href="${pageContext.request.contextPath}/connect/home">Connect Home</a></p>
</div>
</body>
</html>