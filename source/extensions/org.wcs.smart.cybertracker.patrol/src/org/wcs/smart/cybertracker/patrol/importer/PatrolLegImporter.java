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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.importer.AbstractSmartImporter;
import org.wcs.smart.cybertracker.importer.SightsMultiObsUtil;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.model.data.Data.Sightings.S;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SharedUtils;

/**
 * Imports from {@link CyberTrackerPatrol} to {@link Patrol} object
 * and saves imported result as new patrol in database.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolLegImporter extends AbstractPatrolImporter {

	public boolean importData(Patrol patrol, CyberTrackerPatrol ctPatrol) {
		clearWarning();

		if (ctPatrol.getPatrolTransportType() == null) {
			if(!fixTransportError(ctPatrol))
				return false;
		}
		
		Session session = HibernateManager.openSession(new WaypointAttachmentInterceptor());
		try {
			session.beginTransaction();
			
			patrol = CyberTrackerHibernateManager.fetchByUuid(Patrol.class, patrol.getUuid(), session);
			if (patrol.getPatrolType() != ctPatrol.getPatrolType()) {
				CyberTrackerPlugIn.displayError(Messages.PatrolLegImporter_TypeError_Title, MessageFormat.format(Messages.PatrolLegImporter_TypeError_Message, ctPatrol.getPatrolType().getGuiName(Locale.getDefault()), patrol.getPatrolType().getGuiName(Locale.getDefault())), null);
				return false;
			}
			

			PatrolLeg tmpLeg = new PatrolLeg();
			initLegData(tmpLeg, ctPatrol, session);
			if (!checkDuplicate(ctPatrol, tmpLeg, patrol, session)){
				return false;
			}

			List<String> memberOverlaps = validateMemberOverlaping(patrol, ctPatrol);
			if (!memberOverlaps.isEmpty()) {
				String msg = ""; //$NON-NLS-1$
				for (Iterator<String> i = memberOverlaps.iterator(); i.hasNext();) {
					msg += i.next();
					if (i.hasNext())
						msg += "\n"; //$NON-NLS-1$
				}
				CyberTrackerPlugIn.displayError(Messages.PatrolLegImporter_MemberOverlapError_Title, msg, null);
				return false;	
			}
			
			PatrolLeg leg = patrol.addLeg();
			initLegData(leg, ctPatrol, session);
		
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
			
			if (leg.getLeader() == null){
				if (!fixLeaderError(leg, ctPatrol, session)){
					return false;
				}
			}
			if (patrol.hasPilot() && leg.getPilot() == null){
				if (!fixPilotError(leg, ctPatrol, session)){
					return false;
				}
			}
			List<String> patrolMetaWarnings = reportPatrolMetaWarnings(patrol, ctPatrol);
			for (String warnMsg : patrolMetaWarnings) {
				addWarning(warnMsg);
			}
			
			List<S> sList = SightsMultiObsUtil.convertMultiObs(ctPatrol);
			for (S s : sList) {
				addObservations(leg, s, ctPatrol.getElementsMap(), session);
			}

			if (!displayWarnings(ctPatrol))
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
					SmartPlugIn.displayLog(Messages.PatrolLegImporter_ErrorDialog_Message, e);
				}
			});
			return false;
		}
		finally {
			if (session.getTransaction().isActive()){
				session.getTransaction().rollback();
			}
			session.close();
		}
	}

	private List<String> validateMemberOverlaping(Patrol patrol, CyberTrackerPatrol ctPatrol) {
		List<String> errors = new ArrayList<String>();
		if (patrol.getLegs() == null)
			return errors;
		Date ctStart = ctPatrol.getStartDate();
		Date ctEnd = ctPatrol.getEndDate();
		for (PatrolLeg leg : patrol.getLegs()) {
			//ensure that legs overlap in time
			Date legStart = SharedUtils.getDatePart(leg.getStartDate(), false);
			Date legEnd = SharedUtils.getDatePart(leg.getEndDate(), false);
			if (leg.getPatrolLegDays() != null) {
				for (PatrolLegDay pld : leg.getPatrolLegDays()) {
					Date date = SharedUtils.getDatePart(pld.getDate(), false);
					if (date.equals(legStart))
						legStart = AbstractSmartImporter.combine(legStart, pld.getStartTime());
					if (date.equals(legEnd))
						legEnd = AbstractSmartImporter.combine(legEnd, pld.getEndTime());
				}
			}
			
			if (legStart.compareTo(ctEnd) <= 0 && legEnd.compareTo(ctStart) >= 0) {
				for (Employee ctMember : ctPatrol.getMembers()) {
					for (PatrolLegMember plm : leg.getMembers()) {
						if (plm.getMember().equals(ctMember)) {
							errors.add(MessageFormat.format(Messages.PatrolLegImporter_MemberOverlapError_Message, SmartLabelProvider.getFullLabel(ctMember), leg.getId()));
						}
					}
				}
			}
		}
		return errors;
	}

	//ensures that gap between dates is less than a day
	//(in this case we will not have a gap after adding leg to the patrol)
	private boolean isValidTimeDelta(Date from, Date to) {
		from = SharedUtils.getDatePart(from, false);
		to = SharedUtils.getDatePart(to, false);
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
	
	private String labelFor(NamedItem item) {
		return item != null ? item.getName() : ""; //$NON-NLS-1$
	}

	private boolean equal(NamedItem o1, NamedItem o2) {
		if (o1 == null || o2 == null)
			return o1 == o2;
		return o1.getUuid().equals(o2.getUuid());
	}
	
	
	protected boolean checkDuplicate(final CyberTrackerPatrol ctPatrol, 
			PatrolLeg patrolLeg,
			Patrol importTo,
			final Session session){
		
		Criteria c = session.createCriteria(PatrolLeg.class);
		//search for a leg with the samte start date, end date and transport type
		c.add(Restrictions.eq("patrol", importTo)); //$NON-NLS-1$
		c.add(Restrictions.eq("startDate", patrolLeg.getStartDate())); //$NON-NLS-1$
		c.add(Restrictions.eq("endDate", patrolLeg.getEndDate())); //$NON-NLS-1$
		c.add(Restrictions.eq("type", patrolLeg.getType())); //$NON-NLS-1$
		
		@SuppressWarnings("unchecked")
		List<PatrolLeg> patrols = c.list(); 
		
		if (patrols.size() > 0){
			final StringBuilder smartPatrols = new StringBuilder();
			for (PatrolLeg p : patrols){
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
						Messages.PatrolLegImporter_DuplicateDialogTitle, 
						MessageFormat.format(Messages.PatrolLegImporter_DuplicateMessage, new Object[]{getPatrolIdentifier(ctPatrol), smartPatrols.toString()})
						);
				}});
			return ret[0];
		}
		
		return true;
	}
}
