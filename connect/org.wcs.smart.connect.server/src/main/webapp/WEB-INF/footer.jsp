<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>


</div> <!--  end core div -->

<div id="footerid" class="float">
	<c:if test="${empty style_footername }">
  		<img class="float " src="${pageContext.request.contextPath}/css/images/smart_logo.png">
  		<p class="float"> Copyright 2015-2017</p>
	</c:if>
	<c:if test="${not empty style_footername }">
  		${style_footername}
	</c:if>
</div>

