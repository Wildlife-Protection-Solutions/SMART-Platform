<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dataqueue.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<title>SMART Connect - Conservation Areas</title>

<script>
	
</script>

</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader">Data Processing Queue</div>
<p class="infomessage">Lists all items in the SMART Connect Data Processing Queue.</p>

<div>
  <div id="message" class="msgsection"></div>
</div>
<div class="top-spacer"  style="margin-left: -20px" >
  <div class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell">Conservation Area</div>
		<div class="table-cell smart-table-cell">Name</div>
		<div class="table-cell smart-table-cell">Type</div>
		<div class="table-cell smart-table-cell">Status</div>
		<div class="table-cell smart-table-cell">Upload Date</div>
		<div class="table-cell smart-table-cell">Upload User</div>
		<div class="table-cell smart-table-cell"></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
	<c:forEach var="item" items="${items}" varStatus="count">
		<div data-cauuid ="${item.getUuid()}" class="table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
			<div class="table-cell smart-table-cell">${item.getCaName()}</div>
			<div class="table-cell smart-table-cell">${item.getName()}</div>
			<div class="table-cell smart-table-cell">${item.getType()}</div>
			<div class="table-cell smart-table-cell">${item.getStatus()}</div>
			<div class="table-cell smart-table-cell">${item.getUploadedDate()}</div>
			<div class="table-cell smart-table-cell">${item.getUploadedBy()}</div>
			<div class="table-cell smart-table-cell"></div>
			<div class="table-cell smart-table-cell"></div>
		</div>
	</c:forEach>
	</div>  
</div>


</div>



<%@include file="footer.jsp" %>

<div id="deleteDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Delete Item</div>
  <div id="dialogerror" class="errorsection"></div>
  
  <form id="deleteform" onsubmit="return deleteca();" >
    <input type="hidden" name="itemuuid"/>
    <div id="confirmtype">
    	<p>Are you sure you want to delete this item?.</p>
    </div>    
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="Delete" />
     <input class="button" type="button" value="Cancel" onclick="closeDialog('deleteDialog')" />
    </div>
  </form>
  </div>
  
  
</body>
</html>