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



window.onload = function(){
	
	//Hide header/footer and menu if mobile parameter is set:
	if(mobile == "true"){
		document.getElementById('mainheader').style.display = 'none';
		document.getElementById('verticalmenu').style.display = 'none';
		document.getElementById('footerid').style.display = 'none';
		
		document.body.style.width = '38em';
	}

	
	//setup onChange events for filter buttons
	var items = document.getElementsByClassName("updateChange");
	for (var i = 0; i < items.length; i++){
		items[i].addEventListener("change", refreshAlerts);
	}
	document.getElementById('filterDate').addEventListener("change", checkForCustomDates);
	document.getElementById('datePickerFrom').addEventListener("change", refreshAlerts);
	document.getElementById('datePickerTo').addEventListener("change", refreshAlerts);

	
	
	//setup date picker for alert filters

	var picker = new Pikaday({
		field: document.getElementById('datePickerFrom'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});

	var picker = new Pikaday({
		field: document.getElementById('datePickerTo'),
		firstDay: 1,
        minDate: new Date('2000-01-01'),
        yearRange: [2000,2050],
        i18n: pickaday_i18n
	});


	
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

	//Load all saved, active layers
	for (i = 0; i < mapLayers.length; ++i) {
		//Mapbox layer type
		if(mapLayers[i][0] == 1){
			var accesstoken = mapLayers[i][1];
			var mapboxId = mapLayers[i][2];

			L.mapbox.accessToken = accesstoken;
			var layer = L.mapbox.tileLayer(mapboxId)
			var flayer = L.mapbox.featureLayer(mapboxId)

			//add the new layer to the list of datalayers
			baseMaps[mapLayers[i][4]] = layer;
			dataLayers[mapLayers[i][4]] = flayer;
			
			
			//add to layer list so it is active to start with
			if(mapLayers[i][5] == "true"){
				activeLayers.push(flayer);
			}

		}else if(mapLayers[i][0] == 2){	//GIScloud layer type

			var token = mapLayers[i][1];
		    var layerName =  mapLayers[i][3];
			
			var giscloud= L.tileLayer.wms("https://editor.giscloud.com/wms/" + token, {
				layers: layerName ,
			    format: 'image/png',
			    transparent: true,
			    attribution: "giscloud.com"
			});
			dataLayers[mapLayers[i][4]] = giscloud;
			//add to layer list so it is active to start with
			if(mapLayers[i][5] == "true"){
				activeLayers.push(giscloud);
			}

		}else if(mapLayers[i][0] == 3){ //WMS type
			var token = mapLayers[i][1];
		    var layerName =  mapLayers[i][3];
		    
		    var wmsLayer = L.tileLayer.wms(token, {
		    	layers: layerName,
		    	tiled: true,
		    	format: 'image/png',
		    	transparent: true,
		    	maxZoom: 14,
		        minZoom: 0,
		        continuousWorld: true
		    });
		
		    dataLayers[mapLayers[i][4]] = wmsLayer;
			//add to layer list so it is active to start with
			if(mapLayers[i][5] == "true"){
				activeLayers.push(wmsLayer);
			}
		}

	    
	}

			
	//initialize the map
	map = new L.Map('map', {center: new L.LatLng(startingLat, startingLong), zoom: startingZoom, layers: activeLayers});

//	redMarker  = L.AwesomeMarkers.icon({
//	    icon: 'car',
//	    markerColor: 'red'
//	  });

 
	getMapFilters();//get the map filter defaults and set them before we make the first call to get alerts/events
	//also it adds the realtime layer to map once the map filter defaults are setup.
	
	//add layer control to map
	layerControl = L.control.layers(baseMaps, dataLayers, {position: 'topleft'});
	layerControl.addTo(map);
	
//Map setup complete.
//--------------------------------------------------	
	
    
    //initialize the tab styles
    settab(tab);
    
    //set the lat/long in the "new alert form", if we can get them from the device automatically
    if (navigator.geolocation) {
    	navigator.geolocation.getCurrentPosition(showLatLong, showError)
    }
      
    //new alert and update alert actions
	document.querySelector("#newalertform").onsubmit = createNewAlert;
	document.querySelector("#updatealertform").onsubmit = submitUpdatedAlert;
	document.querySelector("#cancel").onclick = function(){
		closeDialog('updateAlertDialog');
	};

	refreshAlerts();



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
	return false;
}

//callback for creating user 
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

function settab(tab){
	remove_all_tab_classes();
	switch(tab){
		case 2:
			document.getElementById("map-info-box").style.display = "none";
			document.getElementById("filter-controls").style.display = "none";
			
			document.getElementById("tab2").style.zIndex = 2;
			document.getElementById('tab2').className += "selectedTab";
			document.getElementById('tab2text').className += "selectedTab";

			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab3').className += "unselectedTab";
			document.getElementById('tab3text').className += "unselectedTab";
			break;
		case 3:
			document.getElementById("map-info-box").style.display = "block";
			document.getElementById("filter-controls").style.display = "block";
			
			document.getElementById("tab3").style.zIndex = 2;
			document.getElementById("tab3").className += "selectedTab";
			document.getElementById("tab3text").className += "selectedTab";
			
			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab2').className += "unselectedTab";
			document.getElementById('tab2text').className += "unselectedTab";
			break;
		default:
			document.getElementById("map-info-box").style.display = "block";
			document.getElementById("filter-controls").style.display = "block";
			
			document.getElementById("tab1").style.zIndex = 2;
			document.getElementById('tab1').className += "selectedTab";
			document.getElementById('tab1text').className += "selectedTab";
			
			document.getElementById('tab2').className += "unselectedTab";
			document.getElementById('tab2text').className += "unselectedTab";
			document.getElementById('tab3').className += "unselectedTab";
			document.getElementById('tab3text').className += "unselectedTab";
	}
}

function remove_all_tab_classes(){
	remove_class("tab1", "unselectedTab");
	remove_class("tab2", "unselectedTab");
	remove_class("tab3", "unselectedTab");
	remove_class("tab1text", "unselectedTab");
	remove_class("tab2text", "unselectedTab");
	remove_class("tab3text", "unselectedTab");
	remove_class("tab1", "selectedTab");	
	remove_class("tab2", "selectedTab");
	remove_class("tab3", "selectedTab");
	remove_class("tab1text", "selectedTab");
	remove_class("tab2text", "selectedTab");
	remove_class("tab3text", "selectedTab");

	
	document.getElementById("tab1").style.zIndex = 0;
	document.getElementById("tab2").style.zIndex = 0
	document.getElementById("tab3").style.zIndex = 0

}

function remove_class(id, classname){
//	var regex = new RegExp("(?:^|\s)" + classname + "(?!\S)", "g");
	document.getElementById(id).className = document.getElementById(id).className.replace( classname, '' )
}


/* delete alert*/
function deleteAlert(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var id = this.parentElement.parentElement.getAttribute('data-alertid');
	
	displayConfirmDialog("Delete Alert", i18n("alert.areyousuredeletealert") + id + "?"  , function(){
		hideInfo();
		
		var oReq = new XMLHttpRequest();
		oReq.onload = alertDeleted;
		oReq.open("DELETE", ALERT_URL  + encodeURIComponent(uuid), true);
		oReq.send();
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
	
	if (this.status != 200 && this.status != 201 ) {
		var msg = "Error: ";
		if (this.status == 401){
			msg += i18n("alert.unathorized");
			document.getElementById("map-info-box").innerHTML = "Error trying to update. <a href='javascript:refreshAlerts()'>update now</a>";
		}else if (this.status == 404){
			msg += i18n("alert.invalidurl");
			document.getElementById("map-info-box").innerHTML = "Error trying to update. <a href='javascript:refreshAlerts()'>update now</a>";
		}else if (this.status == 406){
			msg += i18n("alert.toomanyalerts");
			document.getElementById("map-info-box").innerHTML = "Error trying to update. <a href='javascript:refreshAlerts()'>update now</a>";
		}else if (this.status == 500){
			msg += i18n("alert.servererror");
			document.getElementById("map-info-box").innerHTML = "Error trying to update. <a href='javascript:refreshAlerts()'>update now</a>";
		}
		
		
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("tr.alertrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("alerttable");
 	
	try{
		var geojson = JSON.parse(this.responseText);
	 	var alerts = geojson.features;
	 	if(typeof alerts === "undefined" || alerts.length == 0){
		 	var newRow = parent.insertRow(-1);
	 		newRow.style.display = "table-row";
	 		newRow.className = "alertrow errorsection";
	 	    var oCell = newRow.insertCell(0);
	 	    oCell.colSpan = 10;
	 	    oCell.innerHTML = i18n("alert.noalertsfound");
	 	    var str = document.getElementById("tab3text").innerHTML;
			document.getElementById("tab3text").innerHTML = str.substring(0, str.indexOf(':(') +1 ) + "(0)</a>";
		}else{
			var str = document.getElementById("tab3text").innerHTML;
			document.getElementById("tab3text").innerHTML = str.substring(0, str.indexOf(':(') +1) + "(" + alerts.length/2 + ")</a>";
		 	for (var i = 0; i < alerts.length; i ++){
		 		if(alerts[i].geometry.type == "LineString"){
		 			continue; //This is a track feature, ignore it for drawing the table of alerts.
		 		}
		 		var date = alerts[i].properties.date;
		 		var d = date.substr(0, date.length -4);

		 		var row = tableCreateRowTDs(parent,
		 				[alerts[i].properties.type, alerts[i].properties.id, d , alerts[i].properties.desc, alerts[i].properties.level.toString(), alerts[i].properties.status, Math.round(alerts[i].properties.x * 100000)/100000 + " , " + Math.round(alerts[i].properties.y * 100000)/100000, null], 
		 				"alertrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
		 		row.id = "alertRow" + i;
		 		row.dataset.uuid = alerts[i].properties.uuid;
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
	 	}
	}catch(err) {
 		var newRow = parent.insertRow(-1);
 		newRow.style.backgroundColor = "#F00";
 		newRow.className = "alertrow";
 	    var oCell = newRow.insertCell(0);
 	    oCell.colSpan = 10;
 	    oCell.innerHTML = err;
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

//callback for update alert clicked, fill in update alert form with current details 
function showCurrentAlert() {
	if (this.status == 200 ) {
		var r = JSON.parse(this.response);
	} else {
		displayError(parseError(i18n("alert.errorgettingalert") + this.uuid));
	}
	
	document.querySelector("#dialogerror").style.display = "none";
	displayDialog('updateAlertDialog', 'alerttable');
	
	document.getElementById("updatealertform").user_id.value = r.userGeneratedId;
	
	document.getElementById("updatealertform").update_alert_ca.value = r.caUuid;
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
		    "caUuid": form.update_alert_ca.value,
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
		displayInfo("Alert with UUID " + r.uuid + " Updated.");
		refreshAlerts();
	} else {
		displayError(parseError(i18n("alert.errorupdating") + this.statusText + "; " + this.responseText));
	}
	
	closeDialog('updateAlertDialog');

}

function hideShowFilters(){


	var current = document.getElementById('filter-form').style.display;
	if(current == "none"){
		document.getElementById('filter-form').style.display = "block";
		document.getElementById('filter-link').innerHTML = '<image id="filter-button"/>' + i18n("alert.hidefilters");
	}else{
		document.getElementById('filter-form').style.display = "none";
		document.getElementById('filter-link').innerHTML = '<image id="filter-button"/>' + i18n("alert.showfilters");
	}
}


function getFilteredUrl(base){
	var filteredUrl = base;

	var started = {value: false};//using an object so it can be modified in the function calls easily (objects are pass-by-reference)

	filteredUrl += getFilter("filterType", started, "typeUuidFilter");
	filteredUrl += getFilter("filterStatus", started, "statusFilter");
	filteredUrl += getFilter("filterImportance", started, "levelFilter");
	filteredUrl += getFilter("filterCa", started, "caUuidFilter");
	
	filteredUrl += "&textSearchFilter=" +  document.getElementById("filterText").value;

	var dateSelect = document.getElementById("filterDate").value;
	
	if(dateSelect == -1){//custom dates
		var from = new Date(document.getElementById('datePickerFrom').value.substring(4)).getTime();//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		var to = new Date(document.getElementById('datePickerTo').value.substring(4)).getTime() + 86399999; //use end of the day, since it is the "to" date.

		if(isNaN(to) || isNaN(from) || from > to){
			displayError(i18n("alert.invalidcustomdates"));
		}else{
			filteredUrl += "&startDateFilter=" + from;  
			filteredUrl += "&endDateFilter=" + to 
		}
	}else if(dateSelect == -99){//all-time
		//do nothing, no filter gives all dates back.
	}else if(dateSelect >0){ //number of trailing hours from now
		var now = new Date();
		now = now.getTime();
		
		var start = new Date()
		var start = start.getTime() - dateSelect*60*60*1000;  
		
		filteredUrl += "&startDateFilter=" +  start; 
		filteredUrl += "&endDateFilter=" + now; //leaving this out for now, we can show things in the future if times are off slightly
	}

	filteredUrl += "&sortBy=" + document.getElementById('sortBy').value + "&sortAscending=" + document.getElementById('sortAscending').value;
	filteredUrl += "&maxAlertOverride=" + MAX_ALERTS;
	return filteredUrl;
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

function updateRealtimeLayer(updatedUrl){
	if(map.hasLayer(realtime)){
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
   //             var date = new Date(feature.properties.date);
                var date = feature.properties.date;
 //               var date = date.substr(0, feature.properties.date.length -4);
                
                
                if(feature.properties.type == undefined){
                	return "Track Selected - Please click an alert for alert details.";
            	}
                
            	//date = date.tostring();
                var text = i18n("alert.event");
                if(feature.properties.type == 'Unknown Type'){
                	text = text + "<font color='red'>"
                }
                text = text + feature.properties.type;
                if(feature.properties.type == 'Unknown Type'){
                	text = text + "</font>"
                }
                	
                text = text + "<br>" + i18n("alert.alertid") + feature.properties.id +
                	"<br>" + i18n("alert.reportedtime") + date +
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
        document.getElementById("map-info-box").innerHTML = i18n("alert.lastupdated") + now.getDate() + "/" + month + "/" + now.getFullYear() + " " + now.getHours() + ":" + minutes + ":" + seconds + "  <a href='javascript:refreshAlerts()'>update now</a>";
    });
    
    realtime.addTo(map);
//    layerControl.addOverlay(realtime);
}


//----------------------------------------------------------------------------
//javascript client-side filtering. -Might need to go back to this in future or have some combination. For now it is all server-side
//(we don't really want to send 1000 alerts every request, then filter out past 24hrs or only active ones etc.)

//function eventFilter(feature, layer){
//	if(applyFilter("filterImportance",feature.properties.level)
//			&& applyFilter("filterType",feature.properties.typeuuid) 
//			&& applyFilter("filterStatus",feature.properties.status)
//			&& applyFilter("filterCa",feature.properties.cauuid)
//			&& (applyTextFilter("filterText",feature.properties.desc) || applyTextFilter("filterText",feature.properties.id))
//			){
//		return true;
//	}
//	return false;
//}
//
//function applyFilter(classname, value){
//	var options = document.getElementsByClassName(classname);
//	for (var i = 0; i < options.length; i++){
//		if(options[i].checked){
//			if(options[i].value == value) return true;
//		}
//	}
//	return false;
//}
//function applyTextFilter(id, value){
//	var search = document.getElementById(id);
//	if(search.value == "") return true; //blank text search = show everything.
//	if(search.value.search(value) > 0 ) return true;
//	return false;
//}
//----------------------------


//styles for points
function stylePoints(feature, latlng) {
	
	var color = styleColors[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
//	var fillColor = styleFillColors[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
	var opacity = styleOpacity[feature.properties.typeuuid]; //styleColors is defined in alert.jsp's <head>
	var markerIcon = styleMarkerIcon[feature.properties.typeuuid]; 
	var markerColor = styleMarkerColor[feature.properties.typeuuid];
	var spinStr = styleSpin[feature.properties.typeuuid];
	if(spinStr == "true"){
		spin = true;
	}else{
		spin = false;
	}
	
	//various size circles, using icons now, maybe we want both options?
//	var size = 11 - (feature.properties.level * 1.5);
//	
//	if(color == "")color="#000000";
//	if(fillColor == "")fillColor="#0000ff";
//	if(opacity == "")opacity="0.5";
//	var geojsonMarkerOptions = {
//		    radius: size,
//		    fillColor: fillColor,
//		    color: color,
//		    weight: 1,
//		    opacity: 1,
//		    fillOpacity: opacity
//		};
//    return L.circleMarker(latlng, geojsonMarkerOptions);
	marker  = L.AwesomeMarkers.icon({
		prefix: 'fa',
	    icon: markerIcon,
	    iconColor: color,
	    markerColor: markerColor,
	    spin: spin
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
		if (this.status == 401){
			msg += i18n("alert.unathorized");
		}else if (this.status == 404){
			msg += i18n("alert.invalidurl");
		}else{
			msg = JSON.parse(this.responseText).error
		}
		
		displayError(msg);
		return false;
	}
	

	var geojson = JSON.parse(this.responseText);
 	var defaults = geojson[0];
 	
 	interval = defaults.secondsRefresh * 1000;
 	
 	//timer to refresh the alerts at the set interval
 	setInterval(function(){
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
 	
 	
 	//default custom dates so they are not blank to start
 	var today = getTodayAsString();
    document.getElementById("datePickerFrom").value = today;
    document.getElementById("datePickerTo").value = today;
 	

 	
 	checkForCustomDates();
 	
 	refreshAlerts();
}

//check if the user selected or deselected "custom dates" and grey/de-grey the custom inputs. 
function checkForCustomDates(){
	var date = document.getElementById('filterDate');
	if(date.value == -1){
		document.getElementById('datePickerFrom').disabled = false;
		document.getElementById('datePickerTo').disabled = false;
	}else{
		document.getElementById('datePickerFrom').disabled = true;
		document.getElementById('datePickerTo').disabled = true;
	}
}


function getTodayAsString(){
 	var today = new Date();
    var dd = today.getDate();
    var mm = today.getMonth()+1; //January is 0!

    var yyyy = today.getFullYear();
    if(dd<10){
        dd='0'+dd
    } 
    if(mm<10){
        mm='0'+mm
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

function sort(str){
	var asc = document.getElementById('sortAscending').value;
	var cur = document.getElementById('sortBy').value; 
	if( cur == str){
		if(asc == "true"){
			document.getElementById('sortAscending').value = "false";
		}else{
			document.getElementById('sortAscending').value = "true";
		}
	}else{
		document.getElementById('sortBy').value = str;
	}
	
	refreshAlerts();
}


