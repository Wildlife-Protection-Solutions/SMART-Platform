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
	
	
	<div id="simpleview" style="padding-top: 15px">
	</div>
	
  	<div id="quicklinklist" style="display:none"> 
  	<div style="padding-top: 15px">
  		<div style="display:block; margin-bottom:10px; text-decoration: underline"><p><fmt:message key="quicklinks.my"/></p></div>
  		<div style="display:block; border-spacing:0px">
  		<div id ="myquicklinklist" class="table-cell smart-table" >
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.quicklink"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.order"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.actions"/></div>
		
			</div>
		</div>
		</div>
	</div>
	</div>  
	
	
		<form id="newquicklink" name="newquicklink" style="display:none">
		<div style="display:block; margin-bottom:5px; margin-top:20px; text-decoration: underline"><p><fmt:message key="quicklinks.new"/></p></div>
		<div class="table-cell smart-table" style="display:block">
			<div class="table-row">
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.url"/></div>
				<div class="table-cell smart-table-cell"><input name="url" type=text class="formtext" style="width:400px"/></div>
			</div>
			<div class="table-row">
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.label"/></div>
				<div class="table-cell smart-table-cell"><input name="label" type=text class="formtext" maxlength=256 style="width:400px"/></div>
			</div>
		</div>
		<button name="addbutton" onClick="addLink(); return false;" class="button" > 
		<fmt:message key="quicklinks.createquicklink"/></button>
		<fmt:message key="quicklinks.or"/> 
		<button name="addToAllButton" id="addToAllButton" onClick="addLinktoAll(); return false;" style="display:none" class="button" > <fmt:message key="quicklinks.createandaddtoall"/></button>
		<p id="addfromadmin" style="display:none"><select id="admin-selectlist" name="admin-selectlist" class="uielement"></select> <button onClick="addFromAdminList(); return false;" class="button" > <fmt:message key="quicklinks.addtolist"/></button></p>
		</form>
		
		<p id="managemylinks" style="font-size: 0.9em"><br><a href="javascript:manageMylinks();"><fmt:message key="quicklinks.managemylinks"/></a></p>
		<p id="manageall" style="display:none; font-size:0.9em;padding-top:10px" ><a href="javascript:manageQuicklinks();"><fmt:message key="quicklinks.managealllinks"/></a></p>
	
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
		<div style="display:block">
		<div id ="managequicklinktable" class="table-cell smart-table" >
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.link"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.createdon"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.isadmin"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="quicklinks.actions"/></div>
			</div>
		</div>
		</div>
		
		<div class="block top-spacer" style="text-align: right">
	  	<input class="button" type="button" id="cancelmanageall" value="Cancel" />
	  	</div>
	</div>
</body>
</html>