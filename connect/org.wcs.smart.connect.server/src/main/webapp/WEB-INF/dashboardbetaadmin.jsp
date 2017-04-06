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
	
	<div id="dashboardLabeldiv" style="margin-top:10px"><fmt:message key="dashboardbeta.dashbaordname"/><input id="dashboardlabeltext" class="uielement" type="text" style="margin-left:5px; width:300px" value="New Dashboard"/>
		<input type="hidden" id="dashboarduuid" name="reportuuid"/>
	</div>
	<div id="report-wrapper">
    	<div id="report1" style="margin-top: 10px"  class="dashboard-section">
    	<form id="report1form" name="report1form" style="border-bottom:1px solid #72A6C8; padding-bottom: 5px;" >
    		<input id="report1selecthidden" type="hidden"/>
    		<input id="report1paramshidden" type="hidden"/>
    		<input id="report1isccaa" type="hidden"/>
    		<div>Report: <select id='report1select' class='updateChange uielement' name="report1" style="margin-bottom:3px"> 
    		<option value=0>Select the First Report Here</option>
    		</select>
			</div>
			<div>Date Range:
			<select id='filterDate1' class='updateChange uielement' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
			</select>
    		<input disabled type="text" name="report1From" id="report1From" class="date-input-center"> -
    		<input disabled type="text" name="report1To" id="report1To" class="date-input-center">
    		</div>
    		
    		<div id="customparameters1"></div>
    		<button class="button" onClick="rerunReport1(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
			<button id="editCasButton1" onclick="editcas1(); return false" class="button" style="display:none"><fmt:message key="dashboardbeta.editcas"/></button>
		</form>
		<img id="loading1" src="../css/images/loading.svg">
      	<div id="iframe1div"></div>
    	</div>
    	<div id="report2" class="dashboard-section">
    	<form id="report2form" name="report2form" style="border-bottom:1px solid #72A6C8; padding-bottom: 5px;">
    		<input id="report2selecthidden" type="hidden"/>
    		<input id="report2paramshidden" type="hidden"/>
    		<input id="report2isccaa" type="hidden"/>
    		    		
    		<div>Report: <select id='report2select' class='updateChange uielement' name="report2" style="margin-bottom:3px"> 
    		<option value=0>Select the Second Report Here</option>
    		</select>
			</div>
			<div>Date Range:
    		<select id='filterDate2' class='updateChange uielement' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
			</select>
			<input disabled type="text" name="report2From" id="report2From" class="date-input-center" > - 
    		<input disabled type="text" name="report2To" id="report2To" class="date-input-center">
    		</div>
			<button class="button" onClick="rerunReport2(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
			<button id="editCasButton2" onClick="editcas2(); return false;" class="button" style="display:none"><fmt:message key="dashboardbeta.editcas"/></button>
			
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