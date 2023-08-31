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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.IPlanFilterItem;
import org.wcs.smart.plan.ui.LoadPlanJob;
import org.wcs.smart.plan.ui.PlanFilterDialog;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Wizard page for collecting the plan template
 * @author egouge
 * @author jeffloun
 * @since 1.0.0
 */
public class TemplateSelectPlanWizardPage extends PlanWizardPage implements IPlanFilterItem {

	public static final String PAGENAME = Messages.TemplateSelectPlanWizardPage_PageName;
	
	private PlanViewer planTreeViewer;
	
	private PlanFilter currentFilter = new PlanFilter();
	private LoadPlanJob updateJob;
	
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
		center.setLayout(new GridLayout());
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Link lnkFilter = new Link(center, SWT.NONE);
		lnkFilter.setText(Messages.TemplateSelectPlanWizardPage_Filter_Link);
		lnkFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				PlanFilterDialog dialog = new PlanFilterDialog(getShell(), TemplateSelectPlanWizardPage.this);
				dialog.open();
			}
		});
		
		planTreeViewer = new PlanViewer(center, SWT.NONE);
		planTreeViewer.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4 , 1));
		planTreeViewer.refresh();
		
		updateJob = new LoadPlanJob(planTreeViewer, currentFilter);
		updateJob.schedule();
		
		setControl(center);
		super.setTitle(Messages.TemplateSelectPlanWizardPage_Title);
		setMessage(Messages.TemplateSelectPlanWizardPage_Message);
	
	}
	
	
	/**
	 * A job that initializes the query 
	 * filter options
	 */
	public void initModel(Plan p){
		
	}

	
	@Override
	public boolean updateModel(Plan p) {
		Plan source = null;
		
		Session s = ((CreatePlanWizard)getWizard()).getSession();
		PlanEditorInput inputPlan  = (PlanEditorInput) planTreeViewer.getSelectedPlan();
		if (inputPlan == null){
			return true;
		}
		s.beginTransaction();
		try{
			source = (Plan) s.get(Plan.class, inputPlan.getUuid());
			if (source == null){
				SmartPlanPlugIn.displayLog(Messages.TemplateSelectPlanWizardPage_PlanNotFound_Error, null);
				return false;
			}
			if (source.getParent() != null) {
				source.getParent().getStartDate();
				source.getParent().getEndDate();
			}
			
			//fetch real data for nested lazy elements (not lazy bags)
			if (source.getTeam() != null) {
				source.getTeam().getName();
			}
			if (source.getStation() != null) {
				source.getStation().getName();
			}
		
			p.setTemplatePlan(source);
			p.setName(source.getName());
			//p.setId(source.getId());  //DO NOT CLONE ID
			p.setDescription(source.getDescription());
			p.setStartDate(source.getStartDate());
			p.setEndDate(source.getEndDate());
			p.setStation(source.getStation());
			p.setTeam(source.getTeam());
				
			p.setParent(source.getParent());
			p.setType(source.getType());
			p.setUnavailableEmployees(source.getUnavailableEmployees());
				
			//clone the targets
			List<PlanTarget> tars = source.getTargets();
			List<PlanTarget> newTars = new ArrayList<PlanTarget>();
			for(PlanTarget x : tars){
				newTars.add(x.clone());
			}
			p.setTargets(newTars);
		}finally{
			s.getTransaction().rollback();
		}
		return true;
	}


	@Override
	public void updateContent() {
		updateJob.schedule();
	}


	@Override
	public PlanFilter getPlanFilter() {
		return currentFilter;
	}
	
}