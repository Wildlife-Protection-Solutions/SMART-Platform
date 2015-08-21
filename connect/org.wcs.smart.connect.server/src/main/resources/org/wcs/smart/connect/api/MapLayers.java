package org.wcs.smart.connect.api;

import java.net.HttpURLConnection;
import java.text.MessageFormat;
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
import javax.ws.rs.core.MediaType;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;

import com.sun.istack.internal.logging.Logger;

@Path(ConnectRESTApplication.PATH_SEPERATOR + MapLayers.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class MapLayers extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(MapLayers.class);
	
	public static final String PATH = "maplayer"; //$NON-NLS-1$

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	private void validateUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AlertAction.KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have alert management permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@GET
    @Path("")
    public List<MapLayer> getAllLAyers(){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<MapLayer> list = HibernateManager.getMapLayers(s);
			return list;
		}finally{
			s.getTransaction().commit();
		}

	}
	
	@POST
    @Path("/{layerName}")
    public MapLayer addAlert(@PathParam("layerName") String layerName, MapLayer newLayer) {
		validateUser();
			
//		//validate usergenid, is it unique?
//		String err = validateUserGeneratedId(userGenId);
//		if (err != null){
//			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
//		}
//		
//		validateAlertValues(newAlert);
//		
		MapLayer m = new MapLayer();
//
//		//default to now if no date given 
//		if(newAlert.getDate() == null){
//			a.setDate(new Date());
//		}else{
//			a.setDate(newAlert.getDate());
//		}
//		
//		//default to Active for new alerts 
//		if(newAlert.getStatus() == null){
//			a.setStatus(AlertStatusEnum.ACTIVE);
//		}else{
//			a.setStatus(newAlert.getStatus());
//		}
//		
//		a.setDescription(newAlert.getDescription());
//		a.setLevel(newAlert.getLevel());
//
//		a.setUserGeneratedId(userGenId);
//		a.setX(newAlert.getX());
//		a.setY(newAlert.getY());
//		a.setCaUuid(newAlert.getCaUuid());
//		a.setTypeUuid(newAlert.getTypeUuid());
//		
//		a.setCreatorUuid(getCreatorUuid());
//		
//		Session s = HibernateManager.getSession(context);
//		s.beginTransaction();
//		try{
//			s.save(a);
//			s.getTransaction().commit();
//			response.setStatus(Response.Status.CREATED.getStatusCode());
//			response.flushBuffer();
//
//		}catch (SmartConnectException ex){
//			logger.warning(ex.getMessage(), ex);
//			s.getTransaction().rollback();
//			throw ex;
//		}catch (Exception ex){
//			logger.severe(ex.getMessage(), ex);
//			s.getTransaction().rollback();
//			throw new SmartConnectException(ex.getMessage(), ex);
//		}finally{
//			
//		}
		
		return m;
	}
	
	
    @DELETE
    @Path("/{layerUuid}")
    public MapLayer removeUser(@PathParam("layerUuid") UUID layerUuid) {
    	validateUser();
    	MapLayer toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getMapLayer(s, layerUuid);
			if (toDelete == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, 
						MessageFormat.format(Messages.getString("MapLayer.LayerNotFound", SmartUtils.getRequestLocale(request)), layerUuid)); //$NON-NLS-1$
			}
			s.delete(toDelete);
			s.flush();
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.warning(ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.severe(ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}finally{
		}
		return toDelete;
    }
    
    }
