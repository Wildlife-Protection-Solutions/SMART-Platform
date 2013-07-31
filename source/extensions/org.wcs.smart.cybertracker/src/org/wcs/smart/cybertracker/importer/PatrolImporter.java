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

import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Imports from {@link CyberTrackerPatrol} to {@link Patrol} object
 * and saves imported result as new patrol in database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolImporter extends SmartImporter {
	
	public Patrol importData(CyberTrackerPatrol ctPatrol) {
		clearWarning();
		for (String warning : ctPatrol.getWarnings()) {
			addWarning(warning);
		}
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			Patrol patrol = buildPatrol(ctPatrol);
			if (patrol.getFirstLeg().getType() == null) {
				if(!fixTransportError(patrol.getFirstLeg(), ctPatrol, session))
					return null;
			}
			for (S s : ctPatrol.getPatrolData()) {
				addObservations(patrol.getFirstLeg(), s, ctPatrol.getElementsMap(), session);
			}
			
			if (!displayWarnings())
				return null;

			PatrolHibernateManager.savePatrol(patrol, session, true);
			session.getTransaction().commit();
			PatrolEventManager.getInstance().patrolAdded(patrol);
			return patrol;
		} catch (final Exception e) {
			session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.PatrolImporter_Save_Error, e);
				}
			});
			return null;
		}
		finally {
			session.close();
		}
	}
	
	private Patrol buildPatrol(CyberTrackerPatrol ctPatrol) {
		Patrol p = new Patrol();
		p.setConservationArea(SmartDB.getCurrentConservationArea());
		p.setPatrolType(ctPatrol.getPatrolType());
		p.setArmed(ctPatrol.isArmed());
		p.setTeam(ctPatrol.getTeam());
		p.setStation(ctPatrol.getStation());
		p.setMandate(ctPatrol.getMandate());
		p.setObjective(ctPatrol.getObjective());
		p.setComment(ctPatrol.getComment());
		p.setStartDate(ctPatrol.getStartDate());
		p.setEndDate(ctPatrol.getEndDate());
		
		initLegData(p.getFirstLeg(), ctPatrol);

		return p;
	}
	
}
