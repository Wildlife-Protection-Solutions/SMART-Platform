/*
 * Copyright (C) 2019 Wildlife Conservation Society
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

package org.wcs.smart.datagenerator.er;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.MissionDay;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SurveyWaypoint;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Shifts all mission data in the database spatially
 * 
 * 
 * @author Emily
 *
 */
public class MissionSpatialShiftEngine implements IDataEngine{

	@Inject
	private IEventBroker eventBroker;
	
	private Coordinate currentCenter;
	private Coordinate newCenter;
	private double scale;
	
	public MissionSpatialShiftEngine(Coordinate currentCenter, Coordinate newCenter, double scale) {
		this.currentCenter = currentCenter;
		this.newCenter = newCenter;
		this.scale = scale;
	}
	/**
	 * 
	 * @param days
	 */
	public void run(IProgressMonitor progress) throws Exception{
		
		try(Session session = HibernateManager.openSession()){
			try {
				session.beginTransaction();
			
				List<Mission> missions = QueryFactory.buildQuery(session, Mission.class, 
						new Object[] {"survey.surveyDesign.conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
						.list();
				
				for (Mission mission : missions) {
					for (MissionDay missionDay : mission.getMissionDays()) {
						//waypoints
						for (SurveyWaypoint missionwp : missionDay.getWaypoints()) {
							Coordinate c = adjustPoint(missionwp.getWaypoint().getX(), missionwp.getWaypoint().getY());
							missionwp.getWaypoint().setRawX(c.x);
							missionwp.getWaypoint().setRawY(c.y);
							session.saveOrUpdate(missionwp.getWaypoint());
						}
							
						//tracks
						if (missionDay.getTracks() != null ) {
							List<MissionTrack> current = missionDay.getTracks();
							for (MissionTrack track : current) {
								LineString ls = track.getLineString();
								List<Coordinate> newc = new ArrayList<>();
								for (Coordinate c : ls.getCoordinates()) {
									Coordinate n = adjustPoint(c.x, c.y);
									n.setZ(c.getZ());
									newc.add(n);
								}									
								LineString newTrack = GeometryFactoryProvider.getFactory().createLineString(newc.toArray(new Coordinate[newc.size()]));
								track.setLineString(newTrack);
							}
						}
						
					}
					session.saveOrUpdate(mission);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
		//post a general data change event
		eventBroker.post(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, null);
	}
	
	private Coordinate adjustPoint(double x, double y) {
		Coordinate n = new Coordinate((currentCenter.x - x) * scale + newCenter.x, (currentCenter.y - y) * scale + newCenter.y);
		return n;
	}
}
