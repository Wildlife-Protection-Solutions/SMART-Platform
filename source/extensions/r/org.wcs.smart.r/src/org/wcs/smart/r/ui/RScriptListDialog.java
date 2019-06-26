/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.r.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptInterceptor;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * dialog for listing all r scripts
 * @author Emily
 *
 */
public class RScriptListDialog extends SmartStyledTitleDialog {

	private TableViewer cmbScripts;
	private List<RScript> script = null;
	private IStructuredSelection currentSelection;
	
	private MenuItem mnuEdit;
	private MenuItem mnuAdd;
	private MenuItem mnuDelete;
	
	private ToolItem tiNew;
	private ToolItem tiEdit;
	private ToolItem tiDelete;
	 
	private Job loadScript = new Job("load R scripts "){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			script = null;
			try(Session session = HibernateManager.openSession()){
				script = QueryFactory.buildQuery(session, RScript.class,
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
							.list(); 
				script.forEach(c->c.getName()); 
			}
			
			script.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (cmbScripts.getControl().isDisposed()) return;
					cmbScripts.setInput(script);
					cmbScripts.setSelection(currentSelection);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	public RScriptListDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.2));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite tcomp = new Composite(parent, SWT.NONE);
		tcomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		cmbScripts = new TableViewer(tcomp);
		cmbScripts.setContentProvider(ArrayContentProvider.getInstance());
		cmbScripts.setLabelProvider(new NamedItemLabelProvider());
		cmbScripts.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbScripts.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbScripts.getControl().setFocus();
		cmbScripts.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		cmbScripts.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				tiEdit.setEnabled(!cmbScripts.getSelection().isEmpty());
				tiDelete.setEnabled(!cmbScripts.getSelection().isEmpty());
				mnuEdit.setEnabled(!cmbScripts.getSelection().isEmpty());
				mnuDelete.setEnabled(!cmbScripts.getSelection().isEmpty());
			}
		});
		
		TableColumn singleColumn = new TableColumn(cmbScripts.getTable(), SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableColumnLayout.setColumnData(singleColumn, new ColumnWeightData(100));
		tcomp.setLayout(tableColumnLayout);
		
		ToolBar tb = new ToolBar(parent, SWT.VERTICAL | SWT.FLAT | SWT.RIGHT);
		tb.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		tiNew = new ToolItem(tb, SWT.PUSH);
		tiNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		tiNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiNew.addListener(SWT.Selection,e->add());
		
		tiEdit = new ToolItem(tb, SWT.PUSH);
		tiEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.addListener(SWT.Selection,e->edit());
		
		tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.addListener(SWT.Selection,e->delete());
		
		Menu menu = new Menu(cmbScripts.getControl());
		cmbScripts.getControl().setMenu(menu);

		mnuAdd = new MenuItem(menu, SWT.DEFAULT);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection,e->add());
		
		mnuEdit = new MenuItem(menu, SWT.DEFAULT);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		mnuEdit.addListener(SWT.Selection,e->edit());
		
		mnuDelete = new MenuItem(menu, SWT.DEFAULT);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection,e-> delete());
		
		tiNew.setEnabled(true);
		tiEdit.setEnabled(false);
		tiDelete.setEnabled(false);
		mnuAdd.setEnabled(true);
		mnuEdit.setEnabled(false);
		mnuDelete.setEnabled(false);
		
		
		setTitle(Messages.RScriptListDialog_Title);
		getShell().setText(Messages.RScriptListDialog_Title);
		setMessage(Messages.RScriptListDialog_Message);
		
		loadScript.setSystem(true);
		loadScript.schedule();
		
		return parent;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	private void add(){
		RScriptDialog dialog = new RScriptDialog(getShell());
		if (dialog.open() == Window.OK) {
			refresh();
		}	
	}

	private void edit(){
		Object x = ((IStructuredSelection)cmbScripts.getSelection()).getFirstElement();
		if (x instanceof RScript){
			RScript type = (RScript)x;
			RScriptDialog dialog = new RScriptDialog(getShell(), type);
			if (dialog.open() == Window.OK) {
				refresh();
			}
		}
	}
	
	private void delete(){
		List<RScript> toDelete = new ArrayList<RScript>();
		StringBuilder sb = new StringBuilder();
		
		for (Iterator<?> iterator = ((IStructuredSelection)cmbScripts.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof RScript){
				toDelete.add((RScript)x);
				sb.append(((RScript) x).getName());
				sb.append(", "); //$NON-NLS-1$
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.deleteCharAt(sb.length() - 1);
		
		if (!MessageDialog.openConfirm(getShell(), Messages.RScriptListDialog_DeleteTitle, MessageFormat.format(Messages.RScriptListDialog_DeleteMessage, sb.toString()))){
			return;
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {

					monitor.beginTask(Messages.RScriptListDialog_DeleteTask, toDelete.size());
					List<RScript> deleted = new ArrayList<RScript>();
					try(Session s = HibernateManager.openSession(new RScriptInterceptor())){

						for (RScript t : toDelete){
							monitor.subTask(t.getName());
							s.beginTransaction();
							try{
								s.delete(t);
								s.getTransaction().commit();
								deleted.add(t);
							}catch(Exception ex){
								s.getTransaction().rollback();
								RPlugIn.displayLog(MessageFormat.format(Messages.RScriptListDialog_DeleteError1, t.getName(), ex.getMessage()), ex);
							}
							monitor.worked(1);
						}
					}
					deleted.forEach(e->RunScriptMenuContribution.removeScript(e));
					monitor.done();
				}
			});
		} catch (Exception e) {
			RPlugIn.displayLog(Messages.RScriptListDialog_DeleteError2 + e.getMessage(), e);
		}
		refresh();
	}
	
	private void refresh(){
		if (cmbScripts.getControl().isDisposed()) return;
		
		currentSelection = (IStructuredSelection) cmbScripts.getSelection();
		cmbScripts.setInput(new String[]{DialogConstants.LOADING_TEXT});
		loadScript.schedule(0);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}