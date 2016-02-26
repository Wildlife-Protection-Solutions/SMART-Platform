<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title><fmt:message key="login.pagetitle"/></title>
<script type="text/javascript">

function login(){
	var user = document.querySelector("input[name=j_username]").value;
	var pass = document.querySelector("input[name=j_password]").value;

	if (user.length == 0 ){
		document.querySelector("#error").innerHTML = "<fmt:message key="login.userrequired"/>";
		document.querySelector("#error").style.display="block";
		return false;
	}else if (pass.length == 0){
		document.querySelector("#error").innerHTML = "<fmt:message key="login.passwordrequired"/>";
		document.querySelector("#error").style.display="block";
		return false;
	}
	return true;
}
</script>

</head>
<body onload="setStyle(false)">


<%@include file="header.jsp" %>
<div style="display: table-row; height:20px"></div>
<div style="display: table-row;">
  <div style="min-width: 150px;" class="verticalmenu"></div>
  
  <div id="main">
  <div id="error" class="errorsection" style="display: ${loginerror == null ? "none" : "block"}">${loginerror}</div>
  <div id="login_left">
   <form  style="width:200px;" action="${logintarget}" method="POST" id="loginform" onsubmit="return login();">
	   <label class="top-spacer block"><fmt:message key="login.usernamelabel"/></label>
	   <input type="text" name="j_username" class="block formtext" value="smart" tabindex="1"/>
	   <label class="top-spacer block"><fmt:message key="login.passwordlabel"/></label>
	   <input type="password" name="j_password" class="formtext" value="smart" tabindex="2"/>
	   <a href="${pageContext.request.contextPath}/forgot" class="block link_small" tabindex="4"><fmt:message key="login.forgot"/></a>
	   <input class="button block top-spacer" type="submit" value="<fmt:message key="login.login"/>" style="width: 100px" tabindex="3"/>
  </form>
  </div>
  <div id="login_right">
  </div>
  
</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>