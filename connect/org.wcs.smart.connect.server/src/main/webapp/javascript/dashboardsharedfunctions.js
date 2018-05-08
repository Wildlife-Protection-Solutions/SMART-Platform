
//get the possible parameters and put input boxes on the UI for users to fill out.
function getReport1ParametersForUi(){
	getReportParametersForUi(1);
}
function getReport2ParametersForUi(){
	getReportParametersForUi(2);
}

//get the possible parameters and put input boxes on the UI for users to fill out.
function getReportParametersForUi(num){
	//clear custom parameter from any previous reports
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
		document.getElementById('report1paramshidden').value = "";
		document.getElementById('report2paramshidden').value = "";
	}
	parent.innerHTML = "<font style='color:red'>" +i18n("dashboard.loadingparameters") + "</font>";
	
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
	
}

function showParamaterSelection1(){
	var json = JSON.parse(this.responseText);
	showParamaterSelection("customparameters1", json, 1);
}

function showParamaterSelection2(){
	var json = JSON.parse(this.responseText);
	showParamaterSelection("customparameters2", json,2);
}


function showParamaterSelection(divid, json, num){
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
 	
 	
 	if(num ==1){
		var uuidhidden = document.getElementById('report1selecthidden');
 	}else{
		var uuidhidden = document.getElementById('report2selecthidden');
 	}
 	var uuid = uuidhidden.value;
 	
 	
 	//now get the CCAA stuff if needed.
	var oReq = new XMLHttpRequest();
	if(num==1){
		oReq.onload = checkForCCAAReport1;
	}else{
		oReq.onload = checkForCCAAReport2;
	}
	oReq.open("Get", REPORTURL  + "definition/" + uuid , true);
	oReq.send();

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
//			displayDialog('caSelector1', "main");
			document.getElementById('editCasButton1').style.display = "inline-block";
		}else{
//			displayDialog('caSelector2', "main");
			document.getElementById('editCasButton2').style.display = "inline-block";
		}
	//hide the button if it is no longer a CCAA report
	}else{
		parseParametersIntoInputs(num);//need to parse custom params, this is also called after the chain of ajax from getCaList() 10 lines above, so it is run once in each case of this if/else, if that isn't obvious... I keep forgetting it. 
		if(num == 1){
			document.getElementById('editCasButton1').style.display = "none";
		}else{
			document.getElementById('editCasButton2').style.display = "none";
		}
	}
	
}

function addTextboxParamater(param, parent, newGroup){
	if(newGroup == true){
		var f = document.createElement("fieldset");
		var displayText = param.displayText;
		if (displayText == null) displayText = param.name;
		f.innerHTML = "<legend>" + displayText + ":" + "</legend>";
	}else{
		var f = document.createElement("p");
	}
	var newInput = document.createElement("input");
	newInput.setAttribute("id", param.name);
	newInput.setAttribute("name", param.name);
	newInput.style.width="100%";
	newInput.type = "text";

	newInput.oninput = updateReportCustomParamsHiddenValue;
	newInput.onpropertychange = newInput.oninput;
    
	f.appendChild(newInput);
	parent.insertBefore(f, parent.childNodes[4]);
}

function addBooleanParamater(param, parent, newGroup){
	if(newGroup == true){
		var f = document.createElement("fieldset");
		var displayText = param.displayText;
		if (displayText == null) displayText = param.name;
		f.innerHTML = "<legend>" + displayText + ":" + "</legend>";
	}else{
		var f = document.createElement("p");
	}

	var newList = document.createElement("select");
	newList.setAttribute("id", param.name);
	var optionT = new Option("true", "true");
	var optionF = new Option("false", "false");
	//Here we append that text node to our drop down list.
	newList.appendChild(optionT);
	newList.appendChild(optionF);
	newList.style.width="100%";
	
	newList.onchange = function(){updateReportCustomParamsHiddenValue();};

	f.appendChild(newList);
	parent.insertBefore(f, parent.childNodes[4]);
}


