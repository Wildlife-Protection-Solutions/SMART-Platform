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
package org.wcs.smart.patrol.internal.ui;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.SmartUtils;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Patrol item composite for managing patrol start and end dates.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateComposite extends PatrolItemComposite implements SelectionListener{

	private DateTime dtStartDate;
	private DateTime dtEndDate;

	
	/**
	 * Creates a new composite
	 */
	public DateComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Patrol Start Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText("Patrol End Date:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		dtEndDate.addSelectionListener(this);
		dtStartDate.addSelectionListener(this);
		
		return center;
	}


	/**
	 * Alternative to setValues(Patrol).  Can be used if you don't 
	 * have a patrol you are updating, just the dates.  
	 * 
	 * @param startDate the start date
	 * @param endDate the end date
	 */
	public void setValues(Date startDate, Date endDate){
		Calendar cal = SmartUtils.convertDate(startDate);
        dtStartDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
        cal = SmartUtils.convertDate(endDate);
        dtEndDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
	}
	
	/**
	 * Gets the end date selected by the user with the time
	 * set to 23:59:59.
	 * 
	 * @return the end date selected by the user.
	 */
	public Date getEndDate(){
		return new Date(SmartUtils.getDate(dtEndDate).getTime() + 24*60*60*1000 - 1);
	}
	/**
	 * Gets the start date selected by the user with the time set to 0.
	 * @return the start date selected by the user.
	 */
	public Date getStartDate(){
		return SmartUtils.getDate(dtStartDate);
	}
	/**
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
	    if (p.getStartDate() != null){
	    	Calendar cal = SmartUtils.convertDate(p.getStartDate());
	        dtStartDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
	    }
	    if (p.getEndDate() != null){
	    	Calendar cal = SmartUtils.convertDate(p.getEndDate());
	    	dtEndDate.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
	    }
	}

	/**
	 * Updates the patrol with the new dates.  If the patrol has a single
	 * leg then the single patrol leg is also updated.
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(Patrol p) {
		p.setStartDate(getStartDate());
		p.setEndDate(getEndDate());		
		if (p.getLegs().size() == 1){
			p.getFirstLeg().setStartDate(p.getStartDate());
			p.getFirstLeg().setEndDate(p.getEndDate());
			p.createLegDays();
		}
		return true;
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return "Patrol Start and End Dates";
	}
	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (SmartUtils.getDate(dtStartDate).after(SmartUtils.getDate(dtEndDate))){
			setErrorMessage("End date must be after the start date.");
		}else{
			setErrorMessage(null);
		}
		fireChangeListeners();
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getAttribute()
	 */
	@Override
	public int getAttribute() {
		return PatrolEventManager.PATROL_DATES_LEG;
	}
}

