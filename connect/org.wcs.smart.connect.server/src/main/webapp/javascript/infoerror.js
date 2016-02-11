
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

function formatDate(date){
	
     var seconds = date.getSeconds();
     var minutes = date.getMinutes();
     var hours = date.getHours();
     var month = date.getMonth() + 1;
     
     if ( minutes < 10 ) minutes = "0" + minutes;
     if ( seconds < 10 ) seconds = "0" + seconds;
     if ( hours < 10 ) hours = "0" + hours;
     
     return date.getDate() + "/" + month + "/" + date.getFullYear() + "  " + hours + ":" + minutes + ":" + seconds;
}