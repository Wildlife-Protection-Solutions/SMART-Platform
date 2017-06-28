var SHARED_LINK_URL = "../api/sharedlink/";
var USER_URL = "../api/connectuser/getCurrent";

var queries;
var lastSorted;
var to; //timeout to slow auto-search a bit. It is cleared each time another character/change is typed so we don't fire too many updates too fast.

var definedDates = ["query.last30days","query.last60days","query.monthtodate","query.lastmonth","query.yeartodate","query.lastyear","query.alldates","query.custom"];
var definedDateKeys = ["last30days", "last60days", "monthtodate", "lastmonth", "yeartodate", "lastyear", "alldates", "custom"];

var startDatePicker, endDatePicker;

var shpValues = ["entityobservation", "entitywaypoint","intelligencerecord",  "surveymission",
                 "surveymissiontrack", "observationobservation", "observationwaypoint", 
                 "patrolobservation", "patrolquery", "patrolwaypoint", 
                 "surveyobservation", "surveywaypoint"];

var tifValues = ["entitygrid", "observationgrid","patrolgrid",  "surveygrid"];

var isDateChanging = false;

/* configure events on html elements */
window.onload = function(){

	document.getElementById('textsearch').value = search;
	getCaList();
	getQueryList();

	
	document.getElementById("runQueryButton").onclick = function(){
		closeDialog('urlOptionsDialog');
		window.open(generateUrl(QUERYLINKURL));
	};

	document.getElementById("cancel").onclick = function(){
		closeDialog('urlOptionsDialog');
	};
	
	document.getElementById("close").onclick = function(){
		var overlaydiv = document.querySelector(".overlay-widgetlevel2");
		overlaydiv.parentNode.removeChild(overlaydiv);
		resetUrlDialog();
		closeDialog('SharedLinksDialog');
	};
	
	document.getElementById("quickMinSelect").onchange = function(){
		var number = document.getElementById("quickMinSelect").value;
		if (number > 0){
			document.getElementById("expiresAfter").value = number;
			document.getElementById("expiresAfter").disabled=true;
		}else{
			document.getElementById("expiresAfter").disabled=false;
		}
	}
	
	document.getElementById("queryformat").onchange = function(){
		updateSRIDVisibility();
	}
	document.getElementById("sridDropdown").onchange = function(){
		var srid = document.getElementById("sridDropdown").value;
		document.getElementById("srid").disabled = true;
		document.getElementById("zoneLabel").style.display = "none";
		document.getElementById("utmzone").style.display = "none";
		
		if(srid == 32600 || srid == 32700){
			document.getElementById("zoneLabel").style.display = "block";
			document.getElementById("utmzone").style.display = "block";
			document.getElementById("srid").value = parseInt(srid) + parseInt(document.getElementById("utmzone").value);
		}else if(srid==-1){
			document.getElementById("srid").disabled = false;
		}else{
			
			document.getElementById("srid").value = srid;
		}
	}
	
	document.getElementById("utmzone").onchange = function(){
		document.getElementById("srid").value = parseInt(document.getElementById("utmzone").value) + parseInt(document.getElementById("sridDropdown").value);
	}
	
	
	
	document.getElementById("createcustomlinklink").onclick = function(){
		document.getElementById('createcustomlink').style.display = 'block';
		document.getElementById('createcustomlinktitle').style.display = 'none';
		return false;
	}
	
	document.getElementById("createlinkbutton").onclick = createSharedLink;
	
	//setup date picker for alert filters

	startDatePicker = new Pikaday({
		format: 'YYYY-MM-DD',
		field: document.getElementById('startdate'),
		firstDay: 1,
        minDate: new Date('1950-01-01'),
        yearRange: [1950,2050],
        i18n: pickaday_i18n,
        onSelect: selectCustom
	});

	endDatePicker = new Pikaday({
		field: document.getElementById('enddate'),
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

function updateSRIDVisibility(){
	var type = document.getElementById("queryformat").value;
	if (type == "shp" || type =="geojson"){
		document.getElementById("sridDropdownLobel").style.display = "block";
		document.getElementById("sridDropdown").style.display = "block";
		document.getElementById("srid").style.display = "block";
	}else{
		document.getElementById("sridDropdownLobel").style.display = "none";
		document.getElementById("sridDropdown").style.display = "none";
		document.getElementById("srid").style.display = "none";
		document.getElementById("zoneLabel").style.display = "none";
		document.getElementById("utmzone").style.display = "none";
	}
}
function canExecute(queryType){
	return executeableTypes.indexOf(queryType.toLowerCase())>=0;
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
	
	var startdate = document.getElementById("startdate");
	var enddate = document.getElementById("enddate");

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
			startdate.value = "";
			enddate.value = "";
		}else if (datekey== "custom"){
			
		}
		isDateChanging = false;

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
	var selectedCa = document.getElementById('caselect').value;
	
	var hide = document.getElementById('qhideexe').checked;
 	for (var i = 0; i < queries.length; i ++){
 		if(selectedCa == 'allcas' || selectedCa == queries[i].caUuid){
 			if(search == "" || isFoundInRow(queries[i]) ){
 				if (!hide || canExecute(queries[i].typeKey)){
 			 
 					drawnRowCount++;
 					var row = tableCreateRow(parent, 
 							[queries[i].conservationArea, queries[i].id, queries[i].name, queries[i].type,  null], 
 							"queryrow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
	 		
 					row.dataset.queryuuid = queries[i].uuid;
 					row.dataset.queryname = queries[i].name;
 					row.dataset.querytype = queries[i].typeKey;
 					row.dataset.isccaa = queries[i].isCcaa;
	 		
 					if (canExecute(queries[i].typeKey)){
 						var runicon = document.createElement("a");
 						runicon.className="run-icon";
 						runicon.title= i18n("query.runquery");
 						runicon.onclick = showQueryOptions;
 						row.childNodes[4].appendChild(runicon);
 					}
	 		
//	 		var filter = qdatefilter[queries[i].typeKey][0];
//	 		var csvlink = document.createElement("a");
//	 		csvlink.href="../api/query/" + queries[i].uuid + "?format=csv&date_filter=" + filter;
//	 		csvlink.innerHTML = "csv";
//	 		row.childNodes[5].appendChild(csvlink);
 				}
 			}
 		}
 	}
 	
 	if(queries.length == 0 || drawnRowCount == 0){ //no results or they were all filtered out
 		var row = document.createElement("div");
 		row.className = "queryrow errorsection";
 	    row.innerHTML = i18n("query.noqueriesfound");
 	    row.style.display = "block";
 		parent.appendChild(row);
 	}
}

function showQueryOptions(){
	var uuid = this.parentElement.parentElement.getAttribute('data-queryuuid');
	var name = this.parentElement.parentElement.getAttribute('data-queryname');
	var isccaa = this.parentElement.parentElement.getAttribute('data-isccaa');
	var querytype = this.parentElement.parentElement.getAttribute('data-querytype');
	
	
	document.getElementById("runqueryform").uuid.value = uuid;
	document.getElementById("runqueryform").name.value = name;
	if (isccaa === "true"){
		document.getElementById("cafilter").style.display="block";
	}else{
		document.getElementById("cafilter").style.display="none";
	}
	
	var poselement = document.querySelector("#querytable");
	var pos = getPosition(poselement);
	
	//update output format options
	//update shape option
	var isShape = shpValues.indexOf(querytype) >= 0;
	var item = document.querySelector("#queryformat option[value=shp]");
	var itemjson = document.querySelector("#queryformat option[value=geojson]");
	if (item != null){
		if (isShape){
			item.style.display = "block";
			itemjson.style.display = "block"; 
		}else{
			item.style.display = "none";
			itemjson.style.display = "none";
		}
	}
	//update tiff option
	var isTif = tifValues.indexOf(querytype) >= 0;
	var item = document.querySelector("#queryformat option[value=tif]");
	if (item != null){
		if (isTif){
			item.style.display = "block";
		}else{
			item.style.display = "none";
		}
	}
	
	//update datefilter options
	//remove all existing options
	var datefielddiv = document.getElementById("datefield");
	var currentselection = datefielddiv.value;
	while(datefielddiv.firstChild){
		datefielddiv.removeChild(datefielddiv.firstChild);
	}
	//add correct options for query type; select the previously
	//selected option if available
	var selectedIndex = 0;
	var ops = qdatefilter[querytype];
	for (var i = 0; i < ops.length; i++){
		var doption = ops[i];
		var name = datefilters[doption];
		
		var object = document.createElement("option");
		object.value = doption;
		object.innerHTML = name;
		datefielddiv.appendChild(object);
		
		if (currentselection == doption){
			selectedIndex = i;
		}
	}
	datefielddiv.selectedIndex = selectedIndex;

	// export format selection index
	var formatIndex = 0;
	var formatSelection = document.querySelector("#queryformat").value;
	var titem = document.querySelector("#queryformat option[value=" + formatSelection + "]");
	if (titem.style.display != "none"){
		formatIndex = titem.index;
	}
	document.querySelector("#queryformat").selectedIndex = formatIndex;
	 
	displayDialogLocation('urlOptionsDialog', pos.x, window.pageYOffset + 20);
	updateSRIDVisibility();
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
function initializeUrlDialog(){
	displayURLDialog(generateUrl(QUERYLINKURL));
}


function generateRelativeUrl(root){
	var uuid = document.getElementById('queryuuid').value;
	var format = document.getElementById('queryformat').value;
	var dateField = document.getElementById('datefield').value;
	
	var url = root + uuid +"?format=" + format + "&date_filter=" + dateField
	
	if(document.getElementById('startdate').value != ""){
		var startDate = new Date(document.getElementById('startdate').value.substring(4));//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		var startDateString = startDate.getFullYear() + "-" + (startDate.getMonth() + 1) + "-" + startDate.getDate() + " 00:00:00";
		
		url = url + "&start_date=" + startDateString; 
	}
	if(document.getElementById('enddate').value != ""){
		var endDate = new Date(document.getElementById('enddate').value.substring(4)); //use end of the day, since it is the "to" date.
		var endDateString = endDate.getFullYear() + "-" + (endDate.getMonth()+1) + "-" + endDate.getDate() + " 23:59:59";
	
		url = url + "&end_date=" + endDateString; 
	}
	
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
	var type = document.getElementById("queryformat").value;
	if (type == "shp" || type =="geojson"){
		url = url + "&srid=" + document.getElementById('srid').value;
	}
	return url;
}

function generateUrl(root){
	return resolve(generateRelativeUrl(root));
}




function getCaList(){
	var oReq = new XMLHttpRequest();
		oReq.onload = populateCaList;
		oReq.open("Get", CAURL + '?includeSpatialBoundaries=false', true);
		oReq.send();
}


//only runs once, need to update it to clear list if this gets called more than one in the future. 
function populateCaList(){
	if (this.status != 200) {
		var msg = i18n("query.error");
		if (this.status == 401){
			msg += i18n("query.unauthorized");
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
	
	var oReq = new XMLHttpRequest();
	oReq.onload = setHomeCa;
	oReq.open("Get", USER_URL, true);
	oReq.send();
}

function setHomeCa(){
	if (this.status != 200) {
		var msg = i18n("query.error");
		if (this.status == 401){
			msg += i18n("query.unauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	var parent = document.getElementById('caselect')
	
	var users = JSON.parse(this.responseText);
	parent.value = users.homeCaUuid;
}

function selectAll(parent){
	var parent = document.getElementById('cafilteroptions'); 
	var children = parent.children;
	for (var i = 0; i < children.length; i++) {
	   var child = children[i].children[0];
	   child.checked = true;
	}
}

function selectNone(parent){
	var parent = document.getElementById('cafilteroptions');
	var children = parent.children;
	for (var i = 0; i < children.length; i++) {
	   var child = children[i].children[0];
	   child.checked = false;
	}
}

