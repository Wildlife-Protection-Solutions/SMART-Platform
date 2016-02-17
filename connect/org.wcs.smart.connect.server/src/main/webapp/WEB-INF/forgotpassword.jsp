<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title><fmt:message key="forgotpassword.pagetitle"/></title>
<script type="text/javascript">

function resetPassword(){
	document.querySelector("input[name=reset]").disabled=true;
	
	var CAURL = "./reset";
	var user = document.querySelector("input[name=username]").value;
	var url = CAURL + "?username=" + encodeURIComponent(user);
	
	document.querySelector("#error").innerHTML = "<fmt:message key="forgotpassword.processing"/>";
	document.querySelector("#error").className="msgsection";
	document.querySelector("#error").style.display="block";
	
	var oReq = new XMLHttpRequest();
	oReq.onload = resetcomplete;
	oReq.open("POST", url, true);
	oReq.send();
	
	return false;
}

function resetcomplete(){
	if (this.status == 200) {
		document.querySelector("#error").innerHTML = "<fmt:message key="forgotpassword.emailsent"/>";
		document.querySelector("#error").className="msgsection";
		document.querySelector("#error").style.display="block";
	}else{
		document.querySelector("#error").innerHTML = "<fmt:message key="forgotpassword.emailerror"/>";
		document.querySelector("#error").className="errorsection";
		document.querySelector("#error").style.display="block";
	}
	document.querySelector("input[name=reset]").disabled=false;
}

</script>

</head>
<body>


<%@include file="header.jsp" %>
<div style="display: table-row; height:20px"></div>
<div style="display: table-row;">
  <div style="min-width: 150px;" class="verticalmenu"></div>
  
  <div id="main">
  <div class="pageheader"><fmt:message key="forgotpassword.sectiontitle"/></div>
  <p><fmt:message key="forgotpassword.message"/>
  </p>
  <div id="error" class="errorsection" style="display: none"></div>
   <form  style="width:200px;" id="resetform" onsubmit="return resetPassword();">
   <label class="top-spacer block"><fmt:message key="forgotpassword.username"/></label>
   <input type="text" name="username" class="block formtext" value="" tabindex="1"/>
   <input class="button block top-spacer" type="submit" name="reset" value="<fmt:message key="forgotpassword.resetbutton"/>" style="width: 100px" tabindex="3"/>
  </form>
</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>