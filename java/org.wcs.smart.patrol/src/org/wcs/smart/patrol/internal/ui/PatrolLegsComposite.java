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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.editpatrol.EditPatrolDateLegsDialog;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol item composite that modifies the patrol legs.  Allows users
 * to add, remove, and change patrol legs.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolLegsComposite extends PatrolItemComposite{

	private static final DateFormat DATE_FORMATTER = DateFormat.getDateInstance(DateFormat.MEDIUM);
	
	private Label lblDateInfo;
	private PatrolLegTable patrolLegViewer;
	private Patrol patrol;
	
	private WritableList legs;
	
	private List<PatrolTransportType> typeOps ; 
	private List<Employee> allEmployes; 
	
	private Date patrolStartDate;
	private Date patrolEndDate;
	private boolean canEditDates = false;
	
	private Link lnkEditDate;

	/**
	 * Creates a new patrol legs composite
	 * @param canEditDates true if the patrol dates can be changed, false if only legs can be modified
	 */
	public PatrolLegsComposite(boolean canEditDates) {
		this.canEditDates = canEditDates;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	@Override
	public Composite createComponent(Composite parent, int style) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		if (canEditDates){
			Composite tmp = new Composite(main, SWT.NONE);
			tmp.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
			tmp.setLayout(new GridLayout(2, false));
			lblDateInfo = new Label(tmp, SWT.NONE);
			lblDateInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			lnkEditDate = new Link(tmp, SWT.NONE);
			lnkEditDate.setText("<a>edit...</a>");
			lnkEditDate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			lnkEditDate.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e) {
					EditPatrolDateLegsDialog dialog = new EditPatrolDateLegsDialog(getShell(), patrolStartDate, patrolEndDate);
					if (dialog.open() == Window.OK){
						patrolStartDate = dialog.getStartDate();
						patrolEndDate = dialog.getEndDate();
						lblDateInfo.setText( "Patrol Start: " + DATE_FORMATTER.format(patrolStartDate) + "  Patrol End: " + DATE_FORMATTER.format(patrolEndDate) );
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
		buttonPanel.setLayout(new GridLayout(4, false));
		
		final Button btnAddLeg = new Button(buttonPanel, SWT.PUSH);
		btnAddLeg.setText("Change of Leader");
		btnAddLeg.setSelection(false);
		btnAddLeg.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolLegLeaderChangeDialog leaderDialg = new PatrolLegLeaderChangeDialog(getShell(), pl, legs);
				leaderDialg.open();
				sortAndRefresh();
				fireChangeListeners();
			}

		});
		
		
		final Button btnSplit = new Button(buttonPanel, SWT.PUSH);
		btnSplit.setText("Patrol Split");
		btnSplit.setSelection(false);
		btnSplit.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				PatrolLeg pl =  (PatrolLeg)patrolLegViewer.getSelection().getFirstElement();
				if (pl == null){
					return;
				}
				PatrolLegSplitDialog splitDialog = new PatrolLegSplitDialog(getShell(), pl, typeOps, legs);
				if (splitDialog.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}

		});
		
		final Button btnRemoveLeg = new Button(buttonPanel, SWT.PUSH);
		btnRemoveLeg.setText("Remove Leg");
		btnRemoveLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				if (MessageDialog.openConfirm(getShell(), "Delete Leg", "Are you sure you want to delete this leg?  This action cannot be undone.")){
					removeLeg();	
				}
				
			}
		});
		
		final Button btnEditLeg = new Button(buttonPanel, SWT.PUSH);
		btnEditLeg.setText("Edit Leg");
		btnEditLeg.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e){
				PatrolLeg toEdit = (PatrolLeg)((IStructuredSelection)patrolLegViewer.getSelection()).getFirstElement();
				EditPatrolLegDialog patrolLegDialog = new EditPatrolLegDialog(Display.getDefault().getActiveShell(), toEdit, allEmployes, typeOps, patrolStartDate, patrolEndDate);
				if (patrolLegDialog.open() == Window.OK){
					sortAndRefresh();
					fireChangeListeners();
				}
			}
		});
		
		patrolLegViewer.getTable().addSelectionChangedListener(new ISelectionChangedListener() {			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnAddLeg.setEnabled(! ((IStructuredSelection)patrolLegViewer.getSelection()).isEmpty());
				btnSplit.setEnabled(! ((IStructuredSelection)patrolLegViewer.getSelection()).isEmpty());
				btnRemoveLeg.setEnabled(! ((IStructuredSelection)patrolLegViewer.getSelection()).isEmpty() && legs.size() > 1);
				btnEditLeg.setEnabled(! ((IStructuredSelection)patrolLegViewer.getSelection()).isEmpty());
			}
		});
		
		btnAddLeg.setEnabled(false );
		btnSplit.setEnabled(false );
		btnRemoveLeg.setEnabled(false );
		btnEditLeg.setEnabled( false );
		
		return main;
	}

	private Shell getShell(){
		return Display.getDefault().getActiveShell();
	}

	private void sortAndRefresh(){
		patrolLegViewer.refresh();
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
		this.patrol = p;
		this.legs = new WritableList();
		
		//clone the legs
		for (PatrolLeg leg : p.getLegs()){
			PatrolLeg tmpLeg = new PatrolLeg();
			tmpLeg.setPatrol(p);
			tmpLeg.setId(leg.getId());
			
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
			this.legs.add(tmpLeg);
		}
		
		session.beginTransaction();
		try{
			typeOps = PatrolHibernateManager.getActivePatrolTransporationTypes(patrol.getConservationArea(), session, this.patrol.getPatrolType()); 
			allEmployes = PatrolHibernateManager.getActiveEmployees(patrol.getConservationArea(), session);
			session.getTransaction().rollback();
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog("Error loading patrol types", ex);
			session.getTransaction().rollback();
			session.close();
		}
		patrolLegViewer.setInput(legs);
		
		this.patrolStartDate = (Date)patrol.getStartDate().clone();
		this.patrolEndDate = (Date)patrol.getEndDate().clone();
		
		lblDateInfo.setText( "Patrol Start: " + DATE_FORMATTER.format(patrolStartDate) + "  Patrol End: " + DATE_FORMATTER.format(patrolEndDate) );
		patrolLegViewer.showPilotColum(patrol.hasPilot());
		
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	public boolean updatePatrol(Patrol p) {
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
						
						existing.getMembers().clear();
						for (PatrolLegMember newmember : updatedLeg.getMembers()) {
							newmember.setPatrolLeg(existing);
							existing.getMembers().add(newmember);
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
			newLeg.setPatrol(p);
			p.getLegs().add(newLeg);
		}
		
		//legs no longer used; these must be removed
		for (PatrolLeg toRemove: currentLegs){
			toRemove.setPatrol(null);
			p.getLegs().remove(toRemove);
		}
		
		//create leg days
		p.createLegDays();
		return true;
			
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Modify Patrol Legs";
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getErrorMessage()
	 */
	@Override
	public String getErrorMessage(){
		
		/* ensure each patrol leg between the patrol start and end */
		Date pstart = SmartUtils.getDatePart(patrolStartDate, false);
		Date pend = SmartUtils.getDatePart(patrolEndDate, true);
		
		for (Iterator iterator = legs.iterator(); iterator.hasNext();) {
			PatrolLeg legA = (PatrolLeg) iterator.next();
			
			Date legstart = SmartUtils.getDatePart(legA.getStartDate(), false);
			Date legend = SmartUtils.getDatePart(legA.getEndDate(), true);
			
			if (legstart.after(pend)){
				return legA.getId() + " cannot start after the patrol ends.";
			}
			if (legstart.before(pstart)){
				return legA.getId() + " cannot start before the patrol starts.";
			}
			
			if (legend.after(pend)){
				return legA.getId() + " cannot end after the patrol ends.";
			}
			if (legend.before(pstart)){
				return legA.getId() + " cannot end before the patrol starts.";
			}
		}
		
		/* Ensure that members are not in two places at the same time */
		for (Iterator iterator = legs.iterator(); iterator.hasNext();) {
			PatrolLeg legA = (PatrolLeg) iterator.next();
		
			for (Iterator iterator2 = legs.iterator(); iterator2.hasNext();) {
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
							return "Patrol member " + member.getMember().getLabel() + " cannot be in legs \"" + legA.getId() + "\" and \"" + legB.getId() + "\" as these legs overlap in time";
						}
					}
				}
			}
		}
		
		/* ensure there is at least one leg for each day in the patrol */
		GregorianCalendar calStart = SmartUtils.convertDate(patrolStartDate);
		calStart = new GregorianCalendar(calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH), 0,0,0);		
		GregorianCalendar calEnd = SmartUtils.convertDate(patrolEndDate);
		
		while (calStart.before(calEnd) || calStart.equals(calEnd)){
			boolean found = false;
			for (Iterator iterator = legs.iterator(); iterator.hasNext();) {
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
				return "Patrol does not have a leg for day " + DateFormat.getDateInstance(DateFormat.MEDIUM).format(calStart.getTime()) + ".  At least one leg must exist for each day in the patrol.";
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
