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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

/**
 * String parameter component.
 * @author egouge
 * @since 1.0.0
 */
public class StringParameterComponent extends AbstractBirtParameter{
			
	private Text inputValue = null;
	
	public StringParameterComponent(IParameterDefn def) {
		super(def);
	}

	@Override
	public void createComposite(Composite parent, IDialogSettings settings, Listener onParameterModified) {
		Object initValue = super.getInitializeValue(settings);
		
		createNameLabel(parent);
		
		inputValue = new Text(parent, SWT.SINGLE | SWT.BORDER);
		inputValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (initValue != null){
			inputValue.setText(initValue.toString());
		}
		inputValue.addListener(SWT.Modify, onParameterModified);
	}

	@Override
	public Object getParameterValue() {
		return inputValue.getText();
	}

}

