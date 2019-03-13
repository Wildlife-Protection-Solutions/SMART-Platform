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

package org.wcs.smart.datagenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;

/**
 * Shifts all patrol data in the database
 * for the current conservation area
 * by the given number of days.  
 * 
 * @author Emily
 *
 */
public class DataTimeShiftEngine implements IDataEngine{

	@Inject
	private IEventBroker eventBroker;
	
	private int shiftdays;
	
	public DataTimeShiftEngine(int shiftdays) {
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
			
				List<Patrol> patrols = QueryFactory.buildQuery(session, Patrol.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				
				for (Patrol p : patrols) {
					p.setStartDate(adjustDate(p.getStartDate()));
					p.setEndDate(adjustDate(p.getEndDate()));
					for (PatrolLeg pl : p.getLegs()) {
						pl.setStartDate(adjustDate(pl.getStartDate()));
						pl.setEndDate(adjustDate(pl.getEndDate()));
						for (PatrolLegDay pld : pl.getPatrolLegDays()) {
							pld.setDate(adjustDate(pld.getDate()));
							
							//waypoints
							for (PatrolWaypoint pw : pld.getWaypoints()) {
								pw.getWaypoint().setDateTime(adjustDateTime(pw.getWaypoint().getDateTime()));
								session.saveOrUpdate(pw.getWaypoint());
							}
							
							//tracks
							if (pld.getTrack() != null && pld.getTrack().getLineStrings() != null) {
								List<LineString> current = pld.getTrack().getLineStrings();
								for (LineString ls : current) {
									for (Coordinate c : ls.getCoordinates()) {
										c.setZ(adjustDateTime(new Date( (Double.valueOf(c.getZ())).longValue() )).getTime());
									}
								}
								pld.getTrack().setLineStrings(current);
							}
						}
					}
					session.saveOrUpdate(p);
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
	
	private Date adjustDateTime(Date datetime) {
		LocalDateTime ld = new java.sql.Timestamp(datetime.getTime()).toLocalDateTime();
		ld = ld.plusDays(shiftdays);
		return Date.from(ld.atZone(ZoneId.systemDefault()).toInstant());
	}
	
	private Date adjustDate(Date date) {
		LocalDate ld = new java.sql.Date(date.getTime()).toLocalDate();
		ld = ld.plusDays(shiftdays);
		return Date.from( ld.atStartOfDay(ZoneId.systemDefault()).toInstant() );
	}
}
