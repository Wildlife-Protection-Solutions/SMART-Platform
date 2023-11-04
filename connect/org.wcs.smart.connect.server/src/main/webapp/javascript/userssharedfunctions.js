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


function refreshDesktopUsers(){
	//clear current table
	var objects = document.querySelectorAll("#deskptopuserstable > .desktopuserrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}

	var parent = document.querySelector("#deskptopuserstable");
	var row = document.createElement("div");
	row.className="desktopuserrow";
	row.innerHTML=i18n("users.refreshusertable");
	parent.appendChild(row);
		
 	var oReq = new XMLHttpRequest();
 	oReq.onload = createDesktopUserTable;
 	oReq.open("Get", DESKTOP_USER_URL, true);
 	oReq.send();
}

/* callback that displays all desktop user info */
function createDesktopUserTable(){
	document.querySelector("#desktopdetailinner").style.display="none";
	
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
	var objects = document.querySelectorAll("div.desktopuserrow");
	for (var i = 0; i < objects.length; i++){
		var ele = objects[i];
		ele.parentElement.removeChild(ele);
	}
	
	var parent = document.querySelector("#deskptopuserstable");
 	var users = JSON.parse(this.responseText);
 	for (var i = 0; i < users.length; i ++){
 		var row = tableCreateRow(parent, 
 				[users[i].smartUserId, users[i].caLabel, null, null], 
 				"desktopuserrow " + (i % 2 == 1 ? "smart-table-rowon" : "smart-table-rowoff"));
 		row.dataset.username = users[i].smartUserId;
 		row.dataset.cauuid = users[i].caUuid;
 		row.onclick = showDesktopUserInfo;
 	
 		var editicon = document.createElement("i");
 		editicon.className="fa-regular fa-xl fa-pen-to-square icon-btn-default";
 		editicon.title="edit user";
 		editicon.dataset.username = users[i].smartUserId;
 		editicon.dataset.caUuid = users[i].caUuid;
 		editicon.onclick = getUserEditDetails;
 		row.childNodes[2].appendChild(editicon);
 		
 		var deleteicon = document.createElement("i");
 		deleteicon.className="fa-solid fa-xl fa-xmark icon-btn-default";
 		deleteicon.title="deactivate user";
 		deleteicon.dataset.username = users[i].smartUserId;
 		deleteicon.dataset.caUuid = users[i].caUuid;
 		deleteicon.onclick = deactivateDesktopUser;
 		row.childNodes[3].appendChild(deleteicon);
 	}
}

/* updates the desktop user info section with the current selected user */
function showDesktopUserInfo(){
	if (this == null || this.dataset.username == null) return;
	var username = this.dataset.username;
	var cauuid = this.dataset.cauuid;
	document.querySelector("#desktopdetailinner").style.display="block";
	var currentSelection = document.querySelector("#deskptopuserstable > .smart-table-selectedrow");
	if (currentSelection != null){
		currentSelection.className = currentSelection.className.replace(/(?:^|\s)smart-table-selectedrow(?!\S)/ , '' );
	}
	this.className = this.className + " smart-table-selectedrow";
	var oReq = new XMLHttpRequest();
 	oReq.onload = setDesktopUserDetails;
 	oReq.open("Get", DESKTOP_USER_URL + encodeURIComponent(username) + "?cauuid="+cauuid, true);
 	oReq.send();
}
/* callback for showUserInfo which updates the info */
function setDesktopUserDetails(){
	if (this.status != 200){
		displayError(parseError(i18n("users.couldnotloaduser"), this.responseText));
		return;
	}
	var user = JSON.parse(this.responseText);
	var ele = document.querySelector("#desktopdetailinner");
	
	var html = "<div style='margin-top: 5px'><span class='label-header'>" + i18n("users.usernamelabel") + "</span><span>" + user.smartUserId + "</span><input type='hidden' value='" + user.smartUserId + "' id='edit_username'></div>";
	html += "<div><span class='label-header'>" + i18n("users.idlabel") + "</span><span>" + user.id + "</span></div>";
	html += "<div><span class='label-header'>" + i18n("users.givennamelabel") + "</span><span>" + user.givenName + "</span></div>";
	html += "<div><span class='label-header'>" + i18n("users.familynamelabel") + "</span><span>" + user.familyName + "</span></div>";
	html += "<br/><p>" + i18n("users.calistlabel") + "</p>";
	
	//loop over all their CAs
	
	for (var i = 0; i < user.allCasUserIsIn.length; i ++){
		var ca = user.allCasUserIsIn[i];
		html += "<p><input type='checkbox' class='calist'>";
		html += ca.name + " <input type='hidden' value='" + ca.uuid + "' id='cauuid" + i + "'> <input type='hidden' value='" + ca.name + "' id='calabel" + i + "'></p>";
	}
	html += "<a href='javascript:selectAllCas()'>" + i18n("users.selectAllCas") + "</a>  &nbsp";
	html += "<a href='javascript:selectNoneCas()'>" + i18n("users.selectNoneCas") + "</a></p><br>";
	html += "<p><input type=button class='button checkbutton' disabled value='"+ i18n("users.editSelectedUsers") + "' onClick='javascript:editDesktopUserAcrossCas()'/> &nbsp";
	html += "<input type=button class='button checkbutton' disabled value='"+ i18n("users.deactivateSelectedUsers") + "' onClick='javascript:deactivateDesktopUserAcrossCas()'/>";
	
	
	ele.innerHTML = html;
	
	var elements = document.getElementsByClassName("calist");
	for(i=0; i < elements.length; i++){
		elements[i].onchange = checkForChecks;
	}
}

function checkForChecks(){
	var elements = document.getElementsByClassName("calist");
	var count =0;
	for(i=0; i < elements.length; i++){
		if(elements[i].checked){
			count++;
			break;
		}
	}
	var buttons = document.getElementsByClassName("checkbutton");
	if(count>0){
		for(i=0; i < buttons.length; i++){
				buttons[i].disabled = false;
		}
	}else{
		for(i=0; i < buttons.length; i++){
			buttons[i].disabled = true;
		}
	}
}

/* Deactivate Desktop user */
function deactivateDesktopUser(){
	var username = this.dataset.username;
	var caUuid = this.dataset.caUuid;
	
	displayConfirmDialog(i18n("users.deactivatedesktopuser"), i18n("users.deactivatedesktopwarning") + username +"?  "  , function(){
		hideInfo();

		var oReq = new XMLHttpRequest();
		oReq.onload = desktopUserDeactivated;
		oReq.smartuser=username;
		oReq.open("DELETE", DESKTOP_USER_URL + encodeURIComponent(username) + "?caUuid=" + caUuid, true);
		oReq.send();
	});
	return false;	
}

//callback for deactivate user  
function desktopUserDeactivated() {
	var user = JSON.parse(this.responseText);
	if (this.status == 200) {
		displayInfo(user.smartUserId + i18n("users.userdeactivated"));
	} else {
		displayError(parseError(i18n("users.userdeactivationerror"), this.responseText));
	}
	refreshDesktopUsers();
}


function getUserEditDetails(){
	var username = this.dataset.username;
	var caUuid = this.dataset.caUuid;
	
 	var oReq = new XMLHttpRequest();
 	oReq.onload = showEditDesktopUserDialog;
 	oReq.open("Get", DESKTOP_USER_URL + username + "?cauuid=" + caUuid, true);
 	oReq.send();
 	return false;
} 

function showEditDesktopUserDialog(){
	var user = JSON.parse(this.responseText);

 	document.querySelector("input[name=edit_dca]").value = user.caUuid;
	
	document.querySelector("#editDesktopUserDialog > #dialogerror").innerHTML = "";
	document.querySelector("#editDesktopUserDialog > #dialogerror").style.display = "none";

	
	document.querySelector("input[name=edit_dpassword1]").placeholder = i18n("users.leaveblank");
	document.querySelector("input[name=edit_dpassword1]").value = "";
	document.querySelector("input[name=edit_dpassword2]").placeholder = i18n("users.leaveblank");
 	document.querySelector("input[name=edit_dpassword2]").value = "";
 	document.querySelector("input[name=existing_username]").value = user.smartUserId;
 	document.querySelector("input[name=edit_dusername]").value = user.smartUserId;

 	document.querySelector("input[name=edit_ca_label]").value= user.caLabel;
 	
 	document.querySelector("input[name=edit_familyName]").value = user.familyName;
 	document.querySelector("input[name=edit_givenName]").value = user.givenName;
 	document.querySelector("select[name=edit_gender]").value = user.gender;
 	document.querySelector("input[name=edit_id]").value = user.id

 	//this won't work for users with multiple levels//TODO make sure it doesn't crash it.
 	//document.querySelector("select[name=edit_userLevel]").value = user.userLevelKey;
 	
 	document.querySelector("#newDesktopUserDialog > #dialogerror").style.display = "none";
 	displayDialog('editDesktopUserDialog', 'main');
}

function showNewDesktopUserDialog(){
 	
 	document.querySelector("input[name=dpassword1]").value = "";
 	document.querySelector("input[name=dpassword2]").value = "";
 	document.querySelector("input[name=dusername]").value = "";
 	document.querySelector("select[name=dca]").value = "";
 	document.querySelector("#newDesktopUserDialog > #dialogerror").style.display = "none";
 	displayDialog('newDesktopUserDialog', 'main');
}


//creates a new desktop user
function createNewDesktopUser(){
	var jsonDataArray = makeJsonEmployee("");
	
	if (jsonDataArray == false){
		document.querySelector("#newDesktopUserDialog > #dialogerror").innerHTML = error;
		document.querySelector("#newDesktopUserDialog > #dialogerror").style.display = "block";
		error = "";
		return false;
	}

	//make ajax call
	hideInfo();
	closeDialog('newDesktopUserDialog');
	
	
	for (index = 0; index < jsonDataArray.length; ++index) {
		jsonData = jsonDataArray[index];
		username = jsonData["smartUserId"];
		var oReq = new XMLHttpRequest();
		oReq.onload = desktopUserCreated;
		oReq.open("POST", DESKTOP_USER_URL + encodeURIComponent(username), true);
		oReq.setRequestHeader("Content-type", "application/json");
		oReq.send(JSON.stringify(jsonData));
	}
	
	return false;
}

//callback for creating desktop user 
function desktopUserCreated() {
	if (this.status == 201) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(user.smartUserId  +  i18n("users.accountcreated"));
	} else {
		displayError(parseError(i18n("users.errorcreatinguser"), this.responseText));
	}
	refreshDesktopUsers();
}

