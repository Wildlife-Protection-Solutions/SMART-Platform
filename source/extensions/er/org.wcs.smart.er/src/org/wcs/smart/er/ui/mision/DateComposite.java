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
package org.wcs.smart.er.ui.mision;

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.ui.component.DatesComponent;
/**
 * Mission dates composite
 * 
 * @author Emily
 *
 */
public class DateComposite extends MissionComposite {

	private DatesComponent dates;
	private Mission mission;
	
	@Override
	public Control createControl(Composite parent) {
		dates = new DatesComponent(false);
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
	public void init(Mission mission, Session session) {
		this.mission = mission;
		dates.setStartDate(mission.getStartDate());
		dates.setEndDate(mission.getEndDate());
	}

	@Override
	public void updateDesign(Mission mission) {
		mission.setStartDate(dates.getStartDate());
		mission.setEndDate(dates.getEndDate());
	}


	@Override
	public boolean isValid() {
		if (dates.getEndDate() != null && dates.getStartDate() != null){
			if (dates.getEndDate().before(dates.getStartDate())){
				return false;
			}
		}
		
		if (dates.getStartDate().before( mission.getSurvey().getStartDate()) ||
			dates.getEndDate().before(mission.getSurvey().getStartDate()) ||
			dates.getEndDate().after(mission.getSurvey().getEndDate()) ||
			dates.getStartDate().after(mission.getSurvey().getEndDate())){
			
			dates.setError(MessageFormat.format(
					"The missions dates do not fall within the survey dates ({0} to {1}).",
					new Object[]{DateFormat.getDateInstance().format(mission.getSurvey().getStartDate()),
							DateFormat.getDateInstance().format(mission.getSurvey().getEndDate())}));
					
			return false;
		}
		return true;
	}
	
	@Override
	public String getTitle(){
		return "Mission Dates";
	}
	
	@Override
	public String getDescription(){
		return "Enter the time frame for the mission.";
	}
}
