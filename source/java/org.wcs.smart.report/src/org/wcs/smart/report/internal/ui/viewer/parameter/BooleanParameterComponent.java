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

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.report.internal.Messages;

/**
 * A Boolean parameter component
 * @author egouge
 * @since 1.0.0
 */
public class BooleanParameterComponent extends AbstractBirtParameter{

	/**
	 * @param name
	 * @param displayText
	 */
	public BooleanParameterComponent(IParameterDefn def){
		super(def);
	}

	private Button btnFalse;
	private Button btnTrue;
	
	@Override
	public void createComposite(Composite parent, IDialogSettings settings) {
		Object initValue = super.getInitializeValue(settings);
		
		createNameLabel(parent);
		
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(2, false));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		btnTrue = new Button(temp, SWT.RADIO);
		btnTrue.setText(Messages.BooleanParameterComponent_True_Label);
		
		btnFalse = new Button(temp, SWT.RADIO);
		btnFalse.setText(Messages.BooleanParameterComponent_False_Label);
		
		if (initValue != null && initValue instanceof Boolean){
			btnTrue.setSelection(  (Boolean)initValue );
			btnFalse.setSelection(  !(Boolean)initValue );
		}else{
			btnTrue.setSelection(true);
		}

	}

	@Override
	public Object getParameterValue() {
		return btnTrue.getSelection();
	}

}