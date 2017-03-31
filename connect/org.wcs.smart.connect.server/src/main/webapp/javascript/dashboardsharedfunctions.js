function showParamaterSelection1(){
	var json = JSON.parse(this.responseText);
	showParamaterSelection("customparameters1", json);
}

function showParamaterSelection2(){
	var json = JSON.parse(this.responseText);
	showParamaterSelection("customparameters2", json);
}


function showParamaterSelection(divid, json){
	var parent = document.getElementById(divid);
	parent.innerHTML = "";
	parameterNames.length = 0; //clear the array so we don't parameters from the last report that was run.
	
 	for (var i = 0; i < json.length; i++){
 		if(json[i].type == "GROUP"){
 			if (json[i].name=="Report Dates"){
 				//start and end dates already exist, ignore them
 			}else{ 	
 				var f = document.createElement("fieldset");
 				if(json[i].displayText != null){
 					f.innerHTML = "<legend>" + json[i].displayText + ":" + "</legend>";
 				}else{
 					f.innerHTML = "<legend>" + json[i].name + ":" + "</legend>";
 				}
 				for (var x = 0; x < json[i].children.length; x++){
 					parameterNames.push(json[i].children[x].name);
 					if(json[i].children[x].type == "BOOLEAN"){
 						addBooleanParamater(json[i].children[x], f, false);
 					}else{
 						addTextboxParamater(json[i].children[x], f,false);
 					}
 				}
 				parent.insertBefore(f, parent.childNodes[4]);
 			}
 		}else if(json[i].type == "BOOLEAN"){
 			addBooleanParamater(json[i], parent, true);
 			parameterNames.push(json[i].name);
 		}else{// use a basic textbox for all other types: strings, integers, floats etc.
 			addTextboxParamater(json[i], parent, true);
 			parameterNames.push(json[i].name);
 		}
 	}
 }


function getReport1Parameters(){
	//clear custom paramater from any previous reports
	var parent = document.getElementById("customparameters1");
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
	parent.innerHTML = "<font style='color:red'>Loading custom parameters.</font>";

	var uuid = document.getElementById('report1select').value;
	//update report parameters required
	var oReq = new XMLHttpRequest();
	oReq.onload = showParamaterSelection1;
	oReq.open("Get", REPORTURL  + uuid + "/params", true);
	oReq.send();
}
function getReport2Parameters(){
	var parent = document.getElementById("customparameters2");
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
	parent.innerHTML = "<font style='color:red'>Loading custom parameters.</font>";
	
	var uuid = document.getElementById('report2select').value;
	var oReq = new XMLHttpRequest();
	oReq.onload = showParamaterSelection2;
	oReq.open("Get", REPORTURL  + uuid + "/params", true);
	oReq.send();
}


function addTextboxParamater(param, parent, newGroup){
	if(newGroup == true){
		var f = document.createElement("fieldset");
		f.innerHTML = "<legend>" + param.displayText + ":" + "</legend>";
	}else{
		var f = document.createElement("p");
	}
	var newInput = document.createElement("input");
	newInput.setAttribute("id", param.name);
	newInput.style.width="100%";
	newInput.type = "text";

	f.appendChild(newInput);
	parent.insertBefore(f, parent.childNodes[4]);

}

//check if the user selected or deselected "custom dates" and grey/de-grey the custom inputs. 
function checkForCustomDates(){
	var date = document.getElementById('filterDate1');
	if(date.value == -1){
		document.getElementById('report1From').disabled = false;
		document.getElementById('report1To').disabled = false;
	}else{
		document.getElementById('report1From').disabled = true;
		document.getElementById('report1To').disabled = true;
	}
	
	var date = document.getElementById('filterDate2');
	if(date.value == -1){
		document.getElementById('report2From').disabled = false;
		document.getElementById('report2To').disabled = false;
	}else{
		document.getElementById('report2From').disabled = true;
		document.getElementById('report2To').disabled = true;
	}
	
	//Set the Custom date fields all the time, so we can just use that date range when submitting
	var startYear = new Date();
	var e = document.getElementById('filterDate1');
	var days = e.options[e.selectedIndex].value;
	if(days != -1){
		startYear.setDate(startYear.getDate() - days);
		picker2.setDate(new Date(), false);
		picker1.setDate(startYear, false);
	}
	
	
	startYear = new Date();
	e = document.getElementById('filterDate2');
	days = e.options[e.selectedIndex].value;
	if(days != -1){
		startYear.setDate(startYear.getDate() - days);
		picker4.setDate(new Date(), false);
		picker3.setDate(startYear, false);
	}
}

//enable save date buttons
function date1Changed(){
	document.getElementById('savedatebutton1').disabled = false;	
}
function date2Changed(){
	document.getElementById('savedatebutton2').disabled = false;
}


function getAllReports(){
	var oReq = new XMLHttpRequest();
 	oReq.onload = reportsCallback;
 	oReq.open("Get", REPORTURL, true);
 	oReq.send();
}

