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
package org.wcs.smart.ui.properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.internal.Messages;

/**
 * SMART property page dialog 
 * <p>
 * Manages a hibernate session and conservation area.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AbstractPropertyJHeaderDialog extends TitleAreaDialog {

	protected boolean changesMade;
	
	private String title;
	
	/**
	 * @param parentShell
	 */
	protected AbstractPropertyJHeaderDialog(Shell parent, String title) {
		super(parent);
		this.title = title;		
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);

		//Create an outer composite for spacing
		ScrolledComposite scrolled = new ScrolledComposite(composite, SWT.V_SCROLL | SWT.H_SCROLL );
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// always show the focus control
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite c = createContent(scrolled);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		scrolled.setContent(c);
		scrolled.setMinSize(scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		return composite;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CLOSE_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CLOSE_ID).setFocus();
		
		super.setReturnCode(IDialogConstants.CLOSE_ID);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			performSave();
			super.setReturnCode(IDialogConstants.OK_ID);
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			close();
		}
	}
	
	/**
	 * Updates the buttons of the dialog to reflect current
	 * state.
	 * 
	 * @param ischanged if changes have occurred and dialog needs to be saved
	 */
	protected void setChangesMade(boolean ischanged){
		this.changesMade = ischanged;
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null){
			btn.setEnabled(ischanged);
		}
	}
	
	/**
	 * If there are unsaved changes, the user is prompted to
	 * save changes then the dialog is closed.
	 */
	@Override
	public boolean close(){
		//ensure all edits are finished
		getButtonBar().setFocus();

		if (changesMade){
			if (!validateSave()){
				return false;
			}
		}

		return super.close();  
	}
	
	/**
	 * Validates if the current changes should be saved
	 * 
	 * @return <code>true</code> if users wishes to save and save was successful, <code>true</code> if user does not want to save, <code>false</code> if
	 * cancel pressed or error occured while saving.
	 */
	protected boolean validateSave(){
		if (getErrorMessage() != null){
			if (!MessageDialog.openQuestion(getShell(), Messages.AbstractPropertyJHeaderDialog_ConfirmClose_DialogTitle, Messages.AbstractPropertyJHeaderDialog_ConfirmClose_DialogMessage)){
				return false;
			}
		}else{
			MessageDialog md = new MessageDialog(getShell(), Messages.AbstractPropertyJHeaderDialog_ConfirmSave_DialogTitle, null, Messages.AbstractPropertyJHeaderDialog_ConfirmSave_DialogMessage, MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL},0);
			int ret = md.open();
			if (ret == 2){
				//cancel
				return false;
			}else if (ret == 0){
				//yes
				if (!performSave()){
					return false;
				}else{
					setReturnCode(IDialogConstants.OK_ID);
				}
			}
		}
		return true;
	}
	
	/**
	 * Create the dialog area content
	 * @param parent
	 * @return
	 */
	protected abstract Composite createContent (Composite parent);
	
	/**
	 * Save changes
	 * @return <code>true</code> if changed succesfully saved, <code>false</code> otherwise
	 */
	protected abstract boolean performSave();
	
	@Override
	protected boolean isResizable() {
		return true;
	}
}
