<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title><fmt:message key="setup.pagetitle"/></title>
<script type="text/javascript">

function login(){
	var user = document.querySelector("input[name=username]").value;
	var pass = document.querySelector("input[name=password1]").value;
	var pass2 = document.querySelector("input[name=password2]").value;

	if (user.length == 0 ){
		document.querySelector("#error").innerHTML = "<fmt:message key="setup.userrequired"/>";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass.length == 0){
		document.querySelector("#error").innerHTML = "<fmt:message key="setup.passrequired"/>";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass2.length == 0){
		document.querySelector("#error").innerHTML = "<fmt:message key="setup.passrequired"/>";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass != pass2){
		document.querySelector("#error").innerHTML = "<fmt:message key="setup.passdonotmatch"/>";
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
   <p class="pageheader"><fmt:message key="setup.title"/></p>
   <div id="error" class="errorsection" style="display: ${loginerror == null ? "none" : "block"}">${loginerror}</div>
   
   <p class="top-spacer"><fmt:message key="setup.message"/></p>
    
   <form  style="width:200px;" action="${logintarget}" method="POST" id="newuserform" onsubmit="return login();">
   	<input type="hidden" name="createuser" value="firstuser"/>
    <label class="block top-spacer"><fmt:message key="setup.usernamelabel"/></label>
    <input type="text" name="username" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="setup.emaillabel"/></label>
    <input type="text" name="email" class="formtext block" />
    <label class="block top-spacer"><fmt:message key="setup.passlabel1"/></label>
    <input type="password" name="password1" class="formtext table-row"/>
    <label class="block top-spacer"><fmt:message key="setup.passlabel2"/></label>
    <input type="password" name="password2" class="formtext table-row"/>
    <input class="button block top-spacer" type="submit" value="<fmt:message key="setup.createbutton"/>" />
  </form>
</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>