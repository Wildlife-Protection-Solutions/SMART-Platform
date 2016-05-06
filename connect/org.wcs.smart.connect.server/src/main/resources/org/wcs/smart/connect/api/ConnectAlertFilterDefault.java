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
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;

/**
 * Smart Connect REST API for default filters used on the alert/operations map.
 * allows users to get and set what the defaults are, only one default is allowed for  
 * all users and cas across a single Connect instance, although the database is designed to allow for more eventually.
 * 
 * @author Jeff
 *
 */

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

	/*
	 * Validates the current user has the permission to the provided action
	 * 1 parameter - the action type to test for permission
	 * 
	 * The CanAccess function automatically returns yes for users that have Admin rights
	 * You can also pass in AdminAccountAction.KEY to this function, even though it is a bit redundant  
	 */
	private void validateUser(String key, UUID resource){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if(resource == null){ //check if they can see >0 CAs
				if(!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), key)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have alert management permissions."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
			}else if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), key, resource)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have alert management permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	private void validateUser(String key){
		validateUser(key, null);
	}
	
	
	/**
	 * Get the default filters for showing alerts 
	 * URL: ../server/api/connectalertfilterdefault/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON list of AlertFilterDefault objects, there is only ever 1 object in the list currently. (https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/AlertFilterDefault.java) 
	 */
	@GET
    @Path("")
    public List<AlertFilterDefault> getAlertFilterDefaults(){
		validateUser(AlertAction.VIEW_ALERTS_KEY, null);
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return HibernateManager.getAlertFilterDefaults(s);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * update default filters for showing alerts 
	 * URL: ../server/api/connectalertfilterdefault/{uuid}
	 * Call Type: PUT
	 * Payload: A JSON object of attributes that match the Java attributes you wish to update, EX:
	 * 		{"defaultPastHours":"744","defaultTypeUuids":"b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50,d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52,c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51,e0eebc99-9c0b-4ef8-bb6d-91b9bd380a53,","defaultActive":true,"defaultDisabled":false,"defaultLevel1":true,"defaultLevel2":true,"defaultLevel3":true,"defaultLevel4":true,"defaultLevel5":true,"defaultCaUuids":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce,2a304b75-5b83-4d0a-83cd-52d0b4742c14,00000000-0000-0000-0000-000000000000,fb5938b9-3ecd-4972-819e-867ee42623bb,2c5dbf89-ee89-473e-93fc-cc816205dba7,","defaultText":"","secondsRefresh":"30","startingZoomLevel":"8","startingLong":"-123","startingLat":"48"}
	 * 
	 * attributes that are not going to be updated can be left out entirely if desired.
	 * set defaultPastHours=-99 to select 'all dates' as the default
	 * 
	 * @param	uuid	provided in the URL, the uuid of the alert defaults to update.
	 * @return Returns a JSON AlertFilterDefault object for the updated alert defaults
	 */
	@PUT
    @Path("/{uuid}")
    public AlertFilterDefault updateAlertFilterDefault(@PathParam("uuid") UUID uuid, AlertFilterDefault newDefault) {
    	validateUser(AdminAccountAction.KEY);
    	
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
			
			setDefaultValues(newDefault, toUpdate, s);
			
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
	 * create default filters for the first time 
	 * URL: ../server/api/connectalertfilterdefault/
	 * Call Type: PUT
	 * Payload: A JSON object of attributes that match the Java attributes you wish to update, EX:
	 * 		{"defaultPastHours":"744","defaultTypeUuids":"b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a50,d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a52,c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a51,e0eebc99-9c0b-4ef8-bb6d-91b9bd380a53,","defaultActive":true,"defaultDisabled":false,"defaultLevel1":true,"defaultLevel2":true,"defaultLevel3":true,"defaultLevel4":true,"defaultLevel5":true,"defaultCaUuids":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce,2a304b75-5b83-4d0a-83cd-52d0b4742c14,00000000-0000-0000-0000-000000000000,fb5938b9-3ecd-4972-819e-867ee42623bb,2c5dbf89-ee89-473e-93fc-cc816205dba7,","defaultText":"","secondsRefresh":"30","startingZoomLevel":"8","startingLong":"-123","startingLat":"48"}
	 * 
	 * @return Returns a JSON AlertFilterDefault object for the updated alert defaults
	 */
	@PUT
    @Path("/")
    public AlertFilterDefault updateAlertFilterDefault(AlertFilterDefault newDefault) {
    	validateUser(AdminAccountAction.KEY);
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		AlertFilterDefault toUpdate = new AlertFilterDefault();
		try{
			setDefaultValues(newDefault, toUpdate, s);
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
	
	private void setDefaultValues(AlertFilterDefault newDefault, AlertFilterDefault toUpdate, Session s) {
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
					Messages.getString("ConnectAlertFilterDefaul.LessThanMinRefresh", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		toUpdate.setSecondsRefresh(newDefault.getSecondsRefresh());

		toUpdate.setStartingZoomLevel(newDefault.getStartingZoomLevel());
		toUpdate.setStartingLong(newDefault.getStartingLong());
		toUpdate.setStartingLat(newDefault.getStartingLat());  
		
		s.saveOrUpdate(toUpdate);
		s.getTransaction().commit();
	}
	
}
