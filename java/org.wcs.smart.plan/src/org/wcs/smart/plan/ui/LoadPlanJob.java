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
import org.wcs.smart.hibernate.SmartHibernateManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * Job to update a planviewer based on a plan filter.
 * 
 * @author Emily
 *
 */
public class LoadPlanJob extends Job {
	public static Object[] LOADING_PLANS = new Object[]{"Loading..."};
	
	private PlanViewer planViewer;
	private PlanFilter currentFilter;
	
	/**
	 * Creates a new job that updates the given viewer
	 * based on the values of the given filter.
	 * @param planViewer
	 * @param currentFilter
	 */
	public LoadPlanJob (PlanViewer planViewer, PlanFilter currentFilter){
		super("Load Plan Job");
		this.planViewer = planViewer;
		this.currentFilter = currentFilter;
	}
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Loading Plans", 1);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				planViewer.setRootPlans(LOADING_PLANS);
				planViewer.refresh();
			}
		});
		Session session = SmartHibernateManager.openSession();
		try{
			final List<PlanEditorInput> roots = PlanHibernateManager.getRootPlans(session, currentFilter);
			monitor.internalWorked(0.5);
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					planViewer.setRootPlans(roots.toArray(new Object[roots.size()]));
					planViewer.refresh();
					planViewer.getViewer().expandAll();
				}
			});
		}finally{
			session.close();
		}
		return Status.OK_STATUS;
	}

}
