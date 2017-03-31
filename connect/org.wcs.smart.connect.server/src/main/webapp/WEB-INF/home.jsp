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
		<tr><th>Quicklink</th><th>Order</th><th>Actions</th></tr>
		</table>
	</h2></div>
	<div>
		<form id="newquicklink" name="newquicklink" style="display:none">
			URL: <input name="url" type=text>
			Label: <input name="label" type="text" maxlength=256>
			<button name="addbutton" onClick="addLink(); return false;" class="button" > Create New Quicklink</button> <br> Or <br>
			<button name="addToAllButton" id="addToAllButton" onClick="addLinktoAll(); return false;" style="display:none" class="button" > Create Quicklink and Add to All User's Homepages</button>
			<p id="addfromadmin" style="display:none"><select id="admin-selectlist" name="admin-selectlist"></select> <button onClick="addFromAdminList(); return false;" class="button" > Add the Selected Link to Your List</button></p>
		</form>
		
		<p id="managemylinks"><br><a href="javascript:manageMylinks();">Manage My Quicklinks</a></p>
		<p id="manageall" style="display:none" ><br><a href="javascript:manageQuicklinks();">Manage All Quicklinks</a></p>
	</div>
	</div>
<%@include file="footer.jsp" %>



	<div id="updateUserQuicklinkDialog" style="display: none;" class="dialog">	
	<div class="dialog-title">Update Quicklink</div>
	 	<div id="dialogerror" class="errorsection"></div>
		<form id="updateUserQuicklinkForm" name="updateUserQuicklinkform">
	    		<div id="error" class="errorsection" style="display:none"></div>
	    		<label class="top-spacer block">Quicklink Label:</label>
	    		<input name="update-label" type="text" maxlength=256 size=50>
	    		<label class="top-spacer block">Order Value:</label>
	    		<input name="update-order" type="number">
	    		<input type="hidden" name="update-uuid" value="" />
	 
	  			<div class="block top-spacer">
	  			 <input class="button top-spacer" type="submit" value="   Update    "/>
	  			 <input class="button" type="button" id="canceluser" value="Cancel" />
	  			 </div>
	   	</form>
	 </div>

	<div id="updateQuicklinkDialog" style="display: none;" class="level2dialog">	
	<div class="dialog-title">Update Quicklink</div>
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
		<div class="dialog-title">Manage Quicklinks for All Users</div>
		<p>Deleting links here will remove them and ALL copies that ALL user's have on their homepage.
		<p>Updating URLs will also affect all user copies.
		<table id="managequicklinktable" border=1 style="border-collapse: collapse;">
			<tr><th>Link</th><th>Created On</th><th>Owner is an Admin </th><th>Actions</th>
		</table>
		
		<div class="block top-spacer">
	  			 <input class="button" type="button" id="cancelmanageall" value="Cancel" />
	  			 </div>
	</div>
</body>
</html>