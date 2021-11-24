/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.plan.internal.ui.patrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolPresentationContribution;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.PatrolPlan;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.handlers.OpenPlanHandler;
import org.wcs.smart.plan.ui.targets.TargetProgressViewer;
import org.wcs.smart.plan.ui.targets.TargetProgressViewer.TargetTableColumn;

/**
 * Plan contribution to patrol presentation view
 * 
 * @author Emily
 *
 */
public class PatrolPlanPresentationSummary implements IPatrolPresentationContribution {

	private TargetProgressViewer tblPlans ;
	private List<PlanTarget> targets;
	
	private Patrol patrol;
	
	@Inject
	private MWindow mwindow;
	
	public PatrolPlanPresentationSummary() {
	}

	@Override
	public Composite createSummaryWidget(Composite parent, Patrol patrol, Session session, FormToolkit toolkit) {
		this.patrol = patrol;
		
		List<PatrolPlan> plans = QueryFactory.buildQuery(session, PatrolPlan.class, 
				new Object[] {"id.patrol", patrol}).list(); //$NON-NLS-1$
		if (plans.isEmpty()) {
			return null;
		}
		
		Composite part = toolkit.createComposite(parent, SWT.NONE);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		Section summaryArea = toolkit.createSection(part, Section.TITLE_BAR);
    	summaryArea.setText(Messages.PatrolPlanPresentationSummary_SectionName);
    	summaryArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	Composite planArea = toolkit.createComposite(summaryArea);
    	planArea.setLayout(new GridLayout());
    	((GridLayout)planArea.getLayout()).marginWidth = 0;
    	((GridLayout)planArea.getLayout()).marginHeight = 0;
    	summaryArea.setClient(planArea);
    	planArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	tblPlans = new TargetProgressViewer(planArea, new TargetProgressViewer.TargetTableColumn[]{
    			TargetTableColumn.STATUS_ICON,
    			TargetTableColumn.PLANNAME,
				TargetTableColumn.TARGETNAME,
				TargetTableColumn.STATUS,
				TargetTableColumn.SUMMARY}, toolkit); 
    	tblPlans.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	tblPlans.getSummaryComposite().setLayout(new GridLayout(2, false));
    	((GridLayout)tblPlans.getSummaryComposite().getLayout()).marginWidth = 0;
    	((GridLayout)tblPlans.getSummaryComposite().getLayout()).marginHeight = 0;
    	ToolBar tb = new ToolBar(tblPlans.getSummaryComposite(), SWT.FLAT);
    	ToolItem tiRefresh = new ToolItem(tb, SWT.PUSH);
    	tiRefresh.setToolTipText(Messages.PatrolPlanPresentationSummary_refreshtooltip);
    	tiRefresh.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
    	tiRefresh.addListener(SWT.Selection,e->{
    		getTargetsJob.schedule();
    	});
    	
    	tblPlans.getViewer().addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				openPlan();
			}
		});
    	
    	Menu mnu = new Menu(tblPlans.getViewer().getControl());
    	MenuItem miOpen = new MenuItem(mnu, SWT.PUSH);
    	miOpen.setText(Messages.PatrolPlanPresentationSummary_OpenMenuItem);
    	miOpen.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
    	miOpen.addListener(SWT.Selection, e->openPlan());
    	mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				miOpen.setEnabled(tblPlans.getSelection().getFirstElement() instanceof PlanTarget );
			}
			
			@Override
			public void menuHidden(MenuEvent e) { }
		});
    	tblPlans.getViewer().getControl().setMenu(mnu);
    	
    	targets = new ArrayList<>();
    	for (PatrolPlan pp : plans) {
    		Plan plan = pp.getPlan();
    		while(plan != null) {
    			targets.addAll(plan.getTargets());
    			plan = plan.getParent();
    		}
    	}
    	
    	tblPlans.initValues(targets);
    	
    	computeStatus.schedule();
    	
    	return part;
	}
	
	private void openPlan() {
		Object x = tblPlans.getSelection().getFirstElement();
		if (x == null) return;
		if (!(x instanceof PlanTarget)) return;
		PlanTarget pt = (PlanTarget)x;
		Plan plan = pt.getPlan();
		(new OpenPlanHandler()).openPlan(plan.getUuid(), mwindow);
	}

	private Job getTargetsJob = new Job(Messages.PatrolPlanPresentationSummary_refreshjobname) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(()->{
				tblPlans.initValues(Collections.emptyList());
			});
			
			ArrayList<PlanTarget> ltargets = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<PatrolPlan> plans = QueryFactory.buildQuery(session, PatrolPlan.class, 
						new Object[] {"id.patrol", patrol}).list(); //$NON-NLS-1$
				for (PatrolPlan pp : plans) {
		    		Plan plan = pp.getPlan();
		    		while(plan != null) {
		    			ltargets.addAll(plan.getTargets());
		    			plan = plan.getParent();
		    		}
		    	}
			}
			
			PatrolPlanPresentationSummary.this.targets = ltargets;
			Display.getDefault().syncExec(()->{
				tblPlans.initValues(targets);
			});
			computeStatus.schedule();
			return Status.OK_STATUS;
		}
	};
	
	private Job computeStatus = new Job(Messages.TargetProgressViewer_ComputeTargetStatus_JobTitle){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (targets == null || targets.isEmpty()) return Status.OK_STATUS;
			
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					for (PlanTarget pt : targets){
						pt.refreshStatus(Locale.getDefault(), s);
					}
				}finally{
					s.getTransaction().rollback();
				}
			}
			Display.getDefault().asyncExec(new Runnable(){
				public void run(){
					if (tblPlans != null && !tblPlans.getViewer().getControl().isDisposed()) {
						tblPlans.refresh();
					}
				}
			});
			
			return Status.OK_STATUS;
		}};
}
