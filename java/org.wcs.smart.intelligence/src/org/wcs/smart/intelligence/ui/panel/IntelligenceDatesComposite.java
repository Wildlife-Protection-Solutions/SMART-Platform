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
package org.wcs.smart.intelligence.ui.panel;

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting the intelligence relevant date(s) information
 *  
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceDatesComposite extends IntelligenceComposite {

    private Label dtFromLabel;
    private Label dtToLabel;

	private DateTime dtFromDate;
    private DateTime dtToDate;
    
    private Button multipleDays;

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceDatesComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceDates_Message);
		createControls();
		validate();
	}

	private void createControls() {
        this.setLayout(new GridLayout(1, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        multipleDays = new Button(this, SWT.CHECK);
        multipleDays.setText(Messages.IntelligenceDates_MutliDaysCheckbox_Label);
        multipleDays.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		applyCurrentState();
        		fireInputChangeListeners();
        	}
		});
        
        Composite dateComposite = new Composite(this, SWT.NONE);
        dateComposite.setLayout(new GridLayout(2, false));
        dateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        ((GridData)dateComposite.getLayoutData()).horizontalIndent = 15;
        
        dtFromLabel = new Label(dateComposite, SWT.NONE);
        dtFromLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        dtFromDate = new DateTime(dateComposite, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
        dtFromDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        ((GridData)dtFromDate.getLayoutData()).horizontalIndent = 10;
        dtFromDate.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		fireInputChangeListeners();
        	}
		});
        
        
        dtToLabel = new Label(dateComposite, SWT.NONE);
        dtToLabel.setText(Messages.IntelligenceDates_To_Label);
        dtToLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        dtToDate = new DateTime(dateComposite, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
        dtToDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        ((GridData)dtToDate.getLayoutData()).horizontalIndent = 10;
        dtToDate.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		fireInputChangeListeners();
        	}
		});
       
        applyCurrentState();
    }

    /**
     * updates gui state according to multipleDays checkbox state
     */
    private void applyCurrentState() {
		boolean isMultiple = multipleDays.getSelection();
		String labelText = isMultiple ? Messages.IntelligenceDates_From_Label : Messages.IntelligenceDates_Date_Label;
		dtFromLabel.setText(labelText);
		dtToLabel.setVisible(isMultiple);
		dtToDate.setVisible(isMultiple);
		//line below is required to avoid label truncation
		dtFromLabel.getParent().layout(true, true);
    }	

	@Override
	protected void updateModelInternal(Intelligence intelligence) {
        intelligence.setFromDate(SmartUtils.getDate(dtFromDate));
        Date toDate = multipleDays.getSelection() ? SmartUtils.getDate(dtToDate) : null;
        intelligence.setToDate(toDate);
	}

	@Override
	public void initFromModel(Intelligence intelligence) {
	    if (intelligence.getFromDate() != null) {
	    	SmartUtils.initDateDateTimeWidget(dtFromDate, intelligence.getFromDate());
	    }
	    if (intelligence.getToDate() != null) {
	    	SmartUtils.initDateDateTimeWidget(dtToDate, intelligence.getToDate());
	    	multipleDays.setSelection(true);
	    } else {
	    	dtToDate.setDate(dtFromDate.getYear(), dtFromDate.getMonth(), dtFromDate.getDay());
	    	multipleDays.setSelection(false);
	    }
	    applyCurrentState();
	}
    
	@Override
	protected void validate() {
		if (!isDateRangeValid()) {
			setErrorMessage(Messages.IntelligenceDatesComposite_DateRange_Error);
			return;
		}
		setErrorMessage(null);
	}
	
	private boolean isDateRangeValid() {
		if (multipleDays.getSelection()) {
			Date from = SmartUtils.getDate(dtFromDate);
			Date to = SmartUtils.getDate(dtToDate);
			return from.compareTo(to) <= 0; //from <= to
		}
		return true;
	}
}
