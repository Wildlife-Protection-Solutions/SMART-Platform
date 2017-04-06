var DashboardAPIURL = "../api/dashboardbeta";
var UserURL = "../api/connectuser/iscurrentuseradmin";
var REPORTURL = "../api/report/";

var parameterNames1 = new Array();
var parameterNames2 = new Array();

var picker1, picker2, picker3, picker4;

/* configure events on html elements */
window.onload = function(){
	getReportIds();
	
	initDatePickers();
	
	document.getElementById('filterDate1').addEventListener("change", checkForCustomDates);
	document.getElementById('filterDate2').addEventListener("change", checkForCustomDates);
	document.getElementById('filterDate1').addEventListener("change", date1Changed);
	document.getElementById('filterDate2').addEventListener("change", date2Changed);
	
	getDashBoards();
	getUserDetails();
}



function getReportIds(){
	var oReq = new XMLHttpRequest();
 	oReq.onload = updateReportsFromDashBoardJson;
 	oReq.open("Get", DashboardAPIURL + "/default/" , true);
 	oReq.send();
}


function setDashboardDefaultToCurrentSettings(){
	uuid = document.getElementById('admin-selectlist').value;
	
	form1 = document.getElementById('report1form');
	form2 = document.getElementById('report2form');
	
	if(uuid == null || uuid == ""){
		document.getElementById('errorText').innerHTML = i18n("dashboard.mustselectreports"); 
		displayDialog('errorDialog','main');
		return false;
	}
	
	list1 = getRepor1CustomParameters();
	list2 = getRepor2CustomParameters();
	
	data = {
			"dashboardUuid":uuid,
		    "dateRange1": form1.time_filter.value,
		    "dateRange2": form2.time_filter.value,
		    "customDate1From": form1.report1From.value,
		    "customDate1To": form1.report1To.value,
		    "customDate2From": form2.report2From.value,
		    "customDate2To": form2.report2From.value,
		    "parameterList1": list1,
		    "parameterList2": list2
		    };
	
	var oReq = new XMLHttpRequest();
	oReq.onload = updateReportsFromDashBoardJson;
	oReq.open("PUT", DashboardAPIURL + "/default/", true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
	oReq.send(JSON.stringify(data));
}

function setDashboardDefaultToSelectedDashboard(){
	uuid = document.getElementById('admin-selectlist').value;

	var oReq = new XMLHttpRequest();
	oReq.onload = updateReportsFromDashBoardJson;
	oReq.open("PUT", DashboardAPIURL + "/default/" + uuid, true);
	oReq.send();
}

function getDashBoards(){
	var oReq = new XMLHttpRequest();
 	oReq.onload = listOfDashboardsCallback;
 	oReq.open("Get", DashboardAPIURL , true);
 	oReq.send();
}

function listOfDashboardsCallback(){
	var links = JSON.parse(this.responseText);
	var select = document.getElementById('admin-selectlist'); 
 	for (var i = 0; i < links.length; i++){
 		link = links[i];
 		var opt = document.createElement('option');
 	    opt.value = link.uuid;
 	    opt.innerHTML = link.label;
 	    select.appendChild(opt);
 	}
}

function editDashboard(){
	var e = document.getElementById('admin-selectlist');
	window.location.href = "dashboardbetaadmin?action=edit&uuid=" + e.options[e.selectedIndex].value;
}

function createNewDashboard(){
	window.location.href = "dashboardbetaadmin?action=new";
}

function getUserDetails(){
	var oReq = new XMLHttpRequest();
 	oReq.onload = userCallback;
 	oReq.open("Get", UserURL , true);
 	oReq.send();
}

function userCallback(){
	var admin = JSON.parse(this.responseText);
	if(admin){
		document.getElementById('adminoptions').style.display = "block";
	}
}

function deleteDashboard(){
	var e = document.getElementById('admin-selectlist');
	
	displayConfirmDialog(i18n("dashboard.delete"), i18n("dashboard.areyousuredelete") + e.options[e.selectedIndex].innerText + i18n("dashboard.thiswillremove"), function(){
		var oReq = new XMLHttpRequest();
	 	oReq.onload = function(){location.reload()};
	 	oReq.open("Delete", DashboardAPIURL + "/" + e.options[e.selectedIndex].value , true);
	 	oReq.send();
	});
}

function showDashboard(){
	var e = document.getElementById('admin-selectlist');
	getDashboard(e.options[e.selectedIndex].value);
	
	document.getElementById("report1form").style.display="none";
	document.getElementById("report2form").style.display="none";
}

function updateDateOnReport1(){
	form1 = document.getElementById('report1form');
	data = {
		    "dateRange1": form1.time_filter.value,
		    "customDate1From": form1.report1From.value,
		    "customDate1To": form1.report1To.value
		    };
	
	var oReq = new XMLHttpRequest();
	oReq.onload = updateReportsFromDashBoardJson;
	oReq.open("PUT", DashboardAPIURL + "/default/", true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
	oReq.send(JSON.stringify(data));
}

function updateDateOnReport2(){
	form2 = document.getElementById('report2form');
	data = {
		    "dateRange2": form2.time_filter.value,
		    "customDate2From": form2.report2From.value,
		    "customDate2To": form2.report2To.value
		    };
	
	var oReq = new XMLHttpRequest();
	oReq.onload = updateReportsFromDashBoardJson;
	oReq.open("PUT", DashboardAPIURL + "/default/", true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
	oReq.send(JSON.stringify(data));
}