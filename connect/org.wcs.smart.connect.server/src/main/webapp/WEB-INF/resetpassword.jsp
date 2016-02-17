<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%@include file="includes.jsp" %>
<title><fmt:message key="resetpassword.pagetitle"/></title>
<script type="text/javascript">

function resetPassword(){
	var CAURL = "./reset";
	var password1 = document.querySelector("input[name=password1]").value;
	var password2 = document.querySelector("input[name=password2]").value;
	var token = document.querySelector("input[name=resettoken]").value;
	if (password1 != password2){
		//error
		var msg = "<fmt:message key="resetpassword.passdontmatch"/>"
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
		document.querySelector("#error").innerHTML = "<fmt:message key="resetpassword.passreset"/>  <a href='../connect'><fmt:message key="resetpassword.home"/></a> ";
		document.querySelector("#error").className="msgsection";
		document.querySelector("#error").style.display="block";
	}else{
		var msg = "<fmt:message key="resetpassword.reseterror"/> "
		try {
			msg += JSON.parse(this.responseText).error
		} catch (err) {
		}
		msg += "  <fmt:message key="resetpassword.reseterror2"/>";
		
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
		  		<div class="pageheader"><fmt:message key="resetpassword.title"/></div>
		  		<p><fmt:message key="resetpassword.info"/></p>
		  		<div id="error" class="errorsection" style="display: none"></div>
		  
		   		<form  style="width:200px;" id="resetform" onsubmit="return resetPassword();">
		   			<input type="hidden" name="resettoken" class="block formtext" value="${resettoken}"/>
		   			<label class="top-spacer block"><fmt:message key="resetpassword.newpass1"/></label>
		   			<input type="password" name="password1" class="block formtext" value="" tabindex="1"/>
		   			<label class="top-spacer block"><fmt:message key="resetpassword.newpass2"/></label>
		   			<input type="password" name="password2" class="block formtext" value="" tabindex="1"/>
		   			<input class="button block top-spacer" type="submit" value="<fmt:message key="resetpassword.submit"/>" style="width: 100px" tabindex="3"/>
		  		</form>
			</c:when>
			<c:when test="${resettoken == null}">
				<p><fmt:message key="resetpassword.pagenotfound"/></p>
			</c:when>
		</c:choose>
	</div>
</div>

<%@include file="footer.jsp" %>
</body>
</html>