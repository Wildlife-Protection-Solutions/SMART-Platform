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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;
import org.wcs.smart.ui.properties.DialogConstants;


/**
 * 
 * Dialog box for adding and edition plan targets
 * 
 * @author Jeff
 * @author Emily
 *
 */
public class TargetPropertyPage extends TitleAreaDialog {

	private static final int TAB_FOLDER_HEIGHT_HINT = 480;
	private static final int TAB_FOLDER_WIDTH_HINT = 600;
	
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
			title = Messages.TargetPropertyPage_Create_Title;
		}else{
			title = Messages.TargetPropertyPage_Update_Title + " " + toUpdate.getName(); //$NON-NLS-1$
		}
		
		tabs = new ArrayList<ITargetPage>();
		if (toUpdate == null){
			//new target
			tabs.add(new NumericPlanTargetPropertyPage(this));
			tabs.add(new AdministrativePlanTargetPropertyPage(this));
			tabs.add(new SpatialPlanTargetPropertyPage(this));
		}else{
			if (toUpdate instanceof NumericPlanTarget){
				tabs.add(new NumericPlanTargetPropertyPage(this));
			}else if (toUpdate instanceof AdministrativePlanTarget){
				tabs.add(new AdministrativePlanTargetPropertyPage(this));	
			}else if (toUpdate instanceof SpatialPlanTarget){
				tabs.add(new SpatialPlanTargetPropertyPage(this));	
			}
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
		Composite main =(Composite) super.createDialogArea(parent) ;
		if (tabs.size() > 1) {
			tabFolder = new TabFolder(main, SWT.BORDER);
			tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData) tabFolder.getLayoutData()).widthHint = TAB_FOLDER_WIDTH_HINT;
			((GridData) tabFolder.getLayoutData()).heightHint = TAB_FOLDER_HEIGHT_HINT;
			for (ITargetPage page : tabs) {
				TabItem item = new TabItem(tabFolder, SWT.NONE);
				item.setText(page.getPageName());
				item.setControl(page.createComponent(tabFolder, SWT.NONE));
			}

			tabFolder.pack();
			tabFolder.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					int index = tabFolder.getSelectionIndex();
					if (index >= 0 && index < tabs.size()) {
						enableOK(tabs.get(index).validate());
					}
				}
			});
			setMessage(Messages.TargetPropertyPage_CreateMessage);
			
		} else if (tabs.size() == 1) {
			Composite kid = tabs.get(0).createComponent(main, SWT.NONE);
			kid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			setMessage(tabs.get(0).getPageName());
		}
		if (toUpdate != null){
			for (ITargetPage page : tabs){
				page.initPage(toUpdate);
			}
		}
		setTitle(Messages.TargetPropertyPage_DialogTitle);
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
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		if(toUpdate == null){
			btnOk.setEnabled(false);
		}
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
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
		if (tabFolder != null){
			target = tabs.get(tabFolder.getSelectionIndex());
		}else if (tabs.size() == 1){
			target = tabs.get(0);
		}else{
			throw new IllegalStateException("Too many target options");
		}
		
		if (toUpdate == null){
			pt = target.createTarget();
			parentTargets.add(pt);
		}else{
			pt = toUpdate;
		}
		target.updateTarget(pt);
		
		return true;
	}

	public void addListener(int code, Listener listener) {
		this.getShell().addListener(code, listener);
	}
	
	
}
