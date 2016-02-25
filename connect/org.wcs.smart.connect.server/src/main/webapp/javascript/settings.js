var STYLE_URL = "../api/connectstyle/";
var LAYER_URL = "../api/maplayer/";
var TYPE_URL = "../api/connectalert/alertTypes/";
var DEFAULTS_URL = "../api/connectalertfilterdefault/";
var ACTION_URL = STYLE_URL + "actions/";
var allActions = null;

//A selection of icons, the full list, which you can type into the text box is at http://fortawesome.github.io/Font-Awesome/icons/

var iconOptions = ["ambulance","asterisk","battery-quarter","binoculars","bomb","bug","bullseye","bus","bullhorn","car","check","circle","cog",
                   "clipboard","cloud","crosshairs","exclamation","eye","fire","flask","gavel","group","heartbeat","home","leaf","money","motorcycle","pause","paw",                  
                   "plane","play","stop","tree","trophy","user"];

var iconOptionsLabels = ["ambulance","asterisk","battery low","binoculars","bomb","bug","bullseye","bus","bullhorn","car","check","circle","cog",
                         "clipboard","cloud","crosshairs","exclamation","eye","fire","flask/chemical","gavel","group of people", "heartbeat","home","leaf","money",
                         "motorcycle","pause","paw","plane","play","stop","tree","trophy","person"];


/* configure events on html elements */
window.onload = function(){
	//add new style   ---  
	document.getElementById("btnNewStyleConfiguration").onclick=clearAndShowNewStyleDialog;
	if(numStyles > 0){
		document.getElementById("btnNewStyleConfiguration").style.display = "none";
	}
	//delete style
	elements = document.querySelectorAll(".deleteStyle");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=deleteStyle;
	}
	
	//new style dialog
	document.getElementById("cancelNewStyle").onclick = function(){
		closeDialog('newStyleDialog');
	};
	document.getElementById("newstyleform").onsubmit = createNewStyle;
	
	
	
	//new Layers dialog
	document.getElementById("btnNewLayer").onclick = function(){
	 	displayDialog('layerDialog', 'layertable');
	 	document.getElementById("updateLayerButton").classList.remove("show");
		document.getElementById("updateLayerButton").classList.add("hide");
		document.getElementById("newLayerButton").classList.remove("hide");
		document.getElementById("newLayerButton").classList.add("show");
	} 
	document.getElementById("cancelLayer").onclick = function(){
		closeDialog('layerDialog');
	};
	document.getElementById("newLayerButton").addEventListener("click", createNewLayer);
	document.getElementById("updateLayerButton").addEventListener("click", submitUpdateLayer);
	document.getElementById("type_markerIcon").addEventListener("change", updateExampleIcon);
	document.getElementById("iconOveride").addEventListener("change", updateExampleIconCustom);
	
	document.getElementById("btnNewType").onclick = function(){
	 	displayDialog('typeDialog', 'btnNewType');
	 	document.getElementById("updateTypeButton").classList.remove("show");
		document.getElementById("updateTypeButton").classList.add("hide");
		document.getElementById("newTypeButton").classList.remove("hide");
		document.getElementById("newTypeButton").classList.add("show");
	} 
	document.getElementById("cancelType").onclick = function(){
		closeDialog('typeDialog');
	};
	document.getElementById("newTypeButton").addEventListener("click", createNewType);
	document.getElementById("updateTypeButton").addEventListener("click", submitUpdateType);
//	document.getElementById("btnResetDefaults").addEventListener("click", refreshDefaults);
	document.getElementById("btnUpdateDefaults").addEventListener("click", saveDefaults);
	
	//Layer table and actions
	refreshLayers();
	refreshTypes();
	refreshStyleConfiguration();
	loadIconOptions();
	refreshDefaults();
}


function clearAndShowNewStyleDialog(){
 	document.querySelector("input[name=style_id]").value = "";
 	document.querySelector("#dialogerror").style.display = "none";
 	displayDialog('newStyleDialog', 'btnNewType');
}


