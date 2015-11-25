var USER_URL = "../api/connectuser/";
var ACTION_URL = USER_URL + "actions/";
var allActions = null;

/* configure events on html elements */
window.onload = function(){
	//add new user
	document.querySelector("#btnNewUser").onclick=clearAndShowNewUserDialog;
	
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
}

/* loads all user actions from server */
function loadActions(){
	if (allActions != null) return;
	var oReq = new XMLHttpRequest();
	oReq.onload = setActions;
	oReq.open("Get", ACTION_URL, true);
	oReq.send();	
}

/* callback from loadActions to cache user actions */
function setActions(){
	if (this.status != 200){
		//do something with error
	}else{
		allActions = JSON.parse(this.responseText);
		updateActionsDropDown();
	}
}

/* updates the user info section with the current selected user */
function showUserInfo(){
	if (this == null || this.dataset.username == null) return;
	var username = this.dataset.username;
	
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
	var ele = document.querySelector("#userinfo");
	var table = document.querySelector("#actiontable");
	if (table != null){
		table.parentElement.removeChild(table);
	}
	var html = "<div style='margin-top: 5px'><span class='label-header'>" + i18n("users.usernamelabel") + "</span><span>" + user.username + "</span></div>";
	html +="<div style='margin-top: 5px; margin-bottom: 5px'><span class='label-header'>" + i18n("users.emaillabel") + "</span><span>" + user.email + "</span></div>";
	ele.innerHTML = html;
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = setUserActions;
 	oReq.username = user.username;
 	oReq.open("Get", ACTION_URL + encodeURIComponent(user.username), true);
 	oReq.send();
 	
 	loadActions();
}

/* callback for setting user actions */
function setUserActions(){
	var actions = JSON.parse(this.responseText);
	var ele = document.querySelector("#userinfo");
	
	var table = tableCreate();
	table.id = "actiontable";
	ele.appendChild(table);
	
	tableAddHeader(table, [i18n("users.action"), i18n("users.resource"), ""]);
	
	for (var i = 0; i < actions.length; i ++){
		var row = tableCreateRow(table, 
				[actions[i].actionName, actions[i].resourceName, ""], 
				 (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
		
		var deleteicon = document.createElement("a");
		deleteicon.className="delete-icon";
		deleteicon.title="remove action";
		deleteicon.dataset.username = this.username;
		deleteicon.dataset.actionKey = actions[i].actionKey;
		if (actions[i].resource != null){
			deleteicon.dataset.resourceKey = actions[i].resource;
		}
		deleteicon.onclick = deleteAction;
		deleteicon.href="";
		row.childNodes[2].appendChild(deleteicon);
	}
	
	var row = tableCreateRow(table, 
			["", "", ""], "");
	
	var ddactions = document.createElement("select");
	ddactions.id = "actionKey";
	row.childNodes[0].appendChild(ddactions);
	ddactions.setAttribute("onchange", "updateActionResourceDropDown(); return false;");
	
	var ddresources = document.createElement("select");
	ddresources.style.maxWidth = "24em";
	ddresources.id = "actionResourceKey";
	row.childNodes[1].appendChild(ddresources);
	
	updateActionsDropDown();
	
	var addBtn = document.createElement("button");
	addBtn.className="block button top-spacer";
	addBtn.style = "padding: 0px; display: inline;";
	addBtn.innerHTML = "Add";
	addBtn.setAttribute("onClick", "addAction('" + this.username + "');")
	row.childNodes[2].appendChild(addBtn);
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
	var loc = ACTION_URL;
	loc += encodeURIComponent(username);
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
	var loc = ACTION_URL + encodeURIComponent(username) + "/" + encodeURIComponent(selectedActionKey);
	if (selectedResourceKey.length > 0){
		loc += "/" + selectedResourceKey;
	}
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
	showUserInfo.call(document.querySelector(".selecteduser"))
}

//callback for add action
function actionAdded(){
	if (this.status == 204) {
		displayInfo(this.smartuser + " updated");
	} else {
		displayError(parseError(i18n("alert.erroraddingaction")+ this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector(".selecteduser"))
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


