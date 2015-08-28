package org.wcs.smart.connect.ui.server;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.configure.ShowServerConfigurationHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public abstract class ConnectDialog extends TitleAreaDialog {

	private Label lblServer;
	
	private Text txtUser;
	private Text txtPassword;
	
	ConnectServer cs = null;
	ConnectUser user = null;
	
	public ConnectDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Label l = new Label(main, SWT.NONE);
		l.setText("Server:");
		
		lblServer = new Label(main, SWT.NONE);
		lblServer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnConfigure = new Button(main, SWT.PUSH);
		btnConfigure.setText("Configure...");
		btnConfigure.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				(new ShowServerConfigurationHandler()).execute(getParentShell());
				initData();
			}
		});
		l = new Label(main, SWT.NONE);
		l.setText("Username:");
		
		txtUser = new Text(main, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		l = new Label(main, SWT.NONE);
		l.setText("Password:");
		
		txtPassword = new Text(main, SWT.BORDER | SWT.PASSWORD);
		txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false,2,1));
		
		initData();
		
		return parent;
	}
	
	private void initData(){

		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			cs = (ConnectServer) s.createCriteria(ConnectServer.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.uniqueResult();
			
			user = (ConnectUser) s.get(ConnectUser.class, SmartDB.getCurrentEmployee().getUuid());
					
			s.getTransaction().commit();
		}finally{
			s.close();
		}
		
		lblServer.setText("");
		txtUser.setText("");
		txtPassword.setText("");
		
		if (cs != null){
			lblServer.setText(cs.getServerUrl());
		}
		if (user != null){
			txtUser.setText(user.getConnectUsername());
			txtPassword.setText(user.getConnectPassword());
		}
		
	}
	@Override
	public boolean isResizable(){
		return true;
	}
	
	public void okPressed(){
		final String server = cs.getServerUrl();
		final String user = txtUser.getText().trim();
		final String pass = txtPassword.getText().trim();
		if (server.isEmpty()){
			MessageDialog.openError(getShell(), "Error", "Connect server required. User configure button to setup the connect server.");
			return;
		}
		if (user.isEmpty()){
			MessageDialog.openError(getShell(), "Error", "Connect user name required.");
			return;
		}
		if (pass.isEmpty()){
			MessageDialog.openError(getShell(), "Error", "Connect password required.");
			return;
		}
		
		try(SmartConnect connect = new SmartConnect(cs, user, pass)){
			String error = connect.validateUser();
			if (error != null){
				MessageDialog.openError(getShell(), "Error", error);
				return;
			}
		}
		
		SmartConnect connect = new SmartConnect(cs, user, pass);
		onComplete(connect);
		
		super.okPressed();
	}
	
	protected abstract void onComplete(SmartConnect connect);
}
