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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.wcs.smart.util.SharedUtils;

/**
 * Shifts all mission data in the database
 * for the current conservation area
 * by the given number of days.  
 * 
 * @author Emily
 *
 */
public class MissionTimeShiftEngine implements IDataEngine{

	@Inject
	private IEventBroker eventBroker;
	
	private int shiftdays;
	
	public MissionTimeShiftEngine(int shiftdays) {
		this.shiftdays = shiftdays;
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
					mission.setStartDate(adjustDate(mission.getStartDate()));
					mission.setEndDate(adjustDate(mission.getEndDate()));
					for (MissionDay missionDay : mission.getMissionDays()) {
						missionDay.setDate(adjustDate(missionDay.getDate()));
						
						//waypoints
						for (SurveyWaypoint wp : missionDay.getWaypoints()) {
							wp.getWaypoint().setDateTime(adjustDateTime(wp.getWaypoint().getDateTime()));
							session.saveOrUpdate(wp.getWaypoint());
						}
						
						if (missionDay.getTracks() != null) {
							for (MissionTrack track : missionDay.getTracks()) {
								LineString ls = track.getLineString();
								for (Coordinate c : ls.getCoordinates()) {
									long newz = SharedUtils.toLongTime( adjustDateTime (SharedUtils.toLocalDateTime(c)));
									c.setZ(newz);
								}
								track.setLineString(ls);
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
		
	private LocalDate adjustDate(LocalDate date) {
		return date.plusDays(shiftdays);
	}
	
	private LocalDateTime adjustDateTime(LocalDateTime date) {
		return date.plusDays(shiftdays);
	}
}
