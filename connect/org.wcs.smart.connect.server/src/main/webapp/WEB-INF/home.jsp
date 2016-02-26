<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<title><fmt:message key="home.pagetitle"/></title>
	
</head>

<body onload="setStyle(true)">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id= "main"><div class="pageheader"><fmt:message key="home.welcome"/></div>
<!-- <iframe style="width:100%; height:600px;" src="https://smartconservationsoftware.org/connecthome"></iframe>-->
	<h2>
		<a href="http://smartconservationsoftware.org/connecthome"><fmt:message key="home.news"/></a>
	</h2>
	</div>
<%@include file="footer.jsp" %>
</body>
</html>