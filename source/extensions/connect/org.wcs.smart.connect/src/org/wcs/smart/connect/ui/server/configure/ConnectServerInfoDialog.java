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

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.CaConnectDeleteHandler;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Connect server info dialog for displaying server details.
 * 
 */
public class ConnectServerInfoDialog extends TitleAreaDialog {

	public static final String ID = "org.wcs.smart.preference.connect.ServerConfiguration"; //$NON-NLS-1$
	
	private Label txtServer;
	
	private ConnectServer toUpdate;
	private Button btnSet;
	private Button btnEditServer;
	private Button btnEdit;
	private Button btnAdd;
	private Button btnDelete;
	private Button btnShowReplication;
	private TableViewer tblUsers;
		
	private IServerOptionsPanel[]  optionPanels = OptionPanelManager.createOptionPanels();
	
	/**
	 * Default constructor
	 */
	public ConnectServerInfoDialog(Shell parent) {
		super(parent);
	}

	public int open(){
		return super.open();
		
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group g = new Group(main, SWT.FLAT );
		g.setText(Messages.ConnectServerInfoDialog_ServerLabel);
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g.setLayout(new GridLayout(3, false));
		
		Label lblServer = new Label(g, SWT.NONE);
		lblServer.setText(Messages.ConnectServerInfoDialog_urlLabel);
		
		txtServer = new Label(g, SWT.NONE);
		txtServer.setText(Messages.ConnectServerInfoDialog_urlText);
		txtServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		

		Composite btnPanel = new Composite(g, SWT.NONE);
		btnPanel.setLayout(new GridLayout(2, true));
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
		
		
		
		btnEditServer = new Button(btnPanel, SWT.PUSH);
		btnEditServer.setText(Messages.ConnectServerInfoDialog_editButton);
		btnEditServer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEditServer.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editServer();
			}
		});
		btnEditServer.setToolTipText(Messages.ConnectServerInfoDialog_editTooltip);
		
		btnSet = new Button(btnPanel, SWT.PUSH);
		btnSet.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnSet.getLayoutData()).widthHint = 50;
		btnSet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setServer();
			}
		});
		btnSet.setToolTipText(Messages.ConnectServerInfoDialog_resetTooltip);
		
		TabFolder tabConfig = new TabFolder(g, SWT.NONE);
		tabConfig.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,3,1));
		
		TabItem ti = new TabItem(tabConfig, SWT.DEFAULT);
		ti.setText(Messages.ConnectServerInfoDialog_UserAccountTab);
		ti.setControl(createUserAccountsTab(tabConfig));
		
		for (IServerOptionsPanel p : optionPanels){
			ti = new TabItem(tabConfig, SWT.DEFAULT);
			ti.setText(p.getName());
			ti.setControl(p.createComposite(tabConfig, false));
		}
		
		btnShowReplication = new Button(main, SWT.PUSH);
		btnShowReplication.setText(Messages.ConnectServerInfoDialog_ReplicationBtn);
		btnShowReplication.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ReplicationInfoDialog dialog = new ReplicationInfoDialog(getShell());
				dialog.open();
			}
		});
		
		initControls();
		
		setTitle(Messages.ConnectServerInfoDialog_Title);
		getShell().setText(Messages.ConnectServerInfoDialog_ShellTitle);
		setMessage(Messages.ConnectServerInfoDialog_Message);
		
		return main;
	}
	
	private void initControls(){

		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			ConnectServer server = ConnectHibernateManager.getConnectServer(session);
			
			if (server == null){
				toUpdate = null;
				txtServer.setText(Messages.ConnectServerInfoDialog_NotSet);
				btnSet.setText(Messages.ConnectServerInfoDialog_SetButton);
				btnEditServer.setEnabled(false);
				btnAdd.setEnabled(false);
				tblUsers.setInput(new Object[]{});
				tblUsers.getTable().setEnabled(false);
		
				for (IServerOptionsPanel pnl :optionPanels){
					pnl.initValues(null);
				}
			}else{
				toUpdate = server;
				txtServer.setText(server.getServerUrl());
				btnSet.setText(Messages.ConnectServerInfoDialog_ResetButton);
				btnEditServer.setEnabled(true);
				
				
				btnAdd.setEnabled(true);
				List<?> users= session.createCriteria(ConnectUser.class)
						.add(Restrictions.eq("server", toUpdate)) //$NON-NLS-1$
						.list();
				tblUsers.setInput(users);
				tblUsers.getTable().setEnabled(true);
				
				for (IServerOptionsPanel pnl :optionPanels){
					pnl.initValues(toUpdate);
				}
			}
		}finally{
			session.getTransaction().rollback();
			session.close();
		}
	}
	
	private Composite createUserAccountsTab(Composite parent){
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
	private void setServer(){
		if (toUpdate != null){
			//we need to display a warning that all user and replication information will be lost
			MessageDialog md = new MessageDialog(
					getShell(), Messages.ConnectServerInfoDialog_ResetDialogTitle, null, 
					Messages.ConnectServerInfoDialog_ResetDialogMessage,
					MessageDialog.WARNING, 
					new String[]{IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL}, 1);
			if (md.open() == 1){
				return;
			}
			//delete all existing server information
			if (!deleteServerInfo()) return;
		}

		//get new server information
		ConnectServerWizard wz = new ConnectServerWizard();
		WizardDialog wd = new WizardDialog(getShell(), wz);
		wd.open();
		initControls();
	}
	
	private boolean deleteServerInfo(){
		//turn off replication
		//delete all change log items
		//delete all status records
		//delete all history records
		//delete a connect users
		//delete all server information
		final boolean[] ret = new boolean[]{true};
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
		pmd.run(true, true, new IRunnableWithProgress() {
			
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				monitor.beginTask(Messages.ConnectServerInfoDialog_DeleteServerTaskName, 6);
				
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();

					ConservationArea ca = SmartDB.getCurrentConservationArea();
					(new CaConnectDeleteHandler()).beforeDelete(ca, s, new SubProgressMonitor(monitor, 6));
					
					s.getTransaction().commit();
					toUpdate = null;
				}catch (Exception ex){
					ret[0] = false;
					ConnectPlugIn.displayLog(Messages.ConnectServerInfoDialog_DeleteError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				}finally{
					if (s.getTransaction().isActive()){
						s.getTransaction().rollback();
					}
					s.close();
				}
					
				monitor.done();
			}
		});
		}catch(Exception ex){
			ConnectPlugIn.displayLog(Messages.ConnectServerInfoDialog_DeleteError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			ret[0] = false;
		}
		return ret[0];
	}
	private void editServer(){
		if (toUpdate == null) return;
		
		EditConnectServerInfoDialog dialog = new EditConnectServerInfoDialog(getShell(),toUpdate);
		dialog.open();
		initControls();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private void updateAccountInfo(){
		List<ConnectUser> users = new ArrayList<ConnectUser>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblUsers.getSelection()).iterator(); iterator.hasNext();) {
			ConnectUser connectUser = (ConnectUser) iterator.next();
			users.add(connectUser);
		}
		if (toUpdate == null) return;
		if (users.size() == 0) return;
		
		ConnectUserAccountDialog d = new ConnectUserAccountDialog(getParentShell(), toUpdate, users);
		if (d.open() == Dialog.OK){
			initControls();
		}
				
				
	}
	private void addAccountInfo(){
		if (toUpdate == null) return;
		ConnectUserAccountDialog d = new ConnectUserAccountDialog(getParentShell(), toUpdate, null);
		if (d.open() == Dialog.OK){
			initControls();
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
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			for (ConnectUser c : users){
				s.delete(c);
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.displayLog(Messages.ConnectServerInfoDialog_AccountDeleteError +ex.getMessage(), ex);
		}finally{
			s.close();
		}
		initControls();
				
				
	}

}
