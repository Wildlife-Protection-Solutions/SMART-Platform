<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>

<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/alert.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.3/leaflet.js"></script>
<script type="text/javascript" >
	var mobile="${mobile}";
	var tab = ${tab};
	
</script>
<script src="https://maps.google.com/maps/api/js?v=3.2&sensor=false"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/leaflet-google.js"></script>


<title>SMART Connect - Operational Map</title>
</head>
<body onLoad="initializePage()">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id= "main">
  <div class="pageheader">Alerts</div>
  <article id="tabs" class="tabs">

	<section id="tab1" class="">
		<h2 id="tab1text" class=""><a onclick="settab(1)" href="#tab1">Operational Map</a></h2>
		<div id="map"></div>  
	</section>
	
	<section id="tab2" class="">
		<h2 id="tab2text" class=""><a onclick="settab(2)" href="#tab2">Create New Alert</a></h2>
		<p>
		<form action="${createAlert}" method="POST" id="newAlertForm">
     		<div id="error" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block">Conservation Area:</label>
     		<input type="text" name="j_ca" class="block formtext" />
     		<label class="top-spacer block">Type:</label>
     		<input type="password" name="j_type" class="formtext" />
     		<input class="button block top-spacer" type="submit" value="Submit"/>
    	</form>
		</p>
	</section>
	
	<section id="tab3" class="">
		<h2 id="tab3text" class=" "><a onclick="settab(3)" href="#tab3">View/Manage Alerts</a></h2>
		<p>This content appears on tab 3.</p>
	</section>

  </article>
	
</div>

<%@include file="footer.jsp" %>
</body>
</html>