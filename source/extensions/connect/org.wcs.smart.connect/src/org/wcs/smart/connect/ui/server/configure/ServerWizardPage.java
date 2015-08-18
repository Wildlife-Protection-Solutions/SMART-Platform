package org.wcs.smart.connect.ui.server.configure;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ServerWizardPage extends WizardPage implements ModifyListener {

	private Text txtServer;
	
	public ServerWizardPage(){
		super("SERVER");
	}
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		Composite inner = new Composite(outer, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(inner, SWT.NONE);
		l.setText("Server URL:");
		
		txtServer = new Text(inner, SWT.BORDER);
		txtServer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtServer.setText("https://localhost:8443/server");
		txtServer.addModifyListener(this);
		
		setTitle("SMART Connect Server");
		setMessage("Enter the URL to the SMART Connect Server.  (example: https://www.smartconnect.org/server/)");
		
		setControl(outer);
		
	}
	
	public boolean isPageComplete(){
		if (!super.isPageComplete()){
			return false;
		}
		return !getServerName().isEmpty();
	}
	
	public String getServerName(){
		return txtServer.getText().trim();
		
	}
	
	@Override
	public void modifyText(ModifyEvent e) {
		getWizard().getContainer().updateButtons();
	}

}
