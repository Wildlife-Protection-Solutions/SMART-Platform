/*
 * Copyright (C) 2022 Wildlife Conservation Society
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

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Session;
import org.json.JSONArray;
import org.locationtech.jts.io.ParseException;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.query.custom.CustomIncidentQueryEngine;
import org.wcs.smart.connect.query.custom.CustomPatrolQueryEngine;
import org.wcs.smart.connect.query.custom.CustomQueryEngine;
import org.wcs.smart.connect.query.custom.CustomWaypointQueryEngine;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.CustomQueryAccountAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;


/**
 * SMART REST API that queries patrols, incidents and waypoints and
 * returns the results as custom json format.
 * 
 * @author Emily
 * 
 *
 */
@Path(ConnectRESTApplication.PATH_SEPERATOR + CustomQueryApi.PATH)
@SecuritySchemes(value = {
		@SecurityScheme(name="apikeyquery",  type = SecuritySchemeType.APIKEY,	in = SecuritySchemeIn.QUERY, paramName=SharedLinkApi.TOKEN_QUERY_PARAM)
		})
public class CustomQueryApi extends HttpServlet{
	
	private static final long serialVersionUID = 1L;

	private final Logger logger = Logger.getLogger(CustomQueryApi.class.getName());
	
	public static final String PATH = QueryApi.PATH + ConnectRESTApplication.PATH_SEPERATOR + "custom"; //$NON-NLS-1$
	
	private static final SmartConnectException FILTER_REQURIED = 
			new SmartConnectException(Status.BAD_REQUEST, "Exactly one query filter must be provided"); //$NON-NLS-1$
	
