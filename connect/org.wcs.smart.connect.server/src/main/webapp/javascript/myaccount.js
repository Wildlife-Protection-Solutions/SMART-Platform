var SHAREDLINKURL = "../api/sharedlink/";
var CAURL = "../api/conservationarea/withdataonly/";


/* configure events on html elements */
window.onload = function(){
	document.getElementById("close").onclick = function(){
		closeDialog('SharedLinksDialog');
	};
	
	//populate the "My Default/Home CA" list
	var oReq = new XMLHttpRequest();
	oReq.onload = populateCaList;
	oReq.open("Get", CAURL + '?includeSpatialBoundaries=false', true);
	oReq.send();
	
	document.getElementById("quickMinSelect").onchange = function(){
		var number = document.getElementById("quickMinSelect").value;
		if (number > 0){
			document.getElementById("expiresAfter").value = number;
			document.getElementById("expiresAfter").disabled=true;
		}else{
			document.getElementById("expiresAfter").disabled=false;
		}
	}
	
	document.getElementById("createtokenbutton").onclick = function(){
		createToken();
	};

}


function populateCaList(){
	if (this.status != 200) {
		var msg = i18n("query.error");
		if (this.status == 401){
			msg += i18n("query.unauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	var parent = document.getElementById('caselect')
	var filterparent = document.getElementById("cafilteroptions");
	
	var cas = JSON.parse(this.responseText);
	for (var i = 0; i < cas.length; i ++){
		var opt = document.createElement('option');
		var label = cas[i].label;
		value = cas[i].uuid;
	    opt.value = value;
	    opt.innerHTML = label;
	    parent.appendChild(opt);
	}
	
	var oReq = new XMLHttpRequest();
	oReq.onload = setHomeCa;
	oReq.open("Get", USER_URL + "getCurrent/", true);
	oReq.send();
}

function setHomeCa(){
	if (this.status != 200) {
		var msg = i18n("query.error");
		if (this.status == 401){
			msg += i18n("query.unauthorized");
		}
		try {
			msg = JSON.parse(this.responseText).error
		} catch (err) {
		}
		displayError(msg);
		return;
	}
	var parent = document.getElementById('caselect')
	
	var users = JSON.parse(this.responseText);
	parent.value = users.homeCaUuid;
}




