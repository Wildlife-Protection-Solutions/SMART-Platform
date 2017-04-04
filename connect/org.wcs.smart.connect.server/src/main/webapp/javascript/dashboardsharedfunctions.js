
//get the possible parameters and put input boxes on the UI for users to fill out.
function getReport1ParametersForUi(){
	getReportParametersForUi(1);
}
function getReport2ParametersForUi(){
	getReportParametersForUi(2);
}

//get the possible parameters and put input boxes on the UI for users to fill out.
function getReportParametersForUi(num){
	//clear custom paramater from any previous reports
	if(num ==1){
		var parent = document.getElementById("customparameters1");
		var uuidhidden = document.getElementById('report1selecthidden');
		var select = document.getElementById('report1select');
		if(select != null && select.value != 0){
			uuidhidden.value = select.value; //if the select list is loaded, update the hidden value. If it isn't loaded use the hidden value as is. Race type thing when this function is called on page load, not sure which will finish first. 
		} 
	}else{
		var parent = document.getElementById("customparameters2");
		var uuidhidden = document.getElementById('report2selecthidden');
		var select = document.getElementById('report2select');
		if(select != null && select.value != 0){
			uuidhidden.value = select.value; //if the select list is loaded, update the hidden value. If it isn't loaded use the hidden value as is. Race type thing when this function is called on page load, not sure which will finish first. 
		} 	}
	
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
	parent.innerHTML = "<font style='color:red'>Loading custom parameters.</font>";
	
	var uuid = uuidhidden.value;
	
	//update report parameters required
	var oReq = new XMLHttpRequest();
	if(num==1){
		oReq.onload = showParamaterSelection1;
	}else{
		oReq.onload = showParamaterSelection2;
	}
		
	oReq.open("Get", REPORTURL  + uuid + "/params", true);
	oReq.send();
	
	var oReq = new XMLHttpRequest();
	if(num==1){
		oReq.onload = checkForCCAAReport1;
	}else{
		oReq.onload = checkForCCAAReport2;
	}
	oReq.open("Get", REPORTURL  + "definition/" + uuid , true);
	oReq.send();
}

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
 		}else{// use a basic textbox for all other types: strings, integers, floats etc.
 			addTextboxParamater(json[i], parent, true);
 		}
 	}
 }

function checkForCCAAReport1(){
	var json = JSON.parse(this.responseText);
	checkForCCAAReport(1,json);
}

function checkForCCAAReport2(){
	var json = JSON.parse(this.responseText);
	checkForCCAAReport(2,json);
}

function checkForCCAAReport(num, report){
	//save the report isCCAA so we can check it later
	if(num == 1){
		document.getElementById('report1isccaa').value = report.isCcaa;
	}else{
		document.getElementById('report2isccaa').value = report.isCcaa;
	}
	
	//show the CA selector and CA edit button so they can pick and edit CAs for this report
	if(report.isCcaa){
		getCaList(num);
		if(num == 1){
			displayDialog('caSelector1', "main");
			document.getElementById('editCasButton1').style.display = "inline-block";
		}else{
			displayDialog('caSelector2', "main");
			document.getElementById('editCasButton2').style.display = "inline-block";
		}
	//hide the button if it is no longer a CCAA report
	}else{
		if(num == 1){
			document.getElementById('editCasButton1').style.display = "none";
		}else{
			document.getElementById('editCasButton2').style.display = "none";
		}
	}
	setTimeout(function(){
		parseParametersIntoInputs(num);
	}, 250);
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
	newInput.setAttribute("name", param.name);
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
		if(document.getElementById('report1selecthidden').value != ""){
			document.getElementById('report1select').value = document.getElementById('report1selecthidden').value;
		}	
		if(document.getElementById('report2selecthidden').value != ""){
			document.getElementById('report2select').value = document.getElementById('report2selecthidden').value;
		}
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
		
		document.getElementById('report1paramshidden').value = dashboard.parameterList1;
		document.getElementById('report2paramshidden').value = dashboard.parameterList2;
		
		document.getElementById('filterDate1').value = dashboard.dateRange1;
		document.getElementById('filterDate2').value = dashboard.dateRange2;
		document.getElementById('report1From').value = dashboard.customDate1From;
		document.getElementById('report1To').value = dashboard.customDate1To;
		document.getElementById('report2From').value = dashboard.customDate2From;
		document.getElementById('report2To').value = dashboard.customDate2To;
		
		//if we are not are the admin page we can write the label in the header, otherwise we put it in the input box on the admin page
		if(document.getElementById('report1select') == null){
			document.getElementById('pageheader').innerHTML = dashboard.label;
		}else{//on the admin page we also have a label input box
			document.getElementById('dashboardlabeltext').value = dashboard.label;
		}
	}else{
		document.getElementById('pageheader').innerHTML = "No Default Dashboard Selected Yet";
	}
	
	if(document.getElementById('report1select') != null){ //if we are on the admin page
		getReport1ParametersForUi();
		getReport2ParametersForUi();
	}
	checkForCustomDates();
	setTimeout(function(){
		runReports();
	}, 200);
}

