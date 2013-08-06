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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.util.SmartUtils;

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

			if (patrol.getStartDate().getTime() > leg.getStartDate().getTime()) {
				if (!isValidTimeDelta(leg.getEndDate(), patrol.getStartDate()))
					addWarning(MessageFormat.format(Messages.PatrolLegImporter_Warn_TimeGap_Start, DateFormat.getDateInstance(DateFormat.MEDIUM).format(patrol.getStartDate()), DateFormat.getDateInstance(DateFormat.MEDIUM).format(leg.getEndDate())));
				patrol.setStartDate(leg.getStartDate());
				
			}

			if (patrol.getEndDate().getTime() < leg.getEndDate().getTime()) {
				if (!isValidTimeDelta(patrol.getEndDate(), leg.getStartDate()))
					addWarning(MessageFormat.format(Messages.PatrolLegImporter_Warn_TimeGap_End, DateFormat.getDateInstance(DateFormat.MEDIUM).format(patrol.getEndDate()), DateFormat.getDateInstance(DateFormat.MEDIUM).format(leg.getStartDate())));
				patrol.setEndDate(leg.getEndDate());
			}
			
			List<String> patrolMetaWarnings = reportPatrolMetaWarnings(patrol, ctPatrol);
			for (String warnMsg : patrolMetaWarnings) {
				addWarning(warnMsg);
			}
			
			for (S s : ctPatrol.getPatrolData()) {
				addObservations(leg, s, ctPatrol.getElementsMap(), session);
			}

			if (!displayWarnings())
				return false;

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

	//ensures that gap between dates is less than a day
	//(in this case we will not have a gap after adding leg to the patrol)
	private boolean isValidTimeDelta(Date from, Date to) {
		from = SmartUtils.getDatePart(from, false);
		to = SmartUtils.getDatePart(to, false);
		long delta = to.getTime() - from.getTime();
		return delta <= 1000 * 60 * 60 * 24; //more that a day
	}
	
	private List<String> reportPatrolMetaWarnings(Patrol patrol, CyberTrackerPatrol ctPatrol) {
		List<String> result = new ArrayList<String>();

		if (patrol.isArmed() != ctPatrol.isArmed())
			result.add(MessageFormat.format(Messages.PatrolLegImporter_MetaWarning_IsArmed, armedTextValue(patrol.isArmed()), armedTextValue(ctPatrol.isArmed())));

		if (!equal(patrol.getTeam(), ctPatrol.getTeam()))
			result.add(MessageFormat.format(Messages.PatrolLegImporter_MetaWarning_Team, labelFor(patrol.getTeam()), labelFor(ctPatrol.getTeam())));

		if (!equal(patrol.getStation(), ctPatrol.getStation()))
			result.add(MessageFormat.format(Messages.PatrolLegImporter_MetaWarning_Station, labelFor(patrol.getStation()), labelFor(ctPatrol.getStation())));

		if (!equal(patrol.getMandate(), ctPatrol.getMandate()))
			result.add(MessageFormat.format(Messages.PatrolLegImporter_MetaWarning_Mandate, labelFor(patrol.getMandate()), labelFor(ctPatrol.getMandate())));
		
		return result;
	}
	
	private String armedTextValue(boolean isArmed) {
		return isArmed ? Messages.CTPatrolTableCellLabelProvider_Armed_Yes : Messages.CTPatrolTableCellLabelProvider_Armed_No;
	}
	
	private String labelFor(SimpleListItem item) {
		return item != null ? item.getName() : ""; //$NON-NLS-1$
	}

	private boolean equal(SimpleListItem o1, SimpleListItem o2) {
		if (o1 == null || o2 == null)
			return o1 == o2;
		return Arrays.equals(o1.getUuid(), o2.getUuid());
	}
	
}
