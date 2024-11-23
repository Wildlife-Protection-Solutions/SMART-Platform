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

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol item composite for managing patrol start and end dates.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DateComposite extends PatrolItemComposite implements SelectionListener{

	private DateTime dtStartDate;
	private DateTime dtEndDate;

	private ControlDecoration cdEndDate;
	
	/**
	 * Creates a new composite
	 */
	public DateComposite() {

	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#createComponent(org.eclipse.swt.widgets.Composite, int)
	 */
	public Composite createComponent(Composite parent, int style) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout(1, false));
		
		Composite center = new Composite(main, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.DateComposite_start);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.DateComposite_end);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG| SWT.DATE);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		cdEndDate = new ControlDecoration(dtEndDate, SWT.RIGHT);
		cdEndDate.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMG_DEC_FIELD_WARNING));
		cdEndDate.hide();
		
		dtEndDate.addSelectionListener(this);
		dtStartDate.addSelectionListener(this);
		
		return main;
	}


	/**
	 * Alternative to setValues(Patrol).  Can be used if you don't 
	 * have a patrol you are updating, just the dates.  
	 * 
	 * @param startDate the start date
	 * @param endDate the end date
	 */
	public void setValues(LocalDate startDate, LocalDate endDate){
		SmartUtils.initDateTimeWidget(dtStartDate, startDate);
		SmartUtils.initDateTimeWidget(dtEndDate, endDate);
	}
	
	/**
	 * Gets the end date selected by the user with the time
	 * set to 23:59:59.
	 * 
	 * @return the end date selected by the user.
	 */
	public LocalDate getEndDate(){
		return SmartUtils.toDate(dtEndDate);
	}
	/**
	 * Gets the start date selected by the user with the time set to 0.
	 * @return the start date selected by the user.
	 */
	public LocalDate getStartDate(){
		return SmartUtils.toDate(dtStartDate);
	}
	/**
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#setValues(org.wcs.smart.patrol.model.Patrol, org.hibernate.Session)
	 */
	public void setValues(Patrol p, Session session) {
	    if (p.getStartDate() != null){
	    	SmartUtils.initDateTimeWidget(dtStartDate, p.getStartDate());
	    }
	    if (p.getEndDate() != null){
	    	SmartUtils.initDateTimeWidget(dtEndDate, p.getEndDate());
	    }
	}

	/**
	 * Updates the patrol with the new dates.  If the patrol has a single
	 * leg then the single patrol leg is also updated.
	 * 
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#updatePatrol(org.wcs.smart.patrol.model.Patrol)
	 */
	public boolean updatePatrol(Patrol p, Session session) {
		if (p.getStartDate() != null && p.getEndDate() != null){
			if (getStartDate().isAfter(p.getStartDate()) || getEndDate().isBefore(p.getEndDate())){
				if (!MessageDialog.openQuestion(dtEndDate.getShell(), Messages.DateComposite_WarnTitle, Messages.DateComposite_WarnMessage)){
					return false;
				}
				
			}
		}
		p.setStartDate(getStartDate());
		p.setEndDate(getEndDate());		
		if (p.getLegs().size() == 1){
			p.getFirstLeg().setStartDate(p.getStartDate());
			p.getFirstLeg().setEndDate(p.getEndDate());
			p.createLegDays(session);
		}
		return true;
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.PatrolItemComposite#getTitle()
	 */
	@Override
	public String getTitle() {
		return LabelConstants.DATES;
	}
	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {
		String error = null;
		cdEndDate.hide();
		if (SmartUtils.toDate(dtStartDate).isAfter(SmartUtils.toDate(dtEndDate))){
			error = Messages.DateComposite_Error_EndAfterStart;
		}else{
			
			long days = ChronoUnit.DAYS.between(SmartUtils.toDate(dtStartDate), SmartUtils.toDate(dtEndDate));
			
			if (days > Patrol.MAX_PATROL_LENGTH_DAYS){
				error = MessageFormat.format(
							Messages.DateComposite_toolongerror,
							new Object[]{ Patrol.MAX_PATROL_LENGTH_DAYS});
			}else if(days >= Patrol.WARN_PATROL_LENGTH_DAYS ){
				cdEndDate.setDescriptionText(
						MessageFormat.format(
								Messages.DateComposite_tolongwarn,
								new Object[]{Patrol.WARN_PATROL_LENGTH_DAYS}));
				cdEndDate.show();
			}
		}
		
		setErrorMessage(error);
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

