/*
 * Copyright (C) 2019 Wildlife Conservation Society
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.api.ConnectAlert;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.CyberTracker;
import org.wcs.smart.connect.api.DataQueue;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem.Status;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.CyberTrackerApiKey;
import org.wcs.smart.connect.model.CyberTrackerNavigationLayer;
import org.wcs.smart.connect.model.CyberTrackerPackage;
import org.wcs.smart.connect.model.GeoJsonAlert;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;

/**
 * CyberTracker API that is available via the token authorization
 * scheme.  This api allows users to: view packages, get package
 * details, download package, upload observation data, and create/update 
 * alerts.
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + CyberTrackerNoa.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=ConnectNoaRESTApplication.APIKEY_QUERY_PARAM),
		@SecurityScheme(name="apikeyheader",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.HEADER, paramName=ConnectNoaRESTApplication.APIKEY_HEADER_PARAM)})
public class CyberTrackerNoa {

	public static final String PATH = "cybertracker"; //$NON-NLS-1$

	private final Logger logger = Logger.getLogger(DataQueue.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	@Context private HttpHeaders headers;
	
	/**
	 * Validation is done via api_key in query or X-API-KEY header
	 * and returns the ConservationArea uuid associated with 
	 * the key.  Calling methods must validate ca uuid against their
	 * resource cauuid.
	 * 
	 */
	private UUID validateToken() {
		String token = null;
		if (request.getParameterMap() != null && request.getParameterMap().containsKey(ConnectNoaRESTApplication.APIKEY_QUERY_PARAM)) {
			token = request.getParameterMap().get(ConnectNoaRESTApplication.APIKEY_QUERY_PARAM)[0];
		}
		if (token == null) {
			token = request.getHeader(ConnectNoaRESTApplication.APIKEY_HEADER_PARAM);
		}
		if (token == null || token.isEmpty()) throw new SmartConnectException(Response.Status.UNAUTHORIZED);

		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try {
			
			CyberTrackerApiKey key = QueryFactory.buildQuery(s, CyberTrackerApiKey.class, 
					new Object[] {"apiKey", token}).uniqueResult(); //$NON-NLS-1$
			
			if (key == null) throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			if (key.getApiKey() == null) throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			if (!key.getApiKey().equals(token)) throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			
			return key.getConservationArea().getUuid();
		}finally {
			s.getTransaction().rollback();
		}
	}
	
	
	/**
	 * Gets the package details include the current revision
	 * conservation area and other details.
	 */
	@GET
    @Path("packages/info/{uuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Gets the details about a given SMART Mobile package including the revision number and last uploaded date.",
			security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(schema = @Schema(implementation=CyberTrackerPackageProxy.class))})
	@ApiResponse(responseCode = "400", description = "Invalid package identifier")
	@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
	public CyberTrackerPackageProxy getPackageDetails(@PathParam("uuid") String packageUuidstr){
		
		UUID tokenCaUuid = validateToken();
		
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
			if (!ctpackage.getConservationArea().getUuid().equals(tokenCaUuid)) throw new SmartConnectException(Response.Status.FORBIDDEN); 
			
			return ctpackage.asProxy();
		}finally {
			s.getTransaction().commit();
		}
	}
	

	@GET
    @Path("packages/")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Gets the details about all SMART Mobile packages authorized by the api key.",
			security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(array = @ArraySchema(arraySchema = @Schema(implementation=CyberTrackerPackageProxy.class)))})
	@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
	public List<CyberTrackerPackageProxy> getPackages( ){
		
		UUID tokenCaUuid = validateToken();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<CyberTrackerPackage> ctpackages = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"conservationArea.uuid", tokenCaUuid).list(); //$NON-NLS-1$
		
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
	@Operation(description = "Gets the entire SMART Mobile package as a zip file.",
			security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
	@ApiResponse(responseCode = "200", description="Package returned successfully")
	@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
	@ApiResponse(responseCode = "404", description = "Requested package not found")
	public Response getCtPackage(@PathParam("uuid") String packageUuidstr){
		
		UUID tokenCaUuid = validateToken();
		
		UUID packageUuid = null;
		try{
			packageUuid = UUID.fromString(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTrackerNoa.InvalidPackageError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		java.nio.file.Path file = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			if (!ctpackage.getConservationArea().getUuid().equals(tokenCaUuid)) throw new SmartConnectException(Response.Status.FORBIDDEN); 
			
			file = DataStoreManager.INSTANCE.getRootDirectory()
					.toPath().resolve(CyberTracker.CT_PACKAGE_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
		}finally {
			s.getTransaction().commit();
		}
		
		long size = 0;
		if (file == null || !Files.exists(file)) {
			logger.log(Level.SEVERE, Messages.getString("CyberTrackerNoa.PackageNotFoundError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
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
	
	@GET
    @Path("navigation/")
	@Produces({ MediaType.APPLICATION_JSON })
	@Operation(description = "Gets the details about all SMART Mobile navigation layers authorized by the api key.",
			security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
	@ApiResponse(responseCode = "200", description = "OK", content = {@Content(array = @ArraySchema(arraySchema = @Schema(implementation=CyberTrackerPackageProxy.class)))})
	@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
	public List<CyberTrackerNavigationProxy> getNavigationLayers( ){
		
		UUID tokenCaUuid = validateToken();
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<CyberTrackerNavigationLayer> navigationlayers = QueryFactory.buildQuery(s, CyberTrackerNavigationLayer.class, 
					"conservationArea.uuid", tokenCaUuid).list(); //$NON-NLS-1$
			List<CyberTrackerNavigationProxy> proxies = new ArrayList<>();
			for (CyberTrackerNavigationLayer p : navigationlayers) {
				proxies.add(p.asProxy());
			}
			return proxies;
		}finally {
			s.getTransaction().commit();
		}
	}
	
	/**
	 * Gets the navigation layer
	 */
	@GET
    @Path("navigation/{uuid}")
	@Operation(description = "Gets the entire Navigation layer as a zip file.",
			security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
	@ApiResponse(responseCode = "200", description="Data returned successfully")
	@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
	@ApiResponse(responseCode = "404", description = "Requested navigation layer not found")
	public Response getNavigationLayer(@PathParam("uuid") String uuid){
		
		UUID tokenCaUuid = validateToken();
		
		UUID navigationUuid = null;
		try{
			navigationUuid = UUID.fromString(uuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTrackerNoa.NavLayerNotFound", request.getLocale())); //$NON-NLS-1$
		}
		java.nio.file.Path file = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerNavigationLayer layer = QueryFactory.buildQuery(s, CyberTrackerNavigationLayer.class, 
					"uuid", navigationUuid).uniqueResult(); //$NON-NLS-1$
			if (layer == null) throw new SmartConnectException(Response.Status.NOT_FOUND);
			if (!layer.getConservationArea().getUuid().equals(tokenCaUuid)) throw new SmartConnectException(Response.Status.FORBIDDEN); 
			
			file = DataStoreManager.INSTANCE.getRootDirectory()
					.toPath().resolve(CyberTracker.CT_NAVIGATION_DATASTORE_LOCATION).resolve(layer.getFilename());
		}finally {
			s.getTransaction().commit();
		}
		
		long size = 0;
		if (file == null || !Files.exists(file)) {
			logger.log(Level.SEVERE, "Navigation layer file not found"); //$NON-NLS-1$
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
	 * Upload CyberTracker observation package to data queue
	 */
	@POST
    @Path("data/{cauuid}")
	@Operation(description = "Uploads CyberTracker observations to SMART Connect data queue.",
			security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
	@ApiResponse(responseCode = "200", description="Package uploaded successfully")
	@ApiResponse(responseCode = "400", description = "Invalid parameters or context")
	@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
    public Response uploadPacket(
    		@Parameter(description="The Conservation Area uuid associated with the data") @PathParam("cauuid") String caUuid, 
    		@Parameter(description="JSON CyberTracker data.  If compressed the CONTENT_ENCODING header should be set to deflate") InputStream data){
		
		UUID tokenCaUuid = validateToken();
		
		UUID ca = null;
		try{
			ca = UuidUtils.stringToUuid(caUuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + caUuid + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("DataQueue.BadRequest", SmartUtils.getRequestLocale(request)), ex); //$NON-NLS-1$
		}
		
		if (!tokenCaUuid.equals(ca)) throw new SmartConnectException(Response.Status.FORBIDDEN);
		
		ServerDataQueueItem item = new ServerDataQueueItem();
		
		Session s = HibernateManager.getSession(context);
		
		s.beginTransaction();
		try{		
			item.setConservationArea(ca);
			item.setName("CyberTracker " + DateFormat.getDateTimeInstance().format(new Date())); //$NON-NLS-1$
			if (request.getHeader(HttpHeaders.CONTENT_ENCODING) != null && request.getHeader(HttpHeaders.CONTENT_ENCODING).equalsIgnoreCase("deflate")){ //$NON-NLS-1$
				item.setType("JSON_ZLIB_CT"); //$NON-NLS-1$
			}else{
				item.setType("JSON_CT"); //$NON-NLS-1$
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
		
		java.nio.file.Path upfile = DataStoreManager.INSTANCE.getFile(item.getFile()).toPath();
		if (!Files.exists(upfile.getParent())) {
			try {
				Files.createDirectories(upfile.getParent());
			} catch (IOException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				item.setStatus(Status.ERROR);
				item.setStatusMessage("Unable to create directory: " + upfile.getParent().toString() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				thrown = ex;
			}
		}
		
		if (thrown == null) {
			try(OutputStream out = Files.newOutputStream(upfile, StandardOpenOption.CREATE)){ 
				IOUtils.copy(data, out);
				item.setStatus(Status.QUEUED);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				item.setStatus(Status.ERROR);
				item.setStatusMessage(Messages.getString("CtDataApi.WriteError", request.getLocale()) + ex.getMessage()); //$NON-NLS-1$
				thrown = ex;
			}
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
		if (thrown != null) throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CtDataApi.WriteError", request.getLocale()) + thrown.getMessage()); //$NON-NLS-1$
		
		return Response.ok().build();		
	}
	
	
	/**
	 * Create a new alert or update the track position of an existing alert.
	 * 
	 *  <p>
	 *  If you are updating an existing track, call this API with the same usergenid more than once.
	 *  If this case the systems adds the past x,y coordinates 
	 *  to a historical "track" of sorts and overwrites the other attributes with the latest data. 
	 *  This is the way users can send a repetitive
	 *  "ping" to keep a last-known location and past track of devices without 
	 *  creating a new alert everytime the location is updated. 
	 * </p>
	 * 
	 * @param	usergenid	provided in the URL, the user generated ID of the alert. The system generates a UUID automatically.
	 * @return Returns a JSON Alert object for the created alert
	 */
	@PUT
    @Path("alert/{usergenid}")
	@Operation(description = "Creates a new alert or updates the position of an existing alert.  If you are "
			+ "updating an existing alert call this API with the smae usergenid more than once. "
			+ "If this case the system adds the past x,y coordinates to a historical track and overwrites the other "
			+ "attributes with the latest data.  This is the way users can send a repetitive pings to keep the "
			+ "last known location and past track of devices without creating a new alert each time the location is updated",
		security = {@SecurityRequirement(name="apikeyheader"), @SecurityRequirement(name="apikeyquery")})
		@ApiResponse(responseCode = "200", description="Package uploaded successfully")
		@ApiResponse(responseCode = "400", description = "Invalid parameters or context")
		@ApiResponse(responseCode = "401", description = "Invalid authorization credientials")
    public Alert addAlert(
    		@Parameter(description="The id to associated with the alert.  If this id already exists in the system the alert will be updated, otherwise a new alert with this id will be created ")
    		@PathParam("usergenid") String userGenId, 
    		@Parameter(description="The alert settings.") GeoJsonAlert newGeoJsonAlert) {	
		
		UUID tokenCaUuid = validateToken();

		if (!tokenCaUuid.equals(newGeoJsonAlert.getCaUuid())) throw new SmartConnectException(Response.Status.FORBIDDEN);
		
		Alert newAlert = ConnectAlert.convertAndValidateAlert(newGeoJsonAlert, request);
		newAlert.setCreatorUuid(null);
		newAlert.setSource(Alert.Source.CYBERTRACKER);
		newAlert.setUserGeneratedId(userGenId);
		
		//validate usergenid, is it unique? If so, update the existing one and return instead of saving a new one.
		Alert existingAlert = ConnectAlert.findAlert(userGenId, request);
		try {
			if (existingAlert != null) {
				if (!existingAlert.getCa().getUuid().equals(newGeoJsonAlert.getCaUuid())) throw new SmartConnectException(Response.Status.BAD_REQUEST);
				newAlert.setTrack(null);
				ConnectAlert.updateAlert(existingAlert, newAlert, true, request);
				response.setStatus(Response.Status.OK.getStatusCode());
				response.flushBuffer();
				return existingAlert;
			}else {
				ConnectAlert.saveAlert(newAlert, newGeoJsonAlert.getCaUuid(), request);
				response.setStatus(Response.Status.CREATED.getStatusCode());
				response.flushBuffer();
				
				return newAlert;
			}
		}catch (SmartConnectException ex) {
			logger.log(Level.WARNING, ex.getMessage(), ex);
			throw ex;
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(ex.getMessage(), ex);
		}

	}

}
