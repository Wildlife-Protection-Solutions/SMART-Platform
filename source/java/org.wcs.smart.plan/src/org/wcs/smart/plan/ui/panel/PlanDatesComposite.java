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
package org.wcs.smart.plan.ui.panel;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.util.SmartUtils;

/**
 * Composite for collecting the plan relevant date(s) information
 *  
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanDatesComposite extends PlanComposite {

	private DateTime dtStartDate;
	private DateTime dtEndDate;

	
	private Date parentStartDate;
	private Date parentEndDate;
	
	private ControlDecoration cdEndDate;
	private ControlDecoration cdStartDate;

	/**
	 * @param parent
	 * @param style
	 */
	public PlanDatesComposite(Composite parent, int style) {
		super(parent, style);
		setMessage("Enter a name and description for the new Plan:");
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        
        Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Plan Start Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtStartDate = new DateTime(this, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)dtStartDate.getLayoutData()).horizontalIndent = 10;
		
		lbl = new Label(this, SWT.NONE);
		lbl.setText("Plan End Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(this, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		((GridData)dtEndDate.getLayoutData()).horizontalIndent = 10;
		
		cdEndDate = new ControlDecoration(dtEndDate, SWT.LEFT);
		cdEndDate.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        cdEndDate.setShowHover(true);
		cdEndDate.hide();
		
		cdStartDate = new ControlDecoration(dtStartDate, SWT.LEFT);
		cdStartDate.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        cdStartDate.setShowHover(true);
		cdStartDate.hide();

		Listener validate = new Listener() {
			@Override
			public void handleEvent(Event event) {
				fireInputChangeListeners();
			}
		};
		dtStartDate.addListener(SWT.Selection, validate);
		dtEndDate.addListener(SWT.Selection, validate);

    }

    	@Override
	public boolean updateModel(Plan plan) {
    		plan.setEndDate(SmartUtils.getDate(dtEndDate));
    		plan.setStartDate(SmartUtils.getDate(dtStartDate));
        return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		try{
			Calendar startDate = SmartUtils.convertDate(plan.getStartDate());
			Calendar endDate = SmartUtils.convertDate(plan.getEndDate());
			dtStartDate.setDate(startDate.get(Calendar.YEAR), startDate.get(Calendar.MONTH), startDate.get(Calendar.DATE));
			dtEndDate.setDate(endDate.get(Calendar.YEAR), endDate.get(Calendar.MONTH), endDate.get(Calendar.DATE));
			if(plan.getParent() != null){
				parentStartDate = plan.getParent().getStartDate();
			}else{
				parentStartDate = null;
			}
			
			if(plan.getParent() != null){
				parentEndDate = plan.getParent().getEndDate();
			}else{
				parentEndDate = null;
			}
		}catch (Exception e) {
			// OK, no data to update will give us Exceptions
		}
		isDataValid();

	}
    
	@Override
	public boolean isDataValid() {
		return isValidDates();
	}
	
	private boolean isValidDates() {
		boolean isValid = true;
		cdEndDate.hide();
		if(dtStartDate == null || dtEndDate == null){
			isValid = false;
			cdEndDate.show();
			cdEndDate.setDescriptionText("You must select valid dates.");
		}

		if( (SmartUtils.getDate(dtEndDate)).before(SmartUtils.getDate(dtStartDate)) ){
			isValid = false;
			cdEndDate.show();
			cdEndDate.setDescriptionText("End date must be after the start date.");
		}
		
		if(parentEndDate != null && (SmartUtils.getDate(dtEndDate)).after(parentEndDate) ){
			isValid = false;
			cdEndDate.show();
			cdEndDate.setDescriptionText("End date must not be after the Parent Plan's end date(" + parentEndDate + ")");
		}
		if(parentEndDate != null && (SmartUtils.getDate(dtStartDate)).before(parentStartDate) ){
			isValid = false;
			cdStartDate.show();
			cdStartDate.setDescriptionText("Start date must not be before the Parent Plan's end date(" + parentStartDate + ")");
		}
		return isValid;
	}
}
