<html>
<head>
	<%@include file="includes.jsp" %>
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/pikaday.css" />
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dashboardsharedfunctions.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dashboardbetaadmin.js"></script>
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
	<div id="dashboardLabeldiv">Dashboard Name:<Input id="dashboardlabeltext" type="text" value="New Dashboard"/>
		<input type="hidden" id="dashboarduuid" name="reportuuid"/>
	</div>
	<div id="report-wrapper">
    	<div id="report1">
    	<form id="report1form" name="report1form">
    		<input id="report1selecthidden" type="hidden"/>
    		<input id="report1paramshidden" type="hidden"/>
    		<input id="report1isccaa" type="hidden"/>
    		<select id='report1select' class='updateChange' name="report1" style="margin-bottom:3px"> 
    		<option value=0>Select the First Report Here</option>
    		</select>
    		<select id='filterDate1' class='updateChange' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
			</select>
			<button class="button" onClick="rerunReport1(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
			<button id="editCasButton1" onclick="editcas1(); return false" class="button" style="display:none"><fmt:message key="dashboardbeta.editcas"/></button>
			<br>
    		<input disabled type="text" name="report1From" id="report1From" class="date-input-center"> -
    		<input disabled type="text" name="report1To" id="report1To" class="date-input-center">
    		<div id="customparameters1"></div>
		</form>
		<img id="loading1" src="../css/images/loading.svg">
      	<div id="iframe1div"></div>
    	</div>
    	<div id="report2">
    	<form id="report2form" name="report2form">
    		<input id="report2selecthidden" type="hidden"/>
    		<input id="report2paramshidden" type="hidden"/>
    		<input id="report2isccaa" type="hidden"/>
    		<select id='report2select' class='updateChange' name="report2" style="margin-bottom:3px"> 
    		<option value=0>Select the Second Report Here</option>
    		</select>
    		<select id='filterDate2' class='updateChange' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
			</select>
			<button class="button" onClick="rerunReport2(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
			<button id="editCasButton2" onClick="editcas2(); return false;" class="button" style="display:none"><fmt:message key="dashboardbeta.editcas"/></button>
			<br>
			<input disabled type="text" name="report2From" id="report2From" class="date-input-center" > - 
    		<input disabled type="text" name="report2To" id="report2To" class="date-input-center">
    		<div id="customparameters2"></div>
		</form>
    	<img id="loading2" src="../css/images/loading.svg">
      	<div id="iframe2div"></div>

    	</div>
  	</div>
  	<div id="configuration">
  		<form>
  			<p id="adminoptions"><button id="saveButton" onClick="saveOrEditDashboard(); return false;" class="button"><fmt:message key="dashboardbeta.createdashboard"/></button></p>
  		</form>
  	</div>
	</div>
<%@include file="footer.jsp" %>

<div id="errorDialog" style="display: none;" class="dialog">
	<div id="errorText"></div>
	<button id="errorOKButton" class="button" onClick="closeDialog('errorDialog');"><fmt:message key="dashboardbeta.ok"/></button>
</div>

<div id="caSelector1" style="display: none;" class="dialog">
	<div class="dialog-title"><fmt:message key="dashboardbeta.leftsidecas"/></div>
	<p><a href="javascript:selectAllCas1();"><fmt:message key="dashboardbeta.selectall"/></a>
		<a href="javascript:selectNoneCas1();"><fmt:message key="dashboardbeta.selectnone"/></a>
	<div id="caCheckboxes1" class="caSelector">
	</div>
	<button class="button" onClick="caSelectorUpdated(1)"><fmt:message key="dashboardbeta.ok"/></button>
</div>

<div id="caSelector2" style="display: none;" class="dialog">
	<div class="dialog-title"><fmt:message key="dashboardbeta.rightsidecas"/></div>
	<p><a href="javascript:selectAllCas2();"><fmt:message key="dashboardbeta.selectall"/></a>
		<a href="javascript:selectNoneCas2();"><fmt:message key="dashboardbeta.selectnone"/></a>
	<div id="caCheckboxes2" class="caSelector">
	</div>
	<button class="button" onClick="caSelectorUpdated(2)"><fmt:message key="dashboardbeta.ok"/></button>
</div>

</body>
</html>