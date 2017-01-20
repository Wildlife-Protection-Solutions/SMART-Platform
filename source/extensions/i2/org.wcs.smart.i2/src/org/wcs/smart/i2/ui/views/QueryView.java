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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
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
import org.hibernate.criterion.Restrictions;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.QueryManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.editors.query.IntelQueryEditor;
import org.wcs.smart.i2.ui.handler.OpenQueryHandler;
import org.wcs.smart.i2.ui.views.query.FilterTreeContentProvider;
import org.wcs.smart.i2.ui.views.query.FilterTreeItem;
import org.wcs.smart.i2.ui.views.query.FilterTreeLabelProvider;
import org.wcs.smart.i2.ui.views.query.LoadFilterOptions;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.E3Utils;

/**
 * View for listing all intelligence queries in the system
 *  
 * @author Emily
 *
 */
public class QueryView {

	public static final String REFRESHLABEL_KEY = "refreshlabel";

	public static final String ID = "org.wcs.smart.i2.ui.view.queries";
	
	@Inject
	private IEclipseContext context;
	
	private QueryViewerFilter queryFilter;
	private ListViewer queryList;
	
	private TreeViewer filterTree = null;
	private Job refreshJob;
	
	//query filter tree
	private Composite treePart;
	private Listener refreshListener = (event)->refreshFiltersView();
	
