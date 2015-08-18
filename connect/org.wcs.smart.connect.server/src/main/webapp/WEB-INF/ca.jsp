<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/ca.js"></script>
<title>SMART Connect - Conservation Areas</title>

<script>
	
</script>

</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader">Conservation Areas</div>
<p class="infomessage">Lists all conservation areas currently active in SMART Connect</p>

<div>
  <div id="message" class="msgsection"></div>
  <div id="error" class="errorsection"></div>
</div>
<div class="top-spacer"  style="margin-left: -20px" >
  <div class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell">Label</div>
		<div class="table-cell smart-table-cell">UUID</div>
		<div class="table-cell smart-table-cell">Status</div>
		<div class="table-cell smart-table-cell">Version</div>
		<div class="table-cell smart-table-cell"></div>
	</div>
	<c:forEach var="ca" items="${cas}" varStatus="count">
		<div data-cauuid ="${ca.getUuid()}" class="carow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
			<div class="table-cell smart-table-cell">${ca.getLabel()}</div>
			<div class="table-cell smart-table-cell">${ca.getUuid()}</div>
			<div class="table-cell smart-table-cell">${ca.getStatus()}</div>
			<div class="table-cell smart-table-cell">${ca.getVersion().toString()}</div>
			<div class="table-cell smart-table-cell "><a href="" data-cauuid = "${ca.getUuid()}" title="delete conservation area" class="deleteca delete-icon"></a></div>
		</div>
	</c:forEach>
	</div>  
</div>

</div>

<%@include file="footer.jsp" %>

</body>
</html>