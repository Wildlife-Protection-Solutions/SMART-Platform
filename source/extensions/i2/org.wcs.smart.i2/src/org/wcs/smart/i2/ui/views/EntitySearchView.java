/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.search.IIntelEntitySearch;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.search.IntelSearchResultItem;
import org.wcs.smart.i2.search.LoadSavedSearches;
import org.wcs.smart.i2.search.SearchProxy;
import org.wcs.smart.i2.ui.EntitySearchJob;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.dialogs.SaveSearchDialog;
import org.wcs.smart.i2.ui.views.entity.search.AdvancedEntitySearchPanel;
import org.wcs.smart.ui.TranslateNamesHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * View for entity search and results
 * @author Emily
 *
 */
public class EntitySearchView {

	/**
	 * context location of entity search results
	 */
	public static final String ENTITY_SEARCH_RESULTS_KEY = "org.wcs.smart.i2.entity.search";
	
	public static final String ID = "org.wcs.smart.i2.view.entitysearch";
	
	private static final int searchDelay = 500;
	
	@Inject
	private IEclipseContext context;
	
	private EntitySearchResultTable entityList;
	private FormToolkit toolkit;
	
	
	private ComboViewer cmbSavedSearch;
	private TableComboViewer cmbEntityType;
	private FilterComposite txtSearch;
	private Font boldFont;
	private Font hlFont;
	
	private LoadEntityTypeJob entityTypeJob = new LoadEntityTypeJob();
	
	private Hyperlink basicSearch;
	private Hyperlink advancedSearch;
	private Hyperlink savedSearch;
	
	private StackLayout searchStack;
	private Composite searchArea;
	private SashForm sashForm;
	
	private IntelEntitySearch lastSearch = null;
	private AdvancedEntitySearchPanel advancedSearchPanel;
	
	private HashMap<Control, int[]> weightMap = new HashMap<>();
	
	private LoadSavedSearches loadSearchJob = new LoadSavedSearches() {
		ISelection lastSelection;
		@Override
		protected void beforeSearch(){
			Display.getDefault().syncExec(()->{
				lastSelection = cmbSavedSearch.getSelection();
				cmbSavedSearch.setInput(new String[]{DialogConstants.LOADING_TEXT});
			});
		}
		@Override
		protected  void searchesLoaded(List<SearchProxy> queries) {
			Display.getDefault().syncExec(()->{
				cmbSavedSearch.setInput(queries);
				cmbSavedSearch.setSelection(lastSelection);
				if(cmbSavedSearch.getSelection().isEmpty() && !queries.isEmpty()){
					cmbSavedSearch.setSelection(new StructuredSelection(queries.get(0)));	
				}
			});
		}
	};
	
