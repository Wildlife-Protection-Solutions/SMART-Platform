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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.ui.createpatrol.EmployeeSelectComposite;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * Patrol item composite that allows users
 * to select both patrol members and the 
 * patrol leader/pilot.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EmployeeLeaderPilotComposite extends PatrolItemComposite{


	private EmployeeSelectComposite empListComposite = null;
	private LeaderPilotComposite leaderPilotComp;
	
	/**
	 * 
	 */
	public EmployeeLeaderPilotComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		empListComposite = new EmployeeSelectComposite(main, SWT.NONE);
		empListComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		empListComposite.addSelectionChangedListener(new EmployeeSelectComposite.IListChanged(){
			public void listChanged(List<Employee> newEmployees) {
				if (newEmployees.size() == 0){
					setErrorMessage("At least one employee must be selected.");
				}else{
					setErrorMessage(null);
				}
				fireChangeListeners();
			}
		});
		leaderPilotComp = new LeaderPilotComposite();
		leaderPilotComp.createComponent(main, SWT.NONE);
		leaderPilotComp.addChangeListener( new IPatrolItemChangeListener() {
			@Override
			public void itemChanged() {
				fireChangeListeners();
			}
		});

		return main;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
		
		ArrayList<Employee> current = new ArrayList<Employee>();
    	if (p.getFirstLeg().getMembers() != null){
    		for (Iterator iterator = p.getFirstLeg().getMembers().iterator(); iterator.hasNext();) {
    			PatrolLegMember employee = (PatrolLegMember) iterator.next();
    			current.add(employee.getMember());
    		}
    	}    	
		empListComposite.getSelectedEmployees();
		empListComposite.setEmployeeData(HibernateManager.getActiveEmployees(p.getConservationArea(), session), current);
		leaderPilotComp.setEmployeeList(empListComposite.getSelectedEmployees(),p);
	}
	
	/**
	 * @return error message from validating input
	 */
	public String getErrorMessage(){
		if (super.getErrorMessage() != null){
			return super.getErrorMessage();
		}
		if (leaderPilotComp.getErrorMessage() != null){
			return leaderPilotComp.getErrorMessage();
		}
		return null;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public void updatePatrol(Patrol p) {
		PatrolLeg firstLeg = p.getFirstLeg();
		firstLeg.clearPatrolLegMembers();
		if (empListComposite.getSelectedEmployees().size() <= 0){
			throw new PatrolSaveException("At least one member must be selected.");
		}
    	for (Iterator iterator = empListComposite.getSelectedEmployees().iterator(); iterator.hasNext();) {
			Employee e = (Employee) iterator.next();
			firstLeg.addPatrolLegMember(e);
		}
    	
		leaderPilotComp.updatePatrol(p);
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Patrol Members";
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}
}
