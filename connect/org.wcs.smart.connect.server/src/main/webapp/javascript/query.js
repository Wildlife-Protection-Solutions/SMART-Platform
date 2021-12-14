var SHARED_LINK_URL = "../api/sharedlink/";
var USER_URL = "../api/connectuser/getCurrent";
const QUERY_URL = "../api/query/"

var queries;
var selectedFolder = -1;
var lastSorted = "name";
var to; //timeout to slow auto-search a bit. It is cleared each time another character/change is typed so we don't fire too many updates too fast.

var startDatePicker, endDatePicker;

var isDateChanging = false;

var folderId;


/* configure events on html elements */
window.onload = function(){
	menuCheckOnload();

	document.getElementById('textsearch').value = search;
	getCaList();

	
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
	var dateSelect = document.getElementById("defineddates");
	populateQueryDates(dateSelect);
	
	//TODO: I commented this out; not sure why it's here
	var dateUpdateHandler = buildUpdateDateHandler(dateSelect, startDatePicker, endDatePicker, null);
	dateSelect.addEventListener("change", dateUpdateHandler);
	dateUpdateHandler();
	
	
	// we are catching events at the query-list level
	// in case we add or remove folders/items dynamically
	const queryList = document.getElementById('foldertable');
	queryList.addEventListener('click', function(evt) {
		var element = evt.target;
		
		var openonly = false;
		if (!element.classList.contains('folder-icon')){
			//select folder
			if(!element.classList.contains('folder-name')) {
				element = element.parentNode;
			}
			if(!element.classList.contains('folder-name')) return;

			selectFolder(element);
			updateQueryTable();
			
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
				if(!folderIcon.classList.contains('fa-folder-open-o')) {
					folderIcon.classList.remove('fa-folder-o');
					folderIcon.classList.add('fa-folder-open-o')
					toggleDisplay(element.nextElementSibling);
				}
			}else{
			
				if(folderIcon.classList.contains('fa-folder-open-o')) {
					folderIcon.classList.remove('fa-folder-open-o');
					folderIcon.classList.add('fa-folder-o');
				} else {
					folderIcon.classList.remove('fa-folder-o');
					folderIcon.classList.add('fa-folder-open-o')
				}
				toggleDisplay(element.nextElementSibling);

			}
		} 
	});
	
}

//element is the folder-name dive
function selectFolder(element){
	const queryList = document.getElementById('foldertable');

	selectedFolder = element.getAttribute("data-folderid");

	element.classList.add('folder-selected');
	var path = element.getAttribute("data-path");
	if (path != null){
		document.getElementById('querypath').innerHTML = path;
	}else{
		document.getElementById('querypath').innerHTML = i18n("query.allqueries");
	}
	
	var list = Array.from(queryList.getElementsByClassName("folder-selected"))
	
	list.forEach(function(ele){
		ele.classList.remove("folder-selected");
		ele.parentElement.dataset.selected = "false";
	});

	selectedFolder = element.getAttribute("data-folderid");
	var path = element.getAttribute("data-path");
	
	updateQueryTable();
	element.parentElement.dataset.selected="true";
	element.classList.add('folder-selected');
	if (path != null){
		document.getElementById('querypath').innerHTML = path;
	}else{
		document.getElementById('querypath').innerHTML = i18n("query.allqueries");
	}
	document.getElementById('querytable').scrollTop = 0;
}

