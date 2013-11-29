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
package org.wcs.smart.incident.ui;

import java.text.DateFormat;
import java.util.Date;
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
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IIncidentListener;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;

/**
 * Incident list view.
 * @author Emily
 *
 */
public class IndIncidentListView extends ViewPart implements IIncidentFilteringView {

	public static final String ID = "org.wcs.smart.observation.ui.incidientView"; //$NON-NLS-1$
	
	private TableViewer incidentListViewer;
	private IncidentFilter filter = new IncidentFilter();
	
	private Object[] loadingInput = new Object[]{Messages.IndIncidentListView_LoadingLabel};
	
	
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
			if (partRef.getId().equals(IncidentEditor.ID)){
				IWorkbenchPart part = partRef.getPart(false);
				if (part instanceof IncidentEditor){
					incidentListViewer.setSelection(new StructuredSelection(  ((IncidentEditor) part).getEditorInput() ));
					getSite().getPage().bringToTop(getSite().getPart());
				}
			}
			
		}
	};
	
	private IIncidentListener ilistener = new IIncidentListener() {
		
		@Override
		public void handleEvent(int eventType, Object source) {
			if (eventType == IncidentEventManager.INCIDENT_ADDED || 
					eventType == IncidentEventManager.INCIDENT_DELETED){
				updateContent(100);
			}else if (eventType == IncidentEventManager.INCIDENT_MODIFIED){
				//TODO: find a way to do this only if id or date updated
				updateContent(1000);
			}
		}
	};
	
	/*
	 * Job that updates the patrol list based on the current filter
	 */
	private Job updateJob = new Job(Messages.IndIncidentListView_UpdateJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.IndIncidentListView_LoadingLabel, 1);

			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				Query query = filter.buildQuery(s);
				List<?> results  = query.list();
				final IncidentEditorInput[] input = new IncidentEditorInput[results.size()];
				int i = 0;
				for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
					Object[] data = (Object[]) iterator.next();					
					input[i++] = new IncidentEditorInput((byte[])data[0], (Integer)data[1], (Date)data[2]);
				}
				
				monitor.internalWorked(0.5);
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						incidentListViewer.setInput(input);
						incidentListViewer.refresh();
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
	 * Creates a new vies
	 */
	public IndIncidentListView() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
		
		IncidentEventManager.getInstance().addListener(ilistener);
	}

	public void dispose() {		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);
		IncidentEventManager.getInstance().removeListener(ilistener);
		super.dispose();
	}

//	/**
//	 * 
//	 * @return the current filter
//	 */
//	public PatrolViewFilter getFilter() {
//		return this.filter;
//	}

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
		
		incidentListViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		Table list = incidentListViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		incidentListViewer.setLabelProvider(new LabelProvider(){
			
			@Override
			public Image getImage(Object element){
				if (element instanceof IncidentEditorInput){
					return IncidentPlugIn.getDefault().getImageRegistry().get(IncidentPlugIn.INCIDENT_ICON);
				}
				return null;
			}
			@Override
			public String getText(Object element) {
				if (element instanceof IncidentEditorInput){
					return ((IncidentEditorInput)element).getId() + "  [" + DateFormat.getDateInstance(DateFormat.SHORT).format( ((IncidentEditorInput)element).getDateTime()) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return super.getText(element);
			}
		});
		incidentListViewer.setContentProvider(ArrayContentProvider.getInstance());
		incidentListViewer.setInput(loadingInput);
		incidentListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();
//		
//		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, patrolListener);
//		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolListener);
//		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolListener);
		
		incidentListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection)incidentListViewer.getSelection()).getFirstElement();;
				if (!(selection instanceof IncidentEditorInput)){
					return;
				}
				IncidentEditorInput p = (IncidentEditorInput)selection;
				if (p != null){
					IWorkbenchPage page = null;
					try {
						page = getSite().getPage();
						page.openEditor(p, IncidentEditor.ID);							
					} catch (Throwable t) {
						IncidentPlugIn.displayLog(t.getLocalizedMessage(), t);
					}
				}
				
			}
		});
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		Menu menu = menuManager.createContextMenu(incidentListViewer.getControl());
		incidentListViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager,  incidentListViewer);
		getSite().setSelectionProvider(incidentListViewer);
	}
	
	public IncidentFilter getFilter(){
		return this.filter;
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
		incidentListViewer.getControl().setFocus();
	}
}
