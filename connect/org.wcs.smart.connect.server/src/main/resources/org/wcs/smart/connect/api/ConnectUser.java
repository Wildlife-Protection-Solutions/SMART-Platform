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
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.apache.BcryptCredentialHandler;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;


/**
 * SMART Connect REST API for Users.
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectUser.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
@SecuritySchemes(value = {
	@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
})
public class ConnectUser extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectUser.class.getName());
	
	public static final String PATH = "connectuser"; //$NON-NLS-1$

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
	 * <p>Get all active users.</p>
	 * <p>
	 * URL: ../server/api/connectuser/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @return Returns a JSON Array of SmartUser objects for all active users (<a href="https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/SmartUser.java">SmartUser</a>)
	 * 
	 */
	@GET
    @Path("")
	@Operation(description = "Get all active Connect users.")
    public List<SmartUser> getActiveUsers(){
		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return (ArrayList<SmartUser>) HibernateManager.getActiveUsers(s);
		}finally{
			s.getTransaction().commit();
		}
	}

	
	
	/**
	 * <p>Gets all inactive users.</p>
	 * <p>
	 * URL: ../server/api/connectuser/getinactive/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @return Returns a JSON Array of SmartUser objects for all inactive users 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/getinactive/")
	@Operation(description = "Gets all inactive users.")
    public List<SmartUser> getInactiveUsers(){
		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return (ArrayList<SmartUser>) HibernateManager.getInactiveUsers(s);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * <p>Returns whether the current user is an admin user or not.</p>
	 * <p>
	 * URL: ../server/api/iscurrentuseradmin/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @return Returns a JSON SmartUser object for the selected user 
	 */
	
	@GET
    @Path("/iscurrentuseradmin/")
	@Operation(description = "Returns whether the current user is an admin user or not.")
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
	 * <p>Gets the current user's details</p>
	 * <p>
	 * URL: ../server/api/connectuser/getCurrent/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @return Returns a JSON SmartUser object of the currently logged in user 
	 */	
	@GET
    @Path("/getCurrent/")
	@Operation(description = "Get the detailed information about the current user.")
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
	 * <p>Gets a single user's details</p>
	 * <p>
	 * URL: ../server/api/connectuser/{username}<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param username	provided in the URL, the username of the requested user.
	 * @param validateOnly Boolean, optional, set to true if you only want to validate this user and not get their full details back. 
	 * 
	 * @return Returns a JSON SmartUser object for the selected user 
	 */
	
	@GET
    @Path("/{username}")
	@Operation(description = "Get a users details.")
    public SmartUser getUser(
    		@Parameter(description="the username of the requested user") @PathParam("username") String username, 
    		@Parameter(description="optional, set to true if you only want to validate this user and not get their full details back") @QueryParam("validate") String validateOnly){
		Boolean validate = false;
		if (validateOnly != null){
			try{
				validate = Boolean.valueOf(validateOnly);
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid validate parameter."); //$NON-NLS-1$
			}
		}
		if (validate){
			return null;
		}
		
		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
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
	 * <p>Create a new user</p>
	 * <p>
	 * URL: ../server/api/connectuser/{username}<br>
	 * Call Type: POST
	 * Payload: A JSON object of attributes that match the Java attributes.
	 * </p>
	 * <pre>{
	 *   username: "testtest", 
	 *   email: "testtest", 
	 *   password: "testtest"
	 *}</pre> 
	 * 
	 * @param	username	provided in the URL, the username of the user.
	 * @return Returns a JSON SmartUser object for the created user 
	 */
	@POST
    @Path("/{username}")
	@Operation(description = "Create a new user.")
    public SmartUser addUser(@Parameter(description="the username of the user") @PathParam("username") String user, 
    		@Parameter(description="other details about the user") SmartUser newUser) {

		if(isCaAdminUser()){
			//you are allowed
		}else{
			isAdminUser();//throws an exception if you are not allowed still. 
		};
		
		if (newUser.getUsername() != null && newUser.getUsername().length() > 0 && !newUser.getUsername().equals(user)){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("ConnectUser.invalidusernames", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		String err = validateUserName(user, SmartUtils.getRequestLocale(request));
		if (err != null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
		}
		err = validatePassword(newUser.getPassword(), SmartUtils.getRequestLocale(request));
		if (err != null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, err);
		}
		
		SmartUser suser = new SmartUser();
		suser.setUsername(user);
		suser.setPassword(BcryptCredentialHandler.hashPassword(newUser.getPassword()));
		suser.setEmail(newUser.getEmail());
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			//ensure username is unique
			Long userCnt = QueryFactory.buildCountQuery(s, SmartUser.class, new Object[] {"username", suser.getUsername()}); //$NON-NLS-1$
			if (userCnt > 0){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotUnique", SmartUtils.getRequestLocale(request)), suser.getUsername())); //$NON-NLS-1$
			}
			
			s.save(suser);
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
		return suser;
	}
 
	/**
	 * <p>Update a user's details</p>
	 * <p>
	 * URL: ../server/api/connectuser/{username}<br>
	 * Call Type: PUT<br>
	 * Payload: A JSON object of attributes you wish to update
	 * </p>
	 * Example:
	 * <pre>{
	 *   username: "testtest", 
	 *   email: "testtest@email.com"
	 *}</pre>
	 * Password Change Example:
	 * <pre>{
	 *   oldpassword: "testtest", 
	 *   password: "testtest1"
	 *}</pre>
	 * 
	 * Only non-null attributes are updated.  For home ca use a value of 99999999-9999-9999-9999-999999999999 to
	 * update the home ca to null.
	 * 
	 * @param	username	provided in the URL, the username of the requested user. 
	 * @return Returns a JSON SmartUser object for the updated user
	 */
    @PUT
    @Path("/{username}")
    @Operation(description = "Update a user's details")
    public SmartUser updateUser(
    		@Parameter(description="the username of the requested user") @PathParam("username") String olduser,
    		@Parameter(description="the new values for the user details") SmartUser newUser) {
    	
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
				if (newUser.getHomeCaUuid().equals(UuidUtils.stringToUuid("99999999-9999-9999-9999-999999999999"))) { //$NON-NLS-1$
					toUpdate.setHomeCaUuid(null);
				}else {
					toUpdate.setHomeCaUuid(newUser.getHomeCaUuid());
				}
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
	 * <p>Activate an inactive user</p>
	 * URL: ../server/api/connectuser/activate/{username}<br>
	 * Call Type: PUT<br>
	 * Payload: none<br>
	 * 
	 * @param	username	provided in the URL, the username of the requested user. 
	 * @return Returns a JSON SmartUser object for the activated user
	 */
    @PUT
    @Path("/activate/{username}")
    @Operation(description = "Activate an inactive user.")
    public SmartUser activateUser(
    		@Parameter(description="the username to activate") @PathParam("username") String username) {
    	
    	isAdminUser();
    	
    	SmartUser user;
    	
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			user = QueryFactory.buildQuery(s, SmartUser.class, "username", username).uniqueResult(); //$NON-NLS-1$

			SmartUserRole role = new SmartUserRole();
			role.setUsername(username);
			role.setRole(HibernateManager.getSmartRole(s));
			
			s.save(role);
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
		return user;
    }
    
    /**
	 * <p>Deactivate a user</p>
	 * <p>
	 * URL: ../server/api/connectuser/activate/{username}<br>
	 * Call Type: DELETE
	 * </p>
	 * 
	 * @param	username	provided in the URL, the username of the requested user. 
	 * @return Returns a JSON SmartUser object for the deactivated user
	 */
    @DELETE
    @Path("/activate/{username}")
    @Operation(description = "Deactivate a user.")
    public SmartUser deactivateUser(
    		@Parameter(description="the username to deactivate") @PathParam("username") String username) {
    	isAdminUser();
    	
    	SmartUser user;
    	
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			user = QueryFactory.buildQuery(s, SmartUser.class, "username", username).uniqueResult(); //$NON-NLS-1$
			List<SmartUserRole> roles = HibernateManager.getUserRoles(s, user.getUsername());
			for(SmartUserRole role: roles){
				s.delete(role);
				s.flush();
			}
			
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
		return user;
    }
    
    /**
	 * <p>Deletes a user</p>
	 * <p>
	 * URL: ../server/api/connectuser/{username}</br>
	 * Call Type: Delete
	 * </p>
	 * 
	 * @param	username	provided in the URL, the username of the requested user. 
	 * @return Returns a JSON SmartUser object for the deleted user
	 */
    @DELETE
    @Path("/{username}")
    @Operation(description = "Deletes a user.")
    public SmartUser removeUser(@Parameter(description="the username to delete") @PathParam("username") String username) {
    	isAdminUser();
    	SmartUser toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getUser(s, username);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), username)); //$NON-NLS-1$
			}
			s.delete(toDelete);
			s.flush();
			//cannot delete the last administrator user
			SecurityManager.INSTANCE.validateSingleAdminUser(s, SmartUtils.getRequestLocale(request));
			
			s.getTransaction().commit();
			
			//if we are deleting the current user we should auto logout
			if (toDelete.getUsername().equals(request.getUserPrincipal().getName())){
				request.logout();
			}
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
 
    /**
     * Validates usernames
     * @param username String of the username
     * @param l language locale 
     * @return
     */
    public static String validateUserName(String username, Locale l){
    	if (username == null 
    			|| username.length() < SmartUser.MIN_USERNAME_LENGTH 
    			|| username.length() > SmartUser.MAX_USERNAME_LENGTH){
    		return MessageFormat.format(Messages.getString("ConnectUser.UserMinRequirement", l), SmartUser.MIN_USERNAME_LENGTH, SmartUser.MAX_USERNAME_LENGTH); //$NON-NLS-1$
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
    			|| (password.length() < SmartUser.MIN_PASS_LENGTH)
    			|| password.length() > SmartUser.MAX_PASS_LENGTH){ 
    		return MessageFormat.format(Messages.getString("ConnectUser.PassRequirements", l), SmartUser.MIN_PASS_LENGTH, SmartUser.MAX_PASS_LENGTH); //$NON-NLS-1$
    	}
    	return null;
    }
}
