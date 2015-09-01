var CAURL = "../api/conservationarea/";

/* configure events on html elements */
window.onload = function(){
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
function downloadca(){
	alert('download');
	var cauuid = this.dataset.cauuid;
	window.open('https://localhost:8443/server/api/conservationarea/' + cauuid + '?data=all', '_blank');
}

function confirmdeleteca(){
	
	var cauuid = this.dataset.cauuid;
	var status = this.dataset.status;
	
	if (status == 'DATA'){
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

	document.querySelector("#dialogerror").style.display = "none";
	
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
		document.querySelector("#dialogerror").style.display = "block";
		document.querySelector("#dialogerror").innerHTML = "Username required.";
		return false;
	}
	if (password.length == 0){
		document.querySelector("#dialogerror").style.display = "block";
		document.querySelector("#dialogerror").innerHTML = "Password required.";
		return false;
	}
	
	var ok = window.confirm("Are you sure you want to the conservation area " + cauuid + "?");
	if (!ok) return false;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = caDeleted;
	oReq.cauuid=cauuid;
	oReq.open("DELETE", CAURL + encodeURIComponent(cauuid) + "?username=" + encodeURIComponent(username) + "&password="+encodeURIComponent(password)+"&dataonly="+encodeURIComponent(dataonly), true);
	oReq.send();
	
	closeDialog('deleteDialog');
	return false;
}


function caDeleted(){
	if (this.status == 204) {
		displayInfo("Conservation area (" + this.cauuid + ") data deleted");
	} else if (this.status == 401){
		displayError(parseError("Error deleting conservation area " + this.cauuid + ". Unauthorized.", this.responseText));
	}else{
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
 		deleteicon.dataset.status = cas[i].status;
 		deleteicon.onclick = confirmdeleteca;
 		deleteicon.href="";
 		row.childNodes[4].appendChild(deleteicon);
 		
 		var downloadca = document.createElement("a");
 		deleteicon.className="downloadca";
 		deleteicon.title="downloadca";
 		deleteicon.dataset.cauuid = cas[i].uuid;
 		deleteicon.onclick = downloadca;
 		deleteicon.href="";
 		row.childNodes[4].appendChild(downloadca);
 		
 		
 	}
}