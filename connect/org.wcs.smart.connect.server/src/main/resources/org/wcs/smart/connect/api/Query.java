package org.wcs.smart.connect.api;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path(ConnectRESTApplication.PATH_SEPERATOR + Query.PATH)
public class Query extends HttpServlet{
	public static final String PATH = "query"; //$NON-NLS-1$

	
	@GET
    @Path("/{queryuuid}")
	public Response getQueryResults(@PathParam("queryuuid") String queryUuid, 
			@QueryParam("format") String format, 
			@QueryParam("delimiter") String delimiter){

		
		
		//return accepted
		return Response.accepted().
	            build();
	}
}
