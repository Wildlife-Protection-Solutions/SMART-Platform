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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartActionsProxy;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.SmartUserActionProxy;
import org.wcs.smart.connect.security.ActionManager;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.ISmartConnectAction;
import org.wcs.smart.connect.security.ResourceOption;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.connect.security.UserAccountsAction;

/**
 * SMART Connect REST API for configuring user permission and actions.
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectUser.PATH +
		ConnectRESTApplication.PATH_SEPERATOR + ConnectUserAction.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectUserAction extends HttpServlet {
	
	public static final String PATH = "actions";
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectUserAction.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	/*
	 * Ensures the user has access to change/view user actions
	 */
	private void validateUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), UserAccountsAction.KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to modify user acction details."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Lists all available actions.
	 * 
	 * @return
	 */
	@GET
    @Path("/")
    public List<SmartActionsProxy> getActions(){
		validateUser();
		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		try{
			List<SmartActionsProxy> actionResources = new ArrayList<SmartActionsProxy>();
			
			for (ISmartConnectAction a : ActionManager.INSTANCE.getAllActions()){
				for (String actionKey : a.getActionKeys()){
					SmartActionsProxy next = new SmartActionsProxy(a.getActionName(actionKey, SmartUtils.getRequestLocale(request)), actionKey);
					List<ResourceOption> options = a.getResourceOptions(actionKey,s, SmartUtils.getRequestLocale(request));
					if (options == null || options.size() == 0){
						next.addResource(Messages.getString("ConnectUserAction.NA", SmartUtils.getRequestLocale(request)), null); //$NON-NLS-1$
					}else{
						for (ResourceOption op : options){
							next.addResource(op.getName(), op.getUuid());
						}
					}
					actionResources.add(next);
				}
			}
			return actionResources;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.ActionError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Lists all actions associated with a given user.
	 * 
	 * @param username
	 * @return
	 */
	@GET
    @Path("/{username}")
    public List<SmartUserActionProxy> getUserActions(@PathParam("username") String username){
		validateUser();
		
		Session s = HibernateManager.getSession(context, SmartUtils.getRequestLocale(request));
		s.beginTransaction();
		try{
			@SuppressWarnings("unchecked")
			List<SmartUserAction> actions = s.createCriteria(SmartUserAction.class)
					.add(Restrictions.eq("username", username))
					.list();
			
			List<SmartUserActionProxy> items = new ArrayList<SmartUserActionProxy>();
			for (SmartUserAction a : actions){
				SmartUserActionProxy p = new SmartUserActionProxy();
				p.setActionKey(a.getAction());
				p.setResource(a.getResource());
				
				ISmartConnectAction action = ActionManager.INSTANCE.findAction(a.getAction());
				p.setActionName(action.getActionName(a.getAction(), SmartUtils.getRequestLocale(request)));
				if (a.getResource() != null){
					p.setResourceName(action.getResourceName(a.getResource(), s, SmartUtils.getRequestLocale(request)));
				}
				items.add(p);
			}
			return items;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR,
					Messages.getString("ConnectUserAction.UserError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Removes an action from a given user.
	 * 
	 * @param username
	 * @param action
	 */
	@DELETE
    @Path("/{username}/{action}")
    public void getUserActions(@PathParam("username") String username,
    		@PathParam("action") String action
    		){
		deleteUserActions(username, action, null);
	}
	
	/**
	 * Removes an action and specific resource from a given user.
	 * 
	 * @param username
	 * @param action
	 * @param resource
	 */
	@DELETE
    @Path("/{username}/{action}/{resource}")
    public void deleteUserActions(@PathParam("username") String username,
    		@PathParam("action") String action,
    		@PathParam("resource") String resource){
		
		validateUser();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			@SuppressWarnings("unchecked")
			List<SmartUserAction> actions = s.createCriteria(SmartUserAction.class)
					.add(Restrictions.eq("username", username)) //$NON-NLS-1$
					.add(Restrictions.eq("action", action)) //$NON-NLS-1$
					.list();
			
			
			for(SmartUserAction a : actions){
				if ((resource == null && a.getResource() == null) ||
					(a.getResource() != null && a.getResource().toString().equals(resource))){		
					s.delete(a);
				}
			}
			s.flush();
			
			// we need at least one admin user
			Long adminCnt = (Long) s.createCriteria(SmartUserAction.class)
					.add(Restrictions.eq("action", AdminAccountAction.KEY)) //$NON-NLS-1$
					.setProjection(Projections.rowCount()).uniqueResult();
			if (adminCnt <= 0) {
				throw new SmartConnectException(
						Response.Status.BAD_REQUEST,
						Messages.getString("ConnectUserAction.AdminError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.UserDeleteError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}	
	}
	
	
	/**
	 * Adds a new action for a given user.
	 * @param username
	 * @param actionKey
	 */
	@POST
    @Path("/{username}/{action}")
    public void addUserActions(@PathParam("username") String username,
    		@PathParam("action") String actionKey){
		addUserActions(username, actionKey, null);
	}
	
	/**
	 * Add a new action with a specific resource for a given user.
	 * @param username
	 * @param actionKey
	 * @param resourceKey
	 */
	@POST
    @Path("/{username}/{action}/{resource}")
    public void addUserActions(@PathParam("username") String username,
    		@PathParam("action") String actionKey,
    		@PathParam("resource") String resourceKey){
		validateUser();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartUserAction newaction = new SmartUserAction();
			newaction.setAction(actionKey);
			newaction.setUsername(username);
			if (resourceKey != null){
				newaction.setResource(UUID.fromString(resourceKey));
			}
			s.save(newaction);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.UserAddError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
}
