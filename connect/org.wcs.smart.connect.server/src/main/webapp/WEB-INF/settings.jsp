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
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/leaflet/leaflet.awesome-markers.css">
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/settings.js"></script>

<title><fmt:message key="settings.title" /></title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
  <div class="pageheader"><fmt:message key="settings.pageheader" /></div>
  <div>
    <div id="message" class="msgsection"></div>
   </div>


<div class="overflow settingsTable">

	<div class="block settings-header"><fmt:message key="settings.layerheader" /></div>
	<button class="button top-spacer" id="btnNewLayer"><fmt:message key="settings.newlayerbutton" /></button>
	
	<table id="layertable"  class="top-spacer smart-table" style="width:100%">
		<tr class="table-row smart-table-header">
		  <th><fmt:message key="settings.layerorder" /></th>
		  <th><fmt:message key="settings.layername" /></th>
		  <th><fmt:message key="settings.type" /></th>
		  <th><fmt:message key="settings.onbydefault" /></th>
		  <th><fmt:message key="settings.token" /></th>
		  <th><fmt:message key="settings.layerlist" /></th>		  
		  <th><fmt:message key="actions" /></th>
		</tr>
	</table>
</div>


<div class="overflow settingsTable">
	<div class="block settings-header"><fmt:message key="settings.styleheader" /></div> 
	<button class="button top-spacer" id="btnNewType"><fmt:message key="settings.addnewstyle" /></button>
	
	<table id="typetable" class="top-spacer smart-table">
		<tr class="table-type-row smart-table-header"><th><fmt:message key="settings.alerttype" /></th>
				<th><fmt:message key="settings.outlinecolor" /></th>
				<th><fmt:message key="settings.opacity" /></th>
				<th><fmt:message key="settings.markerIcon" /></th>
				<th><fmt:message key="settings.markerColor" /></th>
				<th><fmt:message key="settings.iconSpin" /></th>
				<th><fmt:message key="actions" /></th>
		</tr>
	</table>
</div>

<div class="overflow settingsTable">
	<div class="block settings-header"><fmt:message key="settings.defaultsheader" />
	</div>
	<form class="top-spacer" id="filter-form" name="filter-form" onsubmit="return false;">
	<table id="defaultstable" class="top-spacer smart-table" style="width:100%">
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
		<td class="td-align-top">
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
		<td class="td-align-top">
			<p><fmt:message key="alert.filters.types" /><br>
			<c:forEach var="type" items="${alertTypes}" varStatus="count">
     			<label><input id= "${type.getUuid()}" class='filterType updateChange' value="${type.getUuid()}" type="checkbox"/>${type.getLabel()}</label><br> 
			</c:forEach> 
			</p>
		</td>
		<td class="td-align-top" >
			<p><fmt:message key="alert.filters.status" /><br>
			<c:forEach var="s" items="${status}" varStatus="count">
				<label><input id="status_${s[0]}" class='filterStatus updateChange' value="${s[0]}" type="checkbox"/>${s[1]}</label><br>
			</c:forEach>
		</td>
		<td class="td-align-top">
			<p><fmt:message key="alert.filters.importance" /><br>
				<label><input id="level1" class='filterImportance updateChange' type="checkbox" value=1/><fmt:message key="alert.eventimportance1"/></label><br>
				<label><input id="level2" class='filterImportance updateChange' type="checkbox" value=2/><fmt:message key="alert.eventimportance2"/></label><br>
				<label><input id="level3" class='filterImportance updateChange' type="checkbox" value=3/><fmt:message key="alert.eventimportance3"/></label><br>
				<label><input id="level4" class='filterImportance updateChange' type="checkbox" value=4/><fmt:message key="alert.eventimportance4"/></label><br>
				<label><input id="level5" class='filterImportance updateChange' type="checkbox" value=5/><fmt:message key="alert.eventimportance5"/></label><br>
			</p>
			
		</td>
		<td class="td-align-top">
			<p><fmt:message key="alert.filters.ca" /><br>
			<c:forEach var="ca" items="${cas}" varStatus="count">
				<label><input id="${ca.getUuid()}" class='filterCa updateChange' value="${ca.getUuid()}" type="checkbox"/>${ca.getLabel()}</label><br>
			</c:forEach>
			</p>
			<p>
		</td>
		<td class="td-align-top">
			<fmt:message key="alert.filters.text" /><br>
			<input id='filterText' class='updateChange' style='width:7em' name="textFilter" type="text"></input>
		</td>
		<td class="td-align-top">
			<font class="defaultLabel"><fmt:message key="settings.refresh" /> </font> <input id='secondsRefresh' class='updateChange' style='width:3.5em' name="secondsRefresh" type="number" min=5/><br>
			<font class="defaultLabel"><fmt:message key="settings.startingzoom" /></font> <input id='startingZoom' class='updateChange' style='width:3.5em' name="startingZoom" type="number" min=1 max=12/> <br>
			<font class="defaultLabel"><fmt:message key="settings.startinglong" /></font> <input id='startingLong' class='updateChange' style='width:3.5em' name="startingLat" type="text" /> <br>
			<font class="defaultLabel"><fmt:message key="settings.startinglat" /></font> <input id='startingLat' class='updateChange' style='width:3.5em' name="startingLong" type="text" /> <br>
		</td>
		</tr>
	</table>
	<button class="button " id="btnUpdateDefaults"><fmt:message key="settings.savedefaults" /></button>
	</form>
