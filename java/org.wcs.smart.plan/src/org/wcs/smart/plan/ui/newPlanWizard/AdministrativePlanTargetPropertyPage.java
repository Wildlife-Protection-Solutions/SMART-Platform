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
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.util.SmartUtils;






public class AdministrativePlanTargetPropertyPage implements ITargetPage{

	private TargetPropertyPage parentWindow;
	
	private Text targetDesc;
	private Text targetName;
	
	private ControlDecoration cdTargetName;
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public AdministrativePlanTargetPropertyPage(TargetPropertyPage parentWindow) {
		this.parentWindow = parentWindow;
	}
	
	public String getPageName(){
		return AdministrativePlanTarget.TARGET_GUI_NAME;
	}
	
	@Override
	public Composite createComponent(Composite parent, int style) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Target Name:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetName = new Text(center, SWT.BORDER | SWT.LEFT);
		targetName.setTextLimit(32);
		targetName.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText("Target Description:");
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

		targetDesc= new Text(center, SWT.BORDER | SWT.LEFT);
		targetDesc.setTextLimit(2048);
		targetDesc.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, true));
		

		KeyListener validate = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				validate();
			}
		};
		targetName.addKeyListener(validate);
		
		cdTargetName = createDecoration(targetName);

		validate();
		
		return center;
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/**
	 * Validate the input fields
	 * 
	 * @return <code>false</code> if not complete, <code>true</code> otherwise
	 */
	public boolean validate() {

		boolean isComplete = true;
		if (targetName.getText().trim().isEmpty()
				|| ! SmartUtils.isSimpleString(targetName.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Employee.MAX_NAME_LENGTH) ) {
			cdTargetName.show();
			cdTargetName.setDescriptionText("Name cannot be empty or use the following characters: " + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX);
			isComplete = false;
		}else{
			cdTargetName.hide();
		}
		
		if(isComplete){
			parentWindow.enableOK(true);
		}else{
			parentWindow.enableOK(false);
		}
		return isComplete;
	}


	public String getTargetDesc() {
		return targetDesc.getText();
	}
	public void setTargetDesc(String targetDesc) {
		this.targetDesc.setText( targetDesc);
	}
	
	public String getTargetName() {
		return targetName.getText();
	}
	public void setTargetName(String targetName) {
		this.targetName.setText(targetName);
	}
	
	@Override
	public void initPage(PlanTarget p) {
		AdministrativePlanTarget pt = (AdministrativePlanTarget) p;
		this.targetName.setText(pt.getName());
		this.targetDesc.setText(pt.getTargetDesc());
		validate();
	}
	
	public PlanTarget createTarget(){
		return new AdministrativePlanTarget();
	}
	
	public void updateTarget(PlanTarget pt){
		AdministrativePlanTarget target = (AdministrativePlanTarget)pt;
		target.setName(getTargetName());
		target.setTargetDesc(getTargetDesc());
		
	}

}

