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
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * Smart Connect REST API for Alert/operations map saved layers.
 * 
 * Users can add new layers using this api to configure their Connect basemap.
 * The connect web page also uses this api when loading to get all the information
 * for layers it is supposed to draw.
 * 
 * Currently this is setup to have only a single configuration across each Connect instance. 
 * 
 * @author Jeff
 *
 */

@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectMapLayers.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectMapLayers extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectMapLayers.class.getName());
	
	public static final String PATH = "maplayer"; //$NON-NLS-1$

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	private void validateUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have alert management permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
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
				throw new SmartConnectException(Response.Status.NOT_FOUND);
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
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.InvalidType", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (newLayer.getMapboxId().length() > 64){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.MapBoxIdTooLong", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (newLayer.getToken().length() > 256){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.TokenTooLong", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		MapLayer m = new MapLayer();
		
		m.setLayerOrder(newLayer.getLayerOrder());
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
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
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
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConnectMapLayers.MapLayerNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			
			toUpdate.setLayerOrder(newLayer.getLayerOrder());

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

			//validate values
			if(toUpdate.getLayerType() < 0 || toUpdate.getLayerType() > 10){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.InvalidLayerType", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			if(toUpdate.getMapboxId().length() > 64){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.MapBoxIdTooLong", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			if(toUpdate.getToken().length() > 256){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.TokenTooLong", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			if(toUpdate.getLayerName().length() > 32){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.LayerNameTooLong", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}

			
			
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
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("MapLayer.LayerNotFound", SmartUtils.getRequestLocale(request)), layerUuid)); //$NON-NLS-1$
			}
			s.delete(toDelete);
			s.flush();
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
		return toDelete;
    }
    
    }
