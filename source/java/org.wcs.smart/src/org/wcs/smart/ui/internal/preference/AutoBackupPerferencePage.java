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
package org.wcs.smart.ui.internal.preference;

import java.io.File;
import java.util.Properties;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.backup.AutoBackupEngine;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;
/**
 * Preference page for configuring auto-backups
 * 
 * @author Emily
 *
 */
public class AutoBackupPerferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private static final String DEFAULT_DELETE_TIMER = "30"; //$NON-NLS-1$
	private static final String DEFAULT_TIMER = "-1"; //$NON-NLS-1$
	
	private Properties prop;
	private Text days;
	private Text deleteDays;
	private Text txtBackupDir;

	private ControlDecoration cdTimer;
	private ControlDecoration cdDeleteTimer;
	private ControlDecoration cdLoc;
	
	public AutoBackupPerferencePage() {
	}

	public AutoBackupPerferencePage(String title) {
		super(title);
	}

	public AutoBackupPerferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public boolean performOk() {
		if (!isEditable()){
			return true;
		}
		prop.setProperty(AutoBackupEngine.PROP_BACKUP_TIMER, days.getText());
		prop.setProperty(AutoBackupEngine.PROP_DELETE_TIMER, deleteDays.getText());
		prop.setProperty(AutoBackupEngine.PROP_BACKUP_LOCATION, txtBackupDir.getText());
		if(!prop.containsKey(AutoBackupEngine.PROP_LASTBACKUP)){
			prop.setProperty(AutoBackupEngine.PROP_LASTBACKUP, "0"); //$NON-NLS-1$
		}
		if (!AutoBackupEngine.setAutoBackupProperties(prop)){
			return false;
		}
		return true;
	}
	
	@Override
	protected void performDefaults() {
		if (!isEditable()){
			return;
		}
		super.performDefaults();
		days.setText(DEFAULT_TIMER);
		deleteDays.setText(DEFAULT_DELETE_TIMER);
		
		File temp = new File(System.getProperty("user.dir")); //$NON-NLS-1$
		String loc = temp.getParent() + File.separatorChar + "SMART_Backups"; //$NON-NLS-1$
		txtBackupDir.setText(loc);
		validate();
	}
	
	private boolean isEditable(){
		return (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN ||
				SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER);
	}
	
	@Override
	public void init(IWorkbench workbench) {
		
	}

	private void initPref(){
		if (!isEditable()){
			return;
		}
		prop = AutoBackupEngine.getAutoBackupProperties();
		
		if(prop.containsKey(AutoBackupEngine.PROP_BACKUP_TIMER)){
			days.setText(prop.getProperty(AutoBackupEngine.PROP_BACKUP_TIMER));
			
		}
		if(prop.containsKey(AutoBackupEngine.PROP_DELETE_TIMER)){
			deleteDays.setText(prop.getProperty(AutoBackupEngine.PROP_DELETE_TIMER));
		}
		if(prop.containsKey(AutoBackupEngine.PROP_BACKUP_LOCATION)){
			txtBackupDir.setText(prop.getProperty(AutoBackupEngine.PROP_BACKUP_LOCATION));
		}
	}
	
	@Override
	protected Control createContents(Composite parent) {
		KeyListener validate = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				validate();
			}
		};
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (!isEditable()){
			Label lbl = new Label(main, SWT.WRAP);
			lbl.setText(Messages.AutoBackupPerferencePage_InvalidUser);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			((GridData)lbl.getLayoutData()).widthHint = 100;
			return main;
		}
		
		Label info1 = new Label(main, SWT.WRAP);
		info1.setText(Messages.AutoBackupDialog_Message);
		info1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)info1.getLayoutData()).widthHint = 100;
		
		Label lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		int indent = 20;
		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.AutoBackupDialog_TimeLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
				
		Composite backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		backup.setLayout(new GridLayout(3, false));
		lbl = new Label(backup, SWT.NONE);
		lbl.setText(Messages.AutoBackupDialog_BackupEveryXDays_1);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = indent;

		days = new Text(backup, SWT.BORDER);
		
		days.setText(DEFAULT_TIMER);
		days.setTextLimit(3);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 18;
		days.setLayoutData(data);
		
		days.addKeyListener(validate);
		
		lbl = new Label(backup, SWT.NONE);
		lbl.setText(Messages.AutoBackupDialog_BackupEveryXDays_2 + "*"); //$NON-NLS-1$
		
		Label lbl2 = new Label(main, SWT.WRAP);
		lbl2.setText("*" + Messages.AutoBackupDialog_TimerInfo);  //$NON-NLS-1$
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,3,1));
		((GridData)lbl2.getLayoutData()).widthHint = 100;
		
		lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.AutoBackupDialog_DeleteSectionLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		backup.setLayout(new GridLayout(3, false));
		
		Label dlbl = new Label(backup, SWT.NONE);
		dlbl.setText(Messages.AutoBackupDialog_DeleteLabel1);
		dlbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		((GridData)dlbl.getLayoutData()).horizontalIndent = indent;

		deleteDays = new Text(backup, SWT.BORDER);
		
		deleteDays.setText(DEFAULT_DELETE_TIMER);
		deleteDays.setTextLimit(3);
		GridData ddata = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		ddata.widthHint = 18;
		deleteDays.setLayoutData(ddata);
		
		deleteDays.addKeyListener(validate);
		
		Label ddayslbl = new Label(backup, SWT.NONE);
		ddayslbl.setText(Messages.AutoBackupDialog_DeleteLabel2);

		lbl = new Label(main, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.AutoBackupDialog_BackupLocationLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		backup = new Composite(main, SWT.NONE);
		backup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		backup.setLayout(new GridLayout(2, false));
		
		
		txtBackupDir = new Text(backup, SWT.BORDER | SWT.SINGLE);
		File temp = new File(System.getProperty("user.dir")); //$NON-NLS-1$
		String loc = temp.getParent() + File.separatorChar + "SMART_Backups"; //$NON-NLS-1$
		File b = new File(loc);
		if(!b.exists()){
			SmartUtils.createDirectory(b);
		}
		
		txtBackupDir.setText(loc);
		
		txtBackupDir.setEditable(true);
		txtBackupDir.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)txtBackupDir.getLayoutData()).horizontalIndent = indent;
		txtBackupDir.setSelection(txtBackupDir.getText().length());
		
		txtBackupDir.addKeyListener(validate);
		
		Button btnBrowse = new Button(backup, SWT.NONE);
		btnBrowse.setText(Messages.AutoBackupDialog_BrowseButton);
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
		

