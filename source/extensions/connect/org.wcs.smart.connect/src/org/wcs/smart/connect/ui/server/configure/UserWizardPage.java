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

public class UserWizardPage extends WizardPage implements ModifyListener{

	public static final String NAME = "USER";
	
	private Text txtUser;
	private Text txtPass;
	
	public UserWizardPage(){
		super(NAME);
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
		l.setText("Connect Username:");
	
		txtUser = new Text(inner, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(inner, SWT.NONE);
		l.setText("Connect Password:");
		
		txtPass = new Text(inner, SWT.BORDER | SWT.PASSWORD);
		txtPass.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtPass.addModifyListener(this);
		txtUser.addModifyListener(this);
		
		setTitle("SMART Connect User Account");
		setMessage("Enter your SMART Connect credentials.  These should be different from your SMART Desktop username/password.");
		setControl(outer);
	}

	public boolean isPageComplete(){
		if (!super.isPageComplete()){
			return false;
		}
		return !getUsername().isEmpty() && !getPassword().isEmpty();
	}
	
	public String getUsername(){
		return this.txtUser.getText().trim();
	}
	
	public String getPassword(){
		return this.txtPass.getText().trim();
	}
	
	@Override
	public void modifyText(ModifyEvent e) {
		getWizard().getContainer().updateButtons();
	}
}
