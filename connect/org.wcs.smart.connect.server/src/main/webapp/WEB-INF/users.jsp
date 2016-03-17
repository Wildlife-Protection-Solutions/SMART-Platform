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

<title><fmt:message key="users.pagetitle"/></title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
	<div class="tabheader pageheader">
  		<a id="users" class="tab "><fmt:message key="users.usertitle"/></a>
  		<a id="userroles" class="tab "><fmt:message key="users.roletitle"/></a>
  	</div>
  	<div>
    	<div id="message" class="msgsection"></div>
  	</div>
  <!--  user tab section  -->
  <div id="users_body" class="tabbody">
  	<p class="infomessage"><fmt:message key="users.usermessage"/></p>
  	<div>
    	<button class="block button top-spacer" id="btnNewUser"><fmt:message key="users.createnewbutton"/></button>
  	</div>
  
	<div class="top-spacer"  style="margin-left: -20px" >

		<div class="user-tables-wrapper user-tables-float">
  		<div id="usertable" class="user-tables-float table-cell smart-table">
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell"><fmt:message key="users.userlabel"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="users.emaillabel"/></div>
				<div class="table-cell smart-table-cell"></div>
				<div class="table-cell smart-table-cell"></div>
			</div>
			<c:forEach var="user" items="${users}" varStatus="count">
			<div data-username ="${user.getUsername()}" class="smartuser userrow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
				<div class="table-cell smart-table-cell">${user.getUsername()}</div>
				<div class="table-cell smart-table-cell">${user.getEmail()}</div>
				<div class="table-cell smart-table-cell "><a href="" data-username = "${user.getUsername()}" data-email="${user.getEmail()}" title="<fmt:message key="users.editusertooltip"/>" class="edituser update-icon"></a></div>
 				<div class="table-cell smart-table-cell "><a href="" data-username = "${user.getUsername()}" title="<fmt:message key="users.deactivateusertooltipusertooltip"/>" class="deactivateuser delete-icon"></a></div> 
<%-- 				<div class="table-cell smart-table-cell "><a href="" data-username = "${user.getUsername()}" title="<fmt:message key="users.deleteusertooltip"/>" class="deleteuser delete-icon"></a></div> --%>

			</div>
			</c:forEach>  
  		</div>
		
 
  		<div id="inactiveusertable" class=" user-tables-float user-tables-clear table-cell smart-table">
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell"><fmt:message key="users.disableduserlabel"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="users.emaillabel"/></div>
				<div class="table-cell smart-table-cell"></div>
				<div class="table-cell smart-table-cell"></div>
			</div>
			<c:forEach var="user" items="${inactiveusers}" varStatus="count">
			<div data-username ="${user.getUsername()}" class="smartinactiveuser inactiveuserrow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
				<div class="table-cell smart-table-cell">${user.getUsername()}</div>
				<div class="table-cell smart-table-cell">${user.getEmail()}</div>
				<div class="table-cell smart-table-cell "><a href="" data-username = "${user.getUsername()}" title="<fmt:message key="users.activateusertooltip"/>" class="activateuser run-icon"></a></div>
				<div class="table-cell smart-table-cell "><a href="" data-username = "${user.getUsername()}" title="<fmt:message key="users.deleteusertooltip"/>" class="deleteuser delete-icon"></a></div>
			</div>
			</c:forEach>  
  		</div>
  		</div>


		<div id="userdetails" style="width:100%;" class="table-cell border-section">
			<div class="pageheader"><fmt:message key="users.userdetails"/></div>
			<div id="userinfo">
				<div id="userinfodefaults"></div>
				<div id="userinfobuttons"class="tabheader" style="background-color:#EFEFEF; margin-top:25px;">
					<a id="roletab" class="tab"><fmt:message key="users.rolestab"/></a>
					<a id="actiontab" class="tab"><fmt:message key="users.actionstab"/></a>
				</div>
				<div id="roletab_body" class="tabbody">
					<div class="add_select_section">
						<select id="roleKey" class="select-limit"><option><fmt:message key="users.loading"/></option></select>
						<button id="addRole" class="block button " style = "padding: 0px; display: inline;"><fmt:message key="users.addrolebutton"/></button>
					</div>
				</div>
				<div id="actiontab_body" class="tabbody">
					<div class="add_select_section">
						<select id="actionKey" class="select-limit"><option><fmt:message key="users.loading"/></option></select>
						<select id="actionResourceKey" class="select-limit"><option><fmt:message key="users.loading"/></option></select>
						<button id="addAction" class="block button " style = "padding: 0px; display: inline;"><fmt:message key="users.addactionbutton"/></button>
					</div>
				</div>
			</div>
		</div>

	</div>
	</div>
	
	<!--  roles tab section  -->
	<div id="userroles_body" class="tabbody">
	<p class="infomessage"><fmt:message key="users.rolemessage"/></p>
	<div>
    	<div id="rolemessage" class="msgsection"></div>
    	<div id="roleerror" class="errorsection"></div>
    	<button class="block button top-spacer" id="btnNewRole"><fmt:message key="users.newrolebutton"/></button>
  	</div>
  
	<div class="top-spacer"  style="margin-left: -20px" >
  		<div id="allroletable" class="table-cell smart-table">
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell"><fmt:message key="users.rolelabel"/></div>
				<div class="table-cell smart-table-cell"></div>
				<div class="table-cell smart-table-cell"></div>
			</div>
  		</div>

		<div id="roledetails" style="width:100%;" class="table-cell border-section">
			<div class="pageheader"><fmt:message key="users.roledetails"/></div>
			<div id="roledetailinner">
				<div id="roleinfodefaults"></div>
				
				<div class="add_select_section">
					<select id="roleActionKey" class="select-limit"><option><fmt:message key="users.loading"/></option></select>
					<select id="roleActionResourceKey" class="select-limit"><option><fmt:message key="users.loading"/></option></select>
					<button id="addRoleAction" class="block button " style = "padding: 0px; display: inline;"><fmt:message key="users.roleaddactionbutton"/></button>
				</div>
			</div>
		</div>

	</div>
	</div>
