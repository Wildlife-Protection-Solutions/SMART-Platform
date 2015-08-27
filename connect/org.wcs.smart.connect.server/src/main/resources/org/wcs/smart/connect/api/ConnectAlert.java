package org.wcs.smart.connect.api;

import java.net.HttpURLConnection;
import java.text.MessageFormat;
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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;

import com.sun.istack.internal.logging.Logger;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
import org.json.*;

@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectAlert.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectAlert extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectAlert.class);
	
	public static final String PATH = "connectalert"; //$NON-NLS-1$

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
    @Path("/alertTypes/")
    public List<AlertType> getAlertTypes(){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return HibernateManager.getAlertTypes(s);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@GET
    @Path("")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String getAllAlerts(){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<Alert> list = HibernateManager.getAlerts(s);
			
			return convertToGeoJson(s, list).toString();
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@GET
    @Path("/{alertUuid}")
    public Alert getAlert(@PathParam("alertUuid") UUID alertUuid){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			Alert a = HibernateManager.getAlert(s, alertUuid);
			if (a == null){
				logger.info("Alert ID: " + alertUuid + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
			return a;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	
	@GET
    @Path("/ca/{caUuid}")
    public List<Alert> getAlertsByCa(@PathParam("caUuid") UUID caUuid){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<Alert> a = HibernateManager.getAlertsByCa(s, caUuid);
			if (a == null){
				logger.info("Not alerts found for CA ID: " + caUuid ); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
			return a;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	
	@POST
    @Path("/{usergenid}")
    public Alert addAlert(@PathParam("usergenid") String userGenId, Alert newAlert) {
		validateUser();
			
		//validate usergenid, is it unique?
		String err = validateUserGeneratedId(userGenId);
		if (err != null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
		}
		
		validateAlertValues(newAlert);
		
		Alert a = new Alert();

		//default to now if no date given 
		if(newAlert.getDate() == null){
			a.setDate(new Date());
		}else{
			a.setDate(newAlert.getDate());
		}
		
		//default to Active for new alerts 
		if(newAlert.getStatus() == null){
			a.setStatus(AlertStatusEnum.ACTIVE);
		}else{
			a.setStatus(newAlert.getStatus());
		}
		
		a.setDescription(newAlert.getDescription());
		a.setLevel(newAlert.getLevel());

		a.setUserGeneratedId(userGenId);
		a.setX(newAlert.getX());
		a.setY(newAlert.getY());
		a.setCaUuid(newAlert.getCaUuid());
		a.setTypeUuid(newAlert.getTypeUuid());
		
		a.setCreatorUuid(getCreatorUuid());
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(a);
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
		
		return a;
	}
 

	@PUT
    @Path("/{usergenid}")
    public Alert updateAlert(@PathParam("usergenid") String oldAlertId, Alert newAlert) {
    	validateUser();
    	validateAlertValues(newAlert);
    	
    	Alert toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (Alert)s.createCriteria(Alert.class)
					.add(Restrictions.eq("userGeneratedId", oldAlertId)) //$NON-NLS-1$
					.uniqueResult();
			
			if (toUpdate == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertNotFound", SmartUtils.getRequestLocale(request)), oldAlertId)); //$NON-NLS-1$
			}
			
			if (newAlert.getUserGeneratedId() != null && !oldAlertId.equals(newAlert.getUserGeneratedId())){
				//ensure new usergenID is unique
				Alert existingAlert = HibernateManager.getAlertByUserId(s,newAlert.getUserGeneratedId());
				if (existingAlert != null){
					throw new SmartConnectException(
							HttpURLConnection.HTTP_BAD_REQUEST,
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
			if (newAlert.getX() != null){
				toUpdate.setX(newAlert.getX());
			}			
			if (newAlert.getY() != null){
				toUpdate.setY(newAlert.getY());
			}
			
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
    @Path("/{alertUuid}")
    public Alert removeUser(@PathParam("alertUuid") UUID alertUuid) {
    	validateUser();
    	Alert toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getAlert(s, alertUuid);
			if (toDelete == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectAlert.AlertNotFound", SmartUtils.getRequestLocale(request)), alertUuid)); //$NON-NLS-1$
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
    		return "Alert Type Not Found";
    	}
		return null;
	}
    
    private String validateUserGeneratedId(String userGenId) {
    	Session s = HibernateManager.getSession(context);
		Alert a = new Alert();
		s.beginTransaction();
		try{
	    	a = HibernateManager.getAlertByUserId(s, userGenId);
		}finally{
			s.getTransaction().commit();
		}

    	if(a != null){
    		return "Alert with this User Generated ID already exist, cannot create duplicate";
    	}
		return null; 
	}
    
    private String validateCa(UUID caUuid) {
    	Session s = HibernateManager.getSession(context);
		ConservationAreaInfo ca = new ConservationAreaInfo();
		s.beginTransaction();
		try{
	    	ca = HibernateManager.getConservationAreaInfo(s, caUuid);
		}finally{
			s.getTransaction().commit();
		}

    	if(ca == null){
    		return "Not a valid Conservation Area ID";
    	}
		return null;
	}
    private UUID getCreatorUuid() {
    	SmartUser user;
    	
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
		}finally{
			s.getTransaction().commit();
		}
		
		return user.getUuid();
	}

    private void validateAlertValues(Alert newAlert) {
		//validate type
		String err = validateAlertType(newAlert.getTypeUuid());
		if (err != null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
		}
		
		//validate alert CA UUID
		err = validateCa(newAlert.getCaUuid());
		if (err != null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
		}
		
		//validate x,y are valid Lat-Long values
		if (newAlert.getX() > 180 || newAlert.getX() < -180 || newAlert.getY() > 90 || newAlert.getY() < -90 || newAlert.getY() == null || newAlert.getX() == null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid Longitude or Latitude :" + newAlert.getX() + "," + newAlert.getY() );
		}
		
		//validate Level
		if (newAlert.getLevel() == null || newAlert.getLevel() < -32768 || newAlert.getLevel() > 32767){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "Invalid Level (must be a an integer between -32768 and 32767):" + newAlert.getLevel());
		}
		
	}
    
    private JSONObject convertToGeoJson(Session s , List<Alert> list) throws HibernateException{
    	 JSONObject featureCollection = new JSONObject();
    	    try {
    	        featureCollection.put("type", "FeatureCollection");
    	        JSONArray featureList = new JSONArray();

    	        for (Alert obj : list) {
    	            // {"geometry": {"type": "Point", "coordinates": [-94.149, 36.33]}
    	            JSONObject point = new JSONObject();
    	            point.put("type", "Point");
    	            // construct a JSONArray from a string; can also use an array or list
    	            JSONArray coord = new JSONArray("["+obj.getX()+","+obj.getY()+"]");
    	            point.put("coordinates", coord);
    	            JSONObject feature = new JSONObject();
    	            feature.put("geometry", point);
    	            
    	            JSONObject properties = new JSONObject();
    	            properties.put("uuid", obj.getUuid());
    	            properties.put("cauuid", obj.getCaUuid());
    	            properties.put("creatoruuid", obj.getCreatorUuid());
    	            properties.put("date", obj.getDate());
    	            properties.put("desc", obj.getDescription());
    	            properties.put("level", obj.getLevel());
    	            properties.put("status", obj.getStatus());
    	            properties.put("typeuuid", obj.getTypeUuid());

    	            AlertType type = HibernateManager.getAlertType(s, obj.getTypeUuid());
    	    		properties.put("type", type.getLabel());
    	            
    	            properties.put("id", obj.getUserGeneratedId());
    	            properties.put("x", obj.getX());
    	            properties.put("y", obj.getY());

    	            feature.put("properties", properties);
    	            featureList.put(feature);
    	            feature.put("type", "Feature");
    	            featureCollection.put("features", featureList);
    	        }
    	        
    	    } catch (JSONException e) {
    	    	throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, "can't save json object: "+e.toString());
    	    }
    	 return featureCollection;
    }
}
