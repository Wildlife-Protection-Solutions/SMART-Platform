<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div id="verticalmenu" class="verticalmenu" style="display:table-cell;width:24px;">
 <div style="display:flex;flex-flow:column nowrap">

<!-- Hamburger menu by (https://www.iconfinder.com/icons/134216/hamburger_lines_menu_icon) Timothy Miller under (https://creativecommons.org/licenses/by-sa/3.0/legalcode) creative commons SA-3.0 license, no changes were made.   
  Other menu icons made by (https://www.flaticon.com/authors/gregor-cresnar) Gregor Cresnar, found on https://www.flaticon.com/ , licensed under (http://creativecommons.org/licenses/by/3.0/) CC 3.0 BY 
  -->


<div style="height:24px;padding:3px;"><a href="javascript:hamburgerMenu()"><img height="24" width="24" src="../css/images/hamburger.png"></a>
</div>

<c:forEach var="item" items="${menuitems}">
 	<div style="" class="${item[2]}" onclick="window.location='${item[1]}'">
 		<img title="${item[0]}" height="24" width="24" src="../css/images/${item[3]}"/>
		<div class="textMenu" style="display:none;vertical-align:super">${item[0]}</div>
	</div>
 </c:forEach>

 </div>
</div>
