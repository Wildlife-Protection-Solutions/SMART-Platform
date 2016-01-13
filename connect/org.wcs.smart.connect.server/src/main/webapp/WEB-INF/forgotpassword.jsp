<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title>SMART Connect - Password Reset</title>
<script type="text/javascript">

function resetPassword(){
	document.querySelector("input[name=reset]").disabled=true;
	
	var CAURL = "./reset";
	var user = document.querySelector("input[name=username]").value;
	var url = CAURL + "?username=" + encodeURIComponent(user);
	
	document.querySelector("#error").innerHTML = "Processing...";
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
		document.querySelector("#error").innerHTML = "An email has been sent with further instructions.";
		document.querySelector("#error").className="msgsection";
		document.querySelector("#error").style.display="block";
	}else{
		document.querySelector("#error").innerHTML = "Error occurred.  Contact your Connect adminstrator.";
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
  <div class="pageheader">Forgot Password</div>
  <p>To reset your password you must provide your Connect username. If you supplied an email with your account you will be emailed a link
  where you can use to reset your Connect password.  If you have not supplied an email with your Connect account you will
  have to contact your Connect administrator to reset your password.
  </p>
  <div id="error" class="errorsection" style="display: none"></div>
   <form  style="width:200px;" id="resetform" onsubmit="return resetPassword();">
   <label class="top-spacer block">Username:</label>
   <input type="text" name="username" class="block formtext" value="smart" tabindex="1"/>
   <input class="button block top-spacer" type="submit" name="reset" value="Reset" style="width: 100px" tabindex="3"/>
  </form>
</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>