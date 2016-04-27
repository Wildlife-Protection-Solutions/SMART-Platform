<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
	<%@include file="includes.jsp" %>
	
	<title><fmt:message key="ca.pagetitle"/></title>
	
</head>

<body style="${style_bodycss}">
	<%@include file="header.jsp" %>
	<%@include file="menu.jsp" %>
	<div id="main">
		<div class="pageheader">SMART Connect System Information</div>
		
		
		<div>
			<p>Connect Version: ${connectVersion}</p>
			<p>Last Updated:  ${connectUpdated}</p>
		</div>

		<div class="catable table-cell smart-table info-tables-float">
		Conservation Areas
  				<div class="table-row smart-table-header">
  					<div class="table-cell smart-table-cell">CA LAbel</div>
					<div class="table-cell smart-table-cell">CA UUID</div>
					<div class="table-cell smart-table-cell">Version</div>
					<div class="table-cell smart-table-cell">Status</div>
					<div class="table-cell smart-table-cell">lock Key</div>
				</div>
				<c:forEach var="ca" items="${areas}" varStatus="count">
					<div class="carow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
						<div class="table-cell smart-table-cell">${ca.getLabel()}</div>
						<div class="table-cell smart-table-cell">${ca.getUuid()}</div>
						<div class="table-cell smart-table-cell">${ca.getVersion()}</div>
						<div class="table-cell smart-table-cell">${ca.getStatus()}</div>
						<div class="table-cell smart-table-cell">${ca.getLockKey()}</div>
					</div>

				</c:forEach>
			</div>  
		
			<div class="catable table-cell smart-table info-tables-float">
			Connect Installed Plugins
  				<div class="table-row smart-table-header">
  					<div class="table-cell smart-table-cell">Plugin ID</div>
  					<div class="table-cell smart-table-cell">Version</div>
				</div>
				<c:forEach var="p" items="${plugins}" varStatus="count">
					<div class="carow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
						<div class="table-cell smart-table-cell">${p.getPluginId()}</div>
						<div class="table-cell smart-table-cell">${p.getVersion()}</div>
					</div>

				</c:forEach>
			</div>  

			<div class="catable table-cell smart-table info-tables-float">
  			Conservation Area Plugins
  				<div class="table-row smart-table-header">
					<div class="table-cell smart-table-cell">CA UUID</div>
  					<div class="table-cell smart-table-cell">Plugin ID</div>
  					<div class="table-cell smart-table-cell">Version</div>
				</div>
				<c:forEach var="v" items="${versions}" varStatus="count">
					<div class="carow table-row ${count.index % 2 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
						<div class="table-cell smart-table-cell">${v.getConservationAreaUuid()}</div>
						<div class="table-cell smart-table-cell">${v.getPluginId()}</div>
						<div class="table-cell smart-table-cell">${v.getVersion()}</div>
					</div>

				</c:forEach>
			</div>  
			
			<div class="catable table-cell smart-table info-tables-float">
			Environment Variables
  				<div class="table-row smart-table-header">
  					<div class="table-cell smart-table-cell">Variable</div>
  					<div class="table-cell smart-table-cell">Value</div>
				</div>
				<c:forEach var="var" items="${vars}" varStatus="count">
					<c:if test="${(count.index % 2 == 0)}">
						<div class="carow table-row ${count.index % 4 == 0 ? 'smart-table-rowon' : 'smart-table-rowoff'}">
							<div class="table-cell smart-table-cell">${var}</div>
							<div class="table-cell smart-table-cell">${vars[count.index +1]}</div>
						</div>
					</c:if>

				</c:forEach>
			</div>  


		</div>
	</div>
	
	<%@include file="footer.jsp" %>

</body>
</html>