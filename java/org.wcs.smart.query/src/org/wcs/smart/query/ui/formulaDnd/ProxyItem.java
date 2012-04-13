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
package org.wcs.smart.query.ui.formulaDnd;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This proxy item is used as a placeholder when
 * dragging items around the query area.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ProxyItem extends Composite {

	private Label lbl = null;
	
	/**
	 * Creates new proxy item
	 * 
	 * @param parent
	 */
	public ProxyItem(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginBottom = 2;
		layout.marginLeft = 4;
		layout.marginRight = 4;
		layout.marginTop = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		
		super.setLayout(layout);
		this.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		GridData gd = new GridData();
		gd.horizontalIndent = 5;
		gd.verticalIndent = 0;
		
		super.setData(gd);
		
		Composite inner = new Composite(this, SWT.BORDER);
		inner.setLayout(new GridLayout(1, false));
		inner.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		gd = new GridData();
		gd.heightHint = 23;
		inner.setData(gd);
		
		
		lbl = new Label(inner, SWT.NONE);
		lbl.setText("");
		lbl.setVisible(false);
	}
	
	/**
	 * Sets the proxy item label text
	 * @param text
	 */
	public void setLabelText(String text){
		this.lbl.setText(text);
	}
}
