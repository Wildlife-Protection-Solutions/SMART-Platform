var reports;
var lastSorted;
var to; //timeout to slow auto-search a bit. It is cleared each time another character/change is typed so we don't fire too many updates too fast.
var SHARED_LINK_URL = "../api/sharedlink/";
var USER_URL = "../api/connectuser/getCurrent";
const REPORT_URL = "../api/report/"

var definedDates = ["report.last30days","report.last60days","report.monthtodate","report.lastmonth","report.yeartodate","report.lastyear","report.alldates","report.custom"];
var definedDateKeys = ["last30days", "last60days", "monthtodate", "lastmonth", "yeartodate", "lastyear", "alldates", "custom"];

var startDatePicker, endDatePicker;

var isDateChanging = false;

var parameterNames = new Array();

var selectedFolder = -1;
var folderId;

/* configure events on html elements */
window.onload = function(){
	menuCheckOnload();

	document.getElementById('textsearch').value = search;
	getCaList();

	
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
				window.alert(i18n("report.carequired"));
				return false;
			}
		}
		var csString = "";
		for(x=0; x < parameterNames.length; x++){
			name = parameterNames[x];
			if(document.getElementById(name).dataset.isRequired === 'true' && document.getElementById(name).value == ""){
				window.alert(i18n("report.missingparam") + name);
				return false;
			}
		}
		
		closeDialog('urlOptionsDialog');
		window.open(generateUrl(REPORTURL));
	};

	document.getElementById("cancel").onclick = function(){
		closeDialog('urlOptionsDialog');
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
	document.getElementById("createcustomlinklink").onclick = function(){
		document.getElementById('createcustomlink').style.display = 'block';
		document.getElementById('createcustomlinktitle').style.display = 'none';
		return false;
	}
	
	document.getElementById("createlinkbutton").onclick = createSharedLink;
	
	document.getElementById("close").onclick = function(){
		var overlaydiv = document.querySelector(".overlay-widgetlevel2");
		overlaydiv.parentNode.removeChild(overlaydiv);
		resetUrlDialog();
		closeDialog('SharedLinksDialog');
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
	
	
	// we are catching events at the query-list level
	// in case we add or remove folders/items dynamically
	const reportFolderList = document.getElementById('foldertable');
	reportFolderList.addEventListener('click', function(evt) {
		var element = evt.target;
		var openonly = false;

		if (!element.classList.contains('folder-icon')){
			//select folder
			if(!element.classList.contains('folder-name')) {
				element = element.parentNode;
			}
			if(!element.classList.contains('folder-name')) return;

			selectFolder(element);
			updateReportTable();
			//if it's not open then open it
			openonly = true;
		}
		// bubble up events to the parent folder or folder-item
		if(!element.classList.contains('folder-name')) {
			element = element.parentNode;
		}
		if(element.classList.contains('folder-name')) {
			// clicking on the folder toggles the folder icon
			// as well as toggling the display of the folder contents
			const folderIcon = element.getElementsByClassName('folder-icon')[0];
			if (openonly){
				if(!folderIcon.classList.contains('fa-folder-open')) {
					folderIcon.classList.remove('fa-folder');
					folderIcon.classList.add('fa-folder-open')
					toggleDisplay(element.nextElementSibling);
				}
			}else{
			
				if(folderIcon.classList.contains('fa-folder-open')) {
					folderIcon.classList.remove('fa-folder-open');
					folderIcon.classList.add('fa-folder');
				} else {
					folderIcon.classList.remove('fa-folder');
					folderIcon.classList.add('fa-folder-open')
				}
				toggleDisplay(element.nextElementSibling);

			}
		} 
	});
	
}

//element is the folder-name dive
function selectFolder(element){
	const folderTable = document.getElementById('foldertable');

	selectedFolder = element.getAttribute("data-folderid");

	element.classList.add('folder-selected');
	var path = element.getAttribute("data-path");
	if (path != null){
		document.getElementById('reportpath').innerHTML = path;
	}else{
		document.getElementById('reportpath').innerHTML = "All Reports";
	}
	
	Array.from(folderTable.getElementsByClassName("folder-selected")).forEach(function(ele){
		ele.classList.remove("folder-selected");
		ele.parentElement.dataset.selected = "false";
	});

	selectedFolder = element.getAttribute("data-folderid");
	var path = element.getAttribute("data-path");
	
	updateReportTable();
	element.parentElement.dataset.selected="true";
	element.classList.add('folder-selected');
	if (path != null){
		document.getElementById('reportpath').innerHTML = path;
	}else{
		document.getElementById('reportpath').innerHTML = i18n("report.allreports");
	}
	document.getElementById('reporttable').scrollTop = 0;
}

function toggleDisplay(element) {
    if(element.style.display == "none") {
      element.style.display = "";
    } else {
    	element.style.display = "none";
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

function showReportOptions(){
	var uuid = this.parentElement.parentElement.getAttribute('data-reportuuid');
	var name = this.parentElement.parentElement.getAttribute('data-reportname');
	var isccaa = this.parentElement.parentElement.getAttribute('data-isccaa');
	
	document.querySelector("#dialogerror").style.display = "none";
	
	document.getElementById("runreportform").uuid.value = uuid;
	document.getElementById("runreportform").name.value = name;
	document.getElementById("runreportform").querySelector("#reportname").innerHTML = name;
	if (isccaa === "true"){
		document.getElementById("cafilter").style.display="block";
	}else{
		document.getElementById("cafilter").style.display="none";
	}
	
	var poselement = document.querySelector("#reporttable");
	var pos = getPosition(poselement);
	
	//clear custom paramater from any previous reports
	var parent = document.getElementById("customParameters");
	while (parent.firstChild) {
		parent.removeChild(parent.firstChild);
	}
	parent.innerHTML = "<font style='color:red'>" + i18n("report.loadcustomparams") + "</font>";

	//update report parameters required
	var oReq = new XMLHttpRequest();
	oReq.onload = showParameterSelection;
	oReq.onerror = parameterError;
	oReq.ontimeout = parameterError;
	oReq.open("Get", REPORTURL  + uuid + "/params", true);
	oReq.timeout = 15000;
	oReq.send();

	
	document.querySelector("#reportformat").selectedIndex = 0;
	displayDialogLocation('urlOptionsDialog', pos.x, window.pageYOffset + 20);

}

// error handler for getting parameters
function parameterError() {
	var parent = document.getElementById("customParameters");
	parent.innerHTML = "<font style='color:red'>" + i18n("report.paramerror") + "</font>";	
}

//callback from getting parameters
//this function adds GUI items to match each required parameter
function showParameterSelection(){
	var parent = document.getElementById("customParameters");
	document.getElementById("parameters_fieldset").style.display = "none";
	parent.innerHTML = "";
	var json = JSON.parse(this.responseText);
	parameterNames.length = 0; //clear the array so we don't parameters from the last report that was run.
	
	var otherparams = document.createElement("fieldset");
	otherparams.innerHTML = "<legend>" + "Other Parameters" + "</legend>";
	var otherdiv = document.createElement("div");
	otherparams.appendChild(otherdiv);
	otherdiv.className = "table";
	otherdiv.style.width = "100%";
	
	var hasother = false;
 	for (var i = 0; i < json.length; i++){
 		if(json[i].type == "GROUP"){
 			if (json[i].name=="Report Dates"){
 				//start and end dates already made; use default gui but still add to parameter list
 				document.getElementById("parameters_fieldset").style.display = "block";
 				for (var x = 0; x < json[i].children.length; x++){
 					parameterNames.push(json[i].children[x].name);
 				}
 			}else{ 	
 				var f = document.createElement("fieldset");
 				if(json[i].displayText != null){
 					f.innerHTML = "<legend>" + json[i].displayText +  "</legend>";
 				}else{
 					f.innerHTML = "<legend>" + json[i].name + "</legend>";
 				}
 				
 				var div = document.createElement("div");
 				f.appendChild(div);
 				div.className = "table";
 				div.style.width = "100%";
 				for (var x = 0; x < json[i].children.length; x++){
 					parameterNames.push(json[i].children[x].name);
 					if(json[i].children[x].type == "BOOLEAN"){
 						addBooleanParamater(json[i].children[x], div);
 					}else if (json[i].options.length > 0){
 			 			addOptionParameter(json[i].children[x], div)
 					}else{
 						addTextboxParamater(json[i].children[x], div);
 					}
 				}
 				parent.insertBefore(f, parent.childNodes[4]);
 			}
 		}else{
 			hasother = true;
 			parameterNames.push(json[i].name);
 			
 			if(json[i].type == "BOOLEAN"){
 	 			addBooleanParamater(json[i], otherdiv);
 	 		}else if (json[i].options.length > 0){
 	 			addOptionParameter(json[i], otherdiv)
 	 		}else{
 	 			addTextboxParamater(json[i], otherdiv);
 	 		}
 		}
 		
 	}
 	if (hasother) parent.insertBefore(otherparams, parent.childNodes[4]);

 }

function isFoundInRow(row){
	if (search == null || search=="") return true;
	if(isIn(row.dataset.reportname)){
		return true;
	}
	return false;
}

function isIn(text){
	if(text == undefined)return false;
	
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
		updateFolderTable();
		updateReportTable();
	}, 600);
	

}
function getUrlOnly(){
	  window.prompt(i18n("report.copytoclipboard"), generateUrl(REPORTLINKURL));
}



function generateRelativeUrl(root){
	var uuid = document.getElementById('reportuuid').value;
	
	//add the UUID
	var url = root + uuid + "?format=" + encodeURIComponent(document.getElementById('reportformat').value);

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
			url = url + "&cafilter=" + encodeURIComponent(cafilter.substring(1));
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
			csString += encodeURIComponent(name) + "," + encodeURIComponent(dateStr) + ",";
		}else{
			csString += encodeURIComponent(name) + "," + encodeURIComponent(document.getElementById(name).value) + ",";
		}
	}
	url = url + "&parameterList=" + csString;
	return url;
}

