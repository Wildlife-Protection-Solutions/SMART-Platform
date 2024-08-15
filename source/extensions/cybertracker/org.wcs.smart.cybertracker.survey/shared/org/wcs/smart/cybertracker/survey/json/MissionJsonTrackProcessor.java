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
package org.wcs.smart.cybertracker.survey.json;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.json.CtJsonObservationParser;
import org.wcs.smart.cybertracker.json.CtJsonUtil;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.JsonTrackUtils;
import org.wcs.smart.cybertracker.json.SmartMobileProcessingError;
import org.wcs.smart.cybertracker.survey.model.CtMissionLink;
import org.wcs.smart.cybertracker.survey.model.ISurveyCyberTrackerLabelProvider;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.SharedUtils;

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

	public static final Object TRACK_LBL = new Object();
	
	protected Set<Mission> modifiedMissions;
	protected List<JsonImportWarning> warnings;
	protected Locale locale;
	private ConservationArea ca;
	
	public MissionJsonTrackProcessor(ConservationArea ca) {
		this.ca = ca;
	}

	public Set<Mission> getModifiedMissions(){
		return modifiedMissions;
	}
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session, Locale l, IProgressMonitor monitor) throws Exception{
		this.locale = l;
		
		modifiedMissions = new HashSet<Mission>();
		warnings = new ArrayList<>();
		
		List<JSONObject> processed = new ArrayList<JSONObject>();
		
		HashMap<String, List<CtMissionLink>> linkmap = new HashMap<>();
		SubMonitor smonitor = SubMonitor.convert(monitor, features.size());

		for (JSONObject feature : features){
			smonitor.worked(1);
			if (!CtJsonUtil.isTrackPoint(feature)) continue;

			JSONObject properties = (JSONObject) feature.get(CtJsonObservationParser.PROPERTIES_KEY);
			JSONObject geom = (JSONObject) feature.get(CtJsonObservationParser.GEOMETRY_KEY);
			JSONArray pntArray = (JSONArray) geom.get(CtJsonObservationParser.GEOMETRY_COORDINATE_KEY);
			if (pntArray.size() < 2 || pntArray.get(0) == null || pntArray.get(1) == null) continue;
			
			Double x = ((Number) pntArray.get(0)).doubleValue();
			Double y = ((Number) pntArray.get(1)).doubleValue();
			LocalDateTime dt = CtJsonUtil.parseJsonDateTime((String)properties.get(CtJsonObservationParser.DATETIME_KEY));

			String deviceId = (String) properties.get(CtJsonObservationParser.DEVICE_ID);
			
			List<CtMissionLink> links = linkmap.get(deviceId);
			if (links == null) {
				//links in the same conservation area
				String query = "SELECT ml FROM CtMissionLink ml join ml.mission m WHERE m.survey.surveyDesign.conservationArea = :ca and ml.deviceId = :deviceid";
				links = session.createQuery(query, CtMissionLink.class)
					.setParameter("ca", this.ca)
					.setParameter("deviceid", deviceId)
					.list();				
				linkmap.put(deviceId, links);
			}
			
			//we want to find the patrol leg with a day that matches this day and time
			Set<MissionDay> matches = new HashSet<MissionDay>();
			for (CtMissionLink link : links){
				if (CtJsonUtil.isDateBetween(dt.toLocalDate(), link.getMission().getStartDate(), link.getMission().getEndDate())){
					
					//lets look for a patrol leg day that matches
					for (MissionDay pld : link.getMission().getMissionDays()){
						if (pld.getDate().isEqual(dt.toLocalDate()) &&
								CtJsonUtil.isTimeBetween(dt.toLocalTime(), pld.getStartTime(), pld.getEndTime())){
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
					
					if (!md.getMission().getSurvey().getSurveyDesign().getConservationArea().equals(this.ca)) {
						String message = SmartContext.INSTANCE.getClass(ISurveyCyberTrackerLabelProvider.class).getLabel(MissionJsonProcessor.CA_ERROR, locale);
						throw new SmartMobileProcessingError(MessageFormat.format(message, this.ca.getNameLabel(), 
							md.getMission().getSurvey().getSurveyDesign().getConservationArea().getNameLabel()));
					}
					
					addPointToMisisonTracks(md, new Coordinate(x,y), dt);
					processed.add(feature);
					
					if (md.getMission().getUuid() != null) modifiedMissions.add(md.getMission());
				}else{
					StringBuilder sb = new StringBuilder();
					for (MissionDay pld : matches){
						sb.append(pld.getMission().getId() + "(" + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(pld.getDate()) + "), "); //$NON-NLS-1$ //$NON-NLS-2$
					}
					sb.deleteCharAt(sb.length() - 1);
					sb.deleteCharAt(sb.length() - 1);
					warnings.add(new MissionJsonImportWarning(MissionJsonImportWarning.WarningType.TRACK_POINT_MULTI_MATCHES, DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(dt), sb.toString()));
				}
				
			}
		}
		return processed;

	}

	@Override
	public void cleanUp() {
		//nothing to clean up
	}
	
	@Override
	public String getStatusMessage(Locale l) {
		return null;
	}
	
	@Override
	public List<JsonImportWarning> getWarnings(){
		return this.warnings;
	}
	
	private static String getTrackLabel(Locale l) {
		return SmartContext.INSTANCE.getClass(ISurveyCyberTrackerLabelProvider.class).getLabel(TRACK_LBL, l);
	}
	
	public static void addSuPointToMisisonTracks(MissionDay md, SamplingUnit su, Coordinate c, LocalDateTime dt, Locale locale) throws Exception{
		if (md.getTracks() == null) md.setTracks(new ArrayList<MissionTrack>());
		
		MissionTrack addTo = null;
		if (md.getTracks().isEmpty()){
			MissionTrack newTrack = new MissionTrack();
			md.getTracks().add(newTrack);
			newTrack.setMissionDay(md);
			newTrack.setId(MessageFormat.format(getTrackLabel(locale),  md.getTracks().size()));
			newTrack.setSamplingUnit(su);
			addTo = newTrack;
			
		}else{
			//if the last track point is the same sampling unit then add to that
			//track; otherwise create a new track with the new sampling unit			
			MissionTrack lastTrack = null;
			double lastTime = -1;
			for (MissionTrack t: md.getTracks()){
				if (lastTrack == null || lastTime < t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).getZ()){
					lastTrack = t;
					lastTime = t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).getZ();
				}
			}
			
			if (isEqual(lastTrack.getSamplingUnit(), su)){
				//add point to track
				addTo = lastTrack;
			}else{
				//create a new track and add to new track
				MissionTrack newTrack = new MissionTrack();
				md.getTracks().add(newTrack);
				newTrack.setMissionDay(md);
				newTrack.setId(MessageFormat.format(getTrackLabel(locale),  md.getTracks().size()));
				newTrack.setSamplingUnit(su);
				addTo = newTrack;
			}
		}
		LineString newLs = JsonTrackUtils.addPointToTrack(addTo.getLineString(), c, dt);
		addTo.setLineString(newLs);
	}
	
	private static boolean isEqual(SamplingUnit s1, SamplingUnit s2){
		if (s1 == null && s2 == null) return true;
		if (s1 != null && s2 != null) return s1.equals(s2);
		return false;
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
	public void addPointToMisisonTracks(MissionDay md, Coordinate c, LocalDateTime dt) throws Exception{
		if (md.getTracks() == null) md.setTracks(new ArrayList<MissionTrack>());
		
		MissionTrack addTo = null;
		if (md.getTracks().isEmpty()){
			MissionTrack newTrack = new MissionTrack();
			md.getTracks().add(newTrack);
			newTrack.setMissionDay(md);
			newTrack.setId(MessageFormat.format(getTrackLabel(locale),  md.getTracks().size()));
			addTo = newTrack;
			
		}else{
			double z = SharedUtils.toLongTime(dt);
			//see if point fits between existing track points
			for (MissionTrack t : md.getTracks()){
				double t1 = t.getLineString().getCoordinateN(0).getZ();
				double t2 = t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).getZ();
				if (t1 <= z && z <= t2){
					addTo = t;
					break;
				}
			}
			if (addTo == null){
				//add to 
				double lastTrack = -1;
				for (MissionTrack t : md.getTracks()){
					double t2 = t.getLineString().getCoordinateN(t.getLineString().getNumPoints()-1).getZ();
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
