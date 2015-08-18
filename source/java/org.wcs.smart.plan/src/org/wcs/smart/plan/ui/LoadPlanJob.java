package org.wcs.smart.plan.ui;
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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Job to update a planviewer based on a plan filter.
 * 
 * @author Emily
 *
 */
public class LoadPlanJob extends Job {
	public static Object[] LOADING_PLANS = new Object[]{Messages.LoadPlanJob_Loading};
	public static final String NONE_LABEL = Messages.LoadPlanJob_None;
	
	private PlanViewer planViewer;
	private PlanFilter currentFilter;
	
	private Object currentSelection;
	private boolean addNone;

	private boolean updatingViewerSelection = false;
	
	/**
	 * Creates a new job that updates the given viewer
	 * based on the values of the given filter.
	 * @param planViewer
	 * @param currentFilter
	 */
	public LoadPlanJob (PlanViewer planViewer, PlanFilter currentFilter){
		this(planViewer, currentFilter, false);
	}
	
	/**
	 * Creates a new job that updates the given viewer
	 * based on the values of the given filter.
	 * @param planViewer
	 * @param currentFilter
	 */
	public LoadPlanJob (PlanViewer planViewer, PlanFilter currentFilter, boolean addNone){
		super(Messages.LoadPlanJob_Title);
		this.planViewer = planViewer;
		this.currentFilter = currentFilter;
		this.addNone = addNone;
	}
	
	public void setDefaultSelection(Object defaultSelection){
		this.currentSelection = defaultSelection;
		setUpdatingViewerSelection(true);
		planViewer.setSelection(currentSelection);
		setUpdatingViewerSelection(false);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(Messages.LoadPlanJob_LoadPlans_Task, 1);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				planViewer.setRootPlans(LOADING_PLANS);
				planViewer.refresh();
			}
		});
		Session session = HibernateManager.openSession();
		try{
			final List roots = PlanHibernateManager.getRootPlans(session, currentFilter);
			if (addNone){
				roots.add(0, NONE_LABEL);
			}
			monitor.internalWorked(0.5);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					if (planViewer.getViewer().getTree().isDisposed()) return;
					planViewer.setRootPlans(roots.toArray());
					planViewer.refresh();
					planViewer.getViewer().expandAll();
					if (currentSelection != null){
						setUpdatingViewerSelection(true);
						planViewer.setSelection(currentSelection);
						setUpdatingViewerSelection(false);
					}
				}
			});
		}finally{
			session.close();
		}
		return Status.OK_STATUS;
	}

	public boolean isUpdatingViewerSelection() {
		return updatingViewerSelection;
	}

	private void setUpdatingViewerSelection(boolean updatingViewerSelection) {
		this.updatingViewerSelection = updatingViewerSelection;
	}

	
}
