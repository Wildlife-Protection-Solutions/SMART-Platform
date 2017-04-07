<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<c:if test="${not empty pageContext.request.userPrincipal}">
	<div id="userheader">
		<form action="${pageContext.request.contextPath}/logout" method="post">
		<span id="userlogin" data-username="${pageContext.request.userPrincipal.name}">
			${pageContext.request.userPrincipal.name}
		</span> |
		<input type="submit" value="<fmt:message key="header.logout"/>" class="linkButton"/>
		</form>
	</div>
</c:if>
<div id="mainheader" style="${style_headercss}">
<c:choose>
 <c:when test="${empty style_headername}"> 
  SMART Connect
 </c:when>
 <c:otherwise>
	${style_headername}
 </c:otherwise>
</c:choose> 

</div>

<div id="core">

