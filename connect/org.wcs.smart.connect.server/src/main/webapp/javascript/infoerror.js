//hide error message
function hideError() {
	document.querySelector("#error").style.display = "none";
}
//displays error message
function displayError(msg) {
	document.querySelector("#error").style.display = "block";
	document.querySelector("#error").innerHTML = msg;
}

function hideInfo(){
	document.querySelector("#message").style.display = "none";
}

function displayInfo(msg){
	document.querySelector("#message").style.display = "block";
	document.querySelector("#message").innerHTML = msg;
}

/*
 * parses SMART standard JSON error response and combines
 * with msg to make an error message 
 */
function parseError(msg, json){
	var error = msg;
	if (json != null && json.length > 0){
		try{
			error = error + ": " + JSON.parse(json).error;
		}catch(err){}
	}
	return error;
}