var USER_URL = "../api/connectuser/";
var PRIVILEGE_URL = "../api/privileges";

var allActions = null;
var allRoles = null;

/* configure events on html elements */
window.onload = function(){
	//add new user
	document.querySelector("#btnNewUser").onclick=clearAndShowNewUserDialog;
	
	document.querySelector("#roletab").onclick=function(){showTab("roletab");}
	document.querySelector("#actiontab").onclick=function(){showTab("actiontab");}
	showTab("roletab");
	
	document.querySelector("#users").onclick=function(){showTab("users");}
	document.querySelector("#userroles").onclick=function(){showTab("userroles");}
	showTab("users");
	
	//show user info
	var elements = document.querySelectorAll(".smartuser");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=showUserInfo;
	}
	
	//delete user
	elements = document.querySelectorAll(".deleteuser");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=deleteUser;
	}
	
	//new user dialog
	document.querySelector("#cancelnewuser").onclick = function(){
		closeDialog('newUserDialog');
	};
	document.querySelector("#newuserform").onsubmit = createNewUser;
	clearUserInfo();
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

/* loads all user actions from server */
function loadActions(){
	if (allActions != null) return;
	var oReq = new XMLHttpRequest();
	oReq.onload = setActions;
	oReq.open("Get", PRIVILEGE_URL + "/actions", true);
	oReq.send();	
}

/* loads all user actions from server */
function loadRoles(){
	if (allRoles != null) return;
	var oReq = new XMLHttpRequest();
	oReq.onload = setRoles;
	oReq.open("Get", PRIVILEGE_URL + "/roles", true);
	oReq.send();	
}

