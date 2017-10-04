var CAURL = "../api/conservationarea";
var DATA_CAURL = "../api/conservationarea/withdataonly"
var DOWNLOADCA = false;

/* configure events on html elements */
window.onload = function(){
	
	document.querySelector("#calist").onclick=function(){showTab("calist");}
	document.querySelector("#dmmanager").onclick=function(){showTab("dmmanager");}
	showTab("calist");
	
	//add new ca
	var newbtn =document.querySelector("#btnNewCa");
	if (newbtn != null){
		newbtn.onclick=clearAndShowNewCaDialog;
	}
	
	//delete user
	elements = document.querySelectorAll(".deleteca");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=confirmdeleteca;
	}
	
	elements = document.querySelectorAll(".downloadca");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=downloadca;
	}
	
	//info
	elements = document.querySelectorAll("#infoca");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=showcainfo;
	}
	
	document.querySelector("#selectNoneDmCa").onclick=function(){selectDmCa(false); return false;};
	document.querySelector("#selectAllDmCa").onclick=function(){selectDmCa(true); return false;};
	
	document.querySelector("#btnMergeDm").onclick=function(){submitDmUpdate(); return false;};
	
	refreshDmCaList();
}

function selectDmCa(state){
	var elements = document.querySelectorAll("#dm_calist > label > input");
	for (var i = 0; i < elements.length; i ++){
		elements[i].checked = state;
	}
}


