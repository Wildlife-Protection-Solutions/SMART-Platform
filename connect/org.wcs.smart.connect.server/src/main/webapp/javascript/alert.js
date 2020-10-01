const QUERY_URL = "../api/query/"
var ALERT_URL = "../api/connectalert/";
//var ALERT_URL = "https://office.refractions.net:8443/server/api/echoapi/";
var FILTER_URL = "../api/connectalertfilterdefault/";
var interval = 7000; //# of milli-seconds between map refresh on the alert layer,
					//overridden by the defaults once they load
var MAX_ALERTS = 1000; //override value to be sent to the api. The server by default also has a 1000 alert limit, so this is repetitive, but easily to modify than the server API limit. 
var realtime;
var map;
var layerControl;
var redMarker;
var deletetimer;
var queryNames = {};
var queryLayers = {};
var queryTypeKeys = {};

const queryColors = ['#FF0000', '#0000FF', '#FF00FF', '#00FFFF', 
					'#FF9933', '#FF3399', '#33FF99', '#9933FF', '#3399FF',
					'#FF6600', '#FF0066', '#66FF00', '#00FF66', '#6600FF', '#0066FF'];
var nextQueryColor = 0;
var assignedQueryColors = {};

window.onload = function() {
	menuCheckOnload();
	
	//Hide header/footer and menu if mobile parameter is set:
	if(mobile == "true"){
		document.getElementById('mainheader').style.display = 'none';
		document.getElementById('verticalmenu').style.display = 'none';
		document.getElementById('footerid').style.display = 'none';
		
		document.body.style.width = '38em';
	}
	
//------------------------------------------------------------
//Setup map layers
    // The real-time layer that auto-refreshes to show alert
    
    //OSM Basemap Layer - the Hardcoded basemaps
    var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
	var osmAttrib='Map data © <a href="http://openstreetmap.org">OpenStreetMap</a> contributors';
	var osm = new L.TileLayer(osmUrl, {minZoom: 1, maxZoom: 18, attribution: osmAttrib});
	
	var baseMaps = {
			"Basemap Off": L.tileLayer(''),
			"OSM basemap": osm,
	};
	
	dataLayers = {
	};
	
	var activeLayers = [osm];

	// Load all saved, active layers
	for (i = 0; i < mapLayers.length; ++i) {
		if(mapLayers[i][0] == "WMS"){ //WMS type
			var token = mapLayers[i][1];
		    var wmslayers =  mapLayers[i][2];
		    
		    var wmsLayer = L.tileLayer.wms(token, {
		    	layers: wmslayers,
		    	tiled: true,
		    	format: 'image/png',
		    	transparent: true,
		    	maxZoom: 14,
		        minZoom: 0,
		        continuousWorld: true
		    });
		
		    dataLayers[mapLayers[i][3]] = wmsLayer;
			//add to layer list so it is active to start with
			if(mapLayers[i][4] == "true"){
				activeLayers.push(wmsLayer);
			}
		}   
	}
			
	//initialize the map
	map = new L.Map('map', {center: new L.LatLng(startingLat, startingLong), zoom: startingZoom, layers: activeLayers});

 
	getMapFilters();//get the map filter defaults and set them before we make the first call to get alerts/events
	//also it adds the realtime layer to map once the map filter defaults are setup.
	
	//add layer control to map
	layerControl = L.control.layers(baseMaps, dataLayers, {position: 'topleft'});
	layerControl.addTo(map);

	var legend = document.getElementsByClassName("legend");
	
//Map setup complete.
//--------------------------------------------------	

    //set the lat/long in the "new alert form", if we can get them from the device automatically
    if (navigator.geolocation) {
    	navigator.geolocation.getCurrentPosition(showLatLong, showError)
    }
      
	//setup date picker for alert filters
	new Pikaday({
		field: document.getElementById('datePickerFrom'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});

	new Pikaday({
		field: document.getElementById('datePickerTo'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});

	// setup date picker for query filters
	var queryDatePickerFrom = new Pikaday({
		field: document.getElementById('queryDatePickerFrom'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n,
        onSelect: queryDateRangeUpdate
	});

	var queryDatePickerTo = new Pikaday({
		field: document.getElementById('queryDatePickerTo'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n,
        onSelect: queryDateRangeUpdate
	});

    //new alert and update alert actions
	document.querySelector("#newalertform").onsubmit = createNewAlert;
	document.querySelector("#updatealertform").onsubmit = submitUpdatedAlert;
	document.querySelector("#cancel").onclick = function(){
		closeDialog('updateAlertDialog');
		var overlaydiv = document.querySelector(".overlay-widgetlevel2");
		overlaydiv.parentNode.removeChild(overlaydiv);
	};

	//setup onChange events for filter buttons
	var items = document.getElementsByClassName("updateChange");
	for (var i = 0; i < items.length; i++){
		items[i].addEventListener("change", refreshAlerts);
	}
	document.getElementById('filterDate').addEventListener("change", checkForCustomDates);
	document.getElementById('datePickerFrom').addEventListener("change", refreshAlerts);
	document.getElementById('datePickerTo').addEventListener("change", refreshAlerts);

	// setup query date range picker
	var queryDateSelect = document.getElementById("queryDate");
	populateQueryDates(queryDateSelect)
	var queryDateUpdateHandler = buildUpdateDateHandler(queryDateSelect, queryDatePickerFrom, queryDatePickerTo, refreshQueries);
	queryDateSelect.addEventListener("change", queryDateUpdateHandler);
	queryDateUpdateHandler();
	
	const controlItemIcons = document.querySelectorAll('.control-item-icon');
	controlItemIcons.forEach(el => el.addEventListener('click', function(e) {
		toggleDisplay(e.target.previousElementSibling);
		toggleDisplay(e.target.parentNode.nextElementSibling);
	}));
	
	// we are catching events at the query-list level
	// in case we add or remove folders/items dynamically
	const queryList = document.getElementById('query-list');
	queryList.addEventListener('click', function(evt) {
		var element = evt.target;
		// bubble up events to the parent folder or folder-item
		if(!element.classList.contains('folder-name')
				&& !element.classList.contains('folder-item')) {
			element = element.parentNode;
		}
		if(element.classList.contains('folder-name')) {
			// clicking on the folder toggles the folder icon
			// as well as toggling the display of the folder contents
			const folderIcon = element.getElementsByClassName('folder-icon')[0];
			if(folderIcon.classList.contains('fa-folder-open-o')) {
				folderIcon.classList.remove('fa-folder-open-o');
				folderIcon.classList.add('fa-folder-o');
			} else {
				folderIcon.classList.remove('fa-folder-o');
				folderIcon.classList.add('fa-folder-open-o')
			}
			toggleDisplay(element.nextElementSibling);
		} else if(element.classList.contains('folder-item')) {
			// clicking on the item toggles the query layer on the map 
			const checkbox = element.querySelectorAll("input[type='checkbox']")[0];
			if(evt.target != checkbox) {
				checkbox.checked = !checkbox.checked;
			}
			var colorBox = element.querySelectorAll("span")[0];
			if(checkbox.checked) {
				var color = addQueryLayer(checkbox.id);
				colorBox.style['background-color'] = color; 
				colorBox.style.display = "";
			} else {
				removeQueryLayer(checkbox.id);
				colorBox.style.display = "none";
			}
		}
	});
	
	document.getElementById("queryFilterText").addEventListener('input', filterQueryList);

 	//default custom dates so they are not blank to start
 	var today = getTodayAsString();
    document.getElementById("datePickerFrom").value = today;
    document.getElementById("datePickerTo").value = today;
 	
 	checkForCustomDates();
	
 	var controlMenu = document.getElementById("control-menu");
 	L.DomEvent.disableScrollPropagation(controlMenu);
 	L.DomEvent.disableClickPropagation(controlMenu);
 	
	refreshAlerts();

	loadQueries();

}

function toggleDisplay(element) {
    if(element.style.display == "none") {
      element.style.display = "";
    } else {
    	element.style.display = "none";
    }
}

function loadQueries() {
	var oReq = new XMLHttpRequest();
 	oReq.onload = handleQueries;
 	oReq.open("Get", QUERY_URL + "tree/?type=" + shpValues.join(','), true);
 	oReq.send();
}

function handleQueries() {
	const data = JSON.parse(this.responseText);
	const queryList = document.getElementById("query-list");
	for(var i=0; i < data.length; i++) {
		addFolder(data[i], queryList, "");
	}
}

// recursively adds nested folders and queries
function addFolder(data, parent, parentName) {
	var folderDiv = document.createElement('div');
	folderDiv.classList.add('folder');
	parent.appendChild(folderDiv);
	folderDiv.innerHTML = "<div class=\"folder-name\"><i class=\"folder-icon fa fa-folder-open-o fa-lg\"></i>" + data['name'] + "</div>";
	folderContentsDiv = document.createElement('div');
	folderContentsDiv.classList.add('folder-contents');
	folderDiv.appendChild(folderContentsDiv);
	var thisParentName = (parentName == "" ? "CA: " : parentName + "Folder: ") + data['name'] + "<br/>";
	for(var i = 0; i < data['subFolders'].length; i++) {
		addFolder(data['subFolders'][i], folderContentsDiv, thisParentName);
	}
	for(var i = 0; i < data['queries'].length; i++) {
		var query = data['queries'][i];
		folderItemDiv = document.createElement('div');
		folderItemDiv.classList.add('folder-item');
		queryTypeKeys[query.uuid] = query.typeKey;
		queryNames[query.uuid] = thisParentName + "Query: " + query.name + " [" + query.type + "]";
		folderItemDiv.innerHTML = "<input id=\"" + query.uuid + "\" type=\"checkbox\"/><span style=\"display: none;\" class=\"color-box\"></span><img class=\"query-icon\" src=\"../css/images/query_icons/" + query.iconName + "\" title=\"" + query.type + "\">" + query.name;
		folderContentsDiv.appendChild(folderItemDiv);
	}
}

function filterQueryList() {
	const filterStr = document.getElementById('queryFilterText').value.toUpperCase();
	var childFolders = document.getElementById('query-list').getElementsByClassName('folder');
	for(var i = 0; i < childFolders.length; i++) {
		var matched = filterQueryListRecursive(childFolders[i], filterStr);
	}
}

function filterQueryListRecursive(folder, filterStr) {
	var foundMatch = false;
	var childFolders = folder.getElementsByClassName('folder');
	for(var i = 0; i < childFolders.length; i++) {
		var matched = filterQueryListRecursive(childFolders[i], filterStr);
		foundMatch = foundMatch || matched;
	}
	var childQueries = folder.getElementsByClassName('folder-item');
	for(var i = 0; i < childQueries.length; i++) {
		if(childQueries[i].innerText.toUpperCase().indexOf(filterStr) > -1) {
			foundMatch = true;
			childQueries[i].style.display = '';
		} else {
			childQueries[i].style.display = 'none';
		}
	}
	if(foundMatch) {
		folder.style.display = '';
	} else {
		folder.style.display = 'none';
	}
	return foundMatch;
}

function getQueryColor(id) {
	var color = assignedQueryColors[id];
	if(!color) {
		color = queryColors[nextQueryColor];
		nextQueryColor = (nextQueryColor + 1) % queryColors.length;
		assignedQueryColors[id] = color;
	}
	return color;
}

function addQueryLayer(id) {
	var color = getQueryColor(id);
 	var oReq = new XMLHttpRequest();
 	oReq.responseType = 'json';
 	oReq.onload = function() {
 		data = this.response;
 		
 		var pointStyle = {
			radius: 5,
		    fillColor: color,
		    color: color,
		    weight: 1,
		    opacity: 1,
		    fillOpacity: 0.5
 		};
 		var lineStyle = {
 			    color: color
 	 	};
 		var layer = L.geoJson(data.features, {
 			style: lineStyle,
 			pointToLayer: function (feature, latlng) {
 		        return L.circleMarker(latlng, pointStyle);
 		    }
 		}).bindPopup(function (layer) {
 		    var p = layer.feature.properties;
 		    var c = '<div class="queryPopupTitle">' + queryNames[id] + '</div><div class="queryPopupTable"><table><tbody>';
 		    for(const prop in p) {
 		    	c += "<tr><td>" + prop + "</td><td>" + p[prop] + "</td></tr>";
 		    }
 		    c += "</tbody></table></div>";
 		    return c;
 		},  {maxWidth: 325});
 		if(queryLayers[id]) {
 			removeQueryLayer(id);
 		}
 		map.addLayer(layer);
 		queryLayers[id] = layer;
 	};
 	var dateFilter = "";
	if(document.getElementById('queryDatePickerFrom').value != ""){
		var startDate = new Date(document.getElementById('queryDatePickerFrom').value.substring(4));//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		var startDateString = startDate.getFullYear() + "-" + (startDate.getMonth() + 1) + "-" + startDate.getDate() + " 00:00:00";
		
		dateFilter += "&start_date=" + startDateString; 
	}
	if(document.getElementById('queryDatePickerTo').value != ""){
		var endDate = new Date(document.getElementById('queryDatePickerTo').value.substring(4)); //use end of the day, since it is the "to" date.
		var endDateString = endDate.getFullYear() + "-" + (endDate.getMonth()+1) + "-" + endDate.getDate() + " 23:59:59";
	
		dateFilter += "&end_date=" + endDateString; 
	}
 	oReq.open("Get", QUERY_URL + id + "?format=geojson&srid=4326&date_filter=" + qdatefilter[queryTypeKeys[id]][0] + dateFilter , true);
 	oReq.send();
 	return color;
}

function removeQueryLayer(id) {
	map.removeLayer(queryLayers[id]);
	queryLayers[id] = null;
}

function queryDateRangeUpdate() {
	// don't refresh yet if we are in the middle of updating dates to a new range
	if (isDateChanging) return;
	var queryDateSelect = document.getElementById("queryDate");
	for (var i = 0; i < definedDateKeys.length; i ++){
		if (definedDateKeys[i] == "custom"){
			queryDateSelect.selectedIndex = i;
			break;
		}
	}
	refreshQueries();
}

function refreshQueries() {
	// update query layers to use new date range
	for(id in queryLayers) {
		addQueryLayer(id);
	}
}

//creates a new alert
function createNewAlert() {
	var cauuid = document.querySelector("select[name=alert_ca]").value;
	var alerttypeuuid = document.querySelector("select[name=alert_type]").value;
	var long = document.querySelector("input[name=long]").value;
	var lat = document.querySelector("input[name=lat]").value;
	var desc = document.querySelector("textarea[name=alert_description]").value;
	var level = document.querySelector("select[name=level]").value;
	
	var error = "";
	if (long > 180 || long < -180 ) {
		error = i18n("alert.invalidlong");
	}else if (lat > 90 || lat < -90){
		error = i18n("alert.invalidlat");
	}

	if (error.length > 0){
		displayError(error);
		return false;
	}
	usergenid = Math.random() * 1000000000;
	usergenid = Math.round(usergenid);
	
	
	var jsonData = { "type": "FeatureCollection", "features": [
	{ "type": "Feature",
	  "geometry": 
	  	{ "type": "Point", "coordinates": [ long , lat] 
	    },
	  "properties": {
	    "deviceId": "0",
	    "id": "0",
	    "latitude": 0,
	    "longitude": 0,
	    "altitude": 0,
	    "accuracy": 0,
	    "caUuid": cauuid,
	    "level": level,
	    "description": desc,
	    "typeUuid": alerttypeuuid,
	    "sighting": {}
	  }
	}
	]};
	
	//make ajax call
	hideInfo();
	var oReq = new XMLHttpRequest();
	oReq.onload = alertCreated;
	oReq.open("POST", ALERT_URL  + encodeURIComponent(usergenid), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	
	buttonCancelCreateAlert()//closes the pop-up dialog and returns to the normal map view
	return false;
}

//callback for creating alert 
function alertCreated() {
	refreshAlerts();
	
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(i18n("alert.alertcreated"));
	} else {
		displayError(parseError(i18n("alert.errorcreatingalert"), this.responseText));
	}

}

function showLatLong(position){
	document.getElementById("lat").value = position.coords.latitude
	document.getElementById("long").value = position.coords.longitude
    	
}

function showError(error) {
   	document.getElementById("long").value = i18n("alert.unabletodetectlocation");
}



function remove_class(id, classname){
//	var regex = new RegExp("(?:^|\s)" + classname + "(?!\S)", "g");
	document.getElementById(id).className = document.getElementById(id).className.replace( classname, '' )
}


/* delete alert*/
function deleteAlert(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var date = this.parentElement.parentElement.getAttribute('data-alertdate');
	date = new Date(Date.parse(date));// converts to local time by parsing into millisecs then loading millisecs into a new Date object.
	var type = this.parentElement.parentElement.getAttribute('data-alerttype');

	var overlaydiv = document.createElement('div');
	overlaydiv.setAttribute("class", "overlay-widgetlevel2");
	document.body.appendChild(overlaydiv);

	displayConfirmDialog("Delete Alert",  i18n("alert.areyousuredeletealert") + type + " on " + date + "?"  , function(){
		hideInfo();
		
		var oReq = new XMLHttpRequest();
		oReq.onload = alertDeleted;
		oReq.open("DELETE", ALERT_URL  + encodeURIComponent(uuid), true);
		oReq.send();
		var overlaydiv = document.querySelector(".overlay-widgetlevel2");
		overlaydiv.parentNode.removeChild(overlaydiv);
		return false;	
	});
	return false;
}

//callback for delete alert  
function alertDeleted() {
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("alert.alertdeleted") + r.userGeneratedId);
		refreshAlerts();
	} else {
		displayError(parseError(i18n("alert.errordeletealert") + " : " + this.statusText));
	}
}

/* reload alert table */
function refreshAlerts(){
	//clear current table
	hideInfo();
	
	var objects = document.querySelectorAll("tr.alertrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById("alerttable");
	var row = document.createElement("tr");
	row.className="alertrow";
	row.innerHTML= i18n("alert.refreshing");
	parent.appendChild(row);
	
	var filteredUrl = getFilteredUrl(ALERT_URL);
	
	updateRealtimeLayer(filteredUrl);
	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createAlertTable;
 	oReq.open("Get", filteredUrl , true);
 	oReq.send();
}

/* callback that displays all alert info */
function createAlertTable(){
	var element = document.getElementById("alertTableMessage");
	if (this.status != 200 && this.status != 201 ) {
		var msg = i18n("alert.error");
		document.getElementById("map-info-box").innerHTML = i18n("alert.errortrying") ;
		if (this.status == 401) {
			msg += i18n("alert.unathorized");
		} else if (this.status == 404) {
			msg += i18n("alert.invalidurl");
		} else if (this.status == 406) {
			msg += i18n("alert.toomanyalerts");
		} else if (this.status == 500) {
			msg += i18n("alert.servererror");
		}
		
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		
		
		element.style.display = "block";
		element.innerHTML = msg;
		element.className = "msgsection";
		return;
	} else {
		element.style.display = "none";//hide any previous errors
	}
	//clear current table
	var objects = document.querySelectorAll("tr.alertrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("alerttable");
 	
	try {
		var geojson = JSON.parse(this.responseText);
	 	var alerts = geojson.features;
	 	if(typeof alerts === "undefined" || alerts.length == 0){
		 	var newRow = parent.insertRow(-1);
	 		newRow.style.display = "table-row";
	 		newRow.className = "alertrow errorsection";	 		
	 		var oCell = newRow.insertCell(0);
	 	    oCell.colSpan = 10;
	 	    oCell.innerHTML = i18n("alert.noalertsfound");
	 	    
	 	    
	 		var newRow2 = parent.insertRow(-1);
	 		newRow2.style.display = "table-row";
	 		newRow2.className = "table-row alertrow";
	 		var cell = document.createElement("td");
			cell.className="table-cell smart-table-cell";
			cell.colSpan = "10";
			
	 	    a = document.createElement("a");
			var closeDiv = document.createElement("div");
			closeDiv.classList.add("button");
			closeDiv.style.float = "right";
			closeDiv.innerHTML = i18n("alert.close");
			a.appendChild(closeDiv);
			a.href="javascript:buttonCloseManageAlerts()";
			cell.appendChild(a);
			newRow2.appendChild(cell);

		} else {
			var str = document.getElementById("numberofalerts").innerHTML;
			document.getElementById("numberofalerts").innerHTML = alerts.length/2 ;
		 	for (var i = 0; i < alerts.length; i ++){
		 		if(alerts[i].geometry.type == "LineString"){
		 			continue; //This is a track feature, ignore it for drawing the table of alerts.
		 		}
 
		 		date = formatAlertDateTime(alerts[i].properties.date);//new Date(Date.parse(alerts[i].properties.date));// converts to local time by parsing into millisecs then loading millisecs into a new Date object.

		 		var row = tableCreateRowTDs(parent,
		 				[alerts[i].properties.type, alerts[i].properties.caname, date , alerts[i].properties.desc, alerts[i].properties.level.toString(), alerts[i].properties.status, Math.round(alerts[i].properties.x * 100000)/100000 + " , " + Math.round(alerts[i].properties.y * 100000)/100000, null], 
		 				"alertrow " + ((i/2) % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
		 		row.id = "alertRow" + i;
		 		row.dataset.uuid = alerts[i].properties.uuid;
		 		row.dataset.alertdate = alerts[i].properties.date;
		 		row.dataset.alerttype = alerts[i].properties.type;
		 		row.dataset.alertid = alerts[i].properties.id;
	
		 		if(canupdate){
		 			var updateicon = document.createElement("a");
			 		updateicon.className="update-icon";
			 		updateicon.title="update alert";
			 		updateicon.onclick = updateAlert;
			 		updateicon.href="";
			 		row.childNodes[7].appendChild(updateicon);
		 		}
		 		if(candelete){
			 		var deleteicon = document.createElement("a");
			 		deleteicon.className="delete-icon";
			 		deleteicon.title="delete alert";
			 		deleteicon.onclick = deleteAlert;
			 		deleteicon.href="";
			 		row.childNodes[7].appendChild(deleteicon);
		 		}
		 	}
		 	
		 	var lastrow = document.createElement("tr");
			lastrow.className="table-row alertrow";
			
			cell = document.createElement("td");
			cell.className="table-cell smart-table-cell";
			cell.colSpan = "8";
			var a = document.createElement("a");
			var deleteAllDiv = document.createElement("div");
			deleteAllDiv.classList.add("button");
			deleteAllDiv.style.float = "left";
			deleteAllDiv.innerHTML = i18n("alert.deleteall");
			a.appendChild(deleteAllDiv);
			a.href="javascript:deleteFilteredAlerts()";
			cell.appendChild(a);
			lastrow.appendChild(cell);
			
			
			a = document.createElement("a");
			var closeDiv = document.createElement("div");
			closeDiv.classList.add("button");
			closeDiv.style.float = "right";
			closeDiv.innerHTML = i18n("alert.close");
			a.appendChild(closeDiv);
			a.href="javascript:buttonCloseManageAlerts()";
			cell.appendChild(a);
			lastrow.appendChild(cell);
			
			parent.appendChild(lastrow);
		}
	} catch(err) {
 		var newRow = parent.insertRow(-1);
 		newRow.style.backgroundColor = "#F00";
 		newRow.className = "alertrow";
 	    var oCell = newRow.insertCell(0);
 	    oCell.colSpan = 10;
 	    oCell.innerHTML = err;
 	    
 	    var newRow2 = parent.insertRow(-1);
		newRow2.style.display = "table-row";
		newRow2.className = "table-row alertrow";
		var cell = document.createElement("td");
		cell.className="table-cell smart-table-cell";
		cell.colSpan = "10";
		
	    a = document.createElement("a");
		var closeDiv = document.createElement("div");
		closeDiv.classList.add("button");
		closeDiv.style.float = "right";
		closeDiv.innerHTML = i18n("alert.close");
		a.appendChild(closeDiv);
		a.href="javascript:buttonCloseManageAlerts()";
		cell.appendChild(a);
		newRow2.appendChild(cell);
 	    
	}

}


/* update alert button clicked */
function updateAlert(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	document.getElementById("updatealertform").uuid.value = uuid;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = showCurrentAlert;
	oReq.open("GET", ALERT_URL  + encodeURIComponent(uuid), true);
	oReq.send();
	return false;	
}

//Create alert button was clicked. Show dialog
function buttonCreateAlert(){
	document.querySelector("#dialogerror").style.display = "none";
	displayDialog('createAlertDialog', 'main');
	
}

function buttonCancelCreateAlert(){
	closeDialog('createAlertDialog');
	document.getElementById("message").style.display = "none";
}

function buttonCloseManageAlerts(){
	closeDialog('manageAlertsDialog');
	document.getElementById("message").style.display = "none";
}

function buttonManageAlerts(){
	document.querySelector("#dialogerror").style.display = "none";
	displayDialog('manageAlertsDialog', 'main');
	
}

function buttonExportImage() {
	var downloadOptions = {
        container: map._container,
        exclude: ['.leaflet-control-container'],
        format: 'image/png',
        fileName: 'smart_map_export.png',
	};
    var promise = map.downloadExport(downloadOptions);
    var data = promise.then(function (result) {
    	return result;
    });
}

//update alert clicked, fill in update alert form with current details 
function showCurrentAlert() {
	if (this.status == 200 ) {
		var r = JSON.parse(this.response);
	} else {
		displayError(parseError(i18n("alert.errorgettingalert") + this.uuid));
	}
	
	document.querySelector("#dialogerror").style.display = "none";
	displayDialog('updateAlertDialog', 'alerttable');
	
	var overlaydiv = document.createElement('div');
	overlaydiv.setAttribute("class", "overlay-widgetlevel2");
	document.body.appendChild(overlaydiv);
	
	document.getElementById("updatealertform").user_id.value = r.userGeneratedId;
	
	document.getElementById("updatealertform").update_alert_ca.value = r.ca.uuid;
	document.getElementById("updatealertform").update_alert_type.value = r.typeUuid;
	document.getElementById("updatealertform").update_level.value = r.level;
	document.getElementById("updatealertform").update_status.value = r.status;
	document.getElementById("updatealertform").update_long.value = r.x;
	document.getElementById("updatealertform").update_lat.value = r.y;
	document.getElementById("updatealertform").update_track.value = r.track;
	document.getElementById("updatealertform").update_alert_description.value = r.description;
}

/*submit updated alert details on an existing alert*/
function submitUpdatedAlert(){
	var form = document.getElementById("updatealertform");
	var userId = form.user_id.value
	var error = "";
	
	if( isNaN(form.update_long.value) || form.update_long.value < -180 || form.update_long.value > 180 ){
		error += i18n("alert.invalidlong") + "<br>";
	}
	if( isNaN(form.update_lat.value) || form.update_lat.value < -90 || form.update_lat.value > 90 ){
		error += i18n("alert.invalidlat") +"<br>";
	}
	if (error.length > 0){
		document.querySelector("#dialogerror").innerHTML = error;
		document.querySelector("#dialogerror").style.display = "block";
		return false;
	}
	hideInfo();
	
	//generate the data to send
	data = {
		    "typeUuid": form.update_alert_type.value,
		    "level": form.update_level.value ,
		    "status": form.update_status.value,
		    "x": form.update_long.value,
		    "y": form.update_lat.value,
		    "track": form.update_track.value,
		    "description": form.update_alert_description.value
		    };
	var oReq = new XMLHttpRequest();
	oReq.onload = AlertUpdated;
	oReq.open("PUT", ALERT_URL + encodeURIComponent(userId), true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
	oReq.send(JSON.stringify(data));
	return false;	
}

function AlertUpdated(){
	if (this.status == 200 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("alert.alertwithuuid") + r.uuid + i18n("alert.updated"));
		refreshAlerts();
	} else {
		displayError(parseError(i18n("alert.errorupdating") + this.statusText + "; " + this.responseText));
	}
	var overlaydiv = document.querySelector(".overlay-widgetlevel2");
	overlaydiv.parentNode.removeChild(overlaydiv);
	closeDialog('updateAlertDialog');

}

function getFilteredUrl(base){
	var filteredUrl = base;

	var started = {value: false};//using an object so it can be modified in the function calls easily (objects are pass-by-reference)

	filteredUrl += getFilter("filterType", started, "typeUuidFilter");
	filteredUrl += getFilter("filterStatus", started, "statusFilter");
	filteredUrl += getFilter("filterImportance", started, "levelFilter");
	filteredUrl += getFilter("filterCa", started, "caUuidFilter");
	
	filteredUrl += "&textSearchFilter=" +  document.getElementById("filterText").value;

	var dateRange = getDateRange('filterDate', 'datePickerFrom', 'datePickerTo');
	if(dateRange && dateRange['start']) {
		filteredUrl += "&startDateFilter=" + dateRange['start'];
	}
	if(dateRange && dateRange['end']) {
		filteredUrl += "&endDateFilter=" + dateRange['end'];
	}

	filteredUrl += "&sortBy=" + document.getElementById('sortBy').value + "&sortAscending=" + document.getElementById('sortAscending').value;
	filteredUrl += "&maxAlertOverride=" + MAX_ALERTS;
	return filteredUrl;
}

function getDateRange(dateSelectId, fromDatePickerId, toDatePickerId) {
	var dateSelect = document.getElementById(dateSelectId).value;
	
	if(dateSelect == -1) { //custom dates
		var from = new Date(document.getElementById(fromDatePickerId).value.substring(4)).getTime();//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		var to = new Date(document.getElementById(toDatePickerId).value.substring(4)).getTime() + 86399999; //use end of the day, since it is the "to" date.

		if(isNaN(to) || isNaN(from) || from > to){
			displayError(i18n("alert.invalidcustomdates"));
		} else {
			return {
				'start': from,  
				'end': to
			};
		}
	} else if(dateSelect == -99) { //all-time
		return null;
	} else if(dateSelect > 0) { //number of trailing hours from now
		var now = new Date();
		now = (new Date(now.getTime() + 86400000)).getTime(); //add 24 hours to time frame
		
		var start = new Date();
		start = start.getTime() - dateSelect*60*60*1000;  
		return {
			'start': start,  
			'end': now
		};		
	}
}

function getFilter(classname, started, filterName){
	var url = "";
	var filterString = "";
	var options = document.getElementsByClassName(classname);
	for (var i = 0; i < options.length; i++){
		if(options[i].checked){
			filterString += options[i].value + ",";
		}
	}
	if(started.value == false){
		started.value = true;
		url += "?";
	}else{
		url += "&";
	}
	url += filterName + "=" + encodeURI(filterString);
	return url;
}

function updateRealtimeLayer(updatedUrl) {
	if(map.hasLayer(realtime)) {
		realtime.stop();
		map.removeLayer(realtime);
//		layerControl.removeLayer(realtime);//doesn't quite work nicely, when you have it unselected it doesn't work.	
	}
	
	var url = getFilteredUrl(ALERT_URL) + "&"; //the "&" stops any additional data from messing up the last filter parameter in the url.
	
	realtime = L.realtime({
    	url: url,
        crossOrigin: true,
        type: 'json'
    }, {
        interval: 9000000, //not needed anymore, probably should stop using the 'realtime' object at all since we have our own javascript interval timer now for refreshes 
//        filter: eventFilter,   //client side-filtering, not using this for now.
        pointToLayer: stylePoints,
        style: styleFunction
    });
   
    realtime.on('update', function(e) {
        var coordPart = function(v, dirs) {
                return dirs.charAt(v >= 0 ? 0 : 1) +
                    (Math.round(Math.abs(v) * 100) / 100).toString();
            },
	            popupContent = function(fId) {
            	
                var feature = e.features[fId];
                var c = feature.geometry.coordinates;

                d = formatAlertDateTime(feature.properties.date);
                
                if(feature.properties.type == undefined) {
                	return i18n("alert.trackselected");
            	}

                var text = i18n("alert.event");
                if(feature.properties.type == 'Unknown Type') {
                	text = text + "<font color='red'>"
                }
                text = text + feature.properties.type;
                if(feature.properties.type == 'Unknown Type') {
                	text = text + "</font>"
                }
                	
                text = text + 
                	"<br>" + i18n("alert.reportedtime") + d +
                	"<br>" + i18n("alert.location") +
                    coordPart(c[1], 'NS') + ', ' + coordPart(c[0], 'EW') +
                    "<br>" +i18n("alert.description") + feature.properties.desc + 
                    "<br>" + i18n("alert.importance") + feature.properties.level;
                return text;
            }
            ,
            bindFeaturePopup = function(fId) {
                realtime.getLayer(fId).bindPopup(popupContent(fId));
            };
            //,
//            updateFeaturePopup = function(fId) {
//                realtime.getLayer(fId).getPopup().setContent(popupContent(fId));
//            };
        Object.keys(e.enter).forEach(bindFeaturePopup);
        //Object.keys(e.update).forEach(updateFeaturePopup);
        var now = new Date();
        var seconds = now.getSeconds();
        var minutes = now.getMinutes();
        var month = now.getMonth() + 1;
        if(minutes <10) minutes = "0" + minutes;
        if(seconds<10) seconds = "0" + seconds;
        document.getElementById("map-info-box").innerHTML = i18n("alert.lastupdated") + now.getDate() + "/" + month + "/" + now.getFullYear() + " " + now.getHours() + ":" + minutes + ":" + seconds;
//        + "  <a href='javascript:refreshAlerts()'>" + i18n("alert.updatenow") + "</a>"
    });
    
    realtime.addTo(map);
}

function formatAlertDateTime(datestr){
	var d = new Date(datestr);
	var options = { month: 'short', day: 'numeric', year: 'numeric', hour:'numeric', minute:'numeric', second:'numeric' };
	return new Intl.DateTimeFormat( navigator.languages, options).format(d);
}

//styles for points
function stylePoints(feature, latlng) {
	
	var color = styleColors[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
	var opacity = styleOpacity[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
	var markerIcon = styleMarkerIcon[feature.properties.typeuuid]; 
	var markerColor = styleMarkerColor[feature.properties.typeuuid];
	var spinStr = styleSpin[feature.properties.typeuuid];
	var customIcon = styleCustomIcon[feature.properties.typeuuid];
	if(spinStr == "true") {
		spin = true;
	} else {
		spin = false;
	}	

	marker  = L.AwesomeMarkers.icon({
		prefix: 'fa',
	    icon: markerIcon,
	    iconColor: color,
	    markerColor: markerColor,
	    spin: spin,
	    html: "<b>" + customIcon + "</b>" //bold always looks better to me 
	});
	
    return L.marker(latlng, {icon: marker});
}

//styles for lines
function styleFunction(feature) {
	var color = styleColors[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
	var opacity = styleOpacity[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
    return {"fillOpacity": opacity, 
    	"color":color,
    	"weight": 3,
    	"opacity":opacity,
    	};
};

function getMapFilters(){
 	var oReq = new XMLHttpRequest();
 	oReq.onload = setMapFilters;
 	oReq.open("Get", FILTER_URL, true);
 	oReq.send();
}

function setMapFilters(){
	if (this.status != 200 && this.status != 201 ) {
		var msg = i18n("alert.errorlabel");
		if (this.status == 401) {
			msg += i18n("alert.unathorized");
		} else if (this.status == 404) {
			msg += i18n("alert.invalidurl");
		} else {
			msg = JSON.parse(this.responseText).error
		}
		
		displayError(msg);
		return false;
	}
	

	var geojson = JSON.parse(this.responseText);
 	var defaults = geojson[0];
 	
 	interval = defaults.secondsRefresh * 1000;
 	
 	//timer to refresh the alerts at the set interval
 	setInterval(function() {
 		refreshAlerts()
 	}, interval);
 	
 	var filter_form = document.getElementById('filter-form');
 	
 	document.getElementById('filterDate').value = defaults.defaultPastHours;
 	
 	var statuses = document.getElementsByClassName('filterStatus');
 	for(var x=0 ; x < statuses.length; x++){
 		if(statuses[x].name == "ACTIVE"){
 			statuses[x].checked = defaults.defaultActive;
 		}
 		if(statuses[x].name == "DISABLED"){
 			statuses[x].checked = defaults.defaultDisabled;
 		}
 	}
 	var levels = document.getElementsByClassName('filterImportance');
 	for(x=0 ; x < levels.length; x++){
 		switch (levels[x].name){
 		case 'level1':
 			levels[x].checked = defaults.defaultLevel1;
 			break;
 		case 'level2':
 			levels[x].checked = defaults.defaultLevel2;
 			break;
 		case 'level3':
 			levels[x].checked = defaults.defaultLevel3;
 			break;
 		case 'level4':
 			levels[x].checked = defaults.defaultLevel4;
 			break;
 		case 'level5':
 			levels[x].checked = defaults.defaultLevel5;
 			break;
 		}
 	}
 	document.getElementById('filterDate').value = defaults.defaultPastHours;
 	checkForCustomDates();
 	
 	var str = defaults.defaultTypeUuids;
 	var typeUuids = str.split(',');
 	for(x=0 ; x < typeUuids.length; x++){
 		if(filter_form[typeUuids[x]] != null){
 			filter_form[typeUuids[x]].checked = true;
 		}
 	}
 	
 	var str = defaults.defaultCaUuids;
 	var caUuids = str.split(',');
 	for(x=0 ; x < caUuids.length; x++){
 		if(filter_form[caUuids[x]] != null){
 			filter_form[caUuids[x]].checked = true;
 		}
 	}

 	document.getElementById('filterText').value = defaults.defaultText;
 	 	
 	refreshAlerts();
}

//check if the user selected or deselected "custom dates" and grey/de-grey the custom inputs. 
function checkForCustomDates() {
	var date = document.getElementById('filterDate');
	if(date.value == -1) {
		document.getElementById('datePickerFrom').disabled = false;
		document.getElementById('datePickerTo').disabled = false;
	} else {
		document.getElementById('datePickerFrom').disabled = true;
		document.getElementById('datePickerTo').disabled = true;
	}
	refreshAlerts();
}

function getTodayAsString(){
 	var today = new Date();
    var dd = today.getDate();
    var mm = today.getMonth() + 1; //January is 0!

    var yyyy = today.getFullYear();
    if(dd<10){
        dd = '0' + dd
    } 
    if(mm<10){
        mm = '0' + mm
    } 
    
    
    //These all need to be converted back to dates and the conversion doesn't seem to work on other languages, so dates are English only.
    var weekday = new Array(7);
    weekday[0] = "Sun";
    weekday[1] = "Mon";
    weekday[2] = "Tue";
    weekday[3] = "Wed";
    weekday[4] = "Thu";
    weekday[5] = "Fri";
    weekday[6] = "Sat";
    
    var monthText= new Array(12);
    monthText[0] = "Jan";
    monthText[1] = "Feb";
    monthText[2] = "Mar";
    monthText[3] = "Apr";
    monthText[4] = "May";
    monthText[5] = "Jun";
    monthText[6] = "Jul";
    monthText[7] = "Aug";
    monthText[8] = "Sep";
    monthText[9] = "Oct";
    monthText[10] = "Nov";
    monthText[11] = "Dec";

    var n = weekday[today.getDay()];
    var month = monthText[today.getMonth()];

    return n + ' ' + month + ' ' + dd + ' ' + yyyy;
}

function sort(str) {
	var asc = document.getElementById('sortAscending').value;
	var cur = document.getElementById('sortBy').value; 
	if(cur == str){
		if(asc == "true") {
			document.getElementById('sortAscending').value = "false";
		} else {
			document.getElementById('sortAscending').value = "true";
		}
	} else {
		document.getElementById('sortBy').value = str;
	}
	
	refreshAlerts();
}

//Delete all alerts that meet the current filter criteria
function deleteFilteredAlerts() {
	displayConfirmDialog(i18n("alert.deleteallheading"), i18n("alert.areyousuredeleteallalerts"), function() {
		var alerts = document.querySelectorAll(".alertrow");
		for (var i = 0; i < alerts.length-1; i++){ //-1 because the delete all link is in the list as well. 
			var uuid = alerts[i].dataset.uuid;
			var oReq = new XMLHttpRequest();
			oReq.onload = allAlertDeleted;
			oReq.open("DELETE", ALERT_URL  + encodeURIComponent(uuid), true);
			oReq.send();
		}
	});
}
function allAlertDeleted() {	
	if (this.status == 200  && this.status != 201 ) {
	} else {
		displayError(parseError(i18n("alert.errordeletealert") + " : " + this.statusText));
	}
	clearTimeout(deletetimer);  //reset the delay if another delete is completed before 2 sec is up.
	deletetimer = setTimeout(refreshAlerts,2000); //don't want to run this a million times, just run it after waiting for 2sec 
	return false;
}