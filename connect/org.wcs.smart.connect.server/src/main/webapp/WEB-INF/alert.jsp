<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>

<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/alert.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.js"></script>
<script type="text/javascript" >
	var mobile="${mobile}";
	var tab = ${tab};
</script>
<script src="https://maps.google.com/maps/api/js?v=3.2&sensor=false"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-google.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-realtime.js"></script>


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
		<div id="map"></div>  
	</section>
	
	<section id="tab2" class="">
		<h2 id="tab2text" class=""><a onclick="settab(2)">Create New Alert</a></h2>
		<p>
		<form id="newalertform">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block">Conservation Area:</label>
     		<select name="alert_ca" class="block formtext">
     		<c:forEach var="ca" items="${cas}" varStatus="count">
     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
			</c:forEach> 
     		</select>
     		
     		<label class="top-spacer block">Type:</label>
     		<select name="alert_type" class="block formtext">
     		<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<option value="${type.getUuid()}"> ${type.getLabel()} </option> 
			</c:forEach> 
     		</select>

			<label class="top-spacer block">Event Importance:</label>
			<select name="level" class="block formtext">
			<option value=1>1(Highest)</option>
			<option value=2>2</option>
			<option value=3>3</option>
			<option value=4>4</option>
			<option value=5>5(Lowest)</option>
			</select>
			
			<label class="top-spacer block">Longitude:</label><input id="long" type="text" name="long">
			<label class="top-spacer block">Latitude:</label><input id="lat" type="text" name="lat" >
			
			
			<label class="top-spacer block">Description:</label>
			<textarea name="alert_description" rows="5" cols="30"></textarea>
   			<input class="button block top-spacer" type="submit" value="   Submit    "/>
    	</form>
		</p>
	</section>
	
	<section id="tab3" class="">
		<h2 id="tab3text" class=" "><a onclick="settab(3)">View/Manage Alerts</a></h2>
		<table id="alerttable">
		<tr class="table-row smart-table-header"><th>Alert Id</th><th>Date</th><th>Description</th><th>Level</th><th>Status</th><th>Location</th><th>Action</th></tr>
		</table> 
	</section>

  </article>
	
</div>

<%@include file="footer.jsp" %>
</body>
</html>