<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>

<%@include file="includes.jsp" %>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />

<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/alert.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-src.js"></script>
<script type="text/javascript" src='https://api.mapbox.com/mapbox.js/v2.2.2/mapbox.js'></script>

<script type="text/javascript" >
	var mobile="${mobile}";
	var tab = ${tab};
	var styleColors = {
		<c:forEach var="type" items="${alertTypes}" varStatus="count">
 			"${type.getUuid()}" : "${type.getColor()}", 
		</c:forEach> 
	};
	var styleFillColors = {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getFillColor()}", 
			</c:forEach> 
		};
	var styleOpacity = {
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
	 			"${type.getUuid()}" : "${type.getOpacity()}", 
			</c:forEach> 
		};
	
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

<title>SMART Connect - Operational Map</title>
</head>
<body>
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id= "main">
  <div class="pageheader">Alerts</div>
  <div>
    <div id="message" class="msgsection"></div>
    <div id="error" class="errorsection"></div>
  </div>
  <article id="tabs" class="tabs">
	<section id="tab1" class="">
		<h2 id="tab1text" class=""><a onclick="settab(1)">Operational Map</a></h2>
		<div id="map">
		</div>
	</section>
	
	<section id="tab2" class="">
		<h2 id="tab2text" class=""><a onclick="settab(2)">Create New Alert</a></h2>
		<p>
		<form id="newalertform">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block">Conservation Area:</label>
     		<select name="alert_ca" class="block formtext alert-select">
     		<c:forEach var="ca" items="${cas}" varStatus="count">
     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
			</c:forEach> 
     		</select>
     		
     		<label class="top-spacer block">Type:</label>
     		<select name="alert_type" class="block formtext alert-select">
     		<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<option value="${type.getUuid()}"> ${type.getLabel()} </option> 
			</c:forEach> 
     		</select>

			<label class="top-spacer block">Event Importance:</label>
			<select name="level" class="block formtext alert-select">
			<option value=1>1(Highest)</option>
			<option value=2>2</option>
			<option value=3>3</option>
			<option value=4>4</option>
			<option value=5>5(Lowest)</option>
			</select>
			
			<label class="top-spacer block">Longitude:</label><input id="long" type="text" name="long">
			<label class="top-spacer block">Latitude:</label><input id="lat" type="text" name="lat" >
			
			<label class="top-spacer block">Description:</label>
			<textarea name="alert_description" rows="5" cols="72"></textarea>
   			<input class="button block top-spacer" type="submit" value="   Submit    "/>
    	</form>
		</p>
	</section>
	
	<section id="tab3" class="">
		
		<h2 id="tab3text" class=" "><a onclick="settab(3)">View/Manage Alerts</a></h2>
		<div class="overflow"><table id="alerttable">
		<tr class="table-row smart-table-header"><th>Type</th><th>Alert Id</th><th>Date</th><th>Description</th><th>Level</th><th>Status</th><th>Location</th><th>Action</th></tr>
		</table>
		</div> 
	</section>
	
	<div id="filter-controls">
			<a id="filter-link" onClick="hideShowFilters()"><image id="filter-button"/>Show Filters</a>

			<form id="filter-form" name="filter-form" onsubmit="return false;">

			<select id='filterDate' class='updateChange' name="time_filter">
			<option value=1>within 1 hour</option>
			<option value=2>within 2 hours</option>
			<option value=4>within 4 hours</option>
			<option value=8>within 8 hours</option>
			<option value=12>within 12 hours</option>
			<option value=24>within 24 hours</option>
			<option value=48>within 2 days</option>
			<option value=168>within a week</option>
			<option value=744>within a month</option>
			<option value=-99 selected>All dates</option>
			<option value=-1 selected>Custom Dates</option>
			</select>

			<br><input type="text" id="datePickerFrom" class="date-input">
			<font class="date-text">to </font><input type="text" id="datePickerTo" class="date-input">

			<p>Include Types:<br>
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<input class='filterType updateChange' name = "${type.getUuid()}" value="${type.getUuid()}" type="checkbox"> ${type.getLabel()} </input><br> 
			</c:forEach> 
			</p>
			
			<p>Status:<br>
			<c:forEach var="s" items="${status}" varStatus="count">
				<input class='filterStatus updateChange' name="${s}" value="${s}" type="checkbox" checked>${s}</input><br>
			</c:forEach>
			
			<p>Include Importance:<br>
			<input class='filterImportance updateChange' type="checkbox" name="level1" value=1 checked>1(Highest)</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level2" value=2 checked>2</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level3" value=3 checked>3</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level4" value=4 checked>4</input><br>
			<input class='filterImportance updateChange' type="checkbox" name="level5" value=5 checked>5(Lowest)</input><br>
			</p>
			<p>Include data from CA:<br>
			<c:forEach var="ca" items="${cas}" varStatus="count">
				<input class='filterCa updateChange' name="${ca.getUuid()}" value="${ca.getUuid()}" type="checkbox">${ca.getLabel()}</input><br>
			</c:forEach>
			</p>
			<p>
			Contains Text:<br>
			<input id='filterText' class='updateChange' name="textFilter" type="text"></input>
			</p> 
			</form>

	</div>
  </article>
	
</div>

<%@include file="footer.jsp" %>


<div id="updateAlertDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Update Alert</div>
  <div id="dialogerror" class="errorsection"></div>
  <div>Update Alert</div>
	<form id="updatealertform" name="updatealertform">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block">Conservation Area:</label>
     		<input type="hidden" name="uuid" value="" />
     		<input type="hidden" name="user_id" value="" />
     		<select name="update_alert_ca" class="block formtext alert-select">
     		<c:forEach var="ca" items="${cas}" varStatus="count">
     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
			</c:forEach> 
     		</select>
     		
     		<label class="top-spacer block">Type:</label>
     		<select name="update_alert_type" class="block formtext alert-select">
     		<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<option value="${type.getUuid()}"> ${type.getLabel()} </option> 
			</c:forEach> 
     		</select>

			<label class="top-spacer block">Event Importance:</label>
			<select name="update_level" class="block formtext alert-select">
			<option value=1>1(Highest)</option>
			<option value=2>2</option>
			<option value=3>3</option>
			<option value=4>4</option>
			<option value=5>5(Lowest)</option>
			</select>
			
			<label class="top-spacer block">Status:</label>
			<select name="update_status" class="block formtext alert-select">
			<option value="ACTIVE">ACTIVE</option>
			<option value="DISABLED">DISABLED</option>
			</select>
			
			<label class="top-spacer block">Longitude:</label><input id="long" type="text" name="update_long">
			<label class="top-spacer block">Latitude:</label><input id="lat" type="text" name="update_lat" >
			
			
			<label class="top-spacer block">Description:</label>
			<textarea name="update_alert_description" rows="5" cols="45"></textarea>
   			<div class="block top-spacer" style="text-align:right">
   			 <input class="button top-spacer" type="submit" value="   Update    "/>
   			 <input class="button" type="button" id="cancel" value="Cancel" />
   			 </div>
    	</form>
  </div>

</body>
</html>