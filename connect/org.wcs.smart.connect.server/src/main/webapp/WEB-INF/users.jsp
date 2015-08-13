<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/users.js"></script>

<title>SMART Connect - Users</title>
</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
  <div class="pageheader">Connect Users</div>
  <p class="infomessage">Manage connect user accounts here.</p>
  <div>
    <div id="message" class="msgsection"></div>
    <div id="error" class="errorsection"></div>
    
    <button class="block button top-spacer" id="btnNewUser">Create New User</button>
  </div>
  
<div class="top-spacer"  style="margin-left: -20px" >
  <div class="usertable table-cell smart-table">
  	<div class="table-row smart-table-header">
			<div class="table-cell smart-table-cell">User</div>
			<div class="table-cell smart-table-cell">Email</div>
			<div class="table-cell smart-table-cell"></div>
		</div>
	<c:forEach var="user" items="${users}" varStatus="count">
		<div data-username ="${user.getUsername()}" class="smartuser userrow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
			<div class="table-cell smart-table-cell">${user.getUsername()}</div>
			<div class="table-cell smart-table-cell">${user.getEmail()}</div>
			<div class="table-cell smart-table-cell "><a href="" data-username = "${user.getUsername()}" title="delete user" class="deleteuser delete-icon"></a></div>
		</div>
	</c:forEach>  
  </div>

<div id="userdetails" style="width:100%; padding-left: 10px; border-left: 1px solid #3A4469;" class="table-cell">
<div class="pageheader">User Details</div>
<div id="userinfo">
</div>
</div>

</div>

</div>
<%@include file="footer.jsp" %>


<div id="newUserDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Create New Smart User</div>
  <div id="dialogerror" class="errorsection"></div>
  <div>Create a new SMART Connect User</div>
  <form id="newuserform" >
    <label class="block top-spacer">Username:</label>
    <input type="text" name="username" class="formtext block" />
    <label class="block top-spacer">Email:</label>
    <input type="text" name="email" class="formtext block" />
    <label class="block top-spacer">Password:</label>
    <input type="password" name="password1" class="formtext table-row"/>
    <label class="block top-spacer">Re-enter Password:</label>
    <input type="password" name="password2" class="formtext table-row"/>
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="Create User" />
     <input class="button" type="button" id="cancelnewuser" value="Cancel" />
    </div>
  </form>
  </div>

</body>
</html>