	private IAreaModifiedListener areaListener = new IAreaModifiedListener() {
		@Override
		public void areasUpdated(AreaType type) {
			sourceModified();
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
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{"Saved Queries", "Query Filters"}, parent, toolkit, parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
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
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
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
		
		queryList = new ListViewer(part, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		queryList.setContentProvider(ArrayContentProvider.getInstance());
		queryList.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof QueryProxy) return ((QueryProxy) element).getName();
				return super.getText(element);
			}
		});
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
			modifiedLabel.setText("Elements modified, click to refresh");
			
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
		filterTree.setContentProvider(new FilterTreeContentProvider());
		filterTree.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addFilterSelectionToQuery();
			}
		});
		treePart.addListener(SWT.Resize, e->{
			filterTree.getTree().setBounds(0,0,treePart.getBounds().width, treePart.getBounds().height);
		});

		Menu mnu = new Menu(filterTree.getTree());
		MenuItem addToQuery = new MenuItem(mnu,SWT.PUSH);
		addToQuery.setText("Add to Query");
		addToQuery.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addToQuery.addListener(SWT.Selection, event->addFilterSelectionToQuery());
		filterTree.getTree().setMenu(mnu);
		
		refreshJob = new LoadFilterOptions(filterTree);
		
		return part;
	}
	
	private void addFilterSelectionToQuery(){
		IntelQueryEditor addTo = null;
		EPartService pService = context.get(EPartService.class);
		for (MPart part : context.get(EPartService.class).getParts()){
			if (pService.isPartVisible(part)){
				Object item = E3Utils.getSourceObject(part);
				if (item instanceof IntelQueryEditor){
					addTo = (IntelQueryEditor) item;
					break;
				}
			}
		}
		
		IStructuredSelection selection = (IStructuredSelection) filterTree.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object element = (Object) iterator.next();
			if (element instanceof FilterTreeItem){
				DropItem[] di = ((FilterTreeItem) element).asDropItem();
				if (di == null) continue;
				addTo.addDropItems(di);
				
			}
			
		}
	}
	
	private void refreshFiltersView(){	
		filterTree.setInput(null);
		refreshJob.schedule();
	}
	
	private void deleteSelection(){
		int cnt = ((IStructuredSelection)queryList.getSelection()).size();
		if (!MessageDialog.openQuestion(context.get(Shell.class), "Delete Queries", MessageFormat.format("Are you sure you want to delete the {0} selected queries?  This action cannot be undone.", cnt))){
			return;
		}
		
		List<IntelRecordObservationQuery> removed = new ArrayList<IntelRecordObservationQuery>();
		for (Iterator<?> iterator = ((IStructuredSelection)queryList.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof QueryProxy){
				IntelRecordObservationQuery deletedItem = QueryManager.INSTANCE.deleteQuery(((QueryProxy) x).getUuid());
				if (deletedItem != null){
					removed.add(deletedItem);
				}
			}
		}
		context.get(IEventBroker.class).post(IntelEvents.QUERY_DELETED, removed);
	}
	private void openSelection(){
		for (Iterator<?> iterator = ((IStructuredSelection)queryList.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			//open query
			if (x instanceof QueryProxy){
				(new OpenQueryHandler()).openQuery(((QueryProxy) x).getUuid(), true);
			}
		}
	}
	private void createMenu(ListViewer control){
		Menu m = new Menu(control.getControl());
		control.getControl().setMenu(m);
	
		List<MenuItem> items = new ArrayList<MenuItem>();
		
		MenuItem miOpen = new MenuItem(m, SWT.PUSH);
		miOpen.setText("Open");
		miOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openSelection();
			}
		});
		items.add(miOpen);

		
		if (IntelSecurityManager.INSTANCE.canEditQuery()){
			MenuItem miDelete = new MenuItem(m, SWT.PUSH);
			miDelete.setText("Delete");
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteSelection();
				}
			});
			items.add(miOpen);

		}
		
		if (IntelSecurityManager.INSTANCE.canViewWorkingSets()){
			MenuItem miAdd = new MenuItem(m, SWT.PUSH);
			miAdd.setText("Add To Working Set");
			miAdd.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
			miAdd.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					for (Iterator<?> iterator = ((IStructuredSelection)control.getSelection()).iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();	
						if (x instanceof QueryProxy){
							WorkingSetManager.INSTANCE.addQueryToActiveWorkingSet(((QueryProxy) x).getUuid(), context);
						}
					}
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
	 private void dbModified(@UIEventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		 refreshQueryList();
		 refreshFiltersView();
	 }
	 
	 @Optional
	 @Inject
	 private void queryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) IntelRecordObservationQuery data){
		 queryList.refresh(new QueryProxy(data.getName(), data.getUuid()));
	 }
	 @Optional
	 @Inject
	 private void multiQueryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) List<IntelRecordObservationQuery> data){
		 data.forEach(i-> queryList.refresh(new QueryProxy(i.getName(), i.getUuid())));
	 }
	 
	@Optional
	@Inject
	private void queryNew(@UIEventTopic(IntelEvents.QUERY_NEW) Object data) {
		refreshQueryList();
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
		queryList.getList().setFocus();
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

	
	Job refreshQueriesJob = new Job("refresh query list"){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QueryProxy> proxyItems;
			Session s = HibernateManager.openSession();
			try{
				List<IntelRecordObservationQuery> items = s.createCriteria(IntelRecordObservationQuery.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
				
				proxyItems = items.stream().map(t->new QueryProxy(t.getName(), t.getUuid())).collect(Collectors.toList());
			}finally{
				s.close();
			}

			proxyItems.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().syncExec(() ->{
				queryList.setInput(proxyItems);
			});
			
			return Status.OK_STATUS;
		}
		
	};
	
	private class QueryProxy implements IAdaptable{
		private String name;
		private UUID uuid;
		public QueryProxy(String name, UUID uuid){
			this.name = name;
			this.uuid = uuid;
		}
		
		public String getName(){
			return this.name;
		}
		public UUID getUuid(){
			return this.uuid;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter.equals(IntelRecordObservationQuery.class)){
				IntelRecordObservationQuery q = new IntelRecordObservationQuery();
				q.setUuid(getUuid());
				return q;
			}
			return null;
		}
		
		@Override
		public boolean equals(Object other){
			if (other == null) return false;
			if (other == this) return true;
			if (getClass() != other.getClass()) return false;
			return Objects.equals(uuid, ((QueryProxy)other).uuid);
		}
		
		@Override
		public int hashCode(){
			return Objects.hash(uuid);
		}
	}
	
	private class QueryViewerFilter extends ViewerFilter{

		private String filterString;
		
		public void setFilterString(String filterString){
			this.filterString = ".*" + filterString.toUpperCase() + ".*";
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