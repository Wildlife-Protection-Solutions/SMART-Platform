package org.wcs.smart.connect.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
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
import org.hibernate.criterion.Order;
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

@Path(ConnectRESTApplication.PATH_SEPERATOR + DataQueue.PATH)
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class DataQueue {

	private final Logger logger = Logger.getLogger(DataQueue.class.getName());
	
	public static final String PATH = "dataqueue"; //$NON-NLS-1$
	
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
	 * This will only return items that the user has permission to see.
	 * 
	 * @param caFilter a comma delimited list of ca uuids (or null for all)
	 * @param status comma delimited list of status values to filter on or null for all
	 * @return a list of all data queue items sorted by date added
	 */
	@GET
    @Path("/items")
	public List<DataQueueItem> getItems(@QueryParam("cafilter") String caFilter ,
			@QueryParam("status") String status){
		
		List<ServerDataQueueItem.Status> statusFilter = null;
		if (status != null){
			statusFilter = new ArrayList<ServerDataQueueItem.Status>();
			for (String stat: status.split(",")){
				try{
					statusFilter.add(ServerDataQueueItem.Status.valueOf(stat));
				}catch (Exception ex){
					throw new SmartConnectException(Response.Status.BAD_REQUEST, "Status filter " + stat + " not supported.");
				}
			}
		}
		
		Session s = HibernateManager.getSession(context);
		List<DataQueueItem> proxyitems = new ArrayList<DataQueueItem>();
		try{
			s.beginTransaction();
			
			List<ConservationAreaInfo> cas = null;
			if (caFilter == null ){
				cas = s.createCriteria(ConservationAreaInfo.class).list();
			}else{
				cas = new ArrayList<ConservationAreaInfo>();
				for (String x : caFilter.split(",")){
					try{
						UUID  cauuid = parseUuid(x);
						ConservationAreaInfo i = (ConservationAreaInfo) s.get(ConservationAreaInfo.class, cauuid);
						if (i != null){
							cas.add(i);
						}
					}catch (Exception ex){
						//failed to parse
						throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid ca filter.  Could not parse uuid");
					}
				}
			}
			for (ConservationAreaInfo ca : cas){
				try{
					validateRead(ca.getUuid(), s);
					
					Criteria c = s.createCriteria(ServerDataQueueItem.class)
							.add(Restrictions.eq("conservationArea", ca.getUuid()));
					if (statusFilter != null){
						c.add(Restrictions.in("status", statusFilter));
					}
					
					List<ServerDataQueueItem> items = c.list();
					for (ServerDataQueueItem item : items){
						ServerDataQueueItemProxy proxy = new ServerDataQueueItemProxy(
								item.getUuid(), item.getName(), ca.getLabel(), item.getType(), item.getStatus(), item.getUploadedDate(), item.getUploadedBy());
						proxyitems.add(proxy);
					}
				}catch (SmartConnectException ex){
					//no access; this is okay do not add these items
				}
			}
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error reading data queue items.", ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, "Error reading data queue items.", ex);
		}finally{
			s.getTransaction().rollback();
		}
		return proxyitems;
	}
	
	/**
	 * Deletes the given data queue item (and associated work item if applicable).
	 * @param uuid
	 */
	@DELETE
    @Path("/items/{uuid}")
	public void deleteItem(@PathParam("uuid") String uuid){
		
		UUID itemUuid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ServerDataQueueItem item = (ServerDataQueueItem)s.get(ServerDataQueueItem.class, itemUuid);
			
			validateDelete(item.getConservationArea(), s);
			
			File toDelete = DataStoreManager.INSTANCE.getFile(item.getName());
			if (toDelete.exists()){
				if (!toDelete.delete()){
					logger.log(Level.WARNING, "Could not delete data queue file: " + toDelete.toString());
				}
			}else{
				logger.log(Level.WARNING, "Could not delete data queue file does not exist to delete: " + toDelete.toString());
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
		}catch(Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Error deleting data queue item: " +ex.getMessage(), ex);
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, "Error removing data queue item", ex);
		}
	}
	
	/**
	 * Creates an item and associated work item for file uploading.  Returns the url that
	 * can be used for uploading the file.
	 * 
	 * @param item
	 * @return
	 */
	@POST
    @Path("/items/")
	public String createItem(ServerDataQueueItem item){
		
		if (item.getConservationArea() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Conservation area not specified.");
		}
		if (item.getType() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Type not specified.");
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			validateUpload(item.getConservationArea(), s);
			
			ConservationAreaInfo ca = (ConservationAreaInfo)s.get(ConservationAreaInfo.class, item.getConservationArea());
			if (ca == null){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid conservation area.");	
			}
			if (ca.getStatus() == ConservationAreaInfo.Status.CCAA){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid conservation area (ccaa configurations are invalid).");
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
			up.setConservationAreaInfo(ca);
			up.setStartTime(new Date());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_DATAQUEUE);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename("");
			s.save(up);
			
			File updir = DataStoreManager.INSTANCE.getFile("dataqueue");
			if (!updir.exists()){
				FileUtils.forceMkdir(updir);
			}
			//TODO: assuming zip - this is WRONG
			up.setLocalFilename(DataStoreManager.INSTANCE.generateFileName("dataqueue" 
					+ File.separator + UuidUtils.uuidToString(up.getUuid()) + ".zip"));
			item.setFile(up.getLocalFilename());
			item.setName(request.getHeader("content-disposition"));
			item.setWorkItem(up.getUuid());
			s.saveOrUpdate(item);
			s.saveOrUpdate(up);
			
			//we have a file to uplodate and we expect more
			
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
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/items/{uuid}")
	public ServerDataQueueItem getItem(@PathParam("uuid") String uuid){
		UUID itemUuid = parseUuid(uuid);
		
		Session s = HibernateManager.getSession(context);
		try{
			
			ServerDataQueueItem item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, itemUuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			validateRead(item.getConservationArea(), s);
			return item;
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not get data queue item: " +uuid, ex);
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, "Could not get data queue item: " + uuid, ex);	
		}
	}
	
	/**
	 * Gets the next item for processing for the given conservation area.
	 * 
	 * @param newItem
	 * @return
	 */
	@PUT
	@Path("/nextitem/{cauuid}")
	public DataQueueItem getNextItem(@PathParam("cauuid") String caUuid){
		UUID ca = parseUuid(caUuid);
		Session s = HibernateManager.getSession(context);
		try{
			validateProcess(ca, s);
			
			ServerDataQueueItem item = (ServerDataQueueItem)s.createCriteria(ServerDataQueueItem.class)
			.add(Restrictions.eq("conservationArea", ca))
			.add(Restrictions.eq("status", ServerDataQueueItem.Status.QUEUED))
			.addOrder(Order.asc("uploadedDate"))
			.uniqueResult();
			
			if (item == null){
				return null;
			}
			return item;
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Could not find next item for processing from data queue (ca: " + caUuid + "). ", ex);
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex);
		}
	}
	
	/**
	 * Updates a given data queue item status.
	 * 
	 * @param newItem
	 * @return
	 */
	@PUT
	@Path("/items/{uuid}/status")
	public void updateItemStatus(@PathParam("uuid") String itemUuid, ServerDataQueueItem.Status newStatus){
		UUID uuid = parseUuid(itemUuid);
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ServerDataQueueItem item = (ServerDataQueueItem) s.get(ServerDataQueueItem.class, uuid);
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND, "Data queue item ont found.");
			}
			validateProcess(item.getConservationArea(), s);
			item.setStatus(newStatus);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Could not update status. ", ex);
			if (ex instanceof SmartConnectException) throw ex;
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex);
		}
	}
	
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
				throw new SmartConnectException(Response.Status.NOT_FOUND, "File not found.");
			}
			validateProcess(item.getConservationArea(), s);
			s.getTransaction().commit();
		}catch (SmartConnectException ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			logger.log(Level.SEVERE, "Unable to get download file." + ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Unable to get download file.", ex); 
		}
	
		//item.getStatus()
		
		final File toReturn = DataStoreManager.INSTANCE.getFile(item.getFile());
		if (!toReturn.exists()){
			throw new SmartConnectException(Response.Status.NOT_FOUND, "Data queue item server not found.");
		}
		
		String range = request.getHeader("Range");
		long start = 0;
		long end = toReturn.length()-1;
		boolean hasRange = false;
		if (range != null && range.length() > 0){
			hasRange = true;
			//parse the range
			try{
				String regex = "^bytes=([0-9]*)-([0-9]*)$";
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
					throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, "Range exceeds the maximum file length.");	
				}
				if (start > end){
					throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, "start byte is greater than end byte.");
				}
			}catch (Exception ex){
				throw new SmartConnectException(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE, "Range could not be parsed.");
			}
		}

		if (!hasRange){
			StreamingOutput stream = new StreamingOutput() {
				@Override
				public void write(OutputStream output) throws IOException {
					try {
						Files.copy(toReturn.toPath(), output);
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Error writing to output stream." + e.getMessage(), e);
					}
				}
		    };
			    
			return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getName() + "\"")
					.header(HttpHeaders.CONTENT_LENGTH, toReturn.length())
					.header("Accept-Ranges", "bytes")
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
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + toReturn.getName() + "\"")
					.header(HttpHeaders.CONTENT_LENGTH, end - start + 1)
					.header("Accept-Ranges", "bytes")
					.header("Content-Range", "bytes " + start + "-" + end + "/" + toReturn.length())
					.build();
		}
	}
	
	private UUID parseUuid(String uuid) throws SmartConnectException{
		UUID itemUuid = null;
		try{
			itemUuid= UuidUtils.stringToUuid(uuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + uuid + ". " + ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Bad request", ex);
		}
		return itemUuid;
	}
}
