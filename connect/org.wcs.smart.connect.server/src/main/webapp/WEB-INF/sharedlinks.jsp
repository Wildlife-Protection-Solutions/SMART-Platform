<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/sharedlinks.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	
	<script type="text/javascript" >
		var SHAREDLINKSERVLETURL = "${pageContext.request.contextPath}" + "/noa/sharedlink/";
		var cas = {
				<c:forEach var="cas" items="${cas}">
				    '${cas.caUuid}': '${cas.label}',
				</c:forEach>
				};
	</script>
		
	<title><fmt:message key="query.pagetitle"/></title>	
</head>

<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
<div class="pageheader"><fmt:message key="sharedlinks.title"/></div>
<div><div id="message" class="msgsection"></div></div>
<div class="top-spacer"> 
</div>

<div class="top-spacer"  style="margin-left: -20px" >
  <div id="linktable" class="linktable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell"><fmt:message key="sharedlinks.runquery" /></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('uuid')"><fmt:message key="query.id" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('url')"><fmt:message key="sharedlinks.url" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('expiresAt')"><fmt:message key="sharedlinks.expiresat" /></a></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
  </div>
  <div>
	<ul>
		<li><fmt:message key="sharedlinks.createnewdetails" /></li>
		<li><font color='red'><bold><fmt:message key="query.sharingDescriptionAll2" /></bold></font></li>
	</ul>
  </div>  
</div>

</div>

<%@include file="footer.jsp" %>

</body>
</html>