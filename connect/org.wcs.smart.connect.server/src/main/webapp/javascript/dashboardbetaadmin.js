var DashboardAPIURL = "../api/dashboardbeta";
var REPORTURL = "../api/report/";
var action = null;

var picker1, picker2, picker3, picker4;

var parameterNames = new Array();

/* configure events on html elements */
window.onload = function(){
	getAllReports();
	
	var url = window.location.href;
	try {
		action = /action=([^&]+)/.exec(url)[1];
	}catch(err) {
		action = "new";
	}
	
	if(action == "edit"){
		var uuid = /uuid=([^&]+)/.exec(url)[1];
		getDashboard(uuid);
		saveButton.innerHTML = "Save Edits";
		document.getElementById('pageheader').innerHTML = document.getElementById('pageheader').innerHTML + " - Edit Dash Board ID: "+ uuid;
		document.getElementById('dashboarduuid').value = uuid;
	}else{
		document.getElementById('pageheader').innerHTML = document.getElementById('pageheader').innerHTML + " - Create New Dash Board";
		document.getElementById('dashboarduuid').value = 0;
	}
	
	initDatePickers();
	
	
	document.getElementById('filterDate1').addEventListener("change", checkForCustomDates);
	document.getElementById('filterDate2').addEventListener("change", checkForCustomDates);
	
	document.getElementById('report1select').addEventListener("change", getReport1Parameters);
	document.getElementById('report2select').addEventListener("change", getReport2Parameters);
	
}


function saveOrEditDashboard(){
	form1 = document.getElementById('report1form');
	form2 = document.getElementById('report2form');
	if(form1.report1.value == 0 || form2.report2.value == 0){
		document.getElementById('errorText').innerHTML = "You must select a report in each of the two drop downs to save a valid Dashboard.";
		displayDialog('errorDialog','main');
		return false;
	}
	
	
	list1 = getRepor1CustomParameters();
	list2 = getRepor2CustomParameters();
	
	data = {
		    "reportUuid1": form1.report1.value,
		    "reportUuid2": form2.report2.value,
		    "dateRange1": form1.time_filter.value,
		    "dateRange2": form2.time_filter.value,
		    "customDate1From": form1.report1From.value,
		    "customDate1To": form1.report1To.value,
		    "customDate2From": form2.report2From.value,
		    "customDate2To": form2.report2To.value,
		    "parameterList1": list1,
		    "parameterList2": list2,
		    "label": document.getElementById('dashboardlabeltext').value
		    };
	if(action == "edit"){
		data.uuid = document.getElementById('dashboarduuid').value;
	}
	
	var oReq = new XMLHttpRequest();
	oReq.onload = ReportCreated;
	oReq.open("POST", DashboardAPIURL, true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
	oReq.send(JSON.stringify(data));
		
}

function ReportCreated(){
	if(this.status == 201){
		document.getElementById('errorOKButton').onclick = function(){
			window.location.href = "dashboardbeta";
		};
		document.getElementById('errorText').innerHTML = "Dashboard Successfully Created or Edited. You will be returned to your Dashboard Page now.";
		displayDialog('errorDialog','main');
	}else{
		document.getElementById('errorText').innerHTML = "Dashboard was NOT Created, something went wrong with the request.";
		displayDialog('errorDialog','main');
	}
}

