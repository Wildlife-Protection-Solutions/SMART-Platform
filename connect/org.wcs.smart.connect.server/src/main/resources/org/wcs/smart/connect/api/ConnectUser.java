package org.wcs.smart.connect.api;

import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.mindrot.jbcrypt.BCrypt;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.apache.BcryptCredentialHandler;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.connect.security.UserAccountsAction;


/**
 * Servlet implementation class UpdateUserInfo
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectUser.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectUser extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectUser.class.getName());
	
	public static final String PATH = "connectuser"; //$NON-NLS-1$

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	private void validateUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), UserAccountsAction.KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have user accounts permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(HttpURLConnection.HTTP_UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@GET
    @Path("")
    public List<SmartUser> getAllUsers(){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return HibernateManager.getUsers(s);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@GET
    @Path("/{username}")
    public SmartUser getUser(@PathParam("username") String username){
		validateUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUser su = HibernateManager.getUser(s, username);
			if (su == null){
				logger.info("User " + username + " not found."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
			return su;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@POST
    @Path("/{username}")
    public SmartUser addUser(@PathParam("username") String user, 
    		SmartUser newUser) {
		validateUser();
		if (newUser.getUsername() != null && newUser.getUsername().length() > 0 && !newUser.getUsername().equals(user)){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("ConnectUser.invalidusernames", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		String err = validateUserName(user, SmartUtils.getRequestLocale(request));
		if (err != null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
		}
		err = validatePassword(newUser.getPassword(), SmartUtils.getRequestLocale(request));
		if (err != null){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
		}
		
		SmartUser suser = new SmartUser();
		suser.setUsername(user);
		suser.setPassword(BcryptCredentialHandler.hashPassword(newUser.getPassword()));
		suser.setEmail(newUser.getEmail());
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			//ensure username is unique
			Long userCnt = (Long)s.createCriteria(SmartUser.class)
				.add(Restrictions.eq("username", suser.getUsername())) //$NON-NLS-1$
				.setProjection(Projections.rowCount()).uniqueResult();
			
			if (userCnt > 0){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotUnique", SmartUtils.getRequestLocale(request)), suser.getUsername())); //$NON-NLS-1$
			}
			
			s.save(suser);
			s.getTransaction().commit();
			response.setStatus(Response.Status.CREATED.getStatusCode());
			response.flushBuffer();
		}catch (SmartConnectException ex){
			logger.warning(ex.getMessage());
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.severe(ex.getMessage());
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}finally{
			
		}
		return suser;
	}
 
    @PUT
    @Path("/{username}")
    public SmartUser updateUser(
    		@PathParam("username") String olduser,
    		SmartUser newUser) {
    	validateUser();
    	if (newUser.getUsername() != null){
    		newUser.setUsername(newUser.getUsername().trim());
    		String err = validateUserName(newUser.getUsername(), SmartUtils.getRequestLocale(request));
    		if (err != null){
    			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
    		}
    	}
		if (newUser.getPassword() != null){
			if (newUser.getOldpassword() == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("ConnectUser.PasswordNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			String err = validatePassword(newUser.getPassword(), SmartUtils.getRequestLocale(request));
			if (err != null){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, err);
			}
		}
		
    	SmartUser toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (SmartUser)s.createCriteria(SmartUser.class)
					.add(Restrictions.eq("username", olduser)) //$NON-NLS-1$
					.list().get(0);
			
			if (newUser.getPassword() != null){
				if (!BCrypt.checkpw(newUser.getOldpassword(), toUpdate.getPassword())){
					throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("ConnectUser.InvalidPassword", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
				}
			}

			if (toUpdate == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), olduser)); //$NON-NLS-1$
			}
			
			if (newUser.getUsername() != null && !olduser.equals(newUser.getUsername())){
				//ensure new username is unique
				Long userCnt = (Long)s.createCriteria(SmartUser.class)
						.add(Restrictions.eq("username", newUser.getUsername())) //$NON-NLS-1$
						.setProjection(Projections.rowCount())
						.list().get(0);
			
				if (userCnt > 0){
					throw new SmartConnectException(
							HttpURLConnection.HTTP_BAD_REQUEST,
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
			
			s.update(toUpdate);
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.warning(ex.getMessage());
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.severe(ex.getMessage());
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}finally{
		}
		return toUpdate;
    }
 
    @DELETE
    @Path("/{username}")
    public SmartUser removeUser(@PathParam("username") String username) {
    	validateUser();
    	SmartUser toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getUser(s, username);
			if (toDelete == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUser.UserNotFound", SmartUtils.getRequestLocale(request)), username)); //$NON-NLS-1$
			}
			s.delete(toDelete);
			s.flush();
			//cannot delete the last administrator user
			Long adminCnt = (Long) s.createCriteria(SmartUserAction.class)
				.add(Restrictions.eq("action", AdminAccountAction.KEY)) //$NON-NLS-1$
				.setProjection(Projections.rowCount())
				.uniqueResult();
			if (adminCnt <= 0){
				throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST,
						Messages.getString("ConnectUser.DeleteAdminErr", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			s.getTransaction().commit();
			
			//if we are deleting the current user we should auto logout
			if (toDelete.getUsername().equals(request.getUserPrincipal().getName())){
				request.logout();
			}
		}catch (SmartConnectException ex){
			logger.warning(ex.getMessage());
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.severe(ex.getMessage());
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}finally{
		}
		return toDelete;
    }
 
    public String validateUserName(String username, Locale l){
    	if (username == null 
    			|| username.length() < SmartUser.MIN_USERNAME_LENGTH 
    			|| username.length() > SmartUser.MAX_USERNAME_LENGTH){
    		return MessageFormat.format(Messages.getString("ConnectUser.UserMinRequirement", l), SmartUser.MIN_USERNAME_LENGTH, SmartUser.MAX_USERNAME_LENGTH); //$NON-NLS-1$
    	}
    	return null;
    }
    
    public String validatePassword(String password, Locale l){
    	//TODO: fix me
    	if (password == null 
    			|| (!password.endsWith("smart") && password.length() < SmartUser.MIN_PASS_LENGTH) //$NON-NLS-1$
    			|| password.length() > SmartUser.MAX_PASS_LENGTH){ 
    		return MessageFormat.format(Messages.getString("ConnectUser.PassRequirements", l), SmartUser.MIN_PASS_LENGTH, SmartUser.MAX_PASS_LENGTH); //$NON-NLS-1$
    	}
    	return null;
    }
}
