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

<div>
    <button class="block button top-spacer" id="btnNewFile">Upload New File</button>
</div>

<div class="top-spacer"  style="margin-left: -20px" >
  <div id="fileTable" class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
	<div class="table-cell smart-table-cell">Conservation Area</div>
	<div class="table-cell smart-table-cell">Name</div>
	<div class="table-cell smart-table-cell">Type</div>
	<div class="table-cell smart-table-cell">Status</div>
	<div class="table-cell smart-table-cell">Upload Date</div>
	<div class="table-cell smart-table-cell">Upload User</div>
	<div class="table-cell smart-table-cell"><fmt:message key="actions" /></div>
	</div>
  </div>  
</div>


</div>

<%@include file="footer.jsp" %>
<div id="updateFileDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Update File Status</div>
  <div id="dialogerror" class="errorsection"></div>
  <form id="updateFileForm" >
  <div>
  <fmt:message key="dataqueue.updatewarning" />
  </div>
	<input name="updateUuid" type="hidden"></input>
    <select name="newStatus" class="formtext block">
    	<option value="QUEUED">QUEUED</option>
		<option value="COMPLETE">COMPLETE</option>
		<option value="ERROR">ERROR</option>
    </select>
    <div class="block top-spacer" style="text-align:right">
     <input id="btnUpdateFile" class="button" type="button" value="Update Status" />
     <input class="button" type="button" id="cancelUpdateFile" value="Cancel" />
    </div>
  </form>
  </div>
 
  <div id="newFileDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Upload New File for Processing</div>
  <div id="dialogerror" class="errorsection"></div>
  <form id="newFileForm" >
    <label class="block top-spacer">CA:</label>
    <select name="conservationArea" class="block formtext alert-select">
     		<c:forEach var="ca" items="${cas}" varStatus="count">
     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
			</c:forEach> 
     		</select>
    <label class="block top-spacer">File Type:</label>
    <select name="type" class="formtext block">
    	<option value="PATROL_XML">Patrol XML File</option>
		<option value="INCIDENT_XML">Incident XML File</option>
		<option value="MISSION_XML">Mission XML File</option>
    </select>
    <label class="block top-spacer">Select File:</label>
    <input id="file" type="file" name="file" class="formtext block" />
    <div class="block top-spacer" style="text-align:right">
     <input id="btnUploadFile" class="button" type="button" value="Upload File" />
     <input class="button" type="button" id="cancelNewFile" value="Cancel" />
    </div>
  </form>
  </div>
  
</body>
</html>