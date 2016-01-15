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
	<div class="tabheader pageheader">
  		<a id="users" class="tab ">Connect User Accounts</a>
  		<a id="userroles" class="tab ">User Roles</a>
  	</div>
  	<div>
    	<div id="message" class="msgsection"></div>
  	</div>
  <!--  roles tab section  -->
  <div id="users_body" class="tabbody">
  	<p class="infomessage">Connect user account and associated permission are managed here. </p>
  	<div>
    	<button class="block button top-spacer" id="btnNewUser">Create New User</button>
  	</div>
  
	<div class="top-spacer"  style="margin-left: -20px" >
  		<div id="usertable" class="table-cell smart-table">
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

		<div id="userdetails" style="width:100%;" class="table-cell border-section">
			<div class="pageheader">User Details</div>
			<div id="userinfo">
				<div id="userinfodefaults"></div>
				<div id="userinfobuttons"class="tabheader">
					<a id="roletab" class="tab">Roles</a>
					<a id="actiontab" class="tab">Actions</a>
				</div>
				<div id="roletab_body" class="tabbody">
					<div class="add_select_section">
						<select id="roleKey" class="select-limit"><option>Loading...</option></select>
						<button id="addRole" class="block button " style = "padding: 0px; display: inline;">Add Role</button>
					</div>
				</div>
				<div id="actiontab_body" class="tabbody">
					<div class="add_select_section">
						<select id="actionKey" class="select-limit"><option>Loading...</option></select>
						<select id="actionResourceKey" class="select-limit"><option>Loading...</option></select>
						<button id="addAction" class="block button " style = "padding: 0px; display: inline;">Add Action</button>
					</div>
				</div>
			</div>
		</div>

	</div>
	</div>
	
	<!--  roles tab section  -->
	<div id="userroles_body" class="tabbody">
	<p class="infomessage">Manage connect user role configurations.</p>
	<div>
    	<div id="rolemessage" class="msgsection"></div>
    	<div id="roleerror" class="errorsection"></div>
    	<button class="block button top-spacer" id="btnNewRole">Create New Role</button>
  	</div>
  
	<div class="top-spacer"  style="margin-left: -20px" >
  		<div id="allroletable" class="table-cell smart-table">
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell">Role Name</div>
				<div class="table-cell smart-table-cell"></div>
			</div>
  		</div>

		<div id="roledetails" style="width:100%;" class="table-cell border-section">
			<div class="pageheader">Role Details</div>
			<div id="roledetailinner">
				<div id="roleinfodefaults"></div>
				
				<div class="add_select_section">
					<select id="roleActionKey" class="select-limit"><option>Loading...</option></select>
					<select id="roleActionResourceKey" class="select-limit"><option>Loading...</option></select>
					<button id="addRoleAction" class="block button " style = "padding: 0px; display: inline;">Add Action</button>
				</div>
			</div>
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
<div id="newRoleDialog" style="display: none;" class="dialog">
  <div class="dialog-title">Create New Role</div>
  <div id="roledialogerror" class="errorsection"></div>
  <div>Create a new role.</div>
  <form id="newroleform" >
    <label class="block top-spacer">Rolename:</label>
    <input type="text" name="rolename" class="formtext block" />
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="Create Role" />
     <input class="button" type="button" id="cancelnewrole" value="Cancel" />
    </div>
  </form>
  </div>
</body>
</html>