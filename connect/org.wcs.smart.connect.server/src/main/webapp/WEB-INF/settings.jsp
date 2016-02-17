<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script src="${pageContext.request.contextPath}/javascript/jscolor.min.js"></script>
<script type="text/javascript" >
	var numStyles=${numstyles};
</script>
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet.awesome-markers.css">
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/settings.js"></script>

<title><fmt:message key="settings.title" /></title>
</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
  <div class="pageheader"><fmt:message key="settings.pageheader" /></div>
  <div>
    <div id="message" class="msgsection"></div>
   </div>


<div class="overflow settingsTable">

	<div class="block" style="text-align:left"><b><fmt:message key="settings.layerheader" /></b> <button class="button top-spacer" id="btnNewLayer"><fmt:message key="settings.newlayerbutton" /></button>
	</div>
	<table id="layertable">
		<tr class="table-row smart-table-header"><th><fmt:message key="settings.layerorder" /></th><th><fmt:message key="settings.layername" /></th><th><fmt:message key="settings.type" /></th><th><fmt:message key="settings.onbydefault" /></th><th><fmt:message key="settings.mapboxid" /></th><th><fmt:message key="settings.layerlist" /></th><th><fmt:message key="settings.token" /></th><th><fmt:message key="actions" /></th>
		</tr>
	</table>
</div>


<div class="overflow settingsTable">
	<div class="block" style="text-align:left"><b><fmt:message key="settings.styleheader" /></b> <button class="button top-spacer" id="btnNewType"><fmt:message key="settings.addnewstyle" /></button>
	</div>
	<form name=styleform>
	<table id="typetable">
		<tr class="table-type-row smart-table-header"><th><fmt:message key="settings.alerttype" /></th>
				<th><fmt:message key="settings.outlinecolor" /></th>
				<th><fmt:message key="settings.opacity" /></th>
				<th><fmt:message key="settings.markerIcon" /></th>
				<th><fmt:message key="settings.markerColor" /></th>
				<th><fmt:message key="settings.iconSpin" /></th>
				<th><fmt:message key="actions" /></th>
		</tr>
	</table>
	</form>
</div>

<div class="overflow settingsTable">
	<div class="block" style="text-align:left"><b><fmt:message key="settings.defaultsheader" /></b>
	</div>
	<form id="filter-form" name="filter-form" onsubmit="return false;">
	<table id="defaultstable">
		<tr class="table-defaults-row smart-table-header">
			<th>
				
				<input id="filter_uuid" type="hidden" name="uuid" />
				<fmt:message key="settings.datetime" />
			</th>
			<th><fmt:message key="settings.alerttypes" /></th>
			<th><fmt:message key="settings.alertstatus" /></th>
			<th><fmt:message key="settings.alertlevel" /></th>
			<th><fmt:message key="settings.castoinclude" /></th>
			<th><fmt:message key="settings.textfilter" /></th>
			<th style="width:14em"><fmt:message key="actions" /></th>
		</tr>
		<tr class="smart-table-rowon">
		<td>
			<select id='filterDate' class='updateChange' name="time_filter">
				<option value=1><fmt:message key="alert.within1" /></option>
				<option value=2><fmt:message key="alert.within2" /></option>
				<option value=4><fmt:message key="alert.within4" /></option>
				<option value=8><fmt:message key="alert.within8" /></option>
				<option value=12><fmt:message key="alert.within12" /></option>
				<option value=24><fmt:message key="alert.within24" /></option>
				<option value=48><fmt:message key="alert.within48" /></option>
				<option value=168><fmt:message key="alert.withinweek" /></option>
				<option value=744><fmt:message key="alert.withinmonth" /></option>
				<option value=-99><fmt:message key="alert.alldates" /></option>
			</select>
		</td>
		<td>
			<p><fmt:message key="alert.filters.types" /><br>
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<label><input id= "${type.getUuid()}" class='filterType updateChange' value="${type.getUuid()}" type="checkbox"/>${type.getLabel()}</label><br> 
			</c:forEach> 
			</p>
		</td>
		<td>
			<p><fmt:message key="alert.filters.status" /><br>
			<c:forEach var="s" items="${status}" varStatus="count">
				<label><input id="status_${s[0]}" class='filterStatus updateChange' value="${s[0]}" type="checkbox"/>${s[1]}</label><br>
			</c:forEach>
		</td>
		<td>
			<p><fmt:message key="alert.filters.importance" /><br>
				<label><input id="level1" class='filterImportance updateChange' type="checkbox" value=1/><fmt:message key="alert.eventimportance1"/></label><br>
				<label><input id="level2" class='filterImportance updateChange' type="checkbox" value=2/><fmt:message key="alert.eventimportance2"/></label><br>
				<label><input id="level3" class='filterImportance updateChange' type="checkbox" value=3/><fmt:message key="alert.eventimportance3"/></label><br>
				<label><input id="level4" class='filterImportance updateChange' type="checkbox" value=4/><fmt:message key="alert.eventimportance4"/></label><br>
				<label><input id="level5" class='filterImportance updateChange' type="checkbox" value=5/><fmt:message key="alert.eventimportance5"/></label><br>
			</p>
			
		</td>
		<td>
			<p><fmt:message key="alert.filters.ca" /><br>
			<c:forEach var="ca" items="${cas}" varStatus="count">
				<label><input id="${ca.getUuid()}" class='filterCa updateChange' value="${ca.getUuid()}" type="checkbox"/>${ca.getLabel()}</label><br>
			</c:forEach>
			</p>
			<p>
		</td>
		<td>
			<fmt:message key="alert.filters.text" /><br>
			<input id='filterText' class='updateChange' style='width:7em' name="textFilter" type="text"></input>
		</td>
		<td>
			<font class="defaultLabel"><fmt:message key="settings.refresh" /> </font> <input id='secondsRefresh' class='updateChange' style='width:3.5em' name="secondsRefresh" type="number" min=5/><br>
			<font class="defaultLabel"><fmt:message key="settings.startingzoom" /></font> <input id='startingZoom' class='updateChange' style='width:3.5em' name="startingZoom" type="number" min=1 max=12/> <br>
			<font class="defaultLabel"><fmt:message key="settings.startinglong" /></font> <input id='startingLong' class='updateChange' style='width:3.5em' name="startingLat" type="text" /> <br>
			<font class="defaultLabel"><fmt:message key="settings.startinglat" /></font> <input id='startingLat' class='updateChange' style='width:3.5em' name="startingLong" type="text" /> <br>

		<button class="button top-spacer" id="btnUpdateDefaults"><fmt:message key="settings.savedefaults" /></button>
		
		</td>
		</tr>
	</table>
	</form>
