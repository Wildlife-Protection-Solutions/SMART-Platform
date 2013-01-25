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
import org.hibernate.Session;
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
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));

		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

		Composite buttonPanel = new Composite(center, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true,
				false));

		btnNew = new Button(buttonPanel, SWT.RADIO);
		btnNew.setText("Create a New Plan from Scratch");
		btnNew.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnNew.setSelection(true);
		
		btnExisting = new Button(buttonPanel, SWT.RADIO);
		btnExisting.setText("Use an existing Plan as a template");
		btnExisting.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		setMessage("When creating a new plan, you can use an existing plan as a template, or simply create a new, blank plan.");
		super.setControl(main);

	}
	
	//nothing to update, just determines if we use a plan template or start afresh
	@Override
	public boolean updateModel(Plan p) {
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {
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