function showTab(name){
	var tab = "#"+name;
	
	var tabElement = document.querySelector(tab);
	
	var allTabs = tabElement.parentElement.querySelectorAll(".tab");
	for( var i=0; i < allTabs.length; i++ ) {
		if (allTabs[i].parentElement == tabElement.parentElement){
			allTabs[i].classList.remove("tabselected");
			document.querySelector("#" + allTabs[i].id+"_body").style.display="none";
		}
	 }
	
	var tabBody = "#"+name + "_body";
	tabElement.classList.add("tabselected");
	document.querySelector(tabBody).style.display="block";
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
		displayError(parseError(i18n("ca.errorcreatingca") + ". " + i18n("ca.unauthorized"), this.responseText));
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

function showcainfo(){
	
	var loadinglabel = i18n("ca.loadinglabel");
	
	document.querySelector("#infolabel").innerHTML = loadinglabel;
	document.querySelector("#infostatus").innerHTML = loadinglabel;
	document.querySelector("#infouuid").innerHTML = loadinglabel;
	document.querySelector("#infodescription").innerHTML = loadinglabel;
	document.querySelector("#infodesignation").innerHTML = loadinglabel;
	document.querySelector("#infoversion").innerHTML = loadinglabel;
	document.querySelector("#inforevision").innerHTML = loadinglabel;
	document.querySelector("#infoorganization").innerHTML = loadinglabel;
	document.querySelector("#infopointofcontact").innerHTML = loadinglabel;
	document.querySelector("#infolocation").innerHTML = loadinglabel;
	document.querySelector("#infoowner").innerHTML = loadinglabel;
	
	displayDialog('caInfoDialog', 'main');
	
	var cauuid = this.dataset.cauuid;
	var geturl = CAURL + '/' + encodeURIComponent(cauuid);
	var oReq = new XMLHttpRequest();
	oReq.onload = showcainfodialog;
	oReq.cauuid = cauuid;
	oReq.open("GET", geturl, true);
	oReq.send();
	return false;
	
}
function showcainfodialog(){
	if (this.status != 200) {
		var msg = i18n("ca.error") + ": ";
		if (this.status == 401){
			msg += i18n("ca.unauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	
	var ca = JSON.parse(this.responseText)
	document.querySelector("#infolabel").innerHTML = ca.label;
	document.querySelector("#infostatus").innerHTML = ca.status;
	document.querySelector("#infouuid").innerHTML = ca.uuid;
	document.querySelector("#infodescription").innerHTML = ca.description == null ? "" : ca.description;
	document.querySelector("#infodesignation").innerHTML = ca.designation == null ? "" : ca.designation;
	document.querySelector("#infoversion").innerHTML = ca.version;
	document.querySelector("#inforevision").innerHTML = ca.revision;
	document.querySelector("#infoorganization").innerHTML = ca.organization;
	document.querySelector("#infopointofcontact").innerHTML = ca.pointOfContact;
	document.querySelector("#infolocation").innerHTML = ca.location;
	document.querySelector("#infoowner").innerHTML = ca.owner;
	
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
		document.querySelector("#downloadDialog > #downloadinfomsg").style.display = "block";
		var onReq = new XMLHttpRequest();
		onReq.onload = updateDownloadStatus;
		onReq.open("GET", location);
		onReq.send();
	} else if (this.status = 401) {
		document.querySelector("#downloadDialog > #dialogerror").style.display = "block";
		document.querySelector("#downloadDialog > #dialogerror").innerHTML = i18n("ca.unauthorized");
		document.querySelector("#downloadDialog > #downloadinfomsg").style.display = "none";
	} else {
		document.querySelector("#downloadDialog > #dialogerror").style.display = "block";
		document.querySelector("#downloadDialog > #dialogerror").innerHTML = i18n("ca.erroroccurred");
		document.querySelector("#downloadDialog > #downloadinfomsg").style.display = "none";
		
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
	var label = this.dataset.label;
	var version = this.dataset.version;
	
	if (status == 'DATA' || status == 'UPLOADING'){
		document.querySelector("#deleteform > div#confirmtype").style.display = 'block';
		document.querySelector("#deleteform > * > input[name=caoption][value=desktop]").checked = true;
	}else{
		
		document.querySelector("#deleteform > div#confirmtype").style.display = 'none';
		document.querySelector("#deleteform > * > input[name=caoption][value=all]").checked = true;
	}
	var formUuidElement = document.querySelector("#deleteform > input[name=cauuid]");
	formUuidElement.setAttribute("value", cauuid);
	var formUuidElement = document.querySelector("#deleteform > input[name=label]");
	formUuidElement.setAttribute("value", label);
	var versionElement = document.querySelector("#deleteform > input[name=version]");
	versionElement.setAttribute("value", version);
	
	displayDialog('deleteDialog', 'main');

	return false;	
}

function deleteca(){

	document.querySelector("#deleteDialog > #dialogerror").style.display = "none";
	
	var cauuid = document.querySelector("#deleteform > input[name=cauuid]").value;
	var label = document.querySelector("#deleteform > input[name=label]").value;
	var username = document.querySelector("#deleteform > input[name=username]").value;
	var password = document.querySelector("#deleteform > input[name=password]").value;
	var version = document.querySelector("#deleteform > input[name=version]").value;
	
	document.querySelector("#deleteform > input[name=username]").value = "";
	document.querySelector("#deleteform > input[name=password]").value = "";
	document.querySelector("#deleteform > input[name=version]").value = "";
	
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
	
	closeDialog('deleteDialog');	
	
	displayConfirmDialog(label, i18n("ca.confirmdeleteca") + label + "?", function(){
		hideInfo();
		var oReq = new XMLHttpRequest();
		oReq.onload = caDeleted;
		oReq.cauuid=cauuid;
		oReq.calabel=label;
		oReq.open("DELETE", CAURL + '/' +encodeURIComponent(cauuid) + "?username=" + encodeURIComponent(username) + "&password="+encodeURIComponent(password)+"&dataonly="+encodeURIComponent(dataonly) + "&version="+encodeURIComponent(version), true);
		oReq.send();
	});
	
	return false;
}


function caDeleted(){
	if (this.status == 204) {
		displayInfo(i18n("ca.cadeleted") + "(" + this.calabel + ")");
	} else if (this.status == 401){
		displayError(parseError(i18n("ca.errordeletingca") + this.calabel + ". Unauthorized.", this.responseText));
	}else{
		displayError(parseError(i18n("ca.errordeletingca") + this.calabel, this.responseText));
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
 	oReq.open("Get", CAURL + '/?includeSpatialBoundaries=false', true);
 	oReq.send();
 	
 	refreshDmCaList();
}

function refreshDmCaList(){
 	//refresh table in data model editor page
 	var dmcalist = document.getElementById("dm_calist");
 	if (dmcalist == null) return;
 	while(dmcalist.hasChildNodes()){
 		dmcalist.removeChild(dmcalist.lastChild);
 	}
 	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createDmCaTable;
 	oReq.open("Get", DATA_CAURL + '/?permission=admin,caadmin,updateca', true);
 	oReq.send();
}

function createCaTable(){
	if (this.status != 200) {
		var msg = i18n("ca.error") + ": ";
		if (this.status == 401){
			msg += i18n("ca.unauthorized");
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
 				[cas[i].label, cas[i].status, null, null, null], 
 				"carow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.cauuid = cas[i].uuid;
 		
 		var infoicon = document.createElement("a");
 		infoicon.className="info-icon";
 		infoicon.title="details...";
 		infoicon.dataset.cauuid = cas[i].uuid;
 		infoicon.onclick = showcainfo;
 		infoicon.href="";
 		row.childNodes[2].appendChild(infoicon);
 		
 		if (cas[i].status == "DATA"){
	 		var downloadca = document.createElement("a");
	 		downloadca.className="downloadca download-icon";
	 		downloadca.title="downloadca";
	 		downloadca.dataset.cauuid = cas[i].uuid;
	 		downloadca.onclick = downloadca;
	 		downloadca.href="";
	 		row.childNodes[3].appendChild(downloadca);
 		}
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="deleteca delete-icon";
 		deleteicon.title="delete conservation area";
 		deleteicon.dataset.cauuid = cas[i].uuid;
 		deleteicon.dataset.status = cas[i].status;
 		deleteicon.dataset.label = cas[i].label;
 		if(cas[i].version == null){
 			var version="";
 		}else{
 			var version=cas[i].version;
 		}
 		deleteicon.dataset.version = version;
 		deleteicon.onclick = confirmdeleteca;
 		deleteicon.href="";
 		row.childNodes[4].appendChild(deleteicon);
 	}
}

function createDmCaTable(){
 	//repeat for data model manager page
 	var dmcalist = document.getElementById("dm_calist");
 	if (dmcalist == null) return;
 	
 	var cas = JSON.parse(this.responseText);
 	for (var i = 0; i < cas.length; i ++){
 		var calbl = document.createElement("label");
 		calbl.className="block";
 		
 		var checkbox = document.createElement("input");
 		checkbox.type="checkbox";
 		checkbox.value=cas[i].uuid;
 		
 		calbl.appendChild(checkbox);
 		dmcalist.appendChild(calbl);
 		
 		calbl.innerHTML = calbl.innerHTML + cas[i].label;
 	}
}

function submitDmUpdate(){
	hideInfo();
	
	var dmcalist = document.getElementById("dm_calist");
 	if (dmcalist == null) return;
 	
 	var file = document.getElementById('dmfile').files[0];
	if (file == null){
		alert(i18n("ca.datamodelrequired"));
		return false;
	}
	
 	var checkedItems = document.querySelectorAll("#dm_calist > label > input:checked ")
 	var cas = [];
 	for (var i = 0; i < checkedItems.length; i ++){
 		cas[i] = checkedItems[i].value;
 	}
 	if (cas.length == 0){
		alert(i18n("ca.datamodelcarequired"));
		return false;
	}
 	
 	displayInfo(i18n("ca.datamodelprocessing"));
 	//Upload the actual file now
    var xhr = new XMLHttpRequest();
    var fd = new FormData();
    xhr.open("POST", "../api/ca/datamodel", true);
    xhr.setRequestHeader("enctype", "multipart/form-data;charset=UTF-8");
    fd.append("dm_file", file);
    fd.append("conservation_areas", cas);
    xhr.onload = mergeDmComplete;
    xhr.send(fd);
}

function mergeDmComplete(){
	if (this.status != 200) {
		var msg = i18n("ca.datamodelmergeerror")
		if (this.status == 401){
			msg += " " + i18n("ca.datamodelmergeerrorunauthorized");
		}
		displayError(parseError(msg , this.responseText));
		return;
	}else{
		var warnings = JSON.parse(this.responseText);
		msg = i18n("ca.datamodelmergecomplete");	
		if (warnings.length > 0){
			msg += i18n("ca.datamodelmergewarn") + "<br>";
			for (var i = 0; i < warnings.length; i ++){
				msg += warnings[i] + "<br>";
			}
		}
		displayInfo(msg);
	}
}