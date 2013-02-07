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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.panel.PlanTargetComposite;

/**
 * Wizard page for collecting the plan targets
 * @author jeff
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage6 extends NewPlanWizardPage {

	private PlanTargetComposite panel;

	
	protected NewPlanWizardPage6() {
		super("Plan Targets");
	}


	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		panel =  new PlanTargetComposite(parent, SWT.NONE); 
		
		setControl(panel);
		setTitle("Targets");
		setMessage("Add plan targets by selecting the \"Add Target...\" button. Use the \"Edit Target ...\" to edit the selected target and \"Delete Target\" to remove a target.");
	}
	


	@Override
	public boolean updateModel(Plan p) {
		panel.updateModel(p);
		p.setTargets(panel.getTargets());
		return true;
	}
	
	@Override
	void initModel(Plan p) {
		panel.initFromModel(p);
		//the user has seen all the pages now, used in calculating whether to show the wizard finish button.
		((CreatePlanWizard) getWizard()).setSeenAll(true);
	}

	
	public void validate(){
		((CreatePlanWizard)getWizard()).validate();
	}
	
}