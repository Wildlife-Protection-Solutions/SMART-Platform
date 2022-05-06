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
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
	<title><fmt:message key="dataqueue.pagetitle"/></title>
</head>

<body style="${style_bodycss}">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id="main">
		<div class="pageheader"><fmt:message key="dataqueue.title"/></div>
		<p class="infomessage"><fmt:message key="dataqueue.info"/></p>
		<div><div id="message" class="msgsection"></div></div>
		
		<div style="padding-top: 10px; padding-bottom: 10px">
			<fmt:message key="dataqueue.cafilter"/> <select id="cafilter" class="formtext"  onchange="filterChanged()" style="max-width:24em; margin-left: 1px; margin-right:6px"></select>
			<fmt:message key="dataqueue.statusfilter"/> <select id="statusfilter" class="formtext"  onchange="filterChanged()" style="max-width:12em; margin-left: 1px; margin-right:6px"></select>
			<fmt:message key="dataqueue.typefilter"/> <select id="typefilter" class="formtext"  onchange="filterChanged()" style="max-width:12em; margin-left: 1px; margin-right:6px"></select>
			<fmt:message key="dataqueue.uploadeddatefilter"/> 
			<input id="datefilter" type="checkbox" onclick="updateDateFilterVisibility(); filterChanged();"/>
			<input id="startdatefilter"  type="text" name="startdate" class="formtext" style="max-width:8em; margin-left: 1px; margin-right:1px;"/>
			<fmt:message key="dataqueue.uploadeddatefilterto"/>
		    <input id="enddatefilter" type="text" name="enddate" class="formtext" style="max-width:8em; margin-left: 1px; margin-right:1px;"/>
		</div>
		

		<div style="display: flex; flex: 1 1 auto; height: 0; border-top: 1px solid #BBBBBB; border-bottom: 1px solid #BBBBBB;">
  			<div style="height: 100%; overflow: auto; flex-grow: 1">
				<div id="fileTable" class="catable table-cell smart-table">
				  	<div class="table-row smart-table-header">
				  		<div class="table-cell smart-table-cell"></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('caName')" ><fmt:message key="dataqueue.calabel"/></a></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('name')" ><fmt:message key="dataqueue.namelabel"/></a></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('type')" ><fmt:message key="dataqueue.typelabel"/></a></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('status')" ><fmt:message key="dataqueue.statuslabel"/></a></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('lastModifiedDate')" ><fmt:message key="dataqueue.lastmodifiedlabel"/></a></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('uploadedDate')" ><fmt:message key="dataqueue.uploadeddatelabel"/></a></div>
						<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('uploadedBy')" ><fmt:message key="dataqueue.uploadeduserlabel"/></a></div>
						<div class="table-cell smart-table-cell"><fmt:message key="actions" /></div>
					</div>
	  			</div> 
	  		</div>
	  		
	  		<div style="flex-grow: 1; margin-left:20px" id="filepreview">
		  		<div class="smart-table-header"><fmt:message key="dataqueue.preview"/></div>
		  		<textarea style="width:90%; height: 90%" id="previewarea"></textarea>
		  		<div><fmt:message key="dataqueue.previewmsg"/></div>
	  		</div>
	  		
		</div>
		<div class="top-spacer link_small" >
			<fmt:message key="dataqueue.lastupdated"/>
			<span id="lastUpdateTime"></span>
			<a id="refreshnow" style="padding-left:3px" href="#"><fmt:message key="dataqueue.refresh"/></a>
			<span style="padding-left: 10px; padding-right:10px">|</span>
			<a id="selectNone" style="padding-right:5px" href="#"><fmt:message key="dataqueue.checknone"/></a>
			<a id="selectCompleted" style="padding-right:5px" href="#"><fmt:message key="dataqueue.checkcomplete"/></a>
			<a id="selectAll" href="#"><fmt:message key="dataqueue.checkall"/></a>
			<span style="padding-left: 10px; padding-right:10px">|</span>
			<!--<input type="button" style="margin-left: 10px; margin-right:10px"  class="button" value="<fmt:message key="dataqueue.deletebtn"/>" id="btnDeleteSelected"/>-->
			<a id="btnDeleteSelected" style="padding-right:5px" href="#"><fmt:message key="dataqueue.deletebtn"/></a>
			<c:if test="${canupload}">
				<span style="padding-left: 10px; padding-right:10px">|</span>
				<!-- <input style="margin-left: 10px; margin-right:10px" type = "button" class="button" id="btnNewFile" value="<fmt:message key="dataqueue.newfilebutton"/>"/>-->
				<a id="btnNewFile" style="padding-right:5px" href="#"><fmt:message key="dataqueue.newfilebutton"/></a>
			</c:if>	
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