/*
 * Copyright (C) 2020 Wildlife Conservation Society
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

import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SmartCollectConnectUser;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.smartcollect.model.SmartCollectUser;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * Secure SMARTCollect api 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + SmartCollectApi.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class SmartCollectApi {

	private final Logger logger = Logger.getLogger(SmartCollectApi.class.getName());
	
	public static final String PATH = "smartcollect"; //$NON-NLS-1$
	
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	
	
	private UUID parseUuid(String uuid) {
		UUID userid = null;
		try {
			userid = UuidUtils.stringToUuid(uuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}
		if (userid == null) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		return userid;
	}
	
	@GET
    @Path("/source")
	@Operation(description="if no query parameter is specified this returns all collect users; "
			+ "otherwise it returns the collect user with the matching source (if not found a new one is created and returned)")
	public List<SmartCollectUser> getUsers(
			@Parameter(description = "the identifier for the user to return; if not found a new user with this identifier is created") @QueryParam("source") String source,
			@Parameter(description = "the deviceid for the user to return; must be provided if source is provided ") @QueryParam("deviceId") String deviceId,
			@Parameter(description = "the search string to use to search for users.  only used if the source parameter is null ") @QueryParam("search") String search, 
			@Parameter(description = "the maximum results from the search; only used if search parameter i not null") @QueryParam("limit") String limit ) {
		if (source == null) {
			if (search == null) {
				List<SmartCollectUser> users = new ArrayList<>();
				Session s = HibernateManager.getSession(context);
				try{
					s.beginTransaction();
					for (SmartCollectConnectUser user : QueryFactory.buildQuery(s, SmartCollectConnectUser.class).list()){
						users.add(user.toSmartCollectUser());
					}
					s.getTransaction().commit();
				}catch (Exception ex) {
					logger.log(Level.SEVERE, "Error fetching collect users", ex); //$NON-NLS-1$
					throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("SmartCollectApi_GetUsersError", request.getLocale()), ex); //$NON-NLS-1$
				}
				users.sort((a,b)->Collator.getInstance().compare(a.getSource(), b.getSource()));
				return users;
			}else {
				
				int thislimit = 50;
				if (limit != null) {
					try {
						thislimit = Integer.parseInt(limit);
						if (thislimit <= 0) thislimit = 50;
					}catch (Exception ex) {}
				}
				
				List<SmartCollectUser> users = new ArrayList<>();
				Session s = HibernateManager.getSession(context);
				try{
					s.beginTransaction();
					List<SmartCollectConnectUser> cus = s.createQuery("FROM SmartCollectConnectUser WHERE source like :src", SmartCollectConnectUser.class) //$NON-NLS-1$
							.setParameter("src", "%" + search + "%") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							.setMaxResults(thislimit)
							.list();
					for (SmartCollectConnectUser user : cus){
						users.add(user.toSmartCollectUser());
					}
					s.getTransaction().commit();
				}catch (Exception ex) {
					logger.log(Level.SEVERE, "Error fetching collect users", ex); //$NON-NLS-1$
					throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("SmartCollectApi_GetUsersError", request.getLocale()), ex); //$NON-NLS-1$
				}
				users.sort((a,b)->Collator.getInstance().compare(a.getSource(), b.getSource()));

				return users;
				
			}
		}else {
			Session s = HibernateManager.getSession(context);

			SmartCollectConnectUser user = null;
			try{
				s.beginTransaction();
				user = QueryFactory.buildQuery(s, SmartCollectConnectUser.class, 
							new Object[]{"source", source}, //$NON-NLS-1$
							new Object[] {"deviceId", deviceId}).uniqueResult(); //$NON-NLS-1$
				if (user == null) {
					//create a new user
					user = new SmartCollectConnectUser();
					user.setSource(source);
					user.setDeviceId(deviceId);
					user.setState(SmartCollectUser.State.NEW);
					
					s.persist(user);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("SmartCollectApi_ValidateUserError", request.getLocale()), ex); //$NON-NLS-1$
			}
			
			return Collections.singletonList(user.toSmartCollectUser());
		}
	}
	

	@GET
    @Path("/source/{uuid: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@Operation(description="get full details about a community user")
	public SmartCollectConnectUser getUser(@PathParam("uuid") String uuid) {
		UUID userid = parseUuid(uuid);
		Session s = HibernateManager.getSession(context);
		SmartCollectConnectUser user = null;
		try{
			s.beginTransaction();
			user = s.get(SmartCollectConnectUser.class, userid);
			if (user == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			s.getTransaction().commit();
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("SmartCollectApi_ValidateUserError", request.getLocale()), ex); //$NON-NLS-1$
		}
		return user;
	}
	
	@DELETE
    @Path("/source/{uuid: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@Operation(description="deletes the community user")
	public void deleteUser(@PathParam("uuid") String uuid) {
		UUID userid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		try{
			s.beginTransaction();
			SmartCollectConnectUser cu = s.get(SmartCollectConnectUser.class, userid);
			if (cu != null) s.remove(cu);
			s.getTransaction().commit();
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("SmartCollectApi_RemoveUserError", request.getLocale())); //$NON-NLS-1$
		}
	}
	
	@PUT
    @Path("/source/{uuid: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@Operation(description="Updates the state associated with the given source.  If sendvalidation is provided and the user provided an email address then attempts to send validation otherwise not update is made")
	public void updateState(@PathParam("uuid") String uuid, 
			@Parameter(description = "the new state for the user") @QueryParam("state") String newState, 
			@Parameter(description = "if true then a new user validation will be sent") @QueryParam("sendvalidation") String sendValidation) {
		
		UUID userid = parseUuid(uuid);
		
		boolean sendvalidation = false;
		if (sendValidation != null) {
			try {
				sendvalidation = Boolean.parseBoolean(sendValidation);
			}catch (Exception ex) {
				throw new SmartConnectException(Response.Status.BAD_REQUEST);
			}
		}
		
		SmartCollectUser.State state = null;
		try {
			state = State.valueOf(newState);
			//cannot request to set state to validation pending or new
			if (state == State.VALIDATION_PENDING) throw new SmartConnectException(Response.Status.BAD_REQUEST);
			if (state == State.NEW) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}
		
		if (state == null && !sendvalidation) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		
		Session s = HibernateManager.getSession(context);
		
		SmartCollectConnectUser user = null;
		s.beginTransaction();
		try{
			user = s.get(SmartCollectConnectUser.class, userid);
			if (user == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			
			
			if (sendvalidation) {
				if (SmartCollectUser.isEmailSource(user.getSource())) {			
					// send email
					try {
						String url = request.getRequestURL().toString();
						String uri = request.getRequestURI();
						if (uri != null && uri.length() > 0) {
							url = url.substring(0, url.indexOf(uri));
						}
						
						sendValidationRequest(request.getLocale(), user, url);
								
					} catch (Exception ex) {
						logger.log(Level.SEVERE, "Sending validation email failed:" + ex.getMessage(), ex); //$NON-NLS-1$
						throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR);
					}
				}else {
					user.setState(state);
				}
			}else {
				user.setState(state);
			}
			s.getTransaction().commit();
		}catch (SmartConnectException ex) {
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex) {
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("SmartCollectApi_UpdateError", request.getLocale()), ex); //$NON-NLS-1$
		}

	}
	
	private  static String generateValidationKey() {
		String v1 = UUID.randomUUID().toString().replaceAll("-",""); //$NON-NLS-1$ //$NON-NLS-2$
		String v2 = UUID.randomUUID().toString().replaceAll("-",""); //$NON-NLS-1$ //$NON-NLS-2$
		String key = v1 + v2;
		return key;
	}
	
	public static void sendValidationRequest(Locale locale,
			SmartCollectConnectUser user, String url) throws Exception{
		
		user.setState(State.VALIDATION_PENDING);					
		user.setValidateSentDate(LocalDateTime.now());
		user.setValidationKey(generateValidationKey());
		user.setValidateSentDate(LocalDateTime.now());
		
		if (locale == null) locale = Locale.getDefault();
		
		javax.naming.Context initCtx = new InitialContext();
		javax.naming.Context envCtx = (javax.naming.Context) initCtx
				.lookup("java:comp/env"); //$NON-NLS-1$
		javax.mail.Session session = (javax.mail.Session) envCtx
				.lookup("mail/Session"); //$NON-NLS-1$
		
		
		String validationUrl = url + "/noa/smartcollect/source/" + user.getValidationKey(); //$NON-NLS-1$
				
		Message message = new MimeMessage(session);
		InternetAddress to[] = new InternetAddress[1];
		to[0] = new InternetAddress(user.getSource());			
		message.setRecipients(Message.RecipientType.TO, to);
		message.setSubject(Messages.getString("SmartCollectApi_ValidationEmailSubject", locale)); //$NON-NLS-1$
		
		message.setContent(MessageFormat.format(
				Messages.getString("SmartCollectApi.ValidationMessage", locale),//$NON-NLS-1$ 
				"<a href=\"" + validationUrl + "\">", "</a>", "<br><br>", "<br>" + validationUrl), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 
				"text/html");  //$NON-NLS-1$
		
		Transport.send(message);
	}
}
