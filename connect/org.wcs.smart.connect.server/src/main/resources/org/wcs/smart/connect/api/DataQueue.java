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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItemProxy;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Data Processing Queue API functions
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + DataQueue.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class DataQueue {

	private final Logger logger = Logger.getLogger(DataQueue.class.getName());
	
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
	 * Get Detailed Data Queue Items
	 * URL: ../server/api/dataqueue/detailedItems/
	 * Call Type: GET
	 * 
	 * @param	cafilter	only show items from these CAs. A comma separated string of UUIDs.
	 * @param	status	comma separated list of status states to include.
	 * 
	 * @return Returns a list of JSON ServerDataQueueItemProxy objects. (https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/dataqueue/ServerDataQueueItemProxy.java) 
	 */
	
	@GET
    @Path("/detailedItems")
	public List<ServerDataQueueItemProxy> getDetailedItems(@QueryParam("cafilter") String caFilter,
			@QueryParam("status") String status){
		List<ServerDataQueueItem.Status> statusFilter = null;
		List<ServerDataQueueItemProxy> proxyitems = new ArrayList<ServerDataQueueItemProxy>();
		getProxyItems(caFilter, status, statusFilter, proxyitems);
		Collections.sort(proxyitems);
		return proxyitems;
	}
		
	/**
	 * Get data Queue Items
	 * This will only return items that the user has permission to see.
	 * URL: ../server/api/dataqueue/items/
	 * Call Type: GET
	 * 
	 * @param caFilter a comma delimited list of ca uuids (or null for all)
	 * @param status comma delimited list of status values to filter on or null for all
	 * @return a list of all data queue items sorted by date added.  
	 */
	@GET
    @Path("/items")
	public List<DataQueueItem> getItems(@QueryParam("cafilter") String caFilter,
			@QueryParam("status") String status){
		
		List<ServerDataQueueItem.Status> statusFilter = null;
		List<ServerDataQueueItemProxy> proxyitems = new ArrayList<ServerDataQueueItemProxy>();
		
		
		getProxyItems(caFilter, status, statusFilter, proxyitems);
		
		List<DataQueueItem> items = new ArrayList<DataQueueItem>();
		for (ServerDataQueueItemProxy item : proxyitems){
			items.add(item);
		}
		return items;
	}

	@SuppressWarnings("unchecked")
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
				cas = s.createCriteria(ConservationAreaInfo.class).list();
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
					
					Criteria c = s.createCriteria(ServerDataQueueItem.class)
							.add(Restrictions.eq("conservationArea", ca.getUuid())); //$NON-NLS-1$
					if (statusFilter != null){
						c.add(Restrictions.in("status", statusFilter)); //$NON-NLS-1$
					}
					
					List<ServerDataQueueItem> items = c.list();
					for (ServerDataQueueItem item : items){
						ServerDataQueueItemProxy proxy = new ServerDataQueueItemProxy(
								item.getUuid(), item.getName(), ca.getUuid(), ca.getLabel(),
								item.getType(), item.getStatus(), 
								item.getLastModified(), item.getUploadedDate(), 
								item.getUploadedBy());
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
	 * Deletes the given data queue item (and associated work item if applicable).
	 * URL: ../server/api/dataqueue/items/{uuid}
	 * Call Type: DELETE
	 * 
	 * @param uuid
	 */
	@DELETE
    @Path("/items/{uuid}")
	public ServerDataQueueItem deleteItem(@PathParam("uuid") String uuid){
		
		UUID itemUuid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ServerDataQueueItem item = (ServerDataQueueItem)s.get(ServerDataQueueItem.class, itemUuid);
			
			validateDelete(item.getConservationArea(), s);
			if (item.getFile() != null){
				File toDelete = DataStoreManager.INSTANCE.getFile(item.getFile());
				if (toDelete.exists()){
					if (!toDelete.delete()){
						logger.log(Level.WARNING, "Could not delete data queue file: " + toDelete.toString()); //$NON-NLS-1$
					}
				}else{
					logger.log(Level.WARNING, "Data queue file does not exist to delete: " + toDelete.toString()); //$NON-NLS-1$
				}
			}
			s.delete(item);
			
			//also attempt to delete associated work item
			if (item.getWorkItem() != null){
				WorkItem i = (WorkItem) s.get(WorkItem.class, item.getWorkItem());
				if (i != null){
					//might be null if deleted for some other reason
					s.delete(i);
				}
			}
			s.getTransaction().commit();
			return item;
		}catch(Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Error deleting data queue item: " +ex.getMessage(), ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("DataQueue.DeleteError", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
	}
	
	/**
	 * Creates an item and associated work item for file uploading.  Returns the URL that
	 * can be used for uploading the file.
	 * URL: ../server/api/dataqueue/items/
	 * Call Type: POST
	 * 
	 * 
	 * @param item
	 * @return the location of where to upload the file in the "location" header, in javascript you can get it like: oReq.getResponseHeader("location");
	 * where oReq is the XMLHttpRequest object used to post this request.
	 */
	@POST
    @Path("/items/")
	public String createItem(ServerDataQueueItem item){
		
		if (item.getConservationArea() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.CaNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		if (item.getType() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.TypeNotProvided", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
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
			String lengthHeader = headers.getRequestHeader("X-Upload-Content-Length").get(0); //$NON-NLS-1$
			if (lengthHeader == null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length not set"); //$NON-NLS-1$
			}
			long totalBytes = -1;
			try{
				totalBytes = Long.parseLong(lengthHeader);
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value", ex); //$NON-NLS-1$
			}
			if (totalBytes <= 0){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "X-Upload-Content-Length invalid value"); //$NON-NLS-1$
			}
			
			item.setStatus(ServerDataQueueItem.Status.UPLOADING);
			item.setStatusMessage(null);
			item.setUploadedBy(request.getUserPrincipal().getName());
			item.setUploadedDate(new Date());
				
			WorkItem up = new WorkItem();
			up.setLocale(request.getLocale());
			up.setConservationAreaInfo(ca);
			up.setStartTime(new Date());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_DATAQUEUE);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename(""); //$NON-NLS-1$
			s.save(up);
			
			File updir = DataStoreManager.INSTANCE.getFile(FILE_STORE_LOCATION);
			if (!updir.exists()){
				FileUtils.forceMkdir(updir);
			}

			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName(FILE_STORE_LOCATION 
					+ File.separator + UuidUtils.uuidToString(up.getUuid()) + ".file")); //$NON-NLS-1$
			item.setFile(up.getLocalFilename());
			item.setWorkItem(up.getUuid());
			
			s.saveOrUpdate(item);
			s.saveOrUpdate(up);
			
			//we have a file to upload and we expect more
			
			response.setHeader(HttpHeaders.LOCATION, up.getStatusURL(request));
			response.setHeader(HttpHeaders.CONTENT_LENGTH, "0"); //$NON-NLS-1$
			
			s.getTransaction().commit();
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
	 * Gets a particular data queue item.
	 * URL: ../server/api/dataqueue/items/{uuid}
	 * Call Type: GET
	 * 
	 * @param uuid	provided in the URL, the uuid of the item requested.
	 * @return JSON representation of a ServerDataQueueItem object. (https://www.assembla.com/spaces/smart-cs/subversion-2/source/HEAD/trunk/connect/org.wcs.smart.connect.server/src/main/resources/org/wcs/smart/connect/dataqueue/ServerDataQueueItem.java) 
	 */
	@GET
	@Path("/items/{uuid}")
	public ServerDataQueueItem getItem(@PathParam("uuid") String uuid){
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
	
		
//TODO Delete this method, the one below seems like it is a superset of the functionality. 
//Need to check if the desktop and web applications use this still and move them over if they do.
	
	
	@PUT
	@Path("/items/{uuid}/status/{status}")
	public DataQueueItem updateItemStatus(@PathParam("uuid") String itemUuid, 
			@PathParam("status") String newStatus){
		
		ServerDataQueueItem.Status serverStatus = null;
		try{
			serverStatus = ServerDataQueueItem.Status.valueOf(newStatus.toUpperCase());
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
			return item;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, Messages.getString("DataQueue.Error2", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex);
		}
	}
	
	/**
	 * Updates the type and status of a data queue item
	 * URL: ../server/api/dataqueue/items/{uuid}
	 * Call Type: PUT
	 * 
	 * Payload: JSON of the attributes to update, ex:
	 * 		{"type":"MISSION_XML","status":"QUEUED"}
	 * 
	 * type options: PATROL_XML, MISSION_XML, INCIDENT_XML
	 * Status options: QUEUED, COMPLETE, ERROR
	 * 
	 * @param uuid	the uuid of the item to update, from the URL
	 * @return a JSON representation of the modified DataQueueItem object.
	 */
	@Consumes({ MediaType.APPLICATION_JSON})
	@PUT
	@Path("/items/{uuid}/")
	public DataQueueItem updateItem(@PathParam("uuid") String itemUuid, ServerDataQueueItem newItem){ 

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
			return item;
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Could not update status. ", ex); //$NON-NLS-1$
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex);
		}
	}
	
	
	/* Used by SMART Desktop Only
	 */

	@GET
    @Path("/items/{uuid}/file")
    public Response getDataQueueItemFile(@PathParam("uuid") String itemUuid){
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
		
		final File toReturn = DataStoreManager.INSTANCE.getFile(item.getFile());
		if (!toReturn.exists()){
			throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataQueue.DqFileNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		String range = request.getHeader("Range"); //$NON-NLS-1$
		long start = 0;
		long end = toReturn.length()-1;
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
				
				if (end > toReturn.length()-1){
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
						Files.copy(toReturn.toPath(), output);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error writing to output stream." + e.getMessage(), e); //$NON-NLS-1$
					}
				}
		    };
			    
			return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getName() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.header(HttpHeaders.CONTENT_LENGTH, toReturn.length())
					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
					.build();
		}else{
			final long startat = start;
			final long endat = end;
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try (InputStream in = Files.newInputStream(toReturn.toPath())){
						IOUtils.copyLarge(in, output, startat, endat - startat + 1);
					}
				}
		    };
			    
			return Response.status(Response.Status.PARTIAL_CONTENT)
					.entity(stream)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getName() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
					.header(HttpHeaders.CONTENT_LENGTH, end - start + 1)
					.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
					.header("Content-Range", "bytes " + start + "-" + end + "/" + toReturn.length()) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
