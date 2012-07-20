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
package org.wcs.smart.report.internal.ui.viewer.parameter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.util.SmartUtils;

/**
 * Date PArameter UI element
 * @author egouge
 * @since 1.0.0
 */
public class DateParameter extends AbstractBirtParameter {

	private DateTime datePicker = null;
	
	/**
	 * 
	 * @param name parameter name
	 * @param displayText parameter display text
	 */
	public DateParameter(String name, String displayText){
		super(name, displayText);
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#createComponent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createComponent(Composite parent) {
		Composite param = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = gl.marginHeight = gl.horizontalSpacing = gl.verticalSpacing = 0;
		param.setLayout(gl);
		
		Label lbl = new Label(param, SWT.NONE);
		lbl.setText(getDisplayText() + ":");
		
		datePicker = new DateTime(param, SWT.DATE | SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER);
		
		return param;
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#getParameterValue()
	 */
	@Override
	public Object getParameterValue() {
		return new java.sql.Date(SmartUtils.getDate(datePicker).getTime());
	}

}
