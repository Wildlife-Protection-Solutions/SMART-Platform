package org.wcs.smart.patrol.json.connecttest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.DataLink;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.json.PatrolAttributeMetadata;
import org.wcs.smart.patrol.json.PatrolJsonFeatureProcessor;
import org.wcs.smart.patrol.json.PatrolJsonFeatureProcessor.PatrolLinkDataType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.UuidUtils;

@SuppressWarnings("nls")

/**
 * Specifically test multi-day patrol leg 
 * and the start times
 * 
 * @author Emily
 *
 */
public class ConnectJsonPatrolProcessorTest2 {

	private ConservationArea ca;
	private Session session;
	
	private UUID patrolUuid = UUID.fromString("8da25003-dada-47a0-b42e-65fe01a2fd2e");
	private UUID legUuid = UUID.fromString("60df95b3-717f-4995-abb6-0583029f7a00");
	
	private Team team1;
	private Station station1;
	private String objective1;
	private String comment1;
	private boolean isArmed1 = false;
	private PatrolMandate mandate1;
	
	private PatrolTransportType typenopilot;
	
	private Employee employee1;
	private Employee employee2;
	
	private String patrolId = "Test Json Patrol Id - MultiDate";
	
	public ConnectJsonPatrolProcessorTest2(ConservationArea ca, Session session) {
		this.ca = ca;
		this.session = session;
	}
	
