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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.menus.IMenuService;
import org.osgi.service.event.Event;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanEventManager.EventType;
import org.wcs.smart.plan.PlanEventManager.IPlanEventListener;
import org.wcs.smart.plan.filter.PlanFilter;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.IPlanFilterItem;
import org.wcs.smart.plan.ui.LoadPlanJob;
import org.wcs.smart.plan.ui.editor.PlanEditor;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.ui.handlers.OpenPlanHandler;
import org.wcs.smart.plan.ui.tree.PlanViewer;
import org.wcs.smart.util.E3Utils;

/**
 * A viewer where users can view all plans.
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanListView implements IPlanFilterItem {

	public static final String ID = "org.wcs.smart.plan.PlanListView"; //$NON-NLS-1$

	private PlanViewer planViewer;
	private PlanFilter currentFilter;
	private LoadPlanJob updateJob;
	
	@Inject private MPart part;
	@Inject private IMenuService menuService;
	
	/**
	 * listener for Plan change events.
	 */
	private IPlanEventListener planListener = new IPlanEventListener(){
		@Override
		public void eventFired(int type, Plan source) {
			updateContent();
		}
	};
	
	
	/**
	 * Default constructor
	 */
	public PlanListView() {
		this.currentFilter = new PlanFilter();
	}
	

	@PreDestroy
	public void dispose() {		
		PlanEventManager.getInstance().removeListener(EventType.PLAN_ADDED, planListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_MODIFIED, planListener);
		PlanEventManager.getInstance().removeListener(EventType.PLAN_DELETED, planListener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
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
					IEclipseContext localCtx = EclipseContextFactory.create();
					localCtx.set(OpenPlanHandler.PLANUUID_PARAM, input.getUuid());
					localCtx.setParent(part.getContext());
					ContextInjectionFactory.invoke(new OpenPlanHandler(), Execute.class, localCtx);
				}
				
			}
		});
		updateJob = new LoadPlanJob(planViewer, currentFilter);
		updateJob.schedule();
				
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.plan.PlanListView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(planViewer.getViewer().getControl());
		planViewer.getViewer().getControl().setMenu(menu);	
	}

	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
		if (partEvent == null) return;
		MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
		Object lpart = E3Utils.getSourceObject(activePart);
		if (lpart instanceof PlanEditor){
			planViewer.setSelection(((PlanEditor)lpart).getEditorInput());
		}
	}
	
	
	/**
	 * Refreshes the Plan list
	 */
	public void updateContent(){
		updateJob.cancel();
		updateJob.schedule();
	}
	
	@Focus
	public void setFocus() {
		planViewer.getViewer().getControl().setFocus();

	}

	@Override
	public PlanFilter getPlanFilter() {
		return this.currentFilter;
	}
    
	public static class PlanListViewWrapper extends DIViewPart<PlanListView>{
		public PlanListViewWrapper(){
			super(PlanListView.class);
		}
		
		@Override
		public void createPartControl(Composite parent){
			super.createPartControl(parent);
			
			getSite().setSelectionProvider(	((PlanListView)getComponent()).planViewer.getViewer() );
		}
	}
}
