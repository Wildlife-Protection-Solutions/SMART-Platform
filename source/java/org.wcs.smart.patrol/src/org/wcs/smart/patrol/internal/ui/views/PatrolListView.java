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
package org.wcs.smart.patrol.internal.ui.views;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.handlers.RadioState;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.PatrolTreeContentProvider.GroupByType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;

/**
 * A viewer where users can view all patrols by a specified filter.
 *   
 * 
 * @author Emily
 *
 */
public class PatrolListView implements IPatrolFilteringView {

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolListView"; //$NON-NLS-1$
	
	public static final String GROUP_BY_EVENT = "PATROL/GROUPBY"; //$NON-NLS-1$
	
	private TreeViewer patrolListViewer;
	private PatrolViewFilter filter = PatrolViewFilter.newInstance();
	
	private PatrolTreeContentProvider contentProvider;
	
	@Inject private IEclipseContext context;
	@Inject private IMenuService menuService;
	@Inject private MPart localPart;
	@Inject private ESelectionService selService;
	/*
	 * Job that updates the patrol list based on the current filter
	 */
	private Job updateJob = new Job(Messages.PatrolListView_UpdatePatrolJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.PatrolListView_Progress_LoadingPatrols, 1);
			patrolListViewer.getControl().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					patrolListViewer.setInput(DialogConstants.LOADING_TEXT);
					patrolListViewer.refresh();
				}
			});
			
			Session s = PatrolHibernateManager.openSession();
			s.beginTransaction();
			try{
				Query query = filter.buildQuery(s);
				List<?> results = query.list();
				final PatrolEditorInput[] input = new PatrolEditorInput[results.size()];
				int i = 0;
				for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
					Object[] data = (Object[]) iterator.next();					
					input[i++] = new PatrolEditorInput((UUID)data[0], (String)data[1], (PatrolType.Type)data[2], (Date)data[3], (Date)data[4]);
				}
				
				monitor.internalWorked(0.5);
				if (patrolListViewer.getControl().isDisposed()) return Status.CANCEL_STATUS;
				patrolListViewer.getControl().getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						patrolListViewer.setInput(input);
						patrolListViewer.refresh();
					}
				});
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * listener for patrol change events.
	 */
	private PatrolEventManager.IPatrolEventListener patrolListener = new IPatrolEventListener(){
		@Override
		public void eventFired(int type, Object source) {
			updateContent(1000);
		}
	};

	private IPatrolEventListener saveListener = new IPatrolEventListener() {
		@Override
		public void eventFired(final int attributeChanged, Object source) {
			if (attributeChanged == PatrolEventManager.PATROL_DATES_LEG){
				updateContent(1000);
			}
		}
	};
		
	/**
	 * Creates a new vies
	 */
	public PatrolListView() {
	}

	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent, EPartService pService){
		if (partEvent == null) return;
		MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
//		if (activePart.getElementId().equals(PatrolEditor.ID)){ //this doesn't work as it returns compatibility id; not editor id
		Object lpart = E3Utils.getSourceObject(activePart);
		if (lpart instanceof PatrolEditor){
			Patrol p = ((PatrolEditor)lpart).getPatrol();
			PatrolEditorInput pi = new PatrolEditorInput(p);	
			patrolListViewer.setSelection(new StructuredSelection(pi));
			pService.bringToTop(localPart);
		}
	}
	
	@Optional
	@Inject
	private void groupByChanged(@EventTopic(GROUP_BY_EVENT) Object data){
		if (!(data instanceof String)) return;
		contentProvider.setGroupBy((String)data);
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		updateContent(100);
	}
	
	@PreDestroy
	public void dispose() {		
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_SAVED, saveListener);
	}

	/**
	 * 
	 * @return the current filter
	 */
	public PatrolViewFilter getFilter() {
		return this.filter;
	}

	@PostConstruct
	public void createPartControl(Composite parent, final MApplication application) {
		
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
		
		patrolListViewer = new TreeViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		Control list = patrolListViewer.getControl();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		String defaultValue = null;
		Object value = context.get(ECommandService.class).getCommand("org.wcs.smart.patrol.view.groupby").getState(RadioState.STATE_ID).getValue(); //$NON-NLS-1$
		if (value instanceof String) defaultValue = (String)value;
		contentProvider = new PatrolTreeContentProvider(defaultValue);
		
		patrolListViewer.setLabelProvider(new PatrolTreeLabelProvider());
		patrolListViewer.setContentProvider(contentProvider);
		patrolListViewer.setInput(DialogConstants.LOADING_TEXT);
		patrolListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		patrolListViewer.addDoubleClickListener(new IDoubleClickListener() {
//			
//			@Override
//			public void doubleClick(DoubleClickEvent event) {
//				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
//				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
//					Object item = (Object) iterator.next();
//					boolean isExpanded = patrolListViewer.getExpandedState(item);
//					//patrolListViewer.setExpandedState(item, !isExpanded);
//				}
//				
//				
//			}
//		});
		updateContent();
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_SAVED, saveListener);
		
		patrolListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection)patrolListViewer.getSelection()).getFirstElement();;
				if (!(selection instanceof PatrolEditorInput)){
					return;
				}
				
				PatrolEditorInput p = (PatrolEditorInput)selection;
				if (p != null){
					IEclipseContext localCtx = EclipseContextFactory.create();
					localCtx.set(OpenPatrolHandler.PATROL_PARAM, p); 
					localCtx.setParent(localPart.getContext());
					ContextInjectionFactory.invoke(new OpenPatrolHandler(), Execute.class, localCtx);
				}				
			}
		});
		
		patrolListViewer.addSelectionChangedListener(new ViewerSelectionListener(selService));
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.patrol.ui.PatrolListView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(patrolListViewer.getControl());
		
		menu.addMenuListener(new MenuListener() {
			
			MenuItem mnuCollapseAll = null;
			MenuItem mnuExpandAll = null;
			@Override
			public void menuShown(MenuEvent e) {
				if (mnuCollapseAll == null){
					new MenuItem(menu,  SWT.SEPARATOR);
					
					mnuCollapseAll = new MenuItem(menu, SWT.PUSH);
					mnuCollapseAll.setText(Messages.PatrolListView_CollapseAllOp);
					mnuCollapseAll.addListener(SWT.Selection, x->{
						patrolListViewer.collapseAll();
					});
					
					mnuExpandAll = new MenuItem(menu, SWT.PUSH);
					mnuExpandAll.setText(Messages.PatrolListView_ExpandAllOp);
					mnuExpandAll.addListener(SWT.Selection, x->{
						patrolListViewer.expandAll();
					});
				}
				mnuCollapseAll.setEnabled(contentProvider.getGroupBy() != GroupByType.NONE);
				mnuExpandAll.setEnabled(contentProvider.getGroupBy() != GroupByType.NONE);
			}
			
			@Override
			public void menuHidden(MenuEvent e) { }
		});
		
		
		patrolListViewer.getControl().setMenu(menu);		
	}

	/**
	 * updates content immediately
	 */
	public void updateContent(){
		updateContent(0);
	}
	/**
	 * Refreshes the list of patrols after delay
	 */
	public void updateContent(int delay){
		updateJob.cancel();
		updateJob.schedule(delay);		
	}
	
	@Focus
	public void setFocus() {
		patrolListViewer.getControl().setFocus();
	}
	
	public static class PatrolListViewWrapper extends DIViewPart<PatrolListView>{
		public PatrolListViewWrapper(){
			super(PatrolListView.class);
		}
	}
}
