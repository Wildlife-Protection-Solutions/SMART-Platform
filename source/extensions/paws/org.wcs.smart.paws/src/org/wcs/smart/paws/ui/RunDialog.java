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
package org.wcs.smart.paws.ui;

import java.time.LocalDate;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * PAWS Run dialog that collects an identifier and date range for
 * the current analysis.
 * 
 * @author Emily
 *
 */
public class RunDialog extends SmartStyledTitleDialog {

	private DateTime dtStart, dtEnd;
	private Text txtId;
	
	private LocalDate start;
	private LocalDate end;
	private String id;
	
	protected RunDialog(Shell parent) {
		super(parent);
	}

	public LocalDate getStartDate(){ return this.start; }
	public LocalDate getEndDate(){ return this.end; }
	public void setStart(LocalDate date){ this.start = date; }
	public void setEnd(LocalDate date){ this.end = date; }
	public void setId(String id){ this.id = id; }
	public String getId(){ return this.id; }
	
	@Override
	public void okPressed(){
		start = ( new java.sql.Date (SharedUtils.getDatePart(SmartUtils.getDate(dtStart), false).getTime())).toLocalDate();
		end = ( new java.sql.Date (SharedUtils.getDatePart(SmartUtils.getDate(dtEnd), false).getTime())).toLocalDate();
		id = txtId.getText();
		
		if (start.isAfter(end)){
			MessageDialog.openError(getShell(), "Error", "End Date cannot be before the Start Date");
			return;
		}
		super.okPressed();
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite main = (Composite) super.createDialogArea(parent);
		
		Composite all = new Composite(main, SWT.NONE);
		all.setLayout(new GridLayout(2, false));
		all.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(all, SWT.NONE);
		l.setText("Name:");
		
		txtId = new Text(all, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (id != null){
			txtId.setText(id);
		}else{
			txtId.setText("New Run Identifier");
		}
		
		l = new Label(all, SWT.NONE);
		l.setText("Start Date:");
		
		dtStart = new DateTime(all, SWT.DATE |SWT.DROP_DOWN);
		if (start != null){
			dtStart.setDate(start.getYear(), start.getMonthValue()-1, start.getDayOfMonth());
		}
		l = new Label(all, SWT.NONE);
		l.setText("End Date:");
		
		dtEnd = new DateTime(all, SWT.DATE |SWT.DROP_DOWN);
		if (end != null){
			dtEnd.setDate(end.getYear(), end.getMonthValue()-1, end.getDayOfMonth());
		}
		
		setTitle("PAWS Analysis");
		getShell().setText("PAWS");
		setMessage("The date range of data send to PAWS Analysis");
		return main;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
