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
package org.wcs.smart.er.ui.component;

import java.util.Date;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Builds a composite that contains a start
 * and end date field that can optionally
 * be not selected.
 * 
 * @author Emily
 *
 */
public class DatesComponent implements SelectionListener{

	private DateTime dtStart; 
	private Button btnCheckStart;
	private DateTime dtEnd; 
	private Button btnCheckEnd;
	
	private ControlDecoration cdEnd;
	private boolean optional;
	
	public DatesComponent(boolean optional){
		this.optional = optional;
	}
	
	
	public Control createComposite(Composite parent){
		Composite part = new Composite(parent, SWT.NONE);
		
		if (optional){
			part.setLayout(new GridLayout(3, false));
		}else{
			part.setLayout(new GridLayout(2, false));
		}
		
		Label l = new Label(part, SWT.NONE);
		l.setText(Messages.DatesComponent_StartDateLabel);
		if (optional){
			btnCheckStart = new Button(part, SWT.CHECK);
		}
		dtStart = new DateTime(part, SWT.DATE | SWT.DROP_DOWN | SWT.LONG);
		
		
		l = new Label(part, SWT.NONE);
		l.setText(Messages.DatesComponent_EndDateLabel);
		if (optional){
			btnCheckEnd = new Button(part, SWT.CHECK);
		}
		dtEnd = new DateTime(part, SWT.DATE | SWT.DROP_DOWN | SWT.LONG);
		
		cdEnd = createDecoration(dtEnd);
		cdEnd.hide();
		
		if (optional){
			btnCheckStart.setSelection(false);
			btnCheckEnd.setSelection(false);
			
			btnCheckStart.addSelectionListener(this);
			btnCheckEnd.addSelectionListener(this);
		}
		
		dtStart.addSelectionListener(this);
		dtEnd.addSelectionListener(this);
		dtStart.setEnabled(false);
		dtEnd.setEnabled(false);
	
		validate();
		return part;
	}
	
	public void addModifiedListener(SelectionListener listener){
		if (optional){
			btnCheckStart.addSelectionListener(listener);
			btnCheckEnd.addSelectionListener(listener);	
		}
		dtStart.addSelectionListener(listener);
		dtEnd.addSelectionListener(listener);
		
	}
	
	/**
	 * Initializes the start date field.
	 * 
	 * @param startDate if null; start date is not enabled 
	 */
	public void setStartDate(Date startDate){
		setDate(startDate, btnCheckStart, dtStart);
	}
	
	/**
	 * Initializes the end date field
	 * @param endDate if null; end date is not enabled
	 */
	public void setEndDate(Date endDate){
		setDate(endDate, btnCheckEnd, dtEnd);
	}
	
	private void setDate(Date date, Button ch, DateTime widget){
		if (date == null){
			if (optional){
				widget.setEnabled(false);
				ch.setSelection(false);
			}else{
				widget.setEnabled(true);
			}
		}else{
			widget.setEnabled(true);
			if (ch != null){
				ch.setSelection(true);
			}
			
			SmartUtils.initDateDateTimeWidget(widget, date);
		}
		validate();
	}
	
	private void validate(){
		String error = null;
		if (optional){
			if (btnCheckEnd.getSelection() && btnCheckStart.getSelection()){
				if (getEndDate().before(getStartDate())){
					error = Messages.DatesComponent_InvalidDate;
				}
			}
		}else{
			if (getEndDate().before(getStartDate())){
				error = Messages.DatesComponent_InvalidDate;
			}
		}
		
		if (error == null){
			cdEnd.hide();
		}else{
			cdEnd.setDescriptionText(error);
			cdEnd.show();
		}
	}
	
	public void setError(String error){
		if (!cdEnd.isVisible()){
			cdEnd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
			cdEnd.setDescriptionText(error);
			cdEnd.show();
		}
	}
	
	public void setWarning(String warning){
		if (!cdEnd.isVisible()){
			cdEnd.setImage(FieldDecorationRegistry.getDefault()
					.getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
			cdEnd.setDescriptionText(warning);
			cdEnd.show();
		}
	}
	/**
	 * 
	 * @return the start date selected or null
	 * if not selected
	 */
	public Date getStartDate(){
		if (optional){
			if (btnCheckStart.getSelection()){
				return SmartUtils.getDate(dtStart);
			}
			return null;
		}else{
			return SmartUtils.getDate(dtStart);
		}
	}
	
	/**
	 * 
	 * @return the end date selected or null
	 * if not selected
	 */
	public Date getEndDate(){
		if (optional){
			if (btnCheckEnd.getSelection()){
				return SmartUtils.getDate(dtEnd);
			}
			return null;
		}else{
			return SmartUtils.getDate(dtEnd);
		}
	}

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.widget == btnCheckStart){
			dtStart.setEnabled(btnCheckStart.getSelection());
		}else if (e.widget == btnCheckEnd){
			dtEnd.setEnabled(btnCheckEnd.getSelection());
		}
		validate();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
	
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setShowHover(true);
		return cd;
	}
	
	/**
	 * 
	 * @return the error string or null if no error
	 */
	public String getError(){
		if (cdEnd.isVisible()){
			return cdEnd.getDescriptionText();
		}
		return null;
	}
}
