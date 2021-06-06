package org.wcs.smart.i2.migrate.intelligence.wizard;


import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.SmartPlugIn;

public class Smart6WizardPage extends WizardPage {

	private Text txtFile;
	
	protected Smart6WizardPage() {
		super("SMART6Page");
	}

	public String getFile() {
		return txtFile.getText();
	}
	
	@Override
	public void createControl(Composite parent) {
	
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite temp = new Composite(outer, SWT.NONE);
		temp.setLayout(new GridLayout(3, false));
		temp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(temp, SWT.NONE);
		l.setText("SMART 6 Backup File:");
		
		txtFile = new Text(temp, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnBrowse = new Button(temp, SWT.PUSH);
		btnBrowse.setText("...");
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnBrowse.getLayoutData()).heightHint = txtFile.computeSize(SWT.DEFAULT,  SWT.DEFAULT).y;
		
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
			fd.setFileName(txtFile.getText());
			fd.setFilterExtensions(new String[] {"*.zip", "*.*"});
			fd.setFilterNames(new String[] {"Zip Files (*.zip)", "All Files (*.*)"});
			String fname = fd.open();
			if (fname == null) return;
			txtFile.setText(fname);
					
		});
		
		setControl(outer);
		
		setTitle("SMART 6");
		setMessage("Select the SMART6 backup to import intelligence from");
		
	}

	
}
