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

function displayDialogCenter(divId){
	var w = window.innerWidth;
	var h = window.innerHeight;
	var dialog = document.querySelector("#" + divId);
	
	dialog.style.display="table";
	dialog.style.position="absolute";
	
	var divW = dialog.offsetWidth;
	var divH = dialog.offsetHeight;
	
	var top = h/2 - divH/2 + window.pageYOffset;
	if (top < 0) top = 0;
	var left = w/2 - divW/2 + window.pageXOffset;
	if (left < 0) left = 0;
	dialog.style.top = top + "px" ;
	dialog.style.left = left + "px";
	
	var overlaydiv = document.createElement('div');
	overlaydiv.setAttribute("class", "overlay-widget");
	document.body.appendChild(overlaydiv);
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


function displayURLDialog(url){
	var overlaydiv = document.createElement('div');
	overlaydiv.setAttribute("class", "overlay-widgetlevel2");
	document.body.appendChild(overlaydiv);

	var linktext = document.getElementById('urllink');
	linktext.value = url;
	
	document.getElementById("quickMinSelect").disabled=false;
	var number = document.getElementById("quickMinSelect").value;
	if (number > 0){
		document.getElementById("expiresAfter").disabled=true;
	}else{
		document.getElementById("expiresAfter").disabled=false;
	}
	
	var poselement = document.querySelector("#urlOptionsDialog");
	var pos = getPosition(poselement);
	displayDialogLocation('SharedLinksDialog', pos.x +30, window.pageYOffset + 50);
	
	linktext.focus();
	linktext.select();
}

function resetUrlDialog(){
	document.getElementById("createlinkbutton").style.display = 'block';
	document.getElementById("createdlink").value = "";
	document.getElementById("createdlink").style.display = 'none';
	
	document.getElementById('createcustomlink').style.display = 'none';
	document.getElementById('createcustomlinktitle').style.display = 'block';
}
