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
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
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
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectSyncHistoryRecord;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.server.replication.ChangeLogTableManager;
import org.wcs.smart.connect.server.replication.SyncHistoryManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Connect server info dialog.
 */
public class ConnectServerInfoDialog extends TitleAreaDialog {

	public static final String ID = "org.wcs.smart.preference.connect.ServerConfiguration"; //$NON-NLS-1$
	
	private Label txtServer;
	
	private ConnectServer toUpdate;
	private Button btnSet;
	private Button btnEdit;
	private Button btnAdd;
	private Button btnDelete;
	private TableViewer tblUsers;
	
	private Label lblServerVersion;
	private Label lblServerRevision;
	private Label lblLocalChanges;
	
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
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group g = new Group(main, SWT.FLAT );
		g.setText("Connect Server");
		g.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		g.setLayout(new GridLayout(3, false));
		
		Label lblServer = new Label(g, SWT.NONE);
		lblServer.setText("URL:");
		
		txtServer = new Label(g, SWT.NONE);
		txtServer.setText("<Server URL>");
		txtServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btnSet = new Button(g, SWT.PUSH);
		btnSet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setUrl();
			}
		});
		
		g = new Group(main, SWT.FLAT );
		g.setText("User Accounts");
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		g.setLayout(new GridLayout(2, false));
		
		Composite tableComposite = new Composite(g, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.FILL ,SWT.FILL, true, true, 1, 1));
		TableColumnLayout layout = new TableColumnLayout();
		tableComposite.setLayout( layout );
		tblUsers = new TableViewer(tableComposite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		tblUsers.setContentProvider(ArrayContentProvider.getInstance());
		tblUsers.getTable().setLayoutData(new GridData(SWT.FILL ,SWT.FILL, true, true));
		tblUsers.getTable().setHeaderVisible(true);
		tblUsers.getTable().setLinesVisible(true);
		
		TableViewerColumn colSmartUser = new TableViewerColumn(tblUsers, SWT.DEFAULT);
		colSmartUser.getColumn().setText("SMART User ID");
		
		layout.setColumnData(colSmartUser.getColumn(), new ColumnWeightData(30, 100, true));
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
		colSmartName.getColumn().setText("SMART Name");
		layout.setColumnData(colSmartName.getColumn(), new ColumnWeightData(30, 100, true));
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
		colConnectUser.getColumn().setText("Connect Username");
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
		
		Composite btnComp = new Composite(g, SWT.NONE);
		btnComp.setLayout(new GridLayout());
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnEdit = new Button(btnComp, SWT.PUSH);
		btnEdit.setText("Edit...");
		btnEdit.setEnabled(false);
		btnEdit.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateAccountInfo();
			}
		});
		btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setText("Add...");
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				addAccountInfo();
			}
		});
		btnDelete = new Button(btnComp, SWT.PUSH);
		btnDelete.setText("Delete");
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

		g = new Group(main, SWT.FLAT );
		g.setText("Replication Information");
		g.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		g.setLayout(new GridLayout(2, false));
		
		Label l = new Label(g, SWT.NONE);
		l.setText("Server Version:");
		lblServerVersion = new Label(g, SWT.NONE);
		lblServerVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		l = new Label(g, SWT.NONE);
		l.setText("Last Server Revision:");
		lblServerRevision = new Label(g, SWT.NONE);
		lblServerRevision.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		l = new Label(g, SWT.NONE);
		l.setText("Local Changes:");
		lblLocalChanges = new Label(g, SWT.NONE);
		lblLocalChanges.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false,1,1));
		
		
		initControls();
		
		setTitle("SMART Connect Configuration");
		getShell().setText("SMART Connect Configuration");
		setMessage("Configure your connection to a SMART Connect Server here.");
		
		return main;
	}
	
	private void initControls(){

		Session session = HibernateManager.openSession();
		try{
			ConnectServer server = (ConnectServer)session.createCriteria(ConnectServer.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.uniqueResult();
			
			if (server == null){
				toUpdate = null;
				txtServer.setText("<Not Set>");
				btnSet.setText("Set");
				btnAdd.setEnabled(false);
				tblUsers.setInput(new Object[]{});
				tblUsers.getTable().setEnabled(false);
				
				lblLocalChanges.setText("N/A");
				lblServerVersion.setText("N/A");
				lblServerRevision.setText("N/A");
			}else{
				toUpdate = server;
				txtServer.setText(server.getServerUrl());
				btnSet.setText("Change");
				btnAdd.setEnabled(true);
				List<?> users= session.createCriteria(ConnectUser.class)
						.add(Restrictions.eq("server", toUpdate))
						.list();
				tblUsers.setInput(users);
				tblUsers.getTable().setEnabled(true);
				
				ConnectServerStatus status = (ConnectServerStatus) session.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());
				if (status == null){
					lblLocalChanges.setText("unknown");
					lblServerVersion.setText("unknown");
					lblServerRevision.setText("unknown");
				}else{
					lblServerVersion.setText( UuidUtils.uuidToString(status.getVersion()));
					lblServerRevision.setText( status.getServerRevision().toString() );
					
					ConnectSyncHistoryRecord  rec = SyncHistoryManager.INSTANCE.getLastNonErrorSyncRecord(session, SmartDB.getCurrentConservationArea(), ConnectSyncHistoryRecord.Type.UPLOAD);
					Long currentRevision = ChangeLogTableManager.INSTANCE.getMaxRevision(session, SmartDB.getCurrentConservationArea());
					if (rec == null && currentRevision == null){
						lblLocalChanges.setText("None");
					}else if (rec == null && currentRevision != null){
						lblLocalChanges.setText("Yes");
					}else if (rec != null && currentRevision == null){
						lblLocalChanges.setText("ERROR");
					}else if (rec != null && currentRevision != null){
						if (currentRevision.longValue() > rec.getEndRevision().longValue()){
							lblLocalChanges.setText("Yes");
						}else if (currentRevision.longValue() == rec.getEndRevision().longValue()){
							lblLocalChanges.setText("No");
						}else{
							lblLocalChanges.setText("ERROR");
						}
					}
					
				}
			}
			
			
		}finally{
			session.close();
		}
	}
	
	private void setUrl(){
		if (toUpdate != null){
			//we need to display a warning that all user and replication information will be lost
			if (!MessageDialog.openQuestion(getShell(), "Connect Server", "Modifying the SMART Connect Server will remove all associated account and replication data from your local database. Are you sure you want to continue?" )){
				return;
			}
		}
		
		ConnectServerWizard wz = new ConnectServerWizard();
		WizardDialog wd = new WizardDialog(getShell(), wz);
		if (wd.open() == Dialog.OK){
			initControls();
		}
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
		
		if (!MessageDialog.openConfirm(getShell(), "Confirm", MessageFormat.format("Are you sure you want to delete the {0} selected account references?", users.size()))){
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
			ConnectPlugIn.displayLog("Error deleting account information: " +ex.getMessage(), ex);
		}finally{
			s.close();
		}
		initControls();
				
				
	}

}
