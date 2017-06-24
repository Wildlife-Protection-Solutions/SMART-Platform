package org.wcs.smart.qa.ui.view;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.ProgressAreaComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.ValidationEngine;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.map.FeatureFactory;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class ValidationResultsEditor extends SmartMapEditorPart {
	
	public static final String ID = "org.wcs.smart.qa.data.validatation.manual"; //$NON-NLS-1$

	private TableViewer tblResults;
	private Label lblResultCnt;
	
	private LoadDefaultLayersJob loadDefaultLayers;
	private FormToolkit toolkit = null;
	
	private DateFilterDropDownComposite dateFilter;
	private TableViewer tblRoutines;
	
	private Composite optionsPanel;
	private Composite resultsPanel;
	private Composite progressPanel;
	private Composite stackPanel;
	private ProgressAreaComposite progressComposite;
	private IEclipseContext parentContext;
	
	private List<Layer> errorLayers;
	
	private Hyperlink lOptions;
	private Hyperlink lResults;
	private Font boldFont, normalFont;
	
	private Collection<QaError> results = null;
	
	public static IEditorInput MANUAL_VALIDATION_INPUT =  new IEditorInput() {
		
		@Override
		public Object getAdapter(Class adapter) {
			return null;
		}
		
		@Override
		public String getToolTipText() {
			return null;
		}
		
		@Override
		public IPersistableElement getPersistable() {
			return null;
		}
		
		@Override
		public String getName() {
			return null;
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return null;
		}
		
		@Override
		public boolean exists() {
			return false;
		}
	};

	
	private enum ResultTableColumn{
		STATUS("Status"),
		DATA_TYPE("Data Type"),
		ROUTINE("Routine"),
		OBJECT_ID("Source ID"),
		DESC("Description");
		
		public String title;
		
		ResultTableColumn(String title){
			this.title = title;
		}
		
		public int getWidth(){
			if (this == STATUS ) return 50;
			return 150;
		}
		
		public String getLabel(Object element){
			if (!(element instanceof QaError)) return element.toString();
			QaError error = (QaError)element;
			if (this == DATA_TYPE){
				return error.getDataProvider().getName(Locale.getDefault());
			}else if (this == ROUTINE){
				return error.getQaRoutine().getName() + " (" + error.getQaRoutine().getRoutineType().getName(Locale.getDefault()) + ")";
			}else if (this == OBJECT_ID){
				return error.getErrorId();
			}else if (this == DESC){
				return error.getErrorDescription();
			}else if (this == STATUS){
				return error.getStatus().name();
			}
			return element.toString();
		}
	};
	    
	public Collection<QaError> getResults(){
		return this.results;
	}

	public void clearResults(){
		//clear table
		results = null;
		tblResults.setInput(null);
		tblResults.getTable().setEnabled(false);
		lblResultCnt.setText("");
		
		//clear map layers
		if (errorLayers != null){
			DeleteLayersCommand cmd = new DeleteLayersCommand(errorLayers.toArray(new ILayer[errorLayers.size()]));
			getMap().sendCommandSync(cmd);
			for (Layer l : errorLayers){
				l.getGeoResource().dispose(new NullProgressMonitor());
			}
			
		}
	}
	

//	private void subscribeToEvent(String eventTopic, EventHandler handler){
//		parentContext.get(IEventBroker.class).subscribe(eventTopic, handler);
//		
//	//	handlers.add(handler);
//	}
	
	private void executeValidation(final ValidationEngine engine){
		clearResults();
		showProgress();
		Job j = new Job("validation job"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor = progressComposite.createProgressMonitor();
				Collection<QaError> errors = null;
				Session s = HibernateManager.openSession();
				try{
					errors = engine.validate(s, monitor);
				}finally{
					s.close();
				}
				Collection<QaError> ferrors = errors;
				Display.getDefault().syncExec(()->{
					setResults(ferrors);
				});
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
	}
	
	
	public void showProgress(){
		((StackLayout)stackPanel.getLayout()).topControl = progressPanel;
		stackPanel.layout();
		
		lResults.setFont(normalFont);
		lOptions.setFont(boldFont);
	}
	
	public void setResults(Collection<QaError> results){
		this.results = results;
		
		for (QaError r : results){
			if (r.getUuid() == null){
				r.setUuid(UUID.randomUUID());
			}
		}
		
		((StackLayout)stackPanel.getLayout()).topControl = resultsPanel;
		stackPanel.layout();
		lResults.setFont(boldFont);
		lOptions.setFont(normalFont);
		
		tblResults.setInput(results);
		tblResults.getTable().setEnabled(true);
		lblResultCnt.setText(MessageFormat.format("{0} items found", results.size()));
		

		
		try{
			SimpleFeatureType pntSchema = FeatureFactory.createPointQaErrorFeatureType();
			SimpleFeatureType lineSchema = FeatureFactory.createLineQaErrorFeatureType();
			
			IGeoResource pntErrorLayer = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(pntSchema);
			IGeoResource lineErrorLayer = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(lineSchema);
			
			List<SimpleFeature> pntFeatures = new ArrayList<>();
			List<SimpleFeature> lineFeatures = new ArrayList<>();
			for (QaError error : results ){
				if (error.getGeometryObject() != null){
					
					if (error.getGeometryObject() instanceof Point){
						SimpleFeature sf = FeatureFactory.createQaFeature(pntSchema, error, Locale.getDefault());
						pntFeatures.add(sf);
					}else if (error.getGeometryObject() instanceof LineString){
						SimpleFeature sf = FeatureFactory.createQaFeature(lineSchema, error, Locale.getDefault());
						lineFeatures.add(sf);
					}else if (error.getGeometryObject() instanceof MultiLineString){
						SimpleFeature sf = FeatureFactory.createQaFeature(lineSchema, error, Locale.getDefault());
						lineFeatures.add(sf);
					}
				}
			}
			pntErrorLayer.resolve(FeatureStore.class, new NullProgressMonitor()).addFeatures(DataUtilities.collection(pntFeatures));
			lineErrorLayer.resolve(FeatureStore.class, new NullProgressMonitor()).addFeatures(DataUtilities.collection(lineFeatures));

			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			toAdd.add(pntErrorLayer);
			toAdd.add(lineErrorLayer);
			AddLayersCommand cmd = new AddLayersCommand(toAdd);
			getMap().sendCommandSync(cmd);
			errorLayers = cmd.getLayers();
			for (Layer l : errorLayers){
				l.setName("Error Results");
			}
			
		}catch (Exception ex){
			ex.printStackTrace();
			//TODO:
		}
	}
	
	
	
	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap());
		loadDefaultLayers.schedule();
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	

	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
//		subscribeToEvent("SMART_QA/MANUAL/EXECUTE", executeValidate);
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form form = toolkit.createForm(parent);
		form.setText("Data Validation Results");
		
		form.getBody().setLayout(new GridLayout());
		SashForm sash = new SashForm(form.getBody(),  SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Composite resultsArea = toolkit.createComposite(sash);
		resultsArea.setLayout(new GridLayout());
		((GridLayout)resultsArea.getLayout()).marginWidth = 0;
		((GridLayout)resultsArea.getLayout()).marginHeight = 0;
		
		
		Composite header = toolkit.createComposite(resultsArea);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).horizontalSpacing = 10;
		//((GridLayout)header.getLayout()).marginHeight = 8;
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		
		lOptions = toolkit.createHyperlink(header, "Options", SWT.NONE);
		lResults = toolkit.createHyperlink(header, "Results", SWT.NONE);
		Label spacer = toolkit.createLabel(header, "");
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		normalFont = lOptions.getFont();
		FontData fd = normalFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(lOptions.getDisplay(), fd);
		lOptions.addListener(SWT.Dispose, e->boldFont.dispose());
		
		lOptions.setFont(boldFont);
		lOptions.setBackground(header.getBackground());
		lResults.setBackground(header.getBackground());
		spacer.setBackground(header.getBackground());
		
		lOptions.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				lOptions.setFont(boldFont);
				lResults.setFont(normalFont);
				((StackLayout)stackPanel.getLayout()).topControl = optionsPanel;
				header.layout();
				stackPanel.layout();
			}
		});
		
		
		lResults.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				lResults.setFont(boldFont);
				lOptions.setFont(normalFont);
				if (tblResults.getInput() == null){
					((StackLayout)stackPanel.getLayout()).topControl = progressPanel;
				}else{
					((StackLayout)stackPanel.getLayout()).topControl = resultsPanel;
				}
				header.layout();
				stackPanel.layout();
			}
		});

		
		stackPanel = toolkit.createComposite(resultsArea, SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		stackPanel.setLayout(new StackLayout());
		
		optionsPanel = toolkit.createComposite(stackPanel);
		optionsPanel.setLayout(new GridLayout());
		createParameterArea(optionsPanel);
		
		progressPanel = toolkit.createComposite(stackPanel);
		progressPanel.setLayout(new GridLayout());
		progressComposite = new ProgressAreaComposite(progressPanel);
		progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		((StackLayout)stackPanel.getLayout()).topControl = optionsPanel;
		
		resultsPanel = toolkit.createComposite(stackPanel);
		resultsPanel.setLayout(new GridLayout());
		
		
		lblResultCnt = toolkit.createLabel(resultsPanel, "");
		lblResultCnt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		tblResults = new TableViewer(resultsPanel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		toolkit.adapt(tblResults.getTable());
		tblResults.getTable().setLinesVisible(true);
		tblResults.getTable().setHeaderVisible(true);
		tblResults.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setEnabled(false);
		for (ResultTableColumn c : ResultTableColumn.values()){
			TableViewerColumn column = new TableViewerColumn(tblResults, SWT.NONE);
			column.getColumn().setText(c.title);
			column.getColumn().setWidth(c.getWidth());
			column.setLabelProvider(new ColumnLabelProvider(){
				public String getText(Object element){
					return c.getLabel(element);
				}
			});
		}
		tblResults.getTable().addListener(SWT.Selection, e->{
			FilterFactory ff = CommonFactoryFinder.getFilterFactory();
			
			List<Filter> selectedFeatures = new ArrayList<>();
			for (Iterator<?> iterator = ((IStructuredSelection)tblResults.getSelection()).iterator(); iterator.hasNext();) {
				Object x = (Object) iterator.next();				
				if (x instanceof QaError){
					selectedFeatures.add(ff.equals(ff.property("fid"), ff.literal(UuidUtils.uuidToString(((QaError) x).getUuid()))));
				}
				
			}
			Filter selection = ff.or(selectedFeatures);
			for (Layer l : errorLayers){
				l.setFilter(Filter.EXCLUDE);
				l.setFilter(selection);
				l.refresh(null);
			}
		
		});
		
		createTableMenu();
		
		
		String[] tools = Arrays.copyOf(MapToolComposite.DEFAULT_MAP_TOOLS, MapToolComposite.DEFAULT_MAP_TOOLS.length + 1);
		tools[tools.length - 1] = ClearSelectionTool.ID;
		mapTools = tools;
		
		Composite mapArea = toolkit.createComposite(sash);
		mapArea.setLayout(new GridLayout());
		
		super.createPartControl(mapArea);
	
		getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, new QaMapInfoToolProvider(this));
		
		sash.setWeights(new int[]{3,5});
        addLayers();
	}

	
	private void createTableMenu(){
		Menu mnu = new Menu(tblResults.getTable());
		
		MenuItem zoomTo = new MenuItem(mnu, SWT.PUSH);
		zoomTo.setText("Zoom To");
		zoomTo.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE));
		zoomTo.addListener(SWT.Selection, e->zoomSelected());
		
		MenuItem clearSelection = new MenuItem(mnu, SWT.PUSH);
		clearSelection.setText("Clear Selection");
		clearSelection.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		clearSelection.addListener(SWT.Selection, e->clearSelection());
		
		tblResults.getTable().setMenu(mnu);
	}
	
	
	public void setSelection(QaError error){
		clearSelection();
		tblResults.setSelection(new StructuredSelection(error));
		tblResults.getTable().showSelection();
	}
	
	private void clearSelection(){
		for (Layer l : getMap().getLayersInternal()){
			l.setFilter(Filter.EXCLUDE);
			l.refresh(null);
		}
		tblResults.setSelection(null);
	}
	private void zoomSelected(){
		Envelope env = null;
		for (Iterator<?> iterator = ((IStructuredSelection)tblResults.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();				
			if (x instanceof QaError && ((QaError) x).getGeometryObject() != null){
				if (env == null){
					env = ((QaError)x).getGeometryObject().getEnvelopeInternal();
				}else{
					env.expandToInclude(((QaError)x).getGeometryObject().getEnvelopeInternal());
				}
			}
		}
		if (env != null){
			env.expandBy(0.001);
			
			ReferencedEnvelope re = new ReferencedEnvelope(env, SmartDB.DATABASE_CRS);
			SetViewportBBoxCommand bbox = new SetViewportBBoxCommand(re);
			getMap().sendCommandASync(bbox);
		}
	}
	
    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers);
    	loadDefaultLayers = null;

        if (toolkit != null){
        	toolkit.dispose();
        	toolkit = null;
        }
        
//        parentContext.get(EventBroker.class).unsubscribe(executeValidate);
        super.dispose();
      
    }

	@Override
	public EditorPart getParentEditor() {
		return this;
	}

	
	private void createParameterArea(Composite parent){
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		toolkit.createLabel(panel, "Date Filter:");
		DateFilter[] dFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.CURRENT_MONTH,
				DateFilter.CUSTOM};
		
		dateFilter = new DateFilterDropDownComposite(panel, dFilters, DateFilter.LAST_30_DAYS);
		toolkit.adapt(dateFilter);
		
		toolkit.createLabel(panel, "Data to Validate:");
		Hyperlink hlink = toolkit.createHyperlink(panel, "refresh list", SWT.NONE);
		hlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				loadRoutines();
			}
		});
		hlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		tblRoutines = new TableViewer(panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblRoutines.getTable());
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance()); 
		tblRoutines.getTable().setLinesVisible(true);
		tblRoutines.getTable().setHeaderVisible(true);
		tblRoutines.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		tblRoutines.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.SPACE){
					boolean newSelection =  !((DataValidator)((IStructuredSelection)tblRoutines.getSelection()).getFirstElement()).isSelected;
					for (Iterator<?>iterator = ((IStructuredSelection)tblRoutines.getSelection()).iterator(); iterator.hasNext();) {
						DataValidator type = (DataValidator) iterator.next();
						type.isSelected = newSelection;
						
					}
					tblRoutines.refresh();
				}
			}
		});
		
		TableViewerColumn checkColumn = new TableViewerColumn(tblRoutines, SWT.CHECK);
		checkColumn.getColumn().setWidth(30);
		checkColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					if (((DataValidator) element).isSelected()){
						return "YES"; 
							
//						return Character.toString((char)0x2611);
					}else{
						return "NO";
//						return Character.toString((char)0x2610);
					}
				}
				return "";
			}
		});
		
		checkColumn.setEditingSupport(new EditingSupport(checkColumn.getViewer()) {
			CellEditor editor = new CheckboxCellEditor(tblRoutines.getTable());;
			
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof DataValidator){
					((DataValidator)element).isSelected = (Boolean)value;
					tblRoutines.refresh();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof DataValidator){
					return ((DataValidator) element).isSelected();
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return editor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return element instanceof DataValidator;
			}
		});
		
		TableViewerColumn dataColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		dataColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					return ((DataValidator) element).getDataProvider().getName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		dataColumn.getColumn().setWidth(150);
		dataColumn.getColumn().setText("Data To Validate");
		
		TableViewerColumn routineColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		routineColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					QaRoutine v = ((DataValidator)element).getRoutine();
					return v.getName() + " (" + v.getRoutineType().getName(Locale.getDefault()) + ")";
				}
				return super.getText(element);
			}
		});
		routineColumn.getColumn().setWidth(150);
		routineColumn.getColumn().setText("Routine To Perform");
		
		TableViewerColumn descColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
		descColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof DataValidator){
					QaRoutine v = ((DataValidator)element).getRoutine();
					return v.getDescription();
				}
				return super.getText(element);
			}
		});
		descColumn.getColumn().setWidth(150);
		descColumn.getColumn().setText("Routine Description");
		
		tblRoutines.setInput(DialogConstants.LOADING_TEXT);

		Button btnExecute = toolkit.createButton(panel, "EXECUTE", SWT.PUSH);
		btnExecute.addListener(SWT.Selection, e->validate());
		loadRoutines();
	}

	private void validate(){
		Date startDate = null;
		Date endDate = null;
		if (dateFilter.getDateFilter() == DateFilter.CUSTOM){
			startDate = dateFilter.getCustomStartDate();
			endDate = dateFilter.getCustomEndDate();
		}else{
			startDate = dateFilter.getDateFilter().getStartDate();
			endDate = dateFilter.getDateFilter().getEndDate();
		}
		ValidationEngine engine = new ValidationEngine();
		
		//TODO: validate this cast
		List<Object> items = (List<Object>) tblRoutines.getInput();
		for (Object x  : items){
			if (x instanceof DataValidator && ((DataValidator) x).isSelected()){
				ValidationTask task = new ValidationTask(((DataValidator) x).getRoutine(), ((DataValidator) x).getDataProvider(), startDate, endDate, SmartDB.getCurrentConservationArea());
				engine.addValidationTask(task);
			}
		}
		executeValidation(engine);
//		eventBroker.send("SMART_QA/MANUAL/EXECUTE", engine);
	}
	
	/*
	 * Loads all possible record sources from db and populates 
	 * provided combo
	 * @param cmbSource
	 */
	private void loadRoutines(){
		tblRoutines.setInput(DialogConstants.LOADING_TEXT);
		j.setSystem(true);
		j.schedule();
	}
	
	Job j = new Job("load routines"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<DataValidator> routines = new ArrayList<>();
			Session s = HibernateManager.openSession();
			try{
				List<QaRoutine> dbroutines = s.createCriteria(QaRoutine.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.list();
				
				Collection<IQaDataProvider> providers = RoutineExtensionManager.INSTANCE.getDataProviders();
				for (IQaDataProvider p : providers){
					for (QaRoutine r : dbroutines){
						if (p.supportsRoutine(r.getRoutineType())){
							routines.add(new DataValidator(r, p));
						}
					}
				}
			}finally{
				s.close();
			}
			Display.getDefault().asyncExec(()->{
				tblRoutines.setInput(routines);
			});
			return Status.OK_STATUS;
		}
		
	};
	private class DataValidator{
		private QaRoutine routine;
		private IQaDataProvider data;
		private boolean isSelected;
		
		public DataValidator(QaRoutine routine, IQaDataProvider data){
			this.routine = routine;
			this.data = data;
		}
		
		public QaRoutine getRoutine(){ return routine; }
		public IQaDataProvider getDataProvider(){ return data; }
		public boolean isSelected(){ return this.isSelected;  }
		public void setSelected(boolean isSelected) { this.isSelected = isSelected; }
		
	}
	
	public static class ManualValidationViewWrapper extends DIViewPart<ManualValidationView>{
		public ManualValidationViewWrapper(){
			super(ManualValidationView.class);
		}
	}

}