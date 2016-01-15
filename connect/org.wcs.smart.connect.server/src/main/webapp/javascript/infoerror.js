
//displays error message
function displayError(msg) {
	var element = document.querySelector("#message");
	element.style.display = "block";
	element.innerHTML = msg;
	element.className = "errorsection";
}

function hideInfo(){
	document.querySelector("#message").style.display = "none";
}

function displayInfo(msg){
	var element = document.querySelector("#message");
	element.style.display = "block";
	element.innerHTML = msg;
	element.className = "msgsection";
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