<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div id="verticalmenu" class="verticalmenu" style="display:table-cell;width:24px;">
 <div style="display:flex;flex-flow:column nowrap">


<div style="height:24px;padding:3px;"><a href="javascript:hamburgerMenu()"><img height="24" width="24" src="../css/images/hamburger.png"></a>
</div>

<c:forEach var="item" items="${menuitems}">
 	<div style="display:flex;flex-flow:row nowrap;height:24px;align-items: center" class="${item[2]}" onclick="window.location='${item[1]}'">
 		<img title="${item[0]}" height="24" width="24" src="../css/images/${item[3]}"/>
		<div class="textMenu" style="display:none;vertical-align:super">${item[0]}</div>
	</div>
 </c:forEach>

 </div>
</div>
