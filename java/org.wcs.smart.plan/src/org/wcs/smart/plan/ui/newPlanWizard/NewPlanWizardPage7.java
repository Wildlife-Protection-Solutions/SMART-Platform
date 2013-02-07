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
import org.wcs.smart.plan.ui.panel.PlanParentIdComposite;

/**
 * Wizard page for selecting a parent plan
 * 
 * @author jeff
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage7 extends NewPlanWizardPage {


	private PlanParentIdComposite panel;

	
	/**
	 * 
	 */
	protected NewPlanWizardPage7() {
		super("Plan Parent");
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		panel =  new PlanParentIdComposite(parent, SWT.NONE);
		
		setControl(panel);
		setMessage("Select the Type of Plan that you wish to create:");		setMessage("A parent plan allows you to group patrol plans into team, station and conservation area plans." +
				"Create the conservation area plan first, then select it in this window when creating each each patrol, " +
				"station or team plan you want to include in the Conservation area plan. Use the same method to create station or team plans.");
	
	}

	@Override
	public boolean updateModel(Plan p) {
		panel.updateModel(p);
		return true;
	}
	
	@Override
	void initModel(Plan p) {
		((CreatePlanWizard) getWizard()).setSeenAll(true);
		panel.initFromModel(p);
	}
	
}