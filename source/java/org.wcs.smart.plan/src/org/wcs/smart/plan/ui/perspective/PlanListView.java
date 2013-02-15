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
package org.wcs.smart.plan.ui.perspective;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanEventManager.EventType;
import org.wcs.smart.plan.PlanEventManager.IPlanEventListener;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.IPlanFilterItem;
import org.wcs.smart.plan.ui.LoadPlanJob;
import org.wcs.smart.plan.ui.editor.PlanEditor;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.tree.PlanViewer;

/**
 * A viewer where users can view all plans.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanListView extends ViewPart implements IPlanFilterItem {

	public static final String ID = "org.wcs.smart.plan.PlanListView"; //$NON-NLS-1$

	private PlanViewer planViewer;
	private PlanFilter currentFilter;
	private LoadPlanJob updateJob;
	

	/**
	 * listener for Plan change events.
	 */
	private IPlanEventListener planListener = new IPlanEventListener(){
		@Override
		public void eventFired(int type, Plan source) {
			updateContent();
		}
	};
	
	 IPartListener2 partListener = new IPartListener2(){
			@Override
			public void partVisible(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partOpened(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partHidden(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partClosed(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {}
			
			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals(PlanEditor.ID)){
					IWorkbenchPart part = partRef.getPart(false);
					if (part instanceof PlanEditor){
						planViewer.setSelection(((PlanEditor) part).getEditorInput());
					}
				}
			}
	    };
	
	/**
	 * Default constructor
	 */
	public PlanListView() {
		this.currentFilter = new PlanFilter();
	}
	

	public void dispose() {		
		getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_ADDED, planListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_DELETED, planListener);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		

		planViewer = new PlanViewer(main);
		planViewer.getViewer().getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4 , 1));
		
		PlanEventManager.getInstance().addListener(EventType.PLAN_ADDED, planListener);
		PlanEventManager.getInstance().addListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().addListener(EventType.PLAN_DELETED, planListener);
		
		TreeViewer control = planViewer.getViewer();
		control.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				//Load the plan into the main view window.
				PlanEditorInput input = (PlanEditorInput) planViewer.getSelectedPlan();
				if (input != null){
					try {
						IWorkbenchPage page = getSite().getPage();
						page.openEditor(input, PlanEditor.ID);						
					} catch (Throwable t) {
						SmartPlanPlugIn.displayLog(t.getLocalizedMessage(), t);
					}
				}
				
			}
		});
		updateJob = new LoadPlanJob(planViewer, currentFilter);
		updateJob.schedule();
				
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(planViewer.getViewer().getControl());
		planViewer.getViewer().getControl().setMenu(menu);
		
		getSite().registerContextMenu(menuManager,  planViewer.getViewer());
		getSite().setSelectionProvider(planViewer.getViewer());
		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
	}


	
	/**
	 * Refreshes the Plan list
	 */
	public void updateContent(){
		updateJob.cancel();
		updateJob.schedule();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		planViewer.getViewer().getControl().setFocus();

	}

	@Override
	public PlanFilter getPlanFilter() {
		return this.currentFilter;
	}
    
}
