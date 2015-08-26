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
	
	@GET
    @Path("/{layerUuid}")
    public MapLayer getLayer(@PathParam("layerUuid") UUID layerUuid){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			MapLayer l = HibernateManager.getMapLayer(s, layerUuid);
			if (l == null){
				logger.info("Layer ID: " + layerUuid + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
			return l;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@POST
    @Path("/{layerName}")
    public MapLayer addLayer(@PathParam("layerName") String layerName, MapLayer newLayer) {
		validateUser();
			
		//validate
		if (newLayer.getLayerType() < 0 && newLayer.getLayerType() > 2){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid layer type provided.");
		}
		if (newLayer.getMapboxId().length() > 64){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Mapbox ID too long (max 64 chars).");
		}
		if (newLayer.getToken().length() > 256){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Token too long (max 256 chars).");
		}
		
		MapLayer m = new MapLayer();
		
		m.setActive(newLayer.isActive());
		m.setLayerName(newLayer.getLayerName());
		m.setMapboxId(newLayer.getMapboxId());
		
		m.setToken(newLayer.getToken());
		m.setWmsLayerList(newLayer.getWmsLayerList());
		m.setLayerType(newLayer.getLayerType());
				
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(m);
			s.getTransaction().commit();
			response.setStatus(Response.Status.CREATED.getStatusCode());
			response.flushBuffer();

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
		
		return m;
	}
	

	@PUT
    @Path("/{uuid}")
    public MapLayer updateLayer(@PathParam("uuid") UUID oldUuid, MapLayer newLayer) {
    	validateUser();
    	
    	MapLayer toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (MapLayer)s.createCriteria(MapLayer.class)
					.add(Restrictions.eq("uuid", oldUuid)) //$NON-NLS-1$
					.uniqueResult();
			
			if (toUpdate == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, "Could not find Map Layer");
			}
					
			if (newLayer.getLayerType() != null){
				toUpdate.setLayerType(newLayer.getLayerType());
			}
			if (newLayer.getMapboxId() != null){
				toUpdate.setMapboxId(newLayer.getMapboxId());
			}
			if (newLayer.getToken() != null){
				toUpdate.setToken(newLayer.getToken());
			}
			if (newLayer.getWmsLayerList() != null){
				toUpdate.setWmsLayerList(newLayer.getWmsLayerList());
			}			
			toUpdate.setActive(newLayer.isActive());
			toUpdate.setLayerName(newLayer.getLayerName());
			
			s.update(toUpdate);
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
		return toUpdate;
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
