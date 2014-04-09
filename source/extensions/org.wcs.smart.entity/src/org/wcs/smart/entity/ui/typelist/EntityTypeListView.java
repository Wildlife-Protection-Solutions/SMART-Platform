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

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.entity.EntityHibernateManager;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.event.EntityEventManager;
import org.wcs.smart.entity.event.IEntityListener;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.model.EntityTypeFilter;
import org.wcs.smart.entity.ui.IEntityTypeFilteringView;
import org.wcs.smart.entity.ui.editor.EntityTypeEditor;
import org.wcs.smart.entity.ui.editor.EntityTypeEditorInput;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * View containing a list of supported entity types.
 *  
 * @author Emily
 *
 */
public class EntityTypeListView extends ViewPart implements IEntityTypeFilteringView{
	
	public static final String ID = "org.wcs.smart.entity.typelist"; //$NON-NLS-1$
	private TableViewer entityListViewer;
	private EntityTypeFilter filter = new EntityTypeFilter();

	private Object[] loadingInput = new Object[]{Messages.EntityTypeListView_LoadingLabel};
	
	
	private IPartListener2 partListener = new IPartListener2() {
		
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
			if (partRef.getId().equals(EntityTypeEditor.ID)){
				IWorkbenchPart part = partRef.getPart(false);
				if (part instanceof EntityTypeEditor){
					entityListViewer.setSelection(new StructuredSelection(  ((EntityTypeEditor) part).getEditorInput() ));
					getSite().getPage().bringToTop(getSite().getPart());
				}
			}
			
		}
	};
	
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
					entityListViewer.setInput(loadingInput);
					entityListViewer.refresh();
				}
			});
			
			EntityTypeEditorInput[] input = null;
			if (SmartDB.isMultipleAnalysis()){
				List<EntityType> ets = EntityHibernateManager.getActiveEntityTypes();
				input = new EntityTypeEditorInput[ets.size()];
				int i = 0;
				for (EntityType et : ets){
					input[i++] = new EntityTypeEditorInput(null, et.getKeyId(), et.getName());
				}
				monitor.internalWorked(0.5);
			}else{
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					Query query = filter.buildQuery(s);
					List<?> results = query.list();
					input = new EntityTypeEditorInput[results.size()];
					int i = 0;
					for	(Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
						Object[] data = (Object[]) iterator.next();					
						input[i++] = new EntityTypeEditorInput((byte[])data[0], (String)data[1], (String)data[2]);
					}
					
					monitor.internalWorked(0.5);
				}finally{
					s.getTransaction().rollback();
					s.close();
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
		
	/**
	 * Creates a new vies
	 */
	public EntityTypeListView() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
	}

	public void dispose() {		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);		
		EntityEventManager.getInstance().removeListener(entityListener);
		super.dispose();
	}

	/**
	 * 
	 * @return the current filter
	 */
	public EntityTypeFilter getFilter() {
		return this.filter;
	}

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
		
		entityListViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		Table list = entityListViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		entityListViewer.setLabelProvider(new EntityTypeLabelProvider());
		entityListViewer.setContentProvider(ArrayContentProvider.getInstance());
		entityListViewer.setInput(loadingInput);
		entityListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();

		entityListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection)entityListViewer.getSelection()).getFirstElement();;
				if (!(selection instanceof EntityTypeEditorInput)){
					return;
				}
				EntityTypeEditorInput p = (EntityTypeEditorInput)selection;
				if (p != null){
					IWorkbenchPage page = null;
					try {
						page = getSite().getPage();
						page.openEditor(p, EntityTypeEditor.ID);						
					} catch (Throwable t) {
						EntityPlugIn.displayLog(t.getLocalizedMessage(), t);
					}
				}
				
			}
		});
		
		EntityEventManager.getInstance().addListener(entityListener);
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(entityListViewer.getControl());
		entityListViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager,  entityListViewer);
		getSite().setSelectionProvider(entityListViewer);
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
	
	@Override
	public void setFocus() {
		entityListViewer.getControl().setFocus();
	}
}