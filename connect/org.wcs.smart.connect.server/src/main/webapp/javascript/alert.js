var ALERT_URL = "../api/connectalert/";
var USER_URL = "../api/connectuser/";
var ACTION_URL = USER_URL + "actions/";
var allActions = null;
var interval = 30000; //# of seconds between map refresh on the alert layer


window.onload = function(){
	//Hide header/footer and menu if mobile parameter is set:
	if(mobile == "true"){
		document.getElementById('mainheader').style.display = 'none';
		document.getElementById('verticalmenu').style.display = 'none';
		document.getElementById('footerid').style.display = 'none';
		
		document.body.style.width = '38em';
	}

	
   
    
    
    // The real-time layer that auto-refreshes to show alerts
    realtime = L.realtime({
    	  url: ALERT_URL,
        crossOrigin: true,
        type: 'json'
    }, {
        interval: interval
    });

    realtime.on('update', function(e) {
        var coordPart = function(v, dirs) {
                return dirs.charAt(v >= 0 ? 0 : 1) +
                    (Math.round(Math.abs(v) * 100) / 100).toString();
            },
            popupContent = function(fId) {
                var feature = e.features[fId],
                    c = feature.geometry.coordinates;
                return 'Event: ' + feature.properties.type + " - " + feature.properties.id + 
                	"<br>Reported time: " + feature.properties.date +
                	"<br>Location: " +
                    coordPart(c[1], 'NS') + ', ' + coordPart(c[0], 'EW');
            },
            bindFeaturePopup = function(fId) {
                realtime.getLayer(fId).bindPopup(popupContent(fId));
            },
            updateFeaturePopup = function(fId) {
                realtime.getLayer(fId).getPopup().setContent(popupContent(fId));
            };


        Object.keys(e.enter).forEach(bindFeaturePopup);
        Object.keys(e.update).forEach(updateFeaturePopup);
    });
    
    //google maps layer - Not supposed to use this with private maps/data, so we won't. 
    //    var googleLayer = new L.Google('ROADMAP');
    //    map.addLayer(googleLayer);

    //OSM Basemap Layer - the only Hardcoded basemap
    var osmUrl='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
	var osmAttrib='Map data © <a href="http://openstreetmap.org">OpenStreetMap</a> contributors';
	var osm = new L.TileLayer(osmUrl, {minZoom: 1, maxZoom: 18, attribution: osmAttrib});		

	
	var baseMaps = {
			"OSM Standard": osm
	};
	
	var dataLayers = {
			"Events": realtime
	};
	
	var activeLayers = [osm, realtime];

	
	for (i = 0; i < mapLayers.length; ++i) {
		//Mapbox layer type
		if(mapLayers[i][0] == 1){
			var accesstoken = mapLayers[i][1];
			var mapboxId = mapLayers[i][2];

			var layer = L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
			    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
			    maxZoom: 18,
			    id: mapboxId,
			    accessToken: accesstoken
			});

			//add the new layer to the list of datalayers
			dataLayers[mapLayers[i][4]] = layer;
			
			//add to layer list so it is active to start with
			if(mapLayers[i][5]){
				activeLayers.push(layer);
			}
			
		}else if(mapLayers[i][0] == 2){

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
			if(mapLayers[i][5]){
				activeLayers.push(giscloud);
			}
		}
	    
	}
//-----------------------	
			
	//initialize the map
	var map = new L.Map('map', {center: new L.LatLng(-7.5, 34.44), zoom: 8, layers: activeLayers});
	
	//add layer control to map
	L.control.layers(baseMaps, dataLayers).addTo(map);
	

	
	

    
    
    //initialize the tab styles
    settab(tab);
    
    //set the lat/long if we can get them from the device automatically
    if (navigator.geolocation) {
    	navigator.geolocation.getCurrentPosition(showLatLong, showError)
    }
    
    
    
    //setup action events
    
    //new alert dialog
	document.querySelector("#newalertform").onsubmit = createNewAlert;
	
	refreshAlerts();
	setTableActions();
	
}


function setTableActions(){
	//delete alert clicked
	elements = document.querySelectorAll(".deletealert");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=deleteAlert;
	}
}

