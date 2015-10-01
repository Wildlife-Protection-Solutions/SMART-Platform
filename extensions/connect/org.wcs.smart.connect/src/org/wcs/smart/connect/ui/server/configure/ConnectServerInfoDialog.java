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
import java.util.HashMap;
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
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.CaConnectDeleteHandler;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServer.Option;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;

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
		
	private HashMap<Option, Label> optionCntrls;
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
		g.setText("Connect Server");
		g.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		g.setLayout(new GridLayout(3, false));
		
		Label lblServer = new Label(g, SWT.NONE);
		lblServer.setText("URL:");
		
		txtServer = new Label(g, SWT.NONE);
		txtServer.setText("<Server URL>");
		txtServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		

		Composite btnPanel = new Composite(g, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2));
		
		btnSet = new Button(btnPanel, SWT.PUSH);
		btnSet.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnSet.getLayoutData()).widthHint = 50;
		btnSet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setServer();
			}
		});
		
		btnEditServer = new Button(btnPanel, SWT.PUSH);
		btnEditServer.setText("Edit");
		btnEditServer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnEditServer.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editServer();
			}
		});
		
		Composite c = new Composite(g, SWT.FLAT );
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		c.setLayout(new GridLayout(4, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 7;
		
		optionCntrls = new HashMap<ConnectServer.Option, Label>();
		for (ConnectServer.Option op : ConnectServer.Option.values()){
			Label l = new Label(c, SWT.NONE);
			l.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(op) + ":");
			l.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(op));
			
			Label lblValue = new Label(c, SWT.NONE);
			optionCntrls.put(op, lblValue);
			lblValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
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

		btnShowReplication = new Button(main, SWT.PUSH);
		btnShowReplication.setText("View Replication Information");
		btnShowReplication.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ReplicationInfoDialog dialog = new ReplicationInfoDialog(getShell());
				dialog.open();
			}
		});
		
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
				btnEditServer.setEnabled(false);
				btnAdd.setEnabled(false);
				tblUsers.setInput(new Object[]{});
				tblUsers.getTable().setEnabled(false);
				
				for (Label l : optionCntrls.values()){
					l.setText("N/A");
				}
			}else{
				toUpdate = server;
				txtServer.setText(server.getServerUrl());
				btnSet.setText("Re-Set");
				btnEditServer.setEnabled(true);
				
				
				btnAdd.setEnabled(true);
				List<?> users= session.createCriteria(ConnectUser.class)
						.add(Restrictions.eq("server", toUpdate))
						.list();
				tblUsers.setInput(users);
				tblUsers.getTable().setEnabled(true);
				
				for (Option o : ConnectServer.Option.values()){
					String value = ServerOptionLabelProvider.INSTANCE.getValueInDisplayUnits(o, toUpdate);
					optionCntrls.get(o).setText(value);
				}
			}
		}finally{
			session.close();
		}
	}
	
	private void setServer(){
		if (toUpdate != null){
			//we need to display a warning that all user and replication information will be lost
			MessageDialog md = new MessageDialog(
					getShell(), "ReSet Server", null, 
					"Resetting the SMART Connect Server will delete all current server configuration.  You will no longer be able to sync changes.  Only do this if you want to upload the Conservation Area to a new SMART Connect server.  If you just want to modify this information cancel this dialog and use the Edit button instead.\n\nAre you sure you want to continue?",
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
				monitor.beginTask("Deleting Existing Server Configuration", 7);
				
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					
					monitor.subTask("Disable replication");
					DerbyReplicationManager.INSTANCE.disableReplication(s);
					monitor.worked(1);
					if (monitor.isCanceled()) return;
					
					ConservationArea ca = SmartDB.getCurrentConservationArea();
					(new CaConnectDeleteHandler()).beforeDelete(ca, s, new SubProgressMonitor(monitor, 6));
					
					s.getTransaction().commit();
					toUpdate = null;
				}catch (Exception ex){
					ret[0] = false;
					ConnectPlugIn.displayLog("Could not remove current server configurations." + "\n\n" + ex.getMessage(), ex);				
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
			ConnectPlugIn.displayLog("Could not remove current server configurations." + "\n\n" + ex.getMessage(), ex);
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
