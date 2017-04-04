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

import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.graphics.ImageData;
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
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetItem;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.WorkingSetLabelProvider;
import org.wcs.smart.i2.ui.dialogs.WorkingSetListDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.handler.OpenQueryHandler;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * Working set view.
 * 
 * @author Emily
 *
 */
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
		header.setLayout(new GridLayout(3, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
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
		
		copyItem = new ToolItem(tools, SWT.PUSH);
		copyItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_COPY));
		copyItem.setToolTipText(Messages.WorkingSetView_copyTooltip);
		copyItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyWorkingSet();	
			}
		});
		copyItem.setEnabled(false);
		
		newItem = new ToolItem(tools, SWT.PUSH);
		newItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		newItem.setToolTipText(Messages.WorkingSetView_createnewTooltip);
		newItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				newWorkingSet();	
			}
		});
		
		selectItem = new ToolItem(tools, SWT.PUSH);
		selectItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_SELECT));
		selectItem.setToolTipText(Messages.WorkingSetView_selectTooltip);
		selectItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectWorkingSet();	
			}
		});
		
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
				
				Date[] dFilters = getDateFilters();		
				
				String dateFilter = dateComp.getDateFilter().name();
				if (dateComp.getDateFilter() == DateFilter.CUSTOM){
					dateFilter += ":" + dFilters[0].getTime() + ":" + dFilters[1].getTime(); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				//save date filter
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					IntelWorkingSet ws = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
					ws.setEntityDateFilter(dateFilter);
					s.getTransaction().commit();
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(Messages.WorkingSetView_SaveDateError, ex);
					s.getTransaction().rollback();
				}finally{
					s.close();
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
					
					if ( event.getElement() instanceof IntelWorkingSetCategory){
						Object[] kids = ((WorkingSetTreeContentProvider)workingsetTree.getContentProvider()).getChildren(event.getElement());
						if (kids != null){
							for (Object x : kids){
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
					
					//update working set
					Session s = HibernateManager.openSession();
					try{
						s.beginTransaction();
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
								if (uuid.equals(layer.getQuery().getUuid())){
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
								if (uuid.equals(layer.getQuery().getUuid())){
									layer.setIsVisible(false);
									break;
								}
							}
						};
						s.getTransaction().commit();
					}catch(Exception ex){
						s.getTransaction().rollback();
						Intelligence2PlugIn.log(Messages.WorkingSetView_SaveVisibilityError + ex.getMessage(), ex);
					}finally{
						s.close();
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
		
		MenuItem delete = new MenuItem(menu, SWT.PUSH);
		delete.setText(Messages.WorkingSetView_RemoveLabel);
		delete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		delete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				removeSelection();
			}
		});
		
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
				delete.setEnabled(enabled);
				open.setEnabled(enabled);
			}
		});
		addDropListener(parent);
	}
	
	/**
	 * Updates the working set with the date selected in the date
	 * filter.  Returns the new dates.
	 * @return
	 */
	private Date[] getDateFilters(){
		Date[] dFilters = new Date[2];
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
		List<IntelRecordObservationQuery> queries = new ArrayList<>();
		for (IntelWorkingSetItem toDelete: toDeleteItems){
			if (toDelete.getCategory() == IntelWorkingSetCategory.ENTITY){
				Session s = HibernateManager.openSession();
				try{
					IntelEntity i = (IntelEntity) s.get(IntelEntity.class, toDelete.getUuid());
					i.getIdAttributeAsText();
					entities.add(i);
				}finally{
					s.close();
				}
			}else if (toDelete.getCategory() == IntelWorkingSetCategory.RECORD){
				Session s = HibernateManager.openSession();
				try{
					IntelRecord i = (IntelRecord) s.get(IntelRecord.class, toDelete.getUuid());
					records.add(i);
				}finally{
					s.close();
				}
			}else if (toDelete.getCategory() == IntelWorkingSetCategory.QUERIES){
				Session s = HibernateManager.openSession();
				try{
					IntelRecordObservationQuery i = (IntelRecordObservationQuery) s.get(IntelRecordObservationQuery.class, toDelete.getUuid());
					queries.add(i);
				}finally{
					s.close();
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
					Session s = HibernateManager.openSession();
					try{
						i = (IntelEntity) s.get(IntelEntity.class, toOpen.getUuid());
						i.getIdAttributeAsText();
					}finally{
						s.close();
					}
					(new OpenEntityHandler()).openEntity(i, context);
				}else if (toOpen.getCategory() == IntelWorkingSetCategory.RECORD){
					IntelRecord i = null;
					Session s = HibernateManager.openSession();
					try{
						i = (IntelRecord) s.get(IntelRecord.class, toOpen.getUuid());
					}finally{
						s.close();
					}
					(new OpenRecordHandler()).openRecord(i, false);
				}else if (toOpen.getCategory() == IntelWorkingSetCategory.QUERIES){
					(new OpenQueryHandler()).openQuery(toOpen.getUuid(), false);
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

	private void copyWorkingSet(){
		IntelWorkingSet set = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			set = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
			String newName = WorkingSetView.getWorkingsetName(activeShell, MessageFormat.format(Messages.WorkingSetView_DefaultWsName, set.getName()));
			if (newName != null){
				set = WorkingSetManager.INSTANCE.clone(set);
				set.setName(newName);
				set.updateName(SmartDB.getCurrentLanguage(), newName);
				set.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), newName);
				s.save(set);
				s.getTransaction().commit();
			}else{
				set = null;
				s.getTransaction().rollback();
			}
			
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.WorkingSetView_DeleteError, lblWorkingSet.getText(), ex.getMessage()), ex);
			return;
		}finally{
			s.close();
		}
		if (set != null){
			context.get(IEventBroker.class).send(IntelEvents.WS_NEW, set);
			setWorkingSet(set, context);
		}
		
	}
	
	@Inject
	@Optional
	private void workingSetLayerVisibilityModified(@UIEventTopic(IntelEvents.ACTIVE_WS_LAYER_VISIBILITY) LayerVisibleEvent event){
		if (isConfigureVisibility) return;
		try{
			isConfigureVisibility = true;
			
			for (UUID uuid : event.allVisible){
				IntelWorkingSetItem i = new IntelWorkingSetItem(null, null, true, uuid);
				workingsetTree.setChecked(i, true);
				workingsetTree.setGrayed(i, false);
			}
			for (UUID uuid : event.partVisible){
				IntelWorkingSetItem i = new IntelWorkingSetItem(null, null, false, uuid);
				workingsetTree.setChecked(i, true);
				workingsetTree.setGrayed(i, true);
			}
			for (UUID uuid : event.notVisible){
				IntelWorkingSetItem i = new IntelWorkingSetItem(null, null, false, uuid);
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
		copyItem.setEnabled(activeWorkingSet != null);
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
		InputDialog in = new InputDialog(activeShell, Messages.WorkingSetView_NewDialogTitle, Messages.WorkingSetView_NewDialogMsg, initialValue, new IInputValidator() {
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
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.save(workingSet);
			s.getTransaction().commit();
		}catch (Exception ex){
			if (s.getTransaction().isActive())s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.WorkingSetView_NewWsError, ex.getMessage()), ex);
		}finally{
			s.close();
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
					Session s = HibernateManager.openSession();
					try{
						ws = (IntelWorkingSet) s.get(IntelWorkingSet.class, workingSetUuid);
						if (ws != null){
							ws.getName();
							for (IntelWorkingSetEntity entity : ws.getEntities()){
								IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.ENTITY, entity.getEntity().getIdAttributeAsText(), entity.getIsVisible(), entity.getEntity().getUuid(), EntityTypeLabelProvider.createImageDescriptor(entity.getEntity().getEntityType()));
								items.add(i);
							}
							
							HashMap<IntelRecordSource, ImageDescriptor> sourceImages = new HashMap<>();
							for (IntelWorkingSetRecord record : ws.getRecords()){
								ImageDescriptor img =  Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD);
								if (record.getRecord().getRecordSource() != null){
									img = sourceImages.get(record.getRecord().getRecordSource());
									if (img == null){
										//get image descriptor for record source	
										img = new ImageDescriptor() {
											
											@Override
											public ImageData getImageData() {
												try{
													BufferedImage image = record.getRecord().getRecordSource().getIconAsImage();
													if (image != null){
														return AWTSWTImageUtils.convertToSWTImage(image).getImageData();
													}
												}catch (Exception ex){
													
												}
												return null;
											}
										};
										sourceImages.put(record.getRecord().getRecordSource(), img);
									}
								}
								IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.RECORD, record.getRecord().getTitle(), record.getIsVisible(), record.getRecord().getUuid(),img);
								items.add(i);
							}
							for (IntelWorkingSetQuery query : ws.getQueries()){
								IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.QUERIES, query.getQuery().getName(), query.getIsVisible(), query.getQuery().getUuid(), Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_ENTITY_QUERY));
								items.add(i);
							}
						}
					}finally{
						s.close();
					}
				}
				
				DateFilter initFilter = DateFilter.LAST_YEAR;
				Date[] dates = new Date[]{initFilter.getStartDate(), initFilter.getEndDate()};
				if (ws != null){
					String dateFilter = ws.getEntityDateFilter();
					try{
						String[] bits = dateFilter.split(":"); //$NON-NLS-1$
						initFilter = DateFilter.valueOf(bits[0]);
						if (initFilter == DateFilter.CUSTOM){
							dates = new Date[]{new Date(Long.valueOf(bits[1])), new Date(Long.valueOf(bits[2]))};
						}
					}catch (Exception ex){
						Intelligence2PlugIn.log("Unable to parse entity date filter for working set : " + dateFilter + ". " + ex.getMessage(), ex); //$NON-NLS-1$ //$NON-NLS-2$
					}
					
				}
				final DateFilter dfilter = initFilter;
				final Date[] dates2 = dates;
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
						
						items.forEach((e) -> { if (e.isVisible()) workingsetTree.setChecked(e, true);  });
						
					}
				});
			}finally{
				isInitializing = false;
			}
			
			return Status.OK_STATUS;
		}
		
	}
}