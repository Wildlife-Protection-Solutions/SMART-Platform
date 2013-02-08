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
package org.wcs.smart.plan.ui.newPlanWizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.tree.PlanViewer;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard page for collecting the plan template
 * @author egouge
 * @author jeffloun
 * @since 1.0.0
 */
public class TemplateSelectPlanWizardPage extends PlanWizardPage implements SelectionListener {

	public static final String PAGENAME = "PlanTemplate";
	
	private DateTime dtStartDate;
	private DateTime dtEndDate;
	private ControlDecoration cdEndDate;
	
	private PlanViewer planTreeViewer;
	private Plan lastSelection = null;
	
	
	/**
	 * 
	 */
	protected TemplateSelectPlanWizardPage() {
		super(PAGENAME);
		
	}

	
	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(4, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));


		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Only show plans from:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = 10;
		
		dtStartDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtStartDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		lbl = new Label(center, SWT.NONE);
		lbl.setText(" to ");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		dtEndDate = new DateTime(center, SWT.BORDER | SWT.DROP_DOWN | SWT.LONG);
		dtEndDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		cdEndDate = new ControlDecoration(lbl, SWT.RIGHT);
		cdEndDate.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_WARNING));
		cdEndDate.hide();
		
		dtEndDate.addSelectionListener(this);
		dtStartDate.addSelectionListener(this);
		

		planTreeViewer = new PlanViewer(center);
		planTreeViewer.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4 , 1));
		
		super.setTitle("Patrol Plan");
		
		setControl(center);
		setMessage("Select the Plan to use as a template:");
	
	}
	
	
	/**
	 * A job that initializes the query 
	 * filter options
	 */
	public void initModel(Plan p){
		List roots = PlanHibernateManager.getAllRootPlans(HibernateManager.openSession());
		planTreeViewer.setRootPlans(roots.toArray(new Object[roots.size()]));
		lastSelection = p.getTemplatePlan();
		if (lastSelection != null){
			planTreeViewer.setSelection(lastSelection);
		}
		

	}

	
	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(SelectionEvent e) {

		
		String error = null;
		cdEndDate.hide();
		if (SmartUtils.getDate(dtStartDate).after(SmartUtils.getDate(dtEndDate))){
			error = "End date must be after the start date.";
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
	
	@Override
	public boolean updateModel(Plan p) {
		Plan t = (Plan) planTreeViewer.getSelectedPlan();
		p.setTemplatePlan(t);
		
		//init our current plan with the values from the template selected
		//exceptions occur if there is no values yet, which is fine.
		try{
			p.setName(t.getName());
			p.setId(t.getId());
			p.setDescription(t.getDescription());
			p.setStartDate(t.getStartDate());
			p.setEndDate(t.getEndDate());
			p.setStation(t.getStation());
			p.setTeam(t.getTeam());
			Plan tmp = t.getParent();
			p.setParent(tmp);
			p.setType(t.getType());
			p.setUnavailableEmployees(t.getUnavailableEmployees());
			
			List<PlanTarget> tars = t.getTargets();
			List<PlanTarget> newTars = new ArrayList<PlanTarget>();
			for(PlanTarget x : tars){
//				x.setPlan(p);
//can't do this yet as the plan doesn't really exist? Hibernate unsaved transient issues.
// it is done in the performFinish() method of the wizard
				
				newTars.add(x.clone());
			}
			p.setTargets(newTars);
		}catch(Exception e){
			//SmartPlanPlugIn.displayLog("Could not clone Plan. " + e.getMessage(), e);
			//this will occur sometimes if you hit next or back without selecting anything, which is not a problem
		}
		
		return true;
	}
	
}