function toggleDisplay(element) {
    if(element.style.display == "none") {
      element.style.display = "";
    } else {
    	element.style.display = "none";
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


function showQueryOptions(){
	var uuid = this.parentElement.parentElement.getAttribute('data-queryuuid');
	var name = this.parentElement.parentElement.getAttribute('data-queryname');
	var isccaa = this.parentElement.parentElement.getAttribute('data-isccaa');
	var querytype = this.parentElement.parentElement.getAttribute('data-querytype');
	
	
	document.getElementById("runqueryform").uuid.value = uuid;
	document.getElementById("runqueryform").name.value = name;
	document.getElementById("runqueryform").querySelector("#queryname").innerHTML = name;
	if (isccaa === "true"){
		document.getElementById("cafilter").style.display="block";
	}else{
		document.getElementById("cafilter").style.display="none";
	}
	
	var poselement = document.querySelector("#foldertable");
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
	var itemHtml = document.querySelector("#queryformat option[value=html]");//turn off HTML option for grid queries which are the same ones tif is available for.
	if (item != null){
		if (isTif){
			item.style.display = "block";
			itemHtml.style.display = "none";
		}else{
			item.style.display = "none";
			itemHtml.style.display = "block";
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
	var datefieldset = document.getElementById("datefieldset");
	if (ops != null){
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
		datefieldset.style.display = "block";
	}else{
		datefieldset.style.display = "none";
	}

	
	
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
	if (search == null || search=="") return true;
	if(isIn(row.dataset.queryname)){
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
		
		updateFolderTable();
		updateQueryTable();
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
		oReq.open("Get", CAURL + '?includeSpatialBoundaries=false&permission=runquery', true);
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
	loadQueries();
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


function loadQueries() {
	var oReq = new XMLHttpRequest();
 	oReq.onload = handleQueries;
 	oReq.open("Get", QUERY_URL + "tree/", true);
 	oReq.send();
}



function handleQueries() {

	const data = JSON.parse(this.responseText);
	var parent = document.getElementById('foldertable');
	
	var items = Array.from(parent.children);
	items.forEach(function(c){ parent.removeChild(c)});
	
	queries = [];
	var objects = document.querySelectorAll("div.queryrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	folderId = 0;
	for(var i=0; i < data.length; i++) {
		folderId++;
		addFolder(data[i], parent, "", 0);
	}
	
	//add an all queries item
	var folderDiv = document.createElement('div');
	folderDiv.classList.add('folder');
	folderDiv.dataset.selected=true;
	parent.appendChild(folderDiv);
	folderDiv.innerHTML = "<div class=\"folder-name folder-selected\" data-folderid=\"-1\" style=\"white-space:nowrap; padding-top:5px; padding-bottom:5px;\"><i style=\"padding-right: 5px\" class=\"folder-icon fa fa-folder-open-o fa-lg\" ></i><span class=\"folder-label\">" + i18n("query.allqueries")  + "</span></div>";
	var folderContentsDiv = document.createElement('div');
	folderContentsDiv.classList.add('folder-contents');
	folderDiv.appendChild(folderContentsDiv);
	
	//sort
	if (lastSorted != ""){
		queries.sort(dynamicSort(lastSorted));
	}
	
	//create query table
	createQueryTable();
	
	updateFolderTable();
}

// recursively adds nested folders and queries
function addFolder(data, parent, path, level) {
	
	var folderDiv = document.createElement('div');
	folderDiv.classList.add('folder');
	folderDiv.dataset.cauuid = data['caUuid'];
	parent.appendChild(folderDiv);
	if (path != ""){
		path += " > "; 
	}
	path += data['name'];

	var icon = "fa-folder-open-o";
	var display = "";
	if (level > 0){
		//hide this folder
		icon = "fa-folder-o";
		display = "none";
	}
	
	var foldernamediv = document.createElement('div');
	foldernamediv.classList.add("folder-name");
	foldernamediv.dataset.path = path;
	foldernamediv.dataset.folderid = folderId;
	foldernamediv.style.paddingTop = "5px";
	foldernamediv.style.paddingBottom = "5px";
	
	folderDiv.appendChild(foldernamediv);
	
	foldernamediv.innerHTML = "<i style=\"padding-right: 5px\" class=\"folder-icon fa " + icon + " fa-lg\" ></i><span class=\"folder-label\">" + data['name'] + "</span>";
	
	var folderContentsDiv = document.createElement('div');
	folderContentsDiv.classList.add('folder-contents');
	folderContentsDiv.style.display=display;
	folderDiv.appendChild(folderContentsDiv);
	
	
	var queryfolderid = folderId;
	
	for(var i = 0; i < data['subFolders'].length; i++) {
		folderId++;
		addFolder(data['subFolders'][i], folderContentsDiv, path, level + 1);
	}
	
	for(var i = 0; i < data['items'].length; i++) {
		var query = data['items'][i];
		query.folderid = queryfolderid;
		queries.push(query);	
	}
	
}

function createQueryTable(){
	var querylist = document.getElementById('querytable');
	var hide = document.getElementById('qhideexe').checked;

 	//clear current table
	var items = Array.from(document.querySelectorAll("div.queryrow"));
	items.forEach(function(element) {element.parentElement.removeChild(element)});
	
	
	var drawnRowCount = 1;
	queries.forEach(function(query){
		var row = tableCreateRow(querylist, 
				[query.conservationArea, "<img src='../css/images/query_icons/" + query.iconName +"' title='" + query.type + "'>" ,  "<span title='" + query.id + " - " + query.name + "'> " + query.name , null], 
				"queryrow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));

		var canexe = canExecute(query.typeKey);
		
		row.dataset.queryuuid = query.uuid;
		row.dataset.queryname = query.name;
		row.dataset.querytype = query.typeKey;
		row.dataset.folderid = query.folderid;
		row.dataset.isccaa = query.isCcaa;
		row.dataset.canexe = canexe;
		
		
		if( hide && !canexe){
			row.style.display="none";
		}else if (selectedFolder == -1 || query.folderid == selectedFolder){
			row.style.display="";
			drawnRowCount++;
		}else{
			row.style.display="none";
		}
		if (canExecute(query.typeKey)){
			var runicon = document.createElement("a");
			runicon.className="run-icon";
			runicon.title= i18n("query.runquery");
			runicon.onclick = showQueryOptions;
			row.childNodes[3].appendChild(runicon);
		}
		
	});
}

function updateFolderTable(){
	//filter folders on ca uuid	
	var foldertable = document.getElementById('foldertable');

	var selectedCa = document.getElementById('caselect').value;
	
	if (selectedCa == "allcas"){
		var items = Array.from(foldertable.getElementsByClassName("folder"));
		items.forEach(function(element){ element.style.display="" });
	}else{
		
		var clearquery = false;
		var visible = null;
		Array.from(foldertable.getElementsByClassName("folder")).forEach(function(element) {
			if (element.getAttribute("data-cauuid") == selectedCa){
				element.style.display="";
				if (visible == null) visible = element;
			}else{
				element.style.display="none";
				if (element.getAttribute("data-selected")=="true"){
					clearquery = true;
				}
			}
		});
		
		if (clearquery && visible != null){
			selectFolder(visible.children[0]);
		}else if (clearquery && visible == null){
			selectedFolder = -2;
		}
		
	}
	
}


function updateQueryTable(){
	
	var hide = document.getElementById('qhideexe').checked;
	var querytable = document.getElementById('querytable');
	
	var elementArray = null;
	
	if (selectedFolder == -1){
		elementArray = Array.from(querytable.getElementsByClassName("queryrow"))
	}else if (selectedFolder == -2){
		//hide all
		var items = Array.from(querytable.getElementsByClassName("queryrow"));
		items.forEach(function(element) {element.style.display="none"});
	}else{
		//hide all
		var items = Array.from(querytable.getElementsByClassName("queryrow"));
		items.forEach(function(element) {element.style.display="none"});
		//show folder
		elementArray = Array.from(querytable.querySelectorAll("[data-folderid='" + selectedFolder + "']"));
	}
	
	if(elementArray != null){
		var rowcnt = 1;
		elementArray.forEach(function(element) {
			if (!hide || element.getAttribute("data-canexe") == "true"){
				if (isFoundInRow(element)){
					element.style.display="";
					element.classList.remove("smart-table-rowon");
					element.classList.remove("smart-table-rowoff");
					element.classList.add(rowcnt % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff")
					rowcnt++;
				}else{
					element.style.display="none";
				}
			}else{
				element.style.display="none";
			}
		});
	}
		
}
