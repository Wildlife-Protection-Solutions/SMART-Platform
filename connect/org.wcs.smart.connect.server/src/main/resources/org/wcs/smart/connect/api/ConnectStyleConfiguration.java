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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.StyleConfiguration;
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
		
	
	/**
	 * Get Style Configuration
	 * URL: ../server/api/connectstyle/
	 * Call Type: GET
	 * 
	 * @return Returns a JSON StyleConfiguration object. ( https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/model/StyleConfiguration.java ) 
	 */
	
	@GET
    @Path("")
	@Produces({ MediaType.APPLICATION_JSON })
    public StyleConfiguration getStyleConfiguration(){
		
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
	
	/**
	 * Create a Style Configuration
	 * URL: ../server/api/connectstyle/
	 * Call Type: POST
	 * Payload: Multi-part form data where attributes match the object of attributes that match the Java attributes you wish to update, EX:
	 * 		
	 * ------WebKitFormBoundaryAxRJfYeZhy4BeRWV
	 * Content-Disposition: form-data; name="server_name"
	 * 
	 * SMART Connect <img src='https://smartconservationsoftware.org/photos/smart_logo_96.png'>
	 * ------WebKitFormBoundaryAxRJfYeZhy4BeRWV
	 * Content-Disposition: form-data; name="footer_text"
	 * <img src='../css/smart_logo.png'> Copyright 2015, 2016
	 * ------WebKitFormBoundaryAxRJfYeZhy4BeRWV
	 * Content-Disposition: form-data; name="header_style"
	 * 
	 * border: 0px solid black;
	 * padding: 10px 5px 10px 5px;
	 * ------WebKitFormBoundaryAxRJfYeZhy4BeRWV
	 * Content-Disposition: form-data; name="body_style"
	 * margin: 0px;
	 * background-color: lightsteelblue
	 * ------WebKitFormBoundaryAxRJfYeZhy4BeRWV
	 * Content-Disposition: form-data; name="header_image"; filename="header.jpg"
	 * Content-Type: image/jpeg
	 * JFIF  H H   .....binary image....
	 * 
	 * ------WebKitFormBoundaryAxRJfYeZhy4BeRWV--
	 * 
	 * 
	 * 
	 * @return Returns a JSON StyleConfiguration object for the created style configuration 
	 */
	@POST
    @Path("")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    public StyleConfiguration addStyleConfiguration(MultipartFormDataInput input) {
		validateUser(AlertAction.CREATE_ALERTS_KEY);
		byte[] bg_image, header_image, login_image = null;
		String body_style = ""; //$NON-NLS-1$
		String header_style = ""; //$NON-NLS-1$
		String footer_text= ""; //$NON-NLS-1$
		String server_name =""; //$NON-NLS-1$
		
		try{
			InputStream in = input.getFormDataMap().get("bg_image").get(0).getBody(InputStream.class,null); //$NON-NLS-1$
			bg_image = IOUtils.toByteArray(in);
			
			InputStream in2 = input.getFormDataMap().get("header_image").get(0).getBody(InputStream.class,null); //$NON-NLS-1$
			header_image = IOUtils.toByteArray(in2);
			
			InputStream in3 = input.getFormDataMap().get("login_image").get(0).getBody(InputStream.class,null); //$NON-NLS-1$
			login_image = IOUtils.toByteArray(in3);
			
			header_style = input.getFormDataMap().get("header_style").get(0).getBodyAsString(); //$NON-NLS-1$
			body_style = input.getFormDataMap().get("body_style").get(0).getBodyAsString(); //$NON-NLS-1$
			footer_text= input.getFormDataMap().get("footer_text").get(0).getBodyAsString(); //$NON-NLS-1$
			server_name = input.getFormDataMap().get("server_name").get(0).getBodyAsString(); //$NON-NLS-1$


	    }catch (Exception ex){
	    	throw new SmartConnectException(ex.getMessage(), ex);
		}
		  
		StyleConfiguration  newStyle = new StyleConfiguration();

		newStyle.setActive(true);
		newStyle.setHeaderImage(header_image);
		newStyle.setBackgroundImage(bg_image);
		newStyle.setLoginImage(login_image);

		newStyle.setStyleId("The Style"); //$NON-NLS-1$
		newStyle.setBodyStyle(body_style);
		newStyle.setHeaderStyle(header_style);
		newStyle.setFooterText(footer_text);
		newStyle.setServerName(server_name);
		
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
	
	/**
	 * Edit Style Configuration
	 * URL: ../server/api/connectstyle/
	 * Call Type: PUT
	 * Payload: Multi-part form data where attributes match the object of attributes that match the Java attributes you wish to update
	 * see the docs for creating a new style for an example.
	 * 
	 * @return Returns a JSON StyleConfiguration object for the created style configuration 
	 */
	@PUT
    @Path("")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
    public StyleConfiguration editStyleConfiguration(MultipartFormDataInput input) {
		validateUser(AlertAction.CREATE_ALERTS_KEY);
		byte[] bg_image, header_image, login_image = null;
		String body_style = ""; //$NON-NLS-1$
		String header_style = ""; //$NON-NLS-1$
		String footer_text= ""; //$NON-NLS-1$
		String server_name =""; //$NON-NLS-1$
		
		StyleConfiguration style;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			style = HibernateManager.getStyleConfiguration(s);
		}catch (Exception e){
			throw e;
		}finally{
			s.getTransaction().commit();
		}
		
		try{
			InputStream in = input.getFormDataMap().get("bg_image").get(0).getBody(InputStream.class,null); //$NON-NLS-1$
			bg_image = IOUtils.toByteArray(in);
			
			InputStream in2 = input.getFormDataMap().get("header_image").get(0).getBody(InputStream.class,null); //$NON-NLS-1$
			header_image = IOUtils.toByteArray(in2);
			
			InputStream in3 = input.getFormDataMap().get("login_image").get(0).getBody(InputStream.class,null); //$NON-NLS-1$
			login_image = IOUtils.toByteArray(in3);
			
			header_style = input.getFormDataMap().get("header_style").get(0).getBodyAsString(); //$NON-NLS-1$
			body_style = input.getFormDataMap().get("body_style").get(0).getBodyAsString(); //$NON-NLS-1$
			footer_text= input.getFormDataMap().get("footer_text").get(0).getBodyAsString(); //$NON-NLS-1$
			server_name = input.getFormDataMap().get("server_name").get(0).getBodyAsString(); //$NON-NLS-1$

	    }catch (Exception ex){
	    	throw new SmartConnectException(ex.getMessage(), ex);
		}

		if(header_image.length > 0){
			style.setHeaderImage(header_image);
		}
		if(bg_image.length > 0){
			style.setBackgroundImage(bg_image);
		}
		if(login_image.length > 0){
			style.setLoginImage(login_image);
		}

		style.setBodyStyle(body_style);
		style.setHeaderStyle(header_style);
		style.setFooterText(footer_text);
		style.setServerName(server_name);

		
		s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.update(style);
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
		
		return style;
	}
	
	/**
	 * Delete Style Configuration
	 * URL: ../server/api/connectstyle/
	 * Call Type: DELETE
	 * 
	 * @return Returns a JSON StyleConfiguration object for the deleted style configuration 
	 */	
    @DELETE
    @Path("/")
    public StyleConfiguration removeStyleConfiguration() {
    	validateUser(AlertAction.DELETE_ALERTS_KEY);
    	StyleConfiguration toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getStyleConfiguration(s);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, ""); //$NON-NLS-1$
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
