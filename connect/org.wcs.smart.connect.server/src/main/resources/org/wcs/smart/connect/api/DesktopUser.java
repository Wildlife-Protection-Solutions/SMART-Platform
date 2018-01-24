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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.apache.BcryptCredentialHandler;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.EmployeeInfo;
import org.wcs.smart.connect.model.SimpleConservationAreaList;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;


/**
 * SMART Connect REST API for Desktop Users/Employees and managing them via the Connect interface.
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + DesktopUser.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class DesktopUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(DesktopUser.class.getName());
	
	public static final String PATH = "desktopuser"; //$NON-NLS-1$

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	private void isAdminUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have user accounts permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	private boolean isCaAdminUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return SecurityManager.INSTANCE.isCaAdmin(s, request.getUserPrincipal().getName());
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Get all Desktop Users accounts.
	 * <p>URL: ../server/api/desktopuser/
	 * <p>Call Type: GET
	 * 
	 * @return Returns a JSON Array of Employee objects for all users
	 * 
	 */
	@GET
    @Path("")
    public List<EmployeeInfo> getUsers(){
		ArrayList<UUID> uuidCaAdminIn = null; 
		
		boolean isCaAdmin = isCaAdminUser();
		if(!isCaAdmin){
			isAdminUser();//throws an exception if invalid user.
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ArrayList<EmployeeInfo> authorizedEmployees = new ArrayList<EmployeeInfo>(); 
			ArrayList<EmployeeInfo> allEmployees = (ArrayList<EmployeeInfo>) HibernateManager.getDesktopUsers(s);
		
			if(isCaAdmin){
				uuidCaAdminIn = SecurityManager.INSTANCE.listOfUuidsIsCaAdminOf(s, request.getUserPrincipal().getName() );
				for(EmployeeInfo e: allEmployees){
					if(uuidCaAdminIn.contains(e.getCaUuid())){
						authorizedEmployees.add(e);
					}
				}
			}else{//admin return all of them
				return allEmployees;
			}
			return authorizedEmployees;
		}catch(Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.BAD_REQUEST, ex); 	
		}finally{
			s.getTransaction().commit();
		}
	}

	
	
	@GET
    @Path("/iscurrentuseradmin/")
    public boolean isCurrentUserAnAdmin(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Gets the current user's details
	 * <p>URL: ../server/api/desktopuser/getCurrent/
	 * <p>Call Type: GET
	 * 
	 * @return Returns a JSON SmartUser object of the currently logged in user 
	 */
	
	@GET
    @Path("/getCurrent/")
    public SmartUser getCurrentUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		String username = request.getUserPrincipal().getName();
		try{
			SmartUser su = HibernateManager.getUser(s, username);
			if (su == null){
				logger.info("User " + username + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			return su;
		}finally{
			s.getTransaction().commit();
		}
	}

	/**
	 * Gets the details of a single from a particular CA, including a list of allother CAs this user also exists in (this list is restricted if the requesting user is only a CaAdmin, to the CAs they can adminsitrate)
	 * <p>URL: ../server/api/desktopuser/{username}
	 * <p>Call Type: GET
	 * 
	 * @return Returns a JSON EmployeeInfo object of the requested user 
	 */
	
	@GET
    @Path("/{username}")
    public EmployeeInfo getUserInCa(@PathParam("username") String username, @QueryParam("cauuid") String cauuid){
		boolean isCaAdmin = isCaAdminUser();
		if(!isCaAdmin){
			isAdminUser();//throws an exception if invalid user.
		}
		
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			Employee e = HibernateManager.getDesktopUserInCa(s, username, cauuid);
			//check if this is a caAdmin they are allowed to do stuff in this CA
			if(isCaAdmin){
				ArrayList<UUID> uuidCaAdminIn = SecurityManager.INSTANCE.listOfUuidsIsCaAdminOf(s, request.getUserPrincipal().getName() );
				if(!uuidCaAdminIn.contains(e.getConservationArea().getUuid())){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have access to this CA."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
			}
			
			EmployeeInfo i = new EmployeeInfo();
			i.setCaUuid(e.getConservationArea().getUuid());
			i.setSmartUserId(e.getSmartUserId());
			i.setFamilyName(e.getFamilyName());
			i.setGivenName(e.getGivenName());
			i.setGender(e.getGender());
			i.setId(e.getId());
			i.setUserLevelKey(e.getSmartUserLevelKeys());
			i.setCaLabel(e.getConservationArea().getName());

			ArrayList<SimpleConservationAreaList> authorizedCas = new ArrayList<SimpleConservationAreaList>();
			ArrayList<SimpleConservationAreaList> allCas = HibernateManager.getDesktopUserAllCas(s, username);
			if(isCaAdmin){
				ArrayList<UUID> uuidCaAdminIn = SecurityManager.INSTANCE.listOfUuidsIsCaAdminOf(s, request.getUserPrincipal().getName() );
				for(SimpleConservationAreaList a: allCas){
					if(uuidCaAdminIn.contains(a.getUuid())){
						authorizedCas.add(a);
					}
				}			
			}else{
				authorizedCas = allCas;
			}
			i.setAllCasUserIsIn(authorizedCas);
			
			return i;
		}catch (Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.NOT_FOUND);
		}finally{
			s.getTransaction().commit();
		}
	}

	
	
	/**
	 * Create a new desktop user
	 * <p>URL: ../server/api/desktopuser/{username}
	 * <p>Call Type: POST
	 * <p>Payload: A JSON object of attributes that match the Java EmployeeInfo class attributes, EX:
	 * 		//TODO add example
	 * 
	 * @param	username	provided in the URL, the username of the user.
	 * @return Returns a JSON SmartUser object for the created user 
	 */
	@POST
    @Path("/{username}")
    public EmployeeInfo addUser(@PathParam("username") String user, 
    		EmployeeInfo newUser) {

		boolean isCaAdmin = isCaAdminUser();
		if(!isCaAdmin){
			isAdminUser();//throws an exception if you are not allowed still. 
		};
		
		if (newUser.getSmartUserId() != null && newUser.getSmartUserId().length() > 0 && !newUser.getSmartUserId().equals(user)){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectUser.invalidusernames", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		String err = validateUserName(user, SmartUtils.getRequestLocale(request));
		if (err != null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
		}
		err = validatePassword(newUser.getSmartPassword(), SmartUtils.getRequestLocale(request));
		if (err != null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
		}
		
		Employee e = new Employee();
		e.setSmartUserId(user);
		e.setSmartPassword(BcryptCredentialHandler.hashPassword(newUser.getSmartPassword()));

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAdminAccountAction.KEY, newUser.getCaUuid()) 
					&& !SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY) 
					){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to add to this CA."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
			
			e.setConservationArea(HibernateManager.getConservationArea(s, newUser.getCaUuid()));
			e.setId(newUser.getId());
			e.setFamilyName(newUser.getFamilyName());
			e.setGivenName(newUser.getGivenName());
			e.setGender(newUser.getGender());
			e.setDateCreated(new Date());
			e.setStartEmploymentDate(new Date());
			ArrayList<SmartUserLevel> level = new ArrayList<SmartUserLevel>();
			level.add(new SmartUserLevel(newUser.getUserLevelKey()));
			e.setSmartUserLevel(level);
			
			
			//ensure username is unique
			Long userCount = QueryFactory.buildCountQuery(s, Employee.class, 
					new Object[] {"smartUserId", e.getSmartUserId()}, //$NON-NLS-1$
					new Object[] {"conservationArea", e.getConservationArea()}); //$NON-NLS-1$
			if (userCount > 0){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotUnique", SmartUtils.getRequestLocale(request)), e.getSmartUserId())); //$NON-NLS-1$
			}
			
			s.save(e);
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
		return newUser;
	}
	 
	
	/**
	 * Update a user's details
	 * <p>URL: ../server/api/connectuser/{username}
	 * <p>Call Type: PUT
	 * <p>Payload: A JSON object of attributes you wish to update 
	 * 		Example: {username: "testtest", email: "testtest@email.com"}
	 * 		<br>Password Change example: {oldpassword: "testtest", password: "testtest1"}
	 * 
	 * @param	username	provided in the URL, the username of the requested user. 
	 * @return Returns a JSON SmartUser object for the updated user
	 */
    @PUT
    @Path("/{username}")
    public EmployeeInfo updateUser(
    		@PathParam("username") String olduser, @QueryParam("cauuid") String cauuid,
    		EmployeeInfo newUser) {
    	
    	boolean isCaAdmin = isCaAdminUser();

    	//if you are editing yourself, skip validation for admin-level user
    	if( !request.getUserPrincipal().getName().equals(olduser)){
    		if(!isCaAdmin){
    			isAdminUser();//throws an exception if invalid user.
    		}
    	}
    	
    	if (newUser.getSmartUserId() != null){
    		newUser.setSmartUserId(newUser.getSmartUserId().trim());
    		String err = validateUserName(newUser.getSmartUserId(), SmartUtils.getRequestLocale(request));
    		if (err != null){
    			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
    		}
    	}
		
    	Employee toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = HibernateManager.getDesktopUserInCa(s, olduser, cauuid);
			
			
			//check if this is a caAdmin they are allowed to do stuff in this CA
			if(isCaAdmin){
				ArrayList<UUID> uuidCaAdminIn = SecurityManager.INSTANCE.listOfUuidsIsCaAdminOf(s, request.getUserPrincipal().getName() );
				if(!uuidCaAdminIn.contains(toUpdate.getConservationArea().getUuid())){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have access to edit this CA."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
			}
			
			if (newUser.getSmartPassword() != null && !newUser.getSmartPassword().trim().isEmpty()){
				
				String err = validatePassword(newUser.getSmartPassword(), SmartUtils.getRequestLocale(request));
				if (err != null){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
				}

				toUpdate.setSmartPassword(BcryptCredentialHandler.hashPassword(newUser.getSmartPassword()));
			}

			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), olduser)); //$NON-NLS-1$
			}
			
			if (newUser.getSmartUserId() != null && !olduser.equals(newUser.getSmartUserId())){
				//ensure new username is unique
				Employee existingUser = HibernateManager.getDesktopUserInCa(s, newUser.getSmartUserId(), cauuid);
				if (existingUser != null){
					throw new SmartConnectException(
							Response.Status.BAD_REQUEST,
							MessageFormat.format(Messages.getString("ConnectUser.UserNotUnique", SmartUtils.getRequestLocale(request)), newUser.getSmartUserId())); //$NON-NLS-1$
				}
				toUpdate.setSmartUserId(newUser.getSmartUserId());
			}
			toUpdate.setConservationArea(HibernateManager.getConservationArea(s, newUser.getCaUuid()));
			toUpdate.setId(newUser.getId());
			toUpdate.setFamilyName(newUser.getFamilyName());
			toUpdate.setGivenName(newUser.getGivenName());
			toUpdate.setGender(newUser.getGender());
			toUpdate.setDateCreated(new Date());
			toUpdate.setStartEmploymentDate(new Date());
			ArrayList<SmartUserLevel> level = new ArrayList<SmartUserLevel>();
			level.add(new SmartUserLevel(newUser.getUserLevelKey()));
			toUpdate.setSmartUserLevel(level);
			
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
		return newUser;
    }
 
    
    /**
	 * Deactivate a user
	 * <p>URL: ../server/api/desktopuser/{username}
	 * <p>Call Type: DELETE
	 * 
	 * @param	username	provided in the URL, the username of the requested user. 
	 * @return Returns a JSON SmartUser object for the deactivated user
	 */
    @DELETE
    @Path("/{username}")
    public EmployeeInfo deactivateUser(
    		@PathParam("username") String username,
    		@QueryParam("caUuid") String caUuid) {

    	boolean isCaAdmin = isCaAdminUser();
		if(!isCaAdmin){
			isAdminUser();//throws an exception if invalid user.
		}
    	Employee toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getDesktopUserInCa(s, username, caUuid);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), username)); //$NON-NLS-1$
			}
			
			//check if this is a caAdmin they are allowed to do stuff in this CA
			if(isCaAdmin){
				ArrayList<UUID> uuidCaAdminIn = SecurityManager.INSTANCE.listOfUuidsIsCaAdminOf(s, request.getUserPrincipal().getName() );
				if(!uuidCaAdminIn.contains(toDelete.getConservationArea().getUuid())){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have access to this CA."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
			}
			
			
			toDelete.setEndEmploymentDate(new Date()); //setting an end date deactivates the user.
			s.save(toDelete);
			s.flush();
			//might have to check for deleting the last administrator user? maybe the DB constraints prevents that
			//TODO
			
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
		EmployeeInfo d = new EmployeeInfo();
		d.setCaLabel(toDelete.getConservationArea().getName());
		d.setCaUuid(toDelete.getConservationArea().getUuid());
		d.setSmartUserId(toDelete.getSmartUserId());
		return d;
    }
 
    /**
	 * Get all Ca UUIDs the CaAdmin is allowed to access
	 * <p>URL: ../server/api/desktopuser/caUuidsAllowed/
	 * <p>Call Type: GET
	 * 
	 * @return Returns a JSON Array of Employee objects for all users
	 * 
	 */
	@GET
    @Path("/caUuidsAllowed/")
    public ArrayList<SimpleConservationAreaList> getCaUuids(){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ArrayList<SimpleConservationAreaList> results = new ArrayList<SimpleConservationAreaList>();
			ArrayList<UUID> uuids = SecurityManager.INSTANCE.listOfUuidsIsCaAdminOf(s, request.getUserPrincipal().getName() );
			for(UUID id : uuids){
				ConservationArea name = HibernateManager.getConservationArea(s, id);
				results.add(new SimpleConservationAreaList(name.getName(), id));
			}
			return results;
		}catch(Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.BAD_REQUEST, ex); 
		}finally{
			s.getTransaction().commit();
		}

	
	}
    
    /**
     * Validates usernames
     * @param username String of the username
     * @param l language locale 
     * @return
     */
    public static String validateUserName(String username, Locale l){
    	if (username == null 
    			|| username.length() < Employee.MIN_SMART_ID_LENGTH 
    			|| username.length() > Employee.MAX_ID_LENGTH){
    		return MessageFormat.format(Messages.getString("DesktopUser.UserMinRequirement", l), Employee.MIN_SMART_ID_LENGTH, Employee.MAX_ID_LENGTH); //$NON-NLS-1$
    	}
    	return null;
    }
    
    /**
     * Validates passwords - ensures between required length.
     * @param password Strin of password
     * @param l language locale
     * @return
     */
    public static String validatePassword(String password, Locale l){
    	if (password == null 
    			|| (password.length() < Employee.MIN_SMART_PASSWORD_LENGTH)
    			|| password.length() > Employee.MAX_SMART_PASSWORD_LENGTH){ 
    		return MessageFormat.format(Messages.getString("DesktopUser.PassRequirements", l), Employee.MIN_SMART_PASSWORD_LENGTH, Employee.MAX_SMART_PASSWORD_LENGTH); //$NON-NLS-1$
    	}
    	return null;
    }
}


