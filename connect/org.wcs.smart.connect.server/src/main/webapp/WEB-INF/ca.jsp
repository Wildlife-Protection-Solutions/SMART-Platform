<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/table.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/infoerror.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/ca.js"></script>
	<script type="text/javascript" src="${pageContext.request.contextPath}/javascript/dialog.js"></script>
	
	<title><fmt:message key="ca.pagetitle"/></title>
	
</head>

<body style="${style_bodycss}">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id="main">
		<div class="pageheader"><fmt:message key="ca.pageheader"/></div>
		<p class="infomessage"><fmt:message key="ca.pageinfo"/></p>
		<div><div id="message" class="msgsection"></div></div>
		<div class="top-spacer"  style="margin-left: -20px" >
  			<div class="catable table-cell smart-table">
  				<div class="table-row smart-table-header">
					<div class="table-cell smart-table-cell"><fmt:message key="ca.labelheader"/></div>
					<div class="table-cell smart-table-cell"><fmt:message key="ca.statusheader"/></div>
					<div class="table-cell smart-table-cell"></div>
					<div class="table-cell smart-table-cell"></div>
					<div class="table-cell smart-table-cell"></div>
				</div>
				<c:forEach var="ca" items="${cas}" varStatus="count">
					<div data-cauuid ="${ca.getUuid()}" class="carow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
						<div class="table-cell smart-table-cell">${ca.getLabel()}</div>
						<div class="table-cell smart-table-cell">${ca.getStatus()}</div>
						<div class="table-cell smart-table-cell"><a href="" class="info-icon" id="infoca" data-cauuid = "${ca.getUuid()}" title="<fmt:message key="ca.details"/>"></a></div>
						<div class="table-cell smart-table-cell ">
							<c:if test="${ca.getStatus() == 'DATA'}">
								<a href=""  data-cauuid = "${ca.getUuid()}" title="<fmt:message key="ca.downloadtooltip"/>" class="downloadca download-icon"></a>
							</c:if>
						</div>
						<div class="table-cell smart-table-cell "><a href=""  data-status = "${ca.getStatus()}" data-cauuid = "${ca.getUuid()}" data-label="${ca.getLabel()}" title="<fmt:message key="ca.deletetooltip"/>" class="deleteca delete-icon"></a></div>
					</div>

				</c:forEach>
			</div>  

		</div>
		<c:if test="${canadd}">
			<div><button id="btnNewCa" class="block button top-spacer"><fmt:message key="ca.createnew"/></button></div>
		</c:if>
	</div>
	
	<%@include file="footer.jsp" %>

	<div id="deleteDialog" style="display: none;" class="dialog">
	  <div class="dialog-title"><fmt:message key="ca.deletecatitle"/></div>
	  <div id="dialogerror" class="errorsection"></div>
	  
	  <form id="deleteform" onsubmit="return deleteca();" >
	    <input type="hidden" name="cauuid"/>
	    <input type="hidden" name="label"/>
	    <div id="confirmtype">
	    	<p><fmt:message key="ca.deleteconfirm"/></p>
	    	<input type="radio" name="caoption" value="desktop" checked/><fmt:message key="ca.deletedesktop"/><br>
	    	<input type="radio" name="caoption"  value="all"/><fmt:message key="ca.deleteall"/><br><br>
	    </div>
	    <p><fmt:message key="ca.userpassword"/></p>
	    <label class="block"><fmt:message key="ca.userlabel"/></label>
	    <input type="text" name="username" class="formtext block" />
	
	    <div class="block top-spacer"><fmt:message key="ca.passwordlabel"/></div>
	    <input type="password" name="password" class="formtext block" />
	    
	    <div class="block top-spacer" style="text-align:right">
	     <input class="button" type="submit" value="<fmt:message key="ca.deletebutton"/>" />
	     <input class="button" type="button" value="<fmt:message key="ca.cancelbutton"/>" onclick="closeDialog('deleteDialog')" />
	    </div>
	  </form>
  </div>
  
  
  <div id="caInfoDialog" style="display: none; width:360px" class="dialog">
    <div class="dialog-title"><fmt:message key="ca.cadetailstitle"/></div>
  	<div class="smart-table">
  	<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.label"/>:</div>
			<div id="infolabel" class="table-cell smart-table-cell"></div>
		</div>
  		<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.description"/>:</div>
			<div id="infodescription" class="table-cell smart-table-cell"></div>
		</div>
		<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.designation"/>:</div>
			<div id="infodesignation" class="table-cell smart-table-cell"></div>
		</div>
  		<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.uuidheader"/>:</div>
			<div id="infouuid" class="table-cell smart-table-cell"></div>
		</div>
		<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.statusheader"/>:</div>
			<div id="infostatus" class="table-cell smart-table-cell"></div>
		</div>
		<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.versionheader"/>:</div>
			<div id="infoversion" class="table-cell smart-table-cell"></div>
		</div>
		<div class="table-row">
			<div class="table-cell smart-table-cell"><fmt:message key="ca.revision"/>:</div>
			<div id="inforevision" class="table-cell smart-table-cell"></div>
		</div>
		<div class="block top-spacer" style="text-align:right">
     	<input class="button" type="button" value="<fmt:message key="ca.cancelbutton"/>" onclick="closeDialog('caInfoDialog'); return false;" />
    	</div>
  	</div>  
  </div>
  
  <div id="downloadDialog" style="display: none;" class="dialog">
    <div class="dialog-title"><fmt:message key="ca.downloadtitle"/></div>
    <div id="dialogerror" class="errorsection"></div>
    <p><fmt:message key="ca.downloadinfo"/></p>
   	<div id="statusurl" style="font-size:0.8em"></div>
   	<div class="block top-spacer" style="text-align:right">
     <input class="button" type="button" value="<fmt:message key="ca.cancelbutton"/>" onclick="return cancelCaDownload();" />
    </div>
  </div>
  
<div id="newDialog" style="display: none;" class="dialog">
  <div class="dialog-title"><fmt:message key="ca.createtitle"/></div>
  <div id="dialogerror" class="errorsection"></div>
  
  <form id="createform" onsubmit="return createca();" >
    <div id="confirmtype">
    	<p class="top-spacer"><fmt:message key="ca.createinfo"/></p>
    	<p class="top-spacer"><fmt:message key="ca.createmessage1"/></p>
    	<p class="top-spacer"><fmt:message key="ca.createmessage2"/></p>
    </div>
    <label class="block top-spacer"><fmt:message key="ca.createlabel"/></label>
    <input type="text" name="calabel" class="formtext block" />
	<label class="block top-spacer"><fmt:message key="ca.createuuid"/></label>
    <input type="text" name="newcauuid" class="formtext block" />
    
    <div class="block top-spacer" style="text-align:right">
     <input class="button" type="submit" value="<fmt:message key="ca.createbutton"/>" />
     <input class="button" type="button" value="<fmt:message key="ca.cancelbutton"/>" onclick="closeDialog('newDialog')" />
    </div>
  </form>
  </div>
</body>
</html>