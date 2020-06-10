package org.wcs.smart.connect.api;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import org.wcs.smart.smartcollect.model.SmartCollectUser;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


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
			@Parameter(description = "the search string to use to search for users.  only used if the source parameter is null ") @QueryParam("search") String search, 
			@Parameter(description = "the maximum results from the search; only used if search parameter i not null") @QueryParam("limit") String limit ) {
		if (source == null) {
			if (search == null) {
				List<SmartCollectUser> users = new ArrayList<>();
				Session s = HibernateManager.getSession(context);
				try{
					s.beginTransaction();
					for (SmartCollectConnectUser user : QueryFactory.buildQuery(s, SmartCollectConnectUser.class).list()){
						SmartCollectUser u = new SmartCollectUser();
						u.setSource(user.getSource());
						u.setState(user.getState());
						u.setUuid(user.getUuid());
						users.add(u);
					}
					s.getTransaction().commit();
				}catch (Exception ex) {
					logger.log(Level.SEVERE, "Error fetching collect users", ex);
					throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error fetching collect users.", ex);
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
					List<SmartCollectConnectUser> cus = s.createQuery("FROM SmartCollectConnectUser WHERE source like :src", SmartCollectConnectUser.class)
							.setParameter("src", "%" + search + "%")
							.setMaxResults(thislimit)
							.list();
					for (SmartCollectConnectUser user : cus){
						SmartCollectUser u = new SmartCollectUser();
						u.setSource(user.getSource());
						u.setState(user.getState());
						u.setUuid(user.getUuid());
						users.add(u);
					}
					s.getTransaction().commit();
				}catch (Exception ex) {
					logger.log(Level.SEVERE, "Error fetching collect users", ex);
					throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error fetching SMART Collect users.", ex);
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
							"source", source).uniqueResult();
				if (user == null) {
					//create a new user
					user = new SmartCollectConnectUser();
					user.setSource(source);
					user.setState(SmartCollectUser.State.NEW);
					
					s.save(user);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error validating SMART Collect user", ex);
			}
			
			SmartCollectUser cu = new SmartCollectUser();
			cu.setSource(user.getSource());
			cu.setState(user.getState());
			cu.setUuid(user.getUuid());
			return Collections.singletonList(cu);
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
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error validating SMART Collect user", ex);
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
			if (cu != null) s.delete(cu);
			s.getTransaction().commit();
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error removing SMART Collect user.");
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
					user.setState(State.VALIDATION_PENDING);
					
					user.setValidateSentDate(new Date());
					String v1 = UUID.randomUUID().toString().replaceAll("-","");
					String v2 = UUID.randomUUID().toString().replaceAll("-","");
					String key = v1 + v2;
					user.setValidationKey(key);
					user.setValidateSentDate(new Date());

					// send email
					try {
						javax.naming.Context initCtx = new InitialContext();
						javax.naming.Context envCtx = (javax.naming.Context) initCtx
								.lookup("java:comp/env"); //$NON-NLS-1$
						javax.mail.Session session = (javax.mail.Session) envCtx
								.lookup("mail/Session"); //$NON-NLS-1$
						
						String url = request.getRequestURL().toString();
						String uri = request.getRequestURI();
						if (uri != null && uri.length() > 0) {
							url = url.substring(0, url.indexOf(uri));
						}
						String validationUrl = url + request.getContextPath() + "/noa/smartcollect/source/" + key;
								
						Message message = new MimeMessage(session);
						InternetAddress to[] = new InternetAddress[1];
						to[0] = new InternetAddress(user.getSource());			
						message.setRecipients(Message.RecipientType.TO, to);
						message.setSubject("SMART Collect User Validation");
						
						message.setContent(
								"Click <a href=\"" + validationUrl + "\">here</a> to confirm the use of your e-mail address as your username for SMART Collect."
								+ "<br><br>If the above link doesn't work paste this url into your browser:<br>" + validationUrl, "text/html"); //$NON-NLS-1$ //$NON-NLS-2$
						Transport.send(message);
					} catch (Exception ex) {
						logger.log(Level.SEVERE, "Sending validation email failed:" + ex.getMessage(), ex);
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
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error updating SMART Collect user state", ex);
		}

	}
}
