package org.wcs.smart.paws.ui;

import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.SmartPlugIn;

public class ErrorText extends Composite {

	private Text txtError;
	private Label img;
	private Color errorColor;
	
	private  Function<String, String> validator;
	
	public ErrorText(Composite parent, Function<String, String> validator) {
		super(parent, SWT.BORDER);
	
		this.validator = validator;
		
		errorColor = new Color(parent.getDisplay(), 255, 230, 230);
		addListener(SWT.Dispose, e->errorColor.dispose());
		
		setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		setLayout(new GridLayout(2, false));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		txtError = new Text(this, SWT.NONE);
		txtError.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtError.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		img = new Label(this, SWT.NONE);
		img.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		img.setVisible(false);
		img.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)img.getLayoutData()).widthHint = 0;
		
		txtError.addListener(SWT.Modify, e->{
			String msg = ErrorText.this.validator.apply(txtError.getText());
			if (msg == null) {
				img.setVisible(false);
				((GridData)img.getLayoutData()).widthHint = 0;
				setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				img.setToolTipText("");; //$NON-NLS-1$
			}else {
				img.setVisible(true);
				((GridData)img.getLayoutData()).widthHint = img.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;		
				setBackground(errorColor);
				img.setToolTipText(msg);
			}
			layout(true);
			for (Listener l : ErrorText.this.getListeners(SWT.Modify)) l.handleEvent(e);
		});
	}
	
	public String isValid() {
		 return validator.apply(txtError.getText());
	}
	public String getText() {
		return txtError.getText();
	}
	
	public void setText(String text) {
		txtError.setText(text);
	}
	
	@Override
	public void setToolTipText(String text){
		txtError.setToolTipText(text);
	}

}
