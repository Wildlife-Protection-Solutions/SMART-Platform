var DATAQUEUEURL = "../api/dataqueue";

var oReq;

var lastSorted = "name";
var files = "";

var startDatePicker;
var endDatePicker;
var warningcache;

/* configure events on html elements */
window.onload = function(){
	menuCheckOnload();
	
	//new user dialog
	var newbtn = document.querySelector("#btnNewFile");
	if (newbtn != null){
		newbtn.onclick=clearAndShowNewFileDialog;
	}
	
	let startbtn = document.querySelector("#btnStartProcessing");
	if (startbtn != null){
		startbtn.onclick = startFileProcessing;
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
	
	document.querySelector("#filedetails").onclick=function(){showTab("filedetails");}
	document.querySelector("#filepreview").onclick=function(){showTab("filepreview");}
	showTab("filedetails");
	
	refreshFileList();
	
	startDatePicker = new Pikaday({
		format: 'YYYY-MM-DD',
		field: document.getElementById('startdatefilter'),
		firstDay: 1,
        minDate: new Date('1950-01-01'),
        yearRange: [1950,2050],
        i18n: pickaday_i18n,
        onSelect: filterChanged
	});

	endDatePicker = new Pikaday({
		field: document.getElementById('enddatefilter'),
		firstDay: 1,
        minDate: new Date('1950-01-01'),
        yearRange: [1950,2050],
        i18n: pickaday_i18n,
        onSelect: filterChanged
	});
	
	updateDateFilterVisibility();
	
	
 	if(typeof(EventSource) !== "undefined") {
		var url = DATAQUEUEURL + '/updates';
     	var source = new EventSource(url);
     	
    	source.addEventListener("dataqueue", function(event) {
			var item = JSON.parse(event.data);
        	if (item.status == null){
				//deleted
				refreshFileList();
			}else{
				//search of item in table and update table
				let index = -1;
				for (var i = 0; i < files.length; i ++){
					if (files[i].uuid == item.uuid){
						index = i;
					}
				}
				if (index == -1){
					//no row likely new - perhaps initiate full refresh
					refreshFileList();
					return;
				}
						
				files[index] = item;
				
				let tr = document.getElementById("fileRow" + index);
				if (tr == null){
					//no row likely new - perhaps initiate full refresh
					refreshFileList();
					return;
				}
				
				//only the status should be updated
		 		tr.dataset.status = item.status;
				while (tr.childNodes[4].firstChild) {
   					 tr.childNodes[4].removeChild(tr.childNodes[4].lastChild);
  				}
				tr.childNodes[4].innerHTML = item.status
						
				if (item.status == "COMPLETE_WARN" || (item.status == "QUEUED" && item.statusMessage )){
					//add a warning icon with tooltip message
					var img = document.createElement("i");
					img.className = getStatusImage("WARNING");
					img.style.color = getStatusImageColor("WARNING");
					img.style.paddingRight="2px";
					tr.childNodes[4].prepend(img);
							
					if (item.status == "COMPLETE_WARN"){
						img.title = i18n("dataqueue.processingwarning");
					}else{
						img.title = i18n("dataqueue.processingrequeued");
					}
				}
						
				var img = document.createElement("i");
				img.className = getStatusImage(item.status);
				img.style.color = getStatusImageColor(item.status);
				img.style.paddingRight="2px";
				tr.childNodes[4].style.whiteSpace = "nowrap";
				tr.childNodes[4].prepend(img);
			}
        	
        	
    	}, false);

    	
    	source.onerror = function(event) {
			source.close();
    	};

	 } else {
     	displayError("Sorry, server-sent events are not supported in your browser...");
 	}
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
	warningcache = [];
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
	updateFileInfoDetails();
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
		 				[null, ca, name, type , status, uploadedDate, null], 
		 				"filerow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
		 		row.id = "fileRow" + i;
		 		row.dataset.uuid = uuid;
		 		row.dataset.status = status;
		 		row.dataset.type = type;
		 		row.dataset.cauuid = caUuid;
				row.dataset.uploadeddate = files[i].uploadedDate;
				
		 		row.onclick = updateFileInfoDetails;

		 		var checkbox = document.createElement("input");
		 		checkbox.type="checkbox";
		 		checkbox.name="dataqueueitem";
		 		checkbox.dataset.status = status;
		 		checkbox.value = uuid;
		 		row.childNodes[0].style.verticalAlign ="middle";
		 		
		 		row.childNodes[0].appendChild(checkbox);
		 		
		 		//TODO - should we check for permissions for update/delete, or assume any data queue permission = all data queue permission
//		 		if(canupdate){
	 				var updateiconi = document.createElement("i");
	 				updateiconi.className = "fa-regular fa-xl fa-pen-to-square icon-btn-default";
	 				updateiconi.title=i18n("dataqueue.edittooltip")
			 		updateiconi.onclick = updateFile;
			 		row.childNodes[row.childNodes.length - 1].appendChild(updateiconi);
//		 		}
//		 		if(candelete){
			 		var deleteicon = document.createElement("i");
			 		deleteicon.className="fa-solid fa-xl fa-xmark icon-btn-default";
			 		deleteicon.style.marginLeft = "5px";			
			 		deleteicon.title=i18n("dataqueue.deletetooltip");
			 		deleteicon.onclick = deleteFile;
			 		row.childNodes[row.childNodes.length - 1].appendChild(deleteicon);
//		 		}
			 	

			 	var downloadicon = document.createElement("a");
			 	downloadicon.className="fa-solid fa-xl fa-download icon-btn-default";
			 	downloadicon.style.marginLeft = "5px";			 			 	
			 	downloadicon.title=i18n("dataqueue.downloadtooltip");
			 	downloadicon.onclick = downloadFile;
			 	row.childNodes[row.childNodes.length - 1].appendChild(downloadicon);
			 	row.childNodes[row.childNodes.length - 1].style.whiteSpace = "nowrap";
			 	
			 	if (status == "COMPLETE_WARN" || (status == "QUEUED" && files[i].statusMessage )){
								
					//add a warning icon with tooltip message
					var img = document.createElement("i");
			 		img.className = getStatusImage("WARNING");
			 		img.style.color = getStatusImageColor("WARNING");
			 		img.style.paddingRight="2px";
					row.childNodes[4].prepend(img);
					
					if (status == "COMPLETE_WARN"){
						img.title = i18n("dataqueue.processingwarning");
					}else{
						img.title = i18n("dataqueue.processingrequeued");
					}
				}
				
			 	var img = document.createElement("i");
			 	img.className = getStatusImage(status);
			 	img.style.color = getStatusImageColor(status);
			 	img.style.paddingRight="2px";
				row.childNodes[4].style.whiteSpace = "nowrap";
			 	row.childNodes[4].prepend(img);
			 	
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
		 	op.innerHTML = i18n("dataqueue.allfilter");
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
		 	op.innerHTML = i18n("dataqueue.allfilter");
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

function getStatusImage(status){
	if (status == "QUEUED"){
		return "fa-regular fa-xl fa-circle-dot ";
	}else if (status == "ERROR"){
		return "fa-regular fa-xl fa-circle-xmark";
	}else if (status == "DUPLICATE"){
		return "fa-regular fa-xl fa-circle-xmark";
	}else if (status == "UPLOADING"){
		return "fa-regular fa-xl fa-circle-up";
	}else if (status == "PROCESSING"){
		return "fa-solid fa-xl fa-circle-half-stroke";					
	}else if (status == "COMPLETE" || status == "COMPLETE_WARN"){
		return "fa-regular fa-xl fa-circle-check";
	}else if (status == "WARNING"){
		return "fa-solid fa-xl fa-triangle-exclamation";
	}
}

function getStatusImageColor(status){
	
	if (status == "QUEUED"){
		return "#003366";
	}else if (status == "ERROR" || status == "DUPLICATE"){
		return "red";
	}else if (status == "PROCESSING" || status == "UPLOADING"){
		return "#003366";
	}else if (status == "COMPLETE" || status == "COMPLETE_WARN"){
		return "green";
	}else if (status == "WARNING"){
		//return "#E9D502";
		return "#003366";
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

function updateDateFilterVisibility(){

	if (document.getElementById('datefilter').checked == true){
		document.getElementById('startdatefilter').style.color="black";
		document.getElementById('startdatefilter').style.background="white";
		document.getElementById('startdatefilter').disabled = false;
		
		document.getElementById('enddatefilter').style.color="black";
		document.getElementById('enddatefilter').style.background="white";
		document.getElementById('enddatefilter').disabled = false;
	}else{
		document.getElementById('startdatefilter').style.color="#bbbbbb";
		document.getElementById('startdatefilter').style.background="#dddddd";
		document.getElementById('startdatefilter').disabled = true;
		
		document.getElementById('enddatefilter').style.color="#bbbbbb";
		document.getElementById('enddatefilter').style.background="#dddddd";
		document.getElementById('enddatefilter').disabled = true;
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
	
	
	var startDateFilter = null;
	var endDateFilter = null;
	
	if (document.getElementById('datefilter').checked == true){
		if(document.getElementById('startdatefilter').value != ""){
			startDateFilter = new Date(document.getElementById('startdatefilter').value.substring(4));//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		}
		if(document.getElementById('enddatefilter').value != ""){
			endDateFilter = new Date(document.getElementById('enddatefilter').value.substring(4));//substring(4) drops the "Wed " from the field, which isnt' a valid date string.
		}
	}
	
	
	var etable = document.getElementById('fileTable');
	rows = etable.children;
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
		
		var dateok = true;
		if (startDateFilter != null && endDateFilter != null && row.dataset.uploadeddate != null){
			var lm = new Date(row.dataset.uploadeddate);
			var lmd = new Date(lm.getFullYear(), lm.getMonth(), lm.getDate());
			dateok = (lmd >= startDateFilter && lmd <= endDateFilter );
		}
		
		if (btypeok && bstatusok && bcaok && dateok){
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
	
	displayConfirmDialog(i18n("dataqueue.deletetitle"), i18n("dataqueue.confirmdelete"), function(){
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
	
	displayConfirmDialog(i18n("dataqueue.deletetitle"), i18n("dataqueue.areyousuredelete"), function(){
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
function updateFileInfoDetails(){
	
	var detailssection = document.getElementById('filedetails_body');
	detailssection.innerHTML = "";
	document.getElementById("previewarea").value = "";
		
		
	if (this == null || this.dataset == null || this.dataset.uuid == null){
		return;	
	}
	
	var itemuuid = this.dataset.uuid;
	
	//update details
	
	
	
	for (var i = 0; i < files.length; i ++){
		if (files[i].uuid == itemuuid){
			var status = "<i style='padding-right: 2px; color: " + getStatusImageColor(files[i].status) + "' class='" + getStatusImage(files[i].status) + "'></i>";
			var inner = "<table>" +
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Status:</td><td>" + status + files[i].status + "</td></tr>"+ 
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Status Message:</td><td>" + (files[i].statusMessage == null ? "" : files[i].statusMessage) + "</td></tr>"+
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Name:</td><td>" + files[i].name + "</td></tr>" +
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Conservation Area:</td><td>" + files[i].caName + "</td></tr>"+
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Type:</td><td>" + files[i].type + "</td></tr>"+
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Uploaded By:</td><td>" + files[i].uploadedBy + "</td></tr>"+
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Uploaded On:</td><td>" + formatDate(files[i].uploadedDate) + "</td></tr>"+
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Modified On:</td><td>" + formatDate(files[i].lastModifiedDate) + "</td></tr>"+						
			"<tr><td style=\"vertical-align: top; white-space: nowrap; padding: 5px;\">Warnings:</td><td><div style=\"max-width: 200px\" id=\"warn_" + itemuuid + "\"></div></td></tr>"+
			"</table>";
			 	
			detailssection.innerHTML=inner;
			break;		
		}
	}
	
	//get warnings: /server/api/dataqueue/items/{uuid}
	if (warningcache[itemuuid] != null){
		var id = document.getElementById("warn_" + itemuuid);
		id.innerHTML = warningcache[itemuuid];
	}else{
		var oReq = new XMLHttpRequest();
	 	oReq.onload = updateDetailWarnings;
	 	oReq.open("Get", DATAQUEUEURL + "/items/" + encodeURIComponent(itemuuid), true);
	 	oReq.send();
	 }
		
	//get preview
	var currentSelection = document.querySelector("#fileTable > .smart-table-selectedrow");
	if (currentSelection != null){
		currentSelection.className = currentSelection.className.replace(/(?:^|\s)smart-table-selectedrow(?!\S)/ , '' );
	}
	this.className = this.className + " smart-table-selectedrow";
	oReq = new XMLHttpRequest();
 	oReq.onload = showPreviewResults;
 	oReq.open("Get", DATAQUEUEURL + "/items/" + encodeURIComponent(itemuuid) + "/preview", true);
 	oReq.send();
}

function startFileProcessing(){
	//ask file processing to start
	var oReq = new XMLHttpRequest();
	oReq.open("Put", DATAQUEUEURL + "/processing/start", true);
	oReq.send();
}

function showPreviewResults(){
	if (this.status != 200){
		document.getElementById("previewarea").value = parseError(i18n("dataqueue.error"), this.responseText);
		return;
	}
	
	document.getElementById("previewarea").value = this.responseText ;
}

function updateDetailWarnings(){
	if (this.status != 200){
		return;
	}
	var r = JSON.parse(this.response);
	var id = document.getElementById("warn_" + r.uuid);
	
	let message = "";
	for (let warn of r.warningMessageList){
		message += warn;
		message += "<br>";
	}
	id.innerHTML = message;
	warningcache[r.uuid] = message;
}