/* callback from loadActions to cache user actions */
function setActions(){
	if (this.status != 200){
		//TODO: do something with error
	}else{
		allActions = JSON.parse(this.responseText);
		updateActionsDropDown();
	}
}
/* callback from loadRoles to cache user actions */
function setRoles(){
	if (this.status != 200){
		//TODO: do something with error
	}else{
		allRoles = JSON.parse(this.responseText);
		updateRolesDropDown();
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
	var currentSelection = document.querySelector(".selecteduser");
	if (currentSelection != null){
		currentSelection.className = currentSelection.className.replace(/(?:^|\s)selecteduser(?!\S)/ , '' );
	}
	this.className = this.className + " selecteduser";
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
	table = document.querySelector("#roletable");
	if (table != null){
		table.parentElement.removeChild(table);
	}
	
	var html = "<div style='margin-top: 5px'><span class='label-header'>" + i18n("users.usernamelabel") + "</span><span>" + user.username + "</span></div>";
	html +="<div style='margin-top: 5px; margin-bottom: 5px'><span class='label-header'>" + i18n("users.emaillabel") + "</span><span>" + user.email + "</span></div>";
	html += "<p>Premissions can be provided through either roles or actions.  Roles are groups of actions and can be reused between users.  Actions are specific to this user.</p>";
	ele.innerHTML = html;
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = setUserPrivileges;
 	oReq.username = user.username;
 	oReq.open("Get", PRIVILEGE_URL + "/user/" + encodeURIComponent(user.username), true);
 	oReq.send();
 	
 	loadActions();
 	loadRoles();
}

/* callback for setting user actions */
function setUserPrivileges(){
	var privis = JSON.parse(this.responseText);
	
	var actionElement = document.querySelector("#actiontab_body");
	var roleElement = document.querySelector("#roletab_body");
	
	var actionTable = tableCreate();
	var roleTable = tableCreate();
	
	actionTable.id = "actiontable";
	roleTable.id = "roletable";
	
	actionElement.appendChild(actionTable);
	roleElement.appendChild(roleTable);
	
	tableAddHeader(actionTable, [i18n("users.action"), i18n("users.resource"), ""]);
	tableAddHeader(roleTable, ["Role Name", ""]);
	
	document.querySelector("#addRole").setAttribute("onClick", "addRole('" + this.username + "');")
	document.querySelector("#addAction").setAttribute("onClick", "addAction('" + this.username + "');")
	
	updateActionsDropDown();
	updateRolesDropDown();
	
	var actionCnt = 1;
	var roleCnt = 0;
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
			
		}else if (privis[i].type.toUpperCase() == "ROLE"){
			var row = tableCreateRow(roleTable, 
					[privis[i].name, ""], 
					 (roleCnt % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
			roleCnt++;
			var deleteicon = document.createElement("a");
			deleteicon.className="delete-icon";
			deleteicon.title="remove action";
			deleteicon.dataset.username = this.username;
			deleteicon.dataset.roleKey = privis[i].key;
			deleteicon.onclick = deleteRole;
			deleteicon.href="";
			row.childNodes[1].appendChild(deleteicon);
		}	
	}
}

/* update actions drop down values */
function updateRolesDropDown(){
	var droles = document.querySelector("#roleKey");
	if (droles == null) return;
	if (droles != null){
		while(droles.lastChild){
			droles.removeChild(droles.lastChild);
		}
	}
	if (allRoles == null) return;
	
	for (var i = 0; i < allRoles.length; i ++){
		var op = document.createElement("option");
		op.value=allRoles[i].key;
		op.innerHTML = allRoles[i].name;
		droles.appendChild(op);
	}
	droles.selectedIndex = 0;
}

/* update actions drop down values */
function updateActionsDropDown(){
	var ddactions = document.querySelector("#actionKey");
	if (ddactions == null) return;
	if (ddactions != null){
		while(ddactions.lastChild){
			ddactions.removeChild(ddactions.lastChild);
		}
	}
	ddactions.setAttribute("onchange", "updateActionResourceDropDown(); return false;");
	
	var ddresources = document.querySelector("#actionResourceKey");
	if (ddresources != null){
		while(ddresources.lastChild){
			ddresources.removeChild(ddresources.lastChild);
		}
	}
	if (allActions == null) return;
	
	for (var i = 0; i < allActions.length; i ++){
		var op = document.createElement("option");
		op.value=allActions[i].key;
		op.innerHTML = allActions[i].name;
		ddactions.appendChild(op);
	}
	ddactions.selectedIndex = 0;
	updateActionResourceDropDown();
}

/* update the resources drop down based on the action selection */
function updateActionResourceDropDown(){
	var ddresources = document.querySelector("#actionResourceKey");
	if (ddresources != null){
		while(ddresources.lastChild){
			ddresources.removeChild(ddresources.lastChild);
		}
	}
	
	var ddactions = document.querySelector("#actionKey");
	var ddresources = document.querySelector("#actionResourceKey");
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

/* reload users table */
function refreshUsers(){
	//clear current table
	var objects = document.querySelectorAll("div.userrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("div.usertable");
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
		var msg = i18n("alert.errorlabel");
		if (this.status == 401){
			msg += i18n("alert.unathorized");
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
	
	var parent = document.querySelector("div.usertable");
 	var users = JSON.parse(this.responseText);
 	for (var i = 0; i < users.length; i ++){
 		var row = tableCreateRow(parent, 
 				[users[i].username, users[i].email, null], 
 				"userrow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.dataset.username = users[i].username;
 		row.onclick = showUserInfo;
 	
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete user";
 		deleteicon.dataset.username = users[i].username;
 		deleteicon.onclick = deleteUser;
 		deleteicon.href="";
 		row.childNodes[2].appendChild(deleteicon);
 	}
}

/* clears and displays new user dialog */
function clearAndShowNewUserDialog(){
 	document.querySelector("input[name=password1]").value = "";
 	document.querySelector("input[name=password2]").value = "";
 	document.querySelector("input[name=username]").value = "";
 	document.querySelector("input[name=email]").value = "";
 	document.querySelector("#dialogerror").style.display = "none";
 	displayDialog('newUserDialog', 'main');
}

/* delete user */
function deleteUser(){
	var username = this.dataset.username;
	var ok = window.confirm(i18n("alert.confirmdeleteuser") + username + "?");
	if (!ok) return false;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = userDeleted;
	oReq.smartuser=username;
	oReq.open("DELETE", USER_URL + encodeURIComponent(username), true);
	oReq.send();
	return false;	
}
/* delete role */
function deleteRole(){
	var username = this.dataset.username;
	var role = this.dataset.roleKey;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = roleDeleted;
	oReq.smartuser = username;
	var loc = PRIVILEGE_URL + "/user/";
	loc += encodeURIComponent(username);
	loc += "/role"; 
	loc += "/" + encodeURIComponent(role);
	oReq.open("DELETE", loc, true);
	oReq.send();
	
	return false;	
}

/* delete action */
function deleteAction(){
	var username = this.dataset.username;
	var action = this.dataset.actionKey;
	var resource = this.dataset.resourceKey;
	
	hideInfo();
	hideError();
	
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

/* add action */
function addAction(username){
	var ddactions = document.querySelector("#actionKey");
	var ddresources = document.querySelector("#actionResourceKey");
	
	var selectedActionKey = ddactions.options[ddactions.selectedIndex].value;
	var selectedResourceKey = ddresources.options[ddresources.selectedIndex].value;
	
	hideInfo();
	hideError();
	var oReq = new XMLHttpRequest();
	oReq.onload = actionAdded;
	oReq.smartuser = username;
	var loc = PRIVILEGE_URL + "/user/" + encodeURIComponent(username) + "/action/" + encodeURIComponent(selectedActionKey);
	if (selectedResourceKey.length > 0){
		loc += "/" + selectedResourceKey;
	}
	oReq.open("POST", loc, true);
	oReq.send();
}


/* add role */
function addRole(username){
	var ddactions = document.querySelector("#roleKey");
	var selectedRoleKey = ddactions.options[ddactions.selectedIndex].value;
	
	hideInfo();
	hideError();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = roleAdded;
	oReq.smartuser = username;
	var loc = PRIVILEGE_URL + "/user/" + encodeURIComponent(username) + "/role/" + encodeURIComponent(selectedRoleKey);
	oReq.open("POST", loc, true);
	oReq.send();
}


//callback for delete user  
function userDeleted() {
	if (this.status == 200) {
		displayInfo(this.smartuser + " deleted");
	} else {
		displayError(parseError(i18n("alert.errordeletingaccount") + this.smartuser, this.responseText));
	}
	refreshUsers();
	
	//if delete the logged in user; refresh page to auto logout
	var currentUser = document.querySelector("#userlogin");
	if (currentUser != null && currentUser.dataset.username != null && this.smartuser === currentUser.dataset.username){
		location.reload(true);
	} 
}

//callback for delete action 
function actionDeleted() {
	if (this.status == 204) {
		displayInfo(this.smartuser + " updated");
	} else {
		displayError(parseError(i18n("alert.errordeletingaction") + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector(".selecteduser"));
}
//callback for delete role 
function roleDeleted() {
	if (this.status == 204) {
		displayInfo(this.smartuser + " updated");
	} else {
		displayError(parseError("Error deleting role for " + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector(".selecteduser"));
}

//callback for add action
function actionAdded(){
	if (this.status == 204) {
		displayInfo(this.smartuser + " updated");
	} else {
		displayError(parseError(i18n("alert.erroraddingaction")+ this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector(".selecteduser"));
}
//callback for add action
function roleAdded(){
	if (this.status == 204) {
		displayInfo(this.smartuser + " updated");
	} else {
		displayError(parseError("Error adding role for user " + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector(".selecteduser"));
}
//creates a new user
function createNewUser() {
	var pass1 = document.querySelector("input[name=password1]").value;
	var pass2 = document.querySelector("input[name=password2]").value;
	var user = document.querySelector("input[name=username]").value;
	var email = document.querySelector("input[name=email]").value;
	
	var error = "";
	if (user.length == 0 ) {
		error = i18n("settings.usernamerequired");
	}else if (pass1.length == 0){
		error = i18n("settings.passwordrequired");
	}else if (pass1 != pass2){
		error = i18n("settings.passwordsdontmatch");
	}

	if (error.length > 0){
		document.querySelector("#dialogerror").innerHTML = error;
		document.querySelector("#dialogerror").style.display = "block";
		return false;
	}
	
	var jsonData = {
		"username" : user,
		"email" : email,
		"password" : pass1
	};

	//make ajax call
	hideError();
	hideInfo();
	document.querySelector("#message").style.display = "none";

	closeDialog('newUserDialog');
	var oReq = new XMLHttpRequest();
	oReq.onload = userCreated;
	oReq.open("POST", USER_URL + encodeURIComponent(user), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

//callback for creating user 
function userCreated() {
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(user.username + " account created");
	} else {
		displayError(parseError(i18n("users.errorcreatinguser"), this.responseText));
	}
	refreshUsers();
}


