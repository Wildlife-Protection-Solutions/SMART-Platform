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
	
	<div id="dashboardLabeldiv" style="margin-top:10px; margin-bottom:10px"><fmt:message key="dashboardbeta.dashbaordname"/><input id="dashboardlabeltext" class="uielement" type="text" style="margin-left:5px; width:300px" value="New Dashboard"/>
		<input type="hidden" id="dashboarduuid" name="reportuuid"/>
	</div>
	<div id="report-wrapper" style="display:table;border-spacing: 3px">
    	<div id="report1" class="dashboard-section" style="display:table-cell; padding: 5px;">
    	<form id="report1form" name="report1form" style="border-bottom:1px solid #72A6C8; padding-bottom: 5px;" >
    		<input id="report1selecthidden" type="hidden"/>
    		<input id="report1paramshidden" type="hidden"/>
    		<input id="report1isccaa" type="hidden"/>
    		<div class="p-2">Report: <select id='report1select' class='updateChange uielement' name="report1" style="margin-bottom:3px"> 
    		<option value=0>Select the First Report Here</option>
    		</select>
			</div>
			<div class="p-2" >Date Range:
			<select id='filterDate1' class='updateChange uielement' name="time_filter" style="margin-bottom:3px" onchange="javascript:changeReport1Date();return false;">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-30><fmt:message key="dashboardbeta.monthtodate"/></option>
				<option value=-365><fmt:message key="dashboardbeta.yeartodate"/></option>
				<option value=-9999><fmt:message key="dashboardbeta.custom"/></option>
			</select>
    		<input disabled type="text" name="report1From" id="report1From" class="date-input-center"> -
    		<input disabled type="text" name="report1To" id="report1To" class="date-input-center">
    		</div>
    		
    		<div class="p-2" id="customparameters1"></div>
    		<div class="p-2"><button class="button" onClick="rerunReport1(); return false;"><fmt:message key="dashboardbeta.runreport"/></button></div>
			<div class="p-2"><button id="editCasButton1" onclick="editcas1(); return false" class="button" style="display:none"><fmt:message key="dashboardbeta.editcas"/></button></div>
		</form>
		<img id="loading1" src="../css/images/loading.svg">
      	<div id="iframe1div"></div>
    	</div>
    	<div id="report2" class="dashboard-section" style="display:table-cell; padding: 5px;">
    	<form id="report2form" name="report2form" style="border-bottom:1px solid #72A6C8; padding-bottom: 5px;">
    		<input id="report2selecthidden" type="hidden"/>
    		<input id="report2paramshidden" type="hidden"/>
    		<input id="report2isccaa" type="hidden"/>
    		    		
    		<div class="p-2" >Report: <select id='report2select' class='updateChange uielement' name="report2" style="margin-bottom:3px"> 
    		<option value=0>Select the Second Report Here</option>
    		</select>
			</div>
			<div class="p-2" >Date Range:
    		<select id='filterDate2' class='updateChange uielement' name="time_filter" style="margin-bottom:3px">
				<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
				<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
				<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
				<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
				<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
				<option value=-30><fmt:message key="dashboardbeta.monthtodate"/></option>
				<option value=-365><fmt:message key="dashboardbeta.yeartodate"/></option>
				<option value=-9999><fmt:message key="dashboardbeta.custom"/></option>
				<option value=-9998><fmt:message key="dashboardbeta.sameasreport1"/></option>
				<option id="previousday" class="extrareport2dates" value=10001><fmt:message key="dashboardbeta.previousday"/></option>
				<option id="samedaylastweek" class="extrareport2dates" value=70001><fmt:message key="dashboardbeta.samedaylastweek"/></option>
				<option id="samedaylastmonth" class="extrareport2dates" value=300001><fmt:message key="dashboardbeta.samedaylastmonth"/></option>
				<option id="samedaylastyear" class="extrareport2dates" value=3650001><fmt:message key="dashboardbeta.samedaylastyear"/></option>
				<option id="previousweek" class="extrareport2dates" value=70007><fmt:message key="dashboardbeta.previousweek"/></option>
				<option id="4weeksprevious" class="extrareport2dates" value=280007><fmt:message key="dashboardbeta.4weeksprevious"/></option>
				<option id="sameweeklastyear" class="extrareport2dates" value=3650007><fmt:message key="dashboardbeta.sameweeklastyear"/></option>
				<option id="previousmonth" class="extrareport2dates" value=300030><fmt:message key="dashboardbeta.previousmonth"/></option>
				<option id="samemonthlastyear" class="extrareport2dates" value=3650030><fmt:message key="dashboardbeta.samemonthlastyear"/></option>
				<option id="previous6month" class="extrareport2dates" value=1800180><fmt:message key="dashboardbeta.previous6month"/></option>
				<option id="same6monthslastyear" class="extrareport2dates" value=3650180><fmt:message key="dashboardbeta.same6monthslastyear"/></option>
				<option id="previousyear" class="extrareport2dates" value=3650365><fmt:message key="dashboardbeta.previousyear"/></option>
			</select>
			<input disabled type="text" name="report2From" id="report2From" class="date-input-center" > - 
    		<input disabled type="text" name="report2To" id="report2To" class="date-input-center">
    		</div>
			<div class="p-2" ><button class="button" onClick="rerunReport2(); return false;"><fmt:message key="dashboardbeta.runreport"/></button></div>
			<div class="p-2" ><button id="editCasButton2" onClick="editcas2(); return false;" class="button" style="display:none"><fmt:message key="dashboardbeta.editcas"/></button></div>
			
    		<div class="p-2" id="customparameters2"></div>
		</form>
    	<img id="loading2" src="../css/images/loading.svg">
      	<div id="iframe2div"></div>

    	</div>
  	</div>
  	<div id="configuration" style="margin-top:10px">
  		<form>
  			<p id="adminoptions"><button id="saveButton" onClick="saveOrEditDashboard(); return false;" class="button"><fmt:message key="dashboardbeta.createdashboard"/></button></p>
  		</form>
  	</div>
	</div>
<%@include file="footer.jsp" %>

<div id="errorDialog" style="display: none;" class="dialog">
	<div id="errorText"></div>
	<div style="text-align:right">
	<button style="width:60px" id="errorOKButton" class="button" onClick="closeDialog('errorDialog');"><fmt:message key="dashboardbeta.ok"/></button>
	</div>
</div>

<div id="caSelector1" style="display: none;" class="dialog">
	<div class="dialog-title"><fmt:message key="dashboardbeta.leftsidecas"/></div>
	<p><a href="javascript:selectAllCas1();"><fmt:message key="dashboardbeta.selectall"/></a>
		<a href="javascript:selectNoneCas1();"><fmt:message key="dashboardbeta.selectnone"/></a>
	<div id="caCheckboxes1" class="caSelector">
	</div>
	<div style="text-align:right">
	<button style="width:60px" class="button" onClick="caSelectorUpdated(1)"><fmt:message key="dashboardbeta.ok"/></button>
	</div>
</div>

<div id="caSelector2" style="display: none;" class="dialog">
	<div class="dialog-title"><fmt:message key="dashboardbeta.rightsidecas"/></div>
	<p><a href="javascript:selectAllCas2();"><fmt:message key="dashboardbeta.selectall"/></a>
		<a href="javascript:selectNoneCas2();"><fmt:message key="dashboardbeta.selectnone"/></a>
	<div id="caCheckboxes2" class="caSelector">
	</div>
	<div style="text-align:right">
	<button style="width:60px" class="button" onClick="caSelectorUpdated(2)"><fmt:message key="dashboardbeta.ok"/></button>
	</div>
</div>

</body>
</html>