<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<title>SMART Connect - Queries</title>

<script>
	
</script>

</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader">Queries</div>
<p class="infomessage"></p>

<div>
  <div id="message" class="msgsection"></div>
  <div id="error" class="errorsection"></div>
</div>
<div class="top-spacer"  style="margin-left: -20px" >
  <div class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell">UUID</div>
		<div class="table-cell smart-table-cell">Name</div>
		<div class="table-cell smart-table-cell">ID</div>
		<div class="table-cell smart-table-cell">Type</div>
		<div class="table-cell smart-table-cell">Conservation Area</div>
		<div class="table-cell smart-table-cell"></div>
	</div>
	<c:forEach var="query" items="${allqueries}" varStatus="count">
		<div data-quuid ="${query.getUuid()}" data-qtype="${query.getType()}" class="queryrow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
			<div class="table-cell smart-table-cell">${query.getUuid()}</div>
			<div class="table-cell smart-table-cell">${query.getName()}</div>
			<div class="table-cell smart-table-cell">${query.getId()}</div>
			<div class="table-cell smart-table-cell">${query.getType()}</div>
			<div class="table-cell smart-table-cell">${query.getConservationArea()}</div>
			<div class="table-cell smart-table-cell"><a href="../api/query/${query.getUuid()}?format=csv&date_filter=waypointdate">csv</a></div>
		</div>
	</c:forEach>
	</div>  
</div>

</div>

<%@include file="footer.jsp" %>

</body>
</html>