//creates a new user
function createNewAlert() {
	var cauuid = document.querySelector("select[name=alert_ca]").value;
	var alerttypeuuid = document.querySelector("select[name=alert_type]").value;
	var long = document.querySelector("input[name=long]").value;
	var lat = document.querySelector("input[name=lat]").value;
	var desc = document.querySelector("textarea[name=alert_description]").value;
	var level = document.querySelector("select[name=level]").value;
	
	var error = "";
	if (long > 180 || long < -180 ) {
		error = "Invalid Longitude (-180 < Long < 180)";
	}else if (lat > 90 || lat < -90){
		error = "Invalid Latitude(-90 < Lat < 90)";
	}

	if (error.length > 0){
		document.querySelector("#dialogerror").innerHTML = error;
		document.querySelector("#dialogerror").style.display = "block";
		return false;
	}
	usergenid = Math.random() * 1000000000;
	usergenid = Math.round(usergenid);
	
	
	var jsonData = {
		"userGeneratedId" : usergenid,
		"caUuid" : cauuid,
		"description" : desc,
		"typeUuid" : alerttypeuuid,
		"x" : long,
		"y" : lat,
		"level" : level
	};

	//make ajax call
	hideError();
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oReq = new XMLHttpRequest();
	oReq.onload = alertCreated;
	oReq.open("POST", ALERT_URL + encodeURIComponent(usergenid), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

//callback for creating user 
function alertCreated() {
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo("Alert created");
	} else {
		displayError(parseError("Error creating Alert", this.responseText));
	}
	
	refreshAlerts();

}
/* loads all user actions from server */
function loadActions(){
	if (allActions != null) return;
	var oReq = new XMLHttpRequest();
	oReq.onload = setActions;
	oReq.open("Get", ACTION_URL, true);
	oReq.send();	
}

/* callback from loadActions to cache user actions */
function setActions(){
	if (this.status != 200){
		//do something with error
	}else{
		allActions = JSON.parse(this.responseText);
		updateActionsDropDown();
	}
}

function showLatLong(position){
	document.getElementById("lat").value = position.coords.latitude
	document.getElementById("long").value = position.coords.longitude
    	
}

function showError(error) {
   	document.getElementById("long").value = "unable to auto-detect location";
}

function settab(tab){
	hideInfo();
	hideError();

	remove_all_tab_classes();
	switch(tab){
		case 2:
			document.getElementById("tab2").style.zIndex = 2;
			document.getElementById('tab2').className += "selectedTab";
			document.getElementById('tab2text').className += "selectedTab";

			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab3').className += "unselectedTab";
			document.getElementById('tab3text').className += "unselectedTab";
			break;
		case 3:
			document.getElementById("tab3").style.zIndex = 2;
			document.getElementById('tab3').className += "selectedTab";
			document.getElementById('tab3text').className += "selectedTab";
			
			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab2').className += "unselectedTab";
			document.getElementById('tab2text').className += "unselectedTab";
			break;
		default:
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
	remove_class("tab1", "selectedTab");	
	remove_class("tab2", "selectedTab");
	remove_class("tab3", "selectedTab");
	remove_class("tab1text", "selectedTab");
	remove_class("tab2text", "selectedTab");
	remove_class("tab3text", "selectedTab");
	remove_class("tab1", "unselectedTab");
	remove_class("tab2", "unselectedTab");
	remove_class("tab3", "unselectedTab");
	remove_class("tab1text", "unselectedTab");
	remove_class("tab2text", "unselectedTab");
	remove_class("tab3text", "unselectedTab");
	
	document.getElementById("tab1").style.zIndex = 0;
	document.getElementById("tab2").style.zIndex = 0
	document.getElementById("tab3").style.zIndex = 0

}

function remove_class(id, classname){
	var regex = new RegExp("(?:^|\s)" + classname + "(?!\S)", "g");
	document.getElementById(id).className = document.getElementById(id).className.replace( regex , '' )
}


/* add alert action */
function createAlert(username){
	var ddactions = document.querySelector("#actionKey");
	var ddresources = document.querySelector("#actionResourceKey");
	
	var selectedActionKey = ddactions.options[ddactions.selectedIndex].value;
	var selectedResourceKey = ddresources.options[ddresources.selectedIndex].value;
	
	hideInfo();
	hideError();
	var oReq = new XMLHttpRequest();
	oReq.onload = actionAdded;
	oReq.smartuser = username;
	var loc = ACTION_URL + encodeURIComponent(username) + "/" + encodeURIComponent(selectedActionKey);
	if (selectedResourceKey.length > 0){
		loc += "/" + selectedResourceKey;
	}
	oReq.open("POST", loc, true);
	oReq.send();
}

/* delete alert*/
function deleteAlert(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var ok = window.confirm("Are you sure you want to delete the alert?");
	if (!ok) return;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = alertDeleted;
//	oReq.smartuser=username;
	oReq.open("DELETE", ALERT_URL + encodeURIComponent(uuid), true);
	oReq.send();
	return false;	
}

//callback for delete alert  
function alertDeleted() {
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo("Deleted Alert with UUID: " + r.uuid);
	} else {
		displayError(parseError("Error deleting alert " + this.uuid));
	}
	refreshAlerts();
	
}

/* reload alert table */
function refreshAlerts(){
	//clear current table
	var objects = document.querySelectorAll("tr.alertrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById("alerttable");
	var row = document.createElement("tr");
	row.className="alertrow";
	row.innerHTML="Refreshing Alert Table...";
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createAlertTable;
 	oReq.open("Get", ALERT_URL, true);
 	oReq.send();
}

/* callback that displays all alert info */
function createAlertTable(){
	
	if (this.status != 200 && this.status != 201 ) {
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
	//clear current table
	var objects = document.querySelectorAll("tr.alertrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("alerttable");
 	var geojson = JSON.parse(this.responseText);
 	var alerts = geojson.features;
 	for (var i = 0; i < alerts.length; i ++){
 		var d = new Date(alerts[i].properties.date);
 		var row = tableCreateRowTDs(parent,
 				[alerts[i].properties.type, alerts[i].properties.id, d.toLocaleString() , alerts[i].properties.desc, alerts[i].properties.level.toString(), alerts[i].properties.status, Math.round(alerts[i].properties.x * 100000)/100000 + " , " + Math.round(alerts[i].properties.y * 100000)/100000, null], 
 				"alertrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.id = "alertRow" + i;
 		row.dataset.uuid = alerts[i].properties.uuid;
 	
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete alert";
 		deleteicon.onclick = deleteAlert;
 		deleteicon.href="";
 		row.childNodes[7].appendChild(deleteicon);
 	}
}

