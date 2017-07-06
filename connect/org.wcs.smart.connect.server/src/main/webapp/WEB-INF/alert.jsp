<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>

<%@include file="includes.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/leaflet/leaflet.css"/>
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet/leaflet.awesome-markers.css">
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-src.js"></script>
<script type="text/javascript" src='https://api.mapbox.com/mapbox.js/v2.2.2/mapbox.js'></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet.awesome-markers.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/alert.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
<script src="https://maps.google.com/maps/api/js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-realtime.js"></script>
<link href='https://api.mapbox.com/mapbox.js/v2.2.2/mapbox.css' rel='stylesheet' />

<script type="text/javascript" >
	var mobile="${mobile}";
	var tab = ${tab};
	
	var styleUuids = [
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}", 
			</c:forEach> 
		];

	
	
	var styleColors = {
		<c:forEach var="type" items="${alertTypes}" varStatus="count">
 			"${type.getUuid()}" : "${type.getColor()}", 
		</c:forEach> 
	};

	var styleOpacity = {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getOpacity()}", 
			</c:forEach> 
		};
	var styleMarkerColor = {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getMarkerColor()}", 
			</c:forEach> 
		};
	var styleMarkerIcon = {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getMarkerIcon()}", 
			</c:forEach> 
		};
	var styleCustomIcon = {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getCustomIcon()}", 
			</c:forEach> 
		};
	var styleSpin= {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getSpin()}", 
			</c:forEach> 
		};
	var markerLabels= {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getLabel()}", 
			</c:forEach> 
		};

	
	
	var startingZoom = ${startingZoom};
	var startingLong = ${startingLong};
	var startingLat = ${startingLat};
	var canupdate = ${canupdate};
	var candelete = ${candelete};
	
	
	<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
	var mapLayers = [ 
	  <c:forEach var="layer" items="${mapLayers}">
		  [ <c:out value="${layer.getLayerType()}"/>,
		  "<c:out value="${layer.getToken()}"/>",
		  "<c:out value="${layer.getMapboxId()}"/>", 
		  "<c:out value="${layer.getWmsLayerList()}"/>",
		  "<c:out value="${layer.getLayerName()}"/>", 
		  "<c:out value="${layer.isActive()}"/>"],
	  </c:forEach>
	];
</script>

