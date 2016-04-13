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

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.util.SmartUtils;

/**
 * Date Parameter UI element
 * @author egouge
 * @since 1.0.0
 */
public class DateParameterComponent extends AbstractBirtParameter {

	private DateTime datePicker = null;
	private DateTime timePicker = null;
	
	private int dFormat = -1;
	private int tFormat = -1;
	
	private Date defaultValue;
	
	/**
	 * 
	 * @param name parameter name
	 * @param displayText parameter display text
	 */
	public DateParameterComponent(String name, String displayText, boolean includeDate, boolean includeTime, Object defaultValue){
		super(name, displayText);
		
		if (defaultValue != null && defaultValue instanceof Date){
			this.defaultValue = (Date) defaultValue;
		}
		if (includeDate){
			dFormat = SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.DATE;
		}
		if (includeTime){
			tFormat = SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.TIME;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#createComponent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createComposite(Composite parent, IDialogSettings settings) {
		SimpleDateFormat sdf = new SimpleDateFormat(ReportParameterDialog.SIMPLE_DATE_FORMAT);
		String x = settings.get(getParameterName());
		if (x != null){
			
			try{
				this.defaultValue = sdf.parse(x);
			}catch (Exception ex){
				//eat me
			}
		}
		Composite param = new Composite(parent, SWT.NONE);
		int numcolumns = 2;
		if (dFormat != -1 && tFormat != -1){
			numcolumns = 3;
		}
		GridLayout gl = new GridLayout(numcolumns, false);
		gl.marginWidth = gl.marginHeight = gl.horizontalSpacing = gl.verticalSpacing = 0;
		param.setLayout(gl);

		Label lbl = new Label(param, SWT.NONE);
		lbl.setText(getDisplayText() + ": "); //$NON-NLS-1$
		if (dFormat != -1){
			datePicker = new DateTime(param, dFormat);
			datePicker.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if (this.defaultValue != null){
				SmartUtils.initDateDateTimeWidget(datePicker, defaultValue);
			}
			
		}
		if (tFormat != -1){
			timePicker = new DateTime(param, tFormat);
			timePicker.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if (this.defaultValue != null){
				SmartUtils.initTimeDateTimeWidget(timePicker, defaultValue);
			}
		}
		return param;
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#getParameterValue()
	 */
	@Override
	public Object getParameterValue() {
		Date d = null;
		Date t = null;
		if (datePicker != null){
			d = SmartUtils.getDate(datePicker);
		}
		if (timePicker != null){
			t = SmartUtils.getTime(timePicker);
		}
		if (d != null && t == null){
			return new java.sql.Date(d.getTime());	
		}else if (d == null && t!= null){
			return new java.sql.Time(t.getTime());
		}else{
			return new java.sql.Date(SmartUtils.combineDateTime(d, new Time( t.getTime()) ).getTime()); 
		}
		
	}

}
