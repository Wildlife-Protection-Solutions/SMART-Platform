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

package org.wcs.smart.plan.ui.targets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;


/**
 * 
 * Dialog box for adding and edition plan targets
 * 
 * @author Jeff
 * @author Emily
 *
 */
public class TargetPropertyPage extends Dialog {

	private static final int TAB_FOLDER_HEIGHT_HINT = 340;
	private static final int TAB_FOLDER_WIDTH_HINT = 400;
	
	private List<PlanTarget> parentTargets;
	private PlanTarget toUpdate;
	
	private String title = null;
	
	private TabFolder tabFolder; 
	private List<ITargetPage> tabs;
	
	/**
	 * Create the dialog.
	 * 
	 * 
	 * 
	 * @param parent
	 * @param style
	 */
	public TargetPropertyPage(Shell parent,  
		List<PlanTarget> parentTargets, PlanTarget toUpdate) {
		super(parent);

		this.toUpdate = toUpdate;
		this.parentTargets = parentTargets;
		
		if (toUpdate == null){
			title = "Create Target";
		}else{
			title = "Update Target: " + toUpdate.getName();
		}
		
		tabs = new ArrayList<ITargetPage>();
		tabs.add(new NumericPlanTargetPropertyPage(this));
		tabs.add(new AdministrativePlanTargetPropertyPage(this));
		tabs.add(new SpatialPlanTargetPropertyPage(this));
	
	}

	private void init() {
		if(toUpdate == null){
			return; //get out if we are creating a new target, nothing to initialize
		}
		
		List<TabItem> toDispose = new ArrayList<TabItem>();
		for (int i = 0; i < tabs.size(); i ++){
			ITargetPage tab = tabs.get(i);			
			if (toUpdate.getClass() == tab.createTarget().getClass()){
				tab.initPage(toUpdate);
			}else{
				toDispose.add(tabFolder.getItems()[i]);
			}
			
		}
		for (TabItem c : toDispose){
			c.dispose();
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
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tabFolder.getLayoutData()).widthHint = TAB_FOLDER_WIDTH_HINT;
		((GridData)tabFolder.getLayoutData()).heightHint = TAB_FOLDER_HEIGHT_HINT;
		for (ITargetPage page : tabs){
			TabItem item = new TabItem(tabFolder, SWT.NONE);
			item.setText(page.getPageName());
			item.setControl(page.createComponent(tabFolder, SWT.NONE));
		}
		
		tabFolder.pack();
		
		init();
		return parent;
	}
	
	/**
	 * Enable/disable the ok button
	 * @param val
	 */
	public void enableOK(boolean val){
		if(getButton(IDialogConstants.OK_ID) != null){
			getButton(IDialogConstants.OK_ID).setEnabled(val);
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, "Save", true);
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
	
	/*
	 * adds the plan target to the plan
	 */
	private boolean performSave(){
		PlanTarget pt;
		ITargetPage target; 
		
		//create new target if necessary
		if (toUpdate == null){
			target = tabs.get(tabFolder.getSelectionIndex());
			pt = target.createTarget();
			//pt.setPlan(parentPlan);
			parentTargets.add(pt);
			target.updateTarget(pt);
		}else{
			//some hacked code here because we drop the other tabs from target types we are not editing, so the tabFolder.getSelectionIndex()
			// will always return 0 even though we need to know if it's a numeric, admin , or map target type... 
			if(toUpdate instanceof NumericPlanTarget){
				target = tabs.get(0);
			}else if(toUpdate instanceof AdministrativePlanTarget){
				target = tabs.get(1);
			}else{
				target = tabs.get(2);
			}
			target.updateTarget(toUpdate);
		}
		return true;
	}

	public void addListener(int code, Listener listener) {
		this.getShell().addListener(code, listener);
	}
	
	
}
