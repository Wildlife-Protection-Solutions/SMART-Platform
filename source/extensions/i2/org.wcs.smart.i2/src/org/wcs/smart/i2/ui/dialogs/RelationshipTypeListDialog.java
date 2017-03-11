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
package org.wcs.smart.i2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RelationshipTypeManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.model.IntelRelationshipTypeAttribute;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.ui.TableColumnViewerFilter;
import org.wcs.smart.i2.ui.TextViewerFilter;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.E3Utils;

/**
 * Dialog for listing relationship types.
 * @author Emily
 *
 */
public class RelationshipTypeListDialog extends TitleAreaDialog {
	
	private static final int ASC = 1;
	private static final int DESC = -1;
	
	@Inject
	private IEventBroker broker;
	@Inject
	private IEclipseContext context;
	
	private TableViewer tblTypes;
	private List<IntelRelationshipType> types = null;
	private TextViewerFilter filter;
	private IStructuredSelection currentSelection;
	
	private MenuItem mnuEdit;
	private MenuItem mnuAdd;
	private MenuItem mnuDelete;
	
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	
	
	private int sortColumn = -1;
	private int sortDirection = ASC;
	
	private Job loadTypes = new Job("load relationship types"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			types = null;
			Session session = HibernateManager.openSession();
			try{
				types = RelationshipTypeManager.INSTANCE.getRelationshipTypes(session, SmartDB.getCurrentConservationArea());
				for (IntelRelationshipType t : types){
					t.getName();
					if (t.getSourceEntityType() != null) t.getSourceEntityType().getName();
					if (t.getTargetEntityType() != null) t.getTargetEntityType().getName();
					if (t.getRelationshipGroup() != null){
						t.getRelationshipGroup().getName();
					}
				}
			}finally{
				session.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					tblTypes.setInput(types);
					tblTypes.setSelection(currentSelection);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	public RelationshipTypeListDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*2));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		FilterComposite typeFilter = new FilterComposite(parent, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				filter.setFilterString(typeFilter.getPatternFilter());
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setVisible(false);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		tblTypes = new TableViewer(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		tblTypes.setContentProvider(ArrayContentProvider.getInstance());
		tblTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblTypes.getControl().setFocus();
		tblTypes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		tblTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!tblTypes.getSelection().isEmpty());
				btnDelete.setEnabled(!tblTypes.getSelection().isEmpty());
				mnuEdit.setEnabled(!tblTypes.getSelection().isEmpty());
				mnuDelete.setEnabled(!tblTypes.getSelection().isEmpty());
			}
		});
		tblTypes.getTable().setHeaderVisible(true);
		tblTypes.getTable().setLinesVisible(true);
		
		SelectionListener sortListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableColumn c = (TableColumn) e.widget;
				int current=-1;
				TableColumn[] cols = tblTypes.getTable().getColumns();
				for (int i = 0; i < cols.length; i ++){
					if (cols[i].equals(c)){
						current = i;
						break;
					}
				}
				
				if (current == sortColumn){
					sortDirection = sortDirection == ASC ? DESC: ASC; 
				}
				sortColumn = current;
				if (sortColumn < 0){
					tblTypes.getTable().setSortColumn(null);
				}else{
					tblTypes.getTable().setSortColumn(tblTypes.getTable().getColumn(sortColumn));
					tblTypes.getTable().setSortDirection(sortDirection == ASC ? SWT.UP : SWT.DOWN);
				}
				tblTypes.refresh();
			}
		};
		tblTypes.setComparator(new ViewerComparator(){
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				int result = 0;
				if (sortColumn >= 0){
					String s1 = ((ColumnLabelProvider)tblTypes.getLabelProvider(sortColumn)).getText(e1);
					String s2 = ((ColumnLabelProvider)tblTypes.getLabelProvider(sortColumn)).getText(e2);
					result = Collator.getInstance().compare(s1, s2);
				}
				return sortDirection * result;
			}
		});
		
		final TableViewerColumn nameColumn = new TableViewerColumn(tblTypes, SWT.DEFAULT);
		nameColumn.getColumn().setText(Messages.RelationshipTypeListDialog_RelationshipColumnName);
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			private RelationshipTypeLabelProvider rl = new RelationshipTypeLabelProvider();
			@Override
			public String getText(Object element){
				if (element instanceof IntelRelationshipType){
					return ((IntelRelationshipType) element).getName();
				}
				return super.getText(element);
			}
			@Override
			public void dispose(){
				super.dispose();
				rl.dispose();
			}
			@Override
			public Image getImage(Object element){
				return rl.getImage(element);
			}
		});
		nameColumn.getColumn().setWidth(150);
		nameColumn.getColumn().addSelectionListener(sortListener);
		
		TableViewerColumn groupColumn = new TableViewerColumn(tblTypes, SWT.DEFAULT);
		groupColumn.getColumn().setText(Messages.RelationshipTypeListDialog_GroupColumnName);
		groupColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element){
				if (element instanceof IntelRelationshipType){
					IntelRelationshipType t = (IntelRelationshipType)element;
					if (t.getRelationshipGroup() != null) return t.getRelationshipGroup().getName();
					return ""; //$NON-NLS-1$
				}
				return super.getText(element);
			}
		});
		groupColumn.getColumn().setWidth(150);
		groupColumn.getColumn().addSelectionListener(sortListener);
		TableViewerColumn sourceColumn = new TableViewerColumn(tblTypes, SWT.DEFAULT);
		sourceColumn.getColumn().setText(Messages.RelationshipTypeListDialog_SrcColumName);
		sourceColumn.setLabelProvider(new ColumnLabelProvider() {
			private EntityTypeLabelProvider el = new EntityTypeLabelProvider();
			@Override
			public String getText(Object element){
				if (element instanceof IntelRelationshipType){
					IntelRelationshipType t = (IntelRelationshipType)element;
					if (t.getSourceEntityType() != null) return t.getSourceEntityType().getName();
					return Messages.RelationshipTypeListDialog_UnknownOption;
				}
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelRelationshipType){
					return el.getImage(((IntelRelationshipType)element).getSourceEntityType());
				}
				return null;
			}
			@Override
			public void dispose(){
				super.dispose();
				el.dispose();
			}
		});
		sourceColumn.getColumn().setWidth(150);
		sourceColumn.getColumn().addSelectionListener(sortListener);
		TableViewerColumn targetColumn = new TableViewerColumn(tblTypes, SWT.DEFAULT);
		targetColumn.getColumn().setText(Messages.RelationshipTypeListDialog_TargetColumName);
		targetColumn.setLabelProvider(new ColumnLabelProvider() {
			private EntityTypeLabelProvider el = new EntityTypeLabelProvider();
			@Override
			public String getText(Object element){
				if (element instanceof IntelRelationshipType){
					IntelRelationshipType t = (IntelRelationshipType)element;
					if (t.getTargetEntityType() != null) return t.getTargetEntityType().getName();
					return Messages.RelationshipTypeListDialog_UnknownOption;
				}
				return super.getText(element);
			}
			@Override
			public Image getImage(Object element){
				if (element instanceof IntelRelationshipType){
					return el.getImage(((IntelRelationshipType)element).getTargetEntityType());
				}
				return null;
			}
			@Override
			public void dispose(){
				super.dispose();
				el.dispose();
			}
		});
		targetColumn.getColumn().setWidth(150);
		targetColumn.getColumn().addSelectionListener(sortListener);
		filter = new TableColumnViewerFilter(tblTypes, 
				(ColumnLabelProvider)tblTypes.getLabelProvider(0), (ColumnLabelProvider)tblTypes.getLabelProvider(1));
		tblTypes.setFilters(new ViewerFilter[]{filter});
		
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnNew = new Button(buttonPanel, SWT.PUSH);
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
		btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		
		btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
		});
		
		Menu menu = new Menu(tblTypes.getControl());
		tblTypes.getControl().setMenu(menu);

		mnuAdd = new MenuItem(menu, SWT.DEFAULT);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				add();
			}
		});
		
		mnuEdit = new MenuItem(menu, SWT.DEFAULT);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		mnuEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edit();
			}
		});
		mnuDelete = new MenuItem(menu, SWT.DEFAULT);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				delete();
			}
		});
		
		btnNew.setEnabled(true);
		btnEdit.setEnabled(false);
		btnDelete.setEnabled(false);
		mnuAdd.setEnabled(true);
		mnuEdit.setEnabled(false);
		mnuDelete.setEnabled(false);
		
		
		setTitle(Messages.RelationshipTypeListDialog_Title);
		getShell().setText(Messages.RelationshipTypeListDialog_Title);
		setMessage(Messages.RelationshipTypeListDialog_Message);
		
		loadTypes.setSystem(true);
		loadTypes.schedule();
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	private void openTypeDialog(IntelRelationshipType type){
		RelationshipTypeDialog ed = new RelationshipTypeDialog(getShell(), type);
		ContextInjectionFactory.inject(ed, context);
		ed.open();
		refresh();
	}
	

	private void add(){
		IntelRelationshipType type = new IntelRelationshipType();
		type.setConservationArea(SmartDB.getCurrentConservationArea());
		type.setAttributes(new ArrayList<IntelRelationshipTypeAttribute>());
		openTypeDialog(type);
	}
	
	
	private boolean checkSaveEditors(IntelRelationshipType type, String action){
		//before we edit the entity type ensure that all editors with this type are not dirty
		List<EntityEditor> toSave = new ArrayList<EntityEditor>();
		StringBuilder sb= new StringBuilder();
		for (MPart part : context.get(EPartService.class).getParts()){
			if (E3Utils.isCompatibilityEditor(part)){
				Object src = E3Utils.getSourceObject(part); 
				if ( src instanceof EntityEditor && ((EntityEditor)src).isDirty()){
					//ensure there is at least one relationship of this type we are modifiying
					if (((EntityEditor)src).hasRelation(type)){
						toSave.add((EntityEditor) src);
						sb.append(((EntityEditor)src).getEntity().getIdAttributeAsText());
						sb.append(", "); //$NON-NLS-1$
					}
				}
			}
		}
		if (!toSave.isEmpty()){
			if (MessageDialog.openQuestion(getShell(), Messages.RelationshipTypeListDialog_DoActionTitle, 
					MessageFormat.format(Messages.RelationshipTypeListDialog_DoActionMessage, action, type.getName(), sb.substring(0, sb.length()-2)))){
				for (EntityEditor e : toSave){
					e.doSave(new NullProgressMonitor());
				}
			}else{
				return false;  //cannot edit
			}
		}
		return true;
	}
	private void edit(){
		Object x = ((IStructuredSelection)tblTypes.getSelection()).getFirstElement();
		if (x instanceof IntelRelationshipType){
			IntelRelationshipType type = (IntelRelationshipType)x;	
			checkSaveEditors(type, Messages.RelationshipTypeListDialog_editingLabel);
			openTypeDialog(type);
		}
	}
	
	private void delete(){
		List<IntelRelationshipType> toDelete = new ArrayList<IntelRelationshipType>();
		StringBuilder sb = new StringBuilder();
		
		for (Iterator<?> iterator = ((IStructuredSelection)tblTypes.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof IntelRelationshipType){
				toDelete.add((IntelRelationshipType)x);
				sb.append(((IntelRelationshipType) x).getName());
				sb.append(", "); //$NON-NLS-1$
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openConfirm(getShell(), Messages.RelationshipTypeListDialog_DeleteDialogTitle, MessageFormat.format(Messages.RelationshipTypeListDialog_DeletedialogMsg, sb.toString()))){
			return;
		}
		for (IntelRelationshipType t : toDelete){
			if (!checkSaveEditors(t, Messages.RelationshipTypeListDialog_deleting)){
				return;
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask(Messages.RelationshipTypeListDialog_DeleteTaskName, toDelete.size());
					List<IntelRelationshipType> deleted = new ArrayList<IntelRelationshipType>();
					Session s = HibernateManager.openSession();
					try{
						for (IntelRelationshipType t : toDelete){
							monitor.subTask(t.getName());
							s.beginTransaction();
							try{
								RelationshipTypeManager.INSTANCE.deleteRelationshipType(t, s);
								s.getTransaction().commit();
								deleted.add(t);
							}catch(Exception ex){
								s.getTransaction().rollback();
								Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.RelationshipTypeListDialog_DeleteError, t.getName(), ex.getMessage()), ex);
							}
							monitor.worked(1);
						}
					}finally{
						s.close();
					}
					monitor.done();
					for (IntelRelationshipType d : deleted){
						broker.send(IntelEvents.RELATION_TYPE_DELETE, d);
					}
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog(Messages.RelationshipTypeListDialog_DeleteError2 +e.getMessage(), e);
		}
		
		refresh();
	}
	
	private void refresh(){
		currentSelection = (IStructuredSelection) tblTypes.getSelection();
		tblTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadTypes.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
}
