/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.qa;

import java.time.LocalDate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.patrol.CleanPatrolEngine;
import org.wcs.smart.cybertracker.patrol.CleanPatrolSettings;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SmartUtils;

import com.ibm.icu.text.MessageFormat;

/**
 * Configuration and run dialog for cleaning smart mobile patrols
 * which did not get ended.
 * 
 * @since 8.1.0
 */
public class CleanAndFixDialog extends SmartStyledTitleDialog{

	private static final String START_KEY = CleanPatrolSettings.KEY + ".start"; //$NON-NLS-1$
	private static final String END_KEY =  CleanPatrolSettings.KEY + ".end"; //$NON-NLS-1$
	
	private Text txtStatus, txtDays, txtDistance, txtClusterDistance, txtClusterMinutes;
	private DateTime dtStart, dtEnd;
	private Button btnGo, btnSaveSetting;

	private String statusMessage = null;
	
	public CleanAndFixDialog(Shell parentShell) {
		super(parentShell);
	}

	
	@Override
	public boolean close(){
		if (btnSaveSetting.isEnabled()) {
			int ret = MessageDialog.open(MessageDialog.QUESTION_WITH_CANCEL, getShell(), "Save" , "There are unsaved changes to the settings. Do you want to save these settings?", SWT.NONE, IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL );
			if (ret == 2) return false;
			if (ret == 0) {
				if (!saveSettings()) return false;
			}			
		}
		return super.close();
	}
	

	private void saveDates() {
		CyberTrackerPlugIn.getDefault().getPreferenceStore().putValue(START_KEY, SmartUtils.toDate(dtStart).toString());
		CyberTrackerPlugIn.getDefault().getPreferenceStore().putValue(END_KEY, SmartUtils.toDate(dtEnd).toString());
	}
	
	private void setSettingModified(boolean isModified) {
		btnSaveSetting.setEnabled(isModified);		
	}
	
	private void initSettings() {
		String value = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(START_KEY);
		if (value != null && !value.isEmpty()) {
			LocalDate start = LocalDate.parse(value);
			SmartUtils.initDateTimeWidget(dtStart, start);
		}
		
		value = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(END_KEY);
		if (value != null && !value.isEmpty()) {
			LocalDate end = LocalDate.parse(value);
			SmartUtils.initDateTimeWidget(dtEnd, end);
		}
		
		
		CleanPatrolSettings settings = null;
		try(Session session = HibernateManager.openSession()){
			try {
				session.beginTransaction();
				settings = CleanPatrolEngine.getOrCreateSettings(session, SmartDB.getCurrentConservationArea());
				session.getTransaction().commit();
			}catch (Exception ex) {
				SmartPlugIn.displayLog(ex.getMessage(), ex);
			}
		}
		if (settings != null) {
			txtDays.setText( String.valueOf(settings.getDays()) );
			
			txtDistance.setText( String.valueOf(settings.getValidTrackDistance()) );
			txtClusterDistance.setText( String.valueOf(settings.getClusterTrackDistance()) );
			txtClusterMinutes.setText( String.valueOf(settings.getClusterMinutes()) );			
		}
		btnSaveSetting.setEnabled(false);

	}
	
	private boolean saveSettings() {
		String[] propKeys = new String[] {CleanPatrolSettings.DISTANCE_KEY, CleanPatrolSettings.CLUSTER_DISTANCE_KEY, CleanPatrolSettings.CLUSTER_MINUTES_KEY, CleanPatrolSettings.DAYS_KEY};
		Text[] propText = new Text[] {txtDistance, txtClusterDistance, txtClusterMinutes, txtDays};
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (int i = 0; i < propKeys.length; i ++) {
					ConservationAreaProperty prop = QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},  //$NON-NLS-1$
							new Object[] {"key", propKeys[i]}).uniqueResult();  //$NON-NLS-1$
					
