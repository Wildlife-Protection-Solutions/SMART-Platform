/*
 * Displays a confirm dialog with cancel and ok buttons with
 * the given title and messsage.
 * 
 * If ok pressed then the function onOkay is executed.
 *  
 */
function displayConfirmDialog(title, message, onOkay){
	var dialogDiv = document.createElement('div');
	
	dialogDiv.setAttribute("class", "dialog");
	dialogDiv.style.display="none";
	dialogDiv.id = "smartconfirmdialog";
	
	var titleDiv = document.createElement('div');
	titleDiv.setAttribute("class", "dialog-title");
	titleDiv.innerHTML = title;
	dialogDiv.appendChild(titleDiv);
	
	var mainDiv = document.createElement('div');
	mainDiv.innerHTML = message;
	dialogDiv.appendChild(mainDiv);
	
	var buttonDiv = document.createElement('div');
	buttonDiv.style.textAlign="center";
	dialogDiv.appendChild(buttonDiv);
	
	var okButton = document.createElement('input')
	okButton.type = "button";
	okButton.setAttribute("class", "button top-spacer");
	okButton.value = "OK";
	okButton.style.paddingLeft = "20px";
	okButton.style.paddingRight = "20px";
	
	var cancelButton = document.createElement('input')
	cancelButton.style.marginLeft = "5px";
	cancelButton.type = "button";
	cancelButton.setAttribute("class", "button top-spacer");
	cancelButton.value = "CANCEL";
	
	buttonDiv.appendChild(okButton);
	buttonDiv.appendChild(cancelButton);
	
	
	okButton.onclick = function(){
		closeDialog(dialogDiv.id);
		document.body.removeChild(dialogDiv);
		onOkay();
	}
	cancelButton.onclick = function(){
		closeDialog(dialogDiv.id);
		document.body.removeChild(dialogDiv);
	}
	document.body.appendChild(dialogDiv);
	
	displayDialogLocation(dialogDiv.id, window.innerWidth / 2 - 150 + window.pageXOffset,   ((window.innerHeight / 2)) - 100 + window.pageYOffset);
}

/*
 * Displays the dialog represented by divId at
 * the x, y position
 */
function displayDialogLocation(divId, x, y){
	var dialog = document.querySelector("#" + divId);
	dialog.style.display = "table";
	dialog.style.position = "absolute";
	
	dialog.style.top = y + "px";
	dialog.style.left = x + "px";
	
	var overlaydiv = document.createElement('div');
	overlaydiv.setAttribute("class", "overlay-widget");
	document.body.appendChild(overlaydiv);
}

/*
 * Displays the dialog represented by divId as a position
 * relative to the element represented by posId
 */
function displayDialog(divId, posId){
	var poselement = document.querySelector("#" + posId);
	var pos = getPosition(poselement);
	displayDialogLocation(divId, pos.x  + window.pageXOffset, pos.y + window.pageYOffset);
}

function closeDialog(divId){
	document.querySelector("#"+ divId).style.display = "none";
	
	var overlaydiv = document.querySelector(".overlay-widget");
	overlaydiv.parentNode.removeChild(overlaydiv);
}

//source: http://www.kirupa.com/html5/get_element_position_using_javascript.htm
function getPosition(element) {
    var xPosition = 0;
    var yPosition = 0;
  
    while(element) {
        xPosition += (element.offsetLeft - element.scrollLeft + element.clientLeft);
        yPosition += (element.offsetTop - element.scrollTop + element.clientTop);
        element = element.offsetParent;
    }
    return { x: xPosition, y: yPosition };
}