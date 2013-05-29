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
package org.wcs.smart.ui.internal.startup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;

/**
 * Simple dialog header with smart logo and a text area.
 * 
 * @author Emily Gouge
 *
 */
public class DialogHeader extends Composite {

	private Label txtHeader;
	
	/**
	 * Create the header composite.
	 * 
	 * @param parent parent composite
	 * @param style composite style
	 */
	public DialogHeader(Composite parent, int style) {
		super(parent, style);
		setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		GridLayout gl = new GridLayout(2, false);
		gl.horizontalSpacing = gl.verticalSpacing = gl.marginHeight = gl.marginWidth = 0;
		setLayout(gl);
		
		txtHeader = new Label(this, SWT.NONE);
		txtHeader.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		txtHeader.setAlignment(SWT.CENTER);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		txtHeader.setLayoutData(gd);
		
		Composite imageComp = new Composite(this, SWT.NONE);
		gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 10;
		imageComp.setLayout(gl);
		imageComp.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		Label label = new Label(imageComp, SWT.NONE);
		label.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		label.setAlignment(SWT.CENTER);

		label.setImage(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON).createImage());
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		imageComp.setLayoutData(gd);

		Label titleBarSeparator = new Label(this, SWT.HORIZONTAL | SWT.SEPARATOR);
		titleBarSeparator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
	}

	/**
	 * Sets the header description text.
	 * @param text
	 */
	public void setHeader(String text){
		txtHeader.setText(text);
	}
	
}
