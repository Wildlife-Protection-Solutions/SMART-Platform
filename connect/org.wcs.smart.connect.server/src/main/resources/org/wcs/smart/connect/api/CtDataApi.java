/* Copyright (C) 2015 Wildlife Conservation Society
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.GeoJsonFeatureCollection;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.security.SecurityManager;


/**
 * SMART Connect REST API for Accepting Data from Cybertracker mobile devices.
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + CtDataApi.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class CtDataApi extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String PATH = "ctdata"; //$NON-NLS-1$

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;

	private void validateUpload(UUID caUuid, Session session) throws SmartConnectException{
		if (!SecurityManager.INSTANCE.canAccess(session, 
				request.getUserPrincipal().getName(), 
				DataQueueAction.ADD_KEY,
				caUuid)){
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	/**
	 * Post a new data packet
	 * URL: ../server/api/ctdata/
	 * Call Type: POST
	 * 
	 * @return a string 
	 * 
	 */
	@POST
    @Path("")
    public String uploadPacket(GeoJsonFeatureCollection packet){
		
		
		if (packet.getConservationAreaUUID() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.CaNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			validateUpload(packet.getConservationAreaUUID(), s);
		}catch (Exception e){
			e.printStackTrace();
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Error Writing CT data to disk");
		}finally{
			s.getTransaction().commit();
		}
			
		DataQueue dataQ = new DataQueue();
		dataQ.configure(this.context, null , this.response, this.request);
			
		ServerDataQueueItem item = new ServerDataQueueItem();
		item.setConservationArea(packet.getConservationAreaUUID());
			
		//TODO create this new type, then uncomment the line below:
		//item.setType(DataQueueItem.Type.CYBERTRACKER_GEOJSON);
		item.setType(DataQueueItem.Type.PATROL_XML);
			
		String name = packet.getname();
		item.setName(name);
		
		dataQ.createItem(item);
		
			
		WorkItem i = (WorkItem) s.get(WorkItem.class, item.getWorkItem());
		File datastoreFile = DataStoreManager.INSTANCE.getFile(i.getLocalFilename());
		try(OutputStream out = Files.newOutputStream(datastoreFile.toPath(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)){
			InputStream data = new ByteArrayInputStream(packet.toString().getBytes()); 
			IOUtils.copy(data, out);
		} catch (IOException e) {
			e.printStackTrace();
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Error Writing CT data to disk"); 
		}
		
		return "";
	}

	
}