function editDesktopUserSubmit(){
		existing_username = document.querySelector("input[name=existing_username]").value
		var jsonDataArray = makeJsonEmployee("edit_");

		hideInfo();

		
		if(jsonDataArray == false){
			document.querySelector("#editDesktopUserDialog > #dialogerror").innerHTML = error;
			document.querySelector("#editDesktopUserDialog > #dialogerror").style.display = "block";
			error="";
			return false;
		}

		closeDialog('editDesktopUserDialog');
		
		var caUuid = document.querySelector("input[name=edit_dca]").value
		//make ajax call

		for (index = 0; index < jsonDataArray.length; ++index) {
			var jsonData = jsonDataArray[index];
			
			var oReq = new XMLHttpRequest();
			oReq.onload = desktopUserEdited;
			oReq.open("PUT", DESKTOP_USER_URL + encodeURIComponent(existing_username) + "?cauuid=" + jsonData["caUuid"], true);
			oReq.setRequestHeader("Content-type", "application/json");
			oReq.send(JSON.stringify(jsonData));
		}
		return false;
}

function desktopUserEdited(){
	if (this.status == 200) {
		//ok
		var user = JSON.parse(this.responseText);
		displayInfo(user.smartUserId  +  i18n("users.accountedited"));
	} else {
		displayError(parseError(i18n("users.erroreditinguser"), this.responseText));
	}
	refreshDesktopUsers();
}