<title><fmt:message key="alert.maptitle" /></title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id= "main">
  <div>
    <div id="message" class="msgsection"></div>
  </div>
  <article id="tabs" class="tabs">
	<section id="tab1" class="">
		<div id="map"></div>
		<div id="spacer"></div>
		
	</section>
	
	
	<div id="filter-controls">
		<p style="text-align:center;font-weight:bold;"><a id="filter-link" onClick="hideShowFilters()"  class="filterIcon"></a></p>

		<form id="filter-form" name="filter-form" action="" onsubmit="return false;">
			<input id="sortBy" type="hidden" name="sortBy" value="date"/>
			<input id="sortAscending" type="hidden" name="sortAscending"  value="false"/>
			
			<p>
			<select id='filterDate' class='updateChange' name="time_filter" style="margin-bottom:3px">
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
				<option value=-1><fmt:message key="alert.customdates" /></option>
			</select>
			</p>
			
			<p>
			<br><input type="text" id="datePickerFrom" class="date-input">
			<font class="date-text">&nbsp &nbsp&nbsp<fmt:message key="alert.dateto"/>&nbsp &nbsp&nbsp</font><input type="text" id="datePickerTo" class="date-input">
			</p>
			
			<p style="clear:both;">
			<fmt:message key="alert.filters.types" /><br>
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
				<div style="min-height: 25px;">
				
     			<input style="position: relative; vertical-align: middle; bottom: 1px; height:25px; margin:0px 0px 0px 4px; padding:0px;"  class='filterType updateChange' name = "${type.getUuid()}" value="${type.getUuid()}" type="checkbox">  

				<div style="float:left; position:relative; height:24px; width:20px;" class="awesome-marker-icon-${type.getMarkerColor()}-legend awesome-marker leaflet-zoom-animated" >
					<i style="color:${type.getColor()}" class="fa fa-${type.getMarkerIcon()}">${type.getCustomIcon()}</i><br>
				</div>

     			${type.getLabel()}
     			</input><br>
     			</div> 
			</c:forEach> 
			</p>
			
			<p><fmt:message key="alert.filters.status" /><br>
			<c:forEach var="s" items="${status}" varStatus="count">
				<label><input class='filterStatus updateChange' value="${s[0]}" type="checkbox" checked/>${s[1]}</label><br>
			</c:forEach>
			
			<p><fmt:message key="alert.filters.importance" /><br>
			<label><input class='filterImportance updateChange' type="checkbox" value=1 checked/><fmt:message key="alert.eventimportance1" /></label><br>
			<label><input class='filterImportance updateChange' type="checkbox" value=2 checked/><fmt:message key="alert.eventimportance2" /></label><br>
			<label><input class='filterImportance updateChange' type="checkbox" value=3 checked/><fmt:message key="alert.eventimportance3" /></label><br>
			<label><input class='filterImportance updateChange' type="checkbox" value=4 checked/><fmt:message key="alert.eventimportance4" /></label><br>
			<label><input class='filterImportance updateChange' type="checkbox" value=5 checked/><fmt:message key="alert.eventimportance5" /></label><br>
			</p>
			<p><fmt:message key="alert.filters.ca" /><br>
			<c:forEach var="ca" items="${cas}" varStatus="count">
				<input class='filterCa updateChange' name="${ca.getUuid()}" value="${ca.getUuid()}" type="checkbox">${ca.getLabel()}</input><br>
			</c:forEach>
			</p>
			<p>
			<fmt:message key="alert.filters.text" /><br>
			<input id='filterText' class='updateChange' name="textFilter" type="text" style="margin-bottom:3px"></input>
			</p> 
		</form>

	</div>
  </article>
 	<div id="buttons">
 		<a href='javascript:refreshAlerts()'><div class="button mapbutton" style="float:left"><fmt:message key="alert.refresh" /></div></a>
 		<a href='javascript:buttonCreateAlert()'><div class="button mapbutton" style="float:left"><fmt:message key="alert.createalert" /></div></a>
 		<a href='javascript:buttonManageAlerts()'><div class="button mapbutton" style="float:left"><fmt:message key="alert.managealerts" /></div></a>
 		
 	</div>
 	<div id="map-info-box" class="link_small">
		<fmt:message key="alert.lastupdated"/>
	</div>
	<div id="tab3text" class="link_small" style="float:right">0</div>
	<div class="link_small" style="float:right"><fmt:message key="alert.alertsshown" /></div>
  
</div>

<%@include file="footer.jsp" %>

<div id="createAlertDialog" style="display: none;" class="dialog">
	<section id="tab2" class="">
		<p>
		<form id="newalertform" style="padding-left:10px">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="alert.calabel" /></label>
     		<select name="alert_ca" class="block  formtext alert-select">
	     		<c:forEach var="ca" items="${cas}" varStatus="count">
	     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
				</c:forEach> 
     		</select>
     		
     		<label class="top-spacer block"><fmt:message key="alert.typelabel" /></label>
     		<select name="alert_type" class="block formtext alert-select">
	     		<c:forEach var="type" items="${alertTypes}" varStatus="count">
	     			<option value="${type.getUuid()}"> ${type.getLabel()} </option> 
				</c:forEach> 
     		</select>

			<label class="top-spacer block"><fmt:message key="alert.eventimportancelabel" /></label>
			<select name="level" class="block formtext alert-select">
				<option value=1><fmt:message key="alert.eventimportance1" /></option>
				<option value=2><fmt:message key="alert.eventimportance2" /></option>
				<option value=3><fmt:message key="alert.eventimportance3" /></option>
				<option value=4><fmt:message key="alert.eventimportance4" /></option>
				<option value=5><fmt:message key="alert.eventimportance5" /></option>
			</select>
			
			<label class="top-spacer block"><fmt:message key="alert.longitudelabel" /></label>
			<input id="long" type="text" name="long" class="formtext" style="width: 20em">
			<label class="top-spacer block"><fmt:message key="alert.latitudelabel" /></label>
			<input id="lat" type="text" name="lat" class="formtext" style="width: 20em">
			<label class="top-spacer block"><fmt:message key="alert.descriptionlabel" /></label>
			<textarea name="alert_description" rows="5" cols="72"></textarea>
   			<input class="button block top-spacer" type="submit" style="float:left" value="   <fmt:message key="alert.submit" />    "/>
   			<input class="button block top-spacer" type="button" onClick="javascript:buttonCancelCreateAlert()" style="float:right" value="<fmt:message key="alert.cancel" />"/>
    	</form>
		</p>
	</section>
