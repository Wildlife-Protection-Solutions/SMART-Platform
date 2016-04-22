var reports;
var lastSorted;
var to; //timeout to slow auto-search a bit. It is cleared each time another character/change is typed so we don't fire too many updates too fast.

var definedDates = ["report.last30days","report.last60days","report.monthtodate","report.lastmonth","report.yeartodate","report.lastyear","report.alldates","report.custom"];
var definedDateKeys = ["last30days", "last60days", "monthtodate", "lastmonth", "yeartodate", "lastyear", "alldates", "custom"];

var startDatePicker, endDatePicker;

var isDateChanging = false;

var parameterNames = new Array();

/* configure events on html elements */
window.onload = function(){

	document.getElementById('textsearch').value = search;
	getCaList();
	getReportList();

	
	document.getElementById("runreportbutton").onclick = function(){
		
		//Validate parameters
		if (document.getElementById('cafilter').style.display.toUpperCase() != "NONE"){
			var valid = 0;
			var elements = document.getElementsByName('ccaafilter');
			for (var i = 0; i < elements.length; i ++){
				if (elements[i].checked){
					valid=1;
					break;
				}
			}
			if (valid == 0){
				window.alert("You must select at least one CA");
				return false;
			}
		}
		var csString = "";
		for(x=0; x < parameterNames.length; x++){
			name = parameterNames[x];
			if(document.getElementById(name).value == ""){
				window.alert("Missing Parameter Valid for: " + name);
				return false;
			}
		}
		
		closeDialog('reportOptionsDialog');
		window.open(generateUrl(REPORTURL));
	};

	document.getElementById("cancel").onclick = function(){
		closeDialog('reportOptionsDialog');
	};
	
	
	//setup date picker for alert filters

	startDatePicker = new Pikaday({
		format: 'YYYY-MM-DD',
		field: document.getElementById('Start Date'),
		firstDay: 1,
        minDate: new Date('1950-01-01'),
        yearRange: [1950,2050],
        i18n: pickaday_i18n,
        onSelect: selectCustom
	});

	endDatePicker = new Pikaday({
		field: document.getElementById('End Date'),
		firstDay: 1,
        minDate: new Date('1950-01-01'),
        yearRange: [1950,2050],
        i18n: pickaday_i18n,
        onSelect: selectCustom
	});


	//populate predefined dates
	var selectdiv = document.getElementById("defineddates");
	selectdiv.onchange = updateDates;
	for (var i = 0; i < definedDates.length; i ++){
		var object = document.createElement("option");
		object.value = definedDateKeys[i];
		object.innerHTML = i18n(definedDates[i]);
		selectdiv.appendChild(object);
		if (definedDateKeys[i] == "alldates"){
			selectdiv.selectedIndex = i;
			updateDates();
		}
	}
	
}

function selectCustom(){
	if (isDateChanging) return;
	var selectdiv = document.getElementById("defineddates");
	for (var i = 0; i < definedDateKeys.length; i ++){
		if (definedDateKeys[i] == "custom"){
			selectdiv.selectedIndex = i;
			return;
		}
	}
}

function updateDates(){
	var dd = document.getElementById("defineddates");
	var datekey = dd.options[dd.selectedIndex].value;
	
	var startdate = document.getElementById("Start Date");
	var enddate = document.getElementById("End Date");

	isDateChanging = true;
		if (datekey== "last30days"){
			var startYear = new Date();
			startYear.setDate(startYear.getDate() - 30);
			endDatePicker.setDate(new Date(), false);
			startDatePicker.setDate(startYear, false);
		}else if (datekey== "last60days"){
			var startYear = new Date();
			startYear.setDate(startYear.getDate() - 60);
			endDatePicker.setDate(new Date(), false);
			startDatePicker.setDate(startYear, false);
		}else if (datekey== "monthtodate"){
			var today = new Date();
			var startYear = new Date(today.getFullYear(), today.getMonth(), 1,0,0,0);
			endDatePicker.setDate(today, false);
			startDatePicker.setDate(startYear, false);
		}else if (datekey== "lastmonth"){
			var startYear = new Date();
			startYear.setMonth(startYear.getMonth() - 1);
			startYear.setDate(1);
			
			var endYear = new Date();
			endYear.setMonth(endYear.getMonth());
			endYear.setDate(0);
			
			endDatePicker.setDate(endYear, false);
			startDatePicker.setDate(startYear, false);
			
		}else if (datekey== "yeartodate"){
			var today = new Date();
			var startYear = new Date(today.getFullYear(), 0, 1,0,0,0);
			endDatePicker.setDate(today, false);
			startDatePicker.setDate(startYear, false);
		}else if (datekey== "lastyear"){
			var today = new Date();
			var startYear = new Date(today.getFullYear() - 1, 0, 1,0,0,0);
			var endYear = new Date(today.getFullYear() - 1, 11, 31, 23,59,59);
			endDatePicker.setDate(endYear, false);
			startDatePicker.setDate(startYear, false);
			
		}else if (datekey== "alldates"){
			startYear = new Date(1979, 0, 1,0,0,0);
			startDatePicker.setDate(startYear, false);
			var today = new Date();
			endDatePicker.setDate(today, false);
		}else if (datekey== "custom"){
			
		}
		isDateChanging = false;

}
function getReportList(){
	//clear current table
	var objects = document.querySelectorAll("div.reportrow");
	
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById('reporttable');
	
	var row = document.createElement("div");
	row.className="reportrow";
	row.innerHTML=i18n("report.refreshingreports");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = queryCallback;
 	oReq.open("Get", REPORTURL, true);
 	oReq.send();
}

function queryCallback(){
	if (this.status != 200) {
		var msg = "Error: ";
		if (this.status == 401){
			msg += "Unauthorized";
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	
 	reports = JSON.parse(this.responseText);
 	
 	sortTable("name");
 	createReportTable();
}

function sortTable(sortColumn){
	if(lastSorted == sortColumn){
		sortColumn = "-" + sortColumn;
		lastSorted = ""; //set it to nothing, so if clicked a 3rd time it sorts in ascending order again.
	}else{
		lastSorted = sortColumn; 
	}
	reports.sort(dynamicSort(sortColumn));
 	createReportTable();
}

function createReportTable(){ 
	var parent = document.getElementById('reporttable');
	

 	//clear current table
	var objects = document.querySelectorAll("div.reportrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	var drawnRowCount = 0;
	var selectedCa = document.getElementById('caselect').value; 
 	for (var i = 0; i < reports.length; i ++){
 		if(selectedCa == 'allcas' || selectedCa == reports[i].caUuid){
 		 if(search == "" || isFoundInRow(reports[i]) ){
 			drawnRowCount++;
	 		var row = tableCreateRow(parent, 
	 				[reports[i].conservationArea, reports[i].id, reports[i].name, null], 
	 				"reportrow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
	 		
	 		row.dataset.reportuuid = reports[i].uuid;
	 		row.dataset.reportname = reports[i].name;
	 		row.dataset.isccaa = reports[i].isCcaa;
	 		
	 		var runicon = document.createElement("a");
	 		runicon.className="run-icon";
	 		runicon.title= i18n("reports.runreport");
	 		runicon.onclick = showReportOptions;
	 		row.childNodes[3].appendChild(runicon);
	 		
 		 }
 		}
 	}
 	
 	if(reports.length == 0 || drawnRowCount == 0){ //no results or they were all filtered out
 		var row = document.createElement("div");
 		row.className = "reportrow errorsection";
 	    row.innerHTML = i18n("report.noreportsfound");
 	    row.style.display = "block";
 		parent.appendChild(row);
 	}
}

function showReportOptions(){
	var uuid = this.parentElement.parentElement.getAttribute('data-reportuuid');
	var name = this.parentElement.parentElement.getAttribute('data-reportname');
	var isccaa = this.parentElement.parentElement.getAttribute('data-isccaa');
	
	document.querySelector("#dialogerror").style.display = "none";
	
	document.getElementById("runreportform").uuid.value = uuid;
	document.getElementById("runreportform").name.value = name;
	if (isccaa === "true"){
		document.getElementById("cafilter").style.display="block";
	}else{
		document.getElementById("cafilter").style.display="none";
	}
	
	var poselement = document.querySelector("#reporttable");
	var pos = getPosition(poselement);
	
	//clear custom paramater from any previous reports
	var parent = document.getElementById("customParamters");
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
	parent.innerHTML = "<font style='color:red'>Loading custom parameters.</font>";

	//update report parameters required
	var oReq = new XMLHttpRequest();
	oReq.onload = showParamaterSelection;
	oReq.open("Get", REPORTURL  + uuid + "/params", true);
	oReq.send();

	
	document.querySelector("#reportformat").selectedIndex = 0;
	displayDialogLocation('reportOptionsDialog', pos.x, window.pageYOffset + 20);

}

//callback from getting parameters
//this function adds GUI items to match each required parameter
function showParamaterSelection(){
	var parent = document.getElementById("customParamters");
	document.getElementById("paramaters_fieldset").style.display = "none";
	parent.innerHTML = "";
	var json = JSON.parse(this.responseText);
 	for (var i = 0; i < json.length; i++){
 		if(json[i].type == "GROUP"){
 			if (json[i].name=="Report Dates"){
 				//start and end dates already made; use default gui but still add to parameter list
 				document.getElementById("paramaters_fieldset").style.display = "block";
 				for (var x = 0; x < json[i].children.length; x++){
 					parameterNames.push(json[i].children[x].name);
 				}
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

function isFoundInRow(row){
	if(isIn(row.conservationArea)|| isIn(row.name)|| isIn(row.type)|| isIn(row.id)){
		return true;
	}
	return false;
}

function isIn(text){
	text = text.toLowerCase();
	if(text.indexOf(search) > -1){
		return true;
	}
	return false;
}


//provides a custom sorting function,  
//param - property: the name of the key to sort on, "-" in the first character means sort descending 
function dynamicSort(property) {
    var sortOrder = 1;
    if(property[0] === "-") {
        sortOrder = -1;
        property = property.substr(1);
    }
    return function (a,b) {
        var result = (a[property].toUpperCase() < b[property].toUpperCase()) ? -1 : (a[property].toUpperCase() > b[property].toUpperCase()) ? 1 : 0;
        return result * sortOrder;
    }
}

function searchChanged(){
	clearTimeout(to);
	setTimeout(function(){
		search = document.getElementById('textsearch').value;
		search = search.toLowerCase();
		createReportTable();	
	}, 600);
	

}
function getUrlOnly(){
	  window.prompt(i18n("report.copytoclipboard"), generateUrl(REPORTURL));
}



function generateUrl(root){
	var uuid = document.getElementById('reportuuid').value;
	
	//add the UUID
	var url = root + uuid + "?format=" + document.getElementById('reportformat').value;

	//add the cafilter if applicable
	if (document.getElementById('cafilter').style.display.toUpperCase() != "NONE"){
		var elements = document.getElementsByName('ccaafilter');
		var cafilter = "";
		for (var i = 0; i < elements.length; i ++){
			if (elements[i].checked){
				cafilter = cafilter + "," + elements[i].value;
			}
		}
		if (cafilter.length > 0){
			url = url + "&cafilter=" + cafilter.substring(1);
		}
	}

	//Add all other parameters to parameterList comma separated list of name/value pairs.
	var csString = "";
	for(x=0; x < parameterNames.length; x++){
		name = parameterNames[x];
		if (name == "Start Date" || name == "End Date"){
			//parse out correct date format
			var startDate = new Date(document.getElementById(name).value.substring(4));
			var dateStr = startDate.getFullYear() + "-" + (startDate.getMonth()+1) + "-" + startDate.getDate() + " 00:00:00";
			csString += name + "," +dateStr + ",";
		}else{
			csString += name + "," + document.getElementById(name).value + ",";
		}
	}
	url = url + "&parameterList=" + csString;
	return resolve(url);
}




function getCaList(){
	var oReq = new XMLHttpRequest();
		oReq.onload = populateCaList;
		oReq.open("Get", CAURL, true);
		oReq.send();
}


//only runs once, need to update it to clear list if this gets called more than one in the future. 
function populateCaList(){
	if (this.status != 200) {
		var msg = i18n("report.error");
		if (this.status == 401){
			msg += i18n("report.unauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	var parent = document.getElementById('caselect')
	var filterparent = document.getElementById("cafilteroptions");
	
	var cas = JSON.parse(this.responseText);
	for (var i = 0; i < cas.length; i ++){
		var opt = document.createElement('option');
		var label = cas[i].label;
		value = cas[i].uuid;
	    opt.value = value;
	    opt.innerHTML = label;
	    parent.appendChild(opt);
	    
	    if (cas[i].status.toUpperCase() == "DATA"){
	    	var lbl = document.createElement("label");
	    	var span = document.createElement("span");
	    	var chk = document.createElement("input");
	    	chk.type = "checkbox";
	    	chk.value=cas[i].uuid
	    	chk.name="ccaafilter";
	    	lbl.appendChild(chk);
	    	lbl.style.display="block";
	    	span.innerHTML=cas[i].label;
	    	lbl.appendChild(span);
	    	filterparent.appendChild(lbl);
	    }
	}			
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


function addBooleanParamater(param, parent, newGroup){
	if(newGroup == true){
		var f = document.createElement("fieldset");
		f.innerHTML = "<legend>" + param.displayText + ":" + "</legend>";
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

	f.appendChild(newList);
	parent.insertBefore(f, parent.childNodes[4]);
	
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