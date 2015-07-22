package org.wcs.smart.connect.api;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.ConservationAreaInfo.Status;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.connect.model.UploadItem.Type;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;

import com.sun.istack.internal.logging.Logger;

@Path(ConnectRESTApplication.PATH_SEPERATOR + ConservationAreas.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConservationAreas extends HttpServlet{
	public static final String PATH = "conservationarea"; //$NON-NLS-1$
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConservationAreas.class);
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
		
	public void configure(ServletContext context, HttpHeaders headers, HttpServletResponse response, HttpServletRequest request){
		this.context = context;
		this.headers= headers;
		this.request = request;
		this.response = response;
	}
	
	private void validateRead(ConservationAreaInfo info, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.VIEWCA_KEY,
				info.getUuid())){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
		}
	}
	
	private void validateDelete(ConservationAreaInfo info, Session s){
		if (!SecurityManager.INSTANCE.canAccess(s, 
				request.getUserPrincipal().getName(), 
				CaAction.DELETECA_KEY,
				info.getUuid())){
			logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to delete ca."); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
		}
	}
	
	@GET
    @Path("/")
    public List<ConservationAreaInfo> getConservationAreas(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<ConservationAreaInfo> conservationAreas = new ArrayList<ConservationAreaInfo>();
			
			List<ConservationAreaInfo> db = s.createCriteria(ConservationAreaInfo.class)
					.list();
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
			logger.severe(ex.getMessage(), ex);
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
	@DELETE
    @Path("/{cauuid}")
    public void deleteConservationArea(@PathParam("cauuid") String caUuid){
		ConservationAreaInfo toDelete = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UUID uuid = UUID.fromString(caUuid);
			
			toDelete = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$
			if (toDelete == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, Messages.getString("ConservationAreas.DoesNotExist", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateDelete(toDelete, s);
			s.delete(toDelete);
			
			SQLQuery query = s.createSQLQuery("DELETE FROM smart.conservation_area WHERE uuid = '" + uuid.toString() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
			query.executeUpdate();
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.warning(ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.severe(ex.getMessage(), ex);
			s.getTransaction().rollback();
			logger.severe(ex.getMessage(), ex);
			throw new SmartConnectException(HttpURLConnection.HTTP_INTERNAL_ERROR, 
					Messages.getString("ConservationAreas.CouldNotDeleteCa", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		//delete all ca data and findstore
		try{
			DataStoreManager.INSTANCE.deleteDirectory(toDelete);
		}catch (Exception ex){
			logger.severe(Messages.getString("ConservationAreas.CouldNotDeleteFilestore", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}

	}
	
	@POST
	@Path("/{cauuid}")
	public String updateNewConservationArea(@PathParam("cauuid") String caUuid){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UUID uuid = UUID.fromString(caUuid);
			
			ConservationAreaInfo ca = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
					.add(Restrictions.eq("uuid", uuid)).uniqueResult(); //$NON-NLS-1$
			if (ca != null){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("ConservationAreas.CaExists", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
					
			String lengthHeader = headers.getRequestHeader("X-Upload-Content-Length").get(0); //$NON-NLS-1$
			if (lengthHeader == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "X-Upload-Content-Length not set"); //$NON-NLS-1$
			}
			int totalBytes = -1;
			try{
				totalBytes = Integer.parseInt(lengthHeader);
			}catch (Exception ex){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "X-Upload-Content-Length invalid value", ex); //$NON-NLS-1$
			}
			if (totalBytes <= 0){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "X-Upload-Content-Length invalid value"); //$NON-NLS-1$
			}
			
			ConservationAreaInfo cai = new ConservationAreaInfo();
			cai.setUuid(uuid);
			cai.setStatus(Status.UPLOADING);
			cai.setLabel(Messages.getString("ConservationAreas.UnknownLbl", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			cai.setVersion(SmartUtils.generateUUID(s, cai));
			s.save(cai);
							
			UploadItem up = new UploadItem();
			up.setConservationAreaInfo(cai);
			up.setStartTime(new Date());
			up.setStatus(UploadItem.Status.UPLOADING);
			up.setType(Type.CA);
			up.setTotalBytes(totalBytes);
			File caDir = DataStoreManager.INSTANCE.getConservationAreaFullPath(cai);
			if (!caDir.exists()){
				FileUtils.forceMkdir(caDir);
			}
			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName(
					DataStoreManager.INSTANCE.getConservationAreaFolder(cai)
					+ File.separator + 
					DataStoreManager.INSTANCE.getConservationAreaFolder(cai)
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
			logger.warning(ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.severe(ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Messages.getString("ConservationAreas.UploadErr", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
		
	}
}
