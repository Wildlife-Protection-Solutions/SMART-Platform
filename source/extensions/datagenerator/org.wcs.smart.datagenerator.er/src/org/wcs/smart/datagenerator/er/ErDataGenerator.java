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
package org.wcs.smart.datagenerator.er;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

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
import org.locationtech.jts.linearref.LengthLocationMap;
import org.locationtech.jts.shape.random.RandomPointsBuilder;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.GeometryAttributeValue;
import org.wcs.smart.ca.datamodel.Attribute.GeometrySource;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.datagenerator.er.internal.Messages;
import org.wcs.smart.datagenerator.er.model.ObservationConfiguration;
import org.wcs.smart.datagenerator.er.model.SurveyConfiguration;
import org.wcs.smart.er.MissionIdGenerator;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.er.model.MissionProperty;
import org.wcs.smart.er.model.MissionPropertyValue;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.common.importwp.ObservationGPSDataImport;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.observation.model.WaypointObservationGroup;

/**
 * Generates sample survey data using information provided by the configuration.
 * 
 * @author Emily
 *
 */
public class ErDataGenerator implements IDataEngine{
	
	private SurveyConfiguration config;
	
	private List<Employee> employees;
	private List<MissionProperty> properties;
	private List<Survey> currentSurveys;
	
	private List<Area> areas;
	
	private HashMap<Range, ObservationConfiguration> observationWeightMap;
	
	private SurveyDesign design;
	private List<SamplingUnit> samplingUnits;
	
	private Random random = new Random();
	private List<Survey> newSurveys;
	
	public ErDataGenerator(SurveyConfiguration config) {
		this.config = config;
	}
	
