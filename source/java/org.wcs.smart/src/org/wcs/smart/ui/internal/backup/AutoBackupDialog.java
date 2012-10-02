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
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.wcs.smart.util.SmartUtils;

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
	private Text deleteDays;
	private Text txtBackupDir;

	private ControlDecoration cdTimer;
	private ControlDecoration cdDeleteTimer;
	private ControlDecoration cdLoc;
	
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
		KeyListener validate = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				validate();
			}
		};
		
		Composite composite = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		int indent = 20;
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText("How often would you like the system to perform an automatic backup? ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
		Composite backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		backup.setLayout(new GridLayout(3, false));
		lbl = new Label(backup, SWT.NONE);
		lbl.setText("Every ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = indent;

		days = new Text(backup, SWT.BORDER);
		String timer = "-1";
		if(prop.containsKey(AutoBackupEngine.PROP_BACKUP_TIMER)){
			timer = prop.getProperty(AutoBackupEngine.PROP_BACKUP_TIMER);
		}
		days.setText(timer);
		days.setTextLimit(3);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 18;
		days.setLayoutData(data);
		
		days.addKeyListener(validate);
		
		lbl = new Label(backup, SWT.NONE);
		lbl.setText(" days*");
		
		
		
		Label lbl2 = new Label(main, SWT.NONE);
		lbl2.setText("* Enter -1 to turn off auto-backup. Enter 0 to perform an auto-backup every time the application is closed."); 
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
		((GridData)lbl2.getLayoutData()).horizontalIndent = indent;
		
		lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("When should automatic backup files be deleted? ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		backup.setLayout(new GridLayout(3, false));
		
		Label dlbl = new Label(backup, SWT.NONE);
		dlbl.setText("Delete files older than ");
		dlbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)dlbl.getLayoutData()).horizontalIndent = indent;

		deleteDays = new Text(backup, SWT.BORDER);
		String deletetimer = "30";
		if(prop.containsKey(AutoBackupEngine.PROP_DELETE_TIMER)){
			deletetimer = prop.getProperty(AutoBackupEngine.PROP_DELETE_TIMER);
		}
		deleteDays.setText(deletetimer);
		deleteDays.setTextLimit(3);
		GridData ddata = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		ddata.widthHint = 18;
		deleteDays.setLayoutData(ddata);
		
		deleteDays.addKeyListener(validate);
		
		Label ddayslbl = new Label(backup, SWT.NONE);
		ddayslbl.setText(" days");

		lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		Label lblb = new Label(main, SWT.NONE);
//		lblb.setText("File:");
//		lblb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText("Where should automatic backup files be placed? ");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		backup.setLayout(new GridLayout(2, false));
		
		
		txtBackupDir = new Text(backup, SWT.DEFAULT);
		File temp = new File(System.getProperty("user.dir"));
		String loc = temp.getParent() + File.separatorChar + "SMART_Backups";
		File b = new File(loc);
		if(!b.exists()){
			SmartUtils.createDirectory(b);
		}
		if(prop.containsKey(AutoBackupEngine.PROP_BACKUP_LOCATION)){
			loc = prop.getProperty(AutoBackupEngine.PROP_BACKUP_LOCATION);
		}
		txtBackupDir.setText(loc);
		
		txtBackupDir.setEditable(true);
		txtBackupDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)txtBackupDir.getLayoutData()).horizontalIndent = indent;
		txtBackupDir.setSelection(txtBackupDir.getText().length());
		
		txtBackupDir.addKeyListener(validate);
		
		Button btnBrowse = new Button(backup, SWT.NONE);
		btnBrowse.setText("Browse...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog fd = new DirectoryDialog(getShell(), SWT.SAVE);
				File f = new File(txtBackupDir.getText());
				fd.setFilterPath(f.getParent());
				
				String file = fd.open();
				if (file == null){
					return;
				}else{
					txtBackupDir.setText(file);
				}
			}
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		
		cdTimer = createDecoration(days);
		cdDeleteTimer = createDecoration(deleteDays);
		cdLoc = createDecoration(txtBackupDir);
		

		setTitle("Auto-Backup Configuration");
		setMessage("Each time the application is closed, the automatic backup system checks to see whether the specified time has passed and a backup is warranted. If so, the system will perform a backup."); 
		super.getShell().setText("SMART System Automatic Backup Settings");
		
		validate();
		
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
			prop.setProperty(AutoBackupEngine.PROP_BACKUP_TIMER, days.getText());
			prop.setProperty(AutoBackupEngine.PROP_DELETE_TIMER, deleteDays.getText());
			prop.setProperty(AutoBackupEngine.PROP_BACKUP_LOCATION, txtBackupDir.getText());
			if(!prop.containsKey(AutoBackupEngine.PROP_LASTBACKUP)){
				prop.setProperty(AutoBackupEngine.PROP_LASTBACKUP, "0");
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

	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}

	/**
	 * Validate the input fields
	 * 
	 * @return <code>false</code> if not complete, <code>true</code> otherwise
	 */
	public boolean validate() {

		ControlDecoration cds[] = { cdTimer, cdDeleteTimer, cdLoc};
		for (int i = 0; i < cds.length; i++) {
			cds[i].hide();
		}
		
		boolean isComplete = true;
		if ( ! isNumeric(days.getText()) ){
			cdTimer.show();
			cdTimer.setDescriptionText("Invalid value, you must specify a valid number of days.");
			isComplete = false;
		}
		
		if ( ! isNumeric(deleteDays.getText()) ){
			cdDeleteTimer.show();
			cdDeleteTimer.setDescriptionText("Invalid value, you must specify a valid number of days.");
			isComplete = false;
		}
		File f = new File(txtBackupDir.getText());
		if (!f.exists()){
			cdLoc.show();
			cdLoc.setDescriptionText("Invalid Directory, you must select a valid directory.");
			isComplete = false;
		}
		Button x = getButton(OK);
		if (x != null){
			x.setEnabled(isComplete);
		}
		return isComplete;
	}
	
	/**
	 * Determines if string can be parsed to double
	 * @param str
	 * @return
	 */
	private static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}


