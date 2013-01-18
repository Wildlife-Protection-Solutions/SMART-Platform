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

/**
 * Dialog for creating new employees or
 * editing existing employees.
 * 
 * @author Emily
 * @since 1.0.0
 */

package org.wcs.smart.plan.ui.newPlanWizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;


public class TargetPropertyPage extends Dialog {

	private Plan parentPlan;
	private PlanTarget toUpdate;
	
	private String title = null;
	private Session session = null;
	

	TabFolder tabFolder; 
	
	public static String AUTO_GENERATE = "system-generated";
	public static String TAB1 = "Numeric";
	public static String TAB2 = "Location Target";
	public static String TAB3 = "Administrative";

	private static Shell parent;
	
	/**
	 * Create the dialog.
	 * 
	 * 
	 * 
	 * @param parent
	 * @param style
	 */
	public TargetPropertyPage(Shell parent,  
		Plan parentPlan, PlanTarget toUpdate) {
		
		super(parent);

		this.toUpdate = toUpdate;
		this.parentPlan = parentPlan;
		this.parent = parent;
		
		
		if (toUpdate == null){
			title = "Create Target";
		}else{
			title = "Update Target: " + toUpdate.getName();
		}
	
	}

	private void init() {
		if(toUpdate == null){
			return; //get out if we are creating a new target, nothing to initialize
		}
		if(toUpdate.getCat() == PlanTarget.tarCategory.ALPHANUMERIC){
			Control ctls[] = tabFolder.getChildren();
			if(ctls[0] instanceof AlphaNumericTarget){
				((AlphaNumericTarget)ctls[0]).setTargetValue(toUpdate.getValue());
				((AlphaNumericTarget)ctls[0]).setTargetName(toUpdate.getName());
				((AlphaNumericTarget)ctls[0]).setTargetOp(toUpdate);
				((AlphaNumericTarget)ctls[0]).setTargetType(toUpdate);
			}
			
		}else if(toUpdate.getCat() == PlanTarget.tarCategory.SPATIAL){
		}else if(toUpdate.getCat() == PlanTarget.tarCategory.ADMIN){
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override 
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		p.x = (int)(p.x * 1.2);
		return p;
	}
	
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		tabFolder = new TabFolder(parent, SWT.BORDER);
		Rectangle clientArea = getShell().getClientArea ();
		tabFolder.setLocation (clientArea.x, clientArea.y);
		
		TabItem item = new TabItem (tabFolder, SWT.NONE);
		item.setText (TAB1);
		AlphaNumericTarget page1 = new AlphaNumericTarget(tabFolder, SWT.PUSH);
		item.setControl (page1.createComponent(tabFolder, SWT.NONE));
		
		
		TabItem item2 = new TabItem (tabFolder, SWT.NONE);
		item2.setText (TAB2);
		AlphaNumericTarget page2 = new AlphaNumericTarget(tabFolder, SWT.PUSH);
		item2.setControl (page2.createComponent(tabFolder, SWT.NONE));
		
		
		TabItem item3 = new TabItem (tabFolder, SWT.NONE);
		item3.setText(TAB3);
		AdministrativeTarget page3 = new AdministrativeTarget(tabFolder, SWT.PUSH);
		item3.setControl (page3.createComponent(tabFolder, SWT.NONE));
		
		tabFolder.pack();
		
		init();
		return parent;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Save", true);
		createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
		
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()){
				setReturnCode(OK);
				close();
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
			close();
		}
	}
	
	private boolean performSave(){
		PlanTarget t;
		if(toUpdate == null){
			t = new PlanTarget();
			t.setPlan(parentPlan);
		}else{
			t = toUpdate;
		}

		TabItem tab[] = tabFolder.getSelection();
		
		if(tab[0].getText() == TAB1){
			Control ctls[] = tabFolder.getChildren();
			if(ctls[0] instanceof AlphaNumericTarget){
				double value = ((AlphaNumericTarget)ctls[0]).getTargetValue();
				String name = ((AlphaNumericTarget)ctls[0]).getTargetName();
				String op = ((AlphaNumericTarget)ctls[0]).getTargetOp();
				String type = ((AlphaNumericTarget)ctls[0]).getTargetType();
				op = op.replace("[", "");
				op = op.replace("]", "");
				type = type.replace("[", "");
				type = type.replace("]", "");
				t.setValue(value);
				t.setOp(op);
				t.setType(type);
				t.setName(name);
				t.setCat(PlanTarget.tarCategory.ALPHANUMERIC);
			}
			
		}else if(tab[0].getText() == TAB2){
			t.setCat(PlanTarget.tarCategory.SPATIAL);
		}else if(tab[0].getText() == TAB3){
			t.setCat(PlanTarget.tarCategory.ALPHANUMERIC);
		}else{
			return false;//not a known tab that is trying to save something.
		}
		
		if(toUpdate == null){
			parentPlan.addTarget(t);
		}else{
			//target object is modified, might need to save or update here explicity?
		}
		return true;
	}

	public void addListener(int code, Listener listener) {
		this.getShell().addListener(code, listener);
	}
	
}
