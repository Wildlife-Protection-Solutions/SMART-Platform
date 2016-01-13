<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title>SMART Connect - Login</title>
<script type="text/javascript">

function login(){
	var user = document.querySelector("input[name=j_username]").value;
	var pass = document.querySelector("input[name=j_password]").value;

	if (user.length == 0 ){
		document.querySelector("#error").innerHTML = "Username is required";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass.length == 0){
		document.querySelector("#error").innerHTML = "Password is required";
		document.querySelector("#error").style.display="block";
		return false;
	}
	return true;
}
</script>

</head>
<body>


<%@include file="header.jsp" %>
<div style="display: table-row; height:20px"></div>
<div style="display: table-row;">
  <div style="min-width: 150px;" class="verticalmenu"></div>
  
  <div id="main">
   <form  style="width:200px;" action="${logintarget}" method="POST" id="loginform" onsubmit="return login();">
   <div id="error" class="errorsection" style="display: ${loginerror == null ? "none" : "block"}">${loginerror}</div>
   <label class="top-spacer block">Username:</label>
   <input type="text" name="j_username" class="block formtext" value="smart" tabindex="1"/>
   <label class="top-spacer block">Password:</label>
   <input type="password" name="j_password" class="formtext" value="smart" tabindex="2"/>
   <a href="${pageContext.request.contextPath}/forgot" class="block link_small" tabindex="4">Forgot Password?</a>
   <input class="button block top-spacer" type="submit" value="Login" style="width: 100px" tabindex="3"/>
  </form>
</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>