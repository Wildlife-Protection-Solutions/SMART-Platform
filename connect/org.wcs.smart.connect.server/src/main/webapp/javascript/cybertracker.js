var CTURL = "../api/cybertracker";
var CAURL = "../api/conservationarea"
	
window.onload = function(){
	menuCheckOnload();
	
	refreshPackageList();
	refreshApiKeyTable();
	
	document.querySelector("#refreshnow").onclick=function(){refreshPackageList(); return false;};
}

function resetApiKey(){
	hideInfo();
	var cauuid = this.dataset.cauuid;
	var oReq = new XMLHttpRequest();
 	oReq.onload = resetApiKeyRes;
 	oReq.calabel = this.dataset.label;
 	oReq.open("Delete", CTURL + "/apikey/" + cauuid, true);
 	oReq.send();
 	return false;
}

function resetApiKeyRes(){
	if (this.status != 200) {
		var msg = "Error resetting CyberTracker API Key: ";
		if (this.status == 401){
			msg += "Unauthorized";
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	var msg = this.calabel + " - API Key Reset.  All CyberTracker packages for this Conservation Area should be removed and regenerated.";
	displayInfo(msg);
}

function refreshApiKeyTable(){
	
	//clear current table
	var objects = document.querySelectorAll("div.ctapikeytable");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#ctapikeytable");
	var row = document.createElement("div");
	row.className="apirow";
	row.innerHTML="Loading Conservation Areas...";
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createApiKeyTable;
 	oReq.open("Get", CAURL + "?includeSpatialBoundaries=false", true);
 	oReq.send();
}

function createApiKeyTable(){
	if (this.status != 200) {
		var msg = "Error loading conservation areas";
		if (this.status == 401){
			msg += "Unauthorized";
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("div.apirow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#ctapikeytable");
 	var packages = JSON.parse(this.responseText);
 	
 	var cnt = 0;
 	for (var i = 0; i < packages.length; i ++){
 		var label = packages[i].label;
 		var uuid = packages[i].uuid;
 		if (uuid != "00000000-0000-0000-0000-000000000000"){
	 		var row = tableCreateRow(parent,[label,null], 
	 				"apirow " + (cnt % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
	 		cnt++;
	 		
		 	var resetbtn = document.createElement("button");
		 	resetbtn.innerHTML="Reset API Key";
		 	resetbtn.className= "block button";
		 	resetbtn.onclick = resetApiKey;
		 	resetbtn.dataset.cauuid = uuid;
		 	resetbtn.dataset.label = label;
		 	row.childNodes[1].appendChild(resetbtn);
 		}
 	}
}

function refreshPackageList(){
	
	//clear current table
	var objects = document.querySelectorAll("div.ctrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#ctpackagetable");
	var row = document.createElement("div");
	row.className="ctrow";
	row.innerHTML="Loading Packages...";
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createPackageTable;
 	oReq.open("Get", CTURL + "/packages", true);
 	oReq.send();
}


function createPackageTable(){
	if (this.status != 200) {
		var msg = "Error loading cybertracker packages: ";
		if (this.status == 401){
			msg += "Unauthorized";
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("div.ctrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#ctpackagetable");
 	var packages = JSON.parse(this.responseText);
 	
 	for (var i = 0; i < packages.length; i ++){
 		var version = packages[i].version;
 		var datepart = version.split('.')[1];
 		
 		var year = datepart.substring(0,4);
 		var month = datepart.substring(4,6);
 		var date = datepart.substring(6,8);
 		var hr = datepart.substring(8,10);
 		var min = datepart.substring(10,12);
 		var sec = datepart.substring(12,14);
 		var rDate = new Date(year, month-1, date, hr, min,sec,0);
 		
 		var upDate = new Date( Date.parse(packages[i].uploadedDate) );
 		
 		var row = tableCreateRow(parent, 
 				[packages[i].name, packages[i].caLabel, upDate.toLocaleString(), rDate.toLocaleString(), packages[i].version, null, null], 
 				"ctrow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.packageuuid = packages[i].uuid;
 		
 		
	 	var downloadca = document.createElement("a");
	 	downloadca.className="download-icon";
	 	downloadca.title="download package";
	 	downloadca.dataset.uuid = packages[i].uuid;
	 	downloadca.onclick = downloadPackage;
	 	downloadca.href="";
	 	row.childNodes[4].appendChild(downloadca);
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete package";
 		deleteicon.dataset.uuid = packages[i].uuid;
 		deleteicon.onclick = deletePackageValidate;
 		deleteicon.href="";
 		row.childNodes[5].appendChild(deleteicon);
 	}
}

function deletePackageValidate(){
	var uuid = this.dataset.uuid;
	
	var formUuidElement = document.querySelector("#deleteform > input[name=packageuuid]");
	formUuidElement.setAttribute("value", uuid);
	
	displayDialog('deleteDialog', 'main');
	
	return false;
}

function deletePackage(){
	closeDialog('deleteDialog');
	var uuid = document.querySelector("#deleteform > input[name=packageuuid]").value;
	var oReq = new XMLHttpRequest();
 	oReq.onload = packageDeleted;
 	oReq.open("Delete", CTURL + "/packages/" + uuid, true);
 	oReq.send();
 	return false;
}


function packageDeleted(){
	if (this.status != 204) {
		var msg = "Error deleting cybertracker package: ";
		if (this.status == 401){
			msg += "Unauthorized";
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	displayInfo("Package deleted.");
	refreshPackageList();
}

function downloadPackage(){
	var uuid = this.dataset.uuid;
	var url = CTURL + "/packages/" + uuid;
	window.open(url, "_self");
	return false;
}