</div>

	

<div id="updateAlertDialog" style="display: none;z-index:201" class="dialog">
  <div class="dialog-title"><fmt:message key="alert.updatealert" /></div>
  <div id="dialogerror" class="errorsection"></div>
	<form id="updatealertform" name="updatealertform">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="alert.calabel" /></label>
     		<input type="hidden" name="uuid" value="" />
     		<input type="hidden" name="user_id" value="" />
     		<select name="update_alert_ca" class="block formtext alert-select" disabled>
     		<c:forEach var="ca" items="${cas}" varStatus="count">
     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
			</c:forEach> 
     		</select>
     		
     		<label class="top-spacer block"><fmt:message key="alert.typelabel" /></label>
     		<select name="update_alert_type" class="block formtext alert-select">
     		<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<option value="${type.getUuid()}"> ${type.getLabel()} </option> 
			</c:forEach> 
     		</select>

			<label class="top-spacer block"><fmt:message key="alert.eventimportancelabel" /></label>
			<select name="update_level" class="block formtext alert-select">
			<option value=1><fmt:message key="alert.alertlevel1"/></option>
			<option value=2><fmt:message key="alert.alertlevel2"/></option>
			<option value=3><fmt:message key="alert.alertlevel3"/></option>
			<option value=4><fmt:message key="alert.alertlevel4"/></option>
			<option value=5><fmt:message key="alert.alertlevel5"/></option>
			</select>
			
			<label class="top-spacer block"><fmt:message key="alert.statuslabel" /></label>
			<select name="update_status" class="block formtext alert-select">
			<c:forEach var="s" items="${status}" varStatus="count">
				<option value="${s[0]}">${s[1]}</option>
			</c:forEach>
			</select>
			
			<label class="top-spacer block "><fmt:message key="alert.longitudelabel" />
			</label><input id="long" type="text" name="update_long" class="formtext" style="width: 20em">
			<label class="top-spacer block "><fmt:message key="alert.latitudelabel" /></label>
			<input id="lat" type="text" name="update_lat" class="formtext" style="width: 20em">
			
			<label class="top-spacer block"><fmt:message key="alert.track" /></label>
			<input size=50 id="track" type="text" name="update_track" class="formtext" style="width: 20em">
			
			
			<label class="top-spacer block"><fmt:message key="alert.descriptionlabel" /></label>
			<textarea name="update_alert_description" rows="5" cols="45"></textarea>
   			<div class="block top-spacer" style="text-align:right">
   			 <input class="button top-spacer" type="submit" value="   <fmt:message key="alert.updatealert" />    "/>
   			 <input class="button" type="button" id="cancel" value="<fmt:message key="alert.cancel" />" />
   			 </div>
    	</form>
  </div>


	
<div id="manageAlertsDialog" style="display: none;width:80%" class="dialog">
	<section id="tab3" class="">
		<div id="alertTableMessage" class="msgsection"></div>
		<div class="overflow">
		<table id="alerttable" style="width:100%">
		<tr class="table-row smart-table-header"><th><a onclick="sort('typeUuid')" href="#"><fmt:message key="alert.type" /></a></th><th><a onclick="sort('ca.label')" href="#"><fmt:message key="query.conservationarea" /></a></th><th><a onclick="sort('date')" href="#"><fmt:message key="alert.date" /></a></th><th><a onclick="sort('description')" href="#"><fmt:message key="alert.description" /></a></th><th><a onclick="sort('level')" href="#"><fmt:message key="alert.eventimportance" /></a></th><th><a onclick="sort('status')" href="#"><fmt:message key="alert.status" /></a></th><th><a onclick="sort('x')" href="#"><fmt:message key="alert.location" /></a></th><th><fmt:message key="actions" /></th></tr>
		</table>
		</div> 
	</section>
</div>



</body>
</html>