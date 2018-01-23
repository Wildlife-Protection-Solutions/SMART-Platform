/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.config;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetHibernateManager;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetModuleSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for asset module settings 
 * @author Emily
 *
 */
public class AssetSettingsDialog extends TitleAreaDialog {

	
	private Text txtStationBuffer;
	private Text txtLocationBuffer;
	
	public AssetSettingsDialog(Shell parentShell) {
		super(parentShell);
	}


	@Override
	public void okPressed() {
		if (!validate()) return;
		
		double station = Double.valueOf(txtStationBuffer.getText());
		double location = Double.valueOf(txtLocationBuffer.getText());
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				AssetModuleSettings setting = QueryFactory.buildQuery(session, AssetModuleSettings.class, 
						new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"keyId", AssetModuleSettings.STATION_BUFFER_KEY}).uniqueResult(); //$NON-NLS-1$
				if (setting == null) {
					setting = new AssetModuleSettings();
					setting.setConservationArea(SmartDB.getCurrentConservationArea());
					setting.setKeyId(AssetModuleSettings.STATION_BUFFER_KEY);
				}
				setting.setValue(String.valueOf(station));
				session.saveOrUpdate(setting);
				
				setting = QueryFactory.buildQuery(session, AssetModuleSettings.class, 
						new Object[]{"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"keyId", AssetModuleSettings.LOCATION_BUFFER_KEY}).uniqueResult(); //$NON-NLS-1$
				if (setting == null) {
					setting = new AssetModuleSettings();
					setting.setConservationArea(SmartDB.getCurrentConservationArea());
					setting.setKeyId(AssetModuleSettings.LOCATION_BUFFER_KEY);
				}
				setting.setValue(String.valueOf(location));
				session.saveOrUpdate(setting);
				
				session.getTransaction().commit();
				
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(parent, SWT.NONE);
		l.setText("Station Buffer (m):");
		l.setToolTipText("The maximum distance between an station and the incident for the incident to be automatically associated with the station.");
		
		txtStationBuffer = new Text(parent, SWT.BORDER);
		txtStationBuffer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtStationBuffer.setText(DialogConstants.LOADING_TEXT);
		txtStationBuffer.addListener(SWT.Modify,e->validate());
		
		l = new Label(parent, SWT.NONE);
		l.setText("Location Buffer (m):");
		l.setToolTipText("The maximum distance between an location and the incident for the incident to be automatically associated with the location.");
		
		txtLocationBuffer = new Text(parent, SWT.BORDER);
		txtLocationBuffer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtLocationBuffer.setText(DialogConstants.LOADING_TEXT);
		txtLocationBuffer.addListener(SWT.Modify,e->validate());
		
		setTitle("Asset Settings");
		getShell().setText("Asset Settings");
		setMessage("Manage the asset settings in the system");
		
		loadSettings.schedule();
		return parent;
	}
	
	private boolean validate() {
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		Double value = null;
		try {
			value = Double.parseDouble(txtStationBuffer.getText());
		}catch (Exception ex) {
			setErrorMessage("Invalid station buffer value.  Value must be valid number greater than zero.");
			return false;
		}
		if (value < 0) {
			setErrorMessage("Invalid station buffer value.  Value must be valid number greater than zero.");
			return false;
		}
		try {
			value = Double.parseDouble(txtLocationBuffer.getText());
		}catch (Exception ex) {
			setErrorMessage("Invalid location buffer value.  Value must be valid number greater than zero.");
			return false;
		}
		if (value < 0) {
			setErrorMessage("Invalid location buffer value.  Value must be valid number greater than zero.");
			return false;
		}
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		setErrorMessage(null);
		return true;
	}
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
		
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private Job loadSettings = new Job("load settings") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			double station = -1;
			double location = -1;
			try(Session session = HibernateManager.openSession()){
				station = AssetHibernateManager.getStationBuffer(session, SmartDB.getCurrentConservationArea());
				location = AssetHibernateManager.getStationLocationBuffer(session, SmartDB.getCurrentConservationArea());
			}
			
			final double fstation = station;
			final double flocation = location;
			
			Display.getDefault().syncExec(()->{
				txtLocationBuffer.setText(String.valueOf(flocation));
				txtStationBuffer.setText(String.valueOf(fstation));
				txtStationBuffer.setEnabled(true);
				txtLocationBuffer.setEnabled(true);
				getButton(IDialogConstants.OK_ID).setEnabled(false);
			});
			return Status.OK_STATUS;
		}
		
	};
}
