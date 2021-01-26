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
var definedDates = ["query.last30days","query.last60days","query.monthtodate","query.lastmonth","query.yeartodate","query.lastyear","query.alldates","query.custom"];
var definedDateKeys = ["last30days", "last60days", "monthtodate", "lastmonth", "yeartodate", "lastyear", "alldates", "custom"];
var isDateChanging = false;

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

// populate predefined dates in query date picker
function populateQueryDates(selectElement) {
	for(var i = 0; i < definedDates.length; i ++) {
		var object = document.createElement("option");
		object.value = definedDateKeys[i];
		object.innerHTML = i18n(definedDates[i]);
		selectElement.appendChild(object);
		if(definedDateKeys[i] == "alldates") {
			selectElement.selectedIndex = i;
		}
	}
}

function buildUpdateDateHandler(selectElement, startDatePicker, endDatePicker, callback) {
	return function(event) {
		updateDates(selectElement, startDatePicker, endDatePicker);
		if (callback != null) return callback(event);
		return true;
	}
}

function updateDates(selectElement, startDatePicker, endDatePicker){
	var datekey = selectElement.options[selectElement.selectedIndex].value;
	
	// this prevents the event handlers for the individual date fields 
	// from switching back to custom date
	isDateChanging = true;
	if(datekey == "last30days") {
		var startDate = new Date();
		startDate.setDate(startDate.getDate() - 30);
		endDatePicker.setDate(new Date(), false);
		startDatePicker.setDate(startDate, false);
	} else if(datekey == "last60days") {
		var startDate = new Date();
		startDate.setDate(startDate.getDate() - 60);
		endDatePicker.setDate(new Date(), false);
		startDatePicker.setDate(startDate, false);
	} else if(datekey == "monthtodate") {
		var today = new Date();
		var startDate = new Date(today.getFullYear(), today.getMonth(), 1,0,0,0);
		endDatePicker.setDate(today, false);
		startDatePicker.setDate(startDate, false);
	} else if(datekey == "lastmonth") {
		var startDate = new Date();
		startDate.setMonth(startDate.getMonth() - 1);
		startDate.setDate(1);
		var endDate = new Date();
		endDate.setDate(0);
		endDatePicker.setDate(endDate, false);
		startDatePicker.setDate(startDate, false);
	} else if (datekey== "yeartodate") {
		var today = new Date();
		var startDate = new Date(today.getFullYear(), 0, 1,0,0,0);
		endDatePicker.setDate(today, false);
		startDatePicker.setDate(startDate, false);
	} else if (datekey== "lastyear") {
		var today = new Date();
		var startDate = new Date(today.getFullYear() - 1, 0, 1,0,0,0);
		var endDate = new Date(today.getFullYear() - 1, 11, 31, 23,59,59);
		endDatePicker.setDate(endDate, false);
		startDatePicker.setDate(startDate, false);
	} else if (datekey== "alldates") {
		endDatePicker.setDate('', false);
		startDatePicker.setDate('', false);
	} else if (datekey== "custom") {
		
	}
	isDateChanging = false;
}



//to support IE11 -> this support is required as this
//is the engine that eclipse uses for the internal browser
//on most windows machines at this time
//Production steps of ECMA-262, Edition 6, 22.1.2.1
if (!Array.from) {
  Array.from = (function () {
      var symbolIterator;
      try {
          symbolIterator = Symbol.iterator
              ? Symbol.iterator
              : 'Symbol(Symbol.iterator)';
      } catch (e) {
          symbolIterator = 'Symbol(Symbol.iterator)';
      }

      var toStr = Object.prototype.toString;
      var isCallable = function (fn) {
          return (
              typeof fn === 'function' ||
              toStr.call(fn) === '[object Function]'
          );
      };
      var toInteger = function (value) {
          var number = Number(value);
          if (isNaN(number)) return 0;
          if (number === 0 || !isFinite(number)) return number;
          return (number > 0 ? 1 : -1) * Math.floor(Math.abs(number));
      };
      var maxSafeInteger = Math.pow(2, 53) - 1;
      var toLength = function (value) {
          var len = toInteger(value);
          return Math.min(Math.max(len, 0), maxSafeInteger);
      };

      var setGetItemHandler = function setGetItemHandler(isIterator, items) {
          var iterator = isIterator && items[symbolIterator]();
          return function getItem(k) {
              return isIterator ? iterator.next() : items[k];
          };
      };

      var getArray = function getArray(
          T,
          A,
          len,
          getItem,
          isIterator,
          mapFn
      ) {
          // 16. Let k be 0.
          var k = 0;

          // 17. Repeat, while k < len… or while iterator is done (also steps a - h)
          while (k < len || isIterator) {
              var item = getItem(k);
              var kValue = isIterator ? item.value : item;

              if (isIterator && item.done) {
                  return A;
              } else {
                  if (mapFn) {
                      A[k] =
                          typeof T === 'undefined'
                              ? mapFn(kValue, k)
                              : mapFn.call(T, kValue, k);
                  } else {
                      A[k] = kValue;
                  }
              }
              k += 1;
          }

          if (isIterator) {
              throw new TypeError(
                  'Array.from: provided arrayLike or iterator has length more then 2 ** 52 - 1'
              );
          } else {
              A.length = len;
          }

          return A;
      };

      // The length property of the from method is 1.
      return function from(arrayLikeOrIterator /*, mapFn, thisArg */) {
          // 1. Let C be the this value.
          var C = this;

          // 2. Let items be ToObject(arrayLikeOrIterator).
          var items = Object(arrayLikeOrIterator);
          var isIterator = isCallable(items[symbolIterator]);

          // 3. ReturnIfAbrupt(items).
          if (arrayLikeOrIterator == null && !isIterator) {
              throw new TypeError(
                  'Array.from requires an array-like object or iterator - not null or undefined'
              );
          }

          // 4. If mapfn is undefined, then let mapping be false.
          var mapFn = arguments.length > 1 ? arguments[1] : void undefined;
          var T;
          if (typeof mapFn !== 'undefined') {
              // 5. else
              // 5. a If IsCallable(mapfn) is false, throw a TypeError exception.
              if (!isCallable(mapFn)) {
                  throw new TypeError(
                      'Array.from: when provided, the second argument must be a function'
                  );
              }

              // 5. b. If thisArg was supplied, let T be thisArg; else let T be undefined.
              if (arguments.length > 2) {
                  T = arguments[2];
              }
          }

          // 10. Let lenValue be Get(items, "length").
          // 11. Let len be ToLength(lenValue).
          var len = toLength(items.length);

          // 13. If IsConstructor(C) is true, then
          // 13. a. Let A be the result of calling the [[Construct]] internal method
          // of C with an argument list containing the single item len.
          // 14. a. Else, Let A be ArrayCreate(len).
          var A = isCallable(C) ? Object(new C(len)) : new Array(len);

          return getArray(
              T,
              A,
              len,
              setGetItemHandler(isIterator, items),
              isIterator,
              mapFn
          );
      };
  })();
}

</script>