</div>
</div>

<%@include file="footer.jsp" %>

<div id="layerDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="settings.layeredit.title" /></div>
  <div id="layerdialogerror" class="errorsection"></div>
	<form id="maplayersform">
     		<div id="layererror" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.orderlabel" /></label>
     		<input class="layer_order" type=number name="layer_order"/>
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.namelabel" /></label>
     		<input class="layer_field" type=text name="layer_name" value="" maxlength="32"/>
     		
     		
     		<input type="hidden" name="uuid" value="" />
     		
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.typelabel" /></label>
			<select name="layer_type" class="block formtext alert-select">
			<option value=1><fmt:message key="settings.layeredit.mapbox" /></option>
			<option value=2><fmt:message key="settings.layeredit.giscloud" /></option>
			<option value=3><fmt:message key="settings.layeredit.wms" /></option>
			</select>
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.onbydefaultlabel" /></label>
     		<select name="layer_status" class="block formtext alert-select">
			<option value="true"><fmt:message key="true" /></option>
			<option value="false"><fmt:message key="false" /></option>
			</select>
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.tokenorurl" /></label>
     		<input class="layer_field" type=text name="layer_token" value="" maxlength="256"/>
 		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.mapboxid" /></label>
     		<input class="layer_field" type=text name="layer_mapbox_id" value="" maxlength="64"/>
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.layerlist" /></label>
     		<input class="layer_field" type=text name="layer_list" value=""/>
     		<div class="top-spacer block">
     			<input id="newLayerButton" class="button" type="button" value="<fmt:message key="settings.createlayerbutton"/>" />
     			<input id="updateLayerButton" class="button" type="button" value="<fmt:message key="settings.updatelayerbutton"/>" />
     			<input class="button" type="button" id="cancelLayer" value="<fmt:message key="settings.cancel"/>" />
     		</div>
    	</form>
  </div>



<div id="typeDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="settings.typeedit.title" /></div>
  <div id="layerdialogerror" class="errorsection"></div>
	<form id="alerttypesform">
     		<div id="layererror" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="settings.typeedit.typelabel" /></label>
     		<input class="type_field" type=text name="type_label" value="" maxlength="32"/>
     		
     		<input type="hidden" name="uuid" value="" />
     		
     		
     		<label class="top-spacer block"><fmt:message key="settings.typeedit.outlinecolorlabel" /></label>
			<input id="type_color" class="type_field jscolor" type=text name="type_color" value="" maxlength="16"/>

      		<label class="top-spacer block"><fmt:message key="settings.typeedit.opactiylabel" /></label>
			<input class="type_field" type=text name="type_opacity" value="" maxlength="8"/>

      		<label class="top-spacer block"><fmt:message key="settings.markerIcon" />: <i id="exampleIcon" class=""></i></label> 
			<select id="type_markerIcon" class="type_field" name="type_markerIcon">
<!-- selected from:		http://fortawesome.github.io/Font-Awesome/icons/ -->				
			</select> <fmt:message key="settings.oroneof"/> <a target="_blank" href="http://fortawesome.github.io/Font-Awesome/icons/">(list)</a>:<input type="text" name="iconOveride" id="iconOveride" value=""/>


      		<label class="top-spacer block"><fmt:message key="settings.markerColor"/>:</label>
			<select class="type_field" name="type_markerColor">
				<option value="red"><fmt:message key="settings.colorred"/></option>
				<option value="darkred"><fmt:message key="settings.colordarkred"/></option>
				<option value="orange"><fmt:message key="settings.colororange"/></option>
				<option value="green"><fmt:message key="settings.colorgreen"/></option>
				<option value="darkgreen"><fmt:message key="settings.colordarkgreen"/></option>
				<option value="blue"><fmt:message key="settings.colorblue"/></option>
				<option value="purple"><fmt:message key="settings.colorpurple"/></option>
				<option value="darkpurple"><fmt:message key="settings.colordarkpurple"/></option>
				<option value="cadetblue"><fmt:message key="settings.colorcadetblue"/></option>
			</select>

			<label class="top-spacer block"><fmt:message key="settings.iconSpin"/>:</label>
     		<select class="type_field" name="type_spin">
     			<option value="true"><fmt:message key="settings.true"/></option>
     			<option value="false"><fmt:message key="settings.false"/></option>
     		</select>
       		<div class="top-spacer block">
     			<input id="newTypeButton" class="button" type="button" value="<fmt:message key="settings.newtypebutton"/>" />
     			<input id="updateTypeButton" class="button" type="button" value="<fmt:message key="settings.updatetypebutton"/>" />
     			<input class="button" type="button" id="cancelType" value="<fmt:message key="settings.cancel"/>" />
     		</div>
    	</form>
  </div>

</body>
</html>