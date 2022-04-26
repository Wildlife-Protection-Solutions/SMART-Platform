/*
 * Copyright (C) 2012 Wildlife Conservation Society
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

import java.io.InputStream;
import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.api.model.WorkItemStatus;

/**
 * JAX-RS REST client api for SMART Connect requests 
 * 
 * @author Emily
 *
 */
public interface ConnectClient {
	
	public static final String USER_PATH = "connectuser"; //$NON-NLS-1$
	public static final String CA_PATH = "conservationarea"; //$NON-NLS-1$
	public static final String UPLOAD_PATH = "uploader"; //$NON-NLS-1$
	
	public static final String DATA_PARAM_ALL = "all"; //$NON-NLS-1$
	public static final String DATA_PARAM_PACKAGE = "changelog"; //$NON-NLS-1$
		
	@GET
    @Path("/" + USER_PATH + "/{username}")
	@Produces(MediaType.APPLICATION_JSON)
    String getUser(@PathParam("username") String username, @QueryParam("validate") String validateOnly);
	
	@GET
    @Path("/" + CA_PATH )
    public List<ConservationAreaProxy> getConservationAreas();
	
	
	@GET
    @Path("/" + CA_PATH + "/{cauuid}")
    public ConservationAreaProxy getConservationArea(@PathParam("cauuid") String caUuid);

	@GET
    @Path("/" + CA_PATH + "/{cauuid}")
    public Response downloadConservationArea(@PathParam("cauuid") String caUuid, @QueryParam("data") String data);
	
	@GET
    @Path("/" + CA_PATH + "/{cauuid}")
    public Response downloadChangeLog(@PathParam("cauuid") String caUuid, @QueryParam("data") String data, @QueryParam("version") String version, @QueryParam("revision") String revision);
	
	@GET
	@Path("/" + UPLOAD_PATH + "/{uploaduuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public WorkItemStatus getStatus(@PathParam("uploaduuid") String uuid);
	
	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces({ MediaType.APPLICATION_JSON })
	public String updateFile(InputStream data);

	@POST
	@Path("/" + CA_PATH + "/{cauuid}")
	public Response getDataUploadUrl(@HeaderParam("X-Upload-Content-Length") Long length, 
			@PathParam("cauuid") String caUuid,
			@QueryParam("version") String versionUuid);
	
	@PUT
	@Path("/" + CA_PATH + "/{cauuid}")
	public Response updateConservationArea(@HeaderParam("X-Upload-Content-Length") Long length, @PathParam("cauuid") String caUuid);

	@GET
    @Path("/connectalert/alertTypes/")
    public List<AlertType> getAlertTypes();
}
