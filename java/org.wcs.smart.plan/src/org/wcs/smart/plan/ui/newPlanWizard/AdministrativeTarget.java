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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;






public class AdministrativeTarget extends Composite{

	private Composite parent;
	
	private Text targetDesc;
	private Text targetName;
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public AdministrativeTarget(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
	}
	
	
	public Composite createComponent(Composite parent, int style) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Target Name:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetName = new Text(center, SWT.BORDER | SWT.LEFT);
		targetName.setTextLimit(32);

		
		GridData data = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		data.widthHint = 170;
		
		targetName.setLayoutData(data);
		
				
		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText("Target Description:");
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetDesc= new Text(center, SWT.BORDER | SWT.LEFT);
		targetDesc.setTextLimit(2048);

		GridData data2 = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		data2.widthHint = 400;
		data2.heightHint = 50; 
		targetDesc.setLayoutData(data2);
		
		return center;
	}
}

