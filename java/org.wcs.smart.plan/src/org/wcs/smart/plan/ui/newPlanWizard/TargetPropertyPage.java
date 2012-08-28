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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.Target;


public class TargetPropertyPage extends Dialog {

	private Plan parentPlan;
	private Target toUpdate;
	
	private String title = null;
	private Session session = null;
	
	TabFolder tabFolder; 
	
	public static String AUTO_GENERATE = "system-generated";

	
	
	/**
	 * Create the dialog.
	 * 
	 * 
	 * 
	 * @param parent
	 * @param style
	 */
	public TargetPropertyPage(Shell parent,  
		Plan parentPlan, Target toUpdate) {
		
		super(parent);
		if (toUpdate == null){
			title = "Create Target";
		}else{
			title = "Update Target: " + toUpdate.getName();
		}
		
		this.toUpdate = toUpdate;
		this.parentPlan = parentPlan;

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
		item.setText ("Numeric");
		AlphaNumericTarget page1 = new AlphaNumericTarget(tabFolder, SWT.PUSH);
		item.setControl (page1.createComponent(tabFolder, SWT.NONE));
		
		
		TabItem item2 = new TabItem (tabFolder, SWT.NONE);
		item2.setText ("Location Target");
		AlphaNumericTarget page2 = new AlphaNumericTarget(tabFolder, SWT.PUSH);
		item2.setControl (page2.createComponent(tabFolder, SWT.NONE));
		
		
		TabItem item3 = new TabItem (tabFolder, SWT.NONE);
		item3.setText ("Administrative");
		AdministrativeTarget page3 = new AdministrativeTarget(tabFolder, SWT.PUSH);
		item3.setControl (page3.createComponent(tabFolder, SWT.NONE));
		
		tabFolder.pack();
		return parent;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, "Save", true);
		btn.setEnabled(false);
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

		return false;
	}
}
