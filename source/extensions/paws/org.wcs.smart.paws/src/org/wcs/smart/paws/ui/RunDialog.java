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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
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

	private Combo dtTrainStart, dtTrainEnd, dtForcastStart, dtForcastEnd;
	private Text txtId;
	
	private Integer trainStart;
	private Integer trainEnd;
	
	private Integer forcastStart;
	private Integer forcastEnd;
	
	private String id;
	
	private Color errorColor;
	
	protected RunDialog(Shell parent) {
		super(parent);

	}

	
	public int getTrainStart(){ return this.trainStart; }
	public int getTrainEnd(){ return this.trainEnd; }
	public int getForcastStart(){ return this.forcastStart; }
	public int getForcastEnd(){ return this.forcastEnd; }
	
	
	public void setDates(int trainStart, int trainEnd, int forcastStart, int forcastEnd) {
		this.trainStart = trainStart;
		this.trainEnd = trainEnd;
		this.forcastStart = forcastStart;
		this.forcastEnd = forcastEnd;
	}
	
	public void setId(String id){ this.id = id; }
	public String getId(){ return this.id; }
	
	private boolean validateInt(Combo txt){
		try {
			int item = Integer.valueOf(txt.getText());
			if (item < 1950 || item > 2300) throw new Exception("value must be between 1950 and 2300");
			txt.setBackground(txt.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			return true;
		}catch (Exception ex) {
			txt.setBackground(errorColor);
			return false;
		}
	}
	@Override
	public void okPressed(){
		boolean error = false;
		if (!validateInt(dtTrainStart)) error = true;
		if (!validateInt(dtTrainEnd)) error = true;
		if (!validateInt(dtForcastStart)) error = true;
		if (!validateInt(dtForcastEnd)) error = true;
		
		if (error) {
			MessageDialog.openInformation(getShell(), "Error", "You must resvole date error before continuing.  All years must be valid integers between 1950 and 2300");
			return;
		}
	
		trainStart = Integer.valueOf(dtTrainStart.getText());
		trainEnd = Integer.valueOf(dtTrainEnd.getText());
		
		forcastStart = Integer.valueOf(dtForcastStart.getText());
		forcastEnd = Integer.valueOf(dtForcastEnd.getText());
		
		id = txtId.getText();
		
		if (trainStart > trainEnd){			
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

		errorColor = new Color(main.getDisplay(), 255, 230, 230);
		main.addListener(SWT.Dispose, e->errorColor.dispose());

		Composite header = new Composite(main, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(header, SWT.NONE);
		l.setText("Run Identifier:");
		
		txtId = new Text(header, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (id != null){
			txtId.setText(id);
		}else{
			txtId.setText("New Run Identifier");
		}
		
		int year = LocalDate.now().getYear();
		
		l = new Label(header, SWT.NONE);
		l.setText("Training Years:");
		
		Listener validate = e->validateInt((Combo)e.widget);
		
		Composite dates = new Composite(header, SWT.NONE);
		dates.setLayout(new GridLayout(3, false));
		dates.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)dates.getLayout()).marginWidth = 0;
		((GridLayout)dates.getLayout()).marginHeight = 0;
		
		dtTrainStart = createDateDropDown(dates);
		dtTrainStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (trainStart != null) {
			dtTrainStart.setText(String.valueOf(trainStart));
		}else {
			dtTrainStart.setText(String.valueOf(year - 5));
		}
		dtTrainStart.addListener(SWT.Modify, validate);
		
		
		l = new Label(dates, SWT.NONE);
		l.setText("to");
		
		dtTrainEnd = createDateDropDown(dates);
		dtTrainEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (trainEnd != null) {
			dtTrainEnd.setText(String.valueOf(trainEnd));
		}else {
			dtTrainEnd.setText(String.valueOf(year - 1));
		}
		dtTrainEnd.addListener(SWT.Modify, validate);
		
		
		l = new Label(header, SWT.NONE);
		l.setText("Forecasting Years:");
		
		dates = new Composite(header, SWT.NONE);
		dates.setLayout(new GridLayout(3, false));
		dates.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)dates.getLayout()).marginWidth = 0;
		((GridLayout)dates.getLayout()).marginHeight = 0;
		
		dtForcastStart = createDateDropDown(dates);
		dtForcastStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (forcastStart != null) {
			dtForcastStart.setText(String.valueOf(forcastStart));
		}else {
			dtForcastStart.setText(String.valueOf(year + 1));
		}
		dtForcastStart.addListener(SWT.Modify, validate);
		
		
		l = new Label(dates, SWT.NONE);
		l.setText("to");
		dtForcastEnd = createDateDropDown(dates);
		dtForcastEnd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (forcastEnd != null) {
			dtForcastEnd.setText(String.valueOf(forcastEnd));
		}else {
			dtForcastEnd.setText(String.valueOf(year + 2));
		}
		dtForcastEnd.addListener(SWT.Modify, validate);
	
//		dtTrainStart.addListener(SWT.Modify, e->{
//			if (startModified) return;
//			dtStart.setDate(Integer.valueOf(dtTrainStart.getText()), 0, 1);
//		});
	
		setTitle("PAWS Analysis");
		getShell().setText("PAWS");
		setMessage("The date range of data send to PAWS Analysis");
		return main;
	}

	@Override
	public boolean isResizable(){
		return true;
	}
	
	private Combo createDateDropDown(Composite parent) {
		Combo dd = new Combo(parent, SWT.DROP_DOWN | SWT.BORDER);
		
		int year = LocalDate.now().getYear();
		String[] items = new String[30];
		for (int i = 0; i < items.length; i ++) {
			items[i] = String.valueOf(year - 20 + i);
		}
		
		dd.setItems(items);
		
		return dd;
	}
}
