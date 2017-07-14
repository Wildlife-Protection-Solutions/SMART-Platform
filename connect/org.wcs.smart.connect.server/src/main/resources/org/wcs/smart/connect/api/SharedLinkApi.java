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

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.QueryAction;
import org.wcs.smart.connect.security.ReportAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.util.UuidUtils;

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
public class SharedLinkApi extends HttpServlet{
	
	public static final String PATH = "sharedlink"; //$NON-NLS-1$
	
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
			return SecurityManager.INSTANCE.isCaAdmin(s, request.getUserPrincipal().getName(), CaAdminAccountAction.KEY);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	
	/**
	 * List all Shared Links
	 * <p>URL: ../server/api/sharedlink/
	 * <p>Call Type: GET
	 * 
	 * @return Returns a JSON array of SharedLink objects
	 */
	@SuppressWarnings("unchecked")
	@GET
    @Path("/")
    public List<SharedLink> getSharedLinks(){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			if(SecurityManager.INSTANCE.isCaAdmin(s, request.getUserPrincipal().getName(), CaAdminAccountAction.KEY)){
				//only return links from the CA(s) they are CaAdmin users for
				Criteria c = s.createCriteria(SmartUserAction.class)
						.add(Restrictions.eq("username", request.getUserPrincipal().getName() )) //$NON-NLS-1$
						.add(Restrictions.eq("action", CaAdminAccountAction.KEY)); //$NON-NLS-1$
				
				List<SmartUserAction> list = (ArrayList<SmartUserAction>)c.list();
		
				List<SharedLink> links = new ArrayList<SharedLink>();
				for(SmartUserAction a : list ){//loop over each CA they are admins of
					UUID caUuid = a.getResource();
					
					List<SharedLink> temp = s.createCriteria(SharedLink.class).add(Restrictions.eq("conservationArea", caUuid)).list();  //$NON-NLS-1$
					for(SharedLink t : temp){//add all shared links from this CA
						links.add(t);
						t.setOwnerUsername( ((SmartUser)s.get(SmartUser.class, t.getOwnerUuid())).getUsername() );
					}
				}
				return links;
			}
			if(!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}else{
				List<SharedLink> links =  s.createCriteria(SharedLink.class).list();
				for (SharedLink l : links){
					l.setOwnerUsername( ((SmartUser)s.get(SmartUser.class, l.getOwnerUuid())).getUsername() );
				}
				return links;
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					Messages.getString("SharedLinks.ListAllError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Create a new shared link. Expired Links are also cleaned up (deleted) when a new link is created.
	 * <p>URL: ../server/api/sharedlink/
	 * <p>Call Type: POST
	 * <p>Payload: A JSON object of the 2 required attributes: expiresAfter and url, ex: 
	 * 		{expiresAfter: 525600, url: "query/api/5e4b8ab8-1313-42b4-886c-ff5b7db8ec65?format=csv&date_filter=waypointdate"}  
	 * 		<expires in 1 year>
	 * @param expiresAfter - 0  means the link will never expire, otherwise it is # of minutes until the link expires
	 * @param url - the url of the report or query you want to link to.
	 * @return Returns a JSON SharedLink object for the created user, the link_uuid attribute is what you need to put into URLs. 
	 */
	@POST
    @Path("/")
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
							
				if (query == null && report == null){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SharedLinkApi.InvalidReportQueryLink", request.getLocale())); //$NON-NLS-1$
				}
				
				boolean hasAccessQuery = SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), QueryAction.RUNQUERY_KEY, uuid);
				boolean hasAccessReport = SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, uuid);
				boolean hasAccessAllReports = false;
				boolean hasAccessAllQueries = false;
				if(query != null){
					hasAccessAllQueries = SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, query.getConservationArea().getUuid());
				}
				if(report != null){
					hasAccessAllReports = SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), ReportAction.RUNREPORT_KEY, report.getConservationArea().getUuid());
				}
				if(!hasAccessQuery && !hasAccessReport && !hasAccessAllQueries && !hasAccessAllReports){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SharedLinkApi.NoAccess", request.getLocale())); //$NON-NLS-1$
				}
				
				//set CA uuid
				if(query != null){
					newLink.setConservationArea(query.getConservationArea().getUuid());
				}else{
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
		SQLQuery q = s.createSQLQuery("DELETE FROM connect.shared_links WHERE expires_at < now()"); //$NON-NLS-1$
		q.executeUpdate();
	}
	
	
	/**
	 * Create a new full-access user token. This allows you to pass a variable, &token=9dd23c7b-657c-492d-9857-9981713438b3  in a URL instead of using the basic auth. 
	 * The access granted is the same as if the token creator was making the request themselves. This token is deleted the same way as SharedLinks, calling the removeSharedlinks with a token
	 * uuid will delete and invalid the token. Expired Links are also cleaned up (deleted) when a new link is created.
	 * 
	 * <p>URL: ../server/api/sharedlink/token/
	 * 
	 * <p>Call Type: POST
	 * 
	 * <p>Payload: A JSON object of the required attributes: expiresAfter  and the optional allowedIP
	 * 		<br>example #1, link that expires in 1 year, can be used from any IP address: 
	 * 		{expiresAfter: 525600"}  
	 * 		
	 * 
	 * 		<br>Example #2, restricted to an IP address, expirees in 1 hour:  
	 * 		{"expiresAfter":"60","allowedIp":"192.168.1.1"}
	 * @param expiresAfter - 0  means the link will never expire, otherwise it is # of minutes until the link expires
	 * @param allowedIp - an IP address that is the only one allowed to use this token. If none is provided the link can be used from anywhere. The IP address is matched using Javascript's 
	 * 				request.getRemoteAddr() function, proxy servers etccan affect this address sometimes. It is recommended you test
	 * 				requests from your desired computer and see what the results of request.getRemoteAddr() is and enter that address in your restriction. 
	 * @return Returns a JSON ShareLink object for the created user, the uuid attribute is the token and can be used for any valid API calls 
	 * 			by appending a token attribute with your uuid to the URL, ex: &token=9dd23c7b-657c-492d-9857-9981713438b3   <-replace the uuid with your valid token uuid.
	 */
	@POST
    @Path("/token/")
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
	 * Delete a shared link
	 * <p>URL: ../server/api/sharedlink/{alertUuid}
	 * <p>Call Type: DELETE
	 * 
	 * @param	Uuid	provided in the URL, the UUID of the shared link to delete.
	 * @return Returns a JSON shared link object for the deleted link 
	 */
    @DELETE
    @Path("/{uuid}")
    public SharedLink removeSharedLink(@PathParam("uuid") UUID uuid) {

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
