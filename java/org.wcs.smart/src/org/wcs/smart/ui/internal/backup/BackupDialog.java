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

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.backup.BackupEngine;

/**
 * Dialog for displaying system
 * backup options to the user.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class BackupDialog extends TitleAreaDialog {

	private Text txtBackupFile;
	private File selectedFile;
	
	/**
	 * @param parentShell
	 */
	public BackupDialog(Shell parentShell) {
		super(parentShell);
	}
	
	/**
	 * @return the default backup file name
	 * @throws IOException
	 */
	private String generateBackupFileName() throws IOException{
		String backupDir = SmartProperties.getInstance().getProperty(SmartProperties.BACKUP_DIRECTORY_KEY);
		File f = new File(backupDir + File.separator + BackupEngine.getDefaultFileName());
		return f.getCanonicalPath();
	}
	
	/**
	 * @return the backup file selected by the user
	 */
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
		lbl.setText("Backup File:");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtBackupFile = new Text(main, SWT.DEFAULT);
		try{
			txtBackupFile.setText(generateBackupFileName());
		}catch (IOException ex){
			ex.printStackTrace();
		}
		txtBackupFile.setEditable(true);
		txtBackupFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		int width = txtBackupFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		if (width > 400){
			width = 400;
		}
		((GridData)txtBackupFile.getLayoutData()).widthHint = width;
		txtBackupFile.setSelection(txtBackupFile.getText().length());
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
				File f = new File(txtBackupFile.getText());
				fd.setFilterPath(f.getParent());
				fd.setFileName(f.getName());
				fd.setFilterNames(new String[]{"zip (*.zip)", "*.*"});
				fd.setFilterExtensions(new String[]{"*.zip", "*.*"});
				
				String file = fd.open();
				if (file == null){
					return;
				}else{
					if (!file.endsWith(".zip")){
						file = file + ".zip";
					}
					txtBackupFile.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));		
		setTitle("Backup Smart System");
		setMessage("Select the file to backup the system to.");
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Backup", true);
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
			File file = new File(txtBackupFile.getText());
			if (file.exists()){
				if (!MessageDialog.openConfirm(getShell(), "Confirm Overwrite", "The file '" + file.getAbsolutePath() + "' already exists.\n\nAre you sure you want to overwrite it?")){
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
