package org.wcs.smart.connect.api;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.wcs.smart.connect.model.CtCommunityUser;
import org.wcs.smart.cybertracker.community.model.CommunityUser;
import org.wcs.smart.cybertracker.community.model.CommunityUser.State;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;


@Path(ConnectRESTApplication.PATH_SEPERATOR + CommunityApi.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class CommunityApi {

	private final Logger logger = Logger.getLogger(CommunityApi.class.getName());
	
	public static final String PATH = "community"; //$NON-NLS-1$
	
	
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
	@Operation(description="if no query parameter is specified this returns all community users; "
			+ "otherwise it returns the community user with the matching source (if not found a new one is created and returned)")
	public Set<CommunityUser> getUsers(@QueryParam("source") String source ) {
		if (source == null) {
			Set<CommunityUser> users = new HashSet<>();
			Session s = HibernateManager.getSession(context);
			try{
				s.beginTransaction();
				for (CtCommunityUser user : QueryFactory.buildQuery(s, CtCommunityUser.class).list()){
					CommunityUser u = new CommunityUser();
					u.setSource(user.getSource());
					u.setState(user.getState());
					u.setUuid(user.getUuid());
					users.add(u);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				logger.log(Level.SEVERE, "Error fetching community users", ex);
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error fetching community users.", ex);
			}
			return users;
		}else {
			Session s = HibernateManager.getSession(context);

			CtCommunityUser user = null;
			try{
				s.beginTransaction();
				user = QueryFactory.buildQuery(s, CtCommunityUser.class, 
							"source", source).uniqueResult();
				if (user == null) {
					//create a new user
					user = new CtCommunityUser();
					user.setSource(source);
					user.setState(CommunityUser.State.NEW);
					
					s.save(user);
				}
				s.getTransaction().commit();
			}catch (Exception ex) {
				throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error validating community connect user", ex);
			}
			if (user == null) throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error validating community connect user");
			
			CommunityUser cu = new CommunityUser();
			cu.setSource(user.getSource());
			cu.setState(user.getState());
			cu.setUuid(user.getUuid());
			return Collections.singleton(cu);
		}
	}
	
	@GET
    @Path("/source/{uuid: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@Operation(description="get full details about a community user")
	public CtCommunityUser getUser(@PathParam("uuid") String uuid) {
		UUID userid = parseUuid(uuid);
		Session s = HibernateManager.getSession(context);
		CtCommunityUser user = null;
		try{
			s.beginTransaction();
			user = s.get(CtCommunityUser.class, userid);
			if (user == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			s.getTransaction().commit();
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error validating community connect user", ex);
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
			CtCommunityUser cu = s.get(CtCommunityUser.class, userid);
			if (cu != null) s.delete(cu);
			s.getTransaction().commit();
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error removing community users.");
		}
	}
	
	@PUT
    @Path("/source/{uuid: [a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}}")
	@Operation(description="Updates the state associated with the given source.  If sendvalidation is provided and the user provided an email address then attempts to send validation otherwise not update is made")
	public void updateState(@PathParam("uuid") String uuid, @QueryParam("state") String newState, @QueryParam("sendvalidation") String sendValidation) {
		
		UUID userid = parseUuid(uuid);
		
		boolean sendvalidation = false;
		if (sendValidation != null) {
			try {
				sendvalidation = Boolean.parseBoolean(sendValidation);
			}catch (Exception ex) {
				throw new SmartConnectException(Response.Status.BAD_REQUEST);
			}
		}
		
		CommunityUser.State state = null;
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
		
		CtCommunityUser user = null;
		try{
			s.beginTransaction();
			user = s.get(CtCommunityUser.class, userid);
			if (user == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			
			
			if (sendvalidation) {
				if (CommunityUser.isEmailSource(user.getSource())) {
					user.setState(State.VALIDATION_PENDING);
					
					user.setValidateSentDate(new Date());
					String v1 = UUID.randomUUID().toString().replaceAll("-","");
					String v2 = UUID.randomUUID().toString().replaceAll("-","");
					String key = v1 + v2;
					user.setValidationKey(key);
					//	TODO: send email
				}else {
					user.setState(state);
				}
			}else {
				user.setState(state);
			}
			s.getTransaction().commit();
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error updating community user state", ex);
		}

	}
}
