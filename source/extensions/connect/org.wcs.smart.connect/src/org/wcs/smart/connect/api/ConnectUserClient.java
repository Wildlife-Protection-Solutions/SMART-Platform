package org.wcs.smart.connect.api;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.wcs.smart.connect.SmartConnect;

@Path("/" + ConnectUserClient.PATH)
public interface ConnectUserClient {
	
	public static final String PATH = "connectuser"; 
	@GET
    @Path("/{username}")
	@Produces(MediaType.APPLICATION_JSON)
    String getUser(@PathParam("username") String username);
	
//	@POST
//    @Path("/{username}")
//	@Produces(MediaType.APPLICATION_JSON)
//    String addUser(@PathParam("username") String user, SmartUser newUser);
// 
//    @PUT
//    @Path("/{username}")
//    String updateUser(
//    		@PathParam("username") String olduser,
//    		SmartUser newUser);
}
