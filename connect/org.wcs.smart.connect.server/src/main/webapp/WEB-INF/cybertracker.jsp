<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html style="height: 100%;">
<head>

<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/cybertracker.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>



<title>CyberTracker Packages</title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
  <div class="tabheader pageheader">CyberTracker Packages</div>
  <p class="infomessage">Lists all CyberTracker packages uploaded to Connect.</p>
  <div>
    <div id="message" class="msgsection"></div>
  </div>
  <!-- Package Table -->
  
  <div class="top-spacer" >
    <div id="ctpackagetable" class="table-cell smart-table">
  	  <div class="table-row smart-table-header">
		  <div class="table-cell smart-table-cell">Name</div>
		  <div class="table-cell smart-table-cell">Conservation Area</div>
		  <div class="table-cell smart-table-cell">Date Uploaded</div>
		  <div class="table-cell smart-table-cell">Revision Date</div>
		  <div class="table-cell smart-table-cell">Revision</div>
		  <div class="table-cell smart-table-cell"></div>
		  <div class="table-cell smart-table-cell"></div>
	  </div>
    </div>
    <a id="refreshnow" href="#">refresh</a>
  </div>	
</div>		

	<%@include file="footer.jsp" %>

	<div id="deleteDialog" style="display: none;" class="dialog">
	  <div class="dialog-title">Delete Package</div>
	  <div id="dialogerror" class="errorsection"></div>
	  
	  <form id="deleteform" onsubmit="return deletePackage();" >
	    <input type="hidden" name="packageuuid"/>
	   	<p>Are you sure you want to delete this package?</p>
	   	<div class="block top-spacer" style="text-align:right">
	     <input class="button" type="submit" value="Delete" />
	     <input class="button" type="button" value="Cancel" onclick="closeDialog('deleteDialog')" />
	    </div>
	  </form>
  </div>
  
</body>
</html>