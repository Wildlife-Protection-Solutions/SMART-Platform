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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.wcs.smart.connect.api.model.ConservationAreaInfo;
import org.wcs.smart.connect.api.model.UploadStatus;

/**
 * JAX-RS REST client api for SMART Connect requests 
 * 
 * @author Emily
 *
 */
public interface ConnectClient {
	
	public static final String USER_PATH = "connectuser";
	public static final String CA_PATH = "conservationarea";
	public static final String UPLOAD_PATH = "uploader";
	
	@GET
    @Path("/" + USER_PATH + "/{username}")
	@Produces(MediaType.APPLICATION_JSON)
    String getUser(@PathParam("username") String username);
	
	
	@GET
    @Path("/" + CA_PATH + "/{cauuid}")
    public ConservationAreaInfo getConservationArea(@PathParam("cauuid") String caUuid);
	
	@GET
	@Path("/" + UPLOAD_PATH + "/{uploaduuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public UploadStatus getStatus(@PathParam("uploaduuid") String uuid);
	
	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces({ MediaType.APPLICATION_JSON })
	public String updateFile(InputStream data);

	@POST
	@Path("/" + CA_PATH + "/{cauuid}")
	public Response getDataUploadUrl(@HeaderParam("X-Upload-Content-Length") Long length, 
			@PathParam("cauuid") String caUuid,
			@QueryParam("version") String versionUuid);
	
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
