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
package org.wcs.smart.entity.ui.typelist;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.event.IEntityListener;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.ui.EntityTypeFilter;
import org.wcs.smart.entity.ui.IEntityTypeFilteringView;
import org.wcs.smart.entity.ui.OpenEntityTypeHandler;
import org.wcs.smart.entity.ui.editor.EntityTypeEditor;
import org.wcs.smart.entity.ui.editor.EntityTypeEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;

/**
 * View containing a list of supported entity types.
 *  
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class EntityTypeListView implements IEntityTypeFilteringView{
	
	public static final String ID = "org.wcs.smart.entity.typelist"; //$NON-NLS-1$
	private TableViewer entityListViewer;
	private EntityTypeFilter filter = new EntityTypeFilter();
	private boolean processevent = true;
	@Inject private IMenuService menuService;
	@Inject private MPart localPart;
	@Inject private ESelectionService selService;
	@Inject private IEclipseContext context;
	
	/*
	 * Job that updates the patrol list based on the current filter
	 */
	private Job updateJob = new Job(Messages.EntityTypeListView_RefreshJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.EntityTypeListView_LoadingProgress, 1);
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					entityListViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
					entityListViewer.refresh();
				}
			});
			
			EntityTypeEditorInput[] input = null;
			if (SmartDB.isMultipleAnalysis()){
				List<EntityType> ets = EntityHibernateManager.getInstance().getActiveEntityTypes();
				input = new EntityTypeEditorInput[ets.size()];
				int i = 0;
				for (EntityType et : ets){
					input[i++] = new EntityTypeEditorInput(null, et.getKeyId(), et.getName());
				}
				monitor.internalWorked(0.5);
			}else{
				try(Session s = HibernateManager.openSession()){
					s.beginTransaction();
					try{
						Query<EntityType> query = filter.buildQuery(s);
						List<EntityType> results = query.list();
						
						input = new EntityTypeEditorInput[results.size()];
						int i = 0;
						for	(EntityType t : results) {
							input[i++] = new EntityTypeEditorInput(t.getUuid(), t.getKeyId(), t.getName());
						}
						
						monitor.internalWorked(0.5);
					}finally{
						s.getTransaction().rollback();
					}
				}
			}
			
			if (input != null){
				final EntityTypeEditorInput[] inputs = input;
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						entityListViewer.setInput(inputs);
						entityListViewer.refresh();
					}
				});
			}
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * listener for entity change events.
	 */
	private IEntityListener entityListener = new IEntityListener() {
		
		@Override
		public void handleEvent(int eventType, Object source) {
			if (eventType == EntityEventManager.ENTITY_TYPE_ADDED || 
				eventType == EntityEventManager.ENTITY_TYPE_MODIFIED || 
				eventType == EntityEventManager.ENTITY_TYPE_DELETED){
				updateContent(1000);
			}
		}
	};
	
	@Inject
	@Optional
	public void partActivation(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		if (event.getProperty(UIEvents.EventTags.ELEMENT) != localPart) return;
		ShowFieldDataPerspective.enableToolbarItem(ID, context);
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		updateContent(100);
	}
	
	/**
	 * activated events
	 * @param partEvent
	 */
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent, EPartService pService){
		if (partEvent == null) return;
		if (!processevent) return;
		try {
			processevent = false;
			MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
			Object lpart = E3Utils.getSourceObject(activePart);
			if (lpart instanceof EntityTypeEditor){
				entityListViewer.setSelection(new StructuredSelection(((EntityTypeEditor)lpart).getEditorInput()));
				pService.bringToTop(localPart);
				pService.activate(activePart);
			}
		}finally {
			processevent = true;
		}
	}
	
	@PreDestroy
	public void dispose() {				
		EntityEventManager.getInstance().removeListener(entityListener);
	}

	/**
	 * 
	 * @return the current filter
	 */
	public EntityTypeFilter getFilter() {
		return this.filter;
	}

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
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tlayout = new TableColumnLayout();
		c.setLayout(tlayout);
		
		entityListViewer = new TableViewer(c, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		entityListViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col = new TableViewerColumn(entityListViewer, SWT.NONE);
		col.setLabelProvider(new EntityTypeLabelProvider());
		tlayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		
		entityListViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		updateContent();

		entityListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection)entityListViewer.getSelection()).getFirstElement();;
				if (!(selection instanceof EntityTypeEditorInput)){
					return;
				}
				EntityTypeEditorInput p = (EntityTypeEditorInput)selection;
				(new OpenEntityTypeHandler()).openEntityType(p, localPart.getContext().get(MWindow.class));
			}
		});
		entityListViewer.addSelectionChangedListener(new ViewerSelectionListener(selService));
		EntityEventManager.getInstance().addListener(entityListener);
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.entity.typelist"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(entityListViewer.getControl());
		entityListViewer.getControl().setMenu(menu);	
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
		entityListViewer.getControl().setFocus();
	}
	
	public static class EntityTypeListViewWrapper extends DIViewPart<EntityTypeListView>{
		public EntityTypeListViewWrapper(){
			super(EntityTypeListView.class);
		}
	}
}