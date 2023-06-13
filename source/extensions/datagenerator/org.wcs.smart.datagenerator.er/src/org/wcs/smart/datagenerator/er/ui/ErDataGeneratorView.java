/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.er.ui;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.common.celleditor.IntegerCellEditor;
import org.wcs.smart.datagenerator.er.DataEngineRunnable;
import org.wcs.smart.datagenerator.er.ErDataGenerator;
import org.wcs.smart.datagenerator.er.ErDataGeneratorPlugIn;
import org.wcs.smart.datagenerator.er.IDataEngine;
import org.wcs.smart.datagenerator.er.MissionSpatialShiftEngine;
import org.wcs.smart.datagenerator.er.MissionTimeShiftEngine;
import org.wcs.smart.datagenerator.er.internal.Messages;
import org.wcs.smart.datagenerator.er.model.ObservationConfiguration;
import org.wcs.smart.datagenerator.er.model.SurveyConfiguration;
import org.wcs.smart.datagenerator.er.model.xml.XmlManager;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.ui.UserNamePasswordDialog;
import org.wcs.smart.ui.ca.datamodel.AttributeFieldFactory;
import org.wcs.smart.ui.ca.datamodel.IAttributeField;
import org.wcs.smart.ui.properties.CategoryTreeDropDown;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * View for collection configuration details for generating
 * sample mission/survey data or updating existing sample data.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class ErDataGeneratorView {
	
	public static final String ID = "org.wcs.smart.datagenerator.er.ui.datageneratorview"; //$NON-NLS-1$
		
	private static final String USER_BBOX = Messages.DataGeneratorView_CustomArea;
	private static final String SHAPEFILE_BBOX = Messages.DataGeneratorView_LoadFromShapefile;
	
	FormToolkit toolkit = null;

	private SurveyConfiguration dataConfig;
	
	@Inject private Shell shell;
	@Inject private UISynchronize ui;
	@Inject private IEclipseContext context;
	
	private TimeShiftComposite timeShiftPanel;
	private Composite generatePanel;
	private SpatialShiftComposite spatialShiftPanel;

	private ComboViewer cmbDesigns;
	private Text txtNumSurvey;
	private Text txtNumMissionsPerSurvey;
	private DateTime dtStart;
	private DateTime dtEnd;
	private Text txtMissionDaysMin;
	private Text txtMissionDaysMax;
	private Text txtMissionEmployeesMin;
	private Text txtMissionEmployeesMax;
	private Text txtMissionWaypointMax;
	private Text txtMissionWaypointMin;
	private Text txtMissionObservationsMax;
	private Text txtMissionObservationsMin;
	private Text txtBoundingBox;
	private TableViewer tblObservations;
	private CategoryTreeDropDown newCategoryViewer;
	private Button btnGenerateData;
	private ComboViewer cmbBoundingBox;
	private Category lastSelectedCategory = null;
	
	private boolean isInitializing = false;
	
	private List<ControlDecoration> decorations = new ArrayList<>();
	
	private LabelProvider typeLabelProvider = new LabelProvider() {
		public String getText(Object element) {
			if (element instanceof ObservationConfiguration.Type) return ((ObservationConfiguration.Type) element).name();
			return super.getText(element);
			
		}};
		
		
	private Path workingFile = null;
	
	/**
	 * Creates new view
	 */
	public ErDataGeneratorView() {
		workingFile = Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), ErDataGeneratorPlugIn.PLUGIN_ID + ".xml"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@PreDestroy
	public void dispose() {
		toolkit.dispose();
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
	
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.adapt(parent);
		
		parent.setLayout(new GridLayout());

		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		generatePanel = createGenerateDataTab(tabs);
		timeShiftPanel = new TimeShiftComposite(tabs, this);
		ContextInjectionFactory.inject(timeShiftPanel, context);
		spatialShiftPanel = new SpatialShiftComposite(tabs, this);
		ContextInjectionFactory.inject(spatialShiftPanel, context);
		
		CTabItem generateTab = new CTabItem(tabs, SWT.NONE);
		generateTab.setText(Messages.DataGeneratorView_DataTab);
		generateTab.setControl(generatePanel);
		
		CTabItem timeShiftTab = new CTabItem(tabs, SWT.NONE);
		timeShiftTab.setText(Messages.DataGeneratorView_TimeShiftTab);
		timeShiftTab.setControl(timeShiftPanel);
		
		CTabItem spatialShiftTab = new CTabItem(tabs, SWT.NONE);
		spatialShiftTab.setText(Messages.DataGeneratorView_SpatialShiftTab);
		spatialShiftTab.setControl(spatialShiftPanel);
		
		tabs.setSelection(0);
		
		initView.schedule();
	}

	Composite createHeader(Composite parent, String message) {
		Composite infoComp = toolkit.createComposite(parent, SWT.BORDER);
		infoComp.setLayout(new GridLayout(2, false));
		((GridLayout)infoComp.getLayout()).marginWidth = 5;
		((GridLayout)infoComp.getLayout()).marginHeight = 10;
		Label  l = toolkit.createLabel(infoComp, ""); //$NON-NLS-1$
		l.setBackground(infoComp.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.INFO_ICON));
		l = toolkit.createLabel(infoComp, message);
		l.setBackground(infoComp.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		infoComp.setBackground(infoComp.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		return infoComp;
	}
	
	Composite createInfoSection(Composite parent, String message) {
		Composite infoComp = toolkit.createComposite(parent, SWT.NONE);
		infoComp.setLayout(new GridLayout(2, false));
		((GridLayout)infoComp.getLayout()).marginWidth = 5;
		((GridLayout)infoComp.getLayout()).marginHeight = 5;
		Label  l = toolkit.createLabel(infoComp, ""); //$NON-NLS-1$
		l.setBackground(infoComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		l = toolkit.createLabel(infoComp, message);
		l.setBackground(infoComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		infoComp.setBackground(infoComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		return infoComp;
	}
	
	private Composite createGenerateDataTab(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());

		//header
		Composite infoComp = createHeader(main, Messages.DataGeneratorView_DataTabDescription);
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2, 1));
		
		Section detailsSection = toolkit.createSection(main,Section.TITLE_BAR);
		detailsSection.setText(Messages.DataGeneratorView_MissionDetailsSection);
		detailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite outer = toolkit.createComposite(detailsSection);
		outer.setLayout(new GridLayout(2, false));
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)outer.getLayout()).horizontalSpacing = 40;
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		detailsSection.setClient(outer);
		
		//generation details
		Composite detailComp = toolkit.createComposite(outer);
		detailComp.setLayout(new GridLayout(4, false));
		detailComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)detailComp.getLayout()).marginWidth = 0;
		((GridLayout)detailComp.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(detailComp, Messages.ErDataGeneratorView_SurveyDesignOp);
		cmbDesigns = new ComboViewer(detailComp, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbDesigns.getControl().setBackground(detailComp.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		cmbDesigns.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SurveyDesign) return ((SurveyDesign) element).getName();
				return super.getText(element);
			}
		});
		cmbDesigns.setContentProvider(ArrayContentProvider.getInstance());
		cmbDesigns.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		cmbDesigns.setInput(DialogConstants.LOADING_TEXT);
		final ControlDecoration cd = createCd(cmbDesigns.getControl());
		
		cmbDesigns.addPostSelectionChangedListener(e->{
			enableGenerateButton(false);
			Object x = cmbDesigns.getStructuredSelection().getFirstElement();
			if (x instanceof SurveyDesign) {
				dataConfig.setSurveyDesignKey(((SurveyDesign)x).getKeyId());
				cd.hide();
				saveConfig();
				enableGenerateButton(true);
			}else {
				cd.setDescriptionText(Messages.ErDataGeneratorView_designRequired);
				cd.show();
			}
		});
		
		
		
		l = toolkit.createLabel(detailComp, Messages.ErDataGeneratorView_NumberOfSurveys);
		txtNumSurvey = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		txtNumSurvey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		addIntegerValidate(txtNumSurvey,  false, d->dataConfig.setNumberOfSurveys(d));
		
		l = toolkit.createLabel(detailComp, Messages.ErDataGeneratorView_MissionsPerSurvey);
		txtNumMissionsPerSurvey = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		txtNumMissionsPerSurvey.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		addIntegerValidate(txtNumMissionsPerSurvey,  false, d->dataConfig.setNumberOfMissionsPerSurvey(d));
		
		
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_DateRangeLbl);
		dtStart = new DateTime(detailComp, SWT.DATE | SWT.DROP_DOWN);
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_ToLbl);
		dtEnd = new DateTime(detailComp, SWT.DATE | SWT.DROP_DOWN);
		ControlDecoration cdDtEnd = createCd(dtEnd);
		dtEnd.addListener(SWT.Selection, e->{
			enableGenerateButton(false);
			if (SmartUtils.toDate(dtEnd).isBefore(SmartUtils.toDate(dtStart))) {
				cdDtEnd.setDescriptionText(Messages.DataGeneratorView_EndDateError);
				cdDtEnd.show();
			}else {
				dataConfig.setEndDate( SmartUtils.toDate(dtEnd) );
				cdDtEnd.hide();
				saveConfig();
				enableGenerateButton(true);
			}
		});
		dtStart.addListener(SWT.Selection, e->{
			enableGenerateButton(false);
			if (SmartUtils.toDate(dtEnd).isBefore(SmartUtils.toDate(dtStart))) {
				cdDtEnd.setDescriptionText(Messages.DataGeneratorView_EndDateError);
				cdDtEnd.show();
			}else {
				dataConfig.setStartDate( SmartUtils.toDate(dtStart) );
				cdDtEnd.hide();
				saveConfig();
				enableGenerateButton(true);
			}
		});
		
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_DaysPerMissionLbl);
		txtMissionDaysMin = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		txtMissionDaysMin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_ToLbl);
		txtMissionDaysMax = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		txtMissionDaysMax.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		addIntegerValidate(txtMissionDaysMin, txtMissionDaysMax, false, d->dataConfig.setDaysPerMissionMin(d), d->dataConfig.setDaysPerMissionMax(d));
		
		
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_EmployeesPerMissionLbl);
		txtMissionEmployeesMin = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_ToLbl);
		txtMissionEmployeesMax = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		addIntegerValidate(txtMissionEmployeesMin, txtMissionEmployeesMax, false, d->dataConfig.setEmployeesPerMissionMin(d), d->dataConfig.setEmployeesPerMissionMax(d));
		txtMissionEmployeesMax.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		txtMissionEmployeesMin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_WaypointsPerDaylbl);
		txtMissionWaypointMin = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_ToLbl);
		txtMissionWaypointMax = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		addIntegerValidate(txtMissionWaypointMin, txtMissionWaypointMax, false, d->dataConfig.setWaypointsPerDayMin(d), d->dataConfig.setWaypointsPerDayMax(d));
		
		txtMissionWaypointMax.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		txtMissionWaypointMin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_ObsPerWapointLbl);
		txtMissionObservationsMin = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_ToLbl);
		txtMissionObservationsMax = toolkit.createText(detailComp, ""); //$NON-NLS-1$
		addIntegerValidate(txtMissionObservationsMin, txtMissionObservationsMax, true, d->dataConfig.setObservationsPerWaypointMin(d), d->dataConfig.setObservationsPerWaypointMax(d));
		txtMissionObservationsMax.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		txtMissionObservationsMin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		l = toolkit.createLabel(detailComp, Messages.DataGeneratorView_BBoxLbl);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite boundBoxComp = toolkit.createComposite(detailComp);
		boundBoxComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		boundBoxComp.setLayout(new GridLayout());
		
		
		cmbBoundingBox = new ComboViewer(boundBoxComp, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbBoundingBox.setContentProvider(ArrayContentProvider.getInstance());
		cmbBoundingBox.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Area.AreaType) return ((Area.AreaType) element).getGuiName(Locale.getDefault());
				return super.getText(element);
			}
		});
		cmbBoundingBox.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		toolkit.adapt(cmbBoundingBox.getControl(), true, true);
		
		
		txtBoundingBox = toolkit.createText(boundBoxComp, Messages.DataGeneratorView_SampleBboxValue);
		txtBoundingBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtBoundingBox.setEnabled(false);
		
		ControlDecoration cdBBox = createCd(txtBoundingBox);
		cdBBox.setDescriptionText(Messages.DataGeneratorView_BboxErrorMsg);
		
		cmbBoundingBox.addSelectionChangedListener(e->{
			if (dataConfig == null || isInitializing) return;
			Object x = cmbBoundingBox.getStructuredSelection().getFirstElement();
			
			
			
			txtBoundingBox.setEnabled(x == USER_BBOX || x == SHAPEFILE_BBOX);
			
			if (x == SHAPEFILE_BBOX) {
				Path sfile = loadShapefile();
				if (sfile != null) {
					cmbBoundingBox.setSelection(new StructuredSelection(USER_BBOX));
					//read shapefile find bbox and update ui
					readShapefileBbox(sfile);
				}
				
			}else if (x instanceof Area.AreaType) {
				dataConfig.setBboxArea(((Area.AreaType)x));
				saveConfig();
				cdBBox.hide();
				enableGenerateButton(true);
			}else {
				//validate envelope
				Event evt = new Event();
				evt.widget = txtBoundingBox;
				txtBoundingBox.notifyListeners(SWT.Modify, evt);
			}
			
			
		});
		txtBoundingBox.addListener(SWT.Modify, e->{
			if (dataConfig == null || isInitializing) return;
			String[] parts = txtBoundingBox.getText().split(","); //$NON-NLS-1$
			enableGenerateButton(false);
			if (parts.length != 4) {
				cdBBox.show();
				return;
			}
			double x1, x2, y1, y2;
			try {
				x1 = Double.parseDouble(parts[0]);
				y1 = Double.parseDouble(parts[1]);
				x2 = Double.parseDouble(parts[2]);
				y2 = Double.parseDouble(parts[3]);
				
				if (x1 < -180 || x2 < -180 || x1 > 180 || x2 > 180 || y1 < -90 || y2 < -90 || y1 > 90 || y2 > 90 || x1 > x2 || y1 > y2) {
					cdBBox.show();
					return;
				}
				
			}catch (Exception ex) {
				cdBBox.show();
				return;
			}
			cdBBox.hide();
			enableGenerateButton(true);
			Envelope env = new Envelope(x1, x2, y1, y2);
			dataConfig.setBboxEnvelope(env);
			saveConfig();
		});
		
		

		//actions
		Composite actionComp = toolkit.createComposite(outer);
		actionComp.setLayout(new GridLayout());
		actionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout) actionComp.getLayout()).marginWidth = 0;
		((GridLayout) actionComp.getLayout()).marginHeight = 0;

		btnGenerateData = toolkit.createButton(actionComp, Messages.DataGeneratorView_GenerateBtn, SWT.PUSH);
		btnGenerateData.setEnabled(false);
		btnGenerateData.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnGenerateData.addListener(SWT.Selection, e->doGenerateData());

		Label t = toolkit.createLabel(actionComp, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button btnExportConfig = toolkit.createButton(actionComp, Messages.DataGeneratorView_ExportBtn, SWT.PUSH);
		btnExportConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnExportConfig.addListener(SWT.Selection, e -> exportConfig());

		Button btnImportConfig = toolkit.createButton(actionComp, Messages.DataGeneratorView_ImportBtn, SWT.PUSH);
		btnImportConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnImportConfig.addListener(SWT.Selection, e -> importConfig());

		t = toolkit.createLabel(actionComp, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Button btnReset = toolkit.createButton(actionComp, "Reset", SWT.PUSH); //$NON-NLS-1$
		btnReset.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnReset.addListener(SWT.Selection, e -> {
			SurveyConfiguration c = new SurveyConfiguration();
			initDataGeneratorConfiguration(c);
		});
		
		
		
		//observation details
		Section observationForm = toolkit.createSection(main,Section.TITLE_BAR);
		observationForm.setText(Messages.DataGeneratorView_ObservationsSection);
		observationForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite all = toolkit.createComposite(observationForm);
		all.setLayout(new GridLayout());
		((GridLayout)all.getLayout()).marginWidth = 0;
		((GridLayout)all.getLayout()).marginHeight = 0;
		observationForm.setClient(all);
		
		Composite info= createInfoSection(all, Messages.ErDataGeneratorView_categoriesSdInfo);
		info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SashForm obsComp = new SashForm(all, SWT.HORIZONTAL);
		obsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite obsWeights = toolkit.createComposite(obsComp);
		obsWeights.setLayout(new GridLayout());
		((GridLayout)obsWeights.getLayout()).marginWidth = 0;
		((GridLayout)obsWeights.getLayout()).marginHeight = 0;
		obsWeights.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tblObservations = new TableViewer(obsWeights, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tblObservations.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblObservations.getTable().setHeaderVisible(true);
		tblObservations.getTable().setLinesVisible(true);
		tblObservations.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn colWeights = new TableViewerColumn(tblObservations, SWT.NONE);
		colWeights.getColumn().setText(Messages.DataGeneratorView_WeightColumn);
		colWeights.getColumn().setWidth(100);
		colWeights.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				ObservationConfiguration om = (ObservationConfiguration)element;
				if (element instanceof ObservationConfiguration) return MessageFormat.format("{0} ({1}%)", om.getWeight(), (int)Math.round( (om.getWeight() / ((double)dataConfig.getTotalWeight())) * 100));    //$NON-NLS-1$
				return super.getText(element);
			}
		});
		colWeights.setEditingSupport(new EditingSupport(colWeights.getViewer()) {
			private IntegerCellEditor cellEditor = new IntegerCellEditor(tblObservations.getTable());
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof ObservationConfiguration && value instanceof Integer) {
					dataConfig.updateWeight((ObservationConfiguration)element, ((Integer)value));
					tblObservations.refresh();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof ObservationConfiguration) return ((ObservationConfiguration)element).getWeight();
				return 10;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return cellEditor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return (element instanceof ObservationConfiguration);
			}
		});
		
		TableViewerColumn colObservation = new TableViewerColumn(tblObservations, SWT.NONE);
		colObservation.getColumn().setText(Messages.DataGeneratorView_ObsColumn);
		colObservation.getColumn().setWidth(500);
		colObservation.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof ObservationConfiguration) return ((ObservationConfiguration)element).asText();
				return super.getText(element);
			}
		});
		
		Menu mnu = new Menu(tblObservations.getControl());
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->{
			IStructuredSelection sel = tblObservations.getStructuredSelection();
			for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
				Object observationMapping = (Object) iterator.next();
				if (observationMapping instanceof ObservationConfiguration) dataConfig.removeMapping((ObservationConfiguration)observationMapping);
			}
			tblObservations.refresh();
			saveConfig();
		});
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				miDelete.setEnabled(!tblObservations.getSelection().isEmpty());
			}
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		tblObservations.getControl().setMenu(mnu);
		
		Composite obsNew = toolkit.createComposite(obsComp);
		obsNew.setLayout(new GridLayout(2, false));
		obsNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		((GridLayout)obsNew.getLayout()).marginHeight = 0;
		
		l = toolkit.createLabel(obsNew, Messages.DataGeneratorView_CategoryLbl);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		newCategoryViewer = new CategoryTreeDropDown();
		newCategoryViewer.createComposite(obsNew);
		obsNew.addListener(SWT.Dispose, e->newCategoryViewer.dispose());
		
		l = toolkit.createLabel(obsNew, Messages.DataGeneratorView_AttributeLbl);
		l.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		ScrolledComposite scrollNew = new ScrolledComposite(obsNew, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		toolkit.adapt(scrollNew);
		scrollNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrollNew.setExpandHorizontal(true);
		scrollNew.setExpandVertical(true);
		
		Composite obsNewAttributePanel = toolkit.createComposite(scrollNew, SWT.NONE);
		scrollNew.setContent(obsNewAttributePanel);

		newCategoryViewer.addSelectionChangedListener(e-> {
			Category next = newCategoryViewer.getValue();
			if (next == lastSelectedCategory) return;
			lastSelectedCategory = next;

			//dispose of controls
			for (Control ctr : obsNewAttributePanel.getChildren()) ctr.dispose();
			if (newCategoryViewer.getValue() == null) return;
			Category c = null;
			
			//load attribute
			List<Attribute> allatts = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				c = (Category) session.get(Category.class, next.getUuid());
				c.getAllAttribute(allatts, true);

				//lazy load trees & lists
				ArrayDeque<AttributeTreeNode> nodes = new ArrayDeque<>();
				for (Attribute a : allatts) {
					if (a.getType() == AttributeType.TREE) {
						nodes.addAll(a.getActiveTreeNodes());
					}else if (a.getType().isList()) {
						a.getActiveListItems().forEach(li->li.getName());
					}
				}
				while(!nodes.isEmpty()) {
					AttributeTreeNode n = nodes.removeFirst();
					n.getFullCategoryName();
					if (n.getActiveChildren() != null) nodes.addAll(n.getActiveChildren());
				}
			}
			
			//update ui panel
			updateAttributePanel(obsNewAttributePanel, c, allatts);
		});
		
		toolkit.createLabel(obsNew, ""); //$NON-NLS-1$
		
		Button btnAddObservation = toolkit.createButton(obsNew, Messages.DataGeneratorView_AddObsButton, SWT.PUSH);
		btnAddObservation.setEnabled(false);
		btnAddObservation.addListener(SWT.Selection, e->addObservation(obsNewAttributePanel));
		newCategoryViewer.addSelectionChangedListener(event->{
				btnAddObservation.setEnabled(newCategoryViewer.getValue() != null);
		});
		return main;
	}
		
	private void readShapefileBbox(Path sfile) {
		ProgressMonitorDialog pd = new ProgressMonitorDialog(shell);
		try {
			pd.run(true,  false,new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						List<IService> services = CatalogPlugin.getDefault().getLocalCatalog().constructServices(sfile.toUri().toURL(),  new NullProgressMonitor());
						if (services.size() == 0) throw new Exception(Messages.SpatialShiftComposite_ResourcesNotFound + sfile.toString());
						List<? extends IGeoResource> resources = services.get(0).resources(new NullProgressMonitor());
						
						//compute center and approximate scale
						SimpleFeatureSource fs = resources.get(0).resolve(SimpleFeatureSource.class, new NullProgressMonitor());
						//Reproject
						ReferencedEnvelope re = fs.getFeatures().getBounds();
						Envelope e = JTS.transform(re, CRS.findMathTransform(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS));
						
						
						ui.syncExec(()->txtBoundingBox.setText(e.getMinX() + "," + e.getMinY() + "," + e.getMaxX() + "," + e.getMaxY())); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						
					}catch (Exception ex) {
						ErDataGeneratorPlugIn.log(ex.getMessage(), ex);
						ui.syncExec(()->{
							MessageDialog.openError(shell, Messages.SpatialShiftComposite_ReadErrorTitle, MessageFormat.format(Messages.SpatialShiftComposite_ReadErrorMsg, sfile.toString(), ex.getMessage()));
						});
					}
						
					return;
				}
			});
		}catch (Exception ex) {
			ErDataGeneratorPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	private void addIntegerValidate(Text text, boolean canZero, Consumer<Integer> doUpdate) {
		ControlDecoration cd = createCd(text);
		
		text.addModifyListener(e->{
			enableGenerateButton(false);
			try{
				int p = Integer.parseInt(text.getText());
				if (!canZero && p < 1) throw new Exception(Messages.DataGeneratorView_PostiveNumberRequired);
				if (canZero && p < 0) throw new Exception(Messages.DataGeneratorView_PostiveNumberRequired);
				
				doUpdate.accept(p);
				
				cd.hide();
				saveConfig();
				enableGenerateButton(true);
			}catch (Exception ex) {
				if (canZero) {
					cd.setDescriptionText(Messages.DataGeneratorView_IntegerRequired1);
				}else {
					cd.setDescriptionText(Messages.DataGeneratorView_IntegerRequired2);
				}
				cd.show();
			}
		});
	}
	
	private void addIntegerValidate(Text text1, Text text2, boolean canZero, Consumer<Integer> doUpdate1, Consumer<Integer> doUpdate2) {
		ControlDecoration cd1 = createCd(text1);
		ControlDecoration cd2 = createCd(text2);
		
		Listener l = e->{
			enableGenerateButton(false);
			int p1 = 0;
			int p2 = 0;
			try{
				p1 = Integer.parseInt(text1.getText());
			}catch (Exception ex) {
				cd1.show();
				cd1.setDescriptionText(Messages.DataGeneratorView_IntegerRequired3);
				return;
			}
			
			try{
				p2 = Integer.parseInt(text2.getText());
			}catch (Exception ex) {
				cd2.show();
				cd2.setDescriptionText(Messages.DataGeneratorView_IntegerRequired3);
				return;
			}
			
			try {
				if (!canZero && (p1 < 1 || p1 > 100)) throw new Exception(Messages.DataGeneratorView_IntegerRequired4);
				if (canZero && (p1 < 0 || p1 > 100)) throw new Exception(Messages.DataGeneratorView_IntegerRequired5);
			}catch (Exception ex) {
				cd1.show();
				cd1.setDescriptionText(ex.getMessage());
				return;
			}
			try {
				if (!canZero && (p2 < 1 || p2 > 100)) throw new Exception(Messages.DataGeneratorView_IntegerRequired4);
				if (canZero && (p2 < 0 || p2 > 100)) throw new Exception(Messages.DataGeneratorView_IntegerRequired5);
			}catch (Exception ex) {
				cd2.show();
				cd2.setDescriptionText(ex.getMessage());
				return;
			}
			
			if(p2 < p1) {
				cd2.show();
				cd2.setDescriptionText(Messages.DataGeneratorView_MinMaxInvalid);
				return;
			}
			
			if (!isInitializing) {
				doUpdate1.accept(p1);
				doUpdate2.accept(p2);
			}
				
			cd1.hide();
			cd2.hide();
			saveConfig();
			enableGenerateButton(true);
		};
		
		text1.addListener(SWT.Modify, l);
		text2.addListener(SWT.Modify, l);
	}
	
	
	private void saveConfig() {
		//TODO: add listener to save immediately on close
		saveJob.schedule(1000);
	}
	
	private void enableGenerateButton(boolean enable) {
		if (!enable) {
			btnGenerateData.setEnabled(enable);
			return;
		}else {
			for (ControlDecoration c : decorations) {
				if (c.isVisible()) {
					btnGenerateData.setEnabled(false);
					return;
				}
			}
			btnGenerateData.setEnabled(true);
		}
		
	}
	
	ControlDecoration createCd(Control owner) {
		ControlDecoration cd = new ControlDecoration(owner, SWT.TOP | SWT.RIGHT);
		cd.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.hide();
		decorations.add(cd);
		return cd;
	}
	
	private void addObservation(Composite attributePanel) {
		Category c = newCategoryViewer.getValue();
		Object[] attData = (Object[]) attributePanel.getData();
		
		WaypointObservation wo = new WaypointObservation();
		wo.setCategory(c);
		wo.setAttributes(new ArrayList<>());
		HashMap<Attribute, ObservationConfiguration.Type> typeMapping = new HashMap<>();
		
		for (int i = 0; i <attData.length; i +=2) {
			ComboViewer cv = (ComboViewer) attData[i];
			IAttributeField<?> field = (IAttributeField<?>) attData[i+1];
			
			ObservationConfiguration.Type type = (ObservationConfiguration.Type)cv.getStructuredSelection().getFirstElement();
			typeMapping.put(field.getAttribute(), type);
			if (type == ObservationConfiguration.Type.FIXED) {
				WaypointObservationAttribute woa = new WaypointObservationAttribute();
				woa.setAttribute(field.getAttribute());
				woa.setAttributeValue(field.getValue());
				woa.setObservation(wo);
				wo.getAttributes().add(woa);
			}
		}
		
		ObservationConfiguration om = new ObservationConfiguration(wo, typeMapping);
		dataConfig.addMapping(om);
		tblObservations.refresh();
		
		for (int i = 0; i <attData.length; i +=2) {
			ComboViewer cv = (ComboViewer) attData[i];
			IAttributeField<?> field = (IAttributeField<?>) attData[i+1];
			
			field.clear();
			cv.setSelection(new StructuredSelection(ObservationConfiguration.Type.RANDOM));
		}
		saveConfig();
	}
	
	private void updateAttributePanel(Composite attributePanel, Category category, List<Attribute> attributes) {
		attributePanel.setRedraw(false);
		attributePanel.setVisible(false);
		
		attributePanel.setData(null);
		Object[] data = new Object[attributes.size() * 2];
		int counter = 0;
		try {
			attributePanel.setLayout(new GridLayout(3, false));
			
			for (Attribute attribute : attributes) {
				int index = attributePanel.getChildren().length;
				ComboViewer v = createObsTypeComboViewer(attributePanel, attribute.getType());
				IAttributeField<?> field = AttributeFieldFactory.findAttributeField(attribute);
				field.createComposite(attributePanel);
				v.getControl().addListener(SWT.Dispose, e -> field.dispose());
				
				data[counter] = v;
				data[counter+1] = field;
				counter += 2;
				
				//hack to move the combo viewer after the attribute name
				toolkit.adapt(attributePanel.getChildren()[index+1], false, false);
				v.getControl().moveBelow(attributePanel.getChildren()[index + 1]);
				field.setEnabled(false);
				v.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						Object x = v.getStructuredSelection().getFirstElement();
						field.setEnabled(x == ObservationConfiguration.Type.FIXED);
					}
				});
			}
			attributePanel.setData(data);
			attributePanel.layout(true);
			((ScrolledComposite)attributePanel.getParent()).setMinSize(attributePanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));			
		}finally {
			attributePanel.setRedraw(true);
			attributePanel.setVisible(true);
		}
	}
	
	private ComboViewer createObsTypeComboViewer(Composite parent, Attribute.AttributeType aType) {
		ComboViewer v = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		v.setContentProvider(ArrayContentProvider.getInstance());
		v.setLabelProvider(typeLabelProvider);
		v.setInput(ObservationConfiguration.Type.values());
		
		switch(aType) {
		case BOOLEAN:
		case LIST:
		case MLIST:
		case TREE:
			v.setSelection(new StructuredSelection(ObservationConfiguration.Type.RANDOM));	
			break;
		case DATE:
		case NUMERIC:
		case TEXT:
			v.setSelection(new StructuredSelection(ObservationConfiguration.Type.EMPTY));
			break;		
		}
		
		return v;
	}
	
	
	
	@SuppressWarnings("unchecked")
	private void initDataGeneratorConfiguration(SurveyConfiguration config) {
		isInitializing = true;
		
		SurveyDesign init = null;
		try(Session session = HibernateManager.openSession()){
			List<SurveyDesign> designs = QueryFactory.buildQuery(session, SurveyDesign.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
			for (SurveyDesign d : designs) {
				if (d.getKeyId().equalsIgnoreCase(config.getSurveyDesignKey())) {
					init = d;
					break;
				}
			}
			cmbDesigns.setInput(designs);
		}
		
		
		try {
			this.dataConfig = config;
			if (init != null) {
				cmbDesigns.setSelection(new StructuredSelection(init));
			}else {
				cmbDesigns.setSelection(new StructuredSelection());
			}
			
			txtNumSurvey.setText(String.valueOf(dataConfig.getNumberOfSurveys()));
			txtNumMissionsPerSurvey.setText(String.valueOf( dataConfig.getNumberOfMissionsPerSurvey()));
	
			txtMissionDaysMin.setText(String.valueOf(dataConfig.getDaysPerMissionMin()));
			txtMissionDaysMax.setText(String.valueOf(dataConfig.getDaysPerMissionMax()));
			
			txtMissionEmployeesMin.setText(String.valueOf(dataConfig.getEmployeesPerMissionMin()));
			txtMissionEmployeesMax.setText(String.valueOf(dataConfig.getEmployeesPerMissionMax()));
	
			txtMissionWaypointMin.setText(String.valueOf(dataConfig.getWaypointsPerDayMin()));
			txtMissionWaypointMax.setText(String.valueOf(dataConfig.getWaypointsPerDayMax()));
	
			txtMissionObservationsMin.setText(String.valueOf(dataConfig.getObservationsPerWaypointMin()));
			txtMissionObservationsMax.setText(String.valueOf(dataConfig.getObservationsPerWaypointMax()));
	
			tblObservations.setInput(dataConfig.getMappings());
			
			initDateDateTimeWidget(dtStart, config.getStartDate());
			initDateDateTimeWidget(dtEnd, config.getEndDate());
			
			tblObservations.refresh();
			
			if (config.getBboxArea() == null && config.getBboxEnvelope() != null) {
				cmbBoundingBox.setSelection(new StructuredSelection(USER_BBOX));
				StringBuilder sb = new StringBuilder();
				sb.append(config.getBboxEnvelope().getMinX());
				sb.append(","); //$NON-NLS-1$
				sb.append(config.getBboxEnvelope().getMinY());
				sb.append(","); //$NON-NLS-1$
				sb.append(config.getBboxEnvelope().getMaxX());
				sb.append(","); //$NON-NLS-1$
				sb.append(config.getBboxEnvelope().getMaxY());
				txtBoundingBox.setText(sb.toString());
				txtBoundingBox.setEnabled(true);
			}else {
				if ( ((List<Object>)cmbBoundingBox.getInput()).contains(config.getBboxArea()) ) {
					cmbBoundingBox.setSelection(new StructuredSelection(config.getBboxArea()));
					txtBoundingBox.setText(""); //$NON-NLS-1$
					txtBoundingBox.setEnabled(false);
				}else {
					cmbBoundingBox.setSelection(new StructuredSelection(USER_BBOX));
					txtBoundingBox.setText("0,0,1,1"); //$NON-NLS-1$
					txtBoundingBox.setEnabled(true);
				}
			}
		
		}finally {
			isInitializing = false;
		}
	}

	@Focus
	public void setFocus() {
		txtNumSurvey.setFocus();
	}
	
	private void importConfig() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[] {Messages.DataGeneratorView_XmlFile, Messages.DataGeneratorView_AllFile});
		String file = fd.open();
		if (file == null) return;
		Path p = Paths.get(file);
		if (!Files.exists(p)) {
			if (!MessageDialog.openConfirm(shell, Messages.DataGeneratorView_ImportTitle, MessageFormat.format(Messages.DataGeneratorView_FileNotFound, p.toString()))) return;
		}
		
		SurveyConfiguration config = null;
		try(Session session = HibernateManager.openSession()){
			config = XmlManager.INSTANCE.readXmlFile(p, session);
		}catch (Exception ex) {
			ErDataGeneratorPlugIn.displayLog(MessageFormat.format(Messages.DataGeneratorView_ReadError, ex.getMessage()), ex);
			return;
		}
		
		initDataGeneratorConfiguration(config);
	}
	
	private void exportConfig() {
		FileDialog fd = new FileDialog(shell, SWT.SAVE);
		fd.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[] {Messages.DataGeneratorView_XmlFile, Messages.DataGeneratorView_AllFile});
		fd.setFileName("demo_data_config.xml"); //$NON-NLS-1$
		
		String file = fd.open();
		if (file == null) return;
		
		Path p = Paths.get(file);
		if (Files.exists(p)) {
			if (!MessageDialog.openConfirm(shell, Messages.DataGeneratorView_OverwriteTitle, MessageFormat.format(Messages.DataGeneratorView_OverwriteMsg, p.toString()))) return;
		}
		
		try {
			XmlManager.INSTANCE.writeXmlFile(p, dataConfig);
		} catch (Exception e) {
			ErDataGeneratorPlugIn.displayLog(MessageFormat.format(Messages.DataGeneratorView_ExportError, e.getMessage()),e);
			return;
		}
		MessageDialog.openInformation(shell, Messages.DataGeneratorView_CompleteTitle, MessageFormat.format(Messages.DataGeneratorView_ExportComplete, p.toString()));
		
	}

	private boolean validateUser() {
		UserNamePasswordDialog userDialog = new UserNamePasswordDialog(shell,Messages.DataGeneratorView_UserNameTitle,Messages.DataGeneratorView_UserNameMsg,IDialogConstants.OK_LABEL);
		if (userDialog.open() == Window.CANCEL){
			return false;
		}
		
		if (!(userDialog.getUserName().equalsIgnoreCase(SmartDB.getCurrentEmployee().getSmartUserId())
				&& HibernateManager.validatePassword(userDialog.getPassword(), SmartDB.getCurrentEmployee()))){
			MessageDialog.openError(shell, Messages.DataGeneratorView_ErrorTitle, Messages.DataGeneratorView_InvalidUsername);
			return false;
		}
		return true;
	}
	
	void doSpatialShift() {
		MissionSpatialShiftEngine engine = new MissionSpatialShiftEngine(spatialShiftPanel.getCurrentCenter(), spatialShiftPanel.getNewCenter(), spatialShiftPanel.getScale());
		ContextInjectionFactory.inject(engine, context);
		executeTask(engine);
		spatialShiftPanel.refreshBounds();
	}
	void doTimeShift() {
		int days = 0;
		try {
			days = Integer.valueOf(timeShiftPanel.getDays());
		}catch (Exception ex) {
			ErDataGeneratorPlugIn.displayLog(ex.getMessage(), ex);
		}
		
		MissionTimeShiftEngine engine = new MissionTimeShiftEngine(days);
		ContextInjectionFactory.inject(engine, context);
		executeTask(engine);
		timeShiftPanel.refreshDates();
	}
	
	private void doGenerateData() {
		executeTask(new ErDataGenerator(this.dataConfig));
	}
	
	private void executeTask(IDataEngine engine) {
		if (!validateUser()) return;
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		try {
			dialog.run(true, true, new DataEngineRunnable(engine));
		} catch (Exception e) {
			ErDataGeneratorPlugIn.log(e.getMessage(), e);
		}
	}
	
	private static void initDateDateTimeWidget(DateTime dtWidget, LocalDate date){
		dtWidget.setDate(date.getYear(), date.getMonthValue()-1, date.getDayOfMonth());
	}
	
	public static class ErDataGeneratorViewWrapper extends DIViewPart<ErDataGeneratorView> {
		public ErDataGeneratorViewWrapper() {
			super(ErDataGeneratorView.class);
		}
	}
	
	Path loadShapefile() {
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
		fd.setFilterExtensions(new String[] {"*.shp", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFilterNames(new String[] {Messages.SpatialShiftComposite_Shapefile, Messages.SpatialShiftComposite_allFiles});
		String path = fd.open();
		if (path == null) return null;
		
		Path sfile = Paths.get(path);
		if (!Files.exists(sfile)) {
			MessageDialog.openInformation(shell, Messages.SpatialShiftComposite_notFoundTitle, MessageFormat.format(Messages.SpatialShiftComposite_NotFoundMsg, sfile.toString()));
			return null;
		}
		return sfile;
	}
	
	
	private Job initView = new Job("initialize data") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			timeShiftPanel.refreshDates();
			List<Category> roots = new ArrayList<>();
			
			SurveyConfiguration dataConfig = new SurveyConfiguration();
			List<Object> bboxItems = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				
				//@SuppressWarnings("unused")
				//Tuple data = s.createQuery("SELECT min(startDate), max(endDate) FROM Mission WHERE survey.surveyDesign.conservationArea = :ca", Tuple.class) //$NON-NLS-1$
				//	.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				//	.uniqueResult();
				
				CriteriaBuilder cb = s.getCriteriaBuilder();
				CriteriaQuery<Category> c = cb.createQuery(Category.class);
				Root<Category> root = c.from(Category.class);
				c.where(cb.and(
						cb.equal(root.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
						cb.isNull(root.get("parent")) //$NON-NLS-1$
						));
				c.orderBy(cb.asc(root.get("categoryOrder"))); //$NON-NLS-1$
				roots.addAll(s.createQuery(c).getResultList());
				
				//lazy load all children
				ArrayDeque< Category> items = new ArrayDeque<>(roots);
				while(!items.isEmpty()) {
					Category item = items.removeFirst();
					item.getFullCategoryName();
					if (item.getActiveChildren() != null) {
						items.addAll(item.getActiveChildren());
					}
				}
				
				
				if (workingFile != null && Files.exists(workingFile)) {
					try {
						dataConfig = XmlManager.INSTANCE.readXmlFile(workingFile, s);
					}catch (Exception ex){
						ErDataGeneratorPlugIn.log(ex.getMessage(), ex);
					}
				}
				
				List<Area.AreaType> types = s.createQuery("SELECT distinct type FROM Area WHERE conservationArea = :ca", Area.AreaType.class) //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.list();
				bboxItems.addAll(types);
			}
			
			
			bboxItems.add(USER_BBOX);
			bboxItems.add(SHAPEFILE_BBOX);
			final SurveyConfiguration thisDataConfig = dataConfig;
			ui.asyncExec(()->{
				newCategoryViewer.setInput(roots);
				
				cmbBoundingBox.setInput(bboxItems);
				cmbBoundingBox.setSelection(new StructuredSelection(bboxItems.get(0)));
				
				initDataGeneratorConfiguration(thisDataConfig);
				
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	Job saveJob = new Job("saving to filestore") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (dataConfig != null && !isInitializing) {
				try {
					XmlManager.INSTANCE.writeXmlFile(workingFile, dataConfig);
				} catch (Exception ex) {
					ErDataGeneratorPlugIn.log(ex.getMessage(), ex);
				}
			}
			return Status.OK_STATUS;
		}
		
	};
}