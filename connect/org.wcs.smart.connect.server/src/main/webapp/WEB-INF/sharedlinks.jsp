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
		
	<title><fmt:message key="sharedlinks.title"/></title>	
</head>

<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
<div class="pageheader"><fmt:message key="sharedlinks.title"/></div>
<div><div id="message" class="msgsection"></div></div>

<div>
 	<p class="infomessage"><fmt:message key="sharedlinks.createnewdetails" /></p>
 	<p class="smart-warninghighlight"><fmt:message key="query.sharingDescriptionAll2" /></p>
</div>

<div class="top-spacer" style="margin-left: -20px"> 
  <div id="linktable" class="linktable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell"><fmt:message key="sharedlinks.url" /></div>
		<div class="table-cell smart-table-cell"><fmt:message key="sharedlinks.link" /></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('ownerUsername')"><fmt:message key="sharedlinks.createdby"/></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('expiresAt')"><fmt:message key="sharedlinks.expiresat" /></a></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
  </div>
 
</div>

</div>

<%@include file="footer.jsp" %>

</body>
</html>