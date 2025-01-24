/*
 * Copyright (C) 2025 Wildlife Conservation Society
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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.Announcement;
import org.wcs.smart.connect.model.AnnouncementProxy;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * SMART Connect Report REST API
 * @author Emily
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + AnnouncementApi.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class AnnouncementApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	
	
	public static final String PATH = "announcement"; //$NON-NLS-1$

	
	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
	
	/**
	 * <p>Gets all announcements.</p>
	 * <p>
	 * URL: ../server/api/announcement/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param cauuid - The ca UUID 
	 * @return
	 */
	@GET
    @Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Gets all announcements.")
	public List<AnnouncementProxy> getAllAnnouncements() {
			
		cleanUpExpired(context);
		
		try(Session s = HibernateManager.getSession(context, request.getLocale())){
			
			s.beginTransaction();
			try {
				
				List<ConservationAreaInfo> cas = s.createQuery("FROM ConservationAreaInfo", ConservationAreaInfo.class) //$NON-NLS-1$
						.list();
				
				List<ConservationAreaInfo> allowedca = new ArrayList<>();
				
				for (ConservationAreaInfo ca : cas) {
					if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.VIEWCA_KEY, ca.getUuid())){
						allowedca.add(ca);
					}
				}

				return s.createQuery("FROM Announcement WHERE conservationArea in (:infos)", Announcement.class) //$NON-NLS-1$
					.setParameterList("infos", allowedca) //$NON-NLS-1$
					.stream()
					.map(e->e.toProxy())
					.toList();
				
			}finally {
				s.getTransaction().rollback();
			}
		}
	}
	
	/**
	 * <p>Create a new announcement
	 * <p>
	 * URL: ../server/api/announcement<br>
	 * Call Type: POST
	 * </p>
	 * 
	 * @return Update announcement object 
	 */
	@POST
	@Path("/")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Returns the report requested, not the results of the report, but the report object definition/DB row")
	public AnnouncementProxy createAccouncement(
			@Parameter(description="The annoucement details.") AnnouncementProxy proxy){
		
		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		try{		
			ConservationAreaInfo info = s.get(ConservationAreaInfo.class, proxy.getCaUuid());
			if (info == null) {
				throw new SmartConnectException(Status.NOT_FOUND); 
			}
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.UPDATECA_KEY, info.getUuid())){
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			
			Announcement item = new Announcement();
			item.setConservationArea(info);
			item.setMessage(proxy.getMessage());
			item.setCreatedOn(ZonedDateTime.now());
			item.setExpiresOn(proxy.getExpiresOn());
			
			s.persist(item);
			s.getTransaction().commit();
			
			return item.toProxy();
		}catch (Exception ex) {
			s.getTransaction().rollback();
			throw ex;
		}
	}
	
	/**
	 * <p>Gets an existing announcement
	 * <p>
	 * URL: ../server/api/announcement<br>
	 * Call Type: POST
	 * </p>
	 * 
	 * @return announcement object 
	 */
	@GET
	@Path("/{uuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Gets an announcement by id")
	public AnnouncementProxy updateAccouncement(
			@Parameter(description="The announcement UUID") @PathParam("uuid") String auuid){
		
		UUID uuid = UuidUtils.stringToUuid(auuid);		

		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		try{		
			Announcement item = s.get(Announcement.class, uuid);
			if (item == null) throw new SmartConnectException(Status.NOT_FOUND);
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.UPDATECA_KEY, item.getConservationArea().getUuid())){
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			
			return item.toProxy();
			
		}catch (Exception ex){			
			throw ex;
		}finally {
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * <p>Updates an existing announcement
	 * <p>
	 * URL: ../server/api/announcement<br>
	 * Call Type: POST
	 * </p>
	 * 
	 * @return Update announcement object 
	 */
	@PUT
	@Path("/{uuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Updates the message or expired date for the given announcement")
	public AnnouncementProxy updateAccouncement(
			@Parameter(description="The announcement UUID") @PathParam("uuid") String auuid,
			@Parameter(description="The annoucement details.") AnnouncementProxy toUpdate){
		
		UUID uuid = UuidUtils.stringToUuid(auuid);		

		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		try{		
			Announcement item = s.get(Announcement.class, uuid);
			if (item == null) throw new SmartConnectException(Status.NOT_FOUND);
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.UPDATECA_KEY, item.getConservationArea().getUuid())){
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			if (toUpdate.getMessage() != null) {
				item.setMessage(toUpdate.getMessage());
			}
			if (toUpdate.getExpiresOn() != null) {
				item.setExpiresOn(toUpdate.getExpiresOn());
			}
			
			s.getTransaction().commit();
			return item.toProxy();
			
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}
	}
	
	/**
	 * <p>Deletes an existing announcement
	 * <p>
	 * URL: ../server/api/announcement<br>
	 * Call Type: POST
	 * </p>
	 * 
	 * @return Update announcement object 
	 */
	@DELETE
	@Path("/{uuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description="Deletes the given announcement")
	public AnnouncementProxy deleteAccouncement(
			@Parameter(description="The announcement UUID") @PathParam("uuid") String auuid){
		
		UUID uuid = UuidUtils.stringToUuid(auuid);		

		Session s = HibernateManager.getSession(context, request.getLocale());
		s.beginTransaction();
		try{		
			Announcement item = s.get(Announcement.class, uuid);
			if (item == null) throw new SmartConnectException(Status.NOT_FOUND);
			
			//check for permission to this query for this user.
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAction.UPDATECA_KEY, item.getConservationArea().getUuid())){
				throw new SmartConnectException(Status.UNAUTHORIZED);
			}
			
			s.remove(item);
			s.getTransaction().commit();
			return item.toProxy();
			
		}catch (Exception ex){
			s.getTransaction().rollback();
			throw ex;
		}
	}

	/**
	 * session NOT IN transaction
	 * @param session
	 */
	public static void cleanUpExpired(ServletContext context) {
		try(Session session = HibernateManager.openNewSession(context, Locale.getDefault())){
			cleanUpExpired(session);			
		}
	}
	
	/**
	 * session NOT in transaction
	 */
	public static void cleanUpExpired(Session session) {
		session.beginTransaction();
		try {
			ZonedDateTime now = ZonedDateTime.now();
			now = now.minusMonths(3);
			
			session.createMutationQuery("DELETE FROM Announcement WHERE expiresOn < :date") //$NON-NLS-1$
				.setParameter("date", now) //$NON-NLS-1$
				.executeUpdate();
			session.getTransaction().commit();
			
		}catch (Exception ex) {
			Logger.getLogger(AnnouncementApi.class.getName()).log(Level.SEVERE, "Unable to clean up expired announcements", ex); //$NON-NLS-1$
			session.getTransaction().rollback();
		}
	}
}

