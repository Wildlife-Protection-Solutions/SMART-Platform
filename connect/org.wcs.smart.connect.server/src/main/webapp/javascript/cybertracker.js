var CTURL = "../api/cybertracker";
	
window.onload = function(){
	menuCheckOnload();
	
	refreshPackageList();
	refreshApiKeyTable();
	refreshNavigationList();
	
	document.querySelector("#refreshnow").onclick=function(){refreshPackageList(); return false;};
	document.querySelector("#navrefreshnow").onclick=function(){refreshNavigationList(); return false;};
}

function confirmResetApi(){
	var cauuid = this.dataset.cauuid;
	var label = this.dataset.label;
	var formUuidElement = document.querySelector("#resetapiform > input[name=cauuid]");
	formUuidElement.setAttribute("value", cauuid);
	var formUuidElement = document.querySelector("#resetapiform > input[name=label]");
	formUuidElement.setAttribute("value", label);
	
	displayDialog('resetApiDialog', 'main');
	return false;	
}

function resetApiKey(){
	
	closeDialog('resetApiDialog', 'main');
	
	hideInfo();
	var cauuid = document.querySelector("#resetapiform > input[name=cauuid]").value;
	var label = document.querySelector("#resetapiform > input[name=label]").value;

	var oReq = new XMLHttpRequest();
 	oReq.onload = resetApiKeyRes;
 	oReq.calabel = label;
 	oReq.open("Delete", CTURL + "/apikey/" + cauuid, true);
 	oReq.send();
 	return false;
}

function resetApiKeyRes(){
	if (this.status != 200) {
		var msg = i18n("cybertracker.reseterror");
		if (this.status == 401){
			msg += i18n("cybertracker.unauthorized");
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	var msg = this.calabel + " - " + i18n("cybertracker.resetmsg");
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
	row.innerHTML=i18n("cybertracker.loadingkeysmsg");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createApiKeyTable;
 	oReq.open("Get", CTURL + "/apikey/", true);
 	oReq.send();
}

function createApiKeyTable(){
	if (this.status != 200) {
		var msg = i18n("cybertracker.loadingkeyserror");
		if (this.status == 401){
			msg += i18n("cybertracker.unauthorized");
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
 	
 		var row = tableCreateRow(parent,[label,null], 
	 			"apirow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
	 		
		var resetbtn = document.createElement("button");
		resetbtn.innerHTML=i18n("cybertracker.resetbtn");
		resetbtn.className= "block button";
		resetbtn.onclick = confirmResetApi;
		resetbtn.dataset.cauuid = uuid;
		resetbtn.dataset.label = label;
		row.childNodes[1].appendChild(resetbtn);
 		
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
	row.innerHTML=i18n("cybertracker.loadingpackagesmsg");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createPackageTable;
 	oReq.open("Get", CTURL + "/packages", true);
 	oReq.send();
}


function createPackageTable(){
	if (this.status != 200) {
		var msg = i18n("cybertracker.loadingpackageserror");
		if (this.status == 401){
			msg += i18n("cybertracker.unauthorized");
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
 				[packages[i].name, packages[i].caLabel, packages[i].type, upDate.toLocaleString(), rDate.toLocaleString(), packages[i].version, null, null], 
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
		var msg = i18n("cybertracker.packagedeleteerror");
		if (this.status == 401){
			msg += i18n("cybertracker.unauthorized");
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	displayInfo(i18n("cybertracker.packagedeletemsg"));
	refreshPackageList();
}

function downloadPackage(){
	var uuid = this.dataset.uuid;
	var url = CTURL + "/packages/" + uuid;
	window.open(url, "_self");
	return false;
}



function refreshNavigationList(){
	
	//clear current table
	var objects = document.querySelectorAll("div.navrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#navlayertable");
	var row = document.createElement("div");
	row.className="navrow";
	row.innerHTML=i18n("cybertracker.loadingpackagesmsg");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createNavigationTable;
 	oReq.open("Get", CTURL + "/navigationlayers", true);
 	oReq.send();
}


function createNavigationTable(){
	if (this.status != 200) {
		var msg = i18n("cybertracker.loadingpackageserror");
		if (this.status == 401){
			msg += i18n("cybertracker.unauthorized");
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("div.navrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#navlayertable");
 	var packages = JSON.parse(this.responseText);
 	
 	for (var i = 0; i < packages.length; i ++){
 		var upDate = new Date( Date.parse(packages[i].uploadedDate) );
 		
 		var row = tableCreateRow(parent, 
 				[packages[i].name, packages[i].caLabel, upDate.toLocaleString(),  null, null], 
 				"navrow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.packageuuid = packages[i].uuid;
 		
 		
	 	var downloadca = document.createElement("a");
	 	downloadca.className="download-icon";
	 	downloadca.title="download package";
	 	downloadca.dataset.uuid = packages[i].uuid;
	 	downloadca.onclick = downloadNavigation;
	 	downloadca.href="";
	 	row.childNodes[3].appendChild(downloadca);
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete package";
 		deleteicon.dataset.uuid = packages[i].uuid;
 		deleteicon.onclick = deleteNavigationValidate;
 		deleteicon.href="";
 		row.childNodes[4].appendChild(deleteicon);
 	}
}

function deleteNavigationValidate(){
	var uuid = this.dataset.uuid;
	
	var formUuidElement = document.querySelector("#deletenavform > input[name=navuuid]");
	formUuidElement.setAttribute("value", uuid);
	
	displayDialog('deleteNavDialog', 'main');
	
	return false;
}

function deleteNavigation(){
	closeDialog('deleteNavDialog');
	var uuid = document.querySelector("#deletenavform > input[name=navuuid]").value;
	var oReq = new XMLHttpRequest();
 	oReq.onload = navigationDeleted;
 	oReq.open("Delete", CTURL + "/navigationlayers/" + uuid, true);
 	oReq.send();
 	return false;
}


function navigationDeleted(){
	if (this.status != 204) {
		var msg = i18n("cybertracker.packagedeleteerror");
		if (this.status == 401){
			msg += i18n("cybertracker.unauthorized");
		}
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	displayInfo(i18n("cybertracker.packagedeletemsg"));
	refreshNavigationList();
}

function downloadNavigation(){
	var uuid = this.dataset.uuid;
	var url = CTURL + "/navigationlayers/" + uuid;
	window.open(url, "_self");
	return false;
}