package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.sql.Time;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Station;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.dataqueue.cybertracker.IJsonProcessor;
import org.wcs.smart.connect.dataqueue.cybertracker.JsonParser;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter.JsonKey;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil.JsonPatrolKey;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolType.Type;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

public class PatrolJsonProcessor implements IJsonProcessor {
	
	private static final DateFormat DATEFORMAT = new SimpleDateFormat("yyyy/MM/dd");
	private static final DateFormat TIMEFORMAT = new SimpleDateFormat("HH:mm:ss");
	
	private List<String> warnings;
	
	private Set<Patrol> newPatrols;
	private Set<Patrol> modifiedPatrols;
	
	public PatrolJsonProcessor() {
		warnings = new ArrayList<String>();
	}

	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		HashMap<UUID, Patrol> patrols = new HashMap<UUID, Patrol>();
		modifiedPatrols = new HashSet<Patrol>();
		newPatrols = new HashSet<Patrol>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		Waypoint lastWaypoint = null;
		
		for (JSONObject feature : features){
			try{
				JSONObject properties = (JSONObject) feature.get(JsonParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonParser.SIGHTINGS_KEY);
				if (sighting == null) continue;
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				
				if (!PatrolScreensUtil.DATATYPE_PATROL.equalsIgnoreCase(type)) continue;
				
				JsonParser parser = new JsonParser();
				Waypoint wp = parser.createWaypoint(feature, session);
				warnings.addAll(parser.getWarnings());
				
				wp.setSourceId(PatrolWaypointSource.PATROL_WP_SOURCE_ID);
				wp.setConservationArea(SmartDB.getCurrentConservationArea());
				
				boolean isNewWaypoint = true;
				if (wp.getX() == null && wp.getY() == null){
					//TODO: add to last waypoint
					if (lastWaypoint == null){
						//TODO: we have a problem
						System.out.println("no last waypoint to add to");
					}else{
						//merge observations into a single waypoint
						for (WaypointObservation wo : wp.getObservations()){
							wo.setWaypoint(lastWaypoint);
							lastWaypoint.getObservations().add(wo);
						}
						wp = lastWaypoint;
						isNewWaypoint = false;
					}
				}
				lastWaypoint = wp;
				
				if (!isNewWaypoint){
					session.saveOrUpdate(lastWaypoint);
					session.flush();
					processedFeatures.add(feature);
					continue;
				}
				
				String startDate = (String)sighting.get(ScreensUtil.RESULT_START_DATE);
				String startTime = (String)sighting.get(ScreensUtil.RESULT_START_TIME);
				Date dStartDate = DATEFORMAT.parse(startDate);
						
				String pid = (String) sighting.get(ScreensUtil.RESULT_ID);
				//merge patrol 
				UUID ctUuid = UuidUtils.stringToUuid(pid);
				CtPatrolLink link = (CtPatrolLink) session.get(CtPatrolLink.class, ctUuid);
				
				if (link != null){
					//add these observation to the selected patrol leg
					//TODO: potentially we could validate metadata
					addToExistingLeg(link.getPatrolLeg(), wp, startTime, session);
					modifiedPatrols.add(link.getPatrolLeg().getPatrol());
				}else{
					Patrol p = patrols.get(ctUuid);
					if (p == null){
						//create a new patrol as we do not have an object for this patrol.
						p = createPatrolFromSighing(sighting, session);
						patrols.put(ctUuid, p);
						newPatrols.add(p);
					}
					
					//find patrol to add to 
					PatrolLeg addTo = p.getFirstLeg();
					PatrolLegDay addToD = null;
					for (PatrolLegDay pld : addTo.getPatrolLegDays()){
						if (areDatesEqual(pld.getDate(), dStartDate)){
							addToD = pld;
							break;
						}
					}
					if (addToD == null){
						//need to create a new patrol leg day for this day
						addToD = createPatrolLegDay(addTo, dStartDate, startTime);
					}
					PatrolWaypoint pw = new PatrolWaypoint();
					pw.setPatrolLegDay(addToD);
					pw.setWaypoint(wp);
					addToD.getWaypoints().add(pw);
				}
				processedFeatures.add(feature);
				
			}catch (Exception ex){
				//TODO: if there is a session.flush error we have a problem we need to stop and rollback
				CyberTrackerPlugIn.log(ex.getMessage(), ex);
				warnings.add("Error parsing feature information: (feature will not be processed)" + ex.getMessage());
			}
		}
		
