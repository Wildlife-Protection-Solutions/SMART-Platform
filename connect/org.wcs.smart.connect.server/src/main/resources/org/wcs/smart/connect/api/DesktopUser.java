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
import org.mindrot.jbcrypt.BCrypt;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.apache.BcryptCredentialHandler;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.EmployeeInfo;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserRole;
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
			return SecurityManager.INSTANCE.isCaAdmin(s, request.getUserPrincipal().getName(), CaAdminAccountAction.KEY);
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
    public List<Employee> getUsers(){
		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		
		//TODO check for CA-admins and get CAs they are allowed to access then return employees in those only.
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return (ArrayList<Employee>) HibernateManager.getDesktopUsers(s);
			//return new ArrayList<Employee>();
		}catch(Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.BAD_REQUEST, ex); //$NON-NLS-1$	
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
	 * Gets a user's details, could be in more than one CA
	 * <p>URL: ../server/api/desktopuser/{username}
	 * <p>Call Type: GET
	 * 
	 * @param username	provided in the URL, the username of the requested user.
	 * 
	 * @return Returns a List of all the user for each CAs they are in, just the smartuserId, CA Name and CaUuid is returned 
	 */
	
	@GET
    @Path("/{username}")
    public List<Employee> getUser(@PathParam("username") String username, @QueryParam("cauuid") String cauuid){
		Boolean validate = false;

		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		
		//TODO return user to CA-admins only if allowed to access that employees
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<Employee> e = HibernateManager.getDesktopUser(s, username);
			return e;
		}catch (Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.NOT_FOUND);
		}finally{
			s.getTransaction().commit();
		}
	}

	
	/**
	 * Gets a user's details in a particular CA
	 * <p>URL: ../server/api/desktopuser/ca/{username}
	 * <p>Call Type: GET
	 * 
	 * @param username	provided in the URL, the username of the requested user.
	 * @param cauuid  the uuid of the requested ca.
	 * 
	 * @return Returns a user for the CAyou specified 
	 */
	
	@GET
    @Path("/ca/{username}")
    public EmployeeInfo getUserInCa(@PathParam("username") String username, @QueryParam("cauuid") String cauuid){
		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		
		//TODO return user to CA-admins only if allowed to access that employees
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			Employee e = HibernateManager.getDesktopUserInCa(s, username, cauuid);
			EmployeeInfo i = new EmployeeInfo();
			i.setCaUuid(e.getConservationArea().getUuid());
			i.setSmartUserId(e.getSmartUserId());
			i.setFamilyName(e.getFamilyName());
			i.setGivenName(e.getGivenName());
			i.setGender(e.getGender());
			i.setId(e.getId());
			i.setUserLevelKey(e.getSmartUserLevelKeys());
			
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

		if(isCaAdminUser()){
			//you are allowed
		}else{
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
			Long userCount = (Long) s.createQuery("select count(*) from Employee e join e.conservationArea c where e.smartUserId = '" + e.getSmartUserId() + "' AND c.uuid = '" + e.getConservationArea().getUuid() + "'").uniqueResult();
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
    public SmartUser updateUser(
    		@PathParam("username") String olduser,
    		SmartUser newUser) {
    	
    	//if you are editing yourself, skip validation for admin-level user
    	if( !request.getUserPrincipal().getName().equals(olduser)){
   			isAdminUser();//throws an exception if not an Admin
    	}
    	
    	if (newUser.getUsername() != null){
    		newUser.setUsername(newUser.getUsername().trim());
    		String err = validateUserName(newUser.getUsername(), SmartUtils.getRequestLocale(request));
    		if (err != null){
    			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
    		}
    	}
		if (newUser.getPassword() != null){
			if (newUser.getOldpassword() == null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectUser.PasswordNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			String err = validatePassword(newUser.getPassword(), SmartUtils.getRequestLocale(request));
			if (err != null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
			}
		}
		
    	SmartUser toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = QueryFactory.buildQuery(s, SmartUser.class, "username", olduser).uniqueResult(); //$NON-NLS-1$
			if (newUser.getPassword() != null){
				if (!BCrypt.checkpw(newUser.getOldpassword(), toUpdate.getPassword())){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectUser.InvalidPassword", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
				}
			}

			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), olduser)); //$NON-NLS-1$
			}
			
			if (newUser.getUsername() != null && !olduser.equals(newUser.getUsername())){
				//ensure new username is unique
				Long userCnt = QueryFactory.buildCountQuery(s, SmartUser.class, new Object[] {"username", newUser.getUsername()}); //$NON-NLS-1$
				if (userCnt > 0){
					throw new SmartConnectException(
							Response.Status.BAD_REQUEST,
							MessageFormat.format(Messages.getString("ConnectUser.UserNotUnique", SmartUtils.getRequestLocale(request)), newUser.getUsername())); //$NON-NLS-1$
				}
				toUpdate.setUsername(newUser.getUsername());
			}
			
			if (newUser.getPassword() != null){
				//hash password
				toUpdate.setPassword(BcryptCredentialHandler.hashPassword(newUser.getPassword()));	
			}
			
			if (newUser.getEmail() != null){
				toUpdate.setEmail(newUser.getEmail());
			}
			
			if (newUser.getResetDatetime() != null){
				toUpdate.setResetDatetime(newUser.getResetDatetime());
			}
			if (newUser.getResetId() != null){
				toUpdate.setResetId(newUser.getResetId());
			}
			if(newUser.getHomeCaUuid() != null){
				toUpdate.setHomeCaUuid(newUser.getHomeCaUuid());
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

    	isAdminUser();
    	Employee toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getDesktopUserInCa(s, username, caUuid);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), username)); //$NON-NLS-1$
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
		d.setCaUuid(toDelete.getConservationArea().getUuid());
		d.setSmartUserId(toDelete.getSmartUserId());
		return d;
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
