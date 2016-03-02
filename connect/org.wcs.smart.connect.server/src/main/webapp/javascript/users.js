var USER_URL = "../api/connectuser/";
var PRIVILEGE_URL = "../api/privileges";

var allActions = null;
var allRoles = null;

/* configure events on html elements */
window.onload = function(){

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
	
	//edit user
	elements = document.querySelectorAll(".edituser");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=showEditUserDialog;
	}
	
	//update user dialog
	document.querySelector("#canceledituser").onclick = function(){closeDialog('editUserDialog');};
	document.querySelector("#edituserform").onsubmit = editUser;
	
	//new user dialog
	document.querySelector("#btnNewUser").onclick=clearAndShowNewUserDialog;
	document.querySelector("#cancelnewuser").onclick = function(){closeDialog('newUserDialog');};
	document.querySelector("#newuserform").onsubmit = createNewUser;

	//new role
	document.querySelector("#btnNewRole").onclick=clearAndShowNewRoleDialog;
	document.querySelector("#cancelnewrole").onclick = function(){closeDialog('newRoleDialog');};
	document.querySelector("#newroleform").onsubmit = createNewRole;

	clearUserInfo();
	
	document.querySelector("#roledetailinner").style.display = "none";
	refreshRolesTable();
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
		displayError(parseError(i18n("users.errorloadingactions"), this.responseText));
	}else{
		allActions = JSON.parse(this.responseText);
		updateActionsDropDown();
	}
}
/* callback from loadRoles to cache user actions */
function setRoles(){
	if (this.status != 200){
		displayError(parseError(i18n("users.errorloadingroles"), this.responseText));
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
	table = document.querySelector("#roletable");
	if (table != null){
		table.parentElement.removeChild(table);
	}
	
	var html = "<div style='margin-top: 5px'><span class='label-header'>" + i18n("users.usernamelabel") + "</span><span>" + user.username + "</span></div>";
	html +="<div style='margin-top: 5px; margin-bottom: 5px'><span class='label-header'>" + i18n("users.emaillabel") + "</span><span>" + user.email + "</span></div>";
	html += "<p>" + i18n("users.permission") + "</p>";
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
	
	document.querySelector("#addRole").setAttribute("onClick", "addRoleToUser('" + this.username + "');")
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
			deleteicon.onclick = deleteRoleUser;
			deleteicon.href="";
			row.childNodes[1].appendChild(deleteicon);
		}	
	}
}

/* updates role info section with the current selected role */
function showRoleInfo(){
	if (this == null || this.dataset.roleid == null) return;
	var roleid = this.dataset.roleid;
	
	var currentSelection = document.querySelector("#allroletable > .smart-table-selectedrow");
	if (currentSelection != null){
		currentSelection.className = currentSelection.className.replace(/(?:^|\s)smart-table-selectedrow(?!\S)/ , '' );
	}
	this.className = this.className + " smart-table-selectedrow";
	var oReq = new XMLHttpRequest();
 	oReq.onload = setRoleDetails;
 	oReq.roleid = roleid;
 	oReq.open("Get", PRIVILEGE_URL + "/roles/" + encodeURIComponent(roleid) + "/action", true);
 	oReq.send();
}

