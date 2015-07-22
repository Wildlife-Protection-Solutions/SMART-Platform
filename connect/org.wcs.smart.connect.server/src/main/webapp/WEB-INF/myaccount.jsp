<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<title>SMART Connect - User Account</title>

<script>
	// Updates user information.  This will update the username
	// and email only.  Will not update password
	var USER_URL = "../api/connectuser/";
	
	function updateUser() {
		var toUpdate = document.querySelector("input[name=currentuser]").value;
		var user = document.querySelector("input[name=username]").value;
		var email = document.querySelector("input[name=email]").value;
		if (user.length == 0) {
			document.querySelector("#error").innerHTML = "<fmt:message key="myaccount.userrequired" />";
			document.querySelector("#error").style.display = "block";
			return false;
		}

		var newdata = {
			"username" : user,
			"email" : email
		};

		//make ajax call
		updateUserAjax(toUpdate, newdata);
		return false;
	}

	//calls the POST REST api for the user update
	function updateUserAjax(userName, jsonData) {
		hideError();
		document.querySelector("#message").style.display = "none";

		var oReq = new XMLHttpRequest();
		oReq.onload = userUpdated;
		oReq.open("PUT", USER_URL + userName, true);
		oReq.setRequestHeader("Content-type", "application/json");
		oReq.send(JSON.stringify(jsonData));
	}

	//callback for update user ajax 
	function userUpdated() {
		if (this.status == 200) {
			//ok
			document.querySelector("#message").style.display = "block";
			var user = JSON.parse(this.responseText);

			document.querySelector("input[name=currentuser]").innerHTML = user.username;
			document.querySelector("input[name=username]").innerHTML = user.username;
			document.querySelector("input[name=email]").innerHTML = user.email;
		} else {
			//fail
			var msg = "<fmt:message key="myaccount.updateerror"/>";
			try {
				msg = JSON.parse(this.responseText).error
			} catch (err) {
			}

			displayError(msg);
		}
	}

	//hide error message
	function hideError() {
		document.querySelector("#error").style.display = "none";
	}
	//displays error message
	function displayError(msg) {
		document.querySelector("#error").style.display = "block";
		document.querySelector("#error").innerHTML = msg;
	}

	//makes and AJAX REST call to update username
	function updatePassword() {
		var currentpass = document.querySelector("input[name=currentpassword]").value;
		var pass = document.querySelector("input[name=password]").value;
		var pass2 = document.querySelector("input[name=password1]").value;

		document.querySelector("#dialogerror").style.display = "none";
		if (pass != pass2) {
			document.querySelector("#dialogerror").style.display = "block";
			document.querySelector("#dialogerror").innerHTML = "<fmt:message key="myaccount.passdonotmatch"/>";
			return false;
		}

		closeDialog('passwordDialog');

		var toUpdate = document.querySelector("input[name=currentuser]").value;
		var newdata = {
			"oldpassword" : currentpass,
			"password" : pass
		};
		updateUserAjax(toUpdate, newdata);
		return false;
	}
</script>

</head>
<body>
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader">My Account</div>
<div id="message" class="msgsection"><fmt:message key="myaccount.accountupdated"/></div>
<div id="error" class="errorsection"></div>
<p class="infomessage"><fmt:message key="myaccount.info"/></p>

<form style="width:220px; display: block" id="userform" onsubmit="return updateUser();" >
    <input type="hidden" name="currentuser" value="${username}"/>
    <label class="block top-spacer"><fmt:message key="myaccount.username"/>:</label>
    <input type="text" name="username" class="block formtext" value="${username}"/>
    <label class="block top-spacer"><fmt:message key="myaccount.email"/>:</label>
    <input type="text" name="email" class="block formtext" value="${email}"/>
    <div class="block infotext"><fmt:message key="myaccount.emailinfo"/></div>
    <a class="block top-spacer" href="javascript:displayDialog('passwordDialog', 'main');"><fmt:message key="myaccount.changepassword"/></a>
    <input class="block button top-spacer" style="min-width: 100px" type="submit" value="<fmt:message key="myaccount.save"/>" id="submit" />
  </form>
</div>

<%@include file="footer.jsp" %>

<div id="passwordDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="myaccount.passtitle"/></div>
  <div id="dialogerror" class="errorsection"></div>
  
  <form id="passform" onsubmit="return updatePassword();" >
    <label class="block top-spacer"><fmt:message key="myaccount.currentpass"/>:</label>
    <input type="password" name="currentpassword" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="myaccount.newpass"/></label>
    <div class="block infotext"><fmt:message key="myaccount.passrequirement"/></div>
    <input type="password" name="password" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="myaccount.newpass2"/>:</label>
    <input type="password" name="password1" class="formtext table-row"/>
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="<fmt:message key="myaccount.changepassbtn"/>" />
     <input class="button" type="button" value="<fmt:message key="myaccount.cancelbtn"/>" onclick="closeDialog('passwordDialog')" />
    </div>
  </form>
  </div>
</body>
</html>