	public void test() throws Exception{
		//clean up
		try {
			session.beginTransaction();
			
			DataLink link = session.createQuery("FROM DataLink WHERE conservationArea = :ca and providerId = :puuid and dataType = :datatype", DataLink.class) //$NON-NLS-1$
				.setParameter("ca",ca) //$NON-NLS-1$
				.setParameter("puuid", patrolUuid) //$NON-NLS-1$
				.setParameter("datatype", PatrolLinkDataType.PATROL.getKey()) //$NON-NLS-1$
				.uniqueResult();
					
			if (link != null) {
				session.delete(link);
				Patrol p = session.get(Patrol.class, link.getSmartId());
				if (p != null) {
					PatrolManager.getInstance().deletePatrol(p.getUuid(), true, new NullProgressMonitor());
				}
			}
			
		
			session.getTransaction().commit();
		}catch(Exception ex) {
			session.getTransaction().rollback();
			throw ex;
		}
		
			
		List<Team> teams = session.createQuery("FROM Team WHERE conservationArea = :ca", Team.class)
				.setParameter("ca",ca)
				.list();
		if (teams.size() > 1) {
			team1 = teams.get(0);
		}else {
			throw new Exception("You need to configure at least one team.");
		}
		
		List<PatrolMandate> mandates = session.createQuery("FROM PatrolMandate WHERE conservationArea = :ca", PatrolMandate.class)
				.setParameter("ca",ca)
				.list();
		if (mandates.size() > 1) {
			mandate1 = mandates.get(0);
		}else {
			throw new Exception("You need to configure at least one mandate");
		}
		
		List<Station> stations = session.createQuery("FROM Station WHERE conservationArea = :ca", Station.class)
				.setParameter("ca",ca)
				.list();
		if (stations.size() > 1) {
			station1 = stations.get(0);
		}else {
			throw new Exception("You need to configure at least one station");
		}
		
		objective1 = "This if the first objective";
		
		comment1 = "This is the first comment";
		
		isArmed1 = true;
		
		List<PatrolTransportType> types = session.createQuery("FROM PatrolTransportType WHERE conservationArea = :ca", PatrolTransportType.class)
				.setParameter("ca",ca)
				.list();
		for (PatrolTransportType type : types) {
			if (typenopilot == null) {
				typenopilot = type;
			}
		}
		
		if (typenopilot == null ) throw new Exception("You need to configure at least 1 transport type that doesn't require a pilot ");
		
		List<Employee> ees = session.createQuery("FROM Employee WHERE conservationArea = :ca", Employee.class)
				.setParameter("ca",ca)
				.list();
		if (ees.size() > 5) {
			employee1 = ees.get(0);
			employee2 = ees.get(1);
		}else {
			throw new Exception("You need to configure at least five employees");
		}
		
		try {
			testCreatePatrol();
			
			testCreateWaypointDay1();
			testCreateWaypointDay2();
			
			testEndPatrolLeg();
		}finally {
			PatrolEventManager.getInstance().patrolAdded(new Patrol());
		}
		System.out.println("test complete");
	}
	
	
	@SuppressWarnings("unchecked")
	private void testCreatePatrol() throws Exception{
		
		JSONObject patrol = createBaseObject("patrol/new");
		JSONObject properties = (JSONObject) patrol.get("properties");
		JSONObject attributes = (JSONObject) properties.get("smartAttributes");
		
		attributes.put("patrolUuid", UuidUtils.uuidToString(patrolUuid));
		attributes.put("patrolLegUuid", UuidUtils.uuidToString(legUuid));
		
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.TEAM.getKey(), team1.getKeyId());
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.ARMED.getKey(), isArmed1);
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.COMMENT.getKey(), comment1);
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.OBJECTIVE.getKey(), objective1);
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.PATROLID.getKey(), patrolId);
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.STATION.getKey(), UuidUtils.uuidToString(station1.getUuid()));
		
		JSONArray members = new JSONArray();
		members.add(UuidUtils.uuidToString(employee1.getUuid()));
		members.add(UuidUtils.uuidToString(employee2.getUuid()));
		
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.EMPLOYEES.getKey(), members);
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.LEADER.getKey(), UuidUtils.uuidToString(employee1.getUuid()));
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.MANDATE.getKey(), mandate1.getKeyId());
		attributes.put(PatrolAttributeMetadata.FixedPatrolMetadata.TRANSPORT_TYPE.getKey(), typenopilot.getKeyId());
		
		writeToFile(patrol, "test1.json");
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	private JSONObject createBaseObject(String featuretype) {
		JSONObject patrol = new JSONObject();
		patrol.put("type",  "Feature");
		
		JSONObject geometry = new JSONObject();
		geometry.put("type", "Point");
		JSONArray coords = new JSONArray();
		coords.add(-123.1);
		coords.add(48.1);
		geometry.put("coordinates", coords);
		
		patrol.put("geometry",  geometry);
		
		
		JSONObject properties = new JSONObject();
		patrol.put("properties",  properties);
		
		properties.put("dateTime", "2021-11-26T15:14:13");
		properties.put("smartDataType", "patrol");
		properties.put("smartFeatureType", featuretype);
		
		JSONObject attributes = new JSONObject();
		properties.put("smartAttributes", attributes);
		
		
		return patrol;
	}
	
	
	
	@SuppressWarnings("unchecked")
	private void testEndPatrolLeg() throws Exception{
		JSONObject patrol = new JSONObject();
		patrol.put("type",  "Feature");
		
		JSONObject geometry = new JSONObject();
		geometry.put("type", "Point");
		JSONArray coords = new JSONArray();
		coords.add(-123.2);
		coords.add(48.5);
		geometry.put("coordinates", coords);
		
		patrol.put("geometry",  geometry);
		
		JSONObject properties = new JSONObject();
		patrol.put("properties",  properties);
		
		properties.put("dateTime", "2021-11-27T18:14:13");
		properties.put("smartDataType", "patrol");
		properties.put("smartFeatureType", "leg/end");
		
		JSONObject attributes = new JSONObject();
		properties.put("smartAttributes", attributes);;
		
		attributes.put("patrolLegUuid", UuidUtils.uuidToString(legUuid));
		
		writeToFile(patrol, "test4.json");
	}
	
	
	@SuppressWarnings("unchecked")
	private void testCreateWaypointDay1() throws Exception {
		JSONObject patrol = new JSONObject();
		patrol.put("type",  "Feature");
		
		JSONObject geometry = new JSONObject();
		geometry.put("type", "Point");
		JSONArray coords = new JSONArray();
		coords.add(-123.2);
		coords.add(48.5);
		geometry.put("coordinates", coords);
		
		patrol.put("geometry",  geometry);
		
		JSONObject properties = new JSONObject();
		patrol.put("properties",  properties);
		
		properties.put("dateTime", "2021-11-26T20:00:00");
		properties.put("smartDataType", "patrol");
		properties.put("smartFeatureType", "waypoint/new");
		
		JSONObject attributes = new JSONObject();
		properties.put("smartAttributes", attributes);
		
		attributes.put("patrolLegUuid", UuidUtils.uuidToString(legUuid));
		
		writeToFile(patrol, "test2.json");
		
	}
	

	@SuppressWarnings("unchecked")
	private void testCreateWaypointDay2() throws Exception {
		JSONObject patrol = new JSONObject();
		patrol.put("type",  "Feature");
		
		JSONObject geometry = new JSONObject();
		geometry.put("type", "Point");
		JSONArray coords = new JSONArray();
		coords.add(-123.3);
		coords.add(48.4);
		geometry.put("coordinates", coords);
		
		patrol.put("geometry",  geometry);
		
		JSONObject properties = new JSONObject();
		patrol.put("properties",  properties);
		
		properties.put("dateTime", "2021-11-27T08:20:20");
		properties.put("smartDataType", "patrol");
		properties.put("smartFeatureType", "waypoint/new");
		
		JSONObject attributes = new JSONObject();
		properties.put("smartAttributes", attributes);
		
		attributes.put("patrolLegUuid", UuidUtils.uuidToString(legUuid));
		
		writeToFile(patrol, "test3.json");
		
	}
	

	
	private void writeToFile(JSONObject object, String filename) throws IOException {
		Path file = Paths.get("C:\\temp\\patrol_" + filename );
		Files.writeString(file, object.toJSONString());
	}
}
