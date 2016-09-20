<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/causers.js"></script>

<title><fmt:message key="users.pagetitle"/></title>
</head>
<body style="${style_bodycss}">
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
	<div class="tabheader pageheader">
  		<fmt:message key="users.usertitle"/>
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
  		</div>
 
  		<div id="inactiveusertable" class=" user-tables-float user-tables-clear table-cell smart-table">
  			<div class="table-row smart-table-header">
				<div class="table-cell smart-table-cell"><fmt:message key="users.disableduserlabel"/></div>
				<div class="table-cell smart-table-cell"><fmt:message key="users.emaillabel"/></div>
				<div class="table-cell smart-table-cell"></div>
				<div class="table-cell smart-table-cell"></div>
			</div>
  		</div>
  		</div>


		<div id="userdetails" style="width:100%;" class="table-cell border-section">
			<div class="pageheader"><fmt:message key="users.userdetails"/></div>
			<div id="userinfo">
				<div id="userinfodefaults"></div>
				<div id="userinfobuttons"class="tabheader" style="background-color:#EFEFEF; margin-top:25px;">
					<fmt:message key="users.actionstab"/>
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

</body>
</html>