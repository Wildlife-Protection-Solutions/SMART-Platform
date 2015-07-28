
//Write parameters into javascript variables


function initializePage(){
	//Hide header/footer and menu if mobile parameter is set:
	if(mobile == "true"){
		document.getElementById('mainheader').style.display = 'none';
		document.getElementById('verticalmenu').style.display = 'none';
		document.getElementById('footerid').style.display = 'none';
	}
	

	//initialize the map
	var map = new L.Map('map', {center: new L.LatLng(-7.5, 34.44), zoom: 8});
    var googleLayer = new L.Google('ROADMAP');
    map.addLayer(googleLayer);
    
	/*
	L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
	    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
	    maxZoom: 18,
	    id: 'jeffloun.mp3jogfm',
	    accessToken: 'pk.eyJ1IjoiamVmZmxvdW4iLCJhIjoiOTYyMGFkZDk5ZWM2ZDQ5NDc5Njc2Y2ZlOGM4YjQ1YWIifQ.R715pq8aRAM9hRdGcy10Xg'
	}).addTo(map);
	*/
	
	/*var testwms = L.tileLayer.wms("https://editor.giscloud.com/wms/bdd66dd4ade33e6b69aed41b64b2b294", {
	    layers: '1084716:canada_major_lakes',
	    format: 'image/png',
	    transparent: true,
	    attribution: "giscloud.com"
	});
	testwms.addTo(map);
	*/
    
    
    //intialize the tab styles
    settab(tab);
	
}

function settab(tab){
	remove_all_tab_classes();
	switch(tab){
		case 2:
			document.getElementById('tab2').className += "selectedTab";
			document.getElementById('tab2text').className += "selectedTab";

			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab3').className += "unselectedTab";
			document.getElementById('tab3text').className += "unselectedTab";
			break;
		case 3:
			document.getElementById('tab3').className += "selectedTab";
			document.getElementById('tab3text').className += "selectedTab";
			
			document.getElementById('tab1').className += "unselectedTab";
			document.getElementById('tab1text').className += "unselectedTab";
			document.getElementById('tab2').className += "unselectedTab";
			document.getElementById('tab2text').className += "unselectedTab";
			break;
		default:
			document.getElementById('tab1').className += "selectedTab";
			document.getElementById('tab1text').className += "selectedTab";
			
			document.getElementById('tab2').className += "unselectedTab";
			document.getElementById('tab2text').className += "unselectedTab";
			document.getElementById('tab3').className += "unselectedTab";
			document.getElementById('tab3text').className += "unselectedTab";
	}
}

function remove_all_tab_classes(){
	remove_class("tab1", "selectedTab");	
	remove_class("tab2", "selectedTab");
	remove_class("tab3", "selectedTab");
	remove_class("tab1text", "selectedTab");
	remove_class("tab2text", "selectedTab");
	remove_class("tab3text", "selectedTab");
	remove_class("tab1", "unselectedTab");
	remove_class("tab2", "unselectedTab");
	remove_class("tab3", "unselectedTab");
	remove_class("tab1text", "unselectedTab");
	remove_class("tab2text", "unselectedTab");
	remove_class("tab3text", "unselectedTab");
}

function remove_class(id, classname){
	var regex = new RegExp("(?:^|\s)" + classname + "(?!\S)", "g");
	document.getElementById(id).className = document.getElementById(id).className.replace( regex , '' )
}