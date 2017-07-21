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

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
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
import org.locationtech.udig.project.ui.internal.MapPart;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.internal.Messages;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.map.FeatureFactory;
import org.wcs.smart.qa.model.map.QaErrorService;
import org.wcs.smart.udig.AddContentFilterLayersCommand;
import org.wcs.smart.udig.ContentFilterLayerImpl;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.BBoxInfoTool;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;

public class TableMapQaErrorComposite extends SmartMapEditorPart{

	public enum ResultTableColumn{
		STATUS(Messages.TableMapQaErrorComposite_StatusColumName),
		DATA_TYPE(Messages.TableMapQaErrorComposite_DataTypeColumnName),
		ROUTINE(Messages.TableMapQaErrorComposite_RoutineColumnName),
		OBJECT_ID(Messages.TableMapQaErrorComposite_SrcColumnName),
		DESC(Messages.TableMapQaErrorComposite_DescriptionColumName),
		FIX(Messages.TableMapQaErrorComposite_FixColumnName),
		DATE(Messages.TableMapQaErrorComposite_DateColumnName);
		
		public String title;
		
		ResultTableColumn(String title){
			this.title = title;
		}
		
		public int getWidth(){
			if (this == STATUS ) return 50;
			if (this == OBJECT_ID) return 350;
			return 150;
		}
		
		public String getLabel(Object element){
			if (!(element instanceof QaError)) return element.toString();
			QaError error = (QaError)element;
			if (this == DATA_TYPE){
				return error.getDataProvider().getName(Locale.getDefault());
			}else if (this == ROUTINE){
				return error.getQaRoutine().getName();
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
				if (s1 == null) s1 = ""; //$NON-NLS-1$
				if (s2 == null) s2 = ""; //$NON-NLS-1$
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
	protected Label lblSelectedCnt;
	protected ToolItem btnIncludeFixed;
	
	private Font boldFont;
	private FormToolkit toolkit = null;
	
	private ResultTableColumn sortColumn;
	private int sortDirection = 1;
	private boolean sortOnSelection = false;
	
	private ViewerComparator sorter = new ViewerComparator(){
	  @Override
	    public int compare(Viewer viewer, Object o1, Object o2) {
		  if (sortOnSelection){
			  boolean a1 = false;
			  boolean a2 = false;
			  for (Iterator<?> iterator = ((IStructuredSelection)tblResults.getSelection()).iterator(); iterator.hasNext();) {
					Object x = iterator.next();
					if (x.equals(o1)) a1 = true;
					if (x.equals(o2)) a2 = true;
					if (a1 && a2) break;
			  }
			  if (a1 && a2) return 0;
			  return -1* Boolean.compare(a1, a2);
		  }
		  if (sortColumn == null) return 0;
		  if (o1 instanceof QaError && o2 instanceof QaError){
			  return sortDirection * sortColumn.compare((QaError)o1, (QaError)o2);
		  }
		  return 0;
	  }
	};

	private Composite detailsComposite;
	private Listener detailsSizeListener;
	
	public TableMapQaErrorComposite() {
		super();
	}

	protected void createHeaderToolbar(Composite parent){} 
	
	/**
	 * Saves the updates to the provided items
	 * @param items
	 */
	public void saveErrorItems(List<QaError> items){} 

	public List<Layer> getQaMapLayers(){
		return this.errorLayers;
	}
	
	private void updateErrorDetails(){
		for (Control c : detailsComposite.getChildren()){
			c.dispose();
		}
		if (detailsSizeListener != null){
			detailsComposite.removeListener(SWT.Resize, detailsSizeListener);
			detailsSizeListener = null;
		}
		
		if (tblResults.getSelection().isEmpty()) return;
		QaError r = (QaError) ((IStructuredSelection)tblResults.getSelection()).getFirstElement();
		if (r == null) return;
		
		detailsComposite.setLayout(new GridLayout());
		
		IQaAction gotoSource = null;
		for (IQaAction action : InternalExtensionManager.INSTANCE.getQaActions(r.getDataProvider(), getContext())){
			if (action.getId().equals(IQaAction.GOTO_ACTION_ID)){
				gotoSource = action;
				break;
			}
		}
		if (gotoSource != null){
			Hyperlink link = toolkit.createHyperlink(detailsComposite, r.getErrorId(), SWT.WRAP);
			link.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			link.setFont(boldFont);
			final IQaAction fgotoSource = gotoSource;
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					fgotoSource.doAction(Collections.singletonList(r));						
				}
			});
		} else {
			Label l = toolkit.createLabel(detailsComposite, r.getErrorId(), SWT.WRAP);
			l.setFont(boldFont);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		ScrolledComposite scroll = new ScrolledComposite(detailsComposite, SWT.V_SCROLL);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scroll);
		
