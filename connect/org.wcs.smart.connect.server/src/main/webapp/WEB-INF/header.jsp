<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div id="mainheader"><font class="smart-text">SMART </font> Connect</div>
<c:if test="${not empty pageContext.request.userPrincipal}">
	<div id="userheader">
	<form action="${pageContext.request.contextPath}/logout" method="post">
	<span id="userlogin" data-username="${pageContext.request.userPrincipal.name}">${pageContext.request.userPrincipal.name}</span> |<input type="submit" value="Logout" class="linkButton"/>
	</form>
	</div>
</c:if>	

<div id="core">