//creates a new Style
function createNewStyle() {
		
	var style_id = document.getElementById("style_id").value;
	var bg_image = document.getElementById("bg_image").value;
	
	var error = "";
	if (error.length > 0){
		document.querySelector("#dialogerror").innerHTML = error;
		document.querySelector("#dialogerror").style.display = "block";
		return false;
	}
	
	var jsonData = {
		"styleId" : style_id
	};
	//make ajax call
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oData;
	//option 1
//	oData = new FormData();
//	oData.append("json", jsonData);
//	oData.append("bg_image", bg_image);
		
	//option 2
	var form = document.getElementById("newstyleform");
	oData = new FormData(form);
	
	closeDialog('newStyleDialog');
	var oReq = new XMLHttpRequest();
	oReq.onload = styleCreated;
	oReq.open("POST", STYLE_URL, true);
	oReq.send(oData);
	return false;
}


//callback for creating style 
function styleCreated() {
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(user.username + i18n("settings.stylecreated"));
	} else {
		displayError(parseError(i18n("settings.errorcreatingstyle"), this.responseText));
	}
	refreshStyleConfiguration();
}


//callback for creating user 
function userCreated() {
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(user.username + i18n("settings.accountcreated"));
	} else {
		displayError(parseError(i18n("settings.errorcreatingaccount"), this.responseText));
	}
	refreshUsers();
}


