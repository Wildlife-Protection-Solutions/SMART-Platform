package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.sql.Time;
import java.text.DateFormat;
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
import org.json.simple.parser.JSONParser;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.dataqueue.cybertracker.IJsonProcessor;
import org.wcs.smart.connect.dataqueue.cybertracker.JsonCtParser;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.export.CyberTrackerConfExporter;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.patrol.export.PatrolJsonUtils;
import org.wcs.smart.cybertracker.patrol.export.PatrolScreensUtil;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
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
		HashMap<UUID, PatrolWrapper> patrols = new HashMap<UUID, PatrolWrapper>();
		
		modifiedPatrols = new HashSet<Patrol>();
		newPatrols = new HashSet<Patrol>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();;
		Waypoint lastWaypoint = null;
		
		for (JSONObject feature : features){
			try{
				JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
				if (sighting == null) continue;
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				
				if (!PatrolScreensUtil.DATATYPE_PATROL.equalsIgnoreCase(type)){
					//not a valid patrol point; skip it
					continue;
				}
				
				boolean isPatrolEnd = sighting.containsKey(PatrolScreensUtil.END_PATROL_KEY) ;
				
				String pid = (String) sighting.get(ScreensUtil.RESULT_ID);
				UUID ctUuid = UuidUtils.stringToUuid(pid);
				CtPatrolLink link = (CtPatrolLink) session.get(CtPatrolLink.class, ctUuid);
				
				if (isPatrolEnd){
					//we want to find the patrol and update the end date
					//add the position to the track, but do not create an observation 
					//for this patrol
					Date dt = JsonUtils.JSON_DATE_FORMAT.parse((String)properties.get(JsonCtParser.DATETIME_KEY));
					
					PatrolLeg toUpdate = null;
					if (link != null){
						toUpdate = link.getPatrolLeg();
					}else{
						Patrol temp = patrols.get(ctUuid).patrol;
						if (temp != null) toUpdate = temp.getFirstLeg();	
					}

					if (toUpdate == null){
						warnings.add("End patrol flag found in observation, but no patrol object could be found.");
					}else{
						//update patrol end date and end time for last patrol leg day
						toUpdate.getPatrol().setEndDate(SharedUtils.getDatePart(dt, false));
						for (PatrolLegDay pd : toUpdate.getPatrolLegDays()){
							if (areDatesEqual(pd.getDate(), toUpdate.getEndDate())){
								pd.setEndTime(new Time(dt.getTime()));
								break;
							}
						}
						//TODO: add waypoint to patrol track
					}
					processedFeatures.add(feature);
					continue;
				}
				
				//create a Waypoint
				JsonCtParser parser = new JsonCtParser();
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
						
				//merge patrol 				
				if (link != null){
					//add these observation to the selected patrol leg
					//TODO: potentially we could validate metadata
					addToExistingLeg(link.getPatrolLeg(), wp, startTime, session);
					modifiedPatrols.add(link.getPatrolLeg().getPatrol());
				}else{
					Patrol p = patrols.get(ctUuid).patrol;
					if (p == null){
						//create a new patrol as we do not have an object for this patrol.
						p = createPatrolFromSighing(sighting, session);
						String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
						patrols.put(ctUuid, new PatrolWrapper(p, deviceId));
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
		
		String defaultValues = (String)sighting.get(PatrolScreensUtil.RESULT_DEFAULT_META_VALUES);
		CyberTrackerPatrol ct = PatrolJsonUtils.parsePatrolMetadata((JSONObject) (new JSONParser()).parse(defaultValues), sighting, session);
		
		
		String startDate = (String)sighting.get(ScreensUtil.RESULT_START_DATE);
		String startTime = (String)sighting.get(ScreensUtil.RESULT_START_TIME);
		
		
		Date dStartDate = DATEFORMAT.parse(startDate);
		p.setEndDate(dStartDate);
		p.setStartDate(dStartDate);
		
		p.setArmed(ct.isArmed());
		p.setComment(ct.getComment());
		p.setMandate(ct.getMandate());
		p.setObjective(ct.getObjective());
		p.setPatrolType(ct.getPatrolType());
		p.setStation(ct.getStation());
		p.setTeam(ct.getTeam());
		
		// create new leg and add members and set transport type
		PatrolLeg pl = p.addLeg();
		pl.setPatrolLegDays(new ArrayList<PatrolLegDay>());
		
		for (Employee member: ct.getMembers()){
			PatrolLegMember item = pl.addPatrolLegMember(member);
			if (member.equals(ct.getPilot())) item.setIsPilot(true);
			if (member.equals(ct.getLeader())) item.setIsLeader(true);
		}
	
		pl.setType(ct.getPatrolTransportType());
		
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

		session.flush();
		session.saveOrUpdate(wp);
		session.flush();
		
		PatrolLegDay addToD = null;
		for (PatrolLegDay pld : addTo.getPatrolLegDays()){
			if (areDatesEqual(pld.getDate(), wp.getDateTime())){
				addToD = pld;
				break;
			}
		}
		
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
	
	public class PatrolWrapper{
		public Patrol patrol;
		public String ctDeviceId;
		
		public PatrolWrapper(Patrol patrol, String ctDeviceId){
			this.patrol =  patrol;
			this.ctDeviceId = ctDeviceId;
		}
	}
}
