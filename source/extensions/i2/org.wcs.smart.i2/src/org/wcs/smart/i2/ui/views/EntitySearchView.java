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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.UIEvents.EventTags;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
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
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.search.AllEntitySearch;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.search.IIntelEntitySearch;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.i2.search.LoadSavedSearches;
import org.wcs.smart.i2.search.SearchProxy;
import org.wcs.smart.i2.search.SpatialEntitySearch;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityPerspective;
import org.wcs.smart.i2.ui.EntitySearchJob;
import org.wcs.smart.i2.ui.dialogs.SaveSearchDialog;
import org.wcs.smart.i2.ui.views.entity.search.AdvancedEntitySearchPanel;
import org.wcs.smart.i2.ui.views.entity.search.AllPanel;
import org.wcs.smart.i2.ui.views.entity.search.BasicEntitySearchPanel;
import org.wcs.smart.i2.ui.views.entity.search.SpatialSearchPanel;
import org.wcs.smart.ui.TranslateNamesHandler;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Geometry;

/**
 * View for entity search and results
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class EntitySearchView {

	/**
	 * Context location of entity search results.  This will return the list currently
	 * displayed in the view; NOT ALL the search results.  It returns List<IntelEntity>.
	 */
	public static final String ENTITY_SEARCH_RESULTS_KEY = "org.wcs.smart.i2.entity.search"; //$NON-NLS-1$
	
	public static final String ID = "org.wcs.smart.i2.view.entitysearch"; //$NON-NLS-1$
	
	private static final int searchDelay = 500;
	
	public enum Panel {BASIC, ADVANCED, SAVED, SPATIAL, ALL};
	
	@Inject
	private IEclipseContext context;
	
	private EntitySearchResultTable entityList;
	private FormToolkit toolkit;
	
	private ComboViewer cmbSavedSearch;
	
	private Font boldFont;
	private Font hlFont;
	
	
	private Hyperlink basicSearch;
	private Hyperlink advancedSearch;
	private Hyperlink savedSearch;
	private Hyperlink spatialSearch;
	private Hyperlink allTable;
	
	
	private StackLayout searchStack;
	private StackLayout outerStack;
	private Composite searchArea;
	private SashForm searchSashForm;
	
	private IntelEntitySearch lastSearch = null;
	
	private BasicEntitySearchPanel basicPanel = null;
	private AdvancedEntitySearchPanel advancedSearchPanel;
	private SpatialSearchPanel spatialPanel;
	private AllPanel allPanel = null;
	
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
			Display.getDefault().asyncExec(()->{
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
		}
		
		@Override
		public void afterSearch(IntelSearchResult searchResult, IProgressMonitor monitor) {
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					entityList.setEntities(searchResult);
				}
			});
			
		}
		
		@Override
		public void onError(Exception ex) {
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			Display.getDefault().asyncExec(new Runnable(){
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

	public List<IntelEntity> getEntities(){
		return this.entityList.getEntities();
	}
	
	public void setPanel(Panel p) {
		switch(p) {
		case ADVANCED:
			updateHyperlink(new HyperlinkEvent(advancedSearch, null, null, -1));
			return;
		case ALL:
			updateHyperlink(new HyperlinkEvent(allTable, null, null, -1));
			return;
		case BASIC:
			updateHyperlink(new HyperlinkEvent(basicSearch, null, null, -1));
			return;
		case SAVED:
			updateHyperlink(new HyperlinkEvent(savedSearch, null, null, -1));
			return;
		case SPATIAL:
			updateHyperlink(new HyperlinkEvent(spatialSearch, null, null, -1));
			return;
		}
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
		
		toolkit.adapt(parent);
		
		Composite header = toolkit.createComposite(parent);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createHeaderOptions(header);
		
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new StackLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outerStack = (StackLayout) main.getLayout();

		searchSashForm = new SashForm(main, SWT.VERTICAL);
		searchSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		searchArea = toolkit.createComposite(searchSashForm);
		searchArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		searchStack = new StackLayout();
		searchArea.setLayout(searchStack);
		
		createBasicSearch(searchArea);
		createAdvancedSearch(searchArea);
		createSavedSearch(searchArea);
		createSpatialSearch(searchArea);
		
		searchStack.topControl = searchArea.getChildren()[0];
	
		//spacer
		Composite searchResultsPanel = toolkit.createComposite(searchSashForm);
		searchResultsPanel.setLayout(new GridLayout());
		((GridLayout)searchResultsPanel.getLayout()).marginWidth = 0;
		((GridLayout)searchResultsPanel.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(searchResultsPanel, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		entityList = new EntitySearchResultTable(searchResultsPanel, toolkit, context);
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		loadSearchJob.schedule();
		
		searchSashForm.setWeights(new int[]{20,80});
		
		//enforce a minimum size
		entityList.addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event event) {
				Point topSize = ((StackLayout)searchArea.getLayout()).topControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int topHeight = topSize.y+5;
				int totalHeight = searchSashForm.getBounds().height;
				if (topHeight > totalHeight) return;
				if (searchSashForm.getChildren()[0].getBounds().height < topHeight){
					searchSashForm.setWeights(new int[]{topHeight - searchSashForm.getSashWidth(), totalHeight - topHeight});
				}
			}
				
		});
		
		
		allPanel = new AllPanel(main, this, toolkit);
		ContextInjectionFactory.inject(allPanel, context);
		allPanel.setLayout(new GridLayout());
		allPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		outerStack.topControl = searchSashForm;
		searchStack.topControl = searchArea.getChildren()[0];
		searchArea.layout();
		searchSashForm.getParent().layout();
		
		context.get(EPartService.class).activate(context.get(MPart.class), true);
		
		if (context.get(EModelService.class).getActivePerspective(context.get(MWindow.class)).getElementId().equals(EntityPerspective.ID)) {
			setPanel(Panel.ALL);
		}else {
			setPanel(Panel.BASIC);
			basicPanel.doSearch();
		}
	}
	
	@Inject
	@Optional
	public void subscribeTopicSelectedElement(@EventTopic
	        (UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT) org.osgi.service.event.Event event) {
		//when the Entity Perspective is selected ensure the all entity panel is displayed.
	    Object element = event.getProperty(EventTags.ELEMENT);
	    Object newValue = event.getProperty(EventTags.NEW_VALUE);
	    // ensure that the selected element of a perspective stack is changed and that this is a perspective
	    if (!(element instanceof MPerspectiveStack) || !(newValue instanceof MPerspective)) {
	        return;
	    }

		if (((MPerspective) newValue).getElementId().equals(EntityPerspective.ID)) {
			setPanel(Panel.ALL);
		}
	}
	
	private void updateHyperlink(HyperlinkEvent e){
		basicSearch.setFont(hlFont);
		advancedSearch.setFont(hlFont);
		savedSearch.setFont(hlFont);
		spatialSearch.setFont(hlFont);
		allTable.setFont(hlFont);
		((Hyperlink)e.widget).setFont(boldFont);
		((Hyperlink)e.widget).getParent().layout();
		
		
		weightMap.put(searchStack.topControl, searchSashForm.getWeights());
		if (e.widget == basicSearch){
			searchStack.topControl = searchArea.getChildren()[0];
			outerStack.topControl = searchSashForm;
		}else if (e.widget == advancedSearch){
			searchStack.topControl = searchArea.getChildren()[1];
			outerStack.topControl = searchSashForm;
		}else if (e.widget == savedSearch){
			searchStack.topControl = searchArea.getChildren()[2];
			outerStack.topControl = searchSashForm;
		}else if (e.widget == spatialSearch) {
			searchStack.topControl = searchArea.getChildren()[3];
			outerStack.topControl = searchSashForm;
		}else if (e.widget == allTable) {
			outerStack.topControl = allPanel;
		}
		int[] weights = weightMap.get(searchStack.topControl);
		if (weights == null) weights = new int[]{20,80};
		searchSashForm.setWeights(weights);
		searchArea.layout();
		searchSashForm.getParent().layout();
//		basicSearch.getParent().layout();
	}
	
	public boolean isSpatialActive() {
		return searchStack.topControl == searchArea.getChildren()[3];
	}
	
	private void createHeaderOptions(Composite parent){
		Composite header = toolkit.createComposite(parent, SWT.NONE);
		header.setLayout(new GridLayout(5, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		basicSearch = toolkit.createHyperlink(header, Messages.EntitySearchView_BasicSearchTab, SWT.NONE);
		basicSearch.setToolTipText(Messages.EntitySearchView_BasicSearchTooltip);
		advancedSearch = toolkit.createHyperlink(header, Messages.EntitySearchView_AdvSearchTab, SWT.NONE);
		savedSearch = toolkit.createHyperlink(header, Messages.EntitySearchView_SavedSearchTab, SWT.NONE);
		spatialSearch = toolkit.createHyperlink(header, Messages.EntitySearchView_SpatialSearchLabel, SWT.NONE);
		spatialSearch.setToolTipText(Messages.EntitySearchView_SpatialSearchTooltip);
		
		allTable = toolkit.createHyperlink(header, Messages.EntitySearchView_AllLabel, SWT.NONE);
		allTable.setToolTipText(Messages.EntitySearchView_AllTooltip);
		
		IHyperlinkListener hlistener = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				updateHyperlink(e);
			}
		};
		basicSearch.addHyperlinkListener(hlistener);
		advancedSearch.addHyperlinkListener(hlistener);
		savedSearch.addHyperlinkListener(hlistener);
		spatialSearch.addHyperlinkListener(hlistener);
		allTable.addHyperlinkListener(hlistener);
		
		basicSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		advancedSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		savedSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		spatialSearch.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		allTable.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
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
		delete.setToolTipText(Messages.EntitySearchView_DeleteTooltip);
		delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		delete.setEnabled(IntelSecurityManager.INSTANCE.canEditEntity());
		
		ToolItem rename = new ToolItem(tb, SWT.PUSH);
		rename.addListener(SWT.Selection, (event)->{
			Object x = ((IStructuredSelection)cmbSavedSearch.getSelection()).getFirstElement();
			if (x instanceof SearchProxy){
				IntelEntitySearch toEdit = null;
				try(Session s = HibernateManager.openSession()){
					toEdit = (IntelEntitySearch) s.get(IntelEntitySearch.class, ((SearchProxy) x).getUuid());
					if (toEdit == null) return;
					toEdit.getNames().size();	
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.EntitySearchView_RenameError1, ((SearchProxy) x).getName(), ex.getMessage()), ex);
					return;
				}
				
				try {
					(new TranslateNamesHandler ()).execute(new StructuredSelection(toEdit), context.get(Shell.class));
					context.get(IEventBroker.class).send(IntelEvents.ENTITY_SEARCH_MODIFIED, toEdit);
				} catch (Exception ex) {
					Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.EntitySearchView_RenameError2, ((SearchProxy) x).getName(), ex.getMessage()), ex);
				}
				
			}
		});
		rename.setToolTipText(Messages.EntitySearchView_renametooltip);
		rename.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		rename.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		ToolItem refresh = new ToolItem(tb, SWT.PUSH);
		refresh.addListener(SWT.Selection, (event)->loadSearchJob.schedule());
		refresh.setToolTipText(Messages.EntitySearchView_refreshtooltip);
		refresh.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refresh.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		toolkit.createLabel(core, Messages.EntitySearchView_SavedSearchLabel);
		
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
		cmbSavedSearch.getControl().setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
		
		Button btnLoad = toolkit.createButton(core, Messages.EntitySearchView_LoadButtonText, SWT.PUSH);
		btnLoad.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2, 1));
		btnLoad.addListener(SWT.Selection, (event)->loadSearch());
		btnLoad.setEnabled(IntelSecurityManager.INSTANCE.canViewEntities());
	}
	
	
	private void createSpatialSearch(Composite parent){
		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout());
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		spatialPanel = new SpatialSearchPanel(core, context, toolkit, this);
		spatialPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	
	private void createAdvancedSearch(Composite parent){
		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		advancedSearchPanel = new AdvancedEntitySearchPanel(core, this, toolkit);
		advancedSearchPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	/**
	 * Performs a spatial search based on the location in the provided record
	 * 
	 * @param recordUuid
	 */
	public void doSpatialSearch(UUID recordUuid){
		spatialPanel.selectRecord(recordUuid);
		doSpatialSearch(recordUuid, null);
	}
	
	/**
	 * Performs a spatial search based on the provided geometry
	 * @param geometry
	 */
	public void doSpatialSearch(Geometry geometry){
		doSpatialSearch(null, geometry);
	}
	
	private void doSpatialSearch(UUID recordUuid, Geometry geometry){
		StringBuilder sb = new StringBuilder();
		sb.append(IntelEntitySearch.Type.RECORD.key);
		sb.append(IntelEntitySearch.SEPARATOR);
		sb.append(spatialPanel.getDistance());
		sb.append(IntelEntitySearch.SEPARATOR);
		sb.append(spatialPanel.getEntityTypeFilters());
		IntelEntitySearch search = new IntelEntitySearch();
		search.setSearchString(sb.toString());
		
		IIntelEntitySearch iSearch = null;
		if (recordUuid != null) {
			iSearch = new SpatialEntitySearch(search, recordUuid);
		}else if (geometry != null) {
			iSearch = new SpatialEntitySearch(search, geometry, SmartDB.getCurrentConservationArea());
		}
		updateHyperlink(new HyperlinkEvent(spatialSearch, null, null, -1));
		doSearch(iSearch, searchDelay);
	}
	
	private void loadSearch(){
		if (cmbSavedSearch.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)cmbSavedSearch.getSelection()).getFirstElement();
		if (!(x instanceof SearchProxy)) return;
		
		IntelEntitySearch search = null;
		try(Session s = HibernateManager.openSession()){
			search = (IntelEntitySearch)s.get(IntelEntitySearch.class, ((SearchProxy)x).getUuid());
		}
		if (search == null) return; //not found
		IIntelEntitySearch iSearch = IIntelEntitySearch.parseSearchString(search.getSearchString(), search.getConservationArea());
		if (iSearch instanceof BasicEntitySearch) {
			setSearch((BasicEntitySearch)iSearch);
			updateHyperlink(new HyperlinkEvent(basicSearch, null, null, -1));
			doSearch(null, searchDelay);
		}else if (iSearch instanceof AdvancedEntitySearch){
			//advanced search
			AdvancedEntitySearch advsearch = (AdvancedEntitySearch) iSearch;
			advancedSearchPanel.initPanel(advsearch);
			updateHyperlink(new HyperlinkEvent(advancedSearch, null, null, -1));
			advancedSearchPanel.doSearch();
		}else if (iSearch instanceof AllEntitySearch) {
			AllEntitySearch allSearch = (AllEntitySearch)iSearch;
			allPanel.initPanel(allSearch); //this will reload data as necessary
			updateHyperlink(new HyperlinkEvent(allTable, null, null, -1));
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
			if (!MessageDialog.openConfirm(cmbSavedSearch.getControl().getShell(), Messages.EntitySearchView_DeleteTitle, MessageFormat.format(Messages.EntitySearchView_DeleteMessage, ((SearchProxy) x).getName()))){
				return;
			}
			
			IntelEntitySearch toDelete = null;
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try {
					toDelete = (IntelEntitySearch) s.get(IntelEntitySearch.class, ((SearchProxy) x).getUuid());
					if (toDelete != null){
						s.delete(toDelete);
					}
					s.getTransaction().commit();
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.EntitySearchView_DeleteError, ((SearchProxy) x).getName(), ex.getMessage()), ex);
					return;
				}
			}
			if (toDelete != null) context.get(IEventBroker.class).send(IntelEvents.ENTITY_SEARCH_DELETED, toDelete);
		}
	}
	
	/*
	 * Initializes the basic search panel with the provided search
	 */
	private void setSearch(BasicEntitySearch search){
		basicPanel.setSearch(search);
	}
	
	/*
	 * Creates the basic search panel
	 */
	private Composite createBasicSearch(Composite parent){
		Composite core = toolkit.createComposite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		basicPanel = new BasicEntitySearchPanel(core, this, toolkit);
		basicPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return core;
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
		allPanel.refresh(searchDelay);
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_ALL) Collection<IntelEntity> entity){
		doSearch(null, searchDelay);
		allPanel.refresh(searchDelay);
	}
	
	@Inject
	@Optional
	private void entityTypesModified(@UIEventTopic(IntelEvents.ENTITY_TYPE_ALL) Object data){
		basicPanel.refresh();
		allPanel.refresh(searchDelay);
		doSearch(null, searchDelay);
	}

	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		basicPanel.refresh();
		allPanel.refresh(searchDelay);
		loadSearchJob.schedule();
		doSearch(null, searchDelay);
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
	public void doBasicSearch(BasicEntitySearch search, long delay){
		doSearch(search, delay < 0 ? searchDelay : delay );
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
		if (!IntelSecurityManager.INSTANCE.canViewEntities()) {
			entityList.setSearchError(new Exception(Messages.EntitySearchView_unauthorized));
			return;
		}
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

}