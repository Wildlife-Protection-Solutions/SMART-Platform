var ALERT_URL = "../api/connectalert/";
var USER_URL = "../api/connectuser/";
var ACTION_URL = USER_URL + "actions/";
var allActions = null;


function initializePage(){
	//Hide header/footer and menu if mobile parameter is set:
	if(mobile == "true"){
		document.getElementById('mainheader').style.display = 'none';
		document.getElementById('verticalmenu').style.display = 'none';
		document.getElementById('footerid').style.display = 'none';
	}

	//initialize the map
	var map = new L.Map('map', {center: new L.LatLng(-7.5, 34.44), zoom: 8});
    var googleLayer = new L.Google('ROADMAP');
    map.addLayer(googleLayer);
    
	/*
	L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
	    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
	    maxZoom: 18,
	    id: 'jeffloun.mp3jogfm',
	    accessToken: 'pk.eyJ1IjoiamVmZmxvdW4iLCJhIjoiOTYyMGFkZDk5ZWM2ZDQ5NDc5Njc2Y2ZlOGM4YjQ1YWIifQ.R715pq8aRAM9hRdGcy10Xg'
	}).addTo(map);
	*/
	
	/*var testwms = L.tileLayer.wms("https://editor.giscloud.com/wms/bdd66dd4ade33e6b69aed41b64b2b294", {
	    layers: '1084716:canada_major_lakes',
	    format: 'image/png',
	    transparent: true,
	    attribution: "giscloud.com"
	});
	testwms.addTo(map);
	*/
    
    
    //initialize the tab styles
    settab(tab);
    
    //set the lat/long if we can get them from the device automatically
    if (navigator.geolocation) {
    	navigator.geolocation.getCurrentPosition(showLatLong, showError)
    }
    
    //new alert dialog
	document.querySelector("#newalertform").onsubmit = createNewAlert;
	
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
	remove_all_tab_classes();
	switch(tab){
		case 2:
			document.getElementById('tab2').className += "selectedTab";
			document.getElementById('tab2text').className += "selectedTab";

			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab3').className += "unselectedTab";
			document.getElementById('tab3text').className += "unselectedTab";
			break;
		case 3:
			document.getElementById('tab3').className += "selectedTab";
			document.getElementById('tab3text').className += "selectedTab";
			
			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab2').className += "unselectedTab";
			document.getElementById('tab2text').className += "unselectedTab";
			break;
		default:
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
