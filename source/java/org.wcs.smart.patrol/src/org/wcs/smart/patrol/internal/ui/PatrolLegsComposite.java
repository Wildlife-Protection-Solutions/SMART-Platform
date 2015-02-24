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
package org.wcs.smart.patrol.internal.ui;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.editpatrol.EditPatrolDateLegsDialog;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol item composite that modifies the patrol legs.  Allows users
 * to add, remove, and change patrol legs.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegsComposite extends PatrolItemComposite{

	private DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
	
	private static final String START_INFO_LABEL = Messages.PatrolLegsComposite_PatrolStart_Label;
	private static final String END_INFO_LABEL = Messages.PatrolLegsComposite_PatrolEnd_Label;
	
	private Label lblDateInfo;
	private PatrolLegTable patrolLegViewer;
	private Patrol patrol;
	
	private ArrayList<PatrolLeg> legs;
	
	private List<PatrolTransportType> typeOps ; 
	private List<Employee> allEmployes; 
	
	private Date patrolStartDate;
	private Date patrolEndDate;
	private boolean canEditDates = false;
	
	private Session session;
	
	private Link lnkEditDate;
	private Composite main;
	

	/**
	 * Creates a new patrol legs composite
	 * @param canEditDates true if the patrol dates can be changed, false if only legs can be modified
	 * @param currentSession - database session
	 */
	public PatrolLegsComposite(boolean canEditDates) {
		this.canEditDates = canEditDates;
	}
	
	/**
	 * 
	 * @return the initial size for the dialog or null
	 * if use default
	 */
	public Point getInitialSize(){
		try{
			int width = (int) (getShell().getBounds().width * 0.6);
			if (width < 500){
				width = 500;
			}
			int height = (int) (getShell().getBounds().height * 0.6);
			if (height < 350){
				height = 350;
			}
			return new Point(width, height);
		}catch (Exception ex){}
		return null;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	@Override
	public Composite createComponent(Composite parent, int style) {
		main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		if (canEditDates){
			Composite tmp = new Composite(main, SWT.NONE);
			tmp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
			tmp.setLayout(new GridLayout(2, false));
			lblDateInfo = new Label(tmp, SWT.NONE);
			lblDateInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			lnkEditDate = new Link(tmp, SWT.NONE);
			lnkEditDate.setText("<a>" + PatrolUtils.EDIT_LINK_TEXT + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
			lnkEditDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			lnkEditDate.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					EditPatrolDateLegsDialog dialog = new EditPatrolDateLegsDialog(getShell(), patrolStartDate, patrolEndDate);
					if (dialog.open() == Window.OK){
						patrolStartDate = dialog.getStartDate();
						patrolEndDate = dialog.getEndDate();
						lblDateInfo.setText(START_INFO_LABEL + ": " + dateFormatter.format(patrolStartDate) + "  " + END_INFO_LABEL + ": " + dateFormatter.format(patrolEndDate) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
						fireChangeListeners();
					}
				}
			});
		}else{
			lblDateInfo = new Label(main, SWT.NONE);
			lblDateInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		patrolLegViewer = new PatrolLegTable();
		patrolLegViewer.createTable(main);
		
		Composite buttonPanel = new Composite(main, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(5, false));
		
		final Button btnChangeTransport = new Button(buttonPanel, SWT.PUSH);
		btnChangeTransport.setText(Messages.PatrolLegsComposite_ChangeTransport_Button);
		btnChangeTransport.setSelection(false);
		btnChangeTransport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolTransportChangeDialog leaderDialg = new PatrolTransportChangeDialog(getShell(), 
						pl, legs, session);
				if (leaderDialg.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		final Button btnAddLeg = new Button(buttonPanel, SWT.PUSH);
		btnAddLeg.setText(Messages.PatrolLegsComposite_ChangeLeader_Button);
		btnAddLeg.setSelection(false);
		btnAddLeg.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolLegLeaderChangeDialog leaderDialg = new PatrolLegLeaderChangeDialog(getShell(),
						pl, legs);
				if (leaderDialg.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		
		final Button btnSplit = new Button(buttonPanel, SWT.PUSH);
		btnSplit.setText(Messages.PatrolLegsComposite_SplitPatrol_Button);
		btnSplit.setSelection(false);
		btnSplit.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolLegSplitDialog splitDialog = new PatrolLegSplitDialog(getShell(), pl,
						typeOps, legs);
				if (splitDialog.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		final Button btnRemoveLeg = new Button(buttonPanel, SWT.PUSH);
		btnRemoveLeg.setText(Messages.PatrolLegsComposite_RemoveLeg_Button);
		btnRemoveLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				if (MessageDialog.openConfirm(getShell(), Messages.PatrolLegsComposite_DeleteLeg_ConfirmDialog_Title, Messages.PatrolLegsComposite_DeleteLeg_ConfirmDialog_Message)){
					removeLeg();	
				}
				
			}
		});
		
		final Button btnEditLeg = new Button(buttonPanel, SWT.PUSH);
		btnEditLeg.setText(Messages.PatrolLegsComposite_EditLeg_Button);
		btnEditLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				PatrolLeg toEdit = (PatrolLeg)((IStructuredSelection)patrolLegViewer.getSelection()).getFirstElement();
				EditPatrolLegDialog patrolLegDialog = new EditPatrolLegDialog(getShell(), toEdit, allEmployes, typeOps, patrolStartDate, patrolEndDate);
				if (patrolLegDialog.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}
		});
		
		patrolLegViewer.getTable().addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isEmpty =  ((IStructuredSelection)patrolLegViewer.getSelection()).isEmpty();
				btnChangeTransport.setEnabled(!isEmpty);
				btnAddLeg.setEnabled(!isEmpty);
				btnSplit.setEnabled(!isEmpty);
				btnRemoveLeg.setEnabled(!isEmpty && legs.size() > 1);
				btnEditLeg.setEnabled(!isEmpty);
			}
		});
		
		btnAddLeg.setEnabled(false );
		btnSplit.setEnabled(false );
		btnRemoveLeg.setEnabled(false );
		btnEditLeg.setEnabled( false );
		btnChangeTransport.setEnabled(false);
		
		return main;
	}

	private Shell getShell(){
		return main.getShell();
	}

	private void sortAndRefresh(){
		Collections.sort(legs, new Comparator<PatrolLeg>() {
			@Override
			public int compare(PatrolLeg o1, PatrolLeg o2) {
				int x =  o1.getStartDate().compareTo(o2.getStartDate());
				if (x == 0){
					return Collator.getInstance().compare(o1.getId(), o2.getId());
				}
				return x;
			}
		});
		patrolLegViewer.refresh();
		patrolLegViewer.getTable().setSelection(null);
	}
	
	private void removeLeg(){
		PatrolLeg toDelete = (PatrolLeg)((IStructuredSelection)patrolLegViewer.getSelection()).getFirstElement();
		toDelete.setPatrol(null);
		legs.remove(toDelete);
		sortAndRefresh();
		fireChangeListeners();
		
	}
	
	/**
	 * Clones all the existing patrol legs, and updates the ui components
	 * to contain the data provided by the patrol.
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	@Override
	public void setValues(Patrol p, Session session) {
		this.session = session;
		this.patrol = p;
		this.legs = new ArrayList<PatrolLeg>();
		
		//clone the legs
		for (PatrolLeg leg : p.getLegs()){
			PatrolLeg tmpLeg = clonePatrolLeg(leg);
			
			//start time
			Date date = null;
			if (leg.getPatrolLegDays().size() > 0){
				date = SmartUtils.combineDateTime(leg.getStartDate(), leg.getPatrolLegDays().get(0).getStartTime());	
			}else{
				date = SmartUtils.getDatePart(leg.getStartDate(), false);
			}
			tmpLeg.setStartDate(date);
			
			//end time
			date = null;
			if (leg.getPatrolLegDays().size() > 0){
				date = SmartUtils.combineDateTime(leg.getEndDate(), leg.getPatrolLegDays().get(leg.getPatrolLegDays().size() - 1).getEndTime());	
			}else{
				date = SmartUtils.getDatePart(leg.getEndDate(), true);
			}
			tmpLeg.setEndDate(date);
			
			this.legs.add(tmpLeg);
		}
		
		session.beginTransaction();
		try{
			typeOps = PatrolHibernateManager.getActivePatrolTransporationTypes(patrol.getConservationArea(), session, this.patrol.getPatrolType()); 
			allEmployes = PatrolHibernateManager.getActiveEmployees(patrol.getConservationArea(), session);
			session.getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.PatrolLegsComposite_Error_LoadingPatrolTypes, ex);
			session.getTransaction().rollback();
		}
		patrolLegViewer.setInput(legs);
		
		this.patrolStartDate = (Date)patrol.getStartDate().clone();
		this.patrolEndDate = (Date)patrol.getEndDate().clone();
		
		lblDateInfo.setText(START_INFO_LABEL + ": " + dateFormatter.format(patrolStartDate) + "  " + END_INFO_LABEL + ": " + dateFormatter.format(patrolEndDate) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		patrolLegViewer.showPilotColum(patrol.hasPilot());
		sortAndRefresh();
	}

	private PatrolLeg clonePatrolLeg(PatrolLeg leg){
		PatrolLeg tmpLeg = new PatrolLeg();
		tmpLeg.setPatrol(leg.getPatrol());
		tmpLeg.setId(leg.getId());
		
		//start time
		tmpLeg.setStartDate(leg.getStartDate());
		tmpLeg.setEndDate(leg.getEndDate());
		//type
		tmpLeg.setType(leg.getType());
		//members
		tmpLeg.setMembers(new ArrayList<PatrolLegMember>());
		for (PatrolLegMember mem : leg.getMembers()){
			PatrolLegMember clone = mem.clone();
			clone.setPatrolLeg(tmpLeg);
			tmpLeg.getMembers().add(clone);
		}
		//uuid
		tmpLeg.setUuid(leg.getUuid());
		return tmpLeg;
	}
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public boolean updatePatrol(Patrol p, Session session) {
		if (this.canEditDates){
			p.setStartDate(patrolStartDate);
			p.setEndDate(patrolEndDate);
		}
		HashSet<PatrolLeg> currentLegs = new HashSet<PatrolLeg>();
		if (p.getLegs() == null){
			p.setLegs(new ArrayList<PatrolLeg>());
		}else{
			//current legs
			for (PatrolLeg leg: p.getLegs()){
				currentLegs.add(leg);
			}
		}
		
		ArrayList<PatrolLeg> allLegs = new ArrayList<PatrolLeg>();
		allLegs.addAll(legs);
		session.flush();
		for (Iterator<PatrolLeg> iterator = allLegs.iterator(); iterator.hasNext();) {
			PatrolLeg updatedLeg = (PatrolLeg) iterator.next();
			if (updatedLeg.getUuid() != null){
				//find in the existing
				for (PatrolLeg existing : currentLegs){
					if (existing.getUuid() != null && Arrays.equals(updatedLeg.getUuid(), existing.getUuid())){
						//update existing leg
						existing.setId(updatedLeg.getId());
						existing.setEndDate(updatedLeg.getEndDate());
						existing.setStartDate(updatedLeg.getStartDate());
						existing.setType(updatedLeg.getType());
						
						//remove existing members
						for (PatrolLegMember m : existing.getMembers()){
							m.setId(null);
						}
						existing.getMembers().clear();
						session.flush();
						
						//replace with new members
						for (PatrolLegMember newmember : updatedLeg.getMembers()) {
							PatrolLegMember m = newmember.clone();
							m.setPatrolLeg(existing);
							existing.getMembers().add(m);
						}
						
						currentLegs.remove(existing);
						iterator.remove();
						break;
					}
				}
			}
		}
		
		//new legs
		for (Iterator<PatrolLeg> iterator = allLegs.iterator(); iterator.hasNext();) {
			PatrolLeg newLeg = (PatrolLeg) iterator.next();
			
			PatrolLeg clone = clonePatrolLeg(newLeg);
			clone.setPatrol(p);
			p.getLegs().add(clone);
		}
		
		//legs no longer used; these must be removed
		for (PatrolLeg toRemove: currentLegs){
			//we need to make sure we delete all waypoints here
			if(toRemove.getPatrolLegDays() != null){
				for (PatrolLegDay pld : toRemove.getPatrolLegDays()){
					if (pld.getWaypoints() != null){
						for (PatrolWaypoint pw : pld.getWaypoints()){
							session.delete(pw.getWaypoint());
						}
					}
				}
			}
			toRemove.setPatrol(null);
			p.getLegs().remove(toRemove);
		}
		
		//create leg days
		p.createLegDays(session);
		return true;
			
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.PatrolLegsComposite_Title;
	}

	public int getLegCount(){
		if (legs == null){ return 0; }
		return legs.size();
	}
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getErrorMessage()
	 */
	@Override
	public String getErrorMessage(){
		
		/* ensure each patrol leg between the patrol start and end */
		Date pstart = SmartUtils.getDatePart(patrolStartDate, false);
		Date pend = SmartUtils.getDatePart(patrolEndDate, true);
		
		for (Iterator<?> iterator = legs.iterator(); iterator.hasNext();) {
			PatrolLeg legA = (PatrolLeg) iterator.next();
			
			Date legstart = SmartUtils.getDatePart(legA.getStartDate(), false);
			Date legend = SmartUtils.getDatePart(legA.getEndDate(), true);
			
			if (legstart.after(pend)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_A, new Object[]{legA.getId()});
			}
			if (legstart.before(pstart)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_B, new Object[]{legA.getId()});
			}
			
			if (legend.after(pend)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_C, new Object[]{legA.getId()});
			}
			if (legend.before(pstart)){
				return MessageFormat.format(Messages.PatrolLegsComposite_LegError_D, new Object[]{legA.getId()});
			}
		}
		
		/* Ensure that members are not in two places at the same time */
		for (Iterator<?> iterator = legs.iterator(); iterator.hasNext();) {
			PatrolLeg legA = (PatrolLeg) iterator.next();
		
			for (Iterator<?> iterator2 = legs.iterator(); iterator2.hasNext();) {
				PatrolLeg legB = (PatrolLeg) iterator2.next();
				if (legA.equals(legB)){
					continue;
				}else if(! ( (legB.getEndDate().before(legA.getStartDate()) || legB.getEndDate().equals(legA.getStartDate())) ||
						(legB.getStartDate().after(legA.getEndDate()) ||legB.getStartDate().equals(legA.getEndDate())   ) )){
					
					//legs overlap ensure members don't
					HashSet<Employee> bMembers = new HashSet<Employee>();
					for (PatrolLegMember member : legB.getMembers()){
						bMembers.add(member.getMember());
					}
					for (PatrolLegMember member : legA.getMembers()){
						if (bMembers.contains(member.getMember())){
							return MessageFormat.format(Messages.PatrolLegsComposite_LegError_E, 
								new Object[]{member.getMember().getFullLabel(), legA.getId(), legB.getId() });
						}
					}
				}
			}
		}
		
		/* ensure there is at least one leg for each day in the patrol */
		Calendar calStart = SmartUtils.convertDate(patrolStartDate);
		calStart.set(Calendar.HOUR, 0);
		calStart.set(Calendar.MINUTE, 0);
		calStart.set(Calendar.SECOND, 0);
		calStart.set(Calendar.MILLISECOND, 0);
				
		Calendar calEnd = SmartUtils.convertDate(patrolEndDate);
		
		while (calStart.before(calEnd) || calStart.equals(calEnd)){
			boolean found = false;
			for (Iterator<?> iterator = legs.iterator(); iterator.hasNext();) {
				PatrolLeg leg = (PatrolLeg) iterator.next();
				Date legStart = SmartUtils.getDatePart(leg.getStartDate(), false);
				Date legEnd = SmartUtils.getDatePart(leg.getEndDate(), true);
				
				if ( (calStart.getTime().before(legEnd) || calStart.getTime().equals(legEnd)) && 
						(calStart.getTime().after(legStart) ||calStart.getTime().equals(legStart)) ){
					found = true;
					break;
				}
				
			}
			if (!found){
				return MessageFormat.format(Messages.PatrolLegsComposite_Error_MissingLegDay, new Object[]{ DateFormat.getDateInstance(DateFormat.MEDIUM).format(calStart.getTime()) });
			}			
			calStart.add(Calendar.DAY_OF_MONTH, 1);
		}		
		return null;		
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}
}
