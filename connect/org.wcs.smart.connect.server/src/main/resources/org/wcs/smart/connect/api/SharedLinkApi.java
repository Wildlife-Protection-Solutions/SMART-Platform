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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.AdvIntelAction;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.QueryAction;
import org.wcs.smart.connect.security.ReportAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * SMART Connect ShareLink REST API - allows you to create and disable shared links (aka: sessions aka: tokens)
 * 
 * @author  Jeff
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + SharedLinkApi.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class SharedLinkApi extends HttpServlet{
	
	public static final String PATH = "sharedlink"; //$NON-NLS-1$

	/**
	 * The api key parameter for authentication using a token 
	 */
	public static final String TOKEN_QUERY_PARAM = "token"; //$NON-NLS-1$

	
	private final Logger logger = Logger.getLogger(SharedLinkApi.class.getName());
	
	private static final long serialVersionUID = 1L;
	
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

	private boolean isCaAdminUser(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			return SecurityManager.INSTANCE.isCaAdmin(s, request.getUserPrincipal().getName());
		}finally{
			s.getTransaction().commit();
		}
	}
	
	private String getUserName(UUID uuid, Session session) {
		if (uuid == null) return ""; //$NON-NLS-1$
		SmartUser user = session.get(SmartUser.class, uuid);
		if (user == null) return ""; //$NON-NLS-1$
		return user.getUsername();
	}
	
	/**
	 * <p>List all Shared Links</p>
	 * <p>
	 * URL: ../server/api/sharedlink/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @return Returns a JSON array of SharedLink objects
	 */
	@GET
    @Path("/")
	@Operation(description="List all Shared Links")
    public List<SharedLink> getSharedLinks(){
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if(SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				//admins can see all shared links
				List<SharedLink> links =  QueryFactory.buildQuery(s, SharedLink.class).list();
				for (SharedLink l : links){
					l.setPermissionUsername(getUserName(l.getPermissionUserUuid(), s));
					l.setOwnerUsername(  getUserName(l.getOwnerUuid(), s) );
				}
				return links;
			}
			if(SecurityManager.INSTANCE.isCaAdmin(s, request.getUserPrincipal().getName())){
				//only return links from the CA(s) they are CaAdmin users for
				List<SmartUserAction> list = QueryFactory.buildQuery(s, SmartUserAction.class, 
						new Object[] {"username", request.getUserPrincipal().getName()}, //$NON-NLS-1$
						new Object[] {"action", CaAdminAccountAction.KEY}).list(); //$NON-NLS-1$
				List<SharedLink> links = new ArrayList<SharedLink>();
				for(SmartUserAction a : list ){//loop over each CA they are admins of
					UUID caUuid = a.getResource();
					
					List<SharedLink> temp = QueryFactory.buildQuery(s, SharedLink.class, "conservationArea", caUuid).list();  //$NON-NLS-1$
					for(SharedLink t : temp){//add all shared links from this CA
						links.add(t);
						t.setPermissionUsername(getUserName(t.getPermissionUserUuid(), s));
						t.setOwnerUsername(  getUserName(t.getOwnerUuid(), s) );
					}
				}
				//add links for tokens without CA, but for which they are the owner
				
				List<SharedLink> temp = QueryFactory.buildQuery(s, SharedLink.class, 
						new Object[] {"conservationArea", null}, //$NON-NLS-1$
						new Object[] {"ownerUuid", HibernateManager.getUser(s, request.getUserPrincipal().getName()).getUuid()}).list();  //$NON-NLS-1$
				for(SharedLink t : temp){//add all shared links from this CA
					links.add(t);
					t.setPermissionUsername(getUserName(t.getPermissionUserUuid(), s));
					t.setOwnerUsername(  getUserName(t.getOwnerUuid(), s) );
				}
				return links;
			}
			
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("SharedLinks.ListAllError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * <p>Create a new shared link. Expired Links are also cleaned up (deleted) when a new link is created.</p>
	 * 
	 * <p>
	 * URL: ../server/api/sharedlink/<br>
	 * Call Type: POST<br>
	 * Payload: A JSON object of the 2 required attributes: expiresAfter and url
	 * </p> 
	 * 	<pre>{
	 *   expiresAfter: 525600, 
	 *   url: "query/api/5e4b8ab8-1313-42b4-886c-ff5b7db8ec65?format=csv&date_filter=waypointdate"
	 *}</pre>  
	 * 	
	 * The above link would expire e in 1 year.  (expiresAfter is represented in minutes)
	 * 
	 * @param newLink - JSON representation of SharedLink object
	 * @return a JSON SharedLink object for the created user, the link_uuid attribute is what you need to put into URLs. 
	 */
	@POST
    @Path("/")
	@Operation(description="Create a new shared link. Expired Links are also cleaned up (deleted) when a new link is created.")
    public SharedLink createSharedLink(SharedLink newLink) {
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			//is this a valid user at all, check first before even looking at the url and 
			if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY)){
				logger.info("User " + request.getUserPrincipal().getName() + " does not have user accounts permissions."); //$NON-NLS-1$ //$NON-NLS-2$
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}

			//some verification the URL is valid before we just accept any URL that users want to circumvent basic auth for.
			Pattern pattern = Pattern.compile("api/(.*?)\\?"); //$NON-NLS-1$
			Matcher matcher = pattern.matcher(newLink.getUrl());
			if(matcher.find()){
				String stringUuid = matcher.group(1);
				UUID uuid = UuidUtils.stringToUuid(stringUuid);
				
				Query query = QueryManager.INSTANCE.findQuery(uuid, s);
				Report report = (Report) s.get(Report.class, uuid);
				AbstractIntelQuery query2 = null;
				if (query == null && report == null) {
					//check for advanced intelligence query
					query2 = QueryManager.INSTANCE.findIntelQuery(uuid, s);
				}
							
				if (query == null && report == null && query2 == null){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SharedLinkApi.InvalidReportQueryLink", request.getLocale())); //$NON-NLS-1$
				}
				
				boolean canAccessQuery = false;
				if (query != null) {
					if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, uuid)) {
						//admin or specific access to query
						canAccessQuery = true;
					}else if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, query.getConservationArea().getUuid())) {
						//admin or access to conservation area
						canAccessQuery = true;
					}else if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, null)) {
						//admin or access to all queries
						canAccessQuery = true;
					}
				}
				boolean canAccessIntelQuery = false;
				if (query2 != null) {
					if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdvIntelAction.RUNQUERY_KEY, uuid)) {
						//admin or specific access to query
						canAccessIntelQuery = true;
					}else if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdvIntelAction.RUNQUERY_KEY, query.getConservationArea().getUuid())) {
						//admin or specific access to query
						canAccessIntelQuery = true;
					}
						
				}
				
				boolean canAccessReport = false;
				if (report != null) {
					if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, uuid)) {
						//admin or specific access to report
						canAccessReport = true;
					}else if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, report.getConservationArea().getUuid())) {
						//admin or access to conservation area
						canAccessReport = true;
					}else if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, null)) {
						//admin or access to all report
						canAccessReport = true;
					}
				}
				
			
				if(!canAccessQuery && !canAccessReport &&!canAccessIntelQuery){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SharedLinkApi.NoAccess", request.getLocale())); //$NON-NLS-1$
				}
				
				//set CA uuid
				if(query != null){
					newLink.setConservationArea(query.getConservationArea().getUuid());
				}else if (query2 != null) {
					newLink.setConservationArea(query2.getConservationArea().getUuid());
				}else if (report != null) {
					newLink.setConservationArea(report.getConservationArea().getUuid());
				}
				
				deleteExpiredLinks(s);
			}
		    
			//set expiration date
			Date created = new Date();
			newLink.setDateCreated(new Timestamp(created.getTime()));
			int mins = newLink.getExpiresAfter();
			if (mins == 0){
				//never expire
				newLink.setExpiresAt(new Timestamp(4102444800000l));
			}else if (mins > 0){
				//long is important here or else anything over 35790 mins or so breaks the Integer limit when converted to milliseconds
				long now = created.getTime();
				long ex = now + ((long)mins*1000*60);
				newLink.setExpiresAt(new Timestamp(ex));
			}else{
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SharedLinkApi.InvalidExpiresAfterValue",request.getLocale())); //$NON-NLS-1$
			}

			
			//set the ownerUUID
			SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			newLink.setOwnerUuid(user.getUuid());
			
			//set is_user_token
			newLink.setUserToken(false);
			
			s.save(newLink);
			
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}finally{
			s.getTransaction().commit();
		}
		return newLink;
	}

	private void deleteExpiredLinks(Session s) {
		//clean up any old, expired links. Seems like a vaguely reasonable place to do it: 
		//the more you create, the more often the clean-up occurs.
		s.createNativeQuery("DELETE FROM connect.shared_links WHERE expires_at < now()").executeUpdate(); //$NON-NLS-1$
	}
	
	
	/**
	 * <p>Create a new full-access user token. This allows you to pass a variable, &token=9dd23c7b-657c-492d-9857-9981713438b3  in a URL instead of using the basic auth. 
	 * The access granted is the same as if the token creator was making the request themselves. This token is deleted the same way as SharedLinks, calling the removeSharedlinks with a token
	 * uuid will delete and invalid the token. Expired Links are also cleaned up (deleted) when a new link is created.
	 * </p>
	 * 
	 * <p>
	 * URL: ../server/api/sharedlink/token/<br>
	 * Call Type: POST<br>
	 * Payload: A JSON object of the required attributes: expiresAfter  and the optional allowedIP
	 * </p>
	 * <p>This link expires in 1 year and can be used from any IP address:</p>
	 * <pre>{
	 *    expiresAfter: 525600"
	 *}</pre>  
	 * 		
	 * <p>This link expires in 1 hour and has a restricted UP address</p>
	 * <pre>{
	 *    "expiresAfter":"60","
	 *    "allowedIp":"192.168.1.1"
	 *}</pre>
	 *
	 * <p>expiresAfter:  0  means the link will never expire, otherwise it is # of minute until the link expires<br>
	 * allowedIp:  an IP address that is the only one allowed to use this token. If none is provided the link can be used from anywhere. The IP address is matched using Javascript's 
	 * 				request.getRemoteAddr() function, proxy servers etccan affect this address sometimes. It is recommended you test
	 * 				requests from your desired computer and see what the results of request.getRemoteAddr() is and enter that address in your restriction.
	 * </p>
	 * @param newLink SharedLink JSON object
	 *   
	 * @return this JSON ShareLink created -the uuid attribute is the token and can be used for any valid API calls 
	 * 			by appending a token attribute with your uuid to the URL, ex: &token=9dd23c7b-657c-492d-9857-9981713438b3   <-replace the uuid with your valid token uuid.
	 */
	@POST
    @Path("/token/")
	@Operation(description="Create a new full-access user token. This allows you to pass a variable, &token=9dd23c7b-657c-492d-9857-9981713438b3  in a URL instead of using the basic auth. \r\n" + 
			"	 * The access granted is the same as if the token creator was making the request themselves. This token is deleted the same way as SharedLinks, calling the removeSharedlinks with a token\r\n" + 
			"	 * uuid will delete and invalid the token. Expired Links are also cleaned up (deleted) when a new link is created.")
    public SharedLink createToken(SharedLink newLink) {
		if(!isCaAdminUser()){
			isAdminUser();//throws an exception if invalid user.
		}
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
		    
			//set expiration date			
			Date created = new Date();
			newLink.setDateCreated(new Timestamp(created.getTime()));
			int mins = newLink.getExpiresAfter();
			if (mins == 0){
				//never expire
				newLink.setExpiresAt(new Timestamp(4102444800000l));
			}else if (mins > 0){
				//long is important here or else anything over 35790 mins or so breaks the Integer limit when converted to milliseconds 
				long now = created.getTime();
				long ex = now + ((long)mins*1000*60);
				newLink.setExpiresAt(new Timestamp(ex));
			}else{
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SharedLinkApi.InvalidExplireValue",request.getLocale())); //$NON-NLS-1$
			}

			
			//set the ownerUUID
			SmartUser user = HibernateManager.getUser(s, request.getUserPrincipal().getName());
			newLink.setOwnerUuid(user.getUuid());
			
			//set is_user_token
			newLink.setUserToken(true);
			
			s.save(newLink);
			
			//clean up any old, expired links. Seems like a vaguely reasonable place to do it: 
			//the more you create, the more often the clean-up occurs.
			deleteExpiredLinks(s);
			
		}catch(Exception e){
			throw e;
		}finally{
			s.getTransaction().commit();
		}
		return newLink;
	}
	
	
	
	/**
	 * <p>Delete a shared link</p>
	 * <p>
	 * URL: ../server/api/sharedlink/{uuid}<br>
	 * Call Type: DELETE
	 * </p>
	 * 
	 * @param uuid provided in the URL, the UUID of the shared link to delete.
	 * @return the JSON SharedLink object for the deleted link 
	 */
    @DELETE
    @Path("/{uuid}")
    @Operation(description="Delete a shared link")
    public SharedLink removeSharedLink(@Parameter(description="the link's uuid to be deleted") @PathParam("uuid") UUID uuid) {

    	SharedLink toDelete = null;
    	Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			toDelete = HibernateManager.getSharedLink(s, uuid);
			if (toDelete == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, 
						MessageFormat.format("", uuid));  //$NON-NLS-1$
			}
			
			if(!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
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
