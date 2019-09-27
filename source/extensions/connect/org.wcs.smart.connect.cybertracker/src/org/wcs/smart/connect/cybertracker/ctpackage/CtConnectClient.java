/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;

/**
 * Client for cybertracker api for managing packages and navigation layers
 * @author Emily
 *
 */
public interface CtConnectClient extends ConnectClient{
	
	@POST
	@Path("/cybertracker/packages/{uuid}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadCtPackage(@HeaderParam("X-Upload-Content-Length") Long length, 
			@PathParam("uuid") String packageuuid, CyberTrackerPackageProxy proxy);

	@GET
    @Path("/cybertracker/packages")
    public List<CyberTrackerPackageProxy> getCtPackages(@QueryParam("cauuid") String caUuid);

	@POST
	@Path("/cybertracker/navigationlayers/{uuid}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response uploadNavigationLayer(@HeaderParam("X-Upload-Content-Length") Long length, 
			@PathParam("uuid") String navigationuuid, CyberTrackerNavigationProxy proxy);

	@GET
    @Path("/cybertracker/navigationlayers")
    public List<CyberTrackerNavigationProxy> getNavigationLayers(@QueryParam("cauuid") String caUuid);
	
	@GET
    @Path("/cybertracker/apikey/{cauuid}")
    public String getApiKey(@PathParam("cauuid") String caUuid);

}
