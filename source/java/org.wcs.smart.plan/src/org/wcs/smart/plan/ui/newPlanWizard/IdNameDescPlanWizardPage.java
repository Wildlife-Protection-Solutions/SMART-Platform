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
import org.wcs.smart.plan.ui.panel.IInputChangeListener;
import org.wcs.smart.plan.ui.panel.PlanIdNameDescComposite;


/**
 * Wizard page for collecting the plan id,
 * name, and description
 * 
 * @author jeff
 * @author egouge
 * @since 1.0.0
 */
public class IdNameDescPlanWizardPage extends PlanWizardPage {

	private PlanIdNameDescComposite panel;
	/**
	 * 
	 */
	protected IdNameDescPlanWizardPage() {
		super("Plan Details");
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		panel =  new PlanIdNameDescComposite(parent, SWT.NONE); 
		
		panel.addInputChangeListener(new IInputChangeListener(){
			@Override
			public void inputChanged() {
				if(!panel.isDataValid()){
					((CreatePlanWizard) getWizard()).setCanFinish(false);
					setPageComplete(false);
				}else{
					((CreatePlanWizard) getWizard()).validate();
					setPageComplete(true);
				}
			}
		
		});
		
		setControl(panel);
		setTitle(panel.getTitle());
		setMessage("Enter a name and description for the new plan.");
		
	}



	@Override
	public boolean updateModel(Plan p) {
		return panel.updateModel(p); 
	}
	
	@Override
	void initModel(Plan p) {
		panel.initFromModel(p);
	}
}