//		setTitle(Messages.AutoBackupDialog_Title);
//		setMessage(Messages.AutoBackupDialog_Message); 
//		super.getShell().setText(Messages.AutoBackupDialog_SellTitle);
		setMessage(Messages.AutoBackupDialog_Title);
		
		initPref();
		validate();
		
		return main;
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
	private boolean validate() {
		boolean isComplete = true;
		String error = null;
		if ( ! isInteger(days.getText()) || Integer.parseInt(days.getText()) < -1){
			cdTimer.show();
			error = Messages.AutoBackupDialog_Error_InvalidNumberDays;
			cdTimer.setDescriptionText(Messages.AutoBackupDialog_Error_InvalidNumberDays);
			isComplete = false;
		}else{
			cdTimer.hide();
		}
		
		if ( ! isInteger(deleteDays.getText()) || Integer.parseInt(deleteDays.getText()) < 0 ){
			cdDeleteTimer.show();
			error = Messages.AutoBackupDialog_Error_InvalidNumberDays;
			cdDeleteTimer.setDescriptionText(Messages.AutoBackupDialog_Error_InvalidNumberDays);
			isComplete = false;
		}else{
			cdDeleteTimer.hide();
			
		}
		File f = new File(txtBackupDir.getText());
		if (!f.exists()){
			cdLoc.show();
			error = Messages.AutoBackupDialog_Error_InvalidDirectory;
			cdLoc.setDescriptionText(Messages.AutoBackupDialog_Error_InvalidDirectory);
			isComplete = false;
		}else{
			cdLoc.hide();
		}
		setErrorMessage(error);
		setValid(isComplete);
		return isComplete;
	}
	
	/**
	 * Determines if string can be parsed to double
	 * @param str
	 * @return
	 */
	private static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}
