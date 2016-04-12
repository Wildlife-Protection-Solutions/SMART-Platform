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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.filter.AlertFilter;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.GeoJsonAlert;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * Smart Connect REST API for Alerts and alert types.
 * 'getAllAlerts' returns geojson so it can be drawn directly on maps
 * 
 * @author Jeff
 *
 */


@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectAlert.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectAlert extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectAlert.class.getName());
	
	public static final String PATH = "connectalert"; //$NON-NLS-1$
	
	public static final int MAX_ALERTS_TO_RETURN = 1000; //return an error if the request results in > than this many alerts to return.

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	
	/*
	 * Validates the current user has the permission to the provided action
	 * 1 parameter - the action type to test for permission
	 * 
	 * The CanAccess function automatically returns yes for users that have Admin rights
	 * You can also pass in AdminAccountAction.KEY to this function, even though it is a bit redundant  
	 */
	private void validateUser(String key){
		validateUser(key, null);
	}
	
	private void validateUser(String key, UUID resource){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if(resource == null){ //check if they can see >0 CAs
				if(!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), key)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have alert permissions."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
			}else if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), key, resource)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have permissions for this request."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Get all Alert Types
	 * URL: ../server/api/connectalert/alertTypes/
	 * Call Type: GET
	 * 
	 * @return Returns a list of JSON AlertType objects. ( https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/AlertType.java )
	 */
	@GET
    @Path("/alertTypes/")
    public List<AlertType> getAlertTypes(){
		validateUser(AlertAction.VIEW_ALERTS_KEY);
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return HibernateManager.getAlertTypes(s);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Get a single Alert Type 
	 * URL: ../server/api/connectalert/alertTypes/{uuid}
	 * Call Type: GET
	 * 
	 * @param	uuid	provided in the URL, the uuid of the type.
	 * @return Returns a JSON representation of an AlertType object for the created user 
	 */
	@GET
    @Path("/alertTypes/{uuid}")
    public AlertType getAlertType(@PathParam("uuid") UUID uuid){
		validateUser(AlertAction.VIEW_ALERTS_KEY);

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			AlertType a = HibernateManager.getAlertType(s, uuid);
			if (a == null){
				logger.info("Alert ID: " + uuid + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			return a;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	
	/**
	 * Update an Alert Type
	 * URL: ../server/api/connectalert/alertTypes/{uuid}
	 * Call Type: PUT
	 * Payload: A JSON object of attributes that match the Java attributes, EX:
	 * 		{"uuid":"d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52","key":"intelligence","label":"Intelligence","color":"#1929FF","opacity":".99","markerIcon":"birthday-cake","markerColor":"red","spin":false} 
	 * 		Only attributes you want to change need to be included.
	 *  
	 * @param	uuid	provided in the URL, the uuid of the alert type you are updating.
	 * @return Returns a JSON representation of an AlertType object for the created user 
	 */
	@PUT
    @Path("/alertTypes/{uuid}")
    public AlertType updateAlertType(@PathParam("uuid") UUID uuid, AlertType newAlertType) {
    	validateUser(AdminAccountAction.KEY);
    	
    	AlertType toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (AlertType)s.createCriteria(AlertType.class)
					.add(Restrictions.eq("uuid", uuid)) //$NON-NLS-1$
					.uniqueResult();
			
			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertTypeNotFound", SmartUtils.getRequestLocale(request)), uuid)); //$NON-NLS-1$
			}
			
			if (newAlertType.getLabel() != null){
				toUpdate.setLabel(newAlertType.getLabel());
			}
			if (newAlertType.getColor() != null){
				toUpdate.setColor(newAlertType.getColor());
			}
//			if (newAlertType.getFillColor() != null){
//				toUpdate.setFillColor(newAlertType.getFillColor());
//			}
			if (newAlertType.getOpacity() != null){
				toUpdate.setOpacity(newAlertType.getOpacity());
			}
			if (newAlertType.getMarkerColor() != null){
				toUpdate.setMarkerColor(newAlertType.getMarkerColor());
			}
			if (newAlertType.getMarkerIcon()!= null){
				toUpdate.setMarkerIcon(newAlertType.getMarkerIcon());
			}
			toUpdate.setSpin(newAlertType.getSpin());
			
			
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
	 * Create a new Alert Type
	 * URL: ../server/api/connectalert/alertTypes/{label}
	 * Call Type: POST
	 * Payload: A JSON object of attributes that match the Java attributes, EX:
	 * 		{"label":"New Type Name","color":"5AFF54","opacity":".80","markerIcon":"cloud","markerColor":"blue","spin":"false"}
	 * 
	 * @param	label	provided in the URL, the label/name of the new type (the system automatically creates a uuid)
	 * @return Returns a JSON representation of the new AlertType object created 
	 */
	@POST
    @Path("/alertTypes/{label}")
    public AlertType addAlertType(@PathParam("label") String label, AlertType newAlertType) {
		validateUser(AdminAccountAction.KEY);
			
		AlertType a = new AlertType();

		a.setLabel(newAlertType.getLabel());
		a.setColor(newAlertType.getColor());
//		a.setFillColor(newAlertType.getFillColor());
		a.setOpacity(newAlertType.getOpacity());
		a.setMarkerColor(newAlertType.getMarkerColor());
		a.setMarkerIcon(newAlertType.getMarkerIcon());
		a.setSpin(newAlertType.getSpin());
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(a);
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
		
		return a;
	}
	
	
	/**
	 * Delete an alert type
	 * URL: ../server/api/connectalert/alertTypes/{uuid}
	 * Call Type: DELETE
	 * 
	 * @param	uuid	provided in the URL, the uuid of the alert type to be delete.
	 * @return Returns a JSON AlertType object of the deleted type
	 */
	@DELETE
    @Path("/alertTypes/{uuid}")
    public AlertType removeAlertType(@PathParam("uuid") UUID uuid) {
    	validateUser(AdminAccountAction.KEY);
    	AlertType toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getAlertType(s, uuid);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertTypeNotFound", SmartUtils.getRequestLocale(request)), uuid)); //$NON-NLS-1$
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
	
	/*
	 * Main function used by the alert map to take all filters and provide matching filters in GeoJSON so it can be directly drawn on the map.
	 * 
	 * all the parameters are strings because it was easier to send all the values from a HTML form (tick boxes)that way since you can send multiple comma-separated values
	 * they are converted in the AlertFilter Class into proper Lists
	 */
	
	/**
	 * Get a filtered Alert List as GeoJSON 
	 * URL: ../server/api/connectalert/
	 * Call Type: GET
	 * 
	 * Full Example:  https://office.refractions.net:8443/server/api/connectalert/?typeUuidFilter=b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50,d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52,c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51,e0eebc99-9c0b-4ef8-bb6d-91b9bd380a53,&statusFilter=ACTIVE,DISABLED,&levelFilter=1,2,5,&caUuidFilter=8f7fbe1b-201a-4ef4-bda8-14f5581e65ce,2a304b75-5b83-4d0a-83cd-52d0b4742c14,&textSearchFilter=&startDateFilter=1455401895908&endDateFilter=1458076695908&sortBy=userGeneratedId&sortAscending=true&maxAlertOverride=1000&
	 * 
	 * @param levelFilter	A comma separated list of which levels to include, ex: levelFilter=1,2,5  
	 * @param typeUuidFilter	A comma separated list of which types(uuids) to include. ex:typeUuidFilter=b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50,d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52, 
	 * @param statusFilter	A comma separated list of which status option to include, ex: statusFilter=ACTIVE,DISABLED 
	 * @param caUuidFilter	A comma separated list of which CAs (uuids) to include, ex: caUuidFilter=8f7fbe1b-201a-4ef4-bda8-14f5581e65ce,2a304b75-5b83-4d0a-83cd-52d0b4742c14
	 * @param startDateFilter	in the form of number of milliseconds since Jan 1, 1970, same as javascript's Date.getTime(), ex: startDateFilter=1455401895908
	 * @param endDateFilter	in the form of number of milliseconds since Jan 1, 1970, same as javascript's Date.getTime() ex: endDateFilter=1458076695908
	 * @param textSearchFilter	leave blanks to return all text results. Otherwise, any text in this filter must appears in the alert name or description, ex: textSearchFilter=abc123
	 * @param sortBy	which column name to sort the data on (date, userGeneratedId, description, level, status, x, y) sortBy=userGeneratedId
	 * @param sortAscending	sort ascending or descending, exs: &sortAscending=true  or   &sortAscending=false 
	 * @param maxAlertOverride	the maximum number of alerts, if there is more than this number, an error response occurs.
	 * 
	 * @return Returns a GeoJSON List of Alerts that meet the filter requirements 
	 */
	@GET
    @Path("")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String getAllAlerts( @QueryParam(value="levelFilter") String levelFilter, 
    			@QueryParam(value="typeUuidFilter") String typeUuidFilter,
    			@QueryParam(value="statusFilter") String statusFilter,    		
    			@QueryParam(value="caUuidFilter") String caUuidFilter, 
    			@QueryParam(value="startDateFilter") String startDateFilter,
    			@QueryParam(value="endDateFilter") String endDateFilter,
    			@QueryParam(value="textSearchFilter") String textSearchFilter,
    			@QueryParam(value="sortBy") String sortBy,
    			@QueryParam(value="sortAscending") Boolean sortAscending,
    			@QueryParam(value="maxAlertOverride") String maxAlertOverride
    			){
		validateUser(AlertAction.VIEW_ALERTS_KEY, null);
		
		AlertFilter af;
		try{
			af = new AlertFilter(levelFilter, typeUuidFilter, statusFilter, 
					caUuidFilter, startDateFilter, endDateFilter, textSearchFilter, 
					sortBy, sortAscending, SmartUtils.getRequestLocale(request));
		}catch (Exception e){
			throw e;
		}
		

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<Alert> list = af.getAlerts(s, request.getUserPrincipal().getName());
			int maxAlerts=MAX_ALERTS_TO_RETURN;
			if(maxAlertOverride != null && Integer.parseInt(maxAlertOverride) > 0){
				maxAlerts = Integer.parseInt(maxAlertOverride) ;	
			}
			if(list.size() > maxAlerts){
				logger.info("Too many alerts match the query. " + list.size() + " > (max)" + maxAlerts ); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.NOT_ACCEPTABLE);
			}
			return convertToGeoJson(s, list).toString();
		} catch (NumberFormatException e) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST + Messages.getString("ConnectAlert.InvalidMaxAlerts",SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}catch (Exception e){
			throw e;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Get a single Alert
	 * URL: ../server/api/connectalert/{alertUuid}
	 * Call Type: GET
	 * 
	 * @param	alertUuid	provided in the URL, the UUID of the alert you want.
	 * @return Returns a JSON representation of an Alert object ( https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/Alert.java )
	 */
	@GET
    @Path("/{alertUuid}")
    public Alert getAlert(@PathParam("alertUuid") UUID alertUuid){
		validateUser(AlertAction.VIEW_ALERTS_KEY);

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			Alert a = HibernateManager.getAlert(s, alertUuid);
			if (a == null){
				logger.info("Alert ID: " + alertUuid + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			
			if(SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AlertAction.VIEW_ALERTS_KEY, a.getCaUuid()) ){
						return a;
			}else{
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Gets a list of all alerts for a single CA
	 * URL: ../server/api/connectalert/ca/{caUuid}
	 * Call Type: GET
	 * 
	 * @param	caUuid	provided in the URL, the UUID of the CA.
	 * @return Returns a JSON list of Alert objects in the specified CA 
	 */
	@GET
    @Path("/ca/{caUuid}")
    public List<Alert> getAlertsByCa(@PathParam("caUuid") UUID caUuid){
		validateUser(AlertAction.VIEW_ALERTS_KEY, caUuid);
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<Alert> a = HibernateManager.getAlertsByCa(s, caUuid);
			if (a == null){
				logger.info("Not alerts found for CA ID: " + caUuid ); //$NON-NLS-1$
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			return a;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Create a new Alert
	 * URL: ../server/api/connectalert/{usergenid}
	 * Call Type: POST
	 * Payload: A GeoJSON object that has properties that match the Java attributes of an Alert, EX:{"type":"FeatureCollection",
	 * 		"features":[{"type":"Feature",
	 * 		"geometry":{"type":"Point","coordinates":["-123.36296859999997","48.4307441"]},
	 * 		"properties":{"deviceId":"0","id":"0","latitude":0,"longitude":0,"altitude":0,"accuracy":0,
	 * 			"caUuid":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce",
	 * 			"level":"1",
	 * 			"description":"",
	 * 			"typeUuid":"b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50",
	 * 			"sighting":{}
	 * 		}}]}
	 *
	 * you can also include a "date", but typically just leave it blank and the server generates the time of creation automatically
	 * 
	 *  Note on Tracks / Updates: If you call this API with the same usergenid more than once, the systems adds the past x,y coordinates 
	 *  to a historical "track" of sorts and overwrites the other attributes with the latest data. This is the way users can send a repetative
	 *  "ping" to keep a last-known location and past track of devices without creating a new alert everytime the location is updated. 
	 * 
	 * @param	usergenid	provided in the URL, the user generated ID of the alert. The system generates a UUID automatically.
	 * @return Returns a JSON Alert object for the created alert 
	 */
	@POST
    @Path("/{usergenid}")
    public Alert addAlert(@PathParam("usergenid") String userGenId, GeoJsonAlert newAlert) {
		boolean error = validateAlertValues(newAlert);
		
		validateUser(AlertAction.CREATE_ALERTS_KEY, newAlert.getCaUuid());
		
		Alert a = new Alert();
		
		a.setUserGeneratedId(userGenId);

		//default to now if no date given 
		if(newAlert.getDateTime() == null){
			a.setDate(new Date());
		}else{
			a.setDate(newAlert.getDateTime());
		}
		
		//default to Active for new alerts 
		a.setStatus(AlertStatusEnum.ACTIVE);
		a.setDescription(newAlert.getDescription());
		a.setLevel(newAlert.getLevel());

		a.setUserGeneratedId(userGenId);
		a.setX(newAlert.getLongitude());
		a.setY(newAlert.getLatitude());
		String track = "[ [" +newAlert.getLongitude() + " , " + newAlert.getLatitude() + "]" + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		a.setTrack(track);
		
		a.setCaUuid(newAlert.getCaUuid());
		
		if(error){
			a.setDescription(newAlert.getDescription() + Messages.getString("ConnectAlert.UnknownAlertTypeDescription",SmartUtils.getRequestLocale(request)) );
			a.setTypeUuid(UUID.fromString("00000000-0000-0000-0000-000000000000"));
		}else{
			a.setTypeUuid(newAlert.getTypeUuid());
		}
	
		a.setCreatorUuid(getCreatorUuid());
		
		//validate usergenid, is it unique? If so, update the existing one and return instead of saving a new one.
		String err = validateGeneratedId(userGenId);
		if (err != null){
			//alert already exists, try updating the existing one. Since Cybertracker can't track whether it is the first or second+
			//time they send off alerts, we can't enforce them to use our Update API...
			a.setTrack(null); //We don't want the default track that was created above if this is actually an update.
			updateAndEditAlert(userGenId, a, true);
			return a;
		}
		
		//confirmed this is a new request, save it.
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(a);
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
		
		return a;
	}

	/* This updatewon't transfer the existing X,Y into the past track
	 * Users like cybertracker can us the AddAlerts POST with an existing 
	 * userGenId to update the X,Y while keeping the old one in the track
	 * 
	 * Setup this way since users won't always know whether they sent a create request yet, 
	 * and if they sent two very quickly no matter what order they will both work where an update received first would fail. 
	 */
	
	/**
	 * Edit Alert details
	 * URL: ../server/api/connectalert/{usergenid}
	 * Call Type: PUT
	 * Payload: A JSON object of attributes that match the Java attributes you wish to update, EX:
	 * 		{"caUuid":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce","typeUuid":"b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50","level":"1","status":"DISABLED","x":"-123.362","y":"48.4","track":"[ [-123.36296859999997 , 48.4307441]]","description":""}
	 * 
	 * attributes that are not going to be updated can be left out entirely if desired.
	 * 
	 * @param	usergenid	provided in the URL, the user generated id of the alert.
	 * @return Returns a JSON Alert object for the updated alert 
	 */
	@PUT
    @Path("/{usergenid}")
    public Alert editAlert(@PathParam("usergenid") String oldAlertId, Alert newAlert) {
		return updateAndEditAlert( oldAlertId,  newAlert, false);
	}

 
	/**
	 * Delete an Alert
	 * URL: ../server/api/connectalert/{alertUuid}
	 * Call Type: DELETE
	 * 
	 * @param	alertUuid	provided in the URL, the UUID of the alert to delete.
	 * @return Returns a JSON Alert object for the deleted user 
	 */
    @DELETE
    @Path("/{alertUuid}")
    public Alert removeAlert(@PathParam("alertUuid") UUID alertUuid) {

    	Alert toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getAlert(s, alertUuid);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertNotFound", SmartUtils.getRequestLocale(request)), alertUuid)); //$NON-NLS-1$
			}
			
			if(!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AlertAction.DELETE_ALERTS_KEY, toDelete.getCaUuid())){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
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
 
    private String validateAlertType(UUID typeUuid) {
		Session s = HibernateManager.getSession(context);
		AlertType at = new AlertType();
		s.beginTransaction();
		try{
	    	at = HibernateManager.getAlertType(s, typeUuid);
		}finally{
			s.getTransaction().commit();
		}

    	if(at == null){
    		return Messages.getString("ConnectAlert.AlertTypeNotFound1", SmartUtils.getRequestLocale(request)); //$NON-NLS-1$
    	}
		return null;
	}
    
    private String validateGeneratedId(String userGenId) {
    	Session s = HibernateManager.getSession(context);
		Alert a = null;
		s.beginTransaction();
		try{
	    	a = HibernateManager.getAlertByUserId(s, userGenId);
		}finally{
			s.getTransaction().commit();
		}

    	if(a != null){
    		return Messages.getString("ConnectAlert.AlertExists", SmartUtils.getRequestLocale(request)); //$NON-NLS-1$
    	}
		return null; 
	}
    
    private String validateCa(UUID caUuid) {
    	Session s = HibernateManager.getSession(context);
		ConservationAreaInfo ca = null;
		s.beginTransaction();
		try{
	    	ca = HibernateManager.getConservationAreaInfo(s, caUuid);
		}finally{
			s.getTransaction().commit();
		}

    	if(ca == null){
    		return Messages.getString("ConnectAlert.InvalidCa", SmartUtils.getRequestLocale(request)); //$NON-NLS-1$
    	}
		return null;
	}
    private UUID getCreatorUuid() {
    	SmartUser user = null;
    	
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
		}finally{
			s.getTransaction().commit();
		}
		
		return user.getUuid();
	}

    private boolean validateAlertValues(GeoJsonAlert newAlert) {
    	Alert a = new Alert();
    	a.setCaUuid(newAlert.getCaUuid());
    	a.setX(newAlert.getLongitude());
    	a.setY(newAlert.getLatitude());
    	a.setTypeUuid(newAlert.getTypeUuid());
    	a.setLevel(newAlert.getLevel());
    	return validateAlertValues(a); 
    }
    
    private boolean validateAlertValues(Alert newAlert) {
		//validate type
		String err = validateAlertType(newAlert.getTypeUuid());
		if (err != null){
			//we don't want to throw away alerts that might have old/deleted/invalid types, we want to show them somehow still, with errors noted.
			return true;
		}
		
		//validate alert CA UUID
		err = validateCa(newAlert.getCaUuid());
		if (err != null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
		}
		
		//validate x,y are valid Lat-Long values
		if (newAlert.getX() > 180 || newAlert.getX() < -180 || newAlert.getY() > 90 || newAlert.getY() < -90 || newAlert.getY() == null || newAlert.getX() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, MessageFormat.format(Messages.getString("ConnectAlert.InvalidLatLon", SmartUtils.getRequestLocale(request)), newAlert.getX(), newAlert.getY()) ); //$NON-NLS-1$
		}
		
		//validate Level
		if (newAlert.getLevel() == null || newAlert.getLevel() < -32768 || newAlert.getLevel() > 32767){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, MessageFormat.format(Messages.getString("ConnectAlert.InvalidLevel", SmartUtils.getRequestLocale(request)), newAlert.getLevel())); //$NON-NLS-1$
		}
		return false;
	}
    
    private JSONObject convertToGeoJson(Session s , List<Alert> list) throws HibernateException{
    	 JSONObject featureCollection = new JSONObject();
    	    try {
    	        featureCollection.put("type", "FeatureCollection"); //$NON-NLS-1$ //$NON-NLS-2$
    	        JSONArray featureList = new JSONArray();

    	        for (Alert obj : list) {
    	            // {"geometry": {"type": "Point", "coordinates": [-94.149, 36.33]}
    	            JSONObject point = new JSONObject();
    	            point.put("type", "Point"); //$NON-NLS-1$ //$NON-NLS-2$
    	            // construct a JSONArray from a string; can also use an array or list
    	            JSONArray coord = new JSONArray("["+obj.getX()+","+obj.getY()+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    	            point.put("coordinates", coord); //$NON-NLS-1$
    	            JSONObject feature = new JSONObject();
    	            feature.put("geometry", point); //$NON-NLS-1$
    	            
    	            JSONObject properties = new JSONObject();
    	            properties.put("uuid", obj.getUuid()); //$NON-NLS-1$
    	            properties.put("cauuid", obj.getCaUuid()); //$NON-NLS-1$
    	            properties.put("creatoruuid", obj.getCreatorUuid()); //$NON-NLS-1$
    	            properties.put("date", obj.getDate()); //$NON-NLS-1$
    	            properties.put("desc", obj.getDescription()); //$NON-NLS-1$
    	            properties.put("level", obj.getLevel()); //$NON-NLS-1$
    	            properties.put("status", obj.getStatus().getGuiName(SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
    	            properties.put("typeuuid", obj.getTypeUuid()); //$NON-NLS-1$

    	            AlertType type = HibernateManager.getAlertTypeIncludeUnknown(s, obj.getTypeUuid());
    	    		properties.put("type", type.getLabel()); //$NON-NLS-1$
    	            
    	            properties.put("id", obj.getUserGeneratedId()); //$NON-NLS-1$
    	            properties.put("x", obj.getX()); //$NON-NLS-1$
    	            properties.put("y", obj.getY()); //$NON-NLS-1$

    	            feature.put("properties", properties); //$NON-NLS-1$
    	            feature.put("type", "Feature"); //$NON-NLS-1$ //$NON-NLS-2$
    	            featureList.put(feature);
    	            
    	            //the Track feature
    	            JSONObject propertiesTrack = new JSONObject();
    	            propertiesTrack.put("id", obj.getUserGeneratedId() + "Track"); //$NON-NLS-1$ //$NON-NLS-2$
    	            propertiesTrack.put("typeuuid", obj.getTypeUuid()); //need these to draw the right colors and popups //$NON-NLS-1$
    	            propertiesTrack.put("date", obj.getDate()); //$NON-NLS-1$
    	            propertiesTrack.put("desc", obj.getDescription()); //$NON-NLS-1$
    	            propertiesTrack.put("level", obj.getLevel()); //$NON-NLS-1$
    	            JSONObject line = new JSONObject();
    	            JSONArray a = new JSONArray(obj.getTrack());
    	            line.put("coordinates", a); //$NON-NLS-1$
    	            line.put("type", "LineString"); //$NON-NLS-1$ //$NON-NLS-2$
    	            JSONObject featureTrack = new JSONObject();
    	            featureTrack.put("geometry", line); //$NON-NLS-1$
    	            featureTrack.put("type", "Feature"); //$NON-NLS-1$ //$NON-NLS-2$
    	            featureTrack.put("properties", propertiesTrack); //$NON-NLS-1$
    	            
    	            featureList.put(featureTrack);
    	        }
    	        featureCollection.put("features", featureList); //$NON-NLS-1$
    	        
    	    } catch (JSONException e) {
    	    	throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectAlert.ConvertError", SmartUtils.getRequestLocale(request))+e.toString()); //$NON-NLS-1$
    	    }
    	 return featureCollection;
    }
    
    private Alert updateAndEditAlert(String oldAlertId, Alert newAlert, boolean keepPoint){
    	validateAlertValues(newAlert);
    	validateUser(AlertAction.UPDATE_ALERTS_KEY, newAlert.getCaUuid());
    	
    	Alert toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (Alert)s.createCriteria(Alert.class)
					.add(Restrictions.eq("userGeneratedId", oldAlertId)) //$NON-NLS-1$
					.uniqueResult();
			
			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertNotFound", SmartUtils.getRequestLocale(request)), oldAlertId)); //$NON-NLS-1$
			}
			
			if (newAlert.getUserGeneratedId() != null && !oldAlertId.equals(newAlert.getUserGeneratedId())){
				//ensure new usergenID is unique
				Alert existingAlert = HibernateManager.getAlertByUserId(s,newAlert.getUserGeneratedId());
				if (existingAlert != null){
					throw new SmartConnectException(
							Response.Status.BAD_REQUEST,
							MessageFormat.format(Messages.getString("ConnectAlert.AlertNotUnique", SmartUtils.getRequestLocale(request)), newAlert.getUserGeneratedId())); //$NON-NLS-1$
				}
				toUpdate.setUserGeneratedId(newAlert.getUserGeneratedId());
			}
			
			if (newAlert.getCaUuid() != null){
				toUpdate.setCaUuid(newAlert.getCaUuid());
			}
			if (newAlert.getCreatorUuid() != null){
				toUpdate.setCreatorUuid(newAlert.getCreatorUuid());
			}
			if (newAlert.getDate() != null){
				toUpdate.setDate(newAlert.getDate());
			}
			if (newAlert.getDescription()!= null){
				toUpdate.setDescription(newAlert.getDescription());
			}			
			if (newAlert.getLevel() != null){
				toUpdate.setLevel(newAlert.getLevel());
			}
			if (newAlert.getStatus() != null){
				toUpdate.setStatus(newAlert.getStatus());
			}			
			if (newAlert.getTypeUuid() != null){
				toUpdate.setTypeUuid(newAlert.getTypeUuid());
			}
			
		
			if(keepPoint){//put the existing point onto the end of the track.
				if(newAlert.getX() != null && newAlert.getY() != null){  
					String current = toUpdate.getTrack();
					current = current.substring(0, current.length()-1);
					current = current + ", [" +newAlert.getX() + " , " + newAlert.getY() + "]" + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					toUpdate.setTrack(current);
				}
			}
			if(newAlert.getTrack() != null){
				if(validateTrack(newAlert.getTrack()) ){
					toUpdate.setTrack(newAlert.getTrack());
				}else{
					throw new SmartConnectException("Invalid Track Provided");
				}
			}
			
			if (newAlert.getX() != null){
				toUpdate.setX(newAlert.getX());
			}			
			if (newAlert.getY() != null){
				toUpdate.setY(newAlert.getY());
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

	private boolean validateTrack(String string) {
		string = string.replaceAll("\\s",""); //strip whitespace
		
		if(!string.substring(0,1).equals("[") || !string.substring(string.length() - 1, string.length()).equals("]")){
			return false;
		}
		string = string.substring(1, string.length() - 1); // Get rid of braces.
		String[] parts = string.split(",");
		int x = 0;
		boolean odd = true;
		for (String part : parts) {
			if ( (x & 1) == 0 ){ 
				odd = false;
			}else { 
				odd = true;
			}
			if(odd){
				if(!part.substring(part.length() - 1, part.length()).equals("]")){
					return false;
				}
				part = part.substring(0,part.length()-1);
			}else{ //even, the first character should be a bracket
				if(!part.substring(0,1).equals("[")) {
					return false;
				}
				part = part.substring(1,part.length());
			}
		    try{
		    	Double.parseDouble(part);
		    }catch(NumberFormatException ex){
		    	return false;
		    }
		    x++;
		}
		return true;
	}
}
