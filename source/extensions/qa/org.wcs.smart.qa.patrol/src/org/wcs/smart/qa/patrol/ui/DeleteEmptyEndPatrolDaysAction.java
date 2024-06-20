/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.patrol.internal.Messages;
import org.wcs.smart.qa.patrol.routine.EmptyEndPatrolDaysType;
import org.wcs.smart.qa.patrol.routine.PatrolDataProvider;
import org.wcs.smart.ui.properties.DialogConstants;

import com.ibm.icu.text.MessageFormat;

/**
 * Delete patrol waypoint action.  Applicable for
 * PatrolWaypointDataProvider.
 * 
 * @author Emily
 *
 */
public class DeleteEmptyEndPatrolDaysAction implements IQaAction {

	@Override
	public boolean doAction(List<QaError> items) {
		List<QaError> toProcess = new ArrayList<>();
		
		for (QaError e : items){
			if (e.getDataProviderId().equals(PatrolDataProvider.ID) && 
					e.getQaRoutine().getRoutineType().getId().equalsIgnoreCase(EmptyEndPatrolDaysType.ID)){
				toProcess.add(e);
				
			}
		}
		if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), DialogConstants.DELETE_BUTTON_TEXT, 
				Messages.DeleteEmptyEndPatrolDaysAction_ConfirmMessage)){
			return false;
		}
		
		Set<Patrol> modified = new HashSet<>();
		
		for (QaError item : toProcess){
			processItem(item, modified);
			
		}
		
		//fire patrol events
		for (Patrol d : modified){
			PatrolEventManager.getInstance().patrolSaved(d,true);
		}
		return true;
	}

	@Override
	public boolean supportsMultiple() {
		return true;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.action.patrol.removeemptyenddays"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return Messages.DeleteEmptyEndPatrolDaysAction_ActionName;
	}
	
	private void updateItem(QaError item, Session s, QaError.Status status, String message) {
		
		QaError dbitem = s.get(QaError.class, item.getUuid());
		if (dbitem == null) {
			dbitem = item;
		}
		
		if (s.getTransaction().isActive()) {
			
			
			dbitem.setStatus(status);
			dbitem.setFixMessage(message);
			
		}else {
			try {
				s.beginTransaction();
				
				dbitem.setStatus(status);
				dbitem.setFixMessage(message);
				s.getTransaction().commit();
			}catch (Exception ex) {
				QaPlugIn.displayLog(ex.getMessage(), ex);
			}	
		}
		
	}
	private void processItem(QaError item, Set<Patrol> modified) {
		
		List<LocalDate> toDelete = new ArrayList<>();
		TreeSet<LocalDate> alldates = new TreeSet<>((a,b) -> -a.compareTo(b));
		String patrolId = ""; //$NON-NLS-1$
		
		try(Session s = HibernateManager.openSession()){
			
			Patrol p = s.get(Patrol.class, item.getSourceId());
			if (p == null) {
				updateItem(item, s, QaError.Status.ERROR, Messages.DeleteEmptyEndPatrolDaysAction_PatrolNotFoundError);
				return;
			}
			patrolId = p.getId();
			
			//find the end empty patrol legs
			HashMap<LocalDate, Integer> obsCnt = new HashMap<>();
			
			for (PatrolLeg pl : p.getLegs()) {
				for (PatrolLegDay d : pl.getPatrolLegDays()) {
					alldates.add(d.getDate());
					int cnt = d.getWaypoints().size();
					if (!obsCnt.containsKey(d.getDate())) {
						obsCnt.put(d.getDate(), cnt);
					}else {
						obsCnt.put(d.getDate(), obsCnt.get(d.getDate()) + cnt);
					}
				}
			}
				
			 
			for(LocalDate d : alldates) {
				if (obsCnt.get(d) == 0) {
					toDelete.add(d);
				}else {
					break;
				}
			}
			
		} //end of db session
		
		
		if (toDelete.size() > 0) {
			if (toDelete.size() == alldates.size()) {
				//deleting everything confirm with user first
				if (!MessageDialog.openConfirm(Display.getDefault().getActiveShell(), DialogConstants.DELETE_BUTTON_TEXT, 
						MessageFormat.format(Messages.DeleteEmptyEndPatrolDaysAction_DeletePatrolConfirm, patrolId))){
					return ;
				}
				boolean isDeleted = false;
				try {
					isDeleted = PatrolManager.getInstance().deletePatrol(item.getSourceId(), true, new NullProgressMonitor());
				} catch (Exception e1) {
					QaPlugIn.log(e1.getMessage(), e1);
					isDeleted = false;
					e1.printStackTrace();
				}
				try(Session session = HibernateManager.openSession()){
					if (isDeleted) {
						updateItem(item, session, QaError.Status.FIXED, Messages.DeleteEmptyEndPatrolDaysAction_PatrolDeletedStatus);
					}else {
						updateItem(item, session, QaError.Status.ERROR, Messages.DeleteEmptyEndPatrolDaysAction_PatrolDeletedErrorStatus);
					}
				}
			} else {
				try(Session session = HibernateManager.openSession()){
				
					session.beginTransaction();
					try {
						Patrol p = session.get(Patrol.class, item.getSourceId());
						
						List<PatrolLeg> ldelete = new ArrayList<>();
						LocalDate newPEnd = p.getStartDate();
						for (PatrolLeg pl : p.getLegs()) {
							List<PatrolLegDay> plddelete = new ArrayList<>();
							
							LocalDate newEnd = pl.getStartDate();
							for (PatrolLegDay pld : pl.getPatrolLegDays()) {
								if (toDelete.contains(pld.getDate())) {
									//delete me
									plddelete.add(pld);
								}else {
									if (pld.getDate().isAfter(newEnd)) {
										newEnd = pld.getDate();
									}
									if (pld.getDate().isAfter(newPEnd)) {
										newPEnd = pld.getDate();
									}
								}
							}
							
							pl.getPatrolLegDays().removeAll(plddelete);
							if (pl.getPatrolLegDays().isEmpty()) {
								//delete leg
								ldelete.add(pl);
							}else {
								//update end date
								pl.setEndDate(newEnd);
							}
						}
						p.getLegs().removeAll(ldelete);
						p.setEndDate(newPEnd);
						//update patrol end date
						
						modified.add(p);
						
						updateItem(item, session, QaError.Status.FIXED, Messages.DeleteEmptyEndPatrolDaysAction_EmptyDaysRemovedStatus);

						session.getTransaction().commit();
					}catch (Exception ex) {
						session.getTransaction().rollback();
						QaPlugIn.displayLog(MessageFormat.format(Messages.DeleteEmptyEndPatrolDaysAction_RemoteError, patrolId, ex.getMessage()), ex);
						
						updateItem(item, session, QaError.Status.ERROR, Messages.DeleteEmptyEndPatrolDaysAction_GeneralError + ex.getMessage());						
					}
				}
			}
			
		}
	}
}
