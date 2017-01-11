package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;

public class ErrorPanel extends Composite {
	
	private Label errorImage;
	private Label errorLabel;
	
	public ErrorPanel(Composite parent) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(2, false));
		
		errorImage = new Label(this, SWT.NONE);
		errorImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		
		errorLabel = new Label(this, SWT.NONE);
		errorLabel.setText("");
		errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public void setError(String message){
		this.errorLabel.setText(message);
	}
	
}
