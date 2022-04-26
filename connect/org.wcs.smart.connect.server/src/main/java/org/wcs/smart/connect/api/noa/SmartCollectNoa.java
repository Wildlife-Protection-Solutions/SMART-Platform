/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.connect.api.noa;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import org.wcs.smart.connect.model.SmartCollectConnectUser;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


/**
 * SMARTCollect api that is visible to the public
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + SmartCollectNoa.PATH)
public class SmartCollectNoa {

	public static final String PATH = "smartcollect"; //$NON-NLS-1$
	
	private final Logger logger = Logger.getLogger(SmartCollectNoa.class.getName());

	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	@Context private HttpHeaders headers;
	
	private URL getRootUrl() throws MalformedURLException{
		URL url = new URL(request.getRequestURL().toString());
		String sp = context.getContextPath();
		URL rootUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), sp);
		return rootUrl;
	}
	
	
	@GET
    @Path("packages/")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Gets the details about all SMART Collect packages on this server.")
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(array = @ArraySchema(schema = @Schema(implementation=CyberTrackerPackageProxy.class)))})
	public List<CyberTrackerPackageProxy> getPackages(){
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<CyberTrackerPackage> ctpackages = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"type", SmartCollectPackage.PACKAGE_TYPENAME).list(); //$NON-NLS-1$
		
			
			List<CyberTrackerPackageProxy> proxies = new ArrayList<>();
			for (CyberTrackerPackage e : ctpackages) {
				if (e.getIsPrivate()) continue;
				proxies.add(e.asProxy(getRootUrl()));
			}
			return proxies;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not get package details", ex); //$NON-NLS-1$
		}finally {
			if (s.getTransaction().isActive()) s.getTransaction().commit();
		}
	}
	
	/**
	 * Gets the cybertracker package
	 */
	@GET
    @Path("packages/{uuid}")
	@Operation(description = "Gets the SMART Collect package as a zip file.")
	@ApiResponse(responseCode = "200", description="Package returned successfully")
	@ApiResponse(responseCode = "404", description = "Requested package not found")
	public Response getCtPackage(@PathParam("uuid") String packageUuidstr){
		
		UUID packageUuid = null;
		try{
			packageUuid = UuidUtils.stringToUuid(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SmartCollectNoa.InvalidIdentifier", request.getLocale()));  //$NON-NLS-1$
		}
		
		java.nio.file.Path file = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			if (!ctpackage.getType().equals(SmartCollectPackage.PACKAGE_TYPENAME)) throw new SmartConnectException(Response.Status.NOT_FOUND); 
			file = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CyberTracker.CT_PACKAGE_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
		}finally {
			s.getTransaction().commit();
		}
		
		long size = 0;
		if (file == null || !Files.exists(file)) {
			logger.log(Level.SEVERE,"SMART Collect package file not found."); //$NON-NLS-1$
			throw new SmartConnectException("SMART Collect package file not found."); //$NON-NLS-1$
		}
		try {
			size = Files.size(file);
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(ex.getMessage(), ex);
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
	@Operation(description = "Gets the details about a given SMART Collect package including the revision number and last uploaded date.")
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(schema = @Schema(implementation=CyberTrackerPackageProxy.class))})
	@ApiResponse(responseCode = "400", description = "Invalid package identifier")
	public CyberTrackerPackageProxy getPackageDetails(@PathParam("uuid") String packageUuidstr){
		
		UUID packageUuid = null;
		try{
			packageUuid = UuidUtils.stringToUuid(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTrackerNoa.InvalidPackageError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			
			if (ctpackage == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			if (!ctpackage.getType().equals(SmartCollectPackage.PACKAGE_TYPENAME)) throw new SmartConnectException(Response.Status.NOT_FOUND);
			return ctpackage.asProxy(getRootUrl());
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not get package details", ex); //$NON-NLS-1$
		}finally {
			if (s.getTransaction().isActive()) s.getTransaction().commit();
		}
	}

	/**
	 * Validates a user - the user clicks on a link and provides their validation key. 
	 * If it matches the key in the database they are validated.
	 */
	@GET
    @Path("source/{validationkey}")
	@Produces({ MediaType.TEXT_HTML })
	@Operation(description = "Accepts validation from a source user")
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(schema = @Schema(implementation=CyberTrackerPackageProxy.class))})
	@ApiResponse(responseCode = "400", description = "Could not authorization use")
	public String validateUser(@PathParam("validationkey") String validationkey ){

		try {
			Session s = HibernateManager.getSession(context);
			s.beginTransaction();
			try{
				
				SmartCollectConnectUser user = s.createQuery("FROM SmartCollectConnectUser WHERE validationKey = :key", SmartCollectConnectUser.class) //$NON-NLS-1$
						.setParameter("key", validationkey).uniqueResult(); //$NON-NLS-1$
				
				if (user == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
				
				if (user.getState() == State.BLACKLISTED) {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SmartCollectNoa.BlacklistedUser", request.getLocale())); //$NON-NLS-1$
				}else if (user.getState() == State.VALIDATED) {
					//already validated
				}else if (user.getValidateSentDate() != null && user.getValidationKey().equals(validationkey)) {
					//check timeout
					
					if (ChronoUnit.DAYS.between(user.getValidateSentDate(), LocalDateTime.now()) > 72) {
						throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SmartCollectNoa.ValidationTimeout", request.getLocale())); //$NON-NLS-1$
					}
					user.setState(State.VALIDATED);
					
				}else {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("SmartCollectNoa.InvalidKey", request.getLocale())); //$NON-NLS-1$
				}
				
			}finally {
				s.getTransaction().commit();
			}
		}catch (SmartConnectException ex) {
			return MessageFormat.format(Messages.getString("SmartCollectNoa.ValidationError", request.getLocale()), "<html><title>","</title><body>", ex.getMessage(), "</body></html>");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		
		return MessageFormat.format(Messages.getString("SmartCollectNoa.Validated", request.getLocale()), "<html><title>","</title><body>","</body></html>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}
}
