/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views.query.dropitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;

/**
 * Error drop item 
 * 
 * @author Emily
 *
 */
public class ErrorDropItem extends DropItem {

	private Color redColor = null;
	private String errorMessage = null;
	
	public ErrorDropItem(String errorMessage){
		this.errorMessage = errorMessage;
	}
	
	@Override
	public String getText() {
		return errorMessage;
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (redColor != null){
			redColor.dispose();
		}
		
	}

	@Override
	public String asQueryPart() {
		return null;
	}

	@Override
	protected void createComposite(Composite parent) {
		redColor =  new Color(Display.getDefault(),new RGB(255, 210,210) );
		parent.setBackground(redColor);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setBackground(redColor);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		c.setLayout(gl);
		
		Label lblImage = new Label(c, SWT.NONE);
		lblImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		lblImage.setBackground(redColor);
		Label lbl = new Label(c, SWT.NONE);
		lbl.setText(errorMessage);
		lbl.setBackground(redColor);
	}

}