/* callback for setting user actions */
function setRoleDetails(){
	document.querySelector("#roledetailinner").style.display = "block";
	if (this.status != 200){
		displayError(parseError(i18n("users.roleerror"), this.responseText));
		return;
	}
	
	loadActions();
	
	var ele = document.querySelector("#userinfodefaults");
	var table = document.querySelector("#actiontable");
	if (table != null){
		table.parentElement.removeChild(table);
	}
	table = document.querySelector("#roletable");
	if (table != null){
		table.parentElement.removeChild(table);
	}
	
	var rolename ="";
	for (var i = 0; i < allRoles.length; i ++){
		if (allRoles[i].key == this.roleid){
			rolename = allRoles[i].name;
		}
	}
	var html = "<div style='margin-top: 5px'><span class='label-header'>" + i18n("users.rolename") + " </span><span>" + rolename + "</span></div>";
	document.querySelector("#roleinfodefaults").innerHTML = html;
	
	var privis = JSON.parse(this.responseText);
	
	var existing = document.querySelector("#roleactiontable");
	if (existing != null){
		existing.parentElement.removeChild(existing);
	}
	var actionElement = document.querySelector("#roledetailinner");
	
	var actionTable = tableCreate();
	actionTable.id = "roleactiontable";
	actionElement.appendChild(actionTable);
	
	tableAddHeader(actionTable, [i18n("users.action"), i18n("users.resource"), ""]);
	
	document.querySelector("#addRoleAction").setAttribute("onClick", "addActionToRole('" + this.roleid + "');")
	updateActionsDropDown();
	
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
			deleteicon.title="remove action from role";
			deleteicon.dataset.roleid = this.roleid;
			deleteicon.dataset.actionKey = privis[i].key;
			if (privis[i].resource != null){
				deleteicon.dataset.resourceKey = privis[i].resource;
			}
			deleteicon.onclick = deleteActionFromRole;
			deleteicon.href="";
			row.childNodes[2].appendChild(deleteicon);	
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


/* reload roles table */
function refreshRolesTable(){
	//clear current table
	var objects = document.querySelectorAll("div.rolerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#allroletable");
	var row = document.createElement("div");
	row.className="rolerow";
	row.innerHTML= i18n("users.loading");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createRoleTable;
 	oReq.open("Get", PRIVILEGE_URL + "/roles", true);
 	oReq.send();
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
 	
 		var deleteicon = document.createElement("a");
 		deleteicon.className="update-icon";
 		deleteicon.title="edit user";
 		deleteicon.dataset.username = users[i].username;
 		deleteicon.dataset.email = users[i].email;
 		deleteicon.onclick = showEditUserDialog;
 		deleteicon.href="";
 		row.childNodes[2].appendChild(deleteicon);
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete user";
 		deleteicon.dataset.username = users[i].username;
 		deleteicon.onclick = deleteUser;
 		deleteicon.href="";
 		row.childNodes[3].appendChild(deleteicon);
 	}
}