function makeJsonEmployee(prefix){
	var pass1 = document.querySelector("input[name=" + prefix + "dpassword1]").value;
	var pass2 = document.querySelector("input[name=" + prefix + "dpassword2]").value;
	var user = document.querySelector("input[name=" + prefix + "dusername]").value;
	
	var caUuids = [];
	if(prefix=="edit_"){
		caSelect = document.getElementById("edit_multi_dca");
		if(caSelect.style.display == "block"){//if we are editing across numerous CAs, get them all
			for(i=0; i< caSelect.length; i++){
				caUuids.push(caSelect.options[i].value);
			}
		}else{//regular editing of a user in a single CA, put it in a list to make it compatible with other options. 
			var caUuid = document.querySelector("input[name=" + prefix + "dca]").value;
			caUuids.push(caUuid);
		}
		
		caSelect.style.display = "none";//reset the view to the default, single CA only.
		document.querySelector("input[name=edit_ca_label]").style.display = "block";
		
	}else{//creating a new CA, you are allowed to multiselect CAs to create in more than one CA at a time.
		var caUuids = getSelectValues(document.querySelector("select[name=" + prefix + "dca]"));
	}
	
	var familyName= document.querySelector("input[name=" + prefix + "familyName]").value;
	var givenName = document.querySelector("input[name=" + prefix + "givenName]").value;
	var gender = document.querySelector("select[name=" + prefix + "gender]").value;
	
	var id = document.querySelector("input[name=" + prefix + "id]").value;

	var element = document.querySelector("select[name=" + prefix + "userLevel]");
	var userLevel = "";
	if (element != null){
		userLevel = document.querySelector("select[name=" + prefix + "userLevel]").value;
	}
	
	if (user.length < 3 ) {
		error = i18n("settings.usernamelength");
	}else if (pass1.length < 6 && prefix != "edit_"){
		error = i18n("settings.passwordlength");
	}else if (pass1 != pass2 ){
		error = i18n("settings.passwordsdontmatch");
	}else if (caUuids.length == 0 ) {
		error = i18n("settings.carequired");
	}else if (id.length < 4 || id.length > 16) {
		error = i18n("settings.idlength");
	}
	if(error.length > 0)return false;

	var employeeArray = [];
	
	for (index = 0; index < caUuids.length; ++index) {
		caUuid = caUuids[index];
		
		var jsonEmployee = {
			"caUuid" : caUuid,
			"id" : id,
			"givenName" : givenName,
			"familyName" : familyName,
			"gender" : gender,
			"smartUserId" : user,
			"smartPassword" : pass1,
			"userLevelKey" : userLevel
		};
		employeeArray.push(jsonEmployee);
	}
	return employeeArray;
}

