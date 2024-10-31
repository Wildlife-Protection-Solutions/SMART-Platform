/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.qa;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.patrol.CleanPatrolEngine;
import org.wcs.smart.cybertracker.patrol.CleanPatrolSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;

/**
 * Desktop processor for processing non-ended smart patrols. This
 * ends the patrols and tries to clean up track data at the
 * end of the patrol (remove cluster of points, or empty days)
 */
public class UnendedPatrolProcessor extends CleanPatrolEngine{

	private ConservationArea ca;
	
	private List<Patrol> error;
	private List<Patrol> modified;
	
	public UnendedPatrolProcessor(ConservationArea ca, CleanPatrolSettings settings) {
		super(settings);
		this.ca = ca;		
	}
		
	public String getStatusMessage() {
		if (modified.isEmpty() && error.isEmpty()) return "No data found that required processing";
		
		StringBuilder sb = new StringBuilder();
		sb.append("The following patrols have been cleaned and ended:\n");
		for (Patrol p : modified) {
			sb.append(p.getId());
			sb.append("\n");
		}
		
		if (!error.isEmpty()) {
			sb.append("\nThe following patrols generated errors when processing:\n");
			for (Patrol p : error) {
				sb.append(p.getId());
				sb.append("\n");
			}
		}
		
		return sb.toString();
	}
	
	
	/**
	 * works on patrols that started between start (inclusive) and end (exclusive)
	 * @param startDate
	 * @param endDate
	 */
	public void doWork(LocalDate startDate, LocalDate endDate, IProgressMonitor monitor) {
		modified = new ArrayList<>();
		error = new ArrayList<>();
		
		LocalDate now = LocalDate.now();
		
		List<Patrol> tocleanup = new ArrayList<>();
		
		monitor.beginTask("Loading Data", 1);
		
		try(Session session = HibernateManager.openSession()){
			
			//find all patrols that started between start and end
			//are ct patrols and not ended		
			String hql = "SELECT p FROM CtPatrolLink k join k.patrolLeg l join l.patrol p WHERE p.startDate >= :start and p.endDate < :end and k.lastObservationCnt != -1 and p.conservationArea = :ca"; //$NON-NLS-1$
			List<Patrol> tovalidate = session.createQuery(hql, Patrol.class)
					.setParameter("start", startDate) //$NON-NLS-1$
					.setParameter("end", endDate) //$NON-NLS-1$
					.setParameter("ca", ca) //$NON-NLS-1$
					.list();
			for (Patrol p : tovalidate) {
				if (tocleanup.contains(p)) continue;
				
				if (p.getEndDate().plusDays(settings.getDays()).isBefore(now)) {
					//no data in the last x days
					tocleanup.add(p);
					continue;
				}
				
				//find the last day with at least one observation
				LocalDate lastDayWithData = LocalDate.MIN;
				for (PatrolLeg pl : p.getLegs()) {
					for (PatrolLegDay pld : pl.getPatrolLegDays()) {
						if (!pld.getWaypoints().isEmpty() && pld.getDate().isAfter(lastDayWithData)) {
							lastDayWithData = pld.getDate();
						}
					}
				}
				
				
				if (lastDayWithData.plusDays(settings.getDays()).isBefore(now)) {
					//no observations in the last x days
					tocleanup.add(p);
				}	
			}	
		}
		
		monitor.beginTask("Processing data", tocleanup.size()+1);
		for(Patrol p : tocleanup) {
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					p = session.get(Patrol.class, p.getUuid());
					if (cleanUpAndEndPatrol(p, session)) {
						modified.add(p);
					}
					session.getTransaction().commit();
				}catch (Exception ex) {
					SmartPlugIn.displayLog(MessageFormat.format("Unable to clean up patrol {0}: {1}", p.getId(), ex.getMessage()), ex);
					session.getTransaction().rollback();
					error.add(p);
				}
			}
			monitor.worked(1);
		}		

		//fire patrol modified event
		monitor.beginTask("Refreshing data", modified.size());
		for (Patrol p : modified) {
			PatrolEventManager.getInstance().patrolSaved(p, true);
			monitor.worked(1);	
		}
		
	}
	
}