function reportsCallback(){
	var reports = JSON.parse(this.responseText);
	var select1 = document.getElementById('report1select'); 
	var select2 = document.getElementById('report2select');
 	for (var i = 0; i < reports.length; i++){
 		report = reports[i];
 		var opt = document.createElement('option');
 		var opt2 = document.createElement('option');
 	    opt.value = report.uuid
 	    opt.innerHTML = report.name;
 	    opt2.value = report.uuid
	    opt2.innerHTML = report.name;
 	    select1.appendChild(opt);
 	    select2.appendChild(opt2);
 	}
 	
 	//if we are on the admin page, we want to set the report UUIDs now that we have all the report UUIDs loaded. Couldn't do it on loading the Dashboard itself if this function happened to be slowed. So I put it in a dom object and now we can set the real select <option>
	if(document.getElementById('report1select') != null){
		document.getElementById('report1select').value = document.getElementById('report1selecthidden').value;
		document.getElementById('report2select').value = document.getElementById('report2selecthidden').value;
	}
}


function initDatePickers(){
	picker1 = new Pikaday({
		field: document.getElementById('report1From'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});

	picker2 = new Pikaday({
		field: document.getElementById('report1To'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});
	
	picker3 = new Pikaday({
		field: document.getElementById('report2From'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});

	picker4 = new Pikaday({
		field: document.getElementById('report2To'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});
}

function getDashboard(uuid){
	var oReq = new XMLHttpRequest();
 	oReq.onload = updateReportsFromDashBoardJson;
 	oReq.open("Get", DashboardAPIURL + "/" + uuid, true);
 	oReq.send();
}

function updateReportsFromDashBoardJson(){
	var dashboard = JSON.parse(this.responseText);
	if(dashboard.uuid != null){
		document.getElementById('report1selecthidden').value = dashboard.reportUuid1;
		document.getElementById('report2selecthidden').value = dashboard.reportUuid2;
		
		document.getElementById('filterDate1').value = dashboard.dateRange1;
		document.getElementById('filterDate2').value = dashboard.dateRange2;
		document.getElementById('report1From').value = dashboard.customDate1From;
		document.getElementById('report1To').value = dashboard.customDate1To;
		document.getElementById('report2From').value = dashboard.customDate2From;
		document.getElementById('report2To').value = dashboard.customDate2To;
		
		//if we are not are the admin page we can write the label in the header, otherwise we want to leave the header alone
		if(document.getElementById('report1select') == null){
			document.getElementById('pageheader').innerHTML = dashboard.label;
		}else{//on the admin page we also have a label input box
			document.getElementById('dashboardlabeltext').value = dashboard.label;
		}
	}else{
		document.getElementById('pageheader').innerHTML = "No Default Dashboard Selected Yet";
	}
	checkForCustomDates();
	runReports();
}

//actually run the reports and put the results in the iframes
function runReports(){
	rerunReport1();
	rerunReport2();
}
function rerunReport1(){
	document.getElementById('iframe1').src = resolve(generateRelativeUrl(REPORTURL, 1));
}

function rerunReport2(){
	document.getElementById('iframe2').src = resolve(generateRelativeUrl(REPORTURL, 2));
}

function generateRelativeUrl(root, report){
	if(report == 1){//get the data from the inputs for report #1 
		var uuid = document.getElementById('report1selecthidden').value;
		if(uuid == ""){
			return "about:blank";
		}
		//add the UUID
		var url = root + uuid + "?format=HTML" ;
		
		var startDate = new Date(document.getElementById('report1From').value.substring(4));
		var dateStr = startDate.getFullYear() + "-" + (startDate.getMonth()+1) + "-" + startDate.getDate() + " 00:00:00";
		url += "&parameterList=Start Date" + "," +dateStr + ",";
	
		var endDate = new Date(document.getElementById('report1To').value.substring(4));
		dateStr = endDate.getFullYear() + "-" + (endDate.getMonth()+1) + "-" + endDate.getDate() + " 00:00:00";
		url += "End Date" + "," +dateStr + ",";
	}else{
		var uuid = document.getElementById('report2selecthidden').value;
		
		//add the UUID
		if(uuid == ""){
			return "about:blank";
		}
		var url = root + uuid + "?format=HTML" ;
		
		var startDate = new Date(document.getElementById('report2From').value.substring(4));
		var dateStr = startDate.getFullYear() + "-" + (startDate.getMonth()+1) + "-" + startDate.getDate() + " 00:00:00";
		url += "&parameterList=Start Date" + "," +dateStr + ",";
	
		var endDate = new Date(document.getElementById('report2To').value.substring(4));
		dateStr = endDate.getFullYear() + "-" + (endDate.getMonth()+1) + "-" + endDate.getDate() + " 00:00:00";
		url += "End Date" + "," +dateStr + ",";
	}
	
	return url;
}

//get full URL from a relative one, used to give full-url link to users to share.
function resolve(url) {
	  var doc      = document
	    , old_base = doc.getElementsByTagName('base')[0]
	    , old_href = old_base && old_base.href
	    , doc_head = doc.head || doc.getElementsByTagName('head')[0]
	    , our_base = old_base || doc_head.appendChild(doc.createElement('base'))
	    , resolver = doc.createElement('a')
	    , resolved_url
	    ;

	  resolver.href = url;
	  resolved_url  = resolver.href; // browser magic at work here

	  return resolved_url;
}



function getRepor1CustomParameters(){
	//TODO
	return "";
}

function getRepor2CustomParameters(){
	//TODO
	return "";
}

