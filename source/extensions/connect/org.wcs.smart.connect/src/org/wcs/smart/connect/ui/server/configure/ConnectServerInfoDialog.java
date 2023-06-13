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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.ConnectServerManager;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.SmartWizardDialog;


/**
 * Connect server info dialog for displaying server details.
 * 
 */
public class ConnectServerInfoDialog extends SmartStyledTitleDialog {

	public static final String ID = "org.wcs.smart.preference.connect.ServerConfiguration"; //$NON-NLS-1$
	
	private Label txtServer;
	
	private ConnectServer toUpdate;
	private Button btnSet;
	private Button btnEditServer;
	
	private Button btnShowReplication;
	
	private List<IServerOptionsPanel> allPanels;
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
		
		SmartUiUtils.createHeaderLabel(main, Messages.ConnectServerInfoDialog_ServerLabel);
		
		Composite g = new Composite(main, SWT.FLAT );
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setLayout(new GridLayout(3, false));
		
		Label lblServer = new Label(g, SWT.NONE);
		lblServer.setText(Messages.ConnectServerInfoDialog_urlLabel);
		lblServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		
		txtServer = new Label(g, SWT.NONE);
		txtServer.setText(Messages.ConnectServerInfoDialog_urlText);
		txtServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Composite btnPanel = new Composite(g, SWT.NONE);
		btnPanel.setLayout(new GridLayout(2, true));
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 2));
		((GridLayout)btnPanel.getLayout()).marginHeight = 0;
		
		btnEditServer = new Button(btnPanel, SWT.PUSH);
		btnEditServer.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEditServer.setBackground(btnPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
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
		btnSet.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		btnSet.setBackground(btnPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnSet.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnSet.getLayoutData()).widthHint = 70;
		btnSet.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setServer();
			}
		});
		btnSet.setToolTipText(Messages.ConnectServerInfoDialog_resetTooltip);
		
		SmartUiUtils.createHeaderLabel(main, Messages.ConnectServerInfoDialog_SettingsSection);
		
		Composite partComposite = new Composite(main, SWT.FLAT );
		partComposite.setLayout(new GridLayout(2, false));
		partComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		allPanels = new ArrayList<>();
		allPanels.add(new UserInfoPanel());
		for (IServerOptionsPanel pp : OptionPanelManager.createOptionPanels(SmartDB.getCurrentConservationArea())){
			allPanels.add(pp);
		}
		
		ListViewer lstViewer = new ListViewer(partComposite, SWT.BORDER | SWT.V_SCROLL);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		lstViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IServerOptionsPanel) return ((IServerOptionsPanel) element).getName();
				return super.getText(element);
			}
		});
		lstViewer.setInput(allPanels);
		lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite rightPanel = new Composite(partComposite, SWT.BORDER);
		rightPanel.setLayout(new StackLayout());
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((StackLayout)rightPanel.getLayout()).marginWidth = 0;
		((StackLayout)rightPanel.getLayout()).marginHeight = 0;

		HashMap<IServerOptionsPanel, Composite> panels = new HashMap<>();
		for (IServerOptionsPanel p : allPanels){
			ScrolledComposite scroll = new ScrolledComposite(rightPanel, SWT.V_SCROLL | SWT.H_SCROLL);
			Composite part = p.createComposite(scroll, false);
			scroll.setContent(part);
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			scroll.setMinSize(part.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			panels.put(p,  scroll);
		}
		
		lstViewer.addSelectionChangedListener(e->{
			IServerOptionsPanel pp = (IServerOptionsPanel)lstViewer.getStructuredSelection().getFirstElement();
			((StackLayout)rightPanel.getLayout()).topControl = panels.get(pp);	
			rightPanel.layout();
		});
		
		lstViewer.setSelection(new StructuredSelection(allPanels.get(0)));
		
		btnShowReplication = new Button(main, SWT.PUSH);
		btnShowReplication.setText(Messages.ConnectServerInfoDialog_ReplicationBtn);
		btnShowReplication.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
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

		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				ConnectServer server = ConnectHibernateManager.getConnectServer(session);
				
				if (server == null){
					toUpdate = null;
					txtServer.setText(Messages.ConnectServerInfoDialog_NotSet);
					btnSet.setText(Messages.ConnectServerInfoDialog_SetButton);
					btnEditServer.setEnabled(false);
					for (IServerOptionsPanel pnl : allPanels){
						pnl.initValues(null, session);
					}
				}else{
					toUpdate = server;
					txtServer.setText(server.getServerUrl());
					btnSet.setText(Messages.ConnectServerInfoDialog_ResetButton);
					btnEditServer.setEnabled(true);
					
					for (IServerOptionsPanel pnl : allPanels){
						pnl.initValues(toUpdate, session);
					}
				}
			}finally{
				session.getTransaction().rollback();
			}
		}
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
		WizardDialog wd = new SmartWizardDialog(getShell(), wz);
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
				try{
					ConnectServerManager.INSTANCE.deleteConnectServerData(monitor);
					toUpdate = null;
					ret[0] = true;
				}catch (Exception ex){
					ret[0] = false;
					ConnectPlugIn.displayLog(Messages.ConnectServerInfoDialog_DeleteError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				}
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
		if (!PlatformUI.getWorkbench().isClosing()) initControls();
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	

}
