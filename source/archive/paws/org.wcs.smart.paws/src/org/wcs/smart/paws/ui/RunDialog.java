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
import java.util.Collection;
import java.util.Collections;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * PAWS Run dialog that collects an identifier and date range for
 * the current analysis.
 * 
 * @author Emily
 *
 */
public class RunDialog extends SmartStyledTitleDialog {

	private CCombo dtTrainStart, dtTrainEnd, dtForcastStart;//, dtForcastEnd;
	private Text txtId;
	
	private Integer trainStart;
	private Integer trainEnd;
	private Integer forcastStart;
	
	private String id;
	private Color errorColor;
	
	private int smartDataStartYear = 2000;
	
	protected RunDialog(Shell parent) {
		super(parent);
	}

	public int getTrainStart(){ return this.trainStart; }
	public int getTrainEnd(){ return this.trainEnd; }
	public int getForcastStart(){ return this.forcastStart; }
	public int getForcastEnd(){ return this.forcastStart; }
	public void setId(String id){ this.id = id; }
	public String getId(){ return this.id; }
	
	public void setDates(int trainStart, int trainEnd, int forcastStart, int forcastEnd) {
		this.trainStart = trainStart;
		this.trainEnd = trainEnd;
		this.forcastStart = forcastStart;
	}
	
	
	private boolean validateInt(CCombo txt){
		try {
			int item = Integer.valueOf(txt.getText());
			if (item < 1950 || item > 2300) throw new Exception(Messages.RunDialog_validYears);
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
//		if (!validateInt(dtForcastEnd)) error = true;
		
		if (error) {
			MessageDialog.openInformation(getShell(), Messages.RunDialog_ErrorTitle, Messages.RunDialog_DateErrors);
			return;
		}
		
		trainStart = Integer.valueOf(dtTrainStart.getText());
		trainEnd = Integer.valueOf(dtTrainEnd.getText());

		forcastStart = Integer.valueOf(dtForcastStart.getText());
	
		id = txtId.getText();
		
		if (trainStart > trainEnd){
			MessageDialog.openError(getShell(), Messages.RunDialog_ErrorTitle, Messages.RunDialog_TrainDateError1);
			return;
		}
				
		super.okPressed();
	}
	
	
	@Override
	public Control createDialogArea(Composite parent){

		try(Session session = HibernateManager.openSession()){
			Collection<ConservationArea> cas;
			if (SmartDB.isMultipleAnalysis()) {
				cas = SmartDB.getConservationAreaConfiguration().getConservationAreas();
			}else {
				cas = Collections.singletonList(SmartDB.getCurrentConservationArea());
			}
			Integer startyear = session.createQuery("SELECT min(year(dateTime)) FROM Waypoint WHERE conservationArea IN (:cas)", Integer.class) //$NON-NLS-1$
					.setParameterList("cas",  cas) //$NON-NLS-1$
					.uniqueResult();
			if (startyear != null) smartDataStartYear = startyear;
			
		}
		
		Composite main = (Composite) super.createDialogArea(parent);

		errorColor = new Color(main.getDisplay(), 255, 230, 230);
		main.addListener(SWT.Dispose, e->errorColor.dispose());
		
		Composite outer = new Composite(main, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Composite header = new Composite(outer, SWT.NONE);
		header.setLayout(new GridLayout(4, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(header, SWT.NONE);
		l.setText(Messages.RunDialog_NameLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		txtId = new Text(header, SWT.BORDER);
		txtId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		if (id != null){
			txtId.setText(id);
		}else{
			txtId.setText(Messages.RunDialog_RunIdText);
		}
		
		int year = LocalDate.now().getYear();

		l = new Label(header, SWT.NONE);
		l.setText(Messages.RunDialog_TrainYearLabel);
		l.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
		
		Listener validate = e->validateInt((CCombo)e.widget);
		
		dtTrainStart = createDateDropDown(header, smartDataStartYear, LocalDate.now().getYear());
		dtTrainStart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		if (trainStart != null) {
			dtTrainStart.setText(String.valueOf(trainStart));
		}else {
			dtTrainStart.setText(String.valueOf(year));
		}
		dtTrainStart.addListener(SWT.Modify, validate);
		
		l = new Label(header, SWT.NONE);
		l.setText(Messages.RunDialog_To);
		
		dtTrainEnd = createDateDropDown(header, smartDataStartYear, LocalDate.now().getYear());
		dtTrainEnd.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		if (trainEnd != null) {
			dtTrainEnd.setText(String.valueOf(trainEnd));
		}else {
			dtTrainEnd.setText(String.valueOf(year));
		}
		dtTrainEnd.addListener(SWT.Modify, validate);
		
		
		l = new Label(header, SWT.NONE);
		l.setText(Messages.RunDialog_ForcaseYearLabel);
		
		dtForcastStart = createDateDropDown(header, LocalDate.now().getYear(), LocalDate.now().getYear()+20);
		dtForcastStart.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 3, 1));
		if (forcastStart != null) {
			dtForcastStart.setText(String.valueOf(forcastStart));
		}else {
			dtForcastStart.setText(String.valueOf(year + 1));
		}
		dtForcastStart.addListener(SWT.Modify, validate);

		setTitle(Messages.RunDialog_Title);
		getShell().setText(Messages.RunDialog_ShellTitle);
		setMessage(Messages.RunDialog_Message);
		return main;
	}

	@Override
	public boolean isResizable(){
		return true;
	}
	
	private CCombo createDateDropDown(Composite parent, int startyear, int endyear) {
		CCombo dd = new CCombo(parent, SWT.DROP_DOWN | SWT.BORDER);
		
		String[] items = new String[endyear-startyear + 1];
		for (int i = startyear; i<= endyear ; i ++) {
			items[i-startyear] = String.valueOf(i);
		}
		dd.setItems(items);
		
		return dd;
	}
}
