//gets the styles from the database and sets them on the page.
function setStyle(isLoggedIn){

//set images
	var header = document.getElementById("mainheader");
	header.style.backgroundImage = "url('../getImage?locationId=1')";
	document.body.style.backgroundImage = "url('../getImage?locationId=2')";
	var login = document.getElementById("login_right");
	if(login != null){
		login.style.backgroundImage = "url('../getImage?locationId=3')";
	}
	
	if(isLoggedIn){
		var oReq = new XMLHttpRequest();
		oReq.onload = styleReceived;
		oReq.open("Get", "../api/connectstyle/", true);
		oReq.send();
	}
}


//callback for setting the styles 
function styleReceived() {
	var header = document.getElementById("mainheader");
	if (this.status == 200) {
		//request response OK
		var style = JSON.parse(this.responseText);

		header.style.cssText = style.headerStyle;
		header.innerHTML = style.serverName;
		document.body.style.cssText = style.bodyStyle;
		document.getElementById("footerid").innerHTML = style.footerText;
	}

	header.style.backgroundImage = "url('../getImage?locationId=1')";
	document.body.style.backgroundImage = "url('../getImage?locationId=2')";
}