/* reload map layer table */
function refreshLayers(){
	//clear current Map layer table
	var objects = document.querySelectorAll("tr.layerrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById("layertable");
	var row = document.createElement("tr");
	row.className="layerrow";
	row.innerHTML=i18n("settings.refreshinglayers");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createLayerTable;
 	oReq.open("Get", LAYER_URL, true);
 	oReq.send();
}

/* callback that displays all layer info */
function createLayerTable(){
	
	if (this.status != 200 && this.status != 201 ) {
		var msg = i18n("alert.errorlabel");
		if (this.status == 401){
			msg += i18n("alert.unathorized");
		}else{
			msg += i18n("alert.servererror");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("tr.layerrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("layertable");
 	var layers = JSON.parse(this.responseText);
 	for (var i = 0; i < layers.length; i ++){
 		var type = layers[i].layerType;
 		var typeText = i18n("settings.unknown");
 		if(type == 1){
 			typeText = "Mapbox.com";
 		}else if(type == 2){
 			typeText = "GISCloud.com";
 		}else if (type==3){
 			typeText = "WMS";
 		}
 		var active = i18n("settings.false")
 		if (layers[i].active){
 			active = i18n("settings.true")
 		}
 		var row = tableCreateRowTDs(parent,
 				[layers[i].layerOrder, layers[i].layerName, typeText , active, layers[i].mapboxId, layers[i].wmsLayerList, null, null], 
 				"layerrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.id = "layerRow" + i;
 		row.dataset.uuid = layers[i].uuid;

 	    var scrollable = document.createElement("div");
 	    scrollable.className = "scrollable";
 	    scrollable.innerHTML = layers[i].token;
 		row.childNodes[6].appendChild(scrollable);

 		var updateicon = document.createElement("a");
 		updateicon.className="update-icon";
 		updateicon.title= i18n("settings.updatelayer");
 		updateicon.onclick = updateLayer;
 		updateicon.href="";
 		row.childNodes[7].appendChild(updateicon);

 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title= i18n("settings.deletelayer");
 		deleteicon.onclick = deleteLayer;
 		deleteicon.href="";
 		row.childNodes[7].appendChild(deleteicon);
 	}
}

/* delete layer*/
function deleteLayer(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var ok = window.confirm(i18n("settings.suredeletelayer"));
	if (!ok) return false;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = layerDeleted;
	oReq.open("DELETE", LAYER_URL + encodeURIComponent(uuid), true);
	oReq.send();
	return false;	
}

//callback for delete layer  
function layerDeleted() {
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("settings.deletedlayer") + r.layerName);
	} else {
		displayError(parseError(i18n("settings.errordeletinglayer") + this.uuid));
	}
	refreshLayers();
	
}

function createNewLayer(){
	
	var layer_order = document.querySelector("input[name=layer_order]").value;
	var layer_name = document.querySelector("input[name=layer_name]").value;
	var layer_mapbox_id = document.querySelector("input[name=layer_mapbox_id]").value;
	var layer_list = document.querySelector("input[name=layer_list]").value;
	var layer_token = document.querySelector("input[name=layer_token]").value;
	var layer_type = document.querySelector("select[name=layer_type]").value;
	var layer_status = document.querySelector("select[name=layer_status]").value;
	
	
	var jsonData = {
		"layerOrder" : layer_order,
		"layerName" : layer_name,
		"wmsLayerList" : layer_list,
		"layerType" : layer_type,
		"token" : layer_token,
		"mapboxId" : layer_mapbox_id,
		"active" : layer_status
	};

	//make ajax call
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oReq = new XMLHttpRequest();
	oReq.onload = layerCreated;
	oReq.open("POST", LAYER_URL + encodeURIComponent(layer_name), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}


function layerCreated(){
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(i18n("settings.layercreated"));
	} else {
		displayError(i18n("settings.errorcreatinglayer") + this.responseText + "; " + this.statusText);
	}
	refreshLayers();
	closeDialog('layerDialog');
}

function updateLayer(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	document.getElementById("maplayersform").uuid.value = uuid;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = showCurrentLayer;
	oReq.open("GET", LAYER_URL + encodeURIComponent(uuid), true);
	oReq.send();
	return false;	
}


//callback for update layer clicked, fill in update layer form with current details 
function showCurrentLayer() {
	if (this.status == 200 ) {
		var r = JSON.parse(this.response);
	} else {
		displayError(parseError(i18n("settings.errorcreatinglayer") + this.uuid));
	}
	
	document.querySelector("#layerdialogerror").style.display = "none";
	
	var form = document.getElementById("maplayersform");
	
	form.layer_order.value = r.layerOrder;
	form.layer_name.value = r.layerName;
	form.layer_type.value = r.layerType;
	form.layer_status.value = r.active;
	form.layer_token.value = r.token;
	form.layer_mapbox_id.value = r.mapboxId;
	form.layer_list.value = r.wmsLayerList;

	document.getElementById("updateLayerButton").classList.remove("hide");
	document.getElementById("updateLayerButton").classList.add("show");
	
	document.getElementById("newLayerButton").classList.remove("show");
	document.getElementById("newLayerButton").classList.add("hide");
	
	displayDialog('layerDialog', 'layertable');
}


function submitUpdateLayer(){
	var uuid = document.getElementById("maplayersform").uuid.value;
	
	var layer_order = document.querySelector("input[name=layer_order]").value;
	var layer_name = document.querySelector("input[name=layer_name]").value;
	var layer_mapbox_id = document.querySelector("input[name=layer_mapbox_id]").value;
	var layer_list = document.querySelector("input[name=layer_list]").value;
	var layer_token = document.querySelector("input[name=layer_token]").value;
	var layer_type = document.querySelector("select[name=layer_type]").value;
	var layer_status = document.querySelector("select[name=layer_status]").value;
	
	
	var jsonData = {
		"layerOrder" : layer_order,
		"layerName" : layer_name,
		"wmsLayerList" : layer_list,
		"layerType" : layer_type,
		"token" : layer_token,
		"mapboxId" : layer_mapbox_id,
		"active" : layer_status
	};

	//make ajax call
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oReq = new XMLHttpRequest();
	oReq.onload = layerUpdated;
	oReq.open("PUT", LAYER_URL + encodeURIComponent(uuid), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.setRequestHeader("Accept","application/json");
	oReq.send(JSON.stringify(jsonData));	
}

function layerUpdated(){
	if (this.status == 200) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(i18n("settings.layerupdated") );
	} else {
		displayError(i18n("settings.errorupdatinglayer")  + this.responseText + "; " + this.statusText);
	}
	refreshLayers();
	closeDialog('layerDialog');
}

/* reload map types table */
function refreshTypes(){
	//clear current types table
	var objects = document.querySelectorAll("tr.typerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById("typetable");
	var row = document.createElement("tr");
	row.className="typerow";
	row.innerHTML=i18n("settings.refreshtypes");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createTypeTable;
 	oReq.open("Get", TYPE_URL, true);
 	oReq.send();
}

/* callback that displays all type info */
function createTypeTable(){
	
	if (this.status != 200 && this.status != 201 ) {
		var msg = i18n("alert.errorlabel");
		if (this.status == 401){
			msg += i18n("alert.unathorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table of the "refreshing..." message, so this isn't a total duplication of effort.
	var objects = document.querySelectorAll("tr.typerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("typetable");
 	var types = JSON.parse(this.responseText);
 	for (var i = 0; i < types.length; i ++){
 		var label = types[i].label;
 		var color = types[i].color;
 		var fillColor = types[i].fillColor;
 		var opacity = types[i].opacity;
 		var markerIcon = "<i class='fa fa-" + types[i].markerIcon + "'></i>";
 		var markerColor = i18n("settings." + types[i].markerColor);

 		var spin = types[i].spin;
 		

 		//not using fillColor for now, maybe we want more options in future.
 		var row = tableCreateRowTDs(parent,
 				[label, color, opacity, markerIcon, markerColor, spin, null], 
 				"white typerow");
 		row.id = "typerow" + i;
 		row.dataset.uuid = types[i].uuid;
 	
 		row.childNodes[1].style.backgroundColor = color;
 		if(color == "000000" || color == "#000000" ){
 			row.childNodes[1].style.color = "#ffffff";
 			row.childNodes[2].style.color = "#ffffff";
 		}
// 		row.childNodes[2].style.backgroundColor = fillColor;
// 		row.childNodes[2].style.opacity = opacity;
// 		if(fillColor == "000000"){
// 			row.childNodes[2].style.color = "#ffffff";
// 			row.childNodes[3].style.color = "#ffffff";
// 		}
 		row.childNodes[2].style.backgroundColor = color;
 		row.childNodes[2].style.opacity = opacity;
 		
 		row.childNodes[3].style.color = color;
 		 		
 		var updateicon = document.createElement("a");
 		updateicon.className="update-icon";
 		updateicon.title= i18n("settings.updatetype");
 		updateicon.onclick = updateType;
 		updateicon.href="";
 		row.childNodes[6].appendChild(updateicon);

 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title= i18n("settings.deletetype");
 		deleteicon.onclick = deleteType;
 		deleteicon.href="";
 		row.childNodes[6].appendChild(deleteicon);
 	}
}

function updateType(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	document.getElementById("alerttypesform").uuid.value = uuid;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = showCurrentType;
	oReq.open("GET", TYPE_URL + encodeURIComponent(uuid), true);
	oReq.send();
	return false;
}

function showCurrentType() {
	if (this.status == 200 ) {
		var r = JSON.parse(this.response);
	} else {
		displayError(parseError(i18n("settings.errorgettingalert") + this.label));
	}
	
	document.querySelector("#layerdialogerror").style.display = "none";
	
	var form = document.getElementById("alerttypesform");
	
	form.type_label.value = r.label;
	form.type_color.value = r.color;
//	form.type_fillcolor.value = r.fillColor;
	form.type_opacity.value = r.opacity;
	form.type_markerIcon.value = r.markerIcon;
	form.type_markerColor.value = r.markerColor;
	form.type_spin.value = r.spin;
	form.iconOveride.value = r.markerIcon;
	
	document.getElementById("exampleIcon").className = "fa fa-" + r.markerIcon;
	
	document.getElementById("type_color").style.backgroundColor = '#' + r.color;
//	document.getElementById("type_fillcolor").style.backgroundColor ='#' + r.fillColor;
	
	document.getElementById("updateTypeButton").classList.remove("hide");
	document.getElementById("updateTypeButton").classList.add("show");
	
	document.getElementById("newTypeButton").classList.remove("show");
	document.getElementById("newTypeButton").classList.add("hide");
	
	//update the sample icon

	displayDialog('typeDialog', 'btnNewType');
}

function deleteType(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var ok = window.confirm(i18n("settings.areyoursuredeletetype"));
	if (!ok) return false;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = typeDeleted;
	oReq.open("DELETE", TYPE_URL + encodeURIComponent(uuid), true);
	oReq.send();
	return false;
}

function typeDeleted(){
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("settings.deletedtype") + r.label);
	} else {
		displayError(parseError(i18n("settings.errordeletingtype") + this.response));
	}
	refreshTypes();
}

function createNewType(){
	
	var typeLabel = document.querySelector("input[name=type_label]").value;
	var typeColor = document.querySelector("input[name=type_color]").value;
//	var typeFillColor = document.querySelector("input[name=type_fillcolor]").value;
	var typeOpacity = document.querySelector("input[name=type_opacity]").value;
	var markerIcon = document.querySelector("select[name=type_markerIcon]").value;
	var markerColor = document.querySelector("select[name=type_markerColor]").value;
	var spin = document.querySelector("select[name=type_spin]").value;
	
	var jsonData = {
		"label" : typeLabel,
		"color" : typeColor,
//		"fillColor" : typeFillColor,
		"opacity" : typeOpacity,
		"markerIcon" : markerIcon,
		"markerColor" : markerColor,
		"spin" : spin
	};

	//make ajax call
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oReq = new XMLHttpRequest();
	oReq.onload = typeCreated;
	oReq.open("POST", TYPE_URL + encodeURIComponent(typeLabel), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}


function typeCreated(){
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
	} else {
		displayError(i18n("settings.errorcreatingtype") + this.responseText + "; " + this.statusText);
	}
	refreshTypes();
	closeDialog('typeDialog');
}

function submitUpdateType(){
	var uuid = document.getElementById("alerttypesform").uuid.value;
		
	var typeLabel = document.querySelector("input[name=type_label]").value;
	var typeColor = document.querySelector("input[name=type_color]").value;
//	var typeFillColor = document.querySelector("input[name=type_fillcolor]").value;
	var typeOpacity = document.querySelector("input[name=type_opacity]").value;
	var markerIcon = document.querySelector("select[name=type_markerIcon]").value;
	var override = document.getElementById("iconOveride").value
	if(override != ""){
		markerIcon = override;
	}
	
	var markerColor = document.querySelector("select[name=type_markerColor]").value;
	var spin = document.querySelector("select[name=type_spin]").value
	
	
	var jsonData = {
		"label" : typeLabel,
		"color" : typeColor,
//		"fillColor" : typeFillColor,
		"opacity" : typeOpacity,
		"markerIcon" : markerIcon,
		"markerColor" : markerColor,
		"spin" : spin
	};
	//make ajax call
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oReq = new XMLHttpRequest();
	oReq.onload = typeUpdated;
	oReq.open("PUT", TYPE_URL + encodeURIComponent(uuid), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.setRequestHeader("Accept","application/json");
	oReq.send(JSON.stringify(jsonData));
}

function typeUpdated(){
	if (this.status == 200) {
		//ok
		var user = JSON.parse(this.responseText);
	} else {
		displayError(i18n("settings.errorupdatingtype") + this.responseText + "; " + this.statusText);
	}
	refreshTypes();
	closeDialog('typeDialog');
}


/* reload filter defaults table */
function refreshDefaults(){
	//clear current defaults table
	var elem = document.getElementById('filter-form').elements;
    for(var i = 0; i < elem.length; i++){
    	elem[i].checked = false;
    }
	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createDefaultsTable;
 	oReq.open("Get", DEFAULTS_URL, true);
 	oReq.send();
}

//
function createDefaultsTable(){
	if (this.status != 200 && this.status != 201 ) {
		var msg = "Error: ";
		if (this.status == 401){
			msg += i18n("alert.unathorized");
		}else if (this.status == 404){
			msg += i18n("alert.invalidurl");
		}else{
			msg += i18n("alert.servererror");
		}
		
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("tr.defaultsrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("defaultstable");
 	
	var list = JSON.parse(this.responseText);
	document.getElementById('filterDate').value = list[0].defaultPastHours;
		
	var types = list[0].defaultTypeUuids.split(',');
	for(x=0 ; x < types.length; x++){
		var type = document.getElementById(types[x])
		if(type != null){
			type.checked = true;
		}
	}
	document.getElementById('status_ACTIVE').checked = list[0].defaultActive;
	document.getElementById('status_DISABLED').checked = list[0].defaultDisabled;

	document.getElementById('level1').checked = list[0].defaultLevel1;
	document.getElementById('level2').checked = list[0].defaultLevel2;
	document.getElementById('level3').checked = list[0].defaultLevel3;
	document.getElementById('level4').checked = list[0].defaultLevel4;
	document.getElementById('level5').checked = list[0].defaultLevel5;

	var cas = list[0].defaultCaUuids.split(',');
	for(x=0 ; x < cas.length; x++){
		var ca = document.getElementById(cas[x])
		if(ca != null){
			ca.checked = true;
		}
	}

	if(list[0].defaultText != null){
		document.getElementById('filterText').value = list[0].defaultText;
	}
	
	document.getElementById('secondsRefresh').value = list[0].secondsRefresh;
	document.getElementById('startingZoom').value = list[0].startingZoomLevel;
	document.getElementById('startingLong').value = list[0].startingLong;
	document.getElementById('startingLat').value = list[0].startingLat;
	
	document.getElementById("filter_uuid").value = list[0].uuid;
}

function saveDefaults(){
	var json = getFilterJSON();
	var uuid = document.getElementById("filter_uuid").value;
	
	//make ajax call
	hideInfo();
	document.querySelector("#message").style.display = "none";

	var oReq = new XMLHttpRequest();
	oReq.onload = defaultsUpdated;
	oReq.open("PUT", DEFAULTS_URL + encodeURIComponent(uuid), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.setRequestHeader("Accept","application/json");
	oReq.send(JSON.stringify(json));
}


function getFilterJSON(){
	var json = "";
	var date = document.getElementById("filterDate").value;

	var types = getCommaSeparatedList("filterType");
	var cas = getCommaSeparatedList("filterCa");

	json = {
			"defaultPastHours" : document.getElementById("filterDate").value,
			"defaultTypeUuids" :types, 
			"defaultActive" : document.getElementById("status_ACTIVE").checked,
			"defaultDisabled" : document.getElementById("status_DISABLED").checked, 
			"defaultLevel1" : document.getElementById("level1").checked,
			"defaultLevel2" : document.getElementById("level2").checked,
			"defaultLevel3" : document.getElementById("level3").checked,
			"defaultLevel4" : document.getElementById("level4").checked,
			"defaultLevel5" : document.getElementById("level5").checked,
			"defaultCaUuids" : cas,
			"defaultText" : document.getElementById("filterText").value,
			"secondsRefresh" : document.getElementById("secondsRefresh").value,
			"startingZoomLevel" : document.getElementById("startingZoom").value,
			"startingLong" : document.getElementById("startingLong").value,
			"startingLat" : document.getElementById("startingLat").value
			
		};
	return json;
}

function defaultsUpdated(){
	if (this.status == 200) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(i18n("settings.defaultfiltersupdates"));
	} else {
		displayError(i18n("settings.errorupdaingdefaultfilters") + this.responseText + "; " + this.statusText);
	}
}

function getCommaSeparatedList(classname){
	var str = "";
	var options = document.getElementsByClassName(classname);
	for (var i = 0; i < options.length; i++){
		if(options[i].checked){
			str += options[i].value + ",";
		}
	}
	return str;
}

/* reload style configuration*/
function refreshStyleConfiguration(){
	//clear current table
	var objects = document.querySelectorAll("tr.stylerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById("styletable");
	var row = document.createElement("tr");
	row.className="stylerow";
	row.innerHTML=i18n("settings.refreshtypes");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createStyleConfigurationTable;
 	oReq.open("Get", STYLE_URL, true);
 	oReq.send();
}

/* callback that displays all style configuration info */
function createStyleConfigurationTable(){
	//clear current table of the "refreshing..." message, so this isn't a total duplication of effort.
	var objects = document.querySelectorAll("tr.stylerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	
	if (this.status != 200 && this.status != 201 ) {
		var msg = i18n("alert.errorlabel");
		if (this.status == 401){
			msg += i18n("alert.unathorized");
			displayError(msg); 
		}
		return;
	}
	
	
	var parent = document.getElementById("styletable");
 	var style = JSON.parse(this.responseText);

 	var styleId = style.styleId;
 	var footerText = style.footerText;
 	var serverName = style.serverName;
 	var active = style.active;
 	
 	var row = tableCreateRowTDs(parent,
 				[styleId, active,serverName, footerText, null], 
 				"white stylerow");
 	row.id = "stylerow" + i;
 	row.dataset.uuid = style.uuid;
	
	var deleteicon = document.createElement("a");
	deleteicon.className="delete-icon";
	deleteicon.title= i18n("settings.deletetype");
	deleteicon.onclick = deleteStyle;
	deleteicon.href="";
	row.childNodes[4].appendChild(deleteicon);

}

function updateStyle(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	document.getElementById("newstyleform").style_uuid.value = uuid;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = showCurrentStyle;
	oReq.open("GET", STYLE_URL , true);
	oReq.send();
	return false;	
}

function showCurrentStyle(){
	if (this.status == 200 ) {
		var r = JSON.parse(this.response);
	} else {
		displayError(parseError(i18n("settings.errorgettingstyle") + this.label));
	}
	
	document.querySelector("#dialogerror").style.display = "none";
	
	var form = document.getElementById("newstyleform");
	
	form.style_id.value = r.styleId;
	
	displayDialog('updateStyleDialog', 'btnNewStyleConfiguration');
}

function deleteStyle(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var ok = window.confirm(i18n("settings.areyoursuredeletetype"));
	if (!ok) return false;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = typeDeleted;
	oReq.open("DELETE", STYLE_URL, true);
	oReq.send();
	return false;
}

function typeDeleted(){
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("settings.deletedstyle") + r.label);
	} else {
		displayError(parseError(i18n("settings.errordeletingstyle") + this.response));
	}
	refreshStyleConfiguration();
	document.getElementById("btnNewStyleConfiguration").style.display = "block";
}

function updateExampleIcon(){
	document.getElementById("exampleIcon").className = "fa fa-" + document.getElementById("type_markerIcon").value;
	document.getElementById("iconOveride").value = document.getElementById("type_markerIcon").value
	return false;
}
function updateExampleIconCustom(){
	document.getElementById("exampleIcon").className = "fa fa-" + document.getElementById("iconOveride").value;	
	document.getElementById("type_markerIcon").value = document.getElementById("iconOveride").value;
}
function loadIconOptions(){
	var select = document.getElementById("type_markerIcon");
	for (var i = 0; i<=iconOptions.length-1; i++){
	    var opt = document.createElement('option');
	    opt.value = iconOptions[i];
	    opt.innerHTML = iconOptionsLabels[i];
	    select.appendChild(opt);
	}
}
