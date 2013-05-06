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
package org.wcs.smart.cybertracker.export;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Dialog for exporting CyberTracker application data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerExportDialog extends TitleAreaDialog {

	public CyberTrackerExportDialog(Shell parentShell) {
		super(parentShell);
	}

	private Text txtFolder;
	private File selectedFile;
	
	public File getSelectedFile(){
		return this.selectedFile;
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.CyberTrackerExportDialog_Label_File);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		txtFolder = new Text(main, SWT.BORDER);
		txtFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFolder.setText("c:\\dev\\CyberTracker\\out");
		
		txtFolder.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFolder.getText() != null && !txtFolder.getText().isEmpty()) {
					if (getButton(IDialogConstants.OK_ID) != null) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}
				}
			}
		});
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.CyberTrackerExportDialog_Button_Browse);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell(), SWT.SAVE);
				
				if (txtFolder.getText() != null && !txtFolder.getText().isEmpty()) {
					dd.setFilterPath(txtFolder.getText());
				}
				String f = dd.open();
				if (f != null) {
					txtFolder.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));		
		setTitle(Messages.CyberTrackerExportDialog_Title);
		setMessage(Messages.CyberTrackerExportDialog_Message);
		super.getShell().setText(Messages.CyberTrackerExportDialog_Title);
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, Messages.CyberTrackerExportDialog_Button_Export, true);
		createButton(parent, IDialogConstants.CANCEL_ID,IDialogConstants.CANCEL_LABEL, false);
		getButton(IDialogConstants.CANCEL_ID).setFocus();
		super.setReturnCode(IDialogConstants.CANCEL_ID);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			File file = new File(txtFolder.getText());
			if (!file.exists()) {
				try {
					FileUtils.forceMkdir(file);
				} catch (IOException ex) {
					MessageDialog.openError(getShell(), Messages.CyberTrackerExportDialog_ErrDialog_Title, Messages.CyberTrackerExportDialog_Err_Dialog_Message);
					return;	
				}
			}
			selectedFile = file;
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
}
