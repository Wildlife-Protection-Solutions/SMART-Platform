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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.report.BirtEngine;
import org.wcs.smart.connect.report.ReportProxy;
import org.wcs.smart.connect.report.query.ServerSmartConnection;
import org.wcs.smart.connect.security.ReportAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

/**
 * SMART Connect Report REST API
 * @author Emily
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ReportApi.PATH)
public class ReportApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ReportApi.class.getName());
	
	public static final String PATH = "report"; //$NON-NLS-1$

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
	@SuppressWarnings("unchecked")
	@GET
    @Path("/{reportuuid}")
	public Response exectueReport(@PathParam("reportuuid") String reportUuid, 
			@QueryParam("format") String format,
			@QueryParam("start_date") String start,
			@QueryParam("end_date") String end,
			@QueryParam("date_filter") String filter,
			@QueryParam("delimiter") String delimiter,
			@QueryParam("cafilter") String cafilter){

		UUID uuid = UuidUtils.stringToUuid(reportUuid);
		
		Report report = null;
		Session s = HibernateManager.getSession(context, request.getLocale());
		
		List<ConservationArea> conservationAreas = new ArrayList<ConservationArea>();
		try{
			s.beginTransaction();
			s.clear();	
			report = (Report) s.get(Report.class, uuid);
			
			if (report == null) throw new SmartConnectException(Status.NOT_FOUND, "Report not found");
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, uuid)){
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, report.getConservationArea().getUuid())){
					//access is OK since they have access to All Queries in this CA.
				}else{
					return createErrorResponse(Status.UNAUTHORIZED, "You do not have permission to access this report."); 
				}
			}
			
			
			String caFilter = null;
			if (report.getConservationArea().getIsCcaa()){
				//we use the ccaafilter; otherwise we ignore it
				conservationAreas.addAll(parseCaFilter(cafilter, s));
			}else{
				conservationAreas.add(report.getConservationArea());
			}
			
		}finally{
			s.getTransaction().rollback();
		}
		
		try{
		//TODO: create one at start up and shut down at shutdown
		IReportEngine engine = BirtEngine.getBirtEngine(context);
		
//		IReportEngine engine = ReportEngineManager.getBirtReportEngine();
		java.nio.file.Path reportFile = FileSystems.getDefault().getPath(
				report.getConservationArea().getFileDataStoreLocation(),
				"reports",
				report.getFilename());
				
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("Start Date", new Date((new Date()).getTime() - 1000l *60 *60*24*30*3));
		parameters.put("End Date", new Date());
		
//		parameters.put("Start Date", new Date(115,0,1));
//		parameters.put("End Date", new Date(115,1,1));
		try(InputStream inStream = Files.newInputStream(reportFile)){
			final IReportRunnable design = engine.openReportDesign(inStream);
					
			try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
				HTMLRenderOption options = new HTMLRenderOption();
				options = new HTMLRenderOption( );
				options.setOutputStream(bos);
				options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
				options.setOutputFormat(HTMLRenderOption.HTML);
				IRunAndRenderTask task = engine.createRunAndRenderTask(design);

				//add items to app context
				Map items = task.getAppContext();
				items.put(ServerSmartConnection.CONNECTION_KEY, HibernateManager.getSession(context, request.getLocale()));
				items.put(SmartConnection.LOCAL_CONTEXT_VAR, request.getLocale());
				items.put("org.wcs.smart.report.ca", conservationAreas);
				
				try{
					task.setRenderOption(options);
					task.setParameterValues(parameters);
					task.run();
				}finally{
					task.close();
				}
				
				String html = bos.toString("UTF8");
				return writeHtml(html);
			}
		}
		}catch(Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, "Error running report: " + ex.getMessage()); 
		}
		
	}
	
	private Response writeHtml(final String html){
		
		StreamingOutput sout = new StreamingOutput() {
			
			@Override
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				output.write(html.getBytes(StandardCharsets.UTF_8));
				
			}
		};
		Response rs = Response.ok(sout, MediaType.TEXT_HTML + "; charset=" + StandardCharsets.UTF_8.name()) //$NON-NLS-1$
	            .build();
		return rs;
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
	
	private List<ConservationArea> parseCaFilter(String caFilter, Session session){
		if (caFilter == null) throw new SmartConnectException(Status.BAD_REQUEST, "Invalid Conservation Area Filter.");  //$NON-NLS-1$
		String bits[] = caFilter.split(ConservationAreaFilter.CA_SPLITTER);
		List<ConservationArea> cas = new ArrayList<ConservationArea>();
		
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
		if (cas.size() == 0){
			throw new SmartConnectException(Status.BAD_REQUEST, "Invalid Conservation Area Filter"); //$NON-NLS-1$
		}
		return cas;
	}
	
	/*
	 * returns all Queries the user is able to view 
	 */
	
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<ReportProxy> getAllReportForUser( @QueryParam(value="username") String username){
		List<ReportProxy> allowed = new ArrayList<ReportProxy>();

		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			//Check if they access to All Queries, if so it's simple, return them all
			if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, null)){
				 return getReports(s, request.getLocale(), false);
			}
			
			//short circuit check for access to > 0 queries, if not, return nothing. 
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY)){
				 return allowed;
			}
			
			//Get all Queries and check each one for specific permission to this user.
			List<ReportProxy> all = getReports(s, request.getLocale(), false);
			for (ReportProxy q : all){
				//Do they have access to all queries from this CA? if yes then add it.
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, q.getCaUuid())){
					allowed.add(q);
				}else{
					//Do they have specific permission to this query? if yes then add it.
					if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, q.getUuid())){
						allowed.add(q);
					}
				}
			}
		}finally{
			s.getTransaction().commit();
		}
		
		return allowed; 
	}
	
	private List<ReportProxy> getReports(Session session, final Locale l, Boolean includeMyQueries){
		List<ReportProxy> proxies = new ArrayList<ReportProxy>();
		
		List<Report> reports = session.createCriteria(Report.class).list();
		for (Report r : reports){
			ReportProxy proxy = new ReportProxy(r.getUuid(), r.getName(),  
					r.getConservationArea().getId(), r.getId(), r.getShared(), 
					r.getConservationArea().getUuid(),
					r.getConservationArea().getIsCcaa());
			if(r.getShared() || includeMyQueries){
				proxies.add(proxy);
			}
		}
		Collections.sort(proxies, new Comparator<ReportProxy>() {
			@Override
			public int compare(ReportProxy o1, ReportProxy o2) {
				Collator textCompare = Collator.getInstance(l);
				return textCompare.compare(o1.getName(), o2.getName());
			}
		});
		return proxies;
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
