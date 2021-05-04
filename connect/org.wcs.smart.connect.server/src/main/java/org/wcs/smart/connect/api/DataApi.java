/*
 * Copyright (C) 2021 Wildlife Conservation Society
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.AttachmentInterceptor;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.json.JsonFileProcessor;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.patrol.metadata.PatrolAttributeMetadata;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.util.UuidUtils;

import io.swagger.v3.oas.annotations.Parameter;


/**
 * Data  API.  Provides api for loading JSON data 
 * directly into a Conservation Area using the 
 * standard json format and processors
 * 
 * @author Emily
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR )
@Consumes({ MediaType.APPLICATION_JSON})
@Produces({ MediaType.APPLICATION_JSON })
public class DataApi extends HttpServlet{

	private static final long serialVersionUID = 1L;
	private final Logger logger = Logger.getLogger(DataApi.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpHeaders headers;
	@Context private HttpServletResponse response;
	@Context private HttpServletRequest request;
	

	/**
	 * Gets the signature type metadata for a conservation area.  Signature types can be
	 * added to waypoint attachments to identify specific signatures.
	 * 
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/metadata/signatures/{cauuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getDataModel(
			@Parameter(description="uuid of the conservation area to get signature defintions for") @PathParam("cauuid") String uuid) {
		
		UUID caUuid = parseUuid(uuid);
		
		try(Session s = HibernateManager.getSession(context)){
			s.beginTransaction();
			try {
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.VIEWCA_KEY,
						caUuid)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				
				ConservationArea ca = s.get(ConservationArea.class, caUuid);
				if (ca == null) throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataModelApi.CaNotFound", request.getLocale())); //$NON-NLS-1$
					
				
				List<SignatureType> types = QueryFactory.buildQuery(s, SignatureType.class, 
						new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
				
				JSONArray items = new JSONArray();
				for (SignatureType type : types) {
					JSONObject t = new JSONObject();
					t.put("key", type.getKeyId()); //$NON-NLS-1$
					for (Label l : type.getNames()) {
						String key = "name_" + l.getLanguage().getCode(); //$NON-NLS-1$
						t.put(key,l.getValue());
					}
					items.put(t);
				}
				return Response.status(Response.Status.OK)
						.entity(items.toString())
						.type(MediaType.APPLICATION_JSON)
						.build();
			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	/**
	 * Gets the patrol metadata for a conservation area.
	 * 
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/metadata/patrol/{cauuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<PatrolAttributeMetadata> getPatrolMetadata(
			@Parameter(description="uuid of the conservation area to get patrol metadata for") @PathParam("cauuid") String uuid) {
		
		UUID caUuid = parseUuid(uuid);
		
		try(Session s = HibernateManager.getSession(context)){
			s.beginTransaction();
			try {
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.VIEWCA_KEY,
						caUuid)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				
				ConservationArea ca = s.get(ConservationArea.class, caUuid);
				if (ca == null) throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataModelApi.CaNotFound", request.getLocale())); //$NON-NLS-1$
					
				List<PatrolAttributeMetadata> pMetadata = new ArrayList<>();
				for (PatrolAttributeMetadata.FixedMetadata fixed : PatrolAttributeMetadata.FixedMetadata.values()) {
					pMetadata.add(fixed.toMetadata(s, ca));
				}
				
				List<PatrolAttribute> customs = QueryFactory.buildQuery(s, PatrolAttribute.class, 
						new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
				for (PatrolAttribute custom : customs) {
					pMetadata.add(PatrolAttributeMetadata.toMetadata(custom));
				}
				
				return pMetadata;

			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	@POST
	@Path("/data/{cauuid}")
	public ProcessingResult processJsonData(
			@Parameter(description="uuid of the conservation area associated with the data") @PathParam("cauuid") String uuid,
			String body) {

		UUID caUuid = parseUuid(uuid);
		
		Set<Class<? extends IJsonFeatureProcessor>> processors = new HashSet<>();
		for (IWaypointSource src:WaypointSourceEngine.INSTANCE.getSupportedSources()) {
			Class<? extends IJsonFeatureProcessor> p = src.getJsonFeatureProcessor();
			if (p != null) processors.add(p);
		}
		
		
		try(Session s = HibernateManager.getSession(context, request.getLocale(), new AttachmentInterceptor())){
			s.beginTransaction();
			try {
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.UPDATECA_KEY,
						caUuid)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				
				ConservationArea ca = s.get(ConservationArea.class, caUuid);
				if (ca == null) throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataModelApi.CaNotFound", request.getLocale())); //$NON-NLS-1$
					
				JsonFileProcessor processor = null;
				try {
					try{
						processor = JsonFileProcessor.create(ca, processors, request.getLocale());
					}catch (Exception ex) {
						logger.log(Level.SEVERE, ex.getMessage(),ex);
						throw new SmartConnectException(Response.Status.INTERNAL_SERVER_ERROR,ex.getMessage());
					}
					
					try(InputStream is = new ByteArrayInputStream(body.getBytes())){
						processor.processData(is, s);
					}catch(Exception ex) {
						logger.log(Level.INFO, ex.getMessage(),ex);
						throw new SmartConnectException(Response.Status.BAD_REQUEST, ex.getMessage(), ex);
					}
					
					s.getTransaction().commit();
				}finally {
					if (processor != null) processor.dispose();
				}
				
				ProcessingResult rr = new ProcessingResult(processor.getWarnings(), processor.getMessages().stream().collect(Collectors.joining(" "))); //$NON-NLS-1$
				return rr;
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
		}
		
	}
	
	private UUID parseUuid(String uuid) throws SmartConnectException{
		UUID itemUuid = null;
		try{
			itemUuid= UuidUtils.stringToUuid(uuid);
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Invalid uuid: " + uuid + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
			throw new SmartConnectException(Response.Status.BAD_REQUEST, "Invalid Conservation Area UUID", ex); //$NON-NLS-1$
		}
		return itemUuid;
	}

	class ProcessingResult{
		
		private List<String> warnings;
		private String completeMessage;
		
		public ProcessingResult(List<String> warnings, String message) {
			this.warnings = warnings.isEmpty() ? null : warnings;
			this.completeMessage = message;
		}
		
		public String getMessage() {
			return this.completeMessage;
		}
		
		public List<String> getWarnings(){
			return this.warnings;
		}
	}
}
