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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.Mode;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption.ProfileOptionID;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.ProjectionLabelProvider;

/**
 * Composite that contains controls to edit CyberTracker properties.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CyberTrackerPropertiesComposite extends Composite {
	
	private static final String NONE = Messages.CyberTrackerPropertiesComposite_UnSetProjectionOp;
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

	
	
	private Button btnDisableEditing;
	
	private Button btnTestTime;
	private Button btnSimpleCamera;
	
	private Button btnKioskMode;
	private Button btnUseIncidentGroup;
	
	private Button btnCanPause;
	private Text txtExitPin;
	
	private Text txtSightingFixCount;
	
	private ComboViewer cmbTrackTimer;
	private Text txtTrackTimer;
	
	private Button btnUseGpsTime;
    private Text txtSkipButtonTimeout;
    private Text txtMaxPhotoCount;
    
    private Button btnManualGPS;
    private Button btnAllowSkipManual;
    
    private Button btnUseMapOnSkip;
    
    private ColorSelector btnTrackColor;
    private List<ColorSelector> btnThemeColors;
    
    private ComboViewer cbProjection;
    
	
    private ControlDecoration exitPinDecoration;
    
    
    private ControlDecoration sightingFixCountDecoration;
    private ControlDecoration trackTimerDecoration;
    private ControlDecoration skipButtonTimeoutDecoration;

    
    private ControlDecoration maxPhotoCountDecoration;
	private CTabFolder tabFolder;
	
	private ComboViewer cmbSizes;
	private ComboViewer cmbUnits;
	private ComboViewer cmbArchive;
	private ComboViewer cmbHistory;
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
		
		generalTab.setControl(generalScroll);
		gpsTab.setControl(gpsScroll);
//		fieldmapTab.setControl(mapScroll);
		themeTab.setControl(themeScroll);
		cameraTab.setControl(cameraScroll);
		
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
		
		Label lblSimpleCamera = new Label(generalContainer, SWT.NONE);
		lblSimpleCamera.setText(Messages.CyberTrackerPropertiesComposite_SimpleCameraOp);
		lblSimpleCamera.setToolTipText(Messages.CyberTrackerPropertiesComposite_SimpleCameraOpTooltip);

		btnSimpleCamera = new Button(generalContainer, SWT.CHECK);
		btnSimpleCamera.setToolTipText(lblSimpleCamera.getToolTipText());
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

		Label historyMode= new Label(generalContainer, SWT.NONE);
		historyMode.setText(Messages.CyberTrackerPropertiesComposite_HistoryModeLbl);
		historyMode.setToolTipText(Messages.CyberTrackerPropertiesComposite_HistoryModeTooltip);

		cmbHistory = new ComboViewer(generalContainer, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.SINGLE);
		cmbHistory.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				switch ((CyberTrackerPropertiesProfileOption.Mode)element) {
				case ENABLED: return Messages.CyberTrackerPropertiesComposite_EnabledOp;
				case DISABLED: return Messages.CyberTrackerPropertiesComposite_DisabledOp;
				}
				return super.getText(element);
			}
		});
		cmbHistory.setContentProvider(ArrayContentProvider.getInstance());
		cmbHistory.setInput(CyberTrackerPropertiesProfileOption.Mode.values());
		cmbHistory.addSelectionChangedListener(e->changesMade());
		controls.add(cmbHistory.getControl());
		
		Label archiveMode= new Label(generalContainer, SWT.NONE);
		archiveMode.setText(Messages.CyberTrackerPropertiesComposite_ArchiveModeLbl);
		archiveMode.setToolTipText(Messages.CyberTrackerPropertiesComposite_ArchiveModeTooltip);

		cmbArchive = new ComboViewer(generalContainer, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.SINGLE);
		cmbArchive.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				switch ((CyberTrackerPropertiesProfileOption.Mode)element) {
				case ENABLED: return Messages.CyberTrackerPropertiesComposite_EnabledOp;
				case DISABLED: return Messages.CyberTrackerPropertiesComposite_DisabledOp;
				}
				return super.getText(element);
			}
		});
		cmbArchive.setContentProvider(ArrayContentProvider.getInstance());
		cmbArchive.setInput(CyberTrackerPropertiesProfileOption.Mode.values());
		cmbArchive.addSelectionChangedListener(e->changesMade());
		controls.add(cmbArchive.getControl());
		
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
		
//		removed in SMART8; converted to selecting a projection from the list of CA projections		
//		Label lblProjection = new Label(gpsContainer, SWT.NONE);
//		lblProjection.setText(Messages.CyberTrackerPropertiesDialog_Projection);
//		lblProjection.setToolTipText(Messages.CyberTrackerPropertiesDialog_Projection_Tooltip);
//
//		cbProjection = new ComboViewer(gpsContainer, SWT.READ_ONLY);
//		cbProjection.getControl().setToolTipText(Messages.CyberTrackerPropertiesDialog_Projection_Tooltip);
//		cbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		cbProjection.setContentProvider(ArrayContentProvider.getInstance());
//		cbProjection.setLabelProvider(new CyberTrackerProjectionProvider());
// 		cbProjection.setInput(ProjectionFormat.getIds());
//		cbProjection.addSelectionChangedListener(new ISelectionChangedListener() {
//			@Override
//			public void selectionChanged(SelectionChangedEvent event) {
//				changesMade();
//			}
//		});
//		controls.add(cbProjection.getControl());
		
		Label lblProjection = new Label(gpsContainer, SWT.NONE);
		lblProjection.setText(Messages.CyberTrackerPropertiesComposite_ProjectionLabel);
		lblProjection.setToolTipText(Messages.CyberTrackerPropertiesComposite_ProjectionTooltip);

		cbProjection = new ComboViewer(gpsContainer, SWT.READ_ONLY);
		cbProjection.getControl().setToolTipText(Messages.CyberTrackerPropertiesDialog_Projection_Tooltip);
		cbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cbProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
 		//cbProjection.setInput(ProjectionFormat.getIds());
		try(Session session = HibernateManager.openSession()){
			List<Object> values = new ArrayList<>();
			values.add(NONE);
			values.addAll(HibernateManager.getCaProjectionList(session));
			cbProjection.setInput(values);
		}
		
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


		generalScroll.setMinSize(generalContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		gpsScroll.setMinSize(gpsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		themeScroll.setMinSize(themeContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		cameraScroll.setMinSize(cameraContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		tabFolder.setSelection(0);
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
		btnKioskMode.setSelection(ctProperties.isKioskMode());
		cmbUnits.setSelection(new StructuredSelection(ctProperties.getUnits()));
		cmbArchive.setSelection(new StructuredSelection(ctProperties.getArchiveMode()));
		cmbHistory.setSelection(new StructuredSelection(ctProperties.getHistoryMode()));
		btnUseIncidentGroup.setSelection(ctProperties.getUseIncidentGroupUi());
		btnCanPause.setSelection(ctProperties.isCanPause());
		btnDisableEditing.setSelection(ctProperties.isDisableEditing());
		btnTestTime.setSelection(ctProperties.isTestTime());
		btnSimpleCamera.setSelection(ctProperties.isSimpleCamera());
		txtExitPin.setText(String.valueOf(ctProperties.getExitPin()));
		txtMaxPhotoCount.setText(String.valueOf(ctProperties.getMaxPhotoCount()));
		txtSightingFixCount.setText(String.valueOf(ctProperties.getSightingFixCount()));
		cmbTrackTimer.setSelection(new StructuredSelection(ctProperties.getWaypointTimerType()));
		txtTrackTimer.setText(String.valueOf(ctProperties.getWaypointTimerValue()));
		btnUseGpsTime.setSelection(ctProperties.isUseGpsTime());
		try(Session session = HibernateManager.openSession()){
			Projection prj = ctProperties.getCaProjection(session, false);
			if (prj == null) {
				cbProjection.setSelection(new StructuredSelection(NONE));
			}else {
				cbProjection.setSelection(new StructuredSelection(prj));
			}
		}
		txtSkipButtonTimeout.setText(String.valueOf(ctProperties.getSkipButtonTimeout()));
		btnUseMapOnSkip.setSelection(ctProperties.isUseMapOnSkip());
		btnManualGPS.setSelection(ctProperties.isManualGps());
		btnAllowSkipManual.setSelection(ctProperties.isAllowSkipManualGps());
		
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
		ctProperties.setArchiveMode( (Mode) cmbArchive.getStructuredSelection().getFirstElement()  );
		ctProperties.setHistoryMode( (Mode) cmbHistory.getStructuredSelection().getFirstElement()  );
		ctProperties.setUserIncidentGroupUi(btnUseIncidentGroup.getSelection());
		ctProperties.setKioskMode(btnKioskMode.getSelection());
		ctProperties.setCanPause(btnCanPause.getSelection());
		ctProperties.setExitPin(Integer.valueOf(txtExitPin.getText()));
		ctProperties.setSightingFixCount(Integer.valueOf(txtSightingFixCount.getText()));
		ctProperties.setWaypointTimerType( ((CyberTrackerPropertiesProfileOption.TrackTimerOp)cmbTrackTimer.getStructuredSelection().getFirstElement()) );
		ctProperties.setWaypointTimerValue(Integer.valueOf(txtTrackTimer.getText()));
		
		Object prj = cbProjection.getStructuredSelection().getFirstElement();
		if (prj instanceof Projection pprj) {
			ctProperties.setCaProjection(pprj);
		}else {
			ctProperties.setCaProjection(null);
		}
		ctProperties.setSkipButtonTimeout(Integer.valueOf(txtSkipButtonTimeout.getText()));
		ctProperties.setMaxPhotoCount(Integer.valueOf(txtMaxPhotoCount.getText()));
		ctProperties.setDisableEditing(btnDisableEditing.getSelection());
		ctProperties.setTestTime(btnTestTime.getSelection());
		ctProperties.setSimpleCamera(btnSimpleCamera.getSelection());
		ctProperties.setUseGpsTime(btnUseGpsTime.getSelection());
		ctProperties.setManualGps(btnManualGPS.getSelection());
		ctProperties.setAllowSkipManualGps(btnAllowSkipManual.getSelection());		
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

	
	private boolean validate() {
		return isExitPinValid() && isMaxPhotoCountValid() &&
				isSigtingFixCountValid() && 
				isTrackTimerValid() && isSkipButtonTimeoutValid();
	}

	public interface IPropsChangeListener {
		public void changesMade();
	}
	
	

	/* 
	 * removed in SMART8
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
	*/

}
