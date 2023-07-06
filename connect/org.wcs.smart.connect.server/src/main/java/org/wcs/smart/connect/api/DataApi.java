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

import org.eclipse.core.runtime.RegistryFactory;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.AttachmentInterceptor;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.er.json.SurveyDesignMetadata;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.incident.patrol.IncidentToPatrolProcessor;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.json.JsonFileProcessor;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.patrol.json.PatrolAttributeMetadata;
import org.wcs.smart.patrol.json.PatrolAttributeMetadata.FixedPatrolMetadata;
import org.wcs.smart.patrol.model.PatrolAttribute;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
	 * Gets the patrol metadata for a conservation area.
	 * 
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/metadata/patrol/{cauuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public PatrolMetadata getPatrolMetadata(
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
				for (PatrolAttributeMetadata.FixedPatrolMetadata fixed : PatrolAttributeMetadata.FixedPatrolMetadata.values()) {
					pMetadata.add(fixed.toMetadata(s, ca));
				}
				
				List<PatrolAttributeMetadata> lMetadata = new ArrayList<>();
				for (PatrolAttributeMetadata.FixedPatrolMetadata fixed : PatrolAttributeMetadata.LEG_METADATA_FIELDS) {
					PatrolAttributeMetadata md = fixed.toMetadata(s, ca);
					if (fixed != FixedPatrolMetadata.LEADER && fixed != FixedPatrolMetadata.PILOT) {
						md.setLinkTo("patrolMetadata." + fixed.getKey()); //$NON-NLS-1$
					}
					md.setListOptions(null);
					lMetadata.add(md);
				}
				
				
				List<PatrolAttribute> customs = QueryFactory.buildQuery(s, PatrolAttribute.class, 
						new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
				for (PatrolAttribute custom : customs) {
					pMetadata.add(PatrolAttributeMetadata.toMetadata(custom));
				}
				
				List<PatrolAttributeMetadata> wpMetadata = new ArrayList<>();
				for (PatrolAttributeMetadata.PatrolWaypointMetadata fixed : PatrolAttributeMetadata.PatrolWaypointMetadata.values()) {
					PatrolAttributeMetadata md = fixed.toMetadata(s, ca);
					if (md != null) wpMetadata.add(md);
				}
				
				PatrolMetadata metadata = new PatrolMetadata();
				metadata.patrolMetadata = pMetadata;
				metadata.patrolLegMetadata = lMetadata;
				metadata.waypointMetadata = wpMetadata;
				metadata.signatureMetadata = PatrolAttributeMetadata.getSignatureMetadata(s, ca);
				return metadata;

			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	/**
	 * Gets the mission metadata for a conservation area.
	 * 
	 * @param uuid
	 * @return
	 */
	@GET
	@Path("/metadata/mission/{cauuid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public List<SurveyDesignMetadata> getMissionMetadata(
			@Parameter(description="uuid of the conservation area to get mission metadata for") @PathParam("cauuid") String uuid) {
		
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
				
				return SurveyDesignMetadata.getMetadata(ca, s);

			}finally {
				s.getTransaction().commit();
			}
		}
		
	}
	
	@POST
	@Path("/data/{cauuid}")
	public ProcessingResult processJsonData(
			@Parameter(description="uuid of the conservation area associated with the data") @PathParam("cauuid") String uuid,
			String body) throws Exception {

		UUID caUuid = parseUuid(uuid);
		
		Set<Class<? extends IJsonFeatureProcessor>> processors = new HashSet<>();
		for (IWaypointSource src:WaypointSourceEngine.INSTANCE.getSupportedSources()) {
			Class<? extends IJsonFeatureProcessor> p = src.getJsonFeatureProcessor();
			if (p != null) processors.add(p);
		}
		
		ProcessingResult rr = null;
		try(Session s = HibernateManager.getSession(context, request.getLocale(), new AttachmentInterceptor())){
			
			ConservationArea ca = s.get(ConservationArea.class, caUuid);
			if (ca == null) throw new SmartConnectException(Response.Status.NOT_FOUND, Messages.getString("DataModelApi.CaNotFound", request.getLocale())); //$NON-NLS-1$
			
			
			s.beginTransaction();
			try {
				if (!SecurityManager.INSTANCE.canAccess(s, 
						request.getUserPrincipal().getName(), 
						CaAction.UPDATECA_KEY,
						caUuid)){
					logger.info("User " + request.getUserPrincipal().getName() + " does not have permission to view ca."); //$NON-NLS-1$ //$NON-NLS-2$
					throw new SmartConnectException(Response.Status.UNAUTHORIZED);
				}
				
				
					
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
				
				
				rr = new ProcessingResult(processor.getWarnings(), processor.getMessages().stream().collect(Collectors.joining(" "))); //$NON-NLS-1$
			}catch (Exception ex) {
				s.getTransaction().rollback();
				throw ex;
			}
			

			//attempt to link patrols to incidents
			(new IncidentToPatrolProcessor(ca, true)).doWork(s);
			
			s.beginTransaction();
			try {
				DataLink.cleanUp(RegistryFactory.getRegistry(),s);
				s.getTransaction().commit();
			}catch (Exception ex) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
				s.getTransaction().rollback();
			}
			
			return rr;
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
	
	@JsonInclude(Include.NON_NULL)
	class PatrolMetadata{
		List<PatrolAttributeMetadata> patrolMetadata;
		List<PatrolAttributeMetadata> patrolLegMetadata;
		List<PatrolAttributeMetadata> waypointMetadata;
		PatrolAttributeMetadata signatureMetadata;
		
		public List<PatrolAttributeMetadata> getPatrolMetadata(){
			return patrolMetadata;
		}
		
		public List<PatrolAttributeMetadata> getPatrolLegMetadata(){
			return patrolLegMetadata;
		}
		
		public List<PatrolAttributeMetadata> getWaypointMetadata(){
			return waypointMetadata;
		}
		
		public PatrolAttributeMetadata getSignatureTypes() {
			return this.signatureMetadata;
		}
	}
	
}
