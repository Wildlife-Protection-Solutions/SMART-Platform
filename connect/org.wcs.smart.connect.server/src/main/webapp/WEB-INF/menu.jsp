<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="verticalmenu">


<c:forEach var="item" items="${menuitems}">
	<a href="${item[1]}" class="${item[2]}">${item[0]}</a>
</c:forEach>

</div>
