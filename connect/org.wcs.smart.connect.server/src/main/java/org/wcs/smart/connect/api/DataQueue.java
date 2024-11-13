/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.cybertracker.json.importer.SmartMobileJsonProcessorManager;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItemProxy;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConnectSetting;
import org.wcs.smart.connect.model.ConnectSettingProxy;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * Data Processing Queue API functions
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + DataQueue.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class DataQueue {

	private final Logger logger = Logger.getLogger(DataQueue.class.getName());
	
	public static final String UPLOAD_CONTENT_LENGTH_HEADER = "X-Upload-Content-Length"; //$NON-NLS-1$
	
	public static final String PATH = "dataqueue"; //$NON-NLS-1$
	public static final String FILE_STORE_LOCATION = "dataqueue"; //$NON-NLS-1$
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	
	/**
	 * Configure the class parameters.  During normal use this will
	 * be done automatically.  This is only when you want to user this class
	 * outside for the REST workflow.
	 * 
	 * @param context
	 * @param headers
	 * @param response
	 * @param request
	 */
	public void configure(ServletContext context, HttpHeaders headers, HttpServletResponse response, HttpServletRequest request){
		this.context = context;
		this.headers= headers;
		this.request = request;
		this.response = response;
	}
	
	private void validateRead(UUID caUuid, Session session) throws SmartConnectException{
		if (!SecurityManager.INSTANCE.canAccess(session, 
				request.getUserPrincipal().getName(), 
				DataQueueAction.VIEW_KEY,
				caUuid)){
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	private void validateProcess(UUID caUuid, Session session) throws SmartConnectException{
		if (!SecurityManager.INSTANCE.canAccess(session, 
				request.getUserPrincipal().getName(), 
				DataQueueAction.PROCESS_KEY,
				caUuid)){
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	private void validateUpload(UUID caUuid, Session session) throws SmartConnectException{
		if (!SecurityManager.INSTANCE.canAccess(session, 
				request.getUserPrincipal().getName(), 
				DataQueueAction.ADD_KEY,
				caUuid)){
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
	
	private void validateDelete(UUID caUuid, Session session) throws SmartConnectException{
		if (!SecurityManager.INSTANCE.canAccess(session, 
				request.getUserPrincipal().getName(), 
				DataQueueAction.DELETE_KEY,
				caUuid)){
			throw new SmartConnectException(Response.Status.UNAUTHORIZED);
		}
	}
		
	/**
	 * <p>Get details of data queue items that match provided filter</p>
	 * 
	 * <p>
	 * URL: ../server/api/dataqueue/detailedItems/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param	cafilter	only show items from these CAs. A comma separated string of UUIDs.
	 * @param	status	comma separated list of status states to include.
	 * 
	 * @return Returns a list of JSON ServerDataQueueItemProxy objects. (<a href="https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/dataqueue/ServerDataQueueItemProxy.java">ServerDataQueueItemProxy</a>) 
	 */
	
	@GET
    @Path("/detailedItems")
	@Operation(description="Get details of data queue items that match provided filter")
	public List<ServerDataQueueItemProxy> getDetailedItems(
			@Parameter(description="only show items from these CAs. A comma separated string of UUIDs") @QueryParam("cafilter") String caFilter,
			@Parameter(description="comma separated list of status states to include.") @QueryParam("status") String status){
		List<ServerDataQueueItem.Status> statusFilter = null;
		List<ServerDataQueueItemProxy> proxyitems = new ArrayList<ServerDataQueueItemProxy>();
		getProxyItems(caFilter, status, statusFilter, proxyitems);
		Collections.sort(proxyitems);
		return proxyitems;
	}
		
	/**
	 * <p>Get data queue items that match given filter. This will only return items that the user has permission to see.
	 * <p>
	 * URL: ../server/api/dataqueue/items/<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param caFilter a comma delimited list of ca uuids (or null for all)
	 * @param status comma delimited list of status values to filter on or null for all
	 * @return a list of all data queue items sorted by date added represented by DataQueueItem.  
	 */
	@GET
    @Path("/items")
	@Operation(description="Get data queue items that match given filter. This will only return items that the user has permission to see")
	public List<DataQueueItem> getItems(
			@Parameter(description="a comma delimited list of Conservation Area uuids to filter on or NULL for all)") @QueryParam("cafilter") String caFilter,
			@Parameter(description="comma delimited list of status values to filter on or NULL for all") @QueryParam("status") String status){
		
		List<ServerDataQueueItem.Status> statusFilter = null;
		List<ServerDataQueueItemProxy> proxyitems = new ArrayList<ServerDataQueueItemProxy>();
		
		
		getProxyItems(caFilter, status, statusFilter, proxyitems);
		
		List<DataQueueItem> items = new ArrayList<DataQueueItem>();
		for (ServerDataQueueItemProxy item : proxyitems){
			DataQueueItem i = new DataQueueItem();
			i.setConservationArea(item.getConservationArea());
			i.setName(item.getName());
			i.setType(item.getType());
			i.setUuid(item.getUuid());
			items.add(i);
		}
		return items;
	}

	private void getProxyItems(String caFilter, String status, List<ServerDataQueueItem.Status> statusFilter,
			List<ServerDataQueueItemProxy> proxyitems) {
		if (status != null){
			statusFilter = new ArrayList<ServerDataQueueItem.Status>();
			for (String stat: status.split(",")){ //$NON-NLS-1$
				try{
					statusFilter.add(ServerDataQueueItem.Status.valueOf(stat));
				}catch (Exception ex){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, MessageFormat.format(Messages.getString("DataQueue.StatusfilterNotSupported", SmartUtils.getRequestLocale(request)), stat)); //$NON-NLS-1$
				}
			}
		}
		
		Session s = HibernateManager.getSession(context);

		try{
			s.beginTransaction();
			
			List<ConservationAreaInfo> cas = null;
			if (caFilter == null ){
				cas = QueryFactory.buildQuery(s, ConservationAreaInfo.class).list();
			}else{
				cas = new ArrayList<ConservationAreaInfo>();
				for (String x : caFilter.split(",")){ //$NON-NLS-1$
					try{
						UUID  cauuid = parseUuid(x);
						ConservationAreaInfo i = (ConservationAreaInfo) s.get(ConservationAreaInfo.class, cauuid);
						if (i != null){
							cas.add(i);
						}
					}catch (Exception ex){
						//failed to parse
						throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.InvalidCaInvalidUUID", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
					}
				}
			}
			for (ConservationAreaInfo ca : cas){
				try{
					validateRead(ca.getUuid(), s);
					
					List<ServerDataQueueItem> items = null;
					if(statusFilter == null) {
						items = s.createQuery("FROM ServerDataQueueItem WHERE conservationArea = :ca ", ServerDataQueueItem.class) //$NON-NLS-1$
								.setParameter("ca", ca.getUuid()) //$NON-NLS-1$
								.list();
					}else {
						items = s.createQuery("FROM ServerDataQueueItem WHERE conservationArea = :ca and status in (:status)", ServerDataQueueItem.class) //$NON-NLS-1$
								.setParameter("ca", ca.getUuid()) //$NON-NLS-1$
								.setParameter("status", statusFilter) //$NON-NLS-1$
								.list();
					}
					for (ServerDataQueueItem item : items){
						ServerDataQueueItemProxy proxy = new ServerDataQueueItemProxy(item, ca);
						proxyitems.add(proxy);
					}
				}catch (SmartConnectException ex){
					//no access; this is okay do not add these items
				}
			}
			proxyitems.sort((ServerDataQueueItemProxy d1, ServerDataQueueItemProxy d2) -> d1.getUploadedDate().compareTo(d2.getUploadedDate()));
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error reading data queue items.", ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, Messages.getString("DataQueue.ReadError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * <p>Deletes the given data queue item (and associated work item if applicable).</p>
	 * <p>URL: ../server/api/dataqueue/items/{uuid}<br>
	 * Call Type: DELETE
	 * </p>
	 * 
	 * @param uuid of the data queue item to delete
	 */
	@DELETE
    @Path("/items/{uuid}")
	@Operation(description="Deletes the given data queue item (and associated work item if applicable).")
	public ServerDataQueueItem deleteItem(@Parameter(description="uuid of the data queue item to delete") @PathParam("uuid") String uuid){
		
		UUID itemUuid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ServerDataQueueItem item = (ServerDataQueueItem)s.get(ServerDataQueueItem.class, itemUuid);
			
			validateDelete(item.getConservationArea(), s);
			if (item.getFile() != null){
				java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getFile(item.getFile());
				if (Files.exists(toDelete)){
					try {
						Files.delete(toDelete);
					}catch (Exception ex) {
						logger.log(Level.WARNING, "Could not delete data queue file: " + toDelete.toString(), ex); //$NON-NLS-1$
					}
				}else{
					logger.log(Level.WARNING, "Data queue file does not exist to delete: " + toDelete.toString()); //$NON-NLS-1$
				}
			}
			s.remove(item);
			
			//also attempt to delete associated work item
			if (item.getWorkItem() != null){
				WorkItem i = (WorkItem) s.get(WorkItem.class, item.getWorkItem());
				if (i != null){
					//might be null if deleted for somes other reason
					s.remove(i);
				}
			}
			s.getTransaction().commit();
			
			ServerDataQueueItem i2 = new ServerDataQueueItem();
			i2.setUuid(item.getUuid());
			i2.setConservationArea(item.getConservationArea());
			DataQueueEventService.addUpdateToQueue(i2);
			
			return item;
		}catch(Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Error deleting data queue item: " +ex.getMessage(), ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("DataQueue.DeleteError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
	
	/**
	 * <p>Creates an item and associated work item for file uploading.  Returns the URL that
	 * can be used for uploading the file.</p>
	 * <p>URL: ../server/api/dataqueue/items/<br>
	 * Call Type: POST<br>
	 * Headers: X-Upload-Content-Length - the size in bytes of the file to upload <br>
	 * Payload: a ServerDataQueueItem object
	 * </p>
	 *<pre>{
	 *   "conservationArea":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce",
	 *   "type":"PATROL_XML",
	 *   "name":"patrol1234.xml"
	 *}</pre>
	 * 
	 * @param item - not used in the web API, use the PST payload to provide details of what you want created, see above.
	 * @return the location of where to upload the file in the "location" header, in javascript you can get it like: oReq.getResponseHeader("location");
	 * where oReq is the XMLHttpRequest object used to post this request.
	 */
	@POST
    @Path("/items/")
	@Operation(description="Creates an item and associated work item for file uploading.  Returns the URL that can be used for uploading the file. Call Type: POST<br>\r\n" + 
			"	 * Headers: X-Upload-Content-Length - the size in bytes of the file to upload <br>\r\n" + 
			"	 * Payload: a ServerDataQueueItem object\r\n" + 
			"	 * </p>\r\n" + 
			"	 *<pre>{\r\n" + 
			"	 *   \"conservationArea\":\"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce\",\r\n" + 
			"	 *   \"type\":\"PATROL_XML\",\r\n" + 
			"	 *   \"name\":\"patrol1234.xml\"\r\n" + 
			"	 *}</pre>")
	public String createItem(ServerDataQueueItem item){
		
		if (item.getConservationArea() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.CaNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (item.getType() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.TypeNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		if (headers.getRequestHeader(UPLOAD_CONTENT_LENGTH_HEADER) == null || headers.getRequestHeader(UPLOAD_CONTENT_LENGTH_HEADER).isEmpty()) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length header required"); //$NON-NLS-1$
		}
		String lengthHeader = headers.getRequestHeader(UPLOAD_CONTENT_LENGTH_HEADER).get(0);
		long totalBytes = -1;
		try{
			totalBytes = Long.parseLong(lengthHeader);
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value", ex); //$NON-NLS-1$
		}
		if (totalBytes <= 0){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value"); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			validateUpload(item.getConservationArea(), s);
			
			ConservationAreaInfo ca = (ConservationAreaInfo)s.get(ConservationAreaInfo.class, item.getConservationArea());
			if (ca == null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.InvalidCA", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
			}
			if (ca.getStatus() == ConservationAreaInfo.Status.CCAA){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.InvalidCaCCA", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			item.setStatus(ServerDataQueueItem.Status.UPLOADING);
			item.setStatusMessage(null);
			item.setUploadedBy(request.getUserPrincipal().getName());
			item.setUploadedDate(ZonedDateTime.now());
				
			WorkItem up = new WorkItem();
			up.setLocale(request.getLocale());
			up.setConservationAreaInfo(ca);
			up.setStartTime(LocalDateTime.now());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_DATAQUEUE);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename(""); //$NON-NLS-1$
			up.setUsername(request.getUserPrincipal().getName());
			up.setIp(request.getRemoteAddr());
			s.persist(up);
			
			java.nio.file.Path updir = DataStoreManager.INSTANCE.getFile(FILE_STORE_LOCATION);
			if (!Files.exists(updir)){
				Files.createDirectories(updir);
			}

			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName(FILE_STORE_LOCATION 
					+ File.separator + UuidUtils.uuidToString(up.getUuid()) + ".file")); //$NON-NLS-1$
			item.setFile(up.getLocalFilename());
			item.setWorkItem(up.getUuid());
			
			s.persist(item);
			
			//we have a file to upload and we expect more
			
			response.setHeader(HttpHeaders.LOCATION, up.getStatusURL(request));
			response.setHeader(HttpHeaders.CONTENT_LENGTH, "0"); //$NON-NLS-1$
			
			s.getTransaction().commit();
			
			DataQueueEventService.addUpdateToQueue(item);
			
		}catch (SmartConnectException ex){
			logger.log(Level.WARNING, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Messages.getString("ConservationAreas.UploadErr", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}
	
	/**
	 * <p>Gets a particular data queue item.</p>
	 * <p>
	 * URL: ../server/api/dataqueue/items/{uuid}<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param uuid	provided in the URL, the uuid of the item requested.
	 * @return JSON representation of a ServerDataQueueItem object. (<a href="https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/dataqueue/ServerDataQueueItem.java">ServerDataQueueItem</a>) 
	 */
	@GET
	@Path("/items/{uuid}")
	@Operation(description="Gets a particular data queue item.")
	public ServerDataQueueItem getItem(@Parameter(description="the uuid of the item requested") @PathParam("uuid") String uuid){
		UUID itemUuid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			ServerDataQueueItem item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, itemUuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			validateRead(item.getConservationArea(), s);
			return item;
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not get data queue item: " +uuid, ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, MessageFormat.format(Messages.getString("DataQueue.ItemNotFound", SmartUtils.getRequestLocale(request)), uuid), ex);	 //$NON-NLS-1$
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * <p>Gets a preview for the specific data queue item.</p>
	 * <p>
	 * URL: ../server/api/dataqueue/items/{uuid}/preview<br>
	 * Call Type: GET
	 * </p>
	 * 
	 * @param uuid	provided in the URL, the uuid of the item requested.
	 * @return a string maximum of 10_000 characters for preview 
	 */
	@GET
	@Path("/items/{uuid}/preview")
	@Operation(description="Gets preview for a particular data queue item.")
	public String getItemPreview(@Parameter(description="the uuid of the item requested") @PathParam("uuid") String uuid){
		UUID itemUuid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			ServerDataQueueItem item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, itemUuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			validateRead(item.getConservationArea(), s);
			
			//read the first 10,000 characters from the files
			java.nio.file.Path file = DataStoreManager.INSTANCE.getFile(item.getFile());
			if (!Files.exists(file)){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.DqFileNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}

			try(InputStream reader = Files.newInputStream(file)){
				byte[] data = new byte[10_000];
				int length = reader.read(data);
				return new String(data, 0, length);
			}
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not get data queue item: " +uuid, ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw (SmartConnectException)ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, MessageFormat.format(Messages.getString("DataQueue.ItemNotFound", SmartUtils.getRequestLocale(request)), uuid), ex);	 //$NON-NLS-1$
		}finally{
			s.getTransaction().rollback();
		}
	}
	
	/**
	 * <p>Updates the status of a data queue item</p>
	 * <p>
	 * URL: ../server/api/dataqueue/items/{uuid}/status/{status}<br>
	 * Call Type: PUT
	 * </p>
	 * 
	 * @param uuid	the uuid from the item to update
	 * @param newStatus the new status from the data queue item
	 * @return JSON representation of a DataQueueItem object 
	 */
	@PUT
	@Path("/items/{uuid}/status/{status}")
	@Operation(description="Updates the status of a data queue item")
	public DataQueueItem updateItemStatus(@Parameter(description="the uuid from the item to update") @PathParam("uuid") String itemUuid, 
			@Parameter(description="the new status from the data queue item") @PathParam("status") String newStatus){
		
		ServerDataQueueItem.Status serverStatus = null;
		try{
			serverStatus = ServerDataQueueItem.Status.valueOf(newStatus.toUpperCase(Locale.ROOT));
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, MessageFormat.format(Messages.getString("DataQueue.StatusValueNotSupport", SmartUtils.getRequestLocale(request)), newStatus)); //$NON-NLS-1$
		}
		
		UUID uuid = parseUuid(itemUuid);
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ServerDataQueueItem item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, uuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.ItemNotFound2", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateProcess(item.getConservationArea(), s);
			
			if (serverStatus == ServerDataQueueItem.Status.PROCESSING 
					&& item.getStatus() != ServerDataQueueItem.Status.QUEUED){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.AlreadyProcessed", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			item.setStatus(serverStatus);
			s.getTransaction().commit();
			
			DataQueueEventService.addUpdateToQueue(item);
			
			return item;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, Messages.getString("DataQueue.Error2", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex);
		}
	}
	
	/**
	 * <p>Updates the type and status of a data queue item</p>
	 * <p>
	 * URL: ../server/api/dataqueue/items/{uuid}<br>
	 * Call Type: PUT<br>
	 * Payload: JSON of the attributes to update, ex
	 * </p>
	 * <pre>{
	 *   "type":"MISSION_XML",
	 *   "status":"QUEUED"
	 * }</pre>
	 * 
	 * <p>
	 * Type options: PATROL_XML, MISSION_XML, INCIDENT_XML<br>
	 * Status options: QUEUED, COMPLETE, ERROR
	 * </p>
	 * 
	 * @param uuid	the uuid of the item to update, from the URL
	 * @return a JSON representation of the modified DataQueueItem object.
	 */
	@Consumes({ MediaType.APPLICATION_JSON})
	@PUT
	@Path("/items/{uuid}/")
	@Operation(description="Updates the type and status of a data queue item, e.g. {\r\n" + 
			"	 *   \"type\":\"MISSION_XML\",\r\n" + 
			"	 *   \"status\":\"QUEUED\"\r\n" + 
			"	 * }")
	public DataQueueItem updateItem(@Parameter(description="the uuid of the item to update") @PathParam("uuid") String itemUuid, ServerDataQueueItem newItem){ 

		UUID uuid = parseUuid(itemUuid);
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ServerDataQueueItem item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, uuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.ItemNotFound1", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateProcess(item.getConservationArea(), s);
			
			if (newItem.getStatus() == ServerDataQueueItem.Status.PROCESSING 
					&& item.getStatus() != ServerDataQueueItem.Status.QUEUED){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.ItemAlreadyProcessed", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			item.setStatus(newItem.getStatus());
			item.setType(newItem.getType());
			s.getTransaction().commit();
			
			DataQueueEventService.addUpdateToQueue(item);
			
			return item;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Could not update status. ", ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex);
		} 
		
	}
	

	/**
	 *  <p>Get the data queue settings</p>
	 *  
	 */
	@GET
    @Path("/settings")
	@Operation(description="Get data queue processing settings")
    public List<ConnectSettingProxy> getSettings(){
		
		String[] keys = new String[ConnectSetting.DQ_SETTINGS.length];
		for (int i = 0; i < keys.length; i ++) {
			keys[i] = ConnectSetting.DQ_SETTINGS[i].key;
		}
		
		List<ConnectSettingProxy> settings = new ArrayList<>();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<ConnectSetting> items = s.createQuery("FROM ConnectSetting WHERE key IN (:keys)", ConnectSetting.class) //$NON-NLS-1$
				.setParameterList("keys", keys) //$NON-NLS-1$
				.list();
			for (ConnectSetting setting : items) {
				ConnectSettingProxy p = new ConnectSettingProxy(setting.getKey(),setting.getValue(), null, null);
				settings.add(p);
			}
			
			List<ConservationAreaInfo> infos = s.createQuery("FROM ConservationAreaInfo where uuid != :ccaa", ConservationAreaInfo.class) //$NON-NLS-1$
					.setParameter("ccaa", ConservationArea.MULTIPLE_CA) //$NON-NLS-1$
					.list();
			infos.sort((a,b)->Collator.getInstance().compare(a.getLabel(), b.getLabel()));
			
			for (ConservationAreaInfo info : infos) {
				Boolean value = info.getSmartMobileDqProcessor();
				if (value == null) value = Boolean.TRUE;
				String key =ConnectSetting.CA_PROCESSING_OP_PREFIX + UuidUtils.uuidToString(info.getUuid());
				
				ConnectSettingProxy temp = new ConnectSettingProxy(key, value ? "true" : "false",  //$NON-NLS-1$ //$NON-NLS-2$
						info.getLabel(), Messages.getString("DataQueue.smartProcessingSettingTooltip", request.getLocale()));  //$NON-NLS-1$
				settings.add(temp);
			}
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Unable to get settings." + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST);  
		}finally {
			s.getTransaction().rollback();
		}
		return settings;
	}
	
	/**
	 *  <p>Update a data queue settings</p>
	 *  
	 */
	@PUT
    @Path("/settings/{setting}")
	@Operation(description="Updates the given setting")
	@Consumes({ MediaType.TEXT_PLAIN})
    public void updateSetting(
    		@Parameter(description="Setting key to update") @PathParam("setting") String settingKey,
    		String newValue){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Response.Status.FORBIDDEN);
			}
			
			
			if (settingKey.startsWith(ConnectSetting.CA_PROCESSING_OP_PREFIX)) {
				
				String ca = settingKey.substring(ConnectSetting.CA_PROCESSING_OP_PREFIX.length());
				UUID cauuid = UuidUtils.stringToUuid(ca);
				
				ConservationAreaInfo cainfo = s.get(ConservationAreaInfo.class, cauuid);
				if (cainfo == null) {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.InvalidSettingKey", request.getLocale())); //$NON-NLS-1$
				}
			
				Boolean updatedValue = Boolean.TRUE;
				if (newValue.strip().equalsIgnoreCase("false")) { //$NON-NLS-1$
					updatedValue = Boolean.FALSE;
				}
				cainfo.setSmartMobileDqProcessor(updatedValue);
				
			}else {
				
				ConnectSetting.Setting key = null;
				for (ConnectSetting.Setting setting : ConnectSetting.DQ_SETTINGS) {
					if (setting.key.equalsIgnoreCase(settingKey)) {
						key = setting;
						break;
					}
				}
				if (key == null) {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.InvalidSettingKey", request.getLocale())); //$NON-NLS-1$
				}
				
				String updatedValue = null;
				
				if (key == ConnectSetting.Setting.DQ_SMART_COLLECT_USEROPTION) {
					for (ConnectSetting.SmartCollectUserOption op : ConnectSetting.SmartCollectUserOption.values()) {
						if (op.key.equalsIgnoreCase(newValue.strip())) {
							updatedValue = op.key;
							break;
						}
					}
				}else {
					updatedValue = newValue.strip();
				}
				
				if (updatedValue== null) {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.InvalidSettingValue", request.getLocale())); //$NON-NLS-1$
				}
				
				ConnectSetting setting = s.get(ConnectSetting.class, key.key);
				if (setting == null) {
					setting = new ConnectSetting();
					setting.setKey(key.key);
					s.persist(setting);
				}
				setting.setValue(updatedValue);
			}
			s.getTransaction().commit();
			
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Unable to update settings." + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
			
		}
	}
	
	/**
	 *  <p>Update a data queue settings</p>
	 *  
	 */
	@PUT
    @Path("/processing/start")
	@Operation(description="Starts data queue processing if not already started")
	@Consumes({ MediaType.TEXT_PLAIN})
    public void startProcessing(){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try {
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
				throw new SmartConnectException(Response.Status.FORBIDDEN);
			}
		}finally {
			s.getTransaction().rollback();
		}
		SmartMobileJsonProcessorManager.INSTANCE.startProcessing(HibernateManager.getSessionFactory(context));
	}
	
	/**
	 *  <p>Used by SMART Desktop Only - not to be used by api call</p>
	 *  
	 */

	@GET
    @Path("/items/{uuid}/file")
	@Operation(description="Used by SMART Desktop Only")
    public Response getDataQueueItemFile(@Parameter(description="item UUID") @PathParam("uuid") String itemUuid){
		UUID uuid = parseUuid(itemUuid);
	
		ServerDataQueueItem item = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, uuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.ItemNotFound1", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			validateProcess(item.getConservationArea(), s);
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Unable to get download file." + ex.getMessage(), ex); //$NON-NLS-1$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.DownloadFileNotFound", SmartUtils.getRequestLocale(request)), ex);  //$NON-NLS-1$
		}
	
		//item.getStatus()
		if (item.getFile() == null){
			throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.DqFileNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		final java.nio.file.Path toReturn = DataStoreManager.INSTANCE.getFile(item.getFile());
		if (!Files.exists(toReturn)){
			throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.DqFileNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		long fileSize = -1;
		try {
			fileSize = Files.size(toReturn);
		}catch (IOException ex) {
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage(),ex);
		}
		String range = request.getHeader("Range"); //$NON-NLS-1$
		long start = 0;
		long end = fileSize-1;
		boolean hasRange = false;
		if (range != null && range.length() > 0){
			hasRange = true;
			//parse the range
			try{
				String regex = "^bytes=([0-9]*)-([0-9]*)$"; //$NON-NLS-1$
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(range);
				
				String p1 = m.group(0);
				String p2 = m.group(1);
				if (!p1.isEmpty()){
					start = Long.parseLong(p1);
				}
				if (!p2.isEmpty()){
					end = Long.parseLong(p2);
				}
				
				if (end > Files.size(toReturn)-1){
					throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, Messages.getString("DataQueue.InvalidRange", SmartUtils.getRequestLocale(request)));	 //$NON-NLS-1$
				}
				if (start > end){
					throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, Messages.getString("DataQueue.InvalidStart", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, Messages.getString("DataQueue.InvalidRange2", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
		}

		if (!hasRange){
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try {
						Files.copy(toReturn, output);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error writing to output stream." + e.getMessage(), e); //$NON-NLS-1$
					}
				}
		    };
			    
			return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getFileName().toString() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.header(HttpHeaders.CONTENT_LENGTH, fileSize)
					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
					.build();
		}else{
			final long startat = start;
			final long endat = end;
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try (InputStream in = Files.newInputStream(toReturn)){
						IOUtils.copyLarge(in, output, startat, endat - startat + 1);
					}
				}
		    };
			    
			return Response.status(Response.Status.PARTIAL_CONTENT)
					.entity(stream)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getFileName().toString() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.header(HttpHeaders.CONTENT_LENGTH, end - start + 1)
					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
					.header("Content-Range", "bytes " + start + "-" + end + "/" + fileSize) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					.build();
		}
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
