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
	document.getElementById('admin-selectlist').addEventListener("change", reportChanged);
	
	getDashBoards();
	getUserDetails();
}



function getReportIds(){
	var oReq = new XMLHttpRequest();
 	oReq.onload = updateReportsFromDashBoardJson;
 	oReq.open("Get", DashboardAPIURL + "/default/" , true);
 	oReq.send();
}



function setDashboardDefaultToSelectedDashboard(){
	var uuid = document.getElementById('dashboardtitle').dataset.uuid ;
	
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
 	var selectuuid = document.getElementById('dashboardtitle').dataset.uuid ;
	for (var i = 0; i < links.length; i++){
 		link = links[i];
 		var opt = document.createElement('option');
 	    opt.value = link.uuid;
 	    opt.innerHTML = link.label;
 	    select.appendChild(opt);
 	    
 	    if (link.uuid == selectuuid){
 	    	opt.selected = true;	
 	    }    
 	}
}

function editDashboard(){
	var uuid = document.getElementById('dashboardtitle').dataset.uuid ;
	window.location.href = "dashboardbetaadmin?action=edit&uuid=" + uuid;
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
	
	var adminElement = 	document.getElementById('adminoptions');
	var allkids = adminElement.getElementsByClassName("dashboard-adminonly")
	
	for (var i = 0; i < allkids.length; i++) {
		if (admin){
			allkids[i].style.display = "inline";
		}else{
			allkids[i].style.display = "none";
		}
	}
	adminElement.style.display = "inline";
}

function deleteDashboard(){
	var uuid = document.getElementById('dashboardtitle').dataset.uuid;
	var title = document.getElementById('dashboardtitle').innerHTML ;
	displayConfirmDialog(i18n("dashboard.delete"), i18n("dashboard.areyousuredelete") + title + i18n("dashboard.thiswillremove"), function(){
		var oReq = new XMLHttpRequest();
	 	oReq.onload = function(){location.reload()};
	 	oReq.open("Delete", DashboardAPIURL + "/" + uuid , true);
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


function reportChanged(){
	document.getElementById('savedatebutton1').style.display = "none";
	document.getElementById('savedatebutton2').style.display = "none";
}