/*
 * Copyright (C) 2015 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.connect.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.geotools.referencing.CRS;
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ProjectionProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.AbstractSmartAction;
import org.wcs.smart.connect.model.SmartRoleAction;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.connect.query.QueryFolderProxy;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.QueryProxy;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.CsvExporter;
import org.wcs.smart.connect.query.engine.GeoJsonExporter;
import org.wcs.smart.connect.query.engine.GridQueryResults;
import org.wcs.smart.connect.query.engine.HtmlExporter;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
import org.wcs.smart.connect.query.engine.ShpExporter;
import org.wcs.smart.connect.query.engine.TiffRasterExporter;
import org.wcs.smart.connect.query.engine.i2.IntelObservationQueryResults;
import org.wcs.smart.connect.security.AdvIntelAction;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.QueryAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.export.CsvEntitySummaryQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter.ExportOption;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.QueryGridResultItem;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;


/**
 * SMART Connect Query REST API
 * @author Emily, Jeff
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + QueryApi.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class QueryApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(QueryApi.class.getName());
	
	public static final String PATH = "query"; //$NON-NLS-1$
	
	public enum Direction{ 
		UP("ASC"),  //$NON-NLS-1$
		DOWN("DESC"); //$NON-NLS-1$
		 
		public String sql;
		
		private Direction(String sql) {  
			this.sql = sql;
		} 
	};
	

	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
	/**
	 * <p>Runs a query and returns the results.</p>
	 * <p>
	 * URL: ../server/api/query/{queryuuid}<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param queryuuid		provided in the URL, the uuid of the query requested
	 * @param format	requested format, not all options makes sense for all query types: csv, shp, tif, geojson
	 * @param start_date	start date of query, in the form yyyy-MM-dd
	 * @param end_date	end date of query, in the form yyyy-MM-dd
	 * @param date_filter	date field type, not all make sense for all queries: waypointdate, patrolstart, patrolend, missiontrackdate, missionstartdate, missionenddate, intellreceiveddate
	 * @param delimiter	delimiter override to use in CSV format
	 * @param cafilter	comma separated list of CA uuids, only query these CAs.
	 * @param sortcolumn	the attribute key of the column you wish to sort by. It is tricky to know what columns are valid here as the queries are all very dynamic and there are many type. eg, &sortcolumn=wp_id 
	 * 						for details of valid columns see here:  https://app.assembla.com/spaces/smart-cs/wiki/SMART_Connect_Query_Sorting_Fields  
	 * 
	 * 						NOTE: you will get an error, "{"status": 500, "error:": "Error executing query: ERROR: column "abc" does not exist Position: 52"} if the provided column is not supported, change the sortcolumn parameter and try again.
	 * @param sortdirection set this to "descending" (or "desc") to get reverse order, otherwise you will get ascending order by default.  eg: &sortdirection=descending 
	 * 			This parameter will be ignored if provided without a sortcolumn.
	 * @param srid	the requested output projection SRID. The default is 4326 (WGS84, standard Lat/long coordinates). This parameter is ignored if the 'format' is not geojson or shp. (Note: for google map's projection you must use the official srid of 3857, 900913 doesn't work.
	 * @param includeuuids optional; if "true" then the observation uuid and waypoint uuid are include in the results if appropriate for the query type; otherwise this parameter is ignored 
	 * @return the results of the query, format is whatever is selected using the format parameter.
	 * @throws SQLException 
	 */
	//: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@GET
    @Path("/{queryuuid: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@Operation(description="Runs a query and returns the results.")
	public Response getQueryResults(@Parameter(description="the uuid of the query requested") @PathParam("queryuuid") String queryuuid, 
			@Parameter(description="requested format, not all options makes sense for all query types: csv, shp, html, tif, geojson") @QueryParam("format") String format,
			@Parameter(description="start date of query, in the form yyyy-MM-dd") @QueryParam("start_date") String start_date,
			@Parameter(description="end date of query, in the form yyyy-MM-dd") @QueryParam("end_date") String end_date,
			@Parameter(description="date field type, not all make sense for all queries: waypointdate, patrolstart, patrolend, missiontrackdate, missionstartdate, missionenddate, intellreceiveddate") @QueryParam("date_filter") String date_filter,
			@Parameter(description="delimiter override to use in CSV format") @QueryParam("delimiter") String delimiter,
			@Parameter(description="provided as a comma separated list of CA uuids, only runs the query against these CAs.") @QueryParam("cafilter") String cafilter,
			@Parameter(description="the attribute key of the column you wish to sort by. It is tricky to know what columns are valid here as the queries are all very dynamic and there are many type. eg, &sortcolumn=wp_id \r\n" + 
					"	 * 						for details of valid columns see here:  https://app.assembla.com/spaces/smart-cs/wiki/SMART_Connect_Query_Sorting_Fields  ") @QueryParam("sortcolumn") String sortcolumn,
			@Parameter(description="set this to \"descending\" (or \"desc\") to get reverse order, otherwise you will get ascending order by default.  eg: &sortdirection=descending \r\n" + 
					"	 * 			This parameter will be ignored if provided without a sortcolumn.") @QueryParam("sortdirection") String sortdirection,
			@Parameter(description="the requested output projection SRID. The default is 4326 (WGS84, standard Lat/long coordinates). This parameter is ignored if the 'format' is not geojson or shp. (Note: for google map's projection you must use the official srid of 3857, 900913 doesn't work.") @QueryParam("srid") String srid,
			@Parameter(description="optional; if \"true\" then the observation uuid and waypoint uuid are include in the results if appropriate for the query type; otherwise this parameter is ignored") @QueryParam("includeuuids") String includeuuids) throws SQLException{

		UUID uuid = UuidUtils.stringToUuid(queryuuid);
		QueryApi.Direction sortDirectionInt = QueryApi.Direction.UP;
		if(sortdirection != null && (sortdirection.equalsIgnoreCase("descending") || sortdirection.equalsIgnoreCase("desc") ) ){  //$NON-NLS-1$//$NON-NLS-2$
				sortDirectionInt = QueryApi.Direction.DOWN;
		}
		Boolean includeUuids = false;
		if (includeuuids != null) {
			if (includeuuids.trim().equalsIgnoreCase("TRUE")) { //$NON-NLS-1$
				includeUuids = true;
			}
		}
		LocalDate startDate = null;
		LocalDate endDate = null;
		if (start_date != null){
			try{
				startDate = SmartUtils.parseDate(start_date);
			}catch (Exception ex){
				return createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.StartDateError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
		}
		
		if (end_date != null){
			try{
				endDate = SmartUtils.parseDate(end_date);
			}catch (Exception ex){
				return createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.EndDateError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
		}
		
		if (delimiter == null){
			delimiter = ","; //$NON-NLS-1$
		}

		DateFilter df = null;

		if(date_filter != null) {
			IDateFieldFilter dateField = QueryManager.INSTANCE.findDateField(date_filter);
			IDateFilter dfilter = null;
			if (startDate == null && endDate == null){
				dfilter = AllDatesFilter.INSTANCE; 
			}else{
				if (startDate == null){
					startDate = LocalDate.now();
				}
				if (endDate == null){
					endDate = LocalDate.now();
				}
				dfilter = new CustomDateFilter();
				((CustomDateFilter)dfilter).setDates(startDate, endDate);
			}
			df = new DateFilter(dateField, dfilter);
		}

		IQueryResult result = null;
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try {
			Query query = QueryManager.INSTANCE.findQuery(uuid, s);
			if (query != null) {
				validateDateFilter(query.getTypeKey(), date_filter);
				QueryResult results = executeCoreQuery(query, cafilter, df, srid, format, delimiter,  sortcolumn, sortDirectionInt, includeUuids, s);
				result = results.result;
				return results.response;
			}else {
				AbstractIntelQuery query2 = QueryManager.INSTANCE.findIntelQuery(uuid, s);
				if (query2 == null)  throw new SmartConnectException(Status.BAD_REQUEST, "Query not found."); //$NON-NLS-1$
				validateDateFilter(query2.getTypeKey(), date_filter);
				QueryResult results = executeAdvIntelQuery(query2, cafilter, df, srid, format, delimiter,  sortcolumn, sortDirectionInt, includeUuids, s);
				result = results.result;
				return results.response;
			}
			
		}catch (Exception ex) {
			String error = ex.getMessage();
			if (ex instanceof JDBCException && ex.getCause() instanceof SQLException){
				error = ex.getCause().getMessage();
			}
			logger.log(Level.SEVERE, error, ex);
			//return createErrorResponse(Status.INTERNAL_SERVER_ERROR, error); //$NON-NLS-1$
			return createErrorResponse(Status.INTERNAL_SERVER_ERROR, MessageFormat.format(Messages.getString("QueryApi.ExecuteError", SmartUtils.getRequestLocale(request)), error)); //$NON-NLS-1$
		
		}finally {
			s.getTransaction().rollback();
			
			Session session = HibernateManager.getSession(request.getServletContext(), request.getLocale());
			session.beginTransaction();
			try {
				if (result != null){
					result.dispose(session);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}finally {
				session.getTransaction().commit();
			}
		}
		
	}
	
	private void validateDateFilter(String queryType, String filter) {
		if (filter != null && filter.isEmpty()) filter = null;
		String[] dateFilters = QueryManager.DATE_FILTERS.get(queryType.toLowerCase(Locale.ROOT));
		if (dateFilters == null && filter != null ) {
			throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("QueryApi.InvalidDateField", SmartUtils.getRequestLocale(request)), filter)); //$NON-NLS-1$
		}else if (filter != null ) {
			if (dateFilters == null) throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("QueryApi.InvalidDateField", SmartUtils.getRequestLocale(request)), filter)); //$NON-NLS-1$
			boolean ok = false;
			for (String f : dateFilters) {
				if (filter.equals(f)) {
					ok = true;
					break;
				}
			}
			if (!ok) throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("QueryApi.InvalidDateField", SmartUtils.getRequestLocale(request)), filter)); //$NON-NLS-1$
		}
	}
	
	private QueryResult executeCoreQuery(Query query, String cafilter, DateFilter df, String srid, String format, String delimiter, String sortColumnName, QueryApi.Direction sortDirectionInt, boolean includeUuids, Session s) throws Exception {
		IQueryResult result = null;
		if (query == null){
			//query not found
			return new QueryResult(Response.status(Status.NOT_FOUND).build());
		}
		UUID queryUuid = query.getUuid();	//we clone the query below but do not clone the original uuid so we want to track that here
		if (df != null && !QueryManager.INSTANCE.supportsDateField(query.getTypeKey(), df.getDateFieldOption())){
			return new QueryResult(createErrorResponse(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("QueryApi.InvalidDateFilterForQueryType", SmartUtils.getRequestLocale(request)), df.getDateFieldOption().getGuiName(request.getLocale()), query.getTypeKey()))); //$NON-NLS-1$
		}
		String oldId = query.getId();
		query = query.clone(query.getOwner());
		query.setId(oldId);
		if (query.getConservationArea().getIsCcaa()){
			//we use the ccaafilter; otherwise we ignore it
			query.setConservationAreaFilter(parseCaFilter(cafilter, s));
		}
			
		//for shared links; these links do not have a user but we want to check the j_username which is set to the link creator
		String name = request.getUserPrincipal().getName();
			
		//check for permission to this query for this user.
		if (!SecurityManager.INSTANCE.canAccess(s, name, QueryAction.RUNQUERY_KEY,  queryUuid)){
			if (SecurityManager.INSTANCE.canAccess(s, name, QueryAction.RUNQUERY_KEY, query.getConservationArea().getUuid())){
				//access is OK since they have access to All Queries in this CA.
			}else{
				return new QueryResult(createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.PermissionError", SmartUtils.getRequestLocale(request)))); //$NON-NLS-1$
			}
		}
									
		IQueryEngine engine = QueryManager.INSTANCE.findQueryEngine(query);
		if(engine == null){
			String error = MessageFormat.format(Messages.getString("QueryApi.NoQueryEngine", SmartUtils.getRequestLocale(request)), query.getTypeKey()); //$NON-NLS-1$
			return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, error));
		}
		
		/* configure date filter */
		if(df != null) {
			query.setDateFilter(df);
		}

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(Session.class.getName(), s);
		params.put(Locale.class.getName(), request.getLocale());
		params.put(AbstractQueryEngine.INCLUDE_UUID_PARAMETER, includeUuids);
		result = engine.executeQuery(query, params);
			
		ProjectionProvider prjProvider = null;
		if(srid != null && !srid.equals("")){ //$NON-NLS-1$
			Projection prj = new Projection();
			prj.setParsedCoordinateReferenceSystem(CRS.decode("EPSG:" + srid, true)); //$NON-NLS-1$
			prjProvider= new ProjectionProvider(prj);
		}else{
			//assume to default projection
			Projection prj = new Projection();
			prj.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
			prjProvider = new ProjectionProvider(prj);
		}
		 
		if (format.equalsIgnoreCase(CsvExporter.FORMAT_KEY)){
			java.nio.file.Path outputFile = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(System.nanoTime() + ".smart.tmp"); //$NON-NLS-1$
			CsvExporter exporter = new CsvExporter(outputFile, delimiter.charAt(0),request.getLocale());
			
			if (result instanceof AbstractDbFeatureResultSet
				&& query instanceof SimpleQuery){

				if(sortColumnName != null){
					((AbstractDbFeatureResultSet)result).setSorting(sortColumnName, sortDirectionInt);
					((AbstractDbFeatureResultSet)result).updateSortColumn(s);
				}
				
				exporter.exportResults((SimpleQuery)query, (AbstractDbFeatureResultSet)result, s);
			}else if (result instanceof IMemoryTableResultSet
				&& query instanceof GriddedQuery){
				exporter.exportResults((GriddedQuery)query, (IMemoryTableResultSet<QueryGridResultItem>)result, s);
			}else if (result instanceof SummaryQueryResult){
				exporter.exportResults(query, (SummaryQueryResult)result, s);
			}else{
				return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), result); //$NON-NLS-1$
			}
			return new QueryResult(writeText(outputFile), result);
				
		}else if (format.equalsIgnoreCase(ShpExporter.FORMAT_KEY)){
			String filename = SmartUtils.cleanFileName(query.getName() + "_"+ query.getId()) + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
			java.nio.file.Path outputFile  = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(filename); 
			
			ShpExporter exporter = new ShpExporter(outputFile, request.getLocale());
			exporter.setPrj(prjProvider);
			
			if (result instanceof AbstractDbFeatureResultSet &&
					query instanceof SimpleQuery){
				exporter.exportResults((SimpleQuery)query, (AbstractDbFeatureResultSet)result, s);
			}else{
				return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), result); //$NON-NLS-1$	
			}
			return new QueryResult(writeBinary(outputFile), result);
		}else if (format.equalsIgnoreCase(TiffRasterExporter.FORMAT_KEY)){
			String filename = SmartUtils.cleanFileName(query.getName() + "_"+ query.getId()) + ".tiff"; //$NON-NLS-1$ //$NON-NLS-2$
			java.nio.file.Path outputFile  = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(filename); 
			
			TiffRasterExporter exporter = new TiffRasterExporter(outputFile, request.getLocale());
			
			if (result instanceof GridQueryResults &&
					query instanceof GriddedQuery){
				exporter.exportResults((GriddedQuery)query, (GridQueryResults)result, s);
			}else{
				return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), result); //$NON-NLS-1$	
			}
			return new QueryResult(writeBinary(outputFile), result);
		}else if (format.equalsIgnoreCase(GeoJsonExporter.FORMAT_KEY)){
			GeoJsonExporter exporter = new GeoJsonExporter(request.getLocale(), prjProvider);
			
			if (result instanceof AbstractDbFeatureResultSet && query instanceof SimpleQuery){
				exporter.exportResults((SimpleQuery)query, (AbstractDbFeatureResultSet)result, s);
			}else{
				return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), result); //$NON-NLS-1$	
			}
			return new QueryResult(Response
					.status(Status.OK)
					.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
					.entity(exporter.getGeoJsonOutput() )
					.build(), result);
		}else if (format.equalsIgnoreCase(HtmlExporter.FORMAT_KEY)){
			HtmlExporter exporter = new HtmlExporter(request.getLocale());
			
			if (result instanceof AbstractDbFeatureResultSet
					&& query instanceof SimpleQuery){
						if(sortColumnName != null){
						((AbstractDbFeatureResultSet)result).setSorting(sortColumnName, sortDirectionInt);
						((AbstractDbFeatureResultSet)result).updateSortColumn(s);
					}
					
					exporter.exportResults((SimpleQuery)query, (AbstractDbFeatureResultSet)result, s);
				}else if (result instanceof IMemoryTableResultSet
					&& query instanceof GriddedQuery){
					exporter.exportResults((GriddedQuery)query, (IMemoryTableResultSet<QueryGridResultItem>)result, s);
				}else if (result instanceof SummaryQueryResult){
					exporter.exportResults(query, (SummaryQueryResult)result, s);
				}else{
					return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), result); //$NON-NLS-1$
				}
			return new QueryResult( Response
					.status(Status.OK)
					.header("Content-Type", MediaType.TEXT_HTML) //$NON-NLS-1$
					.entity(exporter.getHtml() )
					.build(), result);
		}else{
			return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), result); //$NON-NLS-1$
		}

	}
	
	
	private QueryResult executeAdvIntelQuery(AbstractIntelQuery query, String cafilter, DateFilter df, String srid, String format, String delimiter, String sortColumnName, QueryApi.Direction sortDirectionInt, boolean includeUuids, Session s) throws Exception {
		org.wcs.smart.i2.query.IQueryResult result = null;
		
		if (query == null){
			//query not found
			return new QueryResult(Response.status(Status.NOT_FOUND).build());
		}

			
		//for shared links; these links do not have a user but we want to check the j_username which is set to the link creator
		String name = request.getUserPrincipal().getName();
			
		//check for permission to this query for this user.
		if (!SecurityManager.INSTANCE.canAccess(s, name, AdvIntelAction.RUNQUERY_KEY,  query.getUuid())){
			if (SecurityManager.INSTANCE.canAccess(s, name, AdvIntelAction.RUNQUERY_KEY, query.getConservationArea().getUuid())){
				//access is OK since they have access to All Queries in this CA.
			}else{
				return new QueryResult(createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.PermissionError", SmartUtils.getRequestLocale(request)))); //$NON-NLS-1$
			}
		}
									
		IIntelQueryEngine engine = QueryManager.INSTANCE.findQueryEngine(query);
		if(engine == null){
			String error = MessageFormat.format(Messages.getString("QueryApi.NoQueryEngine", SmartUtils.getRequestLocale(request)), query.getTypeKey()); //$NON-NLS-1$
			return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, error));
		}
		
		//ca filter
		List<ConservationArea> cas = null;
		if (query.getConservationArea().getIsCcaa()){
			cas = parseCaFilterToCa(cafilter, s);
		}else {
			cas = Collections.singletonList(query.getConservationArea());
		}
		
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(Session.class.getName(), s);
		params.put(Locale.class.getName(), request.getLocale());
		params.put(ConservationArea.class.getName(), cas);
		params.put(AbstractQueryEngine.INCLUDE_UUID_PARAMETER, includeUuids);
		params.put(Principal.class.getName(), request.getUserPrincipal().getName());
		
		LocalDate[] dateFilters = new LocalDate[] {null, null};
		if (df.getDateFilterOption().getDates() != null) {
			dateFilters = df.getDateFilterOption().getDates();
		}
		params.put(LocalDate.class.getName(), dateFilters);
		result = engine.executeQuery(query, params);
			
		ProjectionProvider prjProvider = null;
		if(srid != null && !srid.equals("")){ //$NON-NLS-1$
			Projection prj = new Projection();
			prj.setParsedCoordinateReferenceSystem(CRS.decode("EPSG:" + srid, true)); //$NON-NLS-1$
			prjProvider= new ProjectionProvider(prj);
		}else{
			//assume to default projection
			Projection prj = new Projection();
			prj.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
			prjProvider = new ProjectionProvider(prj);
		}
		 
		if (result instanceof IntelObservationQueryResults){
			((IntelObservationQueryResults)result).setSorting(sortColumnName, sortDirectionInt);
			((IntelObservationQueryResults)result).configureSort(s);
			
			if (format.equalsIgnoreCase(GeoJsonExporter.FORMAT_KEY)){
				GeoJsonExporter exporter = new GeoJsonExporter(request.getLocale(), prjProvider);
				exporter.exportResults( (IntelObservationQueryResults)result, s);
				return new QueryResult(Response
						.status(Status.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
						.entity(exporter.getGeoJsonOutput() )
						.build(), new NoDisposeQueryResult());
			}
		}
		if (result instanceof IPagedQueryResultSet){
			//TODO: sorting
//			((IntelEntityRecordQueryResults)result).setSorting(sortColumnName, sortDirectionInt);
//			((IntelEntityRecordQueryResults)result).configureSort(s);
			if (format.equalsIgnoreCase(CsvExporter.FORMAT_KEY)){
				java.nio.file.Path outputFile = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(System.nanoTime() + ".smart.tmp"); //$NON-NLS-1$
				CsvExporter exporter = new CsvExporter(outputFile, delimiter.charAt(0),request.getLocale());
				exporter.exportResults( (IPagedQueryResultSet)result, s);
				return new QueryResult(writeText(outputFile), new NoDisposeQueryResult());
			}else if (format.equalsIgnoreCase(HtmlExporter.FORMAT_KEY)){
				HtmlExporter exporter = new HtmlExporter(request.getLocale());
				exporter.exportResults( (IPagedQueryResultSet)result, query.getName(), s);
				return new QueryResult( Response
						.status(Status.OK)
						.header("Content-Type", MediaType.TEXT_HTML) //$NON-NLS-1$
						.entity(exporter.getHtml() )
						.build(), new NoDisposeQueryResult());
			}
		}

		if (result instanceof org.wcs.smart.i2.query.SummaryQueryResult){
			if (format.equalsIgnoreCase(CsvExporter.FORMAT_KEY)){
				
				HashMap<ExportOption, Object> exportOptions = new HashMap<>();
				exportOptions.put(ExportOption.DELIMITER, delimiter.charAt(0));
				
				java.nio.file.Path outputFile = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(System.nanoTime() + ".smart.tmp"); //$NON-NLS-1$
				CsvEntitySummaryQueryExporter exporter = new CsvEntitySummaryQueryExporter();
				exporter.exportQuery( s, result, outputFile, exportOptions);
				
				//TODO: delete temporary file??
				return new QueryResult(writeText(outputFile), new NoDisposeQueryResult());
			}else if (format.equalsIgnoreCase(HtmlExporter.FORMAT_KEY)) {
				HtmlExporter exporter = new HtmlExporter(request.getLocale());
				exporter.exportResults( (org.wcs.smart.i2.query.SummaryQueryResult )result, query.getName(), s);
				return new QueryResult( Response
						.status(Status.OK)
						.header("Content-Type", MediaType.TEXT_HTML) //$NON-NLS-1$
						.entity(exporter.getHtml() )
						.build(), new NoDisposeQueryResult());	
			}
		}
		return new QueryResult(createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))), new NoDisposeQueryResult()); //$NON-NLS-1$
	}
	
	private Response writeText(java.nio.file.Path thisfile){
		Response rs = Response.ok(getStream(thisfile), MediaType.TEXT_PLAIN + "; charset=" + StandardCharsets.UTF_8.name()) //$NON-NLS-1$
	            .build();
		return rs;
	}
	
	private Response writeBinary(java.nio.file.Path thisfile){
		Response rs = Response.ok(getStream(thisfile), MediaType.APPLICATION_OCTET_STREAM )
				.header("Content-Disposition", "attachment; filename=" + thisfile.getFileName().toString()) //$NON-NLS-1$ //$NON-NLS-2$
	            .build();
		return rs;
	}
	
	private StreamingOutput getStream(final java.nio.file.Path thisfile){
		if (thisfile == null) throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, "Error executing query request."); //$NON-NLS-1$
		return new StreamingOutput() {
		      @Override
		      public void write(OutputStream output) throws IOException {
		        try {
		        	if (thisfile != null){
		        		FileUtils.copyFile(thisfile.toFile(), output);
		        	}
		        } catch (Exception e) {
		           e.printStackTrace();
		        }finally{
		        	Files.deleteIfExists(thisfile);
		        	
		        }
		      }
		    };
	}
	
	private List<ConservationArea> parseCaFilterToCa(String caFilter, Session session){
		if (caFilter == null) throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("QueryApi.InvalidCAFilter", SmartUtils.getRequestLocale(request)));  //$NON-NLS-1$
		String bits[] = caFilter.split(ConservationAreaFilter.CA_SPLITTER);

		List<ConservationArea> cas = new ArrayList<>();
		
		for (String cafilter : bits){
			try{
				UUID cauuid = UuidUtils.stringToUuid(cafilter);
				ConservationArea ca = (ConservationArea) session.get(ConservationArea.class, cauuid);
				if (ca != null && !ca.getIsCcaa()){
					cas.add(ca);
				}
			}catch (Exception ex){
				//cannot parse UUID for any reason
			}
		}
		return cas;
	}
	
	private String parseCaFilter(String caFilter, Session session){
		if (caFilter == null) throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("QueryApi.InvalidCAFilter", SmartUtils.getRequestLocale(request)));  //$NON-NLS-1$
		String bits[] = caFilter.split(ConservationAreaFilter.CA_SPLITTER);
		StringBuilder validCas = new StringBuilder();
		
		for (String cafilter : bits){
			try{
				UUID cauuid = UuidUtils.stringToUuid(cafilter);
				ConservationArea ca = (ConservationArea) session.get(ConservationArea.class, cauuid);
				if (ca != null && !ca.getIsCcaa()){
					validCas.append(","); //$NON-NLS-1$
					validCas.append(ca.getUuid().toString());
				}
			}catch (Exception ex){
				//cannot parse UUID for any reason
			}
		}
		if (validCas.length() == 0){
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("QueryApi.InvalidCAFilter", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		validCas.deleteCharAt(0);
		return validCas.toString();
	}

	/**
	 * <p>Returns all queries the current user is able to view/run, in a nested folder structure</p> 
	 * <p>
	 * URL: ../server/api/query/tree<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param typeFilter - optional type String - only return queries that match the type key provided, see getAllQueryTypes for list of possible values. leave blank to get everything.
	 * @param caFilter - optional UUID - only return queries that match the CA UUID provided. leave blank to get everything.
	 * @param isCcaaFilter - optional boolean - only returns string that are CCAA queries when True, only ones that are not when False. leave blank to get everything. 
	 * @return A JSON list of root QueryFolderProxy objects, with nested QueryFolderProxy and QueryProxy objects. (<a href="https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/query/QueryFolderProxy.java">Query Folder Proxy</a>)
	 */
	@GET
    @Path("/tree")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<QueryFolderProxy> getQueryTreeForUser(@QueryParam("type") String typeFilter,
			@QueryParam("ca") String caFilter,
			@QueryParam("isccaa") Boolean isCcaaFilter){
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		
		// build the query folder tree
		List<QueryFolderProxy> rootFolders = new ArrayList<QueryFolderProxy>();
		Map<UUID, QueryFolderProxy> foldersByUuid = new HashMap<UUID, QueryFolderProxy>();
		
		// CAs are the root folders
		List<ConservationArea> cas = QueryManager.INSTANCE.getConservationAreas(s);
		for(ConservationArea ca  :cas) {
			QueryFolderProxy caFolder = new QueryFolderProxy(ca.getName() + " [" + ca.getId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			foldersByUuid.put(ca.getUuid(), caFolder);
			rootFolders.add(caFolder);
		}
		
		List<QueryFolder> folders = QueryManager.INSTANCE.getQueryFolders(s);
		Deque<QueryFolder> remainingFolders = new LinkedList<QueryFolder>(folders);
		int loopCount = 0;
		int maxLoops =  folders.size() * folders.size();
		while(!remainingFolders.isEmpty()) {
			loopCount++;
			// just to prevent weird infinite loop failures
			if(loopCount > maxLoops) {
				break;
			}
			QueryFolder f = remainingFolders.removeFirst();
			if(f.getParentFolder() == null) {
				// this is a root folder
				QueryFolderProxy fp = new QueryFolderProxy(f.getName());
				foldersByUuid.put(f.getUuid(), fp);
				// but the CA is the real root folder
				foldersByUuid.get(f.getConservationArea().getUuid()).addSubFolder(fp);
			} else {
				QueryFolderProxy parentFolder = foldersByUuid.get(f.getParentFolder().getUuid());
				if(parentFolder != null) {
					QueryFolderProxy fp = new QueryFolderProxy(f.getName());
					foldersByUuid.put(f.getUuid(), fp);
					parentFolder.addSubFolder(fp);
				} else {
					// we haven't yet processed this folder's parent, put it back in the queue for later
					remainingFolders.addLast(f);
				}
			}
		}
		
		// get all the queries we have access to 
		List<QueryProxy> allowed = new ArrayList<QueryProxy>();
		try {
			allowed.addAll(getQueries(s, request.getLocale()));
			allowed.addAll(getAdvancedIntelQueries(s, request.getLocale()));			
		} catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			s.getTransaction().commit();
		}

		// filter the queries
		List<QueryProxy> filtered = filteredQueries(typeFilter, caFilter, isCcaaFilter, allowed);
		
		// add the queries into the folder tree
		for(QueryProxy q: filtered) {
			if(q.getFolderUuid() != null) {
				foldersByUuid.get(q.getFolderUuid()).addQuery(q);
			} else {
				foldersByUuid.get(q.getCaUuid()).addQuery(q);
			}
		}
		
		return rootFolders;
	}

	
	/**
	 * <p>Returns all queries the current user is able to view/run</p> 
	 * <p>
	 * URL: ../server/api/query/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param typeFilter - optional type String - only return queries that match the type key provided, see getAllQueryTypes for list of possible values. leave blank to get everything.
	 * @param caFilter - optional UUID - only return queries that match the CA UUID provided. leave blank to get everything.
	 * @param isCcaaFilter - optional boolean - only returns string that are CCAA queries when True, only ones that are not when False. leave blank to get everything. 
	 * @return A JSON list of QueryProxy objects. (<a href="https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/query/QueryProxy.java">Query Proxy</a>)
	 */
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Returns all queries the current user is able to view/run")
    public List<QueryProxy> getAllQueriesForUser(@Parameter(description="optional type String - only return queries that match the type key provided, see getAllQueryTypes for list of possible values. leave blank to get everything.") @QueryParam("type") String typeFilter,
    		@Parameter(description="optional UUID - only return queries that match the CA UUID provided. leave blank to get everything.") @QueryParam("ca") String caFilter,
    		@Parameter(description="optional boolean - only returns string that are CCAA queries when True, only ones that are not when False. leave blank to get everything.") @QueryParam("isccaa") Boolean isCcaaFilter){
		List<QueryProxy> allowed = new ArrayList<QueryProxy>();

		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try {
			allowed.addAll(getQueries(s, request.getLocale()));
			allowed.addAll(getAdvancedIntelQueries(s, request.getLocale()));			
		} catch(Exception ex) {
			ex.printStackTrace();
		} finally {
			s.getTransaction().commit();
		}
		
		return filteredQueries(typeFilter, caFilter, isCcaaFilter, allowed); 
	}

	private List<QueryProxy> getQueries(Session s, Locale l) throws Exception {

		if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY)){
			//no query access
			return Collections.emptyList();
		}

		List<QueryProxy> all = QueryManager.INSTANCE.getQueries(s, l);

		//Check if they access to All Queries, if so it's simple, return them all
		if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, null)) {
			return all;
		}
		
		List<QueryProxy> allowed = new ArrayList<QueryProxy>();

		//Get all Queries and check each one for specific permission to this user.
		
		//get all actions for the current user
		List<AbstractSmartAction> allActions = new ArrayList<>();
		allActions.addAll(QueryFactory.buildQuery(s, SmartUserAction.class, "username", request.getUserPrincipal().getName()).list()); //$NON-NLS-1$				
		List<SmartUserRole> roles = HibernateManager.getUserRoles(s, request.getUserPrincipal().getName());
		for (SmartUserRole r : roles) {
			allActions.addAll(QueryFactory.buildQuery(s, SmartRoleAction.class, "role", r.getRole()).list()); //$NON-NLS-1$
		}
		
		for (QueryProxy q : all) {
			boolean canAdd = false;
			
			for (AbstractSmartAction a : allActions) {
				//admin action; and permission to run all queries is captured above

				if (a.getAction().equalsIgnoreCase(CaAdminAccountAction.KEY) && a.getResource().equals(q.getCaUuid())){
					//ca admin
					canAdd = true;
					break;
				} else if (a.getAction().equalsIgnoreCase(QueryAction.RUNQUERY_KEY) && a.getResource().equals(q.getCaUuid())){
					//run query for ca
					canAdd = true;
					break;
				} else if (a.getAction().equalsIgnoreCase(QueryAction.RUNQUERY_KEY) && a.getResource().equals(q.getUuid())) {
					//run query for query
					canAdd = true;
					break;
				}
			}
			if (canAdd) allowed.add(q);
		}
		return allowed;
	}
	

	private List<QueryProxy> getAdvancedIntelQueries(Session s, Locale l) throws Exception {
		
		if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), AdvIntelAction.RUNQUERY_KEY)){
			//no query access
			return Collections.emptyList();
		}
		
		List<QueryProxy> queries = QueryManager.INSTANCE.getAdvancedIntelligenceQueries(s,  request.getLocale());
		if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdvIntelAction.RUNQUERY_KEY, null)){
			//can access all queries
			return queries;
		}
		List<QueryProxy> allowed = new ArrayList<>();
		
		List<AbstractSmartAction> allActions = new ArrayList<>();
		allActions.addAll(QueryFactory.buildQuery(s, SmartUserAction.class, "username", request.getUserPrincipal().getName()).list()); //$NON-NLS-1$				
		List<SmartUserRole> roles = HibernateManager.getUserRoles(s, request.getUserPrincipal().getName());
		for (SmartUserRole r : roles) {
			allActions.addAll(QueryFactory.buildQuery(s, SmartRoleAction.class, "role", r.getRole()).list()); //$NON-NLS-1$
		}
		
		for (QueryProxy q : queries){
			boolean canAdd = false;
			
			for (AbstractSmartAction a : allActions) {
				//note: ca admins caanot access profile queries unless they also have profile query permissions
				if (a.getAction().equalsIgnoreCase(AdvIntelAction.RUNQUERY_KEY) && a.getResource().equals(q.getCaUuid())){
					//run query for ca
					canAdd = true;
					break;
				}else if (a.getAction().equalsIgnoreCase(AdvIntelAction.RUNQUERY_KEY) && a.getResource().equals(q.getUuid())) {
					//run query for query
					canAdd = true;
					break;
				}
			}
			if (canAdd) allowed.add(q);
			

		}
		return allowed;
	}
	
	private List<QueryProxy> filteredQueries(String typeFilter, String caFilter, Boolean isCcaaFilter,
			List<QueryProxy> list) {
		HashSet<String> types = null;
		if(typeFilter != null && !typeFilter.isEmpty()) {
			types = new HashSet<String>();
			String[] typeFilters = typeFilter.split(","); //$NON-NLS-1$
			for(String type : typeFilters) {
				types.add(type.toLowerCase());
			}
		}
		UUID caUuid = null;
		if(caFilter != null && !caFilter.isEmpty()) {
			caUuid = UUID.fromString(caFilter);
		}
		List<QueryProxy> passed = new ArrayList<QueryProxy>();
		for (QueryProxy q : list){
			//type filter
			if(types != null 
					&& !types.contains(q.getTypeKey())) {
				continue;
			}

			//ca Filter
			if(caUuid != null 
					&& !q.getCaUuid().equals(caUuid)) {
				continue;
			}
			
			//ccaa Filter
			if(isCcaaFilter != null
					&& isCcaaFilter != q.getIsCcaa()) {
				continue;
			}

			passed.add(q);
		}
		return passed;
	}

	@SuppressWarnings("unchecked")
	private Response createErrorResponse(Status code, String message){
		JSONObject json = new JSONObject();
		json.put("status", code.getStatusCode()); //$NON-NLS-1$
		json.put("error", message); //$NON-NLS-1$
		
		String error = json.toJSONString();
		return Response
				.status(code)
				.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
				.entity(error)
				.build();
	}
	

	private class NoDisposeQueryResult implements IQueryResult {
		@Override
		public void dispose(Session session) throws SQLException {
		}

		@Override
		public boolean isDisposed() {
			return true;
		}
		
	}
	private class QueryResult{
		Response response;
		IQueryResult result;
		
		public QueryResult(Response response) {
			this(response, null);
		}
		
		public QueryResult(Response response, IQueryResult results) {
			this.response = response;
			this.result = results;
		}
	}
}
		
