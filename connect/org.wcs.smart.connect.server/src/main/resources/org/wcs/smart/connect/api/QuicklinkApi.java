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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
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

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.Quicklink;
import org.wcs.smart.connect.model.QuicklinkWrapper;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.UserQuicklink;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * Smart Connect REST API for Quicklinks
 * 
 * @author Jeff
 *
 */


@Path(ConnectRESTApplication.PATH_SEPERATOR + QuicklinkApi.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class QuicklinkApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = Logger.getLogger(QuicklinkApi.class.getName());
	
	public static final String PATH = "quicklink"; //$NON-NLS-1$
	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	
	//this function throws an unathorized exception if the user is not an admin 
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
	
	//this function returns true of false, no exception is thrown either way.
	private boolean isAdminUserNoException(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY);
		}finally{
			s.getTransaction().commit();
		}
	}

	/**
	 * Get all Quicklinks in the system - must be an admin to use this function 
	 * URL: ../server/api/quicklink/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON representation of a list of Quicklink objects 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/")
    public List<Quicklink> getAllQuickLinks(){
		isAdminUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return s.createCriteria(Quicklink.class).addOrder(Order.desc("adminCreated")).addOrder(Order.asc("createdOn")).list();
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	
	/**
	 * Get all Quicklinks created by admin users
	 * URL: ../server/api/quicklink/adminonly
	 * Call Type: GET
	 * 
	 * @return Returns a JSON representation of a list of Quicklink objects 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/adminonly/")
    public List<Quicklink> getAllAdminCreatedQuickLinks(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return s.createCriteria(Quicklink.class).add(Restrictions.eq("adminCreated", true)).addOrder(Order.asc("label")).list();
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	
	 /**
	 * Updates a Quicklink - must be an admin to do this. 
	 * <p>URL: ../server/api/quicklink/{uuid}
	 * <p>Call Type: PUT
	 * 
	 * <p>Payload: A JSON string containing url and/or label properties to update the specificed QuickLink
	 * 
	 * @param	uuid	provided in the path, the UUID of the quicklink to update.
	 * @return Returns a JSON UserQuicklink object that was deleted 
	 */
    @PUT
    @Path("/{uuid}")
    public Quicklink UpdateQuicklink(@PathParam("uuid") UUID uuid, Quicklink Quicklink) {
    	isAdminUser();
    	Quicklink toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toUpdate = (Quicklink)s.createCriteria(Quicklink.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, "QuickLink Not Found."); 
			}
			if(Quicklink.getLabel() != null){
				toUpdate.setLabel(Quicklink.getLabel());
			}
			if(Quicklink.getUrl() != null){
				toUpdate.setUrl(Quicklink.getUrl());
			}
			s.saveOrUpdate(toUpdate);
			
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
		}
		return toUpdate;
    }
    

	/**
	 * Delete a Quicklink - removes all UserQuicklinks associated with it as well - must be an admin
	 * URL: ../server/api/quicklink/{uuid}
	 * Call Type: DELETE
	 * 
	 * @param	uuid	provided in the path, the UUID of the quicklink to delete.
	 * @return Returns a JSON Quicklink object that was deleted 
	 */
    @DELETE
    @Path("/{uuid}")
    public Quicklink removeQuicklink(@PathParam("uuid") UUID uuid) {
    	isAdminUser();
    	Quicklink toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = (Quicklink)s.createCriteria(Quicklink.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, "QuickLink Not Found."); 
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
		}
		return toDelete;
    }
	
	    
	    
	
	/**
	 * Get a list of Quicklinks to show on the homepage for the User currently logged in. Use this function for Web-UI interaction. 
	 * URL: ../server/api/quicklink/user/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON representation of Quicklink wrappers  for the user currently logged into to a tomcat session 
	 */
	@GET
    @Path("/user/")
    public List<QuicklinkWrapper> getQuickLinksForSessionUser(){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		SmartUser user;
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
		}finally{
			s.getTransaction().rollback();
		}
		if(user == null){
			return null;
		}
		return getQuicklinksForUser(user.getUuid());
	}
	
	/**
	 * Get a list of Quicklinks for a particular User uuid. User this function directly when not using the WEB-UI or otherwise have no session open already.
	 * URL: ../server/api/quicklink/user/{uuid}
	 * Call Type: GET
	 * 
	 * @param	uuid	provided in the URL, the uuid of the user.
	 * @return Returns a JSON representation of Quicklink objects for the user 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/user/{uuid}")
    public List<QuicklinkWrapper> getQuicklinksForUser(@PathParam("uuid") UUID uuid){

		ArrayList<QuicklinkWrapper> list = new ArrayList<QuicklinkWrapper>();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<Object[]> results = s.createQuery("Select q.url, u.labelOverride, u.order, u.uuid from UserQuicklink u join u.quicklink q where u.userUuid = '" + uuid + "' order by u.order").list();
			for (Object[] row : results) {
				list.add(new QuicklinkWrapper((String)row[0], (String)row[1], (int)row[2], (UUID)row[3]));
			}
		}catch(Exception ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}finally{
			s.getTransaction().rollback();
		}
		return list;
	}
	
	
	
	/**
	 * Create a new Quicklink
	 * URL: ../server/api/quicklink/
	 * Call Type: POST
	 * Payload: A GeoJSON object that has properties that match the Java attributes of a Quicklink, leaving out the uuid which will get created in the Database automatically.
	 * 
	 * @return Returns a JSON Quicklink object for the created Quicklink,
	 */
	
	@POST
    @Path("/")
    public Quicklink addQuicklink(Quicklink quicklink) {
		Quicklink q = new Quicklink();
		UUID userUuid = null; 
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			userUuid = user.getUuid();
		}finally{
			s.getTransaction().rollback();
		}
		if(userUuid == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid or Unathorized user trying to create a Quicklink, try logging back in." ); 
		}
		q.setCreatedBy(userUuid);
		q.setCreatedOn(new Timestamp(Instant.now().toEpochMilli()));
		q.setLabel(quicklink.getLabel());
		q.setUrl(quicklink.getUrl());
		q.setAdminCreated(isAdminUserNoException());

		
		s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(q);
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
		}
		
		return q;
	}
	
	/**
	 * Add a Quicklink to a user's homepage list - user the currently logged in user, use this method in the web UI.
	 * URL: ../server/api/quicklink/addtolist/
	 * Call Type: GET
	 * 
	 * @parameter quicklinkUuid - the uuid for the quicklink you want to add to your list
	 * 
	 * @return Returns a JSON UsersQuicklinkList object you just added to your list
	 */
	
	@GET
    @Path("/addtolist/")
    public UserQuicklink addQuicklinkToList(@QueryParam(value="quicklinkUuid") UUID quicklinkUuid) {
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		SmartUser user;
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
		}finally{
			s.getTransaction().rollback();
		}
		if(user == null){
			return null;
		}
		return addQuicklinkToUsersList(user.getUuid(), quicklinkUuid);
	}
	
	/**
	 * Add a Quicklink to a user's homepage list - provide the user-uuid in the url, use this function when you don't have a browser session open and logged in.
	 * URL: ../server/api/quicklink/addtolist/{user uuid}
	 * Call Type: GET
	 * 
	 * @return Returns a JSON QuicklinkWrapper object for just added to your list
	 */
	
	@GET
    @Path("/addtolist/{uuid}")
    public UserQuicklink addQuicklinkToUsersList(@PathParam("uuid") UUID uuid, UUID quicklinkUuid) {
		UserQuicklink listEntry = new UserQuicklink();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			Quicklink quicklink = (Quicklink)s.createCriteria(Quicklink.class).add(Restrictions.eq("uuid", quicklinkUuid)).uniqueResult();
			listEntry.setLabelOverride(quicklink.getLabel());
			listEntry.setOrder(100);
			listEntry.setQuicklink(quicklink);
			listEntry.setUserUuid(uuid);
			s.save(listEntry);
			s.getTransaction().commit();
			response.setStatus(Response.Status.CREATED.getStatusCode());
			response.flushBuffer();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(ex.getMessage(), ex);
		}
		return listEntry;
	}
	
	
	/**
	 * Delete a UserQuicklink - removes a link from that users page, the related quicklink itself is deleted as well if it is owned by this user and no other users are using it.
	 * URL: ../server/api/quicklink/user/{uuid}
	 * Call Type: DELETE
	 * 
	 * @param	uuid	provided in the path, the UUID of the userquicklink to delete.
	 * @return Returns a JSON UserQuicklink object that was deleted 
	 */
    @DELETE
    @Path("/user/{uuid}")
    public UserQuicklink removeUserQuicklink(@PathParam("uuid") UUID uuid) {
    	UserQuicklink toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			toDelete = (UserQuicklink)s.createCriteria(UserQuicklink.class).add(Restrictions.eq("uuid", uuid)).add(Restrictions.eq("userUuid",user.getUuid())).uniqueResult();
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, "User's QuickLink Not Found."); 
			}
			s.delete(toDelete);
			
			// if no one else is using this quicklink and the user is the creator, remove it as well. Quickly clean up user-created links that no one else needs 
			@SuppressWarnings("unchecked")
			ArrayList<UserQuicklink> othersLeft = (ArrayList<UserQuicklink>)s.createCriteria(UserQuicklink.class).add(Restrictions.eq("quicklink.uuid", toDelete.getQuicklink().getUuid())).list();
			if(othersLeft.size() == 0){
				Quicklink ql = (Quicklink)s.createCriteria(Quicklink.class).add(Restrictions.eq("uuid", toDelete.getQuicklink().getUuid())).add(Restrictions.eq("createdBy", user.getUuid())).uniqueResult();
				if(ql != null){
					s.delete(ql);
				}
			}
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
		}
		return toDelete;
    }
    
    
    
    /**
	 * Updates a UserQuicklink 
	 * <p>URL: ../server/api/quicklink/user/{uuid}
	 * <p>Call Type: PUT
	 * 
	 * <p>Payload: A JSON string containing order and/or labelOverride properties to update the specificed UserQuickLink
	 * 
	 * @param	uuid	provided in the path, the UUID of the userquicklink to update.
	 * @return Returns a JSON UserQuicklink object that was deleted 
	 */
    @PUT
    @Path("/user/{uuid}")
    public UserQuicklink UpdateUserQuicklink(@PathParam("uuid") UUID uuid, UserQuicklink userQuicklink) {
    	UserQuicklink toUpdate = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			toUpdate = (UserQuicklink)s.createCriteria(UserQuicklink.class).add(Restrictions.eq("uuid", uuid)).add(Restrictions.eq("userUuid",user.getUuid())).uniqueResult();
			if (toUpdate == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, "User's QuickLink Not Found."); 
			}
			if(userQuicklink.getLabelOverride() != null){
				toUpdate.setLabelOverride(userQuicklink.getLabelOverride());
			}
			if(userQuicklink.getOrder() > -10000){
				toUpdate.setOrder(userQuicklink.getOrder());
			}
			s.saveOrUpdate(toUpdate);
			
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
		}
		return toUpdate;
    }
    
    
    /**
	 * <p>Create a new Quicklink and add it to all user's homepages - must be an admin to do this
	 * <p>URL: ../server/api/quicklink/addtoall/
	 * <p>Call Type: POST
	 * <p>payload: a JSON version of a Quicklink Object, leaving out the uuid which will get created in the Database automatically.
	 * 
	 * @parameter quicklinkUuid - the uuid for the quicklink you want to add to your list
	 * 
	 * @return Returns a JSON UsersQuicklinkList object you just added to your list
	 */
	
	@POST
    @Path("/addtoall/")
    public Quicklink addQuicklinkToAll(Quicklink quicklink) {
		Quicklink q = new Quicklink();
		UUID userUuid = null; 
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			userUuid = user.getUuid();
		}finally{
			s.getTransaction().rollback();
		}
		isAdminUser();//throws an exception if it fails.
		if(userUuid == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid or Unathorized user trying to create a Quicklink, try logging back in." ); 
		}
		
		q.setCreatedBy(userUuid);
		q.setCreatedOn(new Timestamp(Instant.now().toEpochMilli()));
		q.setLabel(quicklink.getLabel());
		q.setUrl(quicklink.getUrl());
		q.setAdminCreated(isAdminUserNoException());
		
		s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(q);
			@SuppressWarnings("unchecked")
			ArrayList<SmartUser> users = (ArrayList<SmartUser>) s.createCriteria(SmartUser.class).list();
			for(SmartUser u : users){
				UserQuicklink ql = new UserQuicklink();
				ql.setOrder(100);
				ql.setLabelOverride(quicklink.getLabel());
				ql.setQuicklink(q);
				ql.setUserUuid(u.getUuid());
				s.save(ql);
			}
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
		}
		
		return q;
	}
	
}