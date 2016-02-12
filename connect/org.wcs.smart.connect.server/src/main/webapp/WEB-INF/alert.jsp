<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>

<%@include file="includes.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/leaflet.css"/>
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet.awesome-markers.css">
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-src.js"></script>
<script type="text/javascript" src='https://api.mapbox.com/mapbox.js/v2.2.2/mapbox.js'></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet.awesome-markers.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/alert.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>



<!-- 	var styleFillColors = { -->
<%-- 			<c:forEach var="type" items="${alertTypes}" varStatus="count"> --%>
<%-- 	 			"${type.getUuid()}" : "${type.getFillColor()}",  --%>
<%-- 			</c:forEach>  --%>
<!-- 		}; -->
<script type="text/javascript" >
	var mobile="${mobile}";
	var tab = ${tab};
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
	var styleSpin= {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getSpin()}", 
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
<script src="https://maps.google.com/maps/api/js?v=3.2&sensor=false"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-realtime.js"></script>
<link href='https://api.mapbox.com/mapbox.js/v2.2.2/mapbox.css' rel='stylesheet' />

<title><fmt:message key="alert.maptitle" /></title>
</head>
<body>
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id= "main">
  <div class="pageheader"><fmt:message key="alert.pageheader" /></div>
  <div>
    <div id="message" class="msgsection"></div>
  </div>
  <article id="tabs" class="tabs">
	<section id="tab1" class="">
		<h2 id="tab1text" class=""><a onclick="settab(1)"><fmt:message key="alert.shortmaptitle" /></a></h2>
		<div id="map">
		</div>
	</section>
	
	<section id="tab2" class="">
		<h2 id="tab2text" class=""><a onclick="settab(2)"><fmt:message key="alert.createnewalert" /></a></h2>
		<p>
		<form id="newalertform">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="alert.calabel" /></label>
     		<select name="alert_ca" class="block formtext alert-select">
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
			
			<label class="top-spacer block"><fmt:message key="alert.longitudelabel" /></label><input id="long" type="text" name="long">
			<label class="top-spacer block"><fmt:message key="alert.latitudelabel" /></label><input id="lat" type="text" name="lat" >
			
			<label class="top-spacer block"><fmt:message key="alert.descriptionlabel" /></label>
			<textarea name="alert_description" rows="5" cols="72"></textarea>
   			<input class="button block top-spacer" type="submit" value="   Submit    "/>
    	</form>
		</p>
	</section>
	
	<section id="tab3" class="">
		
		<h2 id="tab3text" class=" "><a onclick="settab(3)"><fmt:message key="alert.viewmanagealerts" />:(0)</a></h2>
		<div class="overflow"><table id="alerttable">
		<tr class="table-row smart-table-header"><th><a onclick="sort('typeUuid')" href="#"><fmt:message key="alert.type" /></a></th><th><a onclick="sort('userGeneratedId')" href="#"><fmt:message key="alert.id" /></a></th><th><a onclick="sort('date')" href="#"><fmt:message key="alert.type" /></a></th><th><a onclick="sort('description')" href="#"><fmt:message key="alert.description" /></a></th><th><a onclick="sort('level')" href="#"><fmt:message key="alert.eventimportance" /></a></th><th><a onclick="sort('status')" href="#"><fmt:message key="alert.status" /></a></th><th><a onclick="sort('x')" href="#"><fmt:message key="alert.location" /></a></th><th><fmt:message key="actions" /></th></tr>
		</table>
		</div> 
	</section>
	
	<div id="map-info-box">
		Last updated 0/0/0000 00:00:00 <a href='javascript:refreshAlerts()'>update now</a>
	</div>
	
	<div id="filter-controls">
			<a id="filter-link" onClick="hideShowFilters()"><img id="filter-button"/><fmt:message key="alert.showfilters" /></a>

			<form id="filter-form" name="filter-form" onsubmit="return false;">
			<input id="sortBy" type="hidden" name="sortBy" value="userGeneratedId"/>
			<input id="sortAscending" type="hidden" name="sortAscending"  value="true"/>

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
			<option value=-1><fmt:message key="alert.customdates" /></option>
			</select>

			<br><input type="text" id="datePickerFrom" class="date-input">
			<font class="date-text">to </font><input type="text" id="datePickerTo" class="date-input">

			<p><fmt:message key="alert.filters.types" /><br>
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<input class='filterType updateChange' name = "${type.getUuid()}" value="${type.getUuid()}" type="checkbox"> ${type.getLabel()} </input><br> 
			</c:forEach> 
			</p>
			
			<p><fmt:message key="alert.filters.status" /><br>
			<c:forEach var="s" items="${status}" varStatus="count">
				<input class='filterStatus updateChange' name="${s}" value="${s}" type="checkbox" checked>${s}</input><br>
			</c:forEach>
			
			<p><fmt:message key="alert.filters.importance" /><br>
			<input class='filterImportance updateChange' type="checkbox" name="level1" value=1 checked>1(Highest)</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level2" value=2 checked>2</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level3" value=3 checked>3</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level4" value=4 checked>4</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level5" value=5 checked>5(Lowest)</input><br>
			</p>
			<p><fmt:message key="alert.filters.ca" /><br>
			<c:forEach var="ca" items="${cas}" varStatus="count">
				<input class='filterCa updateChange' name="${ca.getUuid()}" value="${ca.getUuid()}" type="checkbox">${ca.getLabel()}</input><br>
			</c:forEach>
			</p>
			<p>
			<fmt:message key="alert.filters.text" /><br>
			<input id='filterText' class='updateChange' name="textFilter" type="text"></input>
			</p> 
			</form>

	</div>
  </article>
	
</div>

<%@include file="footer.jsp" %>


<div id="updateAlertDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="alert.updatealert" /></div>
  <div id="dialogerror" class="errorsection"></div>
	<form id="updatealertform" name="updatealertform">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="alert.calabel" /></label>
     		<input type="hidden" name="uuid" value="" />
     		<input type="hidden" name="user_id" value="" />
     		<select name="update_alert_ca" class="block formtext alert-select">
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
			<option value=1>1(Highest)</option>
			<option value=2>2</option>
			<option value=3>3</option>
			<option value=4>4</option>
			<option value=5>5(Lowest)</option>
			</select>
			
			<label class="top-spacer block"><fmt:message key="alert.statuslabel" /></label>
			<select name="update_status" class="block formtext alert-select">
			<option value="ACTIVE">ACTIVE</option>
			<option value="DISABLED">DISABLED</option>
			</select>
			
			<label class="top-spacer block"><fmt:message key="alert.longitudelabel" /></label><input id="long" type="text" name="update_long">
			<label class="top-spacer block"><fmt:message key="alert.latitudelabel" /></label><input id="lat" type="text" name="update_lat" >
			
			<label class="top-spacer block"><fmt:message key="alert.track" /></label><input size=50 id="track" type="text" name="update_track" >
			
			
			<label class="top-spacer block"><fmt:message key="alert.descriptionlabel" /></label>
			<textarea name="update_alert_description" rows="5" cols="45"></textarea>
   			<div class="block top-spacer" style="text-align:right">
   			 <input class="button top-spacer" type="submit" value="   Update    "/>
   			 <input class="button" type="button" id="cancel" value="Cancel" />
   			 </div>
    	</form>
  </div>

</body>
</html>