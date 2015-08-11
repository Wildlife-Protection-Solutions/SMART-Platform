var STYLE_URL = "../api/connectstyle/";
var ACTION_URL = STYLE_URL + "actions/";
var allActions = null;



/* configure events on html elements */
window.onload = function(){
	//add new user
	document.querySelector("#btnNewStyle").onclick=clearAndShowNewStyleDialog;
	if(numStyles > 0){
		document.getElementById("btnNewStyle").style.display = "none";
	}
	//delete style
	elements = document.querySelectorAll(".deleteStyle");
	for (var i = 0; i < elements.length; i ++){
		elements[i].onclick=deleteStyle;
	}
	
	//new style dialog
	document.querySelector("#cancelNewStyle").onclick = function(){
		closeDialog('newStyleDialog');
	};
	
	document.querySelector("#newStyleForm").onsubmit = createNewStyle;
}


/* clears and displays new user dialog */
function clearAndShowNewStyleDialog(){
 	document.querySelector("input[name=style_id]").value = "";
 	document.querySelector("#dialogerror").style.display = "none";
 	displayDialog('newStyleDialog', 'main');
}

/* delete user */
function deleteUser(){
	var username = this.dataset.username;
	var ok = window.confirm("Are you sure you want to delete the selected style?");
	if (!ok) return;
	
	hideInfo();
	hideError();
	
/*	
 * TODO - fill out proper URL etc
 * 
 * var oReq = new XMLHttpRequest();
	oReq.onload = userDeleted;
	oReq.smartuser=username;
	oReq.open("DELETE", USER_URL + encodeURIComponent(username), true);
	oReq.send();
	return false;
	
		*/
}



//callback for delete user  
function userDeleted() {
	if (this.status == 200) {
		displayInfo(this.smartuser + " deleted");
	} else {
		displayError(parseError("Error deleting account " + this.smartuser, this.responseText));
	}
	refreshUsers();
	
	//if delete the logged in user; refresh page to auto logout
	var currentUser = document.querySelector("#userlogin");
	if (currentUser != null && currentUser.dataset.username != null && this.smartuser === currentUser.dataset.username){
		location.reload(true);
	} 
}


//creates a new Style
function createNewStyle() {
	var pass1 = document.querySelector("input[name=password1]").value;
	var pass2 = document.querySelector("input[name=password2]").value;
	var user = document.querySelector("input[name=username]").value;
	var email = document.querySelector("input[name=email]").value;
	
	var error = "";
	if (user.length == 0 ) {
		error = "Username required";
	}else if (pass1.length == 0){
		error = "Password required";
	}else if (pass1 != pass2){
		error = "Passwords do not match";
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
		displayError(parseError("Error creating account", this.responseText));
	}
	refreshUsers();
}


