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
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.wb.swt.SWTResourceManager;
import org.wcs.smart.SmartPlugIn;

/**
 * Simple dialog header with smart logo and a text area.
 * 
 * @author Emily Gouge
 *
 */
public class DialogHeader extends Composite {

	private Label txtHeader;
	
	public DialogHeader(Composite parent, int style) {
		this(parent, style, true);
	}
	/**
	 * Create the header composite.
	 * 
	 * @param parent parent composite
	 * @param style composite style
	 */
	public DialogHeader(Composite parent, int style, boolean largeFont) {
		super(parent, style);
		setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		setLayout(new FormLayout());
		
		Label label = new Label(this, SWT.NONE);
		label.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		label.setAlignment(SWT.CENTER);
		FormData fd_label = new FormData();
		fd_label.top = new FormAttachment(0);
		fd_label.bottom = new FormAttachment(100);
		fd_label.right = new FormAttachment(100);
		fd_label.left = new FormAttachment(0, 366);
		label.setLayoutData(fd_label);
		label.setImage(ResourceManager.getPluginImage(SmartPlugIn.PLUGIN_ID, "images/icons/smart64.gif"));
		
		txtHeader = new Label(this, SWT.NONE);
		txtHeader.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		if (largeFont){

			txtHeader.setFont(SWTResourceManager.getFont("DejaVu Sans", 15, SWT.NORMAL));
			txtHeader.setAlignment(SWT.CENTER);
		}
		
		FormData fd_txtHeader = new FormData();
		fd_txtHeader.right = new FormAttachment(label, -20);
		fd_txtHeader.bottom = new FormAttachment(100, -25);
		fd_txtHeader.left = new FormAttachment(0, 20);
		fd_txtHeader.top = new FormAttachment(0, 25);
		txtHeader.setLayoutData(fd_txtHeader);
		txtHeader.setText("txtHeader");
	}

	/**
	 * Sets the header description text.
	 * @param text
	 */
	public void setHeader(String text){
		txtHeader.setText(text);
	}
	
}
