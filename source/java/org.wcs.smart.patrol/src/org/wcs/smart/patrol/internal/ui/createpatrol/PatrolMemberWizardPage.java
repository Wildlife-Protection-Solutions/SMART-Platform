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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 *
 * Wizard page to collect patrol members.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolMemberWizardPage extends NewPatrolWizardPage  {
	
	private EmployeeSelectComposite members;
	private List<Employee> allEmployees = null;

	/**
	 * 
	 */
	protected PatrolMemberWizardPage() {
		super(Messages.PatrolMemberWizardPage_PageName);
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		CreatePatrolWizard wizard = (CreatePatrolWizard)getWizard();
		
		wizard.getSession().beginTransaction();
		allEmployees = HibernateManager.getActiveEmployees(wizard.getPatrol().getConservationArea(), wizard.getSession());
		wizard.getSession().getTransaction().rollback();
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.PatrolMemberWizardPage_SelectEmployees_Label);
		
		members = new EmployeeSelectComposite(main, SWT.NONE);
		members.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		members.addSelectionChangedListener(new EmployeeSelectComposite.IListChanged() {
			@Override
			public void listChanged(List<Employee> newEmployees) {
				if (newEmployees.size() == 0){
					setPageComplete(false);
					setErrorMessage(Messages.PatrolMemberWizardPage_Error_NoEmployees);
				}else{
					setPageComplete(true);
					setErrorMessage(null);
				}
				
			}
		});
		
		setErrorMessage(null);
		setMessage(Messages.PatrolMemberWizardPage_PageMessage);
		super.setControl(main);
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.createpatrol.NewPatrolWizardPage#initModel(org.wcs.smart.patrol.model.Patrol)
	 */
	@Override
	void initModel(Patrol p, Session session) {
		PatrolLeg pl = p.getFirstLeg();
    	ArrayList<Employee> current = new ArrayList<Employee>();
    	if (pl.getMembers() != null){
    		for (Iterator iterator = pl.getMembers().iterator(); iterator.hasNext();) {
    			PatrolLegMember employee = (PatrolLegMember) iterator.next();
    			current.add(employee.getMember());
    		}
    	}
    	members.setEmployeeData(allEmployees, current);
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.createpatrol.NewPatrolWizardPage#updateModel()
	 */
	@Override
	public boolean  updateModel(Patrol p) {		
		PatrolLeg firstLeg = p.getFirstLeg();
		firstLeg.clearPatrolLegMembers();
		
    	for (Iterator iterator = members.getSelectedEmployees().iterator(); iterator.hasNext();) {
			Employee e = (Employee) iterator.next();
			firstLeg.addPatrolLegMember(e);
		}
    	return true;
	}
}


