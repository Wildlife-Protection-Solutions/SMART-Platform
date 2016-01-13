<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title>SMART Connect - Password Reset</title>
<script type="text/javascript">

function resetPassword(){
	var CAURL = "./reset";
	var password1 = document.querySelector("input[name=password1]").value;
	var password2 = document.querySelector("input[name=password2]").value;
	var token = document.querySelector("input[name=resettoken]").value;
	if (password1 != password2){
		//error
		var msg = "Passwords do not match."
		document.querySelector("#error").innerHTML = msg;
		document.querySelector("#error").style.display="block";
		return false;
	}	
	var url = CAURL + "?resettoken=" + encodeURIComponent(token) + "&new=" + encodeURIComponent(password1);
	var oReq = new XMLHttpRequest();
	oReq.onload = resetcomplete;
	oReq.open("POST", url, true);
	oReq.send();
	return false;
}

function resetcomplete(){
	if (this.status == 200) {
		document.querySelector("#error").innerHTML = "Password reset.";
		document.querySelector("#error").className="msgsection";
		document.querySelector("#error").style.display="block";
	}else{
		var msg = "Error occurred resetting password. "
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		msg += "  For further help contact your Connect administrator.";
		
		document.querySelector("#error").innerHTML = msg;
		document.querySelector("#error").style.display="block";
		
	}
	document.querySelector("#resetform").style.display="none";
}

</script>
</head>
<body>

<%@include file="header.jsp" %>
<div style="display: table-row; height:20px"></div>
<div style="display: table-row;">
  <div style="min-width: 150px;" class="verticalmenu"></div>
  
	<div id="main">
		<c:choose>
			<c:when test="${resettoken != null}">
		  		<div class="pageheader">Password Reset</div>
		  		<p>To reset your password, enter a new password and press Submit.</p>
		  		<div id="error" class="errorsection" style="display: none"></div>
		  
		   		<form  style="width:200px;" id="resetform" onsubmit="return resetPassword();">
		   			<input type="hidden" name="resettoken" class="block formtext" value="${resettoken}"/>
		   			<label class="top-spacer block">New Password:</label>
		   			<input type="password" name="password1" class="block formtext" value="" tabindex="1"/>
		   			<label class="top-spacer block">Re-enter Password:</label>
		   			<input type="password" name="password2" class="block formtext" value="" tabindex="1"/>
		   			<input class="button block top-spacer" type="submit" value="Submit" style="width: 100px" tabindex="3"/>
		  		</form>
			</c:when>
			<c:when test="${resettoken == null}">
				<p>Page Not Found.</p>
			</c:when>
		</c:choose>
	</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>