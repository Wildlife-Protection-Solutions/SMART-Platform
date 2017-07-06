package org.wcs.smart.qa.ui.view;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
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
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.map.FeatureFactory;
import org.wcs.smart.qa.model.map.QaErrorService;
import org.wcs.smart.udig.AddContentFilterLayersCommand;
import org.wcs.smart.udig.ContentFilterLayerImpl;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;

public class TableMapQaErrorComposite extends SmartMapEditorPart{

	public enum ResultTableColumn{
		STATUS("Status"),
		DATA_TYPE("Data Type"),
		ROUTINE("Routine"),
		OBJECT_ID("Source ID"),
		DESC("Description"),
		FIX("Fix"),
		DATE("Date Validated");
		
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
			}else if (this == DATE){
				return DateFormat.getDateInstance().format(error.getValidateDate());
			}
			return element.toString();
		}
		
		public int compare(QaError object1, QaError object2){
			if (this != DATE ){
				String s1 = getLabel(object1);
				String s2 = getLabel(object2);
				if (s1 == null) s1 = "";
				if (s2 == null) s2 = "";
				return Collator.getInstance().compare(s1, s2);
			}else{
				return object1.getValidateDate().compareTo(object2.getValidateDate());
			}
		}
	};
	
	protected ResultTableColumn[] tableColumns = ResultTableColumn.values();
	
	protected IEclipseContext parentContext;
	
	protected LoadDefaultLayersJob loadDefaultLayers;
	
	protected Collection<QaError> results = null;
	protected List<Layer> errorLayers  = new ArrayList<>();
	protected QaErrorService service = null;
	

	protected TableViewer tblResults;
	protected Label lblResultCnt;
	protected Button btnIncludeFixed ;

	private FormToolkit toolkit = null;
	
	private ResultTableColumn sortColumn;
	private int sortDirection = 1;
	private ViewerComparator sorter = new ViewerComparator(){
	  @Override
	    public int compare(Viewer viewer, Object o1, Object o2) {
		  if (sortColumn == null) return 0;
		  if (o1 instanceof QaError && o2 instanceof QaError){
			  return sortDirection * sortColumn.compare((QaError)o1, (QaError)o2);
		  }
		  return 0;
	  }
	};
	
	public TableMapQaErrorComposite() {
		super();
	}

	protected void createHeaderToolbar(Composite parent){} 
	
	/**
	 * Saves the updates to the provided items
	 * @param items
	 */
	public void saveErrorItems(List<QaError> items){} 

	
	@Override
	public void createPartControl(Composite parent) {
		toolkit =  new FormToolkit(parent.getDisplay());
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
		
		SashForm sash = new SashForm(parent,  SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite main =toolkit.createComposite(sash,  SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite topComp = toolkit.createComposite(main);
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
		
		createHeaderToolbar(topComp);
		
		tblResults = new TableViewer(main, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		toolkit.adapt(tblResults.getTable());
		tblResults.getTable().setLinesVisible(true);
		tblResults.getTable().setHeaderVisible(true);
		tblResults.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setEnabled(false);
		
		for (ResultTableColumn c : tableColumns){
			TableViewerColumn column = new TableViewerColumn(tblResults, SWT.NONE);
			column.getColumn().setText(c.title);
			column.getColumn().setWidth(c.getWidth());
			column.setLabelProvider(new ColumnLabelProvider(){
				public String getText(Object element){
					return c.getLabel(element);
				}
			});
			
			column.getColumn().addSelectionListener(new SelectionListener() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					// TODO Auto-generated method stub
					if (sortColumn == c)
						sortDirection = sortDirection * -1;
					else
						sortColumn = c;
					tblResults.getTable().setSortDirection(sortDirection == 1 ? SWT.UP : SWT.DOWN);
					tblResults.getTable().setSortColumn(column.getColumn());
					tblResults.setComparator(sorter);
					tblResults.refresh();
				}
				
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub
					
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
		
		updateResultsTableFilter();
		
		sash.setWeights(new int[]{3,5});
        addLayers();
	}
	
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
			List<Layer> toDelete = new ArrayList<>();
			List<Layer> iterator = new ArrayList<>();
			iterator.addAll(errorLayers);
			
			for (Layer l : iterator){
				if (l.getMap() != null){
					toDelete.add(l);
				}else{
					errorLayers.remove(l);
				}
			}
			if (!toDelete.isEmpty()){
				DeleteLayersCommand cmd = new DeleteLayersCommand(toDelete.toArray(new ILayer[toDelete.size()])){
					public void run( IProgressMonitor monitor ) throws Exception {
						super.run(monitor);
						errorLayers.removeAll(toDelete);
					}	
				};
				getMap().sendCommandSync(cmd);
			}
			for (Layer l : iterator){
				l.getGeoResource().dispose(new NullProgressMonitor());
			}
			
		}
	}
	
	@Override
	public void dispose() {
		JobUtil.stopJobs(loadDefaultLayers);
		loadDefaultLayers = null;

		if (toolkit != null) {
			toolkit.dispose();
			toolkit = null;
		}
		disposeService();

		super.dispose();

	}
	 
	public IEclipseContext getContext(){
		return this.parentContext;
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
		
		tblResults.setInput(results);
		tblResults.getTable().setEnabled(true);
		lblResultCnt.setText(MessageFormat.format("{0} items found", results.size()));
		lblResultCnt.getParent().layout(true, true);
		
		try{
			disposeService();		
			service = new QaErrorService(results);
			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			toAdd.addAll(service.resources(new NullProgressMonitor()));
			AddContentFilterLayersCommand cmd = new AddContentFilterLayersCommand(toAdd);
			getMap().sendCommandSync(cmd);
			List<Layer> newLayers = cmd.getLayers();
			for (Layer l : newLayers){
				l.setName("Error Results");
				if (l instanceof ContentFilterLayerImpl){
					((ContentFilterLayerImpl)l).setContentFilter(FeatureFactory.newStatusFilter());
				}
			}
			errorLayers.addAll(newLayers);
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
			public void refresh(List<QaError> errors) {
				if (errors != null) saveErrorItems(errors);
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
		clearSelection();
		updateResultsTableFilter();
	}
	
	@Override
	public EditorPart getParentEditor() {
		return this;
	}
}
