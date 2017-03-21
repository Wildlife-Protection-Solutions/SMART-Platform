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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaManager;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;
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
	private TableViewer tblCurrentUsers;
	
	/**
	 * Creates a new agency and rank property page
	 */
	public CcaaUserPropertyPage(Shell parent) {
		super(parent, Messages.CcaaUserPropertyPage_ShellTitle);
	}
	
	@Override
	public Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabFolder folder = new TabFolder(container, SWT.TOP);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		/* current user tab */
		TabItem currentUserTab = new TabItem(folder, SWT.DEFAULT);
		currentUserTab.setText(Messages.CcaaUserPropertyPage_CurrentUserTab);
		Composite currentComp = new Composite(folder, SWT.NONE);
		currentComp.setLayout(new GridLayout(2, false));
		currentComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		currentUserTab.setControl(currentComp);
		
		Label l = new Label(currentComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(currentComp, SWT.WRAP);
		l.setText(Messages.CcaaUserPropertyPage_CurrentUserInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 150;
		
		l = new Label(currentComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(currentComp, SWT.NONE);
		l.setText(Messages.CcaaUserPropertyPage_CurrentUserLabel);
		
		l = new Label(currentComp, SWT.NONE);
		l.setText(SmartDB.getCurrentEmployee().getSmartUserId());
		
		l = new Label(currentComp, SWT.WRAP);
		l.setText(Messages.CcaaUserPropertyPage_CaPermissionsLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		tblCurrentUsers = new TableViewer(currentComp, SWT.SINGLE | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.BORDER);
		tblCurrentUsers.setContentProvider(ArrayContentProvider.getInstance());
		tblCurrentUsers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		tblCurrentUsers.setInput(new String[]{DialogConstants.LOADING_TEXT});
		tblCurrentUsers.getTable().setHeaderVisible(true);
		TableViewerColumn caColumn = new TableViewerColumn(tblCurrentUsers, SWT.DEFAULT);
		caColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof Employee){
					ConservationArea ca = ((Employee)element).getConservationArea();
					return ca.getNameLabel();
				}
				return super.getText(element);
			}
			
			public Color getForeground(Object element) {
				if (element instanceof Employee){
					ConservationArea ca = ((Employee)element).getConservationArea();
					if (SmartDB.getConservationAreaConfiguration().getConservationAreas().contains(ca)){
						return null;
					}
				}
				return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		});
		caColumn.getColumn().setText(Messages.CcaaUserPropertyPage_CaTableColumn);
		caColumn.getColumn().setWidth(200);
		
		
		TableViewerColumn eColumn = new TableViewerColumn(tblCurrentUsers, SWT.DEFAULT);
		eColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof Employee){
					return SmartLabelProvider.getFullLabel(((Employee)element));
				}
				return super.getText(element);
			}
			public Color getForeground(Object element) {
				if (element instanceof Employee){
					ConservationArea ca = ((Employee)element).getConservationArea();
					if (SmartDB.getConservationAreaConfiguration().getConservationAreas().contains(ca)){
						return null;
					}
				}
				return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		});
		eColumn.getColumn().setText(Messages.CcaaUserPropertyPage_EmployeeTableColumn);
		eColumn.getColumn().setWidth(200);
		
		TableViewerColumn pColumn = new TableViewerColumn(tblCurrentUsers, SWT.DEFAULT);
		pColumn.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof Employee){
					String name = ""; //$NON-NLS-1$
					for (String user : ((Employee) element).getSmartUserLevels()){
						name += user +", "; //$NON-NLS-1$
					}
					name = name.substring(0, name.length() - 2);
					return name;
				}
				return super.getText(element);
			}
			
			public Color getForeground(Object element) {
				if (element instanceof Employee){
					ConservationArea ca = ((Employee)element).getConservationArea();
					if (SmartDB.getConservationAreaConfiguration().getConservationAreas().contains(ca)){
						return null;
					}
				}
				return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		});
		pColumn.getColumn().setText(Messages.CcaaUserPropertyPage_PermissionColumn);
		pColumn.getColumn().setWidth(200);
		
		
		/* all users tab */
		TabItem usersTab = new TabItem(folder, SWT.DEFAULT);
		usersTab.setText(Messages.CcaaUserPropertyPage_AllUsersTab);
		Composite usersComp = new Composite(folder, SWT.NONE);
		usersComp.setLayout(new GridLayout(2, false));
		usersComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		usersTab.setControl(usersComp);
		
		l = new Label(usersComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		l = new Label(usersComp, SWT.WRAP);
		l.setText(Messages.CcaaUserPropertyPage_AllUsersInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).widthHint = 150;
		
		l = new Label(usersComp, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		tblEmployee = new ListViewer(usersComp);
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
		
		Composite btncomposite = new Composite(usersComp, SWT.NONE);
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
		setMessage(Messages.CcaaUserPropertyPage_Message1);
		return container;
	}

	@SuppressWarnings("unchecked")
	private void refreshUserList() {
		tblEmployee.setInput(Messages.CcaaUserPropertyPage_Loading);
		Session s = HibernateManager.openSession();;
		
		try{
			List<Employee> users = s.createCriteria(Employee.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.ne("uuid", Employee.SHARED_UUID)) //$NON-NLS-1$
				.list();
			tblEmployee.setInput(users);
			
			List<Employee> currentUsers = new ArrayList<Employee>();
			
			for (Employee e : SmartDB.getConservationAreaConfiguration().getEmployees()){
				Employee emp = (Employee) s.get(Employee.class, e.getUuid());
				emp.getConservationArea().getNameLabel();
				currentUsers.add(emp);
			}
			tblCurrentUsers.setInput(currentUsers);
		}finally{
			s.close();
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
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try{
						for (Employee del : toDelete){
							del = (Employee) s.get(Employee.class, del.getUuid());
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
					}finally{
						s.close();
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
