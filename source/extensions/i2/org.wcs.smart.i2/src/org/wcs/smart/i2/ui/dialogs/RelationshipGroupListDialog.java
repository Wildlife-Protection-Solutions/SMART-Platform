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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.RelationshipTypeManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRelationshipGroup;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.NamedItemViewerFilter;
import org.wcs.smart.i2.ui.RelationshipGroupLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * Dialog for listing entity types.
 * @author Emily
 *
 */
public class RelationshipGroupListDialog extends TitleAreaDialog {

	@Inject
	private IEclipseContext context;
	
	private TableViewer cmbGroups;
	private List<IntelRelationshipGroup> groups = null;
	private NamedItemViewerFilter filter;
	private IStructuredSelection currentSelection;
	
	private MenuItem mnuEdit;
	private MenuItem mnuAdd;
	private MenuItem mnuDelete;
	
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	
	private Job loadGroups = new Job("load relationships groups"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			groups = null;
			Session session = HibernateManager.openSession();
			try{
				groups = RelationshipTypeManager.INSTANCE.getRelationshipGroups(session, SmartDB.getCurrentConservationArea());
			}finally{
				session.close();
			}
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbGroups.setInput(groups);
					cmbGroups.setSelection(currentSelection);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	public RelationshipGroupListDialog(Shell parentShell) {
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
		
		cmbGroups = new TableViewer(parent);
		cmbGroups.setContentProvider(ArrayContentProvider.getInstance());
		cmbGroups.setLabelProvider(new RelationshipGroupLabelProvider());
		cmbGroups.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbGroups.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbGroups.getControl().setFocus();
		cmbGroups.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		cmbGroups.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnEdit.setEnabled(!cmbGroups.getSelection().isEmpty());
				btnDelete.setEnabled(!cmbGroups.getSelection().isEmpty());
				mnuEdit.setEnabled(!cmbGroups.getSelection().isEmpty());
				mnuDelete.setEnabled(!cmbGroups.getSelection().isEmpty());
			}
		});
		
		filter = new NamedItemViewerFilter(cmbGroups);
		cmbGroups.setFilters(new ViewerFilter[]{filter});
		
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
		
		Menu menu = new Menu(cmbGroups.getControl());
		cmbGroups.getControl().setMenu(menu);

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
		
		
		setTitle(Messages.RelationshipGroupListDialog_Title);
		getShell().setText(Messages.RelationshipGroupListDialog_Title);
		setMessage(Messages.RelationshipGroupListDialog_Message);
		
		loadGroups.setSystem(true);
		loadGroups.schedule();
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	private void openGroupDialog(IntelRelationshipGroup group){
		RelationshipGroupDialog ed = new RelationshipGroupDialog(getShell(), group);
		ContextInjectionFactory.inject(ed, context);
		ed.open();
		refresh();
	}
	
	private void add(){
		IntelRelationshipGroup group = new IntelRelationshipGroup();
		group.setConservationArea(SmartDB.getCurrentConservationArea());
		group.setRelationshipTypes(new ArrayList<IntelRelationshipType>());
		openGroupDialog(group);
	}
	
	private void edit(){
		Object x = ((IStructuredSelection)cmbGroups.getSelection()).getFirstElement();
		if (x instanceof IntelRelationshipGroup){
			IntelRelationshipGroup type = (IntelRelationshipGroup)x;
			openGroupDialog(type);
		}
	}
	
	private void delete(){
		List<IntelRelationshipGroup> toDelete = new ArrayList<IntelRelationshipGroup>();
		StringBuilder sb = new StringBuilder();
		
		for (Iterator<?> iterator = ((IStructuredSelection)cmbGroups.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof IntelRelationshipGroup){
				toDelete.add((IntelRelationshipGroup)x);
				sb.append(((IntelRelationshipGroup) x).getName());
				sb.append(", "); //$NON-NLS-1$
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openConfirm(getShell(), Messages.RelationshipGroupListDialog_ConfirmDeleteTitle, MessageFormat.format(Messages.RelationshipGroupListDialog_ConfirmDeleteMes, sb.toString()))){
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask(Messages.RelationshipGroupListDialog_DeleteTaskName, toDelete.size());
					Session s = HibernateManager.openSession();
					try{
						for (IntelRelationshipGroup t : toDelete){
							monitor.subTask(t.getName());
							s.beginTransaction();
							try{
								RelationshipTypeManager.INSTANCE.deleteRelationshipGroup(t, s);
								s.getTransaction().commit();
							}catch(Exception ex){
								s.getTransaction().rollback();
								Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.RelationshipGroupListDialog_DeleteError1, t.getName(), ex.getMessage()), ex);
							}
							monitor.worked(1);
						}
					}finally{
						s.close();
					}
					monitor.done();
					
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog(Messages.RelationshipGroupListDialog_DeleteError2 +e.getMessage(), e);
		}
		
		refresh();
	}
	
	private void refresh(){
		currentSelection = (IStructuredSelection) cmbGroups.getSelection();
		cmbGroups.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadGroups.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