/* callback that displays all user info */
function createRoleTable(){
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
	var objects = document.querySelectorAll("#allroletable > .rolerow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#allroletable");
 	allRoles = JSON.parse(this.responseText);
 	for (var i = 0; i < allRoles.length; i ++){
 		var row = tableCreateRow(parent, 
 				[allRoles[i].name, null, null], 
 				"rolerow " + (i % 2 == 0 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.dataset.roleid = allRoles[i].key;
 		row.onclick = showRoleInfo;
 	
 		var editicon = document.createElement("a");
 		editicon.className="update-icon";
 		editicon.title="edit role";
 		editicon.dataset.roleid = allRoles[i].key;
 		editicon.onclick = clearAndShowEditRoleDialog;
 		editicon.href="";
 		row.childNodes[1].appendChild(editicon);
 		
 		var deleteicon = document.createElement("a");
 		deleteicon.className="delete-icon";
 		deleteicon.title="delete role";
 		deleteicon.dataset.roleid = allRoles[i].key;
 		deleteicon.onclick = deleteRole;
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
 	document.querySelector("#newUserDialog > #dialogerror").style.display = "none";
 	displayDialog('newUserDialog', 'main');
}

/* clears and displays new user dialog */
function clearAndShowNewRoleDialog(){
 	document.querySelector("input[name=rolename]").value = "";
 	document.querySelector("input[name=roleid]").value = "";
 	document.querySelector("#roledialogerror").style.display = "none";
	
 	document.querySelector("#createrolebtn").value="Create Role";
	document.querySelector("#newroleform").onsubmit = createNewRole;
		
 	displayDialog('newRoleDialog', 'main');
}

/* show edit user dialog user */
function showEditUserDialog(){
	var username = this.dataset.username;
	
	document.querySelector("input[name=edit_username_orig]").value = this.dataset.username
 	document.querySelector("input[name=edit_username]").value = this.dataset.username;
 	document.querySelector("input[name=edit_email]").value = this.dataset.email;
 	document.querySelector("#editUserDialog > #dialogerror").style.display = "none";
 	
 	displayDialog('editUserDialog', 'main');
	
 	return false;
}
/* edit user */
function editUser(){
	var username = document.querySelector("input[name=edit_username]").value;
	var email = document.querySelector("input[name=edit_email]").value;
	var usernameorig = document.querySelector("input[name=edit_username_orig]").value;
	
	var jsonData = {
			"username" : username,
			"email" : email,
		};
	
	hideInfo();

	closeDialog('editUserDialog');
	var oReq = new XMLHttpRequest();
	oReq.onload = userEdited;
	oReq.smartuser=usernameorig;
	oReq.open("PUT", USER_URL + encodeURIComponent(usernameorig), true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

function userEdited(){
	if (this.status == 200) {
		displayInfo(this.smartuser + " updated");
	} else {
		displayError(parseError(i18n("users.errorupdatinguser") + this.smartuser, this.responseText));
	}
	refreshUsers();
}

/* delete user */
function deleteUser(){
	var username = this.dataset.username;
	var ok = window.confirm(i18n("users.confirmdeleteuser") + username + "?");
	if (!ok) return false;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = userDeleted;
	oReq.smartuser=username;
	oReq.open("DELETE", USER_URL + encodeURIComponent(username), true);
	oReq.send();
	return false;	
}

/* deletes role */
function deleteRole(){
	var roleId = this.dataset.roleid;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = roleDeleted;
	var loc = PRIVILEGE_URL + "/roles/";
	loc += encodeURIComponent(roleId);
	oReq.open("DELETE", loc, true);
	oReq.send();
	
	return false;	
}

/* delete role from user*/
function deleteRoleUser(){
	var username = this.dataset.username;
	var role = this.dataset.roleKey;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = roleDeletedUser;
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

/* delete action */
function deleteActionFromRole(){
	var roleid = this.dataset.roleid;
	var action = this.dataset.actionKey;
	var resource = this.dataset.resourceKey;
	
	hideInfo();
	
	var loc = PRIVILEGE_URL + "/roles/";
	loc += encodeURIComponent(roleid);
	loc += "/action";
	loc += "/" + encodeURIComponent(action);
	if (resource != null){
		loc += "/" + encodeURIComponent(resource);
	}
	
	var oReq = new XMLHttpRequest();
	oReq.onload = actionDeletedFromRole;
	oReq.roleid = roleid;
	oReq.open("DELETE", loc, true);
	oReq.send();
	return false;	
}

/* add action to role */
function addActionToRole(roleid){
	var ddactions = document.querySelector("#roleActionKey");
	var ddresources = document.querySelector("#roleActionResourceKey");
	
	var selectedActionKey = ddactions.options[ddactions.selectedIndex].value;
	var selectedResourceKey = ddresources.options[ddresources.selectedIndex].value;
	
	hideInfo();
	var oReq = new XMLHttpRequest();
	oReq.onload = actionAddedToRole;
	oReq.roleid = roleid;
	var loc = PRIVILEGE_URL + "/roles/" + encodeURIComponent(roleid) + "/action/" + encodeURIComponent(selectedActionKey);
	if (selectedResourceKey.length > 0){
		loc += "/" + selectedResourceKey;
	}
	oReq.open("POST", loc, true);
	oReq.send();
}

/* add action to user*/
function addAction(username){
	var ddactions = document.querySelector("#actionKey");
	var ddresources = document.querySelector("#actionResourceKey");
	
	var selectedActionKey = ddactions.options[ddactions.selectedIndex].value;
	var selectedResourceKey = ddresources.options[ddresources.selectedIndex].value;
	
	hideInfo();
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
function addRoleToUser(username){
	var ddactions = document.querySelector("#roleKey");
	var selectedRoleKey = ddactions.options[ddactions.selectedIndex].value;
	
	hideInfo();
	
	var oReq = new XMLHttpRequest();
	oReq.onload = roleAddedToUser;
	oReq.smartuser = username;
	var loc = PRIVILEGE_URL + "/user/" + encodeURIComponent(username) + "/role/" + encodeURIComponent(selectedRoleKey);
	oReq.open("POST", loc, true);
	oReq.send();
}


//callback for delete user  
function userDeleted() {
	if (this.status == 200) {
		displayInfo(this.smartuser + i18n("users.userdeleted"));
	} else {
		displayError(parseError(i18n("users.errordeletingaccount") + this.smartuser, this.responseText));
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
		displayInfo(this.smartuser + i18n("users.userupdated"));
	} else {
		displayError(parseError(i18n("users.errordeletingaction") + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector("#usertable > .smart-table-selectedrow"));
}
//callback for delete action 
function actionDeletedFromRole() {
	if (this.status == 204) {
		displayInfo(i18n("users.roleupdated"));
	} else {
		displayError(parseError(i18n("users.deleteactionrole"), this.responseText));
	}
	showRoleInfo.call(document.querySelector("#allroletable > .smart-table-selectedrow"));
}

//callback for delete role 
function roleDeletedUser() {
	if (this.status == 204) {
		displayInfo(this.smartuser + i18n("users.userupdated"));
	} else {
		displayError(parseError(i18n("users.deleteroleuser") + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector("#usertable > .smart-table-selectedrow"));
}

//callback for delete role 
function roleDeleted() {
	allRoles = null;
	if (this.status == 204) {
		displayInfo(i18n("users.roledeleted"));
	} else {
		displayError(parseError(i18n("users.errordeletingrole"), this.responseText));
	}
	refreshRolesTable();
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
//callback for add action
function actionAddedToRole(){
	if (this.status == 204) {
		displayInfo(i18n("users.roleupdated"));
	} else {
		displayError(parseError(i18n("users.erroraddactionrole"), this.responseText));
	}
	showRoleInfo.call(document.querySelector("#allroletable > .smart-table-selectedrow"));
}
//callback for add action
function roleAddedToUser(){
	if (this.status == 204) {
		displayInfo(this.smartuser + i18n("users.userupdated"));
	} else {
		displayError(parseError(i18n("users.erroraddroleuser") + this.smartuser, this.responseText));
	}
	showUserInfo.call(document.querySelector("#usertable > .smart-table-selectedrow"));
}

function clearAndShowEditRoleDialog() {
	var roleid = this.dataset.roleid;
	
	var rolename ="";
	for (var i = 0; i < allRoles.length; i ++){
		if (allRoles[i].key == roleid){
			rolename = allRoles[i].name;
		}
	}
	document.querySelector("input[name=roleid]").value = roleid;
	document.querySelector("input[name=rolename]").value = rolename;
	document.querySelector("#createrolebtn").value="Update Role";
	document.querySelector("#newroleform").onsubmit = updateRole;
	document.querySelector("#roledialogerror").style.display = "none";
	displayDialog('newRoleDialog', 'main');
	return false;
}

//updates a role
function updateRole() {
	var rolename = document.querySelector("input[name=rolename]").value;
	var roleid = document.querySelector("input[name=roleid]").value;
	
	var error = "";
	if (rolename.length == 0 ) {
		error = "The role name cannot be blank.";
	}

	if (error.length > 0){
		document.querySelector("#roledialogerror").innerHTML = error;
		document.querySelector("#roledialogerror").style.display = "block";
		return false;
	}
	
	var jsonData = {
		"name" : rolename
	};

	//make ajax call
	hideInfo();

	closeDialog('newRoleDialog');
	var oReq = new XMLHttpRequest();
	oReq.onload = roleUpdated;
	oReq.open("PUT", PRIVILEGE_URL + "/roles/" + roleid, true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

//creates a new role
function createNewRole() {
	var rolename = document.querySelector("input[name=rolename]").value;
	
	var error = "";
	if (rolename.length == 0 ) {
		error = i18n("users.namerequired");
	}

	if (error.length > 0){
		document.querySelector("#roledialogerror").innerHTML = error;
		document.querySelector("#roledialogerror").style.display = "block";
		return false;
	}
	
	var jsonData = {
		"name" : rolename
	};

	//make ajax call
	hideInfo();

	closeDialog('newRoleDialog');
	var oReq = new XMLHttpRequest();
	oReq.onload = roleCreated;
	oReq.open("POST", PRIVILEGE_URL + "/roles", true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
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
		document.querySelector("#newUserDialog > #dialogerror").innerHTML = error;
		document.querySelector("#newUserDialog > #dialogerror").style.display = "block";
		return false;
	}
	
	var jsonData = {
		"username" : user,
		"email" : email,
		"password" : pass1
	};

	//make ajax call
	hideInfo();

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
		displayInfo(user.username + i18n("users.accountcreated"));
	} else {
		displayError(parseError(i18n("users.errorcreatinguser"), this.responseText));
	}
	refreshUsers();
}

//callback for creating user 
function roleCreated() {
	allRoles = null;
	
	if (this.status == 201) {
		displayInfo(i18n("users.newrole"));
	} else {
		displayError(parseError(i18n("users.newroleerror"), this.responseText));
	}
	refreshRolesTable();
}
function roleUpdated() {
	allRoles = null;
	
	if (this.status == 201) {
		displayInfo(i18n("users.roleupdated"));
	} else {
		displayError(parseError("", this.responseText));
	}
	refreshRolesTable();
}
