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

import java.text.MessageFormat;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.Messages;

/**
 * Numeric parameter component.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class NumberParameterComponent extends AbstractBirtParameter{

	/**
	 * a validator that validates input is integer
	 */
	public final static INumberValidator INTEGER_VALIDATOR = new INumberValidator() {
		@Override
		public Object validate(String text) throws Exception {
			if (text.length() == 0) return true;
			return Integer.parseInt(text);
		}
	};

	/**
	 * a validator that validates input is float
	 */
	public final static INumberValidator FLOAT_VALIDATOR = new INumberValidator() {

		@Override
		public Object validate(String text) throws Exception {
			if (text.length() == 0) return true;
			Float f = Float.parseFloat(text);
			if (f == Float.POSITIVE_INFINITY) throw new Exception(Messages.NumberParameterComponent_ValueToBig);
			if (f == Float.NEGATIVE_INFINITY) throw new Exception(Messages.NumberParameterComponent_ValueToSmall);
			return f;
			
		}
	};

	/**
	 * a validator that validates input is double
	 */
	public final static INumberValidator DOUBLE_VALIDATOR = new INumberValidator() {

		@Override
		public Object validate(String text) throws Exception {
			if (text.length() == 0) return true;
			Double d = Double.parseDouble(text);
			if (d == Double.POSITIVE_INFINITY) throw new Exception(Messages.NumberParameterComponent_ValueToBig);
			if (d == Double.NEGATIVE_INFINITY) throw new Exception(Messages.NumberParameterComponent_ValueToSmall);
			return d;
	}};
				
	private Text inputValue = null;
	private INumberValidator validator;
	
	/**
	 * @param name parameter name
	 * @param displayText display text
	 * @param validator validator
	 */
	public NumberParameterComponent(IParameterDefn def, INumberValidator validator) {
		super(def);
		this.validator = validator;
	}

	@Override
	public void createComposite(Composite parent, IDialogSettings settings, Listener onParameterModified) {
		Object initValue = super.getInitializeValue(settings);

		createNameLabel(parent);

		inputValue = new Text(parent, SWT.SINGLE | SWT.BORDER);
		inputValue.addListener(SWT.Modify, onParameterModified);
		inputValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (initValue != null){
			inputValue.setText(initValue.toString());
		}		
	}

	@Override
	public String validate() {
		String input = inputValue.getText();
		try {
			validator.validate(input);
			return null;
		}catch (Exception ex) {
			return MessageFormat.format(Messages.NumberParameterComponent_InvalidValue, def.getName(), ex.getMessage());
		}
		
	}
	@Override
	public Object getParameterValue() {
		try{
			return validator.validate(inputValue.getText());
		}catch (Exception ex){
			ReportPlugIn.log(Messages.NumberParameterComponent_ConversionError + ex.getLocalizedMessage(), ex);
			return null;
		}
	}

}

interface INumberValidator{
	
	/**
	 * Validates the string returning the converted object 
	 * @param text
	 * @return
	 * @throws Exception if string validation fails
	 */
	public Object validate(String text) throws Exception;
	
}
