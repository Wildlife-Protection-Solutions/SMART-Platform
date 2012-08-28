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
public class NewPlanWizardPage3 extends NewPlanWizardPage {

	
	
	private ComboViewer team = null;
	private ComboViewer station= null;
	private Text unavailable;

	/**
	 * 
	 */
	protected NewPlanWizardPage3() {
		super("Plan Station/Team");
		
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
		lbl.setText("Team (optional):");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		team = new ComboViewer(center, SWT.READ_ONLY);
		team.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		team.setContentProvider(ArrayContentProvider.getInstance());
		team.setLabelProvider(new LabelProvider());
		
		ArrayList<String> options = new ArrayList<String>();
		options.add("Team 1");
		options.add("Team 2");
		options.add("Team 3");
		options.add("Team 4");
		
		team.setInput(options);
		
		Label lbl2 = new Label(center, SWT.NONE);
		lbl2.setText("Station (optional):");
		lbl2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		ArrayList<String> soptions = new ArrayList<String>();
		soptions.add("Station X");
		soptions.add("Station Y");
		soptions.add("Station Z");
		
		station = new ComboViewer(center, SWT.READ_ONLY);
		station.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		station.setContentProvider(ArrayContentProvider.getInstance());
		station.setLabelProvider(new LabelProvider());
		
		station.setInput(soptions);

		
		setControl(center);
		setMessage("Select the associated Team and/or Station for this plan, if applicable:");

	}
	

	@Override
	public boolean updateModel(Plan p) {
		return true;
	}
	
	@Override
	void initModel(Plan p, Session session) {

	}
}