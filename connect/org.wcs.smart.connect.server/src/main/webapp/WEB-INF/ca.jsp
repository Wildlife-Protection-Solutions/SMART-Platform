<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/ca.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<title>SMART Connect - Conservation Areas</title>

<script>
	
</script>

</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader">Conservation Areas</div>
<p class="infomessage">Lists all conservation areas currently active in SMART Connect</p>

<div>
  <div id="message" class="msgsection"></div>
</div>
<div class="top-spacer"  style="margin-left: -20px" >
  <div class="catable table-cell smart-table">
  	<div class="table-row smart-table-header">
		<div class="table-cell smart-table-cell">Label</div>
		<div class="table-cell smart-table-cell">UUID</div>
		<div class="table-cell smart-table-cell">Status</div>
		<div class="table-cell smart-table-cell">Version</div>
		<div class="table-cell smart-table-cell"></div>
		<div class="table-cell smart-table-cell"></div>
	</div>
	<c:forEach var="ca" items="${cas}" varStatus="count">
		<div data-cauuid ="${ca.getUuid()}" class="carow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
			<div class="table-cell smart-table-cell">${ca.getLabel()}</div>
			<div class="table-cell smart-table-cell">${ca.getUuid()}</div>
			<div class="table-cell smart-table-cell">${ca.getStatus()}</div>
			<div class="table-cell smart-table-cell">${ca.getVersion().toString()}</div>
			<div class="table-cell smart-table-cell ">
				<c:if test="${ca.getStatus() == 'DATA'}">
					<a href=""  data-cauuid = "${ca.getUuid()}" title="downloadca" class="downloadca download-icon"></a>
				</c:if>
			</div>
					
			<div class="table-cell smart-table-cell "><a href=""  data-status = "${ca.getStatus()}" data-cauuid = "${ca.getUuid()}" title="delete conservation area" class="deleteca delete-icon"></a></div>
		</div>
	</c:forEach>
	</div>  
</div>

<div>
<button id="btnNewCa" class="block button top-spacer">Create New</button>
</div>

</div>



<%@include file="footer.jsp" %>

<div id="deleteDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Delete Conservation Area</div>
  <div id="dialogerror" class="errorsection"></div>
  
  <form id="deleteform" onsubmit="return deleteca();" >
    <input type="hidden" name="cauuid"/>
    <div id="confirmtype">
    	<p>You can delete the entire Conservation Area from SMART Connect or you can choose to delete
    	only the data managed by SMART Desktop.</p>
    	<input type="radio" name="caoption" value="desktop" checked/>Desktop Data Only<br>
    	<input type="radio" name="caoption"  value="all"/>Entire Conservation Area<br><br>
    </div>
    <p>Re-enter you username and password:</p>
    <label class="block">Username:</label>
    <input type="text" name="username" class="formtext block" />

    <div class="block top-spacer">Password:</div>
    <input type="password" name="password" class="formtext block" />
    
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="Delete" />
     <input class="button" type="button" value="Cancel" onclick="closeDialog('deleteDialog')" />
    </div>
  </form>
  </div>
  
  
  <div id="downloadDialog" style="display: none;" class="dialog">
    <div class="dialog-title">Download Conservation Area</div>
    <div id="dialogerror" class="errorsection"></div>
    <p>Processing download request.  You will be automatically redirected once the download is ready.</p>
   	<div id="statusurl" style="font-size:0.8em"></div>
   	<div class="block top-spacer" style="text-align:right">
     <input class="button" type="button" value="Cancel" onclick="return cancelCaDownload();" />
    </div>
  </div>
  
<div id="newDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Create New Conservation Area</div>
  <div id="dialogerror" class="errorsection"></div>
  
  <form id="createform" onsubmit="return createca();" >
    <div id="confirmtype">
    	<p>Create a new Conservation Area on SMART Connect.</p>
    	<p>If you want 
    	to upload SMART Desktop data to Connect you should NOT use this function, 
    	but upload the Conservation Area directly from the SMART Desktop.  If you do use this option 
    	the UUID must match the SMART Desktop Unique System
    	ID found in the Conservation Area Properties page.</p>
    	<p>If you never want to upload SMART
    	Desktop data to SMART Connect leave the UUID field blank.</p>
    </div>
    <label class="block">Label:</label>
    <input type="text" name="calabel" class="formtext block" />
	<label class="block top-spacer">UUID:</label>
    <input type="text" name="newcauuid" class="formtext block" />
    
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="Create" />
     <input class="button" type="button" value="Cancel" onclick="closeDialog('newDialog')" />
    </div>
  </form>
  </div>
</body>
</html>