function generateUrl(root){
	return resolve(generateRelativeUrl(root));
}




function getCaList(){
	var oReq = new XMLHttpRequest();
		oReq.onload = populateCaList;
		oReq.open("Get", CAURL + '/?includeSpatialBoundaries=false&permission=runreport', true);
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
	    opt.className="formtext";
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
	users.homeCaUuid
	
	if(users.homeCaUuid != null){
		//see if ca exists in list
		var kids = parent.children;
		var found = false;
		for (var i = 0; i < kids.length; i ++){
			if (kids[i].value == users.homeCaUuid){
				found = true;
				break;
			}
		}
		if (found) parent.value = users.homeCaUuid;
	}
	
	loadReports();
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


function addBooleanParamater(param, parent){

	var div = document.createElement("div");
	div.className = "reportparameterrow";
	
	var lbl = document.createElement("label");
	lbl.setAttribute("for",param.name);
	lbl.innerHTML = param.name;
	
	div.appendChild(lbl);
	
	var newList = document.createElement("select");
	newList.setAttribute("id", param.name);
	newList.dataset.isRequired = param.isRequired;
	var optionT = new Option("true", "true");
	var optionF = new Option("false", "false");
	//Here we append that text node to our drop down list.
	newList.appendChild(optionT);
	newList.appendChild(optionF);
	div.appendChild(newList);
	
	parent.appendChild(div);
	
}


function addTextboxParamater(param, parent){
	
	var div = document.createElement("div");
	div.className = "reportparameterrow";
	
	var lbl = document.createElement("label");
	lbl.setAttribute("for",param.name);
	lbl.innerHTML = param.name;
	
	div.appendChild(lbl);
	
	var newInput = document.createElement("input");
	newInput.setAttribute("id", param.name);
	newInput.dataset.isRequired = param.isRequired;
	newInput.type = "text";
	newInput.className = "formtext";

	div.appendChild(newInput);
	
	parent.appendChild(div);
}

function addOptionParameter(param, parent){
	
	var div = document.createElement("div");
	div.className = "reportparameterrow";
	
	var lbl = document.createElement("label");
	lbl.setAttribute("for",param.name);
	lbl.innerHTML = param.name;
	
	div.appendChild(lbl);
	
	var newInput = document.createElement("select");
	newInput.setAttribute("id", param.name);
	newInput.className = "formtext";
	newInput.dataset.isRequired = param.isRequired;
	
 	for (var i = 0; i < param.options.length; i++){
 		var op = document.createElement("option");
 		op.value=param.options[i][1];
 		op.text = param.options[i][0];
 		newInput.appendChild(op);
 	}

	div.appendChild(newInput);
	parent.appendChild(div);
}

function initializeUrlDialog(){
	displayURLDialog(generateUrl(REPORTURL));
}

function createSharedLink(){
	var url = generateRelativeUrl(RELATIVEREPORTLINKURL);
	var expiresAfter = document.getElementById("expiresAfter").value;
	var jsonData = {"url": url,
					"expiresAfter": expiresAfter}
	
	var oReq = new XMLHttpRequest();
	oReq.onload = linkCreated;
	oReq.open("POST", SHARED_LINK_URL, true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

function linkCreated(){
	var link = JSON.parse(this.responseText);
	var status = this.status;
	if(status != 200){
		if(status == 401){
			document.getElementById("createdlink").value = i18n("report.linktimeout") + link.error;
		}else{
			document.getElementById("createdlink").value = i18n("report.linkerror") + link.error;
		}
	}else{
		document.getElementById("createdlink").value = resolve(SHAREDLINKSERVLETURL) + "?uuid=" + link.uuid;
	}
	document.getElementById("createlinkbutton").style.display = "none";
	document.getElementById("createdlink").style = "display: block;";
	document.getElementById("createdlink").select();
	document.getElementById("quickMinSelect").disabled=true;
	document.getElementById("expiresAfter").disabled=true;
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



function loadReports() {
	var oReq = new XMLHttpRequest();
 	oReq.onload = handleReports;
 	oReq.open("Get", REPORT_URL + "tree/", true);
 	oReq.send();
}



function handleReports() {
	const data = JSON.parse(this.responseText);
	var parent = document.getElementById('foldertable');
	Array.from(parent.children).forEach(function(c){parent.removeChild(c)});
	
	reports = [];
	var objects = document.querySelectorAll("div.reportrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	folderId = 0;
	for(var i=0; i < data.length; i++) {
		folderId++;
		addFolder(data[i], parent, "", 0);
	}
	
	//add an all reports item
	var folderDiv = document.createElement('div');
	folderDiv.classList.add('folder');
	folderDiv.dataset.selected=true;
	parent.appendChild(folderDiv);
	folderDiv.innerHTML = "<div class=\"folder-name folder-selected\" data-folderid=\"-1\" style=\"white-space:nowrap; padding-top:5px; padding-bottom:5px;\"><i style=\"padding-right: 5px\" class=\"folder-icon fa-regular fa-folder-open fa-xl\" ></i><span class=\"folder-label\">" + i18n("report.allreports") + "</span></div>";
	var folderContentsDiv = document.createElement('div');
	folderContentsDiv.classList.add('folder-contents');
	folderDiv.appendChild(folderContentsDiv);
	
	//create query table
	createReportTable();
	
	updateFolderTable();
}

// recursively adds nested folders and reports
function addFolder(data, parent, path, level) {
	
	var folderDiv = document.createElement('div');
	folderDiv.classList.add('folder');
	folderDiv.dataset.cauuid = data['caUuid'];
	parent.appendChild(folderDiv);
	if (path != ""){
		path += " > "; 
	}
	path += data['name'];
	
	var icon = "fa-folder-open";
	var display = "";
	if (level > 0){
		//hide this folder
		icon = "fa-folder";
		display = "none";
	}
	
	var foldernamediv = document.createElement('div');
	foldernamediv.classList.add("folder-name");
	foldernamediv.dataset.path = path;
	foldernamediv.dataset.folderid = folderId;
	foldernamediv.style.paddingTop = "5px";
	foldernamediv.style.paddingBottom = "5px";
	
	folderDiv.appendChild(foldernamediv);
	
	foldernamediv.innerHTML = "<i style=\"padding-right: 5px\" class=\"folder-icon fa-regular " + icon + " fa-xl\" ></i><span class=\"folder-label\">" + data['name'] + "</span>";
	
	var folderContentsDiv = document.createElement('div');
	folderContentsDiv.classList.add('folder-contents');
	folderContentsDiv.style.display=display;
	folderDiv.appendChild(folderContentsDiv);
	
	var reportfolderid = folderId;
	
	for(var i = 0; i < data['subFolders'].length; i++) {
		folderId++;
		addFolder(data['subFolders'][i], folderContentsDiv, path, level+1);
	}
	
	for(var i = 0; i < data['items'].length; i++) {
		var report = data['items'][i];
		report.folderid = reportfolderid;
		reports.push(report);	
	}
	
}

function createReportTable(){
	var reportlist = document.getElementById('reporttable');
	
 	//clear current table
	Array.from(document.querySelectorAll("div.reportrow")).forEach(function(element){element.parentElement.removeChild(element)});
	
	
	var drawnRowCount = 1;
	reports.forEach(function(report){
		
		var row = tableCreateRow(reportlist, 
 				[report.conservationArea, "<span title='" + report.id + "'> " + report.name , null], 
 				"reportrow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.reportuuid = report.uuid;
 		row.dataset.reportname = report.name;
 		row.dataset.isccaa = report.isCcaa;
 		row.dataset.folderid = report.folderid;

 		var runicon = document.createElement("i");
		runicon.className="fa-solid fa-xl fa-circle-right icon-btn-default";
 		runicon.title= i18n("reports.runreport");
 		runicon.onclick = showReportOptions;
 		row.childNodes[2].appendChild(runicon);
				
		
		if (selectedFolder == -1 || report.folderid == selectedFolder){
			row.style.display="";
			drawnRowCount++;
		}else{
			row.style.display="none";
		}
		
	});
}

function updateFolderTable(){
	//filter folders on ca uuid	
	var foldertable = document.getElementById('foldertable');
	var selectedCa = document.getElementById('caselect').value;
	
	if (selectedCa == "allcas"){
		Array.from(foldertable.getElementsByClassName("folder")).forEach(function(element){element.style.display=""});
	}else{		
		var clearreport = false;
		var visible = null;
		Array.from(foldertable.getElementsByClassName("folder")).forEach(function(element){
			if (element.getAttribute("data-cauuid") == selectedCa){
				element.style.display="";
				if (visible == null) visible = element;
			}else{
				element.style.display="none";
				if (element.getAttribute("data-selected")=="true"){
					clearreport = true;
				}
			}
		});
		
		if (clearreport && visible != null){
			if (visible != null) selectFolder(visible.children[0]);
		}else if (clearreport && visible == null){
			selectedFolder = -2;
		}
	}
	
}


function updateReportTable(){
	
	var reporttable = document.getElementById('reporttable');
	var elementArray = null;
	if (selectedFolder == -1){
		elementArray = Array.from(reporttable.getElementsByClassName("reportrow"));
	}else if (selectedFolder == -2){
		//hide all
		var items = Array.from(reporttable.getElementsByClassName("reportrow"));
		items.forEach(function(element) {element.style.display="none"});
	}else{
		//hide all
		Array.from(reporttable.getElementsByClassName("reportrow")).forEach(function(element){element.style.display="none"});
		elementArray = Array.from(reporttable.querySelectorAll("[data-folderid='" + selectedFolder + "']"));
	}
	
	var rowcnt = 1;
	elementArray.forEach(function(element) {
		if (isFoundInRow(element)){
			element.style.display="";
			element.classList.remove("smart-table-rowon");
			element.classList.remove("smart-table-rowoff");
			element.classList.add(rowcnt % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff")
			rowcnt++;
		}else{
			element.style.display="none";
		}
	});
	
}
