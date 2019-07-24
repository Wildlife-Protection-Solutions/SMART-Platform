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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.GlobalForestWatch;
import org.wcs.smart.connect.model.GlobalForestWatchProxy;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * API for configuring global forest watch  
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + GlobalForestWatchApi.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class GlobalForestWatchApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(GlobalForestWatchApi.class.getName());
	
	public static final String PATH = "gfw"; //$NON-NLS-1$

	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
    /**
	 * <p>Get all GlobalForestWatch configurations</p>
	 * <p>
	 * URL: ../server/api/gfw<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @return Returns a JSON Array of GlobalForestWatchProxy objects 
	 * 
	 */
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Get all GlobalForestWatch configurations")
    public List<GlobalForestWatchProxy> getGFWSettings(){
		List<GlobalForestWatchProxy> proxies = new ArrayList<GlobalForestWatchProxy>();
		
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			 
			List<GlobalForestWatch> fws = QueryFactory.buildQuery(s, GlobalForestWatch.class).list();
			fws.forEach(fw->{
				proxies.add(new GlobalForestWatchProxy(fw, request));
			});
		}catch (Exception ex) {
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage(),ex);
		}finally {
			s.getTransaction().rollback();
		}
		return proxies;
	}
	
	/**
	 * <p>
	 * Create a new all GlobalForestWatch configurations
	 * </p>
	 * <p>
	 * URL: ../server/api/gfw<br>
	 * Call Type: POST
	 * </p>
	 * 
	 * @param gfw the JSON representation of a GlobalForestWatchProxy object 
	 * @return Returns a JSON Array of representing the GlobalForestWatchProxy object created
	 * 
	 */
	@POST
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Create a new all GlobalForestWatch configurations")
    public GlobalForestWatchProxy createGFW(GlobalForestWatchProxy gfw,  @Context final HttpServletResponse response){
		
		if (gfw.getAlertUuid() == null) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.AlertTypeRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		GlobalForestWatchProxy proxy = null;
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			
			AlertType type = s.get(AlertType.class, gfw.getAlertUuid());
			if (type == null) {
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.AlertTypeRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			if (gfw.getLevel() < 1 || gfw.getLevel() > 5) {
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.InvalidAlertLevel", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			SmartUser creator = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			
			GlobalForestWatch g = new GlobalForestWatch();
			g.setAlertType(type);
			g.setLastDataDate(null);
			g.setCreator(creator);
			g.setLevel(gfw.getLevel());
			s.saveOrUpdate(g);
			s.getTransaction().commit();
			
			proxy = new GlobalForestWatchProxy(g, request);
		}catch(SmartConnectException ex) {
			s.getTransaction().rollback();
			proxy  = null;
			throw ex;
		}catch (Exception ex) {
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			s.getTransaction().rollback();
			proxy = null;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage(),ex);
		}
		response.setStatus(HttpServletResponse.SC_CREATED);
		try {
	        response.flushBuffer();
	    }catch(Exception e){}
		return proxy;
	}
	
	/**
	 * <p>
	 * Delete a GlobalForestWatch configuration
	 * </p>
	 * <p>
	 * URL: ../server/api/gfw/{gfwUuid}<br>
	 * Call Type: DELETE
	 * </p>
	 * 
	 * @param gfwUuuid ths uuid of the GlobalForestWatch configuration to delete 
	 * 
	 */
	@DELETE
	@Path("/{gfwUuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Delete a GlobalForestWatch configuration")
    public void deleteGFW(@Parameter(description="the gfwUUID to delete") @PathParam("gfwUuid") UUID gfwUuid) {
		if (gfwUuid == null) {
			throw new SmartConnectException(Status.BAD_REQUEST);
		}
		
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			GlobalForestWatch w = s.get(GlobalForestWatch.class, gfwUuid);
			if (w == null) {
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.AlertTypeRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			s.delete(w);
			s.getTransaction().commit();
		}catch(SmartConnectException ex) {
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex) {
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage(),ex);
		}
	}
	
	@POST
	@Path("/{gfwUuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Update Global Forestwatch configuration")
    public GlobalForestWatchProxy updateGFW(@Parameter(description="the GFWuuid to update") @PathParam("gfwUuid") UUID gfwUuid, GlobalForestWatchProxy gfw){
		
		if (gfw.getAlertUuid() == null) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.AlertTypeRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (gfw.getLevel() < 1 || gfw.getLevel() > 5) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.InvalidAlertLevel", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		GlobalForestWatchProxy proxy = null;
		Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale());
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			
			AlertType type = s.get(AlertType.class, gfw.getAlertUuid());
			if (type == null) {
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.AlertTypeRequired", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			GlobalForestWatch g = s.get(GlobalForestWatch.class, gfwUuid);
			if (g == null) {
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchApi.GfwNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			g.setLevel(gfw.getLevel());
			g.setAlertType(type);
			g.setLastDataDate(null);
			s.saveOrUpdate(g);
			s.getTransaction().commit();
			
			proxy = new GlobalForestWatchProxy(g, request);
		}catch(SmartConnectException ex) {
			s.getTransaction().rollback();
			proxy  = null;
			throw ex;
		}catch (Exception ex) {
			logger.log(Level.SEVERE,ex.getMessage(),ex);
			s.getTransaction().rollback();
			proxy = null;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage(),ex);
		}
		return proxy;
	}
}
