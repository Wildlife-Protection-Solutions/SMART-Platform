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
	<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/fontawesome.min.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/solid.min.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/regular.min.css" />
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
  <div style="display:table; border-collapse: collapse;">
    <div style="display:table-row;">
      <div style="display:table-cell; white-space:nowrap; padding: 10px 25px 10px 0px"><span title="<fmt:message key="sharedlinks.linkinfo"/>" > <fmt:message key="sharedlinks.linktitle"/> </span></div>
      <div style="display:table-cell;padding: 10px 10px 10px 0px"><p><fmt:message key="sharedlinks.shared_creation"/></p></div>
    </div>
    <div style="display:table-row; " >
      <div style="display:table-cell; white-space:nowrap;padding: 10px 25px 10px 0px"><span title="<fmt:message key="sharedlinks.tokeninfo"/>" ><fmt:message key="sharedlinks.tokentitle" /></span></div>
      <button id="opentokendialog" class="button" type="button"><fmt:message key="sharedlinks.maketoken" /></button>
    </div>
</div>


<div class="top-spacer" style="flex: 1 1 auto; height: 0;">
  <div style="height: 100%; overflow: auto; display: inline-block;">
  <div id="linktable" class="linktable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell"><fmt:message key="sharedlinks.url" /></div>
		<div class="table-cell smart-table-cell"><fmt:message key="sharedlinks.link" /></div>
		<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('allowedIp')"><fmt:message key="sharedlinks.allowedip" /></a></div>
		<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('ownerUsername')"><fmt:message key="sharedlinks.createdby"/></a></div>
		<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('permissionUsername')"><fmt:message key="sharedlinks.permissionuser"/></a></div>
		<div class="table-cell smart-table-cell"><a class="smart-table-header" href="javascript:sortTable('expiresAt')"><fmt:message key="sharedlinks.expiresat" /></a></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
  </div>
  </div>
</div>

</div>
<%@include file="footer.jsp" %>


<div id="SharedLinksDialog" style="display: none; width:425px;" class="level2dialog">
  <div class="dialog-title"><fmt:message key="sharedlinks.maketoken" /></div>
  	<div id="sharedlinkform" style="text-align: right">
     		
					<fieldset class="linkdialog" style="border:0px; margin:0px;padding:0px">
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
						   <td><input id="expiresAfter" type="number" name="expiresAfter" value=60 style="width:4em" min="0" max="2147483647" disabled> <fmt:message key="query.numminutes2"/></td>
						  </tr>
						  <tr>
						   <td ><fmt:message key="sharedlinks.permissionuserrestrict"/></td>
						   <td colspan=2><select id="linkUser" style="width:100%" > </select></td>
						  </tr>
						  <tr>
						   <td ><fmt:message key="sharedlinks.sourceip"/></td>
						   <td colspan=2><input id="allowedIp" type="text" name="allowedIp" style="width:100%" value=""> </td>
						  </tr>
						  <tr>
						   <td colspan=3><fmt:message key="sharedlinks.sourceip2"/></td>
						  </tr>
						  <tr>
						   <td colspan=3><span id="linkusermsg"></span></td>
						  </tr>
						  <tr>
						   <td colspan=3 align="center"><input id="createtokenbutton" class="close button" type="button" value="<fmt:message key="query.creatbutton"/>" /></td>
						  </tr>
						  <tr >
						     <td colspan=3 ><input id="createdlink" class="hide linkdialog" type="text"/></td>
						  </tr>
						</table>
	   				</fieldset>	   				

	   			<div>
	   			    
	   				<input id="close" class="close button" type="button" value="<fmt:message key="query.closebutton"/>" />
	   			</div>
   			
    	</div>
</div>

</body>
</html>