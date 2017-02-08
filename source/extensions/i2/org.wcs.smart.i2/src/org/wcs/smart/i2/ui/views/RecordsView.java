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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;


/**
 * View for displaying records 
 * @author Emily
 *
 */
public class RecordsView {

	public static final String ID = "org.wcs.smart.i2.ui.view.records"; //$NON-NLS-1$

	@Inject
	private IEclipseContext context;
	
	public RecordsView() {
		super();
	}

	private TableViewer lstInProgress;
	private TableViewer lstNewRecords;
	private TableViewer lstAllRecords;
	
	private RecordViewerFilter filter;
	private BasicRecordSearchPanel basicSearchPnl;
	
	
	private ISelectionChangedListener selectOne = new ISelectionChangedListener() {
		
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection().isEmpty()) return;
			
			for (TableViewer viewer : new TableViewer[]{lstInProgress, lstNewRecords, lstAllRecords}){
				if (event.getSelectionProvider() != viewer){
					viewer.setSelection(null);
				}
			}
			
		}
	};
	
	@PostConstruct
	public void createPartControl(final Composite parent) {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Composite thisParent = toolkit.createComposite(parent);
		thisParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
 		thisParent.setLayout(new GridLayout());
 		((GridLayout)thisParent.getLayout()).marginWidth = 0;
 		((GridLayout)thisParent.getLayout()).marginHeight = 0;
 		
 		
		IDoubleClickListener openListener = new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				if (x instanceof IntelRecord){
					(new OpenRecordHandler()).openRecord((IntelRecord)x, false);
				}else if (x instanceof RecordEditorInput){
					(new OpenRecordHandler()).openRecord((RecordEditorInput)x, false);
				}
				
			}
		};
		
		SectionTabHeader tabList = new SectionTabHeader(new String[]{"Unprocessed", "In Progress", "All", "Basic Search"}, thisParent, toolkit, thisParent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		((GridData)tabList.getLayoutData()).verticalIndent = 2;
		
		Composite tabPart = toolkit.createComposite(thisParent, SWT.NONE);
		tabPart.setLayout(new StackLayout());
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite newRecords = toolkit.createComposite(tabPart);
		newRecords.setLayout(new GridLayout());
		lstNewRecords = new TableViewer(newRecords, SWT.V_SCROLL | SWT.H_SCROLL| SWT.MULTI | SWT.BORDER);
		lstNewRecords.setContentProvider(ArrayContentProvider.getInstance());
		lstNewRecords.setLabelProvider(new RecordLabelProvider());
		lstNewRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstNewRecords.addDoubleClickListener(openListener);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		lstNewRecords.getControl().setLayoutData(gd);
		lstNewRecords.addSelectionChangedListener(selectOne);
		
		Composite inProgress = toolkit.createComposite(tabPart);
		inProgress.setLayout(new GridLayout());
		lstInProgress = new TableViewer(inProgress, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER);
		lstInProgress.setContentProvider(ArrayContentProvider.getInstance());
		lstInProgress.setLabelProvider(new RecordLabelProvider());
		lstInProgress.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstInProgress.addDoubleClickListener(openListener);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		lstInProgress.getControl().setLayoutData(gd);
		lstInProgress.addSelectionChangedListener(selectOne);
		
		Composite allRecords = toolkit.createComposite(tabPart);
		allRecords.setLayout(new GridLayout());
		((GridLayout)allRecords.getLayout()).marginWidth = 0;
 		((GridLayout)allRecords.getLayout()).marginHeight = 0;
 		
		Composite allRecordsSection = toolkit.createComposite(allRecords);
		allRecordsSection.setLayout(new GridLayout());
		allRecordsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(allRecordsSection, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (typeFilter.getPatternFilter() == null || typeFilter.getPatternFilter().isEmpty()){
					lstAllRecords.removeFilter(filter);
					filter = null;
				}else{
					if (filter == null){
						filter = new RecordViewerFilter();
						filter.setFilterString(typeFilter.getPatternFilter());
						lstAllRecords.addFilter(filter);
					}else{
						filter.setFilterString(typeFilter.getPatternFilter());
						lstAllRecords.refresh();
					}
				}
			}
		});
		
		lstAllRecords = new TableViewer(allRecordsSection, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		lstAllRecords.setContentProvider(ArrayContentProvider.getInstance());
		lstAllRecords.setLabelProvider(new RecordLabelProvider());
		lstAllRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstAllRecords.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAllRecords.addDoubleClickListener(openListener);
		lstAllRecords.addSelectionChangedListener(selectOne);
		
		Composite basicSearch = toolkit.createComposite(tabPart);
		basicSearch.setLayout(new GridLayout());
		((GridLayout)basicSearch.getLayout()).marginWidth = 0;
 		((GridLayout)basicSearch.getLayout()).marginHeight = 0;
		basicSearchPnl = new BasicRecordSearchPanel(basicSearch);
		basicSearchPnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tabList.setContent(new Composite[]{newRecords,inProgress,allRecords, basicSearch}, tabPart);
		tabList.selectTab(0);
		
		createMenu(lstAllRecords);
		createMenu(lstInProgress);
		createMenu(lstNewRecords);
		
		//add drag and drop support
		lstAllRecords.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(lstAllRecords.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = lstAllRecords.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		//add drag and drop support
		lstInProgress.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(lstInProgress.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = lstInProgress.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		lstNewRecords.addDragSupport(DND.DROP_LINK,new Transfer[]{IntelRecordSelectionTransfer.getTransfer()}, new DragSourceAdapter(){	
			@Override
			public void dragStart(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(lstNewRecords.getSelection());				
			}
			@Override
			public void dragSetData(DragSourceEvent event) {
				if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.dataType)) {
					event.data = lstNewRecords.getSelection();
				}
			}
			@Override
			public void dragFinished(DragSourceEvent event) {
				IntelRecordSelectionTransfer.getTransfer().setSelection(null);
			}
		});
		loadRecordsJob.schedule(0);
	}

	private void createMenu(Viewer control){
		Menu m = new Menu(control.getControl());
		control.getControl().setMenu(m);
	
		MenuItem mi = new MenuItem(m, SWT.PUSH);
		mi.setText("Open");
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				for (Iterator<?> iterator = ((IStructuredSelection)control.getSelection()).iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
				
					if (x instanceof IntelRecord){
						(new OpenRecordHandler()).openRecord((IntelRecord)x, false);
					}else if (x instanceof RecordEditorInput){
						(new OpenRecordHandler()).openRecord((RecordEditorInput)x, false);
					}
				}
			}
		});
		m.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mi.setEnabled(!control.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		
		if (IntelSecurityManager.INSTANCE.canDeleteRecord()){
			MenuItem miDelete = new MenuItem(m, SWT.PUSH);
			miDelete.setText("Delete");
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					List<Object> toDelete = new ArrayList<>();
					for (Iterator<?> iterator = ((IStructuredSelection)control.getSelection()).iterator(); iterator.hasNext();) {
						Object x = (Object) iterator.next();
						if (x instanceof IntelRecord || x instanceof RecordEditorInput){
							toDelete.add(x);
						}
					}
					if (MessageDialog.openConfirm(context.get(Shell.class), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected records?", toDelete.size()))){
						ProgressMonitorDialog pmd = new ProgressMonitorDialog(context.get(Shell.class));
						try {
							pmd.run(true, true, (monitor)-> RecordManager.INSTANCE.deleteRecords(toDelete, context,monitor));
						} catch (Exception ex) {
							Intelligence2PlugIn.displayLog("Error deleting records: " + ex.getMessage(), ex);
						}
						refreshView();
					}
				}
			});
			m.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					miDelete.setEnabled(!control.getSelection().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});
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
						if (x instanceof IntelRecord){
							WorkingSetManager.INSTANCE.addToActiveWorkingSet((IntelRecord) x, context);
						}else if (x instanceof RecordEditorInput){
							WorkingSetManager.INSTANCE.addToActiveWorkingSet((RecordEditorInput) x, context);
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
			
		
	}
	
	public void refreshView(){
		lstAllRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstInProgress.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstNewRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadRecordsJob.schedule();
	}

	
	@Inject
	@Optional
	private void recordModified(@UIEventTopic(IntelEvents.RECORD_ALL) IntelRecord record){
		loadRecordsJob.schedule();
	}

	@Inject
	@Optional
	private void recordSourcesModified(@UIEventTopic(IntelEvents.RECORD_SOURCE_ALL) Object value){
		loadRecordsJob.schedule();
		basicSearchPnl.refreshSource();
	}
	
	@Focus
	public void setFocus() {
		lstInProgress.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class RecordsViewWrapper extends DIViewPart<RecordsView>{
		public RecordsViewWrapper() {
			super(RecordsView.class);
		}
	}

	Job loadRecordsJob = new Job("Loading Intelligence Records"){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// -- load record source images --
			Session s = HibernateManager.openSession();
			final List<IntelRecordSource> sources = new ArrayList<>();
			try{
				sources.addAll(s.createCriteria(IntelRecordSource.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.list());
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					HashMap<IntelRecordSource, Image> srcImages = new HashMap<>();
					
					for (IntelRecordSource src : sources){
						try{
							if (src.getIconAsImage() != null){
								Image i = AWTSWTImageUtils.convertToSWTImage(src.getIconAsImage());
								srcImages.put(src, i);
							}
						}catch(Exception ex){
							Intelligence2PlugIn.log(ex.getMessage(),  ex);
						}
					}
					
					
					if (lstAllRecords.getControl().isDisposed()) return;
					((RecordLabelProvider)lstAllRecords.getLabelProvider()).setSourceImages(srcImages);
					((RecordLabelProvider)lstInProgress.getLabelProvider()).setSourceImages(srcImages);
					((RecordLabelProvider)lstNewRecords.getLabelProvider()).setSourceImages(srcImages);
					
				}
			});
			
			// -- load new and inprogress images --
			final List<IntelRecord> inProgress = new ArrayList<IntelRecord>();
			final List<IntelRecord> newRecords = new ArrayList<IntelRecord>();
			s = HibernateManager.openSession();
			try{
				List<IntelRecord> records = s.createCriteria(IntelRecord.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.add(Restrictions.or(
								Restrictions.eq("status", IntelRecord.Status.PROCESSING),
								Restrictions.eq("status", IntelRecord.Status.NEW)))
						.addOrder(Order.asc("dateCreated"))
						.list();
				for (IntelRecord r : records){
					if (r.getRecordSource() != null){
						r.getRecordSource().getIcon();
					}
					if (r.getStatus() == IntelRecord.Status.PROCESSING){
						inProgress.add(r);
					}else if (r.getStatus() == IntelRecord.Status.NEW){
						newRecords.add(r);
					}
				}
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (lstInProgress.getControl().isDisposed()) return;
					lstInProgress.setInput(inProgress);
					if (lstNewRecords.getControl().isDisposed()) return;
					lstNewRecords.setInput(newRecords);
				}
			});
			
			//--load all records --
			s = HibernateManager.openSession();
			final List<RecordEditorInput> allRecords = new ArrayList<RecordEditorInput>();
			try{
				Query q = s.createQuery("SELECT title, uuid, dateCreated, recordSource.uuid FROM IntelRecord WHERE conservationArea = :ca ORDER BY dateModified desc");
				q.setParameter("ca", SmartDB.getCurrentConservationArea());
				List<Object[]> items = q.list();
				for (Object[] item : items){
					allRecords.add(new RecordEditorInput((String)item[0], (UUID)item[1], (Date)item[2], (UUID)item[3]));
				}
			}finally{
				s.close();
			}
			allRecords.sort((x,y)->-1 * x.getDateCreated().compareTo(y.getDateCreated()));
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (lstAllRecords.getControl().isDisposed()) return;
					lstAllRecords.setInput(allRecords);
				}
			});
			
			

			
			return Status.OK_STATUS;
		}
		
	};
	
	class RecordViewerFilter extends ViewerFilter{

		private String filterString;
		
		public void setFilterString(String filterString){
			this.filterString = ".*" + filterString.toUpperCase() + ".*";
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (filterString == null || filterString.isEmpty()) return true;
		
			RecordEditorInput in = (RecordEditorInput)element;
			if (in.getName().toUpperCase().matches(filterString)) return true;
			if (DateFormat.getDateInstance().format(in.getDateCreated()).toUpperCase().matches(filterString)) return true;
			return false;
		}
		
	}
}