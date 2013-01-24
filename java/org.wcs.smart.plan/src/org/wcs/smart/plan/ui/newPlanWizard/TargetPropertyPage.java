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
 * Dialog for creating new targets or
 * editing existing targets.
 * 
 * @author Jeff
 * @since 1.0.0
 */

package org.wcs.smart.plan.ui.newPlanWizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;


public class TargetPropertyPage extends Dialog {

	private Plan parentPlan;
	private NewPlanWizardPage6 parentWindow;
	private PlanTarget toUpdate;
	
	private String title = null;
	private Session session = null;
	
	public Button btnOk;
	
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
	public TargetPropertyPage(NewPlanWizardPage6 parentWindow, Shell parent,  
		Plan parentPlan, PlanTarget toUpdate) {
		
		super(parent);

		this.toUpdate = toUpdate;
		this.parentPlan = parentPlan;
		this.parent = parent;
		this.parentWindow = parentWindow;
		
		
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
		if(toUpdate instanceof  NumericPlanTarget){
			Control ctls[] = tabFolder.getChildren();
			if(ctls[0] instanceof NumericPlanTargetPropertyPage){
				((NumericPlanTargetPropertyPage)ctls[0]).setPlanTarget( toUpdate);
			}
			tabFolder.setSelection(0);
		}else if(toUpdate instanceof  AdministrativePlanTarget){
			Control ctls[] = tabFolder.getChildren();
			//3rd tab is forth control... 0,2,4...for 1st 2nd 3rd tabs
			if(ctls[4] instanceof AdministrativePlanTargetPropertyPage){
				((AdministrativePlanTargetPropertyPage)ctls[4]).setPlanTarget( toUpdate);
			}
			tabFolder.setSelection(2); //3rd tab
			
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
		NumericPlanTargetPropertyPage page1 = new NumericPlanTargetPropertyPage(this, tabFolder, SWT.PUSH);
		item.setControl (page1.createComponent(tabFolder, SWT.NONE));
		
		
		TabItem item2 = new TabItem (tabFolder, SWT.NONE);
		item2.setText (TAB2);
		NumericPlanTargetPropertyPage page2 = new NumericPlanTargetPropertyPage(this, tabFolder, SWT.PUSH);
		item2.setControl (page2.createComponent(tabFolder, SWT.NONE));
		
		
		TabItem item3 = new TabItem (tabFolder, SWT.NONE);
		item3.setText(TAB3);
		AdministrativePlanTargetPropertyPage page3 = new AdministrativePlanTargetPropertyPage(this, tabFolder, SWT.PUSH);
		item3.setControl (page3.createComponent(tabFolder, SWT.NONE));
		
		tabFolder.pack();
		
		init();
		return parent;
	}
	
	
	public void enableOK(boolean val){
		if(btnOk != null){
			btnOk.setEnabled(val);
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		btnOk = createButton(parent, IDialogConstants.OK_ID, "Save", true);
		if(toUpdate == null){
			btnOk.setEnabled(false);
		}
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
		PlanTarget pt;
		int i = tabFolder.getSelectionIndex();
		Control[] ctls = tabFolder.getChildren();
		
		if(i == 0){//numeric
			if(toUpdate == null){
				pt = new NumericPlanTarget();
				pt.setPlan(parentPlan);
			}else{
				pt = toUpdate;
			}

			double value;
			try{
				value = ((NumericPlanTargetPropertyPage)ctls[0]).getTargetValue();
			}catch(Exception e){
				return false;
			}				
			String name = ((NumericPlanTargetPropertyPage)ctls[0]).getTargetName();
			String op = ((NumericPlanTargetPropertyPage)ctls[0]).getTargetOp();
			String type = ((NumericPlanTargetPropertyPage)ctls[0]).getTargetType();
			if(name == "" || op =="" || type ==""){
				return false;
			}
			op = op.replace("[", "");
			op = op.replace("]", "");
			type = type.replace("[", "");
			type = type.replace("]", "");
			((NumericPlanTarget)pt).setValue(value);
			((NumericPlanTarget)pt).setOp(op);
			((NumericPlanTarget)pt).setType(type);
			pt.setName(name);

			
//		}else if(i==1){ //spatial
			//seems to be 2 controls for each tab, so the 2nd tab is 3 in a 0-indexed array
//			NumericPlanTarget pt = (NumericPlanTarget)t;
//			pt.setCat(PlanTarget.tarCategory.SPATIAL);
		}else if(i == 2){//admin
			if(toUpdate == null){
				pt = new AdministrativePlanTarget();
				pt.setPlan(parentPlan);
			}else{
				pt = (AdministrativePlanTarget)toUpdate;
			}
			//seems to be 2 controls for each tab, so the 3rd tab is 4 in a 0-indexed array
			String name = ((AdministrativePlanTargetPropertyPage)ctls[4]).getTargetName();
			String desc = ((AdministrativePlanTargetPropertyPage)ctls[4]).getTargetDesc();
			desc= desc.replace("[", "");
			desc = desc.replace("]", "");
			((AdministrativePlanTarget)pt).setTargetDesc(desc);
			pt.setName(name);
		}else{
			return false;
		}
		
		if(toUpdate == null){
			parentPlan.addTarget(pt);
		}
		return true;
	}

	public void addListener(int code, Listener listener) {
		this.getShell().addListener(code, listener);
	}
	
	
}
