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
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.mindrot.jbcrypt.BCrypt;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConservationAreaInfo.Status;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.connect.model.UploadItem.Type;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;


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
	private void validateRead(ConservationAreaInfo info, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.VIEWCA_KEY,
				info.getUuid())){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
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
			throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
		}
	}
	
	/**
	 * Lists all conservation areas that the current user has access
	 * to read.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/")
    public List<ConservationAreaInfo> getConservationAreas(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<ConservationAreaInfo> conservationAreas = new ArrayList<ConservationAreaInfo>();
			List<ConservationAreaInfo> db = s.createCriteria(ConservationAreaInfo.class).list();
			for (ConservationAreaInfo ca : db){
				//check to determine if ca is accessable by current user
				try{
					validateRead(ca, s);
					conservationAreas.add(ca);
				}catch(SmartConnectException ex){
					//not valid; ignore
				}
				
			}
			return conservationAreas;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(HttpURLConnection.HTTP_INTERNAL_ERROR, 
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
	 * 
	 * @param caUuid
	 */
	@GET
    @Path("/{cauuid}")
    public ConservationAreaInfo getConservationArea(@PathParam("cauuid") String caUuid){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ConservationAreaInfo info = (ConservationAreaInfo) s.get(ConservationAreaInfo.class, UUID.fromString(caUuid));
			if (info == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
			return info;
		}catch (SmartConnectException ex){
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(HttpURLConnection.HTTP_INTERNAL_ERROR, 
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
	 * 
	 * @param caUuid
	 */
	@DELETE
    @Path("/{cauuid}")
    public void deleteConservationArea(@PathParam("cauuid") String caUuid, 
    		@QueryParam("dataonly") String dataonly,
    		@QueryParam("username") String username,
    		@QueryParam("password") String password){
		
		if (username == null || password == null || username.length() == 0 || password.length() == 0){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Must resupply username and password and query parameter");
		}
		
		if (request.getSession(false) != null){
			//valid user against current session
			if (!username.equals(request.getUserPrincipal().getName())){
				throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
			}
		}
		boolean deleteAll = false;
		if (dataonly == null || dataonly.length() == 0){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid value for query parameter dataonly");
		}
		try{
			deleteAll = !Boolean.valueOf(dataonly);
		}catch (Exception ex){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid value for query parameter dataonly");
		}
		
		UUID caUuidToDelete = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUser su = HibernateManager.getUser(s, username);
			if (su == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
			}
			if (!BCrypt.checkpw(password, su.getPassword())){
				throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
			}
			
			
			UUID uuid = UUID.fromString(caUuid);
			
			ConservationAreaInfo serverDelete = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$
			if (serverDelete == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, Messages.getString("ConservationAreas.DoesNotExist", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateDelete(serverDelete.getUuid(), s);

			//delete desktop data
			String query = "DELETE FROM smart.conservation_area WHERE uuid = '" + serverDelete.getUuid().toString() + "'";
			s.createSQLQuery(query).executeUpdate();
			caUuidToDelete = serverDelete.getUuid();
			
			if (deleteAll){
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
			throw new SmartConnectException(HttpURLConnection.HTTP_INTERNAL_ERROR, 
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
	 * Initiates an upload CA session.
	 * 
	 * @param caUuid
	 * @return
	 */
	@POST
	@Path("/{cauuid}")
	public String createAndUploadConservationArea(@PathParam("cauuid") String caUuid, @QueryParam("version") String versionUuid){
		if (versionUuid == null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "A version must be supplied");
		}
		UUID version = null;
		try{
			version = UUID.fromString(versionUuid);
		}catch (Exception ex){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "A version must be a valid uuid.");
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UUID uuid = UUID.fromString(caUuid);
			ConservationAreaInfo ca = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$

					
			String lengthHeader = headers.getRequestHeader("X-Upload-Content-Length").get(0); //$NON-NLS-1$
			if (lengthHeader == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "X-Upload-Content-Length not set"); //$NON-NLS-1$
			}
			long totalBytes = -1;
			try{
				totalBytes = Long.parseLong(lengthHeader);
			}catch (Exception ex){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "X-Upload-Content-Length invalid value", ex); //$NON-NLS-1$
			}
			if (totalBytes <= 0){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "X-Upload-Content-Length invalid value"); //$NON-NLS-1$
			}
			
			if (ca == null){
				ca = new ConservationAreaInfo();
				ca.setUuid(uuid);
				ca.setStatus(Status.UPLOADING);
				ca.setLabel(Messages.getString("ConservationAreas.UnknownLbl", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				ca.setVersion(version);
				s.save(ca);
			}else{
				if (ca.getStatus() == Status.NODATA){
					//we can upload data
					ca.setStatus(Status.UPLOADING);
					s.saveOrUpdate(ca);
				}else{
					//otherwise ca already exists
					throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("ConservationAreas.CaExists", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}
							
			UploadItem up = new UploadItem();
			up.setConservationAreaInfo(ca);
			up.setStartTime(new Date());
			up.setStatus(UploadItem.Status.UPLOADING);
			up.setType(Type.CA);
			up.setTotalBytes(totalBytes);
			File caDir = DataStoreManager.INSTANCE.getConservationAreaFullPath(ca);
			if (!caDir.exists()){
				FileUtils.forceMkdir(caDir);
			}
			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName(
					DataStoreManager.INSTANCE.getConservationAreaFolder(ca)
					+ File.separator + 
					DataStoreManager.INSTANCE.getConservationAreaFolder(ca)
					+ ".zip")); //$NON-NLS-1$
		
			s.save(up);
			
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
