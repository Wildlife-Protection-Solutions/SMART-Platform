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
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;


/**
 * 
 * Dialog box for adding and edition plan targets
 * 
 * @author Jeff
 * @author Emily
 *
 */
public class TargetPropertyDialog extends SmartStyledTitleDialog{

	private List<PlanTarget> parentTargets;
	private PlanTarget toUpdate;
	
	private String title = null;
	
	private List<ITargetPage> pages;
	private Composite stackPanel;
	private ITargetPage currentPage;
	
	private boolean isDirty = false;
	private boolean savePerformed = false;
	
	
	/**
	 * Create the dialog.
	 * 
	 * 
	 * 
	 * @param parent
	 * @param style
	 */
	public TargetPropertyDialog(Shell parent,  
		List<PlanTarget> parentTargets, PlanTarget toUpdate) {
		super(parent);

		this.toUpdate = toUpdate;
		this.parentTargets = parentTargets;
		
		if (toUpdate == null){
			title = Messages.TargetPropertyPage_Create_Title;
		}else{
			title = Messages.TargetPropertyPage_Update_Title + " " + toUpdate.getName(); //$NON-NLS-1$
		}
		
		pages = new ArrayList<ITargetPage>();
		if (toUpdate == null){
			//new target
			pages.add(new NumericPlanTargetPropertyPage(this));
			pages.add(new AdministrativePlanTargetPropertyPage(this));
			pages.add(new SpatialPlanTargetPropertyPage(this));
		}else{
			if (toUpdate instanceof NumericPlanTarget){
				pages.add(new NumericPlanTargetPropertyPage(this));
			}else if (toUpdate instanceof AdministrativePlanTarget){
				pages.add(new AdministrativePlanTargetPropertyPage(this));	
			}else if (toUpdate instanceof SpatialPlanTarget){
				pages.add(new SpatialPlanTargetPropertyPage(this));	
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
		if (pages.size() > 1) {
			
			Composite inner = new Composite(main, SWT.NONE);
			GridLayout gl = new GridLayout(2, false);
			gl.marginWidth = 10;
			inner.setLayout(gl);
			inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label lbl = new Label(inner, SWT.NONE);
			lbl.setText(Messages.TargetPropertyDialog_TargetTypeLabel);
			
			Composite ops = new Composite(inner, SWT.NONE);
			gl = new GridLayout(pages.size(), false);
			gl.marginHeight = 12;
			ops.setLayout(gl);
			ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

			Label lbl2 = new Label(inner, SWT.HORIZONTAL | SWT.SEPARATOR);
			lbl2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			stackPanel = new Composite(inner, SWT.NONE);
			stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
			stackPanel.setLayout(new StackLayout());
			
			/* create page controls & radio buttons*/
			
			final HashMap<ITargetPage, Control> controls = new HashMap<ITargetPage, Control>();
			
			/* create radio button options */
			for (int i = 0; i < pages.size(); i ++){
				final ITargetPage page = pages.get(i);
				Button radio = new Button(ops, SWT.RADIO);
				if (i == 0){
					currentPage = page;
					radio.setSelection(true);
				}
				radio.setText(page.getPageName());
				radio.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						currentPage = page;
						((StackLayout)stackPanel.getLayout()).topControl = controls.get(page); 
						stackPanel.layout();
						setDirty();
					}
				});
				controls.put(page, page.createComponent(stackPanel, SWT.NONE));
			};
			
			((StackLayout)stackPanel.getLayout()).topControl = controls.get(currentPage);
			setMessage(Messages.TargetPropertyPage_CreateMessage);
			
		} else if (pages.size() == 1) {
			Composite inner = new Composite(main, SWT.NONE);
			GridLayout gl = new GridLayout(1, false);
			gl.marginWidth = 10;
			gl.marginHeight = 10;
			inner.setLayout(gl);
			inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Composite kid = pages.get(0).createComponent(inner, SWT.NONE);
			kid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			setMessage(pages.get(0).getPageName());
			currentPage = pages.get(0);
		}
		if (toUpdate != null){
			for (ITargetPage page : pages){
				page.initPage(toUpdate);
			}
		}
		setTitle(Messages.TargetPropertyPage_DialogTitle);
		
		return parent;
	}

	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		if(toUpdate == null){
			btnOk.setEnabled(false);
		}
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		setDirty(false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()){
				setReturnCode(OK);
				if (closeOnSave()) {
					close();
				}
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			if (isDirty){
				//prompt to save changes
				MessageDialog dialog = new MessageDialog(getShell(), Messages.TargetPropertyDialog_CloseDialogTitle,null, Messages.TargetPropertyDialog_ConfirmClose,MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL,IDialogConstants.NO_LABEL,IDialogConstants.CANCEL_LABEL}, 0); 
				int ret = dialog.open();
				if (ret == 0){
					//yes
					if (performSave()){
						setReturnCode(OK);
						close();
						return;
					}else{
						return;
					}
				}else if (ret == 1){
					//no
					setReturnCode(CANCEL);
					close();
					return;
				}else if (ret == 2){
					//cancel
					return;
				}
			}else{
				setReturnCode(CANCEL);
				close();
			}
		}
	}

	private boolean closeOnSave() {
		//do not close if this is "Edit" operation to be consistent with editors
		//close if this is "Add" to be consistent with wizards
		return pages.size() > 1;
	}
	
	/*
	 * adds the plan target to the plan
	 */
	private boolean performSave(){
		PlanTarget pt;
		ITargetPage target =currentPage;
		
		if (!target.validate()){
			MessageDialog.openError(getShell(), Messages.TargetPropertyDialog_ErrorDialogTitle, Messages.TargetPropertyDialog_InvalidTarget);
			return false;
		}
		if (toUpdate == null){
			pt = target.createTarget();
			parentTargets.add(pt);
		}else{
			pt = toUpdate;
		}
		target.updateTarget(pt);
		setDirty(false);
		savePerformed = true;
		return true;
	}

	public void setDirty(){
		setDirty(true);
	}

	
	private void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		if (currentPage.validate()){
			getButton(IDialogConstants.OK_ID).setEnabled(isDirty);
		}else{
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	public boolean isSavePerformed() {
		return savePerformed;
	}
}