</div>
<%@include file="footer.jsp" %>

<div id="editUserDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="users.updatedialogtitle"/></div>
  <div id="dialogerror" class="errorsection"></div>
  <div id="msg"></div>
  <form id="edituserform" >
  	<input type="hidden" name="edit_username_orig"  />
    <label class="block top-spacer"><fmt:message key="users.updateusernamelabel"/></label>
    <input type="text" name="edit_username" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="users.updateuseremaillabel"/></label>
    <input type="text" name="edit_email" class="formtext block" />
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="<fmt:message key="users.updateuserbutton"/>" />
     <input class="button" type="button" id="canceledituser" value="<fmt:message key="users.cancel"/>" />
    </div>
  </form>
  </div>
<div id="newUserDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="users.newdialogtitle"/></div>
  <div id="dialogerror" class="errorsection"></div>
  <div><fmt:message key="users.newmessage"/></div>
  <form id="newuserform" >
    <label class="block top-spacer"><fmt:message key="users.newusernamelabel"/></label>
    <input type="text" name="username" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="users.newuseremaillabel"/></label>
    <input type="text" name="email" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="users.newpass1"/></label>
    <input type="password" name="password1" class="formtext table-row"/>
    <label class="block top-spacer"><fmt:message key="users.newpass2"/></label>
    <input type="password" name="password2" class="formtext table-row"/>
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="<fmt:message key="users.newuserbutton"/>" />
     <input class="button" type="button" id="cancelnewuser" value="<fmt:message key="users.cancel"/>" />
    </div>
  </form>
  </div>
<div id="newRoleDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="users.newrole"/></div>
  <div id="roledialogerror" class="errorsection"></div>
  <div><fmt:message key="users.newrolemessage"/></div>
  <form id="newroleform" >
    <label class="block top-spacer"><fmt:message key="users.newrolenamelabel"/></label>
    <input type="hidden" name="roleid" />
    <input type="text" name="rolename" class="formtext block" />
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" id="createrolebtn" value="<fmt:message key="users.newrolebutton"/>" />
     <input class="button" type="button" id="cancelnewrole" value="<fmt:message key="users.cancel"/>" />
    </div>
  </form>
  </div>
</body>
</html>