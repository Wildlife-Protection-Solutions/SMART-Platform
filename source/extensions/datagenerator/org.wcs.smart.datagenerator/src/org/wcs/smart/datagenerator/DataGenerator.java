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
package org.wcs.smart.datagenerator;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.geotools.geometry.jts.JTS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.datagenerator.internal.Messages;
import org.wcs.smart.datagenerator.model.ObservationConfiguration;
import org.wcs.smart.datagenerator.model.PatrolConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.common.importwp.ObservationGPSDataImport;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.patrol.model.Track;

/**
 * Generates sample patrol data using information provided by the configuration.
 * 
 * @author Emily
 *
 */
public class DataGenerator implements IDataEngine{
	
	private PatrolConfiguration config;
	
	private List<Employee> employees;
	private List<PatrolTransportType> types;
	private List<PatrolMandate> mandates;
	private List<Team> teams;
	private List<Station> stations;
	private List<Area> areas;
	
	private HashMap<Range, ObservationConfiguration> observationWeightMap;
	
	private Random random = new Random();
	
	public DataGenerator(PatrolConfiguration config) {
		this.config = config;
	}
	
	public void run(IProgressMonitor monitor) throws Exception {
		observationWeightMap = new HashMap<>();
		int current = 1;
		for (ObservationConfiguration m : config.getMappings()) {
			Range r = new Range(current, current+m.getWeight()-1 );
			observationWeightMap.put(r, m);
			current += m.getWeight();
		}
		
		List<Patrol> newPatrols = new ArrayList<>();
		
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.DataGenerator_TaskName, config.getNumberOfPatrols() + 1);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				loadPatrolMetadata(session, progress.split(1));
				
				if (types.isEmpty()) throw new Exception(Messages.DataGenerator_PatrolTypesRequired);
				if (mandates.isEmpty()) throw new Exception(Messages.DataGenerator_PatrolMandatesRequired);
				
