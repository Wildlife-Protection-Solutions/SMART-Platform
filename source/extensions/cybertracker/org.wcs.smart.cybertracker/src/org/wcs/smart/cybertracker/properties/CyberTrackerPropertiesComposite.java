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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesOption;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfileOption;
import org.wcs.smart.cybertracker.model.ProjectionFormat;

/**
 * Composite that contains controls to edit CyberTracker properties.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CyberTrackerPropertiesComposite extends Composite {
	
	private static final String COLOR_KEY = "COLOR"; //$NON-NLS-1$

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
    
    private Label btnTrackColor;
    private Label[] btnThemeColors;
    
    private ComboViewer cbProjection;
    private Text txtUtmZome;
	
    private ControlDecoration exitPinDecoration;
    
    private ControlDecoration sightingAccuracyDecoration;
    private ControlDecoration TrackAccuracyDecoration;
    private ControlDecoration sightingFixCountDecoration;
    private ControlDecoration trackTimerDecoration;
    private ControlDecoration skipButtonTimeoutDecoration;
    
    private ControlDecoration dilutionOfPrecisionDecoration;
    
    private ControlDecoration FileNameDecoration;

    private ControlDecoration utmZoneDecoration;
    
    private ControlDecoration maxPhotoCountDecoration;
	private CTabFolder tabFolder;
	
	private ComboViewer cmbSizes;
	private Text txtWidth, txtHeight;
	private ControlDecoration cdImageWidth, cdImageHeight;
	private Button btnOpResize;
	
	public CyberTrackerPropertiesComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void createContent(Composite parent) {
		
		tabFolder = new CTabFolder (parent, SWT.NONE);
		Rectangle clientArea = parent.getClientArea ();
		tabFolder.setLocation (clientArea.x, clientArea.y);
		
		tabFolder.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		
		
		CTabItem generalTab = new CTabItem (tabFolder, SWT.NONE);
		generalTab.setText (Messages.CyberTrackerPropertiesDialog_0);
		
		CTabItem gpsTab = new CTabItem (tabFolder, SWT.NONE);
		gpsTab.setText (Messages.CyberTrackerPropertiesDialog_1);
		
		CTabItem fieldmapTab = new CTabItem (tabFolder, SWT.NONE);
		fieldmapTab.setText (Messages.CyberTrackerPropertiesDialog_2);
		
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
		
		generalScroll.setContent(generalContainer);
		
		
		ScrolledComposite gpsScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		gpsScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		gpsScroll.setShowFocusedControl(true);
		gpsScroll.setExpandHorizontal(true);
		gpsScroll.setExpandVertical(true);
		
		Composite gpsContainer = new Composite(gpsScroll, SWT.None);
		gpsContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		gpsContainer.setLayout(new GridLayout(2, false));
		gpsScroll.setContent(gpsContainer);
		
		
		ScrolledComposite mapScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		mapScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mapScroll.setShowFocusedControl(true);
		mapScroll.setExpandHorizontal(true);
		mapScroll.setExpandVertical(true);
		
		Composite fieldmapContainer = new Composite(mapScroll, SWT.None);
		fieldmapContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		fieldmapContainer.setLayout(new GridLayout(2, false));
		mapScroll.setContent(fieldmapContainer);
		
		
		ScrolledComposite themeScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		themeScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		themeScroll.setShowFocusedControl(true);
		themeScroll.setExpandHorizontal(true);
		themeScroll.setExpandVertical(true);
		
		Composite themeContainer = new Composite(themeScroll, SWT.None);
		themeContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		themeContainer.setLayout(new GridLayout());
		themeScroll.setContent(themeContainer);
		
		ScrolledComposite cameraScroll = new ScrolledComposite(tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
		cameraScroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cameraScroll.setShowFocusedControl(true);
		cameraScroll.setExpandHorizontal(true);
		cameraScroll.setExpandVertical(true);
		
		Composite cameraContainer = new Composite(cameraScroll, SWT.None);
		cameraContainer.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true,1,1));
		cameraContainer.setLayout(new GridLayout());
		cameraScroll.setContent(cameraContainer);
		
		
		generalTab.setControl(generalScroll);
		gpsTab.setControl(gpsScroll);
		fieldmapTab.setControl(mapScroll);
		themeTab.setControl(themeScroll);
		cameraTab.setControl(cameraScroll);
		
		createThemeTab(themeContainer);
		createCameraTab(cameraContainer);
		
		Label lblUseTitleBar = new Label(generalContainer, SWT.NONE);
		lblUseTitleBar.setText(Messages.CyberTrackerPropertiesDialog_3);
		lblUseTitleBar.setToolTipText(Messages.CyberTrackerPropertiesDialog_4);
		

		btnUseTitleBar = new Button(generalContainer, SWT.CHECK);
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
		
		Label lblLargeTitles = new Label(generalContainer, SWT.NONE);
		lblLargeTitles.setText(Messages.CyberTrackerPropertiesDialog_6);
		lblLargeTitles.setToolTipText(Messages.CyberTrackerPropertiesDialog_7);

		btnLargeTitles = new Button(generalContainer, SWT.CHECK);
		btnLargeTitles.setToolTipText(Messages.CyberTrackerPropertiesDialog_7);
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
		
		Label lblLargeScrollBars = new Label(generalContainer, SWT.NONE);
		lblLargeScrollBars.setText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars);
		lblLargeScrollBars.setToolTipText(Messages.CyberTrackerPropertiesDialog_LargeScrollBars_Tooltip);

		btnLargeScrollBars = new Button(generalContainer, SWT.CHECK);
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
		
		Label lblLargeTabs = new Label(generalContainer, SWT.NONE);
		lblLargeTabs.setText(Messages.CyberTrackerPropertiesDialog_9);
		lblLargeTabs.setToolTipText(Messages.CyberTrackerPropertiesDialog_10);

		btnLargeTabs = new Button(generalContainer, SWT.CHECK);
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



		Label lblAutoNext = new Label(generalContainer, SWT.NONE);
		lblAutoNext.setText(Messages.CyberTrackerPropertiesDialog_AutoNext);
		lblAutoNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_AutoNext_Tooltip);

		btnAutoNext = new Button(generalContainer, SWT.CHECK);
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

		
		Label lblShowEdit = new Label(generalContainer, SWT.NONE);
		lblShowEdit.setText(Messages.CyberTrackerPropertiesDialog_ShowEdit1);
		lblShowEdit.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowEdit_Tooltip);

		btnShowEdit = new Button(generalContainer, SWT.CHECK);
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

		Label lblShowGPS = new Label(generalContainer, SWT.NONE);
		lblShowGPS.setText(Messages.CyberTrackerPropertiesDialog_ShowGPS);
		lblShowGPS.setToolTipText(Messages.CyberTrackerPropertiesDialog_ShowGPS_Tooltip);

		btnShowGPS = new Button(generalContainer, SWT.CHECK);
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

		
		Label lblSimpleCamera = new Label(generalContainer, SWT.NONE);
		lblSimpleCamera.setText(Messages.CyberTrackerPropertiesDialog_SimpleCamera);
		lblSimpleCamera.setToolTipText(Messages.CyberTrackerPropertiesDialog_SimpleCamera_Tooltip);

		btnSimpleCamera = new Button(generalContainer, SWT.CHECK);
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
		

		Label lblSdCard= new Label(generalContainer, SWT.NONE);
		lblSdCard.setText(Messages.CyberTrackerPropertiesDialog_15);
		lblSdCard.setToolTipText(Messages.CyberTrackerPropertiesDialog_16);

		btnSdCard = new Button(generalContainer, SWT.CHECK);
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
		
		
		Label lblResetOnSync= new Label(generalContainer, SWT.NONE);
		lblResetOnSync.setText(Messages.CyberTrackerPropertiesDialog_21);
		lblResetOnSync.setToolTipText(Messages.CyberTrackerPropertiesDialog_22);

		btnResetOnSync = new Button(generalContainer, SWT.CHECK);
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
		
		Label lblResetOnNext= new Label(generalContainer, SWT.NONE);
		lblResetOnNext.setText(Messages.CyberTrackerPropertiesDialog_24);
		lblResetOnNext.setToolTipText(Messages.CyberTrackerPropertiesDialog_25);

		btnResetOnNext = new Button(generalContainer, SWT.CHECK);
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

		exitPinDecoration = new ControlDecoration(txtExitPin, SWT.LEFT);
		exitPinDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		exitPinDecoration.setShowHover(true);
		exitPinDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_ExitPinInvalid, CyberTrackerPropertiesProfile.EXIT_PIN_MIN_VALUE, CyberTrackerPropertiesProfile.EXIT_PIN_MAX_VALUE));
		exitPinDecoration.hide();
		
		

		Label lblMaxPhotoCount = new Label(generalContainer, SWT.NONE);
		lblMaxPhotoCount.setText(Messages.CyberTrackerPropertiesDialog_MaxPhotoCount);
		lblMaxPhotoCount.setToolTipText(Messages.CyberTrackerPropertiesDialog_MaxPhotoCount_Tooltip);

		txtMaxPhotoCount = new Text(generalContainer, SWT.BORDER);
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

		maxPhotoCountDecoration = new ControlDecoration(txtMaxPhotoCount, SWT.LEFT);
		maxPhotoCountDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		maxPhotoCountDecoration.setShowHover(true);
		maxPhotoCountDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_MaxPhotoCountInvalid, CyberTrackerPropertiesProfile.MAX_PHOTO_COUNT_MIN_VALUE, CyberTrackerPropertiesProfile.MAX_PHOTO_COUNT_MAX_VALUE));
		maxPhotoCountDecoration.hide();
		
		Label lblDataFormat = new Label(generalContainer, SWT.NONE);
		lblDataFormat.setText(Messages.CyberTrackerPropertiesComposite_CtDataFormatLbl);
		lblDataFormat.setToolTipText(Messages.CyberTrackerPropertiesComposite_CtDataFormatTp);
		
		cmbDataFormat = new ComboViewer(generalContainer, SWT.READ_ONLY | SWT.DROP_DOWN);
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
		
		Label lblSigtingAccuracy = new Label(gpsContainer, SWT.NONE);
		lblSigtingAccuracy.setText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy);
		lblSigtingAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_SightingAccuracy_Tooltip);

		txtSightingAccuracy = new Text(gpsContainer, SWT.BORDER);
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

		sightingAccuracyDecoration = new ControlDecoration(txtSightingAccuracy, SWT.LEFT);
		sightingAccuracyDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		sightingAccuracyDecoration.setShowHover(true);
		sightingAccuracyDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_SightingAccuracyIvalid, CyberTrackerPropertiesProfile.SIGHTING_ACCURACY_MIN_VALUE, CyberTrackerPropertiesProfile.SIGHTING_ACCURACY_MAX_VALUE));
		sightingAccuracyDecoration.hide();
		

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
		
		Label lblTrackAccuracy = new Label(gpsContainer, SWT.NONE);
		lblTrackAccuracy.setText(Messages.CyberTrackerPropertiesDialog_27);
		lblTrackAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_28);

		txtTrackAccuracy = new Text(gpsContainer, SWT.BORDER);
		txtTrackAccuracy.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtTrackAccuracy.setToolTipText(Messages.CyberTrackerPropertiesDialog_28);
		txtTrackAccuracy.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTrackAccuracyValid()) {
					TrackAccuracyDecoration.hide();
				} else {
					TrackAccuracyDecoration.show();
				}
				changesMade();
			}
		});

		TrackAccuracyDecoration = new ControlDecoration(txtTrackAccuracy, SWT.LEFT);
		TrackAccuracyDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		TrackAccuracyDecoration.setShowHover(true);
		TrackAccuracyDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_30, CyberTrackerPropertiesProfile.TRACK_ACCURACY_MIN_VALUE, CyberTrackerPropertiesProfile.TRACK_ACCURACY_MAX_VALUE));
		TrackAccuracyDecoration.hide();
		

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
		
		cmbTrackTimer = new ComboViewer(trackTimer, SWT.DROP_DOWN);
		cmbTrackTimer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				switch((CyberTrackerPropertiesProfileOption.TrackTimerOp)element) {
				case DISTANCE: return Messages.CyberTrackerPropertiesComposite_TrackTimerDistanceOp;
				case TIME: return Messages.CyberTrackerPropertiesComposite_TrackTimerTimeOp;
				}
				return ""; //$NON-NLS-1$
			}
		});
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
		
		
		
		
		Label lblTimeOffset = new Label(gpsContainer, SWT.NONE);
		lblTimeOffset.setText(Messages.CyberTrackerPropertiesDialog_TimeOffset);
		lblTimeOffset.setToolTipText(Messages.CyberTrackerPropertiesDialog_TimeOffset_Tooltip);

		timeOffset = new ComboViewer(gpsContainer, SWT.READ_ONLY);
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
		
		
		
		Label lblUtmZome = new Label(gpsContainer, SWT.NONE);
		lblUtmZome.setText(Messages.CyberTrackerPropertiesDialog_UtmZone);
		lblUtmZome.setToolTipText(Messages.CyberTrackerPropertiesDialog_UtmZone_Tooltip);

		txtUtmZome = new Text(gpsContainer, SWT.BORDER);
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

		


		Label lblSkipButtonTimeout = new Label(gpsContainer, SWT.NONE);
		lblSkipButtonTimeout.setText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout);
		lblSkipButtonTimeout.setToolTipText(Messages.CyberTrackerPropertiesDialog_SkipButtonTimeout_Tooltip);

		txtSkipButtonTimeout = new Text(gpsContainer, SWT.BORDER);
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

		


		Label lblDilutionOfPrecision = new Label(gpsContainer, SWT.NONE);
		lblDilutionOfPrecision.setText(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision);
		lblDilutionOfPrecision.setToolTipText(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision_Tooltip);

		txtDilutionOfPrecision = new Text(gpsContainer, SWT.BORDER);
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

		dilutionOfPrecisionDecoration = new ControlDecoration(txtDilutionOfPrecision, SWT.LEFT);
		dilutionOfPrecisionDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		dilutionOfPrecisionDecoration.setShowHover(true);
		dilutionOfPrecisionDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesComposite_DilutionOfPrecision_Invalid, CyberTrackerPropertiesProfile.DILUTION_OF_PRECISION_MIN_VALUE, CyberTrackerPropertiesProfile.DILUTION_OF_PRECISION_MAX_VALUE));
		dilutionOfPrecisionDecoration.hide();




		Label lblMapFilename = new Label(fieldmapContainer, SWT.NONE);
		lblMapFilename.setText(Messages.CyberTrackerPropertiesDialog_40);
		lblMapFilename.setToolTipText(Messages.CyberTrackerPropertiesDialog_41);
		
	    Composite fileContainer = new Composite(fieldmapContainer, SWT.NONE);
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
	    		dlg.setFilterExtensions(new String[] {"*.ecw"}); //$NON-NLS-1$
	    		String fn = dlg.open();
	    		if (fn != null) {
	    			txtFileName.setText(fn);
	    		}
	    		changesMade();
	    	}
	    });

		
		Label lblLock100= new Label(fieldmapContainer, SWT.NONE);
		lblLock100.setText(Messages.CyberTrackerPropertiesDialog_45);
		lblLock100.setToolTipText(Messages.CyberTrackerPropertiesDialog_46);

		btnLock100 = new Button(fieldmapContainer, SWT.CHECK);
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
		
		
		
		generalScroll.setMinSize(generalContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		mapScroll.setMinSize(fieldmapContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		gpsScroll.setMinSize(gpsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		themeScroll.setMinSize(themeContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		tabFolder.setSelection(0);
	}

	private void disposeColor(Label label) {
		Color c = (Color) label.getData(COLOR_KEY);
		if (c != null) {
			c.dispose();
		}
		label.setData(COLOR_KEY, null);
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
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnOpResize = new Button(part, SWT.CHECK);
		btnOpResize.setText(Messages.CyberTrackerPropertiesComposite_ResizeOp);
		
		Label info = new Label(part, SWT.WRAP);
		info.setText(Messages.CyberTrackerPropertiesComposite_ResizeMsg);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)info.getLayoutData()).widthHint = 200;
		((GridData)info.getLayoutData()).horizontalIndent = 20;

		cmbSizes = new ComboViewer(part, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbSizes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)cmbSizes.getControl().getLayoutData()).horizontalIndent = 20;
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
		((GridData)custom.getLayoutData()).horizontalIndent = 20;
		
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
			
			boolean iscustom = false;
			if (btnOpResize.getSelection()) {
				PhotoSize s = (PhotoSize) cmbSizes.getStructuredSelection().getFirstElement();
				if (s == PhotoSize.CUSTOM) iscustom = true;
			}
			for (Control kid : custom.getChildren()) kid.setEnabled(iscustom);
			
			validateCamera();
			changesMade();
		});
		
	}
	
	
	private void createThemeTab(Composite parent) {
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout(4, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Label ltrack = new Label(part, SWT.NONE);
		ltrack.setText(Messages.CyberTrackerPropertiesComposite_TrackColorOp);
	
		btnTrackColor = new Label(part, SWT.NONE);
		btnTrackColor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)btnTrackColor.getLayoutData()).widthHint = 30;
		btnTrackColor.addListener(SWT.Dispose, e->disposeColor(btnTrackColor));		
		
		btnTrackColor.addListener(SWT.Paint, e->{
			if (btnTrackColor.getData(COLOR_KEY) != null) e.gc.drawRectangle(0, 0, btnTrackColor.getBounds().width-1, btnTrackColor.getBounds().height-1);
		});
		WidgetElement.setCSSClass(btnTrackColor, "customcolor"); //$NON-NLS-1$
		
		Button btnSetTrackColor = new Button(part, SWT.PUSH);
		btnSetTrackColor.setText("..."); //$NON-NLS-1$
		
		Button btnTrackClear = new Button(part, SWT.PUSH);
		btnTrackClear.setText(Messages.CyberTrackerPropertiesComposite_ClearButton);
		btnTrackClear.addListener(SWT.Selection, e->{
			disposeColor(btnTrackColor);
			btnTrackColor.setBackground(null);
			changesMade();
		});
		Listener changeColorTrack = e->{
			ColorDialog cd = new ColorDialog(parent.getShell());
			cd.setRGB(btnTrackColor.getBackground().getRGB());
			cd.setText(Messages.CyberTrackerPropertiesComposite_ColorSelectionDialogTitle);
			RGB rgb = cd.open();
			if (rgb == null) return;
			
			disposeColor(btnTrackColor);
			Color newColor = new Color(parent.getDisplay(), rgb);
			btnTrackColor.setData(COLOR_KEY, newColor);
			btnTrackColor.setBackground(newColor);
			changesMade();
		};
		
		btnSetTrackColor.addListener(SWT.Selection, changeColorTrack);
		btnTrackColor.addListener(SWT.MouseDoubleClick, changeColorTrack);
		
		btnThemeColors = new Label[4];
		for (int i = 1; i <= 4; i ++) {
			Label l = new Label(part, SWT.NONE);
			l.setText(MessageFormat.format(Messages.CyberTrackerPropertiesComposite_ColorLabel, i));
		
			Label colorLabel = new Label(part, SWT.NONE);
			colorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)colorLabel.getLayoutData()).widthHint = 30;
			colorLabel.addListener(SWT.Dispose, e->disposeColor(colorLabel));		
			WidgetElement.setCSSClass(colorLabel, "customcolor"); //$NON-NLS-1$
			colorLabel.addListener(SWT.Paint, e->{
				if (colorLabel.getData(COLOR_KEY) != null) e.gc.drawRectangle(0, 0, colorLabel.getBounds().width-1, colorLabel.getBounds().height-1);
			});
			
			Button btnColor = new Button(part, SWT.PUSH);
			btnColor.setText("..."); //$NON-NLS-1$
			
			Button btnClear = new Button(part, SWT.PUSH);
			btnClear.setText(Messages.CyberTrackerPropertiesComposite_ClearButton);
			btnClear.addListener(SWT.Selection, e->{
				disposeColor(colorLabel);
				colorLabel.setBackground(null);
				changesMade();
			});
			
			Listener changeColor = e->{
				ColorDialog cd = new ColorDialog(parent.getShell());
				cd.setRGB(colorLabel.getBackground().getRGB());
				cd.setText(Messages.CyberTrackerPropertiesComposite_ColorSelectionDialogTitle);
				RGB rgb = cd.open();
				if (rgb == null) return;
				
				disposeColor(colorLabel);
				Color newColor = new Color(parent.getDisplay(), rgb);
				colorLabel.setData(COLOR_KEY, newColor);
				colorLabel.setBackground(newColor);
				changesMade();
			};
			
			btnColor.addListener(SWT.Selection, changeColor);
			colorLabel.addListener(SWT.MouseDoubleClick, changeColor);
			
			btnThemeColors[i-1] = colorLabel;
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
			Color c = new Color(getDisplay(), r.getRed(), r.getGreen(), r.getBlue(), 255);
			btnTrackColor.setData(COLOR_KEY, c);
			btnTrackColor.setBackground(c);
		}
		
		for (int i = 1; i <= 4; i ++) {
			r = ctProperties.getThemeColor(i);
			if (r != null) {
				Color c = new Color(getDisplay(), r.getRed(), r.getGreen(), r.getBlue(), 255);
				btnThemeColors[i-1].setData(COLOR_KEY, c);
				btnThemeColors[i-1].setBackground(c);
			}
			
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
				for (Control kid : txtWidth.getParent().getChildren()) kid.setEnabled(true);
			}
			cmbSizes.getControl().setEnabled(true);
		}else {
			cmbSizes.getControl().setEnabled(false);
			for (Control kid : txtWidth.getParent().getChildren()) kid.setEnabled(false);
		}
		
		isPopulating = false;
	}

	public boolean recordValuesToObj(CyberTrackerPropertiesProfile ctProperties) {
		if (!validate()) {
			return false;
		}
		ctProperties.setAutoNext(btnAutoNext.getSelection());
		
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
		
		Color c = (Color)btnTrackColor.getData(COLOR_KEY);
		if (c == null) {
			ctProperties.setTrackColor(null);
		}else {
			ctProperties.setTrackColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue()));
		}
		
		for (int i = 1; i <= 4; i ++) {
			c = (Color) btnThemeColors[i-1].getData(COLOR_KEY);
			if (c != null) {
				ctProperties.setThemeColor(i, new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue()));
			}else {
				ctProperties.setThemeColor(i, null);
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
		Control[] controls = new Control[] {btnAutoNext,
				btnLargeScrollBars, btnKioskMode, btnSimpleCamera, btnCanPause, txtExitPin,
				txtSightingAccuracy, txtSightingFixCount, txtTrackTimer, timeOffset.getControl(),
				cbProjection.getControl(), txtUtmZome, txtSkipButtonTimeout, txtDilutionOfPrecision,
				btnShowEdit, btnShowGPS, txtMaxPhotoCount,
				btnUseTitleBar, btnLargeTitles, btnLargeTabs, btnDisableEditing, btnSdCard,
				btnTestTime, btnResetOnSync, btnResetOnNext, txtTrackAccuracy, btnUseGpsTime,
				btnManualGPS, btnAllowSkipManual, txtFileName, btnLock100, btnUseMapOnSkip}; 

		for (Control control : controls) {
			control.setEnabled(!isReadOnly);
		}
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
		return txtFileName.getText().isEmpty() || new File(txtFileName.getText()).isFile();
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