					if (prop == null) {
						prop = new ConservationAreaProperty();
						prop.setConservationArea(SmartDB.getCurrentConservationArea());
						prop.setKey(propKeys[i]);
						session.persist(prop);
					}
					prop.setValue(propText[i].getText());
				}
				session.getTransaction().commit();
				setSettingModified(false);
				return true;
			}catch (Exception ex) {
				session.getTransaction().rollback();
				SmartPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		}
	}
	
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		
		CTabFolder tabs = new CTabFolder(composite,  SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		CTabItem runItem = new CTabItem(tabs, SWT.NONE);
		runItem.setText("Clean Patrols");
		
		Composite runPanel = createRunPanel(tabs);
		runItem.setControl(runPanel);

		CTabItem settingsItem = new CTabItem(tabs, SWT.NONE);
		settingsItem.setText("Settings");
		
		Composite settingsPanel = createSettingPanel(tabs);
		settingsItem.setControl(settingsPanel);
		
		tabs.setSelection(runItem);
		
		
		getShell().setText("Clean & End Patrols");
		setTitle("Clean && End Patrols");
		setMessage("Remove empty days and end SMART Mobile patrols that have not sent an 'End Patrol' observation.");
		
		initSettings();
		
		return composite; 
	}
	
	
	private void validateSettings() {
		try {
			int x = Integer.parseInt(txtDays.getText());
			if (x < 3 || x > 100) {
				throw new Exception("Days must be between 2 and 100");
			}
			
			x = Integer.parseInt(txtDistance.getText());
			if (x < -1 || x > CleanPatrolSettings.MAX_DISTANCE) {
				throw new Exception(MessageFormat.format("Distance must be between -1 and {0}", CleanPatrolSettings.MAX_DISTANCE));
			}
			
			x = Integer.parseInt(txtClusterDistance.getText());
			if (x < -1 || x > CleanPatrolSettings.MAX_DISTANCE) {
				throw new Exception(MessageFormat.format("Distance must be between -1 and {0}", CleanPatrolSettings.MAX_DISTANCE));
			}
			
			x = Integer.parseInt(txtClusterMinutes.getText());
			if (x < 1 || x > 120) {
				throw new Exception("Timeframe must be between 1 and 120");
			}
		}catch (Exception ex) {
			setErrorMessage(ex.getMessage());
			btnGo.setEnabled(false);
			return;
		}
		setErrorMessage(null);
		btnGo.setEnabled(true);
		setSettingModified(true);
	}
	
	private Composite createSettingPanel(Composite parent) {
		Composite settings = new Composite(parent, SWT.NONE);
		settings.setLayout(new GridLayout(3, false));
		settings.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Listener lmodifed = e->validateSettings();
		
		Composite h = SmartUiUtils.createHeaderLabel(settings, "Data Filter");
		h.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		Composite info = new Composite(settings, SWT.NONE);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		info.setLayout(new GridLayout(2, false));
		Label l = new Label(info, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(info, SWT.NONE);
		l.setText("These settings determine what data will be cleaned up.");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(settings, SWT.NONE);
		l.setText("Days since last observation:");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtDays = new Text(settings, SWT.BORDER);
		txtDays.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		txtDays.setText(CleanPatrolSettings.DEFAULT_DAYS);
		txtDays.addListener(SWT.Modify, lmodifed);
				
		l = new Label(settings, SWT.WRAP);
		l.setText("Only non-ended SMART Mobile patrols with an observation more than x days ago will be processed.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 250;
		
		
		h = SmartUiUtils.createHeaderLabel(settings, "Last Valid Day");
		h.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		info = new Composite(settings, SWT.NONE);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		info.setLayout(new GridLayout(2, false));
		l = new Label(info, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(info, SWT.WRAP);
		l.setText("This setting is used to determine what day is the last valid day with data. The last valid day is the latest day in the patrol with observations or valid track data.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		((GridData)l.getLayoutData()).widthHint = 250;

		
		l = new Label(settings, SWT.NONE);
		l.setText("Track distance (m):");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtDistance = new Text(settings, SWT.BORDER);
		txtDistance.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		txtDistance.setText(CleanPatrolSettings.DEFAULT_DISTANCE);
		txtDistance.addListener(SWT.Modify, lmodifed);
		
		l = new Label(settings, SWT.WRAP);
		l.setText("If any track points for the day are further than this distance apart, the track is assumed to be valid (ie not clusted all in one location). Set to -1 to assume all track points are valid.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 250;
		
		h = SmartUiUtils.createHeaderLabel(settings, "Track Clean Settings");
		h.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		info = new Composite(settings, SWT.NONE);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		info.setLayout(new GridLayout(2, false));
		
		l = new Label(info, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		l = new Label(info, SWT.NONE);
		l.setText("These setting are used to remove clusters of track points at the end of the patrol.");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		
		l = new Label(settings, SWT.NONE);
		l.setText("Cluster distance (m):");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtClusterDistance = new Text(settings, SWT.BORDER);
		txtClusterDistance.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		txtClusterDistance.setText(CleanPatrolSettings.DEFAULT_DISTANCE);
		txtClusterDistance.addListener(SWT.Modify, lmodifed);
		
		
		l = new Label(settings, SWT.WRAP);
		l.setText("Track points at the end of patrol that are clusted within this distance will be collapsed to a single point. If all track points are within this distance, the day will be removed from the patrol. Set to -1 to never collapse track points.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 250;
		
		l = new Label(settings, SWT.NONE);
		l.setText("Cluster timeframe (minutes):");
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		txtClusterMinutes = new Text(settings, SWT.BORDER);
		txtClusterMinutes.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData)txtClusterMinutes.getLayoutData()).widthHint = 50;
		txtClusterMinutes.setText(CleanPatrolSettings.DEFAULT_MINUTES);
		txtClusterMinutes.addListener(SWT.Modify, lmodifed);
		
		l = new Label(settings, SWT.WRAP);
		l.setText("This timeframe is used to determine the center point for collapsing track points. All points within x minutes of the end of the patrol are collected, the center found, a buffer of distance Y generated, then all points at the end of the patrol within this area collapsed to that single point.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 250;
		
		Composite btnPanel = new Composite(settings, SWT.NONE);
		btnPanel.setLayout(new GridLayout(2, false));
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		
		Button btnDefaults = new Button(btnPanel, SWT.PUSH);
		btnDefaults.setText("Restore Defaults");
		btnDefaults.addListener(SWT.Selection, e->{
			txtDays.setText(CleanPatrolSettings.DEFAULT_DAYS);
			txtDistance.setText(CleanPatrolSettings.DEFAULT_DISTANCE);
			txtClusterDistance.setText(CleanPatrolSettings.DEFAULT_DISTANCE);
			txtClusterMinutes.setText(CleanPatrolSettings.DEFAULT_MINUTES);
		});

		btnSaveSetting = new Button(btnPanel, SWT.PUSH);
		btnSaveSetting.setText("Save Settings");
		btnSaveSetting.addListener(SWT.Selection, e->saveSettings());
		btnSaveSetting.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON));
		btnSaveSetting.setEnabled(false);
		
		return settings;
	}
	
	
	
	private Composite createRunPanel(Composite parent) {
		Composite output = new Composite(parent, SWT.NONE);
		output.setLayout(new GridLayout(4, false));
		output.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(output, SWT.WRAP);
		l.setText("Process patrols that start between ");
		
		dtStart = new DateTime(output,  SWT.DROP_DOWN);
		
		l = new Label(output, SWT.NONE);
		l.setText(" and ");
		
		dtEnd = new DateTime(output, SWT.DROP_DOWN);
		
		btnGo = new Button(output, SWT.NONE);
		btnGo.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 4, 1));
		btnGo.setText("Clean && End Patrols");
		btnGo.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
		btnGo.addListener(SWT.Selection,  e->doWork());
			
		txtStatus = new Text(output, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		txtStatus.setEditable(false);
		txtStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		((GridData)txtStatus.getLayoutData()).heightHint = 150;
		
		return output;
	}
	
	private void enableControls(boolean enable) {
		dtStart.setEnabled(enable);
		dtEnd.setEnabled(enable);
		btnGo.setEnabled(enable);
		
		txtDays.setEnabled(enable);
		txtDistance.setEnabled(enable);
	}
	
	private void doWork() {

		LocalDate start = SmartUtils.toDate(dtStart);
		LocalDate end = SmartUtils.toDate(dtEnd);
		
		int days = Integer.valueOf(txtDays.getText());
		int distance = Integer.valueOf(txtDistance.getText());
		int minutes = Integer.valueOf(txtClusterMinutes.getText());
		int cdistance = Integer.valueOf(txtClusterDistance.getText());
		
		CleanPatrolSettings settings = new CleanPatrolSettings();
		settings.setDays(days);
		settings.setValidTrackDistance(distance);
		settings.setClusterMinutes(minutes);
		settings.setClusterTrackDistance(cdistance);
		
		saveDates();
		
		enableControls(false);
		txtStatus.setText(""); //$NON-NLS-1$
		
		Job j = new Job("processing patrol") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					UnendedPatrolProcessor processor = new UnendedPatrolProcessor(SmartDB.getCurrentConservationArea(), settings);
					processor.doWork(start, end, txtProgress);
					updateMessage(processor.getStatusMessage());
					
					Display.getDefault().syncExec(()->{					
						enableControls(true);
					});
				}catch (Exception ex) {
					Display.getDefault().syncExec(()->{
						SmartPlugIn.displayLog(ex.getMessage(), ex);
						enableControls(true);
					});
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
		
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	
	
	private void updateMessage(String newMessage) {
		statusMessage = newMessage;
		
		Display.getDefault().asyncExec(()->{
			txtStatus.setText(statusMessage);
		});
	}
	
	
	IProgressMonitor txtProgress = new IProgressMonitor() {
		
		private int size;
		private int current;
		private String message = "Processing";
		
		
		@Override
		public void worked(int work) {
			current ++;
			
			updateMessage(MessageFormat.format("{0} {1} / {2}", message, current, size));
		}
		
		@Override
		public void subTask(String name) {
		}
		
		@Override
		public void setTaskName(String name) {
		}
		
		@Override
		public void setCanceled(boolean value) {
		}
		
		@Override
		public boolean isCanceled() {
			return false;
		}
		
		@Override
		public void internalWorked(double work) {
		}
		
		@Override
		public void done() {
		}
		
		@Override
		public void beginTask(String name, int totalWork) {
			this.message = name;
			this.size = totalWork;
			this.current = 0;
		}
	};
}
