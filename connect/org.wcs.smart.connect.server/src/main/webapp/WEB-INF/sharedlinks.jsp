<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/sharedlinkfunctions.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/sharedlinks.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	
	<script type="text/javascript" >
		var SHAREDLINKSERVLETURL = "${pageContext.request.contextPath}" + "/noa/sharedlink/";
		var SHARED_LINK_URL = "../api/sharedlink/";
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
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('allowedIp')"><fmt:message key="sharedlinks.allowedip" /></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('ownerUsername')"><fmt:message key="sharedlinks.createdby"/></a></div>
		<div class="table-cell smart-table-cell"><a href="javascript:sortTable('expiresAt')"><fmt:message key="sharedlinks.expiresat" /></a></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
  </div>
 
</div>

<div>
<p>
<br>
<fmt:message key="sharedlinks.createtokensentence" /><button id="opentokendialog" type="button"><fmt:message key="sharedlinks.maketoken" /></button>
</p>
</div>

</div>


<%@include file="footer.jsp" %>


<div id="SharedLinksDialog" style="display: none;" class="level2dialog">
  <div class="dialog-title"><fmt:message key="sharedlinks.maketoken" /></div>
  	<form id="sharedlinkform" name="sharedlinkform">
     		<div style="text-align: right">

					<fieldset class="linkdialog">
						<p><span class="smart-warninghighlight"><fmt:message key="sharedlinks.tokenwarning"/></span></p>
						
						<table class="top-spacer" style="width:100%">
						  <tr>
						   <td><fmt:message key="query.numminutes" /></td>
						   <td>
							<select id="quickMinSelect">
								<option value=60><fmt:message key="sharedlinks.onehour"/></option>
								<option value=1440><fmt:message key="sharedlinks.oneday"/></option>
								<option value=10080><fmt:message key="sharedlinks.oneweek"/></option>
								<option value=43200><fmt:message key="sharedlinks.onemonth"/></option>
								<option value=259200><fmt:message key="sharedlinks.sixmonths"/></option>
								<option value=518400><fmt:message key="sharedlinks.oneyear"/></option>
								<option value=-1><fmt:message key="sharedlinks.custom"/></option>
							</select>
						  </td>
						   <td><input id="expiresAfter" type="number" name="expiresAfter" value=60 style='width:65px' min="0" max="2147483647" disabled> <fmt:message key="query.numminutes2"/></td>
						  </tr>
						  <tr>
						   <td colspan=2><fmt:message key="sharedlinks.sourceip"/></td><td><input id="allowedIp" type="text" name="allowedIp" value=192.168.1.1 style='width:125px'> </td>
						  </tr>
						  <tr>
						   <td colspan=3><fmt:message key="sharedlinks.sourceip2"/></td>
						  </tr>
						  
						  <tr >
						     <td colspan=3 align="center"><input id="createtokenbutton" class="close" type="button" value="<fmt:message key="query.creatbutton"/>" /></td>
						  </tr>
						  <tr >
						     <td colspan=3><input id="createdlink" class="hide linkdialog" type="text"/></td>
						  </tr>
						</table>
	   				</fieldset>	   				

	   			<div>
	   				<input id="close" class="close" type="button" value="<fmt:message key="query.closebutton"/>" />
	   			</div>
   			</div>
    	</form>
</div>

</body>
</html>