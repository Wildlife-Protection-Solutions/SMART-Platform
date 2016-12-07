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
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.wcs.smart.ProjectionProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.QueryProxy;
import org.wcs.smart.connect.query.engine.AbstractDbFeatureResultSet;
import org.wcs.smart.connect.query.engine.CsvExporter;
import org.wcs.smart.connect.query.engine.GeoJsonExporter;
import org.wcs.smart.connect.query.engine.GridQueryResults;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
import org.wcs.smart.connect.query.engine.ShpExporter;
import org.wcs.smart.connect.query.engine.TiffRasterExporter;
import org.wcs.smart.connect.security.QueryAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.query.model.filter.date.CustomDateFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * SMART Connect Query REST API
 * @author Emily, Jeff
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + QueryApi.PATH)
public class QueryApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(QueryApi.class.getName());
	
	public static final String PATH = "query"; //$NON-NLS-1$
	
	public enum Direction{ 
		UP("ASC"), 
		DOWN("DESC");
		 
		public String sql;
		
		private Direction(String sql) {  
			this.sql = sql;
		} 
	};
	

	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
	/**
	 * Runs a query and returns the results.
	 * URL: ../server/api/query/{queryuuid}
	 * Call Type: GET
	 * 
	 * @param queryuuid		provided in the URL, the uuid of the query requested
	 * @param format	requested format, not all options makes sense for all query types: csv, shp, tif, geojson
	 * @param srid		srid that any spatial data results should be returned in
	 * @param start_date	start date of query, number of milliseconds since the epoch
	 * @param end_date	end date of query, number of milliseconds since the epoch
	 * @param date_filter	date field type, not all make sense for all queries: waypointdate, patrolstart, patrolend, missiontrackdate, missionstartdate, missionenddate, intellreceiveddate
	 * @param delimiter	delimiter override to use in CSV format
	 * @param cafilter	comma separated list of CA uuids, only query these CAs.
	 * @param sortcolumn	the attribute key of the column you wish to sort by. It is tricky to know what columns are valid here as the queries are all very dynamic and there are many type. eg, &sortcolumn=wp_id 
	 * 						Any data model value "key" works, eg: numberofpeople for "People -> Number of people". 
	 * 						For metadata type values it depends on the query type:
	 * 						Patrol queries will allow: p_id - to sort by Patrol ID, p_objective, p_type, p_legid,
	 * 						if Ca details are included in your query the column names are: ca_name, ca_id 
	 * 						Any queries with waypoint allows: wp_date, wp_id, wp_x, wp_y, wp_direction, wp_distance, wp_time, wp_comment, (wp_source sometimes)
	 * 						survey queries:  surveydesign_startdate, surveydesign_enddate, survey_id, survey_startdate, survey_enddate, mission_id, mission_startdate, mission_enddate, samplingunit_id
	 * 
	 * 						NOTE: you will get an error, "{"status": 500, "error:": "Error executing query: ERROR: column "abc" does not exist Position: 52"} if the provided column is not supported, change the sortcolumn parameter and try again.
	 * @param sortdirection set this to "descending" (or "desc") to get reverse order, otherwise you will get ascending order by default.  eg: &sortdirection=descending 
	 * 			This parameter will be ignored if provided without a sortcolumn.
	 * @param srid	the requested output projection SRID. The default is 4326 (WGS84, standard Lat/long coordinates). This parameter is ignored if the 'format' is not geojson or shp. (Note: for google map's projection you must use the official srid of 3857, 900913 doesn't work. 
	 * @return the results of the query, format is whatever is selected using the format parameter.
	 * @throws SQLException 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/{queryuuid}")
	public Response getQueryResults(@PathParam("queryuuid") String queryUuid, 
			@QueryParam("format") String format,
			@QueryParam("start_date") String start,
			@QueryParam("end_date") String end,
			@QueryParam("date_filter") String filter,
			@QueryParam("delimiter") String delimiter,
			@QueryParam("cafilter") String cafilter,
			@QueryParam("sortcolumn") String sortColumnName,
			@QueryParam("sortdirection") String sortDirection,
			@QueryParam("srid") String srid) throws SQLException{

		UUID uuid = UuidUtils.stringToUuid(queryUuid);
		QueryApi.Direction sortDirectionInt = QueryApi.Direction.UP;
		if(sortDirection != null && (sortDirection.toLowerCase().equals("descending") || sortDirection.toLowerCase().equals("desc") ) ){
				sortDirectionInt = QueryApi.Direction.DOWN;
		}
		
		Date startDate = null;
		Date endDate = null;
		if (start != null){
			try{
				startDate = SmartUtils.parseDate(start);
			}catch (Exception ex){
				return createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.StartDateError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
		}
		
		if (end != null){
			try{
				endDate = SmartUtils.parseDate(end);
			}catch (Exception ex){
				return createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.EndDateError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
		}
		
		IDateFieldFilter dateField = QueryManager.INSTANCE.findDateField(filter);
		if (dateField == null){
			return createErrorResponse(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("QueryApi.InvalidDateField", SmartUtils.getRequestLocale(request)), filter)); //$NON-NLS-1$
		}
		
		if (delimiter == null){
			delimiter = ","; //$NON-NLS-1$
		}
		
		IDateFilter dfilter = null;
		if (startDate == null && endDate == null){
			dfilter = AllDatesFilter.INSTANCE; 
		}else{
			if (startDate == null){
				startDate = new Date(0);
			}
			if (endDate == null){
				endDate = new Date();
			}
			dfilter = new CustomDateFilter();
			((CustomDateFilter)dfilter).setDates(startDate, endDate);
		}
		DateFilter df = new DateFilter(dateField, dfilter);

		
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		Query query = null;
		IQueryResult result = null;
		try{
			query = QueryManager.INSTANCE.findQuery(uuid, s);
			
			if (query == null){
				//query not found
				return Response.status(Status.NOT_FOUND).build();
			}
			if (!QueryManager.INSTANCE.supportsDateField(query.getTypeKey(), df.getDateFieldOption())){
				return createErrorResponse(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("QueryApi.InvalidDateFilterForQueryType", SmartUtils.getRequestLocale(request)), df.getDateFieldOption().getGuiName(request.getLocale()), query.getTypeKey())); //$NON-NLS-1$
			}
			String oldId = query.getId();
			query = query.clone(query.getOwner());
			query.setId(oldId);
			if (query.getConservationArea().getIsCcaa()){
				//we use the ccaafilter; otherwise we ignore it
				query.setConservationAreaFilter(parseCaFilter(cafilter, s));
			}
			
			//for shared links; these links do not have a user but we want to check the j_username which is set to the link creator
			String name="";
			if( request.getUserPrincipal() == null){
				name = (String)request.getAttribute("j_username");	
			}else{
				name = request.getUserPrincipal().getName();
			}
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, name, QueryAction.RUNQUERY_KEY, uuid)){
				if (SecurityManager.INSTANCE.canAccess(s, name, QueryAction.RUNQUERY_KEY, query.getConservationArea().getUuid())){
					//access is OK since they have access to All Queries in this CA.
				}else{
					return createErrorResponse(Status.BAD_REQUEST, Messages.getString("QueryApi.PermissionError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}
									
			IQueryEngine engine = QueryManager.INSTANCE.findQueryEngine(query);
			if(engine == null){
				String error = MessageFormat.format(Messages.getString("QueryApi.NoQueryEngine", SmartUtils.getRequestLocale(request)), query.getTypeKey()); //$NON-NLS-1$
				return createErrorResponse(Status.NOT_IMPLEMENTED, error);
			}
		
			/* configure date filter */
			query.setDateFilter(df);

			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(Session.class.getName(), s);
			params.put(Locale.class.getName(), request.getLocale());
			
			result = engine.executeQuery(query, params);
			
			ProjectionProvider prjProvider = null;
			if(srid != null && !srid.equals("")){
				Projection prj = new Projection();
				prj.setParsedCoordinateReferenceSystem(CRS.decode("EPSG:" + srid, true));
				prjProvider= new ProjectionProvider(prj);
			}else{
				//assume to default projection
				Projection prj = new Projection();
				prj.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
				prjProvider = new ProjectionProvider(prj);
			}
		 
			if (format.equalsIgnoreCase(CsvExporter.FORMAT_KEY)){
				java.nio.file.Path outputFile = SmartContext.INSTANCE.getTempFilestoreLocation().toPath().resolve(System.nanoTime() + ".smart.tmp"); //$NON-NLS-1$
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
					exporter.exportResults((GriddedQuery)query, (IMemoryTableResultSet<GridResultItem>)result, s);
				}else if (result instanceof SummaryQueryResult){
					exporter.exportResults(query, (SummaryQueryResult)result, s);
				}else{
					return createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
				return writeText(outputFile);
				
			}else if (format.equalsIgnoreCase(ShpExporter.FORMAT_KEY)){
				String filename = SmartUtils.cleanFileName(query.getName() + "_"+ query.getId()) + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
				java.nio.file.Path outputFile  = SmartContext.INSTANCE.getTempFilestoreLocation().toPath().resolve(filename); 
				
				ShpExporter exporter = new ShpExporter(outputFile, request.getLocale());
				exporter.setPrj(prjProvider);
				
				if (result instanceof AbstractDbFeatureResultSet &&
						query instanceof SimpleQuery){
					exporter.exportResults((SimpleQuery)query, (AbstractDbFeatureResultSet)result, s);
				}else{
					return createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$	
				}
				return writeBinary(outputFile);
			}else if (format.equalsIgnoreCase(TiffRasterExporter.FORMAT_KEY)){
				String filename = SmartUtils.cleanFileName(query.getName() + "_"+ query.getId()) + ".tiff"; //$NON-NLS-1$ //$NON-NLS-2$
				java.nio.file.Path outputFile  = SmartContext.INSTANCE.getTempFilestoreLocation().toPath().resolve(filename); 
				
				TiffRasterExporter exporter = new TiffRasterExporter(outputFile, request.getLocale());
				
				if (result instanceof GridQueryResults &&
						query instanceof GriddedQuery){
					exporter.exportResults((GriddedQuery)query, (GridQueryResults)result, s);
				}else{
					return createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$	
				}
				return writeBinary(outputFile);
			}else if (format.equalsIgnoreCase(GeoJsonExporter.FORMAT_KEY)){
				//TODO: sort reuslts??
				GeoJsonExporter exporter = new GeoJsonExporter(request.getLocale(), prjProvider);
				
				if (result instanceof AbstractDbFeatureResultSet && query instanceof SimpleQuery){
					exporter.exportResults((SimpleQuery)query, (AbstractDbFeatureResultSet)result, s);
				}else{
					return createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$	
				}
				return Response
						.status(Status.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
						.entity(exporter.getGeoJsonOutput() )
						.build();
			}else{
				return createErrorResponse(Status.NOT_IMPLEMENTED, Messages.getString("QueryApi.ExportFormatNotSupported", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}


		}catch (Exception ex){
			String error = ex.getMessage();
			if (ex instanceof JDBCException && ex.getCause() instanceof SQLException){
				error = ex.getCause().getMessage();
			}
			logger.log(Level.SEVERE, error, ex);
			//return createErrorResponse(Status.INTERNAL_SERVER_ERROR, error); //$NON-NLS-1$
			return createErrorResponse(Status.INTERNAL_SERVER_ERROR, MessageFormat.format(Messages.getString("QueryApi.ExecuteError", SmartUtils.getRequestLocale(request)), error)); //$NON-NLS-1$
		}finally {
			s.getTransaction().commit();
			Session session = HibernateManager.getSession(request.getServletContext(), request.getLocale());
			session.beginTransaction();
			try {
				if (result != null){

					result.dispose(session);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			session.getTransaction().commit();	
		}	
		
		
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
	 * returns all Queries the user is able to view 
	 * URL: ../server/api/query/
	 * Call Type: GET
	 * 
	 * @param type - optional type String - only return queries that match the type key provided, see getAllQueryTypes for list of possible values. leave blank to get everything.
	 * @param ca - optional UUID - only return queries that match the CA UUID provided. leave blank to get everything.
	 * @param isccaa - optional boolean - only returns string that are CCAA queries when True, only ones that are not when False. leave blank to get everything. 
	 * @return A JSON list of QueryProxy objects. ( https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/query/QueryProxy.java )
	 */
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<QueryProxy> getAllQueriesForUser(@QueryParam("type") String typeFilter,
			@QueryParam("ca") String caFilter,
			@QueryParam("isccaa") Boolean isCcaaFilter){
		List<QueryProxy> allowed = new ArrayList<QueryProxy>();

		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			//Check if they access to All Queries, if so it's simple, return them all
			if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, null)){
				allowed = QueryManager.INSTANCE.getQueries(s, request.getLocale());
				return filteredQueries(typeFilter, caFilter, isCcaaFilter, allowed);
			}
			
			//short circuit check for access to > 0 queries 
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY)){
				 return allowed; //allowed is empty at this point
			}
			
			//Get all Queries and check each one for specific permission to this user.
			List<QueryProxy> all = QueryManager.INSTANCE.getQueries(s, request.getLocale());
			for (QueryProxy q : all){
				//Do they have access to all queries from this CA? if yes then add it.
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, q.getCaUuid())){
					allowed.add(q);
				}else{
					//Do they have specific permission to this query? if yes then add it.
					if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, q.getUuid())){
						allowed.add(q);
					}
				}
			}
		}finally{
			s.getTransaction().commit();
		}
		
		return filteredQueries(typeFilter, caFilter, isCcaaFilter, allowed); 
	}
	
	
	
	private List<QueryProxy> filteredQueries(String typeFilter, String caFilter, Boolean isCcaaFilter,
			List<QueryProxy> list) {
		List<QueryProxy> passed = new ArrayList<QueryProxy>();
		for (QueryProxy q : list){
			Boolean pass1 = false;
			Boolean pass2 = false;
			Boolean pass3 = false;
			//type filter
			if(typeFilter != null && !typeFilter.isEmpty()){
				if(q.getTypeKey().equals(typeFilter)){
					pass1 = true;
				}
			}else{
				pass1 = true;
			}
			
			//ca Filter
			if(caFilter != null && !caFilter.isEmpty()){
				UUID uuid = UUID.fromString(caFilter);
				if(q.getCaUuid().equals(uuid)){
					pass2 = true;
				}
			}else{
				pass2 = true;
			}
			
			//ccaa Filter
			if(isCcaaFilter != null){
				if(isCcaaFilter && q.getIsCcaa()){
					pass3=true;
				}else if(!isCcaaFilter && !q.getIsCcaa()){
					pass3=true;
				}
			}else{
				pass3=true;
			}
			if(pass1 && pass2 && pass3){
				passed.add(q);
			}
		}
		return passed;
	}

	private Response createErrorResponse(Status code, String message){
		String error = MessageFormat.format("\"status\": {0}, \"error:\": \"" + message + "\"", code.getStatusCode(), message); //$NON-NLS-1$ //$NON-NLS-2$
		return Response
				.status(code)
				.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
				.entity("{" + error +"}") //$NON-NLS-1$ //$NON-NLS-2$
				.build();
	}
}
