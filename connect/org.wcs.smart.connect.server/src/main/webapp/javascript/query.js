var QUERYURL = "../api/query/";


/* configure events on html elements */
window.onload = function(){
	refreshQueryList();
}

function refreshQueryList(){
	
	//clear current table
	var objects = document.querySelectorAll("div.queryrow");
	
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.getElementById('querytable');
	
	var row = document.createElement("div");
	row.className="queryrow";
	row.innerHTML=i18n("query.refreshingqueries");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createQueryTable;
 	oReq.open("Get", QUERYURL, true);
 	oReq.send();
}

function createQueryTable(){
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
	var objects = document.querySelectorAll("div.queryrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.getElementById('querytable');
 	var queries = JSON.parse(this.responseText);
 	for (var i = 0; i < queries.length; i ++){
 		var row = tableCreateRow(parent, 
 				[queries[i].conservationArea, queries[i].name, queries[i].type, queries[i].id, null], 
 				"queryrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		
 		row.dataset.queryuuid = queries[i].uuid;
 		
 		var runicon = document.createElement("a");
 		runicon.className="run-icon";
 		runicon.title= i18n("query.runquery");
 		runicon.onclick = runQuery(queries[i].uuid);
 		runicon.href="";
 		row.childNodes[4].appendChild(runicon);
 	}
}

function runQuery(uuid){
	
}