function getSelectValues(select) {
	  var result = [];
	  var options = select && select.options;
	  var opt;

	  for (var i=0, iLen=options.length; i<iLen; i++) {
	    opt = options[i];

	    if (opt.selected) {
	      result.push(opt.value || opt.text);
	    }
	  }
	  return result;
}

function selectAllCas(){
	var elements = document.getElementsByClassName("calist");
	for(i=0; i < elements.length; i++){
		elements[i].checked = true;
	}
	checkForChecks();
}

function selectNoneCas(){
	var elements = document.getElementsByClassName("calist");
	for(i=0; i < elements.length; i++){
		elements[i].checked = false;
	}
	checkForChecks();
}

function editDesktopUserAcrossCas(){
	var elements = document.getElementsByClassName("calist");
	caSelect = document.getElementById("edit_multi_dca");
	var i;
    for(i = caSelect.options.length - 1 ; i >= 0 ; i--){
    	caSelect.remove(i);
    }
	var lastcauuid = null
	var count=0;
	for(i=0; i < elements.length; i++){
		if(elements[i].checked){
			count++;
			var option = document.createElement("option");
			lastcauuid = document.getElementById("cauuid" + i).value;
			option.value = lastcauuid; 
			option.text = document.getElementById("calabel" + i).value;
			
			caSelect.add(option)
		}
	}
	if(count == 0){
		return false;
	}
 	document.querySelector("input[name=edit_ca_label]").style.display = "none";
	caSelect.style.display = "block";
	
	var oReq = new XMLHttpRequest();
 	oReq.onload = showEditDesktopUserDialog;
 	oReq.open("Get", DESKTOP_USER_URL + document.getElementById("edit_username").value + "?cauuid=" + lastcauuid, true);
 	oReq.send();
 	return false;
	
}


function deactivateDesktopUserAcrossCas(){
	displayConfirmDialog(i18n("users.deactivatedesktopuser"), i18n("users.deactivatedesktopallwarning") , function(){
		hideInfo();
		var elements = document.getElementsByClassName("calist");
		var username = document.getElementById("edit_username").value;
		for(i=0; i < elements.length; i++){
			if(elements[i].checked){
				cauuid = document.getElementById("cauuid" + i).value;

				var oReq = new XMLHttpRequest();
				oReq.onload = desktopUserDeactivated;
				oReq.open("DELETE", DESKTOP_USER_URL + encodeURIComponent(username) + "?caUuid=" + cauuid, true);
				oReq.send();
			}
		}
		
	});



	return false;
}