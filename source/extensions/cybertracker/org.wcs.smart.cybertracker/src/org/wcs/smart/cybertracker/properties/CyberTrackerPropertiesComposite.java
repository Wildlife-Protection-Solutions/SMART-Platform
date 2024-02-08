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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;
import org.wcs.smart.cybertracker.model.ProjectionFormat;

/**
 * Composite that contains controls to edit CyberTracker properties.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CyberTrackerPropertiesComposite extends Composite {
	
	private static final String CLEARKEY = "CLEAR"; //$NON-NLS-1$
	private static final String COLOR_OP_KEY = "COLOROP"; //$NON-NLS-1$

	private enum PhotoSize{
		SIZE1(640,480),
		SIZE2(1280,960),
		SIZE3(1600,1200),
		SIZE4(2048,1536),
		SIZE5(2560,1920),
		SIZE6(2816,2112),
		SIZE7(3264,2468),
		SIZE8(4200,2800),
		CUSTOM(null, null);
		
		Integer w;
		Integer h;
		
		PhotoSize(Integer w, Integer h){
			this.w = w;
			this.h = h;
		}
		
	}
	
	private List<IPropsChangeListener> listeners = new ArrayList<IPropsChangeListener>();
	private boolean isPopulating = false;

	private Button btnOpen;
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
	private Button btnUseIncidentGroup;
	private Button btnSimpleCamera;
	private Button btnCanPause;
	private Text txtExitPin;
	private ComboViewer cmbDataFormat; 
	
	private Text txtSightingAccuracy;
	private Text txtSightingFixCount;
	private Text txtTrackAccuracy;
	
	private ComboViewer cmbTrackTimer;
	private Text txtTrackTimer;
	
	private Button btnUseGpsTime;
    private ComboViewer timeOffset;
    private Text txtSkipButtonTimeout;
    private Text txtMaxPhotoCount;
    
    private Button btnManualGPS;
    private Button btnAllowSkipManual;
    
    private Text txtDilutionOfPrecision;
    
    private Text txtFileName;
    private Button btnLock100;
    private Button btnUseMapOnSkip;
    
    private ColorSelector btnTrackColor;
    private List<ColorSelector> btnThemeColors;
    
    private ComboViewer cbProjection;
    private Text txtUtmZome;
	
    private ControlDecoration exitPinDecoration;
    
    private ControlDecoration sightingAccuracyDecoration;
    private ControlDecoration trackAccuracyDecoration;
    private ControlDecoration sightingFixCountDecoration;
    private ControlDecoration trackTimerDecoration;
    private ControlDecoration skipButtonTimeoutDecoration;
    
    private ControlDecoration dilutionOfPrecisionDecoration;
    
    private ControlDecoration FileNameDecoration;

    private ControlDecoration utmZoneDecoration;
    
    private ControlDecoration maxPhotoCountDecoration;
	private CTabFolder tabFolder;
	
	private ComboViewer cmbSizes;
	private ComboViewer cmbUnits;
	private Text txtWidth, txtHeight;
	private ControlDecoration cdImageWidth, cdImageHeight;
	private Button btnOpResize;
	
	private List<Control> controls;
	
	public CyberTrackerPropertiesComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void createContent(Composite parent) {
		controls = new ArrayList<>();
		
		tabFolder = new CTabFolder (parent, SWT.NONE);
		Rectangle clientArea = parent.getClientArea ();
		tabFolder.setLocation (clientArea.x, clientArea.y);
		
		tabFolder.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		
		
		CTabItem generalTab = new CTabItem (tabFolder, SWT.NONE);
		generalTab.setText (Messages.CyberTrackerPropertiesDialog_0);
		
		CTabItem gpsTab = new CTabItem (tabFolder, SWT.NONE);
		gpsTab.setText (Messages.CyberTrackerPropertiesDialog_1);
		
//		CTabItem fieldmapTab = new CTabItem (tabFolder, SWT.NONE);
//		fieldmapTab.setText (Messages.CyberTrackerPropertiesDialog_2);
//		
		CTabItem themeTab = new CTabItem (tabFolder, SWT.NONE);
		themeTab.setText (Messages.CyberTrackerPropertiesComposite_ThemeTabName);
		
		CTabItem cameraTab = new CTabItem (tabFolder, SWT.NONE);
		cameraTab.setText (Messages.CyberTrackerPropertiesComposite_PhotoTab);
		
		CTabItem classicTab = new CTabItem (tabFolder, SWT.NONE);
		classicTab.setText (Messages.CyberTrackerPropertiesComposite_ClassicSettings);
		
		ScrolledComposite generalScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		generalScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		generalScroll.setShowFocusedControl(true);
		generalScroll.setExpandHorizontal(true);
		generalScroll.setExpandVertical(true);
		
		Composite generalContainer = new Composite(generalScroll, SWT.None);
		generalContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		generalContainer.setLayout(new GridLayout(2, false));
		((GridLayout)generalContainer.getLayout()).verticalSpacing = 10;
		generalScroll.setContent(generalContainer);
		
		
		ScrolledComposite gpsScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		gpsScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		gpsScroll.setShowFocusedControl(true);
		gpsScroll.setExpandHorizontal(true);
		gpsScroll.setExpandVertical(true);
		
		Composite gpsContainer = new Composite(gpsScroll, SWT.None);
		gpsContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		gpsContainer.setLayout(new GridLayout(2, false));
		((GridLayout)gpsContainer.getLayout()).verticalSpacing = 10;
		gpsScroll.setContent(gpsContainer);
		
		ScrolledComposite themeScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		themeScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		themeScroll.setShowFocusedControl(true);
		themeScroll.setExpandHorizontal(true);
		themeScroll.setExpandVertical(true);
		
		Composite themeContainer = new Composite(themeScroll, SWT.None);
		themeContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		themeContainer.setLayout(new GridLayout());
		((GridLayout)themeContainer.getLayout()).verticalSpacing = 10;
		themeScroll.setContent(themeContainer);
		
		ScrolledComposite cameraScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		cameraScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cameraScroll.setShowFocusedControl(true);
		cameraScroll.setExpandHorizontal(true);
		cameraScroll.setExpandVertical(true);
		
		Composite cameraContainer = new Composite(cameraScroll, SWT.None);
		cameraContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		cameraContainer.setLayout(new GridLayout(2, false));
		((GridLayout)cameraContainer.getLayout()).verticalSpacing = 10;
		cameraScroll.setContent(cameraContainer);
		
		ScrolledComposite classicScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		classicScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		classicScroll.setShowFocusedControl(true);
		classicScroll.setExpandHorizontal(true);
		classicScroll.setExpandVertical(true);
		
		Composite classicContainer = new Composite(classicScroll, SWT.None);
		classicContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		classicContainer.setLayout(new GridLayout(2, false));
		((GridLayout)classicContainer.getLayout()).verticalSpacing = 10;
		classicScroll.setContent(classicContainer);
		
		generalTab.setControl(generalScroll);
		gpsTab.setControl(gpsScroll);
//		fieldmapTab.setControl(mapScroll);
		themeTab.setControl(themeScroll);
		cameraTab.setControl(cameraScroll);
		classicTab.setControl(classicScroll);
		
		createThemeTab(themeContainer);
		createCameraTab(cameraContainer);
		
		Composite llGeneral = SmartUiUtils.createSubHeaderLabel(generalContainer, Messages.CyberTrackerPropertiesComposite_GeneralHeader);
		llGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblIncidentGroup = new Label(generalContainer, SWT.NONE);
		lblIncidentGroup.setText(Messages.CyberTrackerPropertiesComposite_IncidentGroupOp);
		lblIncidentGroup.setToolTipText(Messages.CyberTrackerPropertiesComposite_IncidentGroupTooltip);

		btnUseIncidentGroup = new Button(generalContainer, SWT.CHECK);
		btnUseIncidentGroup.setToolTipText(lblIncidentGroup.getToolTipText());
		btnUseIncidentGroup.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnUseIncidentGroup);
		
		Label lblKioskMode = new Label(generalContainer, SWT.NONE);
		lblKioskMode.setText(Messages.CyberTrackerPropertiesDialog_KioskMode);
		lblKioskMode.setToolTipText(Messages.CyberTrackerPropertiesDialog_KioskMode_Tooltip);

		btnKioskMode = new Button(generalContainer, SWT.CHECK);
		btnKioskMode.setToolTipText(Messages.CyberTrackerPropertiesDialog_KioskMode_Tooltip);
		btnKioskMode.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnKioskMode);
		
		Label lblExitPin = new Label(generalContainer, SWT.NONE);
		lblExitPin.setText(Messages.CyberTrackerPropertiesDialog_ExitPin);
		lblExitPin.setToolTipText(Messages.CyberTrackerPropertiesDialog_ExitPin_Tooltip);

		txtExitPin = new Text(generalContainer, SWT.BORDER);
		txtExitPin.setToolTipText(Messages.CyberTrackerPropertiesDialog_ExitPin_Tooltip);
		txtExitPin.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtExitPin.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isExitPinValid()) {
					exitPinDecoration.hide();
				} else {
					exitPinDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtExitPin);
		
		exitPinDecoration = new ControlDecoration(txtExitPin, SWT.LEFT);
		exitPinDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		exitPinDecoration.setShowHover(true);
		exitPinDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_ExitPinInvalid, CyberTrackerPropertiesProfile.EXIT_PIN_MIN_VALUE, CyberTrackerPropertiesProfile.EXIT_PIN_MAX_VALUE));
		exitPinDecoration.hide();
		
		Label lblCanPause = new Label(generalContainer, SWT.NONE);
		lblCanPause.setText(Messages.CyberTrackerPropertiesDialog_CanPause);
		lblCanPause.setToolTipText(Messages.CyberTrackerPropertiesDialog_CanPause_Tooltip);

		btnCanPause = new Button(generalContainer, SWT.CHECK);
		btnCanPause.setToolTipText(Messages.CyberTrackerPropertiesDialog_CanPause_Tooltip);
		btnCanPause.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnCanPause);
		
		Label lblEditing= new Label(generalContainer, SWT.NONE);
		lblEditing.setText(Messages.CyberTrackerPropertiesDialog_12);
		lblEditing.setToolTipText(Messages.CyberTrackerPropertiesDialog_13);

		btnDisableEditing = new Button(generalContainer, SWT.CHECK);
		btnDisableEditing.setToolTipText(Messages.CyberTrackerPropertiesDialog_13);
		btnDisableEditing.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnDisableEditing);

		Label lblTestTime= new Label(generalContainer, SWT.NONE);
		lblTestTime.setText(Messages.CyberTrackerPropertiesDialog_18);
		lblTestTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_19);

		btnTestTime = new Button(generalContainer, SWT.CHECK);
		btnTestTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_19);
		btnTestTime.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnTestTime);
		
		
		Label lblUnits= new Label(generalContainer, SWT.NONE);
		lblUnits.setText(Messages.CyberTrackerPropertiesComposite_UnitOption);
		lblUnits.setToolTipText(Messages.CyberTrackerPropertiesComposite_UnitOptionTooltip);

		cmbUnits = new ComboViewer(generalContainer, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.SINGLE);
		cmbUnits.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				switch ((CyberTrackerPropertiesProfileOption.Unit)element) {
				case IMPERIAL: return Messages.CyberTrackerPropertiesComposite_ImperialUnits;
				case METRIC: return Messages.CyberTrackerPropertiesComposite_MeticUnits;
				}
				return super.getText(element);
			}
		});
		cmbUnits.setContentProvider(ArrayContentProvider.getInstance());
		cmbUnits.setInput(CyberTrackerPropertiesProfileOption.Unit.values());
		cmbUnits.addSelectionChangedListener(e->changesMade());
		controls.add(cmbUnits.getControl());

		llGeneral = SmartUiUtils.createSubHeaderLabel(gpsContainer, Messages.CyberTrackerPropertiesComposite_GPSSettingsHeader);
		llGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Label lblSightingFixCount = new Label(gpsContainer, SWT.NONE);
		lblSightingFixCount.setText(Messages.CyberTrackerPropertiesDialog_SightingFixCount);
		lblSightingFixCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingFixCount_Tooltip);

		txtSightingFixCount = new Text(gpsContainer, SWT.BORDER);
		txtSightingFixCount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtSightingFixCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingFixCount_Tooltip);
		txtSightingFixCount.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isSigtingFixCountValid()) {
					sightingFixCountDecoration.hide();
				} else {
					sightingFixCountDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtSightingFixCount);
		sightingFixCountDecoration = new ControlDecoration(txtSightingFixCount, SWT.LEFT);
		sightingFixCountDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		sightingFixCountDecoration.setShowHover(true);
		sightingFixCountDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SightingFixCountInvalid, CyberTrackerPropertiesProfile.SIGHTING_FIX_COUNT_MIN_VALUE, CyberTrackerPropertiesProfile.SIGHTING_FIX_COUNT_MAX_VALUE));
		sightingFixCountDecoration.hide();
				
		Label lblTrackTimer = new Label(gpsContainer, SWT.NONE);
		lblTrackTimer.setText(Messages.CyberTrackerPropertiesDialog_TrackTimer1);
		lblTrackTimer.setToolTipText(Messages.CyberTrackerPropertiesDialog_TrackTimer_Tooltip);

		Composite trackTimer = new Composite(gpsContainer, SWT.NONE);
		trackTimer.setLayout(new GridLayout(2, false));
		trackTimer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)trackTimer.getLayout()).marginWidth = 0;
		((GridLayout)trackTimer.getLayout()).marginHeight = 0;
		
		cmbTrackTimer = new ComboViewer(trackTimer, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbTrackTimer.setLabelProvider(new TrackTimerOptionLabelProvider());
		cmbTrackTimer.setContentProvider(ArrayContentProvider.getInstance());
		cmbTrackTimer.setInput(CyberTrackerPropertiesProfileOption.TrackTimerOp.values());
		cmbTrackTimer.setSelection(new StructuredSelection(CyberTrackerPropertiesProfileOption.TrackTimerOp.TIME));
		cmbTrackTimer.addSelectionChangedListener(e->{
			if (isTrackTimerValid()) {
				trackTimerDecoration.hide();
			} else {
				trackTimerDecoration.show();
			}
			changesMade();
		});
		controls.add(cmbTrackTimer.getControl());
		
		txtTrackTimer = new Text(trackTimer, SWT.BORDER);
		txtTrackTimer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtTrackTimer.setToolTipText(Messages.CyberTrackerPropertiesDialog_TrackTimer_Tooltip);
		txtTrackTimer.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTrackTimerValid()) {
					trackTimerDecoration.hide();
				} else {
					trackTimerDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtTrackTimer);
		
		trackTimerDecoration = new ControlDecoration(txtTrackTimer, SWT.LEFT);
		trackTimerDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		trackTimerDecoration.setShowHover(true);
		trackTimerDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_TrackTimerInvalid, CyberTrackerPropertiesProfile.TIME_TRACK_MIN_VALUE, CyberTrackerPropertiesProfile.TIME_TRACK_MAX_VALUE));
		trackTimerDecoration.hide();

		
		
		Label lblUseGpsTime= new Label(gpsContainer, SWT.NONE);
		lblUseGpsTime.setText(Messages.CyberTrackerPropertiesDialog_31);
		lblUseGpsTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_32);

		btnUseGpsTime = new Button(gpsContainer, SWT.CHECK);
		btnUseGpsTime.setToolTipText(Messages.CyberTrackerPropertiesDialog_32);
		btnUseGpsTime.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnUseGpsTime);
		
		
		
		
		
		
		
		Label lblProjection = new Label(gpsContainer, SWT.NONE);
		lblProjection.setText(Messages.CyberTrackerPropertiesDialog_Projection);
		lblProjection.setToolTipText(Messages.CyberTrackerPropertiesDialog_Projection_Tooltip);

		cbProjection = new ComboViewer(gpsContainer, SWT.READ_ONLY);
		cbProjection.getControl().setToolTipText(Messages.CyberTrackerPropertiesDialog_Projection_Tooltip);
		cbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cbProjection.setLabelProvider(new CyberTrackerProjectionProvider());
 		cbProjection.setInput(ProjectionFormat.getIds());
		cbProjection.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				changesMade();
			}
		});
		controls.add(cbProjection.getControl());
		
		
		


		Label lblSkipButtonTimeout = new Label(gpsContainer, SWT.NONE);
		lblSkipButtonTimeout.setText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout);
		lblSkipButtonTimeout.setToolTipText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout_Tooltip);

		txtSkipButtonTimeout = new Text(gpsContainer, SWT.BORDER);
		controls.add(txtSkipButtonTimeout);
		txtSkipButtonTimeout.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtSkipButtonTimeout.setToolTipText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout_Tooltip);
		txtSkipButtonTimeout.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isSkipButtonTimeoutValid()) {
					skipButtonTimeoutDecoration.hide();
				} else {
					skipButtonTimeoutDecoration.show();
				}
				changesMade();
			}
		});

		skipButtonTimeoutDecoration = new ControlDecoration(txtSkipButtonTimeout, SWT.LEFT);
		skipButtonTimeoutDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		skipButtonTimeoutDecoration.setShowHover(true);
		skipButtonTimeoutDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeoutInvalid, CyberTrackerPropertiesProfile.SKIP_BUTTON_TIMEOUT_MIN_VALUE, CyberTrackerPropertiesProfile.SKIP_BUTTON_TIMEOUT_MAX_VALUE));
		skipButtonTimeoutDecoration.hide();
		

		Label lblUseMapOnSkip= new Label(gpsContainer, SWT.NONE);
		lblUseMapOnSkip.setText(Messages.CyberTrackerPropertiesDialog_48);
		lblUseMapOnSkip.setToolTipText(Messages.CyberTrackerPropertiesDialog_49);

		btnUseMapOnSkip = new Button(gpsContainer, SWT.CHECK);
		controls.add(btnUseMapOnSkip);
		btnUseMapOnSkip.setToolTipText(Messages.CyberTrackerPropertiesDialog_49);
		btnUseMapOnSkip.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		
		Label lblManualGPS = new Label(gpsContainer, SWT.NONE);
		lblManualGPS.setText(Messages.CyberTrackerPropertiesDialog_34);
		lblManualGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_35);

		btnManualGPS = new Button(gpsContainer, SWT.CHECK);
		controls.add(btnManualGPS);
		btnManualGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_35);
		btnManualGPS.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
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
		controls.add(btnAllowSkipManual);
		btnAllowSkipManual.setToolTipText(Messages.CyberTrackerPropertiesDialog_38);
		btnAllowSkipManual.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});


		createClassicTab(classicContainer);
		
		
		generalScroll.setMinSize(generalContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		classicScroll.setMinSize(classicContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		gpsScroll.setMinSize(gpsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		themeScroll.setMinSize(themeContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		cameraScroll.setMinSize(cameraContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		tabFolder.setSelection(0);
	}

	private void createClassicTab(Composite classicContainer) {
		
		Composite warn = new Composite(classicContainer, SWT.NONE);
		warn.setLayout(new GridLayout(2, false));
		warn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		Label warnimg = new Label(warn, SWT.NONE);
		warnimg.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		
		Label txtwarn = new Label(warn, SWT.WRAP);
		txtwarn.setText(Messages.CyberTrackerPropertiesComposite_ClassicMessage);
		txtwarn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtwarn.getLayoutData()).widthHint = 150;
		
		Composite llGeneral = SmartUiUtils.createSubHeaderLabel(classicContainer, Messages.CyberTrackerPropertiesComposite_ClassicGeneral);
		llGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblUseTitleBar = new Label(classicContainer, SWT.NONE);
		lblUseTitleBar.setText(Messages.CyberTrackerPropertiesDialog_3);
		lblUseTitleBar.setToolTipText(Messages.CyberTrackerPropertiesDialog_4);
	
		btnUseTitleBar = new Button(classicContainer, SWT.CHECK);
		btnUseTitleBar.setToolTipText(Messages.CyberTrackerPropertiesDialog_4);
		btnUseTitleBar.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnUseTitleBar);
		
		Label lblLargeTitles = new Label(classicContainer, SWT.NONE);
		lblLargeTitles.setText(Messages.CyberTrackerPropertiesDialog_6);
		lblLargeTitles.setToolTipText(Messages.CyberTrackerPropertiesDialog_7);

		btnLargeTitles = new Button(classicContainer, SWT.CHECK);
		btnLargeTitles.setToolTipText(Messages.CyberTrackerPropertiesDialog_7);
		btnLargeTitles.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnLargeTitles);
		
		Label lblLargeScrollBars = new Label(classicContainer, SWT.NONE);
		lblLargeScrollBars.setText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars);
		lblLargeScrollBars.setToolTipText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars_Tooltip);

		btnLargeScrollBars = new Button(classicContainer, SWT.CHECK);
		btnLargeScrollBars.setToolTipText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars_Tooltip);
		btnLargeScrollBars.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnLargeScrollBars);
		
		Label lblLargeTabs = new Label(classicContainer, SWT.NONE);
		lblLargeTabs.setText(Messages.CyberTrackerPropertiesDialog_9);
		lblLargeTabs.setToolTipText(Messages.CyberTrackerPropertiesDialog_10);

		btnLargeTabs = new Button(classicContainer, SWT.CHECK);
		btnLargeTabs.setToolTipText(Messages.CyberTrackerPropertiesDialog_10);
		btnLargeTabs.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnLargeTabs);


		Label lblAutoNext = new Label(classicContainer, SWT.NONE);
		lblAutoNext.setText(Messages.CyberTrackerPropertiesDialog_AutoNext);
		lblAutoNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_AutoNext_Tooltip);

		btnAutoNext = new Button(classicContainer, SWT.CHECK);
		btnAutoNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_AutoNext_Tooltip);
		btnAutoNext.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnAutoNext);
		
		Label lblShowEdit = new Label(classicContainer, SWT.NONE);
		lblShowEdit.setText(Messages.CyberTrackerPropertiesDialog_ShowEdit1);
		lblShowEdit.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowEdit_Tooltip);

		btnShowEdit = new Button(classicContainer, SWT.CHECK);
		btnShowEdit.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowEdit_Tooltip);
		btnShowEdit.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnShowEdit);
		
		Label lblShowGPS = new Label(classicContainer, SWT.NONE);
		lblShowGPS.setText(Messages.CyberTrackerPropertiesDialog_ShowGPS);
		lblShowGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowGPS_Tooltip);

		btnShowGPS = new Button(classicContainer, SWT.CHECK);
		btnShowGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowGPS_Tooltip);
		btnShowGPS.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnShowGPS);
		
		Label lblSimpleCamera = new Label(classicContainer, SWT.NONE);
		lblSimpleCamera.setText(Messages.CyberTrackerPropertiesDialog_SimpleCamera);
		lblSimpleCamera.setToolTipText(Messages.CyberTrackerPropertiesDialog_SimpleCamera_Tooltip);

		btnSimpleCamera = new Button(classicContainer, SWT.CHECK);
		btnSimpleCamera.setToolTipText(Messages.CyberTrackerPropertiesDialog_SimpleCamera_Tooltip);
		btnSimpleCamera.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnSimpleCamera);
		
		Label lblSdCard= new Label(classicContainer, SWT.NONE);
		lblSdCard.setText(Messages.CyberTrackerPropertiesDialog_15);
		lblSdCard.setToolTipText(Messages.CyberTrackerPropertiesDialog_16);

		btnSdCard = new Button(classicContainer, SWT.CHECK);
		btnSdCard.setToolTipText(Messages.CyberTrackerPropertiesDialog_16);
		btnSdCard.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnSdCard);
		
		Label lblResetOnSync= new Label(classicContainer, SWT.NONE);
		lblResetOnSync.setText(Messages.CyberTrackerPropertiesDialog_21);
		lblResetOnSync.setToolTipText(Messages.CyberTrackerPropertiesDialog_22);

		btnResetOnSync = new Button(classicContainer, SWT.CHECK);
		btnResetOnSync.setToolTipText(Messages.CyberTrackerPropertiesDialog_22);
		btnResetOnSync.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnResetOnSync);
		
		Label lblResetOnNext= new Label(classicContainer, SWT.NONE);
		lblResetOnNext.setText(Messages.CyberTrackerPropertiesDialog_24);
		lblResetOnNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_25);

		btnResetOnNext = new Button(classicContainer, SWT.CHECK);
		btnResetOnNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_25);
		btnResetOnNext.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		controls.add(btnResetOnNext);
		
		Label lblDataFormat = new Label(classicContainer, SWT.NONE);
		lblDataFormat.setText(Messages.CyberTrackerPropertiesComposite_CtDataFormatLbl);
		lblDataFormat.setToolTipText(Messages.CyberTrackerPropertiesComposite_CtDataFormatTp);
		
		cmbDataFormat = new ComboViewer(classicContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbDataFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbDataFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbDataFormat.setInput(CyberTrackerPropertiesOption.Protocol.values());
		cmbDataFormat.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof CyberTrackerPropertiesOption.Protocol) {
					return ((CyberTrackerPropertiesOption.Protocol) element).name();
				}
				return super.getText(element);
			}
		});
		cmbDataFormat.addSelectionChangedListener(e->changesMade());
		controls.add(cmbDataFormat.getControl());
		
		
		Composite llGps = SmartUiUtils.createSubHeaderLabel(classicContainer, Messages.CyberTrackerPropertiesComposite_ClassicGPS);
		llGps.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblSigtingAccuracy = new Label(classicContainer, SWT.NONE);
		lblSigtingAccuracy.setText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy);
		lblSigtingAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy_Tooltip);

		txtSightingAccuracy = new Text(classicContainer, SWT.BORDER);
		txtSightingAccuracy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtSightingAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy_Tooltip);
		txtSightingAccuracy.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isSigtingAccuracyValid()) {
					sightingAccuracyDecoration.hide();
				} else {
					sightingAccuracyDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtSightingAccuracy);
		sightingAccuracyDecoration = new ControlDecoration(txtSightingAccuracy, SWT.LEFT);
		sightingAccuracyDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		sightingAccuracyDecoration.setShowHover(true);
		sightingAccuracyDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SightingAccuracyIvalid, CyberTrackerPropertiesProfile.SIGHTING_ACCURACY_MIN_VALUE, CyberTrackerPropertiesProfile.SIGHTING_ACCURACY_MAX_VALUE));
		sightingAccuracyDecoration.hide();
		
		Label lblTrackAccuracy = new Label(classicContainer, SWT.NONE);
		lblTrackAccuracy.setText(Messages.CyberTrackerPropertiesDialog_27);
		lblTrackAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_28);

		txtTrackAccuracy = new Text(classicContainer, SWT.BORDER);
		txtTrackAccuracy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtTrackAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_28);
		txtTrackAccuracy.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTrackAccuracyValid()) {
					trackAccuracyDecoration.hide();
				} else {
					trackAccuracyDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtTrackAccuracy);
		
		trackAccuracyDecoration = new ControlDecoration(txtTrackAccuracy, SWT.LEFT);
		trackAccuracyDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		trackAccuracyDecoration.setShowHover(true);
		trackAccuracyDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_30, CyberTrackerPropertiesProfile.TRACK_ACCURACY_MIN_VALUE, CyberTrackerPropertiesProfile.TRACK_ACCURACY_MAX_VALUE));
		trackAccuracyDecoration.hide();
		
		Label lblTimeOffset = new Label(classicContainer, SWT.NONE);
		lblTimeOffset.setText(Messages.CyberTrackerPropertiesDialog_TimeOffset);
		lblTimeOffset.setToolTipText(Messages.CyberTrackerPropertiesDialog_TimeOffset_Tooltip);

		timeOffset = new ComboViewer(classicContainer, SWT.READ_ONLY);
		timeOffset.getControl().setToolTipText(Messages.CyberTrackerPropertiesDialog_TimeOffset_Tooltip);
		timeOffset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeOffset.setContentProvider(ArrayContentProvider.getInstance());
		timeOffset.setLabelProvider(new CyberTrackerGMTLabelProvider());
 		timeOffset.setInput(CyberTrackerPropertiesProfile.GMT_VALUES);
		timeOffset.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				changesMade();
			}
		});
		controls.add(timeOffset.getControl());
		
		Label lblUtmZome = new Label(classicContainer, SWT.NONE);
		lblUtmZome.setText(Messages.CyberTrackerPropertiesDialog_UtmZone);
		lblUtmZome.setToolTipText(Messages.CyberTrackerPropertiesDialog_UtmZone_Tooltip);

		txtUtmZome = new Text(classicContainer, SWT.BORDER);
		controls.add(txtUtmZome);
		txtUtmZome.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtUtmZome.setToolTipText(Messages.CyberTrackerPropertiesDialog_UtmZone_Tooltip);
		txtUtmZome.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isUtmZoneValid()) {
					utmZoneDecoration.hide();
				} else {
					utmZoneDecoration.show();
				}
				changesMade();
			}
		});

		utmZoneDecoration = new ControlDecoration(txtUtmZome, SWT.LEFT);
		utmZoneDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		utmZoneDecoration.setShowHover(true);
		utmZoneDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_UtmZoneInvalid, CyberTrackerPropertiesProfile.UTM_ZONE_MIN_VALUE, CyberTrackerPropertiesProfile.UTM_ZONE_MAX_VALUE));
		utmZoneDecoration.hide();

		Label lblDilutionOfPrecision = new Label(classicContainer, SWT.NONE);
		lblDilutionOfPrecision.setText(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision);
		lblDilutionOfPrecision.setToolTipText(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision_Tooltip);

		txtDilutionOfPrecision = new Text(classicContainer, SWT.BORDER);
		txtDilutionOfPrecision.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtDilutionOfPrecision.setToolTipText(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision_Tooltip);
		txtDilutionOfPrecision.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isDilutionOfPrecisionValid()) {
					dilutionOfPrecisionDecoration.hide();
				} else {
					dilutionOfPrecisionDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtDilutionOfPrecision);
		
		dilutionOfPrecisionDecoration = new ControlDecoration(txtDilutionOfPrecision, SWT.LEFT);
		dilutionOfPrecisionDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		dilutionOfPrecisionDecoration.setShowHover(true);
		dilutionOfPrecisionDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision_Invalid, CyberTrackerPropertiesProfile.DILUTION_OF_PRECISION_MIN_VALUE, CyberTrackerPropertiesProfile.DILUTION_OF_PRECISION_MAX_VALUE));
		dilutionOfPrecisionDecoration.hide();

		Composite llMap = SmartUiUtils.createSubHeaderLabel(classicContainer, Messages.CyberTrackerPropertiesComposite_ClassicMap);
		llMap.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblMapFilename = new Label(classicContainer, SWT.NONE);
		lblMapFilename.setText(Messages.CyberTrackerPropertiesDialog_40);
		lblMapFilename.setToolTipText(Messages.CyberTrackerPropertiesDialog_41);
		
	    Composite fileContainer = new Composite(classicContainer, SWT.NONE);
	    fileContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
	    fileContainer.setLayout(new GridLayout(2, false));
	    		    
	    txtFileName = new Text(fileContainer, SWT.BORDER);
	    txtFileName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	    txtFileName.setToolTipText(Messages.CyberTrackerPropertiesDialog_41);
		
		txtFileName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isFileNameDecorationValid()) {
					FileNameDecoration.hide();
				} else {
					FileNameDecoration.show();
				}
				changesMade();
			}
		});
		controls.add(txtFileName);
	    
	    FileNameDecoration = new ControlDecoration(txtFileName, SWT.LEFT);
	    FileNameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
	    FileNameDecoration.setShowHover(true);
	    FileNameDecoration.setDescriptionText(Messages.CyberTrackerPropertiesDialog_5);
	    FileNameDecoration.hide();
	    
	    
	    
	    btnOpen = new Button(fileContainer, SWT.PUSH);
	    btnOpen.setLayoutData(new GridData(SWT.RIGHT,SWT.FILL,false,false));
		((GridData)btnOpen.getLayoutData()).heightHint = 10;
		btnOpen.setText(Messages.CyberTrackerPropertiesDialog_42);
		btnOpen.addSelectionListener(new SelectionAdapter() {
		
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
	    		dlg.setFilterNames(new String[] {Messages.CyberTrackerPropertiesDialog_43});
	    		dlg.setFilterExtensions(new String[] {"*.ecw"}); //$NON-NLS-1$
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			txtFileName.setText(fn);
	    		}
	    		changesMade();
	    	}
	    });
		controls.add(btnOpen);
		
		Label lblLock100= new Label(classicContainer, SWT.NONE);
		lblLock100.setText(Messages.CyberTrackerPropertiesDialog_45);
		lblLock100.setToolTipText(Messages.CyberTrackerPropertiesDialog_46);

		btnLock100 = new Button(classicContainer, SWT.CHECK);
		btnLock100.setToolTipText(Messages.CyberTrackerPropertiesDialog_46);
		btnLock100.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				changesMade();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});	
		controls.add(btnLock100);
	}

	
	private void validateCamera() {
		if (!btnOpResize.getSelection()) {
			cdImageHeight.hide();
			cdImageWidth.hide();
			return;
		}
		PhotoSize ps = (PhotoSize) cmbSizes.getStructuredSelection().getFirstElement();
		if (ps == PhotoSize.CUSTOM) {
			try {
				Integer.parseInt(txtWidth.getText());
				cdImageWidth.hide();
			}catch (Exception ex) {
				cdImageWidth.show();
			}
			try {
				Integer.parseInt(txtHeight.getText());
				cdImageHeight.hide();
			}catch (Exception ex) {
				cdImageHeight.show();
			}
		}else {
			cdImageHeight.hide();
			cdImageWidth.hide();
		}
	}

	
	private void createCameraTab(Composite parent) {
		
		Composite llGeneral = SmartUiUtils.createSubHeaderLabel(parent, Messages.CyberTrackerPropertiesComposite_PhotoSettingsHeader);
		llGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Label lblMaxPhotoCount = new Label(parent, SWT.NONE);
		lblMaxPhotoCount.setText(Messages.CyberTrackerPropertiesDialog_MaxPhotoCount);
		lblMaxPhotoCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_MaxPhotoCount_Tooltip);

		txtMaxPhotoCount = new Text(parent, SWT.BORDER);
		txtMaxPhotoCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_MaxPhotoCount_Tooltip);
		txtMaxPhotoCount.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtMaxPhotoCount.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isMaxPhotoCountValid()) {
					maxPhotoCountDecoration.hide();
				} else {
					maxPhotoCountDecoration.show();
				}
				changesMade();
			}

		});
		controls.add(txtMaxPhotoCount);
		maxPhotoCountDecoration = new ControlDecoration(txtMaxPhotoCount, SWT.LEFT);
		maxPhotoCountDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		maxPhotoCountDecoration.setShowHover(true);
		maxPhotoCountDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_MaxPhotoCountInvalid, CyberTrackerPropertiesProfile.MAX_PHOTO_COUNT_MIN_VALUE, CyberTrackerPropertiesProfile.MAX_PHOTO_COUNT_MAX_VALUE));
		maxPhotoCountDecoration.hide();
		
		Label lblResize = new Label(parent, SWT.NONE);
		lblResize.setText(Messages.CyberTrackerPropertiesComposite_ResizeOp);
		lblResize.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));

		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnOpResize = new Button(temp, SWT.CHECK );
		btnOpResize.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
		
		Label info = new Label(temp, SWT.WRAP);
		info.setText(Messages.CyberTrackerPropertiesComposite_ResizeMsg);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)info.getLayoutData()).widthHint = 150;
		
		//spacer
		new Label(parent, SWT.NONE);

		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		cmbSizes = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSizes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbSizes.getControl().getLayoutData()).widthHint = 50;
		cmbSizes.getControl().setEnabled(false);
		cmbSizes.setContentProvider(ArrayContentProvider.getInstance());
		cmbSizes.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				PhotoSize ps = ((PhotoSize)element);
				if (ps == PhotoSize.CUSTOM) return Messages.CyberTrackerPropertiesComposite_Custom;
				return ps.w + " x "+ ps.h; //$NON-NLS-1$
						
			}
		});
		cmbSizes.setInput(PhotoSize.values());
		cmbSizes.setSelection(new StructuredSelection(PhotoSize.SIZE1));
		
		Composite custom = new Composite(part, SWT.NONE);
		custom.setLayout(new GridLayout(6, false));
		custom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l1 = new Label(custom, SWT.NONE);
		l1.setText(Messages.CyberTrackerPropertiesComposite_WidthOp);
		l1.setEnabled(false);
		
		cdImageWidth = new ControlDecoration(l1, SWT.RIGHT);
		cdImageWidth.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdImageWidth.setShowHover(true);
		cdImageWidth.setDescriptionText(Messages.CyberTrackerPropertiesComposite_InvalidWidth);
		cdImageWidth.hide();
		
		txtWidth = new Text(custom, SWT.BORDER);
		txtWidth.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtWidth.getLayoutData()).widthHint = 100;
		txtWidth.setEnabled(false);
		
		Label l2 = new Label(custom, SWT.NONE);
		l2.setText(Messages.CyberTrackerPropertiesComposite_pixel);
		l2.setEnabled(false);
		
		Label l3 = new Label(custom, SWT.NONE);
		l3.setText(Messages.CyberTrackerPropertiesComposite_HeightOp);
		l3.setEnabled(false);
		
		cdImageHeight = new ControlDecoration(l3, SWT.RIGHT);
		cdImageHeight.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdImageHeight.setShowHover(true);
		cdImageHeight.setDescriptionText(Messages.CyberTrackerPropertiesComposite_InvalidHeight);
		cdImageHeight.hide();
		
		txtHeight = new Text(custom, SWT.BORDER);
		txtHeight.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtHeight.getLayoutData()).widthHint = 100;
		txtHeight.setEnabled(false);
		
		Label l4 = new Label(custom, SWT.NONE);
		l4.setText(Messages.CyberTrackerPropertiesComposite_pixel);
		l4.setEnabled(false);
		
		txtWidth.addListener(SWT.Modify, e->{validateCamera(); changesMade();});
		txtHeight.addListener(SWT.Modify, e->{validateCamera(); changesMade();});
		
		cmbSizes.addSelectionChangedListener(e->{
			PhotoSize s = (PhotoSize) cmbSizes.getStructuredSelection().getFirstElement();
			if (s == PhotoSize.CUSTOM) {
				for (Control kid : custom.getChildren()) kid.setEnabled(true);
			}else {
				for (Control kid : custom.getChildren()) kid.setEnabled(false);
			}
			validateCamera();
			changesMade();
		});
		btnOpResize.addListener(SWT.Selection, e->{
			cmbSizes.getControl().setEnabled(btnOpResize.getSelection());
			info.setEnabled(btnOpResize.getSelection());

			boolean iscustom = false;
			if (btnOpResize.getSelection()) {
				PhotoSize s = (PhotoSize) cmbSizes.getStructuredSelection().getFirstElement();
				if (s == PhotoSize.CUSTOM) iscustom = true;
			}
			for (Control kid : custom.getChildren()) kid.setEnabled(iscustom);
			validateCamera();
			changesMade();
		});
		
		controls.add(txtHeight);
		controls.add(txtWidth);
		controls.add(cmbSizes.getControl());
		controls.add(btnOpResize);
		
	}
	
	
	private void createThemeTab(Composite parent) {
		Composite llGeneral = SmartUiUtils.createSubHeaderLabel(parent, Messages.CyberTrackerPropertiesComposite_ThemeHeader);
		llGeneral.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(3, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label ltrack = new Label(part, SWT.NONE);
		ltrack.setText(Messages.CyberTrackerPropertiesComposite_TrackColorOp);
	
		btnTrackColor = new ColorSelector(part);
		controls.add(btnTrackColor.getButton());
		btnTrackColor.addListener(e->{
			btnTrackColor.getButton().setData(CLEARKEY, Boolean.FALSE);
			changesMade();	
		});		
		Button btnTrackClear = new Button(part, SWT.PUSH);
		controls.add(btnTrackClear);
		btnTrackClear.setText(Messages.CyberTrackerPropertiesComposite_ClearButton);
		btnTrackClear.addListener(SWT.Selection, e->{
			btnTrackColor.setColorValue(new RGB(255, 255, 255));
			btnTrackColor.getButton().setData(CLEARKEY, Boolean.TRUE);
			changesMade();
		});
		
		ProfileOptionID[] colorops = new ProfileOptionID[] {
				ProfileOptionID.THEME_COLOR_1,
				ProfileOptionID.THEME_COLOR_2,
				ProfileOptionID.THEME_COLOR_3,
				ProfileOptionID.THEME_COLOR_4,
		};
		btnThemeColors = new ArrayList<>(colorops.length);
		for (ProfileOptionID op : colorops) {
			Label l = new Label(part, SWT.NONE);
			switch(op) {
			case THEME_COLOR_1:l.setText(Messages.CyberTrackerPropertiesComposite_PrimaryColor + ":"); break; //$NON-NLS-1$
			case THEME_COLOR_2:l.setText(Messages.CyberTrackerPropertiesComposite_AccentColor + ":"); break; //$NON-NLS-1$
			case THEME_COLOR_3:l.setText(Messages.CyberTrackerPropertiesComposite_ForegroundColor + ":"); break; //$NON-NLS-1$
			case THEME_COLOR_4:l.setText(Messages.CyberTrackerPropertiesComposite_BackgroundCoor + ":");	break; //$NON-NLS-1$
			default: l.setText(""); //$NON-NLS-1$
			}
			
			ColorSelector colorLabel = new ColorSelector(part);
			colorLabel.getButton().setData(COLOR_OP_KEY, op);
			colorLabel.addListener(e->{
				colorLabel.getButton().setData(CLEARKEY, Boolean.FALSE);
				changesMade();	
			});

			controls.add(colorLabel.getButton());

			Button btnClear = new Button(part, SWT.PUSH);
			btnClear.setText(Messages.CyberTrackerPropertiesComposite_ClearButton);
			controls.add(btnClear);
			btnClear.addListener(SWT.Selection, e->{
				colorLabel.setColorValue(new RGB(255, 255, 255));
				colorLabel.getButton().setData(CLEARKEY, Boolean.TRUE);
				changesMade();
				});

			btnThemeColors.add(colorLabel);
		}
		
	}
	public void addPropsChangeListener(IPropsChangeListener pcl) {
		listeners.add(pcl);
	}
	
	public void removePropsChangeListener(IPropsChangeListener pcl) {
		listeners.remove(pcl);
	}

	private void changesMade() {
		if (isPopulating) {
			return;
		}
		for (IPropsChangeListener pcl : listeners) {
			pcl.changesMade();
		}
	}
	
	public void populateValuesFromObj(CyberTrackerPropertiesProfile ctProperties) {
		isPopulating = true;
		btnUseTitleBar.setSelection(ctProperties.isUseTitleBar());
		btnLargeTitles.setSelection(ctProperties.isUseLargeTitles());
		btnLargeScrollBars.setSelection(ctProperties.isLargeScrollBars());
		btnLargeTabs.setSelection(ctProperties.isUseLargeTabs());
		btnAutoNext.setSelection(ctProperties.isAutoNext());
		btnShowEdit.setSelection(ctProperties.isShowEdit());
		btnShowGPS.setSelection(ctProperties.isShowGPS());
		btnKioskMode.setSelection(ctProperties.isKioskMode());
		cmbUnits.setSelection(new StructuredSelection(ctProperties.getUnits()));
		btnUseIncidentGroup.setSelection(ctProperties.getUseIncidentGroupUi());
		btnSimpleCamera.setSelection(ctProperties.isSimpleCamera());
		btnCanPause.setSelection(ctProperties.isCanPause());
		btnDisableEditing.setSelection(ctProperties.isDisableEditing());
		btnSdCard.setSelection(ctProperties.isUseSdCard());
		btnTestTime.setSelection(ctProperties.isTestTime());
		btnResetOnSync.setSelection(ctProperties.isResetOnSync());
		btnResetOnNext.setSelection(ctProperties.isResetOnNext());
		txtExitPin.setText(String.valueOf(ctProperties.getExitPin()));
		txtMaxPhotoCount.setText(String.valueOf(ctProperties.getMaxPhotoCount()));
		txtSightingAccuracy.setText(String.valueOf(ctProperties.getSightingAccuracy()));
		txtSightingFixCount.setText(String.valueOf(ctProperties.getSightingFixCount()));
		txtTrackAccuracy.setText(String.valueOf(ctProperties.getTrackAccuracy()));
		cmbTrackTimer.setSelection(new StructuredSelection(ctProperties.getWaypointTimerType()));
		txtTrackTimer.setText(String.valueOf(ctProperties.getWaypointTimerValue()));
		btnUseGpsTime.setSelection(ctProperties.isUseGpsTime());
		timeOffset.setSelection(new StructuredSelection(ctProperties.getGpsTimeZone()));
		cbProjection.setSelection(new StructuredSelection(ctProperties.getProjection()));
		txtUtmZome.setText(String.valueOf(ctProperties.getUtmZone()));
		txtSkipButtonTimeout.setText(String.valueOf(ctProperties.getSkipButtonTimeout()));
		txtDilutionOfPrecision.setText(String.valueOf(ctProperties.getDilutionOfPrecision()));
		btnUseMapOnSkip.setSelection(ctProperties.isUseMapOnSkip());
		btnManualGPS.setSelection(ctProperties.isManualGps());
		btnAllowSkipManual.setSelection(ctProperties.isAllowSkipManualGps());
		txtFileName.setText(ctProperties.getFieldMapFilename());
		btnLock100.setSelection(ctProperties.isLock100());
		cmbDataFormat.setSelection(new StructuredSelection(ctProperties.getDataFormat()));
	
		java.awt.Color r = ctProperties.getTrackColor();
		if (r != null) {
			btnTrackColor.setColorValue(new RGB(r.getRed(),r.getGreen(), r.getBlue()));
		}
		btnTrackColor.getButton().setData(CLEARKEY, r==null);
		
		
		for (ColorSelector l : btnThemeColors) {
			r = ctProperties.getThemeColor((ProfileOptionID) l.getButton().getData(COLOR_OP_KEY));
			if (r != null) {
				l.setColorValue(new RGB(r.getRed(),r.getGreen(), r.getBlue()));	
			}
			l.getButton().setData(CLEARKEY, r==null);
		}
		
		btnOpResize.setSelection(ctProperties.getResizePhoto());
		if (ctProperties.getResizePhoto()) {
			int w = ctProperties.getImageWidth();
			int h = ctProperties.getImageHeight();
			
			PhotoSize defaultps = null;
			for (PhotoSize ps : PhotoSize.values()) {
				if (ps == PhotoSize.CUSTOM) continue;
				if (ps.w == w && ps.h == h) {
					defaultps = ps;
					break;
				}
			}
			if (defaultps != null) {
				cmbSizes.setSelection(new StructuredSelection(defaultps));
				for (Control kid : txtWidth.getParent().getChildren()) kid.setEnabled(false);
			}else {
				cmbSizes.setSelection(new StructuredSelection(PhotoSize.CUSTOM));
				txtHeight.setText(String.valueOf(h));
				txtWidth.setText(String.valueOf(w));
				for (Control kid : txtWidth.getParent().getChildren()) kid.setEnabled(btnOpResize.isEnabled());
			}
			if (btnOpResize.isEnabled())cmbSizes.getControl().setEnabled(true);
		}else {
			if (btnOpResize.isEnabled()) {
				cmbSizes.getControl().setEnabled(false);
				for (Control kid : txtWidth.getParent().getChildren()) kid.setEnabled(false);
			}
		}
		
		isPopulating = false;
	}

	public boolean recordValuesToObj(CyberTrackerPropertiesProfile ctProperties) {
		if (!validate()) {
			return false;
		}
		ctProperties.setUnits((CyberTrackerPropertiesProfileOption.Unit)cmbUnits.getStructuredSelection().getFirstElement());
		ctProperties.setAutoNext(btnAutoNext.getSelection());
		ctProperties.setUserIncidentGroupUi(btnUseIncidentGroup.getSelection());
		ctProperties.setLargeScrollBars(btnLargeScrollBars.getSelection());
		ctProperties.setKioskMode(btnKioskMode.getSelection());
		ctProperties.setSimpleCamera(btnSimpleCamera.getSelection());
		ctProperties.setCanPause(btnCanPause.getSelection());
		ctProperties.setExitPin(Integer.valueOf(txtExitPin.getText()));
		ctProperties.setDataFormat((CyberTrackerPropertiesOption.Protocol) cmbDataFormat.getStructuredSelection().getFirstElement());
			
		ctProperties.setSightingAccuracy(Double.valueOf(txtSightingAccuracy.getText()));
		ctProperties.setSightingFixCount(Integer.valueOf(txtSightingFixCount.getText()));
		
		ctProperties.setWaypointTimerType( ((CyberTrackerPropertiesProfileOption.TrackTimerOp)cmbTrackTimer.getStructuredSelection().getFirstElement()) );
		ctProperties.setWaypointTimerValue(Integer.valueOf(txtTrackTimer.getText()));
		
		StructuredSelection selection = (StructuredSelection) timeOffset.getSelection();
		ctProperties.setGpsTimeZone((Integer)selection.getFirstElement());
		selection = (StructuredSelection) cbProjection.getSelection();
		ctProperties.setProjection((Integer)selection.getFirstElement());
		ctProperties.setUtmZone(Integer.valueOf(txtUtmZome.getText()));
		ctProperties.setSkipButtonTimeout(Integer.valueOf(txtSkipButtonTimeout.getText()));
		ctProperties.setDilutionOfPrecision(Integer.valueOf(txtDilutionOfPrecision.getText()));
		
		ctProperties.setShowEdit(btnShowEdit.getSelection());
		ctProperties.setShowGPS(btnShowGPS.getSelection());
		ctProperties.setMaxPhotoCount(Integer.valueOf(txtMaxPhotoCount.getText()));
		
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
		
		RGB c = btnTrackColor.getColorValue();
		if (!((Boolean)btnTrackColor.getButton().getData(CLEARKEY))) {
			ctProperties.setTrackColor(new java.awt.Color(c.red, c.green, c.blue));
		}else {
			ctProperties.setTrackColor(null);
		}
		for (ColorSelector l : btnThemeColors) {
			c = l.getColorValue();
			if (!((Boolean)l.getButton().getData(CLEARKEY))) {
				ctProperties.setThemeColor((ProfileOptionID) l.getButton().getData(COLOR_OP_KEY), new java.awt.Color(c.red, c.green, c.blue));
			}else {
				ctProperties.setThemeColor((ProfileOptionID) l.getButton().getData(COLOR_OP_KEY), null);
			}
		}
		
		if (btnOpResize.getSelection()) {
			int width = 0;
			int height = 0;
			PhotoSize ps = (PhotoSize) cmbSizes.getStructuredSelection().getFirstElement();
			if (ps == PhotoSize.CUSTOM) {
				width = Integer.parseInt(txtWidth.getText());
				height = Integer.parseInt(txtHeight.getText());
			}else {
				width = ps.w;
				height = ps.h;
			}
			ctProperties.setResizePhoto(true,  width,  height);
		}else {
			ctProperties.setResizePhoto(false, 0, 0);
		}
		
		return true;
	}
	
	public void setReadOnly(boolean isReadOnly) {
		for (Control control : controls) control.setEnabled(!isReadOnly);
	}

	private boolean isExitPinValid() {
		if (txtExitPin == null || txtExitPin.getText() == null || txtExitPin.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtExitPin.getText());
			return result >= CyberTrackerPropertiesProfile.EXIT_PIN_MIN_VALUE && result <= CyberTrackerPropertiesProfile.EXIT_PIN_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isTrackAccuracyValid() {
		if (txtTrackAccuracy == null || txtTrackAccuracy.getText() == null || txtTrackAccuracy.getText().isEmpty())
			return false;
		try {
			Double result = Double.valueOf(txtTrackAccuracy.getText());
			return result >= CyberTrackerPropertiesProfile.TRACK_ACCURACY_MIN_VALUE && result <= CyberTrackerPropertiesProfile.TRACK_ACCURACY_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean isSigtingAccuracyValid() {
		if (txtSightingAccuracy == null || txtSightingAccuracy.getText() == null || txtSightingAccuracy.getText().isEmpty())
			return false;
		try {
			Double result = Double.valueOf(txtSightingAccuracy.getText());
			return result >= CyberTrackerPropertiesProfile.SIGHTING_ACCURACY_MIN_VALUE && result <= CyberTrackerPropertiesProfile.SIGHTING_ACCURACY_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isSigtingFixCountValid() {
		if (txtSightingFixCount == null || txtSightingFixCount.getText() == null || txtSightingFixCount.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtSightingFixCount.getText());
			return result >= CyberTrackerPropertiesProfile.SIGHTING_FIX_COUNT_MIN_VALUE && result <= CyberTrackerPropertiesProfile.SIGHTING_FIX_COUNT_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isSkipButtonTimeoutValid() {
		if (txtSkipButtonTimeout == null || txtSkipButtonTimeout.getText() == null || txtSkipButtonTimeout.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtSkipButtonTimeout.getText());
			return result >= CyberTrackerPropertiesProfile.SKIP_BUTTON_TIMEOUT_MIN_VALUE && result <= CyberTrackerPropertiesProfile.SKIP_BUTTON_TIMEOUT_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isDilutionOfPrecisionValid() {
		if (txtDilutionOfPrecision == null || txtDilutionOfPrecision.getText() == null || txtDilutionOfPrecision.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtDilutionOfPrecision.getText());
			return result >= CyberTrackerPropertiesProfile.DILUTION_OF_PRECISION_MIN_VALUE && result <= CyberTrackerPropertiesProfile.DILUTION_OF_PRECISION_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	
	private boolean isTrackTimerValid() {
		if (txtTrackTimer == null || txtTrackTimer.getText() == null || txtTrackTimer.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtTrackTimer.getText());
			return result >= CyberTrackerPropertiesProfile.TIME_TRACK_MIN_VALUE && result <= CyberTrackerPropertiesProfile.TIME_TRACK_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isUtmZoneValid() {
		if (txtUtmZome == null || txtUtmZome.getText() == null || txtUtmZome.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtUtmZome.getText());
			return result >= CyberTrackerPropertiesProfile.UTM_ZONE_MIN_VALUE && result <= CyberTrackerPropertiesProfile.UTM_ZONE_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean isMaxPhotoCountValid() {
		if (txtMaxPhotoCount == null || txtMaxPhotoCount.getText() == null || txtMaxPhotoCount.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtMaxPhotoCount.getText());
			return result >= CyberTrackerPropertiesProfile.MAX_PHOTO_COUNT_MIN_VALUE && result <= CyberTrackerPropertiesProfile.MAX_PHOTO_COUNT_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isFileNameDecorationValid() {
		return txtFileName.getText().isEmpty() || !Files.isDirectory(Paths.get(txtFileName.getText()));
	}

	private boolean validate() {
		return isExitPinValid() && isMaxPhotoCountValid() &&
				isSigtingAccuracyValid() && isSigtingFixCountValid() && 
				isTrackTimerValid() && isSkipButtonTimeoutValid() && isDilutionOfPrecisionValid() &&
				isTrackAccuracyValid() && isUtmZoneValid();
	}

	public interface IPropsChangeListener {
		public void changesMade();
	}
	
	private class CyberTrackerGMTLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Integer) {
				int x = (Integer) element;
				int val = Math.abs(x);
				boolean positive = x >= 0;
				String s = "GMT"; //$NON-NLS-1$
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

	
	private class CyberTrackerProjectionProvider extends LabelProvider {
		
		Map<Integer, String> id2Name;
		
		public CyberTrackerProjectionProvider() {
			id2Name = new HashMap<Integer, String>();
			for (ProjectionFormat p : ProjectionFormat.values()) {
				id2Name.put(p.getId(), p.getGuiName());
			}
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof Integer) {
				String name = id2Name.get((Integer) element);
				if (name != null)
					return name;
			}
			return super.getText(element);
		}
	}

}
