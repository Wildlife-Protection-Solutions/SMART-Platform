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

import java.text.DateFormat;
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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.ViewerSelectionListener;
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
	private TableViewer patrolListViewer;
	private PatrolViewFilter filter = new PatrolViewFilter();
	private Object[] loadingInput = new Object[]{Messages.PatrolListView_LoadingLabel};
	
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
			patrolListViewer.getTable().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					patrolListViewer.setInput(loadingInput);
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
				if (patrolListViewer.getTable().isDisposed()) return Status.CANCEL_STATUS;
				patrolListViewer.getTable().getDisplay().asyncExec(new Runnable() {
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
			PatrolEditorInput pi = new PatrolEditorInput(	((PatrolEditor)lpart).getPatrol().getUuid(), null, null, null, null);	
			patrolListViewer.setSelection(new StructuredSelection(pi));
			pService.bringToTop(localPart);
		}
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
		
		patrolListViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		Table list = patrolListViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		patrolListViewer.setLabelProvider(new LabelProvider(){
			
			@Override
			public Image getImage(Object element){
				if (element instanceof PatrolEditorInput){
					PatrolEditorInput p = (PatrolEditorInput)element;
					return PatrolUtils.getImage(p.getType());			
				}
				return null;
			}
			@Override
			public String getText(Object element) {
				if (element instanceof PatrolEditorInput){
					return ((PatrolEditorInput)element).getPatrolId() + "  [" + DateFormat.getDateInstance(DateFormat.SHORT).format( ((PatrolEditorInput)element).getStartDate()) + " - " + DateFormat.getDateInstance(DateFormat.SHORT).format( ((PatrolEditorInput)element).getEndDate()) + " ]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				return super.getText(element);
			}
		});
		patrolListViewer.setContentProvider(ArrayContentProvider.getInstance());
		patrolListViewer.setInput(loadingInput);
		patrolListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolListener);
		
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
					localCtx.set("patroluuid", p.getUuid()); //$NON-NLS-1$
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
