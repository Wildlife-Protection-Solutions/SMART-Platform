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
	document.querySelector("#cancelNewStyle").onclick = function(){
		closeDialog('newStyleDialog');
	};
	
	document.querySelector("#newstyleform").onsubmit = createNewStyle;
	
	//Layer table and actions
	refreshLayers();
	setTableActions();
}

function setTableActions(){
	//delete layer clicked
	elements = document.querySelectorAll(".deletelayer");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=deletelayer;
	}
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
 				[layers[i].layerName, typeText , active, layers[i].mapboxId, layers[i].wmsLayerList, null], 
 				"layerrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.id = "layerRow" + i;
 		row.dataset.uuid = layers[i].uuid;
 	
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete layer";
 		deleteicon.onclick = deleteLayer;
 		deleteicon.href="";
 		row.childNodes[5].appendChild(deleteicon);
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
		displayInfo("Deleted Layer with UUID: " + r.uuid);
	} else {
		displayError(parseError("Error deleting Layer " + this.uuid));
	}
	refreshLayers();
	
}


