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
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetItem;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.WorkingSetLabelProvider;
import org.wcs.smart.i2.ui.dialogs.WorkingSetListDialog;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.handler.OpenRecordHandler;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

public class WorkingSetView {
	
	public static final String ID = "org.wcs.smart.i2.ui.view.workingset"; //$NON-NLS-1$
	
	@Inject
	private IEclipseContext context;

	@Inject
	private Shell activeShell;
	
	private Label lblWorkingSet;
	private ToolItem deleteItem;
	private ToolItem newItem;
	private ToolItem selectItem;
	
	private TreeViewer workingsetTree;
	private LoadWorkingSetJob job = new LoadWorkingSetJob();
	
	public WorkingSetView() {
		super();
		job.setSystem(true);
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite core = toolkit.createComposite(parent);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = toolkit.createComposite(core);
		header.setLayout(new GridLayout(3, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		lblWorkingSet = toolkit.createLabel(header, "");
		lblWorkingSet.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		FontData fd = lblWorkingSet.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight()+1);
		Font f = new Font(lblWorkingSet.getDisplay(), fd);
		lblWorkingSet.addListener(SWT.Dispose,(e)->f.dispose());
		lblWorkingSet.setFont(f);
		lblWorkingSet.setText("<Not Selected>");
		
		ToolBar tools = new ToolBar(header, SWT.FLAT);
		
		deleteItem = new ToolItem(tools, SWT.PUSH);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.setToolTipText("Delete active working set");
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteActiveWorkingSet();	
			}
		});
		newItem = new ToolItem(tools, SWT.PUSH);
		newItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		newItem.setToolTipText("Create new working set");
		newItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				newWorkingSet();	
			}
		});
		
		selectItem = new ToolItem(tools, SWT.PUSH);
		selectItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_SELECT));
		selectItem.setToolTipText("Select working set");
		selectItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectWorkingSet();	
			}
		});

	
		workingsetTree = new TreeViewer(core, SWT.FULL_SELECTION | SWT.MULTI);
		workingsetTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(workingsetTree.getTree());
		workingsetTree.setLabelProvider(WorkingSetLabelProvider.INSTANCE);
		workingsetTree.setContentProvider(new WorkingSetTreeContentProvider());
		workingsetTree.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openSelection();
			}
		});
		
		Menu menu = new Menu(workingsetTree.getControl());
		workingsetTree.getControl().setMenu(menu);
		
		MenuItem open = new MenuItem(menu, SWT.PUSH);
		open.setText("Open");
		open.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openSelection();
			}
		});
		
		MenuItem delete = new MenuItem(menu, SWT.PUSH);
		delete.setText(DialogConstants.DELETE_BUTTON_TEXT);
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
		for (IntelWorkingSetItem toDelete: toDeleteItems){
			if (toDelete.getCategory() == IntelWorkingSetCategory.ENTITY){
				IntelEntity i = null;
				Session s = HibernateManager.openSession();
				try{
					i = (IntelEntity) s.get(IntelEntity.class, toDelete.getUuid());
					i.getIdAttributeAsText();
				}finally{
					s.close();
				}
				WorkingSetManager.INSTANCE.removeFromWorkingSet(i, context);
			}else if (toDelete.getCategory() == IntelWorkingSetCategory.RECORD){
				IntelRecord i = null;
				Session s = HibernateManager.openSession();
				try{
					i = (IntelRecord) s.get(IntelRecord.class, toDelete.getUuid());
				}finally{
					s.close();
				}
				WorkingSetManager.INSTANCE.removeFromWorkingSet(i, context);
			}else if (toDelete.getCategory() == IntelWorkingSetCategory.QUERIES){
				IntelRecordQuery i = null;
				Session s = HibernateManager.openSession();
				try{
					i = (IntelRecordQuery) s.get(IntelRecordQuery.class, toDelete.getUuid());
				}finally{
					s.close();
				}
				WorkingSetManager.INSTANCE.removeFromWorkingSet(i, context);
			}
		}
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
					IntelRecordQuery i = null;
					Session s = HibernateManager.openSession();
					try{
						i = (IntelRecordQuery) s.get(IntelRecordQuery.class, toOpen.getUuid());
					}finally{
						s.close();
					}
					//TODO:
//					(new OpenRecordHandler()).openRecord(i, false);
				}
				return;
			}
			
		}
	}
	
	private void newWorkingSet(){
		
		InputDialog in = new InputDialog(activeShell, "New Working Set", "Enter a name for the new working set.", "Working Set", new IInputValidator() {
			
			@Override
			public String isValid(String newText) {
				if (newText.trim().isEmpty() || newText.trim().length() > org.wcs.smart.ca.Label.MAX_LENGTH){
					return MessageFormat.format("Name must be provided and few than {0} characters.", org.wcs.smart.ca.Label.MAX_LENGTH);
				}
				return null;
			}
		});
		
		if (in.open() == Window.CANCEL) return;
		
		String name = in.getValue().trim();
		
		IntelWorkingSet workingSet = new IntelWorkingSet();
		workingSet.setConservationArea(SmartDB.getCurrentConservationArea());
		workingSet.updateName(SmartDB.getCurrentLanguage(), name);
		workingSet.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		workingSet.setName(name);
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.save(workingSet);
			s.getTransaction().commit();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(MessageFormat.format("Error creating new working set. {1}", ex.getMessage()), ex);
		}finally{
			s.close();
		}
		WorkingSetManager.INSTANCE.setActiveWorkingSet(workingSet, context);
	}

	private void deleteActiveWorkingSet(){
		if (!MessageDialog.openConfirm(activeShell, "Delete Working Set", MessageFormat.format("Are you sure you want to delete the working set {0}?  This action cannot be undone.", lblWorkingSet.getText()))){
			return;
		}
		IntelWorkingSet set = null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			set = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
			WorkingSetManager.INSTANCE.deleteWorkingSet(s, set);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog(MessageFormat.format("Error removing working set {0}. {1}", lblWorkingSet.getText(), ex.getMessage()), ex);
			return;
		}finally{
			s.close();
		}
		if (set != null) context.get(IEventBroker.class).send(IntelEvents.WS_DELETE, set);
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
			WorkingSetManager.INSTANCE.setActiveWorkingSet(null, context);
		}
	}
	
	@Inject
	@Optional
	private void entityRemoved(@UIEventTopic(IntelEvents.ENTITY_DELETE) IntelEntity e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void entityModified(@UIEventTopic(IntelEvents.ENTITY_MODIFIED) IntelEntity e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void recordRemoved(@UIEventTopic(IntelEvents.RECORD_DELETE) IntelRecord e){
		refreshWithDelay();
	}
	
	@Inject
	@Optional
	private void recordModified(@UIEventTopic(IntelEvents.RECORD_MODIFIED) IntelRecord e){
		refreshWithDelay();
	}
	
	private void refreshWithDelay(){
		job.schedule(250);
	}
	
	private void selectWorkingSet(){
		WorkingSetListDialog dialog = ContextInjectionFactory.make(WorkingSetListDialog.class, context);
		dialog.open();
		
		IntelWorkingSet selection = dialog.getSelection();
		if (selection != null){
			WorkingSetManager.INSTANCE.setActiveWorkingSet(selection, context);
		}
	}
	
	private void setWorkingSet(IntelWorkingSet set){	
		job.setWorkingSetUuid(set == null ? null : set.getUuid());
		job.schedule();
	}
	
	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class WorkingSetViewWrapper extends DIViewPart<WorkingSetView>{
		public WorkingSetViewWrapper() {
			super(WorkingSetView.class);
		}
	}

	private class LoadWorkingSetJob extends Job{
		
		private UUID workingSetUuid;
		private ISelection lastSelection = null;
		private Object[] lastOpenElements = null;
		private boolean isNew = true;
		
		public LoadWorkingSetJob(){
			super("load working set job");
		}
		
		public void setWorkingSetUuid(UUID uuid){
			isNew = uuid != null && !uuid.equals(workingSetUuid);
			this.workingSetUuid = uuid;
		}
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IntelWorkingSet ws = null;
			Display.getDefault().syncExec(()->{
				if (lblWorkingSet.isDisposed()) return;
				lastSelection = workingsetTree.getSelection();
				lblWorkingSet.setText("Loading...");
				
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
					
					ws.getName();
					for (IntelWorkingSetEntity entity : ws.getEntities()){
						IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.ENTITY, entity.getEntity().getIdAttributeAsText(), entity.getEntity().getUuid(), EntityTypeLabelProvider.INSTANCE.createImageDescriptor(entity.getEntity().getEntityType()));
						items.add(i);
					}
					
					for (IntelWorkingSetRecord record : ws.getRecords()){
						IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.RECORD, record.getRecord().getTitle(), record.getRecord().getUuid(), Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD));
						items.add(i);
					}
				}finally{
					s.close();
				}
			}
			final IntelWorkingSet wss = ws;
			Display.getDefault().syncExec(()->{
				if (lblWorkingSet.isDisposed()) return;
				if (wss == null){
					lblWorkingSet.setText("<Not Selected>");
					workingsetTree.setInput(null);
				}else{
					lblWorkingSet.setText(wss.getName());
					workingsetTree.setInput(items);
					workingsetTree.setSelection(lastSelection);
					if (lastOpenElements == null){
						workingsetTree.expandAll();
					}else{
						workingsetTree.setExpandedElements(lastOpenElements);
					}
				}
			});
			
			return Status.OK_STATUS;
		}
		
	}
}