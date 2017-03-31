<html>
<head>
	<%@include file="includes.jsp" %>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dashboardsharedfunctions.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dashboardbetaadmin.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
	<title><fmt:message key="home.pagetitle"/></title>
	
</head>

<body style="${style_bodycss}">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id= "main">
	<div id="pageheader" class="pageheader"><fmt:message key="dashboardbeta.title"/></div>
	<div id="dashboardLabeldiv">Dashboard Name:<Input id="dashboardlabeltext" type="text" value="New Dashboard"/>
		<input type="hidden" id="dashboarduuid" name="reportuuid"/>
	</div>
	<div id="report-wrapper">
    	<div id="report1">
    	<form id="report1form" name="report1form">
    		<input id="report1selecthidden" type="hidden"/>
    		<select id='report1select' class='updateChange' name="report1" style="margin-bottom:3px"> 
    		<option value=0>Select the First Report Here</option>
    		</select>
    		<select id='filterDate1' class='updateChange' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="sharedlinks.oneday"/></option>
				<option value=7><fmt:message key="sharedlinks.oneweek"/></option>
				<option value=30><fmt:message key="sharedlinks.onemonth"/></option>
				<option value=180><fmt:message key="sharedlinks.sixmonths"/></option>
				<option value=365><fmt:message key="sharedlinks.oneyear"/></option>
				<option value=-1><fmt:message key="sharedlinks.custom"/></option>
			</select><br>
    		<input disabled type="text" name="report1From" id="report1From" class="date-input-center"> -
    		<input disabled type="text" name="report1To" id="report1To" class="date-input-center">
    		<div id="customparameters1"></div>
		</form>
      	<iframe id="iframe1" src="" width="100%" height="500" frameborder="0" allowfullscreen sandbox="allow-scripts">
  			<p> <a id=link1' href="">
    		Fallback link for browsers that don't support iframes
			</a></p>
		</iframe>
    	</div>
    	<div id="report2">
    	<form id="report2form" name="report2form">
    		<input id="report2selecthidden" type="hidden"/>
    		<select id='report2select' class='updateChange' name="report2" style="margin-bottom:3px"> 
    		<option value=0>Select the Second Report Here</option>
    		</select>
    		<select id='filterDate2' class='updateChange' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="sharedlinks.oneday"/></option>
				<option value=7><fmt:message key="sharedlinks.oneweek"/></option>
				<option value=30><fmt:message key="sharedlinks.onemonth"/></option>
				<option value=180><fmt:message key="sharedlinks.sixmonths"/></option>
				<option value=365><fmt:message key="sharedlinks.oneyear"/></option>
				<option value=-1><fmt:message key="sharedlinks.custom"/></option>
			</select><br>
			<input disabled type="text" name="report2From" id="report2From" class="date-input-center" > - 
    		<input disabled type="text" name="report2To" id="report2To" class="date-input-center">
    		<div id="customparameters2"></div>
		</form>
    	
      	<iframe id="iframe2" src="" width="100%" height="500" frameborder="0" allowfullscreen sandbox="allow-scripts">
  			<p> <a id="link2" href="">
    		Fallback link for browsers that don't support iframes
			</a></p>
		</iframe>

    	</div>
  	</div>
  	<div id="configuration">
  		<form>
  			<p id="adminoptions"><button id="saveButton" onClick="saveOrEditDashboard(); return false;" class="button">Create New Dashboard</button></p>
  		</form>
  	</div>
	</div>
<%@include file="footer.jsp" %>

<div id="errorDialog" style="display: none;" class="dialog">
	<div id="errorText"></div>
	<button id="errorOKButton" class="button" onClick="closeDialog('errorDialog');">OK</button>
</div>

</body>
</html>