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
package org.wcs.smart.ui.internal.backup;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class BackupDialog extends TitleAreaDialog {

	private Text txtBackupFile;
	
	/**
	 * @param parentShell
	 */
	public BackupDialog(Shell parentShell) {
		super(parentShell);
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);

		composite.setLayout(new GridLayout(3, false));
		
		
		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Backup File:");
		txtBackupFile = new Text(composite, SWT.NONE);
		txtBackupFile.setText("BACKUP.zip");
	
		Button btnBrowse = new Button(composite, SWT.NONE);
		btnBrowse.setText("Browse...");
//		btnBrowse.addSelectionListener(new SelectionAdapter() {
//			
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				// TODO Auto-generated method stub
//				
//			}
//			
//		});
//		
		
		return composite;
	}
	
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Backup", true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();
		
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			super.setReturnCode(IDialogConstants.OK_ID);
		} else if (IDialogConstants.CLOSE_ID == buttonId) {
			close();
		}
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}
