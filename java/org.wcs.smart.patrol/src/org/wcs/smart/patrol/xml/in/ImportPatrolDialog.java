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
package org.wcs.smart.patrol.xml.in;

import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for selecting file to import
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportPatrolDialog  extends TitleAreaDialog {

	private Text txtFile;
	private String fileName;
	private Text txtDirectory;
	private Button btnOpSingle;
	private Button btnOpMuliple;
	private Button btnFileBrowse;
	private Button btnDirectoryBrowse;

	/**
	 * Creates a new dialog
	 * @param parentShell parent shell
	 */
	public ImportPatrolDialog(Shell parentShell) {
		super(parentShell);

	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	protected void buttonPressed(int buttonId) {
		if (btnOpMuliple.getSelection()){
			fileName = txtDirectory.getText();
		}else{
			fileName = txtFile.getText();
		}
		super.buttonPressed(buttonId);
	}

	/**
	 * @return the filename selected by user
	 */
	public String getFileName() {
		return this.fileName;
	}


	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button b = createButton(parent, IDialogConstants.OK_ID, "Import", true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		b.setEnabled(false);
	}

	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnOpSingle = new Button(main, SWT.RADIO);
		btnOpSingle.setText("Import Single Patrol");
		btnOpSingle.addListener(SWT.Selection, new Listener() {			
			@Override
			public void handleEvent(Event event) {
				if (btnOpSingle.getSelection()){
					if (txtFile.getText().length() > 0) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}else{
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					}
				}
				
				txtFile.setEnabled(btnOpSingle.getSelection());
				btnFileBrowse.setEnabled(btnOpSingle.getSelection());
				btnDirectoryBrowse.setEnabled(!btnOpSingle.getSelection());
				txtDirectory.setEnabled(!btnOpSingle.getSelection());
			}
		});
		Composite single = new Composite(main, SWT.NONE);
		single.setLayout(new GridLayout(3, false));
		single.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)single.getLayout()).marginLeft = 15;
		
		Label lbl = new Label(single, SWT.NONE);
		lbl.setText("Source File:");
		txtFile = new Text(single, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (txtFile.getText().length() > 0) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnFileBrowse = new Button(single, SWT.NONE);
		btnFileBrowse.setText("Browse...");
		btnFileBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(ImportPatrolDialog.this
						.getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[] { "*.zip", "*.xml", "*.*" });
				fd.setFilterNames(new String[] { "zip (*.zip)", "xml (*.xml)", "All Files (*.*)" });
				
				fd.setFilterPath(txtFile.getText());
				fd.setFileName(txtFile.getText());
				String f = fd.open();
				
				if (f != null) {
					txtFile.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
				
			}
		});
		
		btnOpMuliple = new Button(main, SWT.RADIO);
		btnOpMuliple.setText("Import Multiple Patrols");
		btnOpMuliple.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				if (btnOpMuliple.getSelection()){
					if (txtDirectory.getText().length() > 0) {
						getButton(IDialogConstants.OK_ID).setEnabled(true);
					}else{
						getButton(IDialogConstants.OK_ID).setEnabled(false);
					}
				}
				
				txtFile.setEnabled(btnOpSingle.getSelection());
				btnFileBrowse.setEnabled(btnOpSingle.getSelection());
				btnDirectoryBrowse.setEnabled(!btnOpSingle.getSelection());
				txtDirectory.setEnabled(!btnOpSingle.getSelection());
				
			}
		});
		
		Composite multi = new Composite(main, SWT.NONE);
		multi.setLayout(new GridLayout(3, false));
		multi.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)multi.getLayout()).marginLeft = 15;
		lbl = new Label(multi, SWT.NONE);
		lbl.setText("Source Directory:");
		txtDirectory = new Text(multi, SWT.BORDER);
		txtDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtDirectory.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (txtDirectory.getText().length() > 0) {
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		btnDirectoryBrowse = new Button(multi, SWT.NONE);
		btnDirectoryBrowse.setText("Browse...");
		btnDirectoryBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(ImportPatrolDialog.this.getShell(), SWT.OPEN);
				dd.setMessage("Select folder containing patrol exports");
				dd.setFilterPath(txtDirectory.getText());
				String f = dd.open();
				if (f != null) {
					txtDirectory.setText(f);
					getButton(IDialogConstants.OK_ID).setEnabled(true);
				}
			}
		});
		
		txtFile.setEnabled(btnOpSingle.getSelection());
		btnFileBrowse.setEnabled(btnOpSingle.getSelection());
		btnDirectoryBrowse.setEnabled(!btnOpSingle.getSelection());
		txtDirectory.setEnabled(!btnOpSingle.getSelection());
		
		setMessage("Select the patrol data location.");
		setTitle("Import Patrol Data");
		getShell().setText("Import Patrols");
		return composite;
	}

}
