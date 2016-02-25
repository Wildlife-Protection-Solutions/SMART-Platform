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

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
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

import org.apache.commons.io.IOUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.filter.AlertFilter;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.GeoJsonAlert;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * Smart Connect Style Configuration API for GUI styling
 * 
 * @author Jeff
 *
 */


@Path(ConnectRESTApplication.PATH_SEPERATOR + ConnectStyleConfiguration.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class ConnectStyleConfiguration extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(ConnectStyleConfiguration.class.getName());
	
	public static final String PATH = "connectstyle"; //$NON-NLS-1$
	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	
	/*
	 * Validates the current user has the permission to the provided action
	 * 1 parameter - the action type to test for permission
	 * 
	 * The CanAccess function automatically returns yes for users that have Admin rights
	 * You can also pass in AdminAccountAction.KEY to this function, even though it is a bit redundant  
	 */
	private void validateUser(String key){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), key)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have permissions for this request."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
		}finally{
			s.getTransaction().commit();
		}
	}
		
	@GET
    @Path("")

    public StyleConfiguration getStyleConfiguration(){
		validateUser(AlertAction.VIEW_ALL_KEY);
		
		StyleConfiguration style;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			style = HibernateManager.getStyleConfiguration(s);
			return style;
		}catch (Exception e){
			throw e;
		}finally{
			s.getTransaction().commit();
		}
	}
	
	@POST
    @Path("")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    public StyleConfiguration addStyleConfiguration(MultipartFormDataInput input) {
		validateUser(AlertAction.CREATE_ALERTS_KEY);
		byte[] bg_image = null;
		String id = "";
		
		try{
			InputStream in = input.getFormDataMap().get("bg_image").get(0).getBody(InputStream.class,null);
			bg_image = IOUtils.toByteArray(in);
					  
		      //bg_image = input.getFormDataMap().get("bg_image").get(0).getStrig().getBytes();
		      id = input.getFormDataMap().get("style_id").get(0).getBodyAsString();

	    }catch (Exception ex){
	    	throw new SmartConnectException(ex.getMessage(), ex);
		}
		  
		StyleConfiguration  newStyle = new StyleConfiguration();

		newStyle.setActive(true);
		newStyle.setBackgroundImage(bg_image);
		newStyle.setStyleId(id);
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.save(newStyle);
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
		
		return newStyle;
	}

	
    @DELETE
    @Path("/")
    public StyleConfiguration removeStyleConfiguration() {
    	validateUser(AlertAction.DELETE_ALL_KEY);
    	StyleConfiguration toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getStyleConfiguration(s);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("ConnectStyle.StyleNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
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
		}finally{
		}
		return toDelete;
    }
    
}
