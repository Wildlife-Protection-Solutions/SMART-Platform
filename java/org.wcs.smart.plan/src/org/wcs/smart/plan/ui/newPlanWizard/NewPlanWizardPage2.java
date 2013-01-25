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
package org.wcs.smart.plan.ui.newPlanWizard;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.Plan.PlanType;


/**
 * Wizard page for collecting the plan type,
 * employees information
 * 
 * @author jeff
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage2 extends NewPlanWizardPage {

	private ComboViewer planType = null;
	private Text unavailableEmployees;
	private Label activeEmployees;
	
	/**
	 * 
	 */
	protected NewPlanWizardPage2() {
		super("Plan Type");
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Plan Type:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		planType = new ComboViewer(center, SWT.READ_ONLY);
		planType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		planType.setContentProvider(ArrayContentProvider.getInstance());
		planType.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((Plan.PlanType)element).guiName;
			}
		});
		planType.setInput(Plan.PlanType.values());
		planType.setSelection(new StructuredSelection(Plan.PlanType.PATROL));
		
		Label lbl2 = new Label(center, SWT.NONE);
		lbl2.setText("Active Rangers:");
		lbl2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		activeEmployees = new Label(center, SWT.NONE);
		activeEmployees.setText("unknown");
		activeEmployees.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText("Unavailable Rangers:");
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		unavailableEmployees = new Text(center, SWT.BORDER);
		unavailableEmployees.setTextLimit(5);
		unavailableEmployees.setText("0");
		unavailableEmployees.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label lbl5 = new Label(center, SWT.NONE);
		lbl5.setText("(vacation, sickness, etc)");
		lbl5.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
				
		setControl(center);
		setMessage("Select the Type of Plan that you wish to create:");

	}
	

	@Override
	public boolean updateModel(Plan p) {
		p.setType((PlanType) ((IStructuredSelection)planType.getSelection()).getFirstElement());
		Integer act = Integer.valueOf(activeEmployees.getText());
		Integer un = Integer.valueOf(this.unavailableEmployees.getText());
		p.setUnavailableEmployees(un);
		p.setActiveEmployees(act);
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {
		//need this transaction as this is not the plan hibernate manager, and I don't want to break other 
		//code by changing the transcation paradigm in the main plug in
		session.beginTransaction();
		Integer act = HibernateManager.getActiveEmployees(p.getConservationArea(), session).size();
		session.getTransaction().rollback();
		
		activeEmployees.setText(act.toString());
		try{
			unavailableEmployees.setText(p.getUnavailableEmployees().toString());
			planType.setSelection(new StructuredSelection(p.getType()));
		}catch (Exception e){
			//do nothing, probably just no template so we can't set the values to anything
		}
	}
}