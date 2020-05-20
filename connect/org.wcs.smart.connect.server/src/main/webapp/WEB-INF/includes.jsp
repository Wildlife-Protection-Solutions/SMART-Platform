<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="org.wcs.smart.connect.i18n.web_messages" />
<meta charset="UTF-8" />
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/css/smart.css"/>
<link href="//fonts.googleapis.com/css?family=Open+Sans" rel="stylesheet" type="text/css">
<link href="//fonts.googleapis.com/css?family=Crimson+Text" rel="stylesheet" type="text/css">
<link href="//fonts.googleapis.com/css?family=Allerta" rel="stylesheet" type="text/css">

<link rel="shortcut icon" href="${pageContext.request.contextPath}/css/images/smart_fav_icon.png"> 

<!--  always include english as this is the fallback language -->
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/i18n/labels_en.js"></script>

<!-- only include the file of the request locale -->
<c:set var="supportedLang" value="es,fr,hi,in,km,lo,ms,ru,th,vi,zh" />
<c:forEach var="item" items="${supportedLang}">
  <c:if test="${item eq pageContext.request.locale.language}">
    <script type="text/javascript" src="${pageContext.request.contextPath}/javascript/i18n/labels_${pageContext.request.locale.language}.js"></script>
  </c:if>
</c:forEach>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/i18n.js"></script>


<script type="text/javascript">

var shpValues = ["entityobservation", "entitywaypoint","intelligencerecord",  "surveymission",
    "surveymissiontrack", "observationobservation", "observationwaypoint", 
    "patrolobservation", "patrolquery", "patrolwaypoint", 
    "surveyobservation", "surveywaypoint",
    "assetobservation", "assetwaypoint"
    ];

var tifValues = ["entitygrid", "observationgrid","patrolgrid",  "surveygrid"];

var menuOpen = true;
//have to call this in each page since I can't overwrite the onload function (again) and the menu doesn't exist yet if you just run it right here.
function menuCheckOnload(){
	var menuState = readCookie("smartMenuState");
	if(menuState == "show"){
		var items = document.getElementsByClassName("textMenu");
		for (i = 0; i < items.length; i++) {
	    	items[i].style.display = "inline-block";
		}
		menuOpen = true;
		//document.getElementById("verticalmenu").style.minWidth = "180px";
	}else{
		var items = document.getElementsByClassName("textMenu");
		for (i = 0; i < items.length; i++) {
	    	items[i].style.display = "none";
		}
		menuOpen = false;
		//document.getElementById("verticalmenu").style.minWidth = "0px";
	}
}

function hamburgerMenu(){
	var newState = true;
	if(menuOpen==true){
		writeCookie("smartMenuState","hidden",7);
		newState = "none";
		menuOpen = false;
		//document.getElementById("verticalmenu").style.minWidth = "0px";
	}else{
		writeCookie("smartMenuState","show",7);
		newState = "inline-block";
		menuOpen = true;
		//document.getElementById("verticalmenu").style.minWidth = "180px";
	}
	
	var items = document.getElementsByClassName("textMenu");
	for (i = 0; i < items.length; i++) {
	    items[i].style.display = newState;
	}

}


function writeCookie(name,value,days) {
    var date, expires;
    if (days) {
        date = new Date();
        date.setTime(date.getTime()+(days*24*60*60*1000));
        expires = "; expires=" + date.toGMTString();
            }else{
        expires = "";
    }
    document.cookie = name + "=" + value + expires + "; path=/";
}

function readCookie(name) {
    var i, c, ca, nameEQ = name + "=";
    ca = document.cookie.split(';');
    for(i=0;i < ca.length;i++) {
        c = ca[i];
        while (c.charAt(0)==' ') {
            c = c.substring(1,c.length);
        }
        if (c.indexOf(nameEQ) == 0) {
            return c.substring(nameEQ.length,c.length);
        }
    }
    return '';
}
</script>

