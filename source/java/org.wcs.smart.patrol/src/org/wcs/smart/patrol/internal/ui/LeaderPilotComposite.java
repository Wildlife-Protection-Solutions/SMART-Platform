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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeLabelProvider;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * Patrol Item Composite for patrol leader and pilot.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class LeaderPilotComposite extends PatrolLegItemComposite{

	private static final String ERROR_PILOT_REQUIRED = Messages.LeaderPilotComposite_Error_PilotRequired;
	private static final String ERROR_LEADER_REQUIRED = Messages.LeaderPilotComposite_Error_LeaderRequired;
	
	private ComboViewer patrolLeaderViewer = null;
	private ComboViewer patrolPilotViewer = null;
	private Label lblPilot;
	
	/**
	 * 
	 */
	public LeaderPilotComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {

		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.LeaderPilotComposite_LeaderLabel);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		patrolLeaderViewer = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		patrolLeaderViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolLeaderViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolLeaderViewer.setLabelProvider(new EmployeeLabelProvider());
		patrolLeaderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
				fireChangeListeners();	
			}
		});
		
		lblPilot = new Label(center, SWT.NONE);
		lblPilot.setText(Messages.LeaderPilotComposite_PilotLabel);
		lblPilot.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			
		patrolPilotViewer = new ComboViewer(center, SWT.DROP_DOWN | SWT.READ_ONLY);
		patrolPilotViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		patrolPilotViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolPilotViewer.setLabelProvider(new EmployeeLabelProvider());
		patrolPilotViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
				fireChangeListeners();	
			}
		});
		
		patrolPilotViewer.getControl().setVisible(false);
		lblPilot.setVisible(false);
		
		return center;
	}
	private void validate(){
		setErrorMessage(null);
		if (patrolPilotViewer.getControl().isVisible() && (patrolPilotViewer.getSelection() == null || ((IStructuredSelection)patrolPilotViewer.getSelection()).getFirstElement() == null )){
			setErrorMessage(ERROR_PILOT_REQUIRED);
		}
		if (patrolLeaderViewer.getSelection() == null || ((IStructuredSelection)patrolLeaderViewer.getSelection()).getFirstElement() == null ){
			setErrorMessage(ERROR_LEADER_REQUIRED);
		}
	}

	/**
	 * <p>
	 * If this composite is used on a page where employees are also
	 * selected use update setEmployeeList(ObservableList, Patrol) to
	 * set the input to the same list that is used for the selected
	 * employees.  This will ensure that the values in the combo
	 * box are updated as the selected employees change.
	 * </p>
	 *  
	 *  @param session - not used and may be null
	 *  
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(PatrolLeg patrolLeg, Session session) {
		List<PatrolLegMember> sortedList = new ArrayList<PatrolLegMember>();
		sortedList.addAll(patrolLeg.getMembers());
		Collections.sort(sortedList, new Comparator<PatrolLegMember>(){
			@Override
			public int compare(PatrolLegMember o1, PatrolLegMember o2) {
				return Collator.getInstance().compare(o1.getMember().getFullLabel(), o2.getMember().getFullLabel());
			}});
		List<PatrolLegMember> wrinput = new ArrayList<PatrolLegMember>(sortedList);
		patrolLeaderViewer.setInput(wrinput);

		if (patrolLeg.getLeader() != null){
			patrolLeaderViewer.setSelection(new StructuredSelection(patrolLeg.getLeader()));
		}else{
			patrolLeaderViewer.setSelection(new StructuredSelection(sortedList.get(0)));
		}

		lblPilot.setVisible(patrolLeg.getPatrol().hasPilot());
		patrolPilotViewer.getControl().setVisible(patrolLeg.getPatrol().hasPilot());
		if (patrolLeg.getPatrol().hasPilot()){
			patrolPilotViewer.setInput(wrinput);
			if (patrolLeg.getPilot() != null){
				patrolPilotViewer.setSelection(new StructuredSelection(patrolLeg.getPilot()));
			}else{
				patrolPilotViewer.setSelection(new StructuredSelection(sortedList.get(0)));
			}
		}
		validate();
	}
	
	public void refresh(){
		if (patrolLeaderViewer != null && !patrolLeaderViewer.getControl().isDisposed()){
			patrolLeaderViewer.refresh();
		}
		if (patrolPilotViewer != null && !patrolPilotViewer.getControl().isDisposed()){
			patrolPilotViewer.refresh();
		}
	}

	/**
	 * Alternative to setValues(Patrol, Session).
	 * 
	 * 
	 * @param list list of Employees.  This is an observable list of employees associated with the patrol.
	 * @param patrol the patrol to select default values from
	 */
	public void setEmployeeList(List<Employee> list, Patrol patrol){
		//leader list
		patrolLeaderViewer.setInput(list);		
		if (patrol.getFirstLeg().getLeader() != null){
			patrolLeaderViewer.setSelection(new StructuredSelection(patrol.getFirstLeg().getLeader().getMember()));
		}else if (list.size() > 0){
			patrolLeaderViewer.setSelection(new StructuredSelection(list.get(0)));
		}

		//pilot list
		lblPilot.setVisible(patrol.hasPilot());
		patrolPilotViewer.getControl().setVisible(patrol.hasPilot());
		if (patrol.hasPilot()){
			patrolPilotViewer.setInput(list);
			if ( patrol.getFirstLeg().getPilot() != null){
				patrolPilotViewer.setSelection(new StructuredSelection(patrol.getFirstLeg().getPilot().getMember()));
			}else if (list.size() > 0){
				patrolPilotViewer.setSelection(new StructuredSelection(list.get(0)));	
			}
		}
		validate();
	}
	
	/**
	 * 
	 * @return the selected leader
	 */
	public Employee getSelectedLeader(){
		Object x = ((IStructuredSelection)patrolLeaderViewer.getSelection()).getFirstElement();
		if (x != null && x instanceof PatrolLegMember){
			return ((PatrolLegMember)x).getMember();
		}else if (x != null && x instanceof Employee){
    		return (Employee)x;
		}
		return null;
	}
	/**
	 * 
	 * @return the selected pilot
	 */
	public Employee getSelectedPilot(){
		if (patrolPilotViewer == null){
			return null;
		}
		Object x = ((IStructuredSelection)patrolPilotViewer.getSelection()).getFirstElement();
		if (x != null && x instanceof PatrolLegMember){
			return ((PatrolLegMember)x).getMember();
		}else if (x != null && x instanceof Employee){
    		return (Employee)x;
		}
		return null;
	}
	
	/**
	 * Updates a given patrol leg with the values from the composite.
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(PatrolLeg patrolLeg) {
		Object x = ((IStructuredSelection)patrolLeaderViewer.getSelection()).getFirstElement();
		if (x == null){
			SmartPatrolPlugIn.displayLog(ERROR_LEADER_REQUIRED, null);
			return false;
		}
		if (x instanceof PatrolLegMember){
			PatrolLegMember plm = (PatrolLegMember)x;
			if (plm != null){
				patrolLeg.setLeader(plm);
			}
			if (patrolLeg.getPatrol().hasPilot()){
				plm = (PatrolLegMember)((IStructuredSelection)patrolPilotViewer.getSelection()).getFirstElement();
				if (plm == null){
					SmartPatrolPlugIn.displayLog(ERROR_PILOT_REQUIRED, null);
					return false;
				}
				patrolLeg.setPilot(plm);
				
			}
    	}else if (x instanceof Employee){
    		Employee plm = (Employee)x;
			if (plm != null){
				for (PatrolLegMember mem : patrolLeg.getMembers()){
					if (mem.getMember().equals(plm)){
						patrolLeg.setLeader(mem);
						break;
					}
				}
				
			}
			if (patrolLeg.getPatrol().hasPilot()){
				plm = (Employee)((IStructuredSelection)patrolPilotViewer.getSelection()).getFirstElement();
				if (plm == null){
					throw new PatrolSaveException(ERROR_PILOT_REQUIRED);	
				}
				for (PatrolLegMember mem : patrolLeg.getMembers()){
					if (mem.getMember().equals(plm)){
						patrolLeg.setPilot(mem);
						break;
					}
				}
			}
    	}
		return true;
    	
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return Messages.LeaderPilotComposite_Title;
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}
}