</div>


<div class="overflow settingsTable">
	<div class="block settings-header"><fmt:message key="settings.styleconfigurationheader" /></div>
	<button class="button top-spacer" id="btnNewStyleConfiguration"><fmt:message key="settings.addnewstyleconfiguration" /></button>
	<table id="styletable" class="top-spacer smart-table">
  	<tr class="table-row smart-table-header">
			<th class="table-cell smart-table-cell"><fmt:message key="settings.style.servername" /></th>
			<th class="table-cell smart-table-cell"><fmt:message key="settings.style.footertext" /></th>
			<th class="table-cell smart-table-cell"><fmt:message key="settings.style.headerstyle" /></th>
			<th class="table-cell smart-table-cell"><fmt:message key="settings.style.bodystyle" /></th>
			<th class="table-cell smart-table-cell"><fmt:message key="settings.style.actions" /></th>
	</tr>
	</table>

</div>
</div>
<%@include file="footer.jsp" %>

<!-- dialogs                  -------------------              -->

<div id="newStyleDialog" style="display: none; width:400px;" class="dialog">
  <div class="dialog-title"><fmt:message key="settings.style.newstyle" /></div>
  <div id="dialogerror" class="errorsection"></div>
  <form id="newstyleform" method="POST" enctype="multipart/form-data">
  
    <label class="block top-spacer"><fmt:message key="settings.style.servername" /></label>
	<input class="formtext" id="server_name" name="server_name" type="text" value="SMART Connect <img src='https://smartconservationsoftware.org/photos/smart_logo_96.png'>"/>
	
    <label class="block top-spacer"><fmt:message key="settings.style.footertext" /></label>
	<input class="formtext" id="footer_text" name="footer_text" type="text" value="<img src='${pageContext.request.contextPath}/css/images/smart_logo.png'> Copyright 2015, 2016"/>
  
    <label class="block top-spacer"><fmt:message key="settings.style.headerstyle" /></label>
    <textarea id="header_style" name="header_style" class="formtext block" rows=5>border: 0px solid black;
padding: 10px 5px 10px 5px;
text-align: center;
color: black;
font-family: 'Allerta', Helvetica, Arial, sans-serif;
font-size: 40px;</textarea>
    
	<label class="block top-spacer"><fmt:message key="settings.style.bodystyle" /></label>
    <textarea id="body_style" name="body_style" class="formtext block" rows=5>margin: 0px; background-color: lightsteelblue</textarea>
    
    <label class="block top-spacer"><fmt:message key="settings.style.titlebackgroundimage" /></label>
    <input id="header_image" type="file" name="header_image" class="formtext block" />
    
    <label class="block top-spacer"><fmt:message key="settings.style.maincontentimage" /></label>
    <input id="bg_image" type="file" name="bg_image" class="formtext block" />
    
    <label class="block top-spacer"><fmt:message key="settings.style.loginpageimage" /></label>
    <input id="login_image" type="file" name="login_image" class="formtext block" />
    
     <input class="button top-spacer" type="submit" id="btnNewStyle" value="<fmt:message key="settings.style.createnewstyle" />" />
     <input class="button top-spacer" type="button" id="cancelNewStyle" value="<fmt:message key="settings.style.cancel" />" />
  </form>
  </div>


