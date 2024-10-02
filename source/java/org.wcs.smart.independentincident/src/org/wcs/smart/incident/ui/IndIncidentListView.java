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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IconCache;
import org.wcs.smart.ca.IconManager.Size;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IIncidentProvider;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.event.IIncidentListener;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.model.IncidentType;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.util.E3Utils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.Tuple;

/**
 * Incident list view.
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class IndIncidentListView implements IIncidentFilteringView {

	public static final String ID = "org.wcs.smart.observation.ui.incidientView"; //$NON-NLS-1$
	
	private TableViewer incidentListViewer;
	private IncidentFilter filter = new IncidentFilter();
	
	private Object[] loadingInput = new Object[]{Messages.IndIncidentListView_LoadingLabel};
	private boolean processevent = true;
	
	@Inject private IMenuService menuService;
	@Inject private MPart localPart;
	@Inject private ESelectionService selService;
	@Inject private IEclipseContext context;
		
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

			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					HashMap<UUID, IncidentType> types = new HashMap<>();
					for (IncidentType t : IncidentManager.getInstance().getIncidentTypes(s, false)) {
						types.put(t.getUuid(), t);
						HibernateManager.loadIcon(t, s);
					}
					
					Query<Tuple> query = filter.buildQuery(s);
					List<Tuple> results  = query.list();
					final IncidentEditorInput[] input = new IncidentEditorInput[results.size()];
					int i = 0;
					for (Iterator<Tuple> iterator = results.iterator(); iterator.hasNext();) {
						Tuple data = iterator.next();					
						UUID typeuuid = (UUID) data.get(4);
						input[i++] = new IncidentEditorInput((UUID)data.get(0), (String)data.get(1), 
								(LocalDateTime)data.get(2), (String)data.get(3), types.get(typeuuid));
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
				}
			}
			return Status.OK_STATUS;
		}
	};
	

	@Inject
	@Optional
	public void partActivation(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		if (event.getProperty(UIEvents.EventTags.ELEMENT) != localPart) return;
		ShowFieldDataPerspective.enableToolbarItem(ID, context);
	}
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent, EPartService pService){
		if (partEvent == null) return;
		if (!processevent) return;
		try {
			processevent = false;
			MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
			Object lpart = E3Utils.getSourceObject(activePart);
			
			if (lpart instanceof IEditorPart){
				String id = ((IEditorPart)lpart).getEditorSite().getId();
				for (IIncidentProvider p : IncidentManager.getInstance().getIncidentProviders()) {
					if (p.getEditorID().equals(id)) {
						incidentListViewer.setSelection(new StructuredSelection(((IEditorPart)lpart).getEditorInput()));
						pService.bringToTop(localPart);
						pService.activate(activePart);
						return;
					}
				}
				
			}
		}finally {
			processevent = true;
		}
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		updateContent(100);
	}
	
	@PreDestroy
	public void dispose() {		
		IncidentEventManager.getInstance().removeListener(ilistener);
	}


	@PostConstruct
	public void createPartControl(Composite parent) {
		localPart.getContext().set(IncidentFilter.class, filter);
		
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new TableColumnLayout());
		
		incidentListViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		incidentListViewer.getTable().setHeaderVisible(false);
		incidentListViewer.getTable().setLinesVisible(false);
		TableViewerColumn col1 = new TableViewerColumn(incidentListViewer, SWT.NONE);
		col1.setLabelProvider(new ColumnLabelProvider(){
			
			IconCache cache = new IconCache(null, Size.ICON);
			
			@Override
			public void dispose() {
				cache.dispose();
			}
			
			@Override
			public Image getImage(Object element){
				if (element instanceof IncidentEditorInput input){
					if (input.getType() == null || input.getType().getIcon() == null) {
						return input.getImage();
					}else {
						return cache.getImage(input.getType());
					}
				}
				return null;
			}
			
			@Override
			public String getText(Object element) {
				if (element instanceof IncidentEditorInput input){
					StringBuilder sb = new StringBuilder();
					sb.append(input.getId());
					sb.append(" ["); //$NON-NLS-1$
					sb.append(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(input.getDateTime()) );
					sb.append("]"); //$NON-NLS-1$
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		((TableColumnLayout)main.getLayout()).setColumnData(col1.getColumn(), new ColumnWeightData(100));
		
		incidentListViewer.setContentProvider(ArrayContentProvider.getInstance());
		incidentListViewer.setInput(loadingInput);
		
		updateContent();
		
		IncidentEventManager.getInstance().addListener(ilistener);
		
		incidentListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection)incidentListViewer.getSelection()).getFirstElement();;
				if (!(selection instanceof IncidentEditorInput)) return;
				(new OpenIncidentHandler()).openIncident(((IncidentEditorInput)selection).getUuid(),
						((IncidentEditorInput)selection).getSourceKey(),
						localPart.getContext().get(MWindow.class));
			}
		});
		incidentListViewer.addSelectionChangedListener(new ViewerSelectionListener(selService));
		
		/* add right click context menu */
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.observation.ui.incidientView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(incidentListViewer.getControl());
		incidentListViewer.getControl().setMenu(menu);	
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
	
	@Focus
	public void setFocus() {
		incidentListViewer.getControl().setFocus();
	}
	
	public static class IndIncidentListViewWrapper extends DIViewPart<IndIncidentListView>{
		public IndIncidentListViewWrapper(){
			super(IndIncidentListView.class);
		}
		
		@Override
		public void createPartControl(Composite parent){
			super.createPartControl(parent);
		}
	}
}
