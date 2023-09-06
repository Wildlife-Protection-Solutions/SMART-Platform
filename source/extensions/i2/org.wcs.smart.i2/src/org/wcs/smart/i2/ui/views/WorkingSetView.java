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

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetDataExporter;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.WorkingSetManager.LayerStatus;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.HiddenWorkingSetItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetItem;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.query.QueryManager;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.TransparentInfoDialog;
import org.wcs.smart.i2.ui.WorkingSetExportDialog;
import org.wcs.smart.i2.ui.WorkingSetLabelProvider;
import org.wcs.smart.i2.ui.dialogs.WorkingSetListDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.handler.OpenQueryHandler;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Working set view.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class WorkingSetView {
	
	public static final String ID = "org.wcs.smart.i2.ui.view.workingset"; //$NON-NLS-1$
	
	/**
	 * The preference store key for the last loaded working set.  The make conservation area specific
	 * need to append the uuid for the current conservation area to the end before loading
	 * or storing the preference.
	 * LAST_WS_PREFERENCE + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid())
	 * 
	 */
	public static final String LAST_WS_PREFERENCE = "org.wcs.smart.i2.workingset.uuid."; //$NON-NLS-1$
	@Inject
	private IEclipseContext context;

	@Inject
	private Shell activeShell;
	
	private Label lblWorkingSet;
	private ToolItem exportItem;
	private ToolItem copyItem;
	private ToolItem newItem;
	private ToolItem selectItem;
	private DateFilterDropDownComposite dateComp;
	private boolean isInitializing = false;
	
	private CheckboxTreeViewer workingsetTree;
	private LoadWorkingSetJob loadWorkingSetJob = new LoadWorkingSetJob();
	private boolean isConfigureVisibility = false;
	
	public WorkingSetView() {
		super();
		loadWorkingSetJob.setSystem(true);
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 2;
		((GridLayout)parent.getLayout()).marginHeight = 2;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite core = toolkit.createComposite(parent);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = toolkit.createComposite(core);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		((GridLayout)header.getLayout()).horizontalSpacing = 0;
		
		lblWorkingSet = toolkit.createLabel(header, ""); //$NON-NLS-1$
		lblWorkingSet.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		FontData fd = lblWorkingSet.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight()+1);
		Font f = new Font(lblWorkingSet.getDisplay(), fd);
		lblWorkingSet.addListener(SWT.Dispose,(e)->f.dispose());
		lblWorkingSet.setFont(f);
		lblWorkingSet.setText(Messages.WorkingSetView_NotSelectedLabel);
		
		ToolBar tools = new ToolBar(header, SWT.FLAT);
		
		exportItem = new ToolItem(tools, SWT.PUSH);
		exportItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EXPORT_ICON));
		exportItem.setToolTipText(Messages.WorkingSetView_ExportTooltip);
		exportItem.addListener(SWT.Selection,e->exportWorkingSet());
		exportItem.setEnabled(false);
		
		if (IntelSecurityManager.INSTANCE.canEditWorkingSet()) {
			copyItem = new ToolItem(tools, SWT.PUSH);
			copyItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_COPY));
			copyItem.setToolTipText(Messages.WorkingSetView_copyTooltip);
			copyItem.addListener(SWT.Selection, e->copyWorkingSet());	
			copyItem.setEnabled(false);
			
			newItem = new ToolItem(tools, SWT.PUSH);
			newItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			newItem.setToolTipText(Messages.WorkingSetView_createnewTooltip);
			newItem.addListener(SWT.Selection, e->newWorkingSet());	
		}
		
		selectItem = new ToolItem(tools, SWT.PUSH);
		selectItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_SELECT));
		selectItem.setToolTipText(Messages.WorkingSetView_selectTooltip);
		selectItem.addListener(SWT.Selection, e->selectWorkingSet());	

		
		DateFilterComposite.DateFilter[] defaultFilters = new DateFilter[]{
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.LAST_1_YEARS,
				DateFilter.LAST_5_YEARS,
				DateFilter.ALL,
				DateFilter.CUSTOM
		};
		DateFilterComposite.DateFilter initialDateFilter = DateFilter.LAST_1_YEARS;
		dateComp = new DateFilterDropDownComposite(core, defaultFilters, initialDateFilter);
		toolkit.adapt(dateComp);
		((GridLayout)dateComp.getLayout()).marginWidth = 5;
		dateComp.setEnabled(false);
		dateComp.addChangeListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (isInitializing) return;
				if (WorkingSetManager.INSTANCE.getActiveWorkingSet() == null) return;
				
				LocalDate[] dFilters = getDateFilters();		
				
				String dateFilter = dateComp.getDateFilter().name();
				if (dateComp.getDateFilter() == DateFilter.CUSTOM){
					dateFilter += ":" + DateTimeFormatter.ISO_LOCAL_DATE.format(dFilters[0]) + ":" + DateTimeFormatter.ISO_LOCAL_DATE.format(dFilters[1]); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				//save date filter
				try(Session s = HibernateManager.openSession()){
					s.beginTransaction();
					try{
						IntelWorkingSet ws = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
						ws.setEntityDateFilter(dateFilter);
						s.getTransaction().commit();
					}catch (Exception ex){
						Intelligence2PlugIn.displayLog(Messages.WorkingSetView_SaveDateError, ex);
						s.getTransaction().rollback();
					}
				}
				
				context.get(IEventBroker.class).send(IntelEvents.ACTIVE_WS_LAYER_DATEFILTER, dFilters);
			}
		});
		
		workingsetTree = new CheckboxTreeViewer(core, SWT.FULL_SELECTION | SWT.MULTI);
		workingsetTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(workingsetTree.getTree());
		workingsetTree.setLabelProvider(new WorkingSetLabelProvider());
		workingsetTree.setContentProvider(new WorkingSetTreeContentProvider());
		workingsetTree.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openSelection();
			}
		});
		
		workingsetTree.addCheckStateListener(new ICheckStateListener() {
			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (isConfigureVisibility) return;
				isConfigureVisibility = true;
				try{
					
					LayerVisibleEvent newEvent = new LayerVisibleEvent();
					if (event.getElement() instanceof HiddenWorkingSetItem) return;
					
					if ( event.getElement() instanceof IntelWorkingSetCategory){
						Object[] kids = ((WorkingSetTreeContentProvider)workingsetTree.getContentProvider()).getChildren(event.getElement());
						if (kids != null){
							for (Object x : kids){
								if (x instanceof HiddenWorkingSetItem) continue;
								workingsetTree.setChecked(x, event.getChecked());
								workingsetTree.setGrayed(x, false);
								if(event.getChecked()){
									newEvent.allVisible.add(((IntelWorkingSetItem)x).getUuid());
								}else{
									newEvent.notVisible.add(((IntelWorkingSetItem)x).getUuid());
								}
							}
						}
						workingsetTree.setGrayed(event.getElement(), false);
					}else if (event.getElement() instanceof IntelWorkingSetItem){
						workingsetTree.setGrayed(event.getElement(), false);
						if(event.getChecked()){
							newEvent.allVisible.add(((IntelWorkingSetItem)event.getElement()).getUuid());
						}else{
							newEvent.notVisible.add(((IntelWorkingSetItem)event.getElement()).getUuid());
						}
						Object parent = ((WorkingSetTreeContentProvider)workingsetTree.getContentProvider()).getParent(event.getElement());
						Object[] kids = ((WorkingSetTreeContentProvider)workingsetTree.getContentProvider()).getChildren(parent);
						if (kids.length > 0){
							
							boolean last = workingsetTree.getChecked(kids[0]);
							workingsetTree.setChecked(parent, last);
							workingsetTree.setGrayed(parent, false);
							for (int i = 1; i < kids.length; i ++){
								if (last != workingsetTree.getChecked(kids[i])){
									workingsetTree.setChecked(parent, true);
									workingsetTree.setGrayed(parent, true);
									break;
								}
							}
						}
					}
					
					if (newEvent.allVisible.isEmpty() && newEvent.notVisible.isEmpty()) return;
					
					//update working set
					try(Session s = HibernateManager.openSession()){
						s.beginTransaction();
						try {
							IntelWorkingSet ws = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
							
							for (UUID uuid : newEvent.allVisible){
								boolean found = false;
								for (IntelWorkingSetEntity layer : ws.getEntities()){
									if (uuid.equals(layer.getEntity().getUuid())){
										layer.setIsVisible(true);
										found = true;
										break;
									}
								}
								if (found) break;
								for (IntelWorkingSetRecord layer : ws.getRecords()){
									if (uuid.equals(layer.getRecord().getUuid())){
										layer.setIsVisible(true);
										found = true;
										break;
									}
								}
								if (found) break;
								for (IntelWorkingSetQuery layer : ws.getQueries()){
									if (uuid.equals(layer.getQuery())){
										layer.setIsVisible(true);
										break;
									}
								}
							};
	
							for (UUID uuid : newEvent.notVisible){
								boolean found = false;
								for (IntelWorkingSetEntity layer : ws.getEntities()){
									if (uuid.equals(layer.getEntity().getUuid())){
										layer.setIsVisible(false);
										found = true;
										break;
									}
								}
								if (found) break;
								for (IntelWorkingSetRecord layer : ws.getRecords()){
									if (uuid.equals(layer.getRecord().getUuid())){
										layer.setIsVisible(false);
										found = true;
										break;
									}
								}
								if (found) break;
								for (IntelWorkingSetQuery layer : ws.getQueries()){
									if (uuid.equals(layer.getQuery())){
										layer.setIsVisible(false);
										break;
									}
								}
							};
							s.getTransaction().commit();
						}catch(Exception ex){
							s.getTransaction().rollback();
							Intelligence2PlugIn.log(Messages.WorkingSetView_SaveVisibilityError + ex.getMessage(), ex);
						}
					}
					context.get(IEventBroker.class).send(IntelEvents.ACTIVE_WS_LAYER_VISIBILITY, newEvent);

				}finally{
					isConfigureVisibility = false;
				}
			}
		});

		
		Menu menu = new Menu(workingsetTree.getControl());
		workingsetTree.getControl().setMenu(menu);
		
		MenuItem open = new MenuItem(menu, SWT.PUSH);
		open.setText(Messages.WorkingSetView_OpenMenuItem);
		open.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openSelection();
			}
		});
		
		MenuItem delete = null;
		if (IntelSecurityManager.INSTANCE.canEditWorkingSet()) {
			delete = new MenuItem(menu, SWT.PUSH);
			delete.setText(Messages.WorkingSetView_RemoveLabel);
			delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			delete.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					removeSelection();
				}
			});
		}
		MenuItem fdelete = delete;
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				boolean enabled = false;
				IStructuredSelection selection = (IStructuredSelection) workingsetTree.getSelection();
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object x = (Object) iterator.next();
					if (x instanceof IntelWorkingSetItem){
						enabled = true;
						break;
					}
				}
				if (fdelete != null) fdelete.setEnabled(enabled);
				open.setEnabled(enabled);
			}
		});
		addDropListener(parent);
		
		if (WorkingSetManager.INSTANCE.getActiveWorkingSet() != null) {
			IntelWorkingSet temp =  new IntelWorkingSet();
			temp.setUuid(WorkingSetManager.INSTANCE.getActiveWorkingSet());
			setWorkingSet(temp);
		}
	}
	
	/**
	 * Updates the working set with the date selected in the date
	 * filter.  Returns the new dates.
	 * @return
	 */
	private LocalDate[] getDateFilters(){
		LocalDate[] dFilters = new LocalDate[2];
		if (dateComp.getDateFilter() == DateFilter.CUSTOM){
			dFilters[0] = dateComp.getCustomStartDate();
			dFilters[1] = dateComp.getCustomEndDate();
		}else{
			dFilters[0] = dateComp.getDateFilter().getStartDate();
			dFilters[1] = dateComp.getDateFilter().getEndDate();
		}
		


		return dFilters;
	}
	
	private void addDropListener(Composite parent){
		if (!IntelSecurityManager.INSTANCE.canEditWorkingSet()) return;
		DropTarget dropTarget = new DropTarget(parent, DND.DROP_LINK);
		dropTarget.setTransfer(new Transfer[]{IntelEntitySelectionTransfer.getTransfer(), IntelRecordSelectionTransfer.getTransfer(), IntelQuerySelectionTransfer.getTransfer()});
		dropTarget.addDropListener(new DropTargetListener() {		
			private PaintListener paintListener = new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.setLineWidth(2);
					e.gc.drawRectangle(0, 0, e.width, e.height);
				}
			};
			
			@Override
			public void dropAccept(DropTargetEvent event) {
			}
			
			@Override
			public void drop(DropTargetEvent event) {
				ISelection s = null;
				if (IntelEntitySelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					s = IntelEntitySelectionTransfer.getTransfer().getSelection();
				}else if (IntelRecordSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
					s = IntelRecordSelectionTransfer.getTransfer().getSelection();
				}else if (IntelQuerySelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
					s = IntelQuerySelectionTransfer.getTransfer().getSelection();
				}
				if (s != null && s instanceof IStructuredSelection) {
					IStructuredSelection sel = (IStructuredSelection)s;
					List<RecordEditorInput> records = new ArrayList<>();
					List<IntelEntity> entities = new ArrayList<>();
					List<UUID> queries = new ArrayList<>();
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						Object element = (Object)iterator.next();
						
						if (element instanceof IAdaptable){
							Object tmp = ((IAdaptable)element).getAdapter(IntelRecord.class);
							if (tmp == null){
								tmp = ((IAdaptable)element).getAdapter(RecordEditorInput.class);
							}
							if (tmp == null){
								tmp = ((IAdaptable)element).getAdapter(IntelEntity.class);
							}
							if (tmp == null){
								tmp = ((IAdaptable)element).getAdapter(IntelRecordObservationQuery.class);
							}
							if (tmp != null){
								element = tmp;
							}
						}
						
						if (element instanceof IntelRecord){
							records.add(new RecordEditorInput((IntelRecord) element));
							
						}else if (element instanceof RecordEditorInput){
							records.add((RecordEditorInput) element);
						}else if (element instanceof IntelEntity){
							entities.add((IntelEntity) element);
									
						}else if (element instanceof IntelRecordObservationQuery){
							queries.add(((IntelRecordObservationQuery)element).getUuid());
						}else {
							TransparentInfoDialog ti = new TransparentInfoDialog(WorkingSetView.this.activeShell, Messages.WorkingSetView_CannotAddItemNotMappable);
							ti.open();
						}
						
					}
					WorkingSetManager.INSTANCE.addRecordInputToActiveWorkingSetRecord(records, context);
					WorkingSetManager.INSTANCE.addEntityToActiveWorkingSet(entities, context);
					WorkingSetManager.INSTANCE.addQueryUuidToActiveWorkingSet(queries, context);
				}
				
				parent.removePaintListener(paintListener);
				parent.redraw();
			}
			
			@Override
			public void dragOver(DropTargetEvent event) {
				 
			}
			
			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				event.detail = DND.DROP_LINK;
			}
			
			@Override
			public void dragLeave(DropTargetEvent event) {
				parent.removePaintListener(paintListener);
				parent.redraw();
			}
			
			@Override
			public void dragEnter(DropTargetEvent event) {
				event.detail = DND.DROP_LINK;
				parent.addPaintListener(paintListener);
				parent.redraw();
			}
		});
	}
	private void removeSelection(){
		IStructuredSelection selection = (IStructuredSelection) workingsetTree.getSelection();
		List<IntelWorkingSetItem> toDeleteItems = new ArrayList<IntelWorkingSetItem>();
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelWorkingSetItem){
				toDeleteItems.add((IntelWorkingSetItem) x);
			}
		}
		List<IntelRecord> records = new ArrayList<>();
		List<IntelEntity> entities = new ArrayList<>();
		List<AbstractIntelQuery> queries = new ArrayList<>();
		for (IntelWorkingSetItem toDelete: toDeleteItems){
			if (toDelete.getCategory() == IntelWorkingSetCategory.ENTITY){
				try(Session s = HibernateManager.openSession()){
					IntelEntity i = (IntelEntity) s.get(IntelEntity.class, toDelete.getUuid());
					i.getIdAttributeAsText();
					entities.add(i);
				}
			}else if (toDelete.getCategory() == IntelWorkingSetCategory.RECORD){
				try(Session s = HibernateManager.openSession()){
					IntelRecord i = (IntelRecord) s.get(IntelRecord.class, toDelete.getUuid());
					records.add(i);
				}
			}else if (toDelete.getCategory() == IntelWorkingSetCategory.QUERIES){
				try(Session s = HibernateManager.openSession()){
					AbstractIntelQuery i = QueryManager.INSTANCE.findQuery(s, toDelete.getUuid(), toDelete.getQueryTypeKey());
					queries.add(i);
				}	
			}
		}
		WorkingSetManager.INSTANCE.removeRecordFromWorkingSet(records, context);
		WorkingSetManager.INSTANCE.removeEntityFromWorkingSet(entities, context);
		WorkingSetManager.INSTANCE.removeQueryFromWorkingSet(queries, context);
	}


	private void openSelection(){
		IStructuredSelection selection = (IStructuredSelection) workingsetTree.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof IntelWorkingSetItem){
				IntelWorkingSetItem toOpen = (IntelWorkingSetItem)x;
				
				if (toOpen.getCategory() == IntelWorkingSetCategory.ENTITY){
					IntelEntity i = null;
					try(Session s = HibernateManager.openSession()){
						i = (IntelEntity) s.get(IntelEntity.class, toOpen.getUuid());
						i.getIdAttributeAsText();
					}
					(new OpenEntityHandler()).openEntity(i, context);
				}else if (toOpen.getCategory() == IntelWorkingSetCategory.RECORD){
					IntelRecord i = null;
					try(Session s = HibernateManager.openSession()){
						i = (IntelRecord) s.get(IntelRecord.class, toOpen.getUuid());
					}
					(new OpenRecordHandler()).openRecord(i, false);
				}else if (toOpen.getCategory() == IntelWorkingSetCategory.QUERIES){
					(new OpenQueryHandler()).openQuery(toOpen.getUuid(), toOpen.getQueryTypeKey(), false);
				}
				return;
			}
			
		}
	}
	
	private void newWorkingSet(){
		IntelWorkingSet newWorkingSet = createWorkingSet(activeShell);
		if (newWorkingSet != null){
			context.get(IEventBroker.class).send(IntelEvents.WS_NEW, newWorkingSet);
			setWorkingSet(newWorkingSet, context);
		}
	}
	
	private void exportWorkingSet(){
		UUID setUuid = WorkingSetManager.INSTANCE.getActiveWorkingSet();
	
		IntelWorkingSet set = null;
		try(Session session = HibernateManager.openSession()){
			set = session.get(IntelWorkingSet.class, setUuid);
		}
		if (set == null) return;
		
		WorkingSetExportDialog wdialog = new WorkingSetExportDialog(activeShell, set);
		if (wdialog.open() != Window.OK) return;
		
		
		Path output = Paths.get(wdialog.getFilename());
		if (Files.exists(output)) {
			if (!MessageDialog.openQuestion(activeShell, Messages.WorkingSetView_FileExistsTitle, MessageFormat.format(Messages.WorkingSetView_FileExistsMsg, output.toString()))) {
				return;
			}
		}
		
		char delimiter = wdialog.getDelimiter();
		Charset charset = wdialog.getCharset();
		Projection prj = wdialog.getProjection();
		try {
			prj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(prj.getDefinition()));
		}catch (Exception ex) {
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
			return;
		}
		final IntelWorkingSet fset = set;
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(activeShell);
		try {
			dialog.run(true, true,  new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						WorkingSetDataExporter.INSTANCE.export(fset, output, delimiter, charset, prj, monitor);
					}catch (Exception ex) {
						throw new InvocationTargetException(ex);
					}
				}
			});
		}catch (Exception ex) {
			Intelligence2PlugIn.displayLog(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(),  ex);
			return;
		}
		MessageDialog.openInformation(activeShell, Messages.WorkingSetView_CompleteTitle, MessageFormat.format(Messages.WorkingSetView_CompleteMsg, output.toString()));
	}
	
	private void copyWorkingSet(){
		IntelWorkingSet set = null;
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				set = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
				String newName = WorkingSetView.getWorkingsetName(activeShell, MessageFormat.format(Messages.WorkingSetView_DefaultWsName, set.getName()));
				if (newName != null){
					set = WorkingSetManager.INSTANCE.clone(set);
					set.setName(newName);
					set.updateName(SmartDB.getCurrentLanguage(), newName);
					set.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), newName);
					s.persist(set);
					s.getTransaction().commit();
				}else{
					set = null;
					s.getTransaction().rollback();
				}
				
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.WorkingSetView_DeleteError, lblWorkingSet.getText(), ex.getMessage()), ex);
				return;
			}
		}
		if (set != null){
			context.get(IEventBroker.class).send(IntelEvents.WS_NEW, set);
			setWorkingSet(set, context);
		}
		
	}
	
	@Optional
	@Inject
	private void activeProfilesChanged(@UIEventTopic(IntelEvents.PROFILES_ALL) Object data){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void workingSetLayerVisibilityModified(@UIEventTopic(IntelEvents.ACTIVE_WS_LAYER_VISIBILITY) LayerVisibleEvent event){
		if (isConfigureVisibility) return;
		try{
			isConfigureVisibility = true;
			
			for (UUID uuid : event.allVisible){
				IntelWorkingSetItem i = new IntelWorkingSetItem(uuid, true);
				workingsetTree.setChecked(i, true);
				workingsetTree.setGrayed(i, false);
			}
			for (UUID uuid : event.partVisible){
				IntelWorkingSetItem i = new IntelWorkingSetItem(uuid, false);
				workingsetTree.setChecked(i, true);
				workingsetTree.setGrayed(i, true);
			}
			for (UUID uuid : event.notVisible){
				IntelWorkingSetItem i = new IntelWorkingSetItem(uuid, false);
				workingsetTree.setChecked(i, false);
				workingsetTree.setGrayed(i, false);
			}
			
		}finally{
			isConfigureVisibility = false;
		}
		
	}
	
	@Inject
	@Optional
	private void workingSetModified(@UIEventTopic(IntelEvents.WS_MODIFIED) IntelWorkingSet workingSet){
		if (workingSet.getUuid().equals(WorkingSetManager.INSTANCE.getActiveWorkingSet())){
			setWorkingSet(workingSet);
		}
	}
	
	@Inject
	@Optional
	private void workingSetSet(@UIEventTopic(IntelEvents.ACTIVE_WS_SET) IntelWorkingSet activeWorkingSet){
		setWorkingSet(activeWorkingSet);
	}
	
	@Inject
	@Optional
	private void workingSetDelete(@UIEventTopic(IntelEvents.WS_DELETE) IntelWorkingSet set){
		if (set.getUuid().equals(WorkingSetManager.INSTANCE.getActiveWorkingSet())){
			setWorkingSet(null, context);
		}
	}
	
	@Inject
	@Optional
	private void entityRemoved(@UIEventTopic(IntelEvents.ENTITY_DELETE) IntelEntity e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void entityRemoved(@UIEventTopic(IntelEvents.ENTITY_DELETE) Collection<IntelEntity> e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_MODIFIED) IntelEntity e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_MODIFIED) Collection<IntelEntity> e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void recordRemoved(@UIEventTopic(IntelEvents.RECORD_DELETE) Object payload){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void recordModified(@UIEventTopic(IntelEvents.RECORD_MODIFIED) IntelRecord e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void queryDeleted(@UIEventTopic(IntelEvents.QUERY_DELETED) Object query){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void queryModified(@UIEventTopic(IntelEvents.QUERY_MODIFIED) Object query){
		refreshWithDelay();
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		refreshWithDelay();
	}
	
	private void refreshWithDelay(){
		loadWorkingSetJob.schedule(250);
	}
	
	private void selectWorkingSet(){
		WorkingSetListDialog dialog = ContextInjectionFactory.make(WorkingSetListDialog.class, context);
		if ( dialog.open() == Window.OK ){
			IntelWorkingSet selection = dialog.getSelection();
			setWorkingSet(selection, context);
		}
	}
	
	private void setWorkingSet(IntelWorkingSet ws, IEclipseContext context){
		try{
			WorkingSetManager.INSTANCE.setActiveWorkingSet(ws, context);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}
	}
	
	private void setWorkingSet(IntelWorkingSet set){	
		if (copyItem != null) copyItem.setEnabled(set != null);
		if (exportItem != null) exportItem.setEnabled(set != null);
		
		loadWorkingSetJob.setWorkingSetUuid(set == null ? null : set.getUuid());
		loadWorkingSetJob.schedule();
	}
	
	@Focus
	public void setFocus() {
		lblWorkingSet.setFocus();
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class WorkingSetViewWrapper extends DIViewPart<WorkingSetView>{
		public WorkingSetViewWrapper() {
			super(WorkingSetView.class);
		}
	}

	
	public static String getWorkingsetName(Shell activeShell, String initialValue){
		if (initialValue == null) initialValue = Messages.WorkingSetView_DefaultName;
		InputDialog in = new SmartStyledInputDialog(activeShell, Messages.WorkingSetView_NewDialogTitle, Messages.WorkingSetView_NewDialogMsg, initialValue, new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText.trim().isEmpty() || newText.trim().length() > org.wcs.smart.ca.Label.MAX_LENGTH){
					return MessageFormat.format(Messages.WorkingSetView_InvalidName, org.wcs.smart.ca.Label.MAX_LENGTH);
				}
				return null;
			}
		});
		
		if (in.open() == Window.CANCEL) return null;
		return in.getValue().trim();
	}
	/**
	 * Must be called from the display thread.  Opens a dialog to collect the name
	 * of the workingset then adds the working set to the database.
	 */
	public static IntelWorkingSet createWorkingSet(Shell activeShell){
		String name = getWorkingsetName(activeShell, null);
		if (name == null) return null;
		
		IntelWorkingSet workingSet = new IntelWorkingSet();
		workingSet.setConservationArea(SmartDB.getCurrentConservationArea());
		workingSet.updateName(SmartDB.getCurrentLanguage(), name);
		workingSet.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		workingSet.setName(name);
		workingSet.setEntityDateFilter(DateFilter.LAST_YEAR.name());
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				s.persist(workingSet);
				s.getTransaction().commit();
			}catch (Exception ex){
				if (s.getTransaction().isActive())s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.WorkingSetView_NewWsError, ex.getMessage()), ex);
			}
		}
		return workingSet;
	}
	
	private class LoadWorkingSetJob extends Job{
		
		private UUID workingSetUuid;
		private ISelection lastSelection = null;
		private Object[] lastOpenElements = null;
		private boolean isNew = true;
		
		public LoadWorkingSetJob(){
			super(Messages.WorkingSetView_loadingJobName);
		}
		
		public void setWorkingSetUuid(UUID uuid){
			isNew = uuid != null && !uuid.equals(workingSetUuid);
			this.workingSetUuid = uuid;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			isInitializing = true;
			try{
				IntelWorkingSet ws = null;
				Display.getDefault().syncExec(()->{
					if (lblWorkingSet.isDisposed()) return;
					lastSelection = workingsetTree.getSelection();
					lblWorkingSet.setText(DialogConstants.LOADING_TEXT);
					
					if (isNew){
						lastOpenElements = null;
					}else{
						lastOpenElements = workingsetTree.getExpandedElements();
					}
					
					workingsetTree.setInput(new String[]{DialogConstants.LOADING_TEXT});
				});
				
				List<IntelWorkingSetItem> items = new ArrayList<IntelWorkingSetItem>();

				if (workingSetUuid != null){
					try(Session s = HibernateManager.openSession()){
						ws = (IntelWorkingSet) s.get(IntelWorkingSet.class, workingSetUuid);
						if (ws != null){
							ws.getName();
							for (IntelWorkingSetEntity entity : ws.getEntities()){
								LayerStatus ls = WorkingSetManager.INSTANCE.canViewItem(entity, null);
								if (ls == LayerStatus.OK) {
									IntelWorkingSetItem i = new IntelWorkingSetItem(entity);
									items.add(i);
								}else{
									items.add(new HiddenWorkingSetItem(IntelWorkingSetCategory.ENTITY, ls));
								}
							}
							
							
							for (IntelWorkingSetRecord record : ws.getRecords()){
								LayerStatus ls = WorkingSetManager.INSTANCE.canViewItem(record, null);
								if (ls == LayerStatus.OK) {
									Image img = Resources.INSTANCE.getImage(record.getRecord().getRecordSource());
									if (img == null) {
										img =  Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
									}
									IntelWorkingSetItem i = new IntelWorkingSetItem(record, img);
									items.add(i);
								}else {
									items.add(new HiddenWorkingSetItem(IntelWorkingSetCategory.RECORD, ls));
								}
							}
							for (IntelWorkingSetQuery query : ws.getQueries()){
								AbstractIntelQuery queryImpl = QueryManager.INSTANCE.findQuery(s, query.getQuery(), query.getQueryType());
								LayerStatus ls = WorkingSetManager.INSTANCE.canViewItem(query, queryImpl);
								if (ls == LayerStatus.OK) {
									IntelWorkingSetItem i = new IntelWorkingSetItem(query, queryImpl);
									items.add(i);
								}else {
									items.add(new HiddenWorkingSetItem(IntelWorkingSetCategory.QUERIES, ls));
								}
							}
						}
					}
				}
				
				DateFilter initFilter = DateFilter.LAST_YEAR;
				LocalDate[] dates = new LocalDate[]{initFilter.getStartDate(), initFilter.getEndDate()};
				if (ws != null){					
					try{
						dates = WorkingSetManager.INSTANCE.parseEntityDateFilter(ws);
					}catch (Exception ex){
						Intelligence2PlugIn.log("Unable to parse entity date filter for working set : " + ws.getEntityDateFilter() + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
					}
					
				}
				final DateFilter dfilter = initFilter;
				final LocalDate[] dates2 = dates;
				final IntelWorkingSet wss = ws;
				
				String key = LAST_WS_PREFERENCE + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid());
				if (wss != null){
					Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(key, UuidUtils.uuidToString(wss.getUuid()));
				}else{
					Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(key, ""); //$NON-NLS-1$
				}
				
				Display.getDefault().syncExec(()->{
					if (lblWorkingSet.isDisposed()) return;
					if (wss == null){
						lblWorkingSet.setText(Messages.WorkingSetView_NotSelectedLabel);
						dateComp.setEnabled(false);
						workingsetTree.setInput(null);
					}else{
						dateComp.setEnabled(true);
						dateComp.setDateFilter(dfilter, dates2);
						
						lblWorkingSet.setText(wss.getName());
						workingsetTree.setInput(items);
						
						workingsetTree.setSelection(lastSelection);
						if (lastOpenElements == null){
							workingsetTree.expandAll();
						}else{
							workingsetTree.setExpandedElements(lastOpenElements);
						}
						
						items.forEach((e) -> { 
							if (e.isVisible()) workingsetTree.setChecked(e, true);  
						});
					}
				});
			}finally{
				isInitializing = false;
			}
			
			return Status.OK_STATUS;
		}
		
	}
}