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
package org.wcs.smart.connect.ui.server.configure;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

public class UserInfoPanel implements IServerOptionsPanel{

	private Button btnEdit;
	private Button btnAdd;
	private Button btnDelete;
	private TableViewer tblUsers;
	
	private ConnectServer toUpdate = null;
	
	@Override
	public boolean isSupported(ConservationArea ca) {
		return true;
	}

	@Override
	public String getName() {
		return Messages.ConnectServerInfoDialog_UserAccountTab;
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public void initValues(ConnectServer server, Session session) {
		toUpdate = server;
		if (server == null) {
			btnAdd.setEnabled(false);
			tblUsers.setInput(new Object[]{});
			tblUsers.getTable().setEnabled(false);
		}else {
			btnAdd.setEnabled(true);
			List<?> users= QueryFactory.buildQuery(session, ConnectUser.class, "server", server).list(); //$NON-NLS-1$
			tblUsers.setInput(users);
			tblUsers.getTable().setEnabled(true);
		}
	}

	@Override
	public void updateServer(ConnectServer server, Session session) {
	}

	@Override
	public void afterSave(ConnectServer server) {
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void addChangeListener(ModifyListener listener) {
		
	}
	
	@Override
	public Composite createComposite(Composite parent, boolean isEditable) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		
		Composite tableComposite = new Composite(main, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.FILL ,SWT.FILL, true, true, 1, 1));
		TableColumnLayout layout = new TableColumnLayout();
		tableComposite.setLayout( layout );
		tblUsers = new TableViewer(tableComposite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tblUsers.setContentProvider(ArrayContentProvider.getInstance());
		tblUsers.getTable().setLayoutData(new GridData(SWT.FILL ,SWT.FILL, true, true));
		tblUsers.getTable().setHeaderVisible(true);
		tblUsers.getTable().setLinesVisible(true);
		
		TableViewerColumn colSmartUser = new TableViewerColumn(tblUsers, SWT.DEFAULT);
		colSmartUser.getColumn().setText(Messages.ConnectServerInfoDialog_SmartUserIdColumnLabel);
		
		layout.setColumnData(colSmartUser.getColumn(), new ColumnWeightData(20, 100, true));
		colSmartUser.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConnectUser){
					return ((ConnectUser) element).getSmartUser().getSmartUserId();
				}
				return super.getText(element);
			}
		});
		TableViewerColumn colSmartName = new TableViewerColumn(tblUsers, SWT.DEFAULT);
		colSmartName.getColumn().setText(Messages.ConnectServerInfoDialog_SmartNameColumnLabel);
		layout.setColumnData(colSmartName.getColumn(), new ColumnWeightData(40, 100, true));
		colSmartName.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConnectUser){
					return SmartLabelProvider.getFullLabel( ((ConnectUser) element).getSmartUser());
				}
				return super.getText(element);
			}
		});
		TableViewerColumn colConnectUser = new TableViewerColumn(tblUsers, SWT.DEFAULT);
		colConnectUser.getColumn().setText(Messages.ConnectServerInfoDialog_ConnectUsernameColumnLabel);
		layout.setColumnData(colConnectUser.getColumn(), new ColumnWeightData(30, 100, true));
		colConnectUser.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof ConnectUser){
					return ((ConnectUser) element).getConnectUsername();
				}
				return super.getText(element);
			}
		});
		
		Composite btnComp = new Composite(main, SWT.NONE);
		btnComp.setLayout(new GridLayout());
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnEdit = new Button(btnComp, SWT.PUSH);
		btnEdit.setText(Messages.ConnectServerInfoDialog_EditBnt);
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateAccountInfo();
			}
		});
		btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setText(Messages.ConnectServerInfoDialog_AddBtn);
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAccountInfo();
			}
		});
		btnDelete = new Button(btnComp, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteAccountInfo();
			}
		});
		
		tblUsers.getTable().addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnEdit.setEnabled(!tblUsers.getSelection().isEmpty());
				btnDelete.setEnabled(!tblUsers.getSelection().isEmpty());
			}
		});
		
		return main;
	}



	private void updateAccountInfo(){
		List<ConnectUser> users = new ArrayList<ConnectUser>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblUsers.getSelection()).iterator(); iterator.hasNext();) {
			ConnectUser connectUser = (ConnectUser) iterator.next();
			users.add(connectUser);
		}
		if (toUpdate == null) return;
		if (users.size() == 0) return;
		
		ConnectUserAccountDialog d = new ConnectUserAccountDialog(getShell(), toUpdate, users);
		if (d.open() == Dialog.OK){
			try(Session s = HibernateManager.openSession()){
				initValues(toUpdate, s);
			}
		}
				
				
	}
	private void addAccountInfo(){
		if (toUpdate == null) return;
		ConnectUserAccountDialog d = new ConnectUserAccountDialog(getShell(), toUpdate, null);
		if (d.open() == Dialog.OK){
			try(Session s = HibernateManager.openSession()){
				initValues(toUpdate, s);
			}
		}
				
				
	}
	
	private void deleteAccountInfo(){
		List<ConnectUser> users = new ArrayList<ConnectUser>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblUsers.getSelection()).iterator(); iterator.hasNext();) {
			ConnectUser connectUser = (ConnectUser) iterator.next();
			users.add(connectUser);
		}
		if (toUpdate == null) return;
		if (users.size() == 0) return;
		
		if (!MessageDialog.openConfirm(getShell(), Messages.ConnectServerInfoDialog_ConfirmTitle, MessageFormat.format(Messages.ConnectServerInfoDialog_ConfirmMessage, users.size()))){
			return;
		}
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				for (ConnectUser c : users){
					s.delete(c);
				}
				s.getTransaction().commit();
			}catch (Exception ex){
				ConnectPlugIn.displayLog(Messages.ConnectServerInfoDialog_AccountDeleteError +ex.getMessage(), ex);
			};
		}
		
		try(Session s = HibernateManager.openSession()){
			initValues(toUpdate, s);
		}
	}
	
	private Shell getShell() {
		return btnAdd.getShell();
	}
}
