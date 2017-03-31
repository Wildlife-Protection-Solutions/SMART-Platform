var SHAREDLINKURL = "../api/sharedlink/";


/* configure events on html elements */
window.onload = function(){
	document.getElementById("close").onclick = function(){
		closeDialog('SharedLinksDialog');
	};
	
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





