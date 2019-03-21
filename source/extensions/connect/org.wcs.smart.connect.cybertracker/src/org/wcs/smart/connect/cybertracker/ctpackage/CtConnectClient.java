package org.wcs.smart.connect.cybertracker.ctpackage;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.wcs.smart.connect.api.ConnectClient;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;

public interface CtConnectClient extends ConnectClient{

	
	@POST
	@Path("/cybertracker/{uuid}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadCtPackage(@HeaderParam("X-Upload-Content-Length") Long length, 
			@PathParam("uuid") String packageuuid,
			CyberTrackerPackageProxy proxy);

	@GET
    @Path("/cybertracker/")
    public List<CyberTrackerPackageProxy> getCtPackages(@QueryParam("cauuid") String caUuid);
	
}
