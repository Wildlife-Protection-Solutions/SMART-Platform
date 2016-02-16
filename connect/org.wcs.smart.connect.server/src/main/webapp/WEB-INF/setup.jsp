<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title>SMART Connect - Configuration</title>
<script type="text/javascript">

function login(){
	var user = document.querySelector("input[name=username]").value;
	var pass = document.querySelector("input[name=password1]").value;
	var pass2 = document.querySelector("input[name=password2]").value;

	if (user.length == 0 ){
		document.querySelector("#error").innerHTML = "Username is required";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass.length == 0){
		document.querySelector("#error").innerHTML = "Password is required";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass2.length == 0){
		document.querySelector("#error").innerHTML = "Password is required";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass != pass2){
		document.querySelector("#error").innerHTML = "Passwords do not match";
		document.querySelector("#error").style.display="block";
		return false;
	}
	return true;
}
</script>

</head>
<body>
<%@include file="header.jsp" %>

<div style="display: table-row;">
  <div style="min-width: 150px;" class="verticalmenu"></div>
  <div id="main">
   <p class="pageheader">SMART Connect Initial Setup</p>
   <div id="error" class="errorsection" style="display: ${loginerror == null ? "none" : "block"}">${loginerror}</div>
   
   <p class="top-spacer">SMART Connect requires at least one administrator user.  Create your administrator user here, then
   use the information to log into Connect where you can further configure the system.</p>
    
   <form  style="width:200px;" action="${logintarget}" method="POST" id="newuserform" onsubmit="return login();">
   	<input type="hidden" name="createuser" value="firstuser"/>
    <label class="block top-spacer">Username:</label>
    <input type="text" name="username" class="formtext block" />
    <label class="block top-spacer">Email:</label>
    <input type="text" name="email" class="formtext block" />
    <label class="block top-spacer">Password:</label>
    <input type="password" name="password1" class="formtext table-row"/>
    <label class="block top-spacer">Re-enter Password:</label>
    <input type="password" name="password2" class="formtext table-row"/>
    <input class="button block top-spacer" type="submit" value="Create User" />
  </form>
</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>