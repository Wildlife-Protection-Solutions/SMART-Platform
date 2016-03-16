<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="../includes.jsp" %>
	<title><fmt:message key="403error.title"/></title>
</head>

<%-- <c:redirect url="${pageContext.request.contextPath}/../logout"/> --%>


<body style="${style_bodycss}">
<div id="mainheader" <c:if test="${not empty style_headercss}"> style="${style_headercss}" </c:if>  >
<c:out value="${empty style_headername ? 'SMART Connect' : style_headername}"/>
</div>

<div style="display:block; padding:20px">
<div class="pageheader"><fmt:message key="403error.title"/></div>
<form action="${pageContext.request.contextPath}/logout" method="post">
	<span id="userlogin" data-username="${pageContext.request.userPrincipal.name}">
		${pageContext.request.userPrincipal.name}
	</span> |
	<input type="submit" value="<fmt:message key="header.logout"/>" class="linkButton"/>
</form>
</div>
</body>
</html>