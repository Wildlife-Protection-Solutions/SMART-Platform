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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * A Boolean parameter component
 * @author egouge
 * @since 1.0.0
 */
public class BooleanParameterComponent extends AbstractBirtParameter{

				
	private Object defaultValue;
	/**
	 * @param name
	 * @param displayText
	 */
	public BooleanParameterComponent(String name, String displayText, Object defaultValue) {
		super(name, displayText);
		this.defaultValue = defaultValue;
	}

	private Button btnFalse;
	private Button btnTrue;
	
	@Override
	public Composite createComposite(Composite parent) {
		
		Composite param = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = gl.marginHeight = gl.horizontalSpacing = gl.verticalSpacing = 0;
		param.setLayout(gl);
		
		Label lbl = new Label(param, SWT.NONE);
		lbl.setText(getDisplayText() + ": ");
		
		btnTrue = new Button(param, SWT.RADIO);
		btnTrue.setText("True");
		
		btnFalse = new Button(param, SWT.RADIO);
		btnFalse.setText("False");
		
		if (defaultValue instanceof Boolean){
			btnTrue.setSelection(  (Boolean)defaultValue );
			btnFalse.setSelection(  !(Boolean)defaultValue );
		}else{
			btnTrue.setSelection(true);
		}

		return param;
	}

	@Override
	public Object getParameterValue() {
		return btnTrue.getSelection();
	}

}