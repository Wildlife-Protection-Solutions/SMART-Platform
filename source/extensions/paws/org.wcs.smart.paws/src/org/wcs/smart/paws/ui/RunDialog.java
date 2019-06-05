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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * PAWS Run dialog that collects an identifier and date range for
 * the current analysis.
 * 
 * @author Emily
 *
 */
public class RunDialog extends SmartStyledTitleDialog {

	private CCombo dtTrainStart, dtTrainEnd, dtTestStart, dtTestEnd, dtForcastStart, dtForcastEnd;
	private Text txtId;
	
	private Integer trainStart;
	private Integer trainEnd;
	private Integer testStart;
	private Integer testEnd;
	private Integer forcastStart;
	private Integer forcastEnd;
	
	private LocalDate dataStart, dataEnd;
	
	private DateTime dtStart, dtEnd;
	
	private String id;
	
	private boolean startModified = false;
	private boolean endModified = false;
	
	protected RunDialog(Shell parent) {
		super(parent);
	}

	public int getTrainStart(){ return this.trainStart; }
	public int getTrainEnd(){ return this.trainEnd; }
	public int getTestStart(){ return this.testStart; }
	public int getTestEnd(){ return this.testEnd; }
	public int getForcastStart(){ return this.forcastStart; }
	public int getForcastEnd(){ return this.forcastEnd; }
	public LocalDate getDataStart() {return this.dataStart; }
	public LocalDate getDataEnd() {return this.dataEnd; }
	
	
	public void setDates(int trainStart, int trainEnd, int testStart, int testEnd, int forcastStart, int forcastEnd, LocalDate dataStart, LocalDate dataEnd) {
		this.trainStart = trainStart;
		this.trainEnd = trainEnd;
		this.testStart = testStart;
		this.testEnd = testEnd;
		this.forcastStart = forcastStart;
		this.forcastEnd = forcastEnd;
		this.dataStart = dataStart;
		this.dataEnd = dataEnd;
	}
	
	public void setId(String id){ this.id = id; }
	public String getId(){ return this.id; }
	
	@Override
	public void okPressed(){
		trainStart = Integer.valueOf(dtTrainStart.getText());
		trainEnd = Integer.valueOf(dtTrainEnd.getText());
		
		testStart = Integer.valueOf(dtTestStart.getText());
		testEnd = Integer.valueOf(dtTestEnd.getText());
		
		forcastStart = Integer.valueOf(dtForcastStart.getText());
		forcastEnd = Integer.valueOf(dtForcastEnd.getText());
		
		this.dataStart = LocalDate.of(dtStart.getYear(), dtStart.getMonth()+1, dtStart.getDay());
		this.dataEnd = LocalDate.of(dtEnd.getYear(), dtEnd.getMonth()+1, dtEnd.getDay());
		
		id = txtId.getText();
		
		if (trainStart > trainEnd){
			MessageDialog.openError(getShell(), "Error", "Training End Date cannot be before the Start Date");
			return;
		}
		
		if (testStart > testEnd){			
			MessageDialog.openError(getShell(), "Error", "Testing End Date cannot be before the Start Date");
			return;
		}
		
		if (forcastStart > forcastEnd){
			MessageDialog.openError(getShell(), "Error", "Forcasting End Date cannot be before the Start Date");
			return;
		}
		
		super.okPressed();
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite main = (Composite) super.createDialogArea(parent);

		Composite outer = new Composite(main, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Composite header = new Composite(outer, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(header, SWT.NONE);
		l.setText("Name:");
		
		txtId = new Text(header, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (id != null){
			txtId.setText(id);
		}else{
			txtId.setText("New Run Identifier");
		}
		
		Label spacer = new Label(outer, SWT.SEPARATOR | SWT.HORIZONTAL);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		int year = LocalDate.now().getYear();
		
		Composite g = new Composite(outer, SWT.NONE);
		g.setLayout(new GridLayout(4, false));
		g.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

		l = new Label(g, SWT.NONE);
		l.setText("Training Years:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		dtTrainStart = createDateDropDown(g);
		dtTrainStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (trainStart != null) {
			dtTrainStart.setText(String.valueOf(trainStart));
		}else {
			dtTrainStart.setText(String.valueOf(year - 5));
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("to");
		
		dtTrainEnd = createDateDropDown(g);
		dtTrainEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (trainEnd != null) {
			dtTrainEnd.setText(String.valueOf(trainEnd));
		}else {
			dtTrainEnd.setText(String.valueOf(year - 1));
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("Test Years:");

		dtTestStart = createDateDropDown(g);
		dtTestStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (testStart != null) {
			dtTestStart.setText(String.valueOf(testStart));
		}else {
			dtTestStart.setText(String.valueOf(year - 1));
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("to");
		dtTestEnd = createDateDropDown(g);
		dtTestEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (testEnd != null) {
			dtTestEnd.setText(String.valueOf(testEnd));
		}else {
			dtTestEnd.setText(String.valueOf(year));
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("Forecasting Years:");
		
		dtForcastStart = createDateDropDown(g);
		dtForcastStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (forcastStart != null) {
			dtForcastStart.setText(String.valueOf(forcastStart));
		}else {
			dtForcastStart.setText(String.valueOf(year + 1));
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("to");
		dtForcastEnd = createDateDropDown(g);
		dtForcastEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (forcastEnd != null) {
			dtForcastEnd.setText(String.valueOf(forcastEnd));
		}else {
			dtForcastEnd.setText(String.valueOf(year + 2));
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("Data Dates:");

		
		dtStart = new DateTime(g, SWT.DROP_DOWN | SWT.MEDIUM);
		dtStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		dtStart.addListener(SWT.Selection, e->startModified=true);
		if (dataStart != null) {
			dtStart.setDate(dataStart.getYear(), dataStart.getMonthValue()-1, dataStart.getDayOfMonth());
			startModified = true;
		}else {
			dtStart.setDate(year-5, 0, 1);
		}
		
		l = new Label(g, SWT.NONE);
		l.setText("to");
		dtEnd = new DateTime(g, SWT.DROP_DOWN |  SWT.MEDIUM);
		dtEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		dtEnd.addListener(SWT.Selection, e->endModified=true);
		if (dataEnd != null) {
			dtEnd.setDate(dataEnd.getYear(), dataEnd.getMonthValue()-1, dataEnd.getDayOfMonth());
			endModified = true;
		}else {
			dtEnd.setDate(year, 0, 1);
		}

		dtTrainStart.addListener(SWT.Modify, e->{
			if (startModified) return;
			dtStart.setDate(Integer.valueOf(dtTrainStart.getText()), 0, 1);
		});
		dtTestEnd.addListener(SWT.Modify, e->{
			if (endModified) return;
			dtEnd.setDate(Integer.valueOf(dtTestEnd.getText()), 0, 1);
		});
		setTitle("PAWS Analysis");
		getShell().setText("PAWS");
		setMessage("The date range of data send to PAWS Analysis");
		return main;
	}

	@Override
	public boolean isResizable(){
		return true;
	}
	
	private CCombo createDateDropDown(Composite parent) {
		CCombo dd = new CCombo(parent, SWT.DROP_DOWN | SWT.BORDER);
		
		int year = LocalDate.now().getYear();
		String[] items = new String[30];
		for (int i = 0; i < items.length; i ++) {
			items[i] = String.valueOf(year - 20 + i);
		}
		
		dd.setItems(items);
		
		return dd;
	}
}
