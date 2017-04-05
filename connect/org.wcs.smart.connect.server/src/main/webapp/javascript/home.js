var QuicklinksURL = "../api/quicklink";
var AddToAllURL = "../api/quicklink/addtoall";
var UserURL = "../api/connectuser/iscurrentuseradmin";

/* configure events on html elements */
window.onload = function(){
	updateLinkList();
	
	document.querySelector("#updateUserQuicklinkForm").onsubmit = submitUserUpdatedQuicklink;
	document.querySelector("#updateQuicklinkForm").onsubmit = submitUpdatedQuicklink;
	document.querySelector("#canceluser").onclick = function(){
		closeDialog('updateUserQuicklinkDialog');
	};
	document.querySelector("#cancel").onclick = function(){
		closeDialog('updateQuicklinkDialog');
	};
	document.querySelector("#cancelmanageall").onclick = function(){
		closeDialog('manageQuicklinksDialog');
		actionComplete();//redraw the main table in case edits affected their own list
	};
	

	
	getUserDetails();
}

function updateLinkList(){
	var objects = document.querySelectorAll("tr.linkrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = writeLinkList;
 	oReq.open("Get", QuicklinksURL + "/user/" , true);
 	oReq.send();
}


function writeLinkList(){
	simpleDiv = document.getElementById('simpleview');
	simpleDiv.innerHTML = "";
	
 	var links = JSON.parse(this.responseText);
 	for (var i = 0; i < links.length; i++){
 		link = links[i];
 		htmllink = document.createElement("a");
 		htmllink.href =  addhttp(link.url);
 		htmllink.text =  link.label;
 		htmllink.target = "_blank";
 		
 		htmllink2 = document.createElement("a");
 		htmllink2.href =  addhttp(link.url);
 		htmllink2.text =  link.label;
 		htmllink2.target = "_blank";
 		
 		simpleDiv.appendChild(htmllink2);
 		simpleDiv.appendChild(document.createElement("br")); 
 		
 		var parent = document.getElementById('quicklinklist');
 		
 		var row = tableCreateRowTDs(parent,
 				[null, link.order,null], 
 				"linkrow smart-table-rowon");
 		row.id = "linkRow" + i;
 		row.dataset.uuid = link.uuid;
 		row.dataset.order = link.order;

 		row.childNodes[0].appendChild(htmllink);
 		
		var upicon = document.createElement("a");
 		upicon.className="up-icon";
 		upicon.title= i18n("quicklinks.reorderhigher");
 		upicon.onclick = updateQuicklinkLowerOrderValue;
		upicon.href="";
 		row.childNodes[2].appendChild(upicon);
 		
 		var downicon = document.createElement("a");
 		downicon.className="down-icon";
 		downicon.title=i18n("quicklinks.reorderlower");
 		downicon.onclick = updateQuicklinkHigherOrderValue;
		downicon.href="";
 		row.childNodes[2].appendChild(downicon);

	 	var updateicon = document.createElement("a");
 		updateicon.className="update-icon";
 		updateicon.title=i18n("quicklinks.updatequicklink");
 		updateicon.onclick = updateUserQuicklink;
 		updateicon.href="";
 		row.childNodes[2].appendChild(updateicon);
 		
 		var deleteicon = document.createElement("a");
	 	deleteicon.className="delete-icon";
	 	deleteicon.title=i18n("quicklinks.deletequicklink");
	 	deleteicon.onclick = deleteUserQuicklink;
	 	deleteicon.href="";
	 	row.childNodes[2].appendChild(deleteicon);

 	}
}

function sendLink(APIurl, returnFunction){
	var url = document.querySelector("input[name=url]").value;
	var label = document.querySelector("input[name=label]").value;
	
	var jsonData = { "url": url,
	             "label": label,
				};
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = returnFunction;
 	oReq.open("POST", APIurl, true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
 	oReq.send(JSON.stringify(jsonData));
 	return false;
}

function addLink(){
	sendLink(QuicklinksURL, addLinkToMyList);
	return false;
}

function addLinktoAll(){
	sendLink(AddToAllURL, actionComplete);
	return false;
}

function addLinkToMyList(){
	var quicklink = JSON.parse(this.responseText);
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = actionComplete();
 	oReq.open("Get", QuicklinksURL + "/addtolist/?quicklinkUuid=" + quicklink.uuid, true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
 	oReq.send();
 	return false;
}

function addhttp(url) {
	   if (!/^(f|ht)tps?:\/\//i.test(url)) {
	      url = "http://" + url;
	   }
	   return url;
}

function actionComplete(){
	setTimeout(updateLinkList,100);
}
function manageActionComplete(){
	setTimeout(redrawtable,100);
}

function updateQuicklinkLowerOrderValue(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var order = this.parentElement.parentElement.getAttribute('data-order');
	var rowIndex = this.parentElement.parentElement.rowIndex;
	var newOrder = this.parentElement.parentElement.parentElement.rows[rowIndex -1].getAttribute('data-order');
	if(newOrder == null){
		newOrder = order -1;
	}else{
		newOrder--;
	}
	updateOrder(uuid, newOrder);// make it lower than the previous existing
								// order
	return false;
}

function updateQuicklinkHigherOrderValue(){
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var rowIndex = this.parentElement.parentElement.rowIndex;
	if(this.parentElement.parentElement.parentElement.rows.length > rowIndex + 1){
		var newOrder = this.parentElement.parentElement.parentElement.rows[rowIndex +1].getAttribute('data-order');
	}else{
		var newOrder = this.parentElement.parentElement.getAttribute('data-order');
	}
	newOrder++;
	updateOrder(uuid, newOrder);// make it higher than the next existing order
	return false;	
}

function updateOrder(uuid, order){
	var jsonData = { "order": order};
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = actionComplete();
 	oReq.open("Put", QuicklinksURL + "/user/" + uuid, true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
 	oReq.send(JSON.stringify(jsonData));
	return false;
}

function deleteQuicklink(){
	
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var deleteURL = QuicklinksURL + "/" + uuid;
	displayConfirmDialog(i18n("quicklink.deletequicklink"),  i18n("quicklink.areyousuredelete") , function(){
		
		
		var oReq = new XMLHttpRequest();
	 	oReq.onload = manageActionComplete();
	 	oReq.open("Delete", deleteURL, true);
		oReq.setRequestHeader("Accept","application/json");
		oReq.setRequestHeader("Content-Type","application/json");
	 	oReq.send();
		return false;
	});
	return false;
}

function deleteUserQuicklink(){
	
	var uuid = this.parentElement.parentElement.getAttribute('data-uuid');
	var deleteURL = QuicklinksURL + "/user/" + uuid;
	displayConfirmDialog(i18n("quicklink.deletequicklink"),  i18n("quicklink.areyousuredelete") , function(){
		
		
		var oReq = new XMLHttpRequest();
	 	oReq.onload = actionComplete();
	 	oReq.open("Delete", deleteURL, true);
		oReq.setRequestHeader("Accept","application/json");
		oReq.setRequestHeader("Content-Type","application/json");
	 	oReq.send();
		return false;
	});
	return false;
}


function updateUserQuicklink(){
	displayDialog('updateUserQuicklinkDialog', 'main');
	document.querySelector("input[name=update-uuid]").value = this.parentElement.parentElement.getAttribute('data-uuid');
	document.querySelector("input[name=update-label]").value = this.parentElement.parentElement.cells[0].childNodes[0].innerText
	document.querySelector("input[name=update-order]").value = this.parentElement.parentElement.getAttribute('data-order');
	return false;
}

function updateQuicklink(){
	displayDialog('updateQuicklinkDialog', this.parentElement.parentElement.id);
	document.querySelector("input[name=update-qluuid]").value = this.parentElement.parentElement.getAttribute('data-uuid');
	document.querySelector("input[name=update-url]").value = this.parentElement.parentElement.getAttribute('data-url');
	
	return false;
}

function submitUserUpdatedQuicklink(){
	var jsonData = {
			"order": document.querySelector("input[name=update-order]").value,
			"labelOverride": document.querySelector("input[name=update-label]").value,
		};
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = actionComplete();
 	oReq.open("Put", QuicklinksURL + "/user/" + document.querySelector("input[name=update-uuid]").value, true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
 	oReq.send(JSON.stringify(jsonData));
	
	closeDialog('updateUserQuicklinkDialog');
	return false;
}

function submitUpdatedQuicklink(){
	var jsonData = {
			"url": document.querySelector("input[name=update-url]").value
		};
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = manageActionComplete();
 	oReq.open("Put", QuicklinksURL + "/" + document.querySelector("input[name=update-qluuid]").value, true);
	oReq.setRequestHeader("Accept","application/json");
	oReq.setRequestHeader("Content-Type","application/json");
 	oReq.send(JSON.stringify(jsonData));
	
	closeDialog('updateQuicklinkDialog');
	return false;
}

function getUserDetails(){
	var oReq = new XMLHttpRequest();
 	oReq.onload = userCallback;
 	oReq.open("Get", UserURL , true);
 	oReq.send();
}

function userCallback(){
	var admin = JSON.parse(this.responseText);
	if(admin){
		document.getElementById('addToAllButton').style.display = "inline-block";
		document.getElementById('manageall').style.display = "block";
	}else{
		document.getElementById('addfromadmin').style.display = "inline-block";
		
		var oReq = new XMLHttpRequest();
	 	oReq.onload = listOfAdminLinksCallback;
	 	oReq.open("Get", QuicklinksURL + "/adminonly" , true);
	 	oReq.send();
	}
}

function listOfAdminLinksCallback(){
	var links = JSON.parse(this.responseText);
	var select = document.getElementById('admin-selectlist'); 
 	for (var i = 0; i < links.length; i++){
 		link = links[i];
 		var opt = document.createElement('option');
 	    opt.value = link.uuid;
 	    opt.innerHTML = link.label;
 	    select.appendChild(opt);
 	}
}

function manageQuicklinks(){
	displayDialog("manageQuicklinksDialog", "main");
	redrawtable();
}

function redrawtable(){
		var oReq = new XMLHttpRequest();
 	oReq.onload = writeManageAllQuicklinksTable;
 	oReq.open("Get", QuicklinksURL , true);
 	oReq.send();
}

function writeManageAllQuicklinksTable(){
	var links = JSON.parse(this.responseText);
	redrawManageAllTable(links);
}


function redrawManageAllTable(links){
	var objects = document.querySelectorAll("tr.quicklinkrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	tablediv = document.getElementById("managequicklinktable");

 	for (var i = 0; i < links.length; i++){
 		link = links[i];
 		htmllink = document.createElement("a");
 		htmllink.href =  addhttp(link.url);
 		htmllink.text =  link.label;
 		htmllink.target = "_blank";
 		
 		var row = tableCreateRowTDs(tablediv,
 				[null, timeConverter(link.createdOn), link.adminCreated,null], 
 				"quicklinkrow smart-table-rowon");
 		row.id = "quicklinkRow" + i;
 		row.dataset.uuid = link.uuid;
 		row.dataset.url = link.url;

 		row.childNodes[0].appendChild(htmllink);

	 	var updateicon = document.createElement("a");
 		updateicon.className="update-icon";
 		updateicon.title="update Quicklink";
 		updateicon.onclick = updateQuicklink;
 		updateicon.href="";
 		row.childNodes[3].appendChild(updateicon);
 		
 		var deleteicon = document.createElement("a");
	 	deleteicon.className="delete-icon";
	 	deleteicon.title="delete Quicklink";
	 	deleteicon.onclick = deleteQuicklink;
	 	deleteicon.href="";
	 	row.childNodes[3].appendChild(deleteicon);
 	}
}

function addFromAdminList(){
	var e = document.getElementById('admin-selectlist');
	var uuid = document.getElementById('admin-selectlist').options[e.selectedIndex].value;
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = actionComplete();
 	oReq.open("Get", QuicklinksURL + "/addtolist/?quicklinkUuid=" + uuid, true);
 	oReq.send();
 	return false;
}

function timeConverter(timestamp){
	  var a = new Date(timestamp);
	  var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
	  var year = a.getFullYear();
	  var month = months[a.getMonth()];
	  var date = a.getDate();
	  var hour = a.getHours();
	  var min = a.getMinutes();
	  if(min < 10) min = "0" + min;
	  var sec = a.getSeconds();
	  if(sec < 10) sec = "0" + sec;
	  var time = date + ' ' + month + ' ' + year + ' ' + hour + ':' + min + ':' + sec ;
	  return time;
}

function manageMylinks(){
	document.getElementById('quicklinklist').style.display = "block";
	document.getElementById('newquicklink').style.display = "block";
	
	document.getElementById('simpleview').style.display = "none";
	document.getElementById('managemylinks').style.display = "none";
}