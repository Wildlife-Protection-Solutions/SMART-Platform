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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;
import org.wcs.smart.util.E3Utils;

/**
 * Dialog for listing entity types.
 * @author Emily
 *
 */
public class EntityTypeListDialog extends TitleAreaDialog {

	@Inject
	private IEventBroker broker;
	@Inject
	private IEclipseContext context;
	
	private TableViewer cmbTypes;
	private List<IntelEntityType> types = null;
	private NamedItemViewerFilter filter;
	private IStructuredSelection currentSelection;
	
	private MenuItem mnuEdit;
	private MenuItem mnuAdd;
	private MenuItem mnuDelete;
	
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	 
	private Job loadTypes = new Job("load entity types"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			types = null;
			Session session = HibernateManager.openSession();
			try{
				types = EntityTypeManager.INSTANCE.getEntityTypes(session, SmartDB.getCurrentConservationArea());
				for (IntelEntityType t : types){
					t.getName();
				}
			}finally{
				session.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (cmbTypes.getControl().isDisposed()) return;
					cmbTypes.setInput(types);
					cmbTypes.setSelection(currentSelection);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	public EntityTypeListDialog(Shell parentShell) {
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
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				filter.setFilterString(typeFilter.getPatternFilter());
			}
		});
		
		Label l = new Label(parent, SWT.NONE);
		l.setVisible(false);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		cmbTypes = new TableViewer(parent);
		cmbTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbTypes.setLabelProvider(new EntityTypeLabelProvider());
		cmbTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbTypes.getControl().setFocus();
		cmbTypes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		cmbTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!cmbTypes.getSelection().isEmpty());
				btnDelete.setEnabled(!cmbTypes.getSelection().isEmpty());
				mnuEdit.setEnabled(!cmbTypes.getSelection().isEmpty());
				mnuDelete.setEnabled(!cmbTypes.getSelection().isEmpty());
			}
		});
		
		filter = new NamedItemViewerFilter(cmbTypes);
		cmbTypes.setFilters(new ViewerFilter[]{filter});
		
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
		
		Menu menu = new Menu(cmbTypes.getControl());
		cmbTypes.getControl().setMenu(menu);

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
		
		
		setTitle("Entity Types");
		getShell().setText("Entity Types");
		setMessage("Manage the entity types in the system.");
		
		loadTypes.setSystem(true);
		loadTypes.schedule();
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	private void add(){
		IntelEntityType type = new IntelEntityType();
		type.setConservationArea(SmartDB.getCurrentConservationArea());
		type.setAttributes(new ArrayList<IntelEntityTypeAttribute>());
		openDialog(type);
		refresh();
	}
	
	private void openDialog(IntelEntityType type){
		IEclipseContext ctx = context.createChild();
		ctx.set(IntelEntityType.class, type);
		ctx.set(Shell.class, getShell());
		EntityTypeDialog ed = ContextInjectionFactory.make(EntityTypeDialog.class, ctx);
		ed.open();
	}
	
	private void edit(){
		Object x = ((IStructuredSelection)cmbTypes.getSelection()).getFirstElement();
		if (x instanceof IntelEntityType){
			IntelEntityType type = (IntelEntityType)x;
			
			//before we edit the entity type ensure that all editors with this type are not dirty
			List<EntityEditor> toSave = new ArrayList<EntityEditor>();
			StringBuilder sb= new StringBuilder();
			for (MPart part : context.get(EPartService.class).getParts()){
				if (E3Utils.isCompatibilityEditor(part)){
					Object src = E3Utils.getSourceObject(part); 
					if ( src instanceof EntityEditor 
							&& ((EntityEditor)src).isDirty()
							&& ((EntityEditor)src).getEntity().getEntityType().equals(type)){
						toSave.add((EntityEditor) src);
						sb.append(((EntityEditor)src).getEntity().getIdAttributeAsText());
						sb.append(", ");
					}
				}
			}
			if (!toSave.isEmpty()){
				if (MessageDialog.openQuestion(getShell(), "Entity Type", 
						MessageFormat.format("Before editing the entity type {0} all changes to the following entities must be saved.  Do you want to save now? \n{1}", type.getName(), sb.substring(0, sb.length()-2)))){
					for (EntityEditor e : toSave){
						e.doSave(new NullProgressMonitor());
					}
				}else{
					return;  //cannot edit
				}
			}
			openDialog(type);
			refresh();
		}
	}
	
	private void delete(){
		List<IntelEntityType> toDelete = new ArrayList<IntelEntityType>();
		StringBuilder sb = new StringBuilder();
		
		for (Iterator<?> iterator = ((IStructuredSelection)cmbTypes.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof IntelEntityType){
				toDelete.add((IntelEntityType)x);
				sb.append(((IntelEntityType) x).getName());
				sb.append(", ");
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openConfirm(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the following entity types?  All entities, attributes, relationships and other references will also be removed.  This action cannot be undone.\n\n{0}", sb.toString()))){
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask("Deleting entity types", toDelete.size());
					List<IntelEntityType> deleted = new ArrayList<IntelEntityType>();
					Session s = HibernateManager.openSession();
					try{
						for (IntelEntityType t : toDelete){
							monitor.subTask(t.getName());
							s.beginTransaction();
							try{
								EntityTypeManager.INSTANCE.deleteEntityType(t, s);
								s.getTransaction().commit();
								deleted.add(t);
							}catch(Exception ex){
								s.getTransaction().rollback();
								Intelligence2PlugIn.displayLog(MessageFormat.format("Unable to delete Entity Type {0}. {1}", t.getName(), ex.getMessage()), ex);
							}
							monitor.worked(1);
						}
					}finally{
						s.close();
					}
					monitor.done();
					for (IntelEntityType d : deleted){
						broker.send(IntelEvents.ENTITY_TYPE_DELETE, d);
					}
					 
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog("Error deleting entity types: " +e.getMessage(), e);
		}
		
		refresh();
	}
	
	private void refresh(){
		if (cmbTypes.getControl().isDisposed()) return;
		
		currentSelection = (IStructuredSelection) cmbTypes.getSelection();
		cmbTypes.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadTypes.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
