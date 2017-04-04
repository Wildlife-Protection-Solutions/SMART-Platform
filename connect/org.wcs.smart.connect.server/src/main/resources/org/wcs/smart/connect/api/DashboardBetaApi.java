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

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.Dashboard;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.UsersDefaultDashboard;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * Smart Connect REST API for Dashboards
 * 
 * @author Jeff
 *
 */


@Path(ConnectRESTApplication.PATH_SEPERATOR + DashboardBetaApi.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class DashboardBetaApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = Logger.getLogger(DashboardBetaApi.class.getName());
	
	public static final String PATH = "dashboardbeta"; //$NON-NLS-1$
	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	
	//this function throws an unauthorized exception if the user is not an admin 
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
	

	/**
	 * Get the Default Dashboard for the current user 
	 * URL: ../server/api/dashboardbeta/default/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON representation of a UsersDefaultDashboard object 
	 */
	@GET
    @Path("/default/")
    public Dashboard getDefaultDashboard(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		SmartUser user;
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			if(user == null) throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			UsersDefaultDashboard dd = (UsersDefaultDashboard)s.createCriteria(UsersDefaultDashboard.class).add(Restrictions.eq("userUuid",user.getUuid())).uniqueResult();
			if(dd == null){
				return new Dashboard();
			}
			Dashboard dashboard = (Dashboard)s.createCriteria(Dashboard.class).add(Restrictions.eq("uuid",dd.getDashboardUuid())).uniqueResult();
			

			//override the user-customizable portions of the dashboard then return to the user.
			dashboard.setCustomDate1From(dd.getCustomDate1From());
			dashboard.setCustomDate1To(dd.getCustomDate1To());
			dashboard.setCustomDate2From(dd.getCustomDate2From());
			dashboard.setCustomDate2To(dd.getCustomDate2To());
			dashboard.setDateRange1(dd.getDateRange1());
			dashboard.setDateRange2(dd.getDateRange2());

			return dashboard; 
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	
	/**
	 * Get all DashBoards in the system 
	 * URL: ../server/api/dashboardbeta/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON representation of a list of Quicklink objects 
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/")
    public List<Dashboard> getAllDashboards(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return s.createCriteria(Dashboard.class).list();
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * Get a DashBoards - must be an admin to use this function 
	 * URL: ../server/api/dashboardbeta/{uuid}
	 * Call Type: GET
	 * 
	 * @return Returns a JSON representation of a Dashboard
	 */
	@GET
    @Path("/{uuid}")
    public Dashboard getDashboard(@PathParam("uuid") UUID uuid){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return (Dashboard) s.createCriteria(Dashboard.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	
	/**
	 * Create or Update a new Dashboard - must be an admin to do this
	 * URL: ../server/api/dashboardbeta
	 * Call Type: POST
	 * Payload: A GeoJSON object that has properties that match the Java attributes of a DashBoard
	 * <p> Examples: {"reportUuid1":"c9e4fcee-8a9d-44e0-997a-8142e79619c7","reportUuid2":"bc6a73c4-105f-407c-8bf6-c5a5847a510b","dateRange1":"1","dateRange2":"1","customDate1":"","customDate2":"","parameterList1":"","parameterList2":"","label":"New Dashboard"}
	 * 
	 * @return Returns a JSON Dashboard object for the created Dashboard
	 */
	
	@POST
    @Path("/")
    public Dashboard addOrUpdateDashboard(Dashboard dashboard) {
		isAdminUser();
		Dashboard d = null;
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{

			if(dashboard.getUuid() != null){
				d = (Dashboard) s.createCriteria(Dashboard.class).add(Restrictions.eq("uuid", dashboard.getUuid())).uniqueResult();
			}
			
			if(d == null){
				d = new Dashboard();
			}
		
			d.setCustomDate1From(dashboard.getCustomDate1From());
			d.setCustomDate1To(dashboard.getCustomDate1To());
			d.setCustomDate2From(dashboard.getCustomDate2From());
			d.setCustomDate2To(dashboard.getCustomDate2To());
			d.setDateRange1(dashboard.getDateRange1());
			d.setDateRange2(dashboard.getDateRange2());
			d.setLabel(dashboard.getLabel().trim());
			d.setParameterList1(dashboard.getParameterList1());
			d.setParameterList2(dashboard.getParameterList2());
			d.setReportUuid1(dashboard.getReportUuid1());
			d.setReportUuid2(dashboard.getReportUuid2());
		
			s.save(d);
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
		
		return d;
	}
	
	
	/**
	 * Delete a DashBoard - must be an admin to use this function 
	 * URL: ../server/api/dashboardbeta/{uuid}
	 * Call Type: DELETE
	 * 
	 * @return Returns a JSON representation of Dashboard object, the one you just deleted 
	 */
	@DELETE
    @Path("/{uuid}")
    public Dashboard deleteDashboard(@PathParam("uuid") UUID uuid){
		isAdminUser();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			Dashboard toDelete = (Dashboard)s.createCriteria(Dashboard.class).add(Restrictions.eq("uuid", uuid)).uniqueResult();
			s.delete(toDelete);
			s.getTransaction().commit();
			return toDelete;
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * Set the User's Default Dashboard to one passed in, used when users want to update their date and parameter settings.
	 * URL: ../server/api/dashboardbeta/default
	 * Call Type: PUT
	 * Payload: A GeoJSON object that has properties that match the Java attributes of a UsersDefaultDashboard
	 * <p> Example:
	 * 
	 * @return Returns a JSON UsersDefaultDashboard object, the one just created. 
	 */
	
	@PUT
    @Path("/default/")
    public Dashboard setUserDefault(UsersDefaultDashboard userDefault) {
		UsersDefaultDashboard d;
		Dashboard dashboard;
		SmartUser user;
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			if(user == null) throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			
			d = (UsersDefaultDashboard) s.createCriteria(UsersDefaultDashboard.class).add(Restrictions.eq("userUuid", user.getUuid())).uniqueResult();
			if(d == null){
				d = new UsersDefaultDashboard();
				d.setUserUuid(user.getUuid());
			}

			if(userDefault.getDashboardUuid() != null){
				d.setDashboardUuid(userDefault.getDashboardUuid());
			}
			if(userDefault.getCustomDate1From() != null){
				d.setCustomDate1From(userDefault.getCustomDate1From());
			}
			if(userDefault.getCustomDate1To() != null){
				d.setCustomDate1To(userDefault.getCustomDate1To());
			}
			if(userDefault.getCustomDate2From() != null){
				d.setCustomDate2From(userDefault.getCustomDate2From());
			}
			if(userDefault.getCustomDate2To() != null){
				d.setCustomDate2To(userDefault.getCustomDate2To());
			}
			if(userDefault.getDateRange1() != 0){
				d.setDateRange1(userDefault.getDateRange1());
			}
			if(userDefault.getDateRange2() != 0){
				d.setDateRange2(userDefault.getDateRange2());
			}
		
			s.saveOrUpdate(d);
			
			//override the user-customizable portions of the dashboard then return to the user Dashboard object.
			dashboard = (Dashboard)s.createCriteria(Dashboard.class).add(Restrictions.eq("uuid",d.getDashboardUuid())).uniqueResult();
			
			dashboard.setCustomDate1From(d.getCustomDate1From());
			dashboard.setCustomDate1To(d.getCustomDate1To());
			dashboard.setCustomDate2From(d.getCustomDate2From());
			dashboard.setCustomDate2To(d.getCustomDate2To());
			dashboard.setDateRange1(d.getDateRange1());
			dashboard.setDateRange2(d.getDateRange2());

			response.setStatus(Response.Status.CREATED.getStatusCode());
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
		
		return dashboard;
	}
	
	
	/**
	 * Set the User's Default Dashboard to an existing dashboard
	 * URL: ../server/api/dashboardbeta/default/{uuid}
	 * Call Type: PUT
	 * Payload: A GeoJSON object that has properties that match the Java attributes of a UsersDefaultDashboard
	 * <p> Example:
	 * 
	 * @parameter uuid - provided in the url, the uuid of the dashboard they want as the new default.
	 * @return Returns a JSON UsersDefaultDashboard object, the one just created. 
	 */
	
	
	@PUT
    @Path("/default/{uuid}")
    public Dashboard setUserDefault(@PathParam("uuid") UUID uuid) {
		UsersDefaultDashboard d;
		Dashboard dashboard;
		SmartUser user;
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			if(user == null) throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			
			d = (UsersDefaultDashboard) s.createCriteria(UsersDefaultDashboard.class).add(Restrictions.eq("userUuid", user.getUuid())).uniqueResult();
			dashboard = (Dashboard)s.createCriteria(Dashboard.class).add(Restrictions.eq("uuid",uuid)).uniqueResult();
			if(d == null){
				d = new UsersDefaultDashboard();
				d.setUserUuid(user.getUuid());
			}

			d.setDashboardUuid(dashboard.getUuid());
			d.setCustomDate1From(dashboard.getCustomDate1From());
			d.setCustomDate1To(dashboard.getCustomDate1To());
			d.setCustomDate2From(dashboard.getCustomDate2From());
			d.setCustomDate2To(dashboard.getCustomDate2To());
			d.setDateRange1(dashboard.getDateRange1());
			d.setDateRange2(dashboard.getDateRange2());
		
			s.saveOrUpdate(d);
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
		
		return dashboard;
	}

}
