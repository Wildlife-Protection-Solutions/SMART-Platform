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
package org.wcs.smart.cybertracker.properties;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker property dialog for managing 
 * CyberTracker application default properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPropertiesDialog extends AbstractPropertyJHeaderDialog {

	private CyberTrackerProperties ctProperties;
	
	private Button btnAutoNext;

	private Button btnUseTitleBar;
	private Button btnLargeTitles;
	private Button btnLargeScrollBars;
	private Button btnLargeTabs;
	
	
	private Button btnDisableEditing;
	private Button btnSdCard;
	private Button btnTestTime;
	private Button btnResetOnSync;
	private Button btnResetOnNext;
	
	private Button btnShowEdit;
	private Button btnShowGPS;
	private Button btnKioskMode;
	private Button btnSimpleCamera;
	private Button btnCanPause;
	private Text txtExitPin;

	private Text txtSightingAccuracy;
	private Text txtSightingFixCount;
	private Text txtTrackAccuracy;
	private Text txtTrackTimer;
	
	private Button btnUseGpsTime;
    private ComboViewer timeOffset;
    private Text txtSkipButtonTimeout;
    private Text txtStorageTime;
    
    private Button btnManualGPS;
    private Button btnAllowSkipManual;
    
    private Text txtFileName;
    private Button btnLock100;
    private Button btnUseMapOnSkip;
	
    private ControlDecoration exitPinDecoration;
    
    private ControlDecoration sightingAccuracyDecoration;
    private ControlDecoration TrackAccuracyDecoration;
    private ControlDecoration sightingFixCountDecoration;
    private ControlDecoration trackTimerDecoration;
    private ControlDecoration skipButtonTimeoutDecoration;
    
    private ControlDecoration storageTimeDecoration;
    
    private ControlDecoration FileNameDecoration;
	
	public CyberTrackerPropertiesDialog() {
		super(Display.getCurrent().getActiveShell(), Messages.CyberTrackerPropertiesDialog_Title);
		Session session = HibernateManager.openSession();
		try {
			ctProperties = CyberTrackerHibernateManager.getProperties(session);
		} finally {
			session.close();
		}
		if (ctProperties == null)
			ctProperties = new CyberTrackerProperties();
	}

	@Override
	protected Composite createContent(Composite parent) {
		
		final TabFolder tabFolder = new TabFolder (parent, SWT.BORDER);
		Rectangle clientArea = parent.getClientArea ();
		tabFolder.setLocation (clientArea.x, clientArea.y);
		
		tabFolder.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		
		
		TabItem generalTab = new TabItem (tabFolder, SWT.NONE);
		generalTab.setText (Messages.CyberTrackerPropertiesDialog_0);
		
		TabItem gpsTab = new TabItem (tabFolder, SWT.NONE);
		gpsTab.setText (Messages.CyberTrackerPropertiesDialog_1);
		
		TabItem fieldmapTab = new TabItem (tabFolder, SWT.NONE);
		fieldmapTab.setText (Messages.CyberTrackerPropertiesDialog_2);
		
		Composite generalContainer = new Composite(tabFolder, SWT.None);
		generalContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		generalContainer.setLayout(new GridLayout(2, false));
		
		Composite gpsContainer = new Composite(tabFolder, SWT.None);
		gpsContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		gpsContainer.setLayout(new GridLayout(2, false));
		
		Composite fieldmapContainer = new Composite(tabFolder, SWT.None);
		fieldmapContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		fieldmapContainer.setLayout(new GridLayout(2, false));
		
		
		generalTab.setControl(generalContainer);
		gpsTab.setControl(gpsContainer);
		fieldmapTab.setControl(fieldmapContainer);
		
		
		Label lblUseTitleBar = new Label(generalContainer, SWT.NONE);
		lblUseTitleBar.setText(Messages.CyberTrackerPropertiesDialog_3);
		lblUseTitleBar.setToolTipText(Messages.CyberTrackerPropertiesDialog_4);
		

		btnUseTitleBar = new Button(generalContainer, SWT.CHECK);
		btnUseTitleBar.setToolTipText(Messages.CyberTrackerPropertiesDialog_4);
		btnUseTitleBar.setSelection(ctProperties.isUseTitleBar());
		btnUseTitleBar.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblLargeTitles = new Label(generalContainer, SWT.NONE);
		lblLargeTitles.setText(Messages.CyberTrackerPropertiesDialog_6);
		lblLargeTitles.setToolTipText(Messages.CyberTrackerPropertiesDialog_7);

		btnLargeTitles = new Button(generalContainer, SWT.CHECK);
		btnLargeTitles.setToolTipText(Messages.CyberTrackerPropertiesDialog_7);
		btnLargeTitles.setSelection(ctProperties.isUseLargeTitles());
		btnUseTitleBar.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblLargeScrollBars = new Label(generalContainer, SWT.NONE);
		lblLargeScrollBars.setText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars);
		lblLargeScrollBars.setToolTipText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars_Tooltip);

		btnLargeScrollBars = new Button(generalContainer, SWT.CHECK);
		btnLargeScrollBars.setToolTipText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars_Tooltip);
		btnLargeScrollBars.setSelection(ctProperties.isLargeScrollBars());
		btnLargeScrollBars.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblLargeTabs = new Label(generalContainer, SWT.NONE);
		lblLargeTabs.setText(Messages.CyberTrackerPropertiesDialog_9);
		lblLargeTabs.setToolTipText(Messages.CyberTrackerPropertiesDialog_10);

		btnLargeTabs = new Button(generalContainer, SWT.CHECK);
		btnLargeTabs.setToolTipText(Messages.CyberTrackerPropertiesDialog_10);
		btnLargeTabs.setSelection(ctProperties.isUseLargeTabs());
		btnLargeTabs.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});



		Label lblAutoNext = new Label(generalContainer, SWT.NONE);
		lblAutoNext.setText(Messages.CyberTrackerPropertiesDialog_AutoNext);
		lblAutoNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_AutoNext_Tooltip);

		btnAutoNext = new Button(generalContainer, SWT.CHECK);
		btnAutoNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_AutoNext_Tooltip);
		btnAutoNext.setSelection(ctProperties.isAutoNext());
		btnAutoNext.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		
		Label lblShowEdit = new Label(generalContainer, SWT.NONE);
		lblShowEdit.setText(Messages.CyberTrackerPropertiesDialog_ShowEdit);
		lblShowEdit.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowEdit_Tooltip);

		btnShowEdit = new Button(generalContainer, SWT.CHECK);
		btnShowEdit.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowEdit_Tooltip);
		btnShowEdit.setSelection(ctProperties.isShowEdit());
		btnShowEdit.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		Label lblShowGPS = new Label(generalContainer, SWT.NONE);
		lblShowGPS.setText(Messages.CyberTrackerPropertiesDialog_ShowGPS);
		lblShowGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowGPS_Tooltip);

		btnShowGPS = new Button(generalContainer, SWT.CHECK);
		btnShowGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowGPS_Tooltip);
		btnShowGPS.setSelection(ctProperties.isShowGPS());
		btnShowGPS.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		
		Label lblKioskMode = new Label(generalContainer, SWT.NONE);
		lblKioskMode.setText(Messages.CyberTrackerPropertiesDialog_KioskMode);
		lblKioskMode.setToolTipText(Messages.CyberTrackerPropertiesDialog_KioskMode_Tooltip);

		btnKioskMode = new Button(generalContainer, SWT.CHECK);
		btnKioskMode.setToolTipText(Messages.CyberTrackerPropertiesDialog_KioskMode_Tooltip);
		btnKioskMode.setSelection(ctProperties.isKioskMode());
		btnKioskMode.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		
		Label lblSimpleCamera = new Label(generalContainer, SWT.NONE);
		lblSimpleCamera.setText(Messages.CyberTrackerPropertiesDialog_SimpleCamera);
		lblSimpleCamera.setToolTipText(Messages.CyberTrackerPropertiesDialog_SimpleCamera_Tooltip);

		btnSimpleCamera = new Button(generalContainer, SWT.CHECK);
		btnSimpleCamera.setToolTipText(Messages.CyberTrackerPropertiesDialog_SimpleCamera_Tooltip);
		btnSimpleCamera.setSelection(ctProperties.isSimpleCamera());
		btnSimpleCamera.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		
		Label lblCanPause = new Label(generalContainer, SWT.NONE);
		lblCanPause.setText(Messages.CyberTrackerPropertiesDialog_CanPause);
		lblCanPause.setToolTipText(Messages.CyberTrackerPropertiesDialog_CanPause_Tooltip);

		btnCanPause = new Button(generalContainer, SWT.CHECK);
		btnCanPause.setToolTipText(Messages.CyberTrackerPropertiesDialog_CanPause_Tooltip);
		btnCanPause.setSelection(ctProperties.isCanPause());
		btnCanPause.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		
		Label lblEditing= new Label(generalContainer, SWT.NONE);
		lblEditing.setText(Messages.CyberTrackerPropertiesDialog_12);
		lblEditing.setToolTipText(Messages.CyberTrackerPropertiesDialog_13);

		btnDisableEditing = new Button(generalContainer, SWT.CHECK);
		btnDisableEditing.setToolTipText(Messages.CyberTrackerPropertiesDialog_13);
		btnDisableEditing.setSelection(ctProperties.isDisableEditing());
		btnDisableEditing.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		

		Label lblSdCard= new Label(generalContainer, SWT.NONE);
		lblSdCard.setText(Messages.CyberTrackerPropertiesDialog_15);
		lblSdCard.setToolTipText(Messages.CyberTrackerPropertiesDialog_16);

		btnSdCard = new Button(generalContainer, SWT.CHECK);
		btnSdCard.setToolTipText(Messages.CyberTrackerPropertiesDialog_16);
		btnSdCard.setSelection(ctProperties.isUseSdCard());
		btnSdCard.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		Label lblTestTime= new Label(generalContainer, SWT.NONE);
		lblTestTime.setText(Messages.CyberTrackerPropertiesDialog_18);
		lblTestTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_19);

		btnTestTime = new Button(generalContainer, SWT.CHECK);
		btnTestTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_19);
		btnTestTime.setSelection(ctProperties.isTestTime());
		btnTestTime.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		
		Label lblResetOnSync= new Label(generalContainer, SWT.NONE);
		lblResetOnSync.setText(Messages.CyberTrackerPropertiesDialog_21);
		lblResetOnSync.setToolTipText(Messages.CyberTrackerPropertiesDialog_22);

		btnResetOnSync = new Button(generalContainer, SWT.CHECK);
		btnResetOnSync.setToolTipText(Messages.CyberTrackerPropertiesDialog_22);
		btnResetOnSync.setSelection(ctProperties.isResetOnSync());
		btnResetOnSync.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblResetOnNext= new Label(generalContainer, SWT.NONE);
		lblResetOnNext.setText(Messages.CyberTrackerPropertiesDialog_24);
		lblResetOnNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_25);

		btnResetOnNext = new Button(generalContainer, SWT.CHECK);
		btnResetOnNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_25);
		btnResetOnNext.setSelection(ctProperties.isResetOnNext());
		btnResetOnNext.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblExitPin = new Label(generalContainer, SWT.NONE);
		lblExitPin.setText(Messages.CyberTrackerPropertiesDialog_ExitPin);
		lblExitPin.setToolTipText(Messages.CyberTrackerPropertiesDialog_ExitPin_Tooltip);

		txtExitPin = new Text(generalContainer, SWT.BORDER);
		txtExitPin.setToolTipText(Messages.CyberTrackerPropertiesDialog_ExitPin_Tooltip);
		txtExitPin.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtExitPin.setText(String.valueOf(ctProperties.getExitPin()));
		txtExitPin.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isExitPinValid()) {
					exitPinDecoration.hide();
				} else {
					exitPinDecoration.show();
				}
				setChangesMade(true);
			}
		});

		exitPinDecoration = new ControlDecoration(txtExitPin, SWT.LEFT);
		exitPinDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		exitPinDecoration.setShowHover(true);
		exitPinDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_ExitPinInvalid, CyberTrackerProperties.EXIT_PIN_MIN_VALUE, CyberTrackerProperties.EXIT_PIN_MAX_VALUE));
		exitPinDecoration.hide();
		
		

		Label lblStorageTime = new Label(generalContainer, SWT.NONE);
		lblStorageTime.setText(Messages.CyberTrackerPropertiesDialog_StorageTime);
		lblStorageTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_StorageTime_Tooltip);

		txtStorageTime = new Text(generalContainer, SWT.BORDER);
		txtStorageTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_StorageTime_Tooltip);
		txtStorageTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtStorageTime.setText(String.valueOf(ctProperties.getStorageTime()));
		txtStorageTime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isStorageTimeValid()) {
					storageTimeDecoration.hide();
				} else {
					storageTimeDecoration.show();
				}
				setChangesMade(true);
			}

		});

		storageTimeDecoration = new ControlDecoration(txtStorageTime, SWT.LEFT);
		storageTimeDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		storageTimeDecoration.setShowHover(true);
		storageTimeDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_StorageTimeInvalid, CyberTrackerProperties.STORAGE_TIME_MIN_VALUE, CyberTrackerProperties.STORAGE_TIME_MAX_VALUE));
		storageTimeDecoration.hide();
		
		
		
		
		Label lblSigtingAccuracy = new Label(gpsContainer, SWT.NONE);
		lblSigtingAccuracy.setText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy);
		lblSigtingAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy_Tooltip);

		txtSightingAccuracy = new Text(gpsContainer, SWT.BORDER);
		txtSightingAccuracy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtSightingAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy_Tooltip);
		txtSightingAccuracy.setText(String.valueOf(ctProperties.getSightingAccuracy()));
		txtSightingAccuracy.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isSigtingAccuracyValid()) {
					sightingAccuracyDecoration.hide();
				} else {
					sightingAccuracyDecoration.show();
				}
				setChangesMade(true);
			}
		});

		sightingAccuracyDecoration = new ControlDecoration(txtSightingAccuracy, SWT.LEFT);
		sightingAccuracyDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		sightingAccuracyDecoration.setShowHover(true);
		sightingAccuracyDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SightingAccuracyIvalid, CyberTrackerProperties.SIGHTING_ACCURACY_MIN_VALUE, CyberTrackerProperties.SIGHTING_ACCURACY_MAX_VALUE));
		sightingAccuracyDecoration.hide();
		

		Label lblSightingFixCount = new Label(gpsContainer, SWT.NONE);
		lblSightingFixCount.setText(Messages.CyberTrackerPropertiesDialog_SightingFixCount);
		lblSightingFixCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingFixCount_Tooltip);

		txtSightingFixCount = new Text(gpsContainer, SWT.BORDER);
		txtSightingFixCount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtSightingFixCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingFixCount_Tooltip);
		txtSightingFixCount.setText(String.valueOf(ctProperties.getSightingFixCount()));
		txtSightingFixCount.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isSigtingFixCountValid()) {
					sightingFixCountDecoration.hide();
				} else {
					sightingFixCountDecoration.show();
				}
				setChangesMade(true);
			}
		});
		
		Label lblTrackAccuracy = new Label(gpsContainer, SWT.NONE);
		lblTrackAccuracy.setText(Messages.CyberTrackerPropertiesDialog_27);
		lblTrackAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_28);

		txtTrackAccuracy = new Text(gpsContainer, SWT.BORDER);
		txtTrackAccuracy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtTrackAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_28);
		txtTrackAccuracy.setText(String.valueOf(ctProperties.getTrackAccuracy()));
		txtTrackAccuracy.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTrackAccuracyValid()) {
					TrackAccuracyDecoration.hide();
				} else {
					TrackAccuracyDecoration.show();
				}
				setChangesMade(true);
			}
		});

		TrackAccuracyDecoration = new ControlDecoration(txtTrackAccuracy, SWT.LEFT);
		TrackAccuracyDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		TrackAccuracyDecoration.setShowHover(true);
		TrackAccuracyDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_30, CyberTrackerProperties.TRACK_ACCURACY_MIN_VALUE, CyberTrackerProperties.TRACK_ACCURACY_MAX_VALUE));
		TrackAccuracyDecoration.hide();
		

		sightingFixCountDecoration = new ControlDecoration(txtSightingFixCount, SWT.LEFT);
		sightingFixCountDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		sightingFixCountDecoration.setShowHover(true);
		sightingFixCountDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SightingFixCountInvalid, CyberTrackerProperties.SIGHTING_FIX_COUNT_MIN_VALUE, CyberTrackerProperties.SIGHTING_FIX_COUNT_MAX_VALUE));
		sightingFixCountDecoration.hide();
		
		Label lblTrackTimer = new Label(gpsContainer, SWT.NONE);
		lblTrackTimer.setText(Messages.CyberTrackerPropertiesDialog_TrackTimer);
		lblTrackTimer.setToolTipText(Messages.CyberTrackerPropertiesDialog_TrackTimer_Tooltip);

		txtTrackTimer = new Text(gpsContainer, SWT.BORDER);
		txtTrackTimer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtTrackTimer.setToolTipText(Messages.CyberTrackerPropertiesDialog_TrackTimer_Tooltip);
		txtTrackTimer.setText(String.valueOf(ctProperties.getWaypointTimer()));
		txtTrackTimer.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTrackTimerValid()) {
					trackTimerDecoration.hide();
				} else {
					trackTimerDecoration.show();
				}
				setChangesMade(true);
			}
		});

		trackTimerDecoration = new ControlDecoration(txtTrackTimer, SWT.LEFT);
		trackTimerDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		trackTimerDecoration.setShowHover(true);
		trackTimerDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_TrackTimerInvalid, CyberTrackerProperties.TIME_TRACK_MIN_VALUE, CyberTrackerProperties.TIME_TRACK_MAX_VALUE));
		trackTimerDecoration.hide();

		
		
		Label lblUseGpsTime= new Label(gpsContainer, SWT.NONE);
		lblUseGpsTime.setText(Messages.CyberTrackerPropertiesDialog_31);
		lblUseGpsTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_32);

		btnUseGpsTime = new Button(gpsContainer, SWT.CHECK);
		btnUseGpsTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_32);
		btnUseGpsTime.setSelection(ctProperties.isUseGpsTime());
		btnUseGpsTime.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		
		
		
		Label lblTimeOffset = new Label(gpsContainer, SWT.NONE);
		lblTimeOffset.setText(Messages.CyberTrackerPropertiesDialog_TimeOffset);
		lblTimeOffset.setToolTipText(Messages.CyberTrackerPropertiesDialog_TimeOffset_Tooltip);

		timeOffset = new ComboViewer(gpsContainer, SWT.READ_ONLY);
		timeOffset.getControl().setToolTipText(Messages.CyberTrackerPropertiesDialog_TimeOffset_Tooltip);
		timeOffset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeOffset.setContentProvider(ArrayContentProvider.getInstance());
		timeOffset.setLabelProvider(new CyberTrackerGTMLabelProvider());
 		timeOffset.setInput(CyberTrackerProperties.GTM_VALUES);
		timeOffset.setSelection(new StructuredSelection(ctProperties.getGpsTimeZone()));
		timeOffset.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setChangesMade(true);
			}
		});


		Label lblSkipButtonTimeout = new Label(gpsContainer, SWT.NONE);
		lblSkipButtonTimeout.setText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout);
		lblSkipButtonTimeout.setToolTipText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout_Tooltip);

		txtSkipButtonTimeout = new Text(gpsContainer, SWT.BORDER);
		txtSkipButtonTimeout.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtSkipButtonTimeout.setToolTipText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout_Tooltip);
		txtSkipButtonTimeout.setText(String.valueOf(ctProperties.getSkipButtonTimeout()));
		txtSkipButtonTimeout.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isSkipButtonTimeoutValid()) {
					skipButtonTimeoutDecoration.hide();
				} else {
					skipButtonTimeoutDecoration.show();
				}
				setChangesMade(true);
			}
		});

		skipButtonTimeoutDecoration = new ControlDecoration(txtSkipButtonTimeout, SWT.LEFT);
		skipButtonTimeoutDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		skipButtonTimeoutDecoration.setShowHover(true);
		skipButtonTimeoutDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeoutInvalid, CyberTrackerProperties.SKIP_BUTTON_TIMEOUT_MIN_VALUE, CyberTrackerProperties.SKIP_BUTTON_TIMEOUT_MAX_VALUE));
		skipButtonTimeoutDecoration.hide();
		
		
		Label lblManualGPS = new Label(gpsContainer, SWT.NONE);
		lblManualGPS.setText(Messages.CyberTrackerPropertiesDialog_34);
		lblManualGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_35);

		btnManualGPS = new Button(gpsContainer, SWT.CHECK);
		btnManualGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_35);
		btnManualGPS.setSelection(ctProperties.isManualGps());
		btnManualGPS.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		
		Label lblAllowSkipManual= new Label(gpsContainer, SWT.NONE);
		lblAllowSkipManual.setText(Messages.CyberTrackerPropertiesDialog_37);
		lblAllowSkipManual.setToolTipText(Messages.CyberTrackerPropertiesDialog_38);

		btnAllowSkipManual = new Button(gpsContainer, SWT.CHECK);
		btnAllowSkipManual.setToolTipText(Messages.CyberTrackerPropertiesDialog_38);
		btnAllowSkipManual.setSelection(ctProperties.isAllowSkipManualGps());
		btnAllowSkipManual.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		
		Label lblMapFilename = new Label(fieldmapContainer, SWT.NONE);
		lblMapFilename.setText(Messages.CyberTrackerPropertiesDialog_40);
		lblMapFilename.setToolTipText(Messages.CyberTrackerPropertiesDialog_41);
		
	    Composite fileContainer = new Composite(fieldmapContainer, SWT.NONE);
	    fileContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
	    fileContainer.setLayout(new GridLayout(2, false));
	    		    
	    txtFileName = new Text(fileContainer, SWT.BORDER);
	    txtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    txtFileName.setToolTipText(Messages.CyberTrackerPropertiesDialog_41);
		txtFileName.setText(ctProperties.getFieldMapFilename());
		
		txtFileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isFileNameDecorationValid()) {
					FileNameDecoration.hide();
				} else {
					FileNameDecoration.show();
				}
				setChangesMade(true);
			}
		});
		
	    
	    FileNameDecoration = new ControlDecoration(txtFileName, SWT.LEFT);
	    FileNameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
	    FileNameDecoration.setShowHover(true);
	    FileNameDecoration.setDescriptionText(Messages.CyberTrackerPropertiesDialog_5);
	    FileNameDecoration.hide();
	    
	    
	    
	    Button open = new Button(fileContainer, SWT.PUSH);
		open.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)open.getLayoutData()).heightHint = 10;
	    open.setText(Messages.CyberTrackerPropertiesDialog_42);
	    open.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {Messages.CyberTrackerPropertiesDialog_43});
	    		dlg.setFilterExtensions(new String[] {Messages.CyberTrackerPropertiesDialog_44});
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			txtFileName.setText(fn);
	    		}
	    		setChangesMade(true);
	    	}
	    });

		
		Label lblLock100= new Label(fieldmapContainer, SWT.NONE);
		lblLock100.setText(Messages.CyberTrackerPropertiesDialog_45);
		lblLock100.setToolTipText(Messages.CyberTrackerPropertiesDialog_46);

		btnLock100 = new Button(fieldmapContainer, SWT.CHECK);
		btnLock100.setToolTipText(Messages.CyberTrackerPropertiesDialog_46);
		btnLock100.setSelection(ctProperties.isLock100());
		btnLock100.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});	
		
		
		Label lblUseMapOnSkip= new Label(fieldmapContainer, SWT.NONE);
		lblUseMapOnSkip.setText(Messages.CyberTrackerPropertiesDialog_48);
		lblUseMapOnSkip.setToolTipText(Messages.CyberTrackerPropertiesDialog_49);

		btnUseMapOnSkip = new Button(fieldmapContainer, SWT.CHECK);
		btnUseMapOnSkip.setToolTipText(Messages.CyberTrackerPropertiesDialog_49);
		btnUseMapOnSkip.setSelection(ctProperties.isUseMapOnSkip());
		btnUseMapOnSkip.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});				
		setTitle(Messages.CyberTrackerPropertiesDialog_Title);
		setMessage(Messages.CyberTrackerPropertiesDialog_Message);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		return tabFolder;
	}

	private boolean isExitPinValid() {
		if (txtExitPin == null || txtExitPin.getText() == null || txtExitPin.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtExitPin.getText());
			return result >= CyberTrackerProperties.EXIT_PIN_MIN_VALUE && result <= CyberTrackerProperties.EXIT_PIN_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isTrackAccuracyValid() {
		if (txtTrackAccuracy == null || txtTrackAccuracy.getText() == null || txtTrackAccuracy.getText().isEmpty())
			return false;
		try {
			Double result = Double.valueOf(txtTrackAccuracy.getText());
			return result >= CyberTrackerProperties.TRACK_ACCURACY_MIN_VALUE && result <= CyberTrackerProperties.TRACK_ACCURACY_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean isSigtingAccuracyValid() {
		if (txtSightingAccuracy == null || txtSightingAccuracy.getText() == null || txtSightingAccuracy.getText().isEmpty())
			return false;
		try {
			Double result = Double.valueOf(txtSightingAccuracy.getText());
			return result >= CyberTrackerProperties.SIGHTING_ACCURACY_MIN_VALUE && result <= CyberTrackerProperties.SIGHTING_ACCURACY_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isSigtingFixCountValid() {
		if (txtSightingFixCount == null || txtSightingFixCount.getText() == null || txtSightingFixCount.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtSightingFixCount.getText());
			return result >= CyberTrackerProperties.SIGHTING_FIX_COUNT_MIN_VALUE && result <= CyberTrackerProperties.SIGHTING_FIX_COUNT_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isSkipButtonTimeoutValid() {
		if (txtSkipButtonTimeout == null || txtSkipButtonTimeout.getText() == null || txtSkipButtonTimeout.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtSkipButtonTimeout.getText());
			return result >= CyberTrackerProperties.SKIP_BUTTON_TIMEOUT_MIN_VALUE && result <= CyberTrackerProperties.SKIP_BUTTON_TIMEOUT_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean isTrackTimerValid() {
		if (txtTrackTimer == null || txtTrackTimer.getText() == null || txtTrackTimer.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtTrackTimer.getText());
			return result >= CyberTrackerProperties.TIME_TRACK_MIN_VALUE && result <= CyberTrackerProperties.TIME_TRACK_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isStorageTimeValid() {
		if (txtStorageTime == null || txtStorageTime.getText() == null || txtStorageTime.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtStorageTime.getText());
			return result >= CyberTrackerProperties.STORAGE_TIME_MIN_VALUE && result <= CyberTrackerProperties.STORAGE_TIME_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean isFileNameDecorationValid() {
		return new File(txtFileName.getText()).isFile();
	}

	private boolean validate() {
		return isStorageTimeValid() && isExitPinValid() &&
				isSigtingAccuracyValid() && isSigtingFixCountValid() && 
				isTrackTimerValid() && isSkipButtonTimeoutValid() && isTrackAccuracyValid();
	}
	
	

	@Override
	protected boolean performSave() {
		if (!validate()) {
			MessageDialog.openError(getShell(), Messages.CyberTrackerPropertiesDialog_Error, Messages.CyberTrackerPropertiesDialog_DataNotValid);
			return false;
		}
		ctProperties.setAutoNext(btnAutoNext.getSelection());
		
		ctProperties.setLargeScrollBars(btnLargeScrollBars.getSelection());
		ctProperties.setKioskMode(btnKioskMode.getSelection());
		ctProperties.setSimpleCamera(btnSimpleCamera.getSelection());
		ctProperties.setCanPause(btnCanPause.getSelection());
		ctProperties.setExitPin(Integer.valueOf(txtExitPin.getText()));
		
			
		ctProperties.setSightingAccuracy(Double.valueOf(txtSightingAccuracy.getText()));
		ctProperties.setSightingFixCount(Integer.valueOf(txtSightingFixCount.getText()));
		ctProperties.setWaypointTimer(Integer.valueOf(txtTrackTimer.getText()));
		StructuredSelection selection = (StructuredSelection) timeOffset.getSelection();
		ctProperties.setGpsTimeZone((Integer)selection.getFirstElement());
		ctProperties.setSkipButtonTimeout(Integer.valueOf(txtSkipButtonTimeout.getText()));
		
		ctProperties.setShowEdit(btnShowEdit.getSelection());
		ctProperties.setShowGPS(btnShowGPS.getSelection());
		ctProperties.setStorageTime(Integer.valueOf(txtStorageTime.getText()));
		
		ctProperties.setUseTitleBar(btnUseTitleBar.getSelection());
		ctProperties.setUseLargeTitles(btnLargeTitles.getSelection());
		ctProperties.setUseLargeTabs(btnLargeTabs.getSelection());
		ctProperties.setDisableEditing(btnDisableEditing.getSelection());
		ctProperties.setUseSdCard(btnSdCard.getSelection());
		ctProperties.setTestTime(btnTestTime.getSelection());
		ctProperties.setResetOnSync(btnResetOnSync.getSelection());
		ctProperties.setResetOnNext(btnResetOnNext.getSelection());
		ctProperties.setTrackAccuracy(Double.valueOf(txtTrackAccuracy.getText()) );
		ctProperties.setUseGpsTime(btnUseGpsTime.getSelection());
		ctProperties.setManualGps(btnManualGPS.getSelection());
		ctProperties.setAllowSkipManualGps(btnAllowSkipManual.getSelection());
		ctProperties.setFieldMapFilename(txtFileName.getText());
		ctProperties.setLock100(btnLock100.getSelection());
		ctProperties.setUseMapOnSkip(btnUseMapOnSkip.getSelection());
		
		Session session = HibernateManager.openSession();
		try {
			CyberTrackerHibernateManager.saveProperties(ctProperties, session);
			setChangesMade(false);
			return true;
		} catch (Exception e) {
			SmartPlugIn.displayLog(getShell(), Messages.CyberTrackerPropertiesDialog_Save_Error, e);
			return false;
		}finally {
			session.close();
		}
	}

	private class CyberTrackerGTMLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Integer) {
				int x = (Integer) element;
				int val = Math.abs(x);
				boolean positive = x >= 0;
				String s = "GTM"; //$NON-NLS-1$
				int hour = val/100;
				int min = val%100;
				if (val != 0) {
					s += positive ? " + " : " - "; //$NON-NLS-1$ //$NON-NLS-2$
					if (hour < 10)
						s += "0"; //$NON-NLS-1$
					s += String.valueOf(hour);
					switch (min) {
					case 0:
						s += ":00"; //$NON-NLS-1$
						break;
					case 50:
						s += ":30"; //$NON-NLS-1$
						break;
					case 75:
						s += ":45"; //$NON-NLS-1$
						break;
					default:
						break;
					}
				}
				return s;
			}
			return super.getText(element);
		}
	}
}