	final private EntitySearchJob searchJob = new EntitySearchJob() {
		
		@Override
		public void beforeSearch(IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					entityList.setEntities(null);
				}
			});
			monitor.done();
		}
		
		@Override
		public void afterSearch(IntelSearchResult entities, IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					context.getParent().set(ENTITY_SEARCH_RESULTS_KEY, entities);
					entityList.setEntities(entities);
				}
			});
			monitor.done();
			
		}
		
		@Override
		public void onError(Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					entityList.setSearchError(ex);
				}
			});
		}
	};
	
	
	
	public EntitySearchView() {
		super();
	}

	public List<IntelSearchResultItem> getEntities(){
		return this.entityList.getEntities();
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		//we seem to need this one and the one at the end to get this view to show on top of 
		//the other views in the perspective stacks
		//I am sure there must be a better way...
		context.get(EPartService.class).activate(context.get(MPart.class), true);	
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		parent = toolkit.createComposite(parent);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite topPanel = toolkit.createComposite(sashForm);
		topPanel.setLayout(new GridLayout());
		((GridLayout)topPanel.getLayout()).marginWidth = 0;
		((GridLayout)topPanel.getLayout()).marginHeight = 0;
		
		createHeaderOptions(topPanel);
		
		searchArea = toolkit.createComposite(topPanel);
		searchArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		searchStack = new StackLayout();
		searchArea.setLayout(searchStack);
		
		createBasicSearch(searchArea);
		createAdvancedSearch(searchArea);
		createSavedSearch(searchArea);
		
		searchStack.topControl = searchArea.getChildren()[0];
	
		//spacer
		Composite searchResultsPanel = toolkit.createComposite(sashForm);
		searchResultsPanel.setLayout(new GridLayout());
		((GridLayout)searchResultsPanel.getLayout()).marginWidth = 0;
		((GridLayout)searchResultsPanel.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(searchResultsPanel, "", SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		entityList = new EntitySearchResultTable(searchResultsPanel, toolkit, context);
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		entityTypeJob.schedule();
		loadSearchJob.schedule();
		
		sashForm.setWeights(new int[]{20,80});
		
		//enforce a minimum size
		entityList.addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event event) {
				Point topSize = topPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int topHeight = topSize.y;
				int totalHeight = sashForm.getBounds().height;
				if (topHeight > totalHeight) return;
				if (sashForm.getChildren()[0].getBounds().height < topHeight){
					sashForm.setWeights(new int[]{topHeight - sashForm.getSashWidth(), totalHeight - topHeight});
				}
			}
				
		});
		
		doBasicSearch(0);
		context.get(EPartService.class).activate(context.get(MPart.class), true);
	}
	
	private void updateHyperlink(HyperlinkEvent e){
		basicSearch.setFont(hlFont);
		advancedSearch.setFont(hlFont);
		savedSearch.setFont(hlFont);
		
		((Hyperlink)e.widget).setFont(boldFont);
	
		weightMap.put(searchStack.topControl, sashForm.getWeights());
		if (e.widget == basicSearch){
			searchStack.topControl = searchArea.getChildren()[0];
		}else if (e.widget == advancedSearch){
			searchStack.topControl = searchArea.getChildren()[1];
		}else if (e.widget == savedSearch){
			searchStack.topControl = searchArea.getChildren()[2];
		}
		int[] weights = weightMap.get(searchStack.topControl);
		if (weights == null) weights = new int[]{20,80};
		sashForm.setWeights(weights);
		searchArea.layout();
		
		basicSearch.getParent().layout();
	}
	
	private void createHeaderOptions(Composite parent){
		Composite header = toolkit.createComposite(parent, SWT.NONE);
		header.setLayout(new GridLayout(3, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		basicSearch = toolkit.createHyperlink(header, "Basic Search", SWT.NONE);
		basicSearch.setToolTipText("searches all entity identifiers and entity type attributes flagged as include in basic search");
		advancedSearch = toolkit.createHyperlink(header, "Advanced Search", SWT.NONE);
		savedSearch = toolkit.createHyperlink(header, "Saved Search", SWT.NONE);
		IHyperlinkListener hlistener = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				updateHyperlink(e);
			}
		};
		basicSearch.addHyperlinkListener(hlistener);
		advancedSearch.addHyperlinkListener(hlistener);
		savedSearch.addHyperlinkListener(hlistener);
		
		hlFont = basicSearch.getFont();
		FontData fd = basicSearch.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		
		basicSearch.setFont(boldFont);
	}
	
	private void createSavedSearch(Composite parent){

		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ToolBar tb = new ToolBar(core, SWT.FLAT);
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2, 1));
		
		ToolItem delete = new ToolItem(tb, SWT.PUSH);
		delete.addListener(SWT.Selection, (event)->deleteSavedSearch());
		
		delete.setToolTipText("delete selected search");
		delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		
		ToolItem rename = new ToolItem(tb, SWT.PUSH);
		rename.addListener(SWT.Selection, (event)->{
			Object x = ((IStructuredSelection)cmbSavedSearch.getSelection()).getFirstElement();
			if (x instanceof SearchProxy){
				IntelEntitySearch toEdit = null;
				Session s = HibernateManager.openSession();
				try{
					toEdit = (IntelEntitySearch) s.get(IntelEntitySearch.class, ((SearchProxy) x).getUuid());
					if (toEdit == null) return;
					toEdit.getNames().size();	
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(MessageFormat.format("Error occured renaming search {0}: {1}", ((SearchProxy) x).getName(), ex.getMessage()), ex);
					return;
				}finally{
					s.close();
				}
				
				try {
					(new TranslateNamesHandler ()).execute(new StructuredSelection(toEdit), context.get(Shell.class));
					context.get(IEventBroker.class).send(IntelEvents.ENTITY_SEARCH_MODIFIED, toEdit);
				} catch (Exception ex) {
					Intelligence2PlugIn.displayLog(MessageFormat.format("Error occured renaming search {0}: {1}", ((SearchProxy) x).getName(), ex.getMessage()), ex);
				}
				
			}
		});
		rename.setToolTipText("rename search");
		rename.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		
		ToolItem refresh = new ToolItem(tb, SWT.PUSH);
		refresh.addListener(SWT.Selection, (event)->loadSearchJob.schedule());
		refresh.setToolTipText("refresh saved search list");
		refresh.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		
		
		toolkit.createLabel(core, "Saved Search:");
		
		cmbSavedSearch = new ComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY);
		toolkit.adapt(cmbSavedSearch.getCombo());
		cmbSavedSearch.setContentProvider(ArrayContentProvider.getInstance());
		cmbSavedSearch.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbSavedSearch.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof SearchProxy){
					return ((SearchProxy) element).getName();
				}
				return super.getText(element);
			}
		});
		cmbSavedSearch.setInput(new String[]{DialogConstants.LOADING_TEXT});
	
		Button btnLoad = toolkit.createButton(core, "Load", SWT.PUSH);
		btnLoad.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2, 1));
		btnLoad.addListener(SWT.Selection, (event)->loadSearch());
	}
	
	private void createAdvancedSearch(Composite parent){
		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		advancedSearchPanel = new AdvancedEntitySearchPanel(core, this, toolkit);
		advancedSearchPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	private void loadSearch(){
		if (cmbSavedSearch.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)cmbSavedSearch.getSelection()).getFirstElement();
		if (!(x instanceof SearchProxy)) return;
		
		IntelEntitySearch search = null;
		Session s = HibernateManager.openSession();
		try{
			search = (IntelEntitySearch)s.get(IntelEntitySearch.class, ((SearchProxy)x).getUuid());
		}finally{
			s.close();
		}
		if (search == null) return; //not found
		
		String searchString = search.getSearchString();
		if (searchString.startsWith(IIntelEntitySearch.Type.BASIC.key + IIntelEntitySearch.SEPARATOR)){
			//basic search
			BasicEntitySearch basicsearch = BasicEntitySearch.parse(searchString);
			setSearch(basicsearch);
			updateHyperlink(new HyperlinkEvent(basicSearch, null, null, -1));
			doSearch(null, searchDelay);
		}else if (searchString.startsWith(IIntelEntitySearch.Type.ADVANCED.key + IIntelEntitySearch.SEPARATOR)){
			//advanced search
			AdvancedEntitySearch advsearch = AdvancedEntitySearch.parse(searchString);
			advancedSearchPanel.initPanel(advsearch);
			updateHyperlink(new HyperlinkEvent(advancedSearch, null, null, -1));
			advancedSearchPanel.doSearch();
		}
		this.lastSearch =  search;
		
	}
	
	/*
	 * deletes the selected saved search
	 */
	private void deleteSavedSearch(){
		if (cmbSavedSearch.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)cmbSavedSearch.getSelection()).getFirstElement();
		if (x instanceof SearchProxy){
			if (!MessageDialog.openConfirm(cmbSavedSearch.getControl().getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the search {0}?", ((SearchProxy) x).getName()))){
				return;
			}
			
			IntelEntitySearch toDelete = null;
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				toDelete = (IntelEntitySearch) s.get(IntelEntitySearch.class, ((SearchProxy) x).getUuid());
				if (toDelete != null){
					s.delete(toDelete);
				}
				s.getTransaction().commit();
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(MessageFormat.format("Error occured while deleting search {0}: {1}", ((SearchProxy) x).getName(), ex.getMessage()), ex);
				return;
			}finally{
				s.close();
			}
			if (toDelete != null) context.get(IEventBroker.class).send(IntelEvents.ENTITY_SEARCH_DELETED, toDelete);
		}
	}
	
	/*
	 * Initializes the basic search panel with the provided search
	 */
	@SuppressWarnings("unchecked")
	private void setSearch(BasicEntitySearch search){
		if (search.getSearchString() != null ){
			txtSearch.setText(search.getSearchString());
			List<Object> types = (List<Object>) cmbEntityType.getInput();
			List<IntelEntityType> selections = new ArrayList<>();
			if (search.getEntityTypes() != null){
				for (Object t : types){
					if (t instanceof IntelEntityType && search.getEntityTypes().contains(((IntelEntityType)t).getKeyId()))
						selections.add((IntelEntityType)t);
				}
			}
			cmbEntityType.setSelection(new StructuredSelection(selections));
		}
	}
	
	/*
	 * Creates the basic search panel
	 */
	private Composite createBasicSearch(Composite parent){
		Composite search = toolkit.createComposite(parent, SWT.NONE);
		search.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		search.setLayout(new GridLayout());
		((GridLayout)search.getLayout()).marginWidth = 0;
		((GridLayout)search.getLayout()).marginHeight = 0;
		
		Composite core = toolkit.createComposite(search, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.createLabel(core, "Search:");
		
		txtSearch = new FilterComposite(core, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				doBasicSearch(searchDelay);
			}
		});
		
		toolkit.createLabel(core, "EntityType:");
		
		cmbEntityType = new TableComboViewer(core, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityType.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolkit.adapt(cmbEntityType.getTableCombo());
		cmbEntityType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				doBasicSearch(500);
			}
		});
		
		Composite bottom = toolkit.createComposite(search,  SWT.NONE);
		bottom.setLayout(new GridLayout(2, false));
		((GridLayout)bottom.getLayout()).marginWidth = 0;
		((GridLayout)bottom.getLayout()).marginHeight = 0;
		bottom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnSearch = toolkit.createButton(bottom, "Search", SWT.PUSH);
		btnSearch.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		btnSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				doBasicSearch(0);	
			}
		});
		Hyperlink saveSearch = toolkit.createHyperlink(bottom, "Save Search", SWT.NONE);
		saveSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
		saveSearch.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				saveBasicSearch();
			}
		});
		
		return search;
	}

	@Inject
	@Optional
	private void searchModified(@UIEventTopic(IntelEvents.ENTITY_SEARCH_ALL) IntelEntitySearch search){
		loadSearchJob.schedule();
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_ALL) IntelEntity entity){
		doSearch(null, searchDelay);
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_ALL) List<IntelEntity> entity){
		doSearch(null, searchDelay);
	}
	
	@Inject
	@Optional
	private void entityTypesModified(@UIEventTopic(IntelEvents.ENTITY_TYPE_ALL) IntelEntityType type){
		entityTypeJob.schedule();
		doSearch(null, searchDelay);
	}

	/*
	 * Creates a basic search object from the basic search panel
	 */
	private BasicEntitySearch createBasicSearch(){
		List<IntelEntityType> filters = new ArrayList<IntelEntityType>();
		for (Iterator<?> iterator = ((IStructuredSelection)cmbEntityType.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if(x instanceof IntelEntityType){
				filters.add((IntelEntityType) x);
			}
			
		}
		BasicEntitySearch search = new BasicEntitySearch(txtSearch.getPatternFilter(), filters);
		return search;
	}
	
	/*
	 * saves the basic search
	 */
	private void saveBasicSearch(){
		saveSearch(createBasicSearch());
	}
	
	/*
	 * Saves a search
	 */
	public void saveSearch(IIntelEntitySearch search){
		SaveSearchDialog dd = new SaveSearchDialog(context.get(Shell.class), search, lastSearch);
		ContextInjectionFactory.inject(dd, context);
		if (dd.open() == Window.OK){
			lastSearch = dd.getSavedSearch();
		}
	}
	
	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
		toolkit.dispose();
		if (boldFont != null) boldFont.dispose();
	}
	
	/*
	 * execute basic search
	 */
	private void doBasicSearch(long delay){
		doSearch(createBasicSearch(), delay);
	}
	
	/**
	 * executes an advanced search
	 */
	public void doAdvancedSearch(AdvancedEntitySearch search, long delay){
		doSearch(search, delay);	
	}
	
	/*
	 * executes search
	 */
	private void doSearch(IIntelEntitySearch search, long delay){
		if (search != null){
			searchJob.setSearch(search);
		}
		searchJob.cancel();
		searchJob.schedule(delay);
	}
	
	public static class EntitySearchViewWrapper extends DIViewPart<EntitySearchView>{
		public EntitySearchViewWrapper() {
			super(EntitySearchView.class);
		}
	}
	
	/*
	 * job for loading entity types
	 */
	private class LoadEntityTypeJob extends Job{

			public LoadEntityTypeJob() {
				super("Refreshing Entity Types");
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> types = new ArrayList<Object>();
				Session session = HibernateManager.openSession();
				try{
					types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(session, SmartDB.getCurrentConservationArea()));
				}finally{
					session.close();
				}
				
				types.add(0, "All Types");
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						cmbEntityType.setInput(types);
					}		
				});
				return Status.OK_STATUS;
			}
	    }
	    
	

}