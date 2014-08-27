package org.wcs.smart.er.ui.samplingunit.export.wizard;

import java.io.File;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.er.internal.Messages;

public class LocationPage extends WizardPage {
	
	private static final String DIR_KEY = "exportsu_directory"; //$NON-NLS-1$
	private Text txtDirectory;
	
	public LocationPage(){
		super("LOCATION_PAGE"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout(3, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(c, SWT.NONE);
		l.setText(Messages.LocationPage_DirectoryLabel);

		txtDirectory = new Text(c, SWT.BORDER);
		txtDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtDirectory.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				validate();
			}
		});
		
		Button btnSelect = new Button(c, SWT.NONE);
		btnSelect.setText("..."); //$NON-NLS-1$
		btnSelect.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dd = new DirectoryDialog(getShell());
				dd.setMessage(Messages.LocationPage_DialogMessage);
				dd.setFilterPath(txtDirectory.getText());
				String x = dd.open();
				if (x != null){
					txtDirectory.setText(x);
					validate();
				}
			}
		});
		
		String def = getWizard().getDialogSettings().get(DIR_KEY);
		if (def != null){
			txtDirectory.setText(def);
		}
		
		setControl(main);
		
		setTitle(Messages.LocationPage_Title);
		setMessage(Messages.LocationPage_Message);
	}
	
	@Override
	public boolean isPageComplete(){
		return getErrorMessage() == null;
	}
	
	private void validate(){
		File f = new File(txtDirectory.getText());
		if(f.isFile()){
			setErrorMessage(Messages.LocationPage_ErrorMessage);
		}else{
			setErrorMessage(null);
		}
		
		//update buttons
		try{
			getContainer().updateButtons();
		}catch(Throwable t){
			//eat this; most likely caused because buttons not set yet
		}
	}
	
	public File getDirectory(){
		File f = new File(txtDirectory.getText());
		getWizard().getDialogSettings().put(DIR_KEY, txtDirectory.getText());
		return f;
	}
}