
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

function createSharedLink(){
	var url = generateRelativeUrl(RELATIVEQUERYLINKURL);
	var expiresAfter = document.getElementById("expiresAfter").value;
	var jsonData = {"url": url,
					"expiresAfter": expiresAfter}
	
	var oReq = new XMLHttpRequest();
	oReq.onload = linkCreated;
	oReq.open("POST", SHARED_LINK_URL, true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

function createToken(){
	var expiresAfter = document.getElementById("expiresAfter").value;
	var allowedIp = document.getElementById("allowedIp").value;
	var jsonData = {"expiresAfter": expiresAfter, "allowedIp": allowedIp}
	
	var oReq = new XMLHttpRequest();
	oReq.onload = tokenCreated;
	oReq.open("POST", SHARED_LINK_URL + "token/", true);
	oReq.setRequestHeader("Content-type", "application/json");
	oReq.send(JSON.stringify(jsonData));
	return false;
}

function linkCreated(){
	var link = JSON.parse(this.responseText);
	var status = this.status;
	if(status != 200){
		if(status == 401){
			document.getElementById("createdlink").value = i18n("sharedlinksfunctions.sessiontimeout")  + link.error;
		}else{
			document.getElementById("createdlink").value = i18n("sharedlinksfunctions.linkerror") + link.error;
		}
	}else{
		document.getElementById("createdlink").value = resolve(SHAREDLINKSERVLETURL) + "?uuid=" + link.uuid;
	}
	document.getElementById("createlinkbutton").style.display = "none";
	document.getElementById("createdlink").style = "display: block;";
	document.getElementById("createdlink").select();
	
	document.getElementById("quickMinSelect").disabled=true;
	document.getElementById("expiresAfter").disabled=true;
	
}


function tokenCreated(){
	var link = JSON.parse(this.responseText);
	var status = this.status;
	if(status != 200){
		if(status == 401){
			document.getElementById("createdlink").value = i18n("sharedlinksfunctions.sessiontimeout")  + link.error;
		}else{
			document.getElementById("createdlink").value = i18n("sharedlinksfunctions.linkerror") + link.error;
		}
	}else{
		document.getElementById("createdlink").value = "&token=" + link.uuid;
	}
	document.getElementById("createtokenbutton").style.display = "none";
	document.getElementById("createdlink").style = "display: block;";
	document.getElementById("createdlink").select();
	
	document.getElementById("quickMinSelect").disabled=true;
	document.getElementById("expiresAfter").disabled=true;
	
}