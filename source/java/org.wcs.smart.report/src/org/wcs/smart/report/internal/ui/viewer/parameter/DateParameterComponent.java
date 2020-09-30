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


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
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
	
	/**
	 * 
	 * @param name parameter name
	 * @param displayText parameter display text
	 */
	public DateParameterComponent(IParameterDefn def){
		super(def);
		
		if (def.getDataType() == IParameterDefn.TYPE_DATE  || def.getDataType() == IParameterDefn.TYPE_DATE_TIME) {
			dFormat = SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.DATE;
		}
		
		if (def.getDataType() == IParameterDefn.TYPE_TIME  || def.getDataType() == IParameterDefn.TYPE_DATE_TIME) {
			tFormat = SWT.DROP_DOWN | SWT.MEDIUM | SWT.BORDER | SWT.TIME;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#createComponent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent, IDialogSettings settings) {
		DateTimeFormatter sdf = DateTimeFormatter.ofPattern(ReportParameterDialog.SIMPLE_DATE_FORMAT);
		Object initValue = super.getInitializeValue(settings);
		
		LocalDateTime initDate = null;
		if (initValue != null) {
			try{
				initDate = LocalDateTime.parse(initValue.toString(),sdf);
			}catch (Exception ex){
				//eat me
			}
		}
		
		
		createNameLabel(parent);
		
		
		Composite param = new Composite(parent, SWT.NONE);
		int numcolumns = 1;
		if (dFormat != -1 && tFormat != -1){
			numcolumns = 2;
		}
		GridLayout gl = new GridLayout(numcolumns, false);
		gl.marginWidth = gl.marginHeight = gl.horizontalSpacing = gl.verticalSpacing = 0;
		param.setLayout(gl);

		if (dFormat != -1){
			datePicker = new DateTime(param, dFormat);
			datePicker.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if (initDate != null){
				SmartUtils.initDateTimeWidget(datePicker, initDate.toLocalDate());
			}
			
		}
		if (tFormat != -1){
			timePicker = new DateTime(param, tFormat);
			timePicker.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			if (initDate != null){
				SmartUtils.initDateTimeWidget(timePicker, initDate.toLocalTime());
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.report.internal.ui.viewer.parameter.IBirtParameter#getParameterValue()
	 */
	@Override
	public Object getParameterValue() {
		LocalDate d = null;
		LocalTime t = null;
		if (datePicker != null){
			d = SmartUtils.toDate(datePicker);
		}
		if (timePicker != null){
			t = SmartUtils.toTime(timePicker);
		}
		if (d != null && t == null){
			return d;	
		}else if (d == null && t!= null){
			return t;
		}else{
			return LocalDateTime.of(d, t); 
		}
		
	}

}
