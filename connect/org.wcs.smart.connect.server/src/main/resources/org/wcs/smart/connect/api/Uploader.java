package org.wcs.smart.connect.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.hibernate.HibernateSessionFactoryListener;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.connect.model.UploadItem.Status;
import org.wcs.smart.connect.model.UploadStatus;
import org.wcs.smart.connect.uploader.UploaderProcessor;


@Path(ConnectRESTApplication.PATH_SEPERATOR + Uploader.PATH)
public class Uploader extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(Uploader.class.getName());
	
	public static final String PATH = "uploader"; //$NON-NLS-1$
	
	@Context private HttpHeaders headers;
	@Context private ServletContext context;
	
	@GET
	@Path("/{uploaduuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public UploadStatus getStatus(@PathParam("uploaduuid") String uuid){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			UploadItem item = (UploadItem) s.get(UploadItem.class, UUID.fromString(uuid));
			if (item == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
			UploadStatus status = new UploadStatus(item);
			File f = DataStoreManager.INSTANCE.getFile(item.getLocalFilename());
			if (!f.exists()){
				status.setCurrentSize(0);
			}else{
				Long size = Files.size(f.toPath());
				status.setCurrentSize(size);
			}
			return status;
		}catch (Exception ex){
			logger.severe(ex.getMessage());
			throw new SmartConnectException(HttpURLConnection.HTTP_INTERNAL_ERROR);
		}finally{
			s.getTransaction().commit();
		}
	}
	
	//TODO: figure how to prevent concurrent calls to this method
	//which would write the same data twice to the file and fail miserably
	@PUT
	@Path("/{uploaduuid}")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response updateFile(@PathParam("uploaduuid") String uuid, InputStream data) throws IOException{

		UploadItem item = null;
		
		//get upload item
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			item = (UploadItem) s.get(UploadItem.class, UUID.fromString(uuid));
			if (item == null){
				throw new SmartConnectException(HttpURLConnection.HTTP_NOT_FOUND);
			}
		}finally{
			s.getTransaction().commit();
		}
		if (item.getStatus() != Status.UPLOADING){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Uploader.Duplicate", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		}
		
		// validate content-type
		boolean octet = false;
		List<String> types = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);
		for (String t : types){
			if (t.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM)){
				octet = true;
			}
		}
		if (!octet) {
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST
					,MessageFormat.format(Messages.getString("Uploader.ContentTypeRequired", SmartUtils.getRequestLocale(headers)), MediaType.APPLICATION_OCTET_STREAM )); //$NON-NLS-1$
		}
		
		//validate content-length
		int length = 0;
		try{
			length = Integer.valueOf(headers.getRequestHeader(HttpHeaders.CONTENT_LENGTH).get(0));
		}catch (Exception ex){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Uploader.InvalidLength", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		}
		if (length < 0){
			throw new SmartConnectException(HttpURLConnection.HTTP_BAD_REQUEST, Messages.getString("Uploader.InvalidLength", SmartUtils.getRequestLocale(headers))); //$NON-NLS-1$
		}
		
		File datastoreFile = DataStoreManager.INSTANCE.getFile(item.getLocalFilename());
		if (!datastoreFile.exists()){
			datastoreFile.createNewFile();
		}
		
		
		//read some bytes from the data and write to file
		byte[] buffer = new byte[1024];
		int read = -1;
		try(OutputStream out = Files.newOutputStream(datastoreFile.toPath(), StandardOpenOption.APPEND)){
			while((read = data.read(buffer)) > 0){
			//	copy read number of bytes from buffer into file
				out.write(buffer, 0, read);
				out.flush();
			}
		}
		
		//TODO: content range?!?
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
			ExecutorService executor = (ExecutorService) context.getAttribute(HibernateSessionFactoryListener.EXECUTOR_KEY);
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
				logger.severe(ex.getMessage());
				s.getTransaction().rollback();
				throw ex;
			}
		}
		
		//return accepted
		return Response.accepted().
	            entity(item).
	            build();
	}
}
