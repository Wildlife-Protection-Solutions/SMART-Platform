<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" >
	var numStyles=${numstyles};
</script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/settings.js"></script>

<title>SMART Connect - Settings</title>
</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
  <div class="pageheader">SMART Connect Settings</div>
  <p class="infomessage">Manage SMART Connect Settings</p>
  <div>
    <div id="message" class="msgsection"></div>
    <div id="error" class="errorsection"></div>
<!--     	<button id="btnNewStyle" class="block button top-spacer">Create New Style</button> -->
    </div>


<div class="overflow settingsTable">
	<div class="block" style="text-align:left"><b>Operational Map Layers Configuration</b> <button class="button top-spacer" id="btnNewLayer">Add New Layer</button>
	</div>
	<table id="layertable">
		<tr class="table-row smart-table-header"><th>Layer Order</th><th>Layer Name</th><th>Type</th><th>On by Default?</th><th>Mapbox ID</th><th>Layer List</th><th>Token</th><th>Actions</th>
		</tr>
	</table>
</div>


<div class="overflow settingsTable">
	<div class="block" style="text-align:left"><b>Alert Types and Styles</b> <button class="button top-spacer" id="btnNewType">Add New Alert Type</button>
	</div>
	<table id="typetable">
		<tr class="table-type-row smart-table-header"><th>Alert Type</th><th>Outline Color(#RRGGBB)</th><th>Fill Color(#RRGGBB)</th><th>Opacity(0-1)</th><th>Actions</th>
		</tr>
	</table>
</div>

<div class="overflow settingsTable">
	<div class="block" style="text-align:left"><b>Default Filter Settings</b>
	</div>
	<table id="defaultstable">
	<form id="filter-form" name="filter-form" onsubmit="return false;">
	<input id="filter_uuid" type="hidden" name="uuid" />
		
		
		<tr class="table-defaults-row smart-table-header"><th>Date/Time</th><th>Alert Types</th><th>Alert Status</th><th>Alert Level</th><th>CAs to Include</th><th>Text Filter</th><th>Actions</th>
		</tr>
		<tr class="smart-table-rowon">
		<td>


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
			<option value=-99>All dates</option>
			</select>
		</td>
		<td>

			<p>Include Types:<br>
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<input id= "${type.getUuid()}" class='filterType updateChange' name = "${type.getUuid()}" value="${type.getUuid()}" type="checkbox"> ${type.getLabel()} </input><br> 
			</c:forEach> 
			</p>
		</td>
		<td>
			<p>Status:<br>
			<c:forEach var="s" items="${status}" varStatus="count">
				<input id="status_${s}" class='filterStatus updateChange' name="${s}" value="${s}" type="checkbox">${s}</input><br>
			</c:forEach>
		</td>
		<td>
			<p>Include Importance:<br>
			<input id="level1" class='filterImportance updateChange' type="checkbox" name="level1" value=1 >1(Highest)</input><br>
			<input id="level2" class='filterImportance updateChange' type="checkbox" name="level2" value=2 >2</input><br>
			<input id="level3" class='filterImportance updateChange' type="checkbox" name="level3" value=3 >3</input><br>
			<input id="level4" class='filterImportance updateChange' type="checkbox" name="level4" value=4 >4</input><br>
			<input id="level5" class='filterImportance updateChange' type="checkbox" name="level5" value=5 >5(Lowest)</input><br>
			</p>
			
		</td>
		<td>
			<p>Include data from CA:<br>
			<c:forEach var="ca" items="${cas}" varStatus="count">
				<input id="${ca.getUuid()}" class='filterCa updateChange' name="${ca.getUuid()}" value="${ca.getUuid()}" type="checkbox">${ca.getLabel()}</input><br>
			</c:forEach>
			</p>
			<p>
		</td>
		<td>
			Contains Text:<br>
			<input id='filterText' class='updateChange' name="textFilter" type="text"></input>
			</p> 

			
		</td>
		<td>
			Events Layer refreshes every:<br>
			<input id='secondsRefresh' class='updateChange' style='width:3.5em' 'name="secondsRefresh" type="number"></input> seconds
			</p> 

		<button class="button top-spacer" id="btnUpdateDefaults">Save Updated Defaults</button>
<!-- 		<button class="button top-spacer" id="btnResetDefaults">Reload Defaults</button> -->

		</td>
		</tr>
	</table>
	</form>
</div>
  
<!-- <div class="top-spacer"  style="margin-left: -20px" > -->
<!--   <div class="styleTable table-cell smart-table"> -->
<!--   	<div class="table-row smart-table-header"> -->
<!-- 			<div class="table-cell smart-table-cell">Style Id</div> -->
<!-- 			<div class="table-cell smart-table-cell">Active?</div> -->
<!-- 			<div class="table-cell smart-table-cell">Server Name</div> -->
<!-- 			<div class="table-cell smart-table-cell">Footer Text</div> -->
<!-- 			<div class="table-cell smart-table-cell">Actions</div> -->
<!-- 		</div> -->
<%-- 	<c:forEach var="style" items="${styles}" varStatus="count"> --%>
<%-- 		<div data-username ="${style.getStyleId()}" class="smartuser styleRow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}"> --%>
<%-- 			<div class="table-cell smart-table-cell">${style.getStyleId()}</div> --%>
<%-- 			<div class="table-cell smart-table-cell">${style.getActive()}</div> --%>
<%-- 			<div class="table-cell smart-table-cell">${style.getServerName()}</div> --%>
<%-- 			<div class="table-cell smart-table-cell">${style.getFooterText()}</div> --%>
<%-- 			<div class="table-cell smart-table-cell "><a href="" data-id = "${style.getStyleId()}" title="delete style" class="deleteuser delete-icon"></a></div> --%>
<!-- 		</div> -->
<%-- 	</c:forEach>   --%>
<!--   </div> -->


</div>

</div>
<%@include file="footer.jsp" %>


<div id="newStyleDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Create a New Style</div>
  <div id="dialogerror" class="errorsection"></div>
  <div>Create a new SMART Connect Server Style</div>
  <form id="newstyleform" action="settings" method="POST" enctype="multipart/form-data">
    <label class="block top-spacer">Style Id:</label>
    <input type="text" name="style_id" class="formtext block" />
    <label class="block top-spacer">Background Image:</label>
    <input type="file" name="bg_image" class="formtext block" />
     <input class="button" type="submit" value="Create Style" />
     <input class="button" type="button" id="cancelNewStyle" value="Cancel" />
    </div>
  </form>
  </div>


<div id="layerDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Map Layer Details</div>
  <div id="layerdialogerror" class="errorsection"></div>
	<form id="maplayersform">
     		<div id="layererror" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block">Layer Order:</label>
     		<input class="layer_order" type=number name="layer_order"/>
     		
     		<label class="top-spacer block">Layer Name:</label>
     		<input class="layer_field" type=text name="layer_name" value="" maxlength="32"/>
     		
     		
     		<input type="hidden" name="uuid" value="" />
     		
     		
     		<label class="top-spacer block">Layer Type:</label>
			<select name="layer_type" class="block formtext alert-select">
			<option value=1>Mapbox.com</option>
			<option value=2>GisCloud.com</option>
			<option value=3>WMS</option>
			</select>
     		
     		<label class="top-spacer block">On By Default?:</label>
     		<select name="layer_status" class="block formtext alert-select">
			<option value="true">True</option>
			<option value="false">False</option>
			</select>
     		
     		<label class="top-spacer block">Service Token or WMS URL (xyz.com/wms?):</label>
     		<input class="layer_field" type=text name="layer_token" value="" maxlength="256"/>
 		
     		<label class="top-spacer block">MapBox ID (mapbox only):</label>
     		<input class="layer_field" type=text name="layer_mapbox_id" value="" maxlength="64"/>
     		
     		<label class="top-spacer block">WMS Layer List (GISCloud & WMS only):</label>
     		<input class="layer_field" type=text name="layer_list" value=""/>
     		<div class="top-spacer block">
     			<input id="newLayerButton" class="button" type="button" value="CreateLayer" />
     			<input id="updateLayerButton" class="button" type="button" value="Update Layer" />
     			<input class="button" type="button" id="cancelLayer" value="Cancel" />
     		</div>
    	</form>
  </div>



<div id="typeDialog" style="display: none;" class="dialog">
  <div class="type-title">Alert Type</div>
  <div id="layerdialogerror" class="errorsection"></div>
	<form id="alerttypesform">
     		<div id="layererror" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block">Type Name:</label>
     		<input class="type_field" type=text name="type_label" value="" maxlength="32"/>
     		
     		<input type="hidden" name="uuid" value="" />
     		
     		
     		<label class="top-spacer block">Outline Color:</label>
			<input class="type_field" type=text name="type_color" value="" maxlength="16"/>
    
      		<label class="top-spacer block">Fill Color:</label>
			<input class="type_field" type=text name="type_fillcolor" value="" maxlength="16"/>
    
      		<label class="top-spacer block">Opacity:</label>
			<input class="type_field" type=text name="type_opacity" value="" maxlength="8"/>
     		
       		<div class="top-spacer block">
     			<input id="newTypeButton" class="button" type="button" value="Create Type" />
     			<input id="updateTypeButton" class="button" type="button" value="Update Type" />
     			<input class="button" type="button" id="cancelType" value="Cancel" />
     		</div>
    	</form>
  </div>

</body>
</html>