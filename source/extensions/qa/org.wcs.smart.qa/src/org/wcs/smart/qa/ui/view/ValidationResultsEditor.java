/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
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
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
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
import org.wcs.smart.qa.model.map.QaErrorService;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.ValidationTask;
import org.wcs.smart.udig.AddContentFilterLayersCommand;
import org.wcs.smart.udig.ContentFilterLayerImpl;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Editor part of displaying the results of manual validation routine.
 * 
 * @author Emily
 *
 */
public class ValidationResultsEditor extends SmartMapEditorPart {
	
	public static final String ID = "org.wcs.smart.qa.data.validatation.manual"; //$NON-NLS-1$

	private TableViewer tblResults;
	private Label lblResultCnt;
	
	private LoadDefaultLayersJob loadDefaultLayers;
	private FormToolkit toolkit = null;
	
	private DateFilterDropDownComposite dateFilter;
	private CheckboxTableViewer tblRoutines;
	
//	private Composite optionsPanel;
//	private Composite resultsPanel;
//	private Composite progressPanel;
	private Composite stackPanel;
	private ProgressAreaComposite progressComposite;
	private IEclipseContext parentContext;
	private Button btnIncludeFixed ;
	
	
	
//	private Hyperlink lOptions;
//	private Hyperlink lResults;
	private Font boldFont, normalFont;
	
	private StackPanelItem progressStackItem;
	private StackPanelItem optionsStackItem;
	private StackPanelItem resultsStackItem;
	
	private Collection<QaError> results = null;
	
	private List<Layer> errorLayers;
	private QaErrorService service = null;
	
