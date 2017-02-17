var SHAREDLINKURL = "../api/sharedlink/";

/* configure events on html elements */
window.onload = function(){
	document.getElementById("createtokenbutton").onclick = function(){
		displayDialogCenter('SharedLinksDialog');
	};
	
	document.getElementById("close").onclick = function(){
		closeDialog('SharedLinksDialog');
	};
	

}


