package org.wcs.smart.connect.api;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.AlertFilterDefault;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;


@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectAlertFilterDefault.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectAlertFilterDefault extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = Logger.getLogger(ConnectAlertFilterDefault.class.getName());
	
	public static final String PATH = "connectalertfilterdefault"; //$NON-NLS-1$

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	private void validateUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AlertAction.KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have alert management permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@GET
    @Path("")
    public List<AlertFilterDefault> getAlertFilterDefaults(){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return HibernateManager.getAlertFilterDefaults(s);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@PUT
    @Path("/{uuid}")
    public AlertFilterDefault updateAlertFilterDefault(@PathParam("uuid") UUID uuid, AlertFilterDefault newDefault) {
    	validateUser();
    	
    	AlertFilterDefault toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (AlertFilterDefault)s.createCriteria(AlertFilterDefault.class)
					.add(Restrictions.eq("uuid", uuid)) //$NON-NLS-1$
					.uniqueResult();
			
			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertFilterDefaultsFound", SmartUtils.getRequestLocale(request)), uuid)); //$NON-NLS-1$
			}
			
			if (newDefault.getDefaultCaUuids()!= null){
				toUpdate.setDefaultCaUuids(newDefault.getDefaultCaUuids());
			}
			if (newDefault.getDefaultPastHours() != 0){
				toUpdate.setDefaultPastHours(newDefault.getDefaultPastHours());
			}
			if (newDefault.getDefaultText()!= null){
				toUpdate.setDefaultText(newDefault.getDefaultText());
			}
			if (newDefault.getDefaultTypeUuids()!= null){
				toUpdate.setDefaultTypeUuids(newDefault.getDefaultTypeUuids());
			}
			toUpdate.setDefaultActive(newDefault.isDefaultActive());
			toUpdate.setDefaultDisabled(newDefault.isDefaultDisabled());
			toUpdate.setDefaultLevel1(newDefault.isDefaultLevel1());
			toUpdate.setDefaultLevel2(newDefault.isDefaultLevel2());
			toUpdate.setDefaultLevel3(newDefault.isDefaultLevel3());
			toUpdate.setDefaultLevel4(newDefault.isDefaultLevel4());
			toUpdate.setDefaultLevel5(newDefault.isDefaultLevel5());
			
			if(newDefault.getSecondsRefresh() < 5){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, 
						MessageFormat.format(Messages.getString("ConnectAlertFilterDefaul.LessThanMinRefresh", SmartUtils.getRequestLocale(request)), uuid)); //$NON-NLS-1$
			}
			toUpdate.setSecondsRefresh(newDefault.getSecondsRefresh());

			toUpdate.setStartingZoomLevel(newDefault.getStartingZoomLevel());
			toUpdate.setStartingLong(newDefault.getStartingLong());
			toUpdate.setStartingLat(newDefault.getStartingLat());  
			
			s.update(toUpdate);
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}finally{
		}
		return toUpdate;
    }
	
}
