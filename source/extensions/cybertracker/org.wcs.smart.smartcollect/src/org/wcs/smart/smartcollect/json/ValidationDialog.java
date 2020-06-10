package org.wcs.smart.smartcollect.json;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.smartcollect.json.SmartCollectDataProcessor.ProcessingOption;
import org.wcs.smart.ui.SmartStyledDialog;

public class ValidationDialog extends SmartStyledDialog {

	private String users;
	private SmartCollectDataProcessor.ProcessingOption option;
	
	public ValidationDialog(Shell parent, String users) {
		super(parent);
		this.users = users;
		option = ProcessingOption.CANCEL;
	}
	
	public ProcessingOption getSelectedOption() {
		return this.option;
	}

	protected Control createDialogArea(Composite parent) {
		 Composite composite = (Composite) super.createDialogArea(parent);
		 composite.setLayout(new GridLayout());
		 ((GridLayout)composite.getLayout()).marginTop = 20;
		 ((GridLayout)composite.getLayout()).marginLeft= 20;
		 ((GridLayout)composite.getLayout()).marginRight = 20;
		 
		 Label l = new Label(composite, SWT.WRAP);
		 l.setText(MessageFormat.format("The user(s): {0} have not been validated for SMART Collect data collection.  You have the following options:", users));
		 l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		 ((GridData)l.getLayoutData()).widthHint = 400;
		 
		 Composite buttonComp = new Composite(composite, SWT.NONE);
		 buttonComp.setLayout(new GridLayout());
		 buttonComp.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		 
		 Button btnLoadAnyways = new Button(buttonComp, SWT.PUSH);
		 btnLoadAnyways.setText("Load Data");
		 btnLoadAnyways.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnLoadAnyways.setToolTipText("Load all data");
		 btnLoadAnyways.addListener(SWT.Selection, e->{
			 option = ProcessingOption.LOADDATA;
			 okPressed();
		 });
		 
		 Button btnValidateAndLoad = new Button(buttonComp, SWT.PUSH);
		 btnValidateAndLoad.setText("Accept User(s) and Load Data");
		 btnValidateAndLoad.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnValidateAndLoad.setToolTipText("Mark the users as a valid user for future data loads and loads all data");
		 btnValidateAndLoad.addListener(SWT.Selection, e->{
			 option = ProcessingOption.ACCEPTANDLOAD;
			 okPressed();
		 });
		
		 
		 Button btnBlacklistSkip = new Button(buttonComp, SWT.PUSH);
		 btnBlacklistSkip.setText("Blacklist User(s) and Discard Data");
		 btnBlacklistSkip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnBlacklistSkip.setToolTipText("Mark the users as blacklisted and throw away data");
		 btnBlacklistSkip.addListener(SWT.Selection, e->{
			 option = ProcessingOption.BLACKLISTANDDISCARD;
			 okPressed();
		 });
		 
		 Button btnSkip = new Button(buttonComp, SWT.PUSH);
		 btnSkip.setText("Discard Data Only");
		 btnSkip.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnSkip.setToolTipText("Throw away all the data");
		 btnSkip.addListener(SWT.Selection, e->{
			 option = ProcessingOption.DISCARD;
			 okPressed();
		 });
		 
		 Button btnRequeue = new Button(buttonComp, SWT.PUSH);
		 btnRequeue.setText("Send Email Verification and Requeue Data");
		 btnRequeue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnRequeue.setToolTipText("Send validation requests to the user and requeue data for future processing.");
		 btnRequeue.addListener(SWT.Selection, e->{
			 option = ProcessingOption.VERIFYREQUEUE;
			 okPressed();
		 });
		 
		 Button btnCancel = new Button(buttonComp, SWT.PUSH);
		 btnCancel.setText("Cancel");
		 btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		 btnCancel.setToolTipText("Cancel data processing");
		 btnCancel.addListener(SWT.Selection, e->{
			 option = ProcessingOption.CANCEL;
			 okPressed();
		 });
		 getShell().setText("Validate SMART Collect Users");
		 return composite;
	 }
	
	protected void createButtonsForButtonBar(Composite parent) {
	}
}
