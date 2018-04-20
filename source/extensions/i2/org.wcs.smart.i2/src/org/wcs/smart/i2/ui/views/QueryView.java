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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.QueryManager;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.IntelQueryLabelProvider;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.editors.query.IQueryEditor;
import org.wcs.smart.i2.ui.handler.NewQueryHandler;
import org.wcs.smart.i2.ui.handler.OpenQueryHandler;
import org.wcs.smart.i2.ui.views.query.EntitySummaryContentProvider;
import org.wcs.smart.i2.ui.views.query.FilterTreeContentProvider;
import org.wcs.smart.i2.ui.views.query.FilterTreeItem;
import org.wcs.smart.i2.ui.views.query.FilterTreeLabelProvider;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.ui.TranslateNamesHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.E3Utils;

/**
 * View for listing all intelligence queries in the system
 *  
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class QueryView {

	public static final String REFRESHLABEL_KEY = "refreshlabel"; //$NON-NLS-1$

	public static final String ID = "org.wcs.smart.i2.ui.view.queries"; //$NON-NLS-1$
	
	@Inject
	private IEclipseContext context;
	
	private QueryViewerFilter queryFilter;
	private TableViewer queryList;
	
	private TreeViewer filterTree = null;
	//private Job refreshJob;
	
	//query filter tree
	private Composite treePart;
	private Listener refreshListener = (event)->refreshFiltersView();
	
	private HashMap<String, ITreeContentProvider> queryToContentProvider;
	
	private IAreaModifiedListener areaListener = new IAreaModifiedListener() {
		@Override
		public void areasUpdated(AreaType type) {
			Display.getDefault().asyncExec(()->sourceModified());
		}
	};
	private IDataModelListener dmListener = new IDataModelListener() {
		@Override
		public void modified() {
			sourceModified();
		}
	};
	private EventHandler entityEvent = new EventHandler() {
		
		@Override
		public void handleEvent(Event event) {
			sourceModified();
		}
	};
	
	public QueryView() {
		super();
		queryToContentProvider = new HashMap<>();
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		
		ConservationAreaManager.getInstance().addAreaChangeListener(areaListener);
		DataModelManager.INSTANCE.addChangeListener(dmListener);
		context.get(IEventBroker.class).subscribe(IntelEvents.ENTITY_ALL, entityEvent);
		context.get(IEventBroker.class).subscribe(IntelEvents.ENTITY_TYPE_ALL, entityEvent);
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		toolkit.adapt(parent);
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{Messages.QueryView_SaveQuerySection, Messages.QueryView_FiltersSection}, parent, toolkit, parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)tabList.getLayoutData()).verticalIndent = 2;
		
		Composite tabPart = toolkit.createComposite(parent, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));	
		
		Composite queryList = createQueryList(tabPart, toolkit);
		Composite filterList = createFilterComposite(tabPart, toolkit);
		
		tabList.setContent(new Composite[]{queryList, filterList}, tabPart);
		tabList.selectTab(0);
		
		
		refreshQueryList();
		refreshFiltersView();
	}
	
	private Composite createQueryList(Composite parent, FormToolkit toolkit){
		Composite part = toolkit.createComposite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		
		FilterComposite typeFilter = new FilterComposite(part, SWT.NONE);
		toolkit.adapt(typeFilter);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				if (typeFilter.getPatternFilter() == null || typeFilter.getPatternFilter().isEmpty()){
					queryList.removeFilter(queryFilter);
					queryFilter = null;
				}else{
					if (queryFilter == null){
						queryFilter = new QueryViewerFilter();
						queryFilter.setFilterString(typeFilter.getPatternFilter());
						queryList.addFilter(queryFilter);
					}else{
						queryFilter.setFilterString(typeFilter.getPatternFilter());
						queryList.refresh();
					}
				}
			}
		});
		
		queryList = new TableViewer(part, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		queryList.setContentProvider(ArrayContentProvider.getInstance());
		queryList.setLabelProvider(new IntelQueryLabelProvider());
		queryList.setInput(new String[]{DialogConstants.LOADING_TEXT});
		queryList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryList.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openSelection();
			}
		});
		
		//add drag support
		queryList.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelQuerySelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelQuerySelectionTransfer.getTransfer().setSelection(queryList.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelQuerySelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = queryList.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelQuerySelectionTransfer.getTransfer().setSelection(null);
			}
		});
		
		createMenu(queryList);
		return part;
	}
	
	private void sourceModified(){
		filterTree.getTree().setEnabled(false);
		if (filterTree.getTree().getData(REFRESHLABEL_KEY) == null){
			Label modifiedLabel = new Label(treePart, SWT.NONE);
			modifiedLabel.setText(Messages.QueryView_refreshRequired);
			
			Rectangle r = filterTree.getTree().getBounds();
			Point size = modifiedLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			
			int x = (int) Math.round((r.width - size.x) / 2.0);
			int y = (int) Math.round((r.height - size.y) / 2.0);
			
			modifiedLabel.setBounds(x, y, size.x, size.y);
			filterTree.getTree().setData(REFRESHLABEL_KEY, modifiedLabel);
			modifiedLabel.moveAbove(filterTree.getTree());
			treePart.layout(true);
			
			modifiedLabel.addListener(SWT.MouseDown, refreshListener);
		}
	}
	
	
	private Composite createFilterComposite(Composite parent, FormToolkit toolkit){
		Composite part = new Composite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		
		treePart = new Composite(part, SWT.NONE);
		treePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treePart.addListener(SWT.MouseDown, refreshListener);
		
		filterTree = new TreeViewer(treePart, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI);
		filterTree.setLabelProvider(new FilterTreeLabelProvider());
		treePart.addListener(SWT.Resize, e->{
			filterTree.getTree().setBounds(0,0,treePart.getBounds().width, treePart.getBounds().height);
		});

		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			filterTree.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					addFilterSelectionToQuery();
				}
			});
			
			Menu mnu = new Menu(filterTree.getTree());
			MenuItem addToQuery = new MenuItem(mnu,SWT.PUSH);
			addToQuery.setText(Messages.QueryView_AddToQueryBtn);
			addToQuery.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addToQuery.addListener(SWT.Selection, event->addFilterSelectionToQuery());
			filterTree.getTree().setMenu(mnu);
			
			mnu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {		
					addToQuery.setEnabled(getActiveQueryEditor() != null && !getSelectedDropItems().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {
				}
			});
		}
		return part;
	}
	
	private IQueryEditor getActiveQueryEditor() {
		IQueryEditor addTo = null;
		EPartService pService = context.get(EPartService.class);
		for (MPart part : context.get(EPartService.class).getParts()){
			if (pService.isPartVisible(part)){
				Object item = E3Utils.getSourceObject(part);
				if (item instanceof IQueryEditor){
					addTo = (IQueryEditor) item;
					break;
				}
			}
		}
		return addTo;
	}
	
	private List<DropItem[]> getSelectedDropItems() {
		List<DropItem[]> items = new ArrayList<>();
		IStructuredSelection selection = (IStructuredSelection) filterTree.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object element = (Object) iterator.next();
			if (element instanceof FilterTreeItem){
				DropItem[] di = ((FilterTreeItem) element).asDropItem();
				if (di == null) continue;
				items.add(di);
			}
		}
		return items;
	}
	
	private void addFilterSelectionToQuery(){
		IQueryEditor addTo = getActiveQueryEditor();
		if (addTo == null) return; //no query open
		for (DropItem[] di : getSelectedDropItems()) {
			addTo.addDropItems(di);
		}
	}
	
	private void refreshFiltersView(){
		if (filterTree.getContentProvider() == null) return;
		filterTree.setInput(DialogConstants.LOADING_TEXT);
		filterTree.refresh();
	}
	
	private void renameQuery(){
		Object x = ((IStructuredSelection)queryList.getSelection()).getFirstElement();
		if (x == null) return;
		if (x instanceof QueryProxy){
			AbstractIntelQuery toEdit = null;
			try(Session s = HibernateManager.openSession()){
				toEdit = QueryManager.INSTANCE.findQuery(s, ((QueryProxy) x).getUuid(),((QueryProxy) x).getTypeKey());
				if (toEdit != null){
					toEdit.getNames();
				}
			}
			if (toEdit == null){
				Intelligence2PlugIn.log(Messages.QueryView_QueryNotFound, null);
				return; //query not found
			}
			
			try {
				(new TranslateNamesHandler()).execute(new StructuredSelection(toEdit), context.get(Shell.class));
				context.get(IEventBroker.class).send(IntelEvents.QUERY_MODIFIED, toEdit);
			} catch (ExecutionException e) {
				Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.QueryView_RenameError, e.getMessage()), e);
			}
		}
		
	}
	
	private void deleteSelection(){
		int cnt = ((IStructuredSelection)queryList.getSelection()).size();
		if (!MessageDialog.openQuestion(context.get(Shell.class), Messages.QueryView_DeleteTitle, MessageFormat.format(Messages.QueryView_DeleteMessage, cnt))){
			return;
		}
		
		List<AbstractIntelQuery> removed = new ArrayList<AbstractIntelQuery>();
		for (Iterator<?> iterator = ((IStructuredSelection)queryList.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof QueryProxy){
				AbstractIntelQuery deletedItem = InternalQueryManager.INSTANCE.deleteQuery(((QueryProxy) x).getUuid(), ((QueryProxy) x).getTypeKey());
				if (deletedItem != null){
					removed.add(deletedItem);
				}
			}
		}
		context.get(IEventBroker.class).post(IntelEvents.QUERY_DELETED, removed);
	}
	
	private void createNewQuery(String queryTypeKey){
		IEclipseContext kid = context.createChild();
		kid.set(NewQueryHandler.QUERY_TYPE_KEY, queryTypeKey);
		ContextInjectionFactory.invoke(new NewQueryHandler(), Execute.class, kid);
	}
	private void openSelection(){
		for (Iterator<?> iterator = ((IStructuredSelection)queryList.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			//open query
			if (x instanceof QueryProxy){
				(new OpenQueryHandler()).openQuery( ((QueryProxy) x).getUuid(), ((QueryProxy)x).getTypeKey(), true );
			}
		}
	}
	private void createMenu(Viewer control){
		Menu m = new Menu(control.getControl());
		control.getControl().setMenu(m);
	
		List<MenuItem> items = new ArrayList<MenuItem>();
		
		MenuItem miOpen = new MenuItem(m, SWT.PUSH);
		miOpen.setText(Messages.QueryView_OpenMenuItem);
		miOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openSelection();
			}
		});
		items.add(miOpen);

		
		if (IntelSecurityManager.INSTANCE.canEditQuery()){
			MenuItem miNew = new MenuItem(m, SWT.CASCADE);
			miNew.setText(Messages.QueryView_NewQueryOption);
			miNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			
			Menu mnuTypes = new Menu(miNew);
			miNew.setMenu(mnuTypes);
			for (String[] queryTypes : InternalQueryManager.INSTANCE.getSupportQueryTypes()) {
				MenuItem mi = new MenuItem(mnuTypes, SWT.PUSH);
				mi.setText(queryTypes[1]);
				if (queryTypes[0].equalsIgnoreCase(IntelEntitySummaryQuery.KEY)) {
					mi.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_QUERY_ENTITYSUM));
				}else if (queryTypes[0].equalsIgnoreCase(IntelRecordObservationQuery.KEY)) {
					mi.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_QUERY_RECORDOBS));
				}else if (queryTypes[0].equalsIgnoreCase(IntelEntityRecordQuery.KEY)) {
					mi.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_QUERY_ENTITYRECORD));
				}
				mi.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						createNewQuery(queryTypes[0]);
					}
				});	
			}
			
			new MenuItem(m, SWT.SEPARATOR);
			
			MenuItem miDelete = new MenuItem(m, SWT.PUSH);
			miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteSelection();
				}
			});
			items.add(miDelete);

			MenuItem miRename = new MenuItem(m, SWT.PUSH);
			miRename.setText(Messages.QueryView_RenameMenuItem);
			miRename.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
			miRename.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					renameQuery();
				}
			});
			items.add(miRename);
		}
		
		if (IntelSecurityManager.INSTANCE.canEditWorkingSet()){
			new MenuItem(m, SWT.SEPARATOR);
			
			MenuItem miAdd = new MenuItem(m, SWT.PUSH);
			miAdd.setText(Messages.QueryView_AddToWsMenuItem);
			miAdd.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
			miAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<UUID> queries = new ArrayList<>();
					for (Iterator<?> iterator = ((IStructuredSelection)control.getSelection()).iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();	
						if (x instanceof QueryProxy){
							queries.add(((QueryProxy) x).getUuid());
						}
					}
					WorkingSetManager.INSTANCE.addQueryUuidToActiveWorkingSet(queries, context);
				}
			});
			
			m.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					miAdd.setEnabled(!control.getSelection().isEmpty() && WorkingSetManager.INSTANCE.isSet());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});
		}
		
		items.forEach(mitem->{
			m.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					mitem.setEnabled(!control.getSelection().isEmpty());
				}
			
				@Override
				public void menuHidden(MenuEvent e) {}
			});
		});
		
	}
	
	
	 @Optional
	 @Inject
	 private void configurationChanged(@UIEventTopic(SmartDB.CCAA_CONFIGURATION_MODIFIED) Object data){
		 InternalQueryManager.INSTANCE.getQueryItemProvider().reset();
		 sourceModified();
	 }
	 
	 @Optional
	 @Inject
	 private void dbModified(@UIEventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		 refreshQueryList();
		 refreshFiltersView();
	 }
	 
	 @Optional
	 @Inject
	 private void queryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) AbstractIntelQuery data){
		 queryList.refresh(new QueryProxy(data.getName(), data.getUuid(), data.getTypeKey()));
	 }
	 @Optional
	 @Inject
	 private void multiQueryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) List<AbstractIntelQuery> data){
		 data.forEach(i-> queryList.refresh(new QueryProxy(i.getName(), i.getUuid(), i.getTypeKey())));
	 }
	 
	@Optional
	@Inject
	private void queryNew(@UIEventTopic(IntelEvents.QUERY_NEW) Object data) {
		refreshQueryList();
	}

	@Inject
	@Optional
	public void handleBringToTop(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		IQueryEditor editor = getActiveQueryEditor();
		if (editor == null) return;
		if (editor.getQuery() == null) return;
		String queryTypeKey = editor.getQuery().getTypeKey();
		
		ITreeContentProvider provider = queryToContentProvider.get(queryTypeKey);
		if (provider == null) {
			if (queryTypeKey.equals(IntelEntitySummaryQuery.KEY)) {
				provider = new EntitySummaryContentProvider();
			}else if (queryTypeKey.equals(IntelRecordObservationQuery.KEY)) {
				provider = new FilterTreeContentProvider(new IntelRecordObservationQuery());
			}else if (queryTypeKey.equals(IntelEntityRecordQuery.KEY)) {
				provider = new FilterTreeContentProvider(new IntelEntityRecordQuery());
			}
			queryToContentProvider.put(queryTypeKey, provider);
		}
		if (provider == null) {
			return;
		}
		if (filterTree.getContentProvider() == provider) {
			filterTree.refresh(true);
			return;
		}
		filterTree.setContentProvider(provider);
		filterTree.setInput(DialogConstants.LOADING_TEXT);
	}
	
	@Optional
	@Inject
	private void queryDeleted(
			@UIEventTopic(IntelEvents.QUERY_DELETED) Object data) {
		refreshQueryList();
	}

	public void refreshView() {
		refreshQueryList();
		refreshFiltersView();	
	}

	private void refreshQueryList() {
		queryList.setInput(new String[] { DialogConstants.LOADING_TEXT });
		refreshQueriesJob.schedule();
		
	}
	 
	@Focus
	public void setFocus() {
		queryList.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
		ConservationAreaManager.getInstance().removeAreaChangeListener(areaListener);
		DataModelManager.INSTANCE.removeChangeListener(dmListener);
		context.get(IEventBroker.class).unsubscribe(entityEvent);
	}
	
	public static class QueryViewWrapper extends DIViewPart<QueryView>{
		public QueryViewWrapper() {
			super(QueryView.class);
		}
	}

	
	Job refreshQueriesJob = new Job(Messages.QueryView_RefreshJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QueryProxy> proxyItems;
			try(Session s = HibernateManager.openSession()){
				List<IntelRecordObservationQuery> items =
						QueryFactory.buildQuery(s, IntelRecordObservationQuery.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
			
				proxyItems = items.stream().map(t->new QueryProxy(t.getName(), t.getUuid(), IntelRecordObservationQuery.KEY)).collect(Collectors.toList());
				
				List<IntelEntitySummaryQuery> items2 =
						QueryFactory.buildQuery(s, IntelEntitySummaryQuery.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
			
				proxyItems.addAll(items2.stream().map(t->new QueryProxy(t.getName(), t.getUuid(), IntelEntitySummaryQuery.KEY)).collect(Collectors.toList()));
				
				List<IntelEntityRecordQuery> items3 =
						QueryFactory.buildQuery(s, IntelEntityRecordQuery.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
			
				proxyItems.addAll(items3.stream().map(t->new QueryProxy(t.getName(), t.getUuid(), IntelEntityRecordQuery.KEY)).collect(Collectors.toList()));
			}

			proxyItems.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().syncExec(() ->{
				queryList.setInput(proxyItems);
			});
			
			return Status.OK_STATUS;
		}	
	};
	
	
	private class QueryViewerFilter extends ViewerFilter{

		private String filterString;
		
		public void setFilterString(String filterString){
			this.filterString = ".*" + filterString.toUpperCase() + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (filterString == null || filterString.isEmpty()) return true;
		
			QueryProxy in = (QueryProxy)element;
			if (in.getName().toUpperCase().matches(filterString)) return true;
			return false;
		}
		
	}
}