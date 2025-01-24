<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html style="height: 100%;">
<head>

<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/cybertracker.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/qrcode.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/userssharedfunctions.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/fontawesome.min.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/solid.min.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/regular.min.css" />



<title><fmt:message key="cybertracker.packagestitle"/></title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">

  <div class="tabheader pageheader">
  		<a id="sm_packages" class="tab"><fmt:message key="cybertracker.header"/></a>
  		<a id="sm_routes" class="tab"><fmt:message key="cybertracker.routestable"/></a>
  		<a id="sm_announcements" class="tab">Announcements</a>
  		<a id="sm_keys" class="tab"><fmt:message key="cybertracker.apikeys"/></a>
  		<a id="sm_collectusers" class="tab"><fmt:message key="cybertracker.collectusersection"/></a>
  </div>
  	
  <div>
    <div id="message" class="msgsection"></div>
  </div>

    <div id="sm_packages_body" class="tabbody" style="flex: 1 1 auto; overflow: auto;">
  
	  <!-- Package Table -->
	  <p class="top-spacer label-header" style="padding-top:4px"><fmt:message key="cybertracker.uploadedpackages"/></p>
	  
	  <div class="top-spacer" >
	    <div id="ctpackagetable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.name"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.ca"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.type"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.publicprivate"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.upload"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.revisiondate"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.qrcode"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.link"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.revision"/></div>
			  <div class="table-cell smart-table-cell"></div>
			  <div class="table-cell smart-table-cell"></div>
		  </div>
	    </div>
	    <a id="refreshnow" href="#"><fmt:message key="cybertracker.refresh"/></a>
	  </div>
	</div>	

    <div id="sm_routes_body" class="tabbody" style="flex: 1 1 auto; overflow: auto;">
	<p class="top-spacer label-header" style="padding-top:4px">Uploaded SMART Mobile Routes.</p>
	  <div class="top-spacer" >
	    <div id="navlayertable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.routestablename"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.routestableca"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.routestableuploaddate"/></div>
			  <div class="table-cell smart-table-cell"></div>
			  <div class="table-cell smart-table-cell"></div>
		  </div>
	    </div>
	    <a id="navrefreshnow" href="#"><fmt:message key="cybertracker.refresh"/></a>
	  </div>
	</div>
	  
	  
	  <!-- Announcements Table -->
	<div id="sm_announcements_body" class="tabbody" style="flex: 1 1 auto; overflow: auto;">
	  <p class="top-spacer" style="padding-top:4px">Here you can setup and configure announcements that are display on SMART Mobile devices. Announcements are configured per Conservation Area.</p>
	  <p style="padding-top:4px">Announcements are automatically deleted from the system 3 months after their expiry date.</p>
	  
	  <button class="block button top-spacer" onclick="showCreateAnnouncementDialog()" >Create New Announcement</button>
	  	
	  <div class="top-spacer" >
	    <div id="announcementstable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell">Conservation Area</div>
			  <div class="table-cell smart-table-cell" style="width: 1500px">Message</div>
			  <div class="table-cell smart-table-cell">Created Date</div>
			  <div class="table-cell smart-table-cell">Expiry Date</div>			  		
			  <div class="table-cell smart-table-cell"></div>
		  </div>
	    </div>
	    <a id="refreshannouncements" href="#"><fmt:message key="cybertracker.refresh"/></a>
	  </div>
	</div>	
	
	  <!--  API Key Table -->
	<div id="sm_keys_body" class="tabbody" style="flex: 1 1 auto; overflow: auto;">
	  <p><fmt:message key="cybertracker.apikeysmessage1"/></p>
	   <div class="top-spacer" >
	  
	     <div id="ctapikeytable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.apikeytable.ca"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.privatekey"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.smartcollectkey"/></div>
		  </div>
	    </div>
	   </div>
	 </div>	   
	   
	<!--  collect Users -->      	  
	<div id="sm_collectusers_body" class="tabbody" style="flex: 1 1 auto; overflow: auto;">
	  <p><fmt:message key="cybertracker.collectusersectionmsg"/></p>
	  <div class="top-spacer"> 
		<!-- Search Parameters -->
		<label><fmt:message key="cybertracker.collectuserseaarch"/></label>
		<input class="formtext" type=text  id="collectusersearch" maxlength=50 oninput="searchCollectUsers()" style="width:30em"/>
	  </div>
	  <div class="top-spacer" >
	  
	     <div id="collectusertable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.collectusername"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.collectdeviceid"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.collectuserstatus"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.collectuseractions"/></div>
		  </div>
	    </div>
	   </div>
	   <p class="top-spacer"><fmt:message key="cybertracker.maxusers1"/> <a href="../api/smartcollect/source" target="collectusers"><fmt:message key="cybertracker.maxusers2"/></a></p>
	   
	 </div>
</div>
		

	<%@include file="footer.jsp" %>

	<div id="deleteDialog" style="display: none;" class="dialog">
	  <div class="dialog-title"><fmt:message key="cybertracker.deletepackage"/></div>
	  <div id="dialogerror" class="errorsection"></div>
	  
	  <form id="deleteform" onsubmit="return deletePackage();" >
	    <input type="hidden" name="packageuuid"/>
	   	<p><fmt:message key="cybertracker.deletepackagemsg"/></p>
	   	<div class="block top-spacer" style="text-align:right">
	     <input class="button" type="submit" value="<fmt:message key="cybertracker.deletebtn"/>" />
	     <input class="button" type="button" value="<fmt:message key="cybertracker.cancelbtn"/>" onclick="closeDialog('deleteDialog')" />
	    </div>
	  </form>
  </div>
  
  <div id="deleteNavDialog" style="display: none;" class="dialog">
	  <div class="dialog-title"><fmt:message key="cybertracker.deletepackage"/></div>
	  <div id="dialogerror" class="errorsection"></div>
	  
	  <form id="deletenavform" onsubmit="return deleteNavigation();" >
	    <input type="hidden" name="navuuid"/>
	   	<p><fmt:message key="cybertracker.deletenavconfirm"/></p>
	   	<div class="block top-spacer" style="text-align:right">
	     <input class="button" type="submit" value="<fmt:message key="cybertracker.deletebtn"/>" />
	     <input class="button" type="button" value="<fmt:message key="cybertracker.cancelbtn"/>" onclick="closeDialog('deleteNavDialog')" />
	    </div>
	  </form>
  </div>
  
  	<div id="resetApiDialog" style="display: none;" class="dialog">
	  <div class="dialog-title"><fmt:message key="cybertracker.resetkey"/></div>
	  
	  <form id="resetapiform" onsubmit="return resetApiKey();" >
	    <input type="hidden" name="cauuid"/>
	    <input type="hidden" name="label"/>
	    <input type="hidden" name="type"/>
	    <p><fmt:message key="cybertracker.resetkeymsg"/></p>
	    <div class="block top-spacer" style="text-align:right">
	     <input class="button" type="submit" value="<fmt:message key="cybertracker.resetbtn"/>" />
	     <input class="button" type="button" value="<fmt:message key="cybertracker.cancelbtn"/>" onclick="closeDialog('resetApiDialog')" />
	    </div>
	  </form>
  </div>
  
   <div id="deleteCollectUserDialog" style="display: none;" class="dialog">
	  <div class="dialog-title"><fmt:message key="cybertracker.deleteuserdialogtitle"/></div>
	  <div id="dialogerror" class="errorsection"></div>
	  
	  <form id="deletecollectform" onsubmit="return deleteCollectUser();" >
	    <input type="hidden" name="uuid"/>
	   	<p><fmt:message key="cybertracker.deleteuserdialogmessage"/></p>
	   	<div class="block top-spacer" style="text-align:right">
	     <input class="button" type="submit" value="<fmt:message key="cybertracker.deletebtn"/>" />
	     <input class="button" type="button" value="<fmt:message key="cybertracker.cancelbtn"/>" onclick="closeDialog('deleteCollectUserDialog')" />
	    </div>
	  </form>
  </div>
  
  
  <div id="createAnnouncementDialog" style="display: none;" class="dialog">
    <div class="dialog-title" id="announcementdialogtitle">Create New Announcement</div>
  
	<section id="tab2" class="">
		<p>
		<form id="newannouncementform" style="padding-left:10px">
     		<div id="announcementerror" class="errorsection"></div>
     		<input id="announcement_uuid" type="hidden">
     		<label class="top-spacer block">Conservation Area</label>
     		<select name="announcement_ca" class="block  formtext ">
	     		<c:forEach var="ca" items="${cas}" varStatus="count">
	     			<option value="${ca.getUuid()}">${ca.getLabel()} </option> 
				</c:forEach> 
     		</select>
     		
			<label class="top-spacer block">Expires On:</label>
			<input id="announcement_expiry" type="datetime-local" class="formtext" style="width: 20em">
			
			
			<label class="top-spacer block">Message</label>
			<textarea name="announcement_message" rows="5" cols="45"></textarea>
			
			<div class="block top-spacer" style="text-align:right">
   			  <input class="button top-spacer" type="submit" value="   Submit   "/>
   			  <input class="button" type="button" onClick="closeDialog('createAnnouncementDialog')" value="   Cancel   "/>
   			  </div>
    	</form>
		</p>
	</section>
</div>
  
</body>
</html>