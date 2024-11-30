<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/query.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/sharedlinkfunctions.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/fontawesome.min.css" />
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/solid.min.css" />
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/regular.min.css" />
	
	<script type="text/javascript" >
		var search="${search}";
		
		var QUERYURL = "${pageContext.request.contextPath}/api/query/";
		var QUERYLINKURL = "${pageContext.request.contextPath}/connect/query/api/";
		var RELATIVEQUERYLINKURL = "/connect/query/api/";
		var SHAREDLINKSERVLETURL = "${pageContext.request.contextPath}/noa/sharedlink/";
		var CAURL = "${pageContext.request.contextPath}/api/conservationarea/withdataonly/";
		var SHAREDLINKURL = "${pageContext.request.contextPath}/api/sharedlink/";
		
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
	<input class="formtext" type=text name="textsearch" id="textsearch" maxlength=30 oninput="searchChanged()" style="width:30em"/>

	<label><fmt:message key="query.inca" /></label>
	<select id="caselect" class="formtext"  onchange="searchChanged()" style="max-width:24em">
	<option value="allcas"><fmt:message key="query.allcas" /></option>
	</select>
</div>
<div>
<input type="checkbox" id="qhideexe" checked onchange="updateQueryTable()"><fmt:message key="query.hidenonexecutable"/>
</div>

<div class="top-spacer" style="flex: 1 1 auto; display:flex; height:0">

  <div style="width: 25%">
    <div class="smart-table-header" style="border-bottom:1px solid #CCCCCC"><fmt:message key="query.folders"/></div>
  	<div id="foldertable" style="height: 100%; overflow: auto;">
  	<div><fmt:message key="query.loading"/></div>
  	</div>  
  </div>
  
  <div style="margin-left:30px">
  	<div class="smart-table-header" style="border-bottom:1px solid #CCCCCC" id="querypath"><fmt:message key="query.allqueries"/></div>
  	<div id="querytable" style="height: 100%; overflow-x: auto;overflow-y:scroll;">
  		<div class="table-row smart-table-header">
			<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('conservationArea')"><fmt:message key="query.conservationarea" /></a></div>
			<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('type')"><fmt:message key="query.type" /></a></div>
			<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('name')"><fmt:message key="query.name" /></a></div>
			<div class="table-cell smart-table-cell"></div>
		</div>
		<div class="table-row queryrow">
			<div class="table-cell smart-table-cell"><fmt:message key="query.loading"/></div>	
		</div>
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
     		
     		<p class="run-title" id="queryname"></p>
     		<input id="queryuuid" class="formtext" type="hidden" name="uuid" value="" style="width:100%" disabled/>
     		
     		<fieldset id="datefieldset">
	     		<legend><fmt:message key="query.datefilter"/></legend>
	     		<p><fmt:message key="query.datefield" />
		     		<select id="datefield" name="datefield" style="width:100%" class="formtext" >
		     		</select>
		     	</p>
		     	<p><fmt:message key="query.datefilterlabel"/>
		     	  <select style="width:100%" id="defineddates" class="formtext" ></select>
		     	</p>
	     		
	     		<div style="margin-left:20px">
		     		<p><fmt:message key="query.startdate" /> <input id="startdate"  type="text" name="startdate" class="formtext date-input" style="float:none;"/></p>
		     		<p><fmt:message key="query.enddate" /> <input id="enddate" type="text" name="enddate" class="formtext date-input" style="float:none;"/></p>
	     		</div>
     		</fieldset>
     		
     		<fieldset>
	     		<legend><fmt:message key="query.format" /></legend>
	     		<select id="queryformat" name="format" style="width:100%" class="formtext" >
					<c:forEach var="exp" items="${exporters}" varStatus="count">
	     				<option value="${exp[0]}">${exp[1]}</option> 
					</c:forEach> 
     			</select>
     			<table><tr><td>
     			<label id="sridDropdownLobel" style="display:none">EPSG:</label>
     			</td><td>
     			<select id="sridDropdown" name="sridDropdown" style="display:none" class="formtext" >
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
	     		<p><a href="javascript:selectAll();"><fmt:message key="query.selectall"/></a>
					<a href="javascript:selectNone();"><fmt:message key="query.selectnone"/></a>
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
     		<div style="text-align: right" >
     			
     			<fieldset class="linkdialog" style='border:1px solid #BBC6F5; border-radius: 3px'>
     				<fmt:message key="query.sharingDescriptionUsers" />
					<input id='urllink' type=text name="urllink" class="linkdialog">
				</fieldset>
				<center><fmt:message key="query.or"/></center>
				<div id='createcustomlinktitle' style="text-align:center">
					<a href='' id='createcustomlinklink'><fmt:message key="query.createcustomtitle"/></a>
				</div>
				<div id='createcustomlink' style='display:none'>
					<fieldset class="linkdialog" style='border:1px solid #BBC6F5; border-radius: 3px'>
						<p><fmt:message key="query.sharingDescriptionAll"/> <span class="smart-warninghighlight"><fmt:message key="query.sharingDescriptionAll2" /></span></p>
						
						<table class="top-spacer" style="width:100%">
						  <tr>
						   <td><fmt:message key="query.numminutes" /></td>
						   <td>
							<select id="quickMinSelect" class="formtext">
								<option value=60><fmt:message key="sharedlinks.onehour"/></option>
								<option value=1440><fmt:message key="sharedlinks.oneday"/></option>
								<option value=10080><fmt:message key="sharedlinks.oneweek"/></option>
								<option value=43200><fmt:message key="sharedlinks.onemonth"/></option>
								<option value=259200><fmt:message key="sharedlinks.sixmonths"/></option>
								<option value=518400><fmt:message key="sharedlinks.oneyear"/></option>
								<option value=-1><fmt:message key="sharedlinks.custom"/></option>
							</select>
						  </td>
						   <td><input id="expiresAfter" class="formtext"  type="number" name="expiresAfter" value=60 style='width:65px' min="0" max="2147483647" disabled> <fmt:message key="query.numminutes2"/></td>
						  </tr>
						  <tr >
						     <td colspan=3 align="center"><input id="createlinkbutton" class="button close" type="button" style="width: 100%" value="<fmt:message key="query.creatbutton"/>" /></td>
						  </tr>
						  <tr >
						     <td colspan=3><input id="createdlink" class="hide linkdialog" type="text"/></td>
						  </tr>
						</table>
	   				</fieldset>	   				
	   			</div>
	   			<div>
	   				<input id="close" class="button close" type="button" value="<fmt:message key="query.closebutton"/>" />
	   			</div>
   			</div>
    	</form>
</div>


</body>
</html>