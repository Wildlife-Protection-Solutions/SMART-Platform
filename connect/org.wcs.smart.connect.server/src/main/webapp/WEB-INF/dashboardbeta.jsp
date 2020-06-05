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
	<div id= "main"><div id="pageheader" class="pageheader"><span id="dashboard"><fmt:message key="dashboardbeta.title"/></span>
	</div>
	
	<div style="padding-top: 5px">
	<select id="admin-selectlist" class="uielement" name="admin-selectlist" onchange="javascript:showDashboard(); return false;"></select>
	<button onClick="runReports(); return false;" class="button" style="padding:3px 10px"> <fmt:message key="dashboardbeta.refreshreports"/></button>

	
	</div>
	<div><div id="message" class="msgsection"></div></div>
	<div id="report-container" style="flex: 1 1 auto; height: 0;">
	<div id="report-wrapper" style="display:table; height: 100%; width: 100%;">
	<div style="display:table-row">
    	<div id="report1" style="display:table-cell; width:50%">
			<div style="height:100%">
   	  			<img id="loading1" style="display:block" src="../css/images/loading.svg">
   	  			<div id="iframe1div" style="height:100%"></div>
			</div>
    	</div>	 
    	
    	<div id="report2" style="display:table-cell; width:50%">
			<div style="height:100%">
   	  			<img id="loading2" style="display:block" src="../css/images/loading.svg">
   	  			<div id="iframe2div" style="height:100%"></div>
			</div>
    	</div>
    </div>
    <div style="display:table-row">
    	<div style="display:table-cell; width:50%; height:1px">
      		<div><span id="reportdate1" style="display:none;"></span>
      		<form id="report1form" name="report1form" >
    			<input id="report1selecthidden" type="hidden"/>
    			<input id="report1paramshidden" type="hidden"/>
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
				<span><input type="text" id="report1From" class="date-input-center uielement"> - <input type="text" id="report1To" class="date-input-center uielement"></span>
				<button style="padding:2px 10px;" class="button" onClick="rerunReport1(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>

    		</form>
    		</div>
		</div>
    	
    	<div style="display:table-cell; width:50%; height:1px">
	    	<div><span id="reportdate2" style="display:none;"></span>
    		<form id="report2form" name="report2form" >
    			<input id="report2selecthidden" type="hidden"/>
    			<input id="report2paramshidden" type="hidden"/>
    		
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
				<span><input type="text" id="report2From" class="date-input-center uielement"> - <input type="text" id="report2To" class="date-input-center uielement"></span>
				<button style="padding:2px 10px;" class="button" onClick="rerunReport2(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
				
    		</form>
    		</div>
    	</div>
    	</div>
  	</div> <!-- report-wrapper -->
  	</div> <!-- report-container -->
  	<div id="configuration" style="border-top: 1px solid; padding-top:8px">
  		<form>
  			
  			<p id="adminoptions" style="display:none;padding:5px">
  				<a class="dashboard-alluser button" href="" onClick="setDashboardDefaultToSelectedDashboard(); return false;"><fmt:message key="dashboardbeta.saveasdefaultdashboard"/></a>
  				<a class="dashboard-adminonly button" href="" onClick="editDashboard(); return false;"><fmt:message key="dashboardbeta.editdashboard"/></a> 
	  			<a class="dashboard-adminonly button" href="" onClick="deleteDashboard(); return false;" ><fmt:message key="dashboardbeta.deletedashboard"/></a>
	  			<a class="dashboard-adminonly button" href="" onClick="createNewDashboard(); return false;" ><fmt:message key="dashboardbeta.createdashboard"/></a> 
  			</p>
  		</form>
  	</div>
	</div>
<%@include file="footer.jsp" %>

<div id="errorDialog" style="display: none;" class="dialog">
	<div id="errorText"></div>
	<div style="text-align:right; width:50px">
		<button id="errorOKButton" class="button" onClick="closeDialog('errorDialog');"><fmt:message key="dashboardbeta.ok"/></button>
	</div>
</div>


</body>
</html>