<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/query.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />

<script type="text/javascript" >
	var search="${search}";
</script>
	
<title>SMART Connect - Queries</title>

<script>
	
</script>

</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader"><fmt:message key="query.queries" /></div>
<div> 


<!-- Search Parameters -->
<label><fmt:message key="query.search" /></label>
<input type=text name="textsearch" id="textsearch" maxlength=30 oninput="searchChanged()"/>

<label><fmt:message key="query.inca" /></label>
<select id="caselect" oninput="searchChanged()" style="max-width:24em">
<option value="allcas"><fmt:message key="query.allcas" /></option>
</select>

</div>
<p class="infomessage"></p>

<div>
  <div id="message" class="msgsection"></div>
  <div id="error" class="errorsection"></div>
</div>
<div class="top-spacer"  style="margin-left: -20px" >

  <div id="querytable" class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('conservationArea')"><fmt:message key="query.conservationarea" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('id')"><fmt:message key="query.id" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('name')"><fmt:message key="query.name" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('type')"><fmt:message key="query.type" /></a></div>

		<div class="table-cell smart-table-cell"></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
	</div>  
</div>

</div>

<%@include file="footer.jsp" %>


<div id="queryOptionsDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="query.runquery" /></div>
  <div id="dialogerror" class="errorsection"></div>
	<form id="runqueryform" name="runqueryform">
     		<div id="error" class="errorsection" style="display:none;"> </div>
     		<div>
     		<label><fmt:message key="query.queryname" /></label>
     		<input id="queryname" type="text" name="name" value="" style="width:100%" disabled/><br>
     		<label><fmt:message key="query.queryuuid" /></label>
     		<input id="queryuuid" type="text" name="uuid" value="" style="width:100%" disabled/><br>
     		
     		<label class="top-spacer block"><fmt:message key="query.startdate" /></label>
     		<input id="startdate" type="text" name="startdate" class="date-input"/><br>
     		<label class="top-spacer block"><fmt:message key="query.enddate" /></label>
     		<input id="enddate" type="text" name="enddate" class="date-input"/><br>
     		
     		
     		<label class="top-spacer block"><fmt:message key="query.datefield" /></label>
     		<select id="datefield" name="datefield" class="">
     		 	<option value="waypointdate"><fmt:message key="query.waypointdate" /></option>
     		 	<option value="patrolstart"><fmt:message key="query.patrolstartdate" /></option>
     		 	<option value="missionstartdate"><fmt:message key="query.missionstartdate" /></option>
     		 	<option value="missionenddate"><fmt:message key="query.missionenddate" /></option>
     		 	<option value="missiontrackdate"><fmt:message key="query.missiontrackdate" /></option>
     		 	<option value="patrolenddate"><fmt:message key="query.patrolenddate" /></option>
     		 	<option value="patrolstartdate"><fmt:message key="query.patrolstartdate" /></option>
     		 	<option value="recieveddate"><fmt:message key="query.receiveddate" /></option>
     		</select>
     		
     		<label class="top-spacer block"><fmt:message key="query.format" /></label>
     		<select id="queryformat" name="format" class="">
     		 	<option value="csv"><fmt:message key="query.csv" /></option>
     		</select>
     		<br>
     		
     		<input id="runQueryButton" class="button top-spacer" type="button" value="  Run Query  "/>
   			<input id="cancel" class="button" type="button" value="Cancel" />
   			<p class="small"><a href="javascript:getUrlOnly()"><fmt:message key="query.geturl" /></a></p>
   			</div>
    	</form>
  </div>

</body>
</html>