<div id="updateStyleDialog" style="display: none;width:400px;" class="dialog">
  <div class="dialog-title">Update Style</div>
  <div id="dialogerror" class="errorsection"></div>
  <form id="updatestyleform" action="settings" method="POST" enctype="multipart/form-data">

    <label class="block top-spacer"><fmt:message key="settings.style.servername" /></label>
	<input class="formtext" id="server_name" name="server_name" type="text" value="SMART Connect"/>
    
    <label class="block top-spacer"><fmt:message key="settings.style.footertext" /></label>
	<input class="formtext" id="footer_text" name="footer_text" type="text" value="Copyright 2015, 2016"/>

    <label class="block top-spacer"><fmt:message key="settings.style.headerstyle" /></label>
    <textarea id="header_style" name="header_style" class="formtext block" rows=5>
    </textarea>

    <label class="block top-spacer"><fmt:message key="settings.style.bodystyle" /></label>
    <textarea id="body_style" name="body_style" class="formtext block" rows=5>
    </textarea>
   
    <label class="block top-spacer"><fmt:message key="settings.style.titlebackgroundimage" /></label>
    <input id="header_image" type="file" name="header_image" class="formtext block" />
    
    <label class="block top-spacer"><fmt:message key="settings.style.maincontentimage" /></label>
    <input id="bg_image" type="file" name="bg_image" class="formtext block" />
    
    <label class="block top-spacer"><fmt:message key="settings.style.loginpageimage" /></label>
    <input id="login_image" type="file" name="login_image" class="formtext block" />
    
     <input class="button top-spacer" type="submit" id="btnUpdateStyle" value="<fmt:message key="settings.style.updatestyle" />" />
     <input class="button top-spacer" type="button" id="cancelUpdateStyle" value="<fmt:message key="settings.style.cancel" />" />
   
  </form>
  </div>

  

<div id="layerDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="settings.layeredit.title" /></div>
  <div id="layerdialogerror" class="errorsection"></div>
	<form id="maplayersform">
     		<div id="layererror" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.orderlabel" /></label>
     		<input class="formtext" type=number name="layer_order"/>
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.namelabel" /></label>
     		<input class="formtext" type=text name="layer_name" value="" maxlength="32"/>
     		
     		<input type="hidden" name="uuid" value="" />
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.onbydefaultlabel" /></label>
     		<select name="layer_status" class="block formtext">
			<option value="true"><fmt:message key="true" /></option>
			<option value="false"><fmt:message key="false" /></option>
			</select>
			
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.typelabel" /></label>
			<select name="layer_type" class="block formtext">
			<option value="WMS"><fmt:message key="settings.layeredit.wms" /></option>
			</select>
     		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.tokenorurl" /></label>
     		<input class="formtext" type=text name="layer_token" value="" maxlength="256"/>
 		
     		<label class="top-spacer block"><fmt:message key="settings.layeredit.layerlist" /></label>
     		<input class="formtext" type=text name="layer_list" value=""/>
     		
     		<div class="top-spacer block" style="text-align: right">
     			<input id="newLayerButton" class="button" type="button" value="<fmt:message key="settings.createlayerbutton"/>" />
     			<input id="updateLayerButton" class="button" type="button" value="<fmt:message key="settings.updatelayerbutton"/>" />
     			<input class="button" type="button" id="cancelLayer" value="<fmt:message key="settings.cancel"/>" />
     		</div>
    	</form>
  </div>



