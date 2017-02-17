<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<%@include file="includes.jsp" %>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/myaccount.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/sharedlinkfunctions.js"></script>

<title><fmt:message key="myaccount.pagetitle"/></title>

<script>
	// Updates user information.  This will update the username
	// and email only.  Will not update password
	var USER_URL = "../api/connectuser/";
	
	function updateUser() {
		var toUpdate = document.querySelector("input[name=currentuser]").value;
		var user = document.querySelector("input[name=username]").value;
		var email = document.querySelector("input[name=email]").value;
		if (user.length == 0) {
			displayError("<fmt:message key="myaccount.userrequired" />");
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
		hideInfo();

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
			displayInfo("<fmt:message key="myaccount.accountupdated" />");
			
			location.reload() //this will log you out of your old username if it changed.
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
<body style="${style_bodycss}">
<%@include file="header.jsp" %>


<%@include file="menu.jsp" %>
<div id="main">
<div class="pageheader"><fmt:message key="myaccount.sectiontitle"/></div>
<div id="message" class="msgsection"><fmt:message key="myaccount.accountupdated"/></div>

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




<div id="SharedLinksDialog" style="display: none;" class="level2dialog">
  <div class="dialog-title"><fmt:message key="query.sharequery" /></div>
  	<form id="sharedlinkform" name="sharedlinkform">
     		<div style="text-align: right">
				<div id='createcustomlink' >
					<fieldset class="linkdialog">
						<p><fmt:message key="sharedlinks.tokenexplanation"/> </p>
						
						<table class="top-spacer" style="width:100%">
						  <tr>
						   <td><fmt:message key="query.numminutes" /></td>
						   <td>
							<select id="quickMinSelect">
								<option value=60>1 hour</option>
								<option value=1440>1 day</option>
								<option value=10080>1 week</option>
								<option value=43200>1 month</option>
								<option value=259200>6 months</option>
								<option value=518400>1 year</option>
								<option value=-1>Custom...</option>
							</select>
						  </td>
						   <td><input id="expiresAfter" type="number" name="expiresAfter" value=60 style='width:65px' min="0" max="2147483647" disabled> <fmt:message key="query.numminutes2"/></td>
						  </tr>
						  <tr >
						     <td colspan=3 align="center"><input id="createlinkbutton" class="close" type="button" value="<fmt:message key="query.creatbutton"/>" /></td>
						  </tr>
						  <tr >
						     <td colspan=3><input id="createdlink" class="hide linkdialog" type="text"/></td>
						  </tr>
						</table>
	   				</fieldset>	   				
	   			</div>
	   			<div>
	   				<input id="close" class="close" type="button" value="<fmt:message key="query.closebutton"/>" />
	   			</div>
   			</div>
    	</form>
</div>
