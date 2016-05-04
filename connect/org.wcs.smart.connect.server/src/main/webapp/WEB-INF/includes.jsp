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

<link rel="shortcut icon" href="${pageContext.request.contextPath}/css/smart_logo_icon.png"> 

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

