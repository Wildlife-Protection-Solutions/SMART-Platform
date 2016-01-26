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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.query.QueryProxy;
import org.wcs.smart.connect.query.engine.AbstractQueryEngine;
import org.wcs.smart.connect.query.engine.CsvExporter;
import org.wcs.smart.connect.query.engine.IDbTableResultSet;
import org.wcs.smart.connect.query.engine.IMemoryTableResultSet;
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
	private final Logger logger = Logger.getLogger(ConnectAlert.class.getName());
	
	public static final String PATH = "query"; //$NON-NLS-1$

	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
	/**
	 * Runs a query and returns the results.
	 * 
	 * @param queryUuid
	 * @param format
	 * @param start
	 * @param end
	 * @param filter
	 * @param delimiter
	 * @param cafilter
	 * @return
	 */
	@GET
    @Path("/{queryuuid}")
	public Response getQueryResults(@PathParam("queryuuid") String queryUuid, 
			@QueryParam("format") String format,
			@QueryParam("start_date") String start,
			@QueryParam("end_date") String end,
			@QueryParam("date_filter") String filter,
			@QueryParam("delimiter") String delimiter,
			@QueryParam("cafilter") String cafilter){

		UUID uuid = UuidUtils.stringToUuid(queryUuid);
		
		Date startDate = null;
		Date endDate = null;
		if (start != null){
			try{
				startDate = SmartUtils.parseDate(start);
			}catch (Exception ex){
				return createErrorResponse(Status.BAD_REQUEST, "Could not parse start date.  Must be of form yyyy-MM-dd H:m:s");
			}
		}
		
		if (end != null){
			try{
				endDate = SmartUtils.parseDate(end);
			}catch (Exception ex){
				return createErrorResponse(Status.BAD_REQUEST, "Could not parse end date.  Must be of form yyyy-MM-dd H:m:s");
			}
		}
		
		IDateFieldFilter dateField = QueryManager.INSTANCE.findDateField(filter);
		if (dateField == null){
			return createErrorResponse(Status.BAD_REQUEST, MessageFormat.format("Invalid date field field.  {0} not supported.", filter));
		}
		
		if (delimiter == null){
			delimiter = ",";
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
		try{
			query = QueryManager.INSTANCE.findQuery(uuid, s);

			if (query == null){
				//query not found
				return Response.status(Status.NOT_FOUND).build();
			}
			if (!QueryManager.INSTANCE.supportsDateField(query.getTypeKey(), df.getDateFieldOption())){
				return createErrorResponse(Status.BAD_REQUEST, MessageFormat.format("The date filter field {0} is not supported for the query type {1}", query.getTypeKey(), df.getDateFieldOption().getGuiName(request.getLocale())));
			}
			query = query.clone(query.getOwner());
			if (query.getConservationArea().getIsCcaa()){
				//we use the ccaafilter; otherwise we ignore it
				query.setConservationAreaFilter(parseCaFilter(cafilter, s));
			}
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, uuid)){
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, query.getConservationArea().getUuid())){
					//access is OK since they have access to All Queries in this CA.
				}else{
					return createErrorResponse(Status.BAD_REQUEST, "You do not have permissions to access this Query.");
				}
			}
	
									
			IQueryEngine engine = QueryManager.INSTANCE.findQueryEngine(query);
			if(engine == null){
				String error = MessageFormat.format("No query engine for query type {1}.", query.getTypeKey());
				return createErrorResponse(Status.NOT_IMPLEMENTED, error);
			}
		
			/* configure date filter */
			query.setDateFilter(df);
			

			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(Session.class.getName(), s);
			params.put(Locale.class.getName(), request.getLocale());
			
			File f = new File(SmartContext.INSTANCE.getTempFilestoreLocation(), System.nanoTime() + ".smart.tmp");
			
			try{
				IQueryResult result = engine.executeQuery(query, params);
			
				if (format.equalsIgnoreCase("csv")){
					CsvExporter exporter = new CsvExporter(f, delimiter.charAt(0),request.getLocale());
				
					if (result instanceof IDbTableResultSet
						&& query instanceof SimpleQuery){
						exporter.exportResults((SimpleQuery)query, (IDbTableResultSet)result, s);
					}else if (result instanceof IMemoryTableResultSet
						&& query instanceof GriddedQuery){
						exporter.exportResults((GriddedQuery)query, (IMemoryTableResultSet<GridResultItem>)result, s);
					}else if (result instanceof SummaryQueryResult){
						exporter.exportResults(query, (SummaryQueryResult)result, s);
					}
				}else{
					return createErrorResponse(Status.NOT_IMPLEMENTED, "Query export not implemented for select query type and format.");
				}
			}finally{
				if (engine instanceof AbstractQueryEngine){
					((AbstractQueryEngine)engine).cleanUp(s);
				}
			}
			//TODO: if file not found then fail; do not attempt to write output
			StreamingOutput stream = new StreamingOutput() {
			      @Override
			      public void write(OutputStream output) throws IOException {
			        try {
			        	FileUtils.copyFile(f, output);
			        	f.delete();
			        } catch (Exception e) {
			           e.printStackTrace();
			        }
			      }
			    };
			
			//return accepted
			Response rs = Response.ok(stream, MediaType.TEXT_PLAIN + "; charset=" + StandardCharsets.UTF_8.name())
		            .build();
			return rs;
		}catch (Exception ex){
			String error = ex.getMessage();
			if (ex instanceof JDBCException && ex.getCause() instanceof SQLException){
				error = ex.getCause().getMessage();
			}
			logger.log(Level.SEVERE, error, ex);
			return createErrorResponse(Status.INTERNAL_SERVER_ERROR, MessageFormat.format("Error executing query: {0}", error));
		}finally{
			s.getTransaction().commit();
		}
		
	}
	
	private String parseCaFilter(String caFilter, Session session){
		if (caFilter == null) throw new SmartConnectException(Status.BAD_REQUEST, "Invalid conservation area filter.  At least one valid conservation area uuid must be provided."); 
		String bits[] = caFilter.split(ConservationAreaFilter.CA_SPLITTER);
		StringBuilder validCas = new StringBuilder();
		
		for (String cafilter : bits){
			try{
				UUID cauuid = UuidUtils.stringToUuid(cafilter);
				ConservationArea ca = (ConservationArea) session.get(ConservationArea.class, cauuid);
				if (ca != null && !ca.getIsCcaa()){
					validCas.append(",");
					validCas.append(ca.getUuid().toString());
				}
			}catch (Exception ex){
				//cannot parse UUID for any reason
			}
		}
		if (validCas.length() == 0){
			throw new SmartConnectException(Status.BAD_REQUEST, "Invalid conservation area filter.  At least one valid conservation area uuid must be provided.");
		}
		validCas.deleteCharAt(0);
		return validCas.toString();
	}
	
	/*
	 * returns all Queries the user is able to view 
	 */
	
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<QueryProxy> getAllQueriesForUser( @QueryParam(value="username") String username){
		List<QueryProxy> allowed = new ArrayList<QueryProxy>();

		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			//Check if they access to All Queries, if so it's simple, return them all
			if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, null)){
				 return QueryManager.INSTANCE.getQueries(s, request.getLocale());
			}
			
			//short circuit check for access to > 0 queries, if not, return nothing. 
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY)){
				 return allowed;
			}
			
			//Get all Queries and check each one for specific permission to this user.
			List<QueryProxy> all = QueryManager.INSTANCE.getQueries(s, request.getLocale());
			for (QueryProxy q : all){
				//Do they have access to all queries from this CA? if yes then add it.
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, q.getCaUuid())){
					allowed.add(q);
				}
				
				//Do they have specific permission to this query? if yes then add it.
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, q.getUuid())){
					allowed.add(q);
				}
			}
		}finally{
			s.getTransaction().commit();
		}
		
		return allowed; 
	}
	
	
	
	private Response createErrorResponse(Status code, String message){
		String error = MessageFormat.format("\"status\": {0}, \"error:\": \"" + message + "\"", code.getStatusCode(), message);
		return Response
				.status(code)
				.header("Content-Type", MediaType.APPLICATION_JSON)
				.entity("{" + error +"}")
				.build();
	}
}
