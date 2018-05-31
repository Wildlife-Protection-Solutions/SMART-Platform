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
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * <p>Smart Connect REST API for Alert/operations map saved layers.</p>
 * <p>
 * Users can add new layers using this api to configure their Connect basemap.
 * The connect web page also uses this api when loading to get all the information
 * for layers it is supposed to draw.
 * </p>
 * <p>Currently this is setup to have only a single configuration across each Connect instance.</p> 
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
	
	
	/**
	 * <p>Get All Map Layers</p>
	 * URL: ../server/api/maplayer/<br>
	 * Call Type: GET<br>
	 * 
	 * @return Returns a JSON list of MapLayer objects. (<a href="https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/MapLayer.java">MapLayer</a>) 
	 */
	
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
	
	/**
	 * <p>Get a single Map Layer</p>
	 * URL: ../server/api/maplayer/{layerUuid}<br>
	 * Call Type: GET
	 * 
	 * @param	layerUuid	provided in the URL, the UUID of of the map layer.
	 * @return Returns a JSON representation of the MapLayer object. 
	 */

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
	
	/**
	 * <p>Create new Map Layer</p>
	 * URL: ../server/api/maplayer/{layername}<br>
	 * Call Type: POST<br>
	 * Payload: A JSON object of attributes that match the Java attributes you wish to create/
	 * <pre>{
	 *  "layerOrder":"052",
	 *  "layerName":"newlayername",
	 *  "wmsLayerList":"layer1,layer2",
	 *  "layerType":"WMS",
	 *  "token":"tokenorwmsurl",
	 *  "mapboxId":"",
	 *  "active":"true"
	 *}</pre>
	 * <p>The only supported value for layer type is "WMS"</p>
	 * 
	 * @param	layername	provided in the URL, a name for the created layer
	 * @return Returns a JSON MapLayer object for the created layer.
	 */

	@POST
    @Path("/{layerName}")
    public MapLayer addLayer(@PathParam("layerName") String layerName, MapLayer newLayer) {
		validateUser();
			
		//validate
		if (newLayer.getToken().length() > 256){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectMapLayers.TokenTooLong", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		MapLayer m = new MapLayer();
		
		m.setLayerOrder(newLayer.getLayerOrder());
		m.setActive(newLayer.isActive());
		m.setLayerName(newLayer.getLayerName());
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
	
	/**
	 * <p>Update Map Layers</p>
	 * URL: ../server/api/maplayer/{uuid}<br>
	 * Call Type: PUT<br>
	 * Payload: A JSON object of attributes that match the Java attributes you wish to update<br>
	 * <pre>{
	 *   "layerOrder":"51",
	 *   "layerName":"newlayername2",
	 *   "wmsLayerList":"layer1,layer2,layer3",
	 *   "layerType":"WMS",
	 *   "token":"tokenorwmsurl",
	 *   "mapboxId":"",
	 *   "active":"true"
	 *}</pre> 		
	 * 
	 * <p>Attributes that are not going to be updated can be left out entirely if desired.</p>
	 * 
	 * @param	uuid	provided in the URL, the uuid id of the layer to update.
	 * @return Returns a JSON MapLayer object for the updated layer 
	 */

	@PUT
    @Path("/{uuid}")
    public MapLayer updateLayer(@PathParam("uuid") UUID oldUuid, MapLayer newLayer) {
    	validateUser();
    	
    	MapLayer toUpdate = null;
    	Session s = HibernateManager.getSession(context);
    	s.beginTransaction();
		try{
			toUpdate =s.get(MapLayer.class, oldUuid);
			
			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConnectMapLayers.MapLayerNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			toUpdate.setLayerOrder(newLayer.getLayerOrder());

			if (newLayer.getLayerType() != null){
				toUpdate.setLayerType(newLayer.getLayerType());
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
	
	/**
	 * <p>Delete a Map Layers</p>
	 * URL: ../server/api/maplayer/{uuid}<br>
	 * Call Type: DELETE
	 * 
	 * @param	uuid	provided in the URL, the uuid of the layer to delete.
	 * @return Returns a JSON MapLayer object for the deleted layer
	 */

    @DELETE
    @Path("/{uuid}")
    public MapLayer removeUser(@PathParam("uuid") UUID uuid) {
    	validateUser();
    	MapLayer toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getMapLayer(s, uuid);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("MapLayer.LayerNotFound", SmartUtils.getRequestLocale(request)), uuid)); //$NON-NLS-1$
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
