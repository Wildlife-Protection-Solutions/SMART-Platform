/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.configure;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.ui.configure.create.EditRoutineDialog;
import org.wcs.smart.qa.ui.configure.create.NewRoutineWizard;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog listing all QA Routines. 
 * 
 * @author Emily
 *
 */
public class RoutinesListDialog extends TitleAreaDialog {

	private TableViewer tblRoutines;
	
	public enum RoutineColumn{
		TYPE("Routine", "The quality assurance routine"),
		NAME("Name", "User defined name for uniquely identifying the routine"),
		AUTO("Auto Execute", "If routine should be auto executed when new data is added to the system"),
		DESC("Description", "Optional user defined description for the routine"),
		PARAMETERS("Parameters", "QA Routine parameters");
		public String guiName;
		public String tooltip;
		
		RoutineColumn(String guiName, String tooltip){
			this.guiName = guiName;
			this.tooltip = tooltip;
		}
	}
	
	public RoutinesListDialog(Shell parentShell) {
		super(parentShell);
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	 
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite)super.createDialogArea(parent);
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout(2, false));
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblRoutines = new TableViewer(outer, SWT.BORDER | SWT.FULL_SELECTION);
		tblRoutines.setContentProvider(ArrayContentProvider.getInstance());
		tblRoutines.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblRoutines.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		for (RoutineColumn c : RoutineColumn.values()){
			TableViewerColumn typeColumn = new TableViewerColumn(tblRoutines, SWT.NONE);
			typeColumn.getColumn().setText(c.guiName);
			typeColumn.getColumn().setToolTipText(c.tooltip);
			typeColumn.setLabelProvider(new ColumnLabelProvider(){
				@Override
				public String getText(Object element) {
					return getValue(c, element);
				}
			});
			typeColumn.getColumn().setWidth(150);
		}
		tblRoutines.getTable().setLinesVisible(true);
		tblRoutines.getTable().setHeaderVisible(true);
		
		Composite buttonPnl = new Composite(outer, SWT.NONE);
		buttonPnl.setLayout(new GridLayout());
		((GridLayout)buttonPnl.getLayout()).marginWidth = 0;
		((GridLayout)buttonPnl.getLayout()).marginHeight = 0;
		buttonPnl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		
		Button btnNew = new Button(buttonPnl, SWT.PUSH);
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.addListener(SWT.Selection, e->add());
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnEdit = new Button(buttonPnl, SWT.PUSH);
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addListener(SWT.Selection, e->edit());
		btnEdit.setEnabled(false);
		
		Button btnDelete = new Button(buttonPnl, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addListener(SWT.Selection, e->delete());
		btnDelete.setEnabled(false);
		
		Menu mnu = new Menu(tblRoutines.getControl());
		tblRoutines.getControl().setMenu(mnu);
		MenuItem mnuNew = new MenuItem(mnu, SWT.PUSH);
		mnuNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuNew.addListener(SWT.Selection, e->add());
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->edit());
		mnuEdit.setEnabled(false);
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->delete());
		mnuDelete.setEnabled(false);

		tblRoutines.addSelectionChangedListener(e->{
			boolean enabled = false;
			if (!tblRoutines.getSelection().isEmpty()){
				if ( ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement() instanceof QaRoutine){
					enabled = true;
				}
			}
			mnuDelete.setEnabled(enabled);
			mnuEdit.setEnabled(enabled);
			btnDelete.setEnabled(enabled);
			btnEdit.setEnabled(enabled);
			
		});
		
		refresh();
		
		getShell().setText("QA Routines");
		setTitle("Quality Assurance Routines");
		setMessage("Currently configures quality assurance routines");
		return parent;
	}
	
	private void add(){
		WizardDialog ws = new WizardDialog(getShell(), new NewRoutineWizard());
		if (ws.open() == Window.OK){
			refresh();
		}
	}
	
	private void edit(){
		if (tblRoutines.getSelection().isEmpty()) return;
		Object x = ((IStructuredSelection)tblRoutines.getSelection()).getFirstElement();
		if (!(x instanceof QaRoutine)) return;
		QaRoutine r = (QaRoutine)x;
		EditRoutineDialog dialog = new EditRoutineDialog(getShell(),r);
		if (dialog.open() == Window.OK){
			refresh();
		}
	}
	
	private void delete(){
		final List<QaRoutine> toDelete = new ArrayList<QaRoutine>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblRoutines.getSelection()).iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof QaRoutine){
				toDelete.add((QaRoutine)x);
			}
		}
		if (toDelete.isEmpty()) return;
		if (!MessageDialog.openConfirm(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected QA Routines? This action cannot be undone.", toDelete.size()))){
			return;
		}
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
		pmd.run(true, false, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask("Deleting QA Routines", toDelete.size());
				Session s = HibernateManager.openSession();
				s.getTransaction().begin();
				try{
					for (QaRoutine r : toDelete){
						Query q = s.createQuery("DELETE FROM QaError where qaRoutine = :r"); //$NON-NLS-1$
						q.setParameter("r", r); //$NON-NLS-1$
						q.executeUpdate();
						
						s.delete(r);
						monitor.worked(1);
					}
					
					s.getTransaction().commit();
				}catch (Exception ex){
					s.getTransaction().rollback();
					throw new InvocationTargetException(ex);
				}finally{
					s.close();
				}
				monitor.done();
			}
		});
		}catch (Exception ex){
			String msg = ex.getMessage();
			if (msg == null && ex.getCause() != null) msg = ex.getCause().getMessage();
			QaPlugIn.displayLog("Error deleting Quality Assurance routines. " + msg, ex);
		}
		refresh();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	
	private String getValue(RoutineColumn field, Object element){
		if (element == null) return "";
		if (!(element instanceof QaRoutine)) return element.toString();
		QaRoutine r = (QaRoutine)element;
		switch(field){
			case AUTO:
				if (r.getAutoCheck()){
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				}else{
					return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
				}
			case DESC:
				if (r.getDescription() == null) return ""; //$NON-NLS-1$
				return r.getDescription();
			case NAME:
				return r.getName();
			case TYPE:
				IQaRoutineType type = r.getRoutineType();
				if (type == null) return "Not Defined";
				return type.getName(Locale.getDefault());
			case PARAMETERS:
				type = r.getRoutineType();
				if (type == null) return "Not Defined";
				return type.getParameterSummary(r);
		}
		return "";
	}
	
	private void refresh(){
		tblRoutines.setInput(new String[]{DialogConstants.LOADING_TEXT});
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}
	
	private Job refreshJob = new Job("refresh"){ //$NON-NLS-1$

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QaRoutine> routines = new ArrayList<>();
		
			Session s = HibernateManager.openSession();
			try{
				routines.addAll(s.createCriteria(QaRoutine.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.list());
				for (QaRoutine r : routines){
					r.getParameters().size();
				}
			}finally{
				s.close();
			}
			Display.getDefault().asyncExec(()->{
				tblRoutines.setInput(routines);
			});
			return Status.OK_STATUS;
		}
		
	};
}
