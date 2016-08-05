/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue.cybertracker.survey;

import java.text.DateFormat;
import java.text.MessageFormat;
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
import org.wcs.smart.connect.dataqueue.cybertracker.survey.internal.Messages;
import org.wcs.smart.connect.dataqueue.cybertracker.survey.model.CtMissionLink;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.util.SharedUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * For processing track log points.  This attempts to find a patrol
 * with appropriate deviceID and leg/day and adds point to that patrol. 
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
public class MissionJsonTrackProcessor  implements IJsonProcessor {

	private Set<Mission> modifiedMissions;
	private List<String> warnings;
	
	public MissionJsonTrackProcessor() {
	}

	public Set<Mission> getModifiedMissions(){
		return modifiedMissions;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		modifiedMissions = new HashSet<Mission>();
		warnings = new ArrayList<>();
		
		List<JSONObject> processed = new ArrayList<JSONObject>();
		
		for (JSONObject feature : features){
			if (!JsonCtParser.isTrackPoint(feature)) continue;

			JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
			JSONObject geom = (JSONObject) feature.get(JsonCtParser.GEOMETRY_KEY);
			JSONArray pntArray = (JSONArray) geom.get(JsonCtParser.GEOMETRY_COORDINATE_KEY);
			if (pntArray.size() < 2 || pntArray.get(0) == null || pntArray.get(1) == null) continue;
			
			Double x = (Double) pntArray.get(0);
			Double y = (Double) pntArray.get(1);
			Date dt = JsonUtils.JSON_DATE_FORMAT.parse((String)properties.get(JsonCtParser.DATETIME_KEY));

			String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
			
			List<CtMissionLink> links = session.createCriteria(CtMissionLink.class)
					.add(Restrictions.eq("deviceId", deviceId)) //$NON-NLS-1$
					.list();

			//we want to find the patrol leg with a day that matches this day and time
			Set<MissionDay> matches = new HashSet<MissionDay>();
			for (CtMissionLink link : links){
				if (JsonCtParser.isDateBetween(dt, link.getMission().getStartDate(), link.getMission().getEndDate())){
					
					//lets look for a patrol leg day that matches
					for (MissionDay pld : link.getMission().getMissionDays()){
						if (SharedUtils.isSameDate(pld.getDate(), dt) &&
								JsonCtParser.isTimeBetween(dt, pld.getStartTime(), pld.getEndTime())){
							matches.add(pld);
						}
					}
					
				}
			}

			if (matches.isEmpty()){
				continue;
			}else{
				if (matches.size() == 1){
					//this is simple
					MissionDay md = matches.iterator().next();
					addPointToMisisonTracks(md, new Coordinate(x,y), dt);
					processed.add(feature);
					
					if (md.getMission().getUuid() != null) modifiedMissions.add(md.getMission());
				}else{
					StringBuilder sb = new StringBuilder();
					for (MissionDay pld : matches){
						sb.append(pld.getMission().getId() + "(" + DateFormat.getDateInstance().format(pld.getDate()) + "), "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sb.deleteCharAt(sb.length() - 1);
					sb.deleteCharAt(sb.length() - 1);
					
					warnings.add(MessageFormat.format(Messages.MissionJsonTrackProcessor_MultipleMatched, DateFormat.getDateTimeInstance().format(dt), sb.toString()));
				}
				
			}
		}
		return processed;

	}

	@Override
	public void afterSave() {
	}

	@Override
	public String getStatusMessage() {
		return null;
	}
	
	public List<String> getWarnings(){
		return this.warnings;
	}
	
	
	public static void addSuPointToMisisonTracks(MissionDay md, SamplingUnit su, Coordinate c, Date dt) throws Exception{
		if (md.getTracks() == null) md.setTracks(new ArrayList<MissionTrack>());
		
		MissionTrack addTo = null;
		if (md.getTracks().isEmpty()){
			MissionTrack newTrack = new MissionTrack();
			md.getTracks().add(newTrack);
			newTrack.setMissionDay(md);
			newTrack.setId(MessageFormat.format(Messages.MissionJsonTrackProcessor_TrackLabel,  md.getTracks().size()));
			newTrack.setSamplingUnit(su);
			addTo = newTrack;
			
		}else{
			//if the last track point is the same sampling unit then add to that
			//track; otherwise create a new track with the new sampling unit			
			MissionTrack lastTrack = null;
			double lastTime = -1;
			for (MissionTrack t: md.getTracks()){
				if (lastTrack == null || lastTime > t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).z){
					lastTrack = t;
					lastTime = t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).z;
				}
			}
			
			if (lastTrack.getSamplingUnit().equals(su)){
				//add point to track
				addTo = lastTrack;
			}else{
				//create a new track and add to new track
				MissionTrack newTrack = new MissionTrack();
				md.getTracks().add(newTrack);
				newTrack.setMissionDay(md);
				newTrack.setId(MessageFormat.format(Messages.MissionJsonTrackProcessor_TrackLabel,  md.getTracks().size()));
				newTrack.setSamplingUnit(su);
				addTo = newTrack;
			}
		}
		LineString newLs = JsonTrackUtils.addPointToTrack(addTo.getLineString(), c, dt);
		addTo.setLineString(newLs);
	}
	
	/*
	 * Searches for track that starts before dt and end after dt.  If found
	 * then the point is added to that track; if not found then it is added 
	 * to the last track.
	 * 
	 * Assumption are:
	 * 1. observations are processed first 
	 * 2. all observations create new track points
	 * 3. when sampling unit is changed a "observation" is made 
	 * 
	 */
	public static void addPointToMisisonTracks(MissionDay md, Coordinate c, Date dt) throws Exception{
		if (md.getTracks() == null) md.setTracks(new ArrayList<MissionTrack>());
		
		MissionTrack addTo = null;
		if (md.getTracks().isEmpty()){
			MissionTrack newTrack = new MissionTrack();
			md.getTracks().add(newTrack);
			newTrack.setMissionDay(md);
			newTrack.setId(MessageFormat.format(Messages.MissionJsonTrackProcessor_TrackLabel,  md.getTracks().size()));
			addTo = newTrack;
			
		}else{
			double z = JsonTrackUtils.convertTimeToGMT(dt);
			//see if point fits between existing track points
			for (MissionTrack t : md.getTracks()){
				double t1 = t.getLineString().getCoordinateN(0).z;
				double t2 = t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).z;
				if (t1 <= z && z <= t2){
					addTo = t;
					break;
				}
			}
			if (addTo == null){
				//add to 
				double lastTrack = -1;
				for (MissionTrack t : md.getTracks()){
					double t2 = t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).z;
					if (t2 > lastTrack){
						lastTrack = t2;
						addTo = t;
					}
					
				}
			}
		}
		
		LineString newLs = JsonTrackUtils.addPointToTrack(addTo.getLineString(), c, dt);
		addTo.setLineString(newLs);
	}
}
