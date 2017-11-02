<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div id="verticalmenu" class="verticalmenu" style="display:table-cell;width:24px;">
 <div style="display:flex;flex-flow:column nowrap">


<div style="height:28px"><a href="javascript:hamburgerMenu()"><img height="24" width="24" src="../css/images/hamburger.png"></a>
</div>

<c:forEach var="item" items="${menuitems}">
 	<div style="display=flex;flex-flow:row nowrap;height:28px;align-items: center">
 		<a href="${item[1]}" "><img height="24" width="24" src="../css/images/${item[3]}"/></a>
		<div class="textMenu" style="display:none;vertical-align:super"><a href="${item[1]}" class="${item[2]}">${item[0]}</a></div>
	</div>
 </c:forEach>

 </div>
</div>
