package org.wcs.smart.connect.dataqueue.cybertracker.patrol;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wcs.smart.connect.dataqueue.cybertracker.IJsonProcessor;
import org.wcs.smart.connect.dataqueue.cybertracker.JsonCtParser;
import org.wcs.smart.connect.dataqueue.cybertracker.JsonTrackUtils;
import org.wcs.smart.connect.dataqueue.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * For processing track log points:
 * 
 * { "type": "Feature",
  "geometry": { "type": "Point", "coordinates": [-0.006034, 0.006701] },
  "properties": {
    "deviceId": "dfc654d8-9b80-4006-b8b1-f906e8285367",
    "id": "00000000-0000-0000-0000-000000000000",
    "dateTime": "2016-07-22T10:27:28.302-07:00",
    "altitude": 274.314483,
    "accuracy": 10.000000
  }
}

 * @author Emily
 *
 */
public class PatrolJsonTrackProcessor  implements IJsonProcessor {

	private Set<Patrol> modifiedPatrols;
	public PatrolJsonTrackProcessor() {
		
	}

	public Set<Patrol> getModifiedPatrols(){
		return modifiedPatrols;
	}
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		modifiedPatrols = new HashSet<Patrol>();
		
		List<JSONObject> processed = new ArrayList<JSONObject>();
		
		for (JSONObject feature : features){
			//only want to process features with no sighting data
			JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
			if (properties == null) continue;
			JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
			if (sighting != null) continue;
			
			JSONObject geom = (JSONObject) feature.get(JsonCtParser.GEOMETRY_KEY);
			if (!"Point".equalsIgnoreCase((String)geom.get(JsonCtParser.GEOMETRY_TYPE_KEY))){
				//only parse points
				continue;
			}
			
			JSONArray pntArray = (JSONArray) geom.get(JsonCtParser.GEOMETRY_COORDINATE_KEY);
			if (pntArray.size() < 2 || pntArray.get(0) == null || pntArray.get(1) == null) continue;
			
			Double x = (Double) pntArray.get(0);
			Double y = (Double) pntArray.get(1);
			Date dt = JsonUtils.JSON_DATE_FORMAT.parse((String)properties.get(JsonCtParser.DATETIME_KEY));

			String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
			
			List<CtPatrolLink> links = session.createCriteria(CtPatrolLink.class)
					.add(Restrictions.eq("deviceId", deviceId))
					.list();
			

			//we want to find the patrol leg with a day that matches this day and time
			List<PatrolLegDay> matches = new ArrayList<PatrolLegDay>();
			for (CtPatrolLink link : links){
				if (JsonCtParser.isDateBetween(dt, link.getPatrolLeg().getStartDate(), link.getPatrolLeg().getEndDate())){
					
					//lets look for a patrol leg day that matches
					for (PatrolLegDay pld : link.getPatrolLeg().getPatrolLegDays()){
						if (JsonCtParser.areDatesEqual(pld.getDate(), dt) &&
								JsonCtParser.isTimeBetween(dt, pld.getStartTime(), pld.getEndTime())){
							matches.add(pld);
						}
					}
					
				}
			}

			if (matches.isEmpty()){
				//there is no link in the database for this patrol that 
				//matches the device id and the date/time
				//this could be due to a number of reasons.  Perhaps the patrol
				//has not yet been created, or the patrol leg day has not yet been created
				//or perhaps this point belongs to a mission track
				
				//TODO:
				System.out.println("no patrols to add track to");
				continue;
			}else{
				if (matches.size() == 1){
					//this is simple
					PatrolLegDay pld = matches.get(0);
					Track t = pld.getTrack();
					if (t == null){
						t = new Track();
						pld.setTrack(t);
						t.setPatrolLegDay(pld);
					}
					
					LineString newLs = JsonTrackUtils.addPointToTrack(t.getLineString(), new Coordinate(x,y), dt);
					t.setLineString(newLs);
					processed.add(feature);
					
					modifiedPatrols.add(pld.getPatrolLeg().getPatrol());
				}else{
					//there is more than one possible match.  We need to do
					//something here; pick one? let the user pick?
					//
					//TODO:
					System.out.println("more than one possible track");
				}
				
			}
		}
		return features;

	}

	@Override
	public void afterSave() {
		// TODO Auto-generated method stub
		
	}
}
