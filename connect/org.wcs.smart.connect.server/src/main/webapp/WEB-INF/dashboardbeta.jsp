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
	<div id="pageheader" class="pageheader"><span id="dashboardtitle"><fmt:message key="dashboardbeta.title"/></span>
	</div>
	
	<div style="padding-top: 5px">
	<select id="admin-selectlist" class="uielement" name="admin-selectlist"></select>
	<button onClick="showDashboard(); return false;" class="button" style="padding:1px"> <fmt:message key="dashboardbeta.viewdashboard"/></button>

	
	</div>
	<div><div id="message" class="msgsection"></div></div>
	
	<div id="report-wrapper" style="height: 80vh; display:table">
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
      		<div style="display:none;"><span id="reportdate1" ></span><a href="" onClick="document.getElementById('report1form').style.display='block'; return false;" style="padding-left:4px; font-size:0.9em"><fmt:message key="dashboardbeta.change"/></a>
      		<form id="report1form" name="report1form" style="display:none">
    			<input id="report1selecthidden" type="hidden"/>
    			<input id="report1paramshidden" type="hidden"/>
				<select id='filterDate1' class='updateChange uielement' name="time_filter" style="margin-bottom:3px">
					<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
					<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
					<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
					<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
					<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
					<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
				</select>
				<span><input type="text" id="report1From" class="date-input-center uielement"> - <input type="text" id="report1To" class="date-input-center uielement"></span>
				<button style="padding:1px" class="button" onClick="rerunReport1(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
				<button id="savedatebutton1" style="padding:1px" class="button" onClick="updateDateOnReport1(); return false;" disabled><fmt:message key="dashboardbeta.datedefault"/></button>
    		</form>
    		</div>
		</div>
    	
    	<div style="display:table-cell; width:50%; height:1px">
	    	<div style="display:none;"><span id="reportdate2" ></span><a href="" onClick="document.getElementById('report2form').style.display='block'; return false;" style="padding-left:4px; font-size:0.9em"><fmt:message key="dashboardbeta.change"/></a>
    		<form id="report2form" name="report2form" style="display:none">
    			<input id="report2selecthidden" type="hidden"/>
    			<input id="report2paramshidden" type="hidden"/>
    		
    			<select id='filterDate2' class='updateChange uielement' name="time_filter" style="margin-bottom:3px">
					<option value=1><fmt:message key="dashboardbeta.oneday"/></option>
					<option value=7><fmt:message key="dashboardbeta.oneweek"/></option>
					<option value=30><fmt:message key="dashboardbeta.onemonth"/></option>
					<option value=180><fmt:message key="dashboardbeta.sixmonths"/></option>
					<option value=365><fmt:message key="dashboardbeta.oneyear"/></option>
					<option value=-1><fmt:message key="dashboardbeta.custom"/></option>
				</select>
				<span><input type="text" id="report2From" class="date-input-center uielement"> - <input type="text" id="report2To" class="date-input-center uielement"></span>
				<button style="padding:1px" class="button" onClick="rerunReport2(); return false;"><fmt:message key="dashboardbeta.runreport"/></button>
				<button id="savedatebutton2" style="padding:1px" class="button" onClick="updateDateOnReport2(); return false;" disabled><fmt:message key="dashboardbeta.datedefault"/></button>
				
    		</form>
    		</div>
    	</div>
    	</div>
  	</div>
  	<div id="configuration" style="border-top: 1px solid; padding-top:5px">
  		<form>
  			
  			<p id="adminoptions" style="display:none">
  				<a class="dashboard-alluser" href="" onClick="setDashboardDefaultToSelectedDashboard(); return false;"><fmt:message key="dashboardbeta.saveasdefaultdashboard"/></a>
  				<a class="dashboard-adminonly" href="" onClick="editDashboard(); return false;"><fmt:message key="dashboardbeta.editdashboard"/></a> 
	  			<a class="dashboard-adminonly" href="" onClick="deleteDashboard(); return false;" ><fmt:message key="dashboardbeta.deletedashboard"/></a>
	  			<a class="dashboard-adminonly" href="" onClick="createNewDashboard(); return false;" ><fmt:message key="dashboardbeta.createdashboard"/></a> 
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