	public void run(IProgressMonitor monitor) throws Exception {
		observationWeightMap = new HashMap<>();
		
		newSurveys = new ArrayList<>();
		
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.DataGenerator_TaskName, config.getNumberOfSurveys() + 1);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				
				design = QueryFactory.buildQuery(session,  SurveyDesign.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"keyId", config.getSurveyDesignKey()}).uniqueResult(); //$NON-NLS-1$
				if (design == null) {
					throw new Exception(MessageFormat.format(Messages.ErDataGenerator_SurveyDesignNotFound, config.getSurveyDesignKey()));
				}
				
				samplingUnits = QueryFactory.buildQuery(session, SamplingUnit.class, 
						new Object[] {"surveyDesign", design}).list(); //$NON-NLS-1$
				
				//setup weighting based on configurable model
				Set<Category> items = null;
				if (design.getConfigurableModel() != null) {
					items = new HashSet<>();
					
					List<CmNode> nodes = new ArrayList<>(design.getConfigurableModel().getNodes());
					while(!nodes.isEmpty()) {
						CmNode n = nodes.remove(0);
						if (n.getCategory() != null) {
							items.add(n.getCategory());
						}else {
							nodes.addAll(n.getChildren());
						}
					}
				}
				//only include categories in the configurable model
				int current = 1;
				for (ObservationConfiguration m : config.getMappings()) {
					if (items == null || items.contains( m.getObservation().getCategory())) {
						Range r = new Range(current, current+m.getWeight()-1 );
						observationWeightMap.put(r, m);
						current += m.getWeight();
					}
				}
				
				
				loadMissionMetadata(session, progress.split(1));
				
				
				for (int i = 0; i < config.getNumberOfSurveys(); i ++) {
					Survey survey  = generateSurvey(session, progress.split(1));
					newSurveys.add(survey);
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
		
		SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_ADDED, newSurveys);
	}
	
	
	private void loadMissionMetadata(Session session, IProgressMonitor monitor) {
		//load employees
		employees = QueryFactory.buildQuery(session, Employee.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"endEmploymentDate", null} //$NON-NLS-1$
				).list();

		properties = QueryFactory.buildQuery(session, MissionProperty.class,
				new Object[] {"id.surveyDesign", design}).list(); //$NON-NLS-1$
		
		currentSurveys = QueryFactory.buildQuery(session, Survey.class, 
				new Object[] {"surveyDesign.conservationArea", SmartDB.getCurrentConservationArea()} //$NON-NLS-1$
				).list();
		
		if (config.getBboxArea() != null) {
			areas = QueryFactory.buildQuery(session, Area.class,
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"type", config.getBboxArea()}).list(); //$NON-NLS-1$
		}
	}
	
	private String getSurveyId() {

		LocalDate now = LocalDate.now();
		String id = now.getYear() + "" + now.getMonthValue() + "" + now.getDayOfMonth() + "-"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		int cnt = 0;
		for (Survey s : currentSurveys) {
			if (s.getId().startsWith(id)) cnt ++;
		}
		for (Survey s : newSurveys) {
			if (s.getId().startsWith(id)) cnt ++;
		}
		
		return id + "" + cnt; //$NON-NLS-1$
	}

	private Survey generateSurvey(Session session, IProgressMonitor monitor) throws Exception {
		
		SubMonitor sub = SubMonitor.convert(monitor);
		sub.beginTask(Messages.ErDataGenerator_Progress1, config.getNumberOfMissionsPerSurvey());
		
		Survey survey = new Survey();
		survey.setSurveyDesign(design);
		survey.setId(getSurveyId());
		survey.setMissions(new ArrayList<>());
		
		session.persist(survey);
		
		for (int i = 0; i < config.getNumberOfMissionsPerSurvey(); i ++) {
			sub.subTask(MessageFormat.format(Messages.ErDataGenerator_Progress2, i , config.getNumberOfMissionsPerSurvey(), survey.getId()));
			sub.split(1);
			Mission mission = createMission(session, monitor);
			survey.getMissions().add(mission);
			mission.setSurvey(survey);
			mission.setId(MissionIdGenerator.INSTANCE.generateMissionId(mission, session));
			session.persist(mission);
			session.flush();
			mission.getMissionDays().forEach(m->{
				m.getWaypoints().forEach(wp->{
					session.persist(wp.getWaypoint());
					session.persist(wp);
				});
			});
		}
		
		return survey;
		
	}
	private Mission createMission(Session session, IProgressMonitor monitor) throws Exception {
		
		//start date must be between config.getStartDate and config.getEndDate
		int offset = (int) ChronoUnit.DAYS.between(config.getStartDate(), config.getEndDate());
		if (offset != 0) {
			offset = random.nextInt(offset+1);
		}
				
		LocalDate start = config.getStartDate().plusDays(offset);
		int length = random.nextInt(config.getDaysPerMissionMax() - config.getDaysPerMissionMin() + 1) + config.getDaysPerMissionMin();
		LocalDate end = start.plusDays(length-1);
		
		Mission mission = new Mission();
		mission.setComment(null);
		mission.setStartDate(start);
		mission.setEndDate(end);
		mission.setMissionDays(new ArrayList<>());
		mission.setMembers(new ArrayList<MissionMember>());
		mission.setMissionPropertyValues(new ArrayList<MissionPropertyValue>());
		//properties
		for (MissionProperty p : properties) {
			if (random.nextInt(100) > 50) continue;
			
			
			MissionPropertyValue property = new MissionPropertyValue();
			property.setMission(mission);
			property.setMissionAttribute(p.getAttribute());
			
			switch(p.getAttribute().getType()) {
			case LIST:
				int index = random.nextInt(p.getAttribute().getAttributeList().size() - 1);
				property.setAttributeListItem(p.getAttribute().getAttributeList().get(index));
				break;
			case NUMERIC:
				property.setNumberValue((double)random.nextInt(5000));
				break;
			case TEXT:
				property.setStringValue(MessageFormat.format(Messages.ErDataGenerator_TextAttributeValue, p.getAttribute().getName()));
				break;
			
			case MLIST:
			case BOOLEAN:
			case DATE:
			case TREE:
			default:
				continue;
			}
			
			mission.getMissionPropertyValues().add(property);
		}
		
		int employeeCnt = random.nextInt(config.getEmployeesPerMissionMax() - config.getEmployeesPerMissionMin() +1) + config.getEmployeesPerMissionMin();
		if (employeeCnt > employees.size()) {
			employeeCnt = employees.size();
		}
		List<Employee> tosearch = new ArrayList<>(employees);
		while(mission.getMembers().size() < employeeCnt){
			if (tosearch.size() == 0) break;
			Employee next = tosearch.get(0);
			if (tosearch.size() > 1) {
				next = tosearch.remove(random.nextInt(tosearch.size()));
			}else {
				next = tosearch.remove(0);
			}
			MissionMember member = new MissionMember();
			member.setMember(next);
			member.setMission(mission);
			mission.getMembers().add(member);
		}		
		mission.getMembers().get(0).setIsLeader(true);
		
		//generate a start position for the mission
		Coordinate startc = generatePosition();
		
		for (int day = 0; day < length; day ++) {

			MissionDay missionDay = new MissionDay();
			
			missionDay.setMission(mission);
			mission.getMissionDays().add(missionDay);
			
			LocalDate date = start.plusDays(day);
			missionDay.setDate(  date );
			missionDay.setRestMinutes(random.nextInt(121));
			
			//start sometime between 1am and 11am
			int startHour = random.nextInt(11) + 1;
			//end sometime between 1pm and 11pm
			int endHour = random.nextInt(11) + 13;
			
			LocalTime startt = LocalTime.of(startHour, random.nextInt(60), random.nextInt(60));
			LocalTime endt = LocalTime.of(endHour, random.nextInt(60), random.nextInt(60));
					
			missionDay.setStartTime(startt);
			missionDay.setEndTime(endt);

			missionDay.setWaypoints(new ArrayList<SurveyWaypoint>());
			
			List<Waypoint> trackpoints = new ArrayList<>();
			int numWaypoints = random.nextInt(config.getWaypointsPerDayMax() - config.getWaypointsPerDayMin() + 1) + config.getWaypointsPerDayMin();
			int numTrackPoints = numWaypoints * (random.nextInt(6) + 1) ;
			if (numTrackPoints < 2) numTrackPoints = 2;
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
			
			
			SamplingUnit unit = null;
			Geometry unitGeom = null;
			if (samplingUnits.size() > 0) {
				unit = samplingUnits.get(random.nextInt(samplingUnits.size()));
				
				unitGeom = unit.getGeometry();
				Coordinate c = null;
				if (unitGeom instanceof Point) {
					c = ((Point)unitGeom).getCoordinate();
				}else if (unitGeom instanceof LineString) {
					c = ((LineString)unitGeom).getCoordinateN(0);
				}else {
					unit = null;
					unitGeom = null;
				}
				
				if (c != null) {
					double percent1 = (random.nextInt(100) + 1) / 100.0;
					double percent2 = (random.nextInt(100) + 1) / 100.0;					
					startc = new Coordinate(c.x + xdir * (maxdegrees * percent1),  c.y + ydir * (maxdegrees * percent2)  );
				}
			}
			
			
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
				if (unitGeom instanceof Point) {
					startc = ((Point)unitGeom).getCoordinate();
					startc = new Coordinate(startc.x + xdir * (maxdegrees * percent1),  startc.y + ydir * (maxdegrees * percent2)  );
				}else if (unitGeom instanceof LineString) {
					startc = LengthLocationMap.getLocation((LineString)unitGeom, (unitGeom.getLength() / numTrackPoints) * i).getCoordinate((LineString)unitGeom);
					startc = new Coordinate(startc.x + xdir * (maxdegrees * percent1),  startc.y + ydir * (maxdegrees * percent2)  );
				}else {
					startc = new Coordinate(startc.x + xdir * (maxdegrees * percent1),  startc.y + ydir * (maxdegrees * percent2)  );
				}
			}
			
			LineString track = ObservationGPSDataImport.convertToLineString(trackpoints);
			MissionTrack t = new MissionTrack();
			t.setId(Messages.ErDataGenerator_TrackId);
			t.setLineString(track);
			missionDay.setTracks(Collections.singletonList(t));
			t.setMissionDay(missionDay);
			
			//randomly pick numwaypoints to use as waypoints
			List<Waypoint> waypoints = new ArrayList<>();
			while(waypoints.size() < numWaypoints) {
				waypoints.add(trackpoints.remove(random.nextInt(trackpoints.size())));
			}
			
			waypoints.sort((a,b)->a.getDateTime().compareTo(b.getDateTime()));
			
		
			
			for (int i = 0; i < numWaypoints; i++) {
				Employee observer = null;
				
				Waypoint wp = waypoints.get(i);
				wp.setAttachments(new ArrayList<>());
				wp.setComment(""); //$NON-NLS-1$
				wp.setConservationArea(design.getConservationArea());
				wp.setId(String.valueOf(i+1));
				wp.setSourceId(SurveyWaypointSource.KEY);
				wp.setObservationGroups(new ArrayList<>());
				if (design.getTrackDistanceDirection()) {
					wp.setDirection((float)( random.nextInt(36000) / 100.0 ) );
					wp.setDistance( (float)(random.nextDouble() * 5000.0));
				}
				if (design.getTrackObserver()) {
					observer = mission.getMembers().get(random.nextInt(mission.getMembers().size())).getMember();
				}
				//session.merge(wp);
				
				SurveyWaypoint pw = new SurveyWaypoint();
				pw.setSamplingUnit(unit);
				pw.setMissionDay(missionDay);
				pw.setWaypoint(wp);
				missionDay.getWaypoints().add(pw);
				
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
						if (m == null) continue;
						
						//create an observation that matches the observation mapping
						WaypointObservation wo = new WaypointObservation();
						wo.setObserver(observer);
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
								woa.setNumberValue2(mapping.getNumberValue2());
								woa.setGeom(mapping.getGeom());
								
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
		
		return mission;
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
		case LINE:
			int x = random.nextInt(15);
			if (x < 2) x = 2;
			Coordinate[] cs = new Coordinate[x];
			for (int i = 0; i < cs.length; i ++) {
				cs[i] = generatePosition();
			}
			Geometry g = GeometryFactoryProvider.getFactory().createLineString(cs);
			GeometrySource src = GeometrySource.values()[random.nextInt(GeometrySource.values().length)];
			return new GeometryAttributeValue(g, src);
			
		case POLYGON:
			cs = new Coordinate[4];
			for (int i = 0; i < cs.length-1; i ++) {
				cs[i] = generatePosition();
			}
			cs[3] = cs[0];
			g = GeometryFactoryProvider.getFactory().createPolygon(cs);
			src = GeometrySource.values()[random.nextInt(GeometrySource.values().length)];
			return new GeometryAttributeValue(g, src);
		
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
