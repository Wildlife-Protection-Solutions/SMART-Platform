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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RecordManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

public class RecordsView {

	public static final String ID = "org.wcs.smart.i2.ui.view.records"; //$NON-NLS-1$

	@Inject
	private IEclipseContext context;
	
	public RecordsView() {
		super();
	}

	private ListViewer lstInProgress;
	private ListViewer lstNewRecords;
	private ListViewer lstAllRecords;
	
	private RecordViewerFilter filter;
	
	private ISelectionChangedListener selectOne = new ISelectionChangedListener() {
		
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			if (event.getSelection().isEmpty()) return;
			
			for (ListViewer viewer : new ListViewer[]{lstInProgress, lstNewRecords, lstAllRecords}){
				if (event.getSelectionProvider() != viewer){
					viewer.setSelection(null);
				}
			}
			
		}
	};
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Composite thisParent = toolkit.createComposite(parent);
		thisParent.setLayout(new GridLayout());
		thisParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
//		ToolBar toolbar = new ToolBar(thisParent, SWT.HORIZONTAL | SWT.FLAT);
//		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
//		
//		ToolItem newItem = new ToolItem(toolbar, SWT.PUSH);
//		newItem.setToolTipText("create new record");
//		newItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD_NEW));
//		newItem.addSelectionListener(new SelectionAdapter(){
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				ContextInjectionFactory.invoke(new NewRecordHandler(), Execute.class, context.createChild());
//			}
//		});
//		
//		ToolItem refreshItem = new ToolItem(toolbar, SWT.PUSH);
//		refreshItem.setToolTipText("refresh list");
//		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
//		refreshItem.addSelectionListener(new SelectionAdapter(){
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				loadRecordsJob.schedule();
//			}
//		});
//		
		
		
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
		
		Section inProgress = toolkit.createSection(thisParent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		inProgress.setText("In Progress");
		inProgress.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		inProgress.setLayout(new GridLayout());
		inProgress.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				((GridData)inProgress.getLayoutData()).grabExcessVerticalSpace = e.getState();
				thisParent.layout();
			}
		});
		lstInProgress = new ListViewer(inProgress, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		inProgress.setClient(lstInProgress.getControl());
		lstInProgress.setContentProvider(ArrayContentProvider.getInstance());
		lstInProgress.setLabelProvider(new RecordLabelProvider());
		lstInProgress.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstInProgress.addDoubleClickListener(openListener);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		lstInProgress.getControl().setLayoutData(gd);
		lstInProgress.addSelectionChangedListener(selectOne);
		
		Section newRecords = toolkit.createSection(thisParent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		newRecords.setText("New Records");
		newRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		newRecords.setLayout(new GridLayout());
		newRecords.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				((GridData)newRecords.getLayoutData()).grabExcessVerticalSpace = e.getState();
				thisParent.layout();
			}
		});

		lstNewRecords = new ListViewer(newRecords, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		newRecords.setClient(lstNewRecords.getControl());
		lstNewRecords.setContentProvider(ArrayContentProvider.getInstance());
		lstNewRecords.setLabelProvider(new RecordLabelProvider());
		lstNewRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstNewRecords.addDoubleClickListener(openListener);
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		lstNewRecords.getControl().setLayoutData(gd);
		lstNewRecords.addSelectionChangedListener(selectOne);
		
		Section allRecords = toolkit.createSection(thisParent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		allRecords.setText("All Records");
		allRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		allRecords.setLayout(new GridLayout());
		allRecords.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				((GridData)allRecords.getLayoutData()).grabExcessVerticalSpace = e.getState();
				thisParent.layout();
			}
		});

		Composite allRecordsSection = toolkit.createComposite(allRecords);
		allRecords.setClient(allRecordsSection);
		allRecordsSection.setLayout(new GridLayout());
		
		FilterComposite typeFilter = new FilterComposite(allRecordsSection, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
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
		
		lstAllRecords = new ListViewer(allRecordsSection, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		lstAllRecords.setContentProvider(ArrayContentProvider.getInstance());
		lstAllRecords.setLabelProvider(new RecordLabelProvider());
		lstAllRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstAllRecords.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstAllRecords.addDoubleClickListener(openListener);
		lstAllRecords.addSelectionChangedListener(selectOne);
		
		createMenu(lstAllRecords);
		createMenu(lstInProgress);
		createMenu(lstNewRecords);
		
		loadRecordsJob.schedule(0);
	}

	private void createMenu(ListViewer control){
		Menu m = new Menu(control.getControl());
		control.getControl().setMenu(m);
	
		MenuItem mi = new MenuItem(m, SWT.PUSH);
		mi.setText("Open");
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object x = ((IStructuredSelection)control.getSelection()).getFirstElement();
				if (x instanceof IntelRecord){
					(new OpenRecordHandler()).openRecord((IntelRecord)x, false);
				}else if (x instanceof RecordEditorInput){
					(new OpenRecordHandler()).openRecord((RecordEditorInput)x, false);
				}
			}
		});
		
		MenuItem miDelete = new MenuItem(m, SWT.PUSH);
		miDelete.setText("Delete");
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object x = ((IStructuredSelection)control.getSelection()).getFirstElement();
				if (x instanceof IntelRecord){
					RecordManager.INSTANCE.deleteRecord((IntelRecord)x, context);
				}else if (x instanceof RecordEditorInput){
					RecordManager.INSTANCE.deleteRecord(((RecordEditorInput)x).getUuid(), context);
				}
			}
		});
		
		MenuItem miAdd = new MenuItem(m, SWT.PUSH);
		miAdd.setText("Add To Working Set");
		miAdd.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
		miAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object x = ((IStructuredSelection)control.getSelection()).getFirstElement();
				if (x instanceof IntelRecord){
					WorkingSetManager.INSTANCE.addToActiveWorkingSet((IntelRecord) x, context);
				}else if (x instanceof RecordEditorInput){
					WorkingSetManager.INSTANCE.addToActiveWorkingSet((RecordEditorInput) x, context);
				}
			}
		});
			
		m.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				miDelete.setEnabled(!control.getSelection().isEmpty());
				mi.setEnabled(!control.getSelection().isEmpty());
				miAdd.setEnabled(!control.getSelection().isEmpty() && WorkingSetManager.INSTANCE.isSet());
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
	}
	
	public void refreshView(){
		loadRecordsJob.schedule();
	}

	
	@Inject
	@Optional
	private void recordModified(@UIEventTopic(IntelEvents.RECORD_ALL) IntelRecord record){
		loadRecordsJob.schedule();
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
			final List<IntelRecord> inProgress = new ArrayList<IntelRecord>();
			Session s = HibernateManager.openSession();
			try{
				inProgress.addAll(s.createCriteria(IntelRecord.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.add(Restrictions.eq("status", IntelRecord.Status.PROCESSING))
						.list());
			}finally{
				s.close();
			}
			inProgress.sort((x,y)->x.getDateCreated().compareTo(y.getDateCreated()));
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					lstInProgress.setInput(inProgress);
				}
			});
			
			final List<IntelRecord> newRecords = new ArrayList<IntelRecord>();
			s = HibernateManager.openSession();
			try{
				newRecords.addAll(s.createCriteria(IntelRecord.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.add(Restrictions.eq("status", IntelRecord.Status.NEW))
						.list());
			}finally{
				s.close();
			}
			newRecords.sort((x,y)->x.getDateCreated().compareTo(y.getDateCreated()));
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					lstNewRecords.setInput(newRecords);
				}
			});
			
			s = HibernateManager.openSession();
			final List<RecordEditorInput> allRecords = new ArrayList<RecordEditorInput>();
			try{
				List<Object[]> items = s.createQuery("SELECT title, uuid, dateCreated FROM IntelRecord order by dateModified desc").list();
				for (Object[] item : items){
					allRecords.add(new RecordEditorInput((String)item[0], (UUID)item[1], (Date)item[2]));
				}
			}finally{
				s.close();
			}
			allRecords.sort((x,y)->-1 * x.getDateCreated().compareTo(y.getDateCreated()));
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
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