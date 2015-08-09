/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.model;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter.CoordinateZComparator;
import org.wcs.smart.cybertracker.model.data.Data;
import org.wcs.smart.cybertracker.model.data.Data.Sightings;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author elitvin
 * @since 4.0.0
 */
public class CyberTrackerRawData {

	public Map<String, Data.Elements.E> elementsMap;
	public Map<String, List<Data.Sightings.S>> tripsMap;
	public List<Coordinate> timerTrackList;

	public CyberTrackerRawData(Data data) {
		super();
		this.elementsMap = buildElementsMap(data);
		this.tripsMap = buildTripsMap(data);
		this.timerTrackList = buildTimerTrackList(data);
	}

	public Map<String, Data.Elements.E> getElementsMap() {
		return elementsMap;
	}

	public Map<String, List<Data.Sightings.S>> getTripsMap() {
		return tripsMap;
	}

	public List<Coordinate> getTimerTrackList() {
		return timerTrackList;
	}

	private Map<String, Data.Elements.E> buildElementsMap(Data data) {
		Map<String, Data.Elements.E> result = new HashMap<String, Data.Elements.E>();
		if (data == null || data.getElements() == null)
			return result;
		for (Data.Elements.E e : data.getElements().getE()) {
			result.put(e.getI(), e);
		}
		return result;
	}

	/**
	 * Returns mapped patrol_id to list of {@link Sightings.S} where each sighting is related to one patrol
	 * 
	 * @param data
	 * @return mapped patrol_id to list of {@link Sightings.S}

	 */
	private Map<String, List<Data.Sightings.S>> buildTripsMap(Data data) {
		Map<String, List<Data.Sightings.S>> result = new HashMap<String, List<Data.Sightings.S>>();
		if (data == null || data.getSightings() == null)
			return result;
		for (Data.Sightings.S s : data.getSightings().getS()) {
			//fetch patrol id value
			String patrolId = null;
			for (Data.Sightings.S.A a : s.getA()) {
				if (ScreensUtil.RESULT_ID.equals(a.getN())) {
					patrolId = a.getV();
				}
			}
			if (patrolId == null)
				continue;
			
			List<Data.Sightings.S> lst = result.get(patrolId);
			if (lst == null) {
				lst = new ArrayList<Data.Sightings.S>();
				result.put(patrolId, lst);
			}
			lst.add(s);
		}
		return result;
	}

	private List<Coordinate> buildTimerTrackList(Data data) {
		List<Coordinate> result = new ArrayList<Coordinate>();
		if (data == null || data.getTimerTracks() == null)
			return result;
		DateFormat formatter = AbstractSmartImporter.createCyberTrackerDateFormatter();
		for (Data.TimerTracks.T t : data.getTimerTracks().getT()) {
			Date date = null;
			Time time = null;
			double x = 0;
			double y = 0;
			for (Data.TimerTracks.T.A a : t.getA()) {
				String i = a.getI();
				if (ICyberTrackerConstants.DATE.equals(i)) {
					try {
						date = formatter.parse(a.getV());
					} catch (ParseException e) {
						CyberTrackerPlugIn.log(e.getMessage(), e);
					}
				} else if (ICyberTrackerConstants.TIME.equals(i)) {
					time = Time.valueOf(a.getV());
				} else if (ICyberTrackerConstants.LATITUDE.equals(i)) {
					y = Double.valueOf(a.getV());
				} else if (ICyberTrackerConstants.LONGITUDE.equals(i)) {
					x = Double.valueOf(a.getV());
				}
			}
			
			if (date == null || time == null)
				continue;
			
			result.add(new Coordinate(x, y, AbstractSmartImporter.combine(date, time).getTime()));
		}
		//sort by date+time
		Collections.sort(result, new CoordinateZComparator());
		return result;
	}
	
}
