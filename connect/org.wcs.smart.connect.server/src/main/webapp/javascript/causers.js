var USER_URL = "../api/connectuser/";
var INACTIVE_USER_URL = "../api/connectuser/getinactive/";
var ACTIVATE_USER_URL  = "../api/connectuser/activate/";
var PRIVILEGE_URL = "../api/privileges";

var allActions = null;

/* configure events on html elements */
window.onload = function(){

	refreshUsers();
	refreshInactiveUsers();
	
	//setup onclicks for showing user info
	var elements = document.querySelectorAll(".smartuser");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=showUserInfo;
	}

}




/* reload users table */
function refreshUsers(){
	//clear current table
	var objects = document.querySelectorAll("#usertable > .userrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#usertable");
	var row = document.createElement("div");
	row.className="userrow";
	row.innerHTML=i18n("users.refreshusertable");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createUserTable;
 	oReq.open("Get", USER_URL, true);
 	oReq.send();
}

/* callback that displays all user info */
function createUserTable(){
	clearUserInfo();
	if (this.status != 200) {
		var msg = i18n("users.errorlabel");
		if (this.status == 401){
			msg += i18n("users.unathorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("div.userrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#usertable");
 	var users = JSON.parse(this.responseText);
 	for (var i = 0; i < users.length; i ++){
 		var row = tableCreateRow(parent, 
 				[users[i].username, users[i].email, null, null], 
 				"userrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.dataset.username = users[i].username;
 		row.onclick = showUserInfo;
 	
 	}
}

/* reload users table */
function refreshInactiveUsers(){
	//clear current table
	var objects = document.querySelectorAll("#inactiveusertable > .inactiveuserrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#inactiveusertable");
	var row = document.createElement("div");
	row.className="inactiveuserrow";
	row.innerHTML=i18n("users.refreshusertable");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createInactiveUserTable;
 	oReq.open("Get", INACTIVE_USER_URL, true);
 	oReq.send();
}

/* callback that displays all user info */
function createInactiveUserTable(){
	clearUserInfo();
	if (this.status != 200) {
		var msg = i18n("users.errorlabel");
		if (this.status == 401){
			msg += i18n("users.unathorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	//clear current table
	var objects = document.querySelectorAll("div.inactiveuserrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#inactiveusertable");
 	var users = JSON.parse(this.responseText);
 	for (var i = 0; i < users.length; i ++){
 		var row = tableCreateRow(parent, 
 				[users[i].username, users[i].email, null, null], 
 				"inactiveuserrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.dataset.username = users[i].username;
// 		row.onclick = showUserInfo; //not for inactive users
 	
 		var activateicon = document.createElement("a");
 		activateicon.className="activateuser run-icon";
 		activateicon.title="Activate User";
 		activateicon.dataset.username = users[i].username;
 		activateicon.onclick = activateUser;
 		activateicon.href="";
 		row.childNodes[2].appendChild(activateicon);
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete user";
 		deleteicon.dataset.username = users[i].username;
 		deleteicon.onclick = deleteUser;
 		deleteicon.href="";
 		row.childNodes[3].appendChild(deleteicon);
 		
 	}
}

function clearUserInfo(){
	document.querySelector("#userinfo").style.display="none";
}

/* updates the user info section with the current selected user */
function showUserInfo(){
	if (this == null || this.dataset.username == null) return;
	var username = this.dataset.username;
	document.querySelector("#userinfo").style.display="block";
	var currentSelection = document.querySelector("#usertable > .smart-table-selectedrow");
	if (currentSelection != null){
		currentSelection.className = currentSelection.className.replace(/(?:^|\s)smart-table-selectedrow(?!\S)/ , '' );
	}
	this.className = this.className + " smart-table-selectedrow";
	var oReq = new XMLHttpRequest();
 	oReq.onload = setUserDetails;
 	oReq.open("Get", USER_URL + encodeURIComponent(username), true);
 	oReq.send();
}


/* callback for showUserInfo which updates the info */
function setUserDetails(){
	if (this.status != 200){
		displayError(parseError(i18n("users.couldnotloaduser"), this.responseText));
		return;
	}
	var user = JSON.parse(this.responseText);
	var ele = document.querySelector("#userinfodefaults");
	var table = document.querySelector("#actiontable");
	if (table != null){
		table.parentElement.removeChild(table);
	}
	
	var html = "<div style='margin-top: 5px'><span class='label-header'>" + i18n("users.usernamelabel") + "</span><span>" + user.username + "</span></div>";
	html +="<div style='margin-top: 5px; margin-bottom: 5px'><span class='label-header'>" + i18n("users.emaillabel") + "</span><span>" + user.email + "</span></div>";
	html += "<p>" + i18n("causers.permission") + "</p>";
	ele.innerHTML = html;
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = setUserPrivileges;
 	oReq.username = user.username;
 	oReq.open("Get", PRIVILEGE_URL + "/user/" + encodeURIComponent(user.username), true);
 	oReq.send();
 	
 	loadActions();
 }


function loadActions(){
	if (allActions != null) return;
	var oReq = new XMLHttpRequest();
	oReq.onload = setActions;
	oReq.open("Get", PRIVILEGE_URL + "/actions", true);
	oReq.send();	
}


/* callback from loadActions to cache user actions */
function setActions(){
	if (this.status != 200){
		displayError(parseError(i18n("users.errorloadingactions"), this.responseText));
	}else{
		allActions = JSON.parse(this.responseText);
		updateActionsDropDown();
	}
}


/* callback for setting user actions */
function setUserPrivileges(){
	var privis = JSON.parse(this.responseText);
	
	var actionElement = document.querySelector("#actiontab_body");
	
	var actionTable = tableCreate();
	
	actionTable.id = "actiontable";
	
	actionElement.appendChild(actionTable);

	
	tableAddHeader(actionTable, [i18n("users.action"), i18n("users.resource"), ""]);
	
	document.querySelector("#addAction").setAttribute("onClick", "addAction('" + this.username + "');")
	
	updateActionsDropDown();
	
	var actionCnt = 1;

	for (var i = 0; i < privis.length; i ++){
		
		if (privis[i].type.toUpperCase() == "ACTION"){
			var row = tableCreateRow(actionTable, 
					[privis[i].name, privis[i].resourceName, ""], 
					 (actionCnt % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
			actionCnt++;
			var deleteicon = document.createElement("a");
			deleteicon.className="delete-icon";
			deleteicon.title="remove action";
			deleteicon.dataset.username = this.username;
			deleteicon.dataset.actionKey = privis[i].key;
			if (privis[i].resource != null){
				deleteicon.dataset.resourceKey = privis[i].resource;
			}
			deleteicon.onclick = deleteAction;
			deleteicon.href="";
			row.childNodes[2].appendChild(deleteicon);

		}	
	}
}

/* update actions drop down values */
function updateActionsDropDown(){
	
	var dropdowns = [document.querySelector("#actionKey"), document.querySelector("#roleActionKey")];
	var resourcedd = [document.querySelector("#actionResourceKey"), document.querySelector("#roleActionResourceKey")];
	for (var i = 0; i < dropdowns.length; i ++){
		var ddactions = dropdowns[i];
		if (ddactions != null){
			while(ddactions.lastChild){
				ddactions.removeChild(ddactions.lastChild);
			}
			var ddresources = resourcedd[i];
			if (ddresources != null){
				ddactions.setAttribute("onchange", "updateActionResourceDropDown(\"" + ddactions.id + "\", \"" + ddresources.id + "\"); return false;");
				
				while(ddresources.lastChild){
					ddresources.removeChild(ddresources.lastChild);
				}
			}
			if (allActions != null){
				for (var j = 0; j < allActions.length; j ++){
					var op = document.createElement("option");
					op.value=allActions[j].key;
					op.innerHTML = allActions[j].name;
					ddactions.appendChild(op);
				}
				ddactions.selectedIndex = 0;
				updateActionResourceDropDown(ddactions.id, ddresources.id);
			}
		}
	} 
}

/* update the resources drop down based on the action selection */
function updateActionResourceDropDown(actionElementId, resourceElementId){
	var ddresources = document.querySelector("#" + resourceElementId);
	if (ddresources != null){
		while(ddresources.lastChild){
			ddresources.removeChild(ddresources.lastChild);
		}
	}
	
	var ddactions = document.querySelector("#" + actionElementId);
	if (ddactions == null || allActions == null || ddresources == null) return;
	var selectedActionKey = ddactions.options[ddactions.selectedIndex].value;
	for (var i = 0; i < allActions.length; i ++){
		if (allActions[i].key === selectedActionKey){
			for (var j = 0; j < allActions[i].resources.length; j ++){
				var op = document.createElement("option");
				if(allActions[i].resources[j].key != null){
					op.value = allActions[i].resources[j].key;
				}else{
					op.value = "";
				}
				op.innerHTML = allActions[i].resources[j].name;
				ddresources.appendChild(op);
			}
		}
	}
}

/* delete action */
function deleteAction(){
	var username = this.dataset.username;
	var action = this.dataset.actionKey;
	var resource = this.dataset.resourceKey;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = actionDeleted;
	oReq.smartuser = username;
	var loc = PRIVILEGE_URL + "/user/";
	loc += encodeURIComponent(username);
	loc += "/action";
	loc += "/" + encodeURIComponent(action);
	if (resource != null){
		loc += "/" + encodeURIComponent(resource);
	}
	oReq.open("DELETE", loc, true);
	oReq.send();
	return false;	
}

//callback for delete action 
function actionDeleted() {
	if (this.status == 204) {
		displayInfo(this.smartuser + i18n("users.userupdated"));
	} else {
		displayError(parseError(i18n("users.errordeletingaction") + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector("#usertable > .smart-table-selectedrow"));
}

//callback for add action
function actionAdded(){
	if (this.status == 204) {
		displayInfo(this.smartuser + i18n("users.userupdated"));
	} else {
		displayError(parseError(i18n("users.erroraddingaction")+ this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector("#usertable > .smart-table-selectedrow"));
}