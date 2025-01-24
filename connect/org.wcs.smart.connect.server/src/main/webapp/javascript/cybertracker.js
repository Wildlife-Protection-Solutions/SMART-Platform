var CTURL = "../api/cybertracker";
var CUSERURL = "../api/smartcollect";
var ANNC_URL = "../api/announcement"

var searchtimeout;

window.onload = function(){
	menuCheckOnload();
	
	refreshPackageList();
	refreshApiKeyTable();
	refreshNavigationList();
	getCollectUsers();
	loadAnnouncements();
	
	document.querySelector("#refreshnow").onclick=function(){refreshPackageList(); return false;};
	document.querySelector("#navrefreshnow").onclick=function(){refreshNavigationList(); return false;};
	
	document.querySelector("#refreshannouncements").onclick=function(){loadAnnouncements(); return false;};
	
	document.querySelector("#sm_packages").onclick=function(){showTab("sm_packages");}
	document.querySelector("#sm_routes").onclick=function(){showTab("sm_routes");}
	document.querySelector("#sm_keys").onclick=function(){showTab("sm_keys");}
	document.querySelector("#sm_collectusers").onclick=function(){showTab("sm_collectusers");}
	document.querySelector("#sm_announcements").onclick=function(){showTab("sm_announcements");}
	showTab("sm_packages");
	
	document.querySelector("#newannouncementform").onsubmit = createAnnouncement;

}

function confirmResetApi(){
	var cauuid = this.dataset.cauuid;
	var label = this.dataset.label;
	var type = this.dataset.type;
	
	var formUuidElement = document.querySelector("#resetapiform > input[name=cauuid]");
	formUuidElement.setAttribute("value", cauuid);
	var formUuidElement = document.querySelector("#resetapiform > input[name=label]");
	formUuidElement.setAttribute("value", label);
	var formUuidElement = document.querySelector("#resetapiform > input[name=type]");
	formUuidElement.setAttribute("value", type);
	
	displayDialog('resetApiDialog', 'main');
	return false;	
}

function resetApiKey(){
	
	closeDialog('resetApiDialog', 'main');
	
	hideInfo();
	var cauuid = document.querySelector("#resetapiform > input[name=cauuid]").value;
	var label = document.querySelector("#resetapiform > input[name=label]").value;
	var type = document.querySelector("#resetapiform > input[name=type]").value;

	var oReq = new XMLHttpRequest();
 	oReq.onload = resetApiKeyRes;
 	oReq.calabel = label;
 	oReq.open("Delete", CTURL + "/apikey/" + cauuid + "?type=" + type, true);
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
 	
 		var row = tableCreateRow(parent,[label,null, null], 
	 			"apirow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
	 		
		var resetbtn = document.createElement("button");
		resetbtn.innerHTML=i18n("cybertracker.resetbtn");
		resetbtn.className= "block button";
		resetbtn.onclick = confirmResetApi;
		resetbtn.dataset.cauuid = uuid;
		resetbtn.dataset.label = label;
		resetbtn.dataset.type = "PRIVATE";
		row.childNodes[1].appendChild(resetbtn);
		
		var resetbtn = document.createElement("button");
		resetbtn.innerHTML=i18n("cybertracker.resetbtn");
		resetbtn.className= "block button";
		resetbtn.onclick = confirmResetApi;
		resetbtn.dataset.cauuid = uuid;
		resetbtn.dataset.label = label;
		resetbtn.dataset.type = "PUBLIC";
		row.childNodes[2].appendChild(resetbtn);
 		
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
 		
 		var linkhtml = "<p style=\"width:300px; overflow-wrap: break-word;\">" + packages[i].appLink + "</p>";
 		
 		var row = tableCreateRow(parent, 
 				[packages[i].name, packages[i].caLabel, packages[i].type,
 				 packages[i].isPrivate ? "PRIVATE" : "PUBLIC",
 				upDate.toLocaleString(), rDate.toLocaleString(),
 				null,
 				linkhtml, 
 				packages[i].version, null, null], 
 				"ctrow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.packageuuid = packages[i].uuid;
 		
 		var qrelement = document.createElement("div");
 		qrelement.id = "qr-" + packages[i].uuid;
	 	row.childNodes[6].appendChild(qrelement);
 		
 		new QRCode(qrelement, {text: packages[i].appLink, width:128, height:128});
 		
	 	var downloadca = document.createElement("i");
	 	downloadca.className="fa-solid fa-xl fa-download icon-btn-default";
	 	downloadca.title="download package";
	 	downloadca.dataset.uuid = packages[i].uuid;
	 	downloadca.onclick = downloadPackage;
	 	row.childNodes[9].appendChild(downloadca);
 		
 		var deleteicon = document.createElement("i");
 		deleteicon.className="fa-solid fa-xl fa-xmark icon-btn-default";
 		deleteicon.title="delete package";
 		deleteicon.dataset.uuid = packages[i].uuid;
 		deleteicon.onclick = deletePackageValidate;
 		row.childNodes[10].appendChild(deleteicon);
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
 		
 		
	 	var downloadca = document.createElement("i");
	 	downloadca.className="fa-solid fa-xl fa-download icon-btn-default";
	 	downloadca.title="download package";
	 	downloadca.dataset.uuid = packages[i].uuid;
	 	downloadca.onclick = downloadNavigation;
	 	row.childNodes[3].appendChild(downloadca);
 		
 		var deleteicon = document.createElement("i");
 		deleteicon.className="fa-solid fa-xl fa-xmark icon-btn-default";
 		deleteicon.title="delete package";
 		deleteicon.dataset.uuid = packages[i].uuid;
 		deleteicon.onclick = deleteNavigationValidate;
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

function searchCollectUsers(){
	clearTimeout(searchtimeout);
	setTimeout(function(){
		getCollectUsers();	
	}, 600);
}



function getCollectUsers(){
	//clear current table
	var parent = document.getElementById('collectusertable');

 	//clear current table
	var objects = document.querySelectorAll("div.ctuserrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var row = document.createElement("div");
	row.className="ctuserrow";
	row.innerHTML=i18n("cybertracker.searchingusers");
	parent.appendChild(row);
	
	var searchtext = document.getElementById('collectusersearch').value;
	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = collectUserCallback;
 	oReq.open("Get", CUSERURL + "/source?search=" + searchtext, true);
 	oReq.send();
}

function collectUserCallback(){
	if (this.status != 200) {
		var msg = i18n("cybertracker.userserror");
		if (this.status == 401){
			msg += i18n("cybertracker.usersnotauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	
	var parent = document.getElementById('collectusertable');

 	//clear current table
	var objects = document.querySelectorAll("div.ctuserrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	var drawnRowCount = 0;
	
	var users = JSON.parse(this.responseText);
	
 	for (var i = 0; i < users.length; i ++){
 		drawnRowCount++;
 		var row = tableCreateRow(parent, 
 				[users[i].source, users[i].deviceId, users[i].state, null], 
 				"ctuserrow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));

 					
 		row.dataset.sourceuuid = users[i].uuid;
 		
 		var deleteicon = document.createElement("i");
 		deleteicon.className="fa-solid fa-xl fa-xmark icon-btn-default icon-btn-spacing";
 		deleteicon.title= "delete user";
 		deleteicon.onclick = deleteCollectUserValidate;
		row.childNodes[3].appendChild(deleteicon);

		var sendvalidateicon = document.createElement("i");
		sendvalidateicon.className="fa-solid fa-xl fa-envelope icon-btn-default icon-btn-spacing";
		sendvalidateicon.title= "send validation request";
		sendvalidateicon.onclick = sendValidationRequest;
		row.childNodes[3].appendChild(sendvalidateicon);
		
		var blacklisticon = document.createElement("i");
		blacklisticon.className="fa-solid fa-xl fa-user-xmark icon-btn-default icon-btn-spacing";
		blacklisticon.title= "blacklist user";
		blacklisticon.onclick = blacklistUser;
		row.childNodes[3].appendChild(blacklisticon);

		var validateicon = document.createElement("i");
		validateicon.className="fa-solid fa-xl fa-user-check icon-btn-default icon-btn-spacing";
		validateicon.title= "validate user";
		validateicon.onclick = validateUser;
		row.childNodes[3].appendChild(validateicon);
 	}
 	
 	if(users.length == 0 || drawnRowCount == 0){ //no results or they were all filtered out
 		var row = document.createElement("div");
 		row.className = "ctuserrow errorsection";
 	    row.innerHTML = i18n("cybertracker.nousers");
 	    row.style.display = "block";
 		parent.appendChild(row);
 	}
}

function sendValidationRequest(){
	var uuid = this.parentElement.parentElement.getAttribute('data-sourceuuid');
	updateState(uuid, "VALIDATED", true);
}

function validateUser(){
	var uuid = this.parentElement.parentElement.getAttribute('data-sourceuuid');
	updateState(uuid, "VALIDATED", false);
}


function blacklistUser(){
	var uuid = this.parentElement.parentElement.getAttribute('data-sourceuuid');
	updateState(uuid, "BLACKLISTED", false);
}

function updateState(uuid, newstate, sendvalidation){	
	var oReq = new XMLHttpRequest();
	oReq.onload = function(){
		if (this.status != 204) {
			alert(i18n("cybertracker.userserror") + JSON.parse(this.responseText).error);
			return;
		}
		getCollectUsers()
	};
	var request = CUSERURL + "/source/" + uuid + "?state=" + newstate;
	if (sendvalidation){
		request = request+"&sendvalidation=true";
	}
	oReq.open("PUT", request, true);
	oReq.send();
}



function deleteCollectUserValidate(){
	var uuid = this.parentElement.parentElement.getAttribute('data-sourceuuid');
	var formUuidElement = document.querySelector("#deletecollectform > input[name=uuid]");
	formUuidElement.setAttribute("value", uuid);
	
	displayDialog('deleteCollectUserDialog', 'main');
	return false;
}

function deleteCollectUser(){
	closeDialog('deleteCollectUserDialog');
	var uuid = document.querySelector("#deletecollectform > input[name=uuid]").value;
	var oReq = new XMLHttpRequest();
 	oReq.onload = collectUserDeleted;
 	oReq.open("Delete", CUSERURL + "/source/" + uuid, true);
 	oReq.send();
 	return false;
}


function collectUserDeleted(){
	if (this.status != 204) {
		if (this.status == 401){
			alert(i18n("cybertracker.unauthorized"));
		}
		var msg= i18n("cybertracker.userserror");
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		alert(msg);
		return;
	}
	getCollectUsers();
}


function deleteAnnouncement(){
	var auuid = this.parentElement.parentElement.getAttribute('data-sourceuuid');
	
	displayConfirmDialog("Delete Announcement",  "Are you sure you want to delete the announcement?"  , function(){
		var oReq = new XMLHttpRequest();
			oReq.onload = loadAnnouncements;
			oReq.open("DELETE", ANNC_URL + "/" + auuid, true);
			oReq.setRequestHeader("Content-type", "application/json");
			oReq.send();	
			return false;	
	});
}

function startEditAnnouncement(){
	
	var auuid = this.parentElement.parentElement.getAttribute('data-sourceuuid');
		
	var oReq = new XMLHttpRequest();
	oReq.open("GET", ANNC_URL + "/" + auuid, true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.onload = editAnnouncement;
	oReq.send();	
	return false;	
}

function editAnnouncement(){

	if (this.status != 200) {
		var msg = "error loading announcements"
		if (this.status == 401){
			msg += i18n("cybertracker.usersnotauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	
	var item = JSON.parse(this.responseText);
	
	document.querySelector("select[name=announcement_ca]").value = item.caUuid;
	document.querySelector("textarea[name=announcement_message]").value = item.message;
	
	var init = new Date(item.expiresOn);
	init.setMinutes(init.getMinutes() - (new Date()).getTimezoneOffset());
	document.querySelector("input[id=announcement_expiry]").value = init.toISOString().slice(0,16);
	
	
	document.querySelector("input[id=announcement_uuid]").value = item.uuid;
	
	document.getElementById('announcementerror').innerHTML = "";
	document.getElementById('announcementerror').style.display="none";
	
	document.getElementById('announcementdialogtitle').innerHTML = "Update Announcement";
	
	displayDialog('createAnnouncementDialog', 'main');

}

function showCreateAnnouncementDialog(){
	
	let x = document.querySelector("input[id=announcement_expiry]").value
	if (!x || x ==""){
		var init = (new Date());
		init = new Date(init.setMonth(init.getMonth() + 3));
		init.setMinutes(init.getMinutes() - init.getTimezoneOffset());
		document.querySelector("input[id=announcement_expiry]").value = init.toISOString().slice(0,16);
	}
	
	document.querySelector("textarea[name=announcement_message]").value = "";
	document.getElementById('announcementerror').innerHTML = "";
	document.getElementById('announcementerror').style.display="none";
	document.querySelector("input[id=announcement_uuid]").value = null;
	document.getElementById('announcementdialogtitle').innerHTML = "Create New Announcement";
	displayDialog('createAnnouncementDialog', 'main');
}



function createAnnouncement(){
	var auuid = document.querySelector("input[id=announcement_uuid]").value;

	var cauuid = document.querySelector("select[name=announcement_ca]").value;
	var desc = document.querySelector("textarea[name=announcement_message]").value;
	var expiry = document.querySelector("input[id=announcement_expiry]").value;
	
	var offset = (new Date()).getTimezoneOffset() * -1;
	var hroffset = offset / 60;
	var minoffset = offset - (hroffset * 60);
	if (minoffset < 10) minoffset = "0" + minoffset;
	
	if (hroffset < 10 && hroffset >= 0){
		hroffset = "+0" + hroffset;
	} else if (hroffset >= 0){
		hroffset = "+" + hroffset;
	}else if (hroffset > -10 && hroffset < 0){
		hroffset = "-0" + (hroffset * -1);
	} else if (hroffset < -10){
		hroffset = hroffset;
	}
	
	expiry = expiry + ":00" + hroffset + ":" + minoffset;
	
	var json = { "caUuid": cauuid, "message": desc, "expiresOn": expiry };
	
	
	if (!auuid){
		//new
		var oReq = new XMLHttpRequest();
		oReq.onload = announcementCreated;
		oReq.open("POST", ANNC_URL, true);
		oReq.setRequestHeader("Content-type", "application/json");
		oReq.send(JSON.stringify(json));
	}else{
		//update
		var oReq = new XMLHttpRequest();
		oReq.onload = announcementCreated;
		oReq.open("PUT", ANNC_URL + "/" + auuid, true);
		oReq.setRequestHeader("Content-type", "application/json");
		oReq.send(JSON.stringify(json));
	}
	
	return false;
}

function announcementCreated(){
	console.log("created");
	console.log(this);
	if (this.status != 200) {
		var msg = "error: "
		if (this.status == 401){
			msg += i18n("cybertracker.usersnotauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		
		document.getElementById('announcementerror').innerHTML = msg;
		document.getElementById('announcementerror').style.display="block";
		
		return;
	}
	closeDialog('createAnnouncementDialog');
	loadAnnouncements();
}

function loadAnnouncements(){
	//clear current table
	var parent = document.getElementById('announcementstable');

 	//clear current table
	var objects = document.querySelectorAll("div.arow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var row = document.createElement("div");
	row.className="arow";
	row.innerHTML=i18n("cybertracker.loadingpackagesmsg");
	parent.appendChild(row);
	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = loadAnnouncementsCallback;
 	oReq.open("Get", ANNC_URL, true);
 	oReq.send();
}

function loadAnnouncementsCallback(){
	if (this.status != 200) {
		var msg = "error loading announcements"
		if (this.status == 401){
			msg += i18n("cybertracker.usersnotauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	
	var parent = document.getElementById('announcementstable');

 	//clear current table
	var objects = document.querySelectorAll("div.arow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	var drawnRowCount = 0;
	
	var items = JSON.parse(this.responseText);
	items.sort((a,b)=>{
		var x = a.caName.localeCompare(b.caName);
		if (x != 0) return x;
		
		return (new Date(a.expiresOn)).getTime() - (new Date(b.expiresOn)).getTime();
	});
	
 	for (var i = 0; i < items.length; i ++){
 		drawnRowCount++;
		
		var message = items[i].message;
		message = message.replaceAll("\n", "<br>");
		
 		var row = tableCreateRow(parent, 
 				[items[i].caName, message, formatDate(new Date(items[i].createdOn)), items[i].expiresOn ? formatDate(new Date(items[i].expiresOn)) : "", null], 
 				"arow " + (drawnRowCount % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));

 					
 		row.dataset.sourceuuid = items[i].uuid;
 		
		row.childNodes[0].style.whiteSpace = 'nowrap';
		row.childNodes[2].style.whiteSpace = 'nowrap';
		row.childNodes[3].style.whiteSpace = 'nowrap';
		row.childNodes[4].style.whiteSpace = 'nowrap';
		
		row.childNodes[0].style.verticalAlign = 'top';
		row.childNodes[2].style.verticalAlign = 'top';
		row.childNodes[3].style.verticalAlign = 'top';
		row.childNodes[4].style.verticalAlign = 'top';
		
		if (items[i].expiresOn && new Date(items[i].expiresOn) < new Date()){
			var warnicon= document.createElement("i");
			warnicon.className="fa-solid fa-xl fa-triangle-exclamation icon-btn-spacing";
			warnicon.style.color="#F1BA07";
			warnicon.title = "notification has expired";
			row.childNodes[3].appendChild(warnicon);
		}
		
 		var deleteicon = document.createElement("i");
 		deleteicon.className="fa-solid fa-xl fa-xmark icon-btn-default icon-btn-spacing";
 		deleteicon.title= "delete announcement";
 		deleteicon.onclick = deleteAnnouncement;
		row.childNodes[4].appendChild(deleteicon);
		
		var editicon = document.createElement("i");
		editicon.className="fa-solid fa-xl fa-pen icon-btn-default icon-btn-spacing";
		editicon.title= "edit announcement user";
		editicon.onclick = startEditAnnouncement;
		row.childNodes[4].appendChild(editicon);

 	}
}
