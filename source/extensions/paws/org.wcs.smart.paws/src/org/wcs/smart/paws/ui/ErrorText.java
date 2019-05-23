/*
 * Copyright (C) 2019 Wildlife Conservation Society
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

/**
 * Text box that allows validation.  If validation fails, the box turns red
 * and an icon is displayed.
 * 
 * @author Emily
 *
 */
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
	
	/**
	 * 
	 * @return if the current string is valid or not
	 */
	public String isValid() {
		 return validator.apply(txtError.getText());
	}
	
	/**
	 * Returns the widget text. 
	 * The text for a text widget is the characters in the widget, or an empty string if this has never been set.
	 * @return the widget text
	 */
	public String getText() {
		return txtError.getText();
	}
	
	/**
	 * Sets the widget text 
	 * @param text the new text
	 */
	public void setText(String text) {
		txtError.setText(text);
	}
	
	/**
	 * Sets the widget tooltip text
	 */
	@Override
	public void setToolTipText(String text){
		txtError.setToolTipText(text);
	}

}
