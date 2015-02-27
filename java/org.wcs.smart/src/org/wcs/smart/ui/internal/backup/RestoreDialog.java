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
import org.wcs.smart.internal.Messages;

/**
 * Dialog for restoring backup file.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class RestoreDialog extends TitleAreaDialog {

	private Text txtRestorefile;
	private File selectedFile;
	
	private String title;
	private String message;
	private String dialogTitle;
	private String okButtonText;
	/**
	 * @param parentShell
	 */
	public RestoreDialog(Shell parentShell, String title, 
			String message, String dialogTitle,String okButtonText) {
		super(parentShell);
		
		this.title = title;
		this.message = message;
		this.dialogTitle = dialogTitle;
		this.okButtonText = okButtonText;
	}
	
	
	/**
	 * @return the restore file selected by the user
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
		lbl.setText(Messages.RestoreDialog_FileNameLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtRestorefile = new Text(main, SWT.SINGLE | SWT.BORDER);
		txtRestorefile.setEditable(true);
		txtRestorefile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		int width = txtRestorefile.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		if (width > 400){
			width = 400;
		}
		((GridData)txtRestorefile.getLayoutData()).widthHint = width;
		txtRestorefile.setSelection(txtRestorefile.getText().length());
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText(Messages.RestoreDialog_BrowseButton);
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				File f = new File(txtRestorefile.getText());
				fd.setFilterPath(f.getParent());
				fd.setFileName(f.getName());
				fd.setFilterNames(new String[]{"zip (*.zip)", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterExtensions(new String[]{"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				
				String file = fd.open();
				if (file == null){
					return;
				}else{
					if (!file.endsWith(".zip")){ //$NON-NLS-1$
						file = file + ".zip"; //$NON-NLS-1$
					}
					txtRestorefile.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));		
//		setTitle("Restore SMART backup");
//		setMessage("Select the file to restore.");
//		super.getShell().setText("Restore");
		
		setTitle(this.title);
		setMessage(this.message);
		super.getShell().setText(this.dialogTitle);
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		//createButton(parent, IDialogConstants.OK_ID, "Restore", true);
		createButton(parent, IDialogConstants.OK_ID, this.okButtonText, true);
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
			File file = new File(txtRestorefile.getText());
			if (!file.exists()){
				MessageDialog.openError(getShell(), Messages.RestoreDialog_Error_DialogTitle, Messages.RestoreDialog_FileError_DialogMessage + file.getAbsolutePath() );
				return;
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
