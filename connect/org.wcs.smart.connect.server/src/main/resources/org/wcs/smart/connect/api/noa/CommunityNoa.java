package org.wcs.smart.connect.api.noa;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.CyberTracker;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.CyberTrackerPackage;
import org.wcs.smart.cybertracker.community.model.CommunityCtPackage;
import org.wcs.smart.hibernate.QueryFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


@Path(ConnectRESTApplication.PATH_SEPERATOR + CommunityNoa.PATH)

public class CommunityNoa {

	public static final String PATH = "community"; //$NON-NLS-1$
	
	private final Logger logger = Logger.getLogger(CommunityNoa.class.getName());

	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	@Context private HttpHeaders headers;
	
	@GET
    @Path("packages/")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Gets the details about all Community packages on this server.")
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(array = @ArraySchema(arraySchema = @Schema(implementation=CyberTrackerPackageProxy.class)))})
	public List<CyberTrackerPackageProxy> getPackages( ){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<CyberTrackerPackage> ctpackages = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"type", CommunityCtPackage.TYPE_NAME).list(); //$NON-NLS-1$
		
			List<CyberTrackerPackageProxy> proxies = new ArrayList<>();
			
			for (CyberTrackerPackage p : ctpackages) {
				proxies.add(p.asProxy());
			}
		
			return proxies;
		}finally {
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Gets the cybertracker package
	 */
	@GET
    @Path("packages/{uuid}")
	@Operation(description = "Gets the Community package as a zip file.")
	@ApiResponse(responseCode = "200", description="Package returned successfully")
	@ApiResponse(responseCode = "404", description = "Requested package not found")
	public Response getCtPackage(@PathParam("uuid") String packageUuidstr){
		
		UUID packageUuid = null;
		try{
			packageUuid = UUID.fromString(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid package identifier"); 
		}
		
		java.nio.file.Path file = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			if (!ctpackage.getType().equals(CommunityCtPackage.TYPE_NAME)) throw new SmartConnectException(Response.Status.NOT_FOUND); 
			
			file = DataStoreManager.INSTANCE.getRootDirectory()
					.toPath().resolve(CyberTracker.CT_PACKAGE_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
		}finally {
			s.getTransaction().commit();
		}
		
		long size = 0;
		if (file == null || !Files.exists(file)) {
			logger.log(Level.SEVERE,"Community package file not found.");
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR);
		}
		try {
			size = Files.size(file);
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		final java.nio.file.Path ffile = file;
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException {
				try {
					Files.copy(ffile, output);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error writing to output stream." + e.getMessage(), e); //$NON-NLS-1$
				}
			}
	    };
		
		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName().toString() + "\"") //$NON-NLS-1$ //$NON-NLS-2$
				.header(HttpHeaders.CONTENT_LENGTH, size)
				.header("Accept-Ranges", "bytes") //$NON-NLS-1$ //$NON-NLS-2$
				.build();
	}
	
	/**
	 * Gets the package details for the specific package
	 */
	@GET
    @Path("packages/info/{uuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Gets the details about a given CyberTracker package including the revision number and last uploaded date.")
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(schema = @Schema(implementation=CyberTrackerPackageProxy.class))})
	@ApiResponse(responseCode = "400", description = "Invalid package identifier")
	public CyberTrackerPackageProxy getPackageDetails(@PathParam("uuid") String packageUuidstr){
		
		UUID packageUuid = null;
		try{
			packageUuid = UUID.fromString(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTrackerNoa.InvalidPackageError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			
			if (ctpackage == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			if (!ctpackage.getType().equals(CommunityCtPackage.TYPE_NAME)) throw new SmartConnectException(Response.Status.NOT_FOUND); 
			
			return ctpackage.asProxy();
		}finally {
			s.getTransaction().commit();
		}
	}

}