//check if the user selected or deselected "custom dates" and grey/de-grey the custom inputs. 
function checkForCustomDates(){
	var report1_startdate = picker1.toString("MMM DD, YYYY");;
	var report1_enddate = picker2.toString("MMM DD, YYYY");;
	
	//Update the Custom date fields all the time, so we can just use that date range when submitting
	var startDate = new Date();
	var e = document.getElementById('filterDate1');
	var days = e.options[e.selectedIndex].value;
	
	if(days != -9999){
		if(days > 0){
			startDate.setDate(startDate.getDate() - days);

		}else if(days == -30){//"month to date" options
			startDate.setDate(startDate.getDate() - getMonthToDate());
		}else if(days == -365){//year to date option
			startDate.setDate(startDate.getDate() - getYearToDate());
		}
		picker1.setDate(startDate, true);
		picker2.setDate(new Date(), true);

		report1_startdate = startDate;
		report1_enddate = new Date();
	}
	
	
	startDate = new Date();
	endDate = new Date();
	e = document.getElementById('filterDate2');
	if(e.options[e.selectedIndex] != null){
		days = e.options[e.selectedIndex].value;
	}else{
		return; //nothing selected 
	}
	if(days != -9999){
		if(days > 0){
			if(days > 1000){//the special ones, that are related to report1
				var daysearlier = Math.round(days /10000); //the values are are all 11112222 where the 1's represent the number of days before Report 1, and the 2's represent the period of time. So 00070007 is a week before R1 and lasts for a week.
				var dayslong = days % 1000; //remainder gives you the last 4 digits, which is how many days long the reporting period is.

				var r1startDate = new Date(document.getElementById('report1From').value.substring(4));
				var r1endDate = new Date(document.getElementById('report1To').value.substring(4));

				if(days == "03650365"){//just shift all dates 1 year earlier in this case, the rest all relative to the start date
					startDate.setYear(r1startDate.getFullYear());
					startDate.setMonth(r1startDate.getMonth());
					startDate.setDate(r1startDate.getDate() - daysearlier);
					
					endDate.setYear(r1endDate.getFullYear());
					endDate.setMonth(r1endDate.getMonth());
					endDate.setDate(r1endDate.getDate() - daysearlier);
					
				}else{
					
					startDate.setYear(r1startDate.getFullYear());
					startDate.setMonth(r1startDate.getMonth());
					startDate.setDate(r1startDate.getDate() - daysearlier);
					
					endDate.setYear(r1startDate.getFullYear());
					endDate.setMonth(r1startDate.getMonth());
					endDate.setDate(r1startDate.getDate() + (dayslong - daysearlier));
				}
			}else{
				startDate.setDate(startDate.getDate() - days);
			}
		}else if(days == -30){//"month to date" options
			startDate.setDate(startDate.getDate() - getMonthToDate());
		}else if(days == -365){//year to date option
			startDate.setDate(startDate.getDate() - getYearToDate());
		}else if(days==-9998){//same as report 1
			startDate = report1_startdate;
			endDate = report1_enddate;
		}
		picker3.setDate(startDate, true);
		picker4.setDate(endDate, true);
	}

	var date = document.getElementById('filterDate1');
	if(date.value == -9999){
		document.getElementById('report1From').disabled = false;
		document.getElementById('report1To').disabled = false;
	}else{
		document.getElementById('report1From').disabled = true;
		document.getElementById('report1To').disabled = true;
	}
	
	var date = document.getElementById('filterDate2');
	if(date.value == -9999){
		document.getElementById('report2From').disabled = false;
		document.getElementById('report2To').disabled = false;
	}else{
		document.getElementById('report2From').disabled = true;
		document.getElementById('report2To').disabled = true;
	}
	if(document.getElementById('savedatebutton1') != null){
		document.getElementById('savedatebutton1').disabled = true;
		document.getElementById('savedatebutton2').disabled = true;
	}
}

