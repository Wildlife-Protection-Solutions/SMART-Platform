<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<div id="mainheader" style="${style_headercss}">

<c:choose>
 <c:when test="${empty style_headername}"> 
  SMART Connect
 </c:when>
 <c:otherwise>
	${style_headername}
 </c:otherwise>
</c:choose> 

</div>

<div id="core">

