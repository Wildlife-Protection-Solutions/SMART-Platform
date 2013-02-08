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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.plan.model.Plan;

/**
 * Wizard page for determining if plan is to be
 * create from a template or not.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage1 extends NewPlanWizardPage {

	private Button btnExisting;
	private Button btnNew;

	/**
	 * 
	 */
	protected NewPlanWizardPage1() {
		super("New Plan");
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
				true));

		Composite center = new Composite(buttonPanel, SWT.NONE);
		center.setLayout(new GridLayout());
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		btnNew = new Button(center, SWT.RADIO);
		btnNew.setText("Create a new plan from scratch");
		btnNew.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnNew.setSelection(true);
		
		btnExisting = new Button(center, SWT.RADIO);
		btnExisting.setText("Use an existing plan as a template");
		btnExisting.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		setTitle("Template");
		setMessage("When creating a new plan, you can use an existing plan as a template, or simply create a new, blank plan.");
		super.setControl(buttonPanel);

	}
	
	//nothing to update, just determines if we use a plan template or start afresh
	@Override
	public boolean updateModel(Plan p) {
		return true;
	}
	
	@Override
	void initModel(Plan p) {
		//nothing to do on this page.
	}
	
	@Override
    public IWizardPage getNextPage() {
        if (getWizard() == null) {
			return null;
		}
        
        if( btnExisting.getSelection() == true){
        	return getWizard().getPage(NewPlanWizardPage2b.PAGENAME);
        }
        return getWizard().getPage(NewPlanWizardPage2.PAGENAME);
    }
	

}