	@Context private ServletContext context; 
	@Context private HttpServletRequest request;
	
	
	/**
	 * <p>Queries patrols for waypoints and returns the results as json.</p>
	 * <p>
	 * URL: ../server/api/query/custom/patrol<br>
	 * Call Type: GET
	 * </p>
	*/
	@GET
    @Path("/patrol")
	@Operation(description="Runs query patrol and returns the results as json")
	public Response runPatrolQuery(
			@Parameter(description="query by smart patrol uuid") @QueryParam("patrol_uuid") String smartpatroluuid,
			@Parameter(description="query by client patrol uuid") @QueryParam("client_patrol_uuid") String clientpatroluuid) {
	
		//only one filter is allowed
		int cnt = 0;
		String[] params = new String[] {smartpatroluuid, clientpatroluuid};
		for (String x : params) {
			if (x != null && !x.trim().isEmpty()) cnt++;
		}
		if (cnt != 1) throw FILTER_REQURIED;
			
		CustomPatrolQueryEngine engine = new CustomPatrolQueryEngine();

		List<Patrol> patrols = null;
		try(Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale())){
			try {
				s.beginTransaction();
					
				Set<UUID> conservationAreas = findConservationAreas(s);
				if (!conservationAreas.isEmpty()) {
					
					if (smartpatroluuid != null) {
						patrols = engine.getPatrolsByPatrolUuid(s, smartpatroluuid, conservationAreas);
					}else if (clientpatroluuid != null) {
						patrols = engine.getPatrolsByClientUuid(s, clientpatroluuid, conservationAreas);
					}
					
					if (patrols == null || patrols.isEmpty() ) {
						//empty list
						return Response
								.status(Status.OK)
								.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
								.entity(((new JSONArray()).toString()))
								.build();
					}
				}
				return Response
						.status(Status.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
						.entity(engine.convertPatrolsToJSON(patrols, s, request.getLocale()).toString())
						.build();
			}finally {
				s.getTransaction().commit();
			}
		}catch(RuntimeException ex) {
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			throw ex;
		}
	}
	/**
	 * <p>Queries patrols for waypoints and returns the results as json.</p>
	 * <p>
	 * URL: ../server/api/query/custom/waypoint/patrol<br>
	 * Call Type: GET
	 * </p>
	*/
	@GET
    @Path("/waypoint/patrol")
	@Operation(description="Runs query patrol and returns the results as json")
	public Response runPatrolQuery(
			@Parameter(description="query by smart patrol uuid") @QueryParam("patrol_uuid") String smartpatroluuid,
			@Parameter(description="query by client patrol uuid") @QueryParam("client_patrol_uuid") String clientpatroluuid,
			@Parameter(description="query by smart patrol leg uuid") @QueryParam("patrolleg_uuid") String smartpatrolleguuid,
			@Parameter(description="query by client patrol leg uuid") @QueryParam("client_patrolleg_uuid") String clientpatrolleguuid,
			@Parameter(description="query by smart patrol id") @QueryParam("patrol_id") String patrolid,
			@Parameter(description="query by waypoint date") @QueryParam("waypoint_date") String wpdate,
			@Parameter(description="query by waypoint uuid") @QueryParam("waypoint_uuid") String wpuuid,
			@Parameter(description="query by waypoint last modified date") @QueryParam("waypoint_lastmodified") String wpLastModified,
			@Parameter(description="query by patrol start date") @QueryParam("patrol_startdate") String patrolStartDate,
			@Parameter(description="query by patrol end date") @QueryParam("patrol_enddate") String patrolEndDate) throws SQLException{

		
		//only one filter is allowed
		int cnt = 0;
		String[] params = new String[] {smartpatroluuid, clientpatroluuid, 
				smartpatrolleguuid, clientpatrolleguuid, patrolid,
				wpLastModified, patrolStartDate, patrolEndDate, wpdate, wpuuid};
		for (String x : params) {
			if (x != null && !x.trim().isEmpty()) cnt++;
		}
		if (cnt != 1) throw FILTER_REQURIED;
		
		CustomPatrolQueryEngine engine = new CustomPatrolQueryEngine();

		List<PatrolWaypoint> wps = null;
		try(Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale())){
			try {
				s.beginTransaction();
				
				Set<UUID> conservationAreas = findConservationAreas(s);
				if (!conservationAreas.isEmpty()) {
					if (smartpatroluuid != null) {
						wps = engine.getWaypointsByPatrolUuid(s, smartpatroluuid, conservationAreas);
					}else if (clientpatroluuid != null) {
						wps = engine.getWaypointsByClientUuid(s, clientpatroluuid, conservationAreas);
					}else if (smartpatrolleguuid != null) {
						wps = engine.getWaypointsByPatrolLegUuid(s, smartpatrolleguuid, conservationAreas);
					}else if (clientpatrolleguuid != null) {
						wps = engine.getWaypointsByClientLegUuid(s, clientpatrolleguuid, conservationAreas);
					}else if (patrolid != null) {
						wps = engine.getWaypointsByPatrolId(s, patrolid, conservationAreas);
					}else if (wpuuid != null) {
						wps = engine.getWaypointsByPatrolWaypointUuid(s, wpuuid, conservationAreas);
					}else if (wpdate != null) {
						LocalDate[] date = parseDateString(wpdate);
						if (date.length == 1) {
							wps = engine.getWaypointsByPatrolWaypointDate(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByPatrolWaypointDate(s, date[0], date[1], conservationAreas);
						}
					}else if (wpLastModified != null) {
						LocalDate[] date = parseDateString(wpLastModified);
						if (date.length == 1) {
							wps = engine.getWaypointsByLastModified(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByLastModified(s, date[0], date[1], conservationAreas);
						}
					}else if (patrolStartDate != null) {
						LocalDate[] date = parseDateString(patrolStartDate);
						if (date.length == 1) {
							wps = engine.getWaypointsByPatrolStartDate(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByPatrolStartDate(s, date[0], date[1], conservationAreas);
						}
					}else if (patrolEndDate != null) {
						LocalDate[] date = parseDateString(patrolEndDate);
						if (date.length == 1) {
							wps = engine.getWaypointsByPatrolEndDate(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByPatrolEndDate(s, date[0], date[1], conservationAreas);
						}
					}
				}
				
				if (wps == null || wps.isEmpty() ) {
					//empty list
					return Response
							.status(Status.OK)
							.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
							.entity(((new JSONArray()).toString()))
							.build();
				}
				return Response
						.status(Status.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
						.entity(engine.convertToJSON(wps, s, request.getLocale()).toString())
						.build();
			}finally {
				s.getTransaction().commit();
			}
		}catch(RuntimeException ex) {
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			throw ex;
		}
	}
	
	/**
	 * <p>Queries patrols for tracks and returns the results as GeoJSON</p>
	 * <p>
	 * URL: ../server/api/query/custom/track/patrol<br>
	 * Call Type: GET
	 * </p>
	*/
	@GET
    @Path("/track/patrol")
	@Operation(description="Queries patrol tracks and and returns the results as GeoJSON")
	public Response runPatrolTrackQuery(
			@Parameter(description="query by smart patrol uuid") @QueryParam("patrol_uuid") String smartpatroluuid,
			@Parameter(description="query by client patrol uuid") @QueryParam("client_patrol_uuid") String clientpatroluuid,
			@Parameter(description="query by smart patrol leg uuid") @QueryParam("patrolleg_uuid") String smartpatrolleguuid,
			@Parameter(description="query by client patrol leg uuid") @QueryParam("client_patrolleg_uuid") String clientpatrolleguuid,
			@Parameter(description="query by smart patrol id") @QueryParam("patrol_id") String patrolid,
			@Parameter(description="query by track date - can provide a single date or range of dates") @QueryParam("track_date") String trackDate
			) throws SQLException{

		
		//only one filter is allowed
		int cnt = 0;
		String[] params = new String[] {smartpatroluuid, clientpatroluuid, 
				smartpatrolleguuid, clientpatrolleguuid, patrolid, trackDate};
		for (String x : params) {
			if (x != null && !x.trim().isEmpty()) cnt++;
		}
		if (cnt != 1) throw FILTER_REQURIED;
		
		CustomPatrolQueryEngine engine = new CustomPatrolQueryEngine();

		List<Track> tracks = new ArrayList<>();
		try(Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale())){
			try {
				s.beginTransaction();
				
				Set<UUID> conservationAreas = findConservationAreas(s);
				if (!conservationAreas.isEmpty()) {
					if (smartpatroluuid != null || clientpatroluuid != null || patrolid != null) {
						List<Patrol> patrols = Collections.emptyList();
						if (smartpatroluuid != null) {
							patrols = engine.getPatrolsByPatrolUuid(s, smartpatroluuid, conservationAreas);
						}else if (clientpatroluuid != null){
							patrols = engine.getPatrolsByClientUuid(s, clientpatroluuid, conservationAreas);
						}else if (patrolid != null) {
							patrols = engine.getPatrolsById(s, patrolid, conservationAreas);

						}
						for(Patrol p : patrols) {
							for (PatrolLeg l : p.getLegs()) {
								for (PatrolLegDay d : l.getPatrolLegDays()) {
									tracks.addAll(d.getTracks());
									if (tracks.size() > CustomQueryEngine.MAX_RESULTS) throw CustomQueryEngine.TOO_MANY_ROWS;
								}
							}
						}
					}else if (smartpatrolleguuid != null || clientpatrolleguuid != null) {
						List<PatrolLeg> legs = Collections.emptyList();
						if (smartpatrolleguuid != null) {
							legs = engine.getPatrolLegByUuid(s, smartpatrolleguuid, conservationAreas);
						}else if (clientpatrolleguuid != null){
							legs = engine.getPatrolLegByClientUuid(s, clientpatrolleguuid, conservationAreas);
						}
						
						for (PatrolLeg l : legs) {
							for (PatrolLegDay d : l.getPatrolLegDays()) {
								tracks.addAll(d.getTracks());
								if (tracks.size() > CustomQueryEngine.MAX_RESULTS) throw CustomQueryEngine.TOO_MANY_ROWS;
							}
						}
						
					}else if (trackDate != null) {
						LocalDate[] date = parseDateString(trackDate);
						if (date.length == 1) {
							tracks = engine.getPatrolTracksByDate(s, date[0], null, conservationAreas);
						}else {
							tracks = engine.getPatrolTracksByDate(s, date[0], date[1], conservationAreas);
						}
						
					}
				}
				
				if (tracks == null || tracks.isEmpty() ) {
					//empty list
					return Response
							.status(Status.OK)
							.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
							.entity(((new JSONArray()).toString()))
							.build();
				}
				try {
					return Response
							.status(Status.OK)
							.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
							.entity(engine.convertTrackToJSON(tracks, s, request.getLocale()).toString())
							.build();
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
			}finally {
				s.getTransaction().commit();
			}
		}catch(RuntimeException ex) {
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			throw ex;
		}
	}

	private Set<UUID> findConservationAreas(Session s) {
		Set<UUID> conservationAreas = new HashSet<>();
		
		//if admin -> access all
		//if ca admin -> access specified ca
		//if custom query api -> access specified ca (can be all cas)
		List<ConservationAreaInfo> allcas = QueryFactory.buildQuery(s, ConservationAreaInfo.class).list();
		if (SecurityManager.INSTANCE.canAccess(s, request.getUserPrincipal().getName(), AdminAccountAction.KEY)) {
			//admin -> can access all cas
			for (ConservationAreaInfo i : allcas) conservationAreas.add(i.getUuid());
		}else {
			for (ConservationAreaInfo i : allcas) {
				if (SecurityManager.INSTANCE.canAccess(s,  request.getUserPrincipal().getName(), CaAdminAccountAction.KEY, i.getUuid())) {
					//ca admin
					conservationAreas.add(i.getUuid());
				}else if (SecurityManager.INSTANCE.canAccess(s,  request.getUserPrincipal().getName(), CustomQueryAccountAction.KEY, i.getUuid())) { 
					//custom query api
					conservationAreas.add(i.getUuid());
				}
			}
		}
		return conservationAreas;
	}
	
	private LocalDate[] parseDateString(String date) {
		if (date.contains(":")) { //$NON-NLS-1$
			String s1 = date.split(":")[0]; //$NON-NLS-1$
			String s2 = date.split(":")[1]; //$NON-NLS-1$
			return new LocalDate[] {LocalDate.parse(s1), LocalDate.parse(s2)};
		}else {
			return new LocalDate[] {LocalDate.parse(date)};
		}
	}
	
	/**
	 * <p>Queries independent incidents and returns results as json</p>
	 * <p>
	 * URL: ../server/api/query/custom/waypoint/incident<br>
	 * Call Type: GET
	 * </p>
	*/
	@GET
    @Path("/waypoint/incident")
	@Operation(description="Runs a custom incident query and returns the results as json")
	public Response runIndependentIncidentQuery(
			@Parameter(description="smart incident uuid") @QueryParam("incident_uuid") String incidentuuid,
			@Parameter(description="client incident uuid") @QueryParam("client_incident_uuid") String clientincidentuuid,
			@Parameter(description="incident id") @QueryParam("incident_id") String incidentid,
			@Parameter(description="waypoint last modified date") @QueryParam("waypoint_lastmodified") String wpLastModified,
			@Parameter(description="waypoint date") @QueryParam("waypoint_date") String wpDate) throws SQLException{

		
		//only one filter is allowed
		int cnt = 0;
		String[] params = new String[] {incidentuuid, clientincidentuuid, 
				incidentid, wpLastModified, wpDate};
		for (String x : params) {
			if (x != null && !x.trim().isEmpty()) cnt++;
		}
		if (cnt != 1) throw FILTER_REQURIED;
		
		CustomIncidentQueryEngine engine = new CustomIncidentQueryEngine();

		List<Waypoint> wps = null;
		try(Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale())){
			try {
				s.beginTransaction();
				
				Set<UUID> conservationAreas = findConservationAreas(s);

				if (!conservationAreas.isEmpty()) {

					if (incidentuuid != null) {
						wps = engine.getWaypointsByUUID(s, incidentuuid, conservationAreas);
					}else if (clientincidentuuid != null) {
						wps = engine.getWaypointsByClient(s, clientincidentuuid, conservationAreas);
					}else if (incidentid != null) {
						wps = engine.getWaypointsById(s, incidentid, conservationAreas);
					}else if (wpLastModified != null) {
						LocalDate[] date = parseDateString(wpLastModified);
						if (date.length == 1) {
							wps = engine.getWaypointsByLastModified(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByLastModified(s, date[0], date[1], conservationAreas);
						}
					}else if (wpDate != null) {
						LocalDate[] date = parseDateString(wpDate);
						if (date.length == 1) {
							wps = engine.getWaypointsByDate(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByDate(s, date[0], date[1], conservationAreas);
						}
					}
				}
				
				if (wps == null || wps.isEmpty()) {
					//empty list
					return Response
							.status(Status.OK)
							.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
							.entity(((new JSONArray()).toString()))
							.build();
				}
				return Response
						.status(Status.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
						.entity(engine.convertToJSON(wps, s).toString())
						.build();
			}finally {
				s.getTransaction().commit();
			}
		}catch(RuntimeException ex) {
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			throw ex;
		}
	}
	
	/**
	 * <p>Queries all waypoints (irregardless of source)</p>
	 * <p>
	 * URL: ../server/api/query/custom/waypoint<br>
	 * Call Type: GET
	 * </p>
	*/
	@GET
    @Path("/waypoint")
	@Operation(description="Queries all waypoints and returns the results as json")
	public Response queryWaypoints(
			@Parameter(description="smart waypoint uuid") @QueryParam("waypoint_uuid") String waypointuuid,
			@Parameter(description="client waypoint uuid") @QueryParam("client_waypoint_uuid") String clientwaypointuuid,
			@Parameter(description="waypoint last modified date") @QueryParam("waypoint_lastmodified") String wpLastModified,
			@Parameter(description="waypoint date") @QueryParam("waypoint_date") String wpDate) throws SQLException{

		
		//only one filter is allowed
		int cnt = 0;
		String[] params = new String[] {waypointuuid, clientwaypointuuid, wpLastModified, wpDate};
		for (String x : params) {
			if (x != null && !x.trim().isEmpty()) cnt++;
		}
		if (cnt != 1) throw FILTER_REQURIED;
		
		CustomWaypointQueryEngine engine = new CustomWaypointQueryEngine();

		List<Waypoint> wps = null;
		try(Session s = HibernateManager.getSession(request.getServletContext(), request.getLocale())){
			try {
				s.beginTransaction();
				
				Set<UUID> conservationAreas = findConservationAreas(s);

				if (!conservationAreas.isEmpty()) {

					if (waypointuuid != null) {
						wps = engine.getWaypointsByUUID(s, waypointuuid, conservationAreas);
					}else if (clientwaypointuuid != null) {
						wps = engine.getWaypointsByClient(s, clientwaypointuuid, conservationAreas);
					}else if (wpLastModified != null) {
						LocalDate[] date = parseDateString(wpLastModified);
						if (date.length == 1) {
							wps = engine.getWaypointsByLastModified(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByLastModified(s, date[0], date[1], conservationAreas);
						}
					}else if (wpDate != null) {
						LocalDate[] date = parseDateString(wpDate);
						if (date.length == 1) {
							wps = engine.getWaypointsByDate(s, date[0], conservationAreas);
						}else if (date.length == 2) {
							wps = engine.getWaypointsByDate(s, date[0], date[1], conservationAreas);
						}
					}
				}
				
				
				if (wps == null || wps.isEmpty()) {
					//empty list
					return Response
							.status(Status.OK)
							.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
							.entity(((new JSONArray()).toString()))
							.build();
				}
				return Response
						.status(Status.OK)
						.header("Content-Type", MediaType.APPLICATION_JSON) //$NON-NLS-1$
						.entity(engine.convertToJSON(wps, s, true).toString())
						.build();
			}finally {
				s.getTransaction().commit();
			}
		}catch(RuntimeException ex) {
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			throw ex;
		}	
	}
	
}


		