		if (!warnings.isEmpty()){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), "Error", "The following warnings were generated while parsing patrol data.", warnings);
					wd.open();
				}	
			});
		}
		
		final boolean[] cancel = new boolean[]{false};
		if (!patrols.isEmpty()){
			//we need to ask the user if they want to create a new patrol or add to an existing patrol
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					PatrolDialog pd = new PatrolDialog(Display.getDefault().getActiveShell(), patrols, session);
					if (pd.open() == Window.CANCEL){
						cancel[0] = true;
					}
				}	
			});
		}
		if (cancel[0]){
			throw new Exception("User cancelled.");
			//TODO make sure to rollback transaction
		}
		return processedFeatures;
	}
	
	
	private Patrol createPatrolFromSighing(JSONObject sighting, Session session) throws Exception{
		Patrol p = new Patrol();
		String ptype = (String)sighting.get(PatrolScreensUtil.RESULT_PATROL_TYPE);
		String ptransport = (String)sighting.get(PatrolScreensUtil.RESULT_TRANSPORT);
		
		String armed = (String)sighting.get(PatrolScreensUtil.RESULT_ARMED);
		String team = (String)sighting.get(PatrolScreensUtil.RESULT_TEAM);
		String station = (String)sighting.get(PatrolScreensUtil.RESULT_STATION);
		String mandate = (String)sighting.get(PatrolScreensUtil.RESULT_MANDATE);
		String objective = (String)sighting.get(PatrolScreensUtil.RESULT_OBJECTIVE);
		String comment = (String)sighting.get(PatrolScreensUtil.RESULT_COMMENTS);
		String leader = (String)sighting.get(PatrolScreensUtil.RESULT_LEADER);
		String pilot = (String)sighting.get(PatrolScreensUtil.RESULT_PILOT);
		String startDate = (String)sighting.get(ScreensUtil.RESULT_START_DATE);
		String startTime = (String)sighting.get(ScreensUtil.RESULT_START_TIME);
		
		List<String> members = new ArrayList<String>();
		for (Object x : sighting.keySet()){
			String key = (String)x;
			if (startsWith(key, JsonKey.EMPLOYEE.key)) members.add(key);
		}
	
		if (armed != null){
			p.setArmed(Boolean.valueOf(armed));
		}
		
		if (team != null && startsWith(team, JsonPatrolKey.TEAM.key)){
			UUID uuid = UuidUtils.stringToUuid(team.substring(JsonPatrolKey.TEAM.key.length() + 1));
			Team teamObj = (Team) session.get(Team.class, uuid);
			if (teamObj == null){
				//TODO: log a warning or error or something
			}
			p.setTeam(teamObj);
		}
	
		if (station != null && startsWith(station, JsonPatrolKey.STATION.key)){
			UUID uuid = UuidUtils.stringToUuid(station.substring(JsonPatrolKey.STATION.key.length() + 1));
			Station stationObj = (Station) session.get(Station.class, uuid);
			if (stationObj == null){
				//TODO: log a warning or error or something
			}
			p.setStation(stationObj);
		}
		
		if (mandate != null && startsWith(mandate, JsonPatrolKey.MANDATE.key)){
			UUID uuid = UuidUtils.stringToUuid(mandate.substring(JsonPatrolKey.MANDATE.key.length() + 1));
			PatrolMandate mandateObj = (PatrolMandate) session.get(PatrolMandate.class, uuid);
			if (mandateObj == null){
				//TODO: log a warning or error or something
			}
			p.setMandate(mandateObj);
		}
		if (ptype != null){
			p.setPatrolType(Type.valueOf(ptype));
		}
		p.setObjective(objective);
		p.setComment(comment);
		
		Date dStartDate = DATEFORMAT.parse(startDate);
		p.setEndDate(dStartDate);
		p.setStartDate(dStartDate);
		
		// create new leg and add members and set transport type
		PatrolLeg pl = p.addLeg();
		pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		for (String member: members){
			UUID uuid = UuidUtils.stringToUuid(member.substring(JsonKey.EMPLOYEE.key.length() + 1));
			Employee employee = (Employee) session.get(Employee.class, uuid);
			PatrolLegMember item = pl.addPatrolLegMember(employee);
			
			if (member.equals(leader)) item.setIsLeader(true);
			if (member.equals(pilot)) item.setIsPilot(true);
		}
	
		if (ptransport != null && startsWith(ptransport, JsonPatrolKey.TRANSPORT_TYPE.key)){
			UUID uuid = UuidUtils.stringToUuid(ptransport.substring(JsonPatrolKey.TRANSPORT_TYPE.key.length() + 1));
			PatrolTransportType transportObj = (PatrolTransportType) session.get(PatrolTransportType.class, uuid);
			if (transportObj == null){
				throw new Exception(MessageFormat.format("Could not find patrol transport type for uuid {0}.", uuid));
			}
			pl.setType(transportObj);
			p.setPatrolType(transportObj.getPatrolType());
		}
		return p;
	}
	
	
	private boolean areDatesEqual(Date date1, Date date2){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date1);
		
		
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		
		int[] fields = new int[]{Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH};
		for (int field : fields){
			if (cal.get(field) != cal2.get(field)) return false;
		}
		return true;
	}
	

	private PatrolLegDay createPatrolLegDay(PatrolLeg addTo, Date date, String startTime) throws ParseException{
		PatrolLegDay addToD = new PatrolLegDay();
		addToD.setDate(SharedUtils.getDatePart(date, false));
		addToD.setEndTime(new Time(SmartUtils.getMidnight().getTime() - 1 ));
		addToD.setPatrolLeg(addTo);
		addTo.getPatrolLegDays().add(addToD);
		addToD.setRestMinutes(0);
		addToD.setStartTime( new Time( TIMEFORMAT.parse(startTime).getTime())  );
		addToD.setWaypoints(new ArrayList<PatrolWaypoint>());
		
		if (addTo.getStartDate().after(addToD.getDate())){
			addTo.setStartDate(addToD.getDate());
		}
		if (addTo.getEndDate().before(addToD.getDate())){
			addTo.setEndDate(addToD.getDate());
		}
		if (addTo.getPatrol().getStartDate().after(addToD.getDate())){
			addTo.getPatrol().setStartDate(addToD.getDate());
		}
		if (addTo.getPatrol().getEndDate().before(addToD.getDate())){
			addTo.getPatrol().setEndDate(addToD.getDate());
		}
		return addToD;
	}
	
	private void addToExistingLeg(PatrolLeg addTo, Waypoint wp, String startTime, Session session)
			throws ParseException {

		PatrolLegDay addToD = null;
		for (PatrolLegDay pld : addTo.getPatrolLegDays()){
			if (areDatesEqual(pld.getDate(), wp.getDateTime())){
				addToD = pld;
				break;
			}
		}
		
		session.saveOrUpdate(wp);
		session.flush();
		
		if (addToD == null){
			//need to create a new patrol leg day for this day
			//also need to check patrol dates
			addToD = createPatrolLegDay(addTo, wp.getDateTime(), startTime);
		}
		
		PatrolWaypoint pw = new PatrolWaypoint();
		pw.setPatrolLegDay(addToD);
		pw.setWaypoint(wp);
		session.save(pw);
		addToD.getWaypoints().add(pw);

		session.saveOrUpdate(addTo.getPatrol());
	}

	private boolean startsWith(String value, String key){
		return value.startsWith(key + CyberTrackerConfExporter.KEY_SEP);
	}
	
	@Override
	public void afterSave(){
		for (Patrol p : modifiedPatrols){
			try{
				PatrolEventManager.getInstance().patrolSaved(p, true);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Importing JSON Data", ex.getMessage(), ex);
			}
		}
		for (Patrol p : newPatrols){
			try{
				PatrolEventManager.getInstance().patrolAdded(p);
			}catch (Exception ex){
				CyberTrackerPlugIn.displayError("Importing JSON Data", ex.getMessage(), ex);
			}
		}
	}
}
