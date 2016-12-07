var SHAREDLINKURL = "../api/sharedlink/";

var lastSorted;
var links = {};

/* configure events on html elements */
window.onload = function(){
	
	refreshLinkList();
}

function confirmdeletelink(){
	
	var uuid = this.dataset.uuid;
	
	displayConfirmDialog("Delete Shared Link", i18n("sharedlinks.areyousuredelete") + uuid + "?"  , function(){
		hideInfo();
		
		var oReq = new XMLHttpRequest();
		oReq.onload = linkDeleted;
		oReq.open("DELETE", SHAREDLINKURL  + encodeURIComponent(uuid), true);
		oReq.send();
		return false;	
	});
	return false;	
}


function linkDeleted(){
	if (this.status == 200) {
		displayInfo(i18n("sharedlink.deleted"));
	} else if (this.status == 401){
		displayError(parseError(i18n("sharedlink.unauthorized"), this.responseText));
	}else{
		displayError(parseError(i18n("sharedlink.errordeleting"), this.responseText));
	}
	refreshLinkList();
}

function refreshLinkList(){
	
	//clear current table
	var objects = document.querySelectorAll("div.linkrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("div.linktable");
	var row = document.createElement("div");
	row.className="linkrow";
	row.innerHTML=i18n("sharedlink.refreshing");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = getLinkListCallBack;
 	oReq.open("Get", SHAREDLINKURL, true);
 	oReq.send();
}
function getLinkListCallBack(){
	if (this.status != 200) {
		var msg = i18n("sharedlink.error") + ": ";
		if (this.status == 401){
			msg += i18n("sharedlink.unauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
 	links = JSON.parse(this.responseText);
	createLinkTable();
}

function createLinkTable(){
	
	//clear current table
	var objects = document.querySelectorAll("div.linkrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("div.linktable");

 	for (var i = 0; i < links.length; i ++){
 		var openlink = SHAREDLINKSERVLETURL + "?uuid=" + links[i].uuid;
 		
 		var date = new Date(links[i].expiresAt);
 		var row = tableCreateRow(parent,
 				[null, links[i].url, links[i].ownerUsername, date.toString() , null], 
 				"linkrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.uuid = links[i].uuid;

 		var aTag1 = document.createElement('a');
 		aTag1.setAttribute('href',openlink);
 		aTag1.setAttribute('target',"_blank");
 		aTag1.innerHTML = resolve(openlink);
 		aTag1.title="Open Link in New Tab"
 		row.childNodes[0].appendChild(aTag1);
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="deleteca delete-icon";
 		deleteicon.title="Delete Shared Link";
 		deleteicon.dataset.uuid = links[i].uuid;
 		deleteicon.onclick = confirmdeletelink;
 		deleteicon.href="";
 		row.childNodes[4].appendChild(deleteicon);
 	}
 	
}

//get full URL from a relative one, used to give full-url link to users to share.
function resolve(url) {
	  var doc      = document
	    , old_base = doc.getElementsByTagName('base')[0]
	    , old_href = old_base && old_base.href
	    , doc_head = doc.head || doc.getElementsByTagName('head')[0]
	    , our_base = old_base || doc_head.appendChild(doc.createElement('base'))
	    , resolver = doc.createElement('a')
	    , resolved_url
	    ;

	  resolver.href = url;
	  resolved_url  = resolver.href; // browser magic at work here

	  return resolved_url;
}

function sortTable(sortColumn){
	if(lastSorted == sortColumn){
		sortColumn = "-" + sortColumn;
		lastSorted = ""; //set it to nothing, so if clicked a 3rd time it sorts in ascending order again.
	}else{
		lastSorted = sortColumn; 
	}
 	links.sort(dynamicSort(sortColumn));
	createLinkTable();
}

//provides a custom sorting function,  
//param - property: the name of the key to sort on, "-" in the first character means sort descending 
function dynamicSort(property) {
    var sortOrder = 1;
    if(property[0] === "-") {
        sortOrder = -1;
        property = property.substr(1);
    }
    return function (a,b) {
    	if (isNaN(a[property])){//text sort
        	var result = (a[property].toUpperCase() < b[property].toUpperCase()) ? -1 : (a[property].toUpperCase() > b[property].toUpperCase()) ? 1 : 0;
        	return result * sortOrder;
    	}else{//numeric sort
    		if(a < b) return -1;
    		return 1
    	}
    }
}