//actually run the reports and put the results in the iframes
function runReports(){
	rerunReport1();
	rerunReport2();
}
function rerunReport1(){
	if(document.getElementById('report1select') != null){ //if we are on the admin page
		document.getElementById('report1paramshidden').value = getRepor1CustomParameters();
	}

	document.getElementById('iframe1').src = resolve(generateRelativeUrl(REPORTURL, 1));
}

function rerunReport2(){
	if(document.getElementById('report2select') != null){ //if we are on the admin page
		document.getElementById('report2paramshidden').value = getRepor2CustomParameters();
	}

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
		
		//add the dates
		var startDate = new Date(document.getElementById('report1From').value.substring(4));
		var dateStr = startDate.getFullYear() + "-" + (startDate.getMonth()+1) + "-" + startDate.getDate() + " 00:00:00";
		url += "&parameterList=Start Date" + "," +dateStr + ",";
	
		var endDate = new Date(document.getElementById('report1To').value.substring(4));
		dateStr = endDate.getFullYear() + "-" + (endDate.getMonth()+1) + "-" + endDate.getDate() + " 00:00:00";
		url += "End Date" + "," +dateStr + ",";
		
		//add the parameters onto the existing 'parameterList' that start above with the dates.
		url += document.getElementById('report1paramshidden').value;

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

		//add the parameters onto the existing 'parameterList' that start above with the dates.
		url += document.getElementById('report2paramshidden').value;

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

function editCasButton1(){
	displayDialog('caSelector1', main);
}
function editCasButton2(){
	displayDialog('caSelector2', main);
}

function getCaList(num){
	var oReq = new XMLHttpRequest();
	if(num==1){
		oReq.onload = populateCaLists1;
	}else{
		oReq.onload = populateCaLists2;
	}
	oReq.open("Get", CAURL + '/?includeSpatialBoundaries=false', true);
	oReq.send();
}

function populateCaLists1(){
	populateCaList(1, this);
}
function populateCaLists2(){
	populateCaList(2, this);
}

function populateCaList(num, response){
	if (response.status != 200) {
		var msg = i18n("report.error");
		if (response.status == 401){
			msg += i18n("report.unauthorized");
		}
		try {
			msg = JSON.parse(response.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	if(num==1){
		var parent = document.getElementById('caCheckboxes1');
	}else{
		var parent = document.getElementById('caCheckboxes2')
	}
	var filterparent = document.getElementById("cafilteroptions");
	//clear any existing list
	if(parent != null){
		while (parent.firstChild) {
			parent.removeChild(parent.firstChild);
		}
	}
	
	var cas = JSON.parse(response.responseText);
	for (var i = 0; i < cas.length; i ++){
		var checkbox = document.createElement('input');
		checkbox.type = "checkbox";
		checkbox.name = cas[i].uuid;
		checkbox.value = cas[i].uuid;
		
		var label = document.createElement('label')
		label.htmlFor = cas[i].uuid;
		label.appendChild(document.createTextNode(cas[i].label));

		parent.appendChild(checkbox);
		parent.appendChild(label);
		parent.appendChild(document.createElement('br'));
	}			
	parent.appendChild(document.createElement('br'));
}

function selectAllCas1(){
	var parent = document.getElementById('caCheckboxes1');
	selectAll(parent);
}
function selectAllCas2(){
	var parent = document.getElementById('caCheckboxes2');
	selectAll(parent);
}
function selectAll(parent){
	 var children = parent.children;
	 for (var i = 0; i < children.length; i++) {
	   var child = children[i];
	   child.checked = true;
	 }
}

function selectNoneCas1(){
	var parent = document.getElementById('caCheckboxes1');
	selectNone(parent);
}
function selectNoneCas2(){
	var parent = document.getElementById('caCheckboxes2');
	selectNone(parent);
}
function selectNone(parent){
	 var children = parent.children;
	 for (var i = 0; i < children.length; i++) {
	   var child = children[i];
	   child.checked = false;
	 }
}

//get the report-compatible url parameters to add to the DashBoard database object.
function getRepor1CustomParameters(){
	var str = "";
	
	//get list of CAs for CCAA reports if necessary
	if(document.getElementById('report1isccaa').value == "true"){
		str = str + "&cafilter=";
		var parent = document.getElementById('caCheckboxes1');
		var children = parent.children;
		for (var i = 0; i < children.length; i++) {
			 var child = children[i];
			 if(child.tagName.toLowerCase() == "input" && child.checked == true){
				 str = str + child.value + ",";
			 } 
		 }
	}
	
	//get custom parameters
	var parent = document.getElementById("customparameters1");
	var children = parent.children;  
	for (var i = 0; i < children.length; i++) {
		 var child = children[i].children[1]; //get the actual "input" element
		 str = str + child.id + "," + child.value;
	 }
	
	return str;
}

function getRepor2CustomParameters(){
	var str = "";
	if(document.getElementById('report2isccaa').value == "true"){
		str = str + "&cafilter=";
		var parent = document.getElementById('caCheckboxes2');
		var children = parent.children;
		for (var i = 0; i < children.length; i++) {
			 var child = children[i];
			 if(child.tagName.toLowerCase() == "input" && child.checked == true){
				 str = str + child.value + ",";
			 } 
		 }
	}

	//get custom parameters
	var parent = document.getElementById("customparameters2");
	var children = parent.children;  
	for (var i = 0; i < children.length; i++) {
		 var child = children[i].children[1]; //get the actual "input" element
		 str = str + child.id + "," + child.value;
	 }

	
	return str;

}

function parseParametersIntoInputs(num){
	if(num == 1){
		var params1str = document.getElementById('report1paramshidden').value; 
		var params1 = params1str.split(','); 

		//pull out any custom parameters
		for(var i=0; i < params1.length; i+=2){
			var name = params1[i];
			var value = params1[i+1];
			var input = getElementInsideContainer("customparameters1",name);
			if(input != null){
				input.value = value;
			}
		}
		
		//pull out any ca filters and check off the correct boxes
		for(var i=0; i < params1.length; i++){
			var name = params1[i];
			if(name.substring(0,10) == "&cafilter="){
				name = name.split("=")[1];
			}
			var input = getElementInsideContainer("caCheckboxes1",name);
			if(input != null){
				input.checked = true;
			}
		}
	}else{
		var params2str = document.getElementById('report2paramshidden').value;
		var params2 = params2str.split(',');
		for(var i=0; i < params2.length; i+=2){
			var name = params2[i];
			var value = params2[i+1];
			var input = getElementInsideContainer("customparameters2",name);
			if(input != null){
				input.value = value;
			}
		}
	}
}



function getElementInsideContainer(containerID, childID) {
    var elms = document.getElementsByName(childID);
    var container = document.getElementById(containerID);
    for (var i=0; i< elms.length; i++){
    	var elm = elms[i];
    	if(elm.parentNode == container || elm.parentNode.parentNode == container){
    		return elm;
    	} 
    }
    return null;
}

function updateHiddenParams(){
	params1 = document.getElementById('report1paramshidden').value;
}