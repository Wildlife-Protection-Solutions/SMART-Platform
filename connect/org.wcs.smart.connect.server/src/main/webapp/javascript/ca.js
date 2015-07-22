var CAURL = "../api/conservationarea/";

/* configure events on html elements */
window.onload = function(){
	//delete user
	elements = document.querySelectorAll(".deleteca");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=deleteca;
	}
}

function deleteca(){
	var cauuid = this.dataset.cauuid;
	var ok = window.confirm("Are you sure you want to the conservation area " + cauuid + "?");
	if (!ok) return;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = caDeleted;
	oReq.cauuid=cauuid;
	oReq.open("DELETE", CAURL + encodeURIComponent(cauuid), true);
	oReq.send();
	return false;	
	
}

function caDeleted(){
	if (this.status == 204) {
		displayInfo(this.cauuid + " deleted");
	} else {
		displayError(parseError("Error deleting conservation area " + this.cauuid, this.responseText));
	}
	refreshCaList();
}

function refreshCaList(){
	
	//clear current table
	var objects = document.querySelectorAll("div.carow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("div.catable");
	var row = document.createElement("div");
	row.className="carow";
	row.innerHTML="Refreshing Conservation Area Table...";
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createCaTable;
 	oReq.open("Get", CAURL, true);
 	oReq.send();
}

function createCaTable(){
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
	//clear current table
	var objects = document.querySelectorAll("div.carow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("div.catable");
 	var cas = JSON.parse(this.responseText);
 	for (var i = 0; i < cas.length; i ++){
 		var row = tableCreateRow(parent, 
 				[cas[i].label, cas[i].uuid, cas[i].status, cas[i].version, null], 
 				"carow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.cauuid = cas[i].uuid;
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete conservation area";
 		deleteicon.dataset.cauuid = cas[i].uuid;
 		deleteicon.onclick = deleteca;
 		deleteicon.href="";
 		row.childNodes[4].appendChild(deleteicon);
 	}
}