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
import javax.ws.rs.core.Response.Status;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartActionsProxy;
import org.wcs.smart.connect.model.SmartRole;
import org.wcs.smart.connect.model.SmartRoleAction;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.SmartUserPermissionProxy;
import org.wcs.smart.connect.model.SmartUserPermissionProxy.Type;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.connect.security.ActionManager;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.ISmartConnectAction;
import org.wcs.smart.connect.security.ResourceOption;
import org.wcs.smart.connect.security.SecurityManager;

/**
 * SMART Connect REST API for configuring user and role
 * permissions and actions.
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectUserAction.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectUserAction extends HttpServlet {
	
	public static final String PATH = "privileges"; //$NON-NLS-1$
	
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
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)){
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
    @Path("/actions")
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
	 * Lists all available roles.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/roles")
    public List<SmartActionsProxy> getRoles(){
		validateUser();
		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		try{
			List<SmartRole> roles = s.createCriteria(SmartRole.class)
					.add(Restrictions.eq("isSystem", false)).list(); //$NON-NLS-1$
			
			List<SmartActionsProxy> actionResources = new ArrayList<SmartActionsProxy>();
			for (SmartRole r : roles){
				SmartActionsProxy proxy = new SmartActionsProxy(r.rolename, r.roleId);
				actionResources.add(proxy);
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
	 * Lists all roles and actions associated with a given user.
	 * 
	 * @param username
	 * @return
	 */
	@GET
    @Path("/user/{username}")
    public List<SmartUserPermissionProxy> getUserPrivileges(@PathParam("username") String username){
		validateUser();
		
		Session s = HibernateManager.getSession(context, SmartUtils.getRequestLocale(request));
		s.beginTransaction();
		try{
			@SuppressWarnings("unchecked")
			List<SmartUserAction> actions = s.createCriteria(SmartUserAction.class)
					.add(Restrictions.eq("username", username)) //$NON-NLS-1$
					.list();
			
			List<SmartUserPermissionProxy> items = new ArrayList<SmartUserPermissionProxy>();
			for (SmartUserAction a : actions){
				SmartUserPermissionProxy p = new SmartUserPermissionProxy(SmartUserPermissionProxy.Type.ACTION);
				p.setKey(a.getAction());
				p.setResource(a.getResource());
				
				ISmartConnectAction action = ActionManager.INSTANCE.findAction(a.getAction());
				p.setName(action.getActionName(a.getAction(), SmartUtils.getRequestLocale(request)));
				if (a.getResource() != null){
					p.setResourceName(action.getResourceName(a.getResource(), s, SmartUtils.getRequestLocale(request)));
				}
				items.add(p);
			}
			
			@SuppressWarnings("unchecked")
			List<SmartUserRole> roles = s.createCriteria(SmartUserRole.class)
					.add(Restrictions.eq("id.username", username)) //$NON-NLS-1$
					.list();
			
			for (SmartUserRole r : roles){
				if (!r.getRole().isSystem){
					SmartUserPermissionProxy p = new SmartUserPermissionProxy(SmartUserPermissionProxy.Type.ROLE);
					p.setKey(r.getRole().getRoleId());
					p.setName(r.getRole().getRoleName());
					items.add(p);
				}
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
    @Path("/user/{username}/action/{action}")
    public void deleteUserAction(@PathParam("username") String username,
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
    @Path("/user/{username}/action/{action}/{resource}")
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
			SecurityManager.INSTANCE.validateSingleAdminUser(s, SmartUtils.getRequestLocale(request));
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.UserDeleteError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}	
	}
	
	/**
	 * Creates a new role. Uses the name property of the action
	 * object and generated a new uuid string as the role id;
	 * 
	 * @param username
	 * @param action
	 */
	@POST
    @Path("/roles")
    public void createRole(SmartUserPermissionProxy action){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = new SmartRole();
			role.setRoleId(UUID.randomUUID().toString().replaceAll("-","")); //$NON-NLS-1$ //$NON-NLS-2$
			role.setIsSystem(false);
			role.setRoleName(action.getName());
			s.save(role);
			s.getTransaction().commit();
			
			response.setStatus(Response.Status.CREATED.getStatusCode());
			response.flushBuffer();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.CreateRoleError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
	
	/**
	 * Updates the role name. Uses the name property of the action
	 * object and generated a new uuid string as the role id;
	 * 
	 * @param username
	 * @param action
	 */
	@PUT
    @Path("/roles/{roleid}")
    public void updateRole(@PathParam("roleid") String roleid,
    		SmartUserPermissionProxy action){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = (SmartRole) s.get(SmartRole.class, roleid);
			if (role == null){
				throw new SmartConnectException(Status.NOT_FOUND);
			}
			role.setRoleName(action.getName());
			s.save(role);
			s.getTransaction().commit();
			
			response.setStatus(Response.Status.CREATED.getStatusCode());
			response.flushBuffer();
		
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			if (ex instanceof SmartConnectException) throw (SmartConnectException)ex;
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.UpdateRoleError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
	/**
	 * Removes a role from the system, includes removing any user
	 * links to the role.
	 * 
	 * @param username
	 * @param action
	 */
	@DELETE
    @Path("/roles/{roleid}")
    public void deleteRole(@PathParam("roleid") String roleid){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = (SmartRole) s.get(SmartRole.class, roleid);
			if (role == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			s.delete(role);
			
			s.flush();
			
			// we need at least one admin user
			SecurityManager.INSTANCE.validateSingleAdminUser(s, SmartUtils.getRequestLocale(request));
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.DeleteRoleError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}	
	}
	
	/**
	 * Removes an action from a given role.
	 * 
	 * @param username
	 * @param action
	 */
	@DELETE
    @Path("/roles/{roleid}/action/{action}")
    public void deleteRoleAction(@PathParam("roleid") String roleid,
    		@PathParam("action") String action){
		deleteRoleActions(roleid, action, null);
	}
	
	/**
	 * Removes an action and specific resource from a given role.
	 * 
	 * @param username
	 * @param action
	 * @param resource
	 */
	@DELETE
    @Path("/roles/{roleid}/action/{action}/{resource}")
    public void deleteRoleActions(@PathParam("roleid") String roleid,
    		@PathParam("action") String action,
    		@PathParam("resource") String resource){
		
		validateUser();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = (SmartRole) s.get(SmartRole.class, roleid);
			if (role == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			
			@SuppressWarnings("unchecked")
			List<SmartRoleAction> actions = s.createCriteria(SmartRoleAction.class)
					.add(Restrictions.eq("role", role)) //$NON-NLS-1$
					.add(Restrictions.eq("action", action)) //$NON-NLS-1$
					.list();
			
			for(SmartRoleAction a : actions){
				if ((resource == null && a.getResource() == null) ||
					(a.getResource() != null && a.getResource().toString().equals(resource))){		
					s.delete(a);
				}
			}
			s.flush();
			
			// we need at least one admin user
			SecurityManager.INSTANCE.validateSingleAdminUser(s, SmartUtils.getRequestLocale(request));
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.DeleteRoleActionError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}	
	}
	
	/**
	 * Adds a new action for a given user.
	 * @param username
	 * @param actionKey
	 */
	@POST
    @Path("/user/{username}/action/{action}")
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
    @Path("/user/{username}/action/{action}/{resource}")
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
			if(ex instanceof ConstraintViolationException){
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
						Messages.getString("ConnectUserAction.UserAddErrorDuplicate", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
			}else{
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
						Messages.getString("ConnectUserAction.UserAddError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Gets all actions associated with a given role
	 * @param roleid
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/roles/{roleid}/action")
    public List<SmartUserPermissionProxy> getRoleActions(@PathParam("roleid") String roleid){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = (SmartRole) s.get(SmartRole.class, roleid);
			if (role == null){
				return null;
			}
			List<SmartUserPermissionProxy> privis = new ArrayList<SmartUserPermissionProxy>();
			
			List<SmartRoleAction> actions = s.createCriteria(SmartRoleAction.class)
					.add(Restrictions.eq("role", role)) //$NON-NLS-1$
					.list();
			for (SmartRoleAction a : actions){
				SmartUserPermissionProxy proxy = new SmartUserPermissionProxy(Type.ACTION);
				proxy.setKey(a.getAction());
				proxy.setResource(a.getResource());
				
				ISmartConnectAction action = ActionManager.INSTANCE.findAction(a.getAction());
				proxy.setName(action.getActionName(a.getAction(), SmartUtils.getRequestLocale(request)));
				if (a.getResource() != null){
					proxy.setResourceName(action.getResourceName(a.getResource(), s, SmartUtils.getRequestLocale(request)));
				}
				privis.add(proxy);
			}
			
			return privis;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR);
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	
	/**
	 * Adds a new action to a role.
	 * @param username
	 * @param actionKey
	 */
	@POST
    @Path("/roles/{roleid}/action/{action}")
    public void addRoleAction(@PathParam("roleid") String roleid,
    		@PathParam("action") String actionKey){
		addRoleActions(roleid, actionKey, null);
	}
	
	/**
	 * Add a new action with a specific resource to a role
	 * @param username
	 * @param actionKey
	 * @param resourceKey
	 */
	@POST
    @Path("/roles/{roleid}/action/{action}/{resource}")
    public void addRoleActions(@PathParam("roleid") String roleid,
    		@PathParam("action") String actionKey,
    		@PathParam("resource") String resourceKey){
		validateUser();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = (SmartRole) s.get(SmartRole.class, roleid);
			if (role == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUserAction.RoleNotFound1", SmartUtils.getRequestLocale(request)), roleid)); //$NON-NLS-1$
			}
			
			SmartRoleAction newaction = new SmartRoleAction();
			newaction.setAction(actionKey);
			newaction.setRole(role);
			if (resourceKey != null){
				newaction.setResource(UUID.fromString(resourceKey));
			}
			s.save(newaction);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			if(ex instanceof ConstraintViolationException){
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
						Messages.getString("ConnectUserAction.AddActionError1", SmartUtils.getRequestLocale(request)), ex);  //$NON-NLS-1$
			}else{
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
						Messages.getString("ConnectUserAction.AddActionError", SmartUtils.getRequestLocale(request)), ex);  //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Removes an role from a given user.
	 * 
	 * @param username
	 * @param action
	 */
	@SuppressWarnings("unchecked")
	@DELETE
    @Path("/user/{username}/role/{role}")
    public void deleteUserRole(@PathParam("username") String username,
    		@PathParam("role") String role){
    		
		validateUser();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			Query q = s.createQuery("FROM SmartUserRole r WHERE r.id.username = :username and r.id.role.roleId = :roleId"); //$NON-NLS-1$
			q.setParameter("username", username); //$NON-NLS-1$
			q.setParameter("roleId", role); //$NON-NLS-1$
			
			List<SmartUserRole> roles = q.list();
			for(SmartUserRole a : roles){
				s.delete(a);
			}
			s.flush();
			
			// we need at least one admin user
			SecurityManager.INSTANCE.validateSingleAdminUser(s, SmartUtils.getRequestLocale(request));
			
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("ConnectUserAction.DeleteUserRoleError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}	
	}
	
	
	/**
	 * Adds a new action for a given user.
	 * @param username
	 * @param actionKey
	 */
	@POST
    @Path("/user/{username}/role/{role}")
    public void addUserRole(@PathParam("username") String username,
    		@PathParam("role") String roleId){
	
		validateUser();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			SmartRole role = (SmartRole) s.get(SmartRole.class, roleId);
			if (role == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format(Messages.getString("ConnectUserAction.RoleDoesNotExist", SmartUtils.getRequestLocale(request)), roleId));  //$NON-NLS-1$
			}
			
			SmartUserRole newrole = new SmartUserRole();
			newrole.setRole(role);
			newrole.setUsername(username);
			
			s.save(newrole);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			if (ex instanceof SmartConnectException ){
				throw ex;
			}
			if(ex instanceof ConstraintViolationException){
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR,
						Messages.getString("ConnectUserAction.RoleAddError1", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
			}
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
						Messages.getString("ConnectUserAction.RoleAddError", SmartUtils.getRequestLocale(request)), ex);  //$NON-NLS-1$
			
		}
	}
	

}
