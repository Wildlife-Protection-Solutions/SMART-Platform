<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/report.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
	
	<script type="text/javascript" >
		var search="${search}";
		
		var REPORTURL = "${pageContext.request.contextPath}/api/report/";
// 		var REPORTLINKURL = "${pageContext.request.contextPath}/connect/report/api/";
		var REPORTLINKURL = "${pageContext.request.contextPath}/api/report/";
		var CAURL = "${pageContext.request.contextPath}/api/conservationarea/withdataonly";
		
// 		var datefilters = {
// 				<c:forEach var="df" items="${datefilters}">
// 				    '${df[0]}': '${df[1]}',
// 				</c:forEach>
// 				};
		
// 		var qdatefilter = {
// 				<c:forEach var="entry" items="${qdatefilters}">
// 				    '${entry.key}': [
// 				    	<c:forEach var="op" items="${entry.value}">
// 				    		'${op}',
// 				    	</c:forEach>
// 				    ],
// 				</c:forEach>
// 				};
	</script>
		
	<title><fmt:message key="report.pagetitle"/></title>	
</head>

<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
<div class="pageheader"><fmt:message key="report.reports"/></div>
<div><div id="message" class="msgsection"></div></div>
<div class="top-spacer"> 
	<!-- Search Parameters -->
	<label><fmt:message key="report.search" /></label>
	<input type=text name="textsearch" id="textsearch" maxlength=30 oninput="searchChanged()"/>

	<label><fmt:message key="report.inca" /></label>
	<select id="caselect" onchange="searchChanged()" style="max-width:24em">
	<option value="allcas"><fmt:message key="report.allcas" /></option>
	</select>
</div>

<div class="top-spacer"  style="margin-left: -20px" >
  <div id="reporttable" class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('conservationArea')"><fmt:message key="report.conservationarea" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('id')"><fmt:message key="report.id" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('name')"><fmt:message key="report.name" /></a></div>
		

		<div class="table-cell smart-table-cell"></div>
	</div>
	</div>  
</div>

</div>

<%@include file="footer.jsp" %>


<div id="reportOptionsDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="report.runreport" /></div>
  <div id="dialogerror" class="errorsection"></div>
	<form id="runreportform">
     		<div id="error" class="errorsection" style="display:none;"> </div>
     		
     		<fieldset>
	     		<legend><fmt:message key="report.reportproperties"/></legend>
	     		<p><fmt:message key="report.reportname" /><input id="reportname" type="text" name="name" value="" style="width:100%" disabled/></p>
	     		<p><fmt:message key="report.reportuuid" /><input id="reportuuid" type="text" name="uuid" value="" style="width:100%" disabled/></p>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="report.datefilter"/></legend>
	     		<p><fmt:message key="report.datefield" />
		     		<select id="datefield" name="datefield" style="width:100%">
			     		<c:forEach var="df" items="${datefilters}" varStatus="count">
		     				<option value="${df[0]}">${df[1]}</option> 
						</c:forEach> 
		     		</select>
		     	</p>
		     	<p><fmt:message key="report.datefilterlabel"/>
		     	  <select style="width:100%" id="defineddates"></select>
		     	</p>
	     		
	     		<div style="margin-left:20px">
		     		<p><fmt:message key="report.startdate" /> <input id="startdate" type="text" name="startdate" class="date-input" style="float:none;"/></p>
		     		<p><fmt:message key="report.enddate" /> <input id="enddate" type="text" name="enddate" class="date-input" style="float:none;"/></p>
	     		</div>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="report.format" /></legend>
	     		<select id="reportformat" name="format" style="width:100%">
					<c:forEach var="exp" items="${exporters}" varStatus="count">
	     				<option value="${exp[0]}">${exp[1]}</option> 
					</c:forEach> 
     			</select>
     		</fieldset>
     		
     		<fieldset id="cafilter">
	     		<legend><fmt:message key="report.cafilters"/></legend>
	     		<div id="cafilteroptions"></div>
     		</fieldset>
     		<div style="text-align: right">
	     		<input id="runreportbutton" class="button top-spacer" type="button" value="  <fmt:message key="report.runbutton"/>  "/>
	   			<input id="cancel" class="button" type="button" value="<fmt:message key="report.cancelbutton"/>" />
   			</div>
   			<p class="small"><a href="javascript:getUrlOnly()"><fmt:message key="report.geturl" /></a></p>
    	</form>
  </div>
</body>
</html>