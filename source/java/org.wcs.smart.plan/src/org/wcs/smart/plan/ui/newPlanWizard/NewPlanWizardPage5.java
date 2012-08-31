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

import java.util.ArrayList;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.util.SmartUtils;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;




/**
 * Wizard page for collecting the patrol comment
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage5 extends NewPlanWizardPage implements SelectionListener {

	
	
	private DateTime dtStartDate;
	private DateTime dtEndDate;

	private ControlDecoration cdEndDate;
	/**
	 * 
	 */
	protected NewPlanWizardPage5() {
		super("Plan Dates");
		
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
		lbl.setText("Plan Start Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)dtStartDate.getLayoutData()).horizontalIndent = 10;
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText("Plan End Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)dtEndDate.getLayoutData()).horizontalIndent = 10;
		cdEndDate = new ControlDecoration(lbl, SWT.RIGHT);
		cdEndDate.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_WARNING));
		cdEndDate.hide();
		
		dtEndDate.addSelectionListener(this);
		dtStartDate.addSelectionListener(this);
		
		setControl(center);
		setMessage("Enter a name and description for the new Plan:");
	
	}
	

	@Override
	public boolean updateModel(Plan p) {
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {

	}
	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		String error = null;
		cdEndDate.hide();
		if (SmartUtils.getDate(dtStartDate).after(SmartUtils.getDate(dtEndDate))){
			error = "End date must be after the start date.";
		}else{
			long startD = SmartUtils.getDate(dtStartDate).getTime();
			long endD = SmartUtils.getDate(dtEndDate).getTime();
			
			if (startD + Patrol.MAX_PATROL_LENGTH_DAYS * 24 * 60 * 60 * 1000.0 < endD){
				error = "Patrol cannot be longer that " + Patrol.MAX_PATROL_LENGTH_DAYS + " days in length.";
			}else if(startD + Patrol.WARN_PATROL_LENGTH_DAYS * 24 * 60 * 60 * 1000.0 < endD){
				cdEndDate.setDescriptionText("Patrol is longer than 30 days");
				cdEndDate.show();
			}
		}
		
		setErrorMessage(error);
		fireChangeListeners();
	}
	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
}