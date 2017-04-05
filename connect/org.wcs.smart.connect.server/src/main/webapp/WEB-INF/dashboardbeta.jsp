<html>
<head>
	<%@include file="includes.jsp" %>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dashboardsharedfunctions.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dashboardbeta.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/pickaday.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
	<title><fmt:message key="home.pagetitle"/></title>
	
</head>

<body style="${style_bodycss}">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id= "main">
	<div id="pageheader" class="pageheader"><fmt:message key="dashboardbeta.title"/></div>
	<div><div id="message" class="msgsection"></div></div>
	<div id="report-wrapper">
    	<div id="report1">
    	<form id="report1form" name="report1form">
    		<input id="report1selecthidden" type="hidden"/>
    		<input id="report1paramshidden" type="hidden"/>
    		<select id='filterDate1' class='updateChange' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
			</select>
			<button class="button" onClick="rerunReport1(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
			<button id="savedatebutton1" class="button" onClick="updateDateOnReport1(); return false;" disabled><fmt:message key="dashboardbeta.datedefault"/></button>
			<br>
    		<input type="text" id="report1From" class="date-input-center"> -
    		<input type="text" id="report1To" class="date-input-center">
		</form>
		<img id="loading1" src="../css/images/loading.svg">
      	<div id="iframe1div"></div>
    	</div>
    	
    	<div id="report2">
    	<form id="report2form" name="report2form">
    		<input id="report2selecthidden" type="hidden"/>
    		<input id="report2paramshidden" type="hidden"/>
    		<select id='filterDate2' class='updateChange' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
			</select>
			<button class="button" onClick="rerunReport2(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
			<button id="savedatebutton2" class="button" onClick="updateDateOnReport2(); return false;" disabled><fmt:message key="dashboardbeta.datedefault"/></button>
			<br>
			<input type="text" id="report2From" class="date-input-center"> - 
    		<input type="text" id="report2To" class="date-input-center">
		</form>
    	<img id="loading2" src="../css/images/loading.svg">
      	<div id="iframe2div"></div>
		</iframe>

    	</div>
  	</div>
  	<div id="configuration">
  		<form>
  			<p id="dashboardList" >
  				<select id="admin-selectlist" name="admin-selectlist"></select>
  				<button onClick="showDashboard(); return false;" class="button"> <fmt:message key="dashboardbeta.viewdashboard"/></button> 
  				<button onClick="setDashboardDefaultToSelectedDashboard(); return false;" class="button" > <fmt:message key="dashboardbeta.saveasdefaultdashboard"/></button>
  			</p>
  			<p id="adminoptions" style="display:none">
  				<button onClick="editDashboard(); return false;" class="button"><fmt:message key="dashboardbeta.editdashboard"/></button> 
	  			<button onClick="deleteDashboard(); return false;" class="button"><fmt:message key="dashboardbeta.deletedashboard"/></button>
	  			<button onClick="createNewDashboard(); return false;" class="button"><fmt:message key="dashboardbeta.createdashboard"/></button> 
  			</p>
  		</form>
  	</div>
	</div>
<%@include file="footer.jsp" %>

<div id="errorDialog" style="display: none;" class="dialog">
	<div id="errorText"></div>
	<button id="errorOKButton" class="button" onClick="closeDialog('errorDialog');"><fmt:message key="dashboardbeta.ok"/></button>
</div>


</body>
</html>