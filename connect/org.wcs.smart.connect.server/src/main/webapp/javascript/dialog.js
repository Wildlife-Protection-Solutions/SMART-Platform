
function displayDialog(divId, posId){
	var dialog = document.querySelector("#" + divId);
	dialog.style.display = "table";
	dialog.style.position = "absolute";
	var poselement = document.querySelector("#" + posId);
	var pos = getPosition(poselement);
	dialog.style.top = pos.y + "px";
	dialog.style.left = pos.x + "px";
	
	var overlaydiv = document.createElement('div');
	overlaydiv.setAttribute("class", "overlay-widget");
	document.body.appendChild(overlaydiv);
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