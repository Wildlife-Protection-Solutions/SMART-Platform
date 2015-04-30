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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting the intelligence received date information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceReceivedComposite extends IntelligenceComposite {

   private DateTime dtReceivedDate;
	
   /**
	 * @param parent
	 * @param style
	 */
	public IntelligenceReceivedComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceReceived_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        Label lbl = new Label(this, SWT.NONE);
        lbl.setText(Messages.IntelligenceReceived_ReceivedDate_Label);
        lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        dtReceivedDate = new DateTime(this, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
        dtReceivedDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
        ((GridData)dtReceivedDate.getLayoutData()).horizontalIndent = 10;
        dtReceivedDate.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		validate();
        		fireInputChangeListeners();
        	}
		});
	}

	protected void validate(){
		String error = null;
		if (SmartUtils.getDate(dtReceivedDate).after(new Date())){
			error = Messages.IntelligenceReceivedComposite_FutureDate;
		}
		setErrorMessage(error);
	}
	@Override
	protected void updateModelInternal(Intelligence intelligence) {
		intelligence.setReceivedDate(SmartUtils.getDate(dtReceivedDate));
		if (intelligence.getFromDate() == null) {
			intelligence.setFromDate(intelligence.getReceivedDate());
		}
	}

	@Override
	public void initFromModel(Intelligence intelligence) {
	    if (intelligence.getReceivedDate() != null) {
	    	SmartUtils.initDateDateTimeWidget(dtReceivedDate, intelligence.getReceivedDate());
	    }
	}
}
