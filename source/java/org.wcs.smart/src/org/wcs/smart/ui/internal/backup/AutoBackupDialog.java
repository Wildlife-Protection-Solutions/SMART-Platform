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
import java.util.Properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.backup.AutoBackupEngine;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for displaying system
 * automatic backup options to the user.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AutoBackupDialog extends TitleAreaDialog {

	private String title;
	private String message;
	private String buttonText;
	private Properties prop;
	private Text days;
	private Text txtBackupFile;
	private File selectedFile;

	
	/**
	 * @param parentShell
	 */
	public AutoBackupDialog(Shell parentShell, String title, String message, String buttonText) {
		super(parentShell);
		this.title = title;
		this.message = message;
		this.buttonText = buttonText;
		
		prop = AutoBackupEngine.getAutoBackupProperties();
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
		
		Label lbldesc = new Label(main, SWT.WRAP);
		Label lbldesc2 = new Label(main, SWT.NONE);

		
		lbldesc.setText("The automatic backup system checks each time the application is closed if the specified time has passed to ");
		lbldesc2.setText("warrant a backup. This means that no backups will occur if the application is left running indefinietly.");
		
		lbldesc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
		lbldesc2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));

		
		Label spc = new Label(main, SWT.NONE);
		spc.setText("");
		spc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("How often would you like the system to automatically backup? Every ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		days = new Text(main, SWT.BORDER);
		String timer = "-1";
		if(prop.contains("backup_timer")){
			timer = prop.getProperty("backup_timer");
		}
		days.setText(timer);
		days.setTextLimit(3);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		data.horizontalIndent = 0;
		data.widthHint = 18;
		days.setLayoutData(data);
		
		Label dayslbl = new Label(main, SWT.NONE);
		dayslbl.setText(" days");
		
		Label lbl2 = new Label(main, SWT.NONE);
		lbl2.setText("(Enter -1 to turn off auto-backup. Enter 0 to back-up every time you shut-down)   Location of the backups:");
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));

		
//		Label lblb = new Label(main, SWT.NONE);
//		lblb.setText("File:");
//		lblb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		txtBackupFile = new Text(main, SWT.DEFAULT);
		
		String loc = new String((new File("")).getAbsolutePath());
		if(prop.contains("backup_location")){
			loc = prop.getProperty("backup_location");
		}
		txtBackupFile.setText(loc);
		
		txtBackupFile.setEditable(true);
		txtBackupFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,2,1));
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
				DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.SAVE);
				File f = new File(txtBackupFile.getText());
				fd.setFilterPath(f.getParent());
				
				String file = fd.open();
				if (file == null){
					return;
				}else{
					txtBackupFile.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));		

		setTitle(title);
		setMessage(message); 
		super.getShell().setText(title);
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, buttonText, true);
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
			prop.setProperty("backup_timer", days.getText());
			prop.setProperty("backup_location", txtBackupFile.getText());
			if(!prop.containsKey("last_backup")){
				prop.setProperty("last_backup", "0");
			}
			AutoBackupEngine.setAutoBackupProperties(prop);
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}


