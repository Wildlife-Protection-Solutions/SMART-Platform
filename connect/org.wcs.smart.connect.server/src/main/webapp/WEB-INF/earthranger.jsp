<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html style="height: 100%;">
<head>

<%@include file="includes.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/fontawesome.min.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/solid.min.css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/css/fontawesome/css/regular.min.css" />
<title>SMART - Earth Ranger</title>
</head>


<script type="text/javascript">

function setUrl(){
	var url = document.getElementById('caselect').value;
	var iframe = document.getElementById('erframe');
	iframe.setAttribute("src", url);
	document.getElementById("opennew").href = url;

}
</script>

<body style="${style_bodycss}">

<%@include file="header.jsp" %>
<%@include file="menu.jsp" %>

<div id="main">
  <div class="pageheader"><fmt:message key="earthranger.title"/></div>
 
  <div style="height: 100%; display: flex; flex-flow: column nowrap;">
    
    <c:if test="${empty caswither}">
    	<p style="margin-top:10px;">
    		<fmt:message key="earthranger.noca"/>
    	</p>
    </c:if>
	<c:if test="${!empty caswither}">
		<div style="display:flex; margin-top:10px; margin-bottom:10px;">
    		<select id="caselect" class="formtext"  onchange="setUrl()" >
				<c:forEach var="exp" items="${caswither}" varStatus="count">
					<option value="${exp[2]}"
						<c:if test="${exp[1]}">selected</c:if>									
					 >${exp[0]}</option> 
				</c:forEach>
			</select>
			<div style="margin-left:10px; margin-right:10px" > 
				<a href="${caswither[0][2]}" target="_blank" id="opennew" 
				class="fa-solid fa-arrow-up-right-from-square fa-xl" 
				title="<fmt:message key="earthranger.opennewwindow"/>"></a>
			</div>
		</div>
		<div style="height: 100%">
			<iframe id="erframe" src="${homeurl}"
			 style="display:flex; border: 1px solid #BBBBBB; width: 100%; height: 100%">
			</iframe>
		</div>
	</c:if>    
	
	
	</div>
   </div>
</body>
</html>