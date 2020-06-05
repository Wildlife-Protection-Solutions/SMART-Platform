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



<title><fmt:message key="cybertracker.packagestitle"/></title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
  <div class="tabheader pageheader"><fmt:message key="cybertracker.header"/></div>
  <p class="infomessage"><fmt:message key="cybertracker.info"/></p>
  <div>
    <div id="message" class="msgsection"></div>
  </div>
  
  <div style="height: 100%;overflow: auto;">
	  <!-- Package Table -->
	  <p class="top-spacer label-header" style="border-top:1px solid; padding-top:4px"><fmt:message key="cybertracker.uploadedpackages"/></p>
	  
	  <div class="top-spacer" >
	    <div id="ctpackagetable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.name"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.ca"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.type"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.upload"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.revisiondate"/></div>
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.packagetable.revision"/></div>
			  <div class="table-cell smart-table-cell"></div>
			  <div class="table-cell smart-table-cell"></div>
		  </div>
	    </div>
	    <a id="refreshnow" href="#"><fmt:message key="cybertracker.refresh"/></a>
	  </div>	
	  
	  <!-- Navigation Layer Table -->
	  <p class="top-spacer label-header" style="border-top:1px solid; padding-top:4px"><fmt:message key="cybertracker.routestable"/></p>
	  
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
	  
	  <!--  API Key Table -->
	  <p class="top-spacer label-header" style="margin-top:50px; border-top:1px solid; padding-top:4px"><fmt:message key="cybertracker.apikeys"/></p>
	  <p><fmt:message key="cybertracker.apikeysmessage1"/></p>
	   <div class="top-spacer" >
	  
	     <div id="ctapikeytable" class="table-cell smart-table">
	  	  <div class="table-row smart-table-header">
			  <div class="table-cell smart-table-cell"><fmt:message key="cybertracker.apikeytable.ca"/></div>
			  <div class="table-cell smart-table-cell">Private</div>
			  <div class="table-cell smart-table-cell">Community</div>
		  </div>
	    </div>
	   </div>
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
  
</body>
</html>