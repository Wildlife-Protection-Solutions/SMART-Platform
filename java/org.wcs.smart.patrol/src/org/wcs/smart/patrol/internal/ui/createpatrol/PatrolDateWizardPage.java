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

import java.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Wizard page to determine the start and end dates of a patrol
 * @author Emily
 * @since 1.0.0
 */
public class PatrolDateWizardPage extends NewPatrolWizardPage implements SelectionListener{

	private DateTime dtStartDate;
	private DateTime dtEndDate;


	/**
	 * @param pageName
	 */
	protected PatrolDateWizardPage() {
		super("Patrol Date Range");
		
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
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Patrol Start Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText("Patrol End Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		dtEndDate.addSelectionListener(this);
		dtStartDate.addSelectionListener(this);
		
		setMessage("Select the start and end dates for the patrol.");
		super.setControl(main);
		
	}
	@Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
        	Patrol p = ((CreatePatrolWizard)getWizard()).getPatrol();
        	if (p.getStartDate() != null){
        		Calendar cal = SmartPlugIn.convertDate(p.getStartDate());
        		dtStartDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        	}
        	if (p.getEndDate() != null){
        		Calendar cal = SmartPlugIn.convertDate(p.getEndDate());
        		dtEndDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        	}
        }
    }
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.createpatrol.NewPatrolWizardPage#updateModel()
	 */
	@Override
	void updateModel(Patrol p) {		
		p.setStartDate(SmartPlugIn.getDate(dtStartDate));
		p.setEndDate(SmartPlugIn.getDate(dtEndDate));
		
		p.getFirstLeg().setStartDate(p.getStartDate());
		p.getFirstLeg().setEndDate(p.getEndDate());
	}


	
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (SmartPlugIn.getDate(dtStartDate).after(SmartPlugIn.getDate(dtEndDate))){
			setErrorMessage("End date must be after the start date.");
			this.setPageComplete(false);
		}else{
			this.setPageComplete(true);
			setErrorMessage(null);
		}
	}
}