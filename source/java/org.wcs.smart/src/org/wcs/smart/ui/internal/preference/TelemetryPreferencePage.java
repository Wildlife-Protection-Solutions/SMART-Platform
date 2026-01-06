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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.PermissionManager;
import org.wcs.smart.TelemetryManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Customized Smart preference page for modifying the gps babel install
 * location.
 * 
 * @author egouge
 * 
 */
public class TelemetryPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.preference.telemetry"; //$NON-NLS-1$

	private Button chTelemetry;
	private Text txtData;
	private boolean initialValue = true;
	
	Job showData =new Job("loading telementry data") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (PermissionManager.INSTANCE.isAdmin()) {
				final String data = TelemetryManager.INSTANCE.packageData(true);
				Display.getDefault().asyncExec(()->setData(data));
			}
			return Status.OK_STATUS;
		}			
	};
	
	public TelemetryPreferencePage() {
		this(null);
	}
	
	public TelemetryPreferencePage(String title) {
		super(title);
		showData.setSystem(true);
		
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		
		boolean enabled = chTelemetry.getSelection();
		if (enabled == initialValue) return false; //nothing to change
		
		if (!enabled && initialValue) {
			boolean ok = MessageDialog.openQuestion(getShell(), "Telemetry", "Disabling telemetry will delete all local telemetry data and no longer send updates to SMART. Are you sure you want to disable telemetry?");
			if (!ok) {
				return true;
			}			
		}
		
		if (!TelemetryManager.INSTANCE.setEnabled(enabled)) {
			MessageDialog.openError(getShell(), DialogConstants.ERROR_STRING, "Unable to update preferences. Please restart SMART and try again. You may also check the log file for more information" );
			refreshStats();
			return false;
		}
		this.initialValue = TelemetryManager.INSTANCE.isEnabled();
		refreshStats();
		return true;
	}

	@Override
	protected void performDefaults() {
		this.chTelemetry.setEnabled(true);
	}

	
	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Label l = new Label(main, SWT.WRAP);
		l.setText("SMART collects telemetry data to help us understand how the software is used in real-world environments. This information allows us to identify potential issues and deliver features that better meet user needs. Telementry data includes feature usage, data quantity and system configuration details. SMART does NOT collect personal or identifiable information such as names, email, or user-generated content. All telemetry data is transmitted securely to our servers at regular intervals. You can see the information sent below and opt-out if desired.");
		
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 300;
		
		initialValue = TelemetryManager.INSTANCE.isEnabled();
				
		chTelemetry = new Button(main, SWT.CHECK);
		chTelemetry.setText("Collect and send telemetry details to SMART to help improve the software");
		chTelemetry.setSelection(TelemetryManager.INSTANCE.isEnabled());
		chTelemetry.addListener(SWT.Selection, e->setTelemetryEnabled());
		chTelemetry.setEnabled(PermissionManager.INSTANCE.isAdmin());		
		
		txtData = new Text(main, SWT.MULTI  | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
		txtData.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtData.setEditable(false);
		txtData.setEnabled(chTelemetry.getSelection());
		txtData.setBackground(txtData.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		txtData.setVisible(PermissionManager.INSTANCE.isAdmin());
		
		refreshStats();

		return main;
	}
	
	private void refreshStats() {
		if (PermissionManager.INSTANCE.isAdmin()) {
			txtData.setText(DialogConstants.LOADING_TEXT);
			showData.schedule();
		}
	}
	private void setData(String data) {
		if (txtData.isDisposed()) return;
		if (data == null) data = ""; //$NON-NLS-1$
		txtData.setText(data);
	}
	
	private void setTelemetryEnabled() {
		
		txtData.setEnabled(chTelemetry.getSelection());
	}
}
