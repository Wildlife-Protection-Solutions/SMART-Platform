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
import org.wcs.smart.plan.model.PlanTarget;


public class AlphaNumericTarget extends Composite {

	private Composite parent;
	
	private Text targetValue;
	private Text targetName;
	private ComboViewer targetType = null;
	private ComboViewer targetOp = null;
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public AlphaNumericTarget(Composite parent, int style) {
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

		data.widthHint = 100;
		
		targetName.setLayoutData(data);
		
		Label lbl2 = new Label(center, SWT.NONE);
		lbl2.setText("Target Type:");
		lbl2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		targetType= new ComboViewer(center, SWT.READ_ONLY);
		targetType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		targetType.setContentProvider(ArrayContentProvider.getInstance());
		targetType.setLabelProvider(new LabelProvider());
		
		ArrayList<String> options = new ArrayList<String>();
		options.add("Distance Travelled");
		options.add("Patrol Hours");
		options.add("Patrol Days");
		options.add("Patrol Man-Hours");
		
		targetType.setInput(options);
		
		Label lbl3 = new Label(center, SWT.NONE);
		lbl3.setText("Operator:");
		lbl3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		targetOp= new ComboViewer(center, SWT.READ_ONLY);
		targetOp.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		targetOp.setContentProvider(ArrayContentProvider.getInstance());
		targetOp.setLabelProvider(new LabelProvider());
		
		ArrayList<String> optionsOp = new ArrayList<String>();
		optionsOp.add(">");
		optionsOp.add("<");
		optionsOp.add("=");
		optionsOp.add("!=");
		
		targetOp.setInput(optionsOp);
		
		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText("Target Value:");
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetValue = new Text(center, SWT.BORDER | SWT.LEFT);
		targetValue.setTextLimit(32);

		data.widthHint = 170;
		targetValue.setLayoutData(data);
		
		return center;
	}
	
	public void setTargetValue(double targetValue) {
		this.targetValue.setText(Double.toString(targetValue));
	}


	public void setTargetName(String targetName) {
		this.targetName.setText(targetName);
	}


	public void setTargetType(PlanTarget pt) {
		this.targetType.setSelection(new StructuredSelection(pt.getType()));
	}


	public void setTargetOp(PlanTarget pt) {
		this.targetOp.setSelection(new StructuredSelection(pt.getOp()));
	}


	public double getTargetValue(){
		return Double.valueOf(targetValue.getText());
	}
	
	public String getTargetName(){
		return targetName.getText();
	}
	public String getTargetType(){
		return targetType.getSelection().toString();
	}
	public String getTargetOp(){
		return targetOp.getSelection().toString();
	}
}

