var CAURL = "../api/conservationarea";
var DOWNLOADCA = false;

/* configure events on html elements */
window.onload = function(){
	//add new ca
	document.querySelector("#btnNewCa").onclick=clearAndShowNewCaDialog;
	
	//delete user
	elements = document.querySelectorAll(".deleteca");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=confirmdeleteca;
	}
	
	elements = document.querySelectorAll(".downloadca");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=downloadca;
	}
}

function clearAndShowNewCaDialog(){
 	document.querySelector("input[name=calabel]").value = "";
 	document.querySelector("input[name=newcauuid]").value = "";
 	document.querySelector("#newDialog > #dialogerror").style.display = "none";
 	displayDialog('newDialog', 'main');
}

function createca(){
	var caLabel = document.querySelector("input[name=calabel]").value;
 	var uuid = document.querySelector("input[name=newcauuid]").value;
 	closeDialog('newDialog');
	var geturl = CAURL + '?cauuid=' + encodeURIComponent(uuid) + '&name=' + encodeURIComponent(caLabel);
	var oReq = new XMLHttpRequest();
	oReq.onload = cacreated;
	oReq.open("POST", geturl, true);
	oReq.send();
	return false;
}

function cacreated(){
	if (this.status == 204) {
		displayInfo("Conservation area created.");
	} else if (this.status == 401){
		displayError(parseError(i18n("ca.errorcreatingca") + ". Unauthorized.", this.responseText));
	}else{
		displayError(parseError(i18n("ca.errorcreatingca"), this.responseText));
	}
	refreshCaList();
}


function cancelCaDownload(){
	DOWNLOADCA = false;
	closeDialog('downloadDialog')
	return false;
}
function downloadca(){
	DOWNLOADCA = true;
	displayDialog('downloadDialog', 'main');
	
	var cauuid = this.dataset.cauuid;
	var geturl = CAURL + '/' + encodeURIComponent(cauuid) + '?data=all';
	var oReq = new XMLHttpRequest();
	oReq.onload = updateDownloadProgress;
	oReq.cauuid=cauuid;
	oReq.open("GET", geturl, true);
	oReq.send();
	return false;
}

function updateDownloadProgress(){
	if (!DOWNLOADCA) return false;
	document.querySelector("#downloadDialog > #statusurl").innerHTML = this.getResponseHeader ("<url>");
	if (this.status == 202) {
		var location = this.getResponseHeader ("Location");
		document.querySelector("#downloadDialog > #dialogerror").style.display = "none";
		document.querySelector("#downloadDialog > #statusurl").innerHTML = location;

		var onReq = new XMLHttpRequest();
		onReq.onload = updateDownloadStatus;
		onReq.open("GET", location);
		onReq.send();
		
	} else {
		document.querySelector("#downloadDialog > #dialogerror").style.display = "block";
		document.querySelector("#downloadDialog > #dialogerror").innerHTML = "Error occurred.";	
	}	
}

function updateDownloadStatus(){
	if (!DOWNLOADCA) return false;
	if (this.status == 200){
		var statusobj = JSON.parse(this.responseText);
		if (statusobj.status == "COMPLETE"){
			var url = JSON.parse(statusobj.message).file_url;
			window.open(url, "_self");
			
			closeDialog('downloadDialog');
		}else{
			var url = this.responseURL;
			//wait 1 second then re-request
			setTimeout(function(){
				var onReq = new XMLHttpRequest();
				onReq.onload = updateDownloadStatus;
				onReq.open("GET", url);
				onReq.send();
			}, 1000);
			//wait and re-request status
			

		}
	}
	
}

function confirmdeleteca(){
	
	var cauuid = this.dataset.cauuid;
	var status = this.dataset.status;
	
	if (status == 'DATA' || status == 'UPLOADING'){
		document.querySelector("#deleteform > div#confirmtype").style.display = 'block';
		document.querySelector("#deleteform > * > input[name=caoption][value=desktop]").checked = true;
	}else{
		
		document.querySelector("#deleteform > div#confirmtype").style.display = 'none';
		document.querySelector("#deleteform > * > input[name=caoption][value=all]").checked = true;
	}
	var formUuidElement = document.querySelector("#deleteform > input[name=cauuid]");
	formUuidElement.setAttribute("value", cauuid);

	displayDialog('deleteDialog', 'main');

	return false;	
}

function deleteca(){

	document.querySelector("#deleteDialog > #dialogerror").style.display = "none";
	
	var cauuid = document.querySelector("#deleteform > input[name=cauuid]").value;
	var username = document.querySelector("#deleteform > input[name=username]").value;
	var password = document.querySelector("#deleteform > input[name=password]").value;
	
	document.querySelector("#deleteform > input[name=username]").value = "";
	document.querySelector("#deleteform > input[name=password]").value = "";
	
	var datavalue = document.querySelector('input[name="caoption"]:checked').value;
	var dataonly = "true";
	if (datavalue == "all"){
		dataonly = "false";
	}
	
	if (username.length == 0){
		document.querySelector("#deleteDialog > #dialogerror").style.display = "block";
		document.querySelector("#deleteDialog > #dialogerror").innerHTML = i18n("settings.usernamerequired") ;
		return false;
	}
	if (password.length == 0){
		document.querySelector("#deleteDialog > #dialogerror").style.display = "block";
		document.querySelector("#deleteDialog > #dialogerror").innerHTML = i18n("settings.passwordrequired");
		return false;
	}
	
	var ok = window.confirm(i18n("ca.confirmdeleteca") + cauuid + "?");
	if (!ok) return false;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = caDeleted;
	oReq.cauuid=cauuid;
	oReq.open("DELETE", CAURL + '/' +encodeURIComponent(cauuid) + "?username=" + encodeURIComponent(username) + "&password="+encodeURIComponent(password)+"&dataonly="+encodeURIComponent(dataonly), true);
	oReq.send();
	
	closeDialog('deleteDialog');
	return false;
}


function caDeleted(){
	if (this.status == 204) {
		displayInfo("Conservation area (" + this.cauuid + ") data deleted");
	} else if (this.status == 401){
		displayError(parseError(i18n("ca.errordeletingca") + this.cauuid + ". Unauthorized.", this.responseText));
	}else{
		displayError(parseError(i18n("ca.errordeletingca") + this.cauuid, this.responseText));
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
	row.innerHTML=i18n("ca.refreshingcas");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createCaTable;
 	oReq.open("Get", CAURL + '/', true);
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
 				[cas[i].label, cas[i].uuid, cas[i].status, cas[i].version, null, null], 
 				"carow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.cauuid = cas[i].uuid;
 		
 		if (cas[i].status == "DATA"){
	 		var downloadca = document.createElement("a");
	 		downloadca.className="downloadca download-icon";
	 		downloadca.title="downloadca";
	 		downloadca.dataset.cauuid = cas[i].uuid;
	 		downloadca.onclick = downloadca;
	 		downloadca.href="";
	 		row.childNodes[4].appendChild(downloadca);
 		}
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="deleteca delete-icon";
 		deleteicon.title="delete conservation area";
 		deleteicon.dataset.cauuid = cas[i].uuid;
 		deleteicon.dataset.status = cas[i].status;
 		deleteicon.onclick = confirmdeleteca;
 		deleteicon.href="";
 		row.childNodes[5].appendChild(deleteicon);
 	}
}