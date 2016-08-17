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
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.ConnectStartupContextListener;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.UploadStatus;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.uploader.UploaderProcessor;


/**
 * REST API for uploading files.
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + Uploader.PATH)
public class Uploader extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(Uploader.class.getName());
	
	public static final String DATASTORE_DIR = "uploads"; //$NON-NLS-1$
	
	public static final String PATH = "uploader"; //$NON-NLS-1$
	
	@Context private HttpHeaders headers;
	@Context private ServletContext context;
	
	/**
	 * Gets the status of the current upload.
	 * 
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/{uploaduuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public UploadStatus getStatus(@PathParam("uploaduuid") String uuid){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			WorkItem item = (WorkItem) s.get(WorkItem.class, UUID.fromString(uuid));
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			UploadStatus status = new UploadStatus(item);
			File f = DataStoreManager.INSTANCE.getFile(item.getLocalFilename());
			
			if (item.getLocalFilename().isEmpty() || !f.exists()){
				status.setCurrentSize(0);
			}else{
				Long size = Files.size(f.toPath());
				status.setCurrentSize(size);
			}
			return status;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	//TODO: figure how to prevent concurrent calls to this method
	//which would write the same data twice to the file and fail miserably
	/**
	 * Uploads a file to the server's data Queue.
	 * 
	 * Payload: Multipart data containing an "upload_file" item, Example:
	 *  
	 * ------WebKitFormBoundaryYhW4Zu5MYMA5orxj
	 * Content-Disposition: form-data; name="upload_file"; filename="Demo_00001.xml"
	 * Content-Type: text/xml
	 * <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
	 * ....the rest of an XML file upload...
	 * ------WebKitFormBoundaryYhW4Zu5MYMA5orxj--
	 * 
	 * @param uuid	the uuid of the workitem, you must first call the "create work item" API to generate this uuid: ../server/api/dataqueue/items/ , then you can upload the file. 
	 * @return HTTP/1.1 202 Accepted, and JSON of WorkItem details.
	 * @throws Exception 
	 */
	@PUT
	@Path("/{uploaduuid}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateFile(@PathParam("uploaduuid") String uuid, InputStream data) throws Exception{
		WorkItem item = null;
		
		item = getWorkItem(uuid);
		
		// validate content-type
		boolean octet = false;
		List<String> types = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);
		for (String t : types){
			if (t.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)){
				octet = true;
			}
		}
		if (!octet) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST
					,MessageFormat.format(Messages.getString("Uploader.ContentTypeRequired", SmartUtils.getRequestLocale(headers)), MediaType.APPLICATION_OCTET_STREAM )); //$NON-NLS-1$
		}
		
		//validate content-length
		int length = 0;
		try{
			length = Integer.valueOf(headers.getRequestHeader(HttpHeaders.CONTENT_LENGTH).get(0));
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("Uploader.InvalidLength", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		}
		if (length < 0){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("Uploader.InvalidLength", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		}
		
		try {
			processInputStream(data, item);
		} catch (Exception e) {
			throw e;
		}
		
		//return accepted
		return Response.accepted().
	            entity(item).
	            build();
		
	}

	private WorkItem getWorkItem(String uuid) {
		WorkItem item;
		//get upload item
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			item = (WorkItem) s.get(WorkItem.class, UUID.fromString(uuid));
			if (item == null){
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
		}finally{
			s.getTransaction().commit();
		}
		
		if (item.getStatus() != Status.UPLOADING){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("Uploader.Duplicate", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		}
		return item;
	}

	private void processInputStream(InputStream data, WorkItem item) throws IOException, Exception {
		Session s;
		File datastoreFile = DataStoreManager.INSTANCE.getFile(item.getLocalFilename());
		try(OutputStream out = Files.newOutputStream(datastoreFile.toPath(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)){
			IOUtils.copy(data, out);
		}

		//if start at bytes already provided we should probably either fail or skip 
		//bytes
		long newFileSize = Files.size(datastoreFile.toPath());		
		if (newFileSize == item.getTotalBytes()){
			//we are finished uploading and ready to start processing
			s = HibernateManager.getSession(context);
			s.beginTransaction();
			try{
				s.update(item);
				item.setStatus(Status.PROCESSING);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				throw ex;
			}

			//start background processor
			ExecutorService executor = (ExecutorService) context.getAttribute(ConnectStartupContextListener.EXECUTOR_KEY);
			executor.execute(new UploaderProcessor(item, HibernateManager.getSessionFactory(context)));
		}else if (newFileSize > item.getTotalBytes()){
			s = HibernateManager.getSession(context);
			s.beginTransaction();
			try{
				s.update(item);
				item.setStatus(Status.ERROR);
				item.setMessage(Messages.getString("Uploader.InvalidSize", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
				s.getTransaction().commit();
				
			}catch (Exception ex){
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				s.getTransaction().rollback();
				throw ex;
			}
		}
	}
	
	/* same as above, through POST method
	 * 
	 * @throws Exception 
	 * */
	
	/**
	 * Uploads data to server via POST
	 * URL: .../server/uploader/{uploaduuid}
	 * 
	 * @param uploaduuid	provided in the URL, uuid of the workItem this file upload belongs to.
	 * @param input	MultipartFormDataInput containing an "upload_file" component  
	 * @return Response
	 * @throws Exception FileNotFound is possible if the upload fails for some reason.
	 */
	@POST
	@Path("/{uploaduuid}")
	@Consumes("multipart/form-data")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateFilePost(@PathParam("uploaduuid") String uuid, MultipartFormDataInput input) throws Exception{
		WorkItem item;
		item = getWorkItem(uuid);
		
		Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
		List<InputPart> inputParts = uploadForm.get("upload_file"); //$NON-NLS-1$

		for (InputPart inputPart : inputParts) {

		 try {
			InputStream inputStream = inputPart.getBody(InputStream.class,null);
			
			try {
				processInputStream(inputStream, item);
			} catch (Exception e) {
				throw e;
			}
			
			//return accepted
			return Response.accepted().
		            entity(item).
		            build();
			
		  } catch (IOException e) {
				throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("Uploader.FileNotFound", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		  }

		}
		throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("Uploader.FileNotFound", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
	}

}
