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
	<title><fmt:message key="dataqueue.pagetitle"/></title>
</head>

<body>
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id="main">
		<div class="pageheader"><fmt:message key="dataqueue.title"/></div>
		<p class="infomessage"><fmt:message key="dataqueue.info"/></p>
		<div><div id="message" class="msgsection"></div></div>
		
		<div><button class="block button top-spacer" id="btnNewFile"><fmt:message key="dataqueue.newfilebutton"/></button></div>

		<div class="top-spacer link_small" >
			<fmt:message key="dataqueue.lastupdated"/><span id="lastUpdateTime"></span><a id="refreshnow" style="padding-left:3px" href="#"><fmt:message key="dataqueue.refresh"/></a>
		</div>
		<div  style="margin-left: -20px" >
			<div id="fileTable" class="catable table-cell smart-table">
			  	<div class="table-row smart-table-header">
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.calabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.namelabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.typelabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.statuslabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.lastmodifiedlabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.uploadeddatelabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="dataqueue.uploadeduserlabel"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="actions" /></div>
				</div>
  			</div>  
		</div>
	</div>
<%@include file="footer.jsp" %>

<div id="updateFileDialog" style="display: none;" class="dialog">
	<div class="dialog-title"><fmt:message key="dataqueue.updatedialogtitle"/></div>
	<div id="dialogerror" class="errorsection"></div>
	<form id="updateFileForm" >
  		<div><fmt:message key="dataqueue.updatewarning" /></div>
		<input name="updateUuid" type="hidden"></input>
		<label class="block top-spacer"><fmt:message key="dataqueue.updatestatuslabel"/></label>
		<select name="newStatus" class="formtext block">
	   		<c:forEach var="status" items="${statusTypes}" varStatus="count">
	     		<option value="${status[1]}">${status[0]} </option> 
			</c:forEach>
    	</select>
    	<label class="block top-spacer"><fmt:message key="dataqueue.updatetypelabel"/></label>
    	<select name="updateType" class="formtext block">
	    	<c:forEach var="type" items="${uploadtypes}" varStatus="count">
	     		<option value="${type[1]}">${type[0]} </option> 
			</c:forEach>
    	</select>
    	<div class="block top-spacer" style="text-align:right">
     		<input id="btnUpdateFile" class="button" type="button" value="<fmt:message key="dataqueue.updatebutton"/>" />
     		<input class="button" type="button" id="cancelUpdateFile" value="<fmt:message key="dataqueue.cancelbutton"/>" />
    		</div>
  	</form>
</div>
 
<div id="newFileDialog" style="display: none;" class="dialog">
	<div class="dialog-title"><fmt:message key="dataqueue.newdialogtitle"/></div>
	<div id="dialogerror" class="errorsection"></div>
	<form id="newFileForm" >
		<label class="block top-spacer"><fmt:message key="dataqueue.newcalabel"/></label>
    	<select name="conservationArea" class="block formtext alert-select">
     		<c:forEach var="ca" items="${cas}" varStatus="count">
     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
			</c:forEach> 
     	</select>
		<label class="block top-spacer"><fmt:message key="dataqueue.newtypelabel"/></label>
		<select name="type" class="formtext block">
	    	<c:forEach var="type" items="${uploadtypes}" varStatus="count">
	     		<option value="${type[1]}">${type[0]} </option> 
			</c:forEach>
    	</select>
    	<label class="block top-spacer"><fmt:message key="dataqueue.newfile"/></label>
    	<input id="file" type="file" name="file" class="formtext block" />
    	<div class="block top-spacer" style="text-align:right">
     		<input id="btnUploadFile" class="button" type="button" value="<fmt:message key="dataqueue.newbutton"/>" />
     		<input class="button" type="button" id="cancelNewFile" value="<fmt:message key="dataqueue.cancelbutton"/>" />
    		</div>
  	</form>
</div>
  
</body>
</html>