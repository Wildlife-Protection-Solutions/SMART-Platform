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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.plan.model.Plan;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;




/**
 * Wizard page for collecting the patrol comment
 * @author egouge
 * @since 1.0.0
 */
public class NewPlanWizardPage4 extends NewPlanWizardPage {

	
	
	private Text planId;
	private Text planName;
	private Text planDesc;

	/**
	 * 
	 */
	protected NewPlanWizardPage4() {
		super("Plan Details");
		
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
		lbl.setText("Plan ID:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		planId = new Text(center, SWT.BORDER | SWT.LEFT);
		planId.setTextLimit(32);

		
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 8;
		data.widthHint = 100;
		
		planId .setLayoutData(data);
		
		Label lbl2 = new Label(center, SWT.NONE);
		lbl2.setText("Plan Name:");
		lbl2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		planName = new Text(center, SWT.BORDER | SWT.LEFT);
		planName.setTextLimit(32);

		data.widthHint = 170;
		planName.setLayoutData(data);

		Label lbl3 = new Label(center, SWT.NONE);
		lbl3.setText("Plan Descrption:");
		lbl3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		planDesc = new Text(center, SWT.BORDER | SWT.LEFT| SWT.WRAP | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 80;
		gd.horizontalIndent = 8;


		planDesc.setLayoutData(gd);
		
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
}