	private ValidationEngine lastValidationEngine;
	
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
		DESC("Description"),
		FIX("Fix");
		
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
				return error.getStatus().getGuiName(Locale.getDefault());
			}else if (this == FIX){
				return error.getFixMessage();
			}
			return element.toString();
		}
	};
	    
	public Collection<QaError> getResults(){
		return this.results;
	}

	public IEclipseContext getContext(){
		return this.parentContext;
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
		progressStackItem.show();
	}
	
	
	private void disposeService(){
		if (service != null){
			service.dispose(new NullProgressMonitor());
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			service = null;
		}
	}
	public void setResults(Collection<QaError> results){
		this.results = results;
		
		for (QaError r : results){
			if (r.getUuid() == null){
				r.setUuid(UUID.randomUUID());
			}
		}
		
		tblResults.setInput(results);
		tblResults.getTable().setEnabled(true);
		lblResultCnt.setText(MessageFormat.format("{0} items found", results.size()));
		lblResultCnt.getParent().layout(true, true);
		resultsStackItem.show();
		
		try{
			disposeService();		
			service = new QaErrorService(results);
			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			toAdd.addAll(service.resources(new NullProgressMonitor()));
			AddContentFilterLayersCommand cmd = new AddContentFilterLayersCommand(toAdd);
			getMap().sendCommandSync(cmd);
			errorLayers = cmd.getLayers();
			for (Layer l : errorLayers){
				l.setName("Error Results");
				if (l instanceof ContentFilterLayerImpl){
					((ContentFilterLayerImpl)l).setContentFilter(FeatureFactory.newStatusFilter());
				}
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
		
		Composite header = toolkit.createComposite(form.getBody());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).horizontalSpacing = 10;
		//((GridLayout)header.getLayout()).marginHeight = 8;
		header.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		
		Hyperlink lOptions = toolkit.createHyperlink(header, "Options", SWT.NONE);
		Hyperlink lResults = toolkit.createHyperlink(header, "Results", SWT.NONE);
		Label spacer = toolkit.createLabel(header, "");
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		normalFont = lOptions.getFont();
		FontData fd = normalFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(lOptions.getDisplay(), fd);
		lOptions.addListener(SWT.Dispose, e->boldFont.dispose());
		
		lOptions.setBackground(header.getBackground());
		lResults.setBackground(header.getBackground());
		spacer.setBackground(header.getBackground());
		
		lOptions.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				optionsStackItem.show();
			}
		});
		
		lResults.addHyperlinkListener(new HyperlinkAdapter() {

			@Override
			public void linkActivated(HyperlinkEvent e) {
				resultsStackItem.show();
			}
		});

		
		stackPanel = toolkit.createComposite(form.getBody(), SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		Composite optionsPanel = toolkit.createComposite(stackPanel);
		optionsPanel.setLayout(new GridLayout());
		createParameterArea(optionsPanel);
		
		Composite progressPanel = toolkit.createComposite(stackPanel);
		progressPanel.setLayout(new GridLayout());
		progressComposite = new ProgressAreaComposite(progressPanel);
		progressComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite resultsPanel = toolkit.createComposite(stackPanel);
		resultsPanel.setLayout(new GridLayout());
		
		resultsStackItem = new StackPanelItem(lResults, resultsPanel);
		optionsStackItem = new StackPanelItem(lOptions, optionsPanel);
		progressStackItem = new StackPanelItem(lResults, progressPanel);

		SashForm sash = new SashForm(resultsPanel,  SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite tableArea = toolkit.createComposite(sash, SWT.NONE);
		tableArea.setLayout(new GridLayout());
		((GridLayout)tableArea.getLayout()).marginWidth = 0;
		((GridLayout)tableArea.getLayout()).marginHeight = 0;
		
		Composite topComp = toolkit.createComposite(tableArea);
		topComp.setLayout(new GridLayout(3, false));
		((GridLayout)topComp.getLayout()).marginWidth = 0;
		((GridLayout)topComp.getLayout()).marginHeight = 0;
		topComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblResultCnt = toolkit.createLabel(topComp, "");
		lblResultCnt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIncludeFixed = toolkit.createButton(topComp, "Include Resolved Items", SWT.CHECK);
		btnIncludeFixed.setSelection(false);
		btnIncludeFixed.addListener(SWT.Selection, e->updateResultsTableFilter());
		btnIncludeFixed.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
		ToolBar tb = new ToolBar(topComp,  SWT.NONE);
		ToolItem btnRefresh = new ToolItem(tb, SWT.PUSH);
//		Button btnRefresh = toolkit.createButton(topComp, "", SWT.PUSH);
		btnRefresh.setToolTipText("Re-run qa routines against the same data.");
		btnRefresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		btnRefresh.addListener(SWT.Selection, e->{
			if (lastValidationEngine != null){
				executeValidation(lastValidationEngine);
			}
		});
		
		tblResults = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
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
			if (errorLayers == null) return;
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
		
		optionsStackItem.show();
		updateResultsTableFilter();
		
		sash.setWeights(new int[]{3,5});
        addLayers();
	}

	private void updateResultsTableFilter(){
		if (btnIncludeFixed.getSelection()){
			tblResults.setFilters(new ViewerFilter[]{});
			
			if (errorLayers != null){
				for (Layer l : errorLayers){
					if (l instanceof ContentFilterLayerImpl){
						((ContentFilterLayerImpl)l).setContentFilter(Filter.INCLUDE);
					}
				}
				getMap().getRenderManager().refresh(null);
			}
		}else{
			tblResults.setFilters(new ViewerFilter[]{new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof QaError){
						if (((QaError) element).getStatus() == QaError.Status.NEW) return true;
						return false;
					}
					return true;
				}
			}});
			
			if (errorLayers != null){
				for (Layer l : errorLayers){
					if (l instanceof ContentFilterLayerImpl){
						((ContentFilterLayerImpl)l).setContentFilter(FeatureFactory.newStatusFilter());
					}
				}
				getMap().getRenderManager().refresh(null);
			}
		}
	}
	
	/**
	 * Refreshes the results table and the map
	 */
	public void refreshResults(){
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		tblResults.refresh();
		getMap().getRenderManager().refresh(null);
		clearSelection();
	}
	
	
	private void createTableMenu(){
		Menu mnu = new Menu(tblResults.getTable());
		
		MenuItem zoomTo = new MenuItem(mnu, SWT.PUSH);
		zoomTo.setText("Zoom To");
		zoomTo.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE));
		zoomTo.addListener(SWT.Selection, e->zoomSelected());
		
		MenuItem clearSelection = new MenuItem(mnu, SWT.PUSH);
		clearSelection.setText("Clear Selection");
		clearSelection.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CLEAR_SELECTION_ICON));
		clearSelection.addListener(SWT.Selection, e->clearSelection());
		
		tblResults.getTable().setMenu(mnu);
		new QaActionMenu(mnu, parentContext, tblResults){
			@Override
			public void refresh() {
				refreshResults();
			}
		};
		
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
        disposeService();
        
        super.dispose();
      
    }

	@Override
	public EditorPart getParentEditor() {
		return this;
	}

	
	private void createParameterArea(Composite parent){
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		Button btnExecute = toolkit.createButton(panel, "Validate Data...", SWT.PUSH);
		btnExecute.addListener(SWT.Selection, e->validate());
		
		Composite dFilter = toolkit.createComposite(panel);
		dFilter.setLayout(new GridLayout(2, false));
		dFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)dFilter.getLayout()).marginWidth = 0;
		((GridLayout)dFilter.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(dFilter, "Dates:");
		DateFilter[] dFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.CURRENT_MONTH,
				DateFilter.CUSTOM};
		
		dateFilter = new DateFilterDropDownComposite(dFilter, dFilters, DateFilter.LAST_30_DAYS);
		toolkit.adapt(dateFilter);
		
		Table tbl = toolkit.createTable(panel, SWT.CHECK| SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblRoutines = new CheckboxTableViewer(tbl);//, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.adapt(tblRoutines.getTable());
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance()); 
		tblRoutines.getTable().setLinesVisible(true);
		tblRoutines.getTable().setHeaderVisible(true);
		tblRoutines.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblRoutines.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.SPACE){
					Object selection = ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
					boolean newValue = !tblRoutines.getChecked(selection);
					
					for (Iterator<?>iterator = ((IStructuredSelection)tblRoutines.getSelection()).iterator(); iterator.hasNext();) {
						DataValidator type = (DataValidator) iterator.next();
						tblRoutines.setChecked(type, newValue);
					}
					tblRoutines.refresh();
					e.doit = false;
				}
			}
		});
		
		TableViewerColumn checkColumn = new TableViewerColumn(tblRoutines, SWT.CHECK);
		checkColumn.getColumn().setWidth(30);
		checkColumn.setLabelProvider(new ColumnLabelProvider(){});
		
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
		
		Hyperlink hlink = toolkit.createHyperlink(panel, "refresh list", SWT.NONE);
		hlink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				loadRoutines();
			}
		});
		hlink.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		
		tblRoutines.setInput(DialogConstants.LOADING_TEXT);

		
		loadRoutines();
	}

	private void validate(){
		if (tblRoutines.getCheckedElements().length == 0){
			MessageDialog.openInformation(getSite().getShell(), "No Routines Selected", "No validation routines selected.  Nothing to validate.");
			return;
		}
		
		Date startDate = null;
		Date endDate = null;
		if (dateFilter.getDateFilter() == DateFilter.CUSTOM){
			startDate = dateFilter.getCustomStartDate();
			endDate = dateFilter.getCustomEndDate();
		}else{
			startDate = dateFilter.getDateFilter().getStartDate();
			endDate = dateFilter.getDateFilter().getEndDate();
		}
		
		if (startDate.after(endDate)){
			MessageDialog.openInformation(getSite().getShell(), "Invalid Dates", "Start date is after end date.  Cannot validate data.");
			return;
		}
		
		lastValidationEngine = new ValidationEngine();
		
		
		//TODO: validate this cast
		for (Object x  : tblRoutines.getCheckedElements()){
			if (x instanceof DataValidator){
				ValidationTask task = new ValidationTask(((DataValidator) x).getRoutine(), ((DataValidator) x).getDataProvider(), startDate, endDate, SmartDB.getCurrentConservationArea());
				lastValidationEngine.addValidationTask(task);
			}
		}
		executeValidation(lastValidationEngine);
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
				tblRoutines.setAllChecked(true);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	private class DataValidator{
		
		private QaRoutine routine;
		private IQaDataProvider data;
		
		public DataValidator(QaRoutine routine, IQaDataProvider data){
			this.routine = routine;
			this.data = data;
		}		
		public QaRoutine getRoutine(){ return routine; }
		public IQaDataProvider getDataProvider(){ return data; }
	}
	
	private class StackPanelItem{
		Hyperlink  lblHeader;
		Composite control;
		
		public StackPanelItem(Hyperlink  lblHeader, Composite control){
			this.lblHeader = lblHeader;
			this.control = control;
		}
		
		public void show(){
			for (Control c : lblHeader.getParent().getChildren()){
				c.setFont(normalFont);
			}
			lblHeader.setFont(boldFont);
			if (tblResults.getInput() == null && this == resultsStackItem){
				((StackLayout)stackPanel.getLayout()).topControl = progressStackItem.control;
			}else{
				((StackLayout)stackPanel.getLayout()).topControl = this.control;
			}
			lblHeader.getParent().layout();
			stackPanel.layout();
		}
	}
}