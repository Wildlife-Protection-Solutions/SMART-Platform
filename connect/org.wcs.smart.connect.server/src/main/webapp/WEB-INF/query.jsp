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
		
		var QUERYURL = "${pageContext.request.contextPath}/api/query/";
		var QUERYLINKURL = "${pageContext.request.contextPath}/connect/query/api/";
		var RELATIVEQUERYLINKURL = "/connect/query/api/";
		var SHAREDLINKSERVLETURL = "${pageContext.request.contextPath}/noa/sharedlink/";
		var CAURL = "${pageContext.request.contextPath}/api/conservationarea/withdataonly/";
		
		var datefilters = {
				<c:forEach var="df" items="${datefilters}">
				    '${df[0]}': '${df[1]}',
				</c:forEach>
				};
		
		var qdatefilter = {
				<c:forEach var="entry" items="${qdatefilters}">
				    '${entry.key}': [
				    	<c:forEach var="op" items="${entry.value}">
				    		'${op}',
				    	</c:forEach>
				    ],
				</c:forEach>
				};
		var executeableTypes = [
			<c:forEach var="type" items="${executabletypes}">
		    	'${type}',
		    </c:forEach>
		];
		
	</script>
		
	<title><fmt:message key="query.pagetitle"/></title>	
</head>

<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
<div class="pageheader"><fmt:message key="query.queries"/></div>
<div><div id="message" class="msgsection"></div></div>
<div class="top-spacer"> 
	<!-- Search Parameters -->
	<label><fmt:message key="query.search" /></label>
	<input type=text name="textsearch" id="textsearch" maxlength=30 oninput="searchChanged()"/>

	<label><fmt:message key="query.inca" /></label>
	<select id="caselect" onchange="searchChanged()" style="max-width:24em">
	<option value="allcas"><fmt:message key="query.allcas" /></option>
	</select>
</div>
<div>
<input type="checkbox" id="qhideexe" checked onchange="searchChanged()"><fmt:message key="query.hidenonexecutable"/>
</div>

<div class="top-spacer"  style="margin-left: -20px" >
  <div id="querytable" class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('conservationArea')"><fmt:message key="query.conservationarea" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('id')"><fmt:message key="query.id" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('name')"><fmt:message key="query.name" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('type')"><fmt:message key="query.type" /></a></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
	</div>  
</div>

</div>

<%@include file="footer.jsp" %>


<div id="urlOptionsDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="query.runquery" /></div>
  <div id="dialogerror" class="errorsection"></div>
	<form id="runqueryform" name="runqueryform">
     		<div id="error" class="errorsection" style="display:none;"> </div>
     		
     		<fieldset>
	     		<legend><fmt:message key="query.queryproperties"/></legend>
	     		<p><fmt:message key="query.queryname" /><input id="queryname" type="text" name="name" value="" style="width:100%" disabled/></p>
	     		<p><fmt:message key="query.queryuuid" /><input id="queryuuid" type="text" name="uuid" value="" style="width:100%" disabled/></p>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="query.datefilter"/></legend>
	     		<p><fmt:message key="query.datefield" />
		     		<select id="datefield" name="datefield" style="width:100%">
		     		</select>
		     	</p>
		     	<p><fmt:message key="query.datefilterlabel"/>
		     	  <select style="width:100%" id="defineddates"></select>
		     	</p>
	     		
	     		<div style="margin-left:20px">
		     		<p><fmt:message key="query.startdate" /> <input id="startdate" type="text" name="startdate" class="date-input" style="float:none;"/></p>
		     		<p><fmt:message key="query.enddate" /> <input id="enddate" type="text" name="enddate" class="date-input" style="float:none;"/></p>
	     		</div>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="query.format" /></legend>
	     		<select id="queryformat" name="format" style="width:100%">
					<c:forEach var="exp" items="${exporters}" varStatus="count">
	     				<option value="${exp[0]}">${exp[1]}</option> 
					</c:forEach> 
     			</select>
     			<table><tr><td>
     			<label id="sridDropdownLobel" style="display:none">EPSG:</label>
     			</td><td>
     			<select id="sridDropdown" name="sridDropdown" style="display:none">
     				<option value="-1">custom</option>
     				<option selected value="4326">4326(lat/long, WGS84)</option>
     				<option value="4269">4269(lat/long, NAD83)</option>
     				<option value="32600">WGS 84 UTM North (select Zone)</option>
     				<option value="32700">WGS 84 UTM South (select Zone)</option>
     				<option value="3857">Google/Bing/OpenStreetMaps Projection</option>
     			</select>
     			</td><td>
     			<input id="srid" type="number" name="srid" value="4326" style="width:65px;display:none" min="0" max="69036405" disabled>
     			</td></tr>
     			</table>
     			<label id="zoneLabel" style="display:none">UTM Zone</label><input id="utmzone" type="number" name="srid" value="1" style="width:65px;display:none" min="0" max="60">
     			
     		</fieldset>
     		
     		<fieldset id="cafilter" style="height:150px; overflow:auto;">
	     		<legend><fmt:message key="query.cafilters"/></legend>
	     		<div id="cafilteroptions"></div>
     		</fieldset>
     		<div style="text-align: right">
	     		<input id="runQueryButton" class="button top-spacer" type="button" value="  <fmt:message key="query.runbutton"/>  "/>
	   			<input id="cancel" class="button" type="button" value="<fmt:message key="query.cancelbutton"/>" />
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
								<option value=60>1 hour</option>
								<option value=1440>1 day</option>
								<option value=10080>1 week</option>
								<option value=43200>1 month</option>
								<option value=259200>6 months</option>
								<option value=518400>1 year</option>
								<option value=-1>Custom...</option>
							</select>
						  </td>
						   <td><input id="expiresAfter" type="number" name="expiresAfter" value=60 style='width:65px' min="0" max="2147483647" disabled> <fmt:message key="query.numminutes2"/></td>
						  </tr>
						  <tr >
						     <td colspan=3 align="center"><input id="createlinkbutton" class="close" type="button" value="<fmt:message key="query.creatbutton"/>" /></td>
						  </tr>
						  <tr >
						     <td colspan=3><input id="createdlink" class="hide linkdialog" type="text"/></td>
						  </tr>
						</table>
	   				</fieldset>	   				
	   			</div>
	   			<div>
	   				<input id="close" class="close" type="button" value="<fmt:message key="query.closebutton"/>" />
	   			</div>
   			</div>
    	</form>
</div>


</body>
</html>