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
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.eclipse.birt.report.engine.api.HTMLRenderOption;
import org.eclipse.birt.report.engine.api.HTMLServerImageHandler;
import org.eclipse.birt.report.engine.api.IHTMLRenderOption;
import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IParameterDefnBase;
import org.eclipse.birt.report.engine.api.IParameterGroupDefn;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.IScalarParameterDefn;
import org.eclipse.birt.report.engine.api.RenderOption;
import org.eclipse.birt.report.engine.api.impl.ParameterSelectionChoice;
import org.eclipse.birt.report.engine.api.impl.ReportEngine;
import org.eclipse.birt.report.engine.api.impl.ScalarParameterDefn;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.birt.SmartRunAndRender;
import org.wcs.smart.birt.parameter.ISmartBirtParameter;
import org.wcs.smart.birt.parameter.ParameterManager;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ReportParameter;
import org.wcs.smart.connect.model.ReportParameter.Type;
import org.wcs.smart.connect.report.BirtEngine;
import org.wcs.smart.connect.report.ReportFormat;
import org.wcs.smart.connect.report.ReportProxy;
import org.wcs.smart.connect.report.query.ServerSmartConnection;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.ReportAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.report.execute.ParameterFinder;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * SMART Connect Report REST API
 * @author Emily
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ReportApi.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class ReportApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ReportApi.class.getName());
	
	public static final String PATH = "report"; //$NON-NLS-1$

	/**
	 * Output directory for images included in reports
	 * Files are cleaned out of this directory when the cleanup job is run
	 */
	public static final String REPORT_IMAGES_DIR = "/images/report"; //$NON-NLS-1$
	
	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
	/**
	 * <p>Runs a report and returns the results.</p>
	 * <p>
	 * URL: ../server/api/report/{reportuuid}<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param reportUuid - The report UUID, passed in as part of the URL 
	 * @param format  - html, pdf, doc, odt are the options.
	 * @param parameterList - any custom parameters in a comma separated list, eg. &parameterList=param1,value1,param2,value2
	 * @param cafilter - only run against CAs listed in this parameter, comma separated list of UUIDs.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/{reportuuid}")
	@Operation(description="Runs a report and returns the results.")
	public Response exectueReport(@Parameter(description="The report UUID") @PathParam("reportuuid") String reportUuid,
			@Parameter(description="only run against CAs listed in this parameter, comma separated list of UUIDs.") @QueryParam("cafilter") String cafilter,
			@Parameter(description="html, pdf, doc, odt are the options.") @QueryParam("format") String format,
			@Parameter(description="any custom parameters in a comma separated list, eg. &parameterList=param1,value1,param2,value2") @QueryParam("parameterList") String parameterList){

		UUID uuid = UuidUtils.stringToUuid(reportUuid);		
		
		Report report = null;
		

		ReportFormat rformat = null;
		for (ReportFormat fr : ReportFormat.values()){
			if (fr.getTypeKey().equalsIgnoreCase(format)){
				rformat= fr;
			}
		}
		if (rformat == null){
			throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("ReportApi.FormatNotSupported", request.getLocale()), format)); //$NON-NLS-1$
		}
		
		List<ConservationArea> conservationAreas = new ArrayList<ConservationArea>();
		try(Session s = HibernateManager.getSession(context, request.getLocale())){
			s.beginTransaction();
			try {
				report = (Report) s.get(Report.class, uuid);
				
				if (report == null) throw new SmartConnectException(Status.NOT_FOUND, Messages.getString("ReportApi.ReportNotFound", request.getLocale())); //$NON-NLS-1$
				
				//for shared links; these links do not have a user but we want to check the j_username which is set to the link creator
				//String name = (String)request.getAttribute("j_username");
				String name = request.getUserPrincipal().getName();
				
				//check for permission to this query for this user.
				if (!SecurityManager.INSTANCE.canAccess(s, name, ReportAction.RUNREPORT_KEY, uuid)){
					if (SecurityManager.INSTANCE.canAccess(s, name, ReportAction.RUNREPORT_KEY, report.getConservationArea().getUuid())){
						//access is OK since they have access to All Queries in this CA.
					}else{
						return createErrorResponse(Status.UNAUTHORIZED, Messages.getString("ReportApi.InvalidAccess", request.getLocale()));  //$NON-NLS-1$
					}
				}
				
				if (report.getConservationArea().getIsCcaa()){
					//we use the ccaafilter; otherwise we ignore it
					conservationAreas.addAll(parseCaFilter(cafilter, s));
				}else{
					conservationAreas.add(report.getConservationArea());
				}
			}finally {
				s.getTransaction().rollback();
			}
		}
				
		try{
			IReportEngine engine = BirtEngine.getBirtEngine(context);
			java.nio.file.Path reportFile = FileSystems.getDefault().getPath(
				report.getConservationArea().getFileDataStoreLocation(),
				Report.REPORT_DIR,
				report.getFilename());

			java.nio.file.Path p = Paths.get(context.getRealPath(REPORT_IMAGES_DIR));
			if (!Files.exists(p)) Files.createDirectories(p);
			
			try(InputStream inStream = Files.newInputStream(reportFile)){
					final IReportRunnable design = engine.openReportDesign(inStream);
					
				try(ByteArrayOutputStream bos = new ByteArrayOutputStream()){
					RenderOption options ;
					if (rformat == ReportFormat.HTML){
						options = new HTMLRenderOption();
						((HTMLRenderOption)options).setBaseImageURL(request.getContextPath() + REPORT_IMAGES_DIR); 
						((HTMLRenderOption)options).setImageDirectory(context.getRealPath(REPORT_IMAGES_DIR));
						((HTMLRenderOption)options).setHtmlPagination(true);
						((HTMLRenderOption)options).setEmbeddable(true);
						((HTMLRenderOption)options).setEnableInlineStyle(true);
						options.setOutputFormat(IHTMLRenderOption.HTML);
					}else{
						options = new RenderOption();
						options.setEmitterID(rformat.getEmitterId());
					}
					options.setSupportedImageFormats("PNG"); //$NON-NLS-1$
					options.setImageHandler(new HTMLServerImageHandler());
					options.setOutputStream(bos);
		
					IRunAndRenderTask task = new SmartRunAndRender((ReportEngine) engine, design, report.getConservationArea(), request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName()); //$NON-NLS-1$
		
					try(Session reportSession = HibernateManager.getSession(context, request.getLocale())){
						
						Map<Object,Object> items = task.getAppContext();
						items.put(BirtConstants.SESSION_PARAM, reportSession);
						items.put(SmartConnection.LOCALE_CONTEXT_VAR, request.getLocale());
						items.put(BirtConstants.CA_PARAM, report.getConservationArea());
						items.put(ServerSmartConnection.CCAA_FILTER_KEY, conservationAreas);
						items.put(ServerSmartConnection.CURRENT_USER_KEY, request.getUserPrincipal().getName());
							
						reportSession.beginTransaction();
						try{
							Map<String, Object> parameters = new HashMap<String, Object>();
								//put all the non-constant parameters in the hashmap	
							covertParametersToMap(parameters, parameterList, reportSession, uuid);

							task.setRenderOption(options);
							task.setParameterValues(parameters);
							task.run();
						}finally{
							reportSession.getTransaction().rollback();
							task.close();
						}
					}
					if(rformat != ReportFormat.HTML){
						return writeBinary(bos, rformat, report);
					}else{
						StringBuilder sb = new StringBuilder();
						sb.append("<html><head><link rel=\"shortcut icon\" href=\"/server/css/images/smart_fav_icon.png\"><title>SMART Connect - " ); //$NON-NLS-1$
						sb.append(report.getName());
						sb.append("</title></head><body>" ); //$NON-NLS-1$
						sb.append(bos.toString(StandardCharsets.UTF_8.name()));
						sb.append("</body></html>"); //$NON-NLS-1$
						return writeHtml(sb.toString());
					}
				}
			}
		}catch(Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("ReportApi.ReportError", request.getLocale()) + ex.getMessage());  //$NON-NLS-1$
		}
		
	}
	
	private Response writeBinary(ByteArrayOutputStream bos, ReportFormat outFormat, Report r){
			StreamingOutput sout = new StreamingOutput() {
			
			@Override
			public void write(OutputStream output) throws IOException,
					WebApplicationException {
				output.write(bos.toByteArray());
				
			}
		};
		
		ResponseBuilder rs = Response.ok(sout, outFormat.getResponseType() + " " ); //$NON-NLS-1$
		String content = outFormat.getContentDisposition(URLUtils.cleanFilename(r.getName()), r.getUuid());
		if (content != null){
			rs.header(HttpHeaders.CONTENT_DISPOSITION, content);
		}
		return rs.build();
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
	
	/**
	 * <p>Returns all report objects the current user is able to view</p>
	 *<p>
	 * URL: ../server/api/report<br>
	 * Call Type: GET
	 * </p>
	 * @return list of ReportProxy JSON objects
	 *
	 **/
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Returns all report objects the current user is able to view")
    public List<ReportProxy> getAllReportForUser(){
		List<ReportProxy> allowed = new ArrayList<ReportProxy>();

		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			//Check if they access to All Reports, if so it's simple, return them all
			if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, null)){
				 return getReports(s, request.getLocale(), false);
			}
			
			//short circuit check for access to > 0 reports, if not, return nothing. 
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY)){
				 return allowed;
			}
			
			//Get all Queries and check each one for specific permission to this user.
			List<ReportProxy> all = getReports(s, request.getLocale(), false);
			for (ReportProxy q : all){
				//Do they have access to all reports from this CA? if yes then add it.
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, q.getCaUuid())){
					allowed.add(q);
				}else{
					//Do they have specific permission to this report? if yes then add it.
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
		List<String> langs = new ArrayList<>();
		langs.add(l.getLanguage());
		if (!l.getCountry().isEmpty()) {
			langs.add(l.getLanguage() + "_" + l.getCountry()); //$NON-NLS-1$
			if (l.getVariant().isEmpty()) langs.add(l.getLanguage() + "_" + l.getCountry() + "_" + l.getVariant()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		HashMap<ReportProxy, String> query2names = new HashMap<>();
		
		String querypart = "SELECT r.uuid, r.id, r.shared, r.conservationArea.uuid, r.conservationArea.id, l.value, z.code " //$NON-NLS-1$
				+ " FROM Report as r JOIN Label as l on l.id.element = r.uuid JOIN l.id.language as z " //$NON-NLS-1$
				+ " WHERE l.id.element = r.uuid and (z.default = true or z.code in (:langs)) "; //$NON-NLS-1$
		
		List<?> results = session.createQuery(querypart)
				.setParameterList("langs",  langs) //$NON-NLS-1$
				.list();
		
		for (Object i : results) {
			Object[] data = (Object[])i;
			
			UUID ruuid = (UUID) data[0];
			String rid = (String)data[1];
			boolean isShared = (boolean)data[2];
			
			UUID cauuid = (UUID)data[3];
			String caid = (String)data[4];
			
			String value = (String)data[5];
			String code = (String)data[6];
			
			if (isShared || includeMyQueries) {
				ReportProxy rp = new ReportProxy(ruuid, value, caid, rid, isShared, cauuid, cauuid.equals(ConservationArea.MULTIPLE_CA));
					
				if (!query2names.containsKey(rp)) {
					query2names.put(rp, code);
				}else {
					String currentcode = query2names.get(rp);
					int cindex = langs.indexOf(currentcode);
					int nindex = langs.indexOf(code);
					if ((cindex == -1 && nindex >= 0) || (cindex != -1 && nindex > cindex)){
						query2names.remove(rp);
						query2names.put(rp, code);
					}
				}
			}
		}
		return new ArrayList<>(query2names.keySet());
	}
	
	/**
	 * <p>Returns the report requested, not the results of the report, but the report definition/DB row</p>
	 * <p>
	 * URL: ../server/api/report/definition/{uuid}<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param reportUuid  the report uuid, passed in through the url
	 * @return JSON representation of a ReportProxy object 
	 */
	@GET
	@Path("/definition/{reportuuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Returns the report requested, not the results of the report, but the report object definition/DB row")
	public ReportProxy getReport(@Parameter(description="the UUID of the report requested") @PathParam("reportuuid") String reportUuid){
		UUID uuid = UuidUtils.stringToUuid(reportUuid);
		
		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		Report r = null;
		ReportProxy proxy = null;
		try{		
			if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, uuid)){
				r = s.get(Report.class, uuid);
				proxy = new ReportProxy(r.getUuid(), r.getName(),  
						r.getConservationArea().getId(), r.getId(), r.getShared(), 
						r.getConservationArea().getUuid(),
						r.getConservationArea().getIsCcaa());
			}
		}finally{
			s.getTransaction().rollback();
		}
		return proxy;
	}
	
	
	/**
	 * <p>Returns all of the parameters of the given report</p>
	 * <p>
	 * URL: ../server/api/report/{uuid}/params<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param reportUuid the report uuid, passed in through the url
	 * @return a JSON list of ReportParameter objects 
	 */
	@GET
    @Path("/{reportuuid}/params")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Returns all of the parameters of the given report")
	public List<ReportParameter> getReportsParameters(@Parameter(description="the report uuid") @PathParam("reportuuid") String reportUuid) throws SmartConnectException{
		UUID uuid = UuidUtils.stringToUuid(reportUuid);
		Session s = HibernateManager.getSession(context, request.getLocale());
		
		List<ReportParameter> parameters; 
		try{
			s.beginTransaction();
			parameters = getParameters(uuid, s);
		}catch (Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw new SmartConnectException(Status.BAD_REQUEST, ex);
		}finally{
			s.getTransaction().rollback();
		}
		return parameters;
	}

	private List<ReportParameter> getParameters(UUID uuid, Session s) throws Exception {
		List<ReportParameter> rparameters = new ArrayList<ReportParameter>();
		
		Report report = (Report) s.get(Report.class, uuid);
		if (report == null) throw new SmartConnectException(Status.NOT_FOUND, Messages.getString("ReportApi.ReportNotFound", request.getLocale())); //$NON-NLS-1$
			
		List<IParameterDefnBase> parameters = ParameterFinder.INSTANCE.getParameters(report, BirtEngine.getBirtEngine(context));

		List<ConservationArea> cas = new ArrayList<>();
		if (!report.getConservationArea().getIsCcaa()) {
			cas.add(report.getConservationArea());
		}else {
			//add all CA's this user has access to
			List<ConservationAreaInfo> db = QueryFactory.buildQuery(s, ConservationAreaInfo.class).list();
			for (ConservationAreaInfo ca : db){
				ConservationArea smartca = (ConservationArea) s.get(ConservationArea.class, ca.getUuid());
				//check to determine if ca is accessible by current user
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), 
						CaAction.VIEWCA_KEY, smartca.getUuid())) {
					cas.add(smartca);
				}
			}
		}
		
		for (IParameterDefnBase param : parameters){
			if (param instanceof IParameterDefn){
				rparameters.add(createParameter(s, (IParameterDefn) param, cas));
			}else if (param instanceof IParameterGroupDefn){
				ReportParameter pp = new ReportParameter();
				pp.setName(param.getName());
				pp.setDisplayText(param.getPromptText());
				pp.setType(Type.GROUP);
				pp.setIsRequired(false);
				rparameters.add(pp);
				
				for (Object child: ((IParameterGroupDefn) param).getContents()){
					if (child instanceof IParameterDefn){
						pp.getChildren().add(createParameter(s,(IParameterDefn) child, cas));
					}else{
						throw new Exception(MessageFormat.format(Messages.getString("ReportApi.ParameterNotSupported", request.getLocale()), param.getName())); //$NON-NLS-1$
					}
				}					
			}else{
				throw new Exception(MessageFormat.format(Messages.getString("ReportApi.ParameterTypeNotSupported", request.getLocale()), param.getName())); //$NON-NLS-1$
			}
		}
		return rparameters;
	}
	
	private ReportParameter createParameter(Session session, IParameterDefn param, List<ConservationArea> cas) throws Exception{
		ReportParameter pp = new ReportParameter();
		pp.setName(param.getName());
		pp.setDisplayText(param.getPromptText());
		pp.setIsRequired(param.isRequired());
		boolean isList = false;
		if (param instanceof ScalarParameterDefn){
			pp.setDefaultValue(((ScalarParameterDefn)param).getDefaultValue());
			
			if (((ScalarParameterDefn)param).getControlType() == IScalarParameterDefn.LIST_BOX) {
				isList = true;
			}		
		}
		
		
		if (isList) {
			if(param.getHandle().getCustomXml() != null && 
					param.getHandle().getCustomXml().startsWith(ISmartBirtParameter.KEY)) {
				String key = param.getHandle().getCustomXml().substring(ISmartBirtParameter.KEY.length());
				for (String xkey : ParameterManager.INSTANCE.findParameter(key)
							.getValues(session, cas, request.getLocale())) {
					pp.addOption(xkey, xkey);
				}
				
			}else {
				for (Object x : param.getSelectionList()) {
					if (x instanceof ParameterSelectionChoice) {
						ParameterSelectionChoice c = (ParameterSelectionChoice)x;
						pp.addOption(c.getLabel(), c.getValue().toString());
					}
				}
			}
			
			if (!param.isRequired()) {
				pp.addOption(0, "", ""); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} 
		
		switch(param.getDataType()){
			case IParameterDefn.TYPE_DATE:
				pp.setType(Type.DATE);
				break;
			case IParameterDefn.TYPE_TIME:
				pp.setType(Type.TIME);
				break;
			case IParameterDefn.TYPE_DATE_TIME:
				pp.setType(Type.DATETIME);
				break;
			case IParameterDefn.TYPE_BOOLEAN:
				pp.setType(Type.BOOLEAN);
				break;
			case IParameterDefn.TYPE_DECIMAL:
			case IParameterDefn.TYPE_FLOAT:
				pp.setType(Type.DOUBLE);
				break;		
			case IParameterDefn.TYPE_INTEGER:
				pp.setType(Type.INTEGER);
				break;
			case IParameterDefn.TYPE_STRING:
				pp.setType(Type.STRING);
				break;
			case IParameterDefn.TYPE_ANY:
			default:
				throw new Exception(MessageFormat.format(Messages.getString("ReportApi.ParameterTypeNotSupported", request.getLocale()), new Object[]{ param.getDataType() })); //$NON-NLS-1$
		}
		return pp;
	}
		
	private Response createErrorResponse(Status code, String message){
		String error = MessageFormat.format("\"status\": {0}, \"error:\": \"" + message + "\"", code.getStatusCode(), message); //$NON-NLS-1$ //$NON-NLS-2$
		return Response
				.status(code)
				.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
				.entity("{" + error +"}") //$NON-NLS-1$ //$NON-NLS-2$
				.build();
	}
	
	private void covertParametersToMap(Map<String, Object> m, String l, Session s, UUID uuid) throws Exception {
		if (l.trim().isEmpty()) return;
		List<ReportParameter> parameters = getParameters(uuid, s);
		
		Map<String, ReportParameter> pmap = new HashMap<>();
		
		List<ReportParameter> toSearch = new ArrayList<ReportParameter>(parameters);
		while(toSearch.size() > 0){
			ReportParameter rp = toSearch.remove(0);
			if (rp.getType() == Type.GROUP){
				toSearch.addAll(rp.getChildren());
			}else {
				pmap.put(rp.getName(), rp);
			}
		}
		
		Object value;
		List<String> items = Arrays.asList(l.split(",", -1)); //$NON-NLS-1$
		for(int x=0; x < items.size()-1; x=x+2){	//parameters always come through with an extra , on the end
			String name = items.get(x);
			
			ReportParameter p = pmap.get(name);
			if (p == null) throw new SmartConnectException(Status.BAD_REQUEST, MessageFormat.format(Messages.getString("ReportApi.InvalidParameter", request.getLocale()), name)); //$NON-NLS-1$
			
			switch(p.getType()){
				case DATE:
					value = java.sql.Date.valueOf( SmartUtils.parseDate(items.get(x+1)) );
					break;
				case DATETIME:
					value = java.sql.Timestamp.valueOf(items.get(x+1));
					break;
				case DOUBLE:
					try {
						value = Double.valueOf(items.get(x+1));
					}catch (NumberFormatException ex) {
						throw new NumberFormatException(MessageFormat.format(Messages.getString("ReportApi.NumberRequired", request.getLocale()), p.getName(), items.get(x+1))); //$NON-NLS-1$
					}
					break;
				case INTEGER:
					try {
						value = Integer.valueOf(items.get(x+1));
					}catch (NumberFormatException ex) {
						throw new NumberFormatException(MessageFormat.format(Messages.getString("ReportApi.IntegerRequired", request.getLocale()), p.getName(), items.get(x+1)));  //$NON-NLS-1$
					}
					break;
				case STRING:
					value = items.get(x+1);
					break;
				case BOOLEAN:
					value = Boolean.valueOf(items.get(x+1));
					break;
				default:
					value = items.get(x+1);
					break;
			}
			m.put(name, value);
		}
	}
}

