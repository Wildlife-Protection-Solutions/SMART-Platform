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

import java.util.Properties;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.InactivityTimeoutHandler;
import org.wcs.smart.user.UserLevelManager;

/**
 * Preference page from managing inactivity timeout option
 * @author Emily
 *
 */
public class InactivityTimeoutPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private Button btnCheck;
	private Label msg1, msg2;
	private Text txtTimeout;
	
	public InactivityTimeoutPreferencePage() {
	}

	public InactivityTimeoutPreferencePage(String title) {
		super(title);
	}

	public InactivityTimeoutPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}


	private boolean isEditable(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN);
	}
	
	@Override
	public boolean performOk() {
		if (!isEditable()){
			return true;
		}
		Properties prop = InactivityTimeoutHandler.INSTANCE.getProperties();

		boolean v = btnCheck.getSelection();
		prop.setProperty(InactivityTimeoutHandler.PROP_INACTIVITY_ENABLED, Boolean.toString(v));
		try {
			prop.setProperty(InactivityTimeoutHandler.PROP_INACTIVITY_TIMEOUT, Integer.valueOf(txtTimeout.getText()).toString());
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
		InactivityTimeoutHandler.INSTANCE.setProperties(prop);
		InactivityTimeoutHandler.INSTANCE.reset();
		
		return true;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout());
		((GridLayout)temp.getLayout()).verticalSpacing = 10;
		
		if (!isEditable()) {
			msg1 = new Label(temp, SWT.WRAP);
			msg1.setText(Messages.InactivityTimeoutPreferencePage_insuffientPermissions);
			msg1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)msg1.getLayoutData()).widthHint = 100;
			return temp;
		}
		
		msg1 = new Label(temp, SWT.WRAP);
		msg1.setText(Messages.InactivityTimeoutPreferencePage_infoMsg);
		msg1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)msg1.getLayoutData()).widthHint = 100;
		
		Label l = new Label(temp, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite c2 = new Composite(temp, SWT.NONE);
		c2.setLayout(new GridLayout(2, false));
		c2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridLayout)c2.getLayout()).marginWidth = 0;
		((GridLayout)c2.getLayout()).marginHeight = 0;
		
		btnCheck = new Button(c2, SWT.CHECK);
		
		l = new Label(c2, SWT.NONE);
		l.setText(Messages.InactivityTimeoutPreferencePage_EnableOp);
		l.addListener(SWT.MouseUp, e->{
			btnCheck.setSelection(!btnCheck.getSelection());
			btnCheck.notifyListeners(SWT.Selection, e);
		});
		Composite c3 = new Composite(temp, SWT.NONE);
		c3.setLayout(new GridLayout(2, false));
		c3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridLayout)c3.getLayout()).marginWidth = 0;
		((GridLayout)c3.getLayout()).marginHeight = 0;
		
		msg1 = new Label(c3, SWT.WRAP);
		msg1.setText(Messages.InactivityTimeoutPreferencePage_LogoutMsg);
		msg1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)msg1.getLayoutData()).widthHint = 50;
		
		txtTimeout = new Text(c3, SWT.BORDER);
		txtTimeout.setText("5"); //$NON-NLS-1$
		txtTimeout.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtTimeout.getLayoutData()).widthHint = 50;
		
		txtTimeout.addListener(SWT.Modify, e->{
			try {
				Integer i = Integer.valueOf(txtTimeout.getText());
				if (i < 1 || i > 24) throw new Exception(Messages.InactivityTimeoutPreferencePage_InvalidTimeoutEx);
				setErrorMessage(null);
			}catch (Exception ex) {
				setErrorMessage(Messages.InactivityTimeoutPreferencePage_InvalidValueMsg + ex.getMessage());
			}
		});
		
		msg2 = new Label(c3, SWT.NONE);
		msg2.setText(Messages.InactivityTimeoutPreferencePage_MinutesLbl);
		
		btnCheck.addListener(SWT.Selection, e->{
			msg1.setEnabled(btnCheck.getSelection());
			msg2.setEnabled(btnCheck.getSelection());
			txtTimeout.setEnabled(btnCheck.getSelection());
		});
		
		Properties prop = InactivityTimeoutHandler.INSTANCE.getProperties();
		
		boolean isenabled = Boolean.valueOf(prop.getProperty(InactivityTimeoutHandler.PROP_INACTIVITY_ENABLED));
		int time = Integer.parseInt(prop.getProperty(InactivityTimeoutHandler.PROP_INACTIVITY_TIMEOUT));
		txtTimeout.setText(String.valueOf(time));
		btnCheck.setSelection(isenabled);
		
		msg1.setEnabled(btnCheck.getSelection());
		msg2.setEnabled(btnCheck.getSelection());
		txtTimeout.setEnabled(btnCheck.getSelection());
		
		return temp;
	}

	@Override
	public void init(IWorkbench workbench) {
	
	}

}