		Composite textArea = toolkit.createComposite(scroll, SWT.NONE);
		scroll.setContent(textArea);
		textArea.setLayout(new GridLayout());
		((GridLayout)textArea.getLayout()).marginWidth = 0;
		((GridLayout)textArea.getLayout()).marginHeight= 0;
		textArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		List<String> labels = new ArrayList<>();
		labels.add(Messages.TableMapQaErrorComposite_StatusLabel + r.getStatus().getGuiName(Locale.getDefault()));
		if (r.getFixMessage() != null && !r.getFixMessage().isEmpty()){
			labels.add("\n" + Messages.TableMapQaErrorComposite_FixLbl + "\n" + r.getFixMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (r.getErrorDescription() != null && !r.getErrorDescription().isEmpty()){
			labels.add("\n" + Messages.TableMapQaErrorComposite_DescriptionLbl + "\n" + r.getErrorDescription()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		labels.add("\n" + Messages.TableMapQaErrorComposite_RoutineLbl + "\n" + r.getQaRoutine().getName()); //$NON-NLS-1$ //$NON-NLS-2$
		labels.add("\n" + Messages.TableMapQaErrorComposite_RoutineTypeLbl + "\n" + r.getQaRoutine().getRoutineType().getName(Locale.getDefault())); //$NON-NLS-1$ //$NON-NLS-2$
		labels.add("\n" + Messages.TableMapQaErrorComposite_DataProviderLbl + "\n" + r.getDataProvider().getName(Locale.getDefault())); //$NON-NLS-1$ //$NON-NLS-2$
		labels.add("\n" + Messages.TableMapQaErrorComposite_DateLbl + "\n" + DateFormat.getDateInstance().format(r.getValidateDate())); //$NON-NLS-1$ //$NON-NLS-2$
		
		for (String label : labels){
			Label l = toolkit.createLabel(textArea, label, SWT.WRAP);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
				
		detailsSizeListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (scroll.isDisposed()) return;
				int width = detailsComposite.getSize().x - scroll.getVerticalBar().getSize().x - 20;
				textArea.setSize(textArea.computeSize(width, SWT.DEFAULT));
				scroll.setMinSize(textArea.computeSize(width, SWT.DEFAULT));
			}
		};
		detailsComposite.addListener(SWT.Resize, detailsSizeListener);
		detailsComposite.getParent().layout(true, true);
		
		int width = detailsComposite.getSize().x - scroll.getVerticalBar().getSize().x - 20;
		textArea.setSize(textArea.computeSize(width, SWT.DEFAULT));
		scroll.setMinSize(textArea.computeSize(width, SWT.DEFAULT));		
	}
	
	/*
	 * order table input by selection rather than column;
	 * this is a one time thing, not maintained as selection
	 * changes
	 */
	private void orderBySelection(){
		tblResults.getTable().setSortColumn(null);
		sortOnSelection = true;
		tblResults.setComparator(sorter);
		tblResults.refresh();
		tblResults.getTable().showSelection();
		sortOnSelection = false;
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit =  new FormToolkit(parent.getDisplay());
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
		
		SashForm sash = new SashForm(parent,  SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FontData fd = sash.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(sash.getDisplay(), fd);
		sash.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite main =toolkit.createComposite(sash);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite topComp = toolkit.createComposite(main);
		topComp.setLayout(new GridLayout(4, false));
		((GridLayout)topComp.getLayout()).marginWidth = 0;
		((GridLayout)topComp.getLayout()).marginHeight = 0;
		topComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblResultCnt = toolkit.createLabel(topComp, ""); //$NON-NLS-1$
		lblResultCnt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		lblSelectedCnt = toolkit.createLabel(topComp, ""); //$NON-NLS-1$
		lblSelectedCnt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		ToolBar tb = new ToolBar(topComp, SWT.NONE);
		
		btnIncludeFixed = new ToolItem(tb,  SWT.CHECK);
		btnIncludeFixed.setSelection(false);
		btnIncludeFixed.addListener(SWT.Selection, e->{
			updateResultsTableFilter();
		});
		btnIncludeFixed.setToolTipText(Messages.TableMapQaErrorComposite_filterTooltip);
		btnIncludeFixed.setImage(QaPlugIn.getDefault().getImageRegistry().get(QaPlugIn.ICON_FILTER));
		
		ToolItem btnOrderSelection = new ToolItem(tb,  SWT.PUSH);
		btnOrderSelection.setSelection(false);
		btnOrderSelection.addListener(SWT.Selection, e->{
			orderBySelection();
		});
		btnOrderSelection.setToolTipText(Messages.TableMapQaErrorComposite_sortOnSelection);
		btnOrderSelection.setImage(QaPlugIn.getDefault().getImageRegistry().get(QaPlugIn.ICON_SORT));
		
		createHeaderToolbar(topComp);
		
		SashForm tableArea = new SashForm(main, SWT.HORIZONTAL);
		tableArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblResults = new TableViewer(tableArea, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
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
					btnOrderSelection.setSelection(false);
					
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
				}
			});
		}
	
//		tblResults.getTable().addListener(SWT.Selection, e->{
		tblResults.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				int cnt = ((IStructuredSelection)tblResults.getSelection()).size();
				lblSelectedCnt.setText(MessageFormat.format(Messages.TableMapQaErrorComposite_selectedLabel, cnt));
				lblSelectedCnt.getParent().layout(true, true);
				
				if (errorLayers == null) return;
				FilterFactory ff = CommonFactoryFinder.getFilterFactory();
				
				List<Filter> selectedFeatures = new ArrayList<>();
				for (Iterator<?> iterator = ((IStructuredSelection)tblResults.getSelection()).iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();				
					if (x instanceof QaError){
						selectedFeatures.add(ff.equals(ff.property("fid"), ff.literal(UuidUtils.uuidToString(((QaError) x).getUuid())))); //$NON-NLS-1$
					}
					
				}
				Filter selection = ff.or(selectedFeatures);
				for (Layer l : errorLayers){
					l.setFilter(Filter.EXCLUDE);
					l.setFilter(selection);
					l.refresh(null);
				}
			}
		});
		
		createTableMenu();
		
		detailsComposite = toolkit.createComposite(tableArea, SWT.BORDER);
		detailsComposite.setLayout(new GridLayout());
		detailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableArea.setWeights(new int[]{70,30});
		
		tblResults.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateErrorDetails();
			}
		});
		
		String[] tools = Arrays.copyOf(MapToolComposite.DEFAULT_MAP_TOOLS, MapToolComposite.DEFAULT_MAP_TOOLS.length + 1);
		for (int i = 0; i < tools.length; i ++){
			if (BBoxInfoTool.ID.equals(tools[i])){
				tools[i] = QaFixTool.ID;
			}
		}
		tools[tools.length - 1] = ClearSelectionTool.ID;
		mapTools = tools;
		
		Composite mapArea = toolkit.createComposite(sash);
		mapArea.setLayout(new GridLayout());	
		super.createPartControl(mapArea);
		getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, new QaMapInfoToolProvider(this));
		
		updateResultsTableFilter();
		
		sash.setWeights(new int[]{3,5});
        addLayers();
        
        
        Menu mapMenu = new Menu(mapViewer.getControl());
        new QaActionMenu(mapMenu, parentContext, tblResults){
			@Override
			public void refresh(List<QaError> errors) {
				if (errors != null) saveErrorItems(errors);
				refreshResults();
			}
			
			@Override
			public void menuShown(MenuEvent e) {
				if (!(ApplicationGIS.getToolManager().getActiveTool() instanceof QaFixTool)){
					for(MenuItem i : mapMenu.getItems()){
						i.dispose();
					}
					return;
				}
				Object x = getMap().getBlackboard().get(QaFixTool.HOVER_ID);
				ISelectionProvider lastProvider = selectionProvider;
				if (x != null && x instanceof QaError){
					final StructuredSelection singleSelection = new StructuredSelection(x);
					selectionProvider = new ISelectionProvider() {
						
						@Override
						public void setSelection(ISelection selection) {}
						
						@Override
						public void removeSelectionChangedListener(ISelectionChangedListener listener) {}
						
						@Override
						public ISelection getSelection() {
							return  singleSelection;
						}
						
						@Override
						public void addSelectionChangedListener(ISelectionChangedListener listener) { }
					};
				}
				super.menuShown(e);
				if( ((IStructuredSelection)selectionProvider.getSelection()).size() == 1){
					//show in table options
					MenuItem miTest = new MenuItem(mapMenu, SWT.NONE,0);
					miTest.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
					miTest.setText(Messages.QaMapInfoToolProvider_ShowInTableLabel);
					QaError element = (QaError)((IStructuredSelection)selectionProvider.getSelection()).getFirstElement();
					miTest.addListener(SWT.Selection, evt->{
						setSelection(element);
					});
					newItems.add(miTest);
				}
				selectionProvider = lastProvider;
			}
		};
		
		mapViewer.getControl().setMenu(mapMenu);
	}
	
	public Collection<QaError> getResults(){
		return this.results;
	}
	
	public void clearResults(){
		//clear table
		results = null;
		tblResults.setInput(null);
		tblResults.getTable().setEnabled(false);
		lblResultCnt.setText(""); //$NON-NLS-1$
		
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
		lblResultCnt.setText(MessageFormat.format(Messages.TableMapQaErrorComposite_CountLabel, results.size()));
		lblResultCnt.getParent().layout(true, true);
		
		try{
			disposeService();		
			service = new QaErrorService(results, Locale.getDefault());
			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			toAdd.addAll(service.resources(new NullProgressMonitor()));
			AddContentFilterLayersCommand cmd = new AddContentFilterLayersCommand(toAdd);
			getMap().sendCommandSync(cmd);
			List<Layer> newLayers = cmd.getLayers();
			for (Layer l : newLayers){
				l.setName(Messages.TableMapQaErrorComposite_ResultsLabel);
				if (l instanceof ContentFilterLayerImpl){
					((ContentFilterLayerImpl)l).setContentFilter(FeatureFactory.newStatusFilter());
				}
			}
			errorLayers.addAll(newLayers);
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
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
		zoomTo.setText(Messages.TableMapQaErrorComposite_ZoomToLabel);
		zoomTo.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE));
		zoomTo.addListener(SWT.Selection, e->zoomSelected());
		
		MenuItem clearSelection = new MenuItem(mnu, SWT.PUSH);
		clearSelection.setText(Messages.TableMapQaErrorComposite_ClearSelectionLbl);
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
	
	/**
	 * Sets the table/map selection and displays the item in the table
	 * @param error
	 */
	public void setSelection(QaError error){
		clearSelection();
		tblResults.setSelection(new StructuredSelection(error));
		tblResults.getTable().showSelection();
	}
	
	/**
	 * Sets the table/map selection and displays the items in the table 
	 * @param errors
	 */
	public void setSelection(List<QaError> errors){
		clearSelection();
		tblResults.setSelection(new StructuredSelection(errors));
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
		//reset tool
		ApplicationGIS.getToolManager().setCurrentEditor(this);
		if (tools != null) tools.selectLastTool();
		//set to correct page
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart() instanceof MapPart){
			ApplicationGIS.getToolManager().setCurrentEditor((MapPart)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart());	
		}
		tblResults.refresh();
		clearSelection();
		updateResultsTableFilter();
	}
	
	@Override
	public EditorPart getParentEditor() {
		return this;
	}
}
