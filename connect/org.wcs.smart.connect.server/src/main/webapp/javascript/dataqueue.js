var DATAQUEUEURL = "../api/dataqueue";

var oReq;

/* configure events on html elements */
window.onload = function(){
	//new user dialog
	document.querySelector("#btnNewFile").onclick=clearAndShowNewFileDialog;
	document.querySelector("#cancelNewFile").onclick = function(){closeDialog('newFileDialog');};
	document.querySelector("#cancelUpdateFile").onclick = function(){closeDialog('updateFileDialog');};
	document.getElementById("btnUploadFile").onclick = createItemOnServer;
	document.getElementById("btnUpdateFile").onclick = updateItemOnServer;

	
	refreshFileList();
}

/*uploads the new file to the server API*/  
function createItemOnServer(){
	var url = DATAQUEUEURL + '/items/';
	var cauuid = document.querySelector("select[name=conservationArea]").value;
	var type = document.querySelector("select[name=type]").value;
	var fileLength = getFilesize();
	var jsonData =  {
			"conservationArea":cauuid,
			"type":type
			}
	
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
  		closeDialog('newFileDialog');
   	} else {
   		displayError(parseError(i18n("dataqueue.erroruploadingfile"), this.responseText));
   		
   	}
	refreshFileList();
}


function updateItemOnServer(){
	var uuid = document.querySelector("input[name=updateUuid]").value;
	var type = document.querySelector("select[name=newStatus]").value;
	var url = DATAQUEUEURL + '/items/' + uuid + "/" + type;

	oReq = new XMLHttpRequest();
	oReq.onload = fileUpdated;
	oReq.open("PUT", url, true);
	oReq.send();
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
// 	document.querySelector("input[name=conservationArea]").value = "";
// 	document.querySelector("input[name=type]").value = "";
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
 	oReq.onload = createFileTable;
 	oReq.open("Get", fileUrl , true);
 	oReq.send();
}
	
function createFileTable(){
	
	if (this.status != 200 && this.status != 201 ) {
		var msg = "Error: ";
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("div.filerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById("fileTable");
 	
	try{
		var files = JSON.parse(this.responseText);

	 	if(typeof files === "undefined" || files.length == 0){
		 	var newRow = document.createElement("div");
		 	parent.appendChild(newRow);
	 		newRow.style.display = "table-row";
	 		newRow.className = "filerow errorsection";
	 	    var oCell = document.createElement("div");
	 	    newRow.appendChild(oCell);
	 	    oCell.colSpan = 10;
	 	    oCell.innerHTML = i18n("dataqueue.nofilesfound");
		}else{
		 	for (var i = 0; i < files.length; i ++){
		 		var caUuid = files[i].conservationArea;
		 		var ca = files[i].caName;
		 		var name = files[i].name;
		 		var type = files[i].type;
		 		var uuid = files[i].uuid;
		 		var status = files[i].status;
		 		var uploadedDate = files[i].uploadedDate;
		 		var d = new Date(uploadedDate);
		 		
		 		var hours = d.getHours(); 
		 		if(d.getHours() < 10){
		 			hours = "0" + hours;
		 		}
		 		var mins = d.getMinutes(); 
		 		if(d.getMinutes() < 10){
		 			mins = "0" + mins;
		 		}
		 		var secs = d.getSeconds();
		 		if(d.getSeconds() < 10){
		 			secs = "0" + secs;
		 		}
		 		
		 		var date = d.getMonth() + "/" + d.getDate() + "/" + d.getFullYear() + "  " + hours + ":" +mins + ":" + secs;  
		 		var uploadedBy = files[i].uploadedBy; 

		 		var row = tableCreateRow(parent,
		 				[ca, name, type , status, date , uploadedBy, null], 
		 				"filerow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
		 		row.id = "fileRow" + i;
		 		row.dataset.uuid = uuid;
	
		 		//TODO - should we check for permissions for update/delete, or assume any data queue permission = all data queue permission
//		 		if(canupdate){
		 			var updateicon = document.createElement("a");
			 		updateicon.className="update-icon";
			 		updateicon.title="update status";
			 		updateicon.onclick = updateFile;
			 		updateicon.href="";
			 		row.childNodes[6].appendChild(updateicon);
//		 		}
//		 		if(candelete){
			 		var deleteicon = document.createElement("a");
			 		deleteicon.className="delete-icon";
			 		deleteicon.title="delete file";
			 		deleteicon.onclick = deleteFile;
			 		deleteicon.href="";
			 		row.childNodes[6].appendChild(deleteicon);
//		 		}
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

function updateFile(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	document.querySelector("input[name=updateUuid]").value = uuid;
	displayDialog('updateFileDialog', 'main');
	return false;
}

function deleteFile(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var ok = window.confirm(i18n("dataqueue.areyousuredelete") );
	if (!ok) return false;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = fileDeleted;
	oReq.open("DELETE", DATAQUEUEURL  + "/items/" + encodeURIComponent(uuid), true);
	oReq.send();
	return false;	
}

function fileDeleted(){
	if (this.status == 200  && this.status != 201 ) {
		var r = JSON.parse(this.response);
		displayInfo(i18n("dataqueue.deletefiletwith") + r.uuid);
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