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
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/fontawesome.min.css" />
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/solid.min.css" />
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/regular.min.css" />
	
	<script type="text/javascript" >
		var search="${search}";
		var REPORTURL = "${pageContext.request.contextPath}/api/report/";
		var REPORTLINKURL = "${pageContext.request.contextPath}/connect/report/api/";
		var RELATIVEREPORTLINKURL = "/connect/report/api/";
		var SHAREDLINKSERVLETURL = "${pageContext.request.contextPath}/noa/sharedlink/";
		var SHAREDLINKURL = "${pageContext.request.contextPath}/connect/report/api/sharedlink/";
		var CAURL = "${pageContext.request.contextPath}/api/conservationarea/withdataonly";
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
	<input type=text class="formtext" style="width:30em" name="textsearch" id="textsearch" maxlength=30 oninput="searchChanged()"/>

	<label><fmt:message key="report.inca" /></label>
	<select id="caselect" onchange="searchChanged()" class="formtext" style="max-width:24em">
	<option value="allcas"><fmt:message key="report.allcas" /></option>
	</select>
</div>

<div class="top-spacer" style="flex: 1 1 auto; height: 0; display:flex;">  
  <div style="width: 25%">
    <div class="smart-table-header" style="border-bottom:1px solid #CCCCCC"><fmt:message key="report.folders"/></div>
  	<div id="foldertable" style="height: 100%; overflow: auto;">
  		<div><fmt:message key="report.loading"/></div>
  	</div>  
  </div>
  
  <div style="margin-left:30px">
  	<div class="smart-table-header" style="border-bottom:1px solid #CCCCCC" id="reportpath"><fmt:message key="report.allreports"/></div>
  	<div id="reporttable" style="height: 100%; overflow-x: auto;overflow-y:scroll;">
  		<div class="table-row smart-table-header">
			<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('conservationArea')"><fmt:message key="report.conservationarea" /></a></div>
			<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('name')"><fmt:message key="report.name" /></a></div>
			<div class="table-cell smart-table-cell"></div>
		</div>
		<div class="table-row reportrow">
			<div class="table-cell smart-table-cell"><fmt:message key="report.loading"/></div>	
		</div>
  	</div>
  </div>

</div>
</div>

<%@include file="footer.jsp" %>


<div id="urlOptionsDialog" style="display: none; min-width: 450px;" class="dialog">
  <div class="dialog-title"><fmt:message key="report.runreport" /></div>
  <div id="dialogerror" class="errorsection"></div>
	<form id="runreportform">
     		<div id="error" class="errorsection" style="display:none;"> </div>
     		
     		<p class="run-title" id="reportname"></p>
     		
	     	<input class="formtext" id="reportuuid" type="hidden" name="uuid" value="" style="width:100%" disabled/>
     		
     		<div id="customParameters"></div>
     		
     		<fieldset id="parameters_fieldset" style="display:none">
	     		<legend><fmt:message key="report.datefilter"/></legend>
		     	
		     	<select style="width:100%" id="defineddates" class="formtext" ></select>
		     	<div>
		     		<p><fmt:message key="report.startdate" /> <input id="Start Date" type="text" name="startdate" class="formtext date-input" style="float:none; margin-top:5px"/>
		     		<fmt:message key="report.enddate" /> <input id="End Date" type="text" name="enddate" class="formtext date-input" style="float:none; margin-top:5px"/></p>
	     		</div>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="report.format" /></legend>
	     		<select id="reportformat" name="format" class="formtext"  style="width:100%">
	     			<c:forEach var="exp" items="${reportformats}" varStatus="count">
	     				<option value="${exp[0]}">${exp[1]}</option> 
					</c:forEach>
     			</select>
     		</fieldset>
     		
     		<fieldset id="cafilter">
	     		<legend><fmt:message key="report.cafilters"/></legend>
	     		<div style="font-size:0.9em; margin-bottom:3px">
	     			<div style="display:flex; margin: 4px 0px;" >
	     			<i class="fa-solid fa-triangle-exclamation icon-btn-default" style="font-size: 1.5em; margin-right:4px;"></i>
	     			<div style="width: 450px"><fmt:message key="report.ccaabindingwarn"/></div>
	     			</div>
	     			<div style="padding: 4px 0px;">
	     				<a href="javascript:selectAll();"><fmt:message key="report.selectall"/></a>
						<a href="javascript:selectNone();"><fmt:message key="report.selectnone"/></a>
					</div>
	     		</div>
	     		
	     		<div id="cafilteroptions"></div>
     		</fieldset>
     		<div style="text-align: right">
	     		<input id="runreportbutton" class="button top-spacer" type="button" value="  <fmt:message key="report.runbutton"/>  "/>
	   			<input id="cancel" class="button" type="button" value="<fmt:message key="report.cancelbutton"/>" />
   			</div>
   			
   			<p class="small"><a title="Get a direct link for this query" href="javascript:initializeUrlDialog()"><fmt:message key="query.geturl" /></a></p>
    	</form>
  </div>
 
 
<div id="SharedLinksDialog" style="display: none;" class="level2dialog">
  <div class="dialog-title"><fmt:message key="query.sharequery" /></div>
  	<form id="sharedlinkform" name="sharedlinkform">
     		<div style="text-align: right">
     			
     			<fieldset class="linkdialog">
     				<fmt:message key="query.sharingDescriptionUsers" />
					<input id='urllink' type=text name="urllink" class="linkdialog">
				</fieldset>
				<center>OR</center>
				<div id='createcustomlinktitle' style="text-align:center">
					<a href='' id='createcustomlinklink'><fmt:message key="query.createcustomtitle"/></a>
				</div>
				<div id='createcustomlink' style='display:none'>
					<fieldset class="linkdialog">
						<p><fmt:message key="query.sharingDescriptionAll"/> <span class="smart-warninghighlight"><fmt:message key="query.sharingDescriptionAll2" /></span></p>
						
						<table class="top-spacer" style="width:100%">
						  <tr>
						   <td><fmt:message key="query.numminutes" /></td>
						   <td>
							<select id="quickMinSelect">
								<option value=60><fmt:message key="sharedlinks.onehour"/></option>
								<option value=1440><fmt:message key="sharedlinks.oneday"/></option>
								<option value=10080><fmt:message key="sharedlinks.oneweek"/></option>
								<option value=43200><fmt:message key="sharedlinks.onemonth"/></option>
								<option value=259200><fmt:message key="sharedlinks.sixmonths"/></option>
								<option value=518400><fmt:message key="sharedlinks.oneyear"/></option>
								<option value=-1><fmt:message key="sharedlinks.custom"/></option>
							</select>
						  </td>
						   <td><input id="expiresAfter" type="number" name="expiresAfter" value=60 style='width:65px' min="0" max="2147483647" disabled> <fmt:message key="query.numminutes2"/></td>
						  </tr>
						  <tr >
						     <td colspan=3 align="center"><input id="createlinkbutton" class="close button" type="button" value="<fmt:message key="query.creatbutton"/>" /></td>
						  </tr>
						  <tr >
						     <td colspan=3><input id="createdlink" class="hide linkdialog" type="text"/></td>
						  </tr>
						</table>
	   				</fieldset>
	   				
	   			</div>
	   			<div>
	   				<input id="close" class="close button" type="button" value="<fmt:message key="query.closebutton"/>" />
	   			</div>
   			</div>
    	</form>
</div>
 
 
</body>
</html>