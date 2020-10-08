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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.wcs.smart.er.internal.Messages;
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
			if (dates.getEndDate().isBefore(dates.getStartDate())){
				return false;
			}
		}
		
		LocalDate startD = dates.getStartDate();
		LocalDate endD = dates.getEndDate();
		long diff = ChronoUnit.DAYS.between(startD,  endD); 
		if ( diff > Mission.MAX_MISSION_LENGTH_DAYS ){
			String error = MessageFormat.format(
						Messages.DateComposite_MaxMissionLength,
						new Object[]{ Mission.MAX_MISSION_LENGTH_DAYS});
			dates.setError(error);
			return false;
		}else if(diff > Mission.WARN_MISSION_LENGTH_DAYS ){
			String warning = 
					MessageFormat.format(
							Messages.DateComposite_WarnMissionLength,
							new Object[]{Mission.WARN_MISSION_LENGTH_DAYS});
			dates.setWarning(warning);
		}
		return true;
	}
	
	@Override
	public String getTitle(){
		return Messages.DateComposite_Title;
	}
	
	@Override
	public String getDescription(){
		return Messages.DateComposite_Description;
	}
}
