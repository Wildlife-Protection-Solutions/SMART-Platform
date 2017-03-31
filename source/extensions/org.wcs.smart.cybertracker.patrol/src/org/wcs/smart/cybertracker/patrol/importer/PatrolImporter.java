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
package org.wcs.smart.cybertracker.patrol.importer;

import java.sql.Time;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;

/**
 * Imports from {@link CyberTrackerPatrol} to {@link Patrol} object
 * and saves imported result as new patrol in database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolImporter extends AbstractPatrolImporter {
	
	public Patrol importData(CyberTrackerPatrol ctPatrol) {
		clearWarning();
		
		for (String warning : ctPatrol.getWarnings()) {
			addWarning(warning);
		}

		if (ctPatrol.getPatrolTransportType() == null) {
			if(!fixTransportError(ctPatrol))
				return null;
		}
		Patrol patrol = null;
		Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		try {
			session.beginTransaction();
			patrol = buildPatrol(ctPatrol, session);
			
			//check if duplicate of existing patrol
			if (!checkDuplicate(ctPatrol, patrol, session)){
				return null;
			}
			
			PatrolLeg firstLeg = patrol.getFirstLeg();
			if (firstLeg.getLeader() == null){
				if (!fixLeaderError(firstLeg, ctPatrol, session)){
					return null;
				}
			}
			if (patrol.hasPilot() && firstLeg.getPilot() == null){
				if (!fixPilotError(firstLeg, ctPatrol, session)){
					return null;
				}
			}
			List<S> sList = extractAndPreProcessSights(ctPatrol);
			RestTimeMap restMap = extractRestTime(sList, ctPatrol.getElementsMap());
			sList = restMap.excludePauseS(sList);
			for (S s : sList) {
				addObservations(firstLeg, s, ctPatrol.getElementsMap(), session);
			}
			for (PatrolLegDay pld : firstLeg.getPatrolLegDays()) {
				pld.setRestMinutes(restMap.getRestMinutes(pld.getDate()));
			}
			
			if (!displayWarnings(ctPatrol))
				return null;

			//resize images if required
			processImages(patrol.getLegs(),session);
			
			PatrolHibernateManager.savePatrol(patrol, session, true);
			session.getTransaction().commit();
		} catch (final Exception e) {
			session.getTransaction().rollback();
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					SmartPlugIn.displayLog(Messages.PatrolImporter_Save_Error, e);
				}
			});
			return null;
		}finally {
			session.close();
		}
		//fire events
		PatrolEventManager.getInstance().patrolAdded(patrol);
		return patrol;
	}
	
	private Patrol buildPatrol(CyberTrackerPatrol ctPatrol, Session session) {
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
		
		initLegData(p.getFirstLeg(), ctPatrol, session);

		return p;
	}
	
	
	protected boolean checkDuplicate(final CyberTrackerPatrol ctPatrol, Patrol patrol, final Session session){
		
		Criteria c = session.createCriteria(Patrol.class);
		c.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())); //$NON-NLS-1$
		c.add(Restrictions.eq("startDate", patrol.getStartDate())); //$NON-NLS-1$
		c.add(Restrictions.eq("endDate", patrol.getEndDate())); //$NON-NLS-1$
		c.add(Restrictions.eq("patrolType", patrol.getPatrolType())); //$NON-NLS-1$
		if (patrol.getStation() == null){
			c.add(Restrictions.isNull("station")); //$NON-NLS-1$
		}else{
			c.add(Restrictions.eq("station", patrol.getStation())); //$NON-NLS-1$
		}
		if (patrol.getTeam() == null){
			c.add(Restrictions.isNull("team")); //$NON-NLS-1$
		}else{
			c.add(Restrictions.eq("team", patrol.getTeam())); //$NON-NLS-1$
		}
		@SuppressWarnings("unchecked")
		List<Patrol> patrols = c.list(); 

		boolean hasDuplicate = false;
		if (patrols.size() > 0) {
			//ensure that it is really a duplicate
			for (Patrol p : patrols) {
				if (isLegTimeMatch(p, patrol)) {
					hasDuplicate = true;
					break;
				}
			}
		}
		
		if (hasDuplicate) {
			final StringBuilder smartPatrols = new StringBuilder();
			for (Patrol p : patrols){
				smartPatrols.append(p.getId());
				smartPatrols.append(", "); //$NON-NLS-1$
			}
			smartPatrols.deleteCharAt(smartPatrols.length() - 1);
			smartPatrols.deleteCharAt(smartPatrols.length() - 1);
			if (smartPatrols.length() > 100){
				smartPatrols.delete(100, smartPatrols.length());
			}
			final boolean[] ret = new boolean[]{false};
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					ret[0] = MessageDialog.openConfirm(Display.getDefault().getActiveShell(),
						Messages.PatrolImporter_ImportDialogTitle, 
						MessageFormat.format(Messages.PatrolImporter_DuplicateMessage, new Object[]{getPatrolIdentifier(ctPatrol), smartPatrols.toString()})
						);
				}});
			return ret[0];
		}
		
		return true;
	}
	
	private boolean isLegTimeMatch(Patrol p1, Patrol p2) {
		List<PatrolLeg> legs1 = p1.getLegs();
		List<PatrolLeg> legs2 = p2.getLegs();
		if (legs1 == null && legs2 == null)
			return true;
		if (legs1 == null || legs2 == null)
			return false;
		if (legs1.size() != legs2.size())
			return false;
		
		for (int i = 0; i < legs1.size(); i++) {
			PatrolLeg l1 = legs1.get(i);
			PatrolLeg l2 = legs2.get(i);
			if (!isLegDayTimeMatch(l1, l2))
				return false;
		}
		return true;
	}

	private boolean isLegDayTimeMatch(PatrolLeg l1, PatrolLeg l2) {
		List<PatrolLegDay> days1 = l1.getPatrolLegDays();
		List<PatrolLegDay> days2 = l2.getPatrolLegDays();
		if (days1 == null && days2 == null)
			return true;
		if (days1 == null || days2 == null)
			return false;
		if (days1.size() != days2.size())
			return false;
		
		for (int i = 0; i < days1.size(); i++) {
			PatrolLegDay d1 = days1.get(i);
			PatrolLegDay d2 = days2.get(i);
			if (!isEqual(d1.getStartTime(), d2.getStartTime()) || !isEqual(d1.getEndTime(), d2.getEndTime()))
				return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private boolean isEqual(Time t1, Time t2) {
		if (t1 == null && t2 == null)
			return true;
		if (t1 == null || t2 == null)
			return false;
		return t1.getHours() == t2.getHours() && t1.getMinutes() == t2.getMinutes() && t1.getSeconds() == t2.getSeconds();
	}
}
