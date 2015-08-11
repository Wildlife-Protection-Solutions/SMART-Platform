<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" >
	var numStyles=${numstyles};
</script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/settings.js"></script>

<title>SMART Connect - Settings</title>
</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
  <div class="pageheader">SMART Connect Settings</div>
  <p class="infomessage">Manage SMART Connect Settings</p>
  <div>
    <div id="message" class="msgsection"></div>
    <div id="error" class="errorsection"></div>
    	<button id="btnNewStyle" class="block button top-spacer">Create New Style</button>
    </div>
  
<div class="top-spacer"  style="margin-left: -20px" >
  <div class="styleTable table-cell smart-table">
  	<div class="table-row smart-table-header">
			<div class="table-cell smart-table-cell">Style Id</div>
			<div class="table-cell smart-table-cell">Active?</div>
			<div class="table-cell smart-table-cell">Server Name</div>
			<div class="table-cell smart-table-cell">Footer Text</div>
			<div class="table-cell smart-table-cell">Actions</div>
		</div>
	<c:forEach var="style" items="${styles}" varStatus="count">
		<div data-username ="${style.getStyleId()}" class="smartuser styleRow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
			<div class="table-cell smart-table-cell">${style.getStyleId()}</div>
			<div class="table-cell smart-table-cell">${style.getActive()}</div>
			<div class="table-cell smart-table-cell">${style.getServerName()}</div>
			<div class="table-cell smart-table-cell">${style.getFooterText()}</div>
			<div class="table-cell smart-table-cell "><a href="" data-id = "${style.getStyleId()}" title="delete style" class="deleteuser delete-icon"></a></div>
		</div>
	</c:forEach>  
  </div>


</div>

</div>
<%@include file="footer.jsp" %>


<div id="newStyleDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Create a New Style</div>
  <div id="dialogerror" class="errorsection"></div>
  <div>Create a new SMART Connect Server Style</div>
  <form id="newuserform" action="settings" method="POST" enctype="multipart/form-data">
    <label class="block top-spacer">Style Id:</label>
    <input type="text" name="style_id" class="formtext block" />
    <label class="block top-spacer">Background Image:</label>
    <input type="file" name="bg_image" class="formtext block" />
     <input class="button" type="submit" value="Create Style" />
     <input class="button" type="button" id="cancelNewStyle" value="Cancel" />
    </div>
  </form>
  </div>

</body>
</html>