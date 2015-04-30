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

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
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
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.internal.Messages;
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
		setMessage(Messages.PlanDatesComposite_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout());
        this.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        
        Composite center = new Composite(this, SWT.NONE);
        center.setLayout(new GridLayout(2, false));
        center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        
        Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PlanDatesComposite_StartDate);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
		dtStartDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		((GridData)dtStartDate.getLayoutData()).horizontalIndent = 10;
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.PlanDatesComposite_EndDate);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG | SWT.DATE);
		dtEndDate.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
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
	protected boolean updateModelInternal(Plan plan) {
		Date start = SmartUtils.getDate(dtStartDate);
		Date end = SmartUtils.getDate(dtEndDate);
		List<String> kids = PlanHibernateManager.getPlanChildrenOutOfDateRange(plan.getUuid(), start, end);
		if (!kids.isEmpty()) {
			MessageDialog.openError(getShell(), Messages.PlanDatesComposite_Title,
					MessageFormat.format(Messages.PlanDatesComposite_ClidRangeViolation_Message, Arrays.toString(kids.toArray())));
			return false;
		}
		plan.setStartDate(start);
		plan.setEndDate(end);
		return true;
	}

	@Override
	public void initFromModel(Plan plan){
		Plan thisParentPlan = null;
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			if(plan.getParent() != null){
				thisParentPlan = (Plan) session.load(Plan.class, plan.getParent().getUuid());
			}

			SmartUtils.initDateDateTimeWidget(dtStartDate, plan.getStartDate() != null ? plan.getStartDate() : new Date());
			SmartUtils.initDateDateTimeWidget(dtEndDate, plan.getEndDate() != null ? plan.getEndDate() : new Date());
			
			if(thisParentPlan != null){
				parentStartDate = thisParentPlan.getStartDate();
			}else{
				parentStartDate = null;
			}
			
			if(thisParentPlan != null){
				parentEndDate = thisParentPlan.getEndDate();
			}else{
				parentEndDate = null;
			}
			session.getTransaction().rollback();
		} finally {
			session.close();

		}

		validate();
	}
    
	protected void validate() {
		setErrorMessage(null);
		cdEndDate.hide();
		cdStartDate.hide();
		if(dtStartDate == null || dtEndDate == null){
			cdEndDate.show();
			cdEndDate.setDescriptionText(Messages.PlanDatesComposite_EndDate_Invalid_Error);
			setErrorMessage(Messages.PlanDatesComposite_EndDate_Invalid_Error);
		}

		if( (SmartUtils.getDate(dtEndDate)).before(SmartUtils.getDate(dtStartDate)) ){
			cdEndDate.show();
			cdEndDate.setDescriptionText(Messages.PlanDatesComposite_EndDate_Range_Error);
			setErrorMessage(Messages.PlanDatesComposite_EndDate_Range_Error);
		}
		
		if(parentEndDate != null && (SmartUtils.getDate(dtEndDate)).after(parentEndDate) ){
			cdEndDate.show();
			String errorText = Messages.PlanDatesComposite_EndDate_Range_Parent_Error + " (" + DateFormat.getDateInstance().format(parentEndDate) + ")";  //$NON-NLS-1$ //$NON-NLS-2$
			cdEndDate.setDescriptionText(errorText);
			setErrorMessage(errorText);
		}
		if(parentEndDate != null && (SmartUtils.getDate(dtStartDate)).before(parentStartDate) ){
			cdStartDate.show();
			String errText = Messages.PlanDatesComposite_StartDate_Range_Parent_Error + " (" + DateFormat.getDateInstance().format(parentStartDate) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			cdStartDate.setDescriptionText(errText);
			setErrorMessage(errText);
		}
	}

	@Override
	public String getTitle() {
		return Messages.PlanDatesComposite_Title;
	}
}
