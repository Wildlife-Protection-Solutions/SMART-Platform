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
import java.util.ArrayList;
import java.util.Collections;
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
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.QueryManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.ui.handler.OpenQueryHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

public class QueryView {

	public static final String ID = "org.wcs.smart.i2.ui.view.queries";
	
	@Inject
	private IEclipseContext context;
	
	private QueryViewerFilter filter;
	private ListViewer viewer;
	
	public QueryView() {
		super();
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		toolkit.adapt(parent);
		
		FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
		toolkit.adapt(typeFilter);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (typeFilter.getPatternFilter() == null || typeFilter.getPatternFilter().isEmpty()){
					viewer.removeFilter(filter);
					filter = null;
				}else{
					if (filter == null){
						filter = new QueryViewerFilter();
						filter.setFilterString(typeFilter.getPatternFilter());
						viewer.addFilter(filter);
					}else{
						filter.setFilterString(typeFilter.getPatternFilter());
						viewer.refresh();
					}
				}
			}
		});
		
		viewer = new ListViewer(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof QueryProxy) return ((QueryProxy) element).getName();
				return super.getText(element);
			}
		});
		viewer.setInput(new String[]{DialogConstants.LOADING_TEXT});
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openSelection();
				
			}
		});
		//add drag support
		viewer.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelQuerySelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelQuerySelectionTransfer.getTransfer().setSelection(viewer.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelQuerySelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = viewer.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelQuerySelectionTransfer.getTransfer().setSelection(null);
			}
		});
		
		createMenu(viewer);
		
		refresh();
	}
	
	private void deleteSelection(){
		List<IntelRecordQuery> removed = new ArrayList<IntelRecordQuery>();
		for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof QueryProxy){
				IntelRecordQuery deletedItem = QueryManager.INSTANCE.deleteQuery(((QueryProxy) x).getUuid());
				if (deletedItem != null){
					removed.add(deletedItem);
				}
			}
		}
		context.get(IEventBroker.class).post(IntelEvents.QUERY_DELETED, removed);
	}
	private void openSelection(){
		for (Iterator<?> iterator = ((IStructuredSelection)viewer.getSelection()).iterator(); iterator.hasNext();) {
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
		 refresh();
	 }
	 
	 @Optional
	 @Inject
	 private void queryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) IntelRecordQuery data){
		 viewer.refresh(new QueryProxy(data.getName(), data.getUuid()));
	 }
	 @Optional
	 @Inject
	 private void multiQueryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) List<IntelRecordQuery> data){
		 data.forEach(i-> viewer.refresh(new QueryProxy(i.getName(), i.getUuid())));
	 }
	 
	 @Optional
	 @Inject
	 private void queryNew(@UIEventTopic(IntelEvents.QUERY_NEW) Object data){
		 refresh();
	 }
	 
	 @Optional
	 @Inject
	 private void queryDeleted(@UIEventTopic(IntelEvents.QUERY_DELETED) Object data){
		 refresh();
	 }
	 
	 public void refresh(){
		 viewer.setInput(new String[]{DialogConstants.LOADING_TEXT});
		 refreshQueriesJob.schedule();
	 }

	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class QueryViewWrapper extends DIViewPart<QueryView>{
		public QueryViewWrapper() {
			super(QueryView.class);
		}
	}

	
	Job refreshQueriesJob = new Job("refresh query list"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QueryProxy> proxyItems;
			Session s = HibernateManager.openSession();
			try{
				List<IntelRecordQuery> items = s.createCriteria(IntelRecordQuery.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.list();
				
				proxyItems = items.stream().map(t->new QueryProxy(t.getName(), t.getUuid())).collect(Collectors.toList());
			}finally{
				s.close();
			}

			proxyItems.sort((a,b)-> Collator.getInstance().compare(a.getName(), b.getName()));
			Display.getDefault().syncExec(() ->{
				viewer.setInput(proxyItems);
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

		@Override
		public Object getAdapter(Class adapter) {
			if (adapter.equals(IntelRecordQuery.class)){
				IntelRecordQuery q = new IntelRecordQuery();
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