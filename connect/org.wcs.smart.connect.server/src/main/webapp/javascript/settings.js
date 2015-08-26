var STYLE_URL = "../api/connectstyle/";
var LAYER_URL = "../api/maplayer/";
var ACTION_URL = STYLE_URL + "actions/";
var allActions = null;



/* configure events on html elements */
window.onload = function(){
	//add new user
	document.querySelector("#btnNewStyle").onclick=clearAndShowNewStyleDialog;
	if(numStyles > 0){
		document.getElementById("btnNewStyle").style.display = "none";
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
	 	displayDialog('layerDialog', 'main');
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
	
	
	//Layer table and actions
	refreshLayers();

}


/* clears and displays new user dialog */
function clearAndShowNewStyleDialog(){
 	document.querySelector("input[name=style_id]").value = "";
 	document.querySelector("#dialogerror").style.display = "none";
 	displayDialog('newStyleDialog', 'main');
}

/* delete user */
function deleteUser(){
	var username = this.dataset.username;
	var ok = window.confirm("Are you sure you want to delete the selected style?");
	if (!ok) return;
	
	hideInfo();
	hideError();
	
/*	
 * TODO - fill out proper URL etc
 * 
 * var oReq = new XMLHttpRequest();
	oReq.onload = userDeleted;
	oReq.smartuser=username;
	oReq.open("DELETE", USER_URL + encodeURIComponent(username), true);
	oReq.send();
	return false;
	
		*/
}



//callback for delete user  
function userDeleted() {
	if (this.status == 200) {
		displayInfo(this.smartuser + " deleted");
	} else {
		displayError(parseError("Error deleting account " + this.smartuser, this.responseText));
	}
	refreshUsers();
	
	//if delete the logged in user; refresh page to auto logout
	var currentUser = document.querySelector("#userlogin");
	if (currentUser != null && currentUser.dataset.username != null && this.smartuser === currentUser.dataset.username){
		location.reload(true);
	} 
}


//creates a new Style
function createNewStyle() {
	var pass1 = document.querySelector("input[name=password1]").value;
	var pass2 = document.querySelector("input[name=password2]").value;
	var user = document.querySelector("input[name=username]").value;
	var email = document.querySelector("input[name=email]").value;
	
	var error = "";
	if (user.length == 0 ) {
		error = "Username required";
	}else if (pass1.length == 0){
		error = "Password required";
	}else if (pass1 != pass2){
		error = "Passwords do not match";
	}

	if (error.length > 0){
		document.querySelector("#dialogerror").innerHTML = error;
		document.querySelector("#dialogerror").style.display = "block";
		return false;
	}
	
	var jsonData = {
		"username" : user,
		"email" : email,
		"password" : pass1
	};

	//make ajax call
	hideError();
	hideInfo();
	document.querySelector("#message").style.display = "none";

	closeDialog('newUserDialog');
	var oReq = new XMLHttpRequest();
	oReq.onload = userCreated;
	oReq.open("POST", USER_URL + encodeURIComponent(user), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

//callback for creating user 
function userCreated() {
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(user.username + " account created");
	} else {
		displayError(parseError("Error creating account", this.responseText));
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
	row.innerHTML="Refreshing Layer Table...";
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createLayerTable;
 	oReq.open("Get", LAYER_URL, true);
 	oReq.send();
}

/* callback that displays all layer info */
function createLayerTable(){
	
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
	var objects = document.querySelectorAll("tr.layerrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("layertable");
 	var layers = JSON.parse(this.responseText);
 	for (var i = 0; i < layers.length; i ++){
 		var type = layers[i].layerType;
 		var typeText = "unknown";
 		if(type == 1){
 			typeText = "Mapbox.com";
 		}else if(type == 2){
 			typeText = "GISCloud.com";
 		}
 		var active = "False";
 		if (layers[i].active){
 			active = "True";
 		}
 		var row = tableCreateRowTDs(parent,
 				[layers[i].layerName, typeText , active, layers[i].mapboxId, layers[i].wmsLayerList, null, null], 
 				"layerrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.id = "layerRow" + i;
 		row.dataset.uuid = layers[i].uuid;

 	    var scrollable = document.createElement("div");
 	    scrollable.className = "scrollable";
 	    scrollable.innerHTML = layers[i].token;
 		row.childNodes[5].appendChild(scrollable);

 	
 		//update goes first, shows second, since it floats right in the css...
 		var updateicon = document.createElement("a");
 		updateicon.className="update-icon";
 		updateicon.title="update layer";
 		updateicon.onclick = updateLayer;
 		updateicon.href="";
 		row.childNodes[6].appendChild(updateicon);

 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete layer";
 		deleteicon.onclick = deleteLayer;
 		deleteicon.href="";
 		row.childNodes[6].appendChild(deleteicon);
 	}
}

/* delete layer*/
function deleteLayer(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var ok = window.confirm("Are you sure you want to delete the layer?");
	if (!ok) return;
	
	hideInfo();
	hideError();
	
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
		displayInfo("Deleted Layer: " + r.layerName);
	} else {
		displayError(parseError("Error deleting Layer " + this.uuid));
	}
	refreshLayers();
	
}

function createNewLayer(){
	
	var layer_name = document.querySelector("input[name=layer_name]").value;
	var layer_mapbox_id = document.querySelector("input[name=layer_mapbox_id]").value;
	var layer_list = document.querySelector("input[name=layer_list]").value;
	var layer_token = document.querySelector("input[name=layer_token]").value;
	var layer_type = document.querySelector("select[name=layer_type]").value;
	var layer_status = document.querySelector("select[name=layer_status]").value;
	
	
	var jsonData = {
		"layerName" : layer_name,
		"wmsLayerList" : layer_list,
		"layerType" : layer_type,
		"token" : layer_token,
		"mapboxId" : layer_mapbox_id,
		"active" : layer_status
	};

	//make ajax call
	hideError();
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
		displayInfo("Layer created");
	} else {
		displayError("Error creating Layer;  " + this.responseText + "; " + this.statusText);
	}
	refreshLayers();
	closeDialog('layerDialog');
}

function updateLayer(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	document.getElementById("maplayersform").uuid.value = uuid;
	
	hideInfo();
	hideError();
	
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
		displayError(parseError("Error getting alert details for layer from server; layer uuid: " + this.uuid));
	}
	
	document.querySelector("#dialogerror").style.display = "none";
	
	var form = document.getElementById("maplayersform");
	
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
	
	displayDialog('layerDialog', 'main');
}


function submitUpdateLayer(){
	var uuid = document.getElementById("maplayersform").uuid.value;
	
	var layer_name = document.querySelector("input[name=layer_name]").value;
	var layer_mapbox_id = document.querySelector("input[name=layer_mapbox_id]").value;
	var layer_list = document.querySelector("input[name=layer_list]").value;
	var layer_token = document.querySelector("input[name=layer_token]").value;
	var layer_type = document.querySelector("select[name=layer_type]").value;
	var layer_status = document.querySelector("select[name=layer_status]").value;
	
	
	var jsonData = {
		"layerName" : layer_name,
		"wmsLayerList" : layer_list,
		"layerType" : layer_type,
		"token" : layer_token,
		"mapboxId" : layer_mapbox_id,
		"active" : layer_status
	};

	//make ajax call
	hideError();
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
		displayInfo("Layer Update");
	} else {
		displayError("Error creating Layer;  " + this.responseText + "; " + this.statusText);
	}
	refreshLayers();
	closeDialog('layerDialog');
}