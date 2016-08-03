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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem.Status;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.util.UuidUtils;


/**
 * SMART Connect REST API for Accepting patrol/survey data from 
 * Cybertracker mobile devices.
 * 
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + CtDataApi.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class CtDataApi extends HttpServlet {
	
	public static final String PATH = "ctdata"; //$NON-NLS-1$
	
	private static final long serialVersionUID = 1L;
	
	private final Logger logger = Logger.getLogger(DataQueue.class.getName());
	
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
	 * URL: ../server/api/ctdata/{cauuid{
	 * Call Type: POST
	 * 
	 * Payload: A JSON object representing a collection of cybertracker
	 * observations
	 *
	 * 
	 */
	@POST
    @Path("/{cauuid}")
    public Response uploadPacket(@PathParam("cauuid") String caUuid, InputStream data){
		
		UUID ca = parseUuid(caUuid);
		
		ServerDataQueueItem item = new ServerDataQueueItem();
		
		Session s = HibernateManager.getSession(context);
		
		s.beginTransaction();
		try{
			validateUpload(ca, s);
			
			item.setConservationArea(ca);
			item.setName("CyberTracker " + DateFormat.getDateTimeInstance().format(new Date())); //$NON-NLS-1$
			if (request.getHeader(HttpHeaders.CONTENT_ENCODING) != null && request.getHeader(HttpHeaders.CONTENT_ENCODING).equalsIgnoreCase("deflate")){
				item.setType(DataQueueItem.Type.JSON_ZLIB_CT);
			}else{
				item.setType(DataQueueItem.Type.JSON_CT);
			}
			item.setFile(null);
			item.setStatus(Status.UPLOADING);
			item.setStatusMessage(null);
			item.setUploadedBy(request.getUserPrincipal().getName());
			item.setUploadedDate(new Date());
			item.setWorkItem(null);
		
			s.save(item);
			s.getTransaction().commit();
		}catch (Exception ex){
			try{
				if (s.getTransaction().isActive())s.getTransaction().rollback();
			}catch(Exception ex2){
				logger.log(Level.SEVERE, ex.getMessage(), ex2);	
			}
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CtDataApi.CreateError", request.getLocale()) + ex.getMessage()); //$NON-NLS-1$
		}
			
		
		//upload file
		Exception thrown = null;
		String localName = DataStoreManager.INSTANCE.generateFileName(DataQueue.FILE_STORE_LOCATION 
				+ File.separator + UuidUtils.uuidToString(item.getUuid()) + ".file"); //$NON-NLS-1$
		item.setFile(localName);
		
		try(Writer out = Files.newBufferedWriter(DataStoreManager.INSTANCE.getFile(item.getFile()).toPath(), StandardOpenOption.CREATE)){ 
			IOUtils.copy(data, out, StandardCharsets.UTF_8);
			item.setStatus(Status.QUEUED);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			item.setStatus(Status.ERROR);
			item.setStatusMessage(Messages.getString("CtDataApi.WriteError", request.getLocale()) + ex.getMessage()); //$NON-NLS-1$
			thrown = ex;
		}
			
		//update item status 
		s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			s.saveOrUpdate(item);
			s.getTransaction().commit();
		}catch (Exception ex){
			try{
				if (s.getTransaction().isActive())s.getTransaction().rollback();
			}catch(Exception ex2){
				logger.log(Level.SEVERE, ex.getMessage(), ex2);	
			}
			
			logger.log(Level.SEVERE, "Error committing item status update: " + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CtDataApi.UpdateError", request.getLocale())); //$NON-NLS-1$
		}
		if (thrown != null)
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CtDataApi.WriteError", request.getLocale()) + thrown.getMessage()); //$NON-NLS-1$
		
		return Response.ok().
	            build();
		
	}

	private UUID parseUuid(String uuid) throws SmartConnectException{
		UUID itemUuid = null;
		try{
			itemUuid= UuidUtils.stringToUuid(uuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + uuid + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.BadRequest", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		return itemUuid;
	}
}
