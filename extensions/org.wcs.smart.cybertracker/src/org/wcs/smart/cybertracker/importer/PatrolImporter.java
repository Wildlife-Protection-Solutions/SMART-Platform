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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * Imports from {@link CyberTrackerPatrol} to {@link Patrol} object
 * and saves imported result as new patrol in database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolImporter extends SmartImporter {
	
	public void importData(CyberTrackerPatrol ctPatrol) {
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			Patrol patrol = buildPatrol(ctPatrol);
			for (S s : ctPatrol.getPatrolData()) {
				addObservations(patrol.getFirstLeg(), s, ctPatrol.getElementsMap(), session);
			}

			PatrolHibernateManager.savePatrol(patrol, session, true);
			session.getTransaction().commit();
		} catch (final Exception e) {
			session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), "Failed to save patrol.", e);
				}
			});
		}
		finally {
			session.close();
		}
	}
	
	private Patrol buildPatrol(CyberTrackerPatrol ctPatrol) {
		Patrol p = new Patrol();
		p.setConservationArea(SmartDB.getCurrentConservationArea());
		p.setPatrolType(ctPatrol.getPatrolType());
		p.getFirstLeg().setType(ctPatrol.getPatrolTransportType());
		p.setArmed(ctPatrol.isArmed());
		p.setTeam(ctPatrol.getTeam());
		p.setStation(ctPatrol.getStation());
		p.setMandate(ctPatrol.getMandate());
		p.setObjective(ctPatrol.getObjective());
		p.setComment(ctPatrol.getComment());
		p.setStartDate(ctPatrol.getStartDate());
		p.setEndDate(ctPatrol.getEndDate());
		p.getFirstLeg().setStartDate(ctPatrol.getStartDate());
		p.getFirstLeg().setEndDate(ctPatrol.getEndDate());
		List<PatrolLegMember> legMembers = new ArrayList<PatrolLegMember>();
		for (Employee e : ctPatrol.getMembers()) {
			PatrolLegMember plm = new PatrolLegMember();
			plm.setPatrolLeg(p.getFirstLeg());
			plm.setMember(e);
			plm.setIsLeader(e.equals(ctPatrol.getLeader()));
			plm.setIsPilot(e.equals(ctPatrol.getPilot()));
			legMembers.add(plm);
		}
		p.getFirstLeg().setMembers(legMembers);
		
		p.createLegDays();
		return p;
	}
	
}
