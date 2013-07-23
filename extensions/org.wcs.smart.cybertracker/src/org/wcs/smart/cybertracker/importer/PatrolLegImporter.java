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
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;

/**
 * Imports from {@link CyberTrackerPatrol} to {@link Patrol} object
 * and saves imported result as new patrol in database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolLegImporter extends SmartImporter {

	public boolean importData(Patrol patrol, CyberTrackerPatrol ctPatrol) {
		clearWarning();
		Session session = HibernateManager.openSession();
		try {
			session.beginTransaction();
			patrol = CyberTrackerHibernateManager.fetchByUuid(Patrol.class, patrol.getUuid(), session);
			PatrolLeg leg = patrol.addLeg();
			initLegData(leg, ctPatrol);
			if (leg.getType() == null) {
				if(!fixTransportError(leg, ctPatrol, session))
					return false;
			}
			leg.createLegDays();
			
			for (S s : ctPatrol.getPatrolData()) {
				addObservations(leg, s, ctPatrol.getElementsMap(), session);
			}
			displayWarnings();

			PatrolHibernateManager.savePatrol(patrol, session, true);
			session.getTransaction().commit();
			PatrolEventManager.getInstance().patrolSaved(patrol, true);
			return true;
		} catch (final Exception e) {
			session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.PatrolLegImporter_ErrorDialog_Message, e);
				}
			});
			return false;
		}
		finally {
			session.close();
		}
		
	}	
}
