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
package org.wcs.smart.er.ui.surveydesign;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.component.DatesComponent;

/**
 * Start and end dates survey design composite.
 * 
 * @author Emily
 *
 */
public class DateComposites extends SurveyDesignComposite {

	private DatesComponent dates;
	
	private SurveyDesign design;
	@Override
	public Control createControl(Composite parent) {
		dates = new DatesComponent(true);
		Control part = dates.createComposite(parent);
		
		dates.addModifiedListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fireChangeListeners();
			}
		});
		
		part.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		return part;
	}

	@Override
	public void init(SurveyDesign design) {
		this.design = design;
		dates.setStartDate(design.getStartDate());
		dates.setEndDate(design.getEndDate());
	}

	@Override
	public void updateDesign(SurveyDesign design) {
		design.setStartDate(dates.getStartDate());
		design.setEndDate(dates.getEndDate());
	}


	@Override
	public boolean isValid() {
		if (dates.getEndDate() != null && dates.getStartDate() != null){
			return !dates.getEndDate().before(dates.getStartDate());
		}
		return true;
	}
	
	@Override
	public String getTitle(){
		return "Survey Dates";
	}
	
	@Override
	public String getDescription(){
		return "Enter the time frame for the survey design (optional).";
	}
}
