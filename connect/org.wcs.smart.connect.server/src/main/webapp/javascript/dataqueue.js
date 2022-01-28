var DATAQUEUEURL = "../api/dataqueue";

var oReq;

var lastSorted = "name";
var files = "";


/* configure events on html elements */
window.onload = function(){
	menuCheckOnload();
	
	//new user dialog
	var newbtn = document.querySelector("#btnNewFile");
	if (newbtn != null){
		newbtn.onclick=clearAndShowNewFileDialog;
	}
	document.querySelector("#cancelNewFile").onclick = function(){closeDialog('newFileDialog');};
	document.querySelector("#cancelUpdateFile").onclick = function(){closeDialog('updateFileDialog');};
	document.getElementById("btnUploadFile").onclick = createItemOnServer;
	document.getElementById("btnUpdateFile").onclick = updateItemOnServer;
	
	document.getElementById("btnDeleteSelected").onclick = deleteSelected;
	document.getElementById("selectCompleted").onclick = checkCompleted;
	document.getElementById("selectAll").onclick = checkAll;
	document.getElementById("selectNone").onclick = checkNone;
	
	document.getElementById("refreshnow").onclick = refreshFileList;

	
	refreshFileList();
}

/*uploads the new file to the server API*/  
function createItemOnServer(){
	var url = DATAQUEUEURL + '/items/';
	var cauuid = document.querySelector("select[name=conservationArea]").value;
	var type = document.querySelector("select[name=type]").value;
	var fileLength = getFilesize();
	var filename = document.getElementById('file').files[0].name;
	
	var jsonData =  {
			"conservationArea":cauuid,
			"type": type,
			"name": filename
			}
	displayInfo(i18n("dataqueue.fileuploading") + filename);
	closeDialog('newFileDialog');
	
	//First request, Get Upload URL from API
	oReq = new XMLHttpRequest();
	oReq.open("POST", url, true);
	oReq.setRequestHeader("Content-Type","application/json");
	oReq.setRequestHeader("X-Upload-Content-Length", fileLength);
	oReq.onload = uploadFile;

	oReq.send(JSON.stringify(jsonData));
	
}

	
function uploadFile(){
	var uploadUrl = oReq.getResponseHeader("location");
	var file = document.getElementById('file').files[0];
	
	//Upload the actual file now
    var xhr = new XMLHttpRequest();
    var fd = new FormData();
    xhr.open("POST", uploadUrl, true);
//    xhr.setRequestHeader("contentType", "multipart/form-data");
    fd.append("upload_file", file);
    xhr.onload = uploadComplete;
    xhr.send(fd);
}

function uploadComplete(){
	
	if(this.status == 202) {
   		var user = JSON.parse(this.responseText);
  		displayInfo(i18n("dataqueue.fileuploaded"));
   	} else {
   		displayError(parseError(i18n("dataqueue.erroruploadingfile"), this.responseText));
   		
   	}
	refreshFileList();
}


function updateItemOnServer(){
	var uuid = document.querySelector("input[name=updateUuid]").value;
	var status = document.querySelector("select[name=newStatus]").value;
	var type = document.querySelector("select[name=updateType]").value;
	var url = DATAQUEUEURL + '/items/' + uuid ;
	var jsonData =  {
			"type": type,
			"status": status
			}
	oReq = new XMLHttpRequest();
	oReq.onload = fileUpdated;

	oReq.open("PUT", url, true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));

	closeDialog('updateFileDialog');
}

function fileUpdated(){
	if(this.status == 200) {
   		var user = JSON.parse(this.responseText);
  		displayInfo(i18n("dataqueue.fileupdated"));
   	} else {
   		displayError(parseError(i18n("dataqueue.errorupdatingfile"), this.responseText));
   	}
	refreshFileList();
}

/* clears and displays new File upload dialog */
function clearAndShowNewFileDialog(){
 	displayDialog('newFileDialog', 'main');
}


function refreshFileList(){
	var parent = document.getElementById("fileTable");
	
	var objects = document.querySelectorAll("div.filerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var row = document.createElement("div");
	row.className="filerow";
	row.innerHTML= i18n("dataqueue.refreshing");
	parent.appendChild(row);
	
	var fileUrl = DATAQUEUEURL + "/detailedItems";
	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = processFileApiResponse;
 	oReq.open("Get", fileUrl , true);
 	oReq.send();
 	
 	return false;
}
	
function processFileApiResponse(){
	
	if (this.status != 200 && this.status != 201 ) {
		var msg = i18n("dataqueue.error");
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	
	var now = new Date();
	
	document.querySelector("#lastUpdateTime").innerHTML = formatDate(new Date());
	
	files = JSON.parse(this.responseText);
	createFilterTable();
}

function createFilterTable(){
	
	//clear current table
	var objects = document.querySelectorAll("div.filerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("fileTable");
 	
 	var caMap = new Map();
 	var statusSet = new Set();
 	var typeSet = new Set();
 	
	try{

	 	if(typeof files === "undefined" || files.length == 0){
		 	var newRow = document.createElement("div");
		 	parent.appendChild(newRow);
		 	newRow.style.display = "block";
	 		newRow.className = "filerow errorsection";
	 		newRow.innerHTML = i18n("dataqueue.nofilesfound");
		}else{
		 	for (var i = 0; i < files.length; i ++){
		 		var caUuid = files[i].conservationArea;
		 		var ca = files[i].caName;
		 		var name = files[i].name;
		 		var type = files[i].type;
		 		var uuid = files[i].uuid;
		 		var status = files[i].status;
		 		var uploadedDate = formatDate(files[i].uploadedDate);
		 		var lastModified = formatDate(files[i].lastModifiedDate);
		 		var uploadedBy = files[i].uploadedBy; 

				caMap.set(caUuid, ca);
		 		statusSet.add(files[i].status);		 		
				typeSet.add(files[i].type);
		 		
		 		
		 		var row = tableCreateRow(parent,
		 				[null, ca, name, type , status, lastModified, uploadedDate, uploadedBy, null], 
		 				"filerow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
		 		row.id = "fileRow" + i;
		 		row.dataset.uuid = uuid;
		 		row.dataset.status = status;
		 		row.dataset.type = type;
		 		row.dataset.cauuid = caUuid;
	
		 		row.onclick = showFilePreview;

		 		var checkbox = document.createElement("input");
		 		checkbox.type="checkbox";
		 		checkbox.name="dataqueueitem";
		 		checkbox.dataset.status = status;
		 		checkbox.value = uuid;
		 		row.childNodes[0].style.verticalAlign ="middle";
		 		
		 		row.childNodes[0].appendChild(checkbox);
		 		
		 		//TODO - should we check for permissions for update/delete, or assume any data queue permission = all data queue permission
//		 		if(canupdate){
	 				var updateicon = document.createElement("a");
			 		updateicon.className="update-icon marginleftright";
			 		updateicon.title=i18n("dataqueue.edittooltip")
			 		updateicon.onclick = updateFile;
			 		updateicon.href="";
			 		row.childNodes[row.childNodes.length - 1].appendChild(updateicon);
//		 		}
//		 		if(candelete){
			 		var deleteicon = document.createElement("a");
			 		deleteicon.className="delete-icon marginleftright";
			 		deleteicon.title=i18n("dataqueue.deletetooltip");
			 		deleteicon.onclick = deleteFile;
			 		deleteicon.href="";
			 		row.childNodes[row.childNodes.length - 1].appendChild(deleteicon);
//		 		}
			 	

			 	var downloadicon = document.createElement("a");
			 	downloadicon.className="download-icon marginleftright";
			 	downloadicon.title=i18n("dataqueue.downloadtooltip");
			 	downloadicon.onclick = downloadFile;
			 	downloadicon.href="";
			 	row.childNodes[row.childNodes.length - 1].appendChild(downloadicon);
			 	
		 	}
		 	
		 	//populate ca filter list
		 	var cafilter = document.getElementById('cafilter');
		 	//remove all
		 	while (cafilter.firstChild) { cafilter.removeChild(cafilter.firstChild); }
		 	//create all option
		 	var op = document.createElement("option");
		 	op.value = "all"
		 	op.innerHTML = "All";
		 	cafilter.appendChild(op);
		 	for (const [cauuid, caname] of caMap){
		 		op = document.createElement("option");
		 		op.value = cauuid
		 		op.innerHTML = caname;
		 		cafilter.appendChild(op);
		 	}
		 	
		 	var statusfilter = document.getElementById('statusfilter');
		 	while(statusfilter.firstChild) {statusfilter.removeChild(statusfilter.firstChild);}
		 	var op = document.createElement("option");
		 	op.value = "all"
		 	op.innerHTML = "All";
		 	statusfilter.appendChild(op);
		 	for (var stat of statusSet){
		 		op = document.createElement("option");
		 		op.value = stat
		 		op.innerHTML = stat;
		 		statusfilter.appendChild(op);
		 	}
		 	
		 	var typefilter = document.getElementById('typefilter');
		 	while(typefilter.firstChild) {typefilter.removeChild(typefilter.firstChild);}
		 	var op = document.createElement("option");
		 	op.value = "all"
		 	op.innerHTML = "All";
		 	typefilter.appendChild(op);
		 	for (var stat of typeSet){
		 		op = document.createElement("option");
		 		op.value = stat
		 		op.innerHTML = stat;
		 		typefilter.appendChild(op);
		 	}
		 	
	 	}
	 	
	}catch(err) {
 		var newRow = document.createElement("div");
 		newRow.style.backgroundColor = "#F00";
 	    var oCell = document.createElement("div");
 	    newRow.appendChild(oCell);
 	    oCell.colSpan = 10;
 	    oCell.innerHTML = err;
 	   parent.appendChild(newRow);
	}
}

function sortTable(sortColumn){
	if(lastSorted == sortColumn){
		sortColumn = "-" + sortColumn;
		lastSorted = ""; //set it to nothing, so if clicked a 3rd time it sorts in ascending order again.
	}else{
		lastSorted = sortColumn; 
	}
 	
	files.sort(dynamicSort(sortColumn));
 	
 	createFilterTable();
}

function dynamicSort(property) {
    var sortOrder = 1;
    if(property[0] === "-") {
        sortOrder = -1;
        property = property.substr(1);
    }
    
    return function (a,b) {
        var result = (a[property].toUpperCase() < b[property].toUpperCase()) ? -1 : (a[property].toUpperCase() > b[property].toUpperCase()) ? 1 : 0;
        return result * sortOrder;
    }
}

function filterChanged(){

	var ecafilter = document.getElementById('cafilter');
	var cafilter = ecafilter.options[ecafilter.selectedIndex].value;
		 	
	var estatusfilter = document.getElementById('statusfilter');
	var statusfilter = estatusfilter.options[estatusfilter.selectedIndex].value;
		 	
	var etypefilter = document.getElementById('typefilter');
	var typefilter = etypefilter.options[etypefilter.selectedIndex].value;
	
	if (cafilter == "all") cafilter = null;
	if (statusfilter == "all") statusfilter = null;
	if (typefilter == "all") typefilter = null;
	
	var etable = document.getElementById('fileTable');
	rows = etable.getElementsByTagName("div");
	for (i = 0; i < rows.length; i++){
		
		var row = rows[i];
		
		var btypeok = true;
		if (typefilter != null){
		 	btypeok = row.dataset.type == null || (row.dataset.type != null && row.dataset.type == typefilter);
		}
		
		var bstatusok = true;
		if (statusfilter != null){
			bstatusok = row.dataset.status == null || (row.dataset.status != null && row.dataset.status == statusfilter);
		}
		
		var bcaok = true;
		if (cafilter != null){
			bcaok = row.dataset.cauuid == null || (cafilter != null && row.dataset.cauuid != null && row.dataset.cauuid == cafilter);
		}
		
		if (btypeok && bstatusok && bcaok){
			row.style.display = "";
		}else{
			row.style.display = "none";
		}
	
	}
	
}


function updateFile(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var status = this.parentElement.parentElement.getAttribute('data-status');
	var fileType= this.parentElement.parentElement.getAttribute('data-type');
	document.querySelector("input[name=updateUuid]").value = uuid;
	document.querySelector("select[name=newStatus]").value = status;
	document.querySelector("select[name=updateType]").value = fileType;
	displayDialog('updateFileDialog', 'main');
	return false;
}

function deleteSelected(){
	
	var checkedBoxes = document.querySelectorAll('input[name=dataqueueitem]:checked');
	if (checkedBoxes.length == 0) return false;
	
	displayConfirmDialog("Confirm Delete", "Are you sure you want to delete the selected items?", function(){
		hideInfo();	
		for (var i = 0; i < checkedBoxes.length; i ++){
			var uuid = checkedBoxes[i].value;
			var oReq = new XMLHttpRequest();
			oReq.onload = fileDeleted;
			
			oReq.open("DELETE", DATAQUEUEURL  + "/items/" + encodeURIComponent(uuid), true);
			oReq.send();
		};
		
	});
	return false;	
}

function checkNone(){
	var checkedBoxes = document.querySelectorAll('input[name=dataqueueitem]');
	for (var i = 0; i< checkedBoxes.length; i ++){
		checkedBoxes[i].checked = false;
	}
	return false;
}

function checkCompleted(){
	
	var checkedBoxes = document.querySelectorAll('input[name=dataqueueitem]');
	for (var i = 0; i< checkedBoxes.length; i ++){
		if (checkedBoxes[i].dataset.status == "COMPLETE"){
			checkedBoxes[i].checked = true;
		}
	}
	return false;
}
function checkAll(){
	var checkedBoxes = document.querySelectorAll('input[name=dataqueueitem]');
	for (var i = 0; i< checkedBoxes.length; i ++){
		if (checkedBoxes[i].parentElement.parentElement.style.display != "none"){
			checkedBoxes[i].checked = true;
		}
	}
	return false;
}


function deleteFile(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	
	displayConfirmDialog("Confirm Delete", i18n("dataqueue.areyousuredelete"), function(){
		hideInfo();
		
		var oReq = new XMLHttpRequest();
		oReq.onload = fileDeleted;
		oReq.open("DELETE", DATAQUEUEURL  + "/items/" + encodeURIComponent(uuid), true);
		oReq.send();
	});
	return false;	
}

function downloadFile(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	hideInfo();
	window.open(DATAQUEUEURL  + "/items/" + encodeURIComponent(uuid) + "/file");
	return false;	
}

function fileDeleted(){
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("dataqueue.deletefilewith") + r.name);
		refreshFileList();
	} else {
		displayError(parseError(i18n("dataqueue.deletefileterror") + this.statusText));
	}
}

function getFilesize(){
    if(window.ActiveXObject){
        var fso = new ActiveXObject("Scripting.FileSystemObject");
        var filepath = document.getElementById('file').value;
        var thefile = fso.getFile(filepath);
        var sizeinbytes = thefile.size;
    }else{
        var sizeinbytes = document.getElementById('file').files[0].size;
    }

    return sizeinbytes;
}


/* updates the user info section with the current selected user */
function showFilePreview(){
	
	if (this == null || this.dataset.uuid == null) return;
	var itemuuid = this.dataset.uuid;
	
	var currentSelection = document.querySelector("#fileTable > .smart-table-selectedrow");
	if (currentSelection != null){
		currentSelection.className = currentSelection.className.replace(/(?:^|\s)smart-table-selectedrow(?!\S)/ , '' );
	}
	this.className = this.className + " smart-table-selectedrow";
	var oReq = new XMLHttpRequest();
 	oReq.onload = showPreviewResults;
 	oReq.open("Get", DATAQUEUEURL + "/items/" + encodeURIComponent(itemuuid) + "/preview", true);
 	oReq.send();
}

function showPreviewResults(){
	if (this.status != 200){
		displayError(parseError(i18n("users.couldnotloaduser"), this.responseText));
		return;
	}
	
	document.getElementById("previewarea").value = this.responseText ;
}