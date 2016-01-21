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
package org.wcs.smart.ui.internal.ca.properties;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * A property page for managing CCAA users. 
 * @author Emily
 *
 */
public class CcaaUserPropertyPage extends AbstractPropertyJHeaderDialog{

	/* ui components */
	private ListViewer tblEmployee;
	private Button btnDelete;
	
	
	/**
	 * Creates a new agency and rank property page
	 */
	public CcaaUserPropertyPage(Shell parent) {
		super(parent, Messages.CcaaUserPropertyPage_ShellTitle);
	}
	
	@Override
	public Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
	
		tblEmployee = new ListViewer(container);
		tblEmployee.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblEmployee.getControl().getLayoutData()).heightHint = 150;
		tblEmployee.setContentProvider(ArrayContentProvider.getInstance());
		tblEmployee.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Employee){
					return ((Employee) element).getSmartUserId();
				}
				return super.getText(element);
			}
		});
		
		Composite btncomposite = new Composite(container, SWT.NONE);
		btncomposite.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		btncomposite.setLayout(new GridLayout());
		
		btnDelete = new Button(btncomposite, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setToolTipText(Messages.CcaaUserPropertyPage_DeleteTooltip);
		btnDelete.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteUsers();
			}
		});
		btnDelete.setEnabled(false);
		
		
		tblEmployee.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnDelete.setEnabled( !tblEmployee.getSelection().isEmpty() );
			}
		});
		
	
		refreshUserList();
		setTitle(Messages.CcaaUserPropertyPage_Title);
		setMessage(Messages.CcaaUserPropertyPage_Message);
		return container;
	}

	@SuppressWarnings("unchecked")
	private void refreshUserList() {
		tblEmployee.setInput(Messages.CcaaUserPropertyPage_Loading);
		Session s = getSession();
		s.beginTransaction();
		try{
			List<Employee> users = getSession().createCriteria(Employee.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.ne("uuid", Employee.SHARED_UUID)) //$NON-NLS-1$
				.list();
			tblEmployee.setInput(users);
		}finally{
			s.getTransaction().rollback();
		}
		
		tblEmployee.refresh();
	}
	
	
	/**
	 * deletes selected employee
	 */
	private void deleteUsers(){
		/* get employee to edit */
		IStructuredSelection sec = (IStructuredSelection)tblEmployee.getSelection();
		if (sec.isEmpty()){
			return;
		}
		@SuppressWarnings("unchecked")
		final List<Employee> toDelete = sec.toList();
		
		String message = null;
		if (toDelete.size() == 1){
			message = MessageFormat.format(
					Messages.CcaaUserPropertyPage_verifyDelete1,
					toDelete.get(0).getSmartUserId());
		}else{
			message = MessageFormat.format(
					Messages.CcaaUserPropertyPage_verifyDelete2, 
					toDelete.size());
		}
		if (!MessageDialog.openConfirm(getShell(), 
				Messages.CcaaUserPropertyPage_verifyDelete3,
				message
				)){
			return;
		}
		ProgressMonitorDialog pd = new ProgressMonitorDialog(getShell());
		final boolean[] restart = {false};
		
		try {
			pd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.CcaaUserPropertyPage_ProgressMsg, toDelete.size());
					Session s = getSession();
					s.beginTransaction();
					try{
						for (Employee del : toDelete){
							monitor.subTask(del.getSmartUserId());
							String deleteError = null;
							try{
								//first run before delete 
								ConservationAreaManager.getInstance()
									.fireEmployeeBeforeDelete(del, s);
								
								//validate delete
								if (!DeleteManager.canDelete(del, s)){
									deleteError = MessageFormat.format(
											Messages.CcaaUserPropertyPage_Error1, del.getSmartUserId());
								}else{
									//delete
									if (del.equals(SmartDB.getCurrentEmployee())){
										restart[0] = true;
									}
									s.delete(del);
								}
							}catch (Exception ex){
								deleteError =  MessageFormat.format(
										Messages.CcaaUserPropertyPage_Error2, del.getSmartUserId(), ex.getMessage());
								SmartPlugIn.log(ex.getMessage(), ex);
							}
							
							if (deleteError != null){
								displayError(deleteError);
							}
							monitor.worked(1);
						}
						s.getTransaction().commit();
					}catch ( final Exception ex){
						try{
							s.getTransaction().rollback();
						}catch (Exception ex2){
							SmartPlugIn.log("Error rolling back transaction", ex2); //$NON-NLS-1$
						}
						displayError(Messages.CcaaUserPropertyPage_Error3 + "\n\n" + ex.getMessage()); //$NON-NLS-1$
						SmartPlugIn.log(ex.getMessage(), ex);						
					}
				}
			});
		} catch (Exception e) {
			SmartPlugIn.displayLog(e.getLocalizedMessage(), e);
		}
		
		if (restart[0]){
			//restart
			PlatformUI.getWorkbench().restart();
			return;
		}
		refreshUserList();
	}
	
	private void displayError(final String message){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				MessageDialog.openInformation(getShell(), Messages.CcaaUserPropertyPage_DeleteDialogTitle, message);
			}});
	}

	/**
	 * Does nothing - changes are made in employee dialog.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	@Override
	protected boolean  performSave() {
		//Does nothing
		return true;
	}

	
}
