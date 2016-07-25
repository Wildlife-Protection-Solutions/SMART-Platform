package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.wcs.smart.connect.dataqueue.cybertracker.JsonTrackUtils;
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
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

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
			JsonCtParser parser = new JsonCtParser();
			
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
						PatrolLegDay pd = findLegDay(toUpdate, toUpdate.getEndDate(), true);
						pd.setEndTime(new Time(dt.getTime()));
						
						//add point to track
						addPointToTrack(pd, parser.readXYFromProperties(feature),dt);
						
					}
					processedFeatures.add(feature);
					continue;
				}
				
				//create a Waypoint
				
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
				PatrolLeg addTo = null;
				if (link != null){
					//add these observation to the selected patrol leg
					//TODO: potentially we could validate metadata
					addToExistingLeg(link.getPatrolLeg(), wp, session);
					modifiedPatrols.add(link.getPatrolLeg().getPatrol());
					addTo = link.getPatrolLeg();

				}else{
					PatrolWrapper pwrapper = patrols.get(ctUuid);
					
					if (pwrapper == null){
						//create a new patrol as we do not have an object for this patrol.
						Patrol p = createPatrolFromSighing(sighting, session);
						String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
						pwrapper = new PatrolWrapper(p, deviceId);
						patrols.put(ctUuid, pwrapper);
						newPatrols.add(p);
					}
					
					Patrol p = pwrapper.patrol;
					//find patrol to add to 
					addTo = p.getFirstLeg();
					PatrolLegDay addToD = findLegDay(addTo, dStartDate, true);
					
					PatrolWaypoint pw = new PatrolWaypoint();
					pw.setPatrolLegDay(addToD);
					pw.setWaypoint(wp);
					addToD.getWaypoints().add(pw);
				}
				
				//add position to track log
				addPointToTrack(addTo, new Coordinate(wp.getX(), wp.getY()), wp.getDateTime());
				
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
		
		//try processing track features
		PatrolJsonTrackProcessor trackProcessor = new PatrolJsonTrackProcessor();
		processedFeatures.addAll(trackProcessor.processJson(features, session));
		
		modifiedPatrols.addAll(trackProcessor.getModifiedPatrols());
		
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
		
	private void addToExistingLeg(PatrolLeg addTo, Waypoint wp, Session session)
			throws ParseException {

		session.flush();
		session.saveOrUpdate(wp);
		session.flush();
		
		PatrolLegDay addToD = findLegDay(addTo, wp.getDateTime(), true);
		
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
	

	
	private static final PatrolLegDay findLegDay(PatrolLeg leg, Date day, boolean create){
		for (PatrolLegDay pld : leg.getPatrolLegDays()){
			if (JsonCtParser.areDatesEqual(pld.getDate(), day)){
				return pld;
			}
		}
		if (!create) return null;
		
		PatrolLegDay pld = new PatrolLegDay();
		pld.setDate(SharedUtils.getDatePart(day, false));
		
		Calendar tmp =GregorianCalendar.getInstance();
		tmp.setTime(day);
		Calendar time = GregorianCalendar.getInstance();
		time.setTimeInMillis(0);
		time.set(Calendar.HOUR_OF_DAY, tmp.get(Calendar.HOUR_OF_DAY));
		time.set(Calendar.MINUTE, tmp.get(Calendar.MINUTE));
		time.set(Calendar.SECOND, tmp.get(Calendar.SECOND));
		time.set(Calendar.MILLISECOND, 0);
		
		pld.setStartTime(new Time(time.getTime().getTime()));
		pld.setEndTime(new Time(SmartUtils.getMidnight().getTime()- 1));

		pld.setRestMinutes(0);
		
		pld.setWaypoints(new ArrayList<PatrolWaypoint>());
		leg.getPatrolLegDays().add(pld);
		pld.setPatrolLeg(leg);
		
		
		//update leg and patrol dates as necessary
		if (leg.getStartDate().after(pld.getDate())){
			leg.setStartDate(pld.getDate());
		}
		if (leg.getEndDate().before(pld.getDate())){
			leg.setEndDate(pld.getDate());
		}
		if (leg.getPatrol().getStartDate().after(pld.getDate())){
			leg.getPatrol().setStartDate(pld.getDate());
		}
		if (leg.getPatrol().getEndDate().before(pld.getDate())){
			leg.getPatrol().setEndDate(pld.getDate());
		}
		
		return pld;
	}
	
	public static final void addPointToTrack(PatrolLegDay pld, Coordinate pnt, Date time) throws Exception{
		if (pnt == null) return;
		if (pld == null) return;
		if (pld.getTrack() == null){
			Track patrolTrack = new Track();
			patrolTrack.setPatrolLegDay(pld);
			pld.setTrack(patrolTrack);
		}
		
		LineString ll = JsonTrackUtils.addPointToTrack(pld.getTrack().getLineString(), pnt, time);
		pld.getTrack().setLineString(ll);
	}
	
	public static final void addPointToTrack(PatrolLeg leg, Coordinate pnt, Date time) throws Exception{
		if (pnt == null) return;
		PatrolLegDay pld = findLegDay(leg, time, true);
		addPointToTrack(pld, pnt, time);
	}
}
