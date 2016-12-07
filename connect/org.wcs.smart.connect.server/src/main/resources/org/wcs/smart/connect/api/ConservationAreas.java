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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.type.PostgresUUIDType;
import org.mindrot.jbcrypt.BCrypt;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.downloader.ca.CaExporterJob;
import org.wcs.smart.connect.downloader.sync.CaChangeLogPackageJob;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.ConnectStartupContextListener;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConservationAreaInfo.Status;
import org.wcs.smart.connect.model.ConservationAreaProxy;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.util.UuidUtils;


/**
 * Smart Connect REST APU for conservation areas.
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ConservationAreas.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConservationAreas extends HttpServlet{
	
	private static final String DATA_PARAM_CHANGELOG_VALUE = "changelog"; //$NON-NLS-1$
	private static final String DATA_PARAM_ALL_VALUE = "all"; //$NON-NLS-1$
	private static final String DATA_PARAM_PACKAGE_VALUE = "package"; //$NON-NLS-1$

	public static final String PATH = "conservationarea"; //$NON-NLS-1$
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConservationAreas.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
		
	/**
	 * Configure the class parameters.  During normal use this will
	 * be done automatically.  This is only when you want to user this class
	 * outside for the REST workflow.
	 * 
	 * @param context
	 * @param headers
	 * @param response
	 * @param request
	 */
	public void configure(ServletContext context, HttpHeaders headers, HttpServletResponse response, HttpServletRequest request){
		this.context = context;
		this.headers= headers;
		this.request = request;
		this.response = response;
	}
	
	/*
	 * Ensures the current user has read access.
	 */
	private void validateRead(UUID cauuid, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.VIEWCA_KEY,
				cauuid)){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	/*
	 * Ensures the current user had delete access for the given ca.
	 */
	private void validateDelete(UUID cauuid, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.DELETECA_KEY,
				cauuid)){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to delete ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	/*
	 * Ensures the current user can create/add a ca to the database
	 */
	private void validateAdd(Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.ADDCA_KEY,
				null)){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to add a ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	
	/*
	 * Ensures the current user can update the give ca
	 */
	private void validateUpdate(UUID cauuid, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.UPDATECA_KEY,
				cauuid)){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to update the ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	/**
	 * List all Conservation Areas
	 * URL: ../server/api/conservationarea/
	 * Call Type: GET
	 * 
	 * @parameter organizationFilter - optional - only return CAs that have the provided text in the organization field
	 * @parameter caJsonFilter - optional - Must be a GeoJson polygon - only return CAs that are completely contained within this GeoJSON Polygon. ie. if a single point of the CA Boundary is outside of it, the ca will not be returned.
	 * 			be sure to encode the geojson, leave no spaces etc. An example of a simple polygon of the world:  caJsonFilter=%7B%22type%22%3A%22Polygon%22%2C%22coordinates%22%3A%5B%5B%5B-180%2C90%5D%2C%5B180%2C90%5D%2C%5B180%2C-90%5D%2C%5B-180%2C-90%5D%2C%5B-180%2C90%5D%5D%5D%7D
	 * 			originally it is caJsonFilter={"type":"Polygon","coordinates":[[[-180,90],[180,90],[180,-90],[-180,-90],[-180,90]]]}  use you local programming language urlencoder, or an online tool like this to do it out manually: http://meyerweb.com/eric/tools/dencoder/ 
	 * @return Returns a JSON array of ConservationAreaProxy objects for the updated user. (https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/ConservationAreaProxy.java)
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/")
    public List<ConservationAreaProxy> getConservationAreas(@QueryParam("organizationFilter") String organizationFilter, @QueryParam("caJsonFilter") String caJsonFilter){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<ConservationAreaProxy> conservationAreas = new ArrayList<ConservationAreaProxy>();
			List<ConservationAreaInfo> db = s.createCriteria(ConservationAreaInfo.class).list();
			for (ConservationAreaInfo ca : db){
				ConservationArea smartca = (ConservationArea) s.get(ConservationArea.class, ca.getUuid());
				ConservationAreaProxy proxy = new ConservationAreaProxy(ca);
				
				//check to determine if ca is accessible by current user
				try{
					validateRead(ca.getUuid(), s);

					if (smartca != null){
						proxy.setDescriptionDesignation(smartca.getDescription(), smartca.getDesignation());
						
						//add the extra metadata we have now
						proxy.setLocation(smartca.getCountry());
						proxy.setPointOfContact(smartca.getPointOfContact());
						proxy.setOrganization(smartca.getOrganization());
						proxy.setOwner(smartca.getOwner());
						
						proxy.setAdministrativeAreasJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom)) )) as geometry) ) from smart.area_geometries where ca_uuid = '" + smartca.getUuid().toString() + "' and area_type = '" + AreaType.ADMIN + "'").uniqueResult() );
						proxy.setCaBoundaryJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom)))) as geometry) ) from smart.area_geometries where ca_uuid = '" + smartca.getUuid().toString() + "' and area_type = '" + AreaType.CA + "'").uniqueResult() );
						proxy.setBufferedManagementAreaJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom))))as geometry) ) from smart.area_geometries where ca_uuid = '" + smartca.getUuid().toString() + "' and area_type = '" + AreaType.BA + "'").uniqueResult() );
						proxy.setManagementSectorsJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom))))as geometry) ) from smart.area_geometries where ca_uuid = '" + smartca.getUuid().toString() + "' and area_type = '" + AreaType.MNGT + "'").uniqueResult() );
						proxy.setPatrolSectorsJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom))))as geometry) ) from smart.area_geometries where ca_uuid = '" + smartca.getUuid().toString() + "' and area_type = '" + AreaType.PATRL + "'").uniqueResult() );
					}
					proxy.setRevision(ChangeLogManager.INSTANCE.getLastRevision(s, ca.getUuid()));
					if(ca.getVersion()== null){
						proxy.setVersion(null);
					}else{
						proxy.setVersion(ca.getVersion());
					}
					
				}catch(SmartConnectException ex){
					//not valid user; ignore
				}

				
				//Check FILTERS
				boolean passedBoundary = false;
				if(caJsonFilter != null && !caJsonFilter.equals("")){
					if(smartca == null){ //no-data CAs should not be returned when there is a geojson filter
						passedBoundary = false;
					}else{
						//if the JSON passed in contains the CABoundry, return true;
						Boolean result = (Boolean)s.createSQLQuery("Select st_contains(  (Select st_geomfromgeojson('" + caJsonFilter + "')) ,  (Select geom from area_geometries where ca_uuid = '" + smartca.getUuid().toString() + "' and area_type = '" + AreaType.CA + "')  )").uniqueResult();
						if(result != null && result == true){
							passedBoundary=true;//can't cast straight to a bool because the result is null if there is no caBoundary layer
						}else{
							passedBoundary=false;
						}
					}
				}else{
					passedBoundary=true; //no filter provided or it is blank, assume they want everything
				}
				boolean passedOrg = false;
				if(organizationFilter == null || organizationFilter.equals("") || (proxy.getOrganization() != null && proxy.getOrganization().toUpperCase().contains(organizationFilter.toUpperCase())) ){
					passedOrg = true;
				}else{
					passedOrg = false;
				}
				if(passedOrg && passedBoundary)conservationAreas.add(proxy);
				
			}
			
			Collections.sort(conservationAreas, new Comparator<ConservationAreaProxy>() {
				@Override
				public int compare(ConservationAreaProxy o1,
						ConservationAreaProxy o2) {
					return o1.getLabel().toUpperCase().compareTo(o2.getLabel().toUpperCase());
				}
			});
			return conservationAreas;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			if(ex instanceof GenericJDBCException){
				throw new SmartConnectException(Response.Status.BAD_REQUEST,
						Messages.getString("ConservationAreas.InvalidJson", SmartUtils.getRequestLocale(request))+ caJsonFilter, ex); //$NON-NLS-1$
			}
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConservationAreas.CaListError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * List all Conservation Areas that have SMART data
	 * URL: ../server/api/conservationarea/withdataonly/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON array of ConservationAreaProxy objects for the updated user. Only returns the CAs with Desktop Data associated.
	 * (https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/ConservationAreaProxy.java)
	 */
	@GET
    @Path("/withdataonly/")
    public List<ConservationAreaProxy> getConservationAreasWithData(){
		
		List<ConservationAreaProxy> conservationAreas = new ArrayList<ConservationAreaProxy>();
		List<ConservationAreaProxy> allConservationAreas = getConservationAreas(null,null);
		for (ConservationAreaProxy ca : allConservationAreas){
			if(ca.getStatus().equals(ConservationAreaInfo.Status.CCAA) || ca.getStatus().equals(ConservationAreaInfo.Status.DATA)){
				conservationAreas.add(ca);
			}
		}
		return conservationAreas;
	}

	/**
	 * Gets a conservation area.  
	 * This function returns different information depending on parameters
	 * provided:
	 *  If no parameters are provided it returns a JSON object
	 * with information about the conservation area.  
	 * If data, version, and revision
	 * are provided with a value of "changelog" for data then a zip file is
	 * returned containing the change log.  In data is provided with a value of 
	 * "all" then a url is returned that represents the status of the ca download
	 * package process.
	 * 
	 * URL: ../server/api/conservationarea/{cauuid}
	 * Call Type: GET
	 * 
	 * @param	caUuid	provided in the URL; This is the CA's UUID you want information about.
	 */
	@GET
    @Path("/{cauuid}")
    public Response getConservationArea(@PathParam("cauuid") String caUuid,
    		@QueryParam("data") String data,
    		@QueryParam("version") String version,
    		@QueryParam("revision") String revision){
		//user validatation is done inside the various functions
		if (data == null){
			return getConservationAreaInfo(caUuid);
		}else {
			if (data.equalsIgnoreCase(DATA_PARAM_CHANGELOG_VALUE)){
				if (version != null && revision != null){
					return buildChangeLog(caUuid, version, revision);
				}else{
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.MissingEelement", SmartUtils.getRequestLocale(request)));			 //$NON-NLS-1$
				}
			}else if (data.equalsIgnoreCase(DATA_PARAM_ALL_VALUE)){
				return buildConservationAreaExport(caUuid);
			}else if (data.equalsIgnoreCase(DATA_PARAM_PACKAGE_VALUE)){
				return getExportFile(version);
			}else{
				throw new SmartConnectException(Response.Status.BAD_REQUEST, MessageFormat.format(Messages.getString("ConservationAreas.InvalidDataParameter", SmartUtils.getRequestLocale(request)), data, DATA_PARAM_ALL_VALUE, DATA_PARAM_CHANGELOG_VALUE));			 //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Gets the download file for the ca.  Ensures the user has read permissions to the ca.
	 * @param statusUuid
	 * @return
	 */
	private Response getExportFile(String statusUuid){
		UUID uuid = null;
		try{
			uuid= UuidUtils.stringToUuid(statusUuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + statusUuid + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.BadRequest", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	
		WorkItem item = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			item = (WorkItem) s.get(WorkItem.class, uuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.DownloadPackageNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateRead(item.getConservationAreaInfo().getUuid(), s);
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			logger.log(Level.SEVERE, Messages.getString("ConservationAreas.DownloadError", SmartUtils.getRequestLocale(request)) + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.DownloadError", SmartUtils.getRequestLocale(request)), ex);  //$NON-NLS-1$
		}
	
		if (item.getType() != WorkItem.Type.DOWN_CA &&
				item.getType() != WorkItem.Type.DOWN_SYNC){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.BadRequest", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (item.getStatus() != WorkItem.Status.COMPLETE){
			throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.PackageNotCreated", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		final File toReturn = DataStoreManager.INSTANCE.getFile(item.getLocalFilename());
		if (!toReturn.exists()){
			throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.CaExportNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		String range = request.getHeader("Range"); //$NON-NLS-1$
		long start = 0;
		long end = toReturn.length()-1;
		boolean hasRange = false;
		if (range != null && range.length() > 0){
			hasRange = true;
			//parse the range
			try{
				String regex = "^bytes=([0-9]*)-([0-9]*)$"; //$NON-NLS-1$
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(range);
				
				String p1 = m.group(0);
				String p2 = m.group(1);
				if (!p1.isEmpty()){
					start = Long.parseLong(p1);
				}
				if (!p2.isEmpty()){
					end = Long.parseLong(p2);
				}
				
				if (end > toReturn.length()-1){
					throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, Messages.getString("ConservationAreas.InvalidRange", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
				}
				if (start > end){
					throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, Messages.getString("ConservationAreas.InvalidRange2", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, Messages.getString("ConservationAreas.InvalidRange3", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
		}

		if (!hasRange){
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try {
						Files.copy(toReturn.toPath(), output);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error writing to output stream." + e.getMessage(), e); //$NON-NLS-1$
					}
				}
		    };
			    
			return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getName() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.header(HttpHeaders.CONTENT_LENGTH, toReturn.length())
					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
					.build();
		}else{
			final long startat = start;
			final long endat = end;
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try (InputStream in = Files.newInputStream(toReturn.toPath())){
						IOUtils.copyLarge(in, output, startat, endat - startat + 1);
					}
				}
		    };
			    
			return Response.status(Response.Status.PARTIAL_CONTENT)
					.entity(stream)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getName() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.header(HttpHeaders.CONTENT_LENGTH, end - start + 1)
					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
					.header("Content-Range", "bytes " + start + "-" + end + "/" + toReturn.length()) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					.build();
		}
	}
	
	/**
	 * Initiates the process to build a conservation area extract
	 * and returns the location of a status url where the status
	 * can be retrieved and when complete the download url.
	 * Validate the user has read ca permission
	 * @param caUuid
	 * @return
	 */
	private Response buildConservationAreaExport(String caUuid){
		UUID uca = null;
		try{
			uca = UuidUtils.stringToUuid(caUuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + caUuid + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.BadRequest", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		WorkItem item = null;
		ConservationAreaInfo info = null;
		//package up change log
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			validateRead(uca, s);
			info = (ConservationAreaInfo) s.get(ConservationAreaInfo.class, uca);
			if (info == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.CANotFound", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
			}

			//create a new download item
			item = new WorkItem();
			item.setLocale(request.getLocale());
			item.setConservationAreaInfo(info);
			item.setLocalFilename(""); //$NON-NLS-1$
			item.setMessage(null);
			item.setStartTime(new Date());
			item.setStatus(WorkItem.Status.PROCESSING);
			item.setTotalBytes(-1);
			item.setType(WorkItem.Type.DOWN_CA);
			
			s.save(item);

			s.getTransaction().commit();
		}catch(SmartConnectException ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Unable to start download Conservation Area process. " + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.CaExportError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		
		try{
			String finishurl = 	request.getScheme() + "://" + request.getServerName()  //$NON-NLS-1$
					+ ":" + request.getServerPort()  //$NON-NLS-1$
					+ request.getContextPath() 
					+ ConnectRESTApplication.PATH_SEPERATOR + ConnectRESTApplication.APP_PATH + ConnectRESTApplication.PATH_SEPERATOR
					+ ConservationAreas.PATH + "/" //$NON-NLS-1$
					+ URLEncoder.encode(uca.toString(), ConnectRESTApplication.UTF8)
					+ "?data=" + DATA_PARAM_PACKAGE_VALUE + "&version=" + item.getUuid().toString(); //$NON-NLS-1$ //$NON-NLS-2$
	
			ExecutorService executor = (ExecutorService) context.getAttribute(ConnectStartupContextListener.EXECUTOR_KEY);
			executor.execute(new CaExporterJob(info, item, finishurl, HibernateManager.getSessionFactory(context)));
				
			String url = item.getStatusURL(request);
			return Response
					.status(Response.Status.ACCEPTED)
					.header(HttpHeaders.LOCATION, url)
					.entity("{\"location\": \"" + url + "\"}").build(); //$NON-NLS-1$ //$NON-NLS-2$
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Unable to start download conservation area process. " + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("ConservationAreas.CaExportError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
	
	/**
	 * Validates the user has read permission to the given ca then initiates
	 * the building change log process.
	 * @param caUuid
	 * @param version
	 * @param revision
	 * @return
	 */
	private Response buildChangeLog(String caUuid, String version, String revision) {
		
		UUID uca = null;
		UUID uversion = null;
		Long lrevision = -1l;
		
		try{
			uversion = UuidUtils.stringToUuid(version);
			lrevision = Long.valueOf(revision);
			uca = UuidUtils.stringToUuid(caUuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid parameters.  Cannot parse one of the conservation area uuid, version uuid or revision number. " + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.BadRequest", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		WorkItem item = null;
		ConservationAreaInfo info = null;
		//package up change log
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			validateRead(uca, s);
			info = (ConservationAreaInfo) s.get(ConservationAreaInfo.class, uca);
			if (info == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.CANotFound", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
			}

			if (!info.getVersion().equals(uversion)){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.VersionsDoNotMatch", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			//create a new download item
			item = new WorkItem();
			item.setLocale(request.getLocale());
			item.setConservationAreaInfo(info);
			item.setLocalFilename(""); //$NON-NLS-1$
			item.setMessage(null);
			item.setStartTime(new Date());
			item.setStatus(WorkItem.Status.PROCESSING);
			item.setTotalBytes(-1);
			item.setType(WorkItem.Type.DOWN_SYNC);
			s.save(item);

			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Unable to start Conservation Area change log download. " + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.CaChangeLogError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		
		try{
			String finishurl = 	request.getScheme() + "://" + request.getServerName()  //$NON-NLS-1$
				+ ":" + request.getServerPort()  //$NON-NLS-1$
				+ request.getContextPath() 
				+ ConnectRESTApplication.PATH_SEPERATOR + ConnectRESTApplication.APP_PATH + ConnectRESTApplication.PATH_SEPERATOR
				+ ConservationAreas.PATH + "/" //$NON-NLS-1$
				+ URLEncoder.encode(uca.toString(), ConnectRESTApplication.UTF8)
				+ "?data=" + DATA_PARAM_PACKAGE_VALUE + "&version=" + item.getUuid().toString(); //$NON-NLS-1$ //$NON-NLS-2$

			CaChangeLogPackageJob job = new CaChangeLogPackageJob(info, item, finishurl, lrevision, s.getSessionFactory());
			ExecutorService executor = (ExecutorService) context.getAttribute(ConnectStartupContextListener.EXECUTOR_KEY);
			executor.execute(job);
		
			String url = item.getStatusURL(request);
			return Response
				.status(Response.Status.ACCEPTED)
				.header(HttpHeaders.LOCATION, url)
				.entity("{\"location\": \"" + url + "\"}").build(); //$NON-NLS-1$ //$NON-NLS-2$
		
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Unable to start Conservation Area change log package process. " + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("ConservationAreas.CaChangeLogError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
	
	
	private Response getConservationAreaInfo(String caUuid){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UUID cuuid = UUID.fromString(caUuid);
			try {
				validateRead(cuuid, s);
			}catch (Exception ex){
				try{
					validateAdd(s);
				}catch (Exception e2){
					throw ex;
				}
			}
			
			ConservationAreaInfo info = (ConservationAreaInfo) s.get(ConservationAreaInfo.class, cuuid);
			if (info == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			ConservationAreaProxy proxy = new ConservationAreaProxy(info);
			
			ConservationArea ca = (ConservationArea) s.get(ConservationArea.class, cuuid);
			if (ca != null){
				proxy.setDescriptionDesignation(ca.getDescription(), ca.getDesignation());
				
				proxy.setLocation(ca.getCountry());
				proxy.setPointOfContact(ca.getPointOfContact());
				proxy.setOrganization(ca.getOrganization());
				proxy.setOwner(ca.getOwner());
				
				proxy.setAdministrativeAreasJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom)) )) as geometry) ) from smart.area_geometries where ca_uuid = '" + ca.getUuid().toString() + "' and area_type = '" + AreaType.ADMIN + "'").uniqueResult() );
				proxy.setCaBoundaryJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom)))) as geometry) ) from smart.area_geometries where ca_uuid = '" + ca.getUuid().toString() + "' and area_type = '" + AreaType.CA + "'").uniqueResult() );
				proxy.setBufferedManagementAreaJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom))))as geometry) ) from smart.area_geometries where ca_uuid = '" + ca.getUuid().toString() + "' and area_type = '" + AreaType.BA + "'").uniqueResult() );
				proxy.setManagementSectorsJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom))))as geometry) ) from smart.area_geometries where ca_uuid = '" + ca.getUuid().toString() + "' and area_type = '" + AreaType.MNGT + "'").uniqueResult() );
				proxy.setPatrolSectorsJson((String)s.createSQLQuery("select st_asgeojson(1, CAST( st_asbinary(st_force2d(st_geomfromwkb(st_union(geom))))as geometry) ) from smart.area_geometries where ca_uuid = '" + ca.getUuid().toString() + "' and area_type = '" + AreaType.PATRL + "'").uniqueResult() );
			}
			proxy.setRevision(ChangeLogManager.INSTANCE.getLastRevision(s, info.getUuid()));
			return Response.ok().entity(proxy).build();
		}catch (SmartConnectException ex){
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConservationAreas.CaListError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}
	/*
	 * TODO: this might need to be done as a background process incase
	 * deleting takes a long time
	 */
	/**
	 * Deletes a given conservation area.
	 * URL: ../server/api/conservationarea/{cauuid}
	 * Call Type: DELETE
	 * 
	 * @param	caUuid	provided in the URL, the ca UUID you wish to delete
	 * @return	void
	 */
	@DELETE
    @Path("/{cauuid}")
    public void deleteConservationArea(@PathParam("cauuid") String caUuid, 
    		@QueryParam("dataonly") String dataonly,
    		@QueryParam("username") String username,
    		@QueryParam("password") String password,
    		@QueryParam("version") String version){
		
		if (username == null || password == null || username.length() == 0 || password.length() == 0){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.UserAndPasswordRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		if (request.getSession(false) != null){
			//valid user against current session
			if (!username.equals(request.getUserPrincipal().getName())){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}
		boolean deleteAll = false;
		if (dataonly == null || dataonly.length() == 0){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.InvalidDataOnlyParameter", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		try{
			deleteAll = !Boolean.valueOf(dataonly);
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.InvalidDataOnlyParameter", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		UUID caUuidToDelete = null;
		Session s = HibernateManager.getSession(context, SmartUtils.getRequestLocale(request));
		s.beginTransaction();
		try{
			SmartUser su = HibernateManager.getUser(s, username);
			if (su == null){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
			if (!BCrypt.checkpw(password, su.getPassword())){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
			
			
			UUID uuid = UUID.fromString(caUuid);
			
			ConservationAreaInfo serverDelete = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$
			if (serverDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.DoesNotExist", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			if((version.equals(null) || version.equals("")) && serverDelete.getVersion()==null){
				//no problem, you can delete unversioned CAs without a version paramater.
			}else if((version == null || version.equals("")) || !(serverDelete.getVersion().equals(UUID.fromString(version))) ){ //null version no longer acceptable at this point, or it doens't match the version
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.VersionDoesNotExist" + serverDelete.getVersion() + "  -- " + UUID.fromString(version), SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateDelete(serverDelete.getUuid(), s);

			
			//need to do some of the (deleteall) work before the desktop data is gone
			caUuidToDelete = serverDelete.getUuid();
			if (deleteAll){
				// delete query actions associated with any query from the CA being deleted
				QueryManager.INSTANCE.removeAccessToQueriesFromCa(serverDelete.getUuid(), s);
			}
			
			//delete desktop data
			String query = "DELETE FROM smart.conservation_area WHERE uuid = :uuid"; //$NON-NLS-1$
			Query q = s.createSQLQuery(query);
			q.setParameter("uuid", serverDelete.getUuid(), PostgresUUIDType.INSTANCE); //$NON-NLS-1$
			q.executeUpdate();
			
			//delete plugin data
			q = s.createQuery("DELETE FROM CaPluginVersion WHERE id.conservationAreaUuid = :ca"); //$NON-NLS-1$
			q.setParameter("ca", serverDelete.getUuid()); //$NON-NLS-1$
			q.executeUpdate();

			//delete change log data
			ChangeLogManager.INSTANCE.deleteItems(s, serverDelete.getUuid());
			
			caUuidToDelete = serverDelete.getUuid();
			if (deleteAll){
				
				//delete actions associated with resource
				q = s.createQuery("DELETE FROM SmartUserAction WHERE resource = :ca"); //$NON-NLS-1$
				q.setParameter("ca", serverDelete.getUuid()); //$NON-NLS-1$
				q.executeUpdate();
				
				//delete server only data
				s.delete(serverDelete);
			}else{
				serverDelete.setStatus(Status.NODATA);
				serverDelete.setVersion(null);
			}
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConservationAreas.CouldNotDeleteCa", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		//delete all ca data and findstore
		try{
			DataStoreManager.INSTANCE.deleteDirectory(caUuidToDelete);
		}catch (Exception ex){
			logger.severe(Messages.getString("ConservationAreas.CouldNotDeleteFilestore", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}

	}
	
	/**
	 * Creates a new conservation area with no data.  Both parameters are optional and generated
	 * by the system if not provided.
	 * 
	 * URL: ../server/api/conservationarea/
	 * Call Type: POST
	 * Payload: none
	 * 
	 * @param caUuid The CA UUID you wish to create, leave it blank if you want the system to create one for you.
	 * @param name 	The CA Name
	 * @return
	 */
	@POST
	@Path("")
	public void createConservationArea(@QueryParam("cauuid") String caUuid, @QueryParam("name") String name){
		UUID uuid = null;
		if (caUuid != null && !caUuid.trim().isEmpty()){
			try{
				uuid = UuidUtils.stringToUuid(caUuid);
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, MessageFormat.format(Messages.getString("ConservationAreas.InvalidCaUuid", SmartUtils.getRequestLocale(request)), caUuid));	 //$NON-NLS-1$
			}
		}else{
			uuid = UUID.randomUUID();
		}
		
		if (name == null || name.trim().isEmpty()){
			name = Messages.getString("ConservationAreas.UnknownLabel", SmartUtils.getRequestLocale(request)); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			validateAdd(s);
			
			ConservationAreaInfo ca = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$
			if (ca != null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.CaExistsError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			ca = new ConservationAreaInfo();
			ca.setLabel(name);
			ca.setUuid(uuid);
			ca.setVersion(null);
			ca.setStatus(Status.NODATA);
					
			s.save(ca);
			s.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("ConservationAreas.CaNotCreated", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
	}
		
	
	/**
	 * Initiates an upload CA session. Used when uploading a full CA from SMART Desktop
	 * 
	 * @param caUuid
	 * @param version
	 * @return
	 */
	@POST
	@Path("/{cauuid}")
	public String createAndUploadConservationArea(@PathParam("cauuid") String caUuid, @QueryParam("version") String versionUuid){
		if (versionUuid == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.VersionNotSupplied", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		UUID version = null;
		try{
			version = UUID.fromString(versionUuid);
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.InvalidVersion", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UUID uuid = UUID.fromString(caUuid);
			ConservationAreaInfo ca = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$

					
			String lengthHeader = headers.getRequestHeader("X-Upload-Content-Length").get(0); //$NON-NLS-1$
			if (lengthHeader == null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length not set"); //$NON-NLS-1$
			}
			long totalBytes = -1;
			try{
				totalBytes = Long.parseLong(lengthHeader);
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value", ex); //$NON-NLS-1$
			}
			if (totalBytes <= 0){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value"); //$NON-NLS-1$
			}
			
			if (ca == null){
				validateAdd(s);
				ca = new ConservationAreaInfo();
				ca.setUuid(uuid);
				ca.setStatus(Status.UPLOADING);
				ca.setLabel(Messages.getString("ConservationAreas.UnknownLbl", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				ca.setVersion(version);
				s.save(ca);
				
				//configure default permissions for user
				SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
				if (user != null){
					//delete
					SmartUserAction newaction = new SmartUserAction();
					newaction.setAction(CaAction.DELETECA_KEY);
					newaction.setUsername(user.getUsername());
					newaction.setResource(ca.getUuid());
					s.save(newaction);
					//view
					newaction = new SmartUserAction();
					newaction.setAction(CaAction.VIEWCA_KEY);
					newaction.setUsername(user.getUsername());
					newaction.setResource(ca.getUuid());
					s.save(newaction);
					//update
					newaction = new SmartUserAction();
					newaction.setAction(CaAction.UPDATECA_KEY);
					newaction.setUsername(user.getUsername());
					newaction.setResource(ca.getUuid());
					s.save(newaction);
				}
				
			}else{
				validateUpdate(ca.getUuid(), s);
				if (ca.getStatus() == Status.NODATA){
					//we can upload data
					ca.setVersion(version);
					ca.setStatus(Status.UPLOADING);
					s.saveOrUpdate(ca);
				}else{
					//otherwise ca already exists
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConservationAreas.CaExists", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}
							
			WorkItem up = new WorkItem();
			up.setLocale(request.getLocale());
			up.setConservationAreaInfo(ca);
			up.setStartTime(new Date());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_CA);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename(""); //$NON-NLS-1$
			s.save(up);
			
			File updir = DataStoreManager.INSTANCE.getFile(Uploader.DATASTORE_DIR);
			if (!updir.exists()){
				FileUtils.forceMkdir(updir);
			}
			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName(Uploader.DATASTORE_DIR 
					+ File.separator + UuidUtils.uuidToString(up.getUuid()) + ".zip")); //$NON-NLS-1$
			
			s.save(up);
			
			//we have a file to uploade and we expect more
			
			response.setHeader(HttpHeaders.LOCATION, up.getStatusURL(request));
			response.setHeader(HttpHeaders.CONTENT_LENGTH, "0"); //$NON-NLS-1$
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Messages.getString("ConservationAreas.UploadErr", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
		
	}
	
	/**
	 * Initiates an upload CA session.
	 * 
	 * @param caUuid
	 * @return
	 */
	@PUT
	@Path("/{cauuid}")
	public String updateConservationArea(@PathParam("cauuid") String caUuid){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UUID uuid = UUID.fromString(caUuid);
			ConservationAreaInfo ca = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$
			if (ca == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConservationAreas.CaNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateUpdate(ca.getUuid(), s);
			
			String lengthHeader = headers.getRequestHeader("X-Upload-Content-Length").get(0); //$NON-NLS-1$
			if (lengthHeader == null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length not set"); //$NON-NLS-1$
			}
			long totalBytes = -1;
			try{
				totalBytes = Long.parseLong(lengthHeader);
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value", ex); //$NON-NLS-1$
			}
			if (totalBytes <= 0){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value"); //$NON-NLS-1$
			}
							
			WorkItem up = new WorkItem();
			up.setLocale(request.getLocale());
			up.setConservationAreaInfo(ca);
			up.setStartTime(new Date());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_SYNC);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename(""); //$NON-NLS-1$
			s.save(up);
			
			File updir = DataStoreManager.INSTANCE.getFile(Uploader.DATASTORE_DIR);
			if (!updir.exists()){
				FileUtils.forceMkdir(updir);
			}
			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName(Uploader.DATASTORE_DIR 
					+ File.separator + UuidUtils.uuidToString(up.getUuid()) + ".zip")); //$NON-NLS-1$
		
			s.saveOrUpdate(up);
			
			//we have a file to uplodate and we expect more
			String uploadURL = request.getScheme() + "://" + request.getServerName()  //$NON-NLS-1$
					+ ":" + request.getServerPort()  //$NON-NLS-1$
					+ request.getContextPath() 
					+ ConnectRESTApplication.PATH_SEPERATOR + ConnectRESTApplication.APP_PATH + ConnectRESTApplication.PATH_SEPERATOR
					+ Uploader.PATH + "/" //$NON-NLS-1$
					+ URLEncoder.encode(up.getUuid().toString(), ConnectRESTApplication.UTF8);
			
			response.setHeader(HttpHeaders.LOCATION, uploadURL);
			response.setHeader(HttpHeaders.CONTENT_LENGTH, "0"); //$NON-NLS-1$
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Messages.getString("ConservationAreas.UploadErr", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
}
