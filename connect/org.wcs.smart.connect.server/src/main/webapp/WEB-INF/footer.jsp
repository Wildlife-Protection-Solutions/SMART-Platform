<%@page import="java.time.LocalDate"%>
<%@page import="java.time.format.DateTimeFormatter"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>


</div> <!-- end core -->
<% String year = DateTimeFormatter.ofPattern("YYYY").format(LocalDate.now()); %>

<div id="footerid">
	<c:if test="${empty style_footername }">
  		<img class="float " src="${pageContext.request.contextPath}/css/images/smart_logo.png">
  		<p class="float"> Copyright 2015-<%=year%></p>
	</c:if>
	<c:if test="${not empty style_footername }">
  		${style_footername}
	</c:if>
</div><!-- end footerid -->
</div><!-- end root-container -->

