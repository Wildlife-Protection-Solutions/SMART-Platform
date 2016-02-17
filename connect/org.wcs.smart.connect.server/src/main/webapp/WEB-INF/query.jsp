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
  <div id="message" class="msgsection"></div>
</div>

<div class="top-spacer"> 
<!-- Search Parameters -->
	<label><fmt:message key="query.search" /></label>
	<input type=text name="textsearch" id="textsearch" maxlength=30 oninput="searchChanged()"/>

	<label><fmt:message key="query.inca" /></label>
	<select id="caselect" onchange="searchChanged()" style="max-width:24em">
	<option value="allcas"><fmt:message key="query.allcas" /></option>
	</select>
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
     		
     		<fieldset>
     		<legend>Query Properties</legend>
     		<p><fmt:message key="query.queryname" /><input id="queryname" type="text" name="name" value="" style="width:100%" disabled/></p>
     		<p><fmt:message key="query.queryuuid" /><input id="queryuuid" type="text" name="uuid" value="" style="width:100%" disabled/></p>
     		</fieldset>
     		
     		<fieldset>
     		<legend>Date Filter</legend>

     		<p><fmt:message key="query.datefield" />
     		
     		
     		
	     		<select id="datefield" name="datefield" style="width:100%">
	     		<c:forEach var="df" items="${datefilters}" varStatus="count">
     				<option value="${df[0]}">${df[1]}</option> 
				</c:forEach> 
	     		</select>
	     		</p>
	     		<p>Date Filter: <select style="width:100%" id="defineddates"></select></p>
     		
     		<div style="margin-left:20px">
	     		<p><fmt:message key="query.startdate" /> <input id="startdate" type="text" name="startdate" class="date-input" style="float:none;"/></p>
	     		<p><fmt:message key="query.enddate" /> <input id="enddate" type="text" name="enddate" class="date-input" style="float:none;"/></p>
     		</div>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="query.format" /></legend>
	     		<select id="queryformat" name="format" style="width:100%">
     		 		<option value="csv"><fmt:message key="query.csv" /></option>
     			</select>
     		</fieldset>
     		
     		<fieldset id="cafilter">
	     		<legend>Conservation Area Filters</legend>
	     		<div id="cafilteroptions"></div>
     		</fieldset>
     		<div style="text-align: right">
	     		<input id="runQueryButton" class="button top-spacer" type="button" value="  Run Query  "/>
	   			<input id="cancel" class="button" type="button" value="Cancel" />
   			</div>
   			<p class="small"><a href="javascript:getUrlOnly()"><fmt:message key="query.geturl" /></a></p>
    	</form>
  </div>
</body>
</html>