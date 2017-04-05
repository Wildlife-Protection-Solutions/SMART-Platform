<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/home.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	<title><fmt:message key="home.pagetitle"/></title>
	
</head>

<body style="${style_bodycss}">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id= "main"><div class="pageheader"><fmt:message key="home.welcome"/></div>
	<div><h2>
		<div id="simpleview"></div>
		<table id="quicklinklist" style="display:none">
		<tr><th><fmt:message key="quicklinks.quicklink"/></th><th><fmt:message key="quicklinks.order"/></th><th><fmt:message key="quicklinks.actions"/></th></tr>
		</table>
	</h2></div>
	<div>
		<form id="newquicklink" name="newquicklink" style="display:none">
			<fmt:message key="quicklinks.url"/><input name="url" type=text>
			<fmt:message key="quicklinks.label"/><input name="label" type="text" maxlength=256>
			<button name="addbutton" onClick="addLink(); return false;" class="button" > <fmt:message key="quicklinks.createquicklink"/></button> 
			<button name="addToAllButton" id="addToAllButton" onClick="addLinktoAll(); return false;" style="display:none" class="button" > <fmt:message key="quicklinks.createandaddtoall"/></button>
			<p id="addfromadmin" style="display:none"><select id="admin-selectlist" name="admin-selectlist"></select> <button onClick="addFromAdminList(); return false;" class="button" > <fmt:message key="quicklinks.addtolist"/></button></p>
		</form>
		
		<p id="managemylinks"><br><a href="javascript:manageMylinks();"><fmt:message key="quicklinks.managemylinks"/></a></p>
		<p id="manageall" style="display:none" ><br><a href="javascript:manageQuicklinks();"><fmt:message key="quicklinks.managealllinks"/></a></p>
	</div>
	</div>
<%@include file="footer.jsp" %>



	<div id="updateUserQuicklinkDialog" style="display: none;" class="dialog">	
	<div class="dialog-title"><fmt:message key="quicklinks.updatelinks"/></div>
	 	<div id="dialogerror" class="errorsection"></div>
		<form id="updateUserQuicklinkForm" name="updateUserQuicklinkform">
	    		<div id="error" class="errorsection" style="display:none"></div>
	    		<label class="top-spacer block"><fmt:message key="quicklinks.quicklinklabel"/></label>
	    		<input name="update-label" type="text" maxlength=256 size=50>
	    		<label class="top-spacer block"><fmt:message key="quicklinks.ordervalue"/></label>
	    		<input name="update-order" type="number">
	    		<input type="hidden" name="update-uuid" value="" />
	 
	  			<div class="block top-spacer">
	  			 <input class="button top-spacer" type="submit" value="   Update    "/>
	  			 <input class="button" type="button" id="canceluser" value="Cancel" />
	  			 </div>
	   	</form>
	 </div>

	<div id="updateQuicklinkDialog" style="display: none;" class="level2dialog">	
	<div class="dialog-title"><fmt:message key="quicklinks.updatequicklink"/></div>
	 	<div id="dialogerror" class="errorsection"></div>
		<form id="updateQuicklinkForm" name="updateQuicklinkform">
	    		<div id="error" class="errorsection" style="display:none"></div>
	    		<label class="top-spacer block">URL:</label>
	    		<input name="update-url" type="text" maxlength=256 size=50>
	    		<input type="hidden" name="update-qluuid" value="" />
	 
	  			<div class="block top-spacer">
	  			 <input class="button top-spacer" type="submit" value="   Update    "/>
	  			 <input class="button" type="button" id="cancel" value="Cancel" />
	  			 </div>
	   	</form>
	 </div>


	<div id="manageQuicklinksDialog" style="display: none;" class="dialog">
		<div class="dialog-title"><fmt:message key="quicklinks.managealltitle"/></div>
		<p><fmt:message key="quicklinks.deletinglinks"/>
		<p><fmt:message key="quicklinks.updatingurls"/>
		<table id="managequicklinktable" border=1 style="border-collapse: collapse;">
			<tr><th><fmt:message key="quicklinks.link"/></th><th><fmt:message key="quicklinks.createdon"/></th><th><fmt:message key="quicklinks.isadmin"/></th><th><fmt:message key="quicklinks.actions"/></th>
		</table>
		
		<div class="block top-spacer">
	  			 <input class="button" type="button" id="cancelmanageall" value="Cancel" />
	  			 </div>
	</div>
</body>
</html>