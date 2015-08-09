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
package org.wcs.smart.cybertracker.importer;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.ElementsUtil;
import org.wcs.smart.cybertracker.model.CyberTrackerRawData;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.cybertracker.model.data.Data.Elements.E;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.hibernate.HibernateManager;

import com.vividsolutions.jts.geom.Coordinate;


/**
 * Common data builder class.
 * Builds specific patrol/survey objects that suitable for further import
 * 
 * @author elitvin
 * @since 4.0.0
 */
public abstract class CyberTrackerDataBuilder {

	public List<ICyberTrackerData> buildRecords(CyberTrackerRawData rawData) {
		List<ICyberTrackerData> records = new ArrayList<ICyberTrackerData>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			for (String id : rawData.getTripsMap().keySet()) {
				List<S> sData = rawData.getTripsMap().get(id);
				ICyberTrackerData ctTripData = createDataRecord(session, rawData.getElementsMap(), sData);
				List<Coordinate> trackPoints = AbstractSmartImporter.listPart(rawData.getTimerTrackList(), ctTripData.getStartDate(), ctTripData.getEndDate());
				
				S lastS = !ctTripData.getSData().isEmpty() ? ctTripData.getSData().get(ctTripData.getSData().size()-1) : null;
				if (lastS != null && !hasWaypointData(lastS, rawData.getElementsMap())) {
					//lastS is last point recorded when "End Patrol" was selected
					//do not record it as a separate waypoint but add to end of the track
					ctTripData.getSData().remove(ctTripData.getSData().size()-1);
					//adding last waypoint to the track
					//TODO: should all waypoints be part of a track?
					Coordinate coord = toCoordinate(lastS);
					if (coord != null)
						trackPoints.add(coord);
				}
				ctTripData.setTimerTrackList(trackPoints);
				records.add(ctTripData);
			}
		} finally {
			session.getTransaction().rollback();
			session.close();
		}
		//sort patrols based on there CT id before returning
		Collections.sort(records, new Comparator<ICyberTrackerData>() {
			@Override
			public int compare(ICyberTrackerData p1, ICyberTrackerData p2) {
				if (p1.getStartDate() == null)
					return -1;
				if (p2.getStartDate() == null)
					return 1;
				return p1.getStartDate().compareTo(p2.getStartDate());
			}});
		return records;
		
	}

	protected abstract ICyberTrackerData createDataRecord(Session session, Map<String, E> elementsMap, List<S> sData);

	protected <T> T fetchFromTag0(Class<T> clazz, E e, Session session) {
		String tag0 = e != null ? e.getTag0() : null;
		if (tag0 != null) {
			return CyberTrackerHibernateManager.fetchByUuid(clazz, tag0, session);
		}
		return null;
	}

	private boolean hasWaypointData(S s, Map<String, E> eMap) {
		//true if at least one category or attribute was selected
		for (S.A a : s.getA()) {
			E e = eMap.get(a.getI());
			if (e != null) {
				if (ElementsUtil.CATEGORY_ELEMENT_TAG.equals(e.getTag1()) || ElementsUtil.ATTRIBUTE_ELEMENT_TAG.equals(e.getTag1()))
					return true;
			}
		}
		return false;
	}

	private Coordinate toCoordinate(S s) {
		DateFormat formatter = AbstractSmartImporter.createCyberTrackerDateFormatter();
		Date date = null;
		Time time = null;
		double x = 0;
		double y = 0;
		for (S.A a : s.getA()) {
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
		
		if (date == null || time == null || x == 0 || y == 0)
			return null;
		
		return new Coordinate(x, y, AbstractSmartImporter.combine(date, time).getTime());
	}
}
