<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>


</div> <!--  end core div -->

<div id="footerid" class="float">
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


	<c:if test="${empty style_footername }">
  		<img class="float " src="${pageContext.request.contextPath}/css/images/smart_logo.png">
  		<p class="float"> Copyright 2015-2017</p>
	</c:if>
	<c:if test="${not empty style_footername }">
  		${style_footername}
	</c:if>
</div>