				for (int i = 0; i < config.getNumberOfPatrols(); i ++) {
					Patrol p = generatePatrol(session, progress.split(1));
					session.save(p);
					p.getLegs().forEach(leg->leg.getPatrolLegDays().forEach(pld->pld.getWaypoints().forEach(pw->{session.save(pw.getWaypoint());session.save(pw);})));
					newPatrols.add(p);
					session.flush();
					progress.checkCanceled();
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}catch (Exception ex) {
			throw ex;
		}
		newPatrols.forEach(p->PatrolEventManager.getInstance().patrolAdded(p));
	}
	
	
	private void loadPatrolMetadata(Session session, IProgressMonitor monitor) {
		//load employees
		employees = QueryFactory.buildQuery(session, Employee.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"endEmploymentDate", null} //$NON-NLS-1$
				).list();
		
		types = QueryFactory.buildQuery(session, PatrolTransportType.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		
		mandates = QueryFactory.buildQuery(session, PatrolMandate.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		
		teams = QueryFactory.buildQuery(session, Team.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		
		stations = QueryFactory.buildQuery(session, Station.class,
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"isActive", true}).list(); //$NON-NLS-1$
		
		if (config.getBboxArea() != null) {
			areas = QueryFactory.buildQuery(session, Area.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"type", config.getBboxArea()}).list(); //$NON-NLS-1$
		}
	}
	
	private Patrol generatePatrol(Session session, IProgressMonitor monitor) throws Exception {
		
		Patrol p = new Patrol();
		p.setConservationArea(SmartDB.getCurrentConservationArea());
		
		p.setArmed(random.nextInt(100) <= 50);
		p.setId(PatrolHibernateManager.generatePatrolId(p, session));
		
		//team is optional
		int v = random.nextInt(teams.size()+1);
		if (v < teams.size()) {
			p.setTeam(teams.get(v));
		}
		
		//station is optional
		v = random.nextInt(stations.size()+1);
		if (v < stations.size()) {
			p.setStation(stations.get(v));
		}
		
		//transport type is required
		v = random.nextInt(types.size());
		PatrolTransportType ttype = types.get(v);
		p.setPatrolType(ttype.getPatrolType());
		
		//start date must be between config.getStartDate and config.getEndDate
		int offset = (int) ChronoUnit.DAYS.between(config.getStartDate(), config.getEndDate());
		if (offset != 0) {
			offset = random.nextInt(offset+1);
		}
		LocalDate start = config.getStartDate().plusDays(offset);
		int length = random.nextInt(config.getDaysPerPatrolMax() - config.getDaysPerPatrolMin() + 1) + config.getDaysPerPatrolMin();
		LocalDate end = start.plusDays(length-1);
		p.setStartDate(start );
		p.setEndDate( end );
				
		//leg
		PatrolLeg pl = new PatrolLeg();
		p.setLegs(new ArrayList<PatrolLeg>());
		p.getLegs().add(pl);
		pl.setPatrol(p);
		pl.setId("Leg 1"); //$NON-NLS-1$
		pl.setStartDate(p.getStartDate());
		pl.setEndDate(p.getEndDate());
		pl.setType(ttype);
		//mandate is require
		pl.setMandate(mandates.get( random.nextInt(mandates.size()) ));
		//members
		pl.setMembers(new ArrayList<PatrolLegMember>());
		int employeeCnt = random.nextInt(config.getEmployeesPerPatrolMax() - config.getEmployeesPerPatrolMin() +1) + config.getEmployeesPerPatrolMin();
		if (employeeCnt > employees.size()) {
			employeeCnt = employees.size();
		}
		List<Employee> tosearch = new ArrayList<>(employees);
		while(pl.getMembers().size() < employeeCnt){
			if (tosearch.size() == 0) break;
			Employee next = tosearch.get(0);
			if (tosearch.size() > 1) {
				next = tosearch.remove(random.nextInt(tosearch.size()));
			}else {
				next = tosearch.remove(0);
			}
			PatrolLegMember mem = new PatrolLegMember();
			mem.setMember(next);
			mem.setPatrolLeg(pl);
			pl.getMembers().add(mem);
		}
		//the first one is the leader
		pl.getMembers().get(0).setIsLeader(true);
		pl.setLeader(pl.getMembers().get(0));
		pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		if (pl.getPatrol().getPatrolType().requiresPilot()) {
			pl.setPilot(pl.getMembers().get(0));	
		}
		
		//generate a start position for the patrol
		Coordinate startc = generatePosition();
		
		for (int day = 0; day < length; day ++) {

			PatrolLegDay pld = new PatrolLegDay();
			pld.setPatrolLeg(pl);
			pl.getPatrolLegDays().add(pld);
			
			LocalDate date = start.plusDays(day);
			pld.setDate(  date );
			pld.setRestMinutes(random.nextInt(121));
			
			//start sometime between 1am and 11am
			int startHour = random.nextInt(11) + 1;
			//end sometime between 1pm and 11pm
			int endHour = random.nextInt(11) + 13;
			
			LocalTime startt = LocalTime.of(startHour, random.nextInt(60), random.nextInt(60));
			LocalTime endt = LocalTime.of(endHour, random.nextInt(60), random.nextInt(60));
					
			pld.setStartTime(startt);
			pld.setEndTime(endt);

			pld.setWaypoints(new ArrayList<PatrolWaypoint>());
			
			List<Waypoint> trackpoints = new ArrayList<>();
			int numWaypoints = random.nextInt(config.getWaypointsPerDayMax() - config.getWaypointsPerDayMin() + 1) + config.getWaypointsPerDayMin();
			int numTrackPoints = numWaypoints * (random.nextInt(6) + 1) ;
			
			int xdir = 1;
			if (random.nextInt(100) < 50) xdir = -1;
			int ydir = 1;
			if (random.nextInt(100) < 50) ydir = -1;
			double onedegreeinmeters = 1;
			try{
				onedegreeinmeters = JTS.orthodromicDistance(startc, new Coordinate(startc.x + 1, startc.y), SmartDB.DATABASE_CRS);
			}catch (Exception ex) {}
			
			double maxdegrees = (150.0 / onedegreeinmeters);
			long seconds = (long) Math.floor( ChronoUnit.SECONDS.between(startt, endt) / (double)numTrackPoints);

			int switchdir = random.nextInt(10) + 10;
			
			for (int i = 0; i < numTrackPoints; i++) {
			
				if (i % switchdir == 0) {
					xdir = 1;
					ydir = 1;
					if (random.nextInt(100) < 50) xdir = -1;
					if (random.nextInt(100) < 50) ydir = -1;
				}
				
				long time = seconds*i + (long)Math.floor(((random.nextInt(100) + 1) / 100.0) * seconds) + startt.toSecondOfDay();
				LocalTime wpTime = LocalTime.ofSecondOfDay(time);
				LocalDateTime wpDateTime = LocalDateTime.of(date, wpTime);
				
				
				Waypoint trackPoint = new Waypoint();
				trackPoint.setDateTime( wpDateTime );
				trackPoint.setRawX(startc.x);
				trackPoint.setRawY(startc.y);
				trackpoints.add(trackPoint);

				//move coordinate 
				double percent1 = (random.nextInt(100) + 1) / 100.0;
				double percent2 = (random.nextInt(100) + 1) / 100.0;
				startc = new Coordinate(startc.x + xdir * (maxdegrees * percent1),  startc.y + ydir * (maxdegrees * percent2)  );				
			}
			
			LineString track = ObservationGPSDataImport.convertToLineString(trackpoints);
			Track t = new Track();
			t.setLineStrings(Arrays.asList(track));
			pld.setTrack(t);
			t.setPatrolLegDay(pld);
			
			//randomly pick numwaypoints to use as waypoints
			List<Waypoint> waypoints = new ArrayList<>();
			while(waypoints.size() < numWaypoints) {
				waypoints.add(trackpoints.remove(random.nextInt(trackpoints.size())));
			}
			
			waypoints.sort((a,b)->a.getDateTime().compareTo(b.getDateTime()));
			
			for (int i = 0; i < numWaypoints; i++) {
				Waypoint wp = waypoints.get(i);
				wp.setAttachments(new ArrayList<>());
				wp.setComment(""); //$NON-NLS-1$
				wp.setConservationArea(p.getConservationArea());
				wp.setId(String.valueOf(i+1));
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setObservationGroups(new ArrayList<>());
				session.save(wp);
				
				PatrolWaypoint pw = new PatrolWaypoint();
				pw.setPatrolLegDay(pld);
				pw.setWaypoint(wp);
				pld.getWaypoints().add(pw);
				
				WaypointObservationGroup group = new WaypointObservationGroup();
				group.setWaypoint(wp);
				group.setObservations(new ArrayList<>());
				
				//add observations to the waypoint
				if (!config.getMappings().isEmpty()) {
					int numObs = random.nextInt(config.getObservationsPerWaypointMax() - config.getObservationsPerWaypointMin() + 1) + config.getObservationsPerWaypointMin();
					for (int j = 0; j < numObs; j ++) {
						
						int item = random.nextInt(config.getTotalWeight())+1;
						ObservationConfiguration m = null;
						for (Entry<Range,ObservationConfiguration> mapitem : observationWeightMap.entrySet()) {
							if (mapitem.getKey().contains(item)) {
								m = mapitem.getValue();
								break;
							}
						}
						
						//create an observation that matches the observation mapping
						WaypointObservation wo = new WaypointObservation();
						wo.setObservationGroup(group);
						wo.setAttributes(new ArrayList<>());
						group.getObservations().add(wo);
						wo.setCategory(m.getObservation().getCategory());
						for (Attribute a : m.getAttributes()) {
							a = session.get(Attribute.class,  a.getUuid());
							ObservationConfiguration.Type mappingType = m.getType(a);
							if (mappingType == ObservationConfiguration.Type.EMPTY) continue;
							if (mappingType == ObservationConfiguration.Type.FIXED) {
								WaypointObservationAttribute woa = new WaypointObservationAttribute();
								woa.setAttribute(a);
								
								WaypointObservationAttribute mapping = m.getObservation().findAttribute(a);
								woa.setAttributeListItem(mapping.getAttributeListItem());
								woa.setAttributeTreeNode(mapping.getAttributeTreeNode());
								woa.setNumberValue(mapping.getNumberValue());
								woa.setStringValue(mapping.getStringValue());
								
								if (mapping.getAttributeListItems() != null) {
									woa.setAttributeListItems(new ArrayList<>());
									for (WaypointObservationAttributeList li : mapping.getAttributeListItems()) {
										WaypointObservationAttributeList liob = new WaypointObservationAttributeList();
										liob.setObservationAttribute(woa);
										liob.setAttributeLisItem(li.getAttributeListItem());
										woa.getAttributeListItems().add(liob);
									}
								}
								
								woa.setObservation(wo);
								wo.getAttributes().add(woa);
							}
							if (mappingType == ObservationConfiguration.Type.RANDOM) {
								WaypointObservationAttribute woa = new WaypointObservationAttribute();
								woa.setAttribute(a);
								Object value = generateRandomValue(a);
								if (value != null) {
									woa.setAttributeValue(value);
									woa.setObservation(wo);
									wo.getAttributes().add(woa);
								}
							}
						}
					}
				}
				if (!group.getObservations().isEmpty()) wp.getObservationGroups().add(group);
			}
			
			//move a bit away from this last waypoint for the day to the next waypoint for the day
			for (int i = 0; i < 2; i ++) {
				double percent1 = (random.nextInt(100) + 1) / 100.0;
				double percent2 = (random.nextInt(100) + 1) / 100.0;
				startc = new Coordinate(startc.x + xdir * (maxdegrees * percent1),  startc.y + ydir * (maxdegrees * percent2)  );
			}
		}
		
		return p;
	}
	
	private Coordinate generatePosition() {
		
		if (config.getBboxArea() == null) {
			Envelope env = config.getBboxEnvelope();		
			RandomPointsBuilder builder = new RandomPointsBuilder();
			builder.setExtent(env);
			builder.setNumPoints(1);
			Geometry g = builder.getGeometry();
			return ((Point)((MultiPoint)g).getGeometryN(0)).getCoordinate();
			
		}else if (areas.isEmpty()){
			return new Coordinate(0,0);
		}else {
			//we need to find a point inside the areas
			int v = random.nextInt(areas.size());
			Area a = areas.get(v);
			
			//find a point inside this area
			RandomPointsBuilder builder = new RandomPointsBuilder();
			builder.setExtent(a.getGeometry());
			builder.setNumPoints(1);
			Geometry g = builder.getGeometry();
			return ((Point)((MultiPoint)g).getGeometryN(0)).getCoordinate();
		}
	}
	
	private Object generateRandomValue(Attribute a) throws Exception {
		switch(a.getType()) {
		case BOOLEAN:
			if (random.nextInt(100) < 50) return Boolean.TRUE;
			return Boolean.FALSE;
		case DATE:
			return LocalDate.now().plusDays(random.nextInt());
		case LIST:
			if (a.getActiveListItems().isEmpty()) return null;
			return a.getActiveListItems().get(random.nextInt(a.getActiveListItems().size()));
		case MLIST:
			int number = random.nextInt(a.getActiveListItems().size()+1);
			if (number == a.getActiveListItems().size()) return null;
			
			List<AttributeListItem> options = new ArrayList<>(a.getActiveListItems());
			for (int i = 0; i < number; i ++) {
				options.remove( random.nextInt(options.size()) );
			}
			return options;
			
		case NUMERIC:
			if (a.getMinValue() != null && a.getMaxValue() != null) {
				if (a.getMaxValue() - a.getMinValue() > 5) {
					return Math.round((a.getMaxValue() - a.getMinValue()) * random.nextDouble() + a.getMinValue());
				}else {
					return (a.getMaxValue() - a.getMinValue()) * random.nextDouble() + a.getMinValue();
				}
			}else if (a.getMinValue() != null) {
				return Math.round(random.nextDouble() * 20 + a.getMinValue());
			}else if (a.getMaxValue() != null) {
				return Math.round(random.nextDouble() * a.getMaxValue());
			}else {
				return random.nextInt(30);
			}
		case TEXT:
			//TODO:
			String[] randomStrings = new String[] {"cat","dog","fish","sheep","cow","donkey"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			return randomStrings[random.nextInt(randomStrings.length)];
		case TREE:
			//put all children in a list and pick one of them
			List<AttributeTreeNode> kids = new ArrayList<>();
			ArrayDeque<AttributeTreeNode> processing = new ArrayDeque<>();
			processing.addAll(a.getActiveTreeNodes());
			while(!processing.isEmpty()) {
				AttributeTreeNode n = processing.removeFirst();
				if (n.getActiveChildren().isEmpty()) {
					kids.add(n);
				}else if (n.getActiveChildren() != null) {
					processing.addAll(n.getActiveChildren());
				}
			}
			if (kids.isEmpty()) return null;
			return kids.get(random.nextInt(kids.size()));
		}
		throw new Exception(MessageFormat.format(Messages.DataGenerator_AttributeTypeNotSupported, a.getType().typeKey));
	}
	
	private class Range{
		
		private int minValue;
		private int maxValue;
		
		/**
		 * Both values are inclusive
		 * @param minValue
		 * @param maxValue
		 */
		public Range(int minValue, int maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
		}
		
		public boolean contains(int value) {
			return value >= minValue && value <= maxValue;
		}
	}
}