//enable save date buttons
function date1Changed(){
	changeReport1Date();//update the report 2 dates, because they could have a Report1-dependant date selected.
}
function date2Changed(){
	//nothing for this event yet
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
 	    opt.innerHTML = report.name + " [" + report.conservationArea + "]";
 	    opt2.value = report.uuid
	    opt2.innerHTML = report.name + " [" + report.conservationArea + "]";
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
        i18n: pickaday_i18n,
        onSelect: function(){date1Changed();}
	});

	picker2 = new Pikaday({
		field: document.getElementById('report1To'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n,
        onSelect: function(){date1Changed();}
	});
	
	picker3 = new Pikaday({
		field: document.getElementById('report2From'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n,
        onSelect: function(){date2Changed();}
	});

	picker4 = new Pikaday({
		field: document.getElementById('report2To'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n,
        onSelect: function(){date2Changed();}
	});
}

function getDashboard(uuid){
	var oReq = new XMLHttpRequest();
 	oReq.onload = updateReportsFromDashBoardJson;
 	oReq.open("Get", DashboardAPIURL + "/" + uuid, true);
 	oReq.send();
}

function updateReportsFromDashBoardJson(){
	
	if (this.status != 200 && this.status != 201) {
		var msg = "";
		if (this.status == 401){
			msg += i18n("dashboard.unauthorized");
		}else{
			try {
				msg = JSON.parse(this.responseText).error
			} catch (err) {
			}
		}
		displayError(msg);
		return;
	}

	
	var dashboard = JSON.parse(this.responseText);
	if(dashboard.uuid != null){
		document.getElementById('report1selecthidden').value = dashboard.reportUuid1;
		document.getElementById('report2selecthidden').value = dashboard.reportUuid2;
		
		document.getElementById('report1paramshidden').value = dashboard.parameterList1;
		document.getElementById('report2paramshidden').value = dashboard.parameterList2;
		
		var from1override=0;
		var from2override=0;
		if(dashboard.dateRange1 == -30){
			from1override = getMonthToDate();
		}else if(dashboard.dateRange1 == -365){
			from1override = getYearToDate();
		}
		
		if(dashboard.dateRange2 == -30){
			from2override = getMonthToDate();
		}else if(dashboard.dateRange2 == -365){
			from2override = getYearToDate();
		}
		document.getElementById('filterDate1').value = dashboard.dateRange1;
		document.getElementById('filterDate2').value = dashboard.dateRange2;
		
		DATEFILTER2GLOBAL = dashboard.dateRange2;

		
		//if it is a "... to date" type of date range, we need to override the saved from-to date range
		if(from1override == 0){//no override, setup dates as they are in the database
			document.getElementById('report1From').value = dashboard.customDate1From;
			document.getElementById('report1To').value = dashboard.customDate1To;
			
			if (document.getElementById('reportdate1') != null){
				document.getElementById('reportdate1').innerHTML = dashboard.customDate1From + " - " + dashboard.customDate1To;
				document.getElementById('reportdate1').parentElement.style.display="block";
			}
		}else{
			var start = new Date();
			start.setDate(start.getDate() - from1override);
			picker1.setDate(start, true);
			picker2.setDate(new Date(), true);

			//now that the picker is set, update the reportdate1 field from the correct picker date 
			if (document.getElementById('reportdate1') != null){

				document.getElementById('report1From').value = picker1.toString("MMM DD, YYYY");
				document.getElementById('report1To').value = picker2.toString("MMM DD, YYYY");
			
				document.getElementById('reportdate1').innerHTML = picker1.toString("MMM DD, YYYY") + " - " + picker2.toString("MMM DD, YYYY");
				document.getElementById('reportdate1').parentElement.style.display="block";
			}
		}
		

		
		if(from2override == 0){//no override, setup dates as they are in the database
			document.getElementById('report2From').value = dashboard.customDate2From;
			document.getElementById('report2To').value = dashboard.customDate2To;
			
			if (document.getElementById('reportdate2') != null){
				document.getElementById('reportdate2').innerHTML = dashboard.customDate2From + " - " + dashboard.customDate2To;
				document.getElementById('reportdate2').parentElement.style.display="block";
			}
		}else{
			var start = new Date();
			start.setDate(start.getDate() - from2override);
			picker3.setDate(start, true);
			picker4.setDate(new Date(), true);

			//now that the picker is set, update the reportdate1 field from the correct picker date 
			if (document.getElementById('reportdate2') != null){

				document.getElementById('report2From').value = picker3.toString("MMM DD, YYYY");
				document.getElementById('report2To').value = picker4.toString("MMM DD, YYYY");
			
				document.getElementById('reportdate2').innerHTML = picker3.toString("MMM DD, YYYY") + " - " + picker4.toString("MMM DD, YYYY");
				document.getElementById('reportdate2').parentElement.style.display="block";
			}
		}


		
		
		//if we are not are the admin page we can write the label in the header, otherwise we put it in the input box on the admin page
		if(document.getElementById('report1select') == null){
			document.getElementById('dashboard').innerHTML = dashboard.label;
			document.getElementById('dashboard').dataset.uuid = dashboard.uuid;
			
			var op = document.querySelectorAll('#admin-selectlist > option[value="' + dashboard.uuid + '"]')
			if (op != null && op[0] != null){
				op[0].selected = true;
			}
			
		}else{//on the admin page we also have a label input box
			document.getElementById('dashboardlabeltext').value = dashboard.label;
		}
	}else{
		document.getElementById('dashboard').innerHTML = i18n("dashboard.nodefaultdashboard");
		document.getElementById('loading1').style.display = "none";
		document.getElementById('loading2').style.display = "none";
	}
	
	if(document.getElementById('report1select') != null){ //if we are on the admin page
		getReport1ParametersForUi();
		getReport2ParametersForUi();
	}
	checkForCustomDates();
	
	if(dashboard.uuid != null){
		setTimeout(function(){
			runReports();
		}, 500);
	}
}

//actually run the reports and put the results in the iframes
function runReports(){
	rerunReport1();
	rerunReport2();
}
function rerunReport1(){
	var framediv = document.getElementById("iframe1div");
	while (framediv.firstChild) {
	    framediv.removeChild(framediv.firstChild);
	}
	document.getElementById('loading1').style.display = "block";
	
	var iframe = createIframe();
	document.getElementById('iframe1div').appendChild(iframe);
	iframe.src = resolve(generateRelativeUrl(REPORTURL, 1));

	iframe.onload = function(){
		frame1load();
	};
	
	if(picker1.getDate() != null && document.getElementById('rpoertdate1') != null ){
		
		document.getElementById('reportdate1').innerHTML = picker1.toString("MMM DD, YYYY") + " - " + picker2.toString("MMM DD, YYYY");
	}
}

function rerunReport2(){
	var framediv = document.getElementById("iframe2div");
	while (framediv.firstChild) {
	    framediv.removeChild(framediv.firstChild);
	}
	document.getElementById('loading2').style.display = "block";
	
	var iframe = createIframe();
	iframe.src = resolve(generateRelativeUrl(REPORTURL, 2));
	document.getElementById('iframe2div').appendChild(iframe);
	
	iframe.onload = function(){
		frame2load();
	};

	if(picker3.getDate() != null && document.getElementById('rpoertdate2') != null ){
		document.getElementById('reportdate2').innerHTML = picker3.toString("MMM DD, YYYY") + " - " + picker4.toString("MMM DD, YYYY");
	}
}

function createIframe(){
	var iframe= document.createElement('iframe');
	iframe.width = "100%";
	iframe.height = "100%";
	iframe.setAttribute('frameborder', 0);
	iframe.allowfullscreen = true;
	iframe.sandbox = "allow-scripts";
	iframe.style.display = "none";
	return iframe;
}


function generateRelativeUrl(root, report){
	if(report == 1){//get the data from the inputs for report #1 
		var uuid = document.getElementById('report1selecthidden').value;
		if(uuid == ""){
			return "about:blank";
		}
		//add the UUID to the url
		var url = root + uuid + "?format=HTML" ;
		
		//add the dates to the url
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

function editcas1(){
	displayDialog('caSelector1', 'report1');
}

function editcas2(){
	displayDialog('caSelector2', 'report2');
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
		if (cas[i].uuid == '00000000-0000-0000-0000-000000000000') continue; //do not add ccaa to list
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
	
	parseParametersIntoInputs(num);
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
	
	//get custom parameters
	var parent = document.getElementById("customparameters1");
	var children = parent.children;  
	for (var i = 0; i < children.length; i++) {
		 var child = children[i].children[1]; //get the actual "input" element
		 str = str + child.id + "," + child.value;
	 }
	
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
	return str;
}

function getRepor2CustomParameters(){
	var str = "";
	//get custom parameters
	var parent = document.getElementById("customparameters2");
	var children = parent.children;  
	for (var i = 0; i < children.length; i++) {
		 var child = children[i].children[1]; //get the actual "input" element
		 str = str + child.id + "," + child.value;
	 }
	
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
	return str;
}

function parseParametersIntoInputs(num){
	if(num == 1){
		var params1str = document.getElementById('report1paramshidden').value; 
		var queryparts = params1str.split('&cafilter=');
		
		var params1 = queryparts[0].split(',');

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
		if (queryparts[1] != null){
			var cafilter = queryparts[1].split(',');
			for(var i=0; i < cafilter.length; i++){
				var input = getElementInsideContainer("caCheckboxes1",cafilter[i]);
				if(input != null){
					input.checked = true;
				}
			}
		}
	}else{
		var params2str = document.getElementById('report2paramshidden').value; 
		var queryparts = params2str.split('&cafilter=');
		
		var params2 = queryparts[0].split(',');

		//pull out any custom parameters
		for(var i=0; i < params2.length; i+=2){
			var name = params2[i];
			var value = params2[i+1];
			var input = getElementInsideContainer("customparameters2",name);
			if(input != null){
				input.value = value;
			}
		}
		
		//pull out any ca filters and check off the correct boxes
		if (queryparts[1] != null){
			var cafilter = queryparts[1].split(',');
			for(var i=0; i < cafilter.length; i++){
				var input = getElementInsideContainer("caCheckboxes2",cafilter[i]);
				if(input != null){
					input.checked = true;
				}
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


function frame1load(){
	document.getElementById('loading1').style.display = "none";
	document.getElementById('iframe1div').firstChild.style.display = "block";
}

function frame2load(){
	document.getElementById('loading2').style.display = "none";
	document.getElementById('iframe2div').firstChild.style.display = "block";
}

function updateReportCustomParamsHiddenValue(){
	document.getElementById('report1paramshidden').value = getRepor1CustomParameters();
	document.getElementById('report2paramshidden').value = getRepor2CustomParameters();
}

function getYearToDate(){
	Date.prototype.isLeapYear = function() {
	    var year = this.getFullYear();
	    if((year & 3) != 0) return false;
	    return ((year % 100) != 0 || (year % 400) == 0);
	};

	// Get Day of Year
	Date.prototype.getDOY = function() {
	    var dayCount = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
	    var mn = this.getMonth();
	    var dn = this.getDate();
	    var dayOfYear = dayCount[mn] + dn;
	    if(mn > 1 && this.isLeapYear()) dayOfYear++;
	    return dayOfYear;
	};
	
	var date = new Date();
	return date.getDOY() - 1;
}

function getMonthToDate(){
	var date = new Date();
	return date.getDate() - 1;
}

function changeReport1Date(){
	
	//turn off all special date options
	var items = document.getElementsByClassName("extrareport2dates");
	for(var i = 0; i < items.length; i++){
	   items.item(i).style.display = "none";  
	}
	
	//turn on the ones we want with the the selected option
	var turn_on = []; 
	date1 = document.getElementById('filterDate1').value;
	if(date1 == 1){
		turn_on.push(document.getElementById('previousday'));
		turn_on.push(document.getElementById('samedaylastweek'));
		turn_on.push(document.getElementById('samedaylastmonth'));
		turn_on.push(document.getElementById('samedaylastyear'));
	}else if(date1 == 7){
		turn_on.push(document.getElementById('previousweek'));
		turn_on.push(document.getElementById('4weeksprevious'));
		turn_on.push(document.getElementById('sameweeklastyear'));
	}else if(date1 ==30){
		turn_on.push(document.getElementById('previousmonth'));
		turn_on.push(document.getElementById('samemonthlastyear'));
	}else if(date1 == 180){				
		turn_on.push(document.getElementById('previous6month'));
		turn_on.push(document.getElementById('same6monthslastyear'));
	}else if(date1 == 365 || date1 == -9999){
		turn_on.push(document.getElementById('previousyear'));
	}
	
	
	for(var i = 0; i < turn_on.length; i++){
		turn_on[i].style.display = "inline";
	}
	checkForCustomDates();
	
	//document.getElementById('filterDate2').value = DATEFILTER2GLOBAL; //hack to make sure we show the report 2 filter, even if it is a special one that wasn't drawn at the time we laoded the dashboards in getDashboard()  
}

function checkForMissingReport2Date(){
	if(document.getElementById('filterDate2').value == ""){
		document.getElementById('filterDate2').value = "huh";//DATEFILTER2GLOBAL;
	}
}
