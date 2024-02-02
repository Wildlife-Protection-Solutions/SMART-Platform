/* Copyright (C) 2019 Wildlife Conservation Society
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

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerNavigationProxy;
import org.wcs.smart.connect.cybertracker.model.CyberTrackerPackageProxy;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.CyberTrackerApiKey;
import org.wcs.smart.connect.model.CyberTrackerNavigationLayer;
import org.wcs.smart.connect.model.CyberTrackerPackage;
import org.wcs.smart.connect.model.CyberTrackerPackage.Status;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.CyberTrackerAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

/**
 * API for listing, downloading and updating CyberTracker packages
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + CyberTracker.PATH)

@Consumes({ MediaType.APPLICATION_JSON})
public class CyberTracker extends HttpServlet{
	
	/*
	 * local to ensure only one key is created at a time
	 */
	private static final Object APIKEYLOCK = new Object();
	
	private static final long serialVersionUID = 1L;
	
	public static final String PATH = "cybertracker"; //$NON-NLS-1$
	public static final String CT_PACKAGE_DATASTORE_LOCATION = "ctpackages"; //$NON-NLS-1$
	public static final String CT_NAVIGATION_DATASTORE_LOCATION = "ctnavigation"; //$NON-NLS-1$

	private final Logger logger = Logger.getLogger(DataQueue.class.getName());
	
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
	/**
	 * Lists all CyberTracker packages uploaded 
	 * to the system that the current user has access
	 * to view. Optionally the user can provide a cauuid as
	 * a filter.
	 * 
	 * @return
	 */
	@GET
    @Path("/packages")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<CyberTrackerPackageProxy> getPackages(@QueryParam("cauuid") String cauuid,
    		@QueryParam("private") String privatepkg){
		UUID caUuid = null;
		try {
			if (cauuid != null && !cauuid.trim().isEmpty()) {
				caUuid = UUID.fromString(cauuid);
			}
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidCaUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		boolean onlyprivate = false;
		if (privatepkg != null && privatepkg.trim().equalsIgnoreCase("true")) { //$NON-NLS-1$
			onlyprivate = true;
		}
		
		List<CyberTrackerPackageProxy> proxies = new ArrayList<>();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			List<CyberTrackerPackage> items = QueryFactory.buildQuery(s, CyberTrackerPackage.class).getResultList();
			
			for (CyberTrackerPackage ca : items) {
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, ca.getConservationArea().getUuid())){
					if (caUuid == null || caUuid.equals(ca.getConservationArea().getUuid())) {
						if (onlyprivate) {
							if (ca.getIsPrivate()) {
								proxies.add(ca.asProxy(getRootUrl()));
							}
						}else {
							proxies.add(ca.asProxy(getRootUrl()));
						}
					}
				}
			}
		
			Collections.sort(proxies, (a,b)->{
				if (a.getCaUuid().equals(b.getCaUuid())) return a.getName().compareToIgnoreCase(b.getName());
				return a.getCaLabel().compareToIgnoreCase(b.getCaLabel());
			});
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not list cybertracker packages", ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
		return proxies;
	}

	/**
	 * Lists all Navigation Layers uploaded 
	 * to the system that the current user has access
	 * to view. Optionally the user can provide a cauuid as
	 * a filter.
	 * 
	 * @return
	 */
	@GET
    @Path("/navigationlayers")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<CyberTrackerNavigationProxy> getNavigationLayers(@QueryParam("cauuid") String cauuid){
		UUID caUuid = null;
		try {
			if (cauuid != null && !cauuid.trim().isEmpty()) {
				caUuid = UUID.fromString(cauuid);
			}
		}catch (Exception ex){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidCaUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		List<CyberTrackerNavigationProxy> proxies = new ArrayList<>();
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			List<CyberTrackerNavigationLayer> items = QueryFactory.buildQuery(s, CyberTrackerNavigationLayer.class).getResultList();
			
			for (CyberTrackerNavigationLayer ca : items) {
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, ca.getConservationArea().getUuid())){
					if (caUuid == null || caUuid.equals(ca.getConservationArea().getUuid())) {
						proxies.add(ca.asProxy());
					}
				}
			}
		
			Collections.sort(proxies, (a,b)->{
				if (a.getCaUuid().equals(b.getCaUuid())) return a.getName().compareToIgnoreCase(b.getName());
				return a.getCaLabel().compareToIgnoreCase(b.getCaLabel());
			});
			
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);

			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not list cybertracker packages", ex); //$NON-NLS-1$
		}finally{
			s.getTransaction().commit();
		}
		return proxies;
	}

	/**
	 * Deletes a cybertracker package from the system
	 * 
	 * @return
	 */
	@DELETE
    @Path("/navigationlayers/{uuid}")
    public void deleteNavigation(@PathParam("uuid") String uuid){
		UUID itemUuid = null;
		try {
			itemUuid = UUID.fromString(uuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid package identifier.", ex); //$NON-NLS-1$
		}
		
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			CyberTrackerNavigationLayer p = QueryFactory.buildQuery(s,  CyberTrackerNavigationLayer.class,
					new Object[] {"uuid", itemUuid}).getSingleResult(); //$NON-NLS-1$
			if (p == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, p.getConservationArea().getUuid())){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
			
			WorkItem wi = s.get(WorkItem.class, p.getWorkItem());
			if (wi != null) {
				s.remove(wi);
			}
			
			//delete the file 
			java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CT_NAVIGATION_DATASTORE_LOCATION).resolve(p.getFilename());
			Files.delete(toDelete);
			
			s.remove(p);
			s.getTransaction().commit();
		}catch (SmartConnectException ex) {
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not list cybertracker packages", ex); //$NON-NLS-1$
		}
	}
	
	/**
	 * Deletes a cybertracker package from the system
	 * 
	 * @return
	 */
	@DELETE
    @Path("/packages/{uuid}")
    public void deletePackage(@PathParam("uuid") String uuid){
		UUID itemUuid = null;
		try {
			itemUuid = UUID.fromString(uuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid package identifier.", ex); //$NON-NLS-1$
		}
		
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			
			CyberTrackerPackage p = QueryFactory.buildQuery(s,  CyberTrackerPackage.class,
					new Object[] {"ctPackageUuid", itemUuid}).getSingleResult(); //$NON-NLS-1$
			if (p == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, p.getConservationArea().getUuid())){
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
			}
			
			WorkItem wi = s.get(WorkItem.class, p.getWorkItem());
			if (wi != null) {
				s.remove(wi);
			}
			
			//delete the file 
			java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CT_PACKAGE_DATASTORE_LOCATION).resolve(p.getFilename());
			if (!Files.exists(toDelete)) {
				logger.log(Level.WARNING, "Package file doesn't exists on server: " + toDelete.toString()); //$NON-NLS-1$
			}else {
				Files.delete(toDelete);
			}
				
			
			s.remove(p);
			s.getTransaction().commit();
		}catch (SmartConnectException ex) {
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not delete cybertracker package.", ex); //$NON-NLS-1$
		}
	}
	
	
	/**
	 * Gets the cybertracker package
	 * 
	 * 
	 */
	@GET
    @Path("/navigationlayers/{uuid}")
	public Response getNavigationLayer(@PathParam("uuid") String uuid){
		UUID layerUuid = null;
		
		try{
			layerUuid = UUID.fromString(uuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidPackageUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		java.nio.file.Path file = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerNavigationLayer ctpackage = QueryFactory.buildQuery(s, CyberTrackerNavigationLayer.class, 
					"uuid", layerUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			file = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CT_NAVIGATION_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
		}finally {
			s.getTransaction().commit();
		}
		
		long size = 0;
		if (file == null || !Files.exists(file)) {
			logger.log(Level.SEVERE, Messages.getString("CyberTracker.PackageNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
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
	 * Gets the cybertracker package
	 * 
	 * 
	 */
	@GET
    @Path("/packages/{uuid}")
	public Response getCtPackage(@PathParam("uuid") String packageUuidstr){
		UUID packageUuid = null;
		
		try{
			packageUuid = UUID.fromString(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidPackageUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		java.nio.file.Path file = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			file = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CT_PACKAGE_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
		}finally {
			s.getTransaction().commit();
		}
		
		long size = 0;
		if (file == null || !Files.exists(file)) {
			logger.log(Level.SEVERE, Messages.getString("CyberTracker.PackageNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
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
	 * Gets the package details include the current revision
	 * conservation area and other details.
	 */
	@GET
    @Path("packages/info/{uuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public CyberTrackerPackageProxy getPackage(@PathParam("uuid") String packageUuidstr){
		UUID packageUuid = null;
		
		try{
			packageUuid = UUID.fromString(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidPackageUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, 
					"ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND);
			}
			
			return ctpackage.asProxy(getRootUrl());
		}catch (SmartConnectException ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw ex;
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			s.getTransaction().rollback();
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, 
					"Could not get cybertracker package", ex); //$NON-NLS-1$
		}finally {
			if (s.getTransaction().isActive()) s.getTransaction().commit();
		}
	}
	
	
	/**
	 * <p>Creates a new package OR updates an existing package.</p>
	 * 
	 * <p>Returns the URL that can be used for uploading the cybertracker package:</p>
	 * <p>URL: ../server/api/cybertracker/<UUID><br>
	 * Call Type: POST<br>
	 * 
	 * Payload: a CyberTrackerPackageProxy object with the name, revision, and caUuid fields populated
	 * The revision should be of the form <uuid>.<yyyyMMddHHmmss>
	 * </p>
	 *<pre>{
	 *   "caUuid":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce",
	 *   "name":"Team A Patrol Package"
	 *   "revision": "8f7fbe1b201a4ef4bda814f5581e65ce.20190901083423"
	 *}</pre>
	 * 
	 * @return the location of where to upload the file in the "location" header, in javascript you can get it like: oReq.getResponseHeader("location");
	 * where oReq is the XMLHttpRequest object used to post this request.
	 */
	@POST
    @Path("/packages/{uuid}")
	public String createPackage(@PathParam("uuid") String packageUuidstr, CyberTrackerPackageProxy proxy){
		UUID packageUuid = null;
		
		try{
			packageUuid = UUID.fromString(packageUuidstr);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidPackageUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		if (proxy.getCaUuid() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Conservation area must be provided"); //$NON-NLS-1$
		}
		
		//parse the revision number and make sure it is of the
		//form <uuid>.<date>
		int index = proxy.getVersion().indexOf('.');
		if (index != 32) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid revision value.  Must be of the form <uuid>.<date>"); //$NON-NLS-1$
		}
		String uuid = proxy.getVersion().substring(0, 32);
		try {
			UuidUtils.stringToUuid(uuid);
			DateTimeFormatter sdf = DateTimeFormatter.ofPattern(ICtPackage.PACKAGE_DATE_FORMAT);
			sdf.parse(proxy.getVersion().substring(33));
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid revision value.  Must be of the form <uuid>.<date>"); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ConservationAreaInfo cainfo = s.get(ConservationAreaInfo.class, proxy.getCaUuid());
			if (cainfo == null) throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.CaNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			
			if (cainfo.getStatus() == ConservationAreaInfo.Status.CCAA){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid conservation area"); //$NON-NLS-1$
			}
			
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, cainfo.getUuid())) {
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
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

			boolean isnew = false;
			CyberTrackerPackage ctpackage = QueryFactory.buildQuery(s, CyberTrackerPackage.class, "ctPackageUuid", packageUuid).uniqueResult(); //$NON-NLS-1$
			if (ctpackage == null) {
				ctpackage = new CyberTrackerPackage();
				ctpackage.setConservationArea(cainfo);
				ctpackage.setCtPackageUuid(packageUuid);
				isnew = true;
			}else {
				if (ctpackage.getStatus() == Status.UPLOADING) {
					throw new SmartConnectException(Response.Status.CONFLICT, Messages.getString("CyberTracker.PackageUploadingError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
				if (!ctpackage.getConservationArea().equals(cainfo)) {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.PackageExistsError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}
			ctpackage.setType(proxy.getType());

			java.nio.file.Path upDir =  DataStoreManager.INSTANCE.getRootDirectory().resolve(CT_PACKAGE_DATASTORE_LOCATION);
			if (!Files.exists(upDir)) Files.createDirectories(upDir);
			
			ctpackage.setIsPrivate(proxy.getIsPrivate());
			ctpackage.setStatus(Status.UPLOADING);
			ctpackage.setName(proxy.getName());
			ctpackage.setUploadedDate(ZonedDateTime.now());
			ctpackage.setVersion(proxy.getVersion());
			StringBuilder sb = new StringBuilder();
			sb.append(UuidUtils.uuidToString(ctpackage.getCtPackageUuid()));
			sb.append("."); //$NON-NLS-1$
			sb.append(ctpackage.getVersion());
			sb.append(".zip"); //$NON-NLS-1$
			ctpackage.setFilename(sb.toString());
			
			if (isnew) s.persist(ctpackage);
			
			//delete file
			java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CT_PACKAGE_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
			if (Files.exists(toDelete)) {
				Files.delete(toDelete);	
			}
			
			WorkItem up = new WorkItem();
			up.setLocale(request.getLocale());
			up.setConservationAreaInfo(cainfo);
			up.setStartTime(LocalDateTime.now());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_CTPACKAGE);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename(ctpackage.getFilename()); 
			s.persist(up);
			
			ctpackage.setWorkItem(up.getUuid());

			java.nio.file.Path p = Paths.get(CT_PACKAGE_DATASTORE_LOCATION);
			up.setLocalFilename(p.resolve(ctpackage.getFilename()).toString());
			
			java.nio.file.Path upFile = DataStoreManager.INSTANCE.getRootDirectory().resolve(up.getLocalFilename());
			if (Files.exists(upFile)) Files.delete(upFile);
			
			
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
	 * <p>Creates a new navigation layer OR updates an existing layer.</p>
	 * 
	 * <p>Returns the URL that can be used for uploading the cybertracker navigation layer:</p>
	 * <p>URL: ../server/api/navigationlayers/<UUID><br>
	 * Call Type: POST<br>
	 * 
	 * Payload: a CyberTrackerNavigationProxy object with the name and caUuid fields populated
	 * 
	 * </p>
	 *<pre>{
	 *   "caUuid":"8f7fbe1b-201a-4ef4-bda8-14f5581e65ce",
	 *   "name":"Team A Patrol Package"
	 *}</pre>
	 * 
	 * @return the location of where to upload the file in the "location" header, in javascript you can get it like: oReq.getResponseHeader("location");
	 * where oReq is the XMLHttpRequest object used to post this request.
	 */
	@POST
    @Path("/navigationlayers/{uuid}")
	public String createNavigationLayer(@PathParam("uuid") String uuid, CyberTrackerNavigationProxy proxy){
		UUID navUuid = null;
		
		try{
			navUuid = UUID.fromString(uuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.InvalidPackageUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		if (proxy.getCaUuid() == null){
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Conservation area must be provided"); //$NON-NLS-1$
		}
		
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			ConservationAreaInfo cainfo = s.get(ConservationAreaInfo.class, proxy.getCaUuid());
			if (cainfo == null) throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.CaNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$

			if (cainfo.getStatus() == ConservationAreaInfo.Status.CCAA){
				throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid conservation area"); //$NON-NLS-1$
			}

			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, cainfo.getUuid())) {
				throw new SmartConnectException(Response.Status.UNAUTHORIZED);
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

			CyberTrackerNavigationLayer ctpackage = QueryFactory.buildQuery(s, CyberTrackerNavigationLayer.class, "uuid", navUuid).uniqueResult(); //$NON-NLS-1$
			boolean isnew = false;
			if (ctpackage == null) {
				ctpackage = new CyberTrackerNavigationLayer();
				ctpackage.setConservationArea(cainfo);
				ctpackage.setUuid(navUuid);
				isnew = true;
			}else {
				if (ctpackage.getStatus() == org.wcs.smart.connect.model.CyberTrackerNavigationLayer.Status.UPLOADING) {
					throw new SmartConnectException(Response.Status.CONFLICT, Messages.getString("CyberTracker.PackageUploadingError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
				if (!ctpackage.getConservationArea().equals(cainfo)) {
					throw new SmartConnectException(Response.Status.BAD_REQUEST, Messages.getString("CyberTracker.PackageExistsError", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
				}
			}

			
			java.nio.file.Path upDir =  DataStoreManager.INSTANCE.getRootDirectory().resolve(CT_NAVIGATION_DATASTORE_LOCATION);
			if (!Files.exists(upDir)) Files.createDirectories(upDir);
			
			ctpackage.setStatus(org.wcs.smart.connect.model.CyberTrackerNavigationLayer.Status.UPLOADING);
			ctpackage.setName(proxy.getName());
			ctpackage.setUploadedDate(ZonedDateTime.now());
			
			StringBuilder sb = new StringBuilder();
			sb.append(URLUtils.cleanFilename(ctpackage.getName()));
			sb.append("."); //$NON-NLS-1$
			sb.append(UuidUtils.uuidToString(ctpackage.getUuid()));
			sb.append(".zip"); //$NON-NLS-1$
			ctpackage.setFilename(sb.toString());
			
			if (isnew) s.persist(ctpackage);
			
			//delete any existing file
			java.nio.file.Path toDelete = DataStoreManager.INSTANCE.getRootDirectory()
					.resolve(CT_NAVIGATION_DATASTORE_LOCATION).resolve(ctpackage.getFilename());
			if (Files.exists(toDelete)) {
				Files.delete(toDelete);	
			}
			
			WorkItem up = new WorkItem();
			up.setLocale(request.getLocale());
			up.setConservationAreaInfo(cainfo);
			up.setStartTime(LocalDateTime.now());
			up.setStatus(WorkItem.Status.UPLOADING);
			up.setType(Type.UP_NAVIGATION);
			up.setTotalBytes(totalBytes);
			up.setLocalFilename(ctpackage.getFilename()); 
			s.persist(up);
			
			ctpackage.setWorkItem(up.getUuid());

			java.nio.file.Path p = Paths.get(CT_NAVIGATION_DATASTORE_LOCATION);
			up.setLocalFilename(p.resolve(ctpackage.getFilename()).toString()); 
			
			
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
	 * Gets the current cybertracker api key for the Conservation Area.  If not created/set
	 * then a new one is created.  This is used for downloading
	 * cybertracker packages / updates.  Also used for uploading
	 * data back to SMART 
	 * 
	 * <p>
	 * URL: ../server/api/cybertracker/apikey<br>
	 * Call Type: GET<br>
	 * </p>
	 * @return
	 */
	@GET
    @Path("/apikey/{uuid}")
    public String getApiKey(@PathParam("uuid") String cauuid, @QueryParam("type") String type){
		
		if (cauuid == null || cauuid.isEmpty()) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		if (type == null || type.isEmpty()) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		CyberTrackerApiKey.Type keytype = null;
		try {
			keytype = CyberTrackerApiKey.Type.valueOf(type);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}
		
		UUID cuuid = null;
		try {
			cuuid = UuidUtils.stringToUuid(cauuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}
		
		ConservationAreaInfo ca = new ConservationAreaInfo();
		ca.setUuid(cuuid);
		
		CyberTrackerApiKey key = null;
		Session s = HibernateManager.getSession(context);
		try{
			s.beginTransaction();
			
			ca = s.get(ConservationAreaInfo.class, ca.getUuid());
			if (ca == null) {
				throw new SmartConnectException(Response.Status.NOT_FOUND, "Conservation area not found on SMART Connect."); //$NON-NLS-1$
			}
			//must be a cybertracker user to be able to create keys
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, ca.getUuid())) throw new SmartConnectException(Response.Status.FORBIDDEN);
			key = QueryFactory.buildQuery(s, CyberTrackerApiKey.class, 
					new Object[] {"id.conservationArea",ca}, //$NON-NLS-1$
					new Object[] {"id.type", keytype}).uniqueResult(); //$NON-NLS-1$
		}finally {
			s.getTransaction().rollback();
		}
		
		if (key == null) {
			synchronized (APIKEYLOCK) {
				s = HibernateManager.getSession(context);
				s.beginTransaction();
				try {
					key = QueryFactory.buildQuery(s, CyberTrackerApiKey.class, 
							new Object[] {"id.conservationArea",ca}, //$NON-NLS-1$
							new Object[] {"id.type",keytype}).uniqueResult(); //$NON-NLS-1$
					if (key == null) {
						key = new CyberTrackerApiKey();
						key.setConservationArea(ca);
						key.setType(keytype);
						key.setApiKey( UUID.randomUUID().toString() );
						s.persist(key);
					}
					s.getTransaction().commit();
				}catch (Exception ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
					if (s.getTransaction().isActive()) s.getTransaction().rollback();
					throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, ex);
				}
			}
		}
		return key.getApiKey();
	}
	
	/**
	 * Gets all the ConservationArea's with api keys that
	 * the current user has access to. 
	 * @return
	 */
	@GET
    @Path("/apikey/")
	@Produces({ MediaType.APPLICATION_JSON })
    public List<ConservationAreaInfo> getApiKeys(){
		List<ConservationAreaInfo> apikeys = new ArrayList<>();

		Session s = HibernateManager.getSession(context);
		try{
			s.beginTransaction();
			List<ConservationAreaInfo> cas = QueryFactory.buildQuery(s, ConservationAreaInfo.class).list();
			for (ConservationAreaInfo ca : cas) {
				if (ca.getUuid().equals(ConservationArea.MULTIPLE_CA)) continue;
				if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CyberTrackerAction.KEY, ca.getUuid())) {
					apikeys.add(ca);
				}
			}
		}finally {
			s.getTransaction().rollback();
		}
		return apikeys;
	}
	
	
	/**
	 * Deletes the cybertracker api key for the given Conservation Area.  
	 * This needs to be 
	 * used with care as once deleted all devices with this key will be unable
	 * to upload data or download packages
	 * 
	 * <p>
	 * URL: ../server/api/cybertracker/apikey<br>
	 * Call Type: DELETE<br>
	 * </p>
	 * 
	 * @return
	 */
	@DELETE
    @Path("/apikey/{uuid}")
    public boolean deleteApiKey(@PathParam("uuid") String cauuid, @QueryParam("type") String type){
		if (cauuid == null || cauuid.isEmpty()) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		if (type == null || type.isEmpty()) throw new SmartConnectException(Response.Status.BAD_REQUEST);
		
		UUID cuuid = null;
		try {
			cuuid = UuidUtils.stringToUuid(cauuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}

		CyberTrackerApiKey.Type keytype = null;
		try {
			keytype = CyberTrackerApiKey.Type.valueOf(type);
		}catch (Exception ex) {
			throw new SmartConnectException(Response.Status.BAD_REQUEST);
		}

		
		ConservationAreaInfo ca = new ConservationAreaInfo();
		ca.setUuid(cuuid);
		
		CyberTrackerApiKey key = null;
		Session s = HibernateManager.getSession(context);
		s.beginTransaction();
		try{
			if (!SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), CaAdminAccountAction.KEY, ca.getUuid())) throw new SmartConnectException(Response.Status.FORBIDDEN);
			key = QueryFactory.buildQuery(s, CyberTrackerApiKey.class,
					new Object[] {"id.conservationArea", ca}, //$NON-NLS-1$
					new Object[] {"id.type", keytype}).uniqueResult(); //$NON-NLS-1$
			if (key != null) s.remove(key);
			s.getTransaction().commit();
		}catch(SmartConnectException ex) {
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
			if (ex instanceof SmartConnectException) throw ex;
		}catch (Exception ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR, ex);
		}
		return true;
	}
}
