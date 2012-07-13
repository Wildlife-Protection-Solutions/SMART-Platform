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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.backup.AutoBackupEngine;

/**
 * Dialog for displaying system
 * automatic backup options to the user.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AutoBackupDialog extends TitleAreaDialog {

	private Properties prop;
	private Text days;
	private Text deletedays;
	private Text txtBackupFile;

	
	/**
	 * @param parentShell
	 */
	public AutoBackupDialog(Shell parentShell) {
		super(parentShell);
		prop = AutoBackupEngine.getAutoBackupProperties();
	}
	
	/**
	 * The <code>TitleAreaDialog</code> implementation of this
	 * <code>Window</code> methods returns an initial size which is at least
	 * some reasonable minimum.
	 * 
	 * @return the initial size of the dialog
	 */
	protected Point getInitialSize() {
		Point shellSize = super.getInitialSize();
		return new Point(Math.min(600, shellSize.x),
				
						shellSize.y);
	}
	
	/**
	 * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		
		
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
//		Label lbldesc = new Label(main, SWT.WRAP);
//		Label lbldesc2 = new Label(main, SWT.NONE);
//
//		
//		lbldesc.setText("The automatic backup system checks each time the application is closed if the specified time has passed to ");
//		lbldesc2.setText("warrant a backup. This means that no backups will occur if the application is left running indefinietly.");
//		
//		lbldesc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
//		lbldesc2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
//
//
//		Label spc = new Label(main, SWT.NONE);
//		spc.setText("");
//		spc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
		
		int indent = 20;
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("How often would you like the system to automatically backup? ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
		Composite backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		backup.setLayout(new GridLayout(3, false));
		lbl = new Label(backup, SWT.NONE);
		lbl.setText("Every");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = indent;

		days = new Text(backup, SWT.BORDER);
		String timer = "-1";
		if(prop.containsKey("backup_timer")){
			timer = prop.getProperty("backup_timer");
		}
		days.setText(timer);
		days.setTextLimit(3);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 18;
		days.setLayoutData(data);
		
		lbl = new Label(backup, SWT.NONE);
		lbl.setText(" days*");
		
		
		
		Label lbl2 = new Label(main, SWT.NONE);
		lbl2.setText("*Enter -1 to turn off auto-backup. Enter 0 to back-up every time you shut-down"); 
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
		((GridData)lbl2.getLayoutData()).horizontalIndent = indent;
		
		lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("When should automatick backup files be deleted? ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		backup.setLayout(new GridLayout(3, false));
		
		Label dlbl = new Label(backup, SWT.NONE);
		dlbl.setText("Delete files older than ");
		dlbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)dlbl.getLayoutData()).horizontalIndent = indent;

		deletedays = new Text(backup, SWT.BORDER);
		String deletetimer = "30";
		if(prop.containsKey("delete_timer")){
			deletetimer = prop.getProperty("delete_timer");
		}
		deletedays.setText(deletetimer);
		deletedays.setTextLimit(3);
		GridData ddata = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		ddata.widthHint = 18;
		deletedays.setLayoutData(ddata);
		
		Label ddayslbl = new Label(backup, SWT.NONE);
		ddayslbl.setText(" days");

		lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		Label lblb = new Label(main, SWT.NONE);
//		lblb.setText("File:");
//		lblb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Where should automatick backup files be placed? ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		backup.setLayout(new GridLayout(2, false));
		
		
		txtBackupFile = new Text(backup, SWT.DEFAULT);
		File temp = new File(System.getProperty("user.dir"));
		String loc = temp.getParent() + File.separatorChar + "SMART_Backups"; 
		if(prop.contains("backup_location")){
			loc = prop.getProperty("backup_location");
		}
		txtBackupFile.setText(loc);
		
		txtBackupFile.setEditable(true);
		txtBackupFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)txtBackupFile.getLayoutData()).horizontalIndent = indent;
		txtBackupFile.setSelection(txtBackupFile.getText().length());
		
		Button btnBrowse = new Button(backup, SWT.NONE);
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

		setTitle("Auto-Backup Configuration");
		setMessage("The automatic backup system checks each time the application is closed if the specified time has passed to warrant a backup. This means that no backups will occur if the application is left running indefinietly."); 
		super.getShell().setText("SMART System Automitic Backup Settings");
		
		return composite;
	}
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "Save", true);
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
			prop.setProperty("delete_timer", deletedays.getText());
			prop.setProperty("backup_location", txtBackupFile.getText());
			if(!prop.containsKey("last_backup")){
				prop.setProperty("last_backup", "0");
			}
			if (!AutoBackupEngine.setAutoBackupProperties(prop)){
				return;
			}
			super.setReturnCode(IDialogConstants.OK_ID);
		}
		close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

}


