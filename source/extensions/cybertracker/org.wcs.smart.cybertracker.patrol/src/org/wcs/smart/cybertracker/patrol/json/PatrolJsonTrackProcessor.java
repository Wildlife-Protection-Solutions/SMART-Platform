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
package org.wcs.smart.cybertracker.patrol.json;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.cybertracker.JsonUtils;
import org.wcs.smart.cybertracker.importer.json.IJsonProcessor;
import org.wcs.smart.cybertracker.importer.json.JsonCtParser;
import org.wcs.smart.cybertracker.importer.json.JsonTrackUtils;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CtPatrolLink;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Track;

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
public class PatrolJsonTrackProcessor implements IJsonProcessor {

	private Set<Patrol> modifiedPatrols;
	private List<String> warnings;
	
	public PatrolJsonTrackProcessor() {
	}

	public Set<Patrol> getModifiedPatrols(){
		return modifiedPatrols;
	}
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception{
		modifiedPatrols = new HashSet<Patrol>();
		warnings = new ArrayList<>();
		
		List<JSONObject> processed = new ArrayList<JSONObject>();
		
		for (JSONObject feature : features){
			if (!JsonCtParser.isTrackPoint(feature)) continue;

			JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
			JSONObject geom = (JSONObject) feature.get(JsonCtParser.GEOMETRY_KEY);
			JSONArray pntArray = (JSONArray) geom.get(JsonCtParser.GEOMETRY_COORDINATE_KEY);
			if (pntArray.size() < 2 || pntArray.get(0) == null || pntArray.get(1) == null) continue;
			
			Double x = ((Number) pntArray.get(0)).doubleValue();
			Double y = ((Number) pntArray.get(1)).doubleValue();
			LocalDateTime dt = JsonUtils.parseJsonDateTime((String)properties.get(JsonCtParser.DATETIME_KEY));

			String deviceId = (String) properties.get(JsonCtParser.DEVICE_ID);
			
			List<CtPatrolLink> links = QueryFactory.buildQuery(session, CtPatrolLink.class,"deviceId", deviceId).list(); //$NON-NLS-1$

			//we want to find the patrol leg with a day that matches this day and time
			List<PatrolLegDay> matches = new ArrayList<PatrolLegDay>();
			for (CtPatrolLink link : links){
				if (JsonCtParser.isDateBetween(dt.toLocalDate(), link.getPatrolLeg().getStartDate(), link.getPatrolLeg().getEndDate())){
					
					//lets look for a patrol leg day that matches
					for (PatrolLegDay pld : link.getPatrolLeg().getPatrolLegDays()){
						if (pld.getDate().isEqual(dt.toLocalDate()) &&
								JsonCtParser.isTimeBetween(dt.toLocalTime(), pld.getStartTime(), pld.getEndTime())){
							matches.add(pld);
						}
					}
					
				}
			}

			
			if (matches.size() == 2) {
				//if matches start point of one, and end point of other 
				//then add to the end point one
				LocalTime dlt = dt.toLocalTime();
				
				PatrolLegDay first = matches.get(0);
				PatrolLegDay second = matches.get(1);
				
				LocalTime firste = first.getEndTime();
				LocalTime firsts = first.getStartTime();
				LocalTime seconde = second.getEndTime();
				LocalTime seconds = second.getStartTime();
				
				if (firste.equals(dlt) && seconds.equals(dlt)) {
					//remove second
					matches.remove(1);
				}else if (firsts.equals(dlt) && seconde.equals(dlt)) {
					//remove first
					matches.remove(0);
				}
			}
			
			if (matches.isEmpty()) continue;
			
			if (matches.size() == 1){
				for (PatrolLegDay pld : matches) {
					Track t = pld.getTrack();
					if (t == null){
						t = new Track();
						pld.setTrack(t);
						t.setPatrolLegDay(pld);
					}
					
					List<LineString> lineStrings = new ArrayList<>(t.getLineStrings());
					if (!lineStrings.isEmpty()) {
						LineString newLs = JsonTrackUtils.addPointToTrack(lineStrings.get(lineStrings.size()-1), new Coordinate(x,y), dt);
						lineStrings.set(lineStrings.size()-1, newLs);
						t.setLineStrings(lineStrings);
					} else {
						LineString newLs = JsonTrackUtils.addPointToTrack(null, new Coordinate(x,y), dt);
						t.setLineStrings(Arrays.asList(newLs));
					}
	
					processed.add(feature);
						
					if (pld.getPatrolLeg().getPatrol().getUuid() != null) modifiedPatrols.add(pld.getPatrolLeg().getPatrol());
				}
			}else{
				StringBuilder sb = new StringBuilder();
				for (PatrolLegDay pld : matches){
					sb.append(pld.getPatrolLeg().getPatrol().getId() + "(" + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(pld.getDate()) + "), "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.deleteCharAt(sb.length() - 1);
				
				warnings.add(MessageFormat.format(Messages.PatrolJsonTrackProcessor_MultiplePnts, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(dt), sb.toString()));
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
}