<div id="typeDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="settings.typeedit.title" /></div>
  <div id="typedialogerror" class="errorsection"></div>
	<form id="alerttypesform">
     		<div id="layererror" class="errorsection" style="display: ${alerterror == null ? "none" : "block"}">${alerterror}</div>
     		<label class="top-spacer block"><fmt:message key="settings.typeedit.typelabel" /></label>
     		<input class="formtext" type=text name="type_label" value="" maxlength="32"/>
     		
     		<input type="hidden" name="uuid" value="" />
     		
     		<label class="top-spacer block"><fmt:message key="settings.typeedit.outlinecolorlabel" /></label>
			<input id="type_color" class=" formtext type_field jscolor" type=text name="type_color" value="" maxlength="16"/>

      		<label class="top-spacer block"><fmt:message key="settings.typeedit.opactiylabel" /></label>
			<input class="formtext" type=text name="type_opacity" value="" maxlength="8"/>

      		<label class="top-spacer block"><fmt:message key="settings.markerIcon" />: <i id="exampleIcon" class=""></i></label> 
      		<span>
			<select id="type_markerIcon" class="formtext" style="width: auto !important" name="type_markerIcon">
		<!-- selected from:		http://fortawesome.github.io/Font-Awesome/icons/ -->				
			</select> <fmt:message key="settings.oroneof"/> 
			<a title="<fmt:message key="settings.iconHover" />" target="_blank" href="http://fortawesome.github.io/Font-Awesome/icons/">
			(<fmt:message key="settings.list" />)</a>:
			<input class="formtext" type="text" name="iconOveride" id="iconOveride" style="width: auto !important" value=""/>
			</span>

      		<label class="top-spacer block"><fmt:message key="settings.markerColor"/>:</label>
			<select class="formtext" name="type_markerColor">
				<option value="white"><fmt:message key="settings.colorwhite"/></option>
				<option value="black"><fmt:message key="settings.colorblack"/></option>
				<option value="gray"><fmt:message key="settings.colorgray"/></option>
				<option value="lightgray"><fmt:message key="settings.colorlightgray"/></option>
				<option value="red"><fmt:message key="settings.colorred"/></option>
				<option value="darkred"><fmt:message key="settings.colordarkred"/></option>
				<option value="orange"><fmt:message key="settings.colororange"/></option>
				<option value="lightgreen"><fmt:message key="settings.colorlightgreen"/></option>
				<option value="green"><fmt:message key="settings.colorgreen"/></option>
				<option value="darkgreen"><fmt:message key="settings.colordarkgreen"/></option>
				<option value="lightblue"><fmt:message key="settings.colorlightblue"/></option>
				<option value="blue"><fmt:message key="settings.colorblue"/></option>
				<option value="darkblue"><fmt:message key="settings.colordarkblue"/></option>
				<option value="cadetblue"><fmt:message key="settings.colorcadetblue"/></option>
				<option value="purple"><fmt:message key="settings.colorpurple"/></option>
				<option value="darkpurple"><fmt:message key="settings.colordarkpurple"/></option>
				<option value="pink"><fmt:message key="settings.colorpink"/></option>
				
			</select>

			<label class="top-spacer block"><fmt:message key="settings.iconSpin"/>:</label>
     		<select class="formtext" name="type_spin">
     			<option value="true"><fmt:message key="settings.true"/></option>
     			<option value="false"><fmt:message key="settings.false"/></option>
     		</select>
       		<div class="top-spacer block" style="text-align: right">
     			<input id="newTypeButton" class="button" type="button" value="<fmt:message key="settings.newtypebutton"/>" />
     			<input id="updateTypeButton" class="button" type="button" value="<fmt:message key="settings.updatetypebutton"/>" />
     			<input class="button" type="button" id="cancelType" value="<fmt:message key="settings.cancel"/>" />
     		</div>
    	</form>
  </div>
<br><br>Icon Credits:<br>
<div>Hamburger menu by <a href="https://www.iconfinder.com/icons/134216/hamburger_lines_menu_icon">Timothy Miller</a> under <a href="https://creativecommons.org/licenses/by-sa/3.0/legalcode">creative commons SA-3.0 license</a>, no changes were made.</div>
<div>Other menu icons made by <a href="https://www.flaticon.com/authors/gregor-cresnar" title="Gregor Cresnar">Gregor Cresnar</a> from <a href="https://www.flaticon.com/" title="Flaticon">www.flaticon.com</a> is licensed by <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0" target="_blank">CC 3.0 BY</a></div>

</body>
</html>