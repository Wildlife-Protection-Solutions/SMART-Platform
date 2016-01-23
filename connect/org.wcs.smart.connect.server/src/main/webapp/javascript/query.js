var QUERYURL = "../api/query/";
var CAURL = "../api/conservationarea/";
var queries;
var lastSorted;
var to; //timeout to slow auto-search a bit. It is cleared each time another character/change is typed so we don't fire too many updates too fast.

var definedDates = ["Last 30 Days", "Last 60 Days", "Month to Date", "Last Month", "Year to Date", "Last Year", "All Dates", "Custom..."];
var definedDateKeys = ["last30days", "last60days", "monthtodate", "lastmonth", "yeartodate", "lastyear", "alldates", "custom"];

var startDatePicker, endDatePicker;
/* configure events on html elements */
window.onload = function(){
	document.getElementById('textsearch').value = search;
	getCaList();
	getQueryList();

	
	document.getElementById("runQueryButton").onclick = function(){
		window.open(generateUrl());
	};

	document.getElementById("cancel").onclick = function(){
		closeDialog('queryOptionsDialog');
	};
	
	
	//setup date picker for alert filters

	startDatePicker = new Pikaday({
		field: document.getElementById('startdate'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050]
	});

	endDatePicker = new Pikaday({
		field: document.getElementById('enddate'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050]
	});

	//populate predefined dates
	var selectdiv = document.getElementById("defineddates");
	selectdiv.onchange = updateDates;
	for (var i = 0; i < definedDates.length; i ++){
		var object = document.createElement("option");
		object.value = definedDateKeys[i];
		object.innerHTML = definedDates[i];
		selectdiv.appendChild(object);
		if (definedDateKeys[i] == "alldates"){
			selectdiv.selectedIndex = i;
			updateDates();
		}
	}
	
}

function updateDates(){
	var dd = document.getElementById("defineddates");
	var datekey = dd.options[dd.selectedIndex].value;
	
	var startdate = document.getElementById("startdate");
	var enddate = document.getElementById("enddate");

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
			startdate.value = "";
			enddate.value = "";
		}else if (datekey== "custom"){
			
		}

}
function getQueryList(){
	//clear current table
	var objects = document.querySelectorAll("div.queryrow");
	
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById('querytable');
	
	var row = document.createElement("div");
	row.className="queryrow";
	row.innerHTML=i18n("query.refreshingqueries");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = queryCallback;
 	oReq.open("Get", QUERYURL, true);
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
	
 	queries = JSON.parse(this.responseText);
 	
 	sortTable("name");
 	createQueryTable();
}

function sortTable(sortColumn){
	if(lastSorted == sortColumn){
		sortColumn = "-" + sortColumn;
		lastSorted = ""; //set it to nothing, so if clicked a 3rd time it sorts in ascending order again.
	}else{
		lastSorted = sortColumn; 
	}
 	queries.sort(dynamicSort(sortColumn));
 	createQueryTable();
}

function createQueryTable(){ 
	var parent = document.getElementById('querytable');
	

 	//clear current table
	var objects = document.querySelectorAll("div.queryrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	var drawnRowCount = 0;
 	for (var i = 0; i < queries.length; i ++){
 		selectedCa = document.getElementById('caselect').value; 
 		if(selectedCa == 'allcas' || selectedCa == queries[i].conservationArea){
 		 if(search == "" || isFoundInRow(queries[i]) ){
 			drawnRowCount++;
	 		var row = tableCreateRow(parent, 
	 				[queries[i].conservationArea, queries[i].id, queries[i].name, queries[i].type, null, null], 
	 				"queryrow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
	 		
	 		row.dataset.queryuuid = queries[i].uuid;
	 		row.dataset.queryname = queries[i].name;
	 		row.dataset.isccaa = queries[i].isCcaa;
	 		
	 		var runicon = document.createElement("a");
	 		runicon.className="run-icon";
	 		runicon.title= i18n("query.runquery");
	 		runicon.onclick = showQueryOptions;
	 		row.childNodes[4].appendChild(runicon);
	 		
	 		var csvlink = document.createElement("a");
	 		csvlink.href="../api/query/" + queries[i].uuid + "?format=csv&date_filter=waypointdate";
	 		csvlink.innerHTML = "csv";
	 		row.childNodes[5].appendChild(csvlink);
 		 }
 		}
 	}
 	
 	if(queries.length == 0 || drawnRowCount == 0){ //no results or they were all filtered out
 		var row = document.createElement("div");
 		row.style.display = "default";
 		row.className = "queryrow errorsection";
 	    row.innerHTML = i18n("query.noqueriesfound");
 		parent.appendChild(row);
 	}
}

function showQueryOptions(){
	var uuid = this.parentElement.parentElement.getAttribute('data-queryuuid');
	var name = this.parentElement.parentElement.getAttribute('data-queryname');
	var isccaa = this.parentElement.parentElement.getAttribute('data-isccaa');
	
	document.querySelector("#dialogerror").style.display = "none";
	
	document.getElementById("runqueryform").uuid.value = uuid;
	document.getElementById("runqueryform").name.value = name;
	if (isccaa === "true"){
		document.getElementById("cafilter").style.display="block";
	}else{
		document.getElementById("cafilter").style.display="none";
	}
	
	displayDialog('queryOptionsDialog', 'querytable');
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
		createQueryTable();	
	}, 600);
	

}
function getUrlOnly(){
	  window.prompt("Copy to clipboard: Ctrl+C, Enter", generateUrl());
}



function generateUrl(){
	var uuid = document.getElementById('queryuuid').value;
	var format = document.getElementById('queryformat').value;
	var dateField = document.getElementById('datefield').value;
	
	var url = QUERYURL + uuid +"?format=" + format + "&date_filter=" + dateField
	
	if(document.getElementById('startdate').value != ""){
		var startDate = new Date(document.getElementById('startdate').value.substring(4));//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		var startDateString = startDate.getFullYear() + "-" + startDate.getMonth() + "-" + startDate.getDate() + " 00:00:00";
		
		url = url + "&start_date=" + startDateString; 
	}
	if(document.getElementById('enddate').value != ""){
		var endDate = new Date(document.getElementById('enddate').value.substring(4)); //use end of the day, since it is the "to" date.
		var endDateString = endDate.getFullYear() + "-" + endDate.getMonth() + "-" + endDate.getDate() + " 23:59:59";
	
		url = url + "&end_date